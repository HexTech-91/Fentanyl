package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoTrapDoor extends Module {
   public static AutoTrapDoor INSTANCE;
   public Vec3d directionVec = null;
   private final EnumSetting<AutoTrapDoor.Page> page = this.add(new EnumSetting<>("Page", AutoTrapDoor.Page.General));
   private final SliderSetting delay;
   private final SliderSetting range;
   private final SliderSetting placeRange;
   private final SliderSetting predictTicks;
   private final SliderSetting closeDelay;
   private final BooleanSetting autoDisable;
   private final BooleanSetting ignoreNakeds;
   private final BooleanSetting antiStandUp;
   private final BooleanSetting ghostHand;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting silent;
   private final BooleanSetting inventory;
   private final BooleanSetting usingPause;
   private final BooleanSetting pauseEat;
   private final Timer timer;
   private final Timer closeTimer;
   private boolean waitingClose;
   private BlockPos pendingClosePos;
   private BlockPos completedPos;
   private PlayerEntity currentTarget;

   public AutoTrapDoor() {
      super("AutoTrapDoor", Module.Category.Combat);
      this.page.setChinese("\u9875\u9762");
      this.delay = this.add(new SliderSetting("Delay", 100, 0, 500, () -> this.page.is(AutoTrapDoor.Page.General)).setSuffix("ms"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.range = this.add(new SliderSetting("Range", 5.0, 0.0, 8.0, () -> this.page.is(AutoTrapDoor.Page.General)).setSuffix("m"));
      this.range.setChinese("\u8303\u56f4");
      this.placeRange = this.add(new SliderSetting("PlaceRange", 4.5, 0.0, 6.0, () -> this.page.is(AutoTrapDoor.Page.General)).setSuffix("m"));
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.predictTicks.setChinese("\u9884\u6d4b\u523b");
      this.closeDelay = this.add(new SliderSetting("CloseDelay", 75, 0, 500, () -> this.page.is(AutoTrapDoor.Page.General)).setSuffix("ms"));
      this.closeDelay.setChinese("\u5173\u95ed\u5ef6\u8fdf");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", false, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.autoDisable.setChinese("\u81ea\u52a8\u7981\u7528");
      this.ignoreNakeds = this.add(new BooleanSetting("IgnoreNakeds", false, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.ignoreNakeds.setChinese("\u5ffd\u7565\u88f8\u88c5");
      this.antiStandUp = this.add(new BooleanSetting("AntiStandUp", true, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.antiStandUp.setChinese("\u9632\u6b62\u7ad9\u7acb");
      this.ghostHand = this.add(new BooleanSetting("GhostHand", true, () -> this.page.is(AutoTrapDoor.Page.General)));
      this.ghostHand.setChinese("\u5e7d\u7075\u624b");
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(AutoTrapDoor.Page.Rotate)));
      this.rotate.setChinese("\u65cb\u8f6c");
      this.yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate)).setParent());
      this.yawStep.setChinese("\u504f\u822a\u6b65\u8fdb");
      this.whenElytra = this.add(
         new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate))
      );
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()));
      this.steps.setChinese("\u6b65\u6570");
      this.checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()).setParent());
      this.checkFov.setChinese("\u4ec5\u89c6\u91ce\u5185");
      this.fov = this.add(
         new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.isOpen() && this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen())
      );
      this.fov.setChinese("\u89c6\u91ce");
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(AutoTrapDoor.Page.Rotate) && this.yawStep.isOpen()));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.silent = this.add(new BooleanSetting("Silent", false, () -> this.page.is(AutoTrapDoor.Page.Swap)));
      this.silent.setChinese("\u9759\u9ed8");
      this.inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(AutoTrapDoor.Page.Swap)));
      this.inventory.setChinese("\u7269\u54c1\u680f\u4ea4\u6362");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.is(AutoTrapDoor.Page.Misc)));
      this.usingPause.setChinese("\u4f7f\u7528\u65f6\u6682\u505c");
      this.pauseEat = this.add(new BooleanSetting("PauseEat", true, () -> this.page.is(AutoTrapDoor.Page.Misc)));
      this.pauseEat.setChinese("\u6682\u505c\u8fdb\u98df");
      this.timer = new Timer();
      this.closeTimer = new Timer();
      this.waitingClose = false;
      this.pendingClosePos = null;
      this.completedPos = null;
      this.currentTarget = null;
      this.setChinese("\u6d3b\u677f\u95e8\u5149\u73af");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.currentTarget != null ? this.currentTarget.getName().getString() : null;
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   private void reset() {
      this.waitingClose = false;
      this.pendingClosePos = null;
      this.completedPos = null;
      this.currentTarget = null;
      this.timer.reset();
      this.closeTimer.reset();
      this.directionVec = null;
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.directionVec != null && this.rotate.getValue() && this.shouldYawStep()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.waitingClose) {
               if (this.pendingClosePos != null && this.closeTimer.passed((long)this.closeDelay.getValue())) {
                  if (this.toggleTrapdoor(this.pendingClosePos, false)) {
                     this.completedPos = this.pendingClosePos;
                     this.timer.reset();
                  }

                  this.pendingClosePos = null;
                  this.waitingClose = false;
               }
            } else if (!this.shouldPause() && this.timer.passed((long)this.delay.getValue())) {
               PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
               if (target == null) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }

                  this.currentTarget = null;
               } else {
                  this.currentTarget = target;
                  if (!this.ignoreNakeds.getValue() || !this.isNaked(target)) {
                     int predict = (int)this.predictTicks.getValue();
                     Vec3d targetPos = this.predictTicks.getValue() > 0.0 ? PredictUtil.getPos(target, predict) : target.getPos();
                     BlockPosX trapdoorPos = new BlockPosX(targetPos.getX(), targetPos.getY() + 1.0, targetPos.getZ());
                     if (trapdoorPos.getY() < 320 && trapdoorPos.getY() >= -64) {
                        if (this.isTrapdoor(trapdoorPos)) {
                           this.handleExistingTrapdoor(trapdoorPos);
                        } else {
                           if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
                              this.completedPos = null;
                           }

                           this.placeBlockOnTarget(trapdoorPos);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldPause() {
      if (Blink.INSTANCE != null && Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return true;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem() ? true : this.pauseEat.getValue() && mc.player.isUsingItem();
      }
   }

   private boolean shouldYawStep() {
      return !this.whenElytra.getValue() && mc.player.isFallFlying() ? false : this.yawStep.getValue();
   }

   private void handleExistingTrapdoor(BlockPos trapdoorPos) {
      boolean isOpen = (Boolean)mc.world.getBlockState(trapdoorPos).get(TrapdoorBlock.OPEN);
      if (isOpen) {
         if (this.toggleTrapdoor(trapdoorPos, false)) {
            this.completedPos = trapdoorPos;
            this.timer.reset();
         }
      } else {
         if (this.completedPos != null && this.completedPos.equals(trapdoorPos)) {
            return;
         }

         if (this.toggleTrapdoor(trapdoorPos, true)) {
            this.waitingClose = true;
            this.pendingClosePos = trapdoorPos;
            this.closeTimer.reset();
            this.timer.reset();
         }
      }
   }

   private void placeBlockOnTarget(BlockPos trapdoorPos) {
      if (this.canPlaceBlock(trapdoorPos)) {
         int slot = this.findTrapdoorSlot();
         if (slot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            this.selectSlot(slot, oldSlot);
            this.restoreSlot(slot, oldSlot);
            if (this.antiStandUp.getValue()) {
               this.waitingClose = true;
               this.pendingClosePos = trapdoorPos;
               this.closeTimer.reset();
            }

            this.timer.reset();
         }
      }
   }

   private boolean canPlaceBlock(BlockPos pos) {
      if (!BlockUtil.canReplace(pos)) {
         return false;
      } else if (this.ghostHand.getValue() ? !this.hasEntityIgnoringPlayers(pos) : !BlockUtil.hasEntity(pos, false)) {
         return this.placeRange.getValue() > 0.0 && mc.player.getPos().distanceTo(pos.toCenterPos()) > this.placeRange.getValue()
            ? false
            : BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null || BlockUtil.allowAirPlace();
      } else {
         return false;
      }
   }

   private int findTrapdoorSlot() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(TrapdoorBlock.class) : InventoryUtil.findClass(TrapdoorBlock.class);
   }

   private boolean toggleTrapdoor(BlockPos trapdoorPos, boolean desiredOpen) {
      if (!this.isTrapdoor(trapdoorPos)) {
         return false;
      } else {
         boolean isOpen = (Boolean)mc.world.getBlockState(trapdoorPos).get(TrapdoorBlock.OPEN);
         if (isOpen == desiredOpen) {
            return false;
         } else if (this.placeRange.getValue() > 0.0 && mc.player.getPos().distanceTo(trapdoorPos.toCenterPos()) > this.placeRange.getValue()) {
            return false;
         } else {
            Direction side = BlockUtil.getClickSide(trapdoorPos);
            return side != null;
         }
      }
   }

   private boolean faceVector(Vec3d vec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(vec);
         return true;
      } else {
         this.directionVec = vec;
         return Alien.ROTATION.inFov(vec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   private boolean isTrapdoor(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() instanceof TrapdoorBlock;
   }

   private boolean hasEntityIgnoringPlayers(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != null && entity.isLiving() && !(entity instanceof PlayerEntity)) {
            return true;
         }
      }

      return false;
   }

   private boolean isNaked(PlayerEntity player) {
      for (int i = 0; i < 4; i++) {
         if (!((ItemStack)player.getInventory().armor.get(i)).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void selectSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private void restoreSlot(int slot, int selectedSlot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, selectedSlot);
         EntityUtil.syncInventory();
      } else if (this.silent.getValue()) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
      } else {
         InventoryUtil.switchToSlot(selectedSlot);
      }
   }

   public static enum Page {
      General,
      Rotate,
      Swap,
      Misc;
   }
}
