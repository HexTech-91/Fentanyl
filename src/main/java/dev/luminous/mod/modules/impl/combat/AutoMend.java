package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoMend extends Module {
   public static AutoMend INSTANCE;
   public final BooleanSetting onlyBroken = this.add(new BooleanSetting("OnlyBroken", true));
   private final SliderSetting delay;
   private final BooleanSetting inventory;
   public final BooleanSetting autoDisable;
   private final Timer delayTimer;
   int exp;
   private boolean throwing;

   public AutoMend() {
      super("AutoMend", Module.Category.Combat);
      this.onlyBroken.setChinese("\u4ec5\u635f\u574f");
      this.delay = this.add(new SliderSetting("Delay", 3, 0, 5));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.inventory = this.add(new BooleanSetting("Inventory", true));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.delayTimer = new Timer();
      this.exp = 0;
      this.throwing = false;
      this.setChinese("\u81ea\u52a8\u7ecf\u9a8c\u74f6");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
      }
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.exp);
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.throwing = this.checkThrow();
      if (this.isThrow()
         && this.delayTimer.passed(this.delay.getValueInt() * 20L)
         && (!this.inventory.getValue() || InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1)) {
         this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
         this.throwExp();
      }

      if (this.autoDisable.getValue() && !this.isThrow()) {
         this.disable();
      }
   }

   public void throwExp() {
      if (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
         sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), 90.0F));
         EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
         this.delayTimer.reset();
      } else if (mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
         sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, var0, Alien.ROTATION.getLastYaw(), 90.0F));
         EntityUtil.swingHand(Hand.OFF_HAND, SwingSide.All);
         this.delayTimer.reset();
      } else {
         int var1;
         if (this.inventory.getValue() && (var1 = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
            int var2 = mc.player.getInventory().selectedSlot;
            InventoryUtil.inventorySwap(var1, var2);
            sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), 90.0F));
            EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
            InventoryUtil.inventorySwap(var1, var2);
            EntityUtil.syncInventory();
            this.delayTimer.reset();
         }
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow() {
      if (this.isOff()) {
         return false;
      } else if (mc.currentScreen != null) {
         return false;
      } else if (InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1
         || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1) {
         if (this.onlyBroken.getValue()) {
            for (ItemStack var2 : mc.player.getInventory().armor) {
               if (!var2.isEmpty() && EntityUtil.getDamagePercent(var2) < 100) {
                  ItemEnchantmentsComponent var3 = EnchantmentHelper.getEnchantments(var2);
                  return var3.getEnchantments()
                     .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.MENDING.getRegistryRef()).getOptional(Enchantments.MENDING).get());
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
