package dev.luminous.asm.accessors;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EnderDragonEntity.class})
public interface IEnderDragonEntity {
   @Accessor("wingPosition")
   float getWingPosition();

   @Accessor("wingPosition")
   void setWingPosition(float var1);

   @Accessor("prevWingPosition")
   float getPrevWingPosition();

   @Accessor("prevWingPosition")
   void setPrevWingPosition(float var1);
}
