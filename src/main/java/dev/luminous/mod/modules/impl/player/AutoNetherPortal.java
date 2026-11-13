package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoNetherPortal extends Module {
   public static AutoNetherPortal INSTANCE;
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", true));
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 2.0, 0.0, 20.0, 1.0).setSuffix("tick"));
   private final BooleanSetting ignite = this.add(new BooleanSetting("Ignite", true));
   private final SliderSetting igniteDelay = this.add(new SliderSetting("IgniteDelay", 2.0, 0.0, 20.0, 1.0).setSuffix("tick"));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private BlockPos base;
   private int placeIndex;
   private int placeClock;
   private int igniteClock;
   private boolean lit;

   public AutoNetherPortal() {
      super("AutoNetherPortal", "Builds and ignites a vanilla nether portal frame", Module.Category.Survival);
      this.setChinese("\u81ea\u52a8\u5730\u72f1\u95e8");
      this.ghostHand.setChinese("\u9b3c\u624b");
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.ignite.setChinese("\u6253\u706b");
      this.igniteDelay.setChinese("\u6253\u706b\u5ef6\u8fdf");
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.base = null;
      this.placeIndex = 0;
      this.placeClock = 0;
      this.igniteClock = 0;
      this.lit = false;
   }

   @Override
   public void onDisable() {
      this.base = null;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         if (this.base == null) {
            this.base = this.findBase();
         }

         if (this.base != null) {
            this.placeClock++;
            if (!this.frameComplete()) {
               int var2 = this.placeDelay.getValueInt();
               if (this.placeClock >= var2) {
                  BlockPos var3 = this.nextMissingFrameBlock();
                  if (var3 != null) {
                     if (this.placeObsidian(var3)) {
                        this.placeClock = 0;
                     }
                  }
               }
            } else if (this.ignite.getValue() && !this.lit) {
               this.igniteClock++;
               if (this.igniteClock >= this.igniteDelay.getValueInt()) {
                  if (this.ignitePortal()) {
                     this.lit = true;
                     if (this.autoDisable.getValue()) {
                        this.disable();
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

   private BlockPos findBase() {
      ClientPlayerEntity var1 = mc.player;
      BlockPos var2 = var1.getBlockPos();
      float var3 = var1.getYaw() * (float) (Math.PI / 180.0);
      int var4 = Math.round(-((float)Math.sin(var3)));
      int var5 = Math.round((float)Math.cos(var3));
      int var6 = -var5;
      return new BlockPos(var2.getX() + var4 * 2 - var6, var2.getY(), var2.getZ() + var5 * 2 - var4);
   }

   private BlockPos framePos(int var1) {
      int var2 = var1 / 4;
      int var3 = var1 % 4;
      float var4 = mc.player.getYaw() * (float) (Math.PI / 180.0);
      int var5 = Math.round(-((float)Math.sin(var4)));
      int var6 = Math.round((float)Math.cos(var4));
      int var7 = -var6;
      return new BlockPos(this.base.getX() + var7 * var3, this.base.getY() + var2, this.base.getZ() + var5 * var3);
   }

   private boolean isFrameIndex(int var1) {
      int var2 = var1 / 4;
      int var3 = var1 % 4;
      return var2 == 0 || var2 == 4 || var3 == 0 || var3 == 3;
   }

   private BlockPos nextMissingFrameBlock() {
      for (int var1 = this.placeIndex; var1 < 20; var1++) {
         if (this.isFrameIndex(var1)) {
            BlockPos var2 = this.framePos(var1);
            if (mc.world.isAir(var2)) {
               this.placeIndex = var1;
               return var2;
            }
         }
      }

      return null;
   }

   private boolean frameComplete() {
      for (int var1 = 0; var1 < 20; var1++) {
         if (this.isFrameIndex(var1) && mc.world.getBlockState(this.framePos(var1)).getBlock() != Blocks.OBSIDIAN) {
            return false;
         }
      }

      return true;
   }

   private boolean placeObsidian(BlockPos var1) {
      int var2 = mc.player.getInventory().selectedSlot;
      int var3 = this.ghostHand.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      if (var3 == -1) {
         return false;
      } else {
         if (this.ghostHand.getValue()) {
            InventoryUtil.inventorySwap(var3, var2);
            BlockUtil.placeBlock(var1, true);
            InventoryUtil.inventorySwap(var3, var2);
         } else {
            InventoryUtil.switchToSlot(var3);
            BlockUtil.placeBlock(var1, true);
            InventoryUtil.switchToSlot(var2);
         }

         return true;
      }
   }

   private boolean ignitePortal() {
      int var1 = mc.player.getInventory().selectedSlot;
      int var2 = this.ghostHand.getValue() ? InventoryUtil.findItemInventorySlot(Items.FLINT_AND_STEEL) : InventoryUtil.findItem(Items.FLINT_AND_STEEL);
      if (var2 == -1) {
         return false;
      } else {
         BlockPos var3 = this.framePos(4);
         Direction var4 = BlockUtil.getClickSide(var3);
         if (var4 == null) {
            var4 = Direction.UP;
         }

         if (this.ghostHand.getValue()) {
            InventoryUtil.inventorySwap(var2, var1);
            BlockUtil.clickBlock(var3, var4, true, Hand.MAIN_HAND);
            InventoryUtil.inventorySwap(var2, var1);
         } else {
            InventoryUtil.switchToSlot(var2);
            BlockUtil.clickBlock(var3, var4, true, Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(var1);
         }

         return true;
      }
   }
}
