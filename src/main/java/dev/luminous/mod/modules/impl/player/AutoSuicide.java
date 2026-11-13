package dev.luminous.mod.modules.impl.player;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;

public class AutoSuicide extends Module {
   public final EnumSetting<AutoSuicide.CommandType> commandType = this.add(new EnumSetting<>("CommandType", AutoSuicide.CommandType.Kill));
   public final BooleanSetting instant = this.add(new BooleanSetting("Instant", false));

   public AutoSuicide() {
      super("AutoSuicide", "\u81ea\u52a8\u53d1\u9001\u81ea\u6740\u6307\u4ee4", Module.Category.Player);
      this.setChinese("\u81ea\u52a8\u81ea\u6740");
      FeatureTranslations.localizeModule(this);
   }

   @Override
   public void onEnable() {
      if (this.instant.getValue()) {
         this.sendSuicideCommand();
      }
   }

   @Override
   public void onDisable() {
   }

   private void sendSuicideCommand() {
      if (mc.player != null && mc.player.networkHandler != null) {
         String command = this.commandType.getValue() == AutoSuicide.CommandType.Kill ? "/kill" : "/514";
         mc.player.networkHandler.sendChatCommand(command.substring(1));
      }
   }

   public static enum CommandType {
      Kill,
      FiveOneFour;
   }
}
