package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.path.PathUtils;
import dev.luminous.api.utils.path.Vec3;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Macro extends Module {
   public static Macro INSTANCE;
   private Macro.State state = Macro.State.FIND_DONKEY;
   private DonkeyEntity targetDonkey = null;
   private BlockPos targetChest = null;
   private int timer = 0;
   private int donkeyInsertCount = 0;
   private static final int DONKEY_INSERT_TARGET = 15;
   private static final int KEEP_SHULKERS = 15;
   private int attackTimer = 0;
   private static final int ATTACK_COOLDOWN_TICKS = 10;
   private final Set<BlockPos> fullChests = new HashSet<>();
   private int placeSearchRadius = 8;
   private ItemEntity pickupTarget = null;
   private int chestOpenRetries = 0;
   private static final int MAX_CHEST_OPEN_RETRIES = 3;
   private static final int WAIT_CHEST_GUI_TIMEOUT = 15;
   private Vec3d donkeyDeathPos = null;
   private int pickupTimer = 0;
   private static final int MAX_PICKUP_TIME = 40;
   private int chestTimeoutTicks = 0;
   private static final int CHEST_TIMEOUT = 600;
   private List<Vec3> currentPath = null;
   private int pathIndex = 0;
   private boolean highObstaclePathing = false;
   private boolean shouldAutoWalk = false;
   private double lastStuckCheckDistance = -1.0;
   private int stuckTimer = 0;
   private static final int STUCK_THRESHOLD_TICKS = 100;
   private static final double STUCK_DISTANCE_THRESHOLD = 0.5;
   private final SliderSetting openDelayMin = this.add(new SliderSetting("OpenDelayMin", 200.0, 0.0, 5000.0, 1.0).setSuffix("ms"));
   private final SliderSetting openDelayMax;
   private final SliderSetting placeInterval;
   private int insertDelayCounter;
   private int nextInsertDelay;
   private final Random random;
   private final List<ChunkPos> chunkOrder;
   private int chunkIndex;
   private long lastScanTime;
   private static final long SCAN_INTERVAL_MS = 5000L;

   public Macro() {
      super("Macro", Module.Category.Survival);
      this.openDelayMin.setChinese("\u6253\u5f00\u5ef6\u8fdf\u6700\u5c0f\u503c");
      this.openDelayMax = this.add(new SliderSetting("OpenDelayMax", 500.0, 0.0, 5000.0, 1.0).setSuffix("ms"));
      this.openDelayMax.setChinese("\u6253\u5f00\u5ef6\u8fdf\u6700\u5927\u503c");
      this.placeInterval = this.add(new SliderSetting("PlaceInterval", 250.0, 0.0, 5000.0, 1.0).setSuffix("ms"));
      this.placeInterval.setChinese("\u653e\u7f6e\u95f4\u9694");
      this.insertDelayCounter = 0;
      this.nextInsertDelay = -1;
      this.random = new Random();
      this.chunkOrder = new ArrayList<>();
      this.chunkIndex = 0;
      this.lastScanTime = 0L;
      this.setChinese("\u667a\u80fd\u9a74\u5237");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.targetDonkey = null;
      this.targetChest = null;
      this.timer = 0;
      this.donkeyInsertCount = 0;
      this.attackTimer = 0;
      this.fullChests.clear();
      this.placeSearchRadius = 8;
      this.pickupTarget = null;
      this.chestOpenRetries = 0;
      this.clearPatrol();
      this.donkeyDeathPos = null;
      this.pickupTimer = 0;
      this.chestTimeoutTicks = 0;
      this.currentPath = null;
      this.pathIndex = 0;
      this.highObstaclePathing = false;
      this.shouldAutoWalk = false;
      this.lastStuckCheckDistance = -1.0;
      this.stuckTimer = 0;
      this.insertDelayCounter = 0;
      this.nextInsertDelay = -1;
      if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().world != null) {
         if (this.countShulkerBoxes() > 15) {
            this.state = Macro.State.FIND_CHEST;
         } else {
            this.state = Macro.State.FIND_DONKEY;
         }

         CommandManager.sendMessage("\u00a7a[Macro] \u5f00\u59cb\u9a74\u5237");
      } else {
         this.state = Macro.State.FIND_DONKEY;
      }
   }

   @Override
   public void onDisable() {
      this.stopAll();
      this.clearPatrol();
      this.pickupTarget = null;
      CommandManager.sendMessage("\u00a7e[Macro] \u505c\u6b62\u9a74\u5237");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().world != null) {
         this.shouldAutoWalk = false;
         switch (this.state) {
            case FIND_DONKEY:
               this.forceStopMoving();
               this.targetDonkey = this.findValidDonkey();
               if (this.targetDonkey != null) {
                  this.state = Macro.State.WALK_TO_DONKEY;
               } else {
                  this.patrolUpdate();
               }
               break;
            case WALK_TO_DONKEY:
               if (this.targetDonkey == null || !this.targetDonkey.isAlive()) {
                  this.state = Macro.State.FIND_DONKEY;
                  return;
               }

               if (mc.player.distanceTo(this.targetDonkey) <= 3.0) {
                  this.forceStopMoving();
                  this.state = Macro.State.OPEN_DONKEY;
               } else {
                  this.shouldAutoWalk = true;
                  this.simpleMoveTo(this.targetDonkey.getPos());
               }
               break;
            case OPEN_DONKEY:
               mc.player.input.sneaking = true;
               mc.interactionManager.interactEntity(mc.player, this.targetDonkey, Hand.MAIN_HAND);
               this.timer = 0;
               this.state = Macro.State.WAIT_DONKEY_GUI;
               break;
            case WAIT_DONKEY_GUI:
               this.timer++;
               if (this.timer > 30) {
                  mc.player.input.sneaking = false;
                  this.state = Macro.State.OPEN_DONKEY;
               } else if (mc.player.currentScreenHandler instanceof HorseScreenHandler) {
                  mc.player.input.sneaking = false;
                  this.donkeyInsertCount = 0;
                  this.state = Macro.State.FILL_DONKEY;
                  this.insertDelayCounter = 0;
                  long minMs = (long)this.openDelayMin.getValue();
                  long maxMs = (long)this.openDelayMax.getValue();
                  if (maxMs >= minMs && maxMs > 0L) {
                     long delayMs = minMs + (long)(this.random.nextDouble() * (maxMs - minMs + 1L));
                     this.nextInsertDelay = (int)Math.round(delayMs / 50.0);
                  } else {
                     this.nextInsertDelay = 0;
                  }
               }
               break;
            case FILL_DONKEY:
               ScreenHandler handlerx = mc.player.currentScreenHandler;
               if (!(handlerx instanceof HorseScreenHandler)) {
                  this.state = Macro.State.OPEN_DONKEY;
                  return;
               }

               if (this.donkeyInsertCount >= 15 || this.countShulkerBoxes() == 0) {
                  this.state = Macro.State.CLOSE_DONKEY_GUI;
                  return;
               }

               if (this.nextInsertDelay > 0) {
                  this.nextInsertDelay--;
                  return;
               }

               int fromSlot = -1;

               for (int idx = 17; idx <= 52; idx++) {
                  ItemStack stack = handlerx.getSlot(idx).getStack();
                  if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                     fromSlot = idx;
                     break;
                  }
               }

               if (fromSlot == -1) {
                  this.state = Macro.State.CLOSE_DONKEY_GUI;
                  return;
               }

               int toSlot = -1;

               for (int idxx = 2; idxx <= 16; idxx++) {
                  if (handlerx.getSlot(idxx).getStack().isEmpty()) {
                     toSlot = idxx;
                     break;
                  }
               }

               if (toSlot == -1) {
                  this.state = Macro.State.CLOSE_DONKEY_GUI;
                  return;
               }

               mc.interactionManager.clickSlot(handlerx.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(handlerx.syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
               if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                  mc.interactionManager.clickSlot(handlerx.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               }

               this.donkeyInsertCount++;
               long intervalMs = (long)this.placeInterval.getValue();
               this.nextInsertDelay = (int)Math.round(intervalMs / 50.0);
               break;
            case CLOSE_DONKEY_GUI:
               mc.player.closeHandledScreen();
               this.state = Macro.State.DISMOUNT;
               break;
            case DISMOUNT:
               if (mc.player.getVehicle() != null) {
                  mc.player.stopRiding();
               }

               this.state = Macro.State.KILL_DONKEY;
               this.attackTimer = 0;
               this.switchToSword();
               break;
            case KILL_DONKEY:
               if (this.targetDonkey != null && this.targetDonkey.isAlive()) {
                  if (this.attackTimer <= 0) {
                     if (mc.player.getInventory().getMainHandStack().getItem() != Items.NETHERITE_SWORD) {
                        this.switchToSword();
                     }

                     mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.targetDonkey, mc.player.isSneaking()));
                     mc.player.resetLastAttackedTicks();
                     this.attackTimer = 10;
                  } else {
                     this.attackTimer--;
                  }

                  if (this.timer++ > 200) {
                     this.donkeyDeathPos = this.targetDonkey.getPos();
                     this.forceStopMoving();
                     this.state = Macro.State.PICKUP_ITEMS;
                     this.timer = 0;
                     this.pickupTimer = 0;
                     this.pickupTarget = null;
                  }
                  break;
               }

               this.donkeyDeathPos = this.targetDonkey != null ? this.targetDonkey.getPos() : mc.player.getPos();
               this.forceStopMoving();
               this.state = Macro.State.PICKUP_ITEMS;
               this.timer = 0;
               this.pickupTimer = 0;
               this.pickupTarget = null;
               return;
            case PICKUP_ITEMS:
               this.pickupTimer++;
               List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(8.0), e -> true);
               boolean reached30 = this.countShulkerBoxes() >= 30;
               boolean inventoryFull = mc.player.getInventory().getEmptySlot() == -1;
               boolean noShulkerOnGround = items.stream()
                  .noneMatch(item -> item.getStack().getItem() instanceof BlockItem bix && bix.getBlock() instanceof ShulkerBoxBlock);
               boolean timedOut = this.pickupTimer > 40;
               if (reached30 || inventoryFull || noShulkerOnGround || timedOut) {
                  this.forceStopMoving();
                  this.state = Macro.State.FIND_CHEST;
                  this.fullChests.clear();
                  this.pickupTarget = null;
                  return;
               }

               if (mc.player.getPos().distanceTo(this.donkeyDeathPos) > 1.5 && this.pickupTarget == null) {
                  this.shouldAutoWalk = true;
                  this.simpleMoveTo(this.donkeyDeathPos);
               } else {
                  if (this.pickupTarget == null || !this.pickupTarget.isAlive() || mc.player.distanceTo(this.pickupTarget) < 1.5) {
                     this.pickupTarget = items.stream().min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
                  }

                  if (this.pickupTarget != null && this.pickupTarget.isAlive()) {
                     this.shouldAutoWalk = true;
                     this.simpleMoveTo(this.pickupTarget.getPos());
                  } else {
                     this.forceStopMoving();
                  }
               }
               break;
            case FIND_CHEST:
               this.forceStopMoving();
               if (this.timer < 3) {
                  this.timer++;
                  return;
               }

               this.timer = 0;
               if (this.targetChest == null) {
                  this.targetChest = this.findNearestUnfilledChestInDonkeyRange();
               }

               if (this.targetChest == null) {
                  this.state = Macro.State.PLACE_CHEST;
                  CommandManager.sendMessage(
                     "\u00a7e[Macro] \u9644\u8fd1\u7bb1\u5b50\u5df2\u6ee1\u6216\u8d85\u51fa\u8303\u56f4\uff0c\u5c1d\u8bd5\u653e\u7f6e\u65b0\u7bb1\u5b50"
                  );
                  return;
               }

               this.chestOpenRetries = 0;
               this.chestTimeoutTicks = 0;
               this.highObstaclePathing = false;
               this.currentPath = null;
               this.lastStuckCheckDistance = -1.0;
               this.stuckTimer = 0;
               this.state = Macro.State.WALK_TO_CHEST;
               break;
            case WALK_TO_CHEST:
               if (this.targetChest == null) {
                  this.state = Macro.State.FIND_CHEST;
                  return;
               }

               this.chestTimeoutTicks++;
               if (this.chestTimeoutTicks > 600) {
                  this.abandonChest();
                  return;
               }

               Vec3d center = Vec3d.ofCenter(this.targetChest);
               double currentDistance = mc.player.getPos().distanceTo(center);
               if (currentDistance <= 2.6) {
                  this.forceStopMoving();
                  this.state = Macro.State.OPEN_CHEST;
                  this.lastStuckCheckDistance = -1.0;
                  this.stuckTimer = 0;
               } else {
                  if (this.lastStuckCheckDistance < 0.0) {
                     this.lastStuckCheckDistance = currentDistance;
                     this.stuckTimer = 0;
                  } else if (Math.abs(currentDistance - this.lastStuckCheckDistance) < 0.5) {
                     this.stuckTimer++;
                     if (this.stuckTimer >= 100) {
                        this.abandonChest();
                        return;
                     }
                  } else {
                     this.lastStuckCheckDistance = currentDistance;
                     this.stuckTimer = 0;
                  }

                  this.shouldAutoWalk = true;
                  this.moveToChest(center);
               }
               break;
            case OPEN_CHEST:
               if (this.targetChest == null) {
                  this.state = Macro.State.FIND_CHEST;
                  return;
               }

               this.chestTimeoutTicks++;
               if (this.chestTimeoutTicks > 600) {
                  this.abandonChest();
                  return;
               }

               Vec3d openCenter = Vec3d.ofCenter(this.targetChest);
               if (mc.player.getPos().distanceTo(openCenter) > 3.0) {
                  this.state = Macro.State.WALK_TO_CHEST;
                  return;
               }

               mc.interactionManager
                  .interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(openCenter.add(0.0, 0.5, 0.0), Direction.UP, this.targetChest, true));
               this.timer = 0;
               this.state = Macro.State.WAIT_CHEST_GUI;
               break;
            case WAIT_CHEST_GUI:
               this.timer++;
               this.chestTimeoutTicks++;
               if (this.chestTimeoutTicks > 600) {
                  mc.player.closeHandledScreen();
                  this.abandonChest();
                  return;
               }

               if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                  this.state = Macro.State.STORE_SHULKERS;
                  this.chestTimeoutTicks = 0;
                  long minMs = (long)this.openDelayMin.getValue();
                  long maxMs = (long)this.openDelayMax.getValue();
                  if (maxMs >= minMs && maxMs > 0L) {
                     long delayMs = minMs + (long)(this.random.nextDouble() * (maxMs - minMs + 1L));
                     this.nextInsertDelay = (int)Math.round(delayMs / 50.0);
                  } else {
                     this.nextInsertDelay = 0;
                  }

                  this.insertDelayCounter = 0;
               } else if (this.timer > 15) {
                  this.chestOpenRetries++;
                  if (this.chestOpenRetries < 3) {
                     this.state = Macro.State.OPEN_CHEST;
                  } else {
                     this.markChestAsFull(this.targetChest);
                     this.targetChest = null;
                     this.state = Macro.State.FIND_CHEST;
                  }
               }
               break;
            case STORE_SHULKERS:
               if (this.nextInsertDelay > 0) {
                  this.nextInsertDelay--;
                  return;
               }

               ScreenHandler handler = mc.player.currentScreenHandler;
               if (!(handler instanceof GenericContainerScreenHandler generic)) {
                  this.markChestAsFull(this.targetChest);
                  mc.player.closeHandledScreen();
                  this.targetChest = null;
                  this.state = Macro.State.FIND_CHEST;
                  return;
               }

               if (this.countShulkerBoxes() <= 15) {
                  this.state = Macro.State.CLOSE_CHEST_GUI;
                  return;
               }

               int end = generic.getRows() * 9 - 1;
               if (this.isContainerFull(handler, 0, end)) {
                  this.markChestAsFull(this.targetChest);
                  mc.player.closeHandledScreen();
                  this.targetChest = null;
                  this.state = Macro.State.FIND_CHEST;
                  return;
               }

               boolean moved = false;

               for (int id = end + 1; id < handler.slots.size(); id++) {
                  ItemStack stack = handler.getSlot(id).getStack();
                  if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                     mc.interactionManager.clickSlot(handler.syncId, id, 0, SlotActionType.QUICK_MOVE, mc.player);
                     moved = true;
                     break;
                  }
               }

               if (moved) {
                  long insertIntervalMs = (long)this.placeInterval.getValue();
                  this.nextInsertDelay = (int)Math.round(insertIntervalMs / 50.0);
               } else {
                  this.state = Macro.State.CLOSE_CHEST_GUI;
               }
               break;
            case CLOSE_CHEST_GUI:
               mc.player.closeHandledScreen();
               this.fullChests.clear();
               this.state = Macro.State.FIND_DONKEY;
               break;
            case PLACE_CHEST:
               if (this.tryPlaceDoubleChest()) {
                  this.state = Macro.State.WALK_TO_CHEST;
               } else {
                  this.placeSearchRadius += 4;
               }
         }
      }
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.shouldAutoWalk && mc.currentScreen != null) {
         mc.options.forwardKey.setPressed(true);
         mc.options.jumpKey.setPressed(mc.player.horizontalCollision);
         mc.player.input.movementForward = 1.0F;
         mc.player.input.movementSideways = 0.0F;
         mc.player.input.jumping = mc.options.jumpKey.isPressed();
      }
   }

   private void abandonChest() {
      this.markChestAsFull(this.targetChest);
      this.targetChest = null;
      this.chestTimeoutTicks = 0;
      this.state = Macro.State.FIND_CHEST;
   }

   private void simpleMoveTo(Vec3d target) {
      this.faceTarget(target);
      boolean shouldJump = this.isFenceOrLowObstacleAhead();
      this.pressForward(shouldJump || mc.player.horizontalCollision);
   }

   private void moveToChest(Vec3d target) {
      if (this.highObstaclePathing) {
         if (this.currentPath != null && this.pathIndex < this.currentPath.size()) {
            if (this.isHighObstacleAhead(target)) {
               Vec3 wp = this.currentPath.get(this.pathIndex);
               Vec3d wpVec = new Vec3d(wp.x(), wp.y(), wp.z());
               if (mc.player.getBlockPos().equals(BlockPos.ofFloored(wpVec.x, wpVec.y - 0.5, wpVec.z))) {
                  this.pathIndex++;
               }

               if (this.pathIndex < this.currentPath.size()) {
                  wp = this.currentPath.get(this.pathIndex);
                  this.faceTarget(new Vec3d(wp.x(), wp.y(), wp.z()));
               } else {
                  this.faceTarget(target);
               }

               this.pressForward(true);
               return;
            }

            this.highObstaclePathing = false;
            this.currentPath = null;
            this.pathIndex = 0;
         } else {
            List<Vec3> raw = PathUtils.computePath(target);
            if (!raw.isEmpty()) {
               this.currentPath = raw;
               this.pathIndex = 0;
            } else {
               this.highObstaclePathing = false;
            }
         }
      }

      if (this.isHighObstacleAhead(target)) {
         this.highObstaclePathing = true;
         List<Vec3> raw = PathUtils.computePath(target);
         if (!raw.isEmpty()) {
            this.currentPath = raw;
            this.pathIndex = 0;
         } else {
            this.highObstaclePathing = false;
         }
      }

      if (!this.highObstaclePathing) {
         boolean shouldJump = this.isFenceOrLowObstacleAhead();
         this.faceTarget(target);
         this.pressForward(shouldJump || mc.player.horizontalCollision);
      }
   }

   private void pressForward(boolean jump) {
      mc.options.forwardKey.setPressed(true);
      mc.options.jumpKey.setPressed(jump);
      if (mc.player != null) {
         mc.player.input.movementForward = 1.0F;
         mc.player.input.movementSideways = 0.0F;
         if (jump) {
            mc.player.input.jumping = true;
         }
      }
   }

   private boolean isHighObstacleAhead(Vec3d target) {
      Vec3d lookVec = mc.player.getRotationVecClient();
      Vec3d checkPos = mc.player.getEyePos().add(lookVec.multiply(1.5));
      int playerFeetY = mc.player.getBlockY();

      for (int y = playerFeetY; y <= playerFeetY + 3; y++) {
         BlockPos bp = BlockPos.ofFloored(checkPos.x, y, checkPos.z);
         if (mc.world.getBlockState(bp).isSolidBlock(mc.world, bp) && y - playerFeetY >= 3) {
            return true;
         }
      }

      return false;
   }

   private boolean isFenceOrLowObstacleAhead() {
      Vec3d lookVec = mc.player.getRotationVecClient();
      BlockPos ahead = BlockPos.ofFloored(mc.player.getEyePos().add(lookVec.multiply(1.5)));
      BlockState bs = mc.world.getBlockState(ahead);
      Block block = bs.getBlock();
      return block instanceof FenceBlock
         || block instanceof WallBlock
         || block instanceof PaneBlock
         || block instanceof StainedGlassBlock
         || !bs.isAir() && bs.isSolidBlock(mc.world, ahead);
   }

   private void faceTarget(Vec3d target) {
      double dx = target.x - mc.player.getX();
      double dz = target.z - mc.player.getZ();
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float diff = Math.abs(yaw - mc.player.getYaw()) % 360.0F;
      if (diff > 180.0F) {
         diff = 360.0F - diff;
      }

      if (diff > 60.0F) {
         mc.player.setYaw(yaw);
         mc.player.setPitch(0.0F);
         mc.player.networkHandler.sendPacket(new LookAndOnGround(yaw, 0.0F, mc.player.isOnGround()));
      }
   }

   private void forceStopMoving() {
      if (mc.player != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.player.input.movementForward = 0.0F;
         mc.player.input.movementSideways = 0.0F;
         mc.player.input.jumping = false;
      }

      this.currentPath = null;
      this.pathIndex = 0;
      this.highObstaclePathing = false;
      this.shouldAutoWalk = false;
   }

   private void stopAll() {
      this.forceStopMoving();
      if (mc.player != null) {
         mc.player.input.sneaking = false;
      }
   }

   private void markChestAsFull(BlockPos pos) {
      if (pos != null) {
         this.fullChests.add(pos);
         BlockState state = mc.world.getBlockState(pos);
         if (state.getBlock() instanceof ChestBlock) {
            ChestType type = (ChestType)state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
               Direction facing = (Direction)state.get(ChestBlock.FACING);
               BlockPos other = pos.offset(type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
               if (mc.world.getBlockState(other).getBlock() instanceof ChestBlock) {
                  this.fullChests.add(other);
               }
            }
         }
      }
   }

   private boolean isChestFull(BlockPos pos) {
      if (this.fullChests.contains(pos)) {
         return true;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         if (state.getBlock() instanceof ChestBlock) {
            ChestType type = (ChestType)state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
               Direction facing = (Direction)state.get(ChestBlock.FACING);
               BlockPos other = pos.offset(type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
               if (this.fullChests.contains(other)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean tryPlaceDoubleChest() {
      if (this.findChestSlot() == -1) {
         return false;
      } else {
         BlockPos playerPos = mc.player.getBlockPos();
         ChunkPos donkeyChunk = this.targetDonkey != null && this.targetDonkey.isAlive() ? new ChunkPos(this.targetDonkey.getBlockPos()) : null;

         for (int r = 4; r <= this.placeSearchRadius; r += 4) {
            for (BlockPos pos : BlockPos.iterateOutwards(playerPos, r, r, r)) {
               for (Direction dir : Direction.values()) {
                  if (dir != Direction.DOWN && dir != Direction.UP) {
                     BlockPos p2 = pos.offset(dir);
                     if (!pos.equals(p2)
                        && this.canPlaceChest(pos)
                        && this.canPlaceChest(p2)
                        && (donkeyChunk == null || !new ChunkPos(pos).equals(donkeyChunk) && !new ChunkPos(p2).equals(donkeyChunk))) {
                        return this.placeChests(pos, p2);
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean placeChests(BlockPos p1, BlockPos p2) {
      int slot = this.findChestSlot();
      if (slot == -1) {
         return false;
      } else {
         int oldSlot = mc.player.getInventory().selectedSlot;
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         this.placeBlockWithSneakIfNeeded(p1);
         this.placeBlockWithSneakIfNeeded(p2);
         mc.player.getInventory().selectedSlot = oldSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
         this.targetChest = p1;
         this.fullChests.remove(p1);
         this.fullChests.remove(p2);
         CommandManager.sendMessage("\u00a7a[Macro] \u653e\u7f6e\u4e86\u65b0\u53cc\u7bb1\u5b50");
         return true;
      }
   }

   private void placeBlockWithSneakIfNeeded(BlockPos pos) {
      BlockState below = mc.world.getBlockState(pos.down());
      boolean needSneak = below.getBlock() instanceof ChestBlock || below.getBlock() instanceof BarrelBlock;
      if (needSneak) {
         mc.player.input.sneaking = true;
      }

      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, true));
      if (needSneak) {
         mc.player.input.sneaking = false;
      }

      try {
         Thread.sleep(50L);
      } catch (InterruptedException var5) {
      }
   }

   private boolean canPlaceChest(BlockPos pos) {
      return (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).isReplaceable()) && mc.world.getBlockState(pos.down()).isSolid();
   }

   private int findChestSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.CHEST) {
            return i;
         }
      }

      return -1;
   }

   private void switchToSword() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_SWORD) {
            mc.player.getInventory().selectedSlot = i;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
            return;
         }
      }
   }

   private DonkeyEntity findValidDonkey() {
      double r = 128.0;
      Box box = new Box(mc.player.getX() - r, mc.world.getBottomY(), mc.player.getZ() - r, mc.player.getX() + r, mc.world.getTopY(), mc.player.getZ() + r);
      return mc.world
         .getEntitiesByClass(DonkeyEntity.class, box, e -> true)
         .stream()
         .filter(e -> e.isAlive() && !e.isBaby() && e.hasChest())
         .filter(e -> this.countDonkeysInChunk(new ChunkPos(e.getBlockPos())) > 6)
         .sorted(
            Comparator.<DonkeyEntity>comparingInt(e -> this.countDonkeysInChunk(new ChunkPos(e.getBlockPos())))
               .reversed()
               .thenComparingDouble(mc.player::distanceTo)
         )
         .findFirst()
         .orElse(null);
   }

   private int countDonkeysInChunk(ChunkPos cp) {
      return mc.world
         .getEntitiesByClass(
            DonkeyEntity.class,
            new Box(cp.getStartX(), mc.world.getBottomY(), cp.getStartZ(), cp.getEndX() + 1, mc.world.getTopY(), cp.getEndZ() + 1),
            e -> true
         )
         .size();
   }

   private Set<ChunkPos> getAllDonkeyChunks() {
      double searchRange = 128.0;
      int viewDist = (int)Math.ceil(searchRange / 16.0);
      ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
      Set<ChunkPos> donkeyChunks = new HashSet<>();

      for (int dx = -viewDist; dx <= viewDist; dx++) {
         for (int dz = -viewDist; dz <= viewDist; dz++) {
            ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
            if (this.countDonkeysInChunk(cp) > 0) {
               donkeyChunks.add(cp);
            }
         }
      }

      return donkeyChunks;
   }

   private void patrolUpdate() {
      long now = System.currentTimeMillis();
      if (now - this.lastScanTime >= 5000L) {
         this.fullRescanAndSort();
         this.lastScanTime = now;
      }

      if (this.chunkOrder.isEmpty()) {
         this.forceStopMoving();
      } else {
         if (this.chunkIndex >= this.chunkOrder.size()) {
            this.fullRescanAndSort();
            this.chunkIndex = 0;
            if (this.chunkOrder.isEmpty()) {
               this.forceStopMoving();
               return;
            }
         }

         ChunkPos currentChunk = this.chunkOrder.get(this.chunkIndex);
         if (this.countDonkeysInChunk(currentChunk) <= 6) {
            this.chunkIndex++;
            this.forceStopMoving();
         } else {
            DonkeyEntity nearest = this.getValidDonkeyInChunk(currentChunk);
            if (nearest != null) {
               this.targetDonkey = nearest;
               this.state = Macro.State.WALK_TO_DONKEY;
            } else {
               this.chunkIndex++;
            }

            this.forceStopMoving();
         }
      }
   }

   private DonkeyEntity getValidDonkeyInChunk(ChunkPos cp) {
      Box box = new Box(cp.getStartX(), mc.world.getBottomY(), cp.getStartZ(), cp.getEndX() + 1, mc.world.getTopY(), cp.getEndZ() + 1);
      return mc.world
         .getEntitiesByClass(DonkeyEntity.class, box, e -> e.isAlive() && !e.isBaby() && e.hasChest())
         .stream()
         .filter(e -> this.countDonkeysInChunk(cp) > 6)
         .min(Comparator.comparingDouble(mc.player::distanceTo))
         .orElse(null);
   }

   private void fullRescanAndSort() {
      double r = 128.0;
      int viewDist = (int)Math.ceil(r / 16.0);
      ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
      List<ChunkPos> allChunks = new ArrayList<>();

      for (int dx = -viewDist; dx <= viewDist; dx++) {
         for (int dz = -viewDist; dz <= viewDist; dz++) {
            ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
            if (this.countDonkeysInChunk(cp) > 6) {
               allChunks.add(cp);
            }
         }
      }

      allChunks.sort(Comparator.comparingInt(this::countDonkeysInChunk).reversed().thenComparingDouble(cpx -> {
         double cx = cpx.getStartX() + 8.5;
         double cz = cpx.getStartZ() + 8.5;
         return mc.player.getPos().distanceTo(new Vec3d(cx, mc.player.getY(), cz));
      }));
      this.chunkOrder.clear();
      this.chunkOrder.addAll(allChunks);
      this.chunkIndex = 0;
   }

   private void clearPatrol() {
      this.forceStopMoving();
      this.chunkOrder.clear();
      this.chunkIndex = 0;
      this.lastScanTime = 0L;
   }

   private Set<ChunkPos> getDonkeyExtendedChunks() {
      Set<ChunkPos> base = this.getAllDonkeyChunks();
      Set<ChunkPos> extended = new HashSet<>(base);

      for (ChunkPos cp : base) {
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               extended.add(new ChunkPos(cp.x + dx, cp.z + dz));
            }
         }
      }

      return extended;
   }

   private BlockPos findNearestUnfilledChestInDonkeyRange() {
      Set<ChunkPos> validChunks = this.getDonkeyExtendedChunks();
      return BlockUtil.getTileEntities()
         .stream()
         .filter(be -> be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)
         .filter(be -> validChunks.contains(new ChunkPos(be.getPos())))
         .<BlockPos>map(BlockEntity::getPos)
         .filter(pos -> !this.isChestFull(pos))
         .filter(pos -> {
            if (mc.world.getBlockEntity(pos) instanceof Inventory inv) {
               for (int i = 0; i < inv.size(); i++) {
                  if (inv.getStack(i).isEmpty()) {
                     return true;
                  }
               }
            }

            return false;
         })
         .min(Comparator.comparingDouble(pos -> mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))))
         .orElse(null);
   }

   private int countShulkerBoxes() {
      int c = 0;

      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
            c++;
         }
      }

      return c;
   }

   private boolean isContainerFull(ScreenHandler handler, int start, int end) {
      for (Slot slot : handler.slots) {
         if (slot.id >= start && slot.id <= end && slot.getStack().isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static enum State {
      FIND_DONKEY,
      WALK_TO_DONKEY,
      OPEN_DONKEY,
      WAIT_DONKEY_GUI,
      FILL_DONKEY,
      CLOSE_DONKEY_GUI,
      DISMOUNT,
      KILL_DONKEY,
      PICKUP_ITEMS,
      FIND_CHEST,
      WALK_TO_CHEST,
      OPEN_CHEST,
      WAIT_CHEST_GUI,
      STORE_SHULKERS,
      CLOSE_CHEST_GUI,
      PLACE_CHEST;
   }
}
