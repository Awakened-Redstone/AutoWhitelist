package com.awakenedredstone.autowhitelist.discord.api.util.profiler;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class EmptyProfileResult implements ProfileResult {
   public static final EmptyProfileResult INSTANCE = new EmptyProfileResult();

   private EmptyProfileResult() {
   }

   public List<ProfilerTiming> getTimings(String parentPath) {
      return Collections.emptyList();
   }

   public boolean save(Path path) {
      return false;
   }

   public long getStartTime() {
      return 0L;
   }

   public int getStartTick() {
      return 0;
   }

   public long getEndTime() {
      return 0L;
   }

   public int getEndTick() {
      return 0;
   }

   public String getRootTimings() {
      return "";
   }
}
