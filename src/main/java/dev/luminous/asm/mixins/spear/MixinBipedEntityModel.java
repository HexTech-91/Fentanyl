package dev.luminous.asm.mixins.spear;

import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BipedEntityModel.class})
public abstract class MixinBipedEntityModel<T extends LivingEntity> extends AnimalModel<T> {
   @Shadow
   @Final
   public ModelPart rightArm;
   @Shadow
   @Final
   public ModelPart leftArm;
   @Shadow
   @Final
   public ModelPart body;
   @Shadow
   @Final
   public ModelPart head;

   @Shadow
   protected abstract ModelPart getArm(Arm var1);

   @Inject(
      method = {"positionRightArm(Lnet/minecraft/entity/LivingEntity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$positionRightArm(T var1, CallbackInfo var2) {
      if (this.spearFix$handleSpearHold((T)var1, Arm.RIGHT) && !var1.isUsingItem() && this.handSwingProgress > 0.0F) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"positionLeftArm(Lnet/minecraft/entity/LivingEntity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$positionLeftArm(T var1, CallbackInfo var2) {
      if (this.spearFix$handleSpearHold((T)var1, Arm.LEFT) && !var1.isUsingItem() && this.handSwingProgress > 0.0F) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"animateArms(Lnet/minecraft/entity/LivingEntity;F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;sin(F)F",
         ordinal = 3
      )},
      cancellable = true
   )
   private void spearFix$animateArms(T var1, float var2, CallbackInfo var3) {
      SpearFix var4 = SpearFix.INSTANCE;
      if (var4 != null && var4.shouldAnimateThirdPerson(var1.getMainHandStack())) {
         Arm var5 = var1.getMainArm();
         this.rightArm.yaw = this.rightArm.yaw - this.body.yaw;
         this.leftArm.yaw = this.leftArm.yaw - this.body.yaw;
         this.leftArm.pitch = this.leftArm.pitch - this.body.yaw;
         ModelPart var6 = this.getArm(var5);
         var6.pitch = var6.pitch + var4.getThirdPersonSwing(this.handSwingProgress);
         var3.cancel();
      }
   }

   @Unique
   private boolean spearFix$handleSpearHold(T var1, Arm var2) {
      SpearFix var3 = SpearFix.INSTANCE;
      if (var3 == null) {
         return false;
      } else {
         ItemStack var4 = var1.getMainArm() == var2 ? var1.getMainHandStack() : var1.getOffHandStack();
         if (!var3.shouldAnimateThirdPerson(var4)) {
            return false;
         } else {
            ModelPart var5 = this.getArm(var2);
            var5.yaw = -0.1F * this.head.yaw;
            var5.pitch = (float) (-Math.PI / 2) + this.head.pitch + 0.8F;
            if (var1.isFallFlying() || var1.getLeaningPitch(var3.getTickDelta()) > 0.0F) {
               var5.pitch -= 0.9599311F;
            }

            var5.roll = var5.roll + (var2 == Arm.RIGHT ? -1.0F : 1.0F) * 0.03F * MathHelper.sin((var1.age + var3.getTickDelta()) * 0.15F);
            return true;
         }
      }
   }
}
