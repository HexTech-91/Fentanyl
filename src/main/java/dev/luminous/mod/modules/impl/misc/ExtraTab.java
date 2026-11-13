package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class ExtraTab extends Module {
   public static ExtraTab INSTANCE;
   public final SliderSetting size = this.add(new SliderSetting("Size", 200.0, 80.0, 1000.0, 1.0));
   public final SliderSetting columns;

   public ExtraTab() {
      super("ExtraTab", Module.Category.Misc);
      this.size.setChinese("\u5927\u5c0f");
      this.columns = this.add(new SliderSetting("Columns", 20.0, 1.0, 100.0, 1.0));
      this.columns.setChinese("\u5217\u6570");
      this.setChinese("\u5217\u8868\u65e0\u4e0a\u9650");
      INSTANCE = this;
   }
}
