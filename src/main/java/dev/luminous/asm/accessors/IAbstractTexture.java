package dev.luminous.asm.accessors;

import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractTexture.class})
public interface IAbstractTexture {
   @Accessor("glId")
   int getGlId();
}
