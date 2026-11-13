package dev.luminous.mod.gui.tclick.core;

import java.awt.Color;

public final class TColor {
   public static Color hcolor1 = new Color(-6974059);
   public static Color acolor = new Color(-8365735);
   public static Color plateColor = new Color(-419430400, true);
   public static Color textColor = Color.WHITE;
   public static Color textColor2 = Color.WHITE;
   public static Color blurColor = new Color(-16773595, true);
   public static float alpha = 0.9F;
   public static float blurStrength = 20.0F;
   public static float blurOpacity = 0.55F;
   public static float hudRound = 4.0F;
   public static int colorSpeed = 18;
   public static Color copy_color = Color.WHITE;
   public static TColor.ColorMode colorMode = TColor.ColorMode.Static;

   private TColor() {
   }

   public static Color getColor(int var0) {
      switch (colorMode) {
         case Sky:
            return skyRainbow(colorSpeed, var0);
         case LightRainbow:
            return rainbow(colorSpeed, var0, 0.6F, 1.0F, 1.0F);
         case Rainbow:
            return rainbow(colorSpeed, var0, 1.0F, 1.0F, 1.0F);
         case Fade:
            return fade(colorSpeed, var0, hcolor1, 1.0F);
         case DoubleColor:
            return twoColoreffect(hcolor1, acolor, colorSpeed, var0);
         case Analogous:
            return interpolateColorsBackAndForth(colorSpeed, var0, hcolor1, getAnalogousColor(acolor), true);
         default:
            return hcolor1;
      }
   }

   public static Color getColor(double var0) {
      return getColor((int)var0);
   }

   public static Color skyRainbow(int var0, int var1) {
      int var2 = (int)((System.currentTimeMillis() / var0 + var1) % 360L);
      int var3;
      return Color.getHSBColor(var2 / 360.0F < 0.5F ? -(var2 / 360.0F) : (var3 = var2 % 360) / 360.0F, 0.5F, 1.0F);
   }

   public static Color rainbow(int var0, int var1, float var2, float var3, float var4) {
      int var5 = (int)((System.currentTimeMillis() / var0 + var1) % 360L);
      float var6 = var5 / 360.0F;
      Color var7 = new Color(Color.HSBtoRGB(var6, var2, var3));
      return new Color(var7.getRed(), var7.getGreen(), var7.getBlue(), Math.max(0, Math.min(255, (int)(var4 * 255.0F))));
   }

   public static Color fade(int var0, int var1, Color var2, float var3) {
      float[] var4 = Color.RGBtoHSB(var2.getRed(), var2.getGreen(), var2.getBlue(), null);
      int var5 = (int)((System.currentTimeMillis() / var0 + var1) % 360L);
      var5 = (var5 > 180 ? 360 - var5 : var5) + 180;
      Color var6 = new Color(Color.HSBtoRGB(var4[0], var4[1], var5 / 360.0F));
      return new Color(var6.getRed(), var6.getGreen(), var6.getBlue(), Math.max(0, Math.min(255, (int)(var3 * 255.0F))));
   }

   public static Color twoColoreffect(Color var0, Color var1, double var2, double var4) {
      int var6 = (int)((System.currentTimeMillis() / var2 + var4) % 360.0);
      var6 = (var6 >= 180 ? 360 - var6 : var6) * 2;
      return interpolateColorC(var0, var1, var6 / 360.0F);
   }

   public static Color interpolateColorsBackAndForth(int var0, int var1, Color var2, Color var3, boolean var4) {
      int var5 = (int)((System.currentTimeMillis() / var0 + var1) % 360L);
      var5 = (var5 >= 180 ? 360 - var5 : var5) * 2;
      return var4 ? interpolateColorHue(var2, var3, var5 / 360.0F) : interpolateColorC(var2, var3, var5 / 360.0F);
   }

   public static Color getAnalogousColor(Color var0) {
      float[] var1 = Color.RGBtoHSB(var0.getRed(), var0.getGreen(), var0.getBlue(), null);
      float var2 = 0.84F;
      float var3 = var1[0] - var2;
      return new Color(Color.HSBtoRGB(var3, var1[1], var1[2]));
   }

   public static Color interpolateColorC(Color var0, Color var1, float var2) {
      var2 = Math.min(1.0F, Math.max(0.0F, var2));
      return new Color(
         interpolateInt(var0.getRed(), var1.getRed(), var2),
         interpolateInt(var0.getGreen(), var1.getGreen(), var2),
         interpolateInt(var0.getBlue(), var1.getBlue(), var2),
         interpolateInt(var0.getAlpha(), var1.getAlpha(), var2)
      );
   }

   public static Color interpolateColorHue(Color var0, Color var1, float var2) {
      var2 = Math.min(1.0F, Math.max(0.0F, var2));
      float[] var3 = Color.RGBtoHSB(var0.getRed(), var0.getGreen(), var0.getBlue(), null);
      float[] var4 = Color.RGBtoHSB(var1.getRed(), var1.getGreen(), var1.getBlue(), null);
      Color var5 = Color.getHSBColor(
         interpolateFloat(var3[0], var4[0], var2), interpolateFloat(var3[1], var4[1], var2), interpolateFloat(var3[2], var4[2], var2)
      );
      return new Color(var5.getRed(), var5.getGreen(), var5.getBlue(), interpolateInt(var0.getAlpha(), var1.getAlpha(), var2));
   }

   public static Color applyOpacity(Color var0, float var1) {
      var1 = Math.min(1.0F, Math.max(0.0F, var1));
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)(var0.getAlpha() * var1));
   }

   public static Color injectAlpha(Color var0, int var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), Math.max(0, Math.min(255, var1)));
   }

   public static Color darker(Color var0, float var1) {
      return new Color(
         Math.max((int)(var0.getRed() * var1), 0), Math.max((int)(var0.getGreen() * var1), 0), Math.max((int)(var0.getBlue() * var1), 0), var0.getAlpha()
      );
   }

   private static float interpolateFloat(float var0, float var1, double var2) {
      return (float)interpolate(var0, var1, (float)var2);
   }

   private static int interpolateInt(int var0, int var1, double var2) {
      return (int)interpolate(var0, var1, (float)var2);
   }

   private static double interpolate(double var0, double var2, double var4) {
      return var0 + (var2 - var0) * var4;
   }

   public static enum ColorMode {
      Static,
      Sky,
      LightRainbow,
      Rainbow,
      Fade,
      DoubleColor,
      Analogous;
   }
}
