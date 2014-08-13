package reka.elasticsearch;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class ElasticsearchModule extends ModuleConfigurer {
	
	private final List<String> indices = new ArrayList<>();
	
	@Conf.Each("index")
	public void index(Config config) {
		if (config.hasValue()) {
			indices.add(config.valueAsString());
		}
	}

	@Override
	public void setup(ModuleInit use) {
		
		AtomicReference<Client> clientRef = new AtomicReference<>();
		
		Path base = Path.path("stuff").add(use.path());
		Path nodePath = base.add("node");
		Path clientPath = base.add("client");
		
		Function<Data,Client> clientFn = data -> data.getContent(clientPath).orElseThrow(() -> runtime("waaaah!")).valueAs(Client.class);
		
		use.init("start node", (data) -> {

			Settings settings = ImmutableSettings.settingsBuilder()
					  .classLoader(Settings.class.getClassLoader())
						// TODO: not sure about this stuff!
	  				  .put("gateway.type", "none")
					  .build();
			
			NodeBuilder builder = nodeBuilder()
					.data(true)
					.local(true)
					.clusterName("james")
					.settings(settings);
			
			Node node = builder.build();
			
			node.start();
			Client client = node.client();
			
			clientRef.set(client);
			
			return data.put(nodePath, nonSerializableContent(node))
					   .put(clientPath, nonSerializableContent(client));
			
		});
		
		use.initAsync("wait for it to go yellow", withClient(clientPath, (client) -> {
			return client.admin().cluster().prepareHealth().setWaitForYellowStatus();
		}));

		
		use.initAsync("list indices", (data) -> {
			
			SettableFuture<MutableData> future = SettableFuture.create();
			
			clientFn.apply(data).admin().cluster().prepareState().execute().addListener(new ActionListener<ClusterStateResponse>(){

				@Override
				public void onResponse(ClusterStateResponse response) {
					for (IndexMetaData imd : response.getState().getMetaData()) {
						System.err.printf("found index [%s]\n", imd.getIndex());
						data.putBool(Path.path("indices", imd.getIndex()), true);
					}
					future.set(data);
				}

				@Override
				public void onFailure(Throwable e) {
					future.setException(e);
				}
				
			});
			
			return future;
		});
		
		use.initParallel((run) -> {
			
			for (String index : indices) {
				
				run.sequential(format("index [%s]", index), (seq) -> {
					
					seq.runAsync(format("create %s (if not exists)", index), (data) -> {
						SettableFuture<MutableData> future = SettableFuture.create();
						
						if (data.existsAt(Path.path("indices", index))) {
							future.set(data);
							return future;
						}
						
						clientFn.apply(data).admin().indices().prepareCreate(index)
							.execute().addListener(new ActionListener<CreateIndexResponse>(){

							@Override
							public void onResponse(CreateIndexResponse response) {
								future.set(data);
							}

							@Override
							public void onFailure(Throwable e) {
								future.setException(e);
							}
							
						});
						
						return future;
					});
					
					seq.run(format("add mappings", index), (data) -> data);
					
				});
				
			}
			
		});

		use.operation(asList(path("query"), path("q"), root()), () -> new ElasticsearchQueryConfigurer(clientRef));
		use.operation(path("index"), () -> new ElasticsearchIndexConfigurer(clientRef));
		
	}
	
	private AsyncOperation withClient(Path clientPath, Function<Client, ActionRequestBuilder<?,?,?>> f) {
		return (data) -> {
			Client client = data.getContent(clientPath).orElseThrow(() -> runtime("waaaah!")).valueAs(Client.class);
			ListenableFuture<?> lf = new ListenableElasticsearchFuture<>(f.apply(client).execute());
			return Futures.transform(lf, new com.google.common.base.Function<Object,MutableData>(){

				@Override
				public MutableData apply(Object obj) {
					return data;
				}
				
			});
		};
	}

}
