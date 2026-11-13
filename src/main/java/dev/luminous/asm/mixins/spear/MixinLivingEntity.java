package dev.luminous.asm.mixins.spear;

import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity {
   @Inject(
      method = {"getHandSwingDuration()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> var1) {
      if (SpearFix.INSTANCE != null) {
         int var2 = SpearFix.INSTANCE.getSwingTicks((LivingEntity)(Object)this);
         if (var2 > 0) {
            var1.setReturnValue(var2);
         }
      }
   }
}
