package dev.luminous.mod.modules.impl.player;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;

public class AntiEffects extends Module {
   public static AntiEffects INSTANCE;
   public final BooleanSetting levitation = this.add(new BooleanSetting("Levitation", true));
   public final BooleanSetting slowFalling;

   public AntiEffects() {
      super("AntiEffects", Module.Category.Player);
      this.levitation.setChinese("\u6f02\u6d6e");
      this.slowFalling = this.add(new BooleanSetting("SlowFalling", true));
      this.slowFalling.setChinese("\u7f13\u964d");
      this.setChinese("\u53cd\u6548\u679c");
      INSTANCE = this;
   }
}
