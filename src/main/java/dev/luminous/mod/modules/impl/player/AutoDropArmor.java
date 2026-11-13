package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoDropArmor extends Module {
   public static AutoDropArmor INSTANCE;
   private final SliderSetting durability = this.add(new SliderSetting("Durability", 10, 1, 100));
   private final SliderSetting dropDelay = this.add(new SliderSetting("DropDelay", 200.0, 0.0, 2000.0, 10.0).setSuffix("ms"));
   private final BooleanSetting inventoryOnly = this.add(new BooleanSetting("InventoryOnly", false));
   private final BooleanSetting includeHelmet = this.add(new BooleanSetting("IncludeHelmet", true));
   private final BooleanSetting notify = this.add(new BooleanSetting("Notify", true));
   private final Timer timer = new Timer();

   public AutoDropArmor() {
      super("AutoDropArmor", "\u81ea\u52a8\u4e22\u5f03\u8010\u4e45\u8fc7\u4f4e\u7684\u62a4\u7532", Module.Category.Player);
      this.durability.setChinese("\u8010\u4e45");
      this.dropDelay.setChinese("\u4e22\u5f03\u5ef6\u8fdf");
      this.inventoryOnly.setChinese("\u53ea\u4e22\u80cc\u5305");
      this.includeHelmet.setChinese("\u5305\u542b\u5934\u76d4");
      this.notify.setChinese("\u63d0\u793a");
      this.setChinese("\u81ea\u52a8\u4e22\u7532");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.timer.reset();
   }

   @EventListener
   public void onTick(TickEvent var1) {
      if (!nullCheck()) {
         if (this.timer.passedMs(this.dropDelay.getValue())) {
            int var2 = this.findArmorSlot();
            if (var2 != -1) {
               this.dropSlot(var2);
               this.timer.reset();
            }
         }
      }
   }

   private int findArmorSlot() {
      int var1 = this.inventoryOnly.getValue() ? 36 : 40;

      for (int var2 = 0; var2 < var1; var2++) {
         if (this.shouldDrop(mc.player.getInventory().getStack(var2))) {
            return var2;
         }
      }

      return -1;
   }

   private boolean shouldDrop(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.getItem() instanceof ArmorItem var2) {
            if (!this.includeHelmet.getValue() && var2.getSlotType() == EquipmentSlot.HEAD) {
               return false;
            } else if (!var1.isDamageable()) {
               return false;
            } else {
               int var5 = var1.getMaxDamage();
               if (var5 <= 0) {
                  return false;
               } else {
                  int var4 = var5 - var1.getDamage();
                  return var4 <= this.durability.getValueInt();
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void dropSlot(int var1) {
      int var2 = var1 < 9 ? var1 + 36 : var1;
      int var3 = mc.player.playerScreenHandler.syncId;
      ItemStack var4 = mc.player.getInventory().getStack(var1);
      String var5 = var4.getName().getString();
      mc.interactionManager.clickSlot(var3, var2, 1, SlotActionType.THROW, mc.player);
      if (this.notify.getValue()) {
         this.sendMessage("\u00a7f[\u00a7b\u81ea\u52a8\u4e22\u7532\u00a7f] \u5df2\u4e22\u5f03 " + var5);
      }
   }
}
