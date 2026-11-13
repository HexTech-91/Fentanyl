package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.PlayerInfo;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class PlayerInfoElements extends Tab {
   public PlayerInfoElements() {
      this.width = 200;
      this.height = 80;
      this.x = Alien.CONFIG.getInt("playerinfo_x", 50);
      this.y = Alien.CONFIG.getInt("playerinfo_y", 50);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && PlayerInfo.INSTANCE.isOn()) {
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
      if (!PlayerInfo.INSTANCE.isOn()) {
         return;
      }

      PlayerInfo.INSTANCE.onRender2D(drawContext, partialTicks);
      Render2DUtil.drawRectOutline(
         drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
      );
   }
}
