package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoSupplyPlus extends Module {
   public static AutoSupplyPlus INSTANCE;
   private final EnumSetting<AutoSupplyPlus.Mode> mode = this.add(new EnumSetting<>("Mode", AutoSupplyPlus.Mode.QuickMove));
   private final SliderSetting delay;
   private final SliderSetting min;
   private final SliderSetting forceDelay;
   private final SliderSetting forceMin;
   private long lastTime;

   public AutoSupplyPlus() {
      super("AutoSupplyPlus", "Replenishes hotbar items from inventory. \u81ea\u52a8\u8865\u7ed9\u70ed\u952e\u680f\u7269\u54c1", Module.Category.Player);
      this.mode.setChinese("\u6a21\u5f0f");
      this.delay = this.add(new SliderSetting("Delay", 2.0, 0.0, 5.0, 0.01));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.min = this.add(new SliderSetting("Min", 50.0, 1.0, 100.0, 1.0));
      this.min.setChinese("\u6700\u5c0f\u767e\u5206\u6bd4");
      this.forceDelay = this.add(new SliderSetting("ForceDelay", 0.2, 0.0, 4.0, 0.01));
      this.forceDelay.setChinese("\u5f3a\u5236\u5ef6\u8fdf");
      this.forceMin = this.add(new SliderSetting("ForceMin", 16.0, 1.0, 100.0, 1.0));
      this.forceMin.setChinese("\u5f3a\u5236\u6700\u5c0f");
      this.lastTime = 0L;
      this.setChinese("\u81ea\u52a8\u8865\u7ed9\uff0b");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.lastTime = 0L;
   }

   @EventListener
   private void onTick(ClientTickEvent var1) {
      if (!nullCheck() && !var1.isPost()) {
         for (int var2 = 0; var2 < 9; var2++) {
            if (this.tryReplenish(var2)) {
               return;
            }
         }
      }
   }

   private boolean tryReplenish(int var1) {
      ItemStack var2 = mc.player.getInventory().getStack(var1);
      if (var2.isEmpty()) {
         return false;
      } else if (!var2.isStackable()) {
         return false;
      } else {
         int var3 = (int)((double)var2.getCount() / var2.getMaxCount() * 100.0);
         if (var3 > this.min.getValueInt()) {
            return false;
         } else {
            for (int var4 = 9; var4 < 36; var4++) {
               ItemStack var5 = mc.player.getInventory().getStack(var4);
               if (!var5.isEmpty() && canStack(var2, var5)) {
                  long var6 = System.currentTimeMillis();
                  if (var3 > this.forceMin.getValueInt()) {
                     if (var6 - this.lastTime < (long)(this.delay.getValue() * 1000.0)) {
                        return false;
                     }
                  } else if (var6 - this.lastTime < (long)(this.forceDelay.getValue() * 1000.0)) {
                     return false;
                  }

                  int var8 = mc.player.playerScreenHandler.syncId;
                  switch ((AutoSupplyPlus.Mode)this.mode.getValue()) {
                     case QuickMove:
                        mc.interactionManager.clickSlot(var8, var4, 0, SlotActionType.QUICK_MOVE, mc.player);
                        break;
                     case ClickSlot:
                        mc.interactionManager.clickSlot(var8, var4, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(var8, var1 + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(var8, var4, 0, SlotActionType.PICKUP, mc.player);
                  }

                  this.lastTime = System.currentTimeMillis();
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static boolean canStack(ItemStack var0, ItemStack var1) {
      if (var0.isEmpty()) {
         return false;
      } else if (var1.isEmpty()) {
         return false;
      } else if (!ItemStack.areItemsAndComponentsEqual(var0, var1)) {
         return false;
      } else {
         return !var0.isStackable() ? false : var0.getCount() < var0.getMaxCount();
      }
   }

   public static enum Mode {
      QuickMove,
      ClickSlot;

      public String getChinese() {
         return switch (this) {
            case QuickMove -> "\u5feb\u901f\u79fb\u52a8";
            case ClickSlot -> "\u70b9\u51fb\u69fd\u4f4d";
         };
      }
   }
}
