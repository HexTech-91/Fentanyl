package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class MainMenu extends Module {
   public static MainMenu INSTANCE;
   private final BooleanSetting shader = this.add(new BooleanSetting("Shader", true));
   private final BooleanSetting customButtons;
   private final EnumSetting<MainMenu.EMode> title;
   private final EnumSetting<MainMenu.Mode> mode;
   private final EnumSetting<MainMenu.ShaderMode> backgroundShader;
   private final SliderSetting fpsLimit;
   private MainMenu.ShaderMode lastRandomShader;

   public MainMenu() {
      super("MainMenu", "1122 \u98ce\u683c\u7740\u8272\u5668\u4e3b\u83dc\u5355\u80cc\u666f.", Module.Category.Client);
      this.shader.setChinese("\u7740\u8272\u5668");
      this.customButtons = this.add(new BooleanSetting("CustomButtons", true));
      this.customButtons.setChinese("\u81ea\u5b9a\u4e49\u6309\u94ae");
      this.title = this.add(new EnumSetting<>("Title", MainMenu.EMode.TROLL_HACK));
      this.title.setChinese("\u6807\u9898");
      this.mode = this.add(new EnumSetting<>("Mode", MainMenu.Mode.SET));
      this.mode.setChinese("\u6a21\u5f0f");
      this.backgroundShader = this.add(new EnumSetting<>("BackgroundShader", MainMenu.ShaderMode.SNOWFLAKE));
      this.backgroundShader.setChinese("\u80cc\u666f\u7740\u8272\u5668");
      this.fpsLimit = this.add(new SliderSetting("FpsLimit", 60.0, 10.0, 240.0, 10.0));
      this.fpsLimit.setChinese("\u5e27\u7387\u9650\u5236");
      this.lastRandomShader = null;
      this.setChinese("\u4e3b\u83dc\u5355");
      INSTANCE = this;
      this.setState(true);
   }

   public boolean isShaderEnabled() {
      return this.shader.getValue();
   }

   public boolean isCustomButtonsEnabled() {
      return this.customButtons.getValue();
   }

   public String getTitleText() {
      return this.title.getValue().displayName;
   }

   public MainMenu.ShaderMode getShaderMode() {
      if (this.mode.getValue() == MainMenu.Mode.RANDOM) {
         MainMenu.ShaderMode[] values = MainMenu.ShaderMode.values();
         MainMenu.ShaderMode chosen = values[(int)(Math.random() * values.length)];
         if (chosen == this.lastRandomShader && values.length > 1) {
            chosen = values[((int)(Math.random() * values.length) + 1) % values.length];
         }

         this.lastRandomShader = chosen;
         return chosen;
      } else {
         return this.backgroundShader.getValue();
      }
   }

   public static enum EMode {
      TROLL_HACK("TrollHack"),
      MINECRAFT("Minecraft");

      public final String displayName;

      private EMode(String displayName) {
         this.displayName = displayName;
      }
   }

   public static enum Mode {
      RANDOM,
      SET;

      public String getChinese() {
         return switch (this) {
            case RANDOM -> "\u968f\u673a";
            case SET -> "\u56fa\u5b9a";
         };
      }
   }

   public static enum ShaderMode {
      SNOWFLAKE,
      WATER,
      HEART,
      STAR,
      FIREFLY;

      public String getChinese() {
         return switch (this) {
            case SNOWFLAKE -> "\u96ea\u82b1";
            case WATER -> "\u6c34\u6ce2";
            case HEART -> "\u7231\u5fc3";
            case STAR -> "\u661f\u7a7a";
            case FIREFLY -> "\u8424\u706b\u866b";
         };
      }
   }
}
