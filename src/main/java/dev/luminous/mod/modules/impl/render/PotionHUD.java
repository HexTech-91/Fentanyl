package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

public class PotionHUD extends Module {
   public static PotionHUD INSTANCE;
   private final SliderSetting x = this.add(new SliderSetting("X", 2, 0, 1500));
   private final SliderSetting y;
   private final ColorSetting color;
   private final SliderSetting iconSize;
   private final BooleanSetting fontSetting;
   private final BooleanSetting shadow;
   private final SliderSetting spacing;
   private final SliderSetting fadeTime;

   public PotionHUD() {
      super(
         "PotionHUD",
         "\u5728\u5c4f\u5e55\u4e0a\u663e\u793a\u5f53\u524d\u836f\u6c34\u6548\u679c\u3001\u56fe\u6807\u53ca\u5269\u4f59\u65f6\u95f4",
         Module.Category.Render
      );
      this.x.setChinese("X\u5750\u6807");
      this.y = this.add(new SliderSetting("Y", 2, 0, 1000));
      this.y.setChinese("Y\u5750\u6807");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
      this.color.setChinese("\u989c\u8272");
      this.iconSize = this.add(new SliderSetting("IconSize", 18.0, 8.0, 32.0, 1.0));
      this.iconSize.setChinese("\u56fe\u6807\u5927\u5c0f");
      this.fontSetting = this.add(new BooleanSetting("Font", true));
      this.fontSetting.setChinese("\u5b57\u4f53");
      this.shadow = this.add(new BooleanSetting("Shadow", true));
      this.shadow.setChinese("\u9634\u5f71");
      this.spacing = this.add(new SliderSetting("Spacing", 2.0, 0.0, 10.0, 1.0));
      this.spacing.setChinese("\u95f4\u8ddd");
      this.fadeTime = this.add(new SliderSetting("FadeTime", 3.0, 0.0, 10.0, 0.1));
      this.fadeTime.setChinese("\u6e10\u53d8\u65f6\u95f4");
      this.setChinese("\u836f\u6c34\u6548\u679c");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (mc.player != null) {
         int posX = this.x.getValueInt();
         int posY = this.y.getValueInt();
         int size = this.iconSize.getValueInt();
         int textColor = this.color.getValue().getRGB();
         boolean useFont = this.fontSetting.getValue();
         boolean useShadow = this.shadow.getValue();
         int gap = this.spacing.getValueInt();
         float fadeSeconds = this.fadeTime.getValueFloat();
         int fadeTicks = (int)(fadeSeconds * 20.0F);
         StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();
         if (spriteManager != null) {
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
               StatusEffect statusEffect = (StatusEffect)effect.getEffectType().value();
               float alpha = 1.0F;
               if (!effect.isInfinite() && fadeTicks > 0) {
                  int remaining = effect.getDuration();
                  alpha = Math.min(1.0F, (float)remaining / fadeTicks);
                  alpha = Math.max(0.0F, alpha);
               }

               Sprite sprite = spriteManager.getSprite(effect.getEffectType());
               if (sprite != null) {
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                  drawContext.drawSprite(posX, posY, 0, size, size, sprite);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               }

               String name = statusEffect.getName().getString();
               int amplifier = effect.getAmplifier();
               String duration = this.getDuration(effect);
               String text = name + " " + this.toRoman(amplifier + 1) + " \u00a7f" + duration;
               float textX = posX + size + 2;
               float fontHeight = useFont ? FontManager.ui.getFontHeight() : 9.0F;
               float textY = posY + (size - fontHeight) / 2.0F;
               int fadedColor = this.applyAlpha(textColor, alpha);
               TextUtil.drawString(drawContext, text, textX, textY, fadedColor, useFont, useShadow);
               posY += size + gap;
            }
         }
      }
   }

   private String getDuration(StatusEffectInstance effect) {
      if (effect.isInfinite()) {
         return "\u221e";
      } else {
         int ticks = effect.getDuration();
         int mins = ticks / 1200;
         int secs = ticks % 1200 / 20;
         return String.format("%d:%02d", mins, secs);
      }
   }

   private String toRoman(int n) {
      switch (n) {
         case 1:
            return "I";
         case 2:
            return "II";
         case 3:
            return "III";
         case 4:
            return "IV";
         case 5:
            return "V";
         case 6:
            return "VI";
         case 7:
            return "VII";
         case 8:
            return "VIII";
         case 9:
            return "IX";
         case 10:
            return "X";
         default:
            return String.valueOf(n);
      }
   }

   private int applyAlpha(int color, float alpha) {
      int a = Math.round((color >> 24 & 0xFF) * alpha);
      return a << 24 | color & 16777215;
   }
}
