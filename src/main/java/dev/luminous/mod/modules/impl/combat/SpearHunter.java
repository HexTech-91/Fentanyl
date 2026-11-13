package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.features.SpearPort;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.render.SpearFix;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

public class SpearHunter extends Module {
   public static SpearHunter INSTANCE;
   private final StringSetting targetName = this.add(new StringSetting("TargetName", ""));
   private final SliderSetting trackRange = this.add(new SliderSetting("TrackRange", 64.0, 4.0, 256.0, 1.0));
   private final BooleanSetting killMode = this.add(new BooleanSetting("Kill", false));
   private final SliderSetting killRange = this.add(new SliderSetting("KillRange", 6.0, 1.0, 10.0, 0.5, this.killMode::getValue));
   private final SliderSetting holdTime = this.add(new SliderSetting("HoldTime", 10000.0, 500.0, 30000.0, 500.0, this.killMode::getValue).setSuffix("ms"));
   private final SliderSetting releaseDelay = this.add(new SliderSetting("ReleaseDelay", 300.0, 50.0, 2000.0, 50.0, this.killMode::getValue).setSuffix("ms"));
   private final BooleanSetting progressiveSpeed = this.add(new BooleanSetting("ProgressiveSpeed", true));
   private final SliderSetting baseSpeed = this.add(new SliderSetting("BaseSpeed", 1.0, 0.1, 10.0, 0.1));
   private final SliderSetting maxSpeed = this.add(new SliderSetting("MaxSpeed", 10.0, 1.0, 20.0, 0.5));
   private final SliderSetting accelAmount = this.add(new SliderSetting("AccelAmount", 0.2, 0.05, 2.0, 0.05, this.progressiveSpeed::getValue));
   private final SliderSetting accelTicks = this.add(new SliderSetting("AccelTicks", 10.0, 1.0, 40.0, 1.0, this.progressiveSpeed::getValue));
   private final SliderSetting frontDist = this.add(new SliderSetting("FrontDist", 8.0, 1.0, 16.0, 0.5));
   private final SliderSetting backDist = this.add(new SliderSetting("BackDist", 8.0, 1.0, 16.0, 0.5));
   private final SliderSetting followDist = this.add(new SliderSetting("FollowDist", 4.0, 1.0, 6.0, 0.5, this.killMode::getValue));
   private final SliderSetting minHeight = this.add(new SliderSetting("MinHeight", 3.0, 1.0, 8.0, 0.5));
   private final SliderSetting turnSpeed = this.add(new SliderSetting("TurnSpeed", 45.0, 10.0, 90.0, 1.0));
   private final SliderSetting pullbackSpeed = this.add(new SliderSetting("PullbackSpeed", 2.1, 0.1, 10.0, 0.1, this.progressiveSpeed::getValue));
   private final BooleanSetting avoidBlocks = this.add(new BooleanSetting("AvoidBlocks", true));
   private final SliderSetting avoidDistance = this.add(new SliderSetting("AvoidDistance", 4.0, 2.0, 10.0, 0.5, this.avoidBlocks::getValue));
   private final SliderSetting avoidStep = this.add(new SliderSetting("AvoidStep", 20.0, 5.0, 90.0, 1.0, this.avoidBlocks::getValue));
   private final SliderSetting avoidMaxAngle = this.add(new SliderSetting("AvoidMaxAngle", 80.0, 10.0, 180.0, 1.0, this.avoidBlocks::getValue));
   private final SliderSetting heightOffset = this.add(new SliderSetting("HeightOffset", 0.0, -4.0, 4.0, 0.1));
   private final SliderSetting verticalStrength = this.add(new SliderSetting("VerticalStrength", 0.08, 0.01, 1.0, 0.01));
   private final SliderSetting maxVertical = this.add(new SliderSetting("MaxVertical", 0.5, 0.05, 2.0, 0.05));
   private final SliderSetting predict = this.add(new SliderSetting("Predict", 1.0, 0.0, 5.0, 0.1));
   private final BooleanSetting autoSpear = this.add(new BooleanSetting("AutoSpear", true));
   private final BooleanSetting ignoreFriend = this.add(new BooleanSetting("IgnoreFriend", true));
   private final BooleanSetting faceTarget = this.add(new BooleanSetting("FaceTarget", true));
   private PlayerEntity target;
   private float lookYaw;
   private float lookPitch;
   private boolean phaseFront = true;
   private boolean enemyMoving;
   private boolean simPressing;
   private boolean waitingRelease;
   private int tickCounter;
   private double currentSpeed;
   private float driveYawSmooth = Float.NaN;
   private boolean enemyGliding;
   private boolean enemyWalking;
   private boolean pullbackDetected;
   private float lineYaw = Float.NaN;
   private double lastEnemyVelX;
   private double lastEnemyVelZ;
   private final Timer pressTimer = new Timer();

