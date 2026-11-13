package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;

public class HealthWarning extends Module {
   public static HealthWarning INSTANCE;
   public final SliderSetting healthThreshold = this.add(new SliderSetting("HealthThreshold", 10.0, 1.0, 20.0, 0.5));
   public final SliderSetting blurStrength = this.add(new SliderSetting("BlurStrength", 6.0, 0.0, 20.0, 0.5));

   public HealthWarning() {
      super("HealthWarning", Module.Category.Render);
      this.setChinese("\u8840\u91cf\u63d0\u9192");
      this.healthThreshold.setChinese("\u81ea\u5b9a\u4e49\u8840\u91cf");
      this.blurStrength.setChinese("\u6a21\u7cca\u5f3a\u5ea6");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (mc.player != null && !(mc.player.getHealth() <= 0.0F) && !(mc.player.getHealth() > this.healthThreshold.getValueFloat())) {
         int var3 = var1.getScaledWindowWidth();
         int var4 = var1.getScaledWindowHeight();
         float var5 = 0.35F + 0.65F * (0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 140.0));
         this.drawRedBlur(var1, var3, var4, var5);
         this.drawWarningTriangle(var1, var3 / 2, var4 / 2, var5);
      }
   }

   private void drawRedBlur(DrawContext var1, int var2, int var3, float var4) {
      float var5 = this.blurStrength.getValueFloat();
      if (!(var5 <= 0.0F)) {
         int var6 = Math.max(10, Math.min(Math.min(var2, var3) / 3, Math.round(10.0F + var5 * 3.0F)));
         float var7 = Math.max(0.5F, var5 * (0.4F + var4 * 0.6F));
         Alien.BLUR.applyBlur(var7, 0.0F, 0.0F, var2, var3);
         int var8 = Math.max(4, Math.min(20, Math.round(4.0F + var5)));
         int var9 = Math.max(6, Math.min(90, Math.round((18.0F + var5 * 3.0F) * var4)));

         for (int var10 = 0; var10 < var8; var10++) {
            float var11 = (float)var10 / var8;
            int var12 = Math.max(1, Math.round(var9 * (1.0F - var11) * (1.0F - var11)));
            int var13 = var12 << 24 | 16715792;
            int var14 = var10 * var6 / var8;
            int var15 = (var10 + 1) * var6 / var8 + 1;
            var1.fill(0, var14, var2, var15, var13);
            var1.fill(0, var3 - var15, var2, var3 - var14, var13);
            var1.fill(var14, var6, var15, var3 - var6, var13);
            var1.fill(var2 - var15, var6, var2 - var14, var3 - var6, var13);
         }
      }
   }

   private void drawWarningTriangle(DrawContext var1, int var2, int var3, float var4) {
      byte var5 = 13;
      int var6 = var3 - 13;
      int var7 = var3 + 11;
      int var8 = var7 - var6;
      int var9 = Math.round(230.0F * var4) << 24 | 14883619;
      int var10 = Math.round(240.0F * var4) << 24 | 16737894;
      int var11 = Math.round(255.0F * var4) << 24 | 16777215;

      for (int var12 = 0; var12 < var8; var12++) {
         float var13 = (float)var12 / (var8 - 1);
         int var14 = Math.max(1, Math.round(var5 * var13));
         int var15 = var2 - var14;
         int var16 = var2 + var14 + 1;
         var1.fill(var15, var6 + var12, var16, var6 + var12 + 1, var9);
         var1.fill(var15, var6 + var12, Math.min(var15 + 2, var16), var6 + var12 + 1, var10);
         var1.fill(Math.max(var15, var16 - 2), var6 + var12, var16, var6 + var12 + 1, var10);
      }

      var1.fill(var2 - var5, var7 - 2, var2 + var5 + 1, var7 + 1, var10);
      var1.fill(var2 - 1, var3 - 6, var2 + 2, var3 + 2, var11);
      var1.fill(var2 - 1, var3 + 5, var2 + 2, var3 + 8, var11);
   }
}
