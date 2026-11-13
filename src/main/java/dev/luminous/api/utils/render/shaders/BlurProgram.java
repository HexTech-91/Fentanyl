package dev.luminous.api.utils.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedCoreShader;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.ladysnake.satin.api.managed.uniform.SamplerUniform;
import org.ladysnake.satin.api.managed.uniform.Uniform1f;
import org.ladysnake.satin.api.managed.uniform.Uniform2f;
import org.ladysnake.satin.api.managed.uniform.Uniform4f;
import org.lwjgl.opengl.GL30;

import static dev.luminous.api.utils.Wrapper.mc;

public class BlurProgram {
   private Uniform2f uSize;
   private Uniform2f uLocation;
   private Uniform1f radius;
   private Uniform2f inputResolution;
   private Uniform1f brightness;
   private Uniform1f quality;
   private Uniform4f color1;
   private SamplerUniform sampler;
   private Framebuffer input;

   public static final ManagedCoreShader BLUR = ShaderEffectManager.getInstance()
      .manageCoreShader(Identifier.of("alien", "blur"), VertexFormats.POSITION);

   public BlurProgram() {
      this.setup();
   }

   public static boolean isAvailable() {
      return BLUR.getProgram() != null;
   }

   public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
      if (!isAvailable()) {
         return;
      }
      if (this.input == null) {
         this.input = new SimpleFramebuffer(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false, MinecraftClient.IS_SYSTEM_MAC);
      }

      float i = (float)mc.getWindow().getScaleFactor();
      this.radius.set(r * i);
      this.uLocation.set(x * i, -y * i + mc.getWindow().getScaledHeight() * i - height * i);
      this.uSize.set(width * i, height * i);
      this.brightness.set(blurOpacity);
      this.quality.set(blurStrenth);
      this.color1.set(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 1.0F);
      this.sampler.set(this.input.getColorAttachment());
   }

   public void use() {
      if (!isAvailable()) {
         return;
      }
      Framebuffer buffer = MinecraftClient.getInstance().getFramebuffer();
      this.input.beginWrite(false);
      GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo);
      GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight, 0, 0, buffer.textureWidth, buffer.textureHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
      buffer.beginWrite(false);
      if (this.input != null && (this.input.textureWidth != mc.getWindow().getFramebufferWidth() || this.input.textureHeight != mc.getWindow().getFramebufferHeight())) {
         this.input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
      }

      this.inputResolution.set((float)buffer.textureWidth, (float)buffer.textureHeight);
      this.sampler.set(this.input.getColorAttachment());
      RenderSystem.setShader(BLUR::getProgram);
   }

   protected void setup() {
      this.inputResolution = BLUR.findUniform2f("InputResolution");
      this.brightness = BLUR.findUniform1f("Brightness");
      this.quality = BLUR.findUniform1f("Quality");
      this.color1 = BLUR.findUniform4f("color1");
      this.uSize = BLUR.findUniform2f("uSize");
      this.uLocation = BLUR.findUniform2f("uLocation");
      this.radius = BLUR.findUniform1f("radius");
      this.sampler = BLUR.findSampler("InputSampler");
   }
}
