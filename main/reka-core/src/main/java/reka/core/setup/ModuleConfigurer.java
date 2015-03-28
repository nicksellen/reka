package reka.core.setup;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
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
import reka.dirs.AppDirs;

public abstract class ModuleConfigurer {

	public static ApplicationSetup setup(IdentityAndVersion idv, ModuleConfigurer root, IdentityStore store) {
		
		ModuleCollector collector = new ModuleCollector();

		Set<ModuleConfigurer> all = collect(root, new HashSet<>());
		Set<ModuleConfigurer> toplevel = findTopLevel(all);
		Map<String, ModuleConfigurer> rootsMap = map(root.uses);

		resolveNamedDependencies(all, rootsMap);
		Map<ModuleConfigurer, FlowSegment> initializeSegments = new HashMap<>();

		for (ModuleConfigurer module : all) {
			if (module.isRoot()) continue;
			
			ModuleSetupContext ctx = new ModuleSetupContext(store);

			AppSetup setup = new AppSetup(idv, module.info(), module.fullAliasOrName(), ctx, collector);
			
			module.setup(setup);
			
			if (setup.includeDefaultStatus() && module.info() != null) {
				collector.statuses.add(() -> StatusProvider.create(module.info().name().slashes(), 
						                                           module.fullAliasOrName().slashes(), 
						                                           module.info().version()));
			}

			setup.buildFlowSegment().ifPresent(segment -> {
				initializeSegments.put(module, segment);
			});

		}

		Optional<FlowSegment> initializeSegment = buildSegment(toplevel, initializeSegments);

		Flow flow;
		FlowVisualizer visualizer;

		if (initializeSegment.isPresent()) {
			Entry<Flow, FlowVisualizer> entry = SingleFlow.create(Path.path("initialize"), initializeSegment.get());
			flow = entry.getKey();
			visualizer = entry.getValue();
		} else {
			flow = NoFlow.INSTANCE;
			visualizer = NoFlowVisualizer.INSTANCE;
		}

		return new ApplicationSetup(flow, visualizer, collector);
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

	public abstract void setup(AppSetup module);

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

	private static Map<String, ModuleConfigurer> map(Collection<ModuleConfigurer> uses) {
		Map<String, ModuleConfigurer> map = new HashMap<>();
		for (ModuleConfigurer use : uses) {
			map.put(use.aliasOrName(), use);
		}
		return map;
	}

}
