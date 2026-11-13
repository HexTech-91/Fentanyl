package dev.luminous.asm.mixins.spear;

import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemFeatureRenderer.class})
public abstract class MixinHeldItemFeatureRenderer {
   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )},
      require = 0
   )
   private void spearFix$renderItem(
      LivingEntity var1, ItemStack var2, ModelTransformationMode var3, Arm var4, MatrixStack var5, VertexConsumerProvider var6, int var7, CallbackInfo var8
   ) {
      if (SpearFix.INSTANCE != null) {
         SpearFix.INSTANCE.applyThirdPersonHeldItemAnimation(var5, var1, var2, var4);
      }
   }
}
