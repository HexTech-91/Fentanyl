package dev.luminous.api.utils.skia.context;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public class SkiaContext {
   private static DirectContext context;
   private static Surface surface;
   private static BackendRenderTarget renderTarget;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static int lastFramebufferId = -1;
   private static Boolean skijaAvailable = null;

   public static boolean isSkijaAvailable() {
      if (skijaAvailable == null) {
         try {
            Class.forName("io.github.humbleui.skija.DirectContext");
            skijaAvailable = true;
         } catch (Throwable var1) {
            skijaAvailable = false;
            System.err.println("[Kimi] Skija native library is not available on this platform. Skia-based rendering will be disabled.");
            var1.printStackTrace();
         }
      }

      return skijaAvailable;
   }

   public static Canvas getCanvas() {
      return surface != null ? surface.getCanvas() : null;
   }

   public static DirectContext getContext() {
      return context;
   }

   public static void createSurface(int width, int height) {
      if (width > 0 && height > 0) {
         if (isSkijaAvailable()) {
            int framebufferId;
            try {
               framebufferId = MinecraftClient.getInstance().getFramebuffer().fbo;
            } catch (Throwable var7) {
               return;
            }

            if (surface == null || renderTarget == null || width != lastWidth || height != lastHeight || framebufferId != lastFramebufferId) {
               if (context == null) {
                  try {
                     context = DirectContext.makeGL();
                  } catch (Throwable var6) {
                     skijaAvailable = false;
                     System.err.println("[Kimi] Failed to create Skija DirectContext. Skia-based rendering will be disabled.");
                     var6.printStackTrace();
                     return;
                  }
               }

               closeSurface();

               try {
                  int sampleCount = Math.max(0, GL11.glGetInteger(32937));
                  int stencilBits = Math.max(0, GL11.glGetInteger(3415));
                  if (stencilBits == 0) {
                     stencilBits = 8;
                  }

                  renderTarget = BackendRenderTarget.makeGL(width, height, sampleCount, stencilBits, framebufferId, 32856);
                  surface = Surface.wrapBackendRenderTarget(
                     context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB()
                  );
                  if (surface == null) {
                     closeSurface();
                     return;
                  }

                  lastWidth = width;
                  lastHeight = height;
                  lastFramebufferId = framebufferId;
               } catch (Throwable var5) {
                  closeSurface();
               }
            }
         }
      }
   }

   public static void draw(Consumer<Canvas> drawingLogic) {
      if (isSkijaAvailable() && context != null && surface != null && drawingLogic != null) {
         int previousActiveTexture = GL11.glGetInteger(34016);
         int previousTextureBinding = GL11.glGetInteger(32873);
         int previousSamplerBinding = GL11.glGetInteger(35097);
         int previousProgram = GL11.glGetInteger(35725);
         int previousArrayBuffer = GL11.glGetInteger(34964);
         int previousElementArrayBuffer = GL11.glGetInteger(34965);
         int previousVertexArray = GL11.glGetInteger(34229);
         int previousFramebufferBinding = GL11.glGetInteger(36006);
         int previousDrawFramebufferBinding = GL11.glGetInteger(36006);
         int previousReadFramebufferBinding = GL11.glGetInteger(36010);
         int previousRenderbufferBinding = GL11.glGetInteger(36007);
         int previousBlendSrcRgb = GL11.glGetInteger(32969);
         int previousBlendDstRgb = GL11.glGetInteger(32968);
         int previousBlendSrcAlpha = GL11.glGetInteger(32971);
         int previousBlendDstAlpha = GL11.glGetInteger(32970);
         int previousBlendEqRgb = GL11.glGetInteger(32777);
         int previousBlendEqAlpha = GL11.glGetInteger(34877);
         int previousDepthFunc = GL11.glGetInteger(2932);
         int previousCullFace = GL11.glGetInteger(2885);
         int previousFrontFace = GL11.glGetInteger(2886);
         boolean blendEnabled = GL11.glIsEnabled(3042);
         boolean depthEnabled = GL11.glIsEnabled(2929);
         boolean cullEnabled = GL11.glIsEnabled(2884);
         boolean scissorEnabled = GL11.glIsEnabled(3089);
         boolean stencilEnabled = GL11.glIsEnabled(2960);
         boolean depthMask = GL11.glGetBoolean(2930);
         ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
         GL11.glGetBooleanv(3107, colorMask);
         boolean colorMaskRed = colorMask.get(0) != 0;
         boolean colorMaskGreen = colorMask.get(1) != 0;
         boolean colorMaskBlue = colorMask.get(2) != 0;
         boolean colorMaskAlpha = colorMask.get(3) != 0;
         IntBuffer previousViewport = BufferUtils.createIntBuffer(4);
         IntBuffer previousScissorBox = BufferUtils.createIntBuffer(4);
         GL11.glGetIntegerv(2978, previousViewport);
         GL11.glGetIntegerv(3088, previousScissorBox);
         int unpackRowLength = GL11.glGetInteger(3314);
         int unpackSkipPixels = GL11.glGetInteger(3316);
         int unpackSkipRows = GL11.glGetInteger(3315);
         int unpackAlignment = GL11.glGetInteger(3317);
         RenderSystem.pixelStore(3314, 0);
         RenderSystem.pixelStore(3316, 0);
         RenderSystem.pixelStore(3315, 0);
         RenderSystem.pixelStore(3317, 4);
         context.resetGLAll();

         try {
            Canvas canvas = getCanvas();
            if (canvas != null) {
               float scaleFactor = (float)MinecraftClient.getInstance().getWindow().getScaleFactor();
               canvas.save();
               canvas.scale(scaleFactor, scaleFactor);
               drawingLogic.accept(canvas);
               canvas.restore();
               context.flush();
               context.resetGLAll();
               return;
            }
         } finally {
            RenderSystem.pixelStore(3314, unpackRowLength);
            RenderSystem.pixelStore(3316, unpackSkipPixels);
            RenderSystem.pixelStore(3315, unpackSkipRows);
            RenderSystem.pixelStore(3317, unpackAlignment);
            if (blendEnabled) {
               RenderSystem.enableBlend();
            } else {
               RenderSystem.disableBlend();
            }

            GL14.glBlendFuncSeparate(previousBlendSrcRgb, previousBlendDstRgb, previousBlendSrcAlpha, previousBlendDstAlpha);
            GL20.glBlendEquationSeparate(previousBlendEqRgb, previousBlendEqAlpha);
            if (depthEnabled) {
               RenderSystem.enableDepthTest();
            } else {
               RenderSystem.disableDepthTest();
            }

            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(depthMask);
            if (cullEnabled) {
               RenderSystem.enableCull();
            } else {
               RenderSystem.disableCull();
            }

            GL11.glCullFace(previousCullFace);
            GL11.glFrontFace(previousFrontFace);
            if (scissorEnabled) {
               GL11.glEnable(3089);
               GL11.glScissor(previousScissorBox.get(0), previousScissorBox.get(1), previousScissorBox.get(2), previousScissorBox.get(3));
            } else {
               GL11.glDisable(3089);
            }

            if (stencilEnabled) {
               GL11.glEnable(2960);
            } else {
               GL11.glDisable(2960);
            }

            GL13.glActiveTexture(previousActiveTexture);
            RenderSystem.activeTexture(previousActiveTexture);
            GL11.glBindTexture(3553, previousTextureBinding);
            GL33.glBindSampler(0, previousSamplerBinding);
            GL20.glUseProgram(previousProgram);
            GL15.glBindBuffer(34962, previousArrayBuffer);
            GL15.glBindBuffer(34963, previousElementArrayBuffer);
            GL30.glBindVertexArray(previousVertexArray);
            GL30.glBindFramebuffer(36160, previousFramebufferBinding);
            GL30.glBindFramebuffer(36009, previousDrawFramebufferBinding);
            GL30.glBindFramebuffer(36008, previousReadFramebufferBinding);
            GL30.glBindRenderbuffer(36161, previousRenderbufferBinding);
            GL11.glViewport(previousViewport.get(0), previousViewport.get(1), previousViewport.get(2), previousViewport.get(3));
            GL11.glColorMask(colorMaskRed, colorMaskGreen, colorMaskBlue, colorMaskAlpha);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static void closeSurface() {
      if (surface != null) {
         surface.close();
         surface = null;
      }

      if (renderTarget != null) {
         renderTarget.close();
         renderTarget = null;
      }

      lastWidth = -1;
      lastHeight = -1;
      lastFramebufferId = -1;
   }
}
