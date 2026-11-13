package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

public class Follower extends Module {
   public static Follower INSTANCE;
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 160.0, 10.0, 512.0, 1.0));
   private final SliderSetting heightOffset = this.add(new SliderSetting("HeightOffset", 0.0, -4.0, 4.0, 0.1));
   private final SliderSetting predict = this.add(new SliderSetting("Predict", 1.4, 0.0, 10.0, 0.1));
   private final SliderSetting yawStep = this.add(new SliderSetting("YawStep", 10.0, 1.0, 180.0, 1.0));
   private final SliderSetting pitchStep = this.add(new SliderSetting("PitchStep", 4.0, 1.0, 90.0, 1.0));
   private final SliderSetting minDistance = this.add(new SliderSetting("MinDistance", 2.8, 0.0, 10.0, 0.1));
   private final BooleanSetting autoEnableElytraFly = this.add(new BooleanSetting("AutoEnableElytraFly", true));
   private final BooleanSetting requireElytraFly = this.add(new BooleanSetting("RequireElytraFly", true));
   private final BooleanSetting onlyControl = this.add(new BooleanSetting("OnlyControlMode", true));
   private final BooleanSetting predictMotion = this.add(new BooleanSetting("PredictMotion", true));
   private final BooleanSetting faceTarget = this.add(new BooleanSetting("FaceTarget", true));
   private final BooleanSetting lockPitch = this.add(new BooleanSetting("LockPitch", true));
   private final BooleanSetting stopWhenClose = this.add(new BooleanSetting("StopWhenClose", true));
   private final BooleanSetting ignoreCreative = this.add(new BooleanSetting("IgnoreCreative", true));
   private final BooleanSetting ignoreInvisible = this.add(new BooleanSetting("IgnoreInvisible", false));
   private final BooleanSetting ignoreFriend = this.add(new BooleanSetting("IgnoreFriend", true));
   private final BooleanSetting avoidBlocks = this.add(new BooleanSetting("AvoidBlocks", true));
   private final SliderSetting avoidDistance = this.add(new SliderSetting("AvoidDistance", 3.0, 1.0, 8.0, 0.1, this.avoidBlocks::getValue));
   private final SliderSetting avoidStep = this.add(new SliderSetting("AvoidStep", 15.0, 5.0, 90.0, 1.0, this.avoidBlocks::getValue));
   private final SliderSetting avoidMaxAngle = this.add(new SliderSetting("AvoidMaxAngle", 55.0, 10.0, 180.0, 1.0, this.avoidBlocks::getValue));
   private final BooleanSetting verticalFollow = this.add(new BooleanSetting("VerticalFollow", true));
   private final SliderSetting verticalOffset = this.add(new SliderSetting("VerticalOffset", 1.0, 0.0, 2.5, 0.1, this.verticalFollow::getValue));
   private final SliderSetting verticalStrength = this.add(new SliderSetting("VerticalStrength", 0.05, 0.01, 1.0, 0.01, this.verticalFollow::getValue));
   private final SliderSetting maxUpVertical = this.add(new SliderSetting("MaxUpVertical", 0.1, 0.01, 1.0, 0.01, this.verticalFollow::getValue));
   private final SliderSetting maxDownVertical = this.add(new SliderSetting("MaxDownVertical", 0.24, 0.01, 1.5, 0.01, this.verticalFollow::getValue));
   private final SliderSetting verticalDeadZone = this.add(new SliderSetting("VerticalDeadZone", 1.0, 0.0, 3.0, 0.1, this.verticalFollow::getValue));
   private final BooleanSetting orbitEnable = this.add(new BooleanSetting("OrbitEnable", true));
   private final SliderSetting orbitRadius = this.add(new SliderSetting("OrbitRadius", 3.0, 1.0, 8.0, 0.1, this.orbitEnable::getValue));
   private final SliderSetting orbitYawOffset = this.add(new SliderSetting("OrbitYawOffset", 16.0, 0.0, 90.0, 1.0, this.orbitEnable::getValue));
   private final SliderSetting orbitSwitchDistance = this.add(new SliderSetting("OrbitSwitchDistance", 2.0, 0.5, 6.0, 0.1, this.orbitEnable::getValue));
   private PlayerEntity target;
   private float rotYaw;
   private float rotPitch;
   private float driveYaw;
   private double verticalDrive;
   private boolean orbitLeft = true;

   public Follower() {
      super("Follower", Module.Category.Combat);
      this.setChinese("\u53cd\u82cd\u8747\u8ffd\u4eba");
      INSTANCE = this;
      this.ignoreInvisible.setChinese("\u5ffd\u7565\u9690\u8eab");
      this.orbitSwitchDistance.setChinese("\u73af\u7ed5\u5207\u6362\u8ddd\u79bb");
      this.requireElytraFly.setChinese("\u9700\u8981\u9798\u7fc5\u98de\u884c");
      this.heightOffset.setChinese("\u9ad8\u5ea6\u504f\u79fb");
      this.maxUpVertical.setChinese("\u6700\u5927\u4e0a\u5347\u901f\u5ea6");
      this.predictMotion.setChinese("\u9884\u6d4b\u79fb\u52a8");
      this.verticalStrength.setChinese("\u5782\u76f4\u529b\u5ea6");
      this.avoidDistance.setChinese("\u907f\u969c\u8ddd\u79bb");
      this.minDistance.setChinese("\u6700\u5c0f\u8ddd\u79bb");
      this.pitchStep.setChinese("\u5782\u76f4\u8f6c\u5411\u6b65\u957f");
      this.ignoreFriend.setChinese("\u5ffd\u7565\u597d\u53cb");
      this.maxDownVertical.setChinese("\u6700\u5927\u4e0b\u964d\u901f\u5ea6");
      this.yawStep.setChinese("\u6c34\u5e73\u8f6c\u5411\u6b65\u957f");
      this.autoEnableElytraFly.setChinese("\u81ea\u52a8\u5f00\u542f\u9798\u7fc5\u98de\u884c");
      this.lockPitch.setChinese("\u9501\u5b9a\u4fef\u4ef0");
      this.verticalFollow.setChinese("\u5782\u76f4\u8ddf\u968f");
      this.verticalDeadZone.setChinese("\u5782\u76f4\u6b7b\u533a");
      this.ignoreCreative.setChinese("\u5ffd\u7565\u521b\u9020");
      this.onlyControl.setChinese("\u4ec5\u63a7\u5236\u6a21\u5f0f");
      this.orbitEnable.setChinese("\u73af\u7ed5");
      this.orbitRadius.setChinese("\u73af\u7ed5\u534a\u5f84");
      this.verticalOffset.setChinese("\u5782\u76f4\u504f\u79fb");
      this.avoidMaxAngle.setChinese("\u6700\u5927\u907f\u969c\u89d2\u5ea6");
      this.predict.setChinese("\u79fb\u52a8\u9884\u6d4b");
      this.faceTarget.setChinese("\u9762\u5411\u76ee\u6807");
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.stopWhenClose.setChinese("\u63a5\u8fd1\u65f6\u505c\u6b62");
      this.orbitYawOffset.setChinese("\u73af\u7ed5\u89d2\u5ea6\u504f\u79fb");
      this.avoidBlocks.setChinese("\u907f\u8ba9\u65b9\u5757");
      this.avoidStep.setChinese("\u907f\u969c\u6b65\u957f");
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.verticalDrive = 0.0;
      if (!nullCheck()) {
         this.rotYaw = mc.player.getYaw();
         this.rotPitch = mc.player.getPitch();
         this.driveYaw = mc.player.getYaw();
      }
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.verticalDrive = 0.0;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target == null) {
            this.verticalDrive = 0.0;
         } else {
            if (this.autoEnableElytraFly.getValue() && ElytraFly.INSTANCE != null && !ElytraFly.INSTANCE.isOn()) {
               ElytraFly.INSTANCE.enable();
            }

            if (!this.canControl()) {
               this.verticalDrive = 0.0;
            } else {
               Vec3d var4 = this.getAimPos(this.target);
               Vec3d var5 = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
               Vec3d var6 = var4.subtract(var5);
               double var7 = var6.x;
               double var9 = var6.y;
               double var11 = var6.z;
               double var13 = Math.sqrt(var7 * var7 + var11 * var11);
               float var15 = (float)Math.toDegrees(Math.atan2(var11, var7)) - 90.0F;
               float var16 = (float)(-Math.toDegrees(Math.atan2(var9, var13)));
               this.rotYaw = this.stepAngle(this.rotYaw, var15, this.yawStep.getValueFloat());
               if (this.lockPitch.getValue()) {
                  this.rotPitch = this.stepAngle(this.rotPitch, var16, this.pitchStep.getValueFloat());
               }

               this.driveYaw = this.resolveDriveYaw(this.rotYaw);
               double var17 = var4.y - this.verticalOffset.getValue();
               double var19 = var17 - var5.y;
               double var2;
               this.verticalDrive = this.verticalFollow.getValue()
                  ? (
                     Math.abs(var19) <= this.verticalDeadZone.getValue()
                        ? 0.0
                        : (
                           (var2 = var19 * this.verticalStrength.getValue()) > 0.0
                              ? Math.min(var2, this.maxUpVertical.getValue())
                              : Math.max(var2, -this.maxDownVertical.getValue())
                        )
                  )
                  : 0.0;
            }
         }
      }
   }

   @EventListener(
      priority = -9999
   )
   public void onRotate(UpdateRotateEvent var1) {
      if (!nullCheck() && this.target != null && this.faceTarget.getValue()) {
         if (this.canControl()) {
            var1.setYaw(this.rotYaw);
            if (this.lockPitch.getValue()) {
               var1.setPitch(this.rotPitch);
            }
         }
      }
   }

   public boolean shouldDrive() {
      if (!this.isOn()) {
         return false;
      } else if (nullCheck()) {
         return false;
      } else if (this.target == null || !this.isTargetValid(this.target)) {
         return false;
      } else {
         return !this.canControl() ? false : !this.stopWhenClose.getValue() || !(mc.player.distanceTo(this.target) <= this.minDistance.getValue());
      }
   }

   public boolean shouldVerticalDrive() {
      return this.shouldDrive() && this.verticalFollow.getValue();
   }

   public float getDriveYaw() {
      return this.driveYaw;
   }

   public float getDrivePitch() {
      return this.rotPitch;
   }

   public double getVerticalDrive() {
      return this.verticalDrive;
   }

   private float resolveDriveYaw(float var1) {
      float var2 = var1;
      if (this.orbitEnable.getValue() && this.target != null) {
         double var5 = mc.player.distanceTo(this.target);
         if (var5 <= this.orbitRadius.getValue()) {
            var2 = var1 + (this.orbitLeft ? this.orbitYawOffset.getValueFloat() : -this.orbitYawOffset.getValueFloat());
         }

         if (var5 <= this.orbitSwitchDistance.getValue()) {
            boolean var7 = this.orbitLeft = !this.orbitLeft;
         }
      }

      if (!this.avoidBlocks.getValue()) {
         return var2;
      } else if (this.isPathClear(var2, this.avoidDistance.getValue())) {
         return var2;
      } else {
         float var3 = this.avoidStep.getValueFloat();
         float var4 = this.avoidMaxAngle.getValueFloat();

         for (float var8 = var3; var8 <= var4; var8 += var3) {
            float var6 = var2 + var8;
            if (this.isPathClear(var6, this.avoidDistance.getValue())) {
               return var6;
            }

            float var9 = var2 - var8;
            if (this.isPathClear(var9, this.avoidDistance.getValue())) {
               return var9;
            }
         }

         return var2;
      }
   }

   private boolean isPathClear(float var1, double var2) {
      if (mc.world != null && mc.player != null) {
         double var4 = Math.toRadians(var1);
         double var6 = -Math.sin(var4);
         double var8 = Math.cos(var4);
         double var10 = mc.player.getY() + 0.6;
         double var12 = 0.6;
         double var14 = var12 * 0.5;

         for (double var16 = 0.6; var16 <= var2; var16 += 0.4) {
            double var20 = mc.player.getX() + var6 * var16;
            double var18;
            Box var22 = new Box(var20 - var14, var10 - 0.6, (var18 = mc.player.getZ() + var8 * var16) - var14, var20 + var14, var10 + 1.2, var18 + var14);
            if (this.hasSolidCollision(var22)) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private boolean hasSolidCollision(Box var1) {
      int var2 = MathHelper.floor(var1.minX);
      int var3 = MathHelper.floor(var1.maxX);
      int var4 = MathHelper.floor(var1.minY);
      int var5 = MathHelper.floor(var1.maxY);
      int var6 = MathHelper.floor(var1.minZ);
      int var7 = MathHelper.floor(var1.maxZ);
      Mutable var8 = new Mutable();

      for (int var9 = var2; var9 <= var3; var9++) {
         for (int var10 = var4; var10 <= var5; var10++) {
            for (int var11 = var6; var11 <= var7; var11++) {
               var8.set(var9, var10, var11);
               if (!mc.world.isAir(var8) && !mc.world.getBlockState(var8).getCollisionShape(mc.world, var8).isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean canControl() {
      if (ElytraFly.INSTANCE == null) {
         return !this.requireElytraFly.getValue();
      } else if (this.requireElytraFly.getValue() && !ElytraFly.INSTANCE.isOn()) {
         return false;
      } else {
         return this.onlyControl.getValue() && ElytraFly.INSTANCE.mode.getValue() != ElytraFly.Mode.Control ? false : ElytraFly.INSTANCE.isFallFlying();
      }
   }

   private PlayerEntity findTarget() {
      PlayerEntity var1 = null;
      double var2 = this.targetRange.getValue() * this.targetRange.getValue();

      for (PlayerEntity var5 : mc.world.getPlayers()) {
         double var6;
         if (this.isTargetValid(var5) && !((var6 = mc.player.squaredDistanceTo(var5)) > var2)) {
            var2 = var6;
            var1 = var5;
         }
      }

      return var1;
   }

   private boolean isTargetValid(PlayerEntity var1) {
      if (var1 == null) {
         return false;
      } else if (var1 == mc.player) {
         return false;
      } else if (!var1.isAlive() || var1.getHealth() <= 0.0F) {
         return false;
      } else if (var1.isSpectator()) {
         return false;
      } else if (this.ignoreCreative.getValue() && var1.getAbilities().creativeMode) {
         return false;
      } else if (this.ignoreInvisible.getValue() && var1.isInvisible()) {
         return false;
      } else if (this.ignoreFriend.getValue() && this.isFriend(var1)) {
         return false;
      } else if (mc.player.squaredDistanceTo(var1) > this.targetRange.getValue() * this.targetRange.getValue()) {
         return false;
      } else {
         String var2 = var1.getName().getString();
         return var2 != null && !var2.isEmpty();
      }
   }

   private boolean isFriend(PlayerEntity var1) {
      return Alien.FRIEND != null && Alien.FRIEND.isFriend(var1);
   }

   private Vec3d getAimPos(PlayerEntity var1) {
      Vec3d var2 = var1.getPos().add(0.0, this.heightOffset.getValue(), 0.0);
      if (this.predictMotion.getValue()) {
         double var3 = this.predict.getValue();
         Vec3d var5 = var1.getVelocity();
         var2 = var2.add(var5.x * var3, var5.y * Math.min(1.2, var3), var5.z * var3);
      }

      return var2;
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
}
