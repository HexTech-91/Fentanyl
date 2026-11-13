package dev.luminous.asm.mixins.spear;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class MixinClientPlayerEntity {
   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   private void spearFix$tickReturn(CallbackInfo var1) {
      if (SpearFix.INSTANCE != null) {
         SpearFix.INSTANCE.tickUseSound((LivingEntity)(Object)this);
      }
   }

   @ModifyExpressionValue(
      method = {"canStartSprinting()Z"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      )},
      require = 0
   )
   private boolean spearFix$allowSprinting(boolean var1) {
      return SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldAllowSprint((LivingEntity)(Object)this) ? false : var1;
   }

   @Inject(
      method = {"setCurrentHand(Lnet/minecraft/util/Hand;)V"},
      at = {@At("HEAD")}
   )
   private void spearFix$onSetCurrentHand(Hand var1, CallbackInfo var2) {
      if (SpearFix.INSTANCE != null) {
         SpearFix.INSTANCE.playUseSound(((LivingEntity)(Object)this).getStackInHand(var1));
      }
   }
}
