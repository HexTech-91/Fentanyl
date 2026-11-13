package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Map;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FakeOpPot extends Module {
   public static FakeOpPot INSTANCE;
   private final BooleanSetting freezeDuration = this.add(new BooleanSetting("Freeze", true));
   private final SliderSetting stackAmount;
   private final SliderSetting maxLevel;
   private boolean wasUsing;

   public FakeOpPot() {
      super("FakeOpPot", Module.Category.Combat);
      this.freezeDuration.setChinese("\u51bb\u7ed3\u65f6\u957f");
      this.stackAmount = this.add(new SliderSetting("Stack", 4.0, 0.0, 20.0, 1.0));
      this.stackAmount.setChinese("\u6bcf\u7ea7\u5438\u6536\u8840\u91cf");
      this.maxLevel = this.add(new SliderSetting("MaxLevel", 255, 1, 255));
      this.maxLevel.setChinese("\u6700\u5927\u7b49\u7ea7");
      this.setChinese("\u4f2aop\u836f");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      StatusEffectInstance var1 = this.getAbsorption();
      return var1 != null ? "Lv." + (var1.getAmplifier() + 1) : null;
   }

   @Override
   public void onEnable() {
      this.wasUsing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (nullCheck()) {
         this.wasUsing = false;
      } else {
         boolean var2 = this.isUsingUsableItem();
         if (var2) {
            this.freezeAbsorption();
            if (!this.wasUsing) {
               this.stackAbsorption();
            }

            this.wasUsing = true;
         } else {
            this.wasUsing = false;
         }
      }
   }

   private boolean isUsingUsableItem() {
      if (!mc.player.isUsingItem()) {
         return false;
      } else {
         ItemStack var1 = mc.player.getMainHandStack();
         ItemStack var2 = mc.player.getOffHandStack();
         return this.isUsable(var1) || this.isUsable(var2);
      }
   }

   private boolean isUsable(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         Item var2 = var1.getItem();
         return var2 != Items.GOLDEN_APPLE && var2 != Items.POTION ? var2 == Items.SPLASH_POTION : true;
      } else {
         return false;
      }
   }

   private StatusEffectInstance getAbsorption() {
      return mc.player.getStatusEffect(StatusEffects.ABSORPTION);
   }

   private void freezeAbsorption() {
      StatusEffectInstance var1 = this.getAbsorption();
      if (var1 != null) {
         if (this.freezeDuration.getValue() && !var1.isInfinite()) {
            Map var2 = mc.player.getActiveStatusEffects();
            StatusEffectInstance var3 = new StatusEffectInstance(StatusEffects.ABSORPTION, Integer.MAX_VALUE, var1.getAmplifier());
            var2.put(StatusEffects.ABSORPTION, var3);
         }
      }
   }

   private void stackAbsorption() {
      StatusEffectInstance var1 = this.getAbsorption();
      int var2 = var1 == null ? 0 : var1.getAmplifier() + 1;
      if (var2 < this.maxLevel.getValueInt()) {
         float var3 = this.stackAmount.getValueFloat();
         float var4 = mc.player.getHealth();
         mc.player.setHealth(var4 + var3);
         Map var5 = mc.player.getActiveStatusEffects();
         int var6 = this.freezeDuration.getValue() ? Integer.MAX_VALUE : (var1 == null ? 2400 : var1.getDuration());
         var5.put(StatusEffects.ABSORPTION, new StatusEffectInstance(StatusEffects.ABSORPTION, var6, var2));
      }
   }
}
