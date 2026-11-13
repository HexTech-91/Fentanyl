package dev.luminous.asm.mixins;

import dev.luminous.mod.features.SpearPort;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityModel.class})
public abstract class MixinPlayerEntityModel<T extends LivingEntity> extends BipedEntityModel<T> {
   @Shadow
   @Final
   public ModelPart leftSleeve;
   @Shadow
   @Final
   public ModelPart rightSleeve;

   protected MixinPlayerEntityModel(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void alien$applyPortedSpearThirdPersonPose(
      T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci
   ) {
      Hand var8 = getActiveSpearHand(entity);
      if (var8 != null) {
         Arm var9 = var8 == Hand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
         ModelPart var10 = var9 == Arm.RIGHT ? this.rightArm : this.leftArm;
         int var11 = var9 == Arm.RIGHT ? 1 : -1;
         float var12 = MathHelper.clamp(entity.getItemUseTime() / 10.0F, 0.0F, 1.0F);
         float var13 = smoothIn(clampCurve(var12, 0.0F, 0.05F));
         float var14 = smoothOut(clampCurve(var12, 0.05F, 0.2F));
         float var15 = smoothIn(clampCurve(var12, 0.4F, 1.0F));
         var10.pitch += (90.0F * var13 - 120.0F * var14 + 30.0F * var15) * (float) (Math.PI / 180.0);
         var10.yaw += var11 * (8.0F * var13 - 18.0F * var14 + 5.0F * var15) * (float) (Math.PI / 180.0);
         var10.roll += var11 * (-8.0F * var13 + 10.0F * var14) * (float) (Math.PI / 180.0);
         if (var9 == Arm.RIGHT) {
            this.rightSleeve.copyTransform(this.rightArm);
         } else {
            this.leftSleeve.copyTransform(this.leftArm);
         }
      }
   }

   private static Hand getActiveSpearHand(LivingEntity entity) {
      if (!entity.isUsingItem()) {
         return null;
      } else {
         Hand var1 = entity.getActiveHand();
         ItemStack var2 = entity.getStackInHand(var1);
         return SpearPort.isSpear(var2) ? var1 : null;
      }
   }

   private static float clampCurve(float value, float start, float end) {
      return MathHelper.clamp((value - start) / (end - start), 0.0F, 1.0F);
   }

   private static float smoothIn(float value) {
      return value * value;
   }

   private static float smoothOut(float value) {
      float var1 = 1.0F - value;
      return 1.0F - var1 * var1;
   }
}
