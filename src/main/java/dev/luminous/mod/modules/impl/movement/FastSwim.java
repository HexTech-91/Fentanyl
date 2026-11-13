package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class FastSwim extends Module {
   public static FastSwim INSTANCE;
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.2, 0.0, 1.0, 0.01));
   public final SliderSetting downFactor;
   private final SliderSetting sneakDownSpeed;
   private final SliderSetting upSpeed;
   private MoveEvent event;

   public FastSwim() {
      super("FastSwim", Module.Category.Movement);
      this.speed.setChinese("\u901f\u5ea6");
      this.downFactor = this.add(new SliderSetting("DownFactor", 0.0, 0.0, 1.0, 1.0E-6));
      this.downFactor.setChinese("\u4e0b\u6c89\u56e0\u5b50");
      this.sneakDownSpeed = this.add(new SliderSetting("SneakDownSpeed", 0.2, 0.0, 1.0, 0.01));
      this.sneakDownSpeed.setChinese("\u4e0b\u6c89\u901f\u5ea6");
      this.upSpeed = this.add(new SliderSetting("UpSpeed", 0.2, 0.0, 1.0, 0.01));
      this.upSpeed.setChinese("\u4e0a\u6d6e\u901f\u5ea6");
      this.setChinese("\u5feb\u901f\u6e38\u6cf3");
      INSTANCE = this;
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player.isInFluid()) {
         mc.player.input.sneaking = false;
      }
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!nullCheck() && mc.player.isInFluid()) {
         this.event = event;
         if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
            this.setY(0.0);
         } else if (mc.options.sneakKey.isPressed()) {
            this.setY(-this.sneakDownSpeed.getValue());
         } else if (mc.player.input.jumping) {
            this.setY(this.upSpeed.getValue());
         } else {
            this.setY(-this.downFactor.getValue());
         }

         double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
         this.setX(dir[0]);
         this.setZ(dir[1]);
      }
   }

   private void setX(double f) {
      this.event.setX(f);
      MovementUtil.setMotionX(f);
   }

   private void setY(double f) {
      this.event.setY(f);
      MovementUtil.setMotionY(f);
   }

   private void setZ(double f) {
      this.event.setZ(f);
      MovementUtil.setMotionZ(f);
   }
}
