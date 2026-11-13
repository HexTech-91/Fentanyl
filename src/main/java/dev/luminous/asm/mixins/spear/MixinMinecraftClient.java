package dev.luminous.asm.mixins.spear;

import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class MixinMinecraftClient {
   @Shadow
   public ClientPlayerEntity player;
   @Shadow
   public HitResult crosshairTarget;

   @Inject(
      method = {"doAttack()Z"},
      at = {@At("HEAD")}
   )
   private void spearFix$doAttack(CallbackInfoReturnable<Boolean> var1) {
      if (SpearFix.INSTANCE != null && this.player != null) {
         SpearFix.INSTANCE.playAttackSound(this.player.getMainHandStack(), this.crosshairTarget instanceof EntityHitResult);
      }
   }
}
