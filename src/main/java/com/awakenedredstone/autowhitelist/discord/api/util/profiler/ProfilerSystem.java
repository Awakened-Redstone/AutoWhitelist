package com.awakenedredstone.autowhitelist.discord.api.util.profiler;

import com.awakenedredstone.autowhitelist.discord.api.util.Util;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ProfilerSystem implements ReadableProfiler {
   private static final long TIMEOUT_NANOSECONDS = Duration.ofMillis(100L).toNanos();
   private static final Logger LOGGER = LogManager.getLogger();
   private final List<String> path = Lists.newArrayList();
   private final LongList timeList = new LongArrayList();
   private final Map<String, LocatedInfo> locationInfos = Maps.newHashMap();
   private final IntSupplier endTickGetter;
   private final LongSupplier timeGetter;
   private final long startTime;
   private final int startTick;
   private String location = "";
   private boolean tickStarted;
   @Nullable
   private ProfilerSystem.LocatedInfo currentInfo;
   private final boolean checkTimeout;
   private final Set<Pair<String, SamplingChannel>> field_33873 = new ObjectArraySet();

   public ProfilerSystem(LongSupplier timeGetter, IntSupplier tickGetter, boolean checkTimeout) {
      this.startTime = timeGetter.getAsLong();
      this.timeGetter = timeGetter;
      this.startTick = tickGetter.getAsInt();
      this.endTickGetter = tickGetter;
      this.checkTimeout = checkTimeout;
   }

   public void startTick() {
      if (this.tickStarted) {
         LOGGER.error("Profiler tick already started - missing endTick()?");
      } else {
         this.tickStarted = true;
         this.location = "";
         this.path.clear();
         this.push("root");
      }
   }

   public void endTick() {
      if (!this.tickStarted) {
         LOGGER.error("Profiler tick already ended - missing startTick()?");
      } else {
         this.pop();
         this.tickStarted = false;
         if (!this.location.isEmpty()) {
            LOGGER.error("Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?", () -> {
               return ProfileResult.getHumanReadableName(this.location);
            });
         }

      }
   }

   public void push(String location) {
      if (!this.tickStarted) {
         LOGGER.error((String)"Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?", (Object)location);
      } else {
         if (!this.location.isEmpty()) {
            this.location = this.location + "\u001e";
         }

         this.location = this.location + location;
         this.path.add(this.location);
         this.timeList.add(Util.getMeasuringTimeNano());
         this.currentInfo = null;
      }
   }

   public void push(Supplier<String> locationGetter) {
      this.push((String)locationGetter.get());
   }

   public void method_37167(SamplingChannel samplingChannel) {
      this.field_33873.add(Pair.of(this.location, samplingChannel));
   }

   public void pop() {
      if (!this.tickStarted) {
         LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
      } else if (this.timeList.isEmpty()) {
         LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
      } else {
         long l = Util.getMeasuringTimeNano();
         long m = this.timeList.removeLong(this.timeList.size() - 1);
         this.path.remove(this.path.size() - 1);
         long n = l - m;
         LocatedInfo locatedInfo = this.getCurrentInfo();
         locatedInfo.time += n;
         ++locatedInfo.visits;
         locatedInfo.field_33874 = Math.max(locatedInfo.field_33874, n);
         locatedInfo.field_33875 = Math.min(locatedInfo.field_33875, n);
         if (this.checkTimeout && n > TIMEOUT_NANOSECONDS) {
            LOGGER.warn("Something's taking too long! '{}' took aprox {} ms", () -> {
               return ProfileResult.getHumanReadableName(this.location);
            }, () -> {
               return (double)n / 1000000.0D;
            });
         }

         this.location = this.path.isEmpty() ? "" : (String)this.path.get(this.path.size() - 1);
         this.currentInfo = null;
      }
   }

   public void swap(String location) {
      this.pop();
      this.push(location);
   }

   public void swap(Supplier<String> locationGetter) {
      this.pop();
      this.push(locationGetter);
   }

   private LocatedInfo getCurrentInfo() {
      if (this.currentInfo == null) {
         this.currentInfo = (LocatedInfo)this.locationInfos.computeIfAbsent(this.location, (string) -> {
            return new LocatedInfo();
         });
      }

      return this.currentInfo;
   }

   public void visit(String marker) {
      this.getCurrentInfo().counts.addTo(marker, 1L);
   }

   public void visit(Supplier<String> markerGetter) {
      this.getCurrentInfo().counts.addTo((String)markerGetter.get(), 1L);
   }

   public ProfileResult getResult() {
      return new ProfileResultImpl(this.locationInfos, this.startTime, this.startTick, this.timeGetter.getAsLong(), this.endTickGetter.getAsInt());
   }

   @Nullable
   public ProfilerSystem.LocatedInfo getInfo(String name) {
      return this.locationInfos.get(name);
   }

   public Set<Pair<String, SamplingChannel>> method_37168() {
      return this.field_33873;
   }

   public static class LocatedInfo implements ProfileLocationInfo {
      long field_33874 = Long.MIN_VALUE;
      long field_33875 = Long.MAX_VALUE;
      long time;
      long visits;
      final Object2LongOpenHashMap<String> counts = new Object2LongOpenHashMap();

      public long getTotalTime() {
         return this.time;
      }

      public long method_37169() {
         return this.field_33874;
      }

      public long getVisitCount() {
         return this.visits;
      }

      public Object2LongMap<String> getCounts() {
         return Object2LongMaps.unmodifiable(this.counts);
      }
   }
}
