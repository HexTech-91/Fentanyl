package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class LowArmorWarning extends Module {
   public static LowArmorWarning INSTANCE;
   private final SliderSetting durabilityThreshold = this.add(new SliderSetting("DurabilityThreshold", 20.0, 0.0, 500.0, 1.0));
   private final SliderSetting textScale = this.add(new SliderSetting("TextScale", 1.5, 0.5, 3.0, 0.1));
   private final SliderSetting posX = this.add(new SliderSetting("PosX", 0.0, -1000.0, 1000.0, 1.0));
   private final SliderSetting posY = this.add(new SliderSetting("PosY", 0.0, -1000.0, 1000.0, 1.0));
   private static final int WARNING_COLOR = Color.RED.getRGB();

   public LowArmorWarning() {
      super("LowArmorWarning", Module.Category.Render);
      this.setChinese("\u4f4e\u62a4\u7532\u9884\u8b66");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (mc.player != null) {
         int var3 = Integer.MAX_VALUE;

         for (int var4 = 0; var4 < 4; var4++) {
            ItemStack var5 = mc.player.getInventory().getArmorStack(var4);
            if (var5 != null && !var5.isEmpty()) {
               int var6 = var5.getMaxDamage() - var5.getDamage();
               if (var6 < var3) {
                  var3 = var6;
               }
            }
         }

         if (var3 != Integer.MAX_VALUE) {
            if (var3 <= (int)this.durabilityThreshold.getValue()) {
               float var11 = mc.getWindow().getScaledWidth();
               float var12 = mc.getWindow().getScaledHeight();
               float var13 = this.textScale.getValueFloat();
               float var7 = TextUtil.getWidth("My armor durability is too low!");
               float var8 = var11 / 2.0F / var13 - var7 / 2.0F + (float)this.posX.getValue() / var13;
               float var9 = var12 / 2.0F / var13 + (float)this.posY.getValue() / var13;
               MatrixStack var10 = var1.getMatrices();
               var10.push();
               var10.scale(var13, var13, 1.0F);
               TextUtil.drawString(var1, "My armor durability is too low!", var8, var9, WARNING_COLOR, false, true);
               var10.pop();
            }
         }
      }
   }
}
