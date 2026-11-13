package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.core.Manager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class JumpCircle extends Module {
   public static JumpCircle INSTANCE;
   private static final Identifier DEFAULT_CIRCLE = Identifier.of("alien", "textures/jumpcircle/circle.png");
   private static final Identifier PORTAL_BUBBLE = Identifier.of("alien", "textures/jumpcircle/hitbubble.png");
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final EnumSetting<JumpCircle.Mode> mode = this.add(new EnumSetting<>("Mode", JumpCircle.Mode.Default));
   private final BooleanSetting easeOut = this.add(new BooleanSetting("EaseOut", true));
   private final BooleanSetting onlySelf = this.add(new BooleanSetting("OnlySelf", false));
   private final List<JumpCircle.Circle> circles = new ArrayList<>();
   private final List<PlayerEntity> groundedCache = new ArrayList<>();
   private Identifier customTexture;

   public JumpCircle() {
      super("JumpCircle", Module.Category.Render);
      this.setChinese("\u8df3\u8dc3\u5706\u5708");
      this.color.setChinese("\u989c\u8272");
      this.mode.setChinese("\u6a21\u5f0f");
      this.easeOut.setChinese("\u7f13\u51fa\u52a8\u753b");
      this.onlySelf.setChinese("\u4ec5\u81ea\u5df1");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.circles.clear();
      this.groundedCache.clear();
      this.customTexture = this.loadCustomTexture();
   }

   @Override
   public void onDisable() {
      this.circles.clear();
      this.groundedCache.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         if (this.mode.is(JumpCircle.Mode.Custom) && this.customTexture == null) {
            this.customTexture = this.loadCustomTexture();
         }

         for (PlayerEntity var3 : mc.world.getPlayers()) {
            if (var3 != null && var3.isOnGround() && !this.groundedCache.contains(var3) && (var3 == mc.player || !this.onlySelf.getValue())) {
               this.groundedCache.add(var3);
            }
         }

         for (PlayerEntity var6 : new ArrayList<>(this.groundedCache)) {
            if (var6 == null || !var6.isOnGround()) {
               Vec3d var4 = new Vec3d(var6.getX(), (int)Math.floor(var6.getY()) + 0.001F, var6.getZ());
               this.circles.add(new JumpCircle.Circle(var4, new Timer()));
               this.groundedCache.remove(var6);
            }
         }

         this.circles.removeIf(var1x -> var1x.timer().passedMs(this.easeOut.getValue() ? 5000.0 : 6000.0));
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (!nullCheck() && !this.circles.isEmpty()) {
         Identifier var2 = this.currentTexture();
         if (var2 != null) {
            Collections.reverse(this.circles);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.setShaderTexture(0, var2);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            BufferBuilder var3 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Vec3d var4 = mc.getEntityRenderDispatcher().camera.getPos();

            for (JumpCircle.Circle var6 : this.circles) {
               float var7 = 1.0F - (float)var6.timer().getPassedTimeMs() / 6000.0F;
               float var8 = (float)var6.timer().getPassedTimeMs() * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F;
               float var9 = 1.0F - (float)Math.pow(1.0F - var8, 4.0);
               float var10 = var9 * 2.0F;
               var1.push();
               var1.translate(var6.pos().x - var4.getX(), var6.pos().y - var4.getY(), var6.pos().z - var4.getZ());
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var9 * 1000.0F));
               float var11 = this.color.getValue().getRed() / 255.0F;
               float var12 = this.color.getValue().getGreen() / 255.0F;
               float var13 = this.color.getValue().getBlue() / 255.0F;
               float var14 = Math.max(0.0F, Math.min(1.0F, var7));
               Matrix4f var15 = var1.peek().getPositionMatrix();
               var3.vertex(var15, -var9, -var9 + var10, 0.0F).texture(0.0F, 1.0F).color(var11, var12, var13, var14);
               var3.vertex(var15, -var9 + var10, -var9 + var10, 0.0F).texture(1.0F, 1.0F).color(var11, var12, var13, var14);
               var3.vertex(var15, -var9 + var10, -var9, 0.0F).texture(1.0F, 0.0F).color(var11, var12, var13, var14);
               var3.vertex(var15, -var9, -var9, 0.0F).texture(0.0F, 0.0F).color(var11, var12, var13, var14);
               var1.pop();
            }

            Render3DUtil.endBuilding(var3);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
            Collections.reverse(this.circles);
         }
      }
   }

   private Identifier currentTexture() {
      return switch ((JumpCircle.Mode)this.mode.getValue()) {
         case Default -> DEFAULT_CIRCLE;
         case Portal -> PORTAL_BUBBLE;
         case Custom -> (Identifier)Objects.requireNonNullElse(this.customTexture, DEFAULT_CIRCLE);
      };
   }

   private Identifier loadCustomTexture() {
      try {
         File var1 = new File(Manager.getFolder(), "images/circle.png");
         if (var1.exists() && var1.isFile()) {
            NativeImage var2;
            try (FileInputStream var3 = new FileInputStream(var1)) {
               var2 = NativeImage.read(var3);
            }

            Identifier var9 = Identifier.of("alien", "textures/jumpcircle/custom.png");
            mc.getTextureManager().registerTexture(var9, new NativeImageBackedTexture(var2));
            return var9;
         } else {
            return null;
         }
      } catch (Exception var8) {
         System.err.println("[JumpCircle] Failed to load custom circle image: " + var8.getMessage());
         return null;
      }
   }

   private record Circle(Vec3d pos, Timer timer) {
   }

   public static enum Mode {
      Default,
      Portal,
      Custom;
   }
}
