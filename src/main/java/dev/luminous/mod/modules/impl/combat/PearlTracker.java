package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.PearlTrajectoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.math.Vec3d;

public class PearlTracker extends Module {
   public static PearlTracker INSTANCE;
   private static long combatSwapLockUntil;
   private final SliderSetting minHealth = this.add(new SliderSetting("MinHealth", 10.0, 0.0, 36.0, 0.5));
   private final SliderSetting maxTrackRange = this.add(new SliderSetting("MaxTrackRange", 192.0, 10.0, 320.0, 1.0));
   private final SliderSetting landingTolerance = this.add(new SliderSetting("LandingTolerance", 4.0, 1.0, 10.0, 0.5));
   private final SliderSetting timeWeight = this.add(new SliderSetting("TimeWeight", 0.35, 0.0, 2.0, 0.05));
   private final Set<Integer> handledPearls = new HashSet<>();
   private EnderPearlEntity trackedPearl;
   private String state = "Idle";

   public PearlTracker() {
      super("PearlTracker", "Follows enemy ender pearl landings with synchronized trajectory prediction", Module.Category.Combat);
      this.setChinese("\u73cd\u73e0\u8ffd\u8e2a");
      this.minHealth.setChinese("\u6700\u4f4e\u8840\u91cf");
      this.maxTrackRange.setChinese("\u6700\u5927\u8ffd\u8e2a\u8ddd\u79bb");
      this.landingTolerance.setChinese("\u843d\u70b9\u8bef\u5dee");
      this.timeWeight.setChinese("\u65f6\u95f4\u540c\u6b65\u6743\u91cd");
      INSTANCE = this;
   }

   public static boolean isCombatSwapLocked() {
      return System.currentTimeMillis() < combatSwapLockUntil || AutoPearl.throwing;
   }

   @Override
   public void onEnable() {
      this.handledPearls.clear();
      this.trackedPearl = null;
      this.state = "Idle";
   }

   @Override
   public void onDisable() {
      this.trackedPearl = null;
      this.state = "Idle";
   }

   @Override
   public String getInfo() {
      return this.state;
   }

