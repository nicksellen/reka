package reka.builtins;

import static reka.api.Path.path;
import static reka.configurer.Configurer.Preconditions.checkConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.EverythingSubscriber;
import reka.configurer.Configurer.ErrorCollector;
import reka.configurer.ErrorReporter;
import reka.configurer.annotations.Conf;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.SetupTrigger;

public class TimerExport implements TriggerConfigurer, ErrorReporter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final static ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
	
	private long ms = -1;
	private Path run;

	@Conf.Val
	@Conf.At("every")
	public void val(String val) {
		ms = Long.valueOf(val);
	}
	
	@Conf.At("run")
	public void run(String val) {
		run = path(val);
	}

	@Override
	public void errors(ErrorCollector errors) {
		if (run == null) errors.add("must provide [run] option");
		if (ms == -1) errors.add("must provide [every] option");
	}
	
	@Override
	public void setupTriggers(SetupTrigger trigger) {
		
		trigger.onStart(app -> {
			
			Flow flow = app.flows().flow(run);
			checkConfig(flow != null, "no run named %s", run);
			e.scheduleAtFixedRate(() -> {
				flow.run(new EverythingSubscriber(){

					@Override
					public void ok(MutableData data) {
					}

					@Override
					public void halted() {
						log.debug("halted :(");
					}

					@Override
					public void error(Data data, Throwable t) {
						log.debug("error :(");
						t.printStackTrace();
					}
				
				});
			}, 0, ms, TimeUnit.MILLISECONDS);	
		});
		
		
	}
	
}