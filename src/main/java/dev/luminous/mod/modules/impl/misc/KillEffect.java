package dev.luminous.mod.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.skia.Skia;
import dev.luminous.api.utils.skia.context.SkiaContext;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.render.DeathFx;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import io.github.humbleui.skija.Canvas;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class KillEffect extends Module {
   private final EnumSetting<KillEffect.KillEffectMode> mode;
   private final BooleanSetting lightning;
   private final BooleanSetting levelUp;
   private final SliderSetting lMaxPitch;
   private final SliderSetting lMinPitch;
   private final BooleanSetting trident;
   private final SliderSetting tMaxPitch;
   private final SliderSetting tMinPitch;
   private final SliderSetting factor;
   private final SliderSetting duration;
   private final SliderSetting scale;
   private final BooleanSetting flash;
   private final List<KillEffect.DeathEffect> deathEffects = new ArrayList<>();
   private static final Identifier YUANQUAN = Identifier.of("alien", "textures/killeffect/yuanquan.png");
   private static final Identifier JIANTOU = Identifier.of("alien", "textures/killeffect/jiantou.png");
   private static final Color SKULL_BONE_COLOR = new Color(240, 238, 228, 255);
   private static final Color SKULL_SHADOW_COLOR = new Color(26, 24, 30, 255);
   private final BooleanSetting sigh;
   private final BooleanSetting skullEffect;
   private final SliderSetting skullSize;
   private final SliderSetting skullDuration;
   private final List<KillEffect.SkullEffect> skullEffects = new ArrayList<>();

   public KillEffect() {
      super("KillEffect", Module.Category.Misc);
      this.setChinese("\u51fb\u6740\u6548\u679c");
      this.mode = this.add(new EnumSetting<>("Mode", KillEffect.KillEffectMode.DEFAULT));
      this.mode.setChinese("\u6a21\u5f0f");
      this.lightning = this.add(new BooleanSetting("Lightning", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT));
      this.lightning.setChinese("\u95ea\u7535");
      this.levelUp = this.add(new BooleanSetting("LevelUp", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT).setParent());
      this.levelUp.setChinese("\u5347\u7ea7\u97f3\u6548");
      this.lMaxPitch = this.add(
         new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.levelUp.isOpen())
      );
      this.lMaxPitch.setChinese("\u5347\u7ea7\u6700\u9ad8\u97f3\u8c03");
      this.lMinPitch = this.add(
         new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.levelUp.isOpen())
      );
      this.lMinPitch.setChinese("\u5347\u7ea7\u6700\u4f4e\u97f3\u8c03");
      this.trident = this.add(new BooleanSetting("Trident", false, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT).setParent());
      this.trident.setChinese("\u4e09\u53c9\u621f\u97f3\u6548");
      this.tMaxPitch = this.add(
         new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.trident.isOpen())
      );
      this.tMaxPitch.setChinese("\u4e09\u53c9\u621f\u6700\u9ad8\u97f3\u8c03");
      this.tMinPitch = this.add(
         new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.trident.isOpen())
      );
      this.tMinPitch.setChinese("\u4e09\u53c9\u621f\u6700\u4f4e\u97f3\u8c03");
      this.factor = this.add(new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT));
      this.factor.setChinese("\u500d\u7387");
      this.duration = this.add(new SliderSetting("Duration", 3.0, 1.0, 10.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie));
      this.duration.setChinese("\u6301\u7eed\u65f6\u95f4");
      this.scale = this.add(new SliderSetting("Scale", 1.0, 0.1, 3.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie));
      this.scale.setChinese("\u5927\u5c0f");
      this.flash = this.add(new BooleanSetting("Flash", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie));
      this.flash.setChinese("\u95ea\u70c1");
      this.sigh = this.add(new BooleanSetting("Sigh", true));
      this.sigh.setChinese("\u53f9");
      this.skullEffect = this.add(new BooleanSetting("Skull", true));
      this.skullEffect.setChinese("\u9ab7\u9ac5");
      this.skullSize = this.add(new SliderSetting("SkullSize", 1.0, 0.3, 3.0, 0.1, () -> this.skullEffect.getValue()));
      this.skullSize.setChinese("\u9ab7\u9ac5\u5927\u5c0f");
      this.skullDuration = this.add(new SliderSetting("SkullDuration", 2.0, 0.5, 8.0, 0.5, () -> this.skullEffect.getValue()));
      this.skullDuration.setChinese("\u9ab7\u9ac5\u6301\u7eed\u65f6\u957f");
   }

   @EventListener
   public void onPlayerDeath(DeathEvent var1) {
      PlayerEntity var2;
      if (!nullCheck() && (var2 = var1.getPlayer()) != null) {
         if (var2 == mc.player) {
            return;
         }

         if (Alien.FRIEND != null && Alien.FRIEND.isFriend(var2)) {
            return;
         }

         if (this.sigh.getValue()) {
            MinecraftClient.getInstance()
               .getSoundManager()
               .play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of("minecraft", "alien.killeffect.sigh")), 1.0F));
         }

         if (this.skullEffect.getValue()) {
            this.skullEffects.add(new KillEffect.SkullEffect(var2.getX(), var2.getY() + 1.0, var2.getZ(), System.currentTimeMillis()));
         }

         if (this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT) {
            for (int var6 = 0; var6 < (int)this.factor.getValue(); var6++) {
               this.doEffect(var2);
            }

            return;
         }

         if (this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
            boolean var3 = false;

            for (KillEffect.DeathEffect var5 : this.deathEffects) {
               if (Math.abs(var5.posX - var2.getX()) < 0.5 && Math.abs(var5.posY - var2.getY()) < 0.5 && Math.abs(var5.posZ - var2.getZ()) < 0.5) {
                  var3 = true;
                  break;
               }
            }

            if (!var3) {
               this.deathEffects.add(new KillEffect.DeathEffect(var2.getX(), var2.getY() + 1.0, var2.getZ(), System.currentTimeMillis()));
            }
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (mc.world != null) {
         if (this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
            this.deathEffects.removeIf(var1x -> System.currentTimeMillis() - var1x.timestamp > this.duration.getValue() * 1000.0);
         }

         this.skullEffects.removeIf(var1x -> System.currentTimeMillis() - var1x.timestamp > this.skullDuration.getValueFloat() * 1000.0);
      }
   }

   @EventListener
   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (!nullCheck() && this.isOn() && this.skullEffect.getValue() && !this.skullEffects.isEmpty() && mc.getWindow() != null) {
         SkiaContext.createSurface(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         long var3 = System.currentTimeMillis();
         float var5 = mc.getRenderTickCounter().getTickDelta(true);
         Iterator var6 = this.skullEffects.iterator();

         while (var6.hasNext()) {
            KillEffect.SkullEffect var7 = (KillEffect.SkullEffect)var6.next();
            if (!this.renderSkull(var7, var3, var5)) {
               var6.remove();
            }
         }
      }
   }

   private boolean renderSkull(KillEffect.SkullEffect var1, long var2, float var4) {
      Canvas var5 = Skia.getCanvas();
      if (var5 == null) {
         return true;
      } else {
         float var6 = this.skullDuration.getValueFloat() * 1000.0F;
         float var7 = (float)(var2 - var1.timestamp);
         if (var7 >= var6) {
            return false;
         } else {
            float var8 = var7 / var6;
            float var9 = var8 < 0.1F ? var8 / 0.1F : (var8 > 0.5F ? 1.0F - (var8 - 0.5F) / 0.5F : 1.0F);
            var9 = MathHelper.clamp(var9, 0.0F, 1.0F);
            if (var9 <= 0.01F) {
               return true;
            } else {
               double var10 = 1.2 * var8;
               double var12 = Math.sin(var7 * 0.003F) * 0.15 * var8;
               double var14 = var1.posX + var12;
               double var16 = var1.posY + var10;
               double var18 = var1.posZ;
               Vec3d var20 = TextUtil.worldSpaceToScreenSpace(new Vec3d(var14, var16, var18));
               if (!(var20.z <= 0.0) && !(var20.z >= 1.0)) {
                  float var21 = (float)var20.x;
                  float var22 = (float)var20.y;
                  float var23 = 34.0F * this.skullSize.getValueFloat();
                  float var24 = var23;
                  float var25 = var23;
                  float var26 = var21 - var23 / 2.0F;
                  float var27 = var22 - var23;
                  int var28 = (int)(var21 - var23 * 1.3F);
                  int var29 = (int)(var27 - var23 * 0.6F);
                  int var30 = (int)(var23 * 2.6F);
                  int var31 = (int)(var23 * 2.8F);
                  DeathFx.pushLayer(var9, var28, var29, var30, var31);

                  try {
                     float var32 = var8 < 0.12F ? 0.6F + 0.4F * (var8 / 0.12F) * (0.7F + 0.3F * (var8 / 0.12F)) : 1.0F;
                     float var33 = var24 * var32;
                     float var34 = var25 * var32;
                     float var35 = var27 + (var25 - var34);
                     float var36 = var23 * 1.35F * var32;
                     float var37 = var23 * 0.34F * var32;
                     float var38 = var27 + var34 * 0.86F;
                     this.drawCrossedBones(var21, var38, var36, var37);
                     this.drawSkullShape(var21, var35, var33, var34);
                  } finally {
                     Skia.restore();
                  }

                  return true;
               } else {
                  return true;
               }
            }
         }
      }
   }

   private void drawSkullShape(float var1, float var2, float var3, float var4) {
      Canvas var5 = Skia.getCanvas();
      if (var5 != null) {
         float var7 = var4 * 0.78F;
         float var8 = var1 - var3 / 2.0F;
         Skia.drawRoundedRect(var8, var2, var3, var7, var3 * 0.45F, SKULL_BONE_COLOR);
         float var10 = var3 * 0.62F;
         float var11 = var4 * 0.24F;
         float var12 = var1 - var10 / 2.0F;
         float var13 = var2 + var7 - var11 * 0.25F;
         Skia.drawRoundedRect(var12, var13, var10, var11, var11 * 0.45F, SKULL_BONE_COLOR);
         float var14 = var3 * 0.24F;
         float var15 = var7 * 0.3F;
         float var16 = var2 + var7 * 0.3F;
         float var17 = var1 - var3 * 0.3F - var14 / 2.0F;
         float var18 = var1 + var3 * 0.3F - var14 / 2.0F;
         Skia.drawRoundedRect(var17, var16, var14, var15, var14 * 0.4F, SKULL_SHADOW_COLOR);
         Skia.drawRoundedRect(var18, var16, var14, var15, var14 * 0.4F, SKULL_SHADOW_COLOR);
         float var19 = var3 * 0.1F;
         float var20 = var7 * 0.16F;
         float var21 = var16 + var15 + var7 * 0.08F;
         Skia.drawRoundedRect(var1 - var19 * 0.65F - var19 / 2.0F, var21, var19, var20, var19 * 0.4F, SKULL_SHADOW_COLOR);
         Skia.drawRoundedRect(var1 + var19 * 0.65F - var19 / 2.0F, var21, var19, var20, var19 * 0.4F, SKULL_SHADOW_COLOR);
         float var22 = var13 + var11 * 0.2F;
         float var23 = var13 + var11 * 0.85F;
         byte var24 = 3;

         for (int var25 = 1; var25 <= var24; var25++) {
            float var26 = var12 + var10 * var25 / (var24 + 1);
            Skia.drawLine(var26, var22, var26, var23, Math.max(1.0F, var10 * 0.035F), SKULL_SHADOW_COLOR);
         }
      }
   }

   private void drawCrossedBones(float var1, float var2, float var3, float var4) {
      Canvas var5 = Skia.getCanvas();
      if (var5 != null) {
         for (int var6 = 0; var6 < 2; var6++) {
            float var7 = var6 == 0 ? -45.0F : 45.0F;
            Skia.save();
            Skia.getCanvas().translate(var1, var2);
            Skia.getCanvas().rotate(var7);
            float var8 = var3 - var4 * 1.6F;
            float var9 = var4 * 0.55F;
            Skia.drawRoundedRect(-var8 / 2.0F, -var9 / 2.0F, var8, var9, var9 * 0.5F, SKULL_BONE_COLOR);
            float var10 = var4 * 0.34F;
            float var11 = var4 * 0.3F;
            float var12 = var8 / 2.0F;
            Skia.drawCircle(var12, -var11, var10, SKULL_BONE_COLOR);
            Skia.drawCircle(var12, var11, var10, SKULL_BONE_COLOR);
            Skia.drawCircle(-var12, -var11, var10, SKULL_BONE_COLOR);
            Skia.drawCircle(-var12, var11, var10, SKULL_BONE_COLOR);
            Skia.restore();
         }
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent var1) {
      if (mc.world != null && this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
         for (KillEffect.DeathEffect var3 : this.deathEffects) {
            if (!this.flash.getValue() || System.currentTimeMillis() / 200L % 2L == 0L) {
               this.drawEffect(var1, var3.posX, var3.posY, var3.posZ, 1.0F);
            }
         }
      }
   }

   private void doEffect(PlayerEntity var1) {
      double var2 = var1.getX();
      double var4 = var1.getY();
      double var6 = var1.getZ();
      if (this.lightning.getValue()) {
         LightningEntity var8 = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         var8.updatePosition(var2, var4, var6);
         var8.refreshPositionAfterTeleport(var2, var4, var6);
         mc.world.addEntity(var8);
      }

      if (this.levelUp.getValue()) {
         mc.world
            .playSound(
               mc.player,
               var2,
               var4,
               var6,
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               100.0F,
               MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat())
            );
      }

      if (this.trident.getValue()) {
         mc.world
            .playSound(
               mc.player,
               var2,
               var4,
               var6,
               SoundEvents.ITEM_TRIDENT_THUNDER,
               SoundCategory.MASTER,
               999.0F,
               MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat())
            );
      }
   }

   private void drawEffect(Render3DEvent var1, double var2, double var4, double var6, float var8) {
      try {
         double var9 = mc.gameRenderer.getCamera().getPos().x;
         double var11 = mc.gameRenderer.getCamera().getPos().y;
         double var13 = mc.gameRenderer.getCamera().getPos().z;
         float var15 = mc.gameRenderer.getCamera().getPitch();
         float var16 = mc.gameRenderer.getCamera().getYaw();
         MatrixStack var17 = new MatrixStack();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         var17.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var15));
         var17.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var16 + 180.0F));
         var17.translate(var2 - var9, var4 - var11, var6 - var13);
         var17.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-var16));
         var17.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var15));
         double var18 = 0.0245 * this.scale.getValue();
         var17.scale((float)var18, (float)var18, (float)var18);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var8);
         RenderSystem.setShaderTexture(0, YUANQUAN);
         this.drawTexture(var17, -32.0F, -32.0F, 64.0F, 64.0F);
         RenderSystem.setShaderTexture(0, JIANTOU);
         this.drawTexture(var17, -5.0F, -5.0F, 64.0F, 64.0F);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } catch (Throwable var20) {
         var20.printStackTrace();
      }
   }

   private void drawTexture(MatrixStack var1, float var2, float var3, float var4, float var5) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      Matrix4f var6 = var1.peek().getPositionMatrix();
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      var7.vertex(var6, var2, var3 + var5, 0.0F).texture(0.0F, 0.0F);
      var7.vertex(var6, var2 + var4, var3 + var5, 0.0F).texture(1.0F, 0.0F);
      var7.vertex(var6, var2 + var4, var3, 0.0F).texture(1.0F, 1.0F);
      var7.vertex(var6, var2, var3, 0.0F).texture(0.0F, 1.0F);
      BufferRenderer.drawWithGlobalProgram(var7.end());
   }

   private static class DeathEffect {
      public final double posX;
      public final double posY;
      public final double posZ;
      public final long timestamp;

      public DeathEffect(double var1, double var3, double var5, long var7) {
         this.posX = var1;
         this.posY = var3;
         this.posZ = var5;
         this.timestamp = var7;
      }
   }

   public static enum KillEffectMode {
      DEFAULT,
      HereHasALowiqDie;
   }

   private static class SkullEffect {
      public final double posX;
      public final double posY;
      public final double posZ;
      public final long timestamp;

      public SkullEffect(double var1, double var3, double var5, long var7) {
         this.posX = var1;
         this.posY = var3;
         this.posZ = var5;
         this.timestamp = var7;
      }
   }
}
