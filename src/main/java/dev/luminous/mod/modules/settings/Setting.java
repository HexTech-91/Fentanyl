package dev.luminous.mod.modules.settings;

import dev.luminous.api.utils.Translation;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.function.BooleanSupplier;

public abstract class Setting {
   private final BooleanSupplier visibility;
   private final String name;
   private String chinese;

   public Setting(String name) {
      this.name = name;
      this.visibility = null;
   }

   public Setting(String name, BooleanSupplier visibilityIn) {
      this.name = name;
      this.visibility = visibilityIn;
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.getAsBoolean();
   }

   public String getName() {
      return this.name;
   }

   public Setting setChinese(String chinese) {
      this.chinese = chinese;
      return this;
   }

   public String getDisplayName() {
      boolean chineseOn = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
      return chineseOn && this.chinese != null ? this.chinese : Translation.translate(this.name);
   }
}
