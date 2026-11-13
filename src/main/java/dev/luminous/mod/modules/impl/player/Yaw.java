package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Yaw extends Module {
   public static Yaw INSTANCE;
   private final BooleanSetting yawLock = this.add(new BooleanSetting("YawLock", true));
   private final BooleanSetting smart;
   private final SliderSetting yaw;
   private final BooleanSetting pitchLock;
   private final SliderSetting pitch;
   private final BooleanSetting lock;

   public Yaw() {
      super("Yaw", Module.Category.Player);
      this.yawLock.setChinese("Yaw\u9501\u5b9a");
      this.smart = this.add(new BooleanSetting("Smart", true));
      this.smart.setChinese("\u667a\u80fd");
      this.yaw = this.add(new SliderSetting("Yaw", 0.0, -180.0, 180.0, 0.1, () -> !this.smart.getValue()));
      this.pitchLock = this.add(new BooleanSetting("PitchLock", true));
      this.pitchLock.setChinese("Pitch\u9501\u5b9a");
      this.pitch = this.add(new SliderSetting("Pitch", 0.0, -90.0, 90.0, 0.1));
      this.lock = this.add(new BooleanSetting("Lock", true));
      this.lock.setChinese("\u9501\u5b9a");
      this.setChinese("\u89c6\u89d2\u9501\u5b9a");
      INSTANCE = this;
      this.yaw.setChinese("\u6c34\u5e73\u89d2\u5ea6");
      this.pitch.setChinese("\u5782\u76f4\u89d2\u5ea6");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.yawLock.getValue()) {
         mc.player.setYaw(this.smart.getValue() ? Math.round((mc.player.getYaw() + 1.0F) / 45.0F) * 45.0F : this.yaw.getValueFloat());
      }

      if (this.pitchLock.getValue()) {
         mc.player.setPitch(this.pitch.getValueFloat());
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.lock.getValue()) {
         event.cancel();
      }
   }
}
