package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.impl.hud.Totem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;

public class TotemElements extends Tab {
   public TotemElements() {
      this.width = 16;
      this.height = 16;
      this.x = Alien.CONFIG.getInt("totem_x", 0);
      this.y = Alien.CONFIG.getInt("totem_y", 150);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && Totem.INSTANCE.isOn()) {
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
      MatrixStack matrixStack = drawContext.getMatrices();
      if (Alien.GUI.isClickGuiOpen() && HudEditor.INSTANCE.isOn()) {
         Render2DUtil.drawRect(drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(0, 0, 0, 70));
      }

      if (mc.player == null) {
         return;
      }

      if (Totem.INSTANCE.lockOnHotbar.getValue()) {
         this.x = mc.getWindow().getWidth() / 4 - this.width / 2;
         this.y = mc.getWindow().getScaledHeight() - this.height;
         if (mc.player.isCreative()) {
            this.y -= 28;
         } else {
            this.y -= 38;
         }
      }

      matrixStack.push();
      int count = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
      drawContext.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), this.x + this.width - 16, this.y + this.height - 16);
      drawContext.drawItemInSlot(mc.textRenderer, Items.TOTEM_OF_UNDYING.getDefaultStack(), this.x + this.width - 16, this.y + this.height - 16);
      drawContext.drawText(
         mc.textRenderer,
         String.valueOf(count),
         this.x + this.width - 16 + 16 - mc.textRenderer.getWidth(String.valueOf(count)) + 2,
         this.y + this.height - 16 + 16 - mc.textRenderer.fontHeight + 2 + 2,
         new Color(255, 255, 255, 255).getRGB(),
         true
      );
      matrixStack.pop();
   }
}
