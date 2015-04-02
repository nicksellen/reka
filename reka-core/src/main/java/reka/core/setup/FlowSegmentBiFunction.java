package reka.core.setup;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.core.config.ConfigurerProvider;

public interface FlowSegmentBiFunction extends BiFunction<ConfigurerProvider, Config, Supplier<FlowSegment>> {
}