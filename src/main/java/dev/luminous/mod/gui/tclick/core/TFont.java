package dev.luminous.mod.gui.tclick.core;

import dev.luminous.mod.gui.fonts.FontRenderer;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;

public final class TFont {
   public static final float Y_OFFSET = -3.0F;
   private final FontRenderer base;

   public TFont(FontRenderer var1) {
      this.base = var1;
   }

   public FontRenderer base() {
      return this.base;
   }

   public float getWidth(String var1) {
      return this.base.getWidth(var1);
   }

   public float getFontHeight() {
      return this.base.getFontHeight();
   }

   public float getStringHeight(String var1) {
      return this.base.getStringHeight(var1);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, int var7) {
      this.base.drawString(var1, var2, var3, var5 + -3.0, var7);
   }

   public void drawString(MatrixStack var1, String var2, float var3, float var4, int var5) {
      this.base.drawString(var1, var2, var3, var4 + -3.0F, var5);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, Color var7) {
      this.base.drawString(var1, var2, var3, var5 + -3.0, var7);
   }

   public void drawString(MatrixStack var1, String var2, float var3, float var4, Color var5) {
      this.base.drawString(var1, var2, var3, var4 + -3.0F, var5);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, int var7, boolean var8) {
      this.base.drawString(var1, var2, var3, var5 + -3.0, var7, var8);
   }

   public void drawStringWithShadow(MatrixStack var1, String var2, double var3, double var5, int var7) {
      this.base.drawStringWithShadow(var1, var2, var3, var5 + -3.0, var7);
   }

   public void drawCenteredString(MatrixStack var1, String var2, double var3, double var5, int var7) {
      this.base.drawCenteredString(var1, var2, var3, var5 + -3.0, var7);
   }

   public void drawCenteredString(MatrixStack var1, String var2, float var3, float var4, int var5) {
      this.base.drawCenteredString(var1, var2, var3, var4 + -3.0F, var5);
   }

   public void drawCenteredString(MatrixStack var1, String var2, double var3, double var5, Color var7) {
      this.base.drawCenteredString(var1, var2, var3, var5 + -3.0, var7);
   }

   public void drawGradientString(MatrixStack var1, String var2, float var3, float var4) {
      this.base.drawGradientString(var1, var2, var3, var4 + -3.0F);
   }
}
