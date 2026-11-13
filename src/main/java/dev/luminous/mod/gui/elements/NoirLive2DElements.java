package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.impl.hud.NoirLive2D;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class NoirLive2DElements extends Tab {
   public NoirLive2DElements() {
      this.x = NoirLive2D.INSTANCE.x.getValueInt();
      this.y = NoirLive2D.INSTANCE.y.getValueInt();
      this.width = NoirLive2D.INSTANCE.getElementWidth();
      this.height = NoirLive2D.INSTANCE.getElementHeight();
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (NoirLive2D.INSTANCE == null || !NoirLive2D.INSTANCE.isOn()) {
         return;
      }

      this.width = NoirLive2D.INSTANCE.getElementWidth();
      this.height = NoirLive2D.INSTANCE.getElementHeight();
      if (HudManager.currentGrabbed == null) {
         this.x = NoirLive2D.INSTANCE.x.getValueInt();
         this.y = NoirLive2D.INSTANCE.y.getValueInt();
         if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
            if (ClickGuiScreen.clicked) {
               HudManager.currentGrabbed = this;
            }
         }
      }
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (NoirLive2D.INSTANCE == null || !NoirLive2D.INSTANCE.isOn() || button != 0) {
         return;
      }

      if (mc.currentScreen instanceof ClickGuiScreen) {
         return;
      }

      double scale = (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
      double scaledX = mouseX * scale;
      double scaledY = mouseY * scale;
      int elementX = NoirLive2D.INSTANCE.x.getValueInt();
      int elementY = NoirLive2D.INSTANCE.y.getValueInt();
      int elementWidth = NoirLive2D.INSTANCE.getElementWidth();
      int elementHeight = NoirLive2D.INSTANCE.getElementHeight();
      if (scaledX >= elementX && scaledX <= elementX + elementWidth && scaledY >= elementY && scaledY <= elementY + elementHeight) {
         NoirLive2D.INSTANCE.reactToClick(scaledX - elementX, scaledY - elementY, elementWidth, elementHeight);
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (NoirLive2D.INSTANCE == null || !NoirLive2D.INSTANCE.isOn()) {
         return;
      }

      NoirLive2D.INSTANCE.x.setValue(this.x);
      NoirLive2D.INSTANCE.y.setValue(this.y);
      NoirLive2D.INSTANCE.onRender2D(drawContext, partialTicks);
      if (Alien.GUI.isClickGuiOpen() && HudEditor.INSTANCE.isOn()) {
         Render2DUtil.drawRectOutline(
            drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
         );
      }
   }
}
