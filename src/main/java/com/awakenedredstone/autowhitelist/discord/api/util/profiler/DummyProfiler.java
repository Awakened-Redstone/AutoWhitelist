package com.awakenedredstone.autowhitelist.discord.api.util.profiler;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

public class DummyProfiler implements ReadableProfiler {
   public static final DummyProfiler INSTANCE = new DummyProfiler();

   private DummyProfiler() {
   }

   public void startTick() {
   }

   public void endTick() {
   }

   public void push(String location) {
   }

   public void push(Supplier<String> locationGetter) {
   }

   public void method_37167(SamplingChannel samplingChannel) {
   }

   public void pop() {
   }

   public void swap(String location) {
   }

   public void swap(Supplier<String> locationGetter) {
   }

   public void visit(String marker) {
   }

   public void visit(Supplier<String> markerGetter) {
   }

   public ProfileResult getResult() {
      return EmptyProfileResult.INSTANCE;
   }

   @Nullable
   public ProfilerSystem.LocatedInfo getInfo(String name) {
      return null;
   }

   public Set<Pair<String, SamplingChannel>> method_37168() {
      return ImmutableSet.of();
   }
}
