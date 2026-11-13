package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;

public class SettingTest extends Module {
   public final BooleanSetting boolean_ = this.add(new BooleanSetting("Boolean", true));
   public final SliderSetting slider;
   public final EnumSetting<SettingTest.EnumMode> enum_;
   public final ColorSetting color;
   public final ColorSetting alphaColor;
   public final ColorSetting noAlphaColor;
   public final StringSetting string;

   public SettingTest() {
      super("SettingTest", "\u8c03\u8bd5\u6a21\u5757\uff0c\u5c55\u793a\u6240\u6709\u8bbe\u7f6e\u7c7b\u578b.", Module.Category.Client);
      this.boolean_.setChinese("\u5e03\u5c14\u503c");
      this.slider = this.add(new SliderSetting("Slider", 5.0, 0.0, 10.0, 0.5));
      this.slider.setChinese("\u6ed1\u6761");
      this.enum_ = this.add(new EnumSetting<>("Enum", SettingTest.EnumMode.ONE));
      this.enum_.setChinese("\u679a\u4e3e");
      this.color = this.add(new ColorSetting("Color", new Color(-12948840, true)));
      this.color.setChinese("\u989c\u8272");
      this.alphaColor = this.add(new ColorSetting("AlphaColor", new Color(-2011028856, true)));
      this.alphaColor.setChinese("\u900f\u660e\u989c\u8272");
      this.noAlphaColor = this.add(new ColorSetting("NoAlphaColor", new Color(-12171613, false)));
      this.noAlphaColor.setChinese("\u65e0\u900f\u660e\u989c\u8272");
      this.string = this.add(new StringSetting("String", "text"));
      this.string.setChinese("\u5b57\u7b26\u4e32");
      this.setChinese("\u8bbe\u7f6e\u6d4b\u8bd5");
   }

   public static enum EnumMode {
      ONE,
      TWO,
      THREE;

      public String getChinese() {
         return switch (this) {
            case ONE -> "\u4e00";
            case TWO -> "\u4e8c";
            case THREE -> "\u4e09";
         };
      }
   }
}
