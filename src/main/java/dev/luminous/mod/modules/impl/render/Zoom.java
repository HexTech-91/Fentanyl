package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Zoom extends Module {
   public static Zoom INSTANCE;
   public static boolean on = false;
   public final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   final SliderSetting fov;
   final Animation animation;
   private final SliderSetting animTime;
   public double currentFov;

   public Zoom() {
      super("Zoom", Module.Category.Survival);
      this.ease.setChinese("\u7f13\u52a8");
      this.fov = this.add(new SliderSetting("Fov", 60.0, 0.0, 130.0, 1.0));
      this.fov.setChinese("\u7f29\u653e\u89c6\u89d2");
      this.animation = new Animation();
      this.animTime = this.add(new SliderSetting("AnimTime", 300, 0, 1000));
      this.animTime.setChinese("\u52a8\u753b\u65f6\u95f4");
      this.setChinese("\u653e\u5927");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new Zoom.ZoomAnim());
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      }
   }

   public class ZoomAnim {
      @EventListener
      public void onRender3D(Render3DEvent var1) {
         if (Zoom.this.isOn()) {
            Zoom.this.currentFov = Zoom.this.animation.get(Zoom.this.fov.getValue(), Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
            Zoom.on = true;
         } else if (Zoom.on) {
            Zoom.this.currentFov = Zoom.this.animation.get(0.0, Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
            if ((int)Zoom.this.currentFov == 0) {
               Zoom.on = false;
            }
         }
      }
   }
}
