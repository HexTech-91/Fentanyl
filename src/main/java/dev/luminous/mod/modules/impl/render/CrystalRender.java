package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class CrystalRender extends Module {
   public static CrystalRender INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 16.0, 4.0, 64.0, 1.0));
   private final BooleanSetting core = this.add(new BooleanSetting("Core", true)).setParent();
   private final BooleanSetting frame = this.add(new BooleanSetting("Frame", true, this.core::getValue));
   private final EnumSetting<Mode> frameMode = this.add(new EnumSetting<>("FrameMode", Mode.Box, this.core::getValue));
   private final ColorSetting coreColor = this.add(new ColorSetting("CoreColor", new Color(255, 85, 255, 130), this.core::getValue));
   private final ColorSetting frameColor = this.add(new ColorSetting("FrameColor", new Color(255, 255, 255, 190), this.core::getValue));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.2, 3.0, 0.05));
   private final SliderSetting height = this.add(new SliderSetting("Height", 1.0, 0.0, 3.0, 0.05));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.0, -1.0, 2.0, 0.05));
   private final SliderSetting rotateSpeed = this.add(new SliderSetting("RotateSpeed", 45.0, 0.0, 360.0, 5.0));
   private final BooleanSetting pulse = this.add(new BooleanSetting("Pulse", true));
   private final BooleanSetting glow = this.add(new BooleanSetting("Glow", true));

   public CrystalRender() {
      super("CrystalRender", "\u6c34\u6676\u6e32\u67d3", Module.Category.Render);
      this.range.setChinese("\u8303\u56f4");
      this.core.setChinese("\u6838\u5fc3");
      this.frame.setChinese("\u6846\u67b6");
      this.frameMode.setChinese("\u6846\u67b6\u6a21\u5f0f");
      this.coreColor.setChinese("\u6838\u5fc3\u989c\u8272");
      this.frameColor.setChinese("\u6846\u67b6\u989c\u8272");
      this.size.setChinese("\u5927\u5c0f");
      this.height.setChinese("\u9ad8\u5ea6");
      this.yOffset.setChinese("Y\u504f\u79fb");
      this.rotateSpeed.setChinese("\u65cb\u8f6c\u901f\u5ea6");
      this.pulse.setChinese("\u547c\u5438");
      this.glow.setChinese("\u5149\u6655");
      this.setChinese("\u6c34\u6676\u6e32\u67d3");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (nullCheck()) {
         return;
      }
      List<EndCrystalEntity> crystals = mc.world
         .getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(this.range.getValue()), Entity::isAlive);
      if (crystals.isEmpty()) {
         return;
      }
      long time = System.currentTimeMillis() - Alien.initTime;
      float tickDelta = Render3DUtil.getTickDelta();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      for (EndCrystalEntity crystal : crystals) {
         double x = MathUtil.interpolate(crystal.lastRenderX, crystal.getX(), tickDelta);
         double y = MathUtil.interpolate(crystal.lastRenderY, crystal.getY(), tickDelta);
         double z = MathUtil.interpolate(crystal.lastRenderZ, crystal.getZ(), tickDelta);
         double wobble = this.pulse.getValue() ? 0.1 * Math.sin(time / 220.0) : 0.0;
         double radiusXZ = Math.max(0.05, 0.55 * this.size.getValue() + wobble);
         double radiusY = Math.max(0.05, 0.55 * this.size.getValue() * this.height.getValue() + wobble);
         float rotation = (float)(time / 1000.0 * this.rotateSpeed.getValue() % 360.0) * (float) (Math.PI / 180.0);
         Vec3d center = new Vec3d(x, y + this.yOffset.getValue(), z);
         if (this.core.getValue()) {
            this.drawCore(center, radiusXZ, radiusY, rotation, this.coreColor.getValue());
         }

         if (this.frame.getValue()) {
            this.drawFrame(center, radiusXZ, radiusY, rotation, this.frameColor.getValue());
         }
      }

      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private void drawCore(Vec3d center, double radiusXZ, double radiusY, float rotation, Color color) {
      Color inner = ColorUtil.injectAlpha(color, Math.max(10, (int)(color.getAlpha() * 0.15F)));
      MatrixStack matrices = Render3DUtil.matrixFrom(center.x, center.y, center.z);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      byte segments = 12;

      for (int side = 0; side < 2; side++) {
         float dir = side == 0 ? 1.0F : -1.0F;
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(color.getRGB());

         for (int i = 0; i <= segments; i++) {
            double angle = rotation + i * (Math.PI * 2) / segments;
            float px = (float)(Math.cos(angle) * radiusXZ);
            float pz = (float)(Math.sin(angle) * radiusXZ);
            buffer.vertex(matrix, px, (float)(radiusY * dir), pz).color(inner.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      if (this.glow.getValue()) {
         Color glowColor = ColorUtil.injectAlpha(color, Math.max(8, (int)(color.getAlpha() * 0.35F)));
         Render3DUtil.drawFill(
            matrices,
            new Box(-radiusXZ * 1.35, -radiusY * 0.35, -radiusXZ * 1.35, radiusXZ * 1.35, radiusY * 0.35, radiusXZ * 1.35),
            glowColor
         );
      }
   }

   private void drawFrame(Vec3d center, double radiusXZ, double radiusY, float rotation, Color color) {
      if (this.frameMode.is(Mode.Box)) {
         Box box = new Box(
            center.x - radiusXZ, center.y - radiusY, center.z - radiusXZ, center.x + radiusXZ, center.y + radiusY, center.z + radiusXZ
         );
         Render3DUtil.drawBox(new MatrixStack(), box, color, 2.0F);
      } else {
         this.drawCage(center, radiusXZ, radiusY, rotation, color);
      }
   }

   private void drawCage(Vec3d center, double radiusXZ, double radiusY, float rotation, Color color) {
      MatrixStack matrices = Render3DUtil.matrixFrom(center.x, center.y, center.z);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.lineWidth(2.0F);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < 4; i++) {
         double angle = rotation + i * (Math.PI * 2) / 4;
         float px = (float)(Math.cos(angle) * radiusXZ);
         float pz = (float)(Math.sin(angle) * radiusXZ);
         buffer.vertex(matrix, px, (float)-radiusY, pz).color(color.getRGB());
         buffer.vertex(matrix, px, (float)radiusY, pz).color(color.getRGB());
      }

      float prevX = 0.0F;
      float prevZ = 0.0F;
      int segments = 24;

      for (int i = 0; i <= segments; i++) {
         double angle = i * (Math.PI * 2) / segments;
         float px = (float)(Math.cos(angle) * radiusXZ);
         float pz = (float)(Math.sin(angle) * radiusXZ);
         if (i > 0) {
            buffer.vertex(matrix, prevX, (float)radiusY, prevZ).color(color.getRGB());
            buffer.vertex(matrix, px, (float)radiusY, pz).color(color.getRGB());
            buffer.vertex(matrix, prevX, (float)-radiusY, prevZ).color(color.getRGB());
            buffer.vertex(matrix, px, (float)-radiusY, pz).color(color.getRGB());
         }

         prevX = px;
         prevZ = pz;
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.lineWidth(1.0F);
   }

   public enum Mode {
      Box,
      Cage
   }
}
