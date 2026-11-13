package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import java.util.concurrent.ThreadLocalRandom;

public final class FakeCoords extends Module {
   public static FakeCoords INSTANCE;
   private int fakeX;
   private int fakeY;
   private int fakeZ;
   private int fakeOtherX;
   private int fakeOtherZ;

   public FakeCoords() {
      super("FakeCoords", Module.Category.Client);
      this.setChinese("\u5047\u5750\u6807");
      INSTANCE = this;
      this.randomize();
   }

   @Override
   public void onEnable() {
      this.randomize();
   }

   public String getCoords() {
      return "XYZ \u00a7f" + this.fakeX + ", " + this.fakeY + ", " + this.fakeZ + "\u00a77 [\u00a7f" + this.fakeOtherX + ", " + this.fakeOtherZ + "\u00a77]";
   }

   private void randomize() {
      ThreadLocalRandom var1 = ThreadLocalRandom.current();
      this.fakeX = var1.nextInt(-30000000, 30000001);
      this.fakeY = var1.nextInt(-64, 321);
      this.fakeZ = var1.nextInt(-30000000, 30000001);
      this.fakeOtherX = var1.nextInt(-30000000, 30000001);
      this.fakeOtherZ = var1.nextInt(-30000000, 30000001);
   }
}
