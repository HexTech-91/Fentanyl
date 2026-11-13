package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.JumpEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IVec3d;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Iterator;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class HighJumpPlus extends Module {
   public static HighJumpPlus INSTANCE;
   private final EnumSetting<HighJumpPlus.Mode> mode = this.add(new EnumSetting<>("Mode", HighJumpPlus.Mode.HighJump));
   public final SliderSetting height;
   private final SliderSetting longDistance;
   private final BooleanSetting airJump;
   private final BooleanSetting rotate;
   private final SliderSetting timeout;
   private final BooleanSetting autoObsidian;
   private final SliderSetting searchRange;
   private boolean attackPending;
   private int attackWaitTicks;
   private boolean windChargePending;
   private boolean waiting;
   private int waitTicks;
   private double prevVelocityY;
   private double prevVelocityX;
   private double prevVelocityZ;
   private BlockPos pendingCrystalPos;

   public HighJumpPlus() {
      super("HighJump+", "\u9b3c\u624b\u6c34\u6676+\u91cd\u9524+\u98ce\u5f39=\u7a33\u5b9a\u9ad8\u8df330\u683c", Module.Category.Movement);
      this.mode.setChinese("\u6a21\u5f0f");
      this.height = this.add(new SliderSetting("Height", 30.0, 1.0, 50.0, 1.0, () -> this.mode.is(HighJumpPlus.Mode.HighJump)));
      this.height.setChinese("\u8df3\u8dc3\u9ad8\u5ea6");
      this.longDistance = this.add(new SliderSetting("Distance", 20.0, 5.0, 50.0, 1.0, () -> this.mode.is(HighJumpPlus.Mode.LongJump)));
      this.longDistance.setChinese("\u8df3\u8dc3\u8ddd\u79bb");
      this.airJump = this.add(new BooleanSetting("AirJump", false));
      this.airJump.setChinese("\u7a7a\u4e2d\u8df3\u8dc3");
      this.rotate = this.add(new BooleanSetting("Rotate", true));
      this.rotate.setChinese("\u65cb\u8f6c");
      this.timeout = this.add(new SliderSetting("Timeout", 0.2, 0.05, 0.5, 0.05).setSuffix("s"));
      this.timeout.setChinese("\u8d85\u65f6");
      this.autoObsidian = this.add(new BooleanSetting("AutoObsidian", true));
      this.autoObsidian.setChinese("\u81ea\u52a8\u9ed1\u66dc\u77f3");
      this.searchRange = this.add(new SliderSetting("SearchRange", 5.0, 1.0, 7.0, 1.0));
      this.searchRange.setChinese("\u641c\u7d22\u8303\u56f4");
      this.attackPending = false;
      this.attackWaitTicks = 0;
      this.windChargePending = false;
      this.waiting = false;
      this.waitTicks = 0;
      this.prevVelocityY = 0.0;
      this.prevVelocityX = 0.0;
      this.prevVelocityZ = 0.0;
      this.pendingCrystalPos = null;
      this.setChinese("\u9ad8\u8df3+");
      INSTANCE = this;
   }

   @EventListener
   public void onJump(JumpEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.airJump.getValue() || mc.player.isOnGround()) {
               int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
               int maceSlot = InventoryUtil.findItem(Items.MACE);
               int windSlot = InventoryUtil.findItem(Items.WIND_CHARGE);
               if (crystalSlot != -1 && maceSlot != -1 && windSlot != -1) {
                  event.cancel();
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  BlockPos obsPos = this.findCrystalBase();
                  if (obsPos == null && this.autoObsidian.getValue()) {
                     int obsidianSlot = InventoryUtil.findBlock(Blocks.OBSIDIAN);
                     if (obsidianSlot != -1) {
                        InventoryUtil.switchToSlot(obsidianSlot);
                        BlockPos feetPos = mc.player.getBlockPos();
                        BlockUtil.clickBlock(feetPos.down(), Direction.UP, this.rotate.getValue());
                        obsPos = feetPos;
                     }
                  }

                  if (obsPos != null) {
                     this.pendingCrystalPos = obsPos.up();
                     InventoryUtil.switchToSlot(crystalSlot);
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(obsPos.toCenterPos().add(0.0, 0.5, 0.0));
                     }

                     BlockUtil.clickBlock(obsPos, Direction.UP, false, Hand.MAIN_HAND);
                     InventoryUtil.switchToSlot(maceSlot);
                     this.attackPending = true;
                     this.attackWaitTicks = 0;
                     InventoryUtil.switchToSlot(oldSlot);
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.attackPending) {
               this.attackWaitTicks++;
               if (this.attackWaitTicks > 6) {
                  this.attackPending = false;
               } else if (this.pendingCrystalPos != null) {
                  Iterator maxTicks = BlockUtil.getEndCrystals(new Box(this.pendingCrystalPos).expand(0.5)).iterator();
                  if (maxTicks.hasNext()) {
                     EndCrystalEntity entity = (EndCrystalEntity)maxTicks.next();
                     int maceSlot = InventoryUtil.findItem(Items.MACE);
                     int oldSlot = mc.player.getInventory().selectedSlot;
                     if (maceSlot != -1) {
                        InventoryUtil.switchToSlot(maceSlot);
                        if (this.rotate.getValue()) {
                           Alien.ROTATION.lookAt(entity.getPos());
                        }

                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        mc.player.resetLastAttackedTicks();
                        InventoryUtil.switchToSlot(oldSlot);
                     }

                     this.attackPending = false;
                     this.windChargePending = true;
                  }
               }
            }

            if (this.windChargePending) {
               this.windChargePending = false;
               int windSlot = InventoryUtil.findItem(Items.WIND_CHARGE);
               if (windSlot == -1) {
                  return;
               }

               int oldSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(windSlot);
               if (this.rotate.getValue()) {
                  Alien.ROTATION.lookAt(new Vec3d(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ()));
               }

               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
               InventoryUtil.switchToSlot(oldSlot);
               Vec3d v = mc.player.getVelocity();
               this.prevVelocityY = v.y;
               this.prevVelocityX = v.x;
               this.prevVelocityZ = v.z;
               this.waiting = true;
               this.waitTicks = 0;
            }

            if (this.waiting) {
               this.waitTicks++;
               int maxTicks = Math.max(1, (int)(this.timeout.getValue() / 0.05));
               if (this.waitTicks > maxTicks) {
                  this.waiting = false;
               } else {
                  Vec3d v = mc.player.getVelocity();
                  double deltaY = v.y - this.prevVelocityY;
                  double deltaX = v.x - this.prevVelocityX;
                  double deltaZ = v.z - this.prevVelocityZ;
                  if (Math.abs(deltaY) > 0.01 || Math.abs(deltaX) > 0.01 || Math.abs(deltaZ) > 0.01) {
                     if (this.mode.is(HighJumpPlus.Mode.LongJump)) {
                        double speed = Math.sqrt(0.16 * this.longDistance.getValue());
                        float yaw = Sprint.getSprintYaw(mc.player.getYaw());
                        double rad = Math.toRadians(yaw);
                        ((IVec3d)v).setX(-Math.sin(rad) * speed);
                        ((IVec3d)v).setZ(Math.cos(rad) * speed);
                        ((IVec3d)v).setY(v.y + 0.42);
                     } else {
                        double motionY = Math.sqrt(0.16 * this.height.getValue());
                        ((IVec3d)v).setY(motionY);
                     }

                     this.waiting = false;
                  }
               }
            }
         }
      }
   }

   private BlockPos findCrystalBase() {
      BlockPos feet = mc.player.getBlockPos();
      int range = this.searchRange.getValueInt();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (int dx = -range; dx <= range; dx++) {
         for (int dz = -range; dz <= range; dz++) {
            for (int dy = -2; dy <= 0; dy++) {
               BlockPos pos = feet.add(dx, dy, dz);
               if (this.isValidBase(pos)) {
                  BlockPos above = pos.up();
                  if (mc.world.isAir(above) && (dx == 0 && dz == 0 && dy == 0 || this.canSeeTop(pos))) {
                     double dist = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
                     if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isValidBase(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   private boolean canSeeTop(BlockPos pos) {
      Vec3d eye = mc.player.getEyePos();
      Vec3d target = pos.toCenterPos().add(0.0, 0.5, 0.0);
      Vec3d dir = target.subtract(eye).normalize();
      double dist = eye.distanceTo(target);
      return !this.isBlocked(eye, dir, pos, dist);
   }

   private boolean isBlocked(Vec3d start, Vec3d dir, BlockPos target, double maxDist) {
      double step = 0.1;

      for (double traveled = 0.0; traveled < maxDist; traveled += step) {
         Vec3d point = start.add(dir.multiply(traveled));
         BlockPos check = BlockPos.ofFloored(point);
         if (!check.equals(target) && !mc.world.isAir(check)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void onDisable() {
      this.attackPending = false;
      this.windChargePending = false;
      this.waiting = false;
      this.waitTicks = 0;
      this.attackWaitTicks = 0;
      this.pendingCrystalPos = null;
   }

   @Override
   public String getInfo() {
      return this.mode.is(HighJumpPlus.Mode.LongJump) ? (int)this.longDistance.getValue() + "\u683c" : (int)this.height.getValue() + "\u683c";
   }

   public static enum Mode {
      HighJump,
      LongJump;
   }
}
