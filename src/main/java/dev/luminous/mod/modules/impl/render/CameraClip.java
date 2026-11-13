package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;

public class CameraClip extends Module {
   public static CameraClip INSTANCE;
   public final SliderSetting getDistance = this.add(new SliderSetting("GetDistance", 4.0, 1.0, 20.0));
   public final SliderSetting animateTime;
   private final EnumSetting<Easing> ease;
   final FadeUtils animation;
   private final BooleanSetting noFront;
   boolean first;

   public CameraClip() {
      super("CameraClip", Module.Category.Render);
      this.getDistance.setChinese("\u8ddd\u79bb");
      this.animateTime = this.add(new SliderSetting("AnimateTime", 200, 0, 1000));
      this.animateTime.setChinese("\u52a8\u753b\u65f6\u95f4");
      this.ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
      this.ease.setChinese("\u7f13\u52a8");
      this.animation = new FadeUtils(300L);
      this.noFront = this.add(new BooleanSetting("NoFront", false));
      this.noFront.setChinese("\u7981\u6b62\u6b63\u9762");
      this.first = false;
      this.setChinese("\u6444\u50cf\u673a\u7a7f\u5899");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && this.noFront.getValue()) {
         mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      this.animation.setLength(this.animateTime.getValueInt());
      if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         if (!this.first) {
            this.first = true;
            this.animation.reset();
         }
      } else if (this.first) {
         this.first = false;
         this.animation.reset();
      }
   }

   public double getDistance() {
      double quad = mc.options.getPerspective() == Perspective.FIRST_PERSON
         ? 1.0 - this.animation.ease(this.ease.getValue())
         : this.animation.ease(this.ease.getValue());
      return this.getDistance.getValue() * quad - 1.0 + 1.0;
   }
}
