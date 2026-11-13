package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Formatting;

public class PotionHud extends Module {
   public static PotionHud INSTANCE;
   private final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255)));
   public float vAnimation;
   public float hAnimation;

   public PotionHud() {
      super("Potions", Module.Category.HUD);
      this.setChinese("药水");
      INSTANCE = this;
   }

   public static String getDuration(StatusEffectInstance pe) {
      if (pe.isInfinite()) {
         return "*:*";
      } else {
         int var1 = pe.getDuration();
         int mins = var1 / 1200;
         String sec = String.format("%02d", var1 % 1200 / 20);
         return mins + ":" + sec;
      }
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      if (mc.player == null) {
         return;
      }

      int y_offset1 = 0;
      float max_width = 50.0F;
      float pointerX = 0.0F;

      for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
         StatusEffect potion = potionEffect.getEffectType().value();
         if (y_offset1 == 0) {
            y_offset1 += 4;
         }

         y_offset1 += 9;
         float nameWidth = FontManager.ui.getWidth(potion.getName().getString() + " " + (potionEffect.getAmplifier() + 1));
         float timeWidth = FontManager.ui.getWidth(getDuration(potionEffect));
         float width = (nameWidth + 5.0F + timeWidth) * 1.4F;
         if (width > max_width) {
            max_width = width;
         }

         if (timeWidth > pointerX) {
            pointerX = timeWidth;
         }
      }

      this.vAnimation = AnimateUtil.fast(this.vAnimation, 14 + y_offset1, 15.0F);
      this.hAnimation = AnimateUtil.fast(this.hAnimation, max_width, 15.0F);
      Render2DUtil.drawHudBase(
         context.getMatrices(),
         (float)this.getPosX(),
         (float)this.getPosY(),
         this.hAnimation,
         this.vAnimation,
         (float)HudEditor.INSTANCE.hudRound.getValue()
      );
      if (HudEditor.INSTANCE.hudStyle.is(HudEditor.HudStyle.Glowing)) {
         FontManager.ui.drawCenteredString(context.getMatrices(), "Potions", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4, this.textColor.getValue().getRGB());
      } else {
         FontManager.ui.drawGradientCenteredString(context.getMatrices(), "Potions", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4);
      }

      if (y_offset1 > 0) {
         if (HudEditor.INSTANCE.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DUtil.drawRectDumbWay(
               context.getMatrices(),
               this.getPosX() + 4,
               this.getPosY() + 13,
               this.getPosX() + this.hAnimation - 4.0F,
               this.getPosY() + 13.5F,
               new Color(0x54FFFFFF, true)
            );
         } else {
            Render2DUtil.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2,
               this.getPosY() + 13.7F,
               this.getPosX() + 2 + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 14,
               Render2DUtil.injectAlpha(this.textColor.getValue(), 0),
               this.textColor.getValue()
            );
            Render2DUtil.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2 + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2 + this.hAnimation - 4.0F,
               this.getPosY() + 14,
               this.textColor.getValue(),
               Render2DUtil.injectAlpha(this.textColor.getValue(), 0)
            );
         }
      }

      Render2DUtil.addWindow(
         context.getMatrices(), (float)this.getPosX(), (float)this.getPosY(), this.getPosX() + this.hAnimation, this.getPosY() + this.vAnimation, 1.0
      );
      int y_offset = 0;

      for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
         StatusEffect potion = potionEffect.getEffectType().value();
         float px = this.getPosX() + (max_width - pointerX - 10.0F);
         context.getMatrices().push();
         context.getMatrices().translate(this.getPosX() + 2.0F, this.getPosY() + 16 + y_offset, 0.0F);
         context.drawSprite(0, 0, 0, 8, 8, mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType()));
         context.getMatrices().pop();
         FontManager.ui
            .drawString(
               context.getMatrices(),
               potion.getName().getString() + " " + Formatting.RED + (potionEffect.getAmplifier() + 1),
               this.getPosX() + 12,
               this.getPosY() + 19 + y_offset,
               this.textColor.getValue().getRGB()
            );
         FontManager.ui
            .drawCenteredString(
               context.getMatrices(),
               getDuration(potionEffect),
               px + (this.getPosX() + max_width - px) / 2.0F,
               this.getPosY() + 15 + y_offset,
               this.textColor.getValue().getRGB()
            );
         Render2DUtil.drawRect(context.getMatrices(), px, this.getPosY() + 17 + y_offset, 0.5F, 8.0F, new Color(0x44FFFFFF, true));
         y_offset += 9;
      }

      Render2DUtil.popWindow();
   }

   private int getPosX() {
      return Alien.GUI.potionHud.getX();
   }

   private int getPosY() {
      return Alien.GUI.potionHud.getY();
   }
}
