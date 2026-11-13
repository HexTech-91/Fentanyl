package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Fly extends Module {
   public static Fly INSTANCE;
   private final SliderSetting speedConfig = this.add(new SliderSetting("SpeedConfig", 2.5, 0.1, 10.0));
   private final SliderSetting vspeedConfig;
   private final BooleanSetting antiKickConfig;
   private final BooleanSetting up;
   private final BooleanSetting allowSneak;
   private final Timer antiKickTimer;
   private final Timer antiKick2Timer;

   public Fly() {
      super("Fly", Module.Category.Movement);
      this.speedConfig.setChinese("\u901f\u5ea6");
      this.vspeedConfig = this.add(new SliderSetting("VspeedConfig", 1.0, 0.1, 5.0));
      this.vspeedConfig.setChinese("\u5782\u76f4\u901f\u5ea6");
      this.antiKickConfig = this.add(new BooleanSetting("AntiKickConfig", true).setParent());
      this.antiKickConfig.setChinese("\u53cd\u8e22\u51fa");
      this.up = this.add(new BooleanSetting("Up", true, this.antiKickConfig::isOpen));
      this.up.setChinese("\u4e0a");
      this.allowSneak = this.add(new BooleanSetting("AllowSneak", false));
      this.allowSneak.setChinese("\u5141\u8bb8\u6f5c\u884c");
      this.antiKickTimer = new Timer();
      this.antiKick2Timer = new Timer();
      this.setChinese("\u98de\u884c");
      INSTANCE = this;
   }

   @Override
   public void onLogin() {
      this.antiKickTimer.reset();
      this.antiKick2Timer.reset();
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.antiKickTimer.reset();
         this.antiKick2Timer.reset();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.antiKickTimer.passed(3900L) && this.antiKickConfig.getValue() && !mc.player.isOnGround()) {
         MovementUtil.setMotionY(-0.04);
         this.antiKickTimer.reset();
      } else if (this.antiKick2Timer.passed(4000L) && this.antiKickConfig.getValue() && !mc.player.isOnGround() && this.up.getValue()) {
         MovementUtil.setMotionY(0.04);
         this.antiKick2Timer.reset();
      } else {
         MovementUtil.setMotionY(0.0);
         if (mc.options.jumpKey.isPressed()) {
            MovementUtil.setMotionY(this.vspeedConfig.getValue());
         } else if (mc.options.sneakKey.isPressed()) {
            MovementUtil.setMotionY(-this.vspeedConfig.getValue());
         }
      }

      double[] move = MovementUtil.directionSpeed(this.speedConfig.getValueFloat());
      MovementUtil.setMotionX(move[0]);
      MovementUtil.setMotionZ(move[1]);
   }

   @EventListener(
      priority = -100
   )
   public void keyboard(KeyboardInputEvent event) {
      if (!this.allowSneak.getValue()) {
         mc.player.input.sneaking = false;
      }
   }
}
