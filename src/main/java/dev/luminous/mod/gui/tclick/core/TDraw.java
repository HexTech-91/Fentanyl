package dev.luminous.mod.gui.tclick.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.render.Render2DUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

public final class TDraw {
   private static final Stack<TRect> clipStack = new Stack<>();
   public static float globalAlpha = 1.0F;
   private static final Map<Integer, TDraw.TShadow> SHADOW_CACHE = new HashMap<>();

   private TDraw() {
   }

   public static boolean isHovered(double var0, double var2, double var4, double var6, double var8, double var10) {
      return var0 >= var4 && var0 - var8 <= var4 && var2 >= var6 && var2 - var10 <= var6;
   }

   public static double interpolate(double var0, double var2, double var4) {
      return var2 + (var0 - var2) * var4;
   }

   public static float interpolateFloat(float var0, float var1, double var2) {
      return (float)(var0 + (var1 - var0) * var2);
   }

   public static float scrollAnimate(float var0, float var1, float var2) {
      boolean var3 = var0 > var1;
      if (var2 < 0.0F) {
         var2 = 0.0F;
      } else if (var2 > 1.0F) {
         var2 = 1.0F;
      }

      float var4 = Math.max(var0, var1) - Math.min(var0, var1);
      float var5 = var4 * var2;
      return var1 + (var3 ? var5 : -var5);
   }

   public static boolean isDark(Color var0) {
      return isDark(var0.getRed() / 255.0F, var0.getGreen() / 255.0F, var0.getBlue() / 255.0F);
   }

   public static boolean isDark(float var0, float var1, float var2) {
      return colorDistance(var0, var1, var2, 0.0F, 0.0F, 0.0F) < colorDistance(var0, var1, var2, 1.0F, 1.0F, 1.0F);
   }

