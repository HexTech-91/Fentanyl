package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.gui.FanqieNovelScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class FanqieReader extends Module {
   public final StringSetting apiBase = this.add(new StringSetting("ApiBase", "https://fanqienovel.com"));

   public FanqieReader() {
      super("FanqieReader", "Open the in-game novel reader", Module.Category.Survival);
      this.setChinese("\u5c0f\u8bf4\u9605\u8bfb");
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         mc.setScreen(new FanqieNovelScreen(this.apiBase.getValue()));
         mc.execute(this::disable);
      }
   }
}
