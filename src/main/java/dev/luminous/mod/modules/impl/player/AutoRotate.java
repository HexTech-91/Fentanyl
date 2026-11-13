package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.SendMovementPacketsEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class AutoRotate extends Module {
   public static AutoRotate INSTANCE;
   private final SliderSetting 旋转速度 = this.add(new SliderSetting("\u65cb\u8f6c\u901f\u5ea6", 3.0, 1.0, 10.0, 0.1));
   private float 当前旋转角度;

   public AutoRotate() {
      super("AutoRotate", "\u6301\u7eed\u65cb\u8f6c\u73a9\u5bb6\u89c6\u89d2\uff0c\u4ea7\u751f\u5e7b\u5f71\u6548\u679c", Module.Category.Player);
      this.旋转速度.setChinese("\u65cb\u8f6c\u901f\u5ea6");
      this.setChinese("\u5e7b\u5f71\u65cb\u8f6c");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.当前旋转角度 = mc.player.getYaw();
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         mc.player.input.pressingForward = false;
         mc.player.input.pressingBack = false;
         mc.player.input.pressingLeft = false;
         mc.player.input.pressingRight = false;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      float 每秒转数 = this.旋转速度.getValueFloat();
      float 每tick增量 = 每秒转数 * 360.0F / 20.0F;
      this.当前旋转角度 += 每tick增量;
      Alien.ROTATION.setRenderRotation(this.当前旋转角度, mc.player.getPitch(), true);
   }

   @EventListener
   public void onSendMovement(SendMovementPacketsEvent event) {
      event.setYaw(this.当前旋转角度);
   }
}
