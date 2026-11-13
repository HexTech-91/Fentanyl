package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ArmorItem.Type;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil implements Wrapper {
   static int lastSlot = -1;
   static int lastSelect = -1;

   public static void inventorySwap(int var0, int var1) {
      if (var0 == lastSlot) {
         switchToSlot(lastSelect);
         lastSlot = -1;
         lastSelect = -1;
      } else if (var0 < 9 || var0 > 35 || var0 - 9 != var1) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var0, var1, SlotActionType.SWAP, mc.player);
      }
   }

   public static void switchToSlot(int var0) {
      mc.player.getInventory().selectedSlot = var0;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(var0));
   }

   public static boolean holdingItem(Class<?> var0) {
      ItemStack var1 = mc.player.getMainHandStack();
      boolean var2 = isInstanceOf(var1, var0);
      if (!var2) {
         var2 = isInstanceOf(mc.player.getOffHandStack(), var0);
      }

      return var2;
   }

   public static boolean isInstanceOf(ItemStack var0, Class<?> var1) {
      if (var0 == null) {
         return false;
      } else {
         Item var2 = var0.getItem();
         if (var1.isInstance(var2)) {
            return true;
         } else if (var2 instanceof BlockItem) {
            Block var3 = ((BlockItem)var2).getBlock();
            return var1.isInstance(var3);
         } else {
            return false;
         }
      }
   }

   public static ItemStack getStackInSlot(int var0) {
      return mc.player.getInventory().getStack(var0);
   }

   public static int findItem(Item var0) {
      for (int var1 = 0; var1 < 9; var1++) {
         Item var2 = getStackInSlot(var1).getItem();
         if (Item.getRawId(var2) == Item.getRawId(var0)) {
            return var1;
         }
      }

      return -1;
   }

   public static int getFood() {
      for (int var0 = 0; var0 < 9; var0++) {
         if (mc.player.getInventory().getStack(var0).getComponents().contains(DataComponentTypes.FOOD)) {
            return var0;
         }
      }

      return -1;
   }

   public static int getPotionCount(StatusEffect var0) {
      int var1 = 0;

      for (int var2 = 0; var2 < 45; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (Item.getRawId(var3.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent var4 = (PotionContentsComponent)var3.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance var6 : var4.getEffects()) {
               if (var6.getEffectType().value() == var0) {
                  var1 += var3.getCount();
               }
            }
         }
      }

      return var1;
   }

   public static int getItemCount(Class<?> var0) {
      int var1 = 0;

      for (Entry var3 : getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)var3.getValue()).getItem() instanceof BlockItem && var0.isInstance(((BlockItem)((ItemStack)var3.getValue()).getItem()).getBlock())) {
            var1 += ((ItemStack)var3.getValue()).getCount();
         }
      }

      return var1;
   }

   public static int getItemCount(Item var0) {
      int var1 = 0;

      for (Entry var3 : getInventoryAndHotbarSlots().entrySet()) {
         if (((ItemStack)var3.getValue()).getItem() == var0) {
            var1 += ((ItemStack)var3.getValue()).getCount();
         }
      }

      if (mc.player.getOffHandStack().getItem() == var0) {
         var1 += mc.player.getOffHandStack().getCount();
      }

      return var1;
   }

   public static int findClass(Class<?> var0) {
      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var3 = getStackInSlot(var1);
         if (!var3.isEmpty()) {
            if (var0.isInstance(var3.getItem())) {
               return var1;
            }

            if (var3.getItem() instanceof BlockItem && var0.isInstance(((BlockItem)var3.getItem()).getBlock())) {
               return var1;
            }
         }
      }

      return -1;
   }

   public static int findClassInventorySlot(Class<?> var0) {
      for (int var1 = 0; var1 < 45; var1++) {
         ItemStack var3 = mc.player.getInventory().getStack(var1);
         if (!var3.isEmpty()) {
            if (var0.isInstance(var3.getItem())) {
               return var1 < 9 ? var1 + 36 : var1;
            }

            if (var3.getItem() instanceof BlockItem && var0.isInstance(((BlockItem)var3.getItem()).getBlock())) {
               return var1 < 9 ? var1 + 36 : var1;
            }
         }
      }

      return -1;
   }

   public static int findBlock(Block var0) {
      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = getStackInSlot(var1);
         if (!var2.isEmpty() && var2.getItem() instanceof BlockItem && ((BlockItem)var2.getItem()).getBlock() == var0) {
            return var1;
         }
      }

      return -1;
   }

   public static int findUnBlock() {
      for (int var0 = 0; var0 < 9; var0++) {
         ItemStack var1 = getStackInSlot(var0);
         if (!(var1.getItem() instanceof BlockItem)) {
            return var0;
         }
      }

      return -1;
   }

   public static int findBlock() {
      for (int var0 = 0; var0 < 9; var0++) {
         ItemStack var2 = getStackInSlot(var0);
         Block var1;
         if (var2.getItem() instanceof BlockItem && !BlockUtil.isClickable(var1 = Block.getBlockFromItem(var2.getItem())) && var1 != Blocks.COBWEB) {
            return var0;
         }
      }

      return -1;
   }

   public static int findBlockInventorySlot(Block var0) {
      return findItemInventorySlot(var0.asItem());
   }

   public static int findItemInventorySlot(Item var0) {
      for (int var1 = 0; var1 < 45; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.getItem() == var0) {
            return var1 < 9 ? var1 + 36 : var1;
         }
      }

      return -1;
   }

   public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
      HashMap var0 = new HashMap();

      for (int var1 = 0; var1 <= 44; var1++) {
         var0.put(var1, mc.player.getInventory().getStack(var1));
      }

      return var0;
   }

   public static int findItemInventorySlotFromZero(Item var0) {
      for (int var1 = 0; var1 < 36; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.getItem() == var0) {
            return var1 < 9 ? var1 + 36 : var1;
         }
      }

      return -1;
   }

   public static int getPotCount(StatusEffect var0) {
      int var1 = 0;

      for (Entry var3 : getInventoryAndHotbarSlots().entrySet()) {
         ItemStack var5 = (ItemStack)var3.getValue();
         PotionContentsComponent var4;
         if (var5.getItem() instanceof PotionItem && (var4 = (PotionContentsComponent)var5.get(DataComponentTypes.POTION_CONTENTS)) != null) {
            for (StatusEffectInstance var7 : var4.getEffects()) {
               if (var7.getEffectType().value() == var0) {
                  var1 += var5.getCount();
                  break;
               }
            }
         }
      }

      return var1;
   }

   public static int findPotInventorySlot(StatusEffect var0) {
      for (int var1 = 0; var1 < 45; var1++) {
         ItemStack var3 = mc.player.getInventory().getStack(var1);
         PotionContentsComponent var2;
         if (!var3.isEmpty() && var3.getItem() instanceof PotionItem && (var2 = (PotionContentsComponent)var3.get(DataComponentTypes.POTION_CONTENTS)) != null) {
            for (StatusEffectInstance var5 : var2.getEffects()) {
               if (var5.getEffectType().value() == var0) {
                  return var1 < 9 ? var1 + 36 : var1;
               }
            }
         }
      }

      return -1;
   }

   public static int findPot(StatusEffect var0) {
      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var3 = getStackInSlot(var1);
         PotionContentsComponent var2;
         if (!var3.isEmpty() && var3.getItem() instanceof PotionItem && (var2 = (PotionContentsComponent)var3.get(DataComponentTypes.POTION_CONTENTS)) != null) {
            for (StatusEffectInstance var5 : var2.getEffects()) {
               if (var5.getEffectType().value() == var0) {
                  return var1;
               }
            }
         }
      }

      return -1;
   }

   public static int getArmorCount(Type var0) {
      int var1 = 0;

      for (Entry var3 : getInventoryAndHotbarSlots().entrySet()) {
         Item var4 = ((ItemStack)var3.getValue()).getItem();
         if (var4 instanceof ArmorItem && ((ArmorItem)var4).getSlotType() == var0.getEquipmentSlot()) {
            var1 += ((ItemStack)var3.getValue()).getCount();
         }
      }

      return var1;
   }

   public static int getClassCount(Class<?> var0) {
      int var1 = 0;

      for (Entry var3 : getInventoryAndHotbarSlots().entrySet()) {
         ItemStack var5 = (ItemStack)var3.getValue();
         if (!var5.isEmpty()) {
            if (var0.isInstance(var5.getItem())) {
               var1 += var5.getCount();
            }

            if (var5.getItem() instanceof BlockItem && var0.isInstance(((BlockItem)var5.getItem()).getBlock())) {
               var1 += var5.getCount();
            }
         }
      }

      return var1;
   }

   public static boolean checkArmorType(Item var0, Type var1) {
      return !(var0 instanceof ArmorItem) ? false : ((ArmorItem)var0).getSlotType() == var1.getEquipmentSlot();
   }

   public static boolean isArmor(ItemStack var0) {
      return var0 != null && !var0.isEmpty() && var0.getItem() instanceof ArmorItem;
   }

   public static boolean isDamageableArmor(ItemStack var0) {
      return isArmor(var0) && var0.isDamageable();
   }

   public static int getArmorDurabilityPercent(ItemStack var0) {
      if (isArmor(var0) && var0.isDamageable()) {
         int var1 = var0.getMaxDamage();
         if (var1 <= 0) {
            return 100;
         } else {
            int var2 = var0.getDamage();
            return (int)((double)(var1 - var2) / var1 * 100.0);
         }
      } else {
         return -1;
      }
   }

   public static int findLowerDurabilityArmor(ItemStack var0) {
      if (!isArmor(var0)) {
         return -1;
      } else {
         EquipmentSlot var1 = ((ArmorItem)var0.getItem()).getSlotType();
         Item var2 = var0.getItem();
         int var3 = getArmorDurabilityPercent(var0);
         int var4 = -1;
         int var5 = Integer.MAX_VALUE;

         for (int var6 = 0; var6 < 36; var6++) {
            ItemStack var7 = mc.player.getInventory().getStack(var6);
            if (isArmor(var7) && var7.getItem() == var2 && ((ArmorItem)var7.getItem()).getSlotType() == var1) {
               int var8 = getArmorDurabilityPercent(var7);
               if (var8 >= 0 && var8 < var3 && var8 < var5) {
                  var4 = var6;
                  var5 = var8;
               }
            }
         }

         return var4;
      }
   }

   public static int findHigherDurabilityArmor(ItemStack var0) {
      if (!isArmor(var0)) {
         return -1;
      } else {
         EquipmentSlot var1 = ((ArmorItem)var0.getItem()).getSlotType();
         Item var2 = var0.getItem();
         int var3 = getArmorDurabilityPercent(var0);
         int var4 = -1;
         int var5 = -1;

         for (int var6 = 0; var6 < 36; var6++) {
            ItemStack var7 = mc.player.getInventory().getStack(var6);
            if (isArmor(var7) && var7.getItem() == var2 && ((ArmorItem)var7.getItem()).getSlotType() == var1) {
               int var8 = getArmorDurabilityPercent(var7);
               if (var8 >= 0 && var8 > var3 && var8 > var5) {
                  var4 = var6;
                  var5 = var8;
               }
            }
         }

         return var4;
      }
   }
}
