package dev.luminous.api.live2d;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.glow.GLProbe;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class CubismNativeRenderer {
   private final Identifier[] textures;
   private Set frontBangDrawables = Set.of();
   private Set hiddenDrawables = Set.of();
   private boolean flipV;
   private boolean skipMaskedDrawables;
   private boolean frontBangsFix = true;
   private boolean debugOrder;
   private boolean debugOrderLogged;

   public CubismNativeRenderer(Identifier[] var1) {
      this.textures = var1;
   }

   public void setFlipV(boolean var1) {
      this.flipV = var1;
   }

   public void setSkipMaskedDrawables(boolean var1) {
      this.skipMaskedDrawables = var1;
   }

   public void setFrontBangsFix(boolean var1) {
      this.frontBangsFix = var1;
   }

   public void setFrontBangDrawables(Set var1) {
      this.frontBangDrawables = Set.copyOf(var1);
   }

   public void setHiddenDrawables(Set var1) {
      this.hiddenDrawables = Set.copyOf(var1);
   }

   public void preloadTextures(MinecraftClient var1) {
      for (Identifier var5 : this.textures) {
         var1.getTextureManager().bindTexture(var5);
      }
   }

   public void setDebugOrder(boolean var1) {
      if (var1 && !this.debugOrder) {
         this.debugOrderLogged = false;
      }

      this.debugOrder = var1;
   }

   public void render(DrawContext var1, CubismNativeModel var2, float var3, float var4, float var5, float var6, float var7) {
      var2.beginFrameCache();
      int var8 = var2.getDrawableCount();
      Integer[] var9 = new Integer[var8];

      for (int var10 = 0; var10 < var8; var10++) {
         var9[var10] = var10;
      }

      Arrays.sort(var9, Comparator.comparingInt(var2::getDrawableRenderOrder).thenComparingInt(var2::getDrawableDrawOrder));
      this.dumpOrderOnce(var2, var9);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      float var21 = var3;
      float var22 = var4;
      Integer[] var14 = var9;
      int var11 = var9.length;

      for (int var12 = 0; var12 < var11; var12++) {
         int var13 = var14[var12];
         if (!this.isHiddenDrawable(var2, var13) && (!this.frontBangsFix || !this.isFrontBangOverlay(var2, var13))) {
            this.renderDrawable(var1, var2, var13, var21, var22, var5, var6, 0.0F, var7);
         }
      }

      if (this.frontBangsFix) {
         var14 = var9;
         var11 = var9.length;

         for (int var23 = 0; var23 < var11; var23++) {
            int var25 = var14[var23];
            if (!this.isHiddenDrawable(var2, var25) && this.isFrontBangOverlay(var2, var25)) {
               this.renderDrawable(var1, var2, var25, var21, var22, var5, var6, 0.0F, var7);
            }
         }
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      GLProbe.resyncGlState("live2d");
   }

   private boolean isHiddenDrawable(CubismNativeModel var1, int var2) {
      return !this.hiddenDrawables.isEmpty() && this.hiddenDrawables.contains(var1.getDrawableId(var2));
   }

   private boolean isFrontBangOverlay(CubismNativeModel var1, int var2) {
      return !this.frontBangDrawables.isEmpty() && this.frontBangDrawables.contains(var1.getDrawableId(var2));
   }

   private void dumpOrderOnce(CubismNativeModel var1, Integer[] var2) {
      if (this.debugOrder && !this.debugOrderLogged) {
         this.debugOrderLogged = true;
         StringBuilder var3 = new StringBuilder("[CubismNativeRenderer] order: ");

         for (int var4 : var2) {
            var3.append(var1.getDrawableId(var4))
               .append('(')
               .append(var1.getDrawableRenderOrder(var4))
               .append('/')
               .append(var1.getDrawableDrawOrder(var4))
               .append(") ");
         }

         System.out.println(var3);
      }
   }

   private void renderDrawable(
      DrawContext var1, CubismNativeModel var2, int var3, float var4, float var5, float var6, float var7, float var8, float var9
   ) {
      if (var2.isDrawableVisible(var3) && (!this.skipMaskedDrawables || var2.getDrawableMaskCount(var3) <= 0)) {
         int var10 = var2.getDrawableTextureIndex(var3);
         if (var10 >= 0 && var10 < this.textures.length) {
            float var11 = var2.getDrawableOpacity(var3) * var9;
            if (!(var11 <= 0.001F)) {
               float[] var12 = var2.getDrawableVertexPositions(var3);
               float[] var13 = var2.getDrawableVertexUvs(var3);
               short[] var14 = var2.getDrawableIndices(var3);
               if (var12.length != 0 && var13.length != 0 && var14.length != 0) {
                  RenderSystem.setShaderTexture(0, this.textures[var10]);
                  if (var2.isDrawableAdditive(var3)) {
                     RenderSystem.blendFuncSeparate(770, 1, 770, 1);
                  } else if (var2.isDrawableMultiplicative(var3)) {
                     RenderSystem.blendFuncSeparate(774, 771, 774, 771);
                  } else {
                     RenderSystem.blendFuncSeparate(770, 771, 770, 771);
                  }

                  Matrix4f var15 = var1.getMatrices().peek().getPositionMatrix();
                  BufferBuilder var16 = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
                  float var17 = var2.getBoundsWidth();
                  float var18 = var2.getBoundsHeight();
                  float var19 = Math.min(var6 / var17, var7 / var18);
                  float var20 = var17 * var19;
                  float var21 = var18 * var19;
                  float var22 = var4 + (var6 - var20) * 0.5F;
                  float var23 = var5 + (var7 - var21) * 0.5F;

                  for (short var25 : var14) {
                     int var26 = var25 & '\uffff';
                     int var27 = var26 * 2;
                     if (var27 + 1 < var12.length && var27 + 1 < var13.length) {
                        float var28 = var22 + (var12[var27] - var2.getBoundsMinX()) * var19;
                        float var29 = var23 + (var2.getBoundsMaxY() - var12[var27 + 1]) * var19;
                        float var30 = var13[var27];
                        float var31 = this.flipV ? 1.0F - var13[var27 + 1] : var13[var27 + 1];
                        var16.vertex(var15, var28, var29, 0.0F).texture(var30, var31).color(1.0F, 1.0F, 1.0F, var11);
                     }
                  }

                  BufferRenderer.drawWithGlobalProgram(var16.end());
                  RenderSystem.defaultBlendFunc();
               }
            }
         }
      }
   }
}

