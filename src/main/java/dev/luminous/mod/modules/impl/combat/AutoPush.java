package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.enums.SnapBack;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoPush extends Module {
   public static AutoPush INSTANCE;
   private final EnumSetting<AutoPush.Page> page = this.add(new EnumSetting<>("Page", AutoPush.Page.General)).setChinese("\u9875\u9762");
   private final BooleanSetting torch = this.add(new BooleanSetting("Torch", false, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting powerPacket = this.add(new BooleanSetting("PowerPacket", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting noEating = this.add(new BooleanSetting("EatingPause", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true, () -> this.page.is(AutoPush.Page.General)));
   private final SliderSetting updateDelay = this.add(new SliderSetting("Delay", 100, 0, 1000, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.page.is(AutoPush.Page.General)));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, () -> this.page.is(AutoPush.Page.General)));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, () -> this.page.is(AutoPush.Page.General)));
   private final SliderSetting surroundCheck = this.add(new SliderSetting("SurroundCheck", 2, 0, 4, () -> this.page.is(AutoPush.Page.General)));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(AutoPush.Page.General)));
   private final SliderSetting maxPistons1 = this.add(
      new SliderSetting("MaxPistons1", 1, 1, 4, () -> this.page.is(AutoPush.Page.General)).setChinese("\u57cb\u811a1\u683c\u6d3b\u585e\u6570")
   );
   private final SliderSetting maxPistons2 = this.add(
      new SliderSetting("MaxPistons2", 2, 1, 4, () -> this.page.is(AutoPush.Page.General)).setChinese("\u57cb\u811a2\u683c\u6d3b\u585e\u6570")
   );
   private final SliderSetting maxPistons3 = this.add(
      new SliderSetting("MaxPistons3", 3, 1, 4, () -> this.page.is(AutoPush.Page.General)).setChinese("\u57cb\u811a3\u683c\u6d3b\u585e\u6570")
   );
   private final SliderSetting maxPistons4 = this.add(
      new SliderSetting("MaxPistons4", 4, 1, 4, () -> this.page.is(AutoPush.Page.General)).setChinese("\u57cb\u811a4\u683c\u6d3b\u585e\u6570")
   );
   private final Timer timer = new Timer();
   public PlayerEntity target;

   public AutoPush() {
      super("AutoPush", Module.Category.Combat);
      this.setChinese("\u6d3b\u585e\u63a8\u4eba");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      AutoCrystal.INSTANCE.lastBreakTimer.reset();
      this.target = null;
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : "Waiting";
   }

   private PlayerEntity getLockedTarget() {
      if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.displayTarget != null) {
         return AutoCrystal.INSTANCE.displayTarget;
      } else {
         return AutoCrystalPlusPlus.INSTANCE.isOn() ? AutoCrystalPlusPlus.INSTANCE.displayTarget : null;
      }
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Alien.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Alien.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Alien.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Alien.ROTATION.snapAt(0.0F, 5.0F);
      }
   }

   boolean isTargetHere(BlockPos pos, Direction pistonFacing, Entity target) {
      Vec3d center = pos.toCenterPos();
      Direction cd = pistonFacing.rotateYClockwise();
      Vec3d p1 = center.offset(pistonFacing, 0.5).offset(cd, 0.5);
      Vec3d p2 = center.offset(pistonFacing, 0.25).offset(cd, -0.5);
      return new Box(p1, p2).intersects(target.getBoundingBox());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedMs(this.updateDelay.getValue())) {
         this.target = null;
         if (!this.selfGround.getValue() || mc.player.isOnGround()) {
            if (this.findBlock(this.getBlockType()) != -1 && this.findClass(PistonBlock.class) != -1) {
               if (!this.noEating.getValue() || !EntityUtil.isUsingItem()) {
                  if (!InteractTweaks.INSTANCE.isOn() || !InteractTweaks.INSTANCE.isEatingGapple()) {
                     if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                        ArrayList<PlayerEntity> players = new ArrayList<>();
                        PlayerEntity locked = this.getLockedTarget();
                        if (locked != null) {
                           players.add(locked);
                        }
                        if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.displayTarget != null && !players.contains(AutoCrystal.INSTANCE.displayTarget)) {
                           players.add(AutoCrystal.INSTANCE.displayTarget);
                        }

                        players.addAll(CombatUtil.getEnemies(this.range.getValue()));
                        ArrayList<Pair<Integer, Integer>> mode = new ArrayList<>();
                        mode.add(new Pair(5, 5));
                        mode.add(new Pair(4, 4));
                        mode.add(new Pair(2, 3));
                        mode.add(new Pair(3, 2));
                        mode.add(new Pair(1, 1));

                        for (PlayerEntity player : new LinkedHashSet<>(players)) {
                           if (this.canPush(player)) {
                              for (Pair<Integer, Integer> i : mode) {
                                 switch (i.getLeft()) {
                                    case 1:
                                       if (this.trySurroundPush(player)) {
                                          this.target = player;
                                          this.timer.reset();
                                          return;
                                       }
                                       break;
                                    case 2:
                                       if (this.tryBurrowPush(player)) {
                                          this.target = player;
                                          this.timer.reset();
                                          return;
                                       }
                                       break;
                                    case 3:
                                       if (this.tryHeadPush(player)) {
                                          this.target = player;
                                          this.timer.reset();
                                          return;
                                       }
                                       break;
                                    case 4:
                                       if (this.tryBurrowHeadPush(player)) {
                                          this.target = player;
                                          this.timer.reset();
                                          return;
                                       }
                                       break;
                                    case 5:
                                       if (this.tryHeadPull(player)) {
                                          this.target = player;
                                          this.timer.reset();
                                          return;
                                       }
                                 }
                              }
                           }
                        }

                        for (PlayerEntity player : new LinkedHashSet<>(players)) {
                           if (this.canPush(player) && this.tryHeadPull(player)) {
                              this.target = player;
                              this.timer.reset();
                              return;
                           }
                        }
                     }
                  }
               }
            } else {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            }
         }
      }
   }

   private boolean trySurroundPush(PlayerEntity player) {
      for (Direction i : Direction.Type.HORIZONTAL) {
         BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
         if (this.isTargetHere(pos, i.getOpposite(), player) && mc.world.canCollide(player, new Box(pos))) {
            if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()), i)) {
               return true;
            }

            if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()).up(), i)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean tryHeadPush(PlayerEntity player) {
      if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
         return false;
      } else {
         for (Direction i : Direction.Type.HORIZONTAL) {
            BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
            Box box = player.getBoundingBox().offset(new Vec3d(i.getOffsetX(), i.getOffsetY(), i.getOffsetZ()));
            if (this.getBlock(pos) != Blocks.AIR
               && this.getBlock(pos.up()) != Blocks.PISTON_HEAD
               && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
               && !this.isTargetHere(pos, i.getOpposite(), player)) {
               if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()).up(), i)) {
                  return true;
               }

               if (this.tryPush(EntityUtil.getEntityPos(player).offset(i.getOpposite()), i)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private int getBurrowDepth(PlayerEntity player) {
      int depth = 0;
      int feetX = (int)Math.floor(player.getX());
      int feetY = (int)Math.floor(player.getY());
      int feetZ = (int)Math.floor(player.getZ());

      for (int dy = 0; dy < 3; dy++) {
         BlockPos check = new BlockPos(feetX, feetY + dy, feetZ);
         Block b = this.getBlock(check);
         if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA) {
            break;
         }

         depth++;
      }

      return depth;
   }

   private int getMaxPistons(int depth) {
      switch (depth) {
         case 1:
            return this.maxPistons1.getValueInt();
         case 2:
            return this.maxPistons2.getValueInt();
         case 3:
            return this.maxPistons3.getValueInt();
         default:
            return this.maxPistons4.getValueInt();
      }
   }

   private boolean tryBurrowPush(PlayerEntity player) {
      int depth = this.getBurrowDepth(player);
      if (depth <= 0) {
         return false;
      } else {
         BlockPos feet = EntityUtil.getEntityPos(player);
         List<Direction> pushDirs = new ArrayList<>();

         for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos target = feet.offset(dir);
            if (this.getBlock(target) == Blocks.AIR) {
               pushDirs.add(dir);
            }
         }

         if (pushDirs.isEmpty()) {
            return false;
         } else {
            Direction dirx = pushDirs.get(0);
            BlockPos pistonPos = feet.offset(dirx.getOpposite()).up();
            int maxP = this.getMaxPistons(depth);
            int placed = 0;

            for (int h = 0; h < maxP && h < depth; h++) {
               if (this.tryPush(pistonPos.down(h), dirx)) {
                  placed++;
               }
            }

            return placed > 0;
         }
      }
   }

   private boolean tryBurrowHeadPush(PlayerEntity player) {
      BlockPos feet = EntityUtil.getEntityPos(player);
      List<Direction> pushDirs = new ArrayList<>();

      for (Direction dir : Direction.Type.HORIZONTAL) {
         BlockPos target = feet.offset(dir);
         if (this.getBlock(target) == Blocks.AIR) {
            pushDirs.add(dir);
         }
      }

      if (pushDirs.isEmpty()) {
         return false;
      } else {
         Direction dirx = pushDirs.get(0);
         BlockPos pistonPos = feet.offset(dirx.getOpposite()).up();
         return this.tryPush(pistonPos, dirx) ? true : this.tryPush(pistonPos.down(), dirx);
      }
   }

   private boolean tryHeadPull(PlayerEntity player) {
      if (!this.mine.getValue()) {
         return false;
      } else if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
         return false;
      } else {
         for (Direction i : Direction.Type.HORIZONTAL) {
            BlockPos pos = EntityUtil.getEntityPos(player).offset(i).up();
            if (this.isTargetHere(EntityUtil.getEntityPos(player).up(), i.getOpposite(), player) && this.tryPush(pos, i.getOpposite())) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean tryPush(BlockPos piston, Direction direction) {
      if (mc.world.getBlockState(piston.offset(direction)).getPistonBehavior() == PistonBehavior.BLOCK) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            if (this.canPowerPiston(piston) && BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               if (this.tryPlacePiston(piston, direction)) {
                  CombatUtil.clearBlockModify();
                  return true;
               }
            } else if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               if (this.tryPlacePiston(piston, direction)) {
                  if (this.tryPowerPiston(piston)) {
                     CombatUtil.clearBlockModify();
                     return true;
                  }

                  CombatUtil.clearBlockModify();
                  return false;
               }
            } else if (this.tryPowerPiston(piston)) {
               if (this.tryPlacePiston(piston, direction)) {
                  CombatUtil.clearBlockModify();
                  return true;
               }

               CombatUtil.clearBlockModify();
               return false;
            }

            CombatUtil.clearBlockModify();
         }

         BlockState state = mc.world.getBlockState(piston);
         if (state.getBlock() instanceof PistonBlock && this.getBlockState(piston).get(FacingBlock.FACING) == direction) {
            for (Direction i : Direction.values()) {
               if (this.getBlock(piston.offset(i)) == this.getBlockType()) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                     return true;
                  }

                  return false;
               }
            }

            this.tryPowerPiston(piston);
            CombatUtil.clearBlockModify();
         }

         return false;
      }
   }

   private Direction getPowerFacing(BlockPos piston) {
      if (BlockUtil.canPlace(piston.offset(Direction.DOWN))) {
         return Direction.DOWN;
      } else {
         for (Direction i : Direction.values()) {
            if ((i != Direction.UP || !this.torch.getValue()) && i != Direction.DOWN && BlockUtil.canPlace(piston.offset(i))) {
               return i;
            }
         }

         return null;
      }
   }

   private boolean canPowerPiston(BlockPos piston) {
      for (Direction i : Direction.values()) {
         if (this.getBlock(piston.offset(i)) == this.getBlockType()) {
            return true;
         }
      }

      return false;
   }

   private boolean tryPowerPiston(BlockPos piston) {
      Direction powerFacing = this.getPowerFacing(piston);
      if (powerFacing == null) {
         return false;
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         int powerSlot = this.findBlock(this.getBlockType());
         this.doSwap(powerSlot);
         BlockUtil.placeBlock(piston.offset(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
         if (this.inventory.getValue()) {
            this.doSwap(powerSlot);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(oldSlot);
         }

         if (this.mine.getValue() && !piston.offset(powerFacing).equals(PacketMine.getBreakPos())) {
            PacketMine.INSTANCE.mine(piston.offset(powerFacing));
         }

         if (this.autoDisable.getValue()) {
            this.disable();
         }

         CombatUtil.setBlockModify(piston.offset(powerFacing), this.getBlockType().getDefaultState());
         return true;
      }
   }

   private Direction getFacingToTarget(BlockPos piston, PlayerEntity target, Direction fallback) {
      Direction best = null;
      double bestAngle = Double.MAX_VALUE;
      Vec3d eye = target.getEyePos();
      Vec3d feet = new Vec3d(target.getX(), piston.getY() + 0.5, target.getZ());

      for (Direction i : Direction.Type.HORIZONTAL) {
         BlockPos pushPos = piston.offset(i);
         if (this.getBlock(pushPos) != Blocks.AIR && this.getBlock(pushPos) != Blocks.FIRE) {
            continue;
         }

         double angle = this.getYaw(piston, i, eye) + this.getYaw(piston, i, feet);
         if (angle < bestAngle) {
            bestAngle = angle;
            best = i;
         }
      }

      return best == null ? fallback : best;
   }

   private double getYaw(BlockPos piston, Direction facing, Vec3d pos) {
      double dx = pos.x - (piston.getX() + 0.5);
      double dz = pos.z - (piston.getZ() + 0.5);
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len < 1.0E-4) {
         return 0.0;
      } else {
         return 1.0 - (dx * facing.getOffsetX() + dz * facing.getOffsetZ()) / len;
      }
   }

   private boolean tryPlacePiston(BlockPos piston, Direction direction) {
      int pistonSlot = this.findClass(PistonBlock.class);
      Direction side = BlockUtil.getPlaceSide(piston);
      if (side == null) {
         return false;
      } else {
         PlayerEntity locked = this.getLockedTarget();
         boolean forceFacing = locked != null && !this.yawDeceive.getValue();
         if (forceFacing) {
            direction = this.getFacingToTarget(piston, locked, direction);
            pistonFacing(direction.getOpposite());
         }

         if (this.rotate.getValue()) {
            Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
         }

         if (this.yawDeceive.getValue()) {
            pistonFacing(direction.getOpposite());
         }

         int old = mc.player.getInventory().selectedSlot;
         this.doSwap(pistonSlot);
         BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
         if (this.inventory.getValue()) {
            this.doSwap(pistonSlot);
            EntityUtil.syncInventory();
         } else {
            this.doSwap(old);
         }

         CombatUtil.setBlockModify(piston, Blocks.PISTON.getDefaultState());
         if (!forceFacing && this.rotate.getValue() && this.yawDeceive.getValue()) {
            Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
         }

         if (this.rotate.getValue() && !AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.None)) {
            Alien.ROTATION.snapBack();
         }

         if (this.mine.getValue()) {
            for (Direction i : Direction.values()) {
               if (!piston.offset(i).equals(PacketMine.getBreakPos())) {
                  PacketMine.INSTANCE.mine(piston.offset(i));
               }
            }
         }

         return true;
      }
   }

   private boolean facingCheck(BlockPos pos) {
      if (!ClientSetting.INSTANCE.lowVersion.getValue()) {
         return true;
      } else {
         Direction direction = MathUtil.getDirectionFromEntityLiving(pos, mc.player);
         return direction != Direction.UP && direction != Direction.DOWN;
      }
   }

   private boolean isTrueFacing(BlockPos pos, Direction facing) {
      if (this.yawDeceive.getValue()) {
         return true;
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return false;
         } else {
            Vec3d directionVec = new Vec3d(
               pos.getX() + 0.5 + side.getVector().getX() * 0.5,
               pos.getY() + 0.5 + side.getVector().getY() * 0.5,
               pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );
            float[] rotation = RotationManager.getRotation(directionVec);
            return MathUtil.getFacingOrder(rotation[0], rotation[1]).getOpposite() == facing;
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Alien.PLAYER.isInWeb(player)) {
         return false;
      } else {
         float[] offset = new float[]{-0.25F, 0.0F, 0.25F};
         int progress = 0;
         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() + 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() - 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() + 1.0)))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() - 1.0)))) {
            progress++;
         }

         for (float x : offset) {
            for (float z : offset) {
               BlockPosX playerPos = new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);

               for (Direction i : Direction.Type.HORIZONTAL) {
                  BlockPos pos = playerPos.offset(i);
                  if (this.isTargetHere(pos, i.getOpposite(), player)) {
                     if (mc.world.canCollide(player, new Box(pos))) {
                        return true;
                     }

                     if (progress > this.surroundCheck.getValue() - 1.0) {
                        return true;
                     }
                  }
               }
            }
         }

         if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
            for (Direction ix : Direction.Type.HORIZONTAL) {
               BlockPos pos = EntityUtil.getEntityPos(player).offset(ix);
               Box box = player.getBoundingBox().offset(new Vec3d(ix.getOffsetX(), ix.getOffsetY(), ix.getOffsetZ()));
               if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD
                  && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                  && !this.isTargetHere(pos, ix.getOpposite(), player)
                  && mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ())))) {
                  return true;
               }
            }
         }

         return progress > this.surroundCheck.getValue() - 1.0 || Alien.HOLE.isHard(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()));
      }
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private Block getBlockType() {
      return this.torch.getValue() ? Blocks.REDSTONE_TORCH : Blocks.REDSTONE_BLOCK;
   }

   private BlockState getBlockState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }

   static enum Page {
      General;
   }
}
