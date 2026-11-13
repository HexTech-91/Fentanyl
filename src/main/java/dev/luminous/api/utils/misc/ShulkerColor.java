package dev.luminous.api.utils.misc;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

public enum ShulkerColor {
   White,
   Orange,
   Magenta,
   LightBlue,
   Yellow,
   Lime,
   Pink,
   Gray,
   LightGray,
   Cyan,
   Purple,
   Blue,
   Brown,
   Green,
   Red,
   Black;

   private static DyeColor[] DYE_BY_ORDINAL;

   public DyeColor dye() {
      DyeColor[] var1 = table();
      return var1[this.ordinal()];
   }

   private static DyeColor[] table() {
      if (DYE_BY_ORDINAL != null) {
         return DYE_BY_ORDINAL;
      } else {
         DyeColor[] var0 = DyeColor.values();
         ShulkerColor[] var1 = values();
         DyeColor[] var2 = new DyeColor[var1.length];

         for (int var3 = 0; var3 < var1.length; var3++) {
            String var4 = var1[var3].name().toLowerCase().replace("_", "");
            DyeColor var5 = var3 < var0.length ? var0[var3] : null;

            for (DyeColor var9 : var0) {
               if (var9.asString().replace("_", "").equalsIgnoreCase(var4)) {
                  var5 = var9;
                  break;
               }
            }

            var2[var3] = var5;
         }

         DYE_BY_ORDINAL = var2;
         return var2;
      }
   }

   public static ShulkerColor of(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         DyeColor var1 = ShulkerBoxBlock.getColor(var0.getItem());
         return var1 == null ? null : fromDye(var1);
      } else {
         return null;
      }
   }

   public static ShulkerColor fromDye(DyeColor var0) {
      if (var0 == null) {
         return null;
      } else {
         ShulkerColor[] var1 = values();
         DyeColor[] var2 = table();

         for (int var3 = 0; var3 < var2.length; var3++) {
            if (var2[var3] == var0) {
               return var1[var3];
            }
         }

         return null;
      }
   }

   public boolean matches(ItemStack var1) {
      return of(var1) == this;
   }

   public static boolean isAnyShulker(ItemStack var0) {
      return var0 != null && !var0.isEmpty() ? var0.getItem() instanceof BlockItem var1 && var1.getBlock() instanceof ShulkerBoxBlock : false;
   }
}
