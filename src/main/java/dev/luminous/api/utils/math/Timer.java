package dev.luminous.api.utils.math;

public class Timer {
   private long startTime = -1L;

   public Timer() {
      this.reset();
   }

   public void reset() {
      this.startTime = System.currentTimeMillis();
   }

   public long getMs() {
      return System.currentTimeMillis() - this.startTime;
   }

   public boolean passedS(double s) {
      return this.passed((long)(s * 1000.0));
   }

   public boolean passedMs(double ms) {
      return this.passed((long)ms);
   }

   public void setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
   }

   public boolean passed(long ms) {
      return System.currentTimeMillis() - this.startTime >= ms;
   }

   public Timer resetAndGet() {
      this.reset();
      return this;
   }

   public boolean passedTicks(int ticks) {
      return this.passed(ticks * 50L);
   }

   public boolean tick(int tick) {
      return this.passedTicks(tick);
   }

   public boolean passedNS(long ns) {
      return System.nanoTime() - this.convertMsToNano(this.startTime) >= ns;
   }

   public long getPassedTimeMs() {
      return System.currentTimeMillis() - this.startTime;
   }

   public long getMs(long nanoTime) {
      return nanoTime / 1000000L;
   }

   public long convertToNS(long timeMs) {
      return timeMs * 1000000L;
   }

   private long convertMsToNano(long ms) {
      return ms * 1000000L;
   }
}