   public SpearHunter() {
      super("SpearHunter", Module.Category.Combat);
      this.setChinese("\u957f\u77db\u5f3a\u8ffd\u8e2a");
      this.targetName.setChinese("\u81ea\u5b9a\u4e49\u8ffd\u8e2a\u540d\u79f0");
      this.trackRange.setChinese("\u8ffd\u8e2a\u8303\u56f4");
      this.killMode.setChinese("\u6740\u622e");
      this.killRange.setChinese("\u6740\u622e\u8303\u56f4");
      this.holdTime.setChinese("\u957f\u6309\u65f6\u95f4");
      this.releaseDelay.setChinese("\u677e\u5f00\u95f4\u9694");
      this.progressiveSpeed.setChinese("\u6e10\u8fdb\u52a0\u901f");
      this.baseSpeed.setChinese("\u57fa\u7840\u901f\u5ea6");
      this.maxSpeed.setChinese("\u6700\u5927\u901f\u5ea6");
      this.accelAmount.setChinese("\u6bcf\u6b21\u52a0\u901f");
      this.accelTicks.setChinese("\u52a0\u901f\u95f4\u9694(tick)");
      this.frontDist.setChinese("\u524d\u65b9\u8ddd\u79bb");
      this.backDist.setChinese("\u540e\u65b9\u8ddd\u79bb");
      this.followDist.setChinese("\u8ddf\u968f\u8ddd\u79bb");
      this.minHeight.setChinese("\u79bb\u5730\u9ad8\u5ea6");
      this.turnSpeed.setChinese("\u8f6c\u5411\u901f\u5ea6");
      this.pullbackSpeed.setChinese("\u62c9\u56de\u901f\u5ea6");
      this.avoidBlocks.setChinese("\u81ea\u52a8\u907f\u969c");
      this.avoidDistance.setChinese("\u907f\u969c\u63a2\u6d4b\u8ddd\u79bb");
      this.avoidStep.setChinese("\u907f\u969c\u6b65\u957f");
      this.avoidMaxAngle.setChinese("\u907f\u969c\u6700\u5927\u89d2\u5ea6");
      this.heightOffset.setChinese("\u9ad8\u5ea6\u504f\u79fb");
      this.verticalStrength.setChinese("\u5782\u76f4\u529b\u5ea6");
      this.maxVertical.setChinese("\u6700\u5927\u5782\u76f4");
      this.predict.setChinese("\u79fb\u52a8\u9884\u6d4b");
      this.autoSpear.setChinese("\u81ea\u52a8\u5207\u957f\u77db");
      this.ignoreFriend.setChinese("\u5ffd\u7565\u597d\u53cb");
      this.faceTarget.setChinese("\u9501\u5b9a\u89c6\u89d2");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      if (this.target == null) {
         return null;
      } else {
         String var1 = this.target.getName().getString();
         return (this.killLocked() ? "[\u6740] " : "") + var1 + " " + String.format("%.1f", this.currentSpeed);
      }
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.phaseFront = true;
      this.enemyMoving = false;
      this.enemyGliding = false;
      this.enemyWalking = false;
      this.tickCounter = 0;
      this.currentSpeed = this.baseSpeed.getValue();
      this.driveYawSmooth = Float.NaN;
      this.lineYaw = Float.NaN;
      this.lastEnemyVelX = 0.0;
      this.lastEnemyVelZ = 0.0;
      this.stopSimPress();
   }

