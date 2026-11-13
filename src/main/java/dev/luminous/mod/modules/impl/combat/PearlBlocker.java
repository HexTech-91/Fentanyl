package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class PearlBlocker extends Module {
   public static PearlBlocker INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 6.0, 0.1));
   private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 30.0, 0.0, 60.0, 1.0));
   private final BooleanSetting predict = this.add(new BooleanSetting("Predict", true));
   private final SliderSetting predictTick = this.add(new SliderSetting("PredictTick", 5.0, 0.0, 20.0, 1.0, () -> this.predict.getValue()));
   private final SliderSetting maxBlocks = this.add(new SliderSetting("MaxBlocks", 2, 1, 4));
   private final double PING_FACTOR = 1.0;
   private final float ROT_SPEED = 180.0F;
   private final int DELAY_MS = 0;
   private final Timer timer = new Timer();
   private boolean isRotating = false;
   private Vec3d rotationTarget = null;
   private float currentYaw = 0.0F;
   private float currentPitch = 0.0F;
   private BooleanSetting eatingPause;
   private boolean eatingPaused;

   public PearlBlocker() {
      super("PearlBlocker", Module.Category.Combat);
      this.setChinese("\u73cd\u73e0\u62e6\u622a");
      INSTANCE = this;
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true));
   }

   @Override
   public void onEnable() {
      this.timer.reset();
      this.isRotating = false;
      if (mc.player != null) {
         this.currentYaw = mc.player.getYaw();
         this.currentPitch = mc.player.getPitch();
      }
   }

   @Override
   public void onDisable() {
      this.isRotating = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (this.eatingPause.getValue()) {
         ClientPlayerEntity var16 = mc.player;
         if (var16 != null && var16.isUsingItem()) {
            ItemStack var17 = var16.getWeaponStack();
            if (var17 != null && !var17.isEmpty() && var17.contains(DataComponentTypes.FOOD)) {
               if (!this.eatingPaused) {
                  this.eatingPaused = true;
                  this.setPaused(true);
               }

               return;
            }
         }

         if (this.eatingPaused) {
            this.eatingPaused = false;
            this.setPaused(false);
         }
      }

      if (mc.world != null && mc.player != null) {
         if (mc.player.isUsingItem()) {
            this.isRotating = false;
         } else if (this.timer.passedMs(0.0)) {
            if (!this.isEnemyNearby()) {
               this.isRotating = false;
            } else {
               int var2 = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
               if (var2 == -1) {
                  this.isRotating = false;
               } else {
                  EnderPearlEntity var3 = this.findClosestTrackedPearl();
                  if (var3 == null) {
                     this.isRotating = false;
                  } else {
                     List var4 = this.calcPearlTrajectory(var3);
                     if (!var4.isEmpty()) {
                        double var5 = Math.pow(this.range.getValue(), 2.0);
                        int var7 = 0;
                        if (this.predict.getValue()) {
                           int var9 = 0;
                           PlayerListEntry var8;
                           if (mc.getNetworkHandler() != null
                              && mc.player != null
                              && (var8 = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid())) != null) {
                              var9 = Math.max(0, var8.getLatency());
                           }

                           int var10 = (int)Math.round(var9 * 1.0 / 50.0);
                           var7 = var10 + this.predictTick.getValueInt();
                        }

                        int var18 = -1;

                        for (int var19 = 0; var19 < var4.size(); var19++) {
                           BlockPos var21 = BlockPos.ofFloored((Position)var4.get(var19));
                           if (mc.player.getEyePos().squaredDistanceTo(var21.toCenterPos()) <= var5
                              && mc.world.getBlockState(var21).isReplaceable()
                              && (
                                 BlockUtil.getPlaceSide(var21) != null
                                    || mc.world.getBlockState(var21.down()).isReplaceable() && BlockUtil.getPlaceSide(var21.down()) != null
                              )) {
                              var18 = var19;
                              if (var19 >= var7) {
                                 break;
                              }
                           }
                        }

                        if (var18 == -1) {
                           this.isRotating = false;
                        } else {
                           ArrayList<BlockPos> var20 = new ArrayList<>();
                           BlockPos var22 = BlockPos.ofFloored((Position)var4.get(var18));
                           if (BlockUtil.getPlaceSide(var22) != null) {
                              var20.add(var22);
                              if (var20.size() < this.maxBlocks.getValueInt() && mc.world.getBlockState(var22.up()).isReplaceable()) {
                                 var20.add(var22.up());
                              }
                           } else {
                              BlockPos var11 = var22.down();
                              var20.add(var11);
                              if (var20.size() < this.maxBlocks.getValueInt()) {
                                 var20.add(var22);
                              }
                           }

                           if (!var20.isEmpty()) {
                              for (BlockPos var12 : var20) {
                                 for (EndCrystalEntity var15 : BlockUtil.getEndCrystals(new Box(var12))) {
                                    if (var15 != null && var15.isAlive() && hitboxBlocksPearl(var4, var15)) {
                                       CombatUtil.attackCrystal(var15, true, false);
                                       this.timer.reset();
                                       return;
                                    }
                                 }
                              }

                              this.rotationTarget = ((BlockPos)var20.get(0)).toCenterPos();
                              this.isRotating = true;
                              int var24 = mc.player.getInventory().selectedSlot;
                              boolean var25 = false;

                              for (BlockPos var27 : var20) {
                                 if (!var25) {
                                    InventoryUtil.inventorySwap(var2, var24);
                                    var25 = true;
                                 }

                                 BlockUtil.placeBlock(var27, false);
                              }

                              if (var25) {
                                 InventoryUtil.inventorySwap(var2, var24);
                                 EntityUtil.syncInventory();
                                 this.timer.reset();
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(UpdateRotateEvent var1) {
      if (mc.player != null) {
         if (this.isRotating && this.rotationTarget != null) {
            float[] var2 = RotationManager.getRotation(this.rotationTarget);
            float var3 = MathHelper.wrapDegrees(var2[0] - this.currentYaw);
            this.currentYaw = this.currentYaw + MathHelper.clamp(var3, -180.0F, 180.0F);
            float var4 = var2[1] - this.currentPitch;
            this.currentPitch = this.currentPitch + MathHelper.clamp(var4, -180.0F, 180.0F);
            this.currentPitch = MathHelper.clamp(this.currentPitch, -90.0F, 90.0F);
            var1.setYaw(this.currentYaw);
            var1.setPitch(this.currentPitch);
         } else {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
         }
      }
   }

   private boolean isEnemyNearby() {
      if (this.enemyRange.getValue() <= 0.0) {
         return true;
      } else {
         Iterator var1 = mc.world.getPlayers().iterator();

         while (true) {
            PlayerEntity var2;
            while (true) {
               if (!var1.hasNext()) {
                  return false;
               }

               var2 = (PlayerEntity)var1.next();
               if (var2 != mc.player && var2.isAlive()) {
                  try {
                     if (Alien.FRIEND.isFriend(var2)) {
                        continue;
                     }
                  } catch (Exception var4) {
                  }
                  break;
               }
            }

            if (mc.player.distanceTo(var2) <= this.enemyRange.getValue()) {
               return true;
            }
         }
      }
   }

   private EnderPearlEntity findClosestTrackedPearl() {
      EnderPearlEntity var1 = null;
      double var2 = Double.MAX_VALUE;

      for (Entity var5 : Alien.THREAD.getEntities()) {
         double var6;
         PlayerEntity var8;
         EnderPearlEntity var9;
         Entity var10;
         if (var5 instanceof EnderPearlEntity
            && (var10 = (var9 = (EnderPearlEntity)var5).getOwner()) != mc.player
            && (!(var10 instanceof PlayerEntity) || !Alien.FRIEND.isFriend(var8 = (PlayerEntity)var10))
            && !((var6 = mc.player.getPos().distanceTo(var9.getPos())) > 30.0)
            && var6 < var2) {
            var2 = var6;
            var1 = var9;
         }
      }

      return var1;
   }

   private List<Vec3d> calcPearlTrajectory(EnderPearlEntity var1) {
      ArrayList var2 = new ArrayList();
      double var3 = var1.getX();
      double var5 = var1.getY();
      double var7 = var1.getZ();
      double var9 = var1.getVelocity().x;
      double var11 = var1.getVelocity().y;
      double var13 = var1.getVelocity().z;
      var2.add(new Vec3d(var3, var5, var7));

      for (int var15 = 0; var15 < 200; var15++) {
         Vec3d var16 = new Vec3d(var3, var5, var7);
         if (mc.world.getBlockState(BlockPos.ofFloored(var3 += var9, var5 += var11, var7 += var13)).getBlock() == Blocks.WATER) {
            var9 *= 0.8;
            var11 *= 0.8;
            var13 *= 0.8;
         } else {
            var9 *= 0.99;
            var11 *= 0.99;
            var13 *= 0.99;
         }

         var11 -= 0.03;
         Vec3d var17 = new Vec3d(var3, var5, var7);
         BlockHitResult var18 = mc.world.raycast(new RaycastContext(var16, var17, ShapeType.OUTLINE, FluidHandling.NONE, var1));
         if (var18 != null && var18.getType() == Type.BLOCK) {
            var2.add(var18.getPos());
            return var2;
         }

         if (var5 <= -65.0) {
            break;
         }

         var2.add(var17);
      }

      return var2;
   }

   private static boolean hitboxBlocksPearl(List<Vec3d> var0, EndCrystalEntity var1) {
      if (var0.size() < 2) {
         return false;
      } else {
         Box var2 = var1.getBoundingBox().expand(0.1);

         for (int var3 = 1; var3 < var0.size(); var3++) {
            Vec3d var5 = (Vec3d)var0.get(var3 - 1);
            if (segmentIntersectsBox(var5, (Vec3d)var0.get(var3), var2)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean segmentIntersectsBox(Vec3d var0, Vec3d var1, Box var2) {
      double[] var3 = new double[]{0.0, 1.0};
      if (!updateSlab(var0.x, var1.x - var0.x, var2.minX, var2.maxX, var3)) {
         return false;
      } else {
         return !updateSlab(var0.y, var1.y - var0.y, var2.minY, var2.maxY, var3) ? false : updateSlab(var0.z, var1.z - var0.z, var2.minZ, var2.maxZ, var3);
      }
   }

   private static boolean updateSlab(double var0, double var2, double var4, double var6, double[] var8) {
      if (!(Math.abs(var2) < 1.0E-9)) {
         double var9 = 1.0 / var2;
         double var11 = (var4 - var0) * var9;
         double var13 = (var6 - var0) * var9;
         if (var11 > var13) {
            double var15 = var11;
            var11 = var13;
            var13 = var15;
         }

         var8[0] = Math.max(var8[0], var11);
         var8[1] = Math.min(var8[1], var13);
         return var8[0] <= var8[1];
      } else {
         return var0 >= var4 && var0 <= var6;
      }
   }
}
