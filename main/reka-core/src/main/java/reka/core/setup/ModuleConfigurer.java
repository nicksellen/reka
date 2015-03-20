package reka.core.setup;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.config.configurer.Configurer.Preconditions.invalidConfig;
import static reka.core.builder.FlowSegments.createParallelSegment;
import static reka.core.builder.FlowSegments.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.PortRequirement;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.app.IdentityAndVersion;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.SingleFlow;
import reka.core.module.ModuleInfo;
import reka.core.runtime.NoFlow;
import reka.core.runtime.NoFlowVisualizer;
import reka.core.setup.ModuleSetup.ApplicationCheck;
import reka.dirs.AppDirs;

public abstract class ModuleConfigurer {

	public static class ModuleInitializer {

		private final Flow flow;
		private final FlowVisualizer visualizer;
		private final ModuleCollector collector;

		ModuleInitializer(Flow initialize, FlowVisualizer visualizer, ModuleCollector collector) {
			this.flow = initialize;
			this.visualizer = visualizer;
			this.collector = collector;
		}

		public Flow flow() {
			return flow;
		}

		public FlowVisualizer visualizer() {
			return visualizer;
		}

		public ModuleCollector collector() {
			return collector;
		}

	}

	public static ModuleInitializer buildInitializer(IdentityAndVersion idv, ModuleConfigurer root) {
		return Utils.process(idv, root);
	}

	public static class ModuleCollector {

		public final Map<Path, FlowSegmentBiFunction> providers;
		public final List<InitFlow> initflows;
		public final List<TriggerCollection> triggers;
		public final List<Runnable> shutdownHandlers;
		public final List<Supplier<StatusProvider>> statuses;
		public final List<Consumer<ApplicationCheck>> checks;
		public final List<PortRequirement> portRequirements;
		
		public ModuleCollector() {
			providers = new HashMap<>();
			initflows = new ArrayList<>();
			triggers = new ArrayList<>();
			shutdownHandlers = new ArrayList<>();
			statuses = new ArrayList<>();
			checks = new ArrayList<>();
			portRequirements = new ArrayList<>();
		}

		private ModuleCollector(ModuleCollector parent) {
			this.providers = immutable(parent.providers);
			this.initflows = immutable(parent.initflows);
			this.triggers = immutable(parent.triggers);
			this.shutdownHandlers = immutable(parent.shutdownHandlers);
			this.statuses = immutable(parent.statuses);
			this.checks = immutable(parent.checks);
			this.portRequirements = immutable(parent.portRequirements);
		}

		public ModuleCollector immutable() {
			return new ModuleCollector(this);
		}

		private static <K, V> Map<K, V> immutable(Map<K, V> in) {
			return unmodifiableMap(new HashMap<>(in));
		}

		private static <T> List<T> immutable(List<T> in) {
			return unmodifiableList(new ArrayList<>(in));
		}

	}

	private static class Utils {
		
		public static ModuleInitializer process(IdentityAndVersion idv, ModuleConfigurer root) {

			ModuleCollector collector = new ModuleCollector();

			Set<ModuleConfigurer> all = collect(root, new HashSet<>());
			Set<ModuleConfigurer> toplevel = findTopLevel(all);
			Map<String, ModuleConfigurer> rootsMap = map(root.uses);

			resolveNamedDependencies(all, rootsMap);
			Map<ModuleConfigurer, FlowSegment> segments = new HashMap<>();

			for (ModuleConfigurer module : all) {
				if (module.isRoot()) continue;
				
				ModuleSetupContext ctx = new ModuleSetupContext(IdentityStore.createConcurrentIdentityStore());

				ModuleSetup init = new ModuleSetup(idv, module.info(), module.fullAliasOrName(), ctx, collector);
				module.setup(init);
				
				if (init.includeDefaultStatus() && module.info() != null) {
					collector.statuses.add(() -> StatusProvider.create(module.info().name().slashes(), 
							                                           module.fullAliasOrName().slashes(), 
							                                           module.info().version()));
				}

				init.buildFlowSegment().ifPresent(segment -> {
					segments.put(module, segment);
				});

			}

			Optional<FlowSegment> segment = buildSegment(toplevel, segments);

			Flow flow;
			FlowVisualizer visualizer;

			if (segment.isPresent()) {
				Entry<Flow, FlowVisualizer> entry = SingleFlow.create(Path.path("initialize"), segment.get());
				flow = entry.getKey();
				visualizer = entry.getValue();
			} else {
				flow = new NoFlow();
				visualizer = new NoFlowVisualizer();
			}

			return new ModuleInitializer(flow, visualizer, collector.immutable());
		}

		private static Optional<FlowSegment> buildSegment(Set<ModuleConfigurer> modules, Map<ModuleConfigurer, FlowSegment> built) {
			List<FlowSegment> segments = new ArrayList<>();
			for (ModuleConfigurer module : modules) {
				buildSegment(module, built).ifPresent(segment -> segments.add(segment));
			}
			return segments.isEmpty() ? Optional.empty() : Optional.of(createParallelSegment(segments));
		}

