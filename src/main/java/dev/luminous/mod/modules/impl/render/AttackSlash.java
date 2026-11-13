package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class AttackSlash extends Module {
   private final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.25, 3.0, 0.25));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.6, 0.5, 3.0, 0.1));
   private final BooleanSetting varyDirection = this.add(new BooleanSetting("VaryDirection", true));
   private final SliderSetting opacity = this.add(new SliderSetting("Opacity", 1.0, 0.2, 1.0, 0.05));
   private final List<AttackSlash.Slash> slashes = new ArrayList<>();
   private final List<AttackSlash.Tracked> tracked = new ArrayList<>();
   private int flip = 1;

   public AttackSlash() {
      super("AttackSlash", Module.Category.Render);
      this.setChinese("\u653b\u51fb\u5200\u5149");
      this.speed.setChinese("\u626b\u8fc7\u901f\u5ea6");
      this.size.setChinese("\u5927\u5c0f");
      this.varyDirection.setChinese("\u65b9\u5411\u53d8\u5316");
      this.opacity.setChinese("\u4e0d\u900f\u660e\u5ea6");
      FeatureTranslations.localizeModule(this);
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send var1) {
      if (!nullCheck()
         && var1.getPacket() instanceof PlayerInteractEntityC2SPacket var2
         && isAttackPacket(var2)
         && Criticals.getEntity(var2) instanceof LivingEntity var5
         && var5 != mc.player
         && !this.tracked.stream().anyMatch(var1x -> var1x.entity == var5)) {
         this.tracked.add(new AttackSlash.Tracked(var5, this.getHealth(var5), new Timer()));
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive var1) {
      if (!nullCheck()) {
         if (var1.getPacket() instanceof ExplosionS2CPacket var2) {
            double var22 = var2.getX();
            double var5 = var2.getY();
            double var7 = var2.getZ();

            for (Entity var10 : mc.world.getPlayers()) {
               if (var10 instanceof LivingEntity var11 && var11 != mc.player && var11.isAlive() && !var11.isRemoved()) {
                  double var12 = var11.getX() - var22;
                  double var14 = var11.getY() - var5;
                  double var16 = var11.getZ() - var7;
                  if (var12 * var12 + var14 * var14 + var16 * var16 < 36.0) {
                     boolean var18 = false;

                     for (AttackSlash.Tracked var20 : this.tracked) {
                        if (var20.entity == var11) {
                           var18 = true;
                           break;
                        }
                     }

                     if (!var18) {
                        this.tracked.add(new AttackSlash.Tracked(var11, this.getHealth(var11), new Timer()));
                     }
                  }
               }
            }
         }

         if (var1.getPacket() instanceof EntityStatusS2CPacket var21
            && var21.getStatus() == 35
            && var21.getEntity(mc.world) instanceof LivingEntity var24
            && var24 != mc.player) {
            this.spawnCrossSlashes(var24);
         }
      }
   }

   @EventListener
   public void onPlaySound(PlaySoundEvent var1) {
      if (!nullCheck() && this.isOn() && var1.sound != null) {
         Identifier var2 = var1.sound.getId();
         if (var2 != null && "item.totem.use".equals(var2.getPath())) {
            double var3 = var1.sound.getX();
            double var5 = var1.sound.getY();
            double var7 = var1.sound.getZ();
            LivingEntity var9 = null;
            double var10 = 256.0;

            for (Entity var13 : mc.world.getPlayers()) {
               if (var13 instanceof LivingEntity var14 && var14 != mc.player && var14.isAlive()) {
                  double var15 = var14.getX() - var3;
                  double var17 = var14.getY() - var5;
                  double var19 = var14.getZ() - var7;
                  double var21 = var15 * var15 + var17 * var17 + var19 * var19;
                  if (var21 < var10) {
                     var10 = var21;
                     var9 = var14;
                  }
               }
            }

            if (var9 != null) {
               this.spawnCrossSlashes(var9);
            }
         }
      }
   }

   private void spawnCrossSlashes(LivingEntity var1) {
      this.flip = -this.flip;
      this.slashes.add(new AttackSlash.Slash(var1, new Timer(), false, 30.0F));
      this.slashes.add(new AttackSlash.Slash(var1, new Timer(), true, -30.0F));
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (nullCheck()) {
         this.slashes.clear();
         this.tracked.clear();
      } else {
         long var2 = (long)(600.0 / this.speed.getValueFloat());
         this.slashes.removeIf(var2x -> var2x.timer.passedMs(var2));
         Iterator var4 = this.tracked.iterator();

         while (var4.hasNext()) {
            AttackSlash.Tracked var5 = (AttackSlash.Tracked)var4.next();
            LivingEntity var6 = var5.entity;
            if (var6 != null && var6.isAlive() && !var5.timer.passedMs(1400.0)) {
               float var7 = this.getHealth(var6);
               float var8 = var5.lastHealth - var7;
               if (var8 > 0.05F) {
                  if (var5.first || var5.slashTimer.passedMs(500.0)) {
                     this.spawnSlash(var6);
                     var5.slashTimer.reset();
                     var5.first = false;
                  }

                  var5.lastHealth = var7;
                  var5.timer.reset();
               } else {
                  if (var7 > var5.lastHealth) {
                     var5.lastHealth = var7;
                  }

                  var5.slashTimer.reset();
                  var5.first = true;
               }
            } else {
               var4.remove();
            }
         }
      }
   }

   private void spawnSlash(LivingEntity var1) {
      this.flip = -this.flip;
      boolean var2 = this.varyDirection.getValue() && this.flip > 0;
      float var3 = (float)(Math.random() * 70.0 - 35.0);
      this.slashes.add(new AttackSlash.Slash(var1, new Timer(), var2, var3));
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (!nullCheck() && !this.slashes.isEmpty()) {
         double var2 = mc.gameRenderer.getCamera().getPos().x;
         double var4 = mc.gameRenderer.getCamera().getPos().y;
         double var6 = mc.gameRenderer.getCamera().getPos().z;
         long var8 = System.currentTimeMillis();
         float var10 = 600.0F / this.speed.getValueFloat();
         float var11 = this.opacity.getValueFloat();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var12 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         for (AttackSlash.Slash var14 : this.slashes) {
            if (var14.target.isAlive()) {
               float var15 = Math.min(1.0F, (float)var14.timer.getMs() / var10);
               float var16 = var14.reverse ? 1.0F - var15 : var15;
               float var17 = var15 < 0.8F ? 1.0F : 1.0F - (var15 - 0.8F) / 0.2F;
               float var18 = var11 * var17;
               Vec3d var19 = var14.target.getPos();
               float var20 = var14.target.getHeight();
               var1.push();
               var1.translate(var19.x - var2, var19.y + var20 * 0.5 - var4, var19.z - var6);
               var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-mc.gameRenderer.getCamera().getPitch()));
               var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw()));
               var1.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var14.spin));
               Matrix4f var21 = var1.peek().getPositionMatrix();
               float var22 = 0.9F * this.size.getValueFloat();
               float var23 = var14.reverse ? -1.0F : 1.0F;
               float var24 = var23 * (var16 * 2.0F - 1.0F) * var22;
               float var25 = var23 * -var22;
               if (var14.reverse) {
                  var25 = var23 * -var22;
                  var24 = var23 * (var16 * 2.0F - 1.0F) * var22;
               }

               byte var26 = 24;
               float var27 = var25;
               float var28 = 0.0F;

               for (int var29 = 1; var29 <= var26; var29++) {
                  float var30 = (float)var29 / var26;
                  float var31 = var25 + (var24 - var25) * var30;
                  float var32 = (float)Math.sin(Math.PI * var30);
                  float var33 = 0.06F * this.size.getValueFloat() * var32 + 0.005F;
                  int var34 = (int)(var18 * 255.0F) << 24 | 16777215;
                  var12.vertex(var21, var27, -var28, 0.0F).color(var34);
                  var12.vertex(var21, var31, -var33, 0.0F).color(var34);
                  var12.vertex(var21, var31, var33, 0.0F).color(var34);
                  var12.vertex(var21, var27, var28, 0.0F).color(var34);
                  var27 = var31;
                  var28 = var33;
               }

               var1.pop();
            }
         }

         Render3DUtil.endBuilding(var12);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @Override
   public void onDisable() {
      this.slashes.clear();
      this.tracked.clear();
   }

   private static boolean isAttackPacket(PlayerInteractEntityC2SPacket var0) {
      return Criticals.getInteractType(var0) == PlayerInteractEntityC2SPacket.InteractType.ATTACK;
   }

   private float getHealth(LivingEntity var1) {
      return var1.getHealth() + var1.getAbsorptionAmount();
   }

   private static class Slash {
      final LivingEntity target;
      final Timer timer;
      final boolean reverse;
      final float spin;

      Slash(LivingEntity var1, Timer var2, boolean var3, float var4) {
         this.target = var1;
         this.timer = var2;
         this.reverse = var3;
         this.spin = var4;
      }
   }

   private static class Tracked {
      final LivingEntity entity;
      float lastHealth;
      final Timer timer;
      final Timer slashTimer = new Timer();
      boolean first = true;

      Tracked(LivingEntity var1, float var2, Timer var3) {
         this.entity = var1;
         this.lastHealth = var2;
         this.timer = var3;
      }
   }
}
