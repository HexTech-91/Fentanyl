package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.impl.hud.NotificationHud;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class NotificationElements extends Tab {
   public NotificationElements() {
      this.x = NotificationHud.INSTANCE.x.getValueInt();
      this.y = NotificationHud.INSTANCE.y.getValueInt();
      this.width = (int)NotificationHud.getCardWidth();
      this.height = (int)NotificationHud.getCardHeight();
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (NotificationHud.INSTANCE == null || !NotificationHud.INSTANCE.isOn()) {
         return;
      }

      this.width = (int)NotificationHud.getCardWidth();
      this.height = (int)NotificationHud.getCardHeight();
      if (HudManager.currentGrabbed == null) {
         this.x = NotificationHud.INSTANCE.x.getValueInt();
         this.y = NotificationHud.INSTANCE.y.getValueInt();
         if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
            if (ClickGuiScreen.clicked) {
               HudManager.currentGrabbed = this;
            }
         }
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (NotificationHud.INSTANCE == null || !NotificationHud.INSTANCE.isOn()) {
         return;
      }

      NotificationHud.INSTANCE.x.setValue(this.x);
      NotificationHud.INSTANCE.y.setValue(this.y);
      NotificationHud.INSTANCE.onRender2D(drawContext, partialTicks);
      if (Alien.GUI.isClickGuiOpen() && HudEditor.INSTANCE.isOn()) {
         Render2DUtil.drawRectOutline(
            drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(255, 255, 255, 120), 0.5F
         );
      }
   }
}
