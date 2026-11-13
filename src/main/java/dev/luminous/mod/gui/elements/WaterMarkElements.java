package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.WaterMark;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class WaterMarkElements extends Tab {
   public WaterMarkElements() {
      this.width = 80;
      this.height = 15;
      this.x = Alien.CONFIG.getInt("watermark_x", 10);
      this.y = Alien.CONFIG.getInt("watermark_y", 10);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && WaterMark.INSTANCE.isOn()) {
         if (mouseX >= this.x && mouseX <= this.x + this.width) {
            if (mouseY >= this.y && mouseY <= this.y + this.height) {
               if (ClickGuiScreen.clicked) {
                  HudManager.currentGrabbed = this;
               }
            }
         }
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (!WaterMark.INSTANCE.isOn()) {
         return;
      }

      WaterMark.INSTANCE.onRender2D(drawContext, partialTicks);
      Render2DUtil.drawRectOutline(
         drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
      );
   }
}
