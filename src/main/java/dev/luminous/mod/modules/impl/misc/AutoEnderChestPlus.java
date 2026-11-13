package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.misc.ShulkerColor;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.EnderChestOverlayRenderer;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Refill;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoEnderChestPlus extends Module {
   public static AutoEnderChestPlus INSTANCE;
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 250.0, 0.0, 5000.0, 10.0).setSuffix("ms"));
   private final SliderSetting openDelay = this.add(new SliderSetting("OpenDelay", 250.0, 0.0, 5000.0, 10.0).setSuffix("ms"));
   private final BooleanSetting autoClose = this.add(new BooleanSetting("AutoClose", true));
   private final SliderSetting closeDelay = this.add(new SliderSetting("CloseDelay", 500.0, 0.0, 5000.0, 10.0, () -> this.autoClose.getValue()).setSuffix("ms"));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final EnumSetting<AutoEnderChestPlus.TakeMode> mode = this.add(new EnumSetting<>("Mode", AutoEnderChestPlus.TakeMode.None));
   private final EnumSetting<ShulkerColor> color = this.add(
      new EnumSetting<>("Color", ShulkerColor.Cyan, () -> this.mode.getValue() == AutoEnderChestPlus.TakeMode.Color)
   );
   private final BooleanSetting silent = this.add(new BooleanSetting("Silent", false));
   private final SliderSetting takeDelay = this.add(new SliderSetting("TakeDelay", 0.0, 0.0, 1000.0, 10.0).setSuffix("ms"));
   private final SliderSetting takeCount = this.add(new SliderSetting("TakeCount", 3.0, 1.0, 27.0, 1.0));
   private final BooleanSetting useLayout = this.add(new BooleanSetting("Layout", true));
   private final BooleanSetting dropBlocking = this.add(new BooleanSetting("DropBlocking", true));
   private final BooleanSetting useNearby = this.add(new BooleanSetting("UseNearby", true));
   private final SliderSetting nearbyRange = this.add(
      new SliderSetting("NearbyRange", 6.0, 1.0, 10.0, 0.5, () -> this.useNearby.getValue())
   );
   private final Timer actionTimer = new Timer();
   private final Timer takeTimer = new Timer();
   private AutoEnderChestPlus.State state = AutoEnderChestPlus.State.WAIT_PLACE;
   private BlockPos placePos;
   private BooleanSetting wallhack;
   private final Set<Integer> cleared = new HashSet<>();
   private final Set<Integer> taken = new HashSet<>();
   private int takenTotal;
   private static boolean overlayActive = false;

   public AutoEnderChestPlus() {
      super("AutoEnderChestPlus", Module.Category.Misc);
      this.setChinese("\u81ea\u52a8\u672b\u5f71\u7bb1+");
      this.placeDelay.setChinese("\u653e\u51fa\u5ef6\u8fdf");
      this.openDelay.setChinese("\u6253\u5f00\u5ef6\u8fdf");
      this.autoClose.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.closeDelay.setChinese("\u5173\u95ed\u5ef6\u8fdf");
      this.autoDisable.setChinese("\u5b8c\u6210\u540e\u81ea\u52a8\u5173\u95ed");
      this.inventorySwap.setChinese("\u80cc\u5305\u5207\u6362");
      this.mode.setChinese("\u6a21\u5f0f");
      this.color.setChinese("\u989c\u8272");
      this.silent.setChinese("\u9759\u9ed8");
      this.takeDelay.setChinese("\u62ff\u53d6\u5ef6\u8fdf");
      this.useLayout.setChinese("\u5e03\u5c40");
      this.takeCount.setChinese("\u62ff\u53d6\u6570\u91cf");
      this.dropBlocking.setChinese("\u4e22\u5f03");
      this.useNearby.setChinese("\u4f7f\u7528\u9644\u8fd1\u672b\u5f71\u7bb1");
      this.nearbyRange.setChinese("\u9644\u8fd1\u8303\u56f4");
      this.wallhack = this.add(new BooleanSetting("Wallhack", true));
      this.wallhack.setChinese("\u9694\u5899\u6253\u5f00");
      INSTANCE = this;
   }

   public static boolean isOverlayActive() {
      return overlayActive;
   }

   public ShulkerColor selectedColor() {
      return this.mode.getValue() == AutoEnderChestPlus.TakeMode.Color ? this.color.getValue() : null;
   }

   public boolean isSilent() {
      return this.silent.getValue();
   }

   public double overlayAnchorX() {
      double var1 = mc.getWindow().getScaledWidth() / 2.0;
      if (this.placePos == null) {
         return var1;
      } else {
         Vec3d var3 = TextUtil.worldSpaceToScreenSpace(this.placePos.toCenterPos());
         return var3 != null && Double.isFinite(var3.x) ? var3.x : var1;
      }
   }

   public double overlayAnchorY() {
      double var1 = mc.getWindow().getScaledHeight() / 2.0;
      if (this.placePos == null) {
         return var1;
      } else {
         Vec3d var3 = TextUtil.worldSpaceToScreenSpace(this.placePos.toCenterPos());
         return var3 != null && Double.isFinite(var3.y) ? var3.y : var1;
      }
   }

   @Override
   public void onEnable() {
      this.reset();
      if (nullCheck()) {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      overlayActive = false;
      EnderChestOverlayRenderer.clearPops();
   }

   @Override
   public void onLogout() {
      this.disable();
   }

   private void silentDisable() {
      if (!this.autoDisable.getValue()) {
         this.reset();
      } else {
         this.disable();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         label107:
         switch (this.state) {
            case WAIT_PLACE:
               if (mc.currentScreen == null && this.actionTimer.passedMs(this.placeDelay.getValue())) {
                  if (this.useNearby.getValue()) {
                     BlockPos var5 = this.findNearbyEnderChest();
                     if (var5 != null) {
                        this.placePos = var5;
                        this.state = AutoEnderChestPlus.State.WAIT_OPEN;
                        this.actionTimer.setMs(Math.max(0L, (long)this.openDelay.getValue() - (long)this.placeDelay.getValue()));
                        break;
                     }
                  }

                  this.placePos = this.findPlacePos();
                  if (this.placePos != null && this.placeEnderChest()) {
                     this.state = AutoEnderChestPlus.State.WAIT_BLOCK;
                     this.actionTimer.reset();
                  }
               }
               break;
            case WAIT_BLOCK:
               if (this.isEnderChestPlaced()) {
                  this.state = AutoEnderChestPlus.State.WAIT_OPEN;
                  this.actionTimer.reset();
               } else if (this.actionTimer.passedMs(1200.0)) {
                  this.state = AutoEnderChestPlus.State.WAIT_PLACE;
                  this.placePos = null;
                  this.actionTimer.reset();
               }
               break;
            case WAIT_OPEN:
               if (!this.isEnderChestPlaced()) {
                  this.state = AutoEnderChestPlus.State.WAIT_PLACE;
                  this.placePos = null;
                  this.actionTimer.reset();
               } else if (mc.currentScreen == null && this.actionTimer.passedMs(this.openDelay.getValue())) {
                  BlockUtil.clickBlock(this.placePos, this.wallhack.getValue() ? Direction.UP : BlockUtil.getClickSide(this.placePos), true);
                  if (Refill.INSTANCE != null) {
                     Refill.INSTANCE.setLastShulkerPos(this.placePos);
                  }

                  this.state = AutoEnderChestPlus.State.WAIT_SCREEN;
                  this.actionTimer.reset();
               }
               break;
            case WAIT_SCREEN:
               if (mc.currentScreen instanceof GenericContainerScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                  this.state = AutoEnderChestPlus.State.TAKING;
                  this.taken.clear();
                  this.cleared.clear();
                  this.takenTotal = 0;
                  this.takeTimer.reset();
               } else if (this.actionTimer.passedMs(1500.0)) {
                  this.state = AutoEnderChestPlus.State.WAIT_OPEN;
                  this.actionTimer.reset();
               }
               break;
            case TAKING:
               if (mc.currentScreen instanceof GenericContainerScreen) {
                  ScreenHandler var3 = mc.player.currentScreenHandler;
                  if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                     GenericContainerScreenHandler var4 = (GenericContainerScreenHandler)var3;
                     if (this.takeTimer.passedMs(this.takeDelay.getValue())) {
                        this.takeAllInOneTick(var4);
                        this.taken.clear();
                        this.cleared.clear();
                        this.takenTotal = 0;
                        this.state = this.autoClose.getValue() ? AutoEnderChestPlus.State.WAIT_CLOSE : AutoEnderChestPlus.State.DONE;
                        this.actionTimer.reset();
                        break label107;
                     }
                     break;
                  }
               }

               this.state = AutoEnderChestPlus.State.WAIT_OPEN;
               this.actionTimer.reset();
               break;
            case WAIT_CLOSE:
               if (mc.currentScreen instanceof GenericContainerScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                  if (this.actionTimer.passedMs(this.closeDelay.getValue())) {
                     mc.player.closeHandledScreen();
                     this.state = AutoEnderChestPlus.State.DONE;
                     this.actionTimer.reset();
                  }
                  break;
               }

               this.state = AutoEnderChestPlus.State.DONE;
               this.actionTimer.reset();
               break;
            case DONE:
               if (this.actionTimer.passedMs(150.0)) {
                  this.silentDisable();
               }
         }

         boolean var2 = this.silent.getValue()
            && this.isOn()
            && mc.currentScreen instanceof GenericContainerScreen
            && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
         if (overlayActive && !var2) {
            EnderChestOverlayRenderer.clearPops();
         }

         overlayActive = var2;
      }
   }

   private void takeAllInOneTick(GenericContainerScreenHandler var1) {
      if (this.mode.getValue() != AutoEnderChestPlus.TakeMode.None) {
         if (!var1.getCursorStack().isEmpty()) {
            this.flushCursor(var1);
         }

         ShulkerColor var2 = this.selectedColor();
         int var3 = Math.min(27, var1.slots.size());
         int var4 = this.freeSlotCount(var1);
         if (var4 <= 0) {
            return;
         }

         int var5 = Math.min(Math.max(1, this.takeCount.getValueInt()), var4);

         for (int var6 = 0; var6 < var3 && var5 > 0; var6++) {
            ItemStack var7 = ((Slot)var1.slots.get(var6)).getStack();
            if (var7 != null && !var7.isEmpty() && this.matchesRequest(var7, var2)) {
               this.takeSlotIntoFreeSlot(var1, var6);
               var5--;
            }
         }

         if (var5 <= 0 || this.freeSlotCount(var1) <= 0) {
            this.reorderToLayout(var1);
         }
      }
   }

   private int freeSlotCount(GenericContainerScreenHandler var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         if (this.playerStack(var3).isEmpty() && this.toScreenSlot(var3, var1) >= 0) {
            var2++;
         }
      }

      return var2;
   }

   private void takeSlotIntoFreeSlot(GenericContainerScreenHandler var1, int var2) {
      int var3 = this.toScreenSlot(this.freeSlot(var1), var1);
      if (var3 >= 0) {
         mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(var1.syncId, var3, 0, SlotActionType.PICKUP, mc.player);
         if (!var1.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
         }

         this.takenTotal++;
      }
   }

   private int takeStep(GenericContainerScreenHandler var1) {
      if (this.mode.getValue() == AutoEnderChestPlus.TakeMode.None) {
         return 1;
      } else if (!var1.getCursorStack().isEmpty()) {
         return this.flushCursor(var1) ? 2 : 0;
      } else if (this.takenCount() >= this.takeCount.getValueInt()) {
         return this.reorderToLayout(var1) ? 2 : 1;
      } else {
         ShulkerColor var2 = this.selectedColor();
         int var3 = Math.min(27, var1.slots.size());

         for (int var4 = 0; var4 < var3; var4++) {
            ItemStack var5 = ((Slot)var1.slots.get(var4)).getStack();
            if (var5 != null && !var5.isEmpty() && this.matchesRequest(var5, var2)) {
               if (this.useLayout.getValue()) {
                  int var6 = this.pickLayoutSlotFor(var1, var5);
                  if (var6 >= 0 && this.takeShulkerToSlot(var1, var4, var6)) {
                     this.taken.add(var6);
                     this.takenTotal++;
                     return 2;
                  }
               }

               int var7 = this.chooseDestination(var1);
               if (var7 < 0) {
                  return 1;
               }

               mc.interactionManager.clickSlot(var1.syncId, var4, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.taken.add(var7);
               this.takenTotal++;
               return 2;
            }
         }

         return this.reorderToLayout(var1) ? 2 : 1;
      }
   }

   private boolean takeShulkerToSlot(GenericContainerScreenHandler var1, int var2, int var3) {
      int var4 = this.toScreenSlot(var3, var1);
      if (var4 < 0) {
         return false;
      } else {
         mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(var1.syncId, var4, 0, SlotActionType.PICKUP, mc.player);
         if (!var1.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
         }

         return true;
      }
   }

   private int takenCount() {
      return this.takenTotal;
   }

   private boolean flushCursor(GenericContainerScreenHandler var1) {
      ItemStack var3 = var1.getCursorStack();
      if (var3.isEmpty()) {
         return true;
      } else {
         int var4 = this.pickLayoutSlotFor(var1, var3);
         if (var4 < 0) {
            var4 = this.pickFreeSlot(var1);
         }

         int var2;
         if (var4 >= 0 && (var2 = this.toScreenSlot(var4, var1)) >= 0) {
            mc.interactionManager.clickSlot(var1.syncId, var2, 0, SlotActionType.PICKUP, mc.player);
            return true;
         } else {
            mc.interactionManager.clickSlot(var1.syncId, -999, 0, SlotActionType.THROW, mc.player);
            return true;
         }
      }
   }

   private boolean reorderToLayout(GenericContainerScreenHandler var1) {
      if (!this.useLayout.getValue()) {
         return false;
      } else {
         Map<Integer, String> var2 = this.plannedShulkerSlots();
         if (var2.isEmpty()) {
            return false;
         } else {
            HashMap<String, Integer> var3 = new HashMap<>();

            for (int var4 = 0; var4 < 36; var4++) {
               ItemStack var5 = this.playerStack(var4);
               if (ShulkerColor.isAnyShulker(var5)) {
                  var3.putIfAbsent(this.itemIdOf(var5), var4);
               }
            }

            for (Entry<Integer, String> var11 : var2.entrySet()) {
               int var7 = var11.getKey();
               String var8 = var11.getValue();
               ItemStack var9 = this.playerStack(var7);
               Integer var6;
               if ((!ShulkerColor.isAnyShulker(var9) || !var8.equals(this.itemIdOf(var9)))
                  && (var6 = (Integer)var3.get(var8)) != null
                  && var6 != var7
                  && this.swapInventorySlots(var1, var6, var7)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private int chooseDestination(GenericContainerScreenHandler var1) {
      Map<Integer, String> var3 = this.useLayout.getValue() ? this.plannedShulkerSlots() : new HashMap<>();

      for (int var2 : var3.keySet()) {
         if (this.playerStack(var2).isEmpty() && this.toScreenSlot(var2, var1) >= 0) {
            return var2;
         }
      }

      if (this.dropBlocking.getValue() && !var3.isEmpty()) {
         for (int var6 : var3.keySet()) {
            ItemStack var5 = this.playerStack(var6);
            if (!var5.isEmpty()
               && !this.cleared.contains(var6)
               && (!ShulkerColor.isAnyShulker(var5) || !var3.get(var6).equals(this.itemIdOf(var5)))
               && this.dropFromInventory(var1, var6)) {
               this.cleared.add(var6);
               this.takeTimer.setMs(Math.max(1L, (long)this.takeDelay.getValue() - 40L));
               return -1;
            }
         }
      }

      return this.pickFreeSlot(var1);
   }

   private int toScreenSlot(int var1, GenericContainerScreenHandler var2) {
      int var3 = var1 < 9 ? var1 + 36 : var1;
      int var4 = var3 + 18;
      return var4 < var2.slots.size() ? var4 : -1;
   }

   private ItemStack playerStack(int var1) {
      if (var1 >= 0 && var1 < 36) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         return var2 == null ? ItemStack.EMPTY : var2;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private Map<Integer, String> plannedShulkerSlots() {
      HashMap<Integer, String> var1 = new HashMap<>();
      if (Refill.INSTANCE == null) {
         return var1;
      } else {
         Map<Integer, Refill.KitRequirement> var2 = Refill.INSTANCE.getActiveKitRequirements();
         if (var2 != null && !var2.isEmpty()) {
            for (Entry<Integer, Refill.KitRequirement> var4 : var2.entrySet()) {
               if (var4.getKey() != null
                  && var4.getValue() != null
                  && var4.getValue().id() != null
                  && this.isShulkerItemId(var4.getValue().id())) {
                  var1.put(var4.getKey(), var4.getValue().id());
               }
            }

            return var1;
         } else {
            return var1;
         }
      }
   }

   private boolean isShulkerItemId(String var1) {
      return var1.endsWith("_shulker_box") || var1.equals("minecraft:shulker_box");
   }

   private String itemIdOf(ItemStack var1) {
      return var1 != null && !var1.isEmpty() ? Registries.ITEM.getId(var1.getItem()).toString() : "";
   }

   private int pickLayoutSlotFor(GenericContainerScreenHandler var1, ItemStack var2) {
      if (!this.useLayout.getValue()) {
         return -1;
      } else {
         String var3 = this.itemIdOf(var2);

         for (Entry var5 : this.plannedShulkerSlots().entrySet()) {
            if (((String)var5.getValue()).equals(var3)
               && this.playerStack((Integer)var5.getKey()).isEmpty()
               && this.toScreenSlot((Integer)var5.getKey(), var1) >= 0) {
               return (Integer)var5.getKey();
            }
         }

         return -1;
      }
   }

   private int freeSlot(GenericContainerScreenHandler var1) {
      return this.pickFreeSlot(var1);
   }

   private int pickFreeSlot(GenericContainerScreenHandler var1) {
      for (int var2 = 0; var2 < 36; var2++) {
         if (this.playerStack(var2).isEmpty() && this.toScreenSlot(var2, var1) >= 0) {
            return var2;
         }
      }

      return -1;
   }

   private boolean dropFromInventory(GenericContainerScreenHandler var1, int var2) {
      int var3 = this.toScreenSlot(var2, var1);
      if (var3 < 0) {
         return false;
      } else {
         mc.interactionManager.clickSlot(var1.syncId, var3, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(var1.syncId, -999, 0, SlotActionType.THROW, mc.player);
         return true;
      }
   }

   private boolean swapInventorySlots(GenericContainerScreenHandler var1, int var2, int var3) {
      int var4 = this.toScreenSlot(var2, var1);
      int var5 = this.toScreenSlot(var3, var1);
      if (var4 >= 0 && var5 >= 0) {
         mc.interactionManager.clickSlot(var1.syncId, var4, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(var1.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
         if (!var1.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(var1.syncId, var4, 0, SlotActionType.PICKUP, mc.player);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean matchesRequest(ItemStack var1, ShulkerColor var2) {
      if (!ShulkerColor.isAnyShulker(var1)) {
         return false;
      } else {
         return this.mode.getValue() != AutoEnderChestPlus.TakeMode.Color ? true : var2 != null && var2.matches(var1);
      }
   }

   private boolean placeEnderChest() {
      int var1 = mc.player.getInventory().selectedSlot;
      int var2 = InventoryUtil.findBlock(Blocks.ENDER_CHEST);
      if (var2 != -1) {
         InventoryUtil.switchToSlot(var2);
         BlockUtil.placeBlock(this.placePos, true);
         InventoryUtil.switchToSlot(var1);
         return true;
      } else if (!this.inventorySwap.getValue()) {
         return false;
      } else {
         int var3 = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
         if (var3 == -1) {
            return false;
         } else {
            InventoryUtil.inventorySwap(var3, var1);
            BlockUtil.placeBlock(this.placePos, true);
            InventoryUtil.inventorySwap(var3, var1);
            return true;
         }
      }
   }

   private BlockPos findPlacePos() {
      BlockPos var1 = null;
      double var2 = Double.MAX_VALUE;

      for (BlockPos var5 : BlockUtil.getSphere(3.0F)) {
         if (mc.world.isAir(var5.up()) && BlockUtil.canPlace(var5, 4.5)) {
            double var6 = mc.player.squaredDistanceTo(var5.toCenterPos());
            if (var6 < var2) {
               var2 = var6;
               var1 = var5;
            }
         }
      }

      return var1;
   }

   private BlockPos findNearbyEnderChest() {
      BlockPos var1 = null;
      double var2 = Double.MAX_VALUE;

      for (BlockPos var5 : BlockUtil.getSphere((float)this.nearbyRange.getValue())) {
         if (mc.world.getBlockState(var5).getBlock() == Blocks.ENDER_CHEST) {
            double var6 = mc.player.squaredDistanceTo(var5.toCenterPos());
            if (var6 < var2 && var6 <= this.nearbyRange.getValue() * this.nearbyRange.getValue()) {
               var2 = var6;
               var1 = var5;
            }
         }
      }

      return var1;
   }

   private boolean isEnderChestPlaced() {
      return this.placePos != null && mc.world.getBlockState(this.placePos).getBlock() == Blocks.ENDER_CHEST;
   }

   private void reset() {
      this.state = AutoEnderChestPlus.State.WAIT_PLACE;
      this.placePos = null;
      this.actionTimer.reset();
      this.takeTimer.reset();
      this.taken.clear();
      this.cleared.clear();
      this.takenTotal = 0;
      overlayActive = false;
      EnderChestOverlayRenderer.clearPops();
   }

   private static enum State {
      WAIT_PLACE,
      WAIT_BLOCK,
      WAIT_OPEN,
      WAIT_SCREEN,
      TAKING,
      WAIT_CLOSE,
      DONE;
   }

   public static enum TakeMode {
      None,
      Color;
   }
}
