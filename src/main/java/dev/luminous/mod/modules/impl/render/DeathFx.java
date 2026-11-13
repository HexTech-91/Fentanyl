package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.skia.Skia;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import java.awt.Color;
import java.lang.reflect.Method;

public final class DeathFx {
   public static final String TEXT = "Ezzzzzz";
   private static Method layerMethod;
   private static boolean layerResolved;
   private static float cachedSize = -1.0F;
   private static float cachedAvailW = -1.0F;
   private static Font cachedFont;
   private static float cachedW;
   private static float cachedH;

   private DeathFx() {
   }

   public static int pushLayer(float var0, int var1, int var2, int var3, int var4) {
      Canvas var5 = Skia.getCanvas();
      if (var5 == null) {
         return 0;
      } else {
         int var6 = Math.round(var0 * 255.0F);
         if (var6 < 0) {
            var6 = 0;
         }

         if (var6 > 255) {
            var6 = 255;
         }

         if (!layerResolved) {
            layerResolved = true;

            try {
               for (Method var10 : var5.getClass().getMethods()) {
                  Class[] var11 = var10.getParameterTypes();
                  if (var10.getName().equals("saveLayerAlpha") && var11.length == 2 && !var11[0].isPrimitive() && var11[1] == int.class) {
                     layerMethod = var10;
                     break;
                  }
               }
            } catch (Throwable var14) {
            }
         }

         if (layerMethod != null) {
            try {
               Object var15 = layerMethod.invoke(var5, Rect.makeXYWH(var1, var2, var3, var4), var6);
               if (var15 instanceof Integer) {
                  return (Integer)var15;
               }
            } catch (Throwable var13) {
            }

            try {
               Object var16 = layerMethod.invoke(var5, null, var6);
               if (var16 instanceof Integer) {
                  return (Integer)var16;
               }
            } catch (Throwable var12) {
            }
         }

         return var5.save();
      }
   }

   public static long clampMs(double var0) {
      long var2 = (long)var0;
      return var2 < 1L ? 1L : var2;
   }

   public static boolean inWindow(long var0, long var2, double var4, double var6) {
      return var0 <= 0L ? false : var2 - var0 < clampMs(var4) + clampMs(var6);
   }

   public static float alphaFactor(long var0, long var2, double var4, double var6) {
      if (var0 <= 0L) {
         return 1.0F;
      } else {
         long var8 = clampMs(var4);
         long var10 = clampMs(var6);
         long var12 = var2 - var0;
         if (var12 >= var8 + var10) {
            return 0.0F;
         } else {
            return var12 <= var8 ? 1.0F : 1.0F - (float)(var12 - var8) / (float)var10;
         }
      }
   }

   public static void drawCard(int var0, int var1, int var2, int var3, float var4) {
      if (var2 > 10 && var3 > 10 && !(var4 <= 0.001F)) {
         float var5 = var2 - 8.0F;
         float var6 = var3 - 8.0F;
         float var7 = Math.min(var6 * 0.9F, 22.0F);
         if (var7 < 4.0F) {
            var7 = 4.0F;
         }

         if (var7 != cachedSize || var5 != cachedAvailW) {
            Font var8 = Skia.getDefaultFont("Ezzzzzz", var7, true);
            float var9 = Skia.getStringWidth("Ezzzzzz", var8);
            if (var9 > var5 && var9 > 0.0F) {
               var7 *= var5 / var9;
               if (var7 < 4.0F) {
                  var7 = 4.0F;
               }

               var8 = Skia.getDefaultFont("Ezzzzzz", var7, true);
               var9 = Skia.getStringWidth("Ezzzzzz", var8);
            }

            cachedSize = var7;
            cachedAvailW = var5;
            cachedFont = var8;
            cachedW = var9;
            cachedH = Skia.getFontHeight(var8);
         }

         int var10 = Math.round(255.0F * var4);
         if (var10 < 0) {
            var10 = 0;
         }

         if (var10 > 255) {
            var10 = 255;
         }

         Color var11 = new Color(248, 248, 248, var10);
         Skia.drawText("Ezzzzzz", var0 + (var2 - cachedW) / 2.0F, var1 + (var3 - cachedH) / 2.0F, var11, cachedFont);
      }
   }

   public static void drawCardText(int var0, int var1, int var2, int var3, float var4, String var5) {
      if (var2 > 10 && var3 > 10 && !(var4 <= 0.001F)) {
         float var6 = var2 - 8.0F;
         float var7 = Math.min(var3 * 0.9F, 22.0F);
         if (var7 < 4.0F) {
            var7 = 4.0F;
         }

         Font var8 = Skia.getDefaultFont(var5, var7, true);
         float var9 = Skia.getStringWidth(var5, var8);
         if (var9 > var6 && var9 > 0.0F) {
            var7 *= var6 / var9;
            if (var7 < 4.0F) {
               var7 = 4.0F;
            }

            var8 = Skia.getDefaultFont(var5, var7, true);
            var9 = Skia.getStringWidth(var5, var8);
         }

         int var10 = Math.round(255.0F * var4);
         if (var10 < 0) {
            var10 = 0;
         }

         if (var10 > 255) {
            var10 = 255;
         }

         Color var11 = new Color(248, 248, 248, var10);
         Skia.drawText(var5, var0 + (var2 - var9) / 2.0F, var1 + (var3 - Skia.getFontHeight(var8)) / 2.0F, var11, var8);
      }
   }
}
