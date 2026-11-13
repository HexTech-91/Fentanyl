package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class CrossbowRapidFire extends Module {
   public static CrossbowRapidFire INSTANCE;
   private final BooleanSetting enable = this.add(new BooleanSetting("Enable", true));
   private final EnumSetting<CrossbowRapidFire.FireMode> fireMode;
   private final Timer fireTimer;

   public CrossbowRapidFire() {
      super(
         "CrossbowRapidFire",
         "\u6309\u4f4f\u53f3\u952e\u5f29\u8fde\u7eed\u53d1\u5c04\uff0c\u6b3a\u9a97\u5f29\u8ba4\u4e3a\u672a\u53d1\u5c04\u5b9e\u73b0\u79d2\u5c04",
         Module.Category.Combat
      );
      this.enable.setChinese("\u542f\u7528");
      this.fireMode = this.add(new EnumSetting<>("FireMode", CrossbowRapidFire.FireMode.Slow));
      this.fireMode.setChinese("\u5c04\u901f");
      this.fireTimer = new Timer();
      this.setChinese("\u5f29\u79d2\u5c04");
      INSTANCE = this;
   }

   private double getFireSpeed() {
      switch ((CrossbowRapidFire.FireMode)this.fireMode.getValue()) {
         case Fast:
            return 5.0;
         case Medium:
            return 1.5;
         case Slow:
         default:
            return 0.3;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.enable.getValue()) {
         if (!nullCheck()) {
            if (mc.options.useKey.isPressed()) {
               ItemStack stack = mc.player.getMainHandStack();
               Hand hand = Hand.MAIN_HAND;
               if (!(stack.getItem() instanceof CrossbowItem)) {
                  stack = mc.player.getOffHandStack();
                  hand = Hand.OFF_HAND;
                  if (!(stack.getItem() instanceof CrossbowItem)) {
                     return;
                  }
               }

               mc.itemUseCooldown = 0;
               if (mc.player.isUsingItem()) {
                  mc.player.stopUsingItem();
               }

               long delayMs = (long)(1000.0 / this.getFireSpeed());
               if (this.fireTimer.passedMs(delayMs)) {
                  mc.interactionManager.interactItem(mc.player, hand);
                  this.fireTimer.reset();
               }
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.enable.getValue() ? (int)this.getFireSpeed() + "/s" : "OFF";
   }

   public static enum FireMode {
      Fast,
      Medium,
      Slow;
   }
}