   private static float colorDistance(float var0, float var1, float var2, float var3, float var4, float var5) {
      float var6 = var3 - var0;
      float var7 = var4 - var1;
      float var8 = var5 - var2;
      return (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
   }

   public static Color applyOpacity(Color var0, float var1) {
      var1 = Math.min(1.0F, Math.max(0.0F, var1));
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), (int)(var0.getAlpha() * var1));
   }

   public static int applyOpacity(int var0, float var1) {
      var1 = Math.min(1.0F, Math.max(0.0F, var1));
      Color var2 = new Color(var0);
      return new Color(var2.getRed(), var2.getGreen(), var2.getBlue(), (int)(var2.getAlpha() * var1)).getRGB();
   }

   public static Color injectAlpha(Color var0, int var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), MathHelper.clamp(var1, 0, 255));
   }

   public static void drawRect(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5) {
      Matrix4f var6 = var0.peek().getPositionMatrix();
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var7.vertex(var6, var1, var2 + var4, 0.0F).color(var5.getRGB());
      var7.vertex(var6, var1 + var3, var2 + var4, 0.0F).color(var5.getRGB());
      var7.vertex(var6, var1 + var3, var2, 0.0F).color(var5.getRGB());
      var7.vertex(var6, var1, var2, 0.0F).color(var5.getRGB());
      BufferRenderer.drawWithGlobalProgram(var7.end());
      endRender();
   }

   public static void drawRound(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, Color var6) {
      resetBlend();
      Render2DUtil.drawRoundedRect(var0, var1, var2, var3, var4, var5, var6);
   }

   public static void draw2DGradientRect(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6, Color var7, Color var8) {
      Matrix4f var9 = var0.peek().getPositionMatrix();
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder var10 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      var10.vertex(var9, var3, var2, 0.0F).color(var8.getRGB());
      var10.vertex(var9, var1, var2, 0.0F).color(var6.getRGB());
      var10.vertex(var9, var1, var4, 0.0F).color(var5.getRGB());
      var10.vertex(var9, var3, var4, 0.0F).color(var7.getRGB());
      BufferRenderer.drawWithGlobalProgram(var10.end());
      endRender();
   }

   public static void verticalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6) {
      resetBlend();
      Render2DUtil.verticalGradient(var0, var1, var2, var3, var4, var5, var6);
   }

   public static void horizontalGradient(MatrixStack var0, float var1, float var2, float var3, float var4, Color var5, Color var6) {
      resetBlend();
      Render2DUtil.horizontalGradient(var0, var1, var2, var3, var4, var5, var6);
   }

   public static void drawHudBase(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, boolean var6) {
      drawPlate(var0, var1, var2, var3, var4, var5, TColor.alpha, TColor.alpha);
   }

   public static void drawHudBase(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, float var6, boolean var7) {
      drawPlate(var0, var1, var2, var3, var4, var5, var6, TColor.alpha);
   }

   public static void drawGuiBase(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      drawPlate(var0, var1, var2, var3, var4, var5, 1.0F, var6);
   }

   private static void drawPlate(MatrixStack var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      resetBlend();
      Color var8 = averageColor(TColor.getColor(270), TColor.getColor(0), TColor.getColor(180), TColor.getColor(90));
      RenderSystem.enableBlend();
      float var9 = 0.0F;

      for (int var10 = 10; var10 >= 0; var10--) {
         float var11 = Math.max(0.0F, Math.min(1.0F, (var10 + 10.0F) / 20.0F));
         float var12 = var11 * var11 * (3.0F - 2.0F * var11);
         float var13 = (1.0F - var12) * var6;
         float var14 = 1.0F - (1.0F - var13) / (1.0F - var9);
         var9 = var13;
         int var15 = (int)(Math.max(0.0F, var14) * 255.0F * globalAlpha);
         if (var15 > 1) {
            Render2DUtil.drawRoundedRect(
               var0,
               var1 - var10,
               var2 - var10,
               var3 + var10 * 2,
               var4 + var10 * 2,
               var5 + var10,
               new Color(var8.getRed(), var8.getGreen(), var8.getBlue(), var15)
            );
         }
      }

      float var16 = 10.0F;
      int var17 = (int)(Math.max(0.0F, Math.min(1.0F, var7)) * 255.0F * globalAlpha);
      Render2DUtil.drawRoundedRect(
         var0, var1, var2, var3, var4, var5, new Color((int)(var8.getRed() / var16), (int)(var8.getGreen() / var16), (int)(var8.getBlue() / var16), var17)
      );
   }

   private static Color averageColor(Color... var0) {
      int var1 = 0;
      int var2 = 0;
      int var3 = 0;

      for (Color var7 : var0) {
         var1 += var7.getRed();
         var2 += var7.getGreen();
         var3 += var7.getBlue();
      }

      int var8 = Math.max(1, var0.length);
      return new Color(var1 / var8, var2 / var8, var3 / var8);
   }

   private static void resetBlend() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void addWindow(MatrixStack var0, float var1, float var2, float var3, float var4, double var5) {
      double var7 = Math.max(0.0, Math.min(1.0025, var5));
      float var9 = var2 + var4;
      float var10 = (float)(var9 * (1.0 - var7));
      float var11 = var2 + var10;
      float var12 = var3;
      float var13 = var4 - var10;
      if (var3 < var1) {
         var12 = var1;
      }

      if (var13 < var11) {
         var13 = var11;
      }

      addWindow(var0, new TRect(var1, var11, var12, var13));
   }

   public static void addWindow(MatrixStack var0, TRect var1) {
      Matrix4f var2 = var0.peek().getPositionMatrix();
      Vector4f var3 = new Vector4f(var1.x1, var1.y1, 0.0F, 1.0F);
      Vector4f var4 = new Vector4f(var1.x2, var1.y2, 0.0F, 1.0F);
      var3.mulTranspose(var2);
      var4.mulTranspose(var2);
      float var5 = var3.x();
      float var6 = var3.y();
      float var7 = var4.x();
      float var8 = var4.y();
      TRect var9 = new TRect(var5, var6, var7, var8);
      if (clipStack.empty()) {
         clipStack.push(var9);
         beginScissor(var9.x1, var9.y1, var9.x2, var9.y2);
      } else {
         TRect var10 = clipStack.peek();
         float var11 = var10.x1;
         float var12 = var10.y1;
         float var13 = var10.x2;
         float var14 = var10.y2;
         float var15 = MathHelper.clamp(var9.x1, var11, var13);
         float var16 = MathHelper.clamp(var9.y1, var12, var14);
         float var17 = MathHelper.clamp(var9.x2, var15, var13);
         float var18 = MathHelper.clamp(var9.y2, var16, var14);
         clipStack.push(new TRect(var15, var16, var17, var18));
         beginScissor(var15, var16, var17, var18);
      }
   }

   public static void popWindow() {
      if (!clipStack.empty()) {
         clipStack.pop();
         if (clipStack.empty()) {
            endScissor();
         } else {
            TRect var0 = clipStack.peek();
            beginScissor(var0.x1, var0.y1, var0.x2, var0.y2);
         }
      }
   }

   private static void beginScissor(double var0, double var2, double var4, double var6) {
      double var8 = Math.max(0.0, var4 - var0);
      double var10 = Math.max(0.0, var6 - var2);
      float var12 = scaleFactor();
      int var13 = (int)((windowHeight() - (var2 + var10)) * var12);
      int var14 = (int)(var0 * var12);
      int var15 = (int)(var8 * var12);
      int var16 = (int)(var10 * var12);
      RenderSystem.enableScissor(var14, var13, var15, var16);
   }

   private static void endScissor() {
      RenderSystem.disableScissor();
   }

   private static float scaleFactor() {
      try {
         return (float)MinecraftClient.getInstance().getWindow().getScaleFactor();
      } catch (Throwable var1) {
         return 1.0F;
      }
   }

   private static double windowHeight() {
      try {
         return MinecraftClient.getInstance().getWindow().getScaledHeight();
      } catch (Throwable var1) {
         return 0.0;
      }
   }

   public static void drawBlurredShadow(MatrixStack var0, float var1, float var2, float var3, float var4, int var5, Color var6) {
      if (var5 > 0 && !(var3 <= 0.0F) && !(var4 <= 0.0F)) {
         var1 -= var5;
         var2 -= var5;
         if (SHADOW_CACHE.size() > 192) {
            for (TDraw.TShadow var8 : SHADOW_CACHE.values()) {
               var8.release();
            }

            SHADOW_CACHE.clear();
         }

         int var16;
         if (!SHADOW_CACHE.containsKey(var16 = (int)((var3 = var3 + var5 * 2) * (var4 = var4 + var5 * 2) + var3 * var5))) {
            try {
               BufferedImage var17 = new BufferedImage((int)var3, (int)var4, 2);
               Graphics var9 = var17.getGraphics();
               var9.setColor(new Color(-1));
               var9.fillRect(var5, var5, (int)(var3 - var5 * 2), (int)(var4 - var5 * 2));
               var9.dispose();
               BufferedImage var10 = new TGaussianFilter(var5).filter(var17, null);
               SHADOW_CACHE.put(var16, new TDraw.TShadow(var10));
            } catch (Throwable var11) {
            }
         } else {
            TDraw.TShadow var18 = SHADOW_CACHE.get(var16);
            if (var18 != null) {
               var18.bind();
               resetBlend();
               RenderSystem.setShaderColor(var6.getRed() / 255.0F, var6.getGreen() / 255.0F, var6.getBlue() / 255.0F, var6.getAlpha() / 255.0F * globalAlpha);
               renderTexture(var0, var1, var2, var3, var4, 0.0F, 0.0F, var3, var4, var3, var4);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.disableBlend();
            }
         }
      }
   }

   public static void sizeAnimation(MatrixStack var0, double var1, double var3, double var5) {
      var0.push();
      var0.translate((float)var1, (float)var3, 0.0F);
      var0.scale((float)var5, (float)var5, 1.0F);
      var0.translate((float)(-var1), (float)(-var3), 0.0F);
   }

   public static void renderTexture(
      MatrixStack var0, double var1, double var3, double var5, double var7, float var9, float var10, double var11, double var13, double var15, double var17
   ) {
      double var19 = var1 + var5;
      double var21 = var3 + var7;
      double var23 = 0.0;
      Matrix4f var25 = var0.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder var26 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      var26.vertex(var25, (float)var1, (float)var21, (float)var23).texture(var9 / (float)var15, (var10 + (float)var13) / (float)var17);
      var26.vertex(var25, (float)var19, (float)var21, (float)var23).texture((var9 + (float)var11) / (float)var15, (var10 + (float)var13) / (float)var17);
      var26.vertex(var25, (float)var19, (float)var3, (float)var23).texture((var9 + (float)var11) / (float)var15, var10 / (float)var17);
      var26.vertex(var25, (float)var1, (float)var3, (float)var23).texture(var9 / (float)var15, (var10 + 0.0F) / (float)var17);
      BufferRenderer.drawWithGlobalProgram(var26.end());
   }

   public static void renderGradientTexture(
      MatrixStack var0,
      double var1,
      double var3,
      double var5,
      double var7,
      float var9,
      float var10,
      double var11,
      double var13,
      double var15,
      double var17,
      Color var19,
      Color var20,
      Color var21,
      Color var22
   ) {
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      BufferBuilder var23 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      renderGradientTextureInternal(var23, var0, var1, var3, var5, var7, var9, var10, var11, var13, var15, var17, var19, var20, var21, var22);
      BufferRenderer.drawWithGlobalProgram(var23.end());
   }

   public static void renderGradientTextureInternal(
      BufferBuilder var0,
      MatrixStack var1,
      double var2,
      double var4,
      double var6,
      double var8,
      float var10,
      float var11,
      double var12,
      double var14,
      double var16,
      double var18,
      Color var20,
      Color var21,
      Color var22,
      Color var23
   ) {
      double var24 = var2 + var6;
      double var26 = var4 + var8;
      double var28 = 0.0;
      Matrix4f var30 = var1.peek().getPositionMatrix();
      var0.vertex(var30, (float)var2, (float)var26, (float)var28).texture(var10 / (float)var16, (var11 + (float)var14) / (float)var18).color(var20.getRGB());
      var0.vertex(var30, (float)var24, (float)var26, (float)var28)
         .texture((var10 + (float)var12) / (float)var16, (var11 + (float)var14) / (float)var18)
         .color(var21.getRGB());
      var0.vertex(var30, (float)var24, (float)var4, (float)var28).texture((var10 + (float)var12) / (float)var16, var11 / (float)var18).color(var22.getRGB());
      var0.vertex(var30, (float)var2, (float)var4, (float)var28).texture(var10 / (float)var16, (var11 + 0.0F) / (float)var18).color(var23.getRGB());
   }

   private static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);
   }

   private static void endRender() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static final class TShadow {
      private final Identifier id = Identifier.of("bullwwclient", "shadow_" + System.nanoTime());

      TShadow(BufferedImage var1) {
         byte[] var2;
         try {
            ByteArrayOutputStream var3 = new ByteArrayOutputStream();
            ImageIO.write(var1, "png", var3);
            var2 = var3.toByteArray();
         } catch (Throwable var7) {
            return;
         }

         try {
            ByteBuffer var8 = BufferUtils.createByteBuffer(var2.length).put(var2);
            var8.flip();
            NativeImageBackedTexture var4 = new NativeImageBackedTexture(NativeImage.read(var8));
            MinecraftClient var5 = MinecraftClient.getInstance();
            var5.execute(() -> var5.getTextureManager().registerTexture(this.id, var4));
         } catch (Throwable var6) {
         }
      }

      void bind() {
         RenderSystem.setShaderTexture(0, this.id);
      }

      void release() {
         try {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(this.id);
         } catch (Throwable var2) {
         }
      }
   }
}
