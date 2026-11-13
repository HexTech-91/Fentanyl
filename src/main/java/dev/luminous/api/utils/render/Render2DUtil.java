package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.shaders.BlurProgram;
import dev.luminous.api.utils.render.shaders.HudShader;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.HudEditor;
import java.awt.Color;
import java.util.Stack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Render2DUtil implements Wrapper {
   public static HudShader HUD_SHADER;
   public static BlurProgram BLUR_PROGRAM;

   public static void initShaders() {
      HUD_SHADER = new HudShader();
      BLUR_PROGRAM = new BlurProgram();
   }

   private static void ensureShaders() {
      if (HUD_SHADER == null || BLUR_PROGRAM == null) {
         initShaders();
      }
   }

   public static void rect(MatrixStack var0, float var1, float var2, float var3, float var4, int var5) {
      rectFilled(var0, var1, var2, var3, var4, var5);
   }

   public static void arrow(MatrixStack var0, float var1, float var2, Color var3) {
      drawRectWithOutline(var0, var1 - 1.0F, var2 - 1.0F, 2.0F, 2.0F, var3, Color.BLACK);
   }

   public static void rectFilled(MatrixStack var0, float var1, float var2, float var3, float var4, int var5) {
      float var6 = (var5 >> 24 & 0xFF) / 255.0F;
      float var7 = (var5 >> 16 & 0xFF) / 255.0F;
      float var8 = (var5 >> 8 & 0xFF) / 255.0F;
      float var9 = (var5 & 0xFF) / 255.0F;
      if (!(var6 <= 0.01)) {
         if (var1 < var3) {
            float var10 = var1;
            var1 = var3;
            var3 = var10;
         }

         if (var2 < var4) {
            float var11 = var2;
            var2 = var4;
            var4 = var11;
         }

         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var12 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         var12.vertex(var0.peek().getPositionMatrix(), var1, var4, 0.0F).color(var7, var8, var9, var6);
         var12.vertex(var0.peek().getPositionMatrix(), var3, var4, 0.0F).color(var7, var8, var9, var6);
         var12.vertex(var0.peek().getPositionMatrix(), var3, var2, 0.0F).color(var7, var8, var9, var6);
         var12.vertex(var0.peek().getPositionMatrix(), var1, var2, 0.0F).color(var7, var8, var9, var6);
         BufferRenderer.drawWithGlobalProgram(var12.end());
         RenderSystem.disableBlend();
      }
   }

   public static void horizontalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6) {
      Matrix4f var7 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var1, var4, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var3, var4, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var3, var2, 0.0F).color(var6.getRGB());
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
   }

   public static void horizontalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, int var5, int var6) {
      Matrix4f var7 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2, 0.0F).color(var5);
      var8.vertex(var7, var1, var4, 0.0F).color(var5);
      var8.vertex(var7, var3, var4, 0.0F).color(var6);
      var8.vertex(var7, var3, var2, 0.0F).color(var6);
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6) {
      Matrix4f var7 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var3, var2, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var3, var4, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var1, var4, 0.0F).color(var6.getRGB());
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, int var5, int var6) {
      Matrix4f var7 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2, 0.0F).color(var5);
      var8.vertex(var7, var3, var2, 0.0F).color(var5);
      var8.vertex(var7, var3, var4, 0.0F).color(var6);
      var8.vertex(var7, var1, var4, 0.0F).color(var6);
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
   }

   public static void drawLinesBatch(MatrixStack var0, float[] var1, int[] var2) {
      if (var1 != null && var2 != null && var1.length >= 4 && var2.length >= 2) {
         Matrix4f var3 = var0.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var4 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         int var5 = Math.min(var1.length / 4, var2.length);

         for (int var6 = 0; var6 < var5; var6++) {
            var4.vertex(var3, var1[var6 * 4], var1[var6 * 4 + 1], 0.0F).color(var2[var6]);
            var4.vertex(var3, var1[var6 * 4 + 2], var1[var6 * 4 + 3], 0.0F).color(var2[var6]);
         }

         BufferRenderer.drawWithGlobalProgram(var4.end());
         RenderSystem.disableBlend();
      }
   }

   public static void drawCirclesBatch(MatrixStack var0, float[] var1, int[] var2, int var3) {
      if (var1 != null && var2 != null && var1.length >= 3) {
         Matrix4f var4 = var0.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var5 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         int var6 = Math.min(var1.length / 3, var2.length);

         for (int var7 = 0; var7 < var6; var7++) {
            float var8 = var1[var7 * 3];
            float var9 = var1[var7 * 3 + 1];
            float var10 = var1[var7 * 3 + 2];
            var5.vertex(var4, var8, var9, 0.0F).color(var2[var7]);

            for (int var11 = 0; var11 <= var3; var11++) {
               double var12 = var11 * (Math.PI * 2) / var3;
               var5.vertex(var4, (float)(var8 + Math.cos(var12) * var10), (float)(var9 + Math.sin(var12) * var10), 0.0F).color(var2[var7]);
            }
         }

         BufferRenderer.drawWithGlobalProgram(var5.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawLine(MatrixStack var0, float var1, float var2, float var3, float var4, int var5) {
      Matrix4f var6 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      var7.vertex(var6, var1, var2, 0.0F).color(var5);
      var7.vertex(var6, var3, var4, 0.0F).color(var5);
      BufferRenderer.drawWithGlobalProgram(var7.end());
      RenderSystem.disableBlend();
   }

   public static void drawRectWithOutline(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6) {
      Matrix4f var7 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2 + var4, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var1 + var3, var2 + var4, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var1 + var3, var2, 0.0F).color(var5.getRGB());
      var8.vertex(var7, var1, var2, 0.0F).color(var5.getRGB());
      BufferRenderer.drawWithGlobalProgram(var8.end());
      var8 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      var8.vertex(var7, var1, var2 + var4, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var1 + var3, var2 + var4, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var1 + var3, var2, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var1, var2, 0.0F).color(var6.getRGB());
      var8.vertex(var7, var1, var2 + var4, 0.0F).color(var6.getRGB());
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack var0, float var1, float var2, float var3, float var4, int var5) {
      Matrix4f var6 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var7.vertex(var6, var1, var2 + var4, 0.0F).color(var5);
      var7.vertex(var6, var1 + var3, var2 + var4, 0.0F).color(var5);
      var7.vertex(var6, var1 + var3, var2, 0.0F).color(var5);
      var7.vertex(var6, var1, var2, 0.0F).color(var5);
      BufferRenderer.drawWithGlobalProgram(var7.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5) {
      drawRect(var0, var1, var2, var3, var4, var5.getRGB());
   }

   public static void drawRect(DrawContext var0, float var1, float var2, float var3, float var4, Color var5) {
      drawRect(var0.getMatrices(), var1, var2, var3, var4, var5);
   }

   public static boolean isHovered(double var0, double var2, double var4, double var6, double var8, double var10) {
      return var0 >= var4 && var0 - var8 <= var4 && var2 >= var6 && var2 - var10 <= var6;
   }

   public static void drawGlow(MatrixStack var0, float var1, float var2, float var3, float var4, int var5) {
      Matrix4f var6 = var0.peek().getPositionMatrix();
      int var7 = ColorUtil.injectAlpha(var5, 20);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      BufferBuilder var8 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float var9 = var3 / 2.0F;
      float var10 = var4 / 2.0F;
      float var11 = var1 + var9;
      float var12 = var2 + var10;
      float var13 = var1 + var3;
      float var14 = var2 + var4;
      var8.vertex(var6, var11, var12, 0.0F).color(var5);
      var8.vertex(var6, var1, var12, 0.0F).color(var7);
      var8.vertex(var6, var1, var2, 0.0F).color(var7);
      var8.vertex(var6, var11, var2, 0.0F).color(var7);
      var8.vertex(var6, var11, var12, 0.0F).color(var5);
      var8.vertex(var6, var11, var2, 0.0F).color(var7);
      var8.vertex(var6, var13, var2, 0.0F).color(var7);
      var8.vertex(var6, var13, var12, 0.0F).color(var7);
      var8.vertex(var6, var11, var12, 0.0F).color(var5);
      var8.vertex(var6, var1, var12, 0.0F).color(var7);
      var8.vertex(var6, var1, var14, 0.0F).color(var7);
      var8.vertex(var6, var11, var14, 0.0F).color(var7);
      var8.vertex(var6, var11, var12, 0.0F).color(var5);
      var8.vertex(var6, var13, var12, 0.0F).color(var7);
      var8.vertex(var6, var13, var14, 0.0F).color(var7);
      var8.vertex(var6, var11, var14, 0.0F).color(var7);
      BufferRenderer.drawWithGlobalProgram(var8.end());
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   public static void drawCircle(MatrixStack var0, float var1, float var2, float var3, Color var4, int var5) {
      Matrix4f var6 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      var7.vertex(var6, var1, var2, 0.0F).color(var4.getRGB());

      for (int var8 = 0; var8 <= var5; var8++) {
         double var9 = var8 * (Math.PI * 2) / var5;
         float var11 = (float)(var1 + Math.cos(var9) * var3);
         float var12 = (float)(var2 + Math.sin(var9) * var3);
         var7.vertex(var6, var11, var12, 0.0F).color(var4.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(var7.end());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawPill(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5) {
      float var6 = var4 / 2.0F;
      drawRect(var0, var1 + var6, var2, var3 - 2.0F * var6, var4, var5);
      drawCircle(var0, var1 + var6, var2 + var6, var6, var5, 64);
      drawCircle(var0, var1 + var3 - var6, var2 + var6, var6, var5, 64);
   }

   public static void drawRoundedStroke(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, Color var6, int var7) {
      Matrix4f var8 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var9 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float var10 = Math.min(var5, Math.min(var3, var4) / 2.0F);
      float var11 = var1 + var10;
      float var12 = var2 + var10;
      float var13 = var1 + var3 - var10;
      float var14 = var2 + var4 - var10;

      for (int var15 = 0; var15 <= var7; var15++) {
         double var16 = (-Math.PI / 2) + var15 * (Math.PI / 2) / var7;
         var9.vertex(var8, (float)(var13 + Math.cos(var16) * var10), (float)(var12 + Math.sin(var16) * var10), 0.0F).color(var6.getRGB());
      }

      for (int var18 = 0; var18 <= var7; var18++) {
         double var21 = 0.0 + var18 * (Math.PI / 2) / var7;
         var9.vertex(var8, (float)(var13 + Math.cos(var21) * var10), (float)(var14 + Math.sin(var21) * var10), 0.0F).color(var6.getRGB());
      }

      for (int var19 = 0; var19 <= var7; var19++) {
         double var22 = (Math.PI / 2) + var19 * (Math.PI / 2) / var7;
         var9.vertex(var8, (float)(var11 + Math.cos(var22) * var10), (float)(var14 + Math.sin(var22) * var10), 0.0F).color(var6.getRGB());
      }

      for (int var20 = 0; var20 <= var7; var20++) {
         double var23 = Math.PI + var20 * (Math.PI / 2) / var7;
         var9.vertex(var8, (float)(var11 + Math.cos(var23) * var10), (float)(var12 + Math.sin(var23) * var10), 0.0F).color(var6.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(var9.end());
      RenderSystem.disableBlend();
   }

   public static void drawRainbowRoundedStroke(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, int var6, float var7, int var8) {
      Matrix4f var9 = var0.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var10 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float var11 = Math.min(var5, Math.min(var3, var4) / 2.0F);
      float var12 = var1 + var11;
      float var13 = var2 + var11;
      float var14 = var1 + var3 - var11;
      float var15 = var2 + var4 - var11;
      double var16 = (double)(System.currentTimeMillis() % (long)(1000.0F / Math.max(0.001F, var7))) / (1000.0F / Math.max(0.001F, var7));

      for (int var18 = 0; var18 <= var6; var18++) {
         double var19 = (-Math.PI / 2) + var18 * (Math.PI / 2) / var6;
         float var21 = (float)((var19 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var22 = Color.HSBtoRGB(var21, 1.0F, 1.0F);
         int var23 = var8 << 24 | var22 & 16777215;
         var10.vertex(var9, (float)(var14 + Math.cos(var19) * var11), (float)(var13 + Math.sin(var19) * var11), 0.0F).color(var23);
      }

      for (int var26 = 0; var26 <= var6; var26++) {
         double var30 = 0.0 + var26 * (Math.PI / 2) / var6;
         float var37 = (float)((var30 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var44 = Color.HSBtoRGB(var37, 1.0F, 1.0F);
         int var47 = var8 << 24 | var44 & 16777215;
         var10.vertex(var9, (float)(var14 + Math.cos(var30) * var11), (float)(var15 + Math.sin(var30) * var11), 0.0F).color(var47);
      }

      for (int var27 = 0; var27 <= var6; var27++) {
         double var31 = (Math.PI / 2) + var27 * (Math.PI / 2) / var6;
         float var38 = (float)((var31 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var45 = Color.HSBtoRGB(var38, 1.0F, 1.0F);
         int var48 = var8 << 24 | var45 & 16777215;
         var10.vertex(var9, (float)(var12 + Math.cos(var31) * var11), (float)(var15 + Math.sin(var31) * var11), 0.0F).color(var48);
      }

      for (int var28 = 0; var28 <= var6; var28++) {
         double var32 = Math.PI + var28 * (Math.PI / 2) / var6;
         float var39 = (float)((var32 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var46 = Color.HSBtoRGB(var39, 1.0F, 1.0F);
         int var49 = var8 << 24 | var46 & 16777215;
         var10.vertex(var9, (float)(var12 + Math.cos(var32) * var11), (float)(var13 + Math.sin(var32) * var11), 0.0F).color(var49);
      }

      BufferRenderer.drawWithGlobalProgram(var10.end());
      BufferBuilder var29 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float var33 = Math.max(0.5F, var11 - 1.2F);

      for (int var20 = 0; var20 <= var6; var20++) {
         double var40 = (-Math.PI / 2) + var20 * (Math.PI / 2) / var6;
         float var50 = (float)((var40 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var24 = Color.HSBtoRGB(var50, 1.0F, 1.0F);
         int var25 = var8 << 24 | var24 & 16777215;
         var29.vertex(var9, (float)(var14 + Math.cos(var40) * var33), (float)(var13 + Math.sin(var40) * var33), 0.0F).color(var25);
      }

      for (int var34 = 0; var34 <= var6; var34++) {
         double var41 = 0.0 + var34 * (Math.PI / 2) / var6;
         float var51 = (float)((var41 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var54 = Color.HSBtoRGB(var51, 1.0F, 1.0F);
         int var57 = var8 << 24 | var54 & 16777215;
         var29.vertex(var9, (float)(var14 + Math.cos(var41) * var33), (float)(var15 + Math.sin(var41) * var33), 0.0F).color(var57);
      }

      for (int var35 = 0; var35 <= var6; var35++) {
         double var42 = (Math.PI / 2) + var35 * (Math.PI / 2) / var6;
         float var52 = (float)((var42 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var55 = Color.HSBtoRGB(var52, 1.0F, 1.0F);
         int var58 = var8 << 24 | var55 & 16777215;
         var29.vertex(var9, (float)(var12 + Math.cos(var42) * var33), (float)(var15 + Math.sin(var42) * var33), 0.0F).color(var58);
      }

      for (int var36 = 0; var36 <= var6; var36++) {
         double var43 = Math.PI + var36 * (Math.PI / 2) / var6;
         float var53 = (float)((var43 + (Math.PI * 2)) / (Math.PI * 2) + var16) % 1.0F;
         int var56 = Color.HSBtoRGB(var53, 1.0F, 1.0F);
         int var59 = var8 << 24 | var56 & 16777215;
         var29.vertex(var9, (float)(var12 + Math.cos(var43) * var33), (float)(var13 + Math.sin(var43) * var33), 0.0F).color(var59);
      }

      BufferRenderer.drawWithGlobalProgram(var29.end());
      RenderSystem.disableBlend();
   }

   public static void drawRoundedRect(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      if (var5 <= 0.0F) {
         drawRect(var0, var1, var2, var3, var4, var6);
      } else {
         float var7 = Math.min(var5, Math.min(var3, var4) / 2.0F);
         drawRect(var0, var1 + var7, var2, var3 - 2.0F * var7, var4, var6);
         drawRect(var0, var1, var2 + var7, var7, var4 - 2.0F * var7, var6);
         drawRect(var0, var1 + var3 - var7, var2 + var7, var7, var4 - 2.0F * var7, var6);
         Matrix4f var8 = var0.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         byte var9 = 48;
         float var10 = var1 + var7;
         float var11 = var2 + var7;
         BufferBuilder var12 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         var12.vertex(var8, var10, var11, 0.0F).color(var6.getRGB());

         for (int var13 = 0; var13 <= var9; var13++) {
            double var14 = Math.PI + var13 * (Math.PI / 2) / var9;
            var12.vertex(var8, (float)(var10 + Math.cos(var14) * var7), (float)(var11 + Math.sin(var14) * var7), 0.0F).color(var6.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(var12.end());
         var10 = var1 + var3 - var7;
         var11 = var2 + var7;
         var12 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         var12.vertex(var8, var10, var11, 0.0F).color(var6.getRGB());

         for (int var25 = 0; var25 <= var9; var25++) {
            double var28 = (Math.PI * 3.0 / 2.0) + var25 * (Math.PI / 2) / var9;
            var12.vertex(var8, (float)(var10 + Math.cos(var28) * var7), (float)(var11 + Math.sin(var28) * var7), 0.0F).color(var6.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(var12.end());
         var10 = var1 + var3 - var7;
         var11 = var2 + var4 - var7;
         var12 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         var12.vertex(var8, var10, var11, 0.0F).color(var6.getRGB());

         for (int var26 = 0; var26 <= var9; var26++) {
            double var29 = 0.0 + var26 * (Math.PI / 2) / var9;
            var12.vertex(var8, (float)(var10 + Math.cos(var29) * var7), (float)(var11 + Math.sin(var29) * var7), 0.0F).color(var6.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(var12.end());
         var10 = var1 + var7;
         var11 = var2 + var4 - var7;
         var12 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         var12.vertex(var8, var10, var11, 0.0F).color(var6.getRGB());

         for (int var27 = 0; var27 <= var9; var27++) {
            double var30 = (Math.PI / 2) + var27 * (Math.PI / 2) / var9;
            var12.vertex(var8, (float)(var10 + Math.cos(var30) * var7), (float)(var11 + Math.sin(var30) * var7), 0.0F).color(var6.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(var12.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawDropShadow(MatrixStack var0, float var1, float var2, float var3, float var4, float var5) {
   }

   public static void drawLutRect(MatrixStack var0, float var1, float var2, float var3, float var4, Identifier var5, int var6, int var7) {
      if (var3 != 0.0F && var4 != 0.0F && var6 > 0) {
         float var8 = var1 + var3;
         float var9 = var2 + var4;
         float var10 = Math.min(var1, var8);
         float var11 = Math.max(var1, var8);
         float var12 = Math.min(var2, var9);
         float var13 = Math.max(var2, var9);
         float var14 = Math.max(1, var6 - 1);
         float var15 = Math.max(0.0F, Math.min(1.0F, var12 / var14));
         float var16 = Math.max(0.0F, Math.min(1.0F, var13 / var14));
         float var17 = Math.max(0, Math.min(255, var7)) / 255.0F;
         Matrix4f var18 = var0.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.setShaderTexture(0, var5);
         BufferBuilder var19 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float var20 = 0.5F;
         var19.vertex(var18, var10, var13, 0.0F).texture(var20, var16).color(1.0F, 1.0F, 1.0F, var17);
         var19.vertex(var18, var11, var13, 0.0F).texture(var20, var16).color(1.0F, 1.0F, 1.0F, var17);
         var19.vertex(var18, var11, var12, 0.0F).texture(var20, var15).color(1.0F, 1.0F, 1.0F, var17);
         var19.vertex(var18, var10, var12, 0.0F).texture(var20, var15).color(1.0F, 1.0F, 1.0F, var17);
         BufferRenderer.drawWithGlobalProgram(var19.end());
         RenderSystem.disableBlend();
      }
   }

   public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color color, Color outlineColor, boolean enableOutline, float outlineThickness) {
      drawRectWithOutline(matrices, x, y, width, height, color, outlineColor, enableOutline, outlineThickness, true, true, true, true);
   }

   public static void drawRectWithOutline(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      Color color,
      Color outlineColor,
      boolean enableOutline,
      float outlineThickness,
      Boolean upSide,
      Boolean downSide,
      Boolean leftSide,
      Boolean rightSide
   ) {
      if (color.getAlpha() > 5) {
         drawRect(matrices, x, y, width, height, color);
      }

      if (enableOutline && outlineThickness > 0) {
         drawRectOutline(matrices, x, y, width, height, outlineColor, outlineThickness, upSide, downSide, leftSide, rightSide);
      }
   }

   public static void drawRectOutline(MatrixStack matrices, float x, float y, float width, float height, Color outlineColor, float outlineThickness) {
      drawRectOutline(matrices, x, y, width, height, outlineColor, outlineThickness, true, true, true, true);
   }

   public static void drawRectOutline(
      MatrixStack matrices,
      float rectX,
      float rectY,
      float rectWidth,
      float rectHeight,
      Color color,
      float thickness,
      Boolean upSide,
      Boolean downSide,
      Boolean leftSide,
      Boolean rightSide
   ) {
      float spacing = 0.2F;
      float outlineX = rectX - spacing - thickness;
      float outlineY = rectY - spacing - thickness;
      float outlineWidth = rectWidth + 2.0F * (spacing + thickness);
      float outlineHeight = rectHeight + 2.0F * (spacing + thickness);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      int rgb = color.getRGB();
      float xEnd = outlineX + outlineWidth;
      float yEnd = outlineY + outlineHeight;
      float t = thickness;
      if (upSide) {
         addQuad(buffer, matrix, outlineX + t, outlineY, xEnd - t, outlineY + t, rgb);
      }

      if (rightSide) {
         addQuad(buffer, matrix, xEnd - t, outlineY + t, xEnd, yEnd - t, rgb);
      }

      if (downSide) {
         addQuad(buffer, matrix, outlineX + t, yEnd - t, xEnd - t, yEnd, rgb);
      }

      if (leftSide) {
         addQuad(buffer, matrix, outlineX, outlineY + t, outlineX + t, yEnd - t, rgb);
      }

      addQuad(buffer, matrix, outlineX, outlineY, outlineX + t, outlineY + t, rgb);
      addQuad(buffer, matrix, xEnd - t, outlineY, xEnd, outlineY + t, rgb);
      addQuad(buffer, matrix, xEnd - t, yEnd - t, xEnd, yEnd, rgb);
      addQuad(buffer, matrix, outlineX, yEnd - t, outlineX + t, yEnd, rgb);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      endRender();
   }

   private static void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
      buffer.vertex(matrix, x1, y2, 0.0F).color(color);
      buffer.vertex(matrix, x2, y2, 0.0F).color(color);
      buffer.vertex(matrix, x2, y1, 0.0F).color(color);
      buffer.vertex(matrix, x1, y1, 0.0F).color(color);
   }

   public static void drawRound(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
      renderRoundedQuad(matrices, color, x, y, width + x, height + y, radius, 4.0);
   }

   public static void renderRoundedQuad(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      renderRoundedQuadInternal(
         matrices.peek().getPositionMatrix(),
         c.getRed() / 255.0F,
         c.getGreen() / 255.0F,
         c.getBlue() / 255.0F,
         c.getAlpha() / 255.0F,
         fromX,
         fromY,
         toX,
         toY,
         radius,
         samples
      );
      endRender();
   }

   public static void renderRoundedQuadInternal(
      Matrix4f matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double radius, double samples
   ) {
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      double[][] map = new double[][]{
         {toX - radius, toY - radius, radius},
         {toX - radius, fromY + radius, radius},
         {fromX + radius, fromY + radius, radius},
         {fromX + radius, toY - radius, radius}
      };

      for (int i = 0; i < 4; i++) {
         double[] current = map[i];
         double rad = current[2];

         for (double r = i * 90.0; r < 90.0 + i * 90.0; r += 90.0 / samples) {
            float rad1 = (float)Math.toRadians(r);
            float sin = (float)(Math.sin(rad1) * rad);
            float cos = (float)(Math.cos(rad1) * rad);
            bufferBuilder.vertex(matrix, (float)current[0] + sin, (float)current[1] + cos, 0.0F).color(cr, cg, cb, ca);
         }

         float rad1 = (float)Math.toRadians(90.0 + i * 90.0);
         float sin = (float)(Math.sin(rad1) * rad);
         float cos = (float)(Math.cos(rad1) * rad);
         bufferBuilder.vertex(matrix, (float)current[0] + sin, (float)current[1] + cos, 0.0F).color(cr, cg, cb, ca);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   public static double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
      return (float)interpolate(oldValue, newValue, (float)interpolationValue);
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return (int)interpolate(oldValue, newValue, (float)interpolationValue);
   }

   public static Color injectAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public static Color interpolateColorC(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      return new Color(
         interpolateInt(color1.getRed(), color2.getRed(), amount),
         interpolateInt(color1.getGreen(), color2.getGreen(), amount),
         interpolateInt(color1.getBlue(), color2.getBlue(), amount),
         interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
      );
   }

   public static Color applyOpacity(Color color, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity));
   }

   public static int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity)).getRGB();
   }

   public static void drawRectDumbWay(MatrixStack matrices, float x, float y, float x1, float y1, Color c1) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y1, 0.0F).color(c1.getRGB());
      buffer.vertex(matrix, x1, y1, 0.0F).color(c1.getRGB());
      buffer.vertex(matrix, x1, y, 0.0F).color(c1.getRGB());
      buffer.vertex(matrix, x, y, 0.0F).color(c1.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      endRender();
   }

   public static void draw2DGradientRect(
      MatrixStack matrices, float left, float top, float right, float bottom, Color leftBottomColor, Color leftTopColor, Color rightBottomColor, Color rightTopColor
   ) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, right, top, 0.0F).color(rightTopColor.getRGB());
      bufferBuilder.vertex(matrix, left, top, 0.0F).color(leftTopColor.getRGB());
      bufferBuilder.vertex(matrix, left, bottom, 0.0F).color(leftBottomColor.getRGB());
      bufferBuilder.vertex(matrix, right, bottom, 0.0F).color(rightBottomColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      endRender();
   }

   public static void drawGuiBase(MatrixStack matrices, float x, float y, float width, float height, float radius, float opacity) {
      ensureShaders();
      if (!HudShader.isAvailable()) {
         drawHudFallback(matrices, x, y, width, height, radius, opacity);
         return;
      }
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      HUD_SHADER.setParameters(x, y, width, height, radius, 1.0F, opacity);
      HUD_SHADER.use();
      BufferRenderer.drawWithGlobalProgram(bb.end());
      endRender();
   }

   private static void drawHudFallback(MatrixStack matrices, float x, float y, float width, float height, float radius, float alpha) {
      Color color = ClickGui.getInstance().getColor(270.0);
      int a = (int)(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
      drawRound(matrices, x, y, width, height, radius, new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
   }

   public static void drawHudBase(MatrixStack matrices, float x, float y, float width, float height) {
      drawHudBase(matrices, x, y, width, height, HudEditor.INSTANCE.hudRound.getValueFloat());
   }

   public static void drawHudBase(MatrixStack matrices, float x, float y, float width, float height, float radius) {
      ensureShaders();
      if (HudEditor.INSTANCE.hudStyle.is(HudEditor.HudStyle.Blurry)) {
         drawRoundedBlur(matrices, x, y, width, height, radius, HudEditor.INSTANCE.blurColor.getValue());
      } else if (!HudShader.isAvailable()) {
         drawHudFallback(matrices, x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat());
      } else {
         BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
         HUD_SHADER.setParameters(x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat(), HudEditor.INSTANCE.alpha.getValueFloat());
         HUD_SHADER.use();
         BufferRenderer.drawWithGlobalProgram(bb.end());
         endRender();
      }
   }

   public static void drawHudBase2(
      MatrixStack matrices, float x, float y, float width, float height, float radius, float blurStrenth, float blurOpacity, float animationFactor
   ) {
      ensureShaders();
      if (HudEditor.INSTANCE.hudStyle.is(HudEditor.HudStyle.Blurry)) {
         blurStrenth *= animationFactor;
         blurOpacity = (float)interpolate(1.0F, blurOpacity, animationFactor);
         Color c = interpolateColorC(Color.BLACK, HudEditor.INSTANCE.blurColor.getValue(), animationFactor);
         drawRoundedBlur(matrices, x, y, width, height, radius, c, blurStrenth, blurOpacity);
      } else if (!HudShader.isAvailable()) {
         drawHudFallback(matrices, x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat());
      } else {
         BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
         HUD_SHADER.setParameters(x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat(), HudEditor.INSTANCE.alpha.getValueFloat());
         HUD_SHADER.use();
         BufferRenderer.drawWithGlobalProgram(bb.end());
         endRender();
      }
   }

   public static void drawHudBase(MatrixStack matrices, float x, float y, float width, float height, float radius, boolean hud) {
      ensureShaders();
      if (!HudShader.isAvailable()) {
         drawHudFallback(matrices, x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat());
         return;
      }
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      HUD_SHADER.setParameters(x, y, width, height, radius, HudEditor.INSTANCE.alpha.getValueFloat(), HudEditor.INSTANCE.alpha.getValueFloat());
      HUD_SHADER.use();
      BufferRenderer.drawWithGlobalProgram(bb.end());
      endRender();
   }

   public static void drawHudBase(MatrixStack matrices, float x, float y, float width, float height, float radius, float alpha) {
      ensureShaders();
      if (!HudShader.isAvailable()) {
         drawHudFallback(matrices, x, y, width, height, radius, alpha);
         return;
      }
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      HUD_SHADER.setParameters(x, y, width, height, radius, alpha, HudEditor.INSTANCE.alpha.getValueFloat());
      HUD_SHADER.use();
      BufferRenderer.drawWithGlobalProgram(bb.end());
      endRender();
   }

   public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1) {
      drawRoundedBlur(
         matrices, x, y, width, height, radius, c1, HudEditor.INSTANCE.blurStrength.getValueFloat(), HudEditor.INSTANCE.blurOpacity.getValueFloat()
      );
   }

   public static void drawRoundedBlur(
      MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity
   ) {
      ensureShaders();
      if (BLUR_PROGRAM == null || !BlurProgram.isAvailable()) {
         drawRound(matrices, x, y, width, height, radius, c1);
         return;
      }
      BufferBuilder bb = preShaderDraw(matrices, x - 10.0F, y - 10.0F, width + 20.0F, height + 20.0F);
      BLUR_PROGRAM.setParameters(x, y, width, height, radius, c1, blurStrenth, blurOpacity);
      BLUR_PROGRAM.use();
      BufferRenderer.drawWithGlobalProgram(bb.end());
      endRender();
   }

   public static BufferBuilder preShaderDraw(MatrixStack matrices, float x, float y, float width, float height) {
      ensureShaders();
      setupRender();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
      return buffer;
   }

   public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
      buffer.vertex(matrix, x, y, 0.0F);
      buffer.vertex(matrix, x, y1, 0.0F);
      buffer.vertex(matrix, x1, y1, 0.0F);
      buffer.vertex(matrix, x1, y, 0.0F);
   }

   static final Stack<Render2DUtil.Rectangle> clipStack = new Stack<>();

   public static void addWindow(MatrixStack stack, Render2DUtil.Rectangle r1) {
      Matrix4f matrix = stack.peek().getPositionMatrix();
      Vector4f coord = new Vector4f(r1.x(), r1.y(), 0.0F, 1.0F);
      Vector4f end = new Vector4f(r1.x1(), r1.y1(), 0.0F, 1.0F);
      coord.mulTranspose(matrix);
      end.mulTranspose(matrix);
      float x = coord.x();
      float y = coord.y();
      float endX = end.x();
      float endY = end.y();
      Render2DUtil.Rectangle r = new Render2DUtil.Rectangle(x, y, endX, endY);
      if (clipStack.empty()) {
         clipStack.push(r);
         beginScissor(r.x(), r.y(), r.x1(), r.y1());
      } else {
         Render2DUtil.Rectangle lastClip = clipStack.peek();
         float lsx = lastClip.x();
         float lsy = lastClip.y();
         float lstx = lastClip.x1();
         float lsty = lastClip.y1();
         float nsx = MathHelper.clamp(r.x(), lsx, lstx);
         float nsy = MathHelper.clamp(r.y(), lsy, lsty);
         float nstx = MathHelper.clamp(r.x1(), nsx, lstx);
         float nsty = MathHelper.clamp(r.y1(), nsy, lsty);
         clipStack.push(new Render2DUtil.Rectangle(nsx, nsy, nstx, nsty));
         beginScissor(nsx, nsy, nstx, nsty);
      }
   }

   public static void beginScissor(double x, double y, double endX, double endY) {
      double width = endX - x;
      double height = endY - y;
      width = Math.max(0.0, width);
      height = Math.max(0.0, height);
      float d = mc.options.getGuiScale().getValue();
      int ay = (int)((mc.getWindow().getScaledHeight() - (y + height)) * d);
      RenderSystem.enableScissor((int)(x * d), ay, (int)(width * d), (int)(height * d));
   }

   public static void endScissor() {
      RenderSystem.disableScissor();
   }

   public static void addWindow(MatrixStack stack, float x, float y, float x1, float y1, double animation_factor) {
      float h = y + y1;
      float h2 = (float)(h * (1.0 - clamp(animation_factor, 0.0, 1.0025F)));
      float x3 = x;
      float y3 = y + h2;
      float x4 = x1;
      float y4 = y1 - h2;
      if (x4 < x3) {
         x4 = x3;
      }

      if (y4 < y3) {
         y4 = y3;
      }

      addWindow(stack, new Render2DUtil.Rectangle(x3, y3, x4, y4));
   }

   public static void popWindow() {
      clipStack.pop();
      if (clipStack.empty()) {
         endScissor();
      } else {
         Render2DUtil.Rectangle r = clipStack.peek();
         beginScissor(r.x(), r.y(), r.x1(), r.y1());
      }
   }

   public static double clamp(double num, double min, double max) {
      return num < min ? min : Math.min(num, max);
   }

   public record Rectangle(float x, float y, float x1, float y1) {
      public boolean contains(double x, double y) {
         return x >= this.x && x <= this.x1 && y >= this.y && y <= this.y1;
      }
   }

   public static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void endRender() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