   @EventListener(
      priority = 1000
   )
   public void onTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck()) {
         this.handledPearls.removeIf(id -> mc.world.getEntityById(id) == null);
         if (this.canFollow()) {
            if (!this.isTrackable(this.trackedPearl)) {
               this.trackedPearl = this.findNewestEnemyPearl();
            }

            if (this.trackedPearl == null) {
               this.state = "Ready";
            } else {
               this.follow(this.trackedPearl);
            }
         }
      }
   }

   @EventListener(
      priority = 1000
   )
   public void onEntitySpawned(EntitySpawnedEvent event) {
      if (!nullCheck() && event.getEntity() instanceof EnderPearlEntity pearl) {
         if (this.canFollow() && this.isTrackable(pearl)) {
            this.trackedPearl = pearl;
            this.follow(pearl);
         }
      }
   }

   private void follow(EnderPearlEntity pearl) {
      if (pearl != null && !this.handledPearls.contains(pearl.getId())) {
         if (this.isWaterPearl(pearl)) {
            this.state = "Water";
            if (this.trackedPearl == pearl) {
               this.trackedPearl = null;
            }
         } else {
            this.state = "Target";
            PearlTrajectoryUtil.Prediction enemyPrediction = PearlTrajectoryUtil.predict(pearl);
            PearlTracker.ThrowPlan plan = this.findImmediatePlan(pearl, enemyPrediction);
            if (plan == null) {
               this.state = "NoPath";
            } else {
               combatSwapLockUntil = System.currentTimeMillis() + 200L;
               if (AutoPearl.INSTANCE != null) {
                  AutoPearl.INSTANCE.throwPearl(plan.yaw(), plan.pitch());
                  this.state = "Thrown";
                  this.handledPearls.add(pearl.getId());
                  this.trackedPearl = null;
               } else {
                  this.state = "SwapFail";
               }
            }
         }
      }
   }

   private boolean canFollow() {
      if (EntityUtil.getHealth(mc.player) <= this.minHealth.getValueFloat()) {
         this.state = "LowHP";
         return false;
      } else if (mc.currentScreen != null || mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
         this.state = "Screen";
         return false;
      } else if (AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isOn() && AutoRegear.INSTANCE.getCurrentShulker() != null) {
         this.state = "Regear";
         return false;
      } else if (AntiRegear.INSTANCE != null && AntiRegear.INSTANCE.isControllingShulker()) {
         this.state = "Regear";
         return false;
      } else if (AutoCrystalPlusPlus.INSTANCE != null && AutoCrystalPlusPlus.INSTANCE.isRefillPaused()) {
         this.state = "Refill";
         return false;
      } else {
         boolean hasPearl = mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)
            || mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)
            || InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) != -1;
         if (!hasPearl) {
            this.state = "NoPearl";
         }

         return hasPearl;
      }
   }

   private EnderPearlEntity findNewestEnemyPearl() {
      EnderPearlEntity newest = null;

      for (EnderPearlEntity pearl : mc.world
         .getEntitiesByClass(EnderPearlEntity.class, mc.player.getBoundingBox().expand(this.maxTrackRange.getValue()), Entity::isAlive)) {
         if (this.isTrackable(pearl) && (newest == null || pearl.age < newest.age)) {
            newest = pearl;
         }
      }

      return newest;
   }

   private boolean isTrackable(EnderPearlEntity pearl) {
      if (pearl == null || !pearl.isAlive() || this.handledPearls.contains(pearl.getId())) {
         return false;
      } else if (this.isWaterPearl(pearl)) {
         return false;
      } else {
         PlayerEntity owner = PearlTrajectoryUtil.resolveOwner(pearl);
         return owner == null
            ? false
            : owner != mc.player
               && owner.isAlive()
               && !Alien.FRIEND.isFriend(owner)
               && mc.player.squaredDistanceTo(pearl) <= this.maxTrackRange.getValue() * this.maxTrackRange.getValue();
      }
   }

   private boolean isWaterPearl(EnderPearlEntity pearl) {
      return pearl.isTouchingWater() || pearl.getWorld().getFluidState(pearl.getBlockPos()).isIn(FluidTags.WATER);
   }

   private PearlTracker.ThrowPlan findImmediatePlan(EnderPearlEntity pearl, PearlTrajectoryUtil.Prediction enemyPrediction) {
      PearlTracker.ThrowPlan directionFallback = this.createDirectionFallback(pearl, enemyPrediction);
      if (enemyPrediction != null && enemyPrediction.collided()) {
         Vec3d landing = enemyPrediction.landingPos();
         int targetTick = enemyPrediction.ticks();
         double tolerance = this.landingTolerance.getValue() + 1.0;
         float landingYaw = this.yawTo(landing);
         PearlTracker.ThrowPlan best = this.searchPlans(landing, targetTick, landingYaw, -85.0F, 85.0F, 2.0F);
         if (best != null && best.landingError() <= tolerance) {
            PearlTracker.ThrowPlan fine = this.searchPlans(
               landing, targetTick, landingYaw, Math.max(-85.0F, best.pitch() - 3.0F), Math.min(85.0F, best.pitch() + 3.0F), 0.25F
            );
            if (fine != null) {
               best = fine;
            }

            return this.chooseDirectionOrLanding(directionFallback, best);
         } else {
            best = this.searchFullYawCircle(landing, targetTick, landingYaw, best);
            if (best != null && best.landingError() <= Math.max(tolerance, 16.0)) {
               return this.chooseDirectionOrLanding(directionFallback, best);
            } else {
               PearlTracker.ThrowPlan nearbyBest = best;

               for (int radius = 2; radius <= 6; radius += 2) {
                  for (int angle = 0; angle < 360; angle += 45) {
                     double radians = Math.toRadians(angle);
                     Vec3d nearby = landing.add(Math.cos(radians) * radius, 0.0, Math.sin(radians) * radius);
                     PearlTracker.ThrowPlan candidate = this.searchPlans(nearby, targetTick, this.yawTo(nearby), -80.0F, 80.0F, 6.0F);
                     nearbyBest = this.betterPlan(nearbyBest, candidate);
                  }
               }

               return this.chooseDirectionOrLanding(directionFallback, nearbyBest);
            }
         }
      } else {
         return directionFallback;
      }
   }

   private PearlTracker.ThrowPlan chooseDirectionOrLanding(PearlTracker.ThrowPlan direction, PearlTracker.ThrowPlan landing) {
      if (direction == null) {
         return landing;
      } else if (landing == null) {
         return direction;
      } else {
         return this.planScore(landing) + 1.0 < this.planScore(direction) ? landing : direction;
      }
   }

   private PearlTracker.ThrowPlan createDirectionFallback(EnderPearlEntity pearl, PearlTrajectoryUtil.Prediction enemyPrediction) {
      Vec3d launchVelocity = this.reconstructLaunchVelocity(pearl);
      PlayerEntity owner = PearlTrajectoryUtil.resolveOwner(pearl);
      if (owner != null) {
         launchVelocity = launchVelocity.subtract(owner.getVelocity());
      }

      if (launchVelocity.lengthSquared() < 1.0E-4) {
         return null;
      } else {
         double horizontal = Math.sqrt(launchVelocity.x * launchVelocity.x + launchVelocity.z * launchVelocity.z);
         float yaw = (float)Math.toDegrees(Math.atan2(-launchVelocity.x, launchVelocity.z));
         float pitch = (float)(-Math.toDegrees(Math.atan2(launchVelocity.y, horizontal)));
         pitch = Math.max(-89.0F, Math.min(89.0F, pitch));
         PearlTrajectoryUtil.Prediction localPrediction = PearlTrajectoryUtil.predictThrow(mc.player, yaw, pitch);
         int targetTick = enemyPrediction == null ? Math.max(1, pearl.age + 20) : enemyPrediction.ticks();
         int ticks = localPrediction == null ? targetTick : localPrediction.ticks();
         double error = enemyPrediction != null && enemyPrediction.collided() && localPrediction != null
            ? distanceToTarget(localPrediction.landingPos(), enemyPrediction.landingPos())
            : 0.0;
         return new PearlTracker.ThrowPlan(yaw, pitch, ticks, error, targetTick);
      }
   }

   private Vec3d reconstructLaunchVelocity(EnderPearlEntity pearl) {
      Vec3d velocity = pearl.getVelocity();
      int ticks = Math.max(0, Math.min(pearl.age, 12));

      for (int i = 0; i < ticks; i++) {
         velocity = new Vec3d(velocity.x / 0.99, (velocity.y + 0.03) / 0.99, velocity.z / 0.99);
      }

      return velocity;
   }

   private PearlTracker.ThrowPlan searchFullYawCircle(Vec3d target, int targetTick, float centerYaw, PearlTracker.ThrowPlan currentBest) {
      PearlTracker.ThrowPlan best = currentBest;
      float[] offsets = new float[]{-165.0F, -135.0F, -105.0F, -75.0F, -45.0F, -25.0F, -12.0F, 12.0F, 25.0F, 45.0F, 75.0F, 105.0F, 135.0F, 165.0F, 180.0F};

      for (float offset : offsets) {
         float yaw = centerYaw + offset;
         PearlTracker.ThrowPlan candidate = this.searchPlans(target, targetTick, yaw, -80.0F, 80.0F, 6.0F);
         best = this.betterPlan(best, candidate);
      }

      return best;
   }

   private PearlTracker.ThrowPlan betterPlan(PearlTracker.ThrowPlan first, PearlTracker.ThrowPlan second) {
      if (second == null) {
         return first;
      } else if (first == null) {
         return second;
      } else {
         return this.planScore(second) < this.planScore(first) ? second : first;
      }
   }

   private double planScore(PearlTracker.ThrowPlan plan) {
      int arrivalTick = plan.ticks() + this.getOutgoingDelayTicks();
      int lateTicks = Math.max(0, arrivalTick - plan.targetTick());
      int earlyTicks = Math.max(0, plan.targetTick() - arrivalTick);
      return plan.landingError() * 3.0 + lateTicks * 2.0 + earlyTicks * 0.05;
   }

   private PearlTracker.ThrowPlan searchPlans(Vec3d target, int targetTick, float yaw, float minPitch, float maxPitch, float step) {
      PearlTracker.ThrowPlan best = null;
      double bestScore = Double.MAX_VALUE;
      float pitch = minPitch;

      while (pitch <= maxPitch + 1.0E-4F) {
         PearlTrajectoryUtil.Prediction prediction = PearlTrajectoryUtil.predictThrow(mc.player, yaw, pitch);
         if (prediction != null && prediction.collided()) {
            double landingError = distanceToTarget(prediction.landingPos(), target);
            int arrivalTick = prediction.ticks() + this.getOutgoingDelayTicks();
            int lateTicks = Math.max(0, arrivalTick - targetTick);
            int earlyTicks = Math.max(0, targetTick - arrivalTick);
            double score = landingError * 3.0 + lateTicks * (6.0 + this.timeWeight.getValue()) + earlyTicks * Math.max(0.05, this.timeWeight.getValue() * 0.05);
            if (score < bestScore) {
               bestScore = score;
               best = new PearlTracker.ThrowPlan(yaw, pitch, prediction.ticks(), landingError, targetTick);
            }
         }

         pitch += step;
      }

      return best;
   }

   private float yawTo(Vec3d target) {
      double dx = target.x - mc.player.getX();
      double dz = target.z - mc.player.getZ();
      return (float)Math.toDegrees(Math.atan2(-dx, dz));
   }

   private static double distanceToTarget(Vec3d first, Vec3d second) {
      double dx = first.x - second.x;
      double dy = (first.y - second.y) * 0.65;
      double dz = first.z - second.z;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private int getOutgoingDelayTicks() {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         int latency = entry == null ? 100 : entry.getLatency();
         return Math.max(1, Math.min(5, (int)Math.ceil(latency / 100.0)));
      } else {
         return 1;
      }
   }

   private static double horizontalDistance(Vec3d first, Vec3d second) {
      double dx = first.x - second.x;
      double dz = first.z - second.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private record ThrowPlan(float yaw, float pitch, int ticks, double landingError, int targetTick) {
   }
}
