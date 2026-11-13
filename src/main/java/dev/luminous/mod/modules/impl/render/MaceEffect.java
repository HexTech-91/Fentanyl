package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class MaceEffect extends Module {
   public static MaceEffect INSTANCE;
   private static final List<MaceEffect.Shockwave> SHOCKWAVES = new ArrayList<>();
   private final SliderSetting duration = this.add(new SliderSetting("Duration", 450, 100, 1500));
   private final SliderSetting radius = this.add(new SliderSetting("Radius", 3.2, 0.5, 8.0, 0.1));
   private final SliderSetting thickness = this.add(new SliderSetting("Thickness", 0.35, 0.05, 1.5, 0.01));
   private final SliderSetting height = this.add(new SliderSetting("Height", 0.06, 0.0, 1.0, 0.01));
   private final SliderSetting opacity = this.add(new SliderSetting("Opacity", 180, 0, 255));
   private final SliderSetting segments = this.add(new SliderSetting("Segments", 50, 12, 120));
   private final BooleanSetting pulse = this.add(new BooleanSetting("Pulse", true));
   private final BooleanSetting doubleWave = this.add(new BooleanSetting("DoubleWave", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 180)));

   public MaceEffect() {
      super("MaceEffect", Module.Category.Render);
      this.setChinese("\u91cd\u9524\u7279\u6548");
      this.duration.setChinese("\u6301\u7eed\u65f6\u95f4");
      this.radius.setChinese("\u534a\u5f84");
      this.thickness.setChinese("\u539a\u5ea6");
      this.height.setChinese("\u9ad8\u5ea6");
      this.opacity.setChinese("\u900f\u660e\u5ea6");
      this.segments.setChinese("\u5206\u6bb5\u6570");
      this.pulse.setChinese("\u8109\u51b2");
      this.doubleWave.setChinese("\u53cc\u91cd\u6ce2\u6d6a");
      this.color.setChinese("\u989c\u8272");
      INSTANCE = this;
   }

   public static void addShockwave(Vec3d pos) {
      if (INSTANCE != null && !INSTANCE.isOff() && !Module.nullCheck()) {
         synchronized (SHOCKWAVES) {
            SHOCKWAVES.add(new MaceEffect.Shockwave(pos, 0.55));
            if (INSTANCE.doubleWave.getValue()) {
               SHOCKWAVES.add(new MaceEffect.Shockwave(pos, 1.0));
            }
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!Module.nullCheck()
         && !event.isCancelled()
         && mc.player.getMainHandStack().isOf(Items.MACE)
         && event.getPacket() instanceof PlayerInteractEntityC2SPacket var3
         && Criticals.getInteractType(var3) == InteractType.ATTACK) {
         Entity var4 = Criticals.getEntity(var3);
         if (var4 instanceof PlayerEntity && this.isFallingMaceSmash()) {
            addShockwave(var4.getPos());
         }
      }
   }

   private boolean isFallingMaceSmash() {
      return mc.player.fallDistance > 1.5F
         && !mc.player.isOnGround()
         && !mc.player.getAbilities().flying
         && !mc.player.isFallFlying()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.hasVehicle();
   }

   @Override
   public void onDisable() {
      synchronized (SHOCKWAVES) {
         SHOCKWAVES.clear();
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!Module.nullCheck()) {
         long var2 = System.currentTimeMillis();
         ArrayList<MaceEffect.Shockwave> var5;
         synchronized (SHOCKWAVES) {
            if (SHOCKWAVES.isEmpty()) {
               return;
            }

            var5 = new ArrayList<>(SHOCKWAVES);
         }

         for (MaceEffect.Shockwave var6 : var5) {
            float var7 = (float)(var2 - var6.startTime) / this.duration.getValueFloat();
            if (var7 >= 1.0F) {
               synchronized (SHOCKWAVES) {
                  SHOCKWAVES.remove(var6);
               }
            } else {
               this.renderShockwave(matrixStack, var6, MathHelper.clamp(var7, 0.0F, 1.0F));
            }
         }
      }
   }

   private void renderShockwave(MatrixStack matrixStack, MaceEffect.Shockwave sw, float progress) {
      float var4 = 1.0F - (1.0F - progress) * (1.0F - progress);
      float var5 = this.radius.getValueFloat() * sw.scale * var4;
      float var6 = Math.max(0.03F, this.thickness.getValueFloat() * (1.0F - progress * 0.65F));
      float var7 = Math.max(0.0F, var5 - var6);
      int var8 = Math.max(0, Math.min(this.opacity.getValueInt(), this.color.getValue().getAlpha()));
      int var9 = (int)(var8 * (1.0F - progress));
      if (var9 > 0 && !(var5 <= 0.0F)) {
         Color var10 = this.color.getValue();
         if (this.pulse.getValue()) {
            var10 = ColorUtil.pulseColor(var10, (int)(progress * 15.0F), 10, 1.0);
         }

         var10 = ColorUtil.injectAlpha(var10, var9);
         double var11 = sw.pos.y + this.height.getValue();
         double var13 = mc.getEntityRenderDispatcher().camera.getPos().getX();
         double var15 = mc.getEntityRenderDispatcher().camera.getPos().getY();
         double var17 = mc.getEntityRenderDispatcher().camera.getPos().getZ();
         matrixStack.push();
         matrixStack.translate(sw.pos.x - var13, var11 - var15, sw.pos.z - var17);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var19 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         Matrix4f var20 = matrixStack.peek().getPositionMatrix();
         int var21 = Math.max(12, this.segments.getValueInt());

         for (int var22 = 0; var22 <= var21; var22++) {
            double var23 = (Math.PI * 2) * var22 / var21;
            float var25 = (float)Math.cos(var23);
            float var26 = (float)Math.sin(var23);
            var19.vertex(var20, var25 * var5, 0.0F, var26 * var5).color(var10.getRGB());
            var19.vertex(var20, var25 * var7, 0.0F, var26 * var7).color(ColorUtil.injectAlpha(var10, Math.max(0, var9 / 4)).getRGB());
         }

         Render3DUtil.endBuilding(var19);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         matrixStack.pop();
      }
   }

   private static class Shockwave {
      final Vec3d pos;
      final long startTime;
      final float scale;

      Shockwave(Vec3d pos, double scale) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();
         this.scale = (float)scale;
      }
   }
}