   @Override
   public void onDisable() {
      this.stopSimPress();
      this.target = null;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target == null) {
            this.stopSimPress();
            return;
         }

         if (ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn() && !ElytraFly.INSTANCE.isFallFlying() && mc.player.getVelocity().y > -0.1) {
         }

         Vec3d var2 = this.target.getVelocity();
         double var3 = Math.sqrt(var2.x * var2.x + var2.z * var2.z);
         if (this.enemyMoving) {
            if (var3 < 0.12) {
               this.enemyMoving = false;
            }
         } else if (var3 > 0.18) {
            this.enemyMoving = true;
            this.lineYaw = Float.NaN;
         }

         if (var3 > 0.18) {
            this.lastEnemyVelX = var2.x / var3;
            this.lastEnemyVelZ = var2.z / var3;
         }

         this.enemyGliding = this.target.isFallFlying();
         this.enemyWalking = this.enemyMoving && !this.enemyGliding;
         if (!this.enemyMoving) {
            if (Float.isNaN(this.lineYaw)) {
               this.lineYaw = mc.player.getYaw();
            }

            double var5 = Math.toRadians(this.lineYaw);
            double var7 = -Math.sin(var5);
            double var9 = Math.cos(var5);
            double var11 = (mc.player.getX() - this.target.getX()) * var7 + (mc.player.getZ() - this.target.getZ()) * var9;
            double var13 = this.frontDist.getValue();
            double var15 = -this.backDist.getValue();
            double var17 = this.phaseFront ? var13 : var15;
            double var19 = this.target.getX() + var7 * var17;
            double var21 = this.target.getZ() + var9 * var17;
            double var23 = var19 - mc.player.getX();
            double var25 = var21 - mc.player.getZ();
            boolean var27 = var23 * var23 + var25 * var25 < 6.25;
            boolean var28 = this.phaseFront ? var11 > var13 + 0.5 : var11 < var15 - 0.5;
            if (var27 || var28) {
               this.phaseFront = !this.phaseFront;
            }
         }

         if (this.progressiveSpeed.getValue() && this.canControl()) {
            this.tickCounter++;
            if (this.tickCounter >= (int)this.accelTicks.getValue()) {
               this.tickCounter = 0;
               this.currentSpeed = Math.min(this.currentSpeed + this.accelAmount.getValue(), this.maxSpeed.getValue());
            }

            if (this.pullbackDetected) {
               this.pullbackDetected = false;
               this.currentSpeed = Math.min(this.currentSpeed, this.pullbackSpeed.getValue());
            }
         }

