package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class AutoKit extends Module {
   public static AutoKit INSTANCE;
   public final StringSetting kitName = this.add(new StringSetting("KitName", ""));
   public final BooleanSetting autoDisable;
   public final BooleanSetting remind;
   boolean kit;
   final Timer timer;

   public AutoKit() {
      super("AutoKit", Module.Category.Misc);
      this.kitName.setChinese("\u914d\u7f6e\u540d\u79f0");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.remind = this.add(new BooleanSetting("Remind", true));
      this.remind.setChinese("\u63d0\u9192");
      this.kit = false;
      this.timer = new Timer();
      this.setChinese("\u81ea\u52a8\u914d\u88c5\u547d\u4ee4");
      INSTANCE = this;
   }

   @Override
   public void onLogin() {
      this.kit = true;
      this.timer.reset();
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      if (event.getPlayer() == mc.player) {
         this.kit = true;
         this.timer.reset();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.kit && this.timer.passedS(2.0)) {
         this.kit = false;
         mc.player.networkHandler.sendCommand(this.kitName.getValue());
      }
   }
}
