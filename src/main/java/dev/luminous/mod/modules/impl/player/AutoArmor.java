package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;
   private final EnumSetting<AutoArmor.EnchantPriority> head = this.add(new EnumSetting<>("Head", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> body;
   private final EnumSetting<AutoArmor.EnchantPriority> tights;
   private final EnumSetting<AutoArmor.EnchantPriority> feet;
   private final BooleanSetting ignoreCurse;
   private final BooleanSetting noMove;
   private final SliderSetting delay;
   private final BooleanSetting autoElytra;
   private final EnumSetting<AutoArmor.HotbarSwapMode> hotbarSwap;
   private final EnumSetting<AutoArmor.InventorySwapMode> inventorySwap;
   private int tickDelay;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.head.setChinese("\u5934\u76d4");
      this.body = this.add(new EnumSetting<>("Body", AutoArmor.EnchantPriority.Protection));
      this.body.setChinese("\u80f8\u7532");
      this.tights = this.add(new EnumSetting<>("Tights", AutoArmor.EnchantPriority.Protection));
      this.tights.setChinese("\u62a4\u817f");
      this.feet = this.add(new EnumSetting<>("Feet", AutoArmor.EnchantPriority.Protection));
      this.feet.setChinese("\u978b\u5b50");
      this.ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
      this.ignoreCurse.setChinese("\u5ffd\u7565\u8bc5\u5492");
      this.noMove = this.add(new BooleanSetting("NoMove", false));
      this.noMove.setChinese("\u7981\u6b62\u79fb\u52a8");
      this.delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.autoElytra = this.add(new BooleanSetting("AutoElytra", true));
      this.autoElytra.setChinese("\u81ea\u52a8\u9798\u7fc5");
      this.hotbarSwap = this.add(new EnumSetting<>("HotbarSwap", AutoArmor.HotbarSwapMode.Swap));
      this.hotbarSwap.setChinese("\u70ed\u952e\u680f\u4ea4\u6362");
      this.inventorySwap = this.add(new EnumSetting<>("InventorySwap", AutoArmor.InventorySwapMode.ClickSlot));
      this.inventorySwap.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.tickDelay = 0;
      this.setChinese("\u81ea\u52a8\u7a7f\u7532");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (EntityUtil.inInventory() && mc.player.playerScreenHandler == mc.player.currentScreenHandler && (!MovementUtil.isMoving() || !this.noMove.getValue())) {
         if (this.tickDelay > 0) {
            this.tickDelay--;
         } else {
            this.tickDelay = this.delay.getValueInt();
            HashMap<EquipmentSlot, int[]> var2 = new HashMap<>(4);
            var2.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getStack(36)), -1, -1});
            var2.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getStack(37)), -1, -1});
            var2.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getStack(38)), -1, -1});
            var2.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getStack(39)), -1, -1});

            for (int var3 = 0; var3 < 36; var3++) {
               if ((mc.player.getInventory().getStack(var3).getItem() instanceof ArmorItem || mc.player.getInventory().getStack(var3).getItem() == Items.ELYTRA)
                  && (
                     mc.player.getInventory().getStack(var3).getItem() != Items.ELYTRA
                        || (!ElytraFly.INSTANCE.isOff() || !this.autoElytra.getValue())
                           && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.packet.getValue())
                  )) {
                  int var4 = this.getProtection(mc.player.getInventory().getStack(var3));
                  EquipmentSlot var5 = mc.player.getInventory().getStack(var3).getItem() instanceof ElytraItem
                     ? EquipmentSlot.CHEST
                     : ((ArmorItem)mc.player.getInventory().getStack(var3).getItem()).getSlotType();

                  for (Entry var7 : var2.entrySet()) {
                     if (this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && var7.getKey() == EquipmentSlot.CHEST) {
                        if ((
                              mc.player.getInventory().getStack(38).isEmpty()
                                 || !(mc.player.getInventory().getStack(38).getItem() instanceof ElytraItem)
                                 || !ElytraItem.isUsable(mc.player.getInventory().getStack(38))
                           )
                           && (
                              ((int[])var7.getValue())[2] == -1
                                 || mc.player.getInventory().getStack(((int[])var7.getValue())[2]).isEmpty()
                                 || !(mc.player.getInventory().getStack(((int[])var7.getValue())[2]).getItem() instanceof ElytraItem)
                                 || !ElytraItem.isUsable(mc.player.getInventory().getStack(((int[])var7.getValue())[2]))
                           )
                           && !mc.player.getInventory().getStack(var3).isEmpty()
                           && mc.player.getInventory().getStack(var3).getItem() instanceof ElytraItem
                           && ElytraItem.isUsable(mc.player.getInventory().getStack(var3))) {
                           ((int[])var7.getValue())[2] = var3;
                        }
                     } else if (var4 > 0 && var7.getKey() == var5 && var4 > ((int[])var7.getValue())[1] && var4 > ((int[])var7.getValue())[3]) {
                        ((int[])var7.getValue())[2] = var3;
                        ((int[])var7.getValue())[3] = var4;
                     }
                  }
               }
            }

            for (Entry var9 : var2.entrySet()) {
               if (((int[])var9.getValue())[2] != -1) {
                  if (((int[])var9.getValue())[2] < 9) {
                     switch ((AutoArmor.HotbarSwapMode)this.hotbarSwap.getValue()) {
                        case Swap:
                           int var11 = 44 - ((int[])var9.getValue())[0];
                           mc.interactionManager
                              .clickSlot(mc.player.currentScreenHandler.syncId, var11, ((int[])var9.getValue())[2], SlotActionType.SWAP, mc.player);
                           EntityUtil.syncInventory();
                           break;
                        case Switch:
                           int var10 = mc.player.getInventory().selectedSlot;
                           InventoryUtil.switchToSlot(((int[])var9.getValue())[2]);
                           sendSequencedPacket(
                              var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                           );
                           InventoryUtil.switchToSlot(var10);
                     }
                  } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                     int var12 = 44 - ((int[])var9.getValue())[0];
                     int var13 = ((int[])var9.getValue())[2];
                     switch ((AutoArmor.InventorySwapMode)this.inventorySwap.getValue()) {
                        case ClickSlot:
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var13, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var12, 0, SlotActionType.PICKUP, mc.player);
                           if (((int[])var9.getValue())[1] != -1) {
                              mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var13, 0, SlotActionType.PICKUP, mc.player);
                           }
                           break;
                        case Pick:
                           mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(var13));
                           sendSequencedPacket(
                              var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                           );
                           mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(var13));
                     }

                     EntityUtil.syncInventory();
                  }

                  return;
               }
            }
         }
      }
   }

   private int getProtection(ItemStack var1) {
      if (!(var1.getItem() instanceof ArmorItem) && var1.getItem() != Items.ELYTRA) {
         return !var1.isEmpty() ? 0 : -1;
      } else {
         int var3 = 0;
         EquipmentSlot var2;
         if (var1.getItem() instanceof ArmorItem var5) {
            var2 = var5.getSlotType();
         } else {
            var2 = EquipmentSlot.BODY;
         }

         if (var1.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(var1)) {
               return 0;
            }

            var3 = 1;
         }

         byte var9 = 1;
         byte var6 = 1;
         switch (var2) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  var6 *= 2;
               } else {
                  var9 *= 2;
               }
               break;
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  var6 *= 2;
               } else {
                  var9 *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  var6 *= 2;
               } else {
                  var9 *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  var6 *= 2;
               } else {
                  var9 *= 2;
               }
         }

         if (var1.hasEnchantments()) {
            ItemEnchantmentsComponent var7 = EnchantmentHelper.getEnchantments(var1);
            if (var7.getEnchantments()
               .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get())) {
               var3 += var7.getLevel(
                     (RegistryEntry)mc.world
                        .getRegistryManager()
                        .getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef())
                        .getOptional(Enchantments.PROTECTION)
                        .get()
                  )
                  * var6;
            }

            if (var7.getEnchantments()
               .contains(
                  mc.world
                     .getRegistryManager()
                     .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                     .getOptional(Enchantments.BLAST_PROTECTION)
                     .get()
               )) {
               var3 += var7.getLevel(
                     (RegistryEntry)mc.world
                        .getRegistryManager()
                        .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                        .getOptional(Enchantments.BLAST_PROTECTION)
                        .get()
                  )
                  * var9;
            }

            if (var7.getEnchantments()
                  .contains(
                     mc.world
                        .getRegistryManager()
                        .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                        .getOptional(Enchantments.BINDING_CURSE)
                        .get()
                  )
               && this.ignoreCurse.getValue()) {
               var3 = -999;
            }
         }

         int var10 = var1.getItem() instanceof ArmorItem ? ((ArmorItem)var1.getItem()).getProtection() : 0;
         int var8 = var10 + var3;
         if (var1.isDamageable() && var1.getDamage() == 0) {
            var8++;
         }

         return var8;
      }
   }

   private static enum EnchantPriority {
      Blast,
      Protection;
   }

   public static enum HotbarSwapMode {
      Swap,
      Switch;
   }

   public static enum InventorySwapMode {
      ClickSlot,
      Pick;
   }
}
