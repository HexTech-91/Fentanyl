package dev.luminous.mod.modules.impl.client;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.mod.gui.InterfaceEditorScreen;
import dev.luminous.mod.modules.Module;

public class InterfaceEditor extends Module {
   public static InterfaceEditor INSTANCE;

   public InterfaceEditor() {
      super("InterfaceEditor", Module.Category.Client);
      this.setChinese("\u754c\u9762\u7f16\u8f91\u5668");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         mc.setScreen(new InterfaceEditorScreen());
      }
   }

   @Override
   public void onDisable() {
      if (mc != null && mc.currentScreen instanceof InterfaceEditorScreen) {
         mc.currentScreen.close();
      }
   }

   @EventListener
   public void onTick(TickEvent var1) {
      if (this.isOn() && mc.currentScreen == null) {
         this.setState(false);
      }
   }
}
