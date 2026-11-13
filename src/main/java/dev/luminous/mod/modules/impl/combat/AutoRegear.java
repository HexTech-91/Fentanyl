package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.InventoryS2CPacketEvent;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ShulkerOverlayRenderer;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoRegear extends Module {
   private static final int MAX_MOVES_PER_TICK = 8;
   private static final int SETTLE_TICKS = 2;
   private static final int MAX_PLACEMENT_ATTEMPTS = 3;
   private static final int MAX_OPEN_ATTEMPTS = 10;
   private static final long PLACEMENT_RETRY_MS = 250L;
   private static final long PLACEMENT_CONFIRM_TIMEOUT_MS = 600L;
   private static final long OPEN_RETRY_MS = 0L;
   private static final long FAILED_POSITION_COOLDOWN_MS = 2000L;
   private static final double SAFE_MIN_PLACEMENT_DISTANCE = 1.5;
   private static final double MOVEMENT_OBSTACLE_DISTANCE = 2.5;
   public static AutoRegear INSTANCE;
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final Timer timeoutTimer;
   final int[] stealCountList;
   private final BooleanSetting autoDisable;
   private final SliderSetting disableTime;
   private final BooleanSetting place;
   private final BooleanSetting inventory;
   private final BooleanSetting preferOpen;
   private final BooleanSetting open;
   private final SliderSetting range;
   private final SliderSetting minRange;
   private final BooleanSetting mine;
   private final BooleanSetting take;
   private final BooleanSetting silent;
   private final BooleanSetting smart;
   private final BooleanSetting forceMove;
   private final SliderSetting crystal;
   private final SliderSetting exp;
   private final SliderSetting totem;
   private final SliderSetting gapple;
   private final SliderSetting obsidian;
   private final SliderSetting web;
   private final SliderSetting glowstone;
   private final SliderSetting anchor;
   private final SliderSetting pearl;
   private final SliderSetting piston;
   private final SliderSetting redstone;
   private final SliderSetting bed;
   private final SliderSetting speed;
   private final SliderSetting resistance;
   private final SliderSetting strength;
   private final BindSetting placeKey;
   private final Timer timer;
   private final Timer mineTimer;
   private final Timer openTimer;
   private final List<BlockPos> openList;
   private final Map<BlockPos, Long> failedPlacePositions;
   public BlockPos placePos;
   private final BooleanSetting overlay = this.add(new BooleanSetting("Overlay", true));
   private final int[] overlayPanel = new int[4];
   private double overlayAnchorX = -1.0;
   private double overlayAnchorY = -1.0;
   private static boolean overlayActive = false;
   private BlockPos lastAnchorPos;
   private BlockPos openPos;
   private boolean opend;
   private boolean on;
   private int activeHandlerSyncId;
   private int readyHandlerSyncId;
   private int emptyHandlerPasses;
   private boolean movedInHandler;
   private int armorReplaceSlot = -1;
   private int placementAttempts;
   private int openAttempts;
   private BlockPos openAttemptPos;

   public static boolean isOverlayActive() {
      AutoRegear var0 = INSTANCE;
      if (var0 != null && var0.isOn() && var0.overlay.getValue()) {
         return overlayActive;
      } else {
         if (overlayActive) {
            overlayActive = false;
            ShulkerOverlayRenderer.clearPops();
         }

         return false;
      }
   }

   public double getOverlayAnchorX() {
      return this.overlayAnchorX >= 0.0 ? this.overlayAnchorX : mc.getWindow().getScaledWidth() / 2.0;
   }

   public double getOverlayAnchorY() {
      return this.overlayAnchorY >= 0.0 ? this.overlayAnchorY : mc.getWindow().getScaledHeight() / 2.0;
   }

   public void reprojectOverlayAnchor() {
      BlockPos var1 = this.getCurrentShulker();
      if (var1 == null) {
         if (this.lastAnchorPos == null || mc.world == null || !(mc.world.getBlockState(this.lastAnchorPos).getBlock() instanceof ShulkerBoxBlock)) {
            this.overlayAnchorX = -1.0;
            this.overlayAnchorY = -1.0;
            return;
         }

         var1 = this.lastAnchorPos;
      } else {
         this.lastAnchorPos = var1;
      }

      Vec3d var2 = TextUtil.worldSpaceToScreenSpace(var1.toCenterPos());
      if (var2 != null && Double.isFinite(var2.x) && Double.isFinite(var2.y)) {
         this.overlayAnchorX = var2.x;
         this.overlayAnchorY = var2.y;
      }
   }

   public List<ItemStack> getOverlayBoxes() {
      ArrayList var1 = new ArrayList();
      if (mc.player == null) {
         return var1;
      } else {
         for (int var2 = 0; var2 < 36; var2++) {
            ItemStack var5 = mc.player.getInventory().getStack(var2);
            BlockItem var3;
            Item var4;
            if (var5 != null
               && !var5.isEmpty()
               && (var4 = var5.getItem()) instanceof BlockItem
               && (var3 = (BlockItem)var4).getBlock() instanceof ShulkerBoxBlock) {
               var1.add(var5);
            }
         }

         return var1;
      }
   }

   private void updateOverlayState() {
      if (mc.player == null) {
         if (overlayActive) {
            overlayActive = false;
            ShulkerOverlayRenderer.clearPops();
         }
      } else {
         boolean var1 = this.overlay.getValue()
            && this.isOn()
            && mc.currentScreen instanceof ShulkerBoxScreen
            && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler;
         if (!var1 && overlayActive) {
            ShulkerOverlayRenderer.clearPops();
         }

         overlayActive = var1;
      }
   }

   public static void renderOverlay(DrawContext var0) {
      AutoRegear var2 = INSTANCE;
      if (var2 != null && mc.player != null) {
         ScreenHandler var1 = mc.player.currentScreenHandler;
         if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            ShulkerBoxScreenHandler var3 = (ShulkerBoxScreenHandler)var1;
            var2.reprojectOverlayAnchor();
            int var4 = mc.getWindow().getScaledWidth();
            int var5 = mc.getWindow().getScaledHeight();
            ShulkerOverlayRenderer.layout(var4, var5, var2.getOverlayAnchorX(), var2.getOverlayAnchorY(), var2.overlayPanel);
            ShulkerOverlayRenderer.render(var0, var3, var2.overlayPanel[0], var2.overlayPanel[1], var3.getCursorStack());
            return;
         }
      }
   }

   public AutoRegear() {
      super("AutoRegear", Module.Category.Combat);
      this.rotate.setChinese("\u8f6c\u5934");
      this.timeoutTimer = new Timer();
      this.stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
      this.disableTime.setChinese("\u5173\u95ed\u5ef6\u8fdf");
      this.place = this.add(new BooleanSetting("Place", true));
      this.place.setChinese("\u653e\u7f6e");
      this.inventory = this.add(new BooleanSetting("Inventory", true));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.preferOpen = this.add(new BooleanSetting("PreferOpen", true));
      this.preferOpen.setChinese("\u4f18\u5148\u6253\u5f00");
      this.open = this.add(new BooleanSetting("Open", true));
      this.open.setChinese("\u6253\u5f00");
      this.range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1));
      this.range.setChinese("\u6700\u5927\u8303\u56f4");
      this.minRange = this.add(new SliderSetting("MinRange", 1.0, 0.0, 3.0, 0.1));
      this.minRange.setChinese("\u6700\u5c0f\u8303\u56f4");
      this.mine = this.add(new BooleanSetting("Mine", true));
      this.mine.setChinese("\u6316\u6398");
      this.take = this.add(new BooleanSetting("Take", true));
      this.take.setChinese("\u62ff\u53d6");
      this.silent = this.add(new BooleanSetting("Silent", true, this.take::getValue));
      this.silent.setChinese("\u9759\u9ed8\u8865\u7ed9");
      this.overlay.setChinese("\u6e32\u67d3\u754c\u9762");
      this.smart = this.add(new BooleanSetting("Smart", true, this.take::getValue).setParent());
      this.smart.setChinese("\u667a\u80fd");
      this.forceMove = this.add(new BooleanSetting("ForceMove", true, () -> this.take.getValue() && this.smart.isOpen()));
      this.forceMove.setChinese("\u5f3a\u5236\u5feb\u901f\u79fb\u52a8");
      this.crystal = this.add(new SliderSetting("Crystal", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.crystal.setChinese("\u6c34\u6676");
      this.exp = this.add(new SliderSetting("Exp", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.exp.setChinese("\u7ecf\u9a8c\u74f6");
      this.totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.take.getValue() && this.smart.isOpen()));
      this.totem.setChinese("\u56fe\u817e");
      this.gapple = this.add(new SliderSetting("Gapple", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.gapple.setChinese("\u91d1\u82f9\u679c");
      this.obsidian = this.add(new SliderSetting("Obsidian", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.obsidian.setChinese("\u9ed1\u66dc\u77f3");
      this.web = this.add(new SliderSetting("Web", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.web.setChinese("\u8718\u86db\u7f51");
      this.glowstone = this.add(new SliderSetting("Glowstone", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.glowstone.setChinese("\u8367\u77f3");
      this.anchor = this.add(new SliderSetting("Anchor", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.anchor.setChinese("\u951a");
      this.pearl = this.add(new SliderSetting("Pearl", 16, 0, 64, () -> this.take.getValue() && this.smart.isOpen()));
      this.pearl.setChinese("\u672b\u5f71\u73cd\u73e0");
      this.piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.piston.setChinese("\u6d3b\u585e");
      this.redstone = this.add(new SliderSetting("Redstone", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.redstone.setChinese("\u7ea2\u77f3");
      this.bed = this.add(new SliderSetting("Bed", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
      this.bed.setChinese("\u5e8a");
      this.speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.speed.setChinese("\u901f\u5ea6");
      this.resistance = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.resistance.setChinese("\u6297\u6027");
      this.strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
      this.strength.setChinese("\u529b\u91cf");
      this.placeKey = this.add(new BindSetting("PlaceKey", -1));
      this.placeKey.setChinese("\u653e\u7f6e\u952e");
      this.timer = new Timer();
      this.mineTimer = new Timer();
      this.openTimer = new Timer();
      this.openList = new ArrayList<>();
      this.failedPlacePositions = new HashMap<>();
      this.placePos = null;
      this.opend = false;
      this.on = false;
      this.activeHandlerSyncId = -1;
      this.readyHandlerSyncId = -1;
      this.emptyHandlerPasses = 0;
      this.movedInHandler = false;
      this.armorReplaceSlot = -1;
      this.placementAttempts = 0;
      this.openAttempts = 0;
      this.openAttemptPos = null;
      this.setChinese("\u81ea\u52a8\u8865\u7ed9");
      INSTANCE = this;
   }

   @EventListener
   private void onOpenScreen(OpenScreenEvent var1) {
   }

   @EventListener
   private void onInventorySync(InventoryS2CPacketEvent var1) {
      if (mc.player != null) {
         ScreenHandler var2 = mc.player.currentScreenHandler;
         if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            ShulkerBoxScreenHandler var3 = (ShulkerBoxScreenHandler)var2;
            if (var1.packet.getSyncId() == var3.syncId) {
               this.readyHandlerSyncId = var3.syncId;
               if (var3.syncId == this.activeHandlerSyncId && this.movedInHandler) {
                  this.emptyHandlerPasses = 0;
               }
            }
         }
      }
   }

   public BlockPos getCurrentShulker() {
      return this.openPos != null ? this.openPos : this.placePos;
   }

   public void mineCurrentShulkerSafe() {
      if (this.mine.getValue() && this.mineTimer.passed(200L)) {
         BlockPos var1 = this.getCurrentShulker();
         if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            mc.player.closeHandledScreen();
         }

         if (var1 != null && mc.world.getBlockState(var1).getBlock() instanceof ShulkerBoxBlock) {
            PacketMine.INSTANCE.mine(var1);
            this.mineTimer.reset();
         }
      }
   }

   public int findShulker() {
      if (this.inventory.getValue()) {
         for (int var1 = 0; var1 < 36; var1++) {
            ItemStack var4 = mc.player.getInventory().getStack(var1);
            BlockItem var2;
            Item var3;
            if (!var4.isEmpty() && (var3 = var4.getItem()) instanceof BlockItem && (var2 = (BlockItem)var3).getBlock() instanceof ShulkerBoxBlock) {
               return var1 < 9 ? var1 + 36 : var1;
            }
         }

         return -1;
      } else {
         return InventoryUtil.findClass(ShulkerBoxBlock.class);
      }
   }

   @Override
   public void onEnable() {
      if (this.minRange.getValue() < 1.5) {
         this.minRange.setValue(1.5);
      }

      this.opend = false;
      this.openPos = null;
      this.timeoutTimer.reset();
      this.mineTimer.reset();
      this.openTimer.reset();
      this.placePos = null;
      this.activeHandlerSyncId = -1;
      this.readyHandlerSyncId = -1;
      this.emptyHandlerPasses = 0;
      this.movedInHandler = false;
      this.armorReplaceSlot = -1;
      this.placementAttempts = 0;
      this.openAttempts = 0;
      this.openAttemptPos = null;
      this.failedPlacePositions.clear();
      if (!nullCheck() && this.place.getValue()) {
         this.setSupplyCombatPause(true);
         this.doPlace();
      }
   }

   private void doPlace() {
      this.doPlace(true);
   }

   private void doPlace(boolean var1) {
      int var2 = mc.player.getInventory().selectedSlot;
      double var3 = Double.MAX_VALUE;
      double var5 = 100.0;
      AutoRegear.PlacementCandidate var7 = null;
      BlockPos var8 = null;
      double var9 = Math.max(1.5, this.minRange.getValue());
      this.timer.reset();
      long var11 = System.currentTimeMillis();
      this.failedPlacePositions.entrySet().removeIf(var2x -> var11 - var2x.getValue() >= 2000L);

      for (BlockPos var14 : BlockUtil.getSphere((float)this.range.getValue())) {
         if (!this.failedPlacePositions.containsKey(var14)) {
            double var16 = MathHelper.sqrt((float)mc.player.squaredDistanceTo(var14.toCenterPos()));
            if (this.preferOpen.getValue()
               && !this.openList.contains(var14)
               && mc.world.getBlockState(var14).getBlock() instanceof ShulkerBoxBlock
               && this.hasOpeningClearance(var14)
               && BlockUtil.getClickSideStrict(var14) != null
               && (var8 == null || var16 < var5)) {
               var5 = var16;
               var8 = var14.toImmutable();
            }

            AutoRegear.PlacementCandidate var15;
            if (var16 >= var9 && (var15 = this.getPlacementCandidate(var14, var16)) != null && (var7 == null || var15.score() < var3)) {
               var3 = var15.score();
               var7 = var15;
            }
         }
      }

      if (var8 != null) {
         this.placePos = null;
         this.openPos = var8;
         this.timeoutTimer.reset();
      } else if (var7 != null) {
         if (this.placementAttempts >= 3) {
            return;
         }

         BlockPos var18 = var7.pos();
         int var19 = this.findShulker();
         if (var19 == -1) {
            this.sendMessage("\u00a74No shulkerbox found.");
            return;
         }

         if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(var19, var2);
            boolean var20 = this.placeBlock(var7);
            InventoryUtil.inventorySwap(var19, var2);
            if (!var20) {
               this.failedPlacePositions.put(var18, System.currentTimeMillis());
               return;
            }
         } else {
            InventoryUtil.switchToSlot(var19);
            boolean var21 = this.placeBlock(var7);
            InventoryUtil.switchToSlot(var2);
            if (!var21) {
               this.failedPlacePositions.put(var18, System.currentTimeMillis());
               return;
            }
         }

         this.placementAttempts++;
         this.placePos = var18;
         this.openPos = var18;
         this.openAttempts = 0;
         this.openAttemptPos = null;
         this.timer.reset();
         this.timeoutTimer.reset();
         this.openPlacedShulkerImmediately(var18);
      } else if (var1) {
         this.sendMessage("\u00a74No place position found.");
      }
   }

   private boolean hasOpeningClearance(BlockPos var1) {
      BlockState var2 = mc.world.getBlockState(var1);
      Direction var3 = var2.getBlock() instanceof ShulkerBoxBlock && var2.contains(Properties.FACING) ? (Direction)var2.get(Properties.FACING) : Direction.UP;
      return this.hasOpeningClearance(var1, var3);
   }

   private boolean hasOpeningClearance(BlockPos var1, Direction var2) {
      BlockPos var3 = var1.offset(var2);
      return mc.world.getBlockState(var3).getCollisionShape(mc.world, var3).isEmpty() && !BlockUtil.hasEntity(var3, false);
   }

   private AutoRegear.PlacementCandidate getPlacementCandidate(BlockPos var1, double var2) {
      if (BlockUtil.clientCanPlace(var1, false) && !this.isImmediateMovementObstacle(var1)) {
         AutoRegear.PlacementCandidate var4 = null;

         for (Direction var8 : Direction.values()) {
            BlockPos var10 = var1.offset(var8);
            Direction var9;
            if (this.canUsePlacementSupport(var1, var10, var9 = var8.getOpposite())) {
               double var11 = var9 == Direction.UP ? 0.0 : (var9 == Direction.DOWN ? 1.0 : 0.35);
               double var13 = mc.player
                  .getEyePos()
                  .squaredDistanceTo(var10.toCenterPos().add(var9.getOffsetX() * 0.5, var9.getOffsetY() * 0.5, var9.getOffsetZ() * 0.5));
               double var15 = this.getPlacementScore(var1, var2) + var11 + var13 * 0.025;
               if (var4 == null || var15 < var4.score()) {
                  var4 = new AutoRegear.PlacementCandidate(var1.toImmutable(), var10.toImmutable(), var9, var15);
               }
            }
         }

         return var4;
      } else {
         return null;
      }
   }

   private boolean canUsePlacementSupport(BlockPos var1, BlockPos var2, Direction var3) {
      return var1.offset(var3.getOpposite()).equals(var2)
         && BlockUtil.clientCanPlace(var1, false)
         && !BlockUtil.canReplace(var2)
         && BlockUtil.canClick(var2)
         && BlockUtil.isStrictDirection(var2, var3, this.range.getValue())
         && this.hasOpeningClearance(var1, var3)
         && !this.isImmediateMovementObstacle(var1);
   }

   private boolean isImmediateMovementObstacle(BlockPos var1) {
      Vec3d var2 = mc.player.getVelocity();
      double var3 = var2.x * var2.x + var2.z * var2.z;
      if (var3 < 9.0E-4) {
         return false;
      } else {
         Vec3d var5 = var1.toCenterPos().subtract(mc.player.getPos());
         double var6 = var5.x * var5.x + var5.z * var5.z;
         if (!(var6 > 6.25) && !(var6 < 1.0E-4)) {
            double var8 = (var2.x * var5.x + var2.z * var5.z) / Math.sqrt(var3 * var6);
            return var8 > 0.65;
         } else {
            return false;
         }
      }
   }

   private double getPlacementScore(BlockPos var1, double var2) {
      Vec3d var12 = var1.toCenterPos().subtract(mc.player.getPos());
      double var13 = Math.sqrt(var12.x * var12.x + var12.z * var12.z);
      double var15 = Math.abs(var1.getY() - mc.player.getBlockY()) * 3.0;
      if (var13 < 1.0E-4) {
         return var2 + var15;
      } else {
         Vec3d var17 = mc.player.getVelocity();
         double var18 = Math.sqrt(var17.x * var17.x + var17.z * var17.z);
         double var6;
         double var8;
         double var10;
         if (var18 > 0.03) {
            var10 = var17.x / var18;
            var8 = var17.z / var18;
            var6 = 4.0;
         } else {
            double var4 = Math.toRadians(mc.player.getYaw());
            var10 = -Math.sin(var4);
            var8 = Math.cos(var4);
            var6 = 1.5;
         }

         double var20 = Math.max(0.0, (var10 * var12.x + var8 * var12.z) / var13);
         return var2 + var15 + var20 * var6;
      }
   }

   private boolean isOpenableShulker(BlockPos var1) {
      return var1 != null
         && mc.world.getBlockState(var1).getBlock() instanceof ShulkerBoxBlock
         && MathHelper.sqrt((float)mc.player.squaredDistanceTo(var1.toCenterPos())) <= this.range.getValue()
         && this.hasOpeningClearance(var1)
         && BlockUtil.getClickSideStrict(var1) != null;
   }

   private BlockPos findNearestOpenShulker() {
      BlockPos var1 = null;
      double var2 = Double.MAX_VALUE;

      for (BlockPos var5 : BlockUtil.getSphere((float)this.range.getValue())) {
         double var6;
         if (!this.openList.contains(var5) && this.isOpenableShulker(var5) && (var6 = mc.player.squaredDistanceTo(var5.toCenterPos())) < var2) {
            var2 = var6;
            var1 = var5.toImmutable();
         }
      }

      return var1;
   }

   private void update() {
      this.stealCountList[0] = (int)(this.crystal.getValue() - InventoryUtil.getItemCount(Items.END_CRYSTAL));
      this.stealCountList[1] = (int)(this.exp.getValue() - InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
      this.stealCountList[2] = (int)(this.totem.getValue() - InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING));
      this.stealCountList[3] = (int)(this.gapple.getValue() - InventoryUtil.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
      this.stealCountList[4] = (int)(this.obsidian.getValue() - InventoryUtil.getItemCount(Blocks.OBSIDIAN.asItem()));
      this.stealCountList[5] = (int)(this.web.getValue() - InventoryUtil.getItemCount(Blocks.COBWEB.asItem()));
      this.stealCountList[6] = (int)(this.glowstone.getValue() - InventoryUtil.getItemCount(Blocks.GLOWSTONE.asItem()));
      this.stealCountList[7] = (int)(this.anchor.getValue() - InventoryUtil.getItemCount(Blocks.RESPAWN_ANCHOR.asItem()));
      this.stealCountList[8] = (int)(this.pearl.getValue() - InventoryUtil.getItemCount(Items.ENDER_PEARL));
      this.stealCountList[9] = (int)(
         this.piston.getValue() - InventoryUtil.getItemCount(Blocks.PISTON.asItem()) - InventoryUtil.getItemCount(Blocks.STICKY_PISTON.asItem())
      );
      this.stealCountList[10] = (int)(this.redstone.getValue() - InventoryUtil.getItemCount(Blocks.REDSTONE_BLOCK.asItem()));
      this.stealCountList[11] = (int)(this.bed.getValue() - InventoryUtil.getItemCount(BedBlock.class));
      this.stealCountList[12] = (int)(this.speed.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value()));
      this.stealCountList[13] = (int)(this.resistance.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value()));
      this.stealCountList[14] = (int)(this.strength.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value()));
   }

   @Override
   public void onDisable() {
      overlayActive = false;
      ShulkerOverlayRenderer.clearPops();
      this.opend = false;
      this.activeHandlerSyncId = -1;
      this.readyHandlerSyncId = -1;
      this.emptyHandlerPasses = 0;
      this.movedInHandler = false;
      if (!nullCheck() && this.placePos != null && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
         BlockUtil.placedPos.remove(this.placePos);
      }

      if (!nullCheck() && this.silent.getValue() && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
         mc.player.closeHandledScreen();
      }

      this.placePos = null;
      this.openPos = null;
      this.setSupplyCombatPause(false);
   }

   private void setSupplyCombatPause(boolean var1) {
      if (AutoCrystal.INSTANCE != null) {
         AutoCrystal.INSTANCE.setRefillPause(var1);
      }

      if (AutoWeb.INSTANCE != null) {
         AutoWeb.INSTANCE.setRefillPause(var1);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.updateOverlayState();
      if (this.autoDisable.getValue() && this.mine.getValue() && this.opend && this.timeoutTimer.passed(this.disableTime.getValueInt())) {
         this.mineCurrentShulkerSafe();
         this.disable();
      } else {
         if (this.smart.getValue()) {
            this.update();
         }

         if (this.placeKey.isPressed() && mc.currentScreen == null) {
            if (!this.on) {
               this.opend = false;
               this.openPos = null;
               this.timeoutTimer.reset();
               this.placePos = null;
               this.placementAttempts = 0;
               this.openAttempts = 0;
               this.openAttemptPos = null;
               this.doPlace();
            }

            this.on = true;
         } else {
            this.on = false;
         }

         this.openList.removeIf(var0 -> !(mc.world.getBlockState(var0).getBlock() instanceof ShulkerBoxBlock));
         if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            if (Refill.INSTANCE == null || !Refill.INSTANCE.isHandlingKit()) {
               this.setSupplyCombatPause(false);
            }

            this.activeHandlerSyncId = -1;
            this.readyHandlerSyncId = -1;
            this.emptyHandlerPasses = 0;
            this.movedInHandler = false;
            if (this.opend) {
               this.opend = false;
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

               if (this.mine.getValue() && this.openPos != null) {
                  this.mineCurrentShulkerSafe();
               }
            } else if (this.open.getValue()) {
               boolean var3 = this.placePos != null && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock);
               if (this.place.getValue() && var3 && this.timer.passed(600L)) {
                  this.failedPlacePositions.put(this.placePos.toImmutable(), System.currentTimeMillis());
                  BlockUtil.placedPos.remove(this.placePos);
                  this.placePos = null;
                  this.openPos = null;
                  this.openAttempts = 0;
                  this.openAttemptPos = null;
                  if (this.placementAttempts < 3) {
                     this.doPlace(false);
                  }
               } else if (this.place.getValue() && this.placePos == null && this.openPos == null && this.placementAttempts < 3 && this.timer.passed(250L)) {
                  this.doPlace(false);
               }

               BlockPos var2 = this.isOpenableShulker(this.openPos) && !this.openList.contains(this.openPos) ? this.openPos : this.findNearestOpenShulker();
               if (var2 != null) {
                  this.openPos = var2;
                  if (!var2.equals(this.openAttemptPos)) {
                     this.openAttemptPos = var2.toImmutable();
                     this.openAttempts = 0;
                     this.openTimer.reset();
                  }

                  if (this.openAttempts < 10 && this.openTimer.passed(0L)) {
                     Direction var6 = BlockUtil.getClickSideStrict(var2);
                     if (var6 != null) {
                        BlockUtil.clickBlock(var2, var6, this.rotate.getValue());
                        this.openAttempts++;
                        this.openTimer.reset();
                     }
                  } else if (this.openAttempts >= 10 && this.autoDisable.getValue()) {
                     this.timeoutToDisable();
                  }
               } else if (this.autoDisable.getValue() && !var3) {
                  this.timeoutToDisable();
               }
            } else if (!this.take.getValue() && this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }
         } else {
            this.setSupplyCombatPause(true);
            ShulkerBoxScreenHandler var13 = (ShulkerBoxScreenHandler)mc.player.currentScreenHandler;
            if (var13.syncId != this.activeHandlerSyncId) {
               this.activeHandlerSyncId = var13.syncId;
               this.emptyHandlerPasses = 0;
               this.movedInHandler = false;
            }

            this.opend = true;
            this.openAttempts = 0;
            this.openAttemptPos = null;
            if (this.openPos != null && !this.openList.contains(this.openPos)) {
               this.openList.add(this.openPos);
            }

            if (this.readyHandlerSyncId != var13.syncId) {
               return;
            }

            boolean var12 = Refill.INSTANCE != null && Refill.INSTANCE.isHandlingKit();
            if (var12) {
               this.emptyHandlerPasses = 0;
            } else if (!this.take.getValue()) {
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }
            } else {
               int var5 = 0;

               for (Slot var7 : var13.slots) {
                  if (var5 >= 8) {
                     break;
                  }

                  if (var7.id < 27 && !var7.getStack().isEmpty()) {
                     AutoRegear.Type var8 = this.needSteal(var7.getStack());
                     if (this.smart.getValue() && var8 != AutoRegear.Type.QuickMove && (var8 != AutoRegear.Type.Stack || !this.forceMove.getValue())) {
                        if (var8 == AutoRegear.Type.Stack) {
                           for (int var15 = 0; var15 < 36; var15++) {
                              ItemStack var10 = mc.player.getInventory().getStack(var15);
                              if (!var10.isEmpty()
                                 && var10.isStackable()
                                 && var10.getItem() == var7.getStack().getItem()
                                 && var10.getCount() < var10.getMaxCount()) {
                                 int var11 = (var15 < 9 ? var15 + 36 : var15) + 18;
                                 mc.interactionManager.clickSlot(var13.syncId, var7.id, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(var13.syncId, var11, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(var13.syncId, var7.id, 0, SlotActionType.PICKUP, mc.player);
                                 var5++;
                                 break;
                              }
                           }
                        }
                     } else {
                        mc.interactionManager.clickSlot(var13.syncId, var7.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                        if (this.armorReplaceSlot != -1) {
                           int var9 = (this.armorReplaceSlot < 9 ? this.armorReplaceSlot + 36 : this.armorReplaceSlot) + 18;
                           mc.interactionManager.clickSlot(var13.syncId, var9, 1, SlotActionType.THROW, mc.player);
                           this.armorReplaceSlot = -1;
                        }

                        var5++;
                     }
                  }
               }

               if (var5 > 0) {
                  this.emptyHandlerPasses = 0;
                  this.movedInHandler = true;
               } else {
                  this.emptyHandlerPasses++;
               }

               if (this.silent.getValue() && var5 == 0 && this.emptyHandlerPasses >= 2) {
                  mc.player.closeHandledScreen();
                  this.activeHandlerSyncId = -1;
                  this.readyHandlerSyncId = -1;
                  this.emptyHandlerPasses = 0;
                  this.movedInHandler = false;
               } else if (this.autoDisable.getValue() && var5 == 0) {
                  this.timeoutToDisable();
               }
            }
         }
      }
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed(this.disableTime.getValueInt())) {
         if (this.mine.getValue()) {
            this.mineCurrentShulkerSafe();
         }

         this.disable();
      }
   }

   private AutoRegear.Type needSteal(ItemStack var1) {
      if (var1.getItem().equals(Items.END_CRYSTAL) && this.stealCountList[0] > 0) {
         this.stealCountList[0] = this.stealCountList[0] - var1.getCount();
         return this.stealCountList[0] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Items.EXPERIENCE_BOTTLE) && this.stealCountList[1] > 0) {
         this.stealCountList[1] = this.stealCountList[1] - var1.getCount();
         return this.stealCountList[1] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Items.TOTEM_OF_UNDYING) && this.stealCountList[2] > 0) {
         this.stealCountList[2] = this.stealCountList[2] - var1.getCount();
         return this.stealCountList[2] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && this.stealCountList[3] > 0) {
         this.stealCountList[3] = this.stealCountList[3] - var1.getCount();
         return this.stealCountList[3] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Blocks.OBSIDIAN.asItem()) && this.stealCountList[4] > 0) {
         this.stealCountList[4] = this.stealCountList[4] - var1.getCount();
         return this.stealCountList[4] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Blocks.COBWEB.asItem()) && this.stealCountList[5] > 0) {
         this.stealCountList[5] = this.stealCountList[5] - var1.getCount();
         return this.stealCountList[5] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Blocks.GLOWSTONE.asItem()) && this.stealCountList[6] > 0) {
         this.stealCountList[6] = this.stealCountList[6] - var1.getCount();
         return this.stealCountList[6] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem()) && this.stealCountList[7] > 0) {
         this.stealCountList[7] = this.stealCountList[7] - var1.getCount();
         return this.stealCountList[7] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Items.ENDER_PEARL) && this.stealCountList[8] > 0) {
         this.stealCountList[8] = this.stealCountList[8] - var1.getCount();
         return this.stealCountList[8] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem() instanceof BlockItem && ((BlockItem)var1.getItem()).getBlock() instanceof PistonBlock && this.stealCountList[9] > 0) {
         this.stealCountList[9] = this.stealCountList[9] - var1.getCount();
         return this.stealCountList[9] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem().equals(Blocks.REDSTONE_BLOCK.asItem()) && this.stealCountList[10] > 0) {
         this.stealCountList[10] = this.stealCountList[10] - var1.getCount();
         return this.stealCountList[10] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (var1.getItem() instanceof BlockItem && ((BlockItem)var1.getItem()).getBlock() instanceof BedBlock && this.stealCountList[11] > 0) {
         this.stealCountList[11] = this.stealCountList[11] - var1.getCount();
         return this.stealCountList[11] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else {
         if (Item.getRawId(var1.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent var2 = (PotionContentsComponent)var1.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance var4 : var2.getEffects()) {
               if (var4.getEffectType().value() == StatusEffects.SPEED.value()) {
                  if (this.stealCountList[12] > 0) {
                     this.stealCountList[12] = this.stealCountList[12] - var1.getCount();
                     return this.stealCountList[12] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
                  }
               } else if (var4.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  if (this.stealCountList[13] > 0) {
                     this.stealCountList[13] = this.stealCountList[13] - var1.getCount();
                     return this.stealCountList[13] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
                  }
               } else if (var4.getEffectType().value() == StatusEffects.STRENGTH.value() && this.stealCountList[14] > 0) {
                  this.stealCountList[14] = this.stealCountList[14] - var1.getCount();
                  return this.stealCountList[14] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
               }
            }
         }

         if (InventoryUtil.isArmor(var1)) {
            int var5 = InventoryUtil.findLowerDurabilityArmor(var1);
            if (var5 != -1) {
               this.armorReplaceSlot = var5;
               return AutoRegear.Type.QuickMove;
            }
         }

         return AutoRegear.Type.None;
      }
   }

   private boolean placeBlock(AutoRegear.PlacementCandidate var1) {
      if (!this.canUsePlacementSupport(var1.pos(), var1.supportPos(), var1.clickSide())) {
         return false;
      } else {
         if (AntiRegear.INSTANCE != null) {
            AntiRegear.INSTANCE.safe.add(var1.pos());
         }

         BlockUtil.placedPos.add(var1.pos());
         BlockUtil.clickBlock(var1.supportPos(), var1.clickSide(), this.rotate.getValue());
         return true;
      }
   }

   private void openPlacedShulkerImmediately(BlockPos var1) {
      Hand var2;
      if (this.open.getValue() && var1 != null && mc.player != null && mc.getNetworkHandler() != null && (var2 = this.getSafeSpeculativeOpenHand()) != null) {
         BlockHitResult var3 = new BlockHitResult(var1.toCenterPos().add(0.0, 0.49, 0.0), Direction.UP, var1, false);
         sendSequencedPacket(var2x -> new PlayerInteractBlockC2SPacket(var2, var3, var2x));
         this.openAttemptPos = var1.toImmutable();
         this.openAttempts = 0;
         this.openTimer.reset();
      }
   }

   private Hand getSafeSpeculativeOpenHand() {
      if (!(mc.player.getOffHandStack().getItem() instanceof BlockItem)) {
         return Hand.OFF_HAND;
      } else {
         return !(mc.player.getMainHandStack().getItem() instanceof BlockItem) ? Hand.MAIN_HAND : null;
      }
   }

   private record PlacementCandidate(BlockPos pos, BlockPos supportPos, Direction clickSide, double score) {
   }

   private static enum Type {
      None,
      Stack,
      QuickMove;
   }
}
