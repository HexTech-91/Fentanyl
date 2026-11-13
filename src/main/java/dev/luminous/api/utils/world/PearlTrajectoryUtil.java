package dev.luminous.api.utils.world;

import dev.luminous.api.utils.Wrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class PearlTrajectoryUtil implements Wrapper {
   private static final int MAX_TICKS = 300;
   private static final Map<EnderPearlEntity, PlayerEntity> INFERRED_OWNERS = new WeakHashMap<>();

   private PearlTrajectoryUtil() {
   }

   public static PearlTrajectoryUtil.Prediction predict(EnderPearlEntity pearl) {
      return pearl != null && mc.world != null ? simulate(pearl.getPos(), pearl.getVelocity(), pearl) : null;
   }

   public static PlayerEntity resolveOwner(EnderPearlEntity pearl) {
      if (pearl != null && mc.world != null) {
         if (pearl.getOwner() instanceof PlayerEntity owner) {
            return owner;
         } else {
            PlayerEntity cached = INFERRED_OWNERS.get(pearl);
            if (cached != null && cached.isAlive() && cached.getWorld() == mc.world) {
               return cached;
            } else {
               Vec3d velocity = pearl.getVelocity();
               double speed = velocity.length();
               Vec3d direction = speed > 0.1 ? velocity.multiply(1.0 / speed) : Vec3d.ZERO;
               int backtrackTicks = Math.max(0, Math.min(pearl.age, 20));
               Vec3d estimatedOrigin = pearl.getPos().subtract(velocity.multiply(backtrackTicks));
               PlayerEntity best = null;
               double bestScore = Double.MAX_VALUE;
               double secondScore = Double.MAX_VALUE;

               for (PlayerEntity player : mc.world.getPlayers()) {
                  if (player.isAlive() && !player.isSpectator()) {
                     Vec3d origin = player.getEyePos().add(0.0, -0.1, 0.0);
                     Vec3d currentOffset = pearl.getPos().subtract(origin);
                     double currentDistance = currentOffset.length();
                     double originDistance = estimatedOrigin.distanceTo(origin);
                     double allowedOriginError = 4.0 + backtrackTicks * 0.35;
                     if (!(currentDistance > 10.0) || !(originDistance > allowedOriginError)) {
                        double alignment = currentDistance > 0.2 && speed > 0.1 ? currentOffset.multiply(1.0 / currentDistance).dotProduct(direction) : 1.0;
                        if (!(alignment < -0.1) || !(originDistance > 3.0)) {
                           double lookAlignment = speed > 0.1 ? player.getRotationVec(1.0F).dotProduct(direction) : 0.0;
                           double score = originDistance + (1.0 - Math.max(0.0, alignment)) * 1.5 + (1.0 - Math.max(-0.25, lookAlignment)) * 3.0;
                           if (score < bestScore) {
                              secondScore = bestScore;
                              bestScore = score;
                              best = player;
                           } else if (score < secondScore) {
                              secondScore = score;
                           }
                        }
                     }
                  }
               }

               if (best != null && !(secondScore - bestScore < 0.35)) {
                  INFERRED_OWNERS.put(pearl, best);
                  return best;
               } else {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public static PearlTrajectoryUtil.Prediction predictThrow(Entity thrower, float yaw, float pitch) {
      if (thrower != null && mc.world != null) {
         Vec3d start = thrower.getEyePos().add(0.0, -0.1, 0.0);
         float yawRadians = yaw * (float) (Math.PI / 180.0);
         float pitchRadians = pitch * (float) (Math.PI / 180.0);
         Vec3d velocity = new Vec3d(
               -MathHelper.sin(yawRadians) * MathHelper.cos(pitchRadians),
               -MathHelper.sin(pitchRadians),
               MathHelper.cos(yawRadians) * MathHelper.cos(pitchRadians)
            )
            .multiply(1.5)
            .add(thrower.getVelocity());
         return simulate(start, velocity, thrower);
      } else {
         return null;
      }
   }

   private static PearlTrajectoryUtil.Prediction simulate(Vec3d start, Vec3d initialVelocity, Entity source) {
      Vec3d position = start;
      Vec3d velocity = initialVelocity;
      List<Vec3d> path = new ArrayList<>();
      path.add(start);

      for (int tick = 1; tick <= 300; tick++) {
         Vec3d next = position.add(velocity);
         HitResult hit = mc.world.raycast(new RaycastContext(position, next, ShapeType.COLLIDER, FluidHandling.NONE, source));
         double maxDistanceSq = hit != null && hit.getType() == Type.BLOCK ? position.squaredDistanceTo(hit.getPos()) : position.squaredDistanceTo(next);
         Entity owner = (Entity)(source instanceof EnderPearlEntity pearl ? resolveOwner(pearl) : source);
         Box searchBox = new Box(position, next).expand(1.0);
         EntityHitResult entityHit = ProjectileUtil.raycast(
            source, position, next, searchBox, entity -> entity != source && entity != owner && !entity.isSpectator() && entity.canHit(), maxDistanceSq
         );
         HitResult nearestHit = (HitResult)(entityHit != null ? entityHit : hit);
         if (nearestHit != null && nearestHit.getType() != Type.MISS) {
            path.add(nearestHit.getPos());
            return new PearlTrajectoryUtil.Prediction(nearestHit.getPos(), tick, true, List.copyOf(path));
         }

         position = next;
         path.add(next);
         if (next.y <= mc.world.getBottomY() - 8.0) {
            return new PearlTrajectoryUtil.Prediction(next, tick, false, List.copyOf(path));
         }

         boolean water = mc.world.getBlockState(BlockPos.ofFloored(next)).isOf(Blocks.WATER);
         velocity = velocity.multiply(water ? 0.8 : 0.99);
         velocity = velocity.add(0.0, -0.03, 0.0);
      }

      return new PearlTrajectoryUtil.Prediction(position, 300, false, List.copyOf(path));
   }

   public record Prediction(Vec3d landingPos, int ticks, boolean collided, List<Vec3d> path) {
   }
}
