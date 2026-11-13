package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class Keystrokes extends Module {
   private final SliderSetting posX = this.add(new SliderSetting("PosX", 26.0, 0.0, 1920.0, 1.0));
   private final SliderSetting posY = this.add(new SliderSetting("PosY", 96.0, 0.0, 1080.0, 1.0));
   private final SliderSetting keyWidth = this.add(new SliderSetting("KeyWidth", 52.0, 20.0, 90.0, 1.0));
   private final SliderSetting keyHeight = this.add(new SliderSetting("KeyHeight", 34.0, 16.0, 60.0, 1.0));
   private final SliderSetting gap = this.add(new SliderSetting("Gap", 2.0, 0.0, 12.0, 1.0));
   private final SliderSetting radius = this.add(new SliderSetting("Radius", 2.0, 0.0, 12.0, 1.0));
   private final SliderSetting activeGlow = this.add(new SliderSetting("Glow", 10.0, 0.0, 20.0, 1.0));
   private final ColorSetting idleColor = this.add(new ColorSetting("IdleColor", new Color(11, 11, 16, 170)));
   private final ColorSetting idleStroke = this.add(new ColorSetting("IdleStroke", new Color(255, 255, 255, 34)));
   private final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255, 255)));
   private final Keystrokes.KeyVisual forwardVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual leftVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual backVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual rightVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual leftMouseVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual rightMouseVisual = new Keystrokes.KeyVisual();
   private final Keystrokes.KeyVisual jumpVisual = new Keystrokes.KeyVisual();

   public Keystrokes() {
      super("Keystrokes", Module.Category.Render);
      this.setChinese("\u6309\u952e\u663e\u793a");
      FeatureTranslations.localizeModule(this);
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         DrawContext context = event.drawContext;
         MatrixStack matrices = event.drawContext.getMatrices();
         float x = this.posX.getValueFloat();
         float y = this.posY.getValueFloat();
         float width = this.keyWidth.getValueFloat();
         float height = this.keyHeight.getValueFloat();
         float gapValue = this.gap.getValueFloat();
         float row2Y = y + height + gapValue;
         float row3Y = row2Y + height + gapValue;
         float row4Y = row3Y + height + gapValue;
         this.update(this.forwardVisual, mc.options.forwardKey);
         this.update(this.leftVisual, mc.options.leftKey);
         this.update(this.backVisual, mc.options.backKey);
         this.update(this.rightVisual, mc.options.rightKey);
         this.update(this.leftMouseVisual, mc.options.attackKey);
         this.update(this.rightMouseVisual, mc.options.useKey);
         this.update(this.jumpVisual, mc.options.jumpKey);
         this.drawKey(context, matrices, x + width + gapValue, y, width, height, "W", this.forwardVisual);
         this.drawKey(context, matrices, x, row2Y, width, height, "A", this.leftVisual);
         this.drawKey(context, matrices, x + width + gapValue, row2Y, width, height, "S", this.backVisual);
         this.drawKey(context, matrices, x + (width + gapValue) * 2.0F, row2Y, width, height, "D", this.rightVisual);
         this.drawKey(context, matrices, x, row3Y, width * 1.5F + gapValue * 0.5F, height, "\u5de6\u952e", this.leftMouseVisual);
         this.drawKey(
            context, matrices, x + width * 1.5F + gapValue * 1.5F, row3Y, width * 1.5F + gapValue * 0.5F, height, "\u53f3\u952e", this.rightMouseVisual
         );
         this.drawKey(context, matrices, x, row4Y, width * 3.0F + gapValue * 2.0F, height, "\u7a7a\u683c", this.jumpVisual);
      }
   }

   private void update(Keystrokes.KeyVisual visual, KeyBinding binding) {
      float target = binding.isPressed() ? 1.0F : 0.0F;
      visual.progress = visual.progress + (target - visual.progress) * 0.22F;
   }

   private void drawKey(DrawContext context, MatrixStack matrices, float x, float y, float width, float height, String label, Keystrokes.KeyVisual visual) {
      Color theme = this.getThemeColor();
      float progress = this.clamp(visual.progress);
      float currentRadius = Math.min(this.radius.getValueFloat(), height / 2.0F);
      Color activeFill = this.blend(new Color(38, 56, 92, 220), ColorUtil.injectAlpha(theme.brighter(), 255), progress);
      Color fillColor = progress > 0.01F ? activeFill : this.idleColor.getValue();
      Color strokeColor = progress > 0.01F ? ColorUtil.injectAlpha(Color.WHITE, (int)(100.0F + 90.0F * progress)) : this.idleStroke.getValue();
      Render2DUtil.drawRoundedRect(matrices, x, y, width, height, currentRadius, fillColor);
      Render2DUtil.drawRoundedStroke(matrices, x, y, width, height, currentRadius, strokeColor, 18);
      if (progress > 0.01F) {
         float glow = this.activeGlow.getValueFloat() * progress;
         Color glowColor = ColorUtil.injectAlpha(theme, (int)(95.0F * progress));
         Render2DUtil.drawRoundedRect(
            matrices, x - glow * 0.18F, y - glow * 0.18F, width + glow * 0.36F, height + glow * 0.36F, currentRadius + glow * 0.12F, glowColor
         );
         Render2DUtil.drawRoundedRect(
            matrices,
            x + 1.0F,
            y + 1.0F,
            width - 2.0F,
            Math.max(2.0F, height * 0.22F),
            Math.max(1.0F, currentRadius - 1.0F),
            new Color(255, 255, 255, (int)(22.0F * progress))
         );
      }

      int textWidth = mc.textRenderer.getWidth(label);
      int textX = Math.round(x + (width - textWidth) / 2.0F);
      int textY = Math.round(y + (height - 8.0F) / 2.0F);
      context.drawText(mc.textRenderer, Text.literal(label), textX, textY, this.textColor.getValue().getRGB(), false);
   }

   private Color getThemeColor() {
      return new Color(95, 155, 255, 255);
   }

   private float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private Color blend(Color from, Color to, float progress) {
      float clamped = this.clamp(progress);
      int red = (int)(from.getRed() + (to.getRed() - from.getRed()) * clamped);
      int green = (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
      int blue = (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
      int alpha = (int)(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
      return new Color(red, green, blue, alpha);
   }

   private static class KeyVisual {
      private float progress;
   }
}
