package com.awakenedredstone.autowhitelist.discord.api.util.profiler;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ReadableProfiler extends Profiler {
   ProfileResult getResult();

   @Nullable
   ProfilerSystem.LocatedInfo getInfo(String name);

   Set<Pair<String, SamplingChannel>> method_37168();
}
