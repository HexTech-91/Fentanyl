package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.TPUtils;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class LongJump extends Module {
   public static LongJump INSTANCE;
   public final EnumSetting<LongJump.JumpMode> jumpMode = this.add(new EnumSetting<>("\u6a21\u5f0f", LongJump.JumpMode.Glide));
   private final SliderSetting vanillaBoostFactor = this.add(
      new SliderSetting("VanillaBoostFactor", 1.261, 0.0, 5.0, () -> this.jumpMode.is(LongJump.JumpMode.Vanilla))
   );
   private final SliderSetting burstInitialSpeed;
   private final SliderSetting burstBoostFactor;
   private final BooleanSetting onlyOnGround;
   private final BooleanSetting onJump;
   private final SliderSetting glideMultiplier;
   private final BooleanSetting autoDisable;
   private final BooleanSetting disableOnRubberband;
   private final SliderSetting tpDistance;
   private final SliderSetting strafeSpeed;
   private final BooleanSetting strafeJump;
   private final SliderSetting strafeLagTime;
   private final BooleanSetting airStop;
   private final Timer lagTimer = new Timer();
   private boolean strafeStop;
   private double strafeSpeedVal;
   private double strafeDistance;
   private int strafeStage;
   private boolean strafeBoost;
   private int stage;
   private double moveSpeed;
   private boolean jumping;
   private int airTicks;
   private int groundTicks;
   private boolean jumped;

   public LongJump() {
      super("LongJump", Module.Category.Movement);
      this.vanillaBoostFactor.setChinese("\u539f\u7248\u52a0\u901f\u56e0\u5b50");
      this.burstInitialSpeed = this.add(new SliderSetting("BurstInitialSpeed", 6, 0, 20, () -> this.jumpMode.is(LongJump.JumpMode.Burst)));
      this.burstInitialSpeed.setChinese("\u7206\u53d1\u521d\u59cb\u901f\u5ea6");
      this.burstBoostFactor = this.add(new SliderSetting("BurstBoostFactor", 2.149, 0.0, 20.0, () -> this.jumpMode.is(LongJump.JumpMode.Burst)));
      this.burstBoostFactor.setChinese("\u7206\u53d1\u52a0\u901f\u56e0\u5b50");
      this.onlyOnGround = this.add(new BooleanSetting("OnlyOnGround", false, () -> this.jumpMode.is(LongJump.JumpMode.Burst)));
      this.onlyOnGround.setChinese("\u4ec5\u5728\u5730\u9762");
      this.onJump = this.add(new BooleanSetting("OnJump", false, () -> this.jumpMode.is(LongJump.JumpMode.Burst)));
      this.onJump.setChinese("\u8df3\u8dc3\u65f6");
      this.glideMultiplier = this.add(new SliderSetting("GlideMultiplier", 1.0, 0.0, 5.0, () -> this.jumpMode.is(LongJump.JumpMode.Glide)));
      this.glideMultiplier.setChinese("\u6ed1\u7fd4\u500d\u7387");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.disableOnRubberband = this.add(new BooleanSetting("DisableOnRubberband", true));
      this.disableOnRubberband.setChinese("\u56de\u5f39\u5173\u95ed");
      this.tpDistance = this.add(new SliderSetting("Distance", 15, 0, 100, () -> this.jumpMode.is(LongJump.JumpMode.WindChargeTP)));
      this.tpDistance.setChinese("\u8ddd\u79bb");
      this.strafeSpeed = this.add(new SliderSetting("StrafeSpeed", 0.2873, 0.0, 1.0, 1.0E-4, () -> this.jumpMode.is(LongJump.JumpMode.Strafe)));
      this.strafeSpeed.setChinese("\u901f\u5ea6");
      this.strafeJump = this.add(new BooleanSetting("StrafeJump", true, () -> this.jumpMode.is(LongJump.JumpMode.Strafe)));
      this.strafeJump.setChinese("\u8df3\u8dc3");
      this.strafeLagTime = this.add(new SliderSetting("StrafeLagTime", 500.0, 0.0, 1000.0, 1.0, () -> this.jumpMode.is(LongJump.JumpMode.Strafe)));
      this.strafeLagTime.setChinese("\u5ef6\u8fdf\u65f6\u95f4");
      this.airStop = this.add(new BooleanSetting("AirStop", false, () -> this.jumpMode.is(LongJump.JumpMode.Strafe)));
      this.airStop.setChinese("\u7a7a\u4e2d\u505c\u6b62");
      this.jumping = false;
      this.jumped = false;
      this.setChinese("\u98ce\u86cb\u957f\u8df3");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.stage = 0;
      this.jumping = false;
      this.airTicks = 0;
      this.groundTicks = -5;
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive var1) {
      if (var1.getPacket() instanceof PlayerPositionLookS2CPacket && this.disableOnRubberband.getValue()) {
         this.disable();
      }
   }

   @EventListener
   private void onWindBurst(PacketEvent.Receive var1) {
      if (this.jumpMode.is(LongJump.JumpMode.WindChargeTP) && mc.player != null) {
         if (var1.getPacket() instanceof ExplosionS2CPacket var4) {
            if (var4.getSoundEvent() == SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST) {
               if (!(mc.player.squaredDistanceTo(var4.getX(), var4.getY(), var4.getZ()) > 5.0)) {
                  Vec3d var5 = mc.player.getRotationVec(1.0F);
                  RaycastContext var6 = new RaycastContext(
                     mc.player.getEyePos(),
                     mc.player.getEyePos().add(var5.multiply(this.tpDistance.getValue())),
                     ShapeType.COLLIDER,
                     FluidHandling.NONE,
                     mc.player
                  );
                  BlockHitResult var7 = mc.world.raycast(var6);
                  BlockPos var2;
                  if (var7.getType() == Type.ENTITY) {
                     Vec3d var8 = mc.player.getEyePos().add(var5.multiply(this.tpDistance.getValue()));
                     if (!(var8.y > mc.player.getY())) {
                        return;
                     }

                     var2 = new BlockPosX(var8);
                  } else {
                     var2 = var7.getBlockPos();
                  }

                  while (var2.getY() > -64 && mc.world.isAir((BlockPos)var2)) {
                     var2 = var2.down();
                  }

                  double var14 = var2.getX() + 0.5;
                  double var10 = var2.getY() + 1.0;
                  double var12 = var2.getZ() + 0.5;
                  TPUtils.newTeleport(new Vec3d(var14, var10, var12));
                  mc.player.setPosition(var14, var10, var12);
                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }
               }
            }
         }
      }
   }

   @EventListener
   private void onPlayerMove(MoveEvent var1) {
      switch ((LongJump.JumpMode)this.jumpMode.getValue()) {
         case Vanilla:
            if (MovementUtil.isMoving() && mc.options.jumpKey.isPressed()) {
               double var12 = this.getDir();
               double var14 = Math.cos(Math.toRadians(var12 + 90.0));
               double var16 = Math.sin(Math.toRadians(var12 + 90.0));
               if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.horizontalCollision) {
                  var1.setX(var14 * 0.29F);
                  var1.setZ(var16 * 0.29F);
               }

               if (var1.getY() == 0.33319999363422365) {
                  var1.setX(var14 * this.vanillaBoostFactor.getValue());
                  var1.setZ(var16 * this.vanillaBoostFactor.getValue());
               }
            }
            break;
         case Burst:
            if (this.stage != 0 && !mc.player.isOnGround() && this.autoDisable.getValue()) {
               this.jumping = true;
            }

            if (this.jumping && mc.player.getY() - (int)mc.player.getY() < 0.01) {
               this.jumping = false;
               this.disable();
            }

            if (this.onlyOnGround.getValue() && !mc.player.isOnGround() && this.stage == 0) {
               return;
            }

            double var11 = mc.player.getX() - mc.player.prevX;
            double var13 = mc.player.getZ() - mc.player.prevZ;
            double var15 = Math.sqrt(var11 * var11 + var13 * var13);
            if (MovementUtil.isMoving() && (!this.onJump.getValue() || mc.options.jumpKey.isPressed()) && !mc.player.isTouchingWater() && !mc.player.isInLava()
               )
             {
               if (this.stage == 0) {
                  this.moveSpeed = this.getMoveSpeed() * this.burstInitialSpeed.getValue();
               } else if (this.stage == 1) {
                  var1.setY(0.42);
                  this.moveSpeed = this.moveSpeed * this.burstBoostFactor.getValue();
               } else if (this.stage == 2) {
                  double var8 = var15 - this.getMoveSpeed();
                  this.moveSpeed = var15 - var8;
               } else {
                  this.moveSpeed = var15 - var15 / 159.0;
               }

               this.moveSpeed = Math.max(this.getMoveSpeed(), this.moveSpeed);
               this.setMoveSpeed(var1, this.moveSpeed);
               if (!mc.player.horizontalCollision
                  && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0))
                  && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, -0.4, 0.0))) {
                  var1.setY(-0.001);
               }

               this.stage++;
            }
         case Glide:
         case WindChargeTP:
         default:
            break;
         case Strafe:
            if (!MovementUtil.isMoving() && this.airStop.getValue()) {
               MovementUtil.setMotionX(0.0);
               MovementUtil.setMotionZ(0.0);
            }

            if (mc.player.isRiding()
               || mc.player.isHoldingOntoLadder()
               || mc.player.getAbilities().flying
               || mc.player.isFallFlying()
               || !MovementUtil.isMoving()) {
               this.resetStrafe();
               this.strafeStop = true;
            } else if (this.strafeStop) {
               this.strafeStop = false;
            } else if (this.lagTimer.passed(this.strafeLagTime.getValueInt())) {
               if (this.strafeStage == 1) {
                  this.strafeSpeedVal = 1.35 * MovementUtil.getSpeed(false, this.strafeSpeed.getValue()) - 0.01;
               } else if (this.strafeStage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.strafeJump.getValue()) {
                  if (this.strafeStage == 3) {
                     this.strafeSpeedVal = this.strafeDistance - 0.66 * (this.strafeDistance - MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
                     this.strafeBoost = !this.strafeBoost;
                  } else {
                     if ((BlockUtil.canCollide(null, mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly)
                        && this.strafeStage > 0) {
                        this.strafeStage = 1;
                     }

                     this.strafeSpeedVal = this.strafeDistance - this.strafeDistance / 159.0;
                  }
               } else {
                  double var2 = 0.3999 + MovementUtil.getJumpSpeed();
                  MovementUtil.setMotionY(var2);
                  var1.setY(var2);
                  this.strafeSpeedVal = this.strafeSpeedVal * (this.strafeBoost ? 1.6835 : 1.395);
               }

               this.strafeSpeedVal = Math.min(this.strafeSpeedVal, 10.0);
               this.strafeSpeedVal = Math.max(this.strafeSpeedVal, MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
               double var10 = mc.player.input.movementForward;
               double var4 = mc.player.input.movementSideways;
               double var6 = mc.player.getYaw();
               if (var10 == 0.0 && var4 == 0.0) {
                  var1.setX(0.0);
                  var1.setZ(0.0);
               } else if (var10 != 0.0 && var4 != 0.0) {
                  var10 *= Math.sin(Math.PI / 4);
                  var4 *= Math.cos(Math.PI / 4);
               }

               var1.setX((var10 * this.strafeSpeedVal * -Math.sin(Math.toRadians(var6)) + var4 * this.strafeSpeedVal * Math.cos(Math.toRadians(var6))) * 0.99);
               var1.setZ((var10 * this.strafeSpeedVal * Math.cos(Math.toRadians(var6)) - var4 * this.strafeSpeedVal * -Math.sin(Math.toRadians(var6))) * 0.99);
               this.strafeStage++;
            }
      }
   }

   public void resetStrafe() {
      this.strafeStage = 4;
      this.strafeSpeedVal = 0.0;
      this.strafeDistance = 0.0;
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         if (this.jumpMode.getValue() == LongJump.JumpMode.Glide) {
            if (!MovementUtil.isMoving()) {
               return;
            }

            float var1 = mc.player.getYaw() + 90.0F;
            double var2 = mc.player.input.movementForward != 0.0F ? (mc.player.input.movementForward > 0.0F ? 1 : -1) : 0.0;
            float[] var4 = new float[]{
               0.4206065F,
               0.4179245F,
               0.41525924F,
               0.41261F,
               0.409978F,
               0.407361F,
               0.404761F,
               0.402178F,
               0.399611F,
               0.39706F,
               0.394525F,
               0.392F,
               0.3894F,
               0.38644F,
               0.383655F,
               0.381105F,
               0.37867F,
               0.37625F,
               0.37384F,
               0.37145F,
               0.369F,
               0.3666F,
               0.3642F,
               0.3618F,
               0.35945F,
               0.357F,
               0.354F,
               0.351F,
               0.348F,
               0.345F,
               0.342F,
               0.339F,
               0.336F,
               0.333F,
               0.33F,
               0.327F,
               0.324F,
               0.321F,
               0.318F,
               0.315F,
               0.312F,
               0.309F,
               0.307F,
               0.305F,
               0.303F,
               0.3F,
               0.297F,
               0.295F,
               0.293F,
               0.291F,
               0.289F,
               0.287F,
               0.285F,
               0.283F,
               0.281F,
               0.279F,
               0.277F,
               0.275F,
               0.273F,
               0.271F,
               0.269F,
               0.267F,
               0.265F,
               0.263F,
               0.261F,
               0.259F,
               0.257F,
               0.255F,
               0.253F,
               0.251F,
               0.249F,
               0.247F,
               0.245F,
               0.243F,
               0.241F,
               0.239F,
               0.237F
            };
            float[] var5 = new float[]{0.3425F, 0.5445F, 0.65425F, 0.685F, 0.675F, 0.2F, 0.895F, 0.719F, 0.76F};
            double var6 = Math.cos(Math.toRadians(var1));
            double var8 = Math.sin(Math.toRadians(var1));
            if (!mc.player.horizontalCollision && !mc.player.isOnGround()) {
               this.jumped = true;
               this.airTicks++;
               this.groundTicks = -5;
               double var10 = mc.player.getVelocity().y;
               if (this.airTicks - 6 >= 0 && this.airTicks - 6 < var5.length) {
                  this.updateY(var10 * var5[this.airTicks - 6] * this.glideMultiplier.getValue());
               }

               if (var10 < -0.2 && var10 > -0.24) {
                  this.updateY(var10 * 0.7 * this.glideMultiplier.getValue());
               } else if (var10 < -0.25 && var10 > -0.32) {
                  this.updateY(var10 * 0.8 * this.glideMultiplier.getValue());
               } else if (var10 < -0.35 && var10 > -0.8) {
                  this.updateY(var10 * 0.98 * this.glideMultiplier.getValue());
               }

               if (this.airTicks - 1 >= 0 && this.airTicks - 1 < var4.length) {
                  mc.player
                     .setVelocity(
                        var2 * var4[this.airTicks - 1] * 3.0 * var6 * this.glideMultiplier.getValue(),
                        mc.player.getVelocity().y,
                        var2 * var4[this.airTicks - 1] * 3.0 * var8 * this.glideMultiplier.getValue()
                     );
               } else {
                  mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
               }
            } else {
               if (this.autoDisable.getValue() && this.jumped) {
                  this.jumped = false;
                  this.disable();
               }

               this.airTicks = 0;
               this.groundTicks++;
               if (this.groundTicks <= 2) {
                  mc.player
                     .setVelocity(
                        var2 * 0.01F * var6 * this.glideMultiplier.getValue(), mc.player.getVelocity().y, var2 * 0.01F * var8 * this.glideMultiplier.getValue()
                     );
               } else {
                  mc.player.setVelocity(var2 * 0.3F * var6 * this.glideMultiplier.getValue(), 0.424F, var2 * 0.3F * var8 * this.glideMultiplier.getValue());
               }
            }
         } else if (this.jumpMode.getValue() == LongJump.JumpMode.WindChargeTP) {
            int var12 = InventoryUtil.findItemInventorySlot(Items.WIND_CHARGE);
            if (var12 == -1) {
               this.disable();
               return;
            }

            Vec3d var13 = null;

            for (Direction var20 : Direction.values()) {
               BlockPos var7 = mc.player.getBlockPos().offset(var20);
               if (!mc.world.isAir(var7) && mc.world.getBlockState(var7).isFullCube(mc.world, var7)) {
                  var13 = this.getClosestPointToSide(mc.player.getEyePos(), var7, var20);
               }
            }

            if (var13 == null) {
               for (Direction var21 : Direction.values()) {
                  for (Direction var24 : Direction.values()) {
                     BlockPos var11 = mc.player.getBlockPos().offset(var21).offset(var24);
                     if (!mc.world.isAir(var11) && mc.world.getBlockState(var11).isFullCube(mc.world, var11)) {
                        var13 = this.getClosestPointToSide(mc.player.getEyePos(), var11, var24);
                     }
                  }
               }
            }

            if (var13 == null) {
               return;
            }

            int var15 = mc.player.getInventory().selectedSlot;
            Alien.ROTATION.snapAt(var13);
            InventoryUtil.inventorySwap(var12, var15);
            sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(var12, var15);
         }
      }
   }

   private Vec3d getClosestPointToSide(Vec3d var1, BlockPos var2, Direction var3) {
      double var4 = var2.getX() + 0.5;
      double var6 = var2.getY() + 0.5;
      double var8 = var2.getZ() + 0.5;
      if (var3 == Direction.DOWN) {
         var6 = var2.getY();
      } else if (var3 == Direction.UP) {
         var6 = var2.getY() + 1.0;
      } else if (var3 == Direction.NORTH) {
         var8 = var2.getZ();
      } else if (var3 == Direction.SOUTH) {
         var8 = var2.getZ() + 1.0;
      } else if (var3 == Direction.WEST) {
         var4 = var2.getX();
      } else if (var3 == Direction.EAST) {
         var4 = var2.getX() + 1.0;
      }

      return new Vec3d(var4, var6, var8);
   }

   private void updateY(double var1) {
      mc.player.setVelocity(mc.player.getVelocity().x, var1, mc.player.getVelocity().z);
   }

   private double getDir() {
      double var1 = 0.0;
      if (mc.player != null) {
         var1 = mc.player.getYaw() + (mc.player.input.movementForward < 0.0F ? 180 : 0);
         if (mc.player.input.movementSideways > 0.0F) {
            var1 += -90.0F * (mc.player.input.movementForward < 0.0F ? -0.5F : (mc.player.input.movementForward > 0.0F ? 0.5F : 1.0F));
         } else if (mc.player.input.movementSideways < 0.0F) {
            var1 += 90.0F * (mc.player.input.movementForward < 0.0F ? -0.5F : (mc.player.input.movementForward > 0.0F ? 0.5F : 1.0F));
         }
      }

      return var1;
   }

   private double getMoveSpeed() {
      double var1 = 0.2873;
      if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
         var1 *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
      }

      return var1;
   }

   private void setMoveSpeed(MoveEvent var1, double var2) {
      double var4 = mc.player.input.movementForward;
      double var6 = mc.player.input.movementSideways;
      float var8 = mc.player.getYaw();
      if (!MovementUtil.isMoving()) {
         var1.setX(0.0);
         var1.setZ(0.0);
      } else {
         if (var4 != 0.0) {
            if (var6 > 0.0) {
               var8 += var4 > 0.0 ? -45 : 45;
            } else if (var6 < 0.0) {
               var8 += var4 > 0.0 ? 45 : -45;
            }
         }

         var6 = 0.0;
         if (var4 > 0.0) {
            var4 = 1.0;
         } else if (var4 < 0.0) {
            var4 = -1.0;
         }
      }

      double var9 = Math.cos(Math.toRadians(var8 + 90.0F));
      double var11 = Math.sin(Math.toRadians(var8 + 90.0F));
      var1.setX(var4 * var2 * var9 + var6 * var2 * var11);
      var1.setZ(var4 * var2 * var11 + var6 * var2 * var9);
   }

   public static enum JumpMode {
      Vanilla,
      Burst,
      Glide,
      WindChargeTP,
      Strafe;
   }
}
