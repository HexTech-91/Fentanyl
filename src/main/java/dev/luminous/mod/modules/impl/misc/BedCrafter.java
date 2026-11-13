package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class BedCrafter extends Module {
   public static BedCrafter INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final SliderSetting range;
   private final BooleanSetting disable;

   public BedCrafter() {
      super("BedCrafter", Module.Category.Survival);
      this.rotate.setChinese("\u8f6c\u5934");
      this.range = this.add(new SliderSetting("Range", 5, 0, 8));
      this.range.setChinese("\u8303\u56f4");
      this.disable = this.add(new BooleanSetting("Disable", true));
      this.disable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.setChinese("\u81ea\u52a8\u5236\u4f5c\u5e8a");
      INSTANCE = this;
   }

   public static int getEmptySlots() {
      int var0 = 0;

      for (int var1 = 0; var1 < 36; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.isEmpty()) {
            var0++;
         }
      }

      return var0;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (getEmptySlots() == 0) {
         if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            mc.player.closeHandledScreen();
         }

         if (this.disable.getValue()) {
            this.disable();
         }
      } else if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
         boolean var2 = false;

         for (RecipeResultCollection var4 : mc.player.getRecipeBook().getOrderedResults()) {
            for (RecipeEntry var6 : var4.getRecipes(true)) {
               if (var6.value().getResult(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                  for (int var7 = 0; var7 < getEmptySlots(); var7++) {
                     var2 = true;
                     mc.interactionManager.clickRecipe(mc.player.currentScreenHandler.syncId, var6, false);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                  }
                  break;
               }
            }
         }

         if (!var2) {
            if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
               mc.player.closeHandledScreen();
            }

            if (this.disable.getValue()) {
               this.disable();
            }
         }
      } else {
         this.doPlace();
      }
   }

   private void doPlace() {
      BlockPos var1 = null;
      double var2 = 100.0;
      boolean var4 = true;

      for (BlockPos var6 : BlockUtil.getSphere(this.range.getValueFloat())) {
         if (mc.world.getBlockState(var6).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(var6) != null) {
            var4 = false;
            var1 = var6;
            break;
         }

         if (BlockUtil.canPlace(var6) && (var1 == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(var6.toCenterPos())) < var2)) {
            var1 = var6;
            var2 = MathHelper.sqrt((float)mc.player.squaredDistanceTo(var6.toCenterPos()));
         }
      }

      if (var1 != null) {
         if (!var4) {
            BlockUtil.clickBlock(var1, BlockUtil.getClickSide(var1), this.rotate.getValue());
         } else {
            if (InventoryUtil.findItem(Items.CRAFTING_TABLE) == -1) {
               return;
            }

            int var7 = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.CRAFTING_TABLE));
            BlockUtil.placeBlock(var1, this.rotate.getValue());
            InventoryUtil.switchToSlot(var7);
         }
      }
   }
}
