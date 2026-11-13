package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.FireworkShooterRotationEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.misc.Teams;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class GrimChase extends Module {
   public static GrimChase INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 96.0, 8.0, 256.0, 1.0));
   private final SliderSetting speed = this.add(new SliderSetting("Speed", 1.6, 0.1, 6.0, 0.1));
   private final SliderSetting retargetRange = this.add(new SliderSetting("RetargetRange", 128.0, 8.0, 256.0, 1.0));
   private final SliderSetting flightGraceMs = this.add(new SliderSetting("FlightGraceMs", 600.0, 0.0, 3000.0, 50.0));
   private final BooleanSetting autoStartElytra = this.add(new BooleanSetting("AutoStartElytra", false));
   private final BooleanSetting boostRocket = this.add(new BooleanSetting("UseFirework", false));
   private final BooleanSetting lockTarget = this.add(new BooleanSetting("LockTarget", true));
   private final BooleanSetting ignoreFriends = this.add(new BooleanSetting("IgnoreFriends", true));
   private final BooleanSetting ignoreTeams = this.add(new BooleanSetting("IgnoreTeams", true));
   private final Timer fireworkTimer = new Timer();
   private final Timer flightTimer = new Timer();
   private PlayerEntity target;
   private float chaseYaw;
   private float chasePitch;

   public GrimChase() {
      super("GrimChase", Module.Category.Movement);
      this.setChinese("Grim\u8ffd\u4eba");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.target == null ? null : this.target.getName().getString();
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.fireworkTimer.reset();
      this.flightTimer.reset();
      super.onDisable();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (mc.player.isFallFlying()) {
            this.flightTimer.reset();
         } else if (!this.isFlightStateActive()) {
            if (this.autoStartElytra.getValue()) {
               ElytraFly.recastElytra(mc.player);
            }

            return;
         }

         this.target = this.resolveTarget();
         if (this.target != null) {
            Vec3d aimPos = this.target.getPos().add(0.0, this.target.getStandingEyeHeight() * 0.9, 0.0);
            float[] rot = RotationManager.getRotation(aimPos);
            this.chaseYaw = rot[0];
            this.chasePitch = rot[1];
            Vec3d dir = rotationVector(this.chaseYaw, this.chasePitch);
            double s = this.speed.getValue();
            double vx = dir.x * s;
            double vy = dir.y * s;
            double vz = dir.z * s;
            mc.player.setVelocity(vx, vy, vz);
            double nx = mc.player.getX() + vx;
            double ny = mc.player.getY() + vy;
            double nz = mc.player.getZ() + vz;
            mc.getNetworkHandler().sendPacket(new Full(nx, ny, nz, this.chaseYaw, this.chasePitch, mc.player.isOnGround()));
            if (this.boostRocket.getValue()
               && ElytraFly.INSTANCE != null
               && ElytraFly.INSTANCE.isOn()
               && this.fireworkTimer.passed(1200L)
               && mc.player.squaredDistanceTo(this.target) >= 64.0) {
               ElytraFly.INSTANCE.off();
               this.fireworkTimer.reset();
            }
         }
      } else {
         this.target = null;
      }
   }

   @EventListener
   public void onFireworkShooterRotation(FireworkShooterRotationEvent event) {
      if (this.isOn() && mc.player != null) {
         if (event.shooter == mc.player) {
            if (this.target != null) {
               event.yaw = this.chaseYaw;
               event.pitch = this.chasePitch;
               event.cancel();
            }
         }
      }
   }

   private PlayerEntity findTarget(float range) {
      PlayerEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (PlayerEntity p : mc.world.getPlayers()) {
         if (this.isValidTarget(p, range)) {
            double d = mc.player.squaredDistanceTo(p);
            if (d < bestDist) {
               best = p;
               bestDist = d;
            }
         }
      }

      return best;
   }

   private PlayerEntity resolveTarget() {
      if (this.lockTarget.getValue() && this.isValidTarget(this.target, this.retargetRange.getValueFloat())) {
         return this.target;
      } else {
         return !this.isValidTarget(this.target, this.range.getValueFloat()) ? this.findTarget(this.range.getValueFloat()) : this.target;
      }
   }

   private boolean isValidTarget(PlayerEntity player, float range) {
      if (player == null || player == mc.player || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
         return false;
      } else if (this.ignoreFriends.getValue() && Alien.FRIEND != null && Alien.FRIEND.isFriend(player)) {
         return false;
      } else {
         return this.ignoreTeams.getValue() && Teams.isSameTeam(player) ? false : mc.player.squaredDistanceTo(player) <= range * range;
      }
   }

   private boolean isFlightStateActive() {
      if (mc.player.isFallFlying()) {
         return true;
      } else {
         return ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying()
            ? true
            : !this.flightTimer.passedMs(this.flightGraceMs.getValue());
      }
   }

   private static Vec3d rotationVector(float yaw, float pitch) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }
}
