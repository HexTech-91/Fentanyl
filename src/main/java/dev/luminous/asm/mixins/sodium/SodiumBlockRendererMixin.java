package dev.luminous.asm.mixins.sodium;

import dev.luminous.mod.modules.impl.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockRenderer.class})
public abstract class SodiumBlockRendererMixin {
   @Inject(
      method = {"renderModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;getColorProvider(Lnet/minecraft/block/Block;)Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void onRenderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo info) {
      if (Xray.shouldBlock(state)) {
         info.cancel();
      }
   }
}
