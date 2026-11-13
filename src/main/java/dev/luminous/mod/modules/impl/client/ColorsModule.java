package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

public class ColorsModule extends Module {
   public static ColorsModule INSTANCE;
   public final EnumSetting<ColorsModule.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", ColorsModule.ColorMode.Custom));
   public final SliderSetting rainbowSpeed = this.add(
      new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1, () -> this.colorMode.getValue() == ColorsModule.ColorMode.Rainbow)
   );
   public final SliderSetting saturation = this.add(
      new SliderSetting("Saturation", 130.0, 1.0, 255.0, () -> this.colorMode.getValue() == ColorsModule.ColorMode.Rainbow)
   );
   public final SliderSetting rainbowDelay = this.add(
      new SliderSetting("Delay", 350, 0, 1000, () -> this.colorMode.getValue() == ColorsModule.ColorMode.Rainbow)
   );

   public ColorsModule() {
      super("Colors", Module.Category.Client);
      this.setChinese("\u989c\u8272");
      INSTANCE = this;
      this.rainbowSpeed.setChinese("\u5f69\u8679\u901f\u5ea6");
      this.saturation.setChinese("\u9971\u548c\u5ea6");
      this.rainbowDelay.setChinese("\u5ef6\u8fdf");
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }

   public Color getColor() {
      return this.getColor(0.0);
   }

   public Color getColor(double var1) {
      if (this.colorMode.getValue() == ColorsModule.ColorMode.Rainbow) {
         double var3 = Math.ceil((System.currentTimeMillis() * this.rainbowSpeed.getValue() + var1 * this.rainbowDelay.getValue()) / 20.0);
         return Color.getHSBColor((float)(var3 % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
      } else {
         return new Color(255, 0, 0);
      }
   }

   public static enum ColorMode {
      Custom,
      Rainbow;
   }
}
