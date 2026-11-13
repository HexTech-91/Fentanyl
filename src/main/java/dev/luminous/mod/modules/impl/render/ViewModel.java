package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.HeldItemRendererEvent;
import dev.luminous.asm.accessors.IHeldItemRenderer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   public final BooleanSetting mainhandSwap = this.add(new BooleanSetting("MainhandSwap", true));
   public final BooleanSetting offhandSwap;
   public final SliderSetting scaleMainX;
   public final SliderSetting scaleMainY;
   public final SliderSetting scaleMainZ;
   public final SliderSetting positionMainX;
   public final SliderSetting positionMainY;
   public final SliderSetting positionMainZ;
   public final SliderSetting rotationMainX;
   public final SliderSetting rotationMainY;
   public final SliderSetting rotationMainZ;
   public final SliderSetting scaleOffX;
   public final SliderSetting scaleOffY;
   public final SliderSetting scaleOffZ;
   public final SliderSetting positionOffX;
   public final SliderSetting positionOffY;
   public final SliderSetting positionOffZ;
   public final SliderSetting rotationOffX;
   public final SliderSetting rotationOffY;
   public final SliderSetting rotationOffZ;
   public final BooleanSetting slowAnimation;
   public final SliderSetting slowAnimationVal;

   public ViewModel() {
      super("ViewModel", Module.Category.Render);
      this.mainhandSwap.setChinese("\u4e3b\u624b\u4ea4\u6362");
      this.offhandSwap = this.add(new BooleanSetting("OffhandSwap", true));
      this.offhandSwap.setChinese("\u526f\u624b\u4ea4\u6362");
      this.scaleMainX = this.add(new SliderSetting("ScaleMainX", 1.0, 0.1F, 5.0, 0.01));
      this.scaleMainX.setChinese("\u4e3b\u624b\u7f29\u653eX");
      this.scaleMainY = this.add(new SliderSetting("ScaleMainY", 1.0, 0.1F, 5.0, 0.01));
      this.scaleMainY.setChinese("\u4e3b\u624b\u7f29\u653eY");
      this.scaleMainZ = this.add(new SliderSetting("ScaleMainZ", 1.0, 0.1F, 5.0, 0.01));
      this.scaleMainZ.setChinese("\u4e3b\u624b\u7f29\u653eZ");
      this.positionMainX = this.add(new SliderSetting("PositionMainX", 0.0, -3.0, 3.0, 0.01));
      this.positionMainX.setChinese("\u4e3b\u624b\u4f4d\u7f6eX");
      this.positionMainY = this.add(new SliderSetting("PositionMainY", 0.0, -3.0, 3.0, 0.01));
      this.positionMainY.setChinese("\u4e3b\u624b\u4f4d\u7f6eY");
      this.positionMainZ = this.add(new SliderSetting("PositionMainZ", 0.0, -3.0, 3.0, 0.01));
      this.positionMainZ.setChinese("\u4e3b\u624b\u4f4d\u7f6eZ");
      this.rotationMainX = this.add(new SliderSetting("RotationMainX", 0.0, -180.0, 180.0, 0.01));
      this.rotationMainX.setChinese("\u4e3b\u624b\u65cb\u8f6cX");
      this.rotationMainY = this.add(new SliderSetting("RotationMainY", 0.0, -180.0, 180.0, 0.01));
      this.rotationMainY.setChinese("\u4e3b\u624b\u65cb\u8f6cY");
      this.rotationMainZ = this.add(new SliderSetting("RotationMainZ", 0.0, -180.0, 180.0, 0.01));
      this.rotationMainZ.setChinese("\u4e3b\u624b\u65cb\u8f6cZ");
      this.scaleOffX = this.add(new SliderSetting("ScaleOffX", 1.0, 0.1F, 5.0, 0.01));
      this.scaleOffX.setChinese("\u526f\u624b\u7f29\u653eX");
      this.scaleOffY = this.add(new SliderSetting("ScaleOffY", 1.0, 0.1F, 5.0, 0.01));
      this.scaleOffY.setChinese("\u526f\u624b\u7f29\u653eY");
      this.scaleOffZ = this.add(new SliderSetting("ScaleOffZ", 1.0, 0.1F, 5.0, 0.01));
      this.scaleOffZ.setChinese("\u526f\u624b\u7f29\u653eZ");
      this.positionOffX = this.add(new SliderSetting("PositionOffX", 0.0, -3.0, 3.0, 0.01));
      this.positionOffX.setChinese("\u526f\u624b\u4f4d\u7f6eX");
      this.positionOffY = this.add(new SliderSetting("PositionOffY", 0.0, -3.0, 3.0, 0.01));
      this.positionOffY.setChinese("\u526f\u624b\u4f4d\u7f6eY");
      this.positionOffZ = this.add(new SliderSetting("PositionOffZ", 0.0, -3.0, 3.0, 0.01));
      this.positionOffZ.setChinese("\u526f\u624b\u4f4d\u7f6eZ");
      this.rotationOffX = this.add(new SliderSetting("RotationOffX", 0.0, -180.0, 180.0, 0.01));
      this.rotationOffX.setChinese("\u526f\u624b\u65cb\u8f6cX");
      this.rotationOffY = this.add(new SliderSetting("RotationOffY", 0.0, -180.0, 180.0, 0.01));
      this.rotationOffY.setChinese("\u526f\u624b\u65cb\u8f6cY");
      this.rotationOffZ = this.add(new SliderSetting("RotationOffZ", 0.0, -180.0, 180.0, 0.01));
      this.rotationOffZ.setChinese("\u526f\u624b\u65cb\u8f6cZ");
      this.slowAnimation = this.add(new BooleanSetting("SlowAnimation", true));
      this.slowAnimation.setChinese("\u6325\u52a8\u901f\u5ea6");
      this.slowAnimationVal = this.add(new SliderSetting("SlowAnimationVal", 6, 1, 50));
      this.slowAnimationVal.setChinese("\u6570\u503c");
      this.setChinese("\u624b\u6301\u6a21\u578b");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!this.mainhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressMainHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressMainHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackMainHand(mc.player.getMainHandStack());
      }

      if (!this.offhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressOffHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressOffHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackOffHand(mc.player.getOffHandStack());
      }
   }

   @EventListener
   private void onHeldItemRender(HeldItemRendererEvent event) {
      if (event.getHand() == Hand.MAIN_HAND) {
         event.getStack().translate(this.positionMainX.getValueFloat(), this.positionMainY.getValueFloat(), this.positionMainZ.getValueFloat());
         event.getStack().scale(this.scaleMainX.getValueFloat(), this.scaleMainY.getValueFloat(), this.scaleMainZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationMainX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationMainY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationMainZ.getValueFloat()));
      } else {
         event.getStack().translate(this.positionOffX.getValueFloat(), this.positionOffY.getValueFloat(), this.positionOffZ.getValueFloat());
         event.getStack().scale(this.scaleOffX.getValueFloat(), this.scaleOffY.getValueFloat(), this.scaleOffZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationOffX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationOffY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationOffZ.getValueFloat()));
      }
   }
}
