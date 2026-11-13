package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class SelfTrap extends Module {
   public static SelfTrap INSTANCE;
   private final EnumSetting<SelfTrap.Page> page = this.add(new EnumSetting<>("Page", SelfTrap.Page.General));
   private final BooleanSetting godMode;
   private final SliderSetting breakTime;
   private final BooleanSetting failedSkip;
   private final SliderSetting placeDelay;
   private final BooleanSetting mineDownward;
   private final BooleanSetting extend;
   private final Timer timer;
   private final SliderSetting blocksPer;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting packetPlace;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting eatPause;
   private final BooleanSetting center;
   private final BooleanSetting inventory;
   private final BooleanSetting enderChest;
   private final BooleanSetting head;
   private final BooleanSetting feet;
   private final BooleanSetting chest;
   private final BindSetting headKey;
   private final BooleanSetting inAir;
   private final BooleanSetting detectMining;
   private final BooleanSetting usingPause;
   private final BooleanSetting noBlockDisable;
   private final BooleanSetting moveDisable;
   private final BooleanSetting jumpDisable;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   public Vec3d directionVec;
   double startX;
   double startY;
   double startZ;
   int progress;
   private boolean shouldCenter;
   public static final List<BlockPos> airList = new ArrayList<>();

   public SelfTrap() {
      super("SelfTrap", Module.Category.Combat);
      this.page.setChinese("\u9875\u9762");
      this.godMode = this.add(new BooleanSetting("GodMode", true, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.godMode.setChinese("\u4e0a\u5e1d\u6a21\u5f0f");
      this.breakTime = this.add(new SliderSetting("BreakTime", 2.0, 0.0, 3.0, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.breakTime.setChinese("\u6316\u6398\u65f6\u95f4");
      this.failedSkip = this.add(new BooleanSetting("FailedSkip", true, () -> this.page.is(SelfTrap.Page.GodMode)));
      this.failedSkip.setChinese("\u5931\u8d25\u8df3\u8fc7");
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.is(SelfTrap.Page.General)));
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.mineDownward = this.add(new BooleanSetting("MineDownward", false, () -> this.page.is(SelfTrap.Page.General)));
      this.mineDownward.setChinese("\u6316\u6398\u4e0b\u65b9");
      this.extend = this.add(new BooleanSetting("Extend", true, () -> this.page.is(SelfTrap.Page.General)));
      this.extend.setChinese("\u6269\u5c55");
      this.timer = new Timer();
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.page.is(SelfTrap.Page.General)));
      this.blocksPer.setChinese("\u6bcf\u6b21\u65b9\u5757");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.is(SelfTrap.Page.General)));
      this.timing.setChinese("\u65f6\u673a");
      this.packetPlace = this.add(new BooleanSetting("PacketPlace", true, () -> this.page.is(SelfTrap.Page.General)));
      this.packetPlace.setChinese("\u53d1\u5305\u653e\u7f6e");
      this.breakCrystal = this.add(new BooleanSetting("BreakCrystal", true, () -> this.page.is(SelfTrap.Page.General)).setParent());
      this.breakCrystal.setChinese("\u6253\u6c34\u6676");
      this.eatPause = this.add(new BooleanSetting("EatPause", true, () -> this.page.is(SelfTrap.Page.General) && this.breakCrystal.isOpen()));
      this.eatPause.setChinese("\u8fdb\u98df\u6682\u505c");
      this.center = this.add(new BooleanSetting("Center", true, () -> this.page.is(SelfTrap.Page.General)));
      this.center.setChinese("\u5c45\u4e2d");
      this.inventory = this.add(new BooleanSetting("Inventory", true, () -> this.page.is(SelfTrap.Page.General)));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.enderChest = this.add(new BooleanSetting("EnderChest", true, () -> this.page.is(SelfTrap.Page.General)));
      this.enderChest.setChinese("\u672b\u5f71\u7bb1");
      this.head = this.add(new BooleanSetting("Head", true, () -> this.page.is(SelfTrap.Page.General)));
      this.head.setChinese("\u5934\u90e8");
      this.feet = this.add(new BooleanSetting("Feet", true, () -> this.page.is(SelfTrap.Page.General)));
      this.feet.setChinese("\u811a\u90e8");
      this.chest = this.add(new BooleanSetting("Chest", true, () -> this.page.is(SelfTrap.Page.General)));
      this.chest.setChinese("\u80f8\u90e8");
      this.headKey = this.add(new BindSetting("HeadKey", -1, () -> this.page.is(SelfTrap.Page.General)));
      this.headKey.setChinese("\u5934\u90e8\u6309\u952e");
      this.inAir = this.add(new BooleanSetting("InAir", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.inAir.setChinese("\u7a7a\u4e2d");
      this.detectMining = this.add(new BooleanSetting("DetectMining", false, () -> this.page.is(SelfTrap.Page.Check)));
      this.detectMining.setChinese("\u68c0\u6d4b\u6316\u6398");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.usingPause.setChinese("\u4f7f\u7528\u4e2d\u6682\u505c");
      this.noBlockDisable = this.add(new BooleanSetting("NoBlockDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.noBlockDisable.setChinese("\u65e0\u65b9\u5757\u5173\u95ed");
      this.moveDisable = this.add(new BooleanSetting("MoveDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.moveDisable.setChinese("\u79fb\u52a8\u5173\u95ed");
      this.jumpDisable = this.add(new BooleanSetting("JumpDisable", true, () -> this.page.is(SelfTrap.Page.Check)));
      this.jumpDisable.setChinese("\u8df3\u8dc3\u5173\u95ed");
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == SelfTrap.Page.Rotate));
      this.rotate.setChinese("\u8f6c\u5934");
      this.yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(
         new BooleanSetting("WhenElytra", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate)
      );
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()));
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(new BooleanSetting("CheckFov", true, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()).setParent());
      this.checkFov.setChinese("\u4ec5\u89c6\u7ebf\u5185");
      this.fov = this.add(
         new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.isOpen() && this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen())
      );
      this.fov.setChinese("\u89c6\u91ce");
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == SelfTrap.Page.Rotate && this.yawStep.isOpen()));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.directionVec = null;
      this.startX = 0.0;
      this.startY = 0.0;
      this.startZ = 0.0;
      this.progress = 0;
      this.shouldCenter = true;
      this.setChinese("\u81ea\u6211\u56f0\u4f4f");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new SelfTrap.SelfTrapTick());
   }

   public static boolean selfIntersectPos(BlockPos pos) {
      return mc.player.getBoundingBox().intersects(new Box(pos));
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double d = vec.x;
      double d2 = vec.z;
      double xz = Math.hypot(d, d2);
      d2 = vec.z;
      double d3 = vec.x;
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - 90.0);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float)yaw, (float)pitch);
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.directionVec != null && this.rotate.getValue() && this.shouldYawStep()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   private static double normalizeAngle(double angleIn) {
      double angle;
      if ((angle = angleIn % 360.0) >= 180.0) {
         angle -= 360.0;
      }

      if (angle < -180.0) {
         angle += 360.0;
      }

      return angle;
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.startX = mc.player.getX();
         this.startY = mc.player.getY();
         this.startZ = mc.player.getZ();
         this.shouldCenter = true;
      } else if (this.moveDisable.getValue() || this.jumpDisable.getValue()) {
         this.disable();
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()
         && (!this.inventory.getValue() || EntityUtil.inInventory())
         && (!this.timing.is(Timing.Pre) || !event.isPost())
         && (!this.timing.is(Timing.Post) || !event.isPre())
         && this.timer.passed((long)this.placeDelay.getValue())) {
         this.directionVec = null;
         this.progress = 0;
         if (!MovementUtil.isMoving() && !mc.options.jumpKey.isPressed()) {
            this.startX = mc.player.getX();
            this.startY = mc.player.getY();
            this.startZ = mc.player.getZ();
         }

         BlockPos pos = EntityUtil.getPlayerPos(true);
         double distanceToStart = MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.startX, this.startY, this.startZ));
         if (this.getBlock() == -1) {
            if (this.noBlockDisable.getValue()) {
               this.disable();
            }
         } else if ((!this.moveDisable.getValue() || !(distanceToStart > 1.0)) && (!this.jumpDisable.getValue() || !mc.player.input.jumping)) {
            if ((!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())
               && (!this.usingPause.getValue() || !mc.player.isUsingItem())
               && (this.inAir.getValue() || mc.player.isOnGround())) {
               if (this.head.getValue()) {
                  Block block = BlockUtil.getBlock(pos.up());
                  if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
                     this.tryPlaceBlock(pos.up(2));
                  } else {
                     this.clickDown(pos.up(2));
                  }
               }

               if (this.feet.getValue()) {
                  this.doSurround(pos);
               }

               if (this.chest.getValue()) {
                  this.doSurround(pos.up());
               }

               airList.clear();
            }
         } else {
            this.disable();
         }
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   @EventListener(
      priority = -1
   )
   public void onMove(MoveEvent event) {
      if (!nullCheck() && this.center.getValue() && !mc.player.isFallFlying()) {
         BlockPos blockPos = EntityUtil.getPlayerPos(true);
         if (mc.player.getX() - blockPos.getX() - 0.5 <= 0.2
            && mc.player.getX() - blockPos.getX() - 0.5 >= -0.2
            && mc.player.getZ() - blockPos.getZ() - 0.5 <= 0.2
            && mc.player.getZ() - 0.5 - blockPos.getZ() >= -0.2) {
            if (this.shouldCenter && (mc.player.isOnGround() || MovementUtil.isMoving())) {
               event.setX(0.0);
               event.setZ(0.0);
               this.shouldCenter = false;
            }
         } else if (this.shouldCenter) {
            Vec3d centerPos = EntityUtil.getPlayerPos(true).toCenterPos();
            float rotation = getRotationTo(mc.player.getPos(), centerPos).x;
            float yawRad = rotation / 180.0F * (float) Math.PI;
            double dist = mc.player.getPos().distanceTo(new Vec3d(centerPos.x, mc.player.getY(), centerPos.z));
            double cappedSpeed = Math.min(0.2873, dist);
            double x = -((float)Math.sin(yawRad)) * cappedSpeed;
            double z = (float)Math.cos(yawRad) * cappedSpeed;
            event.setX(x);
            event.setZ(z);
         }
      }
   }

   private void doSurround(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP) {
            BlockPos offsetPos = pos.offset(i);
            if (this.godMode.getValue()) {
               for (BreakManager.BreakData breakData : Alien.BREAK.breakMap.values()) {
                  if (breakData.getEntity() != null && (!this.failedSkip.getValue() || !breakData.failed) && breakData.pos.equals(offsetPos)) {
                     if (breakData.timer.getMs() >= this.breakTime.getValue() * 1000.0) {
                        airList.add(offsetPos);
                     }
                     break;
                  }
               }
            }

            if (BlockUtil.getPlaceSide(offsetPos) != null) {
               this.tryPlaceBlock(offsetPos);
            } else if (BlockUtil.canReplace(offsetPos)) {
               this.tryPlaceBlock(this.getHelperPos(offsetPos));
            }

            if (selfIntersectPos(offsetPos) && this.extend.getValue()) {
               for (Direction i2 : Direction.values()) {
                  if (i2 != Direction.UP) {
                     BlockPos offsetPos2 = offsetPos.offset(i2);
                     if (this.godMode.getValue()) {
                        for (BreakManager.BreakData breakDatax : Alien.BREAK.breakMap.values()) {
                           if (breakDatax.getEntity() != null && (!this.failedSkip.getValue() || !breakDatax.failed) && breakDatax.pos.equals(offsetPos2)) {
                              if (breakDatax.timer.getMs() >= this.breakTime.getValue() * 1000.0) {
                                 airList.add(offsetPos2);
                              }
                              break;
                           }
                        }
                     }

                     if (selfIntersectPos(offsetPos2)) {
                        for (Direction i3 : Direction.values()) {
                           if (i3 != Direction.UP) {
                              this.tryPlaceBlock(offsetPos2);
                              BlockPos offsetPos3 = offsetPos2.offset(i3);
                              this.tryPlaceBlock(
                                 BlockUtil.getPlaceSide(offsetPos3) == null && BlockUtil.canReplace(offsetPos3) ? this.getHelperPos(offsetPos3) : offsetPos3
                              );
                           }
                        }
                     }

                     this.tryPlaceBlock(
                        BlockUtil.getPlaceSide(offsetPos2) == null && BlockUtil.canReplace(offsetPos2) ? this.getHelperPos(offsetPos2) : offsetPos2
                     );
                  }
               }
            }
         }
      }
   }

   private boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   private void clickDown(BlockPos pos) {
      if (pos != null && (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos))) {
         int block = this.getBlock();
         if (block != -1) {
            Direction side = Direction.DOWN;
            Vec3d directionVec = new Vec3d(
               pos.getX() + 0.5 + side.getVector().getX() * 0.5,
               pos.getY() + 0.5 + side.getVector().getY() * 0.5,
               pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );
            if (BlockUtil.clientCanPlace(pos, true) && airList.contains(pos) && (!this.rotate.getValue() || this.faceVector(directionVec))) {
               if (this.breakCrystal.getValue()) {
                  CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
               } else if (BlockUtil.hasEntity(pos, false)) {
                  return;
               }

               int old = mc.player.getInventory().selectedSlot;
               this.doSwap(block);
               BlockUtil.placedPos.add(pos);
               if (BlockUtil.allowAirPlace()) {
                  BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, this.packetPlace.getValue());
               } else {
                  BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, this.packetPlace.getValue());
               }

               if (this.inventory.getValue()) {
                  this.doSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(old);
               }

               if (this.rotate.getValue() && !this.shouldYawStep()) {
                  Alien.ROTATION.snapBack();
               }

               this.progress++;
               this.timer.reset();
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null && (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos))) {
         BlockPos self = EntityUtil.getPlayerPos(true);
         if ((!this.mineDownward.getValue() || !Objects.equals(PacketMine.getBreakPos(), self.down()) || !Objects.equals(PacketMine.getBreakPos(), pos))
            && this.progress < this.blocksPer.getValue()) {
            int block = this.getBlock();
            if (block != -1) {
               Direction side = BlockUtil.getPlaceSide(pos);
               if (side != null) {
                  Vec3d directionVec = new Vec3d(
                     pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                     pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                     pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                  );
                  if (BlockUtil.canPlace(pos, 6.0, true) && (!this.rotate.getValue() || this.faceVector(directionVec))) {
                     if (this.breakCrystal.getValue()) {
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
                     } else if (BlockUtil.hasEntity(pos, false)) {
                        return;
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(block);
                     BlockUtil.placedPos.add(pos);
                     if (BlockUtil.allowAirPlace()) {
                        BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, this.packetPlace.getValue());
                     } else {
                        BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, this.packetPlace.getValue());
                     }

                     this.timer.reset();
                     if (this.inventory.getValue()) {
                        this.doSwap(block);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && !this.shouldYawStep()) {
                        Alien.ROTATION.snapBack();
                     }

                     this.progress++;
                  }
               }
            }
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

   private int getBlock() {
      if (this.inventory.getValue()) {
         return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
      } else {
         return InventoryUtil.findBlock(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlock(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      }
   }

   public BlockPos getHelperPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if ((!this.detectMining.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
            && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
            && BlockUtil.canPlace(pos.offset(i))) {
            return pos.offset(i);
         }
      }

      return null;
   }

   public static enum Page {
      General,
      Rotate,
      Check,
      GodMode;
   }

   public class SelfTrapTick {
      boolean pressed = false;

      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && (!SelfTrap.this.timing.is(Timing.Pre) || !event.isPost()) && (!SelfTrap.this.timing.is(Timing.Post) || !event.isPre())) {
            if (!SelfTrap.this.headKey.isPressed()) {
               this.pressed = false;
            } else if (!this.pressed) {
               this.pressed = true;
               SelfTrap.this.directionVec = null;
               SelfTrap.this.progress = 0;
               BlockPos pos = EntityUtil.getPlayerPos(true);
               if (SelfTrap.this.getBlock() != -1 && (SelfTrap.this.inAir.getValue() || Wrapper.mc.player.isOnGround())) {
                  Block block = BlockUtil.getBlock(pos.up());
                  if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
                     SelfTrap.this.tryPlaceBlock(pos.up(2));
                  } else {
                     SelfTrap.this.clickDown(pos.up(2));
                  }
               }
            }
         }
      }
   }
}
