package reka.core.bundle;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static reka.api.Path.slashes;
import static reka.configurer.Configurer.configure;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.configurer.Configurer.Preconditions.invalidConfig;
import static reka.core.builder.FlowSegments.par;
import static reka.core.builder.FlowSegments.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.SingleFlow;
import reka.core.config.ConfigurerProvider;
import reka.core.runtime.NoFlow;
import reka.core.runtime.NoFlowVisualizer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class UseConfigurer {
	
	public static class UsesInitializer {
		
		private final Flow flow;
		private final FlowVisualizer visualizer;
		private final Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers;
		private final Map<Path,Supplier<TriggerConfigurer>> triggers;
		private final List<Runnable> shutdownHandlers;
		
		UsesInitializer(Flow initialize, FlowVisualizer visualizer, 
				Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers,
				Map<Path,Supplier<TriggerConfigurer>> triggers,
				List<Runnable> shutdownHandlers) {
			
			this.flow = initialize;
			this.visualizer = visualizer;
			this.providers = ImmutableMap.copyOf(providers);
			this.triggers = ImmutableMap.copyOf(triggers);
			this.shutdownHandlers = ImmutableList.copyOf(shutdownHandlers);
			
		}
		
		public Flow flow() {
			return flow;
		}
		
		public FlowVisualizer visualizer() {
			return visualizer;
		}
		
		public Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers() {
			return providers;
		}
		
		public Map<Path,Supplier<TriggerConfigurer>> triggers() {
			return triggers;
		}
		
		public List<Runnable> shutdownHandlers() {
			return shutdownHandlers;
		}
		
	}
	
	public static UsesInitializer process(UseConfigurer root) {
		return Utils.process(root);
	}
	
	private static class Utils {
		
		public static UsesInitializer process(UseConfigurer root) {
			
			Set<UseConfigurer> all = collect(root, new HashSet<>());
			Set<UseConfigurer> toplevel = findTopLevel(all);
			Map<String,UseConfigurer> rootsMap = map(root.uses);
			resolveNamedDependencies(all, rootsMap);
			
			Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providersCollector = new HashMap<>();
			Map<Path,Supplier<TriggerConfigurer>> triggerCollector = new HashMap<>();
			List<Runnable> shutdownHandlers = new ArrayList<>();
			
			Map<UseConfigurer,FlowSegment> segments = makeSegments(all, providersCollector, triggerCollector, shutdownHandlers);
			
			Optional<FlowSegment> segment = buildSegment(toplevel, segments);
			
			if (segment.isPresent()) {
				
				Entry<Flow, FlowVisualizer> entry = SingleFlow.create(Path.path("initialize"), segment.get(), Data.NONE);
				
				return new UsesInitializer(entry.getKey(), 
										   entry.getValue(), 
										   providersCollector,
										   triggerCollector,
										   shutdownHandlers);
				
			} else {
				return new UsesInitializer(new NoFlow(), new NoFlowVisualizer(), providersCollector, triggerCollector, shutdownHandlers);
			}
			
		}
		
		private static Map<UseConfigurer, FlowSegment> makeSegments(Collection<UseConfigurer> all, 
				Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providersCollector,
				Map<Path,Supplier<TriggerConfigurer>> triggerCollector,
				List<Runnable> shutdownHandlers) {
			
			Map<UseConfigurer,FlowSegment> map = new HashMap<>();
			for (UseConfigurer use : all) {
				UseInit init = new UseInit(use.fullPath());
				use.setup(init);
				Optional<FlowSegment> segment = init.buildFlowSegment();
				if (segment.isPresent()) {
					map.put(use, segment.get());
				}
				
				providersCollector.putAll(init.providers());
				triggerCollector.putAll(init.triggers());
				shutdownHandlers.addAll(init.shutdownHandlers());
				
			}
			return map;
		}
		
		private static Optional<FlowSegment> buildSegment(Set<UseConfigurer> uses, Map<UseConfigurer, FlowSegment> built) {
			
			List<FlowSegment> segments = new ArrayList<>();
			for (UseConfigurer use : uses) {
				Optional<FlowSegment> seg = buildSegment(use, built);
				if (seg.isPresent()) {
					segments.add(seg.get());
				}
			}
			
			return segments.isEmpty() ? Optional.absent() : Optional.of(par(segments));
			
		}
		
		private static Optional<FlowSegment> buildSegment(UseConfigurer use, Map<UseConfigurer, FlowSegment> built) {
			if (use.isRoot()) return Optional.absent();
			
			List<FlowSegment> sequence = new ArrayList<>();
			
			if (built.containsKey(use)) {
				sequence.add(built.get(use));
			}
			
			Optional<FlowSegment> c = buildSegment(use.usedBy, built);
			if (c.isPresent()) {
				sequence.add(c.get());
			}
			
			return sequence.isEmpty() ? Optional.absent() : Optional.of(seq(sequence));
		}
		
		private static void resolveNamedDependencies(Set<UseConfigurer> all, Map<String, UseConfigurer> allMap) {
			for (UseConfigurer use : all) {
				for (String depname : use.usesNames) {
					UseConfigurer dep = allMap.get(depname);
					checkNotNull(dep, "missing dependency: [%s] uses [%s]", use.name(), depname);
					dep.usedBy.add(use);
					use.uses.add(dep);
				}
			}
		}

		private static Set<UseConfigurer> findTopLevel(Collection<UseConfigurer> uses) {
			Set<UseConfigurer> roots = new HashSet<>();
			for (UseConfigurer use : uses) {
				if (use.uses.isEmpty()) {
					roots.add(use);
				}
			}
			return roots;
		}
		
		private static Set<UseConfigurer> collect(UseConfigurer use, Set<UseConfigurer> collector) {
			collector.add(use);
			for (UseConfigurer child : use.uses) {
				collect(child, collector);
			}
			return collector;
		}
		
		public static Map<String,UseConfigurer> map(Collection<UseConfigurer> uses) {
			Map<String,UseConfigurer> map = new HashMap<>();
			for (UseConfigurer use : uses) {
				map.put(use.name(), use);
			}
			return map;
		}
		
	}
	
	private List<Entry<Path, Supplier<UseConfigurer>>> mappings = new ArrayList<>();
	
	private String type;
	private String name;
	
	private boolean isRoot;
	
	private Path parentPath = Path.root();
	private Path path = Path.root();
	
	private final List<String> usesNames = new ArrayList<>();
	
	private final Set<UseConfigurer> usedBy = new HashSet<>();
	private final Set<UseConfigurer> uses = new HashSet<>();

	public UseConfigurer mappings(List<Entry<Path, Supplier<UseConfigurer>>> mappings) {
		this.mappings = mappings;
		
		if (isRoot()) {
			findRootConfigurers();
		}
		
		return this;
	}

	private void findRootConfigurers() {
		for (Entry<Path, Supplier<UseConfigurer>> e : mappings) {
			// all the ones with a root path need to be added automatically
			// we don't need to explicitly load these...
			if (e.getKey().isEmpty()) {
				uses.add(e.getValue().get());
			}
		}
	}

	public abstract void setup(UseInit use);
	
	public boolean isRoot() {
		return isRoot;
	}
	
	public UseConfigurer isRoot(boolean val) {
		isRoot = val;
		return this;
	}
	
	public UseConfigurer path(Path path) {
		this.path = path;
		return this;
	}

	@Conf.Key
	public UseConfigurer type(String val) {
		type = val;
		return this;
	}
	
	@Conf.Val
	public UseConfigurer name(String val) {
		name = val;
		return this;
	}
	
	public String name() {
		return name != null ? name : type;
	}
	
	public String getName() {
		return name();
	}
	
	private Path fullPath() {
		return parentPath.add(slashes(name()));
	}
	
	public String typeAndName() {
		if (type.equals(name())) {
			return type;
		} else {
			return format("%s/%s", type, name());
		}
	}
	
	@Conf.Each("use")
	public void use(Config config) {
		if (config.hasBody()) {
			for (Config childConfig : config.body()) {

				checkConfig(mappings != null, "'%s' is not a valid module (try one of %s)", childConfig.key(), mappingNames());
				
				Supplier<UseConfigurer> supplier = mappingFor(slashes(childConfig.key()));
				
				checkConfig(supplier != null, "'%s' is not a valid module (try one of %s)", childConfig.key(), mappingNames());
				
				UseConfigurer child = supplier.get();
				
				child.mappings(mappings);
				
				child.parentPath(path);
				
				configure(child, childConfig);
				
				uses.add(child);
				child.usedBy.add(this);
				
			}
		} else if (config.hasValue()) {
			usesNames.add(config.valueAsString());
		} else {
			invalidConfig("must have body or value (referencing another dependency)");
		}
	}
	
	private void parentPath(Path val) {
		parentPath = val;
	}

	private Supplier<UseConfigurer> mappingFor(Path path) {
		for (Entry<Path,Supplier<UseConfigurer>> e : mappings) {
			if (e.getKey().equals(path)) {
				return e.getValue();
			}
		}
		return null;
	}
	
	private Collection<String> mappingNames() {
		List<String> result = new ArrayList<>();
		for (Entry<Path,Supplier<UseConfigurer>> e : mappings) {
			if (!e.getKey().isEmpty()) {
				result.add(e.getKey().slashes());
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		return format("%s(\n    name %s\n    params %s)", type, name(), usesNames);
	}

}
