package reka.module.setup;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import reka.config.Config;
import reka.core.config.ConfigurerProvider;
import reka.flow.FlowSegment;

public interface FlowSegmentBiFunction extends BiFunction<ConfigurerProvider, Config, Supplier<FlowSegment>> {
}