package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.impl.hud.Armor;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ArmorElements extends Tab {
   public ArmorElements() {
      this.width = 80;
      this.height = 34;
      this.x = Alien.CONFIG.getInt("armor_x", 0);
      this.y = Alien.CONFIG.getInt("armor_y", 200);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && Armor.INSTANCE.isOn()) {
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
      if (Armor.INSTANCE.isOn()) {
         if (Alien.GUI.isClickGuiOpen() && HudEditor.INSTANCE.isOn()) {
            Render2DUtil.drawRect(drawContext.getMatrices(), this.x, this.y, this.width, this.height, new Color(0, 0, 0, 70));
         }

         if (mc.player == null) {
            return;
         }

         if (Armor.INSTANCE.lockOnHotbar.getValue()) {
            this.x = mc.getWindow().getWidth() / 4 + 12;
            this.y = mc.getWindow().getScaledHeight() - this.height;
            if (mc.player.isCreative()) {
               this.y -= 25;
            } else if (mc.player.isInFluid() && mc.player.getAir() < mc.player.getMaxAir()) {
               this.y -= 48;
            } else {
               this.y -= 38;
            }
         }

         int xOff = 0;

         for (ItemStack armor : mc.player.getInventory().armor) {
            xOff += 20;
            if (armor.isEmpty()) {
               continue;
            }

            matrixStack.push();
            int damage = EntityUtil.getDamagePercent(armor);
            int yOffset = this.height / 2;
            drawContext.drawItem(armor, this.x + this.width - xOff, this.y + yOffset);
            drawContext.drawItemInSlot(mc.textRenderer, armor, this.x + this.width - xOff, this.y + yOffset);
            TextUtil.drawStringScale(
               drawContext,
               damage + "%",
               (float)(this.x + this.width + 2 - xOff),
               (float)(this.y + yOffset - mc.textRenderer.fontHeight / 4.0),
               ColorUtil.fadeColor(new Color(196, 0, 0), new Color(0, 227, 0), damage / 100.0F).getRGB(),
               0.5F,
               true
            );
            matrixStack.pop();
         }
      }
   }
}
