package dev.luminous.mod.modules.settings.impl;

import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import dev.luminous.mod.modules.settings.Setting;
import java.awt.Color;
import java.util.function.BooleanSupplier;

public class ColorSetting extends Setting {
   public static final Timer timer = new Timer();
   public static final float effectSpeed = 4.0F;
   private final Color defaultValue;
   public boolean sync = false;
   public boolean rainbow = false;
   public boolean injectBoolean = false;
   public boolean booleanValue = false;
   public double rainbowSpeed = 0.5;
   public double rainbowSaturation = 0.7;
   public double rainbowBrightness = 0.9;
   public double rainbowAlpha = 1.0;
   private Color value;
   private boolean defaultSync = false;
   private boolean defaultRainbow = false;
   private boolean defaultBooleanValue = false;
   private boolean allowClientColor = true;

   public ColorSetting(String name) {
      this(name, new Color(255, 255, 255));
      this.defaultSync = true;
   }

   public ColorSetting(String name, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = new Color(255, 255, 255);
      this.defaultValue = this.value;
      this.defaultSync = true;
   }

   public ColorSetting(String name, Color defaultValue) {
      super(name);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public ColorSetting(String name, Color defaultValue, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public ColorSetting(String name, int defaultValue) {
      this(name, new Color(defaultValue, true));
   }

   public ColorSetting(String name, int defaultValue, BooleanSupplier visibilityIn) {
      this(name, new Color(defaultValue, true), visibilityIn);
   }

   public Color getValue() {
      if (this.sync || this.rainbow) {
         if (this.allowClientColor) {
            Color preColor = ColorsModule.INSTANCE.getColor();
            this.setValue(new Color(preColor.getRed(), preColor.getGreen(), preColor.getBlue(), this.value.getAlpha()));
         } else {
            float[] HSB = Color.RGBtoHSB(this.value.getRed(), this.value.getGreen(), this.value.getBlue(), null);
            Color preColor = Color.getHSBColor((float)timer.getMs() * 0.36F * 4.0F / 20.0F % 361.0F / 360.0F, HSB[1], HSB[2]);
            this.setValue(new Color(preColor.getRed(), preColor.getGreen(), preColor.getBlue(), this.value.getAlpha()));
         }
      }

      return this.value;
   }

   public void setValue(Color value) {
      this.value = value;
   }

   public void setValue(int value) {
      this.value = new Color(value, true);
   }

   public void setSync(boolean sync) {
      this.sync = sync;
   }

   public ColorSetting injectBoolean(boolean value) {
      this.injectBoolean = true;
      this.defaultBooleanValue = value;
      this.booleanValue = value;
      return this;
   }

   public ColorSetting allowClientColor(boolean value) {
      this.allowClientColor = value;
      return this;
   }

   public Color getDefaultValue() {
      return this.defaultValue;
   }

   public boolean getDefaultBooleanValue() {
      return this.defaultBooleanValue;
   }

   public boolean getDefaultSync() {
      return this.defaultSync;
   }

   public ColorSetting defaultRainbow(boolean defaultRainbow) {
      this.defaultRainbow = defaultRainbow;
      this.rainbow = defaultRainbow;
      return this;
   }

   public boolean getDefaultRainbow() {
      return this.defaultRainbow;
   }

   public void setRainbow(boolean rainbow) {
      this.rainbow = rainbow;
   }

   public ColorSetting setChinese(String var1) {
      super.setChinese(var1);
      return this;
   }
}
