package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.TPUtils;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TpChase extends Module {
   public static TpChase INSTANCE;
   private final EnumSetting<TpChase.Priority> priority = this.add(new EnumSetting<>("Priority", TpChase.Priority.Health));
   private final SliderSetting range = this.add(new SliderSetting("Range", 20.0, 0.0, 64.0, 0.5).setSuffix("m"));
   private final SliderSetting tpDistance = this.add(new SliderSetting("TPDistance", 15.0, 0.0, 100.0, 0.5).setSuffix("m"));
   private final SliderSetting maxYDiff = this.add(new SliderSetting("MaxYDiff", 5.0, 1.0, 20.0, 0.5).setSuffix("m"));
   private final BooleanSetting tpToCenter = this.add(new BooleanSetting("TpToCenter", false));
   private final SliderSetting extraDelay = this.add(new SliderSetting("Cooldown", 0, 0, 1000).setSuffix("ms"));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false));
   private final Timer timer = new Timer();
   private final Timer lastThrowTimer = new Timer();
   private PlayerEntity target;
   private boolean armed = false;

   public TpChase() {
      super("TpChase", "\u8ffd\u4eba", Module.Category.Movement);
      this.setChinese("TP\u8ffd\u4eba");
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.range.setChinese("\u8ffd\u51fb\u8303\u56f4");
      this.tpDistance.setChinese("TP\u8ddd\u79bb");
      this.maxYDiff.setChinese("\u6700\u5927Y\u5dee");
      this.tpToCenter.setChinese("\u8ffd\u5230\u65b9\u5757\u4e2d\u5fc3");
      this.extraDelay.setChinese("\u989d\u5916\u51b7\u5374");
      this.inventorySwap.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.autoDisable.setChinese("\u81ea\u52a8\u7981\u7528");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.armed = false;
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.armed = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         if (this.armed) {
            if (!this.timer.passedMs(1500.0)) {
               return;
            }

            this.armed = false;
         }

         if (!mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE)) {
            if (this.lastThrowTimer.passed(this.extraDelay.getValueInt())) {
               this.target = this.pickTarget();
               if (this.target != null) {
                  int var2 = this.findItem(Items.WIND_CHARGE);
                  if (var2 != -1) {
                     Vec3d var3 = this.getGroundHitVec();
                     if (var3 != null) {
                        int var4 = mc.player.getInventory().selectedSlot;
                        Alien.ROTATION.snapAt(var3);
                        this.swap(var2, var4);
                        sendSequencedPacket(
                           var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                        );
                        this.swap(var2, var4);
                        this.armed = true;
                        this.timer.reset();
                        this.lastThrowTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onWindBurst(PacketEvent.Receive var1) {
      if (!nullCheck() && this.armed) {
         if (var1.getPacket() instanceof ExplosionS2CPacket var2) {
            if (var2.getSoundEvent() == SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST) {
               if (!(mc.player.squaredDistanceTo(var2.getX(), var2.getY(), var2.getZ()) > 5.0)) {
                  this.armed = false;
                  if (this.target != null && this.target.isAlive()) {
                     this.teleportTowards(this.target);
                     this.timer.reset();
                     if (this.autoDisable.getValue()) {
                        this.disable();
                     }
                  }
               }
            }
         }
      }
   }

   private PlayerEntity pickTarget() {
      List<PlayerEntity> var1 = CombatUtil.getEnemies(this.range.getValue());
      if (var1.isEmpty()) {
         return null;
      } else {
         PlayerEntity var2 = null;

         for (PlayerEntity var4 : var1) {
            if (var2 == null) {
               var2 = var4;
            } else if (this.priority.getValue() == TpChase.Priority.Health) {
               if (var4.getHealth() < var2.getHealth()) {
                  var2 = var4;
               }
            } else if (var4.squaredDistanceTo(mc.player) < var2.squaredDistanceTo(mc.player)) {
               var2 = var4;
            }
         }

         return var2;
      }
   }

   private void teleportTowards(PlayerEntity var1) {
      Vec3d var2 = mc.player.getPos();
      Vec3d var3 = var1.getPos();
      if (this.tpToCenter.getValue()) {
         if (var2.distanceTo(var3) > this.tpDistance.getValue()) {
            this.moveTowards(var3, true);
         } else {
            Vec3d var4 = this.getTargetBlockCenter(var1);
            if (var4 != null) {
               TPUtils.newTeleport(var4);
               mc.player.setPosition(var4.x, var4.y, var4.z);
            }
         }
      } else {
         this.moveTowards(var3, false);
      }
   }

   private void moveTowards(Vec3d var1, boolean var2) {
      Vec3d var3 = mc.player.getPos();
      double var4 = var1.x - var3.x;
      double var6 = var1.z - var3.z;
      double var8 = Math.sqrt(var4 * var4 + var6 * var6);
      if (!(var8 < 0.001)) {
         double var10 = Math.min(this.tpDistance.getValue(), var8);
         double var12 = var4 / var8;
         double var14 = var6 / var8;
         double var16 = var3.x + var12 * var10;
         double var18 = var3.z + var14 * var10;
         double var20 = var1.y - var3.y;
         double var22 = this.maxYDiff.getValue();
         double var24 = var3.y + Math.max(-var22, Math.min(var22, var20));
         if (var2) {
            BlockPos var26 = new BlockPos((int)Math.floor(var16), (int)Math.floor(var24), (int)Math.floor(var18));
            var16 = var26.getX() + 0.5;
            var18 = var26.getZ() + 0.5;
            BlockPos var27 = var26;

            while (var27.getY() > mc.world.getBottomY() && mc.world.isAir(var27)) {
               var27 = var27.down();
            }

            if (!mc.world.isAir(var27)) {
               var24 = var27.getY() + 1.0;
            }
         } else {
            var24 = this.findSafeY(var16, var24, var18, var3.y);
         }

         TPUtils.newTeleport(new Vec3d(var16, var24, var18));
         mc.player.setPosition(var16, var24, var18);
      }
   }

   private Vec3d getTargetBlockCenter(PlayerEntity var1) {
      if (mc.world == null) {
         return null;
      } else {
         BlockPos var2 = var1.getBlockPos();
         boolean var3 = !mc.world.isAir(var2);
         if (var3) {
            return new Vec3d(var1.getX(), var1.getY() + 1.0, var1.getZ());
         } else {
            BlockPos var4 = var1.getBlockPos().down();
            BlockPos var5 = var4;

            while (var5.getY() > mc.world.getBottomY() && mc.world.isAir(var5)) {
               var5 = var5.down();
            }

            return var5.getY() > mc.world.getBottomY() && !mc.world.isAir(var5) ? new Vec3d(var5.getX() + 0.5, var5.getY() + 1.0, var5.getZ() + 0.5) : null;
         }
      }
   }

   private double findSafeY(double var1, double var3, double var5, double var7) {
      double var9 = var3;
      if (mc.world != null) {
         for (int var11 = 0; var11 < 12; var11++) {
            Vec3d var12 = new Vec3d(var1, var9, var5);
            if (mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(var12.x - mc.player.getX(), var12.y - mc.player.getY(), var12.z - mc.player.getZ()))) {
               return var9;
            }

            var9++;
         }
      }

      return var3;
   }

   private Vec3d getGroundHitVec() {
      BlockPos var1 = mc.player.getBlockPos();
      BlockPos var2 = var1.down();
      if (!mc.world.isAir(var2) && mc.world.getBlockState(var2).isFullCube(mc.world, var2)) {
         return this.getClosestPointToSide(mc.player.getEyePos(), var2, Direction.DOWN);
      } else if (!mc.world.isAir(var1) && mc.world.getBlockState(var1).isFullCube(mc.world, var1)) {
         return this.getClosestPointToSide(mc.player.getEyePos(), var1, Direction.DOWN);
      } else {
         Vec3d var3 = null;

         for (Direction var7 : Direction.values()) {
            BlockPos var8 = var1.offset(var7);
            if (!mc.world.isAir(var8) && mc.world.getBlockState(var8).isFullCube(mc.world, var8)) {
               var3 = this.getClosestPointToSide(mc.player.getEyePos(), var8, var7);
            }
         }

         if (var3 == null) {
            for (Direction var16 : Direction.values()) {
               for (Direction var11 : Direction.values()) {
                  BlockPos var12 = var1.offset(var16).offset(var11);
                  if (!mc.world.isAir(var12) && mc.world.getBlockState(var12).isFullCube(mc.world, var12)) {
                     var3 = this.getClosestPointToSide(mc.player.getEyePos(), var12, var11);
                  }
               }
            }
         }

         return var3;
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

   private int findItem(Item var1) {
      return this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(var1) : InventoryUtil.findItem(var1);
   }

   private void swap(int var1, int var2) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(var1, var2);
      } else {
         InventoryUtil.switchToSlot(var1);
      }
   }

   public static enum Priority {
      Health,
      Distance;
   }
}