         Vec3d var29 = this.getAimPosition(this.target);
         Vec3d var6 = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
         Vec3d var30 = var29.subtract(var6);
         double var8 = Math.sqrt(var30.x * var30.x + var30.z * var30.z);
         this.lookYaw = (float)Math.toDegrees(Math.atan2(var30.z, var30.x)) - 90.0F;
         this.lookPitch = (float)(-Math.toDegrees(Math.atan2(var30.y, var8)));
         this.handleSpearPress();
      }
   }

   private boolean isSpearFix(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         return SpearFix.INSTANCE != null ? SpearFix.INSTANCE.shouldAnimate(var1) : SpearPort.isSpear(var1);
      } else {
         return false;
      }
   }

   private int findSpearFixSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int var1 = 0; var1 < 9; var1++) {
            if (this.isSpearFix(mc.player.getInventory().getStack(var1))) {
               return var1;
            }
         }

         return -1;
      }
   }

   private void handleSpearPress() {
      if (!this.canControl()) {
         this.stopSimPress();
      } else {
         boolean var1 = this.killLocked();
         if (!var1) {
            if (this.autoSpear.getValue() && !this.isSpearFix(mc.player.getMainHandStack())) {
               int var2 = this.findSpearFixSlot();
               if (var2 == -1) {
                  this.stopSimPress();
                  return;
               }

               if (var2 != mc.player.getInventory().selectedSlot) {
                  this.stopSimPress();
                  InventoryUtil.switchToSlot(var2);
                  return;
               }
            }

            ItemStack var3 = mc.player.getMainHandStack();
            if (!this.isSpearFix(var3)) {
               this.stopSimPress();
               return;
            }
         }

         if (var1) {
            if (!this.simPressing) {
               this.startSimPress();
            }
         } else if (!this.simPressing && !this.waitingRelease) {
            this.startSimPress();
         } else if (this.waitingRelease && this.pressTimer.passedMs(this.releaseDelay.getValue())) {
            this.waitingRelease = false;
            this.startSimPress();
         }
      }
   }

   private void startSimPress() {
      mc.options.useKey.setPressed(true);
      this.simPressing = true;
      this.pressTimer.reset();
   }

   private void stopSimPress() {
      if (this.simPressing && !nullCheck()) {
         mc.options.useKey.setPressed(false);
      }

      this.simPressing = false;
   }

   @EventListener(
      priority = -9999
   )
   public void onRotate(UpdateRotateEvent var1) {
      if (!nullCheck() && this.target != null && this.faceTarget.getValue() && (this.canControl() || this.killLocked())) {
         var1.setYaw(this.lookYaw);
         var1.setPitch(this.lookPitch);
      }
   }

   @EventListener(
      priority = 200
   )
   public void onMove(TravelEvent var1) {
      if (this.isOn() && !nullCheck() && !var1.isPost() && this.target != null && this.canControl()) {
         float var2;
         if (!this.enemyMoving) {
            var2 = this.yawToPoint(this.getShuttlePoint(this.lineYaw, this.phaseFront ? this.frontDist.getValue() : -this.backDist.getValue()));
         } else {
            var2 = this.yawToPoint(this.getChasePoint());
            if (this.enemyGliding) {
               if (Float.isNaN(this.driveYawSmooth)) {
                  this.driveYawSmooth = var2;
               }

               this.driveYawSmooth = this.stepAngle(this.driveYawSmooth, var2, this.turnSpeed.getValueFloat());
               var2 = this.driveYawSmooth;
            }
         }

         if (this.avoidBlocks.getValue() && !this.isPathClear(var2)) {
            float var3 = this.avoidStep.getValueFloat();
            float var4 = this.avoidMaxAngle.getValueFloat();
            float var5 = Float.NaN;

            for (float var6 = var3; var6 <= var4 && Float.isNaN(var5); var6 += var3) {
               if (this.isPathClear(var2 + var6)) {
                  var5 = var2 + var6;
               } else if (this.isPathClear(var2 - var6)) {
                  var5 = var2 - var6;
               }
            }

            if (!Float.isNaN(var5)) {
               var2 = var5;
            }
         }

         if (!this.enemyMoving) {
            this.pullbackDetected = false;
         } else {
            float var21 = Math.abs(MathHelper.wrapDegrees(this.yawToPoint(this.getChasePoint()) - var2));
            this.pullbackDetected = var21 > 55.0F && this.currentSpeed > this.pullbackSpeed.getValue() + 0.1;
         }

         double var22 = Math.toRadians(var2 + 90.0F);
         double var23 = this.currentSpeed;
         MovementUtil.setMotionX(var23 * Math.cos(var22));
         MovementUtil.setMotionZ(var23 * Math.sin(var22));
         double var7 = this.groundBelow();
         double var9 = mc.player.getY();
         double var11 = this.getChasePoint().y + this.heightOffset.getValue();
         double var13 = var11 - var9;
         double var15;
         if (var13 >= 0.0) {
            var15 = 0.0;
         } else {
            double var17 = var9 + var13;
            double var19 = var7 + this.minHeight.getValue();
            var15 = Math.max(var17, var19) - var9;
            var15 *= this.verticalStrength.getValue();
         }

         MovementUtil.setMotionY(Math.min(0.0, Math.max(-this.maxVertical.getValue(), var15)));
         var1.cancel();
         mc.player.move(MovementType.SELF, mc.player.getVelocity());
      }
   }

   private double groundBelow() {
      double var1 = mc.player.getX();
      double var3 = mc.player.getY();
      double var5 = mc.player.getZ();
      int var7 = (int)Math.floor(var3 - 0.6);
      int var8 = Math.max(var7 - 40, -64);
      Mutable var9 = new Mutable();

      for (int var10 = var7; var10 >= var8; var10--) {
         var9.set((int)Math.floor(var1), var10, (int)Math.floor(var5));
         if (mc.world.getBlockState(var9).isSolidBlock(mc.world, var9)) {
            return var10 + 1.0;
         }
      }

      return var3;
   }

   private float stepAngle(float var1, float var2, float var3) {
      float var4 = MathHelper.wrapDegrees(var2 - var1);
      if (var4 > var3) {
         var4 = var3;
      } else if (var4 < -var3) {
         var4 = -var3;
      }

      return var1 + var4;
   }

   private boolean killLocked() {
      return this.killMode.getValue()
         && this.target != null
         && mc.player != null
         && mc.player.squaredDistanceTo(this.target) <= this.killRange.getValue() * this.killRange.getValue();
   }

   private boolean canControl() {
      return ElytraFly.INSTANCE == null ? false : ElytraFly.INSTANCE.isFallFlying();
   }

   private Vec3d getShuttlePoint(float var1, double var2) {
      Vec3d var4 = this.target.getPos();
      double var5 = Math.toRadians(var1);
      double var7 = -Math.sin(var5);
      double var9 = Math.cos(var5);
      return var4.add(var7 * var2, 0.0, var9 * var2);
   }

   private Vec3d getChasePoint() {
      Vec3d var1 = this.target.getPos();
      double var2 = this.killLocked() ? this.followDist.getValue() : this.backDist.getValue();
      return var1.add(-this.lastEnemyVelX * var2, 0.0, -this.lastEnemyVelZ * var2);
   }

   private float yawToPoint(Vec3d var1) {
      double var2 = var1.x - mc.player.getX();
      double var4 = var1.z - mc.player.getZ();
      return (float)Math.toDegrees(Math.atan2(var4, var2)) - 90.0F;
   }

   private boolean isPathClear(float var1) {
      if (mc.world != null && mc.player != null) {
         double var2 = Math.toRadians(var1);
         double var4 = -Math.sin(var2);
         double var6 = Math.cos(var2);
         double var8 = mc.player.getY();
         double var10 = mc.player.getX();
         double var12 = mc.player.getZ();
         double var14 = this.avoidDistance.getValue();
         Mutable var16 = new Mutable();

         for (double var17 = 0.6; var17 <= var14; var17 += 0.4) {
            int var19 = (int)Math.floor(var10 + var4 * var17);
            int var20 = (int)Math.floor(var12 + var6 * var17);

            for (int var21 = (int)Math.floor(var8 + 0.4); var21 <= (int)Math.floor(var8 + 1.8); var21++) {
               var16.set(var19, var21, var20);
               if (mc.world.getBlockState(var16).isSolidBlock(mc.world, var16)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private PlayerEntity findTarget() {
      PlayerEntity var1 = null;
      double var2 = this.trackRange.getValue() * this.trackRange.getValue();

      for (PlayerEntity var5 : mc.world.getPlayers()) {
         double var6;
         if (this.isTargetValid(var5) && (var6 = mc.player.squaredDistanceTo(var5)) <= var2) {
            var2 = var6;
            var1 = var5;
         }
      }

      return var1;
   }

   private boolean isTargetValid(PlayerEntity var1) {
      if (var1 == null || var1 == mc.player || !var1.isAlive() || var1.getHealth() <= 0.0F || var1.isSpectator()) {
         return false;
      } else if (this.ignoreFriend.getValue() && Alien.FRIEND != null && Alien.FRIEND.isFriend(var1)) {
         return false;
      } else {
         String var2 = this.targetName.getValue().trim();
         String var3 = var1.getName().getString();
         return (var2.isEmpty() || var2.equalsIgnoreCase(var3)) && mc.player.squaredDistanceTo(var1) <= this.trackRange.getValue() * this.trackRange.getValue();
      }
   }

   private Vec3d getAimPosition(PlayerEntity var1) {
      Vec3d var2 = var1.getPos().add(0.0, 1.0, 0.0);
      Vec3d var3 = var1.getVelocity();
      double var4 = this.predict.getValue();
      return var2.add(var3.x * var4, var3.y * Math.min(1.2, var4), var3.z * var4);
   }
}
