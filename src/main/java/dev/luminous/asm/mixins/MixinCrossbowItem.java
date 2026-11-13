package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.combat.AutoCrossbowAim;
import dev.luminous.mod.modules.impl.combat.CrossbowRapidFire;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CrossbowItem.class})
public class MixinCrossbowItem {
   private static boolean anyCrossbowModuleOn() {
      return CrossbowRapidFire.INSTANCE != null && CrossbowRapidFire.INSTANCE.isOn() || AutoCrossbowAim.INSTANCE != null && AutoCrossbowAim.INSTANCE.isOn();
   }

   @Inject(
      method = {"use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;"},
      at = {@At("HEAD")}
   )
   private void hookUseHead(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      if (anyCrossbowModuleOn()) {
         if (user != null) {
            ItemStack crossbow = user.getStackInHand(hand);
            if (crossbow.getItem() instanceof CrossbowItem) {
               ChargedProjectilesComponent charged = (ChargedProjectilesComponent)crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
               MixinCrossbowItem.SavedProjectilesHolder.set(
                  charged != null && !charged.isEmpty() ? charged : MixinCrossbowItem.SavedProjectilesHolder.getLast()
               );
               if (charged != null && !charged.isEmpty()) {
                  MixinCrossbowItem.SavedProjectilesHolder.setLast(charged);
               }
            }
         }
      }
   }

   @Inject(
      method = {"use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookUseReturn(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      if (anyCrossbowModuleOn()) {
         if (user != null) {
            ItemStack crossbow = user.getStackInHand(hand);
            if (crossbow.getItem() instanceof CrossbowItem) {
               ChargedProjectilesComponent saved = MixinCrossbowItem.SavedProjectilesHolder.getAndClear();
               if (saved != null && !saved.getProjectiles().isEmpty()) {
                  crossbow.set(DataComponentTypes.CHARGED_PROJECTILES, saved);
                  MixinCrossbowItem.SavedProjectilesHolder.setLast(saved);
               }

               cir.setReturnValue(TypedActionResult.success(crossbow));
            }
         }
      }
   }

   private static final class SavedProjectilesHolder {
      private static final ThreadLocal<ChargedProjectilesComponent> HOLDER = new ThreadLocal<>();
      private static volatile ChargedProjectilesComponent lastCharged;

      static void set(ChargedProjectilesComponent value) {
         HOLDER.set(value);
      }

      static ChargedProjectilesComponent getAndClear() {
         ChargedProjectilesComponent v = HOLDER.get();
         HOLDER.remove();
         return v;
      }

      static ChargedProjectilesComponent getLast() {
         return lastCharged;
      }

      static void setLast(ChargedProjectilesComponent value) {
         lastCharged = value;
      }
   }
}
