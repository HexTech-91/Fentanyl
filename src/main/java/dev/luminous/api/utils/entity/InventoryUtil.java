package dev.luminous.api.utils.entity;

import dev.luminous.api.utils.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil implements Wrapper {
   public static int findBlock(Block block) {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == block.asItem()) {
            return i;
         }
      }

      return -1;
   }

   public static int findClass(Class<?> clazz) {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (clazz.isInstance(stack.getItem())) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlockInventorySlot(Block block) {
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == block.asItem()) {
            return i;
         }
      }

      return -1;
   }

   public static int findClassInventorySlot(Class<?> clazz) {
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (clazz.isInstance(stack.getItem())) {
            return i;
         }
      }

      return -1;
   }

   public static void switchToSlot(int slot) {
      if (slot >= 0 && slot < 9) {
         mc.player.getInventory().selectedSlot = slot;
      }
   }

   public static void inventorySwap(int src, int dest) {
      if (src != dest) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, src, dest, SlotActionType.SWAP, mc.player);
      }
   }

   public static void syncInventory() {
      EntityUtil.syncInventory();
   }

   public static class ShulkerBox {
      public int slot;
      public ItemStack stack;

      public ShulkerBox(int slot, ItemStack stack) {
         this.slot = slot;
         this.stack = stack;
      }

      public static boolean isShulkerBox(ItemStack stack) {
         return stack != null && !stack.isEmpty()
            ? stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock
            : false;
      }

      public static int findShulkerBoxSlot() {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = Wrapper.mc.player.getInventory().getStack(i);
            if (isShulkerBox(stack)) {
               return i;
            }
         }

         return -1;
      }
   }
}
