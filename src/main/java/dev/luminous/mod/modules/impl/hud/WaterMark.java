package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class WaterMark extends Module {
   public static WaterMark INSTANCE;
   private final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.Default));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(-5013249, true)));
   private final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255)));
   private int step = 0;
   private final Timer timer = new Timer();
   private final float minWidth = 2.0F;
   private float width;
   private float height;
   private float textWidth;
   private float textHeight;

   public WaterMark() {
      super("WaterMark", Module.Category.HUD);
      this.setChinese("水印");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int x = Alien.GUI.waterMark.getX();
      int y = Alien.GUI.waterMark.getY();
      if (this.type.getValue() == Type.Default) {
         String text;
         boolean isVersion = false;
         if (this.width >= this.textWidth) {
            text = "FPS: " + Alien.FPS.getFps();
         } else {
            text = Alien.NAME + " " + Alien.VERSION;
            isVersion = true;
         }

         this.textWidth = Math.max(FontManager.ui.getWidth("FPS: " + Alien.FPS.getFps()), FontManager.ui.getWidth(Alien.NAME + " " + Alien.VERSION));
         this.textHeight = FontManager.ui.getFontHeight();
         if (this.timer.passedMs(this.getStepDuration())) {
            this.step = (this.step + 1) % 4;
            this.timer.reset();
         }

         float progress = Math.min(this.timer.getPassedTimeMs() / (float)this.getStepDuration(), 1.0F);
         switch (this.step) {
            case 0:
               this.height = this.lerp(this.textHeight, this.minWidth, progress);
               this.width = this.minWidth;
               break;
            case 1:
               this.width = this.lerp(this.minWidth, this.textWidth, progress);
               this.height = this.minWidth;
               break;
            case 2:
               this.height = this.lerp(this.minWidth, this.textHeight, progress);
               this.width = this.textWidth;
               break;
            case 3:
               this.width = this.lerp(this.textWidth, this.minWidth, progress);
               this.height = this.textHeight;
         }

         Render2DUtil.drawHudBase(drawContext.getMatrices(), x, y, this.width, this.height);
         if (isVersion && text.length() > 0) {
            String firstLetter = text.substring(0, 1);
            String remaining = text.substring(1);
            float firstWidth = FontManager.ui.getWidth(firstLetter);
            FontManager.ui
               .drawString(
                  drawContext.getMatrices(),
                  firstLetter,
                  (int)(x + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F),
                  (int)y + 2,
                  this.color.getValue().getRGB()
               );
            FontManager.ui
               .drawString(
                  drawContext.getMatrices(),
                  remaining,
                  (int)(x + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F + firstWidth),
                  (int)y + 2,
                  this.textColor.getValue().getRGB()
               );
         } else {
            FontManager.ui
               .drawString(
                  drawContext.getMatrices(),
                  text,
                  (int)(x + this.textWidth / 2.0F - FontManager.ui.getWidth(text) / 2.0F),
                  (int)y + 2,
                  this.textColor.getValue().getRGB()
               );
         }
      }
   }

   private float lerp(float start, float end, float progress) {
      return start + (end - start) * progress;
   }

   private long getStepDuration() {
      return switch (this.step) {
         case 0 -> 600L;
         case 1 -> 800L;
         case 2 -> 800L;
         case 3 -> 600L;
         default -> 1000L;
      };
   }

   private enum Type {
      Default,
      Vape,
      Wurst
   }
}
