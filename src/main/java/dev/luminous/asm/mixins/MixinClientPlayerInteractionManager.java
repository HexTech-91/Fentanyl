package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.ClickBlockEvent;
import dev.luminous.api.events.impl.InteractBlockEvent;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class MixinClientPlayerInteractionManager {
   @Shadow
   private ItemStack selectedStack;

   @ModifyVariable(
      method = {"isCurrentlyBreaking(Lnet/minecraft/util/math/BlockPos;)Z"},
      at = @At("STORE")
   )
   private ItemStack stack(ItemStack stack) {
      return InteractTweaks.INSTANCE.noReset() ? this.selectedStack : stack;
   }

   @ModifyConstant(
      method = {"updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"},
      constant = {@Constant(
         intValue = 5
      )}
   )
   private int MiningCooldownFix(int value) {
      return InteractTweaks.INSTANCE.noDelay() ? 0 : value;
   }

   @Inject(
      method = {"interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      InteractItemEvent event = InteractItemEvent.getPre(hand);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"},
      at = {@At("RETURN")}
   )
   private void hookInteractItemReturn(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      Alien.EVENT_BUS.post(InteractItemEvent.getPost(hand));
   }

   @Inject(
      method = {"interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      InteractBlockEvent event = InteractBlockEvent.getPre(hand);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"},
      at = {@At("RETURN")}
   )
   private void hookInteractBlockReturn(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      Alien.EVENT_BUS.post(InteractBlockEvent.getPost(hand));
   }

   @Inject(
      method = {"cancelBlockBreaking()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookCancelBlockBreaking(CallbackInfo callbackInfo) {
      if (InteractTweaks.INSTANCE.noAbort()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      ClickBlockEvent event = ClickBlockEvent.get(pos, direction);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }
}
