package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class LowHealthWarning extends Module {
   public static LowHealthWarning INSTANCE;
   private final SliderSetting healthThreshold = this.add(new SliderSetting("HealthThreshold", 6.0, 0.0, 36.0, 0.5));
   private final SliderSetting textScale = this.add(new SliderSetting("TextScale", 1.5, 0.5, 3.0, 0.1));
   private final SliderSetting posX = this.add(new SliderSetting("PosX", 0.0, -1000.0, 1000.0, 1.0));
   private final SliderSetting posY = this.add(new SliderSetting("PosY", 0.0, -1000.0, 1000.0, 1.0));
   private static final int WARNING_COLOR = Color.RED.getRGB();

   public LowHealthWarning() {
      super("LowHealthWarning", Module.Category.Render);
      this.setChinese("\u4f4e\u8840\u91cf\u9884\u8b66");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (mc.player != null) {
         float var3 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         if (!(var3 > (float)this.healthThreshold.getValue())) {
            float var4 = mc.getWindow().getScaledWidth();
            float var5 = mc.getWindow().getScaledHeight();
            float var6 = this.textScale.getValueFloat();
            float var7 = TextUtil.getWidth("My health is too low!");
            float var8 = var4 / 2.0F / var6 - var7 / 2.0F + (float)this.posX.getValue() / var6;
            float var9 = var5 / 2.0F / var6 + (float)this.posY.getValue() / var6;
            MatrixStack var10 = var1.getMatrices();
            var10.push();
            var10.scale(var6, var6, 1.0F);
            TextUtil.drawString(var1, "My health is too low!", var8, var9, WARNING_COLOR, false, true);
            var10.pop();
         }
      }
   }
}
