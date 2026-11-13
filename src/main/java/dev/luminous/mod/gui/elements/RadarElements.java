package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.Radar;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class RadarElements extends Tab {
   public RadarElements() {
      this.width = 50;
      this.height = 50;
      this.x = Alien.CONFIG.getInt("radar_x", 0);
      this.y = Alien.CONFIG.getInt("radar_y", 0);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && Radar.INSTANCE.isOn()) {
         if (mouseX >= this.x && mouseX <= this.x + this.width) {
            if (mouseY >= this.y && mouseY <= this.y + this.height) {
               if (ClickGuiScreen.clicked) {
                  HudManager.currentGrabbed = this;
               }
            }
         }

         this.width = Radar.INSTANCE.size.getValueInt();
         this.height = Radar.INSTANCE.size.getValueInt();
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (!Radar.INSTANCE.isOn()) {
         return;
      }

      Radar.INSTANCE.onRender2D(drawContext, partialTicks);
      Render2DUtil.drawRectOutline(
         drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
      );
   }
}
