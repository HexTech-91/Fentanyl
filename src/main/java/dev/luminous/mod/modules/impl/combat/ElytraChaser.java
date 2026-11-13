package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class ElytraChaser extends Module {
   public static ElytraChaser INSTANCE;
   private final StringSetting targetName = this.add(new StringSetting("TargetName", ""));
   private final SliderSetting range = this.add(new SliderSetting("Range", 64.0, 4.0, 256.0, 1.0));
   private final BooleanSetting noFly = this.add(new BooleanSetting("NoFly", false));
   private final BooleanSetting verticalFollow = this.add(new BooleanSetting("VerticalFollow", true));
   private final BooleanSetting useFirework = this.add(new BooleanSetting("UseFirework", false));
   private final BooleanSetting ignoreFriend = this.add(new BooleanSetting("IgnoreFriend", true));
   private final BooleanSetting autoEnableElytraFly = this.add(new BooleanSetting("AutoEnableElytraFly", true));
   private final BooleanSetting requireElytraFly = this.add(new BooleanSetting("RequireElytraFly", true));
   private final BooleanSetting faceTarget = this.add(new BooleanSetting("FaceTarget", true));
   private final BooleanSetting lockPitch = this.add(new BooleanSetting("LockPitch", true));
   private final SliderSetting predict = this.add(new SliderSetting("Predict", 1.0, 0.0, 5.0, 0.1));
   private final SliderSetting minDistance = this.add(new SliderSetting("MinDistance", 2.5, 0.0, 10.0, 0.1));
   private final SliderSetting verticalStrength = this.add(new SliderSetting("VerticalStrength", 0.08, 0.01, 1.0, 0.01));
   private final SliderSetting maxVertical = this.add(new SliderSetting("MaxVertical", 0.35, 0.01, 2.0, 0.01));
   private PlayerEntity target;
   private float rotationYaw;
   private float rotationPitch;
   private float driveYaw;
   private double verticalDrive;
   private final Timer fireworkTimer = new Timer();

   public ElytraChaser() {
      super("ElytraChaser", Module.Category.Combat);
      this.setChinese("\u9798\u7fc5\u8ffd\u4eba");
      this.targetName.setChinese("\u81ea\u5b9a\u4e49\u8ffd\u8e2a\u540d\u79f0");
      this.range.setChinese("\u81ea\u5b9a\u4e49\u8303\u56f4");
      this.noFly.setChinese("\u4e0d\u98de\u884c");
      this.verticalFollow.setChinese("\u662f\u5426\u4e0a\u5347");
      this.useFirework.setChinese("\u662f\u5426\u70df\u82b1");
      this.ignoreFriend.setChinese("\u5ffd\u7565\u597d\u53cb");
      this.autoEnableElytraFly.setChinese("\u81ea\u52a8\u5f00\u542f\u9798\u7fc5\u98de\u884c");
      this.requireElytraFly.setChinese("\u9700\u8981\u9798\u7fc5\u98de\u884c");
      this.faceTarget.setChinese("\u9501\u5b9a\u89c6\u89d2");
      this.lockPitch.setChinese("\u9501\u5b9a\u4fef\u4ef0");
      this.predict.setChinese("\u79fb\u52a8\u9884\u6d4b");
      this.minDistance.setChinese("\u6700\u5c0f\u8ddd\u79bb");
      this.verticalStrength.setChinese("\u4e0a\u5347\u529b\u5ea6");
      this.maxVertical.setChinese("\u6700\u5927\u4e0a\u5347\u901f\u5ea6");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.verticalDrive = 0.0;
      this.fireworkTimer.reset();
      if (!nullCheck()) {
         this.rotationYaw = mc.player.getYaw();
         this.rotationPitch = mc.player.getPitch();
         this.driveYaw = this.rotationYaw;
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
            if (!this.noFly.getValue() && this.autoEnableElytraFly.getValue() && ElytraFly.INSTANCE != null && !ElytraFly.INSTANCE.isOn()) {
               ElytraFly.INSTANCE.enable();
            }

            Vec3d var2 = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
            Vec3d var3 = this.getAimPosition(this.target);
            Vec3d var4 = var3.subtract(var2);
            double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
            float var7 = (float)Math.toDegrees(Math.atan2(var4.z, var4.x)) - 90.0F;
            float var8 = (float)(-Math.toDegrees(Math.atan2(var4.y, var5)));
            this.rotationYaw = this.stepAngle(this.rotationYaw, var7, 25.0F);
            if (this.lockPitch.getValue() || this.noFly.getValue()) {
               this.rotationPitch = this.stepAngle(this.rotationPitch, var8, 12.0F);
            }

            this.driveYaw = this.rotationYaw;
            double var9 = var3.y - var2.y;
            double var11 = this.verticalDrive = !this.noFly.getValue() && this.verticalFollow.getValue()
               ? Math.max(-this.maxVertical.getValue(), Math.min(this.maxVertical.getValue(), var9 * this.verticalStrength.getValue()))
               : 0.0;
            if (!this.noFly.getValue()
               && this.useFirework.getValue()
               && this.target.getVelocity().y > 0.03
               && this.fireworkTimer.passedMs(400.0)
               && ElytraFly.INSTANCE != null) {
               boolean var13 = ElytraFly.INSTANCE.isFallFlying();
               if (this.hasFirework() && var13) {
                  ElytraFly.INSTANCE.off();
                  this.fireworkTimer.reset();
               }
            }
         }
      }
   }

   @EventListener(
      priority = -9999
   )
   public void onRotate(UpdateRotateEvent var1) {
      if (!nullCheck() && this.target != null && this.isTargetValid(this.target) && (this.faceTarget.getValue() || this.noFly.getValue())) {
         if (this.noFly.getValue() || this.canControl()) {
            var1.setYaw(this.rotationYaw);
            if (this.lockPitch.getValue() || this.noFly.getValue()) {
               var1.setPitch(this.rotationPitch);
            }
         }
      }
   }

   public boolean shouldDrive() {
      return this.isOn()
         && !this.noFly.getValue()
         && this.target != null
         && this.isTargetValid(this.target)
         && this.canControl()
         && mc.player.distanceTo(this.target) > this.minDistance.getValue();
   }

   public boolean shouldVerticalDrive() {
      return this.shouldDrive() && this.verticalFollow.getValue();
   }

   public float getDriveYaw() {
      return this.driveYaw;
   }

   public double getVerticalDrive() {
      return this.calculateElytraVertical();
   }

   private boolean canControl() {
      if (ElytraFly.INSTANCE == null) {
         return !this.requireElytraFly.getValue();
      } else if (this.requireElytraFly.getValue() && !ElytraFly.INSTANCE.isOn()) {
         return false;
      } else {
         return ElytraFly.INSTANCE.mode.getValue() != ElytraFly.Mode.Control ? false : ElytraFly.INSTANCE.isFallFlying();
      }
   }

   private PlayerEntity findTarget() {
      PlayerEntity var1 = null;
      double var2 = this.range.getValue() * this.range.getValue();

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
         return !var2.isEmpty() && !var2.equalsIgnoreCase(var3) ? false : mc.player.squaredDistanceTo(var1) <= this.range.getValue() * this.range.getValue();
      }
   }

   private Vec3d getAimPosition(PlayerEntity var1) {
      Vec3d var2 = var1.getPos();
      Vec3d var3 = var1.getVelocity();
      double var4 = this.predict.getValue();
      return var2.add(var3.x * var4, var3.y * var4, var3.z * var4);
   }

   private float stepAngle(float var1, float var2, float var3) {
      return var2;
   }

   private double calculateElytraVertical() {
      if (this.noFly.getValue()) {
         return 0.0;
      } else if (!this.verticalFollow.getValue()) {
         return 0.0;
      } else if (this.target == null) {
         return 0.0;
      } else if (Module.mc.player == null) {
         return 0.0;
      } else {
         double var3 = this.target.getY() - Module.mc.player.getY();
         if (!(var3 <= 0.5)) {
            if (ElytraFly.INSTANCE != null) {
               double var1 = ElytraFly.INSTANCE.upFactor.getValue();
               if (Module.mc.player.getVelocity().y < 0.0) {
                  return 0.0;
               }

               return var1;
            }
         } else {
            if (!(var3 < -0.5)) {
               return 0.0;
            }

            if (ElytraFly.INSTANCE != null) {
               return -ElytraFly.INSTANCE.downFactor.getValue();
            }
         }

         return Math.max(Math.min(this.verticalStrength.getValue() * var3, this.maxVertical.getValue()), -this.maxVertical.getValue());
      }
   }

   private boolean hasFirework() {
      if (Module.mc.player != null) {
         for (int var1 = 0; var1 < 36; var1++) {
            if (Module.mc.player.getInventory().getStack(var1).isOf(Items.FIREWORK_ROCKET)) {
               return true;
            }
         }

         if (Module.mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            return true;
         }
      }

      return false;
   }

   @EventListener
   public void onMove(TravelEvent var1) {
      if (!Module.nullCheck() && !var1.isPost() && !this.noFly.getValue() && this.target != null && this.isTargetValid(this.target)) {
         ClientPlayerEntity var2 = Wrapper.mc.player;
         if (var2 != null && var2.isFallFlying()) {
            if (!(var2.distanceTo(this.target) <= this.minDistance.getValue())) {
               double var3 = ElytraFly.INSTANCE.speed.getValue();
               double var5 = Math.toRadians(this.driveYaw + 90.0F);
               double var7 = var3 * Math.cos(var5);
               double var9 = var3 * Math.sin(var5);
               MovementUtil.setMotionX(var7);
               MovementUtil.setMotionZ(var9);
               MovementUtil.setMotionY(this.getVerticalDrive());
               var1.cancel();
               var2.move(MovementType.SELF, var2.getVelocity());
            }

            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
            MovementUtil.setMotionY(0.0);
            var1.cancel();
            var2.move(MovementType.SELF, var2.getVelocity());
         }
      }
   }
}
