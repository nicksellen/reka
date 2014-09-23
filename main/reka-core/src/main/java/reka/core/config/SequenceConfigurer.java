package reka.core.config;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class SequenceConfigurer implements OperationConfigurer {
	
    private final ConfigurerProvider provider;

    public SequenceConfigurer(ConfigurerProvider provider) {
        this.provider = provider;
    }

    private final List<Entry<Config,Supplier<FlowSegment>>> configurers = new ArrayList<>();

    @Conf.Each
    public void each(Config config) {
        configurers.add(createEntry(config, provider.provide(config.key(), provider, config)));
    }
    
    public Supplier<FlowSegment> bind() {
    	return bind(root(), null);
    }
    
	@Override
	public void setup(OperationSetup ops) {
		configurers.forEach(e -> {
			ops.sequential(op -> {

				Config config = e.getKey();
				Supplier<FlowSegment> configurer = e.getValue();

				op.meta().putString("key", config.key());

				if (config.hasValue()) {
					op.meta().putString("value", config.valueAsString());
				}

				op.meta().putInt(dots("location.start.line"), config.source().linenumbers().startLine());
				op.meta().putInt(dots("location.start.pos"), config.source().linenumbers().startPos());
				op.meta().putInt(dots("location.end.line"), config.source().linenumbers().endLine());
				op.meta().putInt(dots("location.end.pos"), config.source().linenumbers().endPos());

				if (config.source().origin().isFile()) {
					op.meta().putString("filename", config.source().origin().file().getAbsolutePath());
				}

				op.add(configurer);
			});
		});
	}

}