		private static Optional<FlowSegment> buildSegment(ModuleConfigurer module, Map<ModuleConfigurer, FlowSegment> built) {
			if (module.isRoot())
				return Optional.empty();

			List<FlowSegment> sequence = new ArrayList<>();

			if (built.containsKey(module)) {
				sequence.add(built.get(module));
			}

			Optional<FlowSegment> c = buildSegment(module.usedBy, built);
			if (c.isPresent()) {
				sequence.add(c.get());
			}

			return sequence.isEmpty() ? Optional.empty() : Optional.of(seq(sequence));
		}

		private static void resolveNamedDependencies(Set<ModuleConfigurer> all, Map<String, ModuleConfigurer> allMap) {
			for (ModuleConfigurer use : all) {
				for (String depname : use.modulesNames) {
					ModuleConfigurer dep = allMap.get(depname);
					checkNotNull(dep, "missing dependency: [%s] uses [%s]", use.aliasOrName(), depname);
					dep.usedBy.add(use);
					use.uses.add(dep);
				}
			}
		}

		private static Set<ModuleConfigurer> findTopLevel(Collection<ModuleConfigurer> uses) {
			Set<ModuleConfigurer> roots = new HashSet<>();
			for (ModuleConfigurer use : uses) {
				if (use.uses.isEmpty()) {
					roots.add(use);
				}
			}
			return roots;
		}

		private static Set<ModuleConfigurer> collect(ModuleConfigurer use, Set<ModuleConfigurer> collector) {
			collector.add(use);
			for (ModuleConfigurer child : use.uses) {
				collect(child, collector);
			}
			return collector;
		}

		public static Map<String, ModuleConfigurer> map(Collection<ModuleConfigurer> uses) {
			Map<String, ModuleConfigurer> map = new HashMap<>();
			for (ModuleConfigurer use : uses) {
				map.put(use.aliasOrName(), use);
			}
			return map;
		}

	}

	private List<ModuleInfo> modules = new ArrayList<>();
	private AppDirs dirs;

	private ModuleInfo info;
	private String name;
	private String alias;

	private boolean isRoot;

	private Path parentPath = Path.root();
	private Path modulePath = Path.root();

	private final List<String> modulesNames = new ArrayList<>();

	private final Set<ModuleConfigurer> usedBy = new HashSet<>();
	private final Set<ModuleConfigurer> uses = new HashSet<>();

	public ModuleConfigurer modules(List<ModuleInfo> modules) {
		this.modules = modules;

		if (isRoot()) {
			findRootConfigurers();
		}

		return this;
	}
	
	public ModuleConfigurer dirs(AppDirs dirs) {
		this.dirs = dirs;
		return this;
	}

	private void findRootConfigurers() {
		for (ModuleInfo e : modules) {
			// all the ones with a root path need to be added automatically
			// we don't need to explicitly load these...
			if (e.name().isEmpty()) {
				uses.add(e.get());
			}
		}
	}

	public abstract void setup(ModuleSetup module);

	public boolean isRoot() {
		return isRoot;
	}

	public ModuleConfigurer isRoot(boolean val) {
		isRoot = val;
		return this;
	}

	public ModuleConfigurer modulePath(Path path) {
		this.modulePath = path;
		return this;
	}
	
	@Conf.Key
	public void name(String val) {
		name = val;
	}
	
	@Conf.Val
	public void alias(String val) {
		alias = val;
	}

	protected void info(ModuleInfo val) {
		info = val;
	}

	public String aliasOrName() {
		return alias != null ? alias : name;
	}

	public ModuleInfo info() {
		return info;
	}
	
	public AppDirs dirs() {
		return dirs;
	}
	
	protected Path fullAliasOrName() {
		return parentPath.add(slashes(aliasOrName()));
	}

	public String typeAndName() {
		if (name.equals(aliasOrName())) {
			return name;
		} else {
			return format("%s/%s", name, aliasOrName());
		}
	}

	public void useThisConfig(Config config) {
		ModuleInfo info = moduleFor(slashes(config.key()));
		checkConfig(info != null, "'%s' is not a valid module (try one of %s)", config.key(), mappingNames().stream().collect(joining(", ")));
		configureModule(info, config);
	}

	protected void configureModule(ModuleInfo info, Config config) {
		ModuleConfigurer module = info.get();
		module.info(info);
		module.dirs(dirs);
		module.modules(modules);
		module.parentPath(modulePath);
		configure(module, config);
		uses.add(module);
		module.usedBy.add(this);
	}

	@Conf.Each("use")
	public void use(Config config) {
		if (config.hasBody()) {
			for (Config childConfig : config.body()) {
				useThisConfig(childConfig);
			}
		} else if (config.hasValue()) {
			modulesNames.add(config.valueAsString());
		} else {
			invalidConfig("must have body or value (referencing another dependency)");
		}
	}

	private void parentPath(Path val) {
		parentPath = val;
	}

	private ModuleInfo moduleFor(Path path) {
		for (ModuleInfo m : modules) {
			if (m.name().equals(path)) {
				return m;
			}
		}
		return null;
	}

	private Collection<String> mappingNames() {
		List<String> result = new ArrayList<>();
		for (ModuleInfo e : modules) {
			if (!e.name().isEmpty()) {
				result.add(e.name().slashes());
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return format("%s(\n    name %s\n    params %s)", name, aliasOrName(), modulesNames);
	}

}
