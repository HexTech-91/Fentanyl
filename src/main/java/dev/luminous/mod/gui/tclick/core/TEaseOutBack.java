package dev.luminous.mod.gui.tclick.core;

import net.minecraft.util.math.MathHelper;

public class TEaseOutBack {
   private int prevTick;
   private int tick;
   private final int maxTick;

   public TEaseOutBack(int var1) {
      this.maxTick = var1;
   }

   public TEaseOutBack() {
      this(10);
   }

   public static double dropAnimation(double var0) {
      return 1.0 + 2.70158 * Math.pow(var0 - 1.0, 3.0) + 1.70158 * Math.pow(var0 - 1.0, 2.0);
   }

   public void update(boolean var1) {
      this.prevTick = this.tick;
      this.tick = MathHelper.clamp(this.tick + (var1 ? 1 : -1), 0, this.maxTick);
   }

   public double getAnimationd() {
      return dropAnimation((this.prevTick + (this.tick - this.prevTick) * TAnim.tickDelta()) / this.maxTick);
   }

   public void reset() {
      this.prevTick = 0;
      this.tick = 0;
   }
}
