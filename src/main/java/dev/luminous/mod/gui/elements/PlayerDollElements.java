package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.PlayerDoll;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class PlayerDollElements extends Tab {
   public static boolean resizing = false;

   public PlayerDollElements() {
      this.width = Alien.CONFIG.getInt("playerdoll_w", 70);
      this.height = Alien.CONFIG.getInt("playerdoll_h", 130);
      this.x = Alien.CONFIG.getInt("playerdoll_x", 50);
      this.y = Alien.CONFIG.getInt("playerdoll_y", 50);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (!PlayerDoll.INSTANCE.isOn()) {
         return;
      }

      if (HudManager.currentGrabbed != null || !ClickGuiScreen.clicked) {
         return;
      }

      if (mouseX >= this.x + this.width - 8 && mouseY >= this.y + this.height - 8 && mouseX <= this.x + this.width + 8 && mouseY <= this.y + this.height + 8) {
         resizing = true;
      } else if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
         HudManager.currentGrabbed = this;
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (!PlayerDoll.INSTANCE.isOn()) {
         return;
      }

      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      Render2DUtil.drawHudBase(matrices, this.x, this.y, this.width, this.height);
      Render2DUtil.drawRectWithOutline(matrices, this.x, this.y, this.width, this.height, new Color(0, 0, 0, 0), new Color(255, 255, 255, 200), true, 1.0F);
      if (resizing && ClickGuiScreen.clicked) {
         int mx = Alien.GUI.getMouseX();
         int my = Alien.GUI.getMouseY();
         this.width = Math.max(40, mx - this.x);
         this.height = Math.max(70, my - this.y);
      } else {
         resizing = false;
      }

      PlayerDoll.INSTANCE.onRender2D(drawContext, partialTicks);
      matrices.pop();
   }
}
