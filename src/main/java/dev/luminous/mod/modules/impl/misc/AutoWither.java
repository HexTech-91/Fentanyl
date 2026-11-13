package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class AutoWither extends Module {
   private static final double PLACE_RANGE = 5.0;
   private static final long CONFIRM_TIMEOUT = 1500L;
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100.0, 0.0, 1000.0, 10.0).setSuffix("ms"));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", false));
   private final Timer timer = new Timer();
   private final Set<Integer> existingWithers = new HashSet<>();
   private List<AutoWither.Placement> structure = List.of();
   private int step;
   private int waitingStep = -1;
   private long waitingSince;
   private boolean waitingForWither;

   public AutoWither() {
      super("AutoWither", "Automatically builds a vanilla wither structure", Module.Category.Survival);
      this.setChinese("\u81ea\u52a8\u51cb\u96f6");
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.ghostHand.setChinese("\u9b3c\u624b");
   }

   @Override
   public void onEnable() {
      this.resetState();
      if (!nullCheck()) {
         for (Entity var2 : Alien.THREAD.getEntities()) {
            if (var2.getType() == EntityType.WITHER) {
               this.existingWithers.add(var2.getId());
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!var1.isPost() && !nullCheck()) {
         if (this.waitingForWither) {
            this.tryNameSpawnedWither();
         } else if (this.step >= 6 && this.findSpawnedWither() != null) {
            this.waitingForWither = true;
            this.tryNameSpawnedWither();
         } else {
            if (this.structure.isEmpty()) {
               if (!this.hasMaterials()) {
                  return;
               }

               this.structure = this.findStructure();
               if (this.structure.isEmpty()) {
                  return;
               }
            }

            if (this.step >= this.structure.size()) {
               this.waitingForWither = true;
               this.timer.reset();
            } else {
               AutoWither.Placement var2 = this.structure.get(this.step);
               if (this.isPlaced(var2)) {
                  this.step++;
                  this.waitingStep = -1;
                  this.timer.reset();
               } else {
                  if (this.waitingStep == this.step) {
                     if (System.currentTimeMillis() - this.waitingSince < 1500L) {
                        return;
                     }

                     if (this.step == this.structure.size() - 1) {
                        this.waitingForWither = true;
                        this.timer.reset();
                        return;
                     }

                     this.waitingStep = -1;
                  }

                  if (this.timer.passed(this.placeDelay.getValueInt())) {
                     int var3 = this.findBlockSlot(var2.block());
                     if (var3 != -1 && BlockUtil.canReplace(var2.pos()) && !BlockUtil.hasEntity(var2.pos(), false)) {
                        int var4 = mc.player.getInventory().selectedSlot;
                        this.swapTo(var3);
                        BlockUtil.placeBlock(var2.pos(), true, true);
                        this.swapBack(var3, var4);
                        this.waitingStep = this.step;
                        this.waitingSince = System.currentTimeMillis();
                        this.timer.reset();
                     } else {
                        this.pauseCurrentStructure();
                     }
                  }
               }
            }
         }
      }
   }

   private void resetState() {
      this.structure = List.of();
      this.step = 0;
      this.waitingStep = -1;
      this.waitingSince = 0L;
      this.waitingForWither = false;
      this.existingWithers.clear();
      this.timer.reset();
   }

   private void pauseCurrentStructure() {
      this.structure = List.of();
      this.step = 0;
      this.waitingStep = -1;
      this.timer.reset();
   }

   private boolean hasMaterials() {
      return this.countBlock(Blocks.SOUL_SAND) >= 4 && this.countBlock(Blocks.WITHER_SKELETON_SKULL) >= 3;
   }

   private int countBlock(Block var1) {
      int var2 = 0;
      int var3 = this.ghostHand.getValue() ? 36 : 9;

      for (int var4 = 0; var4 < var3; var4++) {
         ItemStack var5 = mc.player.getInventory().getStack(var4);
         if (var5.getItem() == var1.asItem()) {
            var2 += var5.getCount();
         }
      }

      return var2;
   }

   private List<AutoWither.Placement> findStructure() {
      BlockPos var1 = mc.player.getBlockPos();
      ArrayList<BlockPos> var2 = new ArrayList<>();

      for (int var3 = 1; var3 <= 4; var3++) {
         for (int var4 = -var3; var4 <= var3; var4++) {
            for (int var5 = -var3; var5 <= var3; var5++) {
               if (Math.max(Math.abs(var4), Math.abs(var5)) == var3) {
                  for (int var6 = -1; var6 <= 1; var6++) {
                     var2.add(var1.add(var4, var6, var5));
                  }
               }
            }
         }
      }

      for (BlockPos var8 : var2) {
         List var9 = this.makeStructure(var8, true);
         if (this.canBuild(var9)) {
            return var9;
         }

         List var10 = this.makeStructure(var8, false);
         if (this.canBuild(var10)) {
            return var10;
         }
      }

      return List.of();
   }

   private List<AutoWither.Placement> makeStructure(BlockPos var1, boolean var2) {
      int var3 = var2 ? 1 : 0;
      int var4 = var2 ? 0 : 1;
      BlockPos var5 = var1.add(0, 1, 0);
      return List.of(
         new AutoWither.Placement(var1, Blocks.SOUL_SAND),
         new AutoWither.Placement(var5, Blocks.SOUL_SAND),
         new AutoWither.Placement(var5.add(-var3, 0, -var4), Blocks.SOUL_SAND),
         new AutoWither.Placement(var5.add(var3, 0, var4), Blocks.SOUL_SAND),
         new AutoWither.Placement(var5.add(-var3, 1, -var4), Blocks.WITHER_SKELETON_SKULL),
         new AutoWither.Placement(var5.add(0, 1, 0), Blocks.WITHER_SKELETON_SKULL),
         new AutoWither.Placement(var5.add(var3, 1, var4), Blocks.WITHER_SKELETON_SKULL)
      );
   }

   private boolean canBuild(List<AutoWither.Placement> var1) {
      if (!BlockUtil.canPlace(((AutoWither.Placement)var1.get(0)).pos(), 5.0, false)) {
         return false;
      } else {
         for (AutoWither.Placement var3 : var1) {
            if (mc.player.getEyePos().squaredDistanceTo(var3.pos().toCenterPos()) > 25.0
               || !BlockUtil.canReplace(var3.pos())
               || BlockUtil.hasEntity(new Box(var3.pos()), false)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isPlaced(AutoWither.Placement var1) {
      Block var2 = mc.world.getBlockState(var1.pos()).getBlock();
      return var2 == var1.block() || var1.block() == Blocks.WITHER_SKELETON_SKULL && var2 == Blocks.WITHER_SKELETON_WALL_SKULL;
   }

   private int findBlockSlot(Block var1) {
      return this.ghostHand.getValue() ? InventoryUtil.findBlockInventorySlot(var1) : InventoryUtil.findBlock(var1);
   }

   private void swapTo(int var1) {
      if (this.ghostHand.getValue()) {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(var1);
      }
   }

   private void swapBack(int var1, int var2) {
      if (this.ghostHand.getValue()) {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         InventoryUtil.switchToSlot(var2);
      }
   }

   private void tryNameSpawnedWither() {
      Entity var1 = this.findSpawnedWither();
      int var2 = this.findNamedNameTag();
      if (var1 != null && var2 != -1) {
         int var3 = mc.player.getInventory().selectedSlot;
         this.swapItemTo(var2);
         mc.interactionManager.interactEntity(mc.player, var1, Hand.MAIN_HAND);
         EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
         this.swapItemBack(var2, var3);
         this.disable();
      } else if (var1 != null || this.timer.passed(5000L)) {
         this.disable();
      }
   }

   private Entity findSpawnedWither() {
      for (Entity var2 : Alien.THREAD.getEntities()) {
         if (var2.getType() == EntityType.WITHER && !this.existingWithers.contains(var2.getId()) && var2.isAlive() && var2.squaredDistanceTo(mc.player) <= 64.0
            )
          {
            return var2;
         }
      }

      return null;
   }

   private int findNamedNameTag() {
      int var1 = this.ghostHand.getValue() ? 36 : 9;

      for (int var2 = 0; var2 < var1; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.getItem() == Items.NAME_TAG && var3.isDamageable()) {
            return this.ghostHand.getValue() && var2 < 9 ? var2 + 36 : var2;
         }
      }

      return -1;
   }

   private void swapItemTo(int var1) {
      if (this.ghostHand.getValue()) {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(var1);
      }
   }

   private void swapItemBack(int var1, int var2) {
      if (this.ghostHand.getValue()) {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
      } else {
         InventoryUtil.switchToSlot(var2);
      }
   }

   private record Placement(BlockPos pos, Block block) {
   }
}
