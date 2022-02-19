package com.awakenedredstone.autowhitelist.discord.api.util.profiler;

public interface Recorder {
   void stop();

   void startTick();

   boolean isActive();

   Profiler getProfiler();

   void endTick();
}
