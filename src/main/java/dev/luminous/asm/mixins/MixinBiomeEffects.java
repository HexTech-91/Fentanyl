package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Skybox;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Biome.class})
public class MixinBiomeEffects {
   @Inject(
      method = {"getWaterFogColor()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSkyColor(CallbackInfoReturnable<Integer> var1) {
      if (Skybox.INSTANCE != null && Skybox.INSTANCE.isOn()) {
         var1.setReturnValue(Skybox.INSTANCE.getRenderedColor());
      }
   }
}
