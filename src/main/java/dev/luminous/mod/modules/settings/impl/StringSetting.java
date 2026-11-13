package dev.luminous.mod.modules.settings.impl;

import dev.luminous.mod.modules.settings.Setting;
import java.util.function.BooleanSupplier;

public class StringSetting extends Setting {
   private final String defaultValue;
   private String value;
   private Runnable task;

   public StringSetting(String name, String value) {
      super(name);
      this.value = value;
      this.defaultValue = value;
   }

   public StringSetting(String name, String value, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.value = value;
      this.defaultValue = value;
   }

   public StringSetting injectTask(Runnable task) {
      this.task = task;
      return this;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String text) {
      this.value = text;
      if (this.task != null) {
         this.task.run();
      }
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public void setValueWithoutTask(String var1) {
      this.value = var1;
   }

   public StringSetting setChinese(String var1) {
      super.setChinese(var1);
      return this;
   }
}
