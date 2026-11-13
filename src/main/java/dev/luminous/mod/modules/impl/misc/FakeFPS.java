package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.core.impl.FPSManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Random;

public class FakeFPS extends Module {
   public static FakeFPS INSTANCE;
   private FPSManager originalFPS;
   private FPSManager fakeFPS;
   private final Random random = new Random();
   private long lastUpdate;
   private int currentFakeFPS;
   private final SliderSetting minFPS = this.add(new SliderSetting("MinFPS", 60.0, 0.0, 10000.0, 1.0));
   private final SliderSetting maxFPS = this.add(new SliderSetting("MaxFPS", 500.0, 0.0, 10000.0, 1.0));

   public FakeFPS() {
      super("FakeFPS", Module.Category.Misc);
      this.minFPS.setChinese("\u6700\u5c0fFPS");
      this.maxFPS.setChinese("\u6700\u5927FPS");
      this.setChinese("\u5047FPS");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.originalFPS = Alien.FPS;
         this.lastUpdate = System.currentTimeMillis();
         this.currentFakeFPS = this.getRandomFPS();
         Alien.FPS = this.fakeFPS = new FPSManager() {
            @Override
            public int getFps() {
               long var1 = System.currentTimeMillis();
               if (var1 - FakeFPS.this.lastUpdate >= 100L) {
                  FakeFPS.this.lastUpdate = var1;
                  FakeFPS.this.currentFakeFPS = FakeFPS.this.getRandomFPS();
               }

               return FakeFPS.this.currentFakeFPS;
            }
         };
      }
   }

   @Override
   public void onDisable() {
      if (this.originalFPS != null) {
         Alien.FPS = this.originalFPS;
         this.originalFPS = null;
      }
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.currentFakeFPS);
   }

   private int getRandomFPS() {
      int var2 = this.minFPS.getValueInt();
      int var1;
      return var2 >= (var1 = this.maxFPS.getValueInt()) ? Math.max(var2, 1) : Math.max(this.random.nextInt(var1 - var2 + 1) + var2, 1);
   }
}
