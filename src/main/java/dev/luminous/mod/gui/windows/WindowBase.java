package dev.luminous.mod.gui.windows;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.items.Item;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class WindowBase extends Item {
   public WindowBase(String name) {
      super(name);
   }

   @Override
   protected void drawString(String text, double x, double y, Color color) {
      this.drawString(text, x, y, color.hashCode());
   }

   @Override
   protected void drawString(String text, double x, double y, int color) {
      boolean shadow = FontManager.isShadowEnabled();
      if (FontManager.isCustomFontEnabled()) {
         FontManager.ui.drawString(Item.context.getMatrices(), text, (int)x, (int)y, color, shadow);
      } else {
         Item.context.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      }
   }

   public void drawScreen(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
   }

   public void mouseClicked(int mouseX, int mouseY, int x, int y, int width, int height, int button) {
   }

   public void mouseReleased(int mouseX, int mouseY, int x, int y, int width, int height, int button) {
   }

   public void mouseScrolled(int mouseX, int mouseY, int x, int y, int width, int height, double verticalAmount) {
   }

   public void keyPressed(int keyCode, int x, int y, int width, int height) {
   }

   public void charTyped(char chr, int x, int y, int width, int height) {
   }

   protected void drawRect(DrawContext context, float x, float y, float width, float height, Color color) {
      Render2DUtil.rect(context.getMatrices(), x, y, x + width, y + height, color.getRGB());
   }

   protected void drawRect(DrawContext context, float x, float y, float width, float height, int color) {
      Render2DUtil.rect(context.getMatrices(), x, y, x + width, y + height, color);
   }

   protected boolean isHovering(int mouseX, int mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
