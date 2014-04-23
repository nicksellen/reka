package reka.elasticsearch;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class UseElasticsearch extends UseConfigurer {
	
	private final List<String> indices = new ArrayList<>();
	
	@Conf.Each("index")
	public void index(Config config) {
		if (config.hasValue()) {
			indices.add(config.valueAsString());
		}
	}

	@Override
	public void setup(UseInit use) {
		
		AtomicReference<Client> clientRef = new AtomicReference<>();
		
		Path base = Path.path("stuff").add(fullPath());
		Path nodePath = base.add("node");
		Path clientPath = base.add("client");
		
		use.run("start node", (data) -> {
			
			NodeBuilder builder = nodeBuilder()
					.data(true)
					.local(true)
					.clusterName("james");
					
			// TODO: not sure about this stuff!
			builder.settings()
				.put("gateway.type", "none")
				.put("index.gateway.type", "none");
			
			Node node = builder.build();
			
			node.start();
			Client client = node.client();
			
			clientRef.set(client);
			
			return data.put(nodePath, nonSerializableContent(node))
					   .put(clientPath, nonSerializableContent(client));
			
		});
		
		use.runAsync("wait for it to go yellow", withClient(clientPath, (client) -> {
			return client.admin().cluster().prepareHealth().setWaitForYellowStatus();
		}));
		
		use.parallel((run) -> {
			
			for (String index : indices) {
				
				run.sequential(format("index [%s]", index), (seq) -> {
					
					seq.runAsync(format("create", index), withClient(clientPath, (client) -> {
						return client.admin().indices().prepareCreate(index);
					}));
					
					seq.run(format("add mappings", index), (data) -> data);
					
				});
				
			}
			
		});

		use.operation(asList("query", "q", ""), () -> new ElasticsearchQueryConfigurer(clientRef));
		use.operation(asList("index"), () -> new ElasticsearchIndexConfigurer(clientRef));
		
	}
	
	private AsyncOperation withClient(Path path, Function<Client,ActionRequestBuilder<?,?,?>> f) {
		return (data) -> {
			Client client = data.getContent(path).orElseThrow(() -> runtime("waaaah!")).valueAs(Client.class);
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
