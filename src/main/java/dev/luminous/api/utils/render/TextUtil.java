package dev.luminous.api.utils.render;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.FontManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class TextUtil implements Wrapper {
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
   private static final int[] VIEWPORT_CACHE = new int[4];
   private static final Matrix4f COMBINED_CACHE = new Matrix4f();
   private static final Matrix4f PROJ_SCRATCH = new Matrix4f();
   private static final Vector3f TARGET_SCRATCH = new Vector3f();
   private static final Vector4f COORD_SCRATCH = new Vector4f();
   private static long combinedStamp = Long.MIN_VALUE;
   private static boolean combinedValid = false;

   public static float getWidth(String var0) {
      return mc.textRenderer.getWidth(var0);
   }

   public static float getHeight() {
      return 9.0F;
   }

   public static void drawStringWithScale(DrawContext var0, String var1, float var2, float var3, int var4, float var5) {
      MatrixStack var6 = var0.getMatrices();
      if (var5 != 1.0F) {
         var6.push();
         var6.scale(var5, var5, 1.0F);
         if (var5 > 1.0F) {
            var6.translate(-var2 / var5, -var3 / var5, 0.0F);
         } else {
            var6.translate(var2 / var5 / 2.0F, var3 / var5 / 2.0F, 0.0F);
         }
      }

      drawString(var0, var1, var2, var3, var4);
      var6.pop();
   }

   public static void drawStringScale(DrawContext var0, String var1, float var2, float var3, int var4, float var5, boolean var6) {
      MatrixStack var7 = var0.getMatrices();
      if (var5 != 1.0F) {
         var7.push();
         var7.scale(var5, var5, 1.0F);
         if (var5 > 1.0F) {
            var7.translate(-var2 / var5, -var3 / var5, 0.0F);
         } else {
            var7.translate(var2 / var5 / 2.0F, var3 / var5 / 2.0F, 0.0F);
         }
      }

      var0.drawText(mc.textRenderer, var1, (int)var2, (int)var3, var4, var6);
      var7.pop();
   }

   public static void drawString(DrawContext var0, String var1, double var2, double var4, int var6) {
      drawString(var0, var1, var2, var4, var6, false);
   }

   public static void drawString(DrawContext var0, String var1, double var2, double var4, int var6, boolean var7) {
      drawString(var0, var1, var2, var4, var6, var7, true);
   }

   public static void drawString(DrawContext var0, String var1, double var2, double var4, int var6, boolean var7, boolean var8) {
      if (var7) {
         FontManager.ui.drawString(var0.getMatrices(), var1, (int)var2, (int)var4, var6, var8);
      } else {
         var0.drawText(mc.textRenderer, var1, (int)var2, (int)var4, var6, var8);
      }
   }

   public static void drawStringPulse(DrawContext var0, String var1, double var2, double var4, Color var6, Color var7, double var8, int var10, boolean var11) {
      char[] var12 = var1.toCharArray();
      int var13 = 0;
      boolean var14 = false;
      String var15 = null;

      for (char var19 : var12) {
         if (var19 == 167) {
            var14 = true;
         } else if (var14) {
            var15 = var19 == 'r' ? null : "\u00a7" + var19;
            var14 = false;
         } else {
            var13++;
            if (var15 != null) {
               drawString(var0, var15 + var19, var2, var4, var6.getRGB(), var11);
            } else {
               drawString(var0, String.valueOf(var19), var2, var4, ColorUtil.pulseColor(var6, var7, var13, var10, var8).getRGB(), var11);
            }

            var2 += var11 ? FontManager.ui.getWidth(String.valueOf(var19)) : mc.textRenderer.getWidth(String.valueOf(var19));
         }
      }
   }

   public static void drawStringPulse(
      DrawContext var0, String var1, double var2, double var4, Color var6, Color var7, double var8, int var10, boolean var11, boolean var12
   ) {
      char[] var13 = var1.toCharArray();
      int var14 = 0;
      boolean var15 = false;
      String var16 = null;

      for (char var20 : var13) {
         if (var20 == 167) {
            var15 = true;
         } else if (var15) {
            var16 = var20 == 'r' ? null : "\u00a7" + var20;
            var15 = false;
         } else {
            var14++;
            if (var16 != null) {
               drawString(var0, var16 + var20, var2, var4, var6.getRGB(), var11, var12);
            } else {
               drawString(var0, String.valueOf(var20), var2, var4, ColorUtil.pulseColor(var6, var7, var14, var10, var8).getRGB(), var11, var12);
            }

            var2 += var11 ? FontManager.ui.getWidth(String.valueOf(var20)) : mc.textRenderer.getWidth(String.valueOf(var20));
         }
      }
   }

   private static void ensureCombinedCache() {
      long var0 = AlienFrameStamp.value;
      if (!combinedValid || var0 != combinedStamp) {
         GL11.glGetIntegerv(2978, VIEWPORT_CACHE);
         PROJ_SCRATCH.set(lastProjMat);
         COMBINED_CACHE.set(lastModMat);
         PROJ_SCRATCH.mul(COMBINED_CACHE, COMBINED_CACHE);
         combinedStamp = var0;
         combinedValid = true;
      }
   }

   public static Vec3d worldSpaceToScreenSpace(Vec3d var0) {
      GL11.glGetIntegerv(2978, VIEWPORT_CACHE);
      PROJ_SCRATCH.set(lastProjMat);
      COMBINED_CACHE.set(lastModMat);
      PROJ_SCRATCH.mul(COMBINED_CACHE, COMBINED_CACHE);
      Camera var1 = mc.getEntityRenderDispatcher().camera;
      Vec3d var2 = var1.getPos();
      int var3 = mc.getWindow().getHeight();
      int var4 = (int)mc.getWindow().getScaleFactor();
      double var5 = 1.0 / var4;
      double var7 = var0.x - var2.x;
      double var9 = var0.y - var2.y;
      double var11 = var0.z - var2.z;
      Vector4f var13 = COORD_SCRATCH.set((float)var7, (float)var9, (float)var11, 1.0F).mul(lastWorldSpaceMatrix);
      COMBINED_CACHE.project(var13.x(), var13.y(), var13.z(), VIEWPORT_CACHE, TARGET_SCRATCH);
      return new Vec3d(TARGET_SCRATCH.x * var5, (var3 - TARGET_SCRATCH.y) * var5, TARGET_SCRATCH.z);
   }
}
