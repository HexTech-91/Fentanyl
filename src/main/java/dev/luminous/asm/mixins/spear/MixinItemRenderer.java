package dev.luminous.asm.mixins.spear;

import dev.luminous.mod.modules.impl.render.SpearFix;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ItemRenderer.class})
public class MixinItemRenderer {
   @Unique
   private ModelTransformationMode spearFix$renderMode;

   @ModifyVariable(
      method = {"renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V"},
      at = @At("HEAD"),
      index = 8,
      argsOnly = true,
      require = 0
   )
   private BakedModel spearFix$replaceRenderModel(
      BakedModel var1, ItemStack var2, ModelTransformationMode var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7, int var8
   ) {
      this.spearFix$renderMode = var3;
      BakedModel var9 = this.spearFix$getSpearModel(var2, var3);
      return var9 == null ? var1 : var9;
   }

   @ModifyVariable(
      method = {"renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/item/ItemStack;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"},
      at = @At("HEAD"),
      index = 1,
      argsOnly = true,
      require = 0
   )
   private BakedModel spearFix$replaceBakedModel(BakedModel var1, BakedModel var2, ItemStack var3, int var4, int var5, MatrixStack var6, VertexConsumer var7) {
      BakedModel var8 = this.spearFix$getSpearModel(var3, this.spearFix$renderMode);
      return var8 == null ? var2 : var8;
   }

   @Unique
   private BakedModel spearFix$getSpearModel(ItemStack var1, ModelTransformationMode var2) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldReplaceModel(var1) && var2 != null) {
         try {
            return ((FabricBakedModelManager)MinecraftClient.getInstance().getBakedModelManager()).getModel(SpearFix.INSTANCE.getModelId(var1, var2));
         } catch (Throwable var4) {
            return null;
         }
      } else {
         return null;
      }
   }
}
