package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class Button extends Item {
   private boolean state;
   private boolean wasHovered;
   public static int hoverColor = -2007673515;
   public static int defaultColor = 290805077;
   public static int defaultTextColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String var1) {
      super(var1);
      this.setHeight(15);
   }

   @Override
   public void drawScreen(DrawContext var1, int var2, int var3, float var4) {
      boolean var5 = this.isHovering(var2, var3);
      if (var5 && !this.wasHovered) {
         hoverSound();
      }

      this.wasHovered = var5;
      float var6 = this.height - 0.5F;
      if (this.getState()) {
         int var7 = var5 ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
         double var8 = this.getColorDelay();
         if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
            Render2DUtil.drawLutRect(
               var1.getMatrices(),
               this.x,
               this.y,
               this.width,
               var6,
               ClickGui.getInstance().getSpectrumLutId(),
               ClickGui.getInstance().getSpectrumLutHeight(),
               var7
            );
         } else {
            Color var10 = ClickGui.getInstance().getActiveColor(var8);
            Render2DUtil.rect(var1.getMatrices(), this.x, this.y, this.x + this.width, this.y + this.height - 0.5F, ColorUtil.injectAlpha(var10, var7).getRGB());
         }
      } else {
         Render2DUtil.rect(var1.getMatrices(), this.x, this.y, this.x + this.width, this.y + this.height - 0.5F, !var5 ? defaultColor : hoverColor);
      }

      float var11 = this.getCenteredTextY(this.y, this.height - 0.5F);
      this.drawString(this.getName(), this.x + 2.3F, var11, this.getState() ? enableTextColor : defaultTextColor);
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (var3 == 0 && this.isHovering(var1, var2)) {
         this.onMouseClick();
      }
   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      sound();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int var1, int var2) {
      for (Component var4 : ClickGuiScreen.getInstance().getComponents()) {
         if (var4.drag) {
            return false;
         }
      }

      return var1 >= this.getX() && var1 <= this.getX() + this.getWidth() && var2 >= this.getY() && var2 <= this.getY() + this.height - 1.0F;
   }
}
