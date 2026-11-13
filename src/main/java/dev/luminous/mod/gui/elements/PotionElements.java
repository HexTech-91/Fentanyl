package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.PotionHud;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class PotionElements extends Tab {
   public PotionElements() {
      this.width = 50;
      this.height = 50;
      this.x = Alien.CONFIG.getInt("potion_x", 0);
      this.y = Alien.CONFIG.getInt("potion_y", 50);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && PotionHud.INSTANCE.isOn()) {
         if (mouseX >= this.x && mouseX <= this.x + this.width) {
            if (mouseY >= this.y && mouseY <= this.y + this.height) {
               if (ClickGuiScreen.clicked) {
                  HudManager.currentGrabbed = this;
               }
            }
         }

         this.width = (int)PotionHud.INSTANCE.hAnimation;
         this.height = (int)PotionHud.INSTANCE.vAnimation;
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (!PotionHud.INSTANCE.isOn()) {
         return;
      }

      PotionHud.INSTANCE.onRender2D(drawContext, partialTicks);
      Render2DUtil.drawRectOutline(
         drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
      );
   }
}
