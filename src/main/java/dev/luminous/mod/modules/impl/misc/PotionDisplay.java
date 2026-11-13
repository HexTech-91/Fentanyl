package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 神龟药水显示：准星下方显示背包内的神龟药水数量（黄色）以及自身神龟效果的剩余时间（蓝色）。
 */
public class PotionDisplay extends Module {
   public static PotionDisplay INSTANCE;

   public final SliderSetting xOffset = this.add(new SliderSetting("X", 0.0, -200.0, 200.0, 1.0));
   public final SliderSetting yOffset = this.add(new SliderSetting("Y", 12.0, -100.0, 200.0, 1.0));
   public final ColorSetting countColor = this.add(new ColorSetting("CountColor", new Color(255, 255, 85)));
   public final ColorSetting timeColor = this.add(new ColorSetting("TimeColor", new Color(85, 170, 255)));

   public PotionDisplay() {
      super("PotionDisplay", Module.Category.Misc);
      this.setChinese("\u836f\u6c34\u663e\u793a");
      this.xOffset.setChinese("X\u504f\u79fb");
      this.yOffset.setChinese("Y\u504f\u79fb");
      this.countColor.setChinese("\u6570\u91cf\u989c\u8272");
      this.timeColor.setChinese("\u65f6\u95f4\u989c\u8272");
      INSTANCE = this;
   }

   /** 神龟药水 = 抗性提升 III 及以上（普通抗性药水是 I / II）。 */
   private static boolean isTurtleMaster(StatusEffectInstance effect) {
      return effect.getEffectType().value() == StatusEffects.RESISTANCE.value() && effect.getAmplifier() >= 2;
   }

   public int getPotionCount() {
      if (mc.player == null) {
         return 0;
      }

      int count = 0;
      for (int i = 0; i < 45; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) {
            continue;
         }

         PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
         for (StatusEffectInstance effect : contents.getEffects()) {
            if (isTurtleMaster(effect)) {
               count += stack.getCount();
               break;
            }
         }
      }

      return count;
   }

   public StatusEffectInstance getPlayerEffect() {
      if (mc.player == null) {
         return null;
      }

      for (StatusEffectInstance instance : mc.player.getStatusEffects()) {
         if (isTurtleMaster(instance)) {
            return instance;
         }
      }

      return null;
   }

   private static String getDuration(StatusEffectInstance effect) {
      if (effect.isInfinite()) {
         return "\u221e";
      }

      int duration = effect.getDuration();
      return String.format("%d:%02d", duration / 1200, duration % 1200 / 20);
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (nullCheck()) {
         return;
      }

      String countText = String.valueOf(this.getPotionCount());
      StatusEffectInstance instance = this.getPlayerEffect();
      String timeText = instance == null ? "0:00" : getDuration(instance);
      float countWidth = mc.textRenderer.getWidth(countText);
      float timeWidth = mc.textRenderer.getWidth(timeText);
      int gap = 4;
      int totalWidth = (int)Math.ceil(countWidth + gap + timeWidth);
      int x = mc.getWindow().getScaledWidth() / 2 - totalWidth / 2 + this.xOffset.getValueInt();
      int y = mc.getWindow().getScaledHeight() / 2 + this.yOffset.getValueInt();

      drawContext.drawText(mc.textRenderer, countText, x, y, this.countColor.getValue().getRGB(), true);
      drawContext.drawText(
         mc.textRenderer, timeText, (int)(x + countWidth + gap), y, this.timeColor.getValue().getRGB(), true
      );
   }
}
