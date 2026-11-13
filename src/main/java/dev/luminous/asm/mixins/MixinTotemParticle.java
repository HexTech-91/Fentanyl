package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.TotemParticleEvent;
import java.awt.Color;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TotemParticle.class})
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(
      method = {"<init>(Lnet/minecraft/client/world/ClientWorld;DDDDDDLnet/minecraft/client/particle/SpriteProvider;)V"},
      at = {@At("TAIL")}
   )
   private void hookInit(
      ClientWorld var1, double var2, double var4, double var6, double var8, double var10, double var12, SpriteProvider var14, CallbackInfo var15
   ) {
      TotemParticleEvent var16 = TotemParticleEvent.get(var8, var10, var12);
      Alien.EVENT_BUS.post(var16);
      if (var16.isCancelled()) {
         dev.luminous.mod.modules.impl.render.TotemParticle var17 = dev.luminous.mod.modules.impl.render.TotemParticle.INSTANCE;
         if (var17 != null && var17.textMode.getValue() != dev.luminous.mod.modules.impl.render.TotemParticle.TextMode.Off) {
            var17.recordPop(var2, var4, var6, var16.velocityX, var16.velocityY, var16.velocityZ);
            this.setAlpha(0.0F);
            this.setColor(0.0F, 0.0F, 0.0F);
         } else {
            this.velocityX = var16.velocityX;
            this.velocityY = var16.velocityY;
            this.velocityZ = var16.velocityZ;
            Color var18 = var16.color;
            if (var18 != null) {
               this.setColor(var18.getRed() / 255.0F, var18.getGreen() / 255.0F, var18.getBlue() / 255.0F);
               this.setAlpha(var18.getAlpha() / 255.0F);
            }
         }
      }
   }
}
