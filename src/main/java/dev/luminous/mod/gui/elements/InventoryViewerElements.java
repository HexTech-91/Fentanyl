package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.hud.InventoryViewer;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class InventoryViewerElements extends Tab {
   private static final int SLOT = 18;
   private static final int PADDING = 2;

   public InventoryViewerElements() {
      this.width = 9 * SLOT + PADDING * 2;
      this.height = 4 * SLOT + PADDING * 2;
      this.x = Alien.CONFIG.getInt("inventoryviewer_x", 150);
      this.y = Alien.CONFIG.getInt("inventoryviewer_y", 50);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (!InventoryViewer.INSTANCE.isOn()) {
         return;
      }

      if (HudManager.currentGrabbed != null || !ClickGuiScreen.clicked) {
         return;
      }

      if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
         HudManager.currentGrabbed = this;
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      if (!InventoryViewer.INSTANCE.isOn()) {
         return;
      }

      if (mc.player == null) {
         return;
      }

      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      Render2DUtil.drawHudBase(matrices, this.x, this.y, this.width, this.height);

      for (int i = 0; i < 36; i++) {
         int col = i % 9;
         int row = i < 9 ? 3 : (i - 9) / 9;
         int slotX = this.x + PADDING + col * SLOT;
         int slotY = this.y + PADDING + row * SLOT;
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            drawContext.drawItem(stack, slotX + 1, slotY + 1);
            drawContext.drawItemInSlot(mc.textRenderer, stack, slotX + 1, slotY + 1);
         }
      }

      matrices.pop();
   }
}
