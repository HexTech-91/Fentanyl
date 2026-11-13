package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.Fonts;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.io.File;
import java.util.List;

public class LoadCommand extends Command {
   public LoadCommand() {
      super("load", "[config]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         this.sendChatMessage("\u00a7fLoading..");
         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         System.out.println("[Alien] Loading config from: " + ConfigManager.options.getAbsolutePath());
         Alien.CONFIG = new ConfigManager();
         int readCount = Alien.CONFIG.getReadCount();
         ConfigManager.options = Manager.getFile("options.txt");
         int loaded = Alien.CONFIG.load();
         Alien.save();
         Fonts.INSTANCE.refresh();
         this.sendChatMessage("\u00a7aLoaded \u00a7f" + loaded + "\u00a7a/\u00a7f" + readCount + " \u00a7asettings from \u00a7f" + parameters[0]);
         String cmp = Alien.CONFIG.getKeyComparison();
         if (cmp != null && !cmp.isEmpty()) {
            this.sendChatMessage(cmp);
         }

         System.out.println("[Alien] Load command complete: " + loaded + " of " + readCount + " settings loaded");
         int verified = 0;

         for (Module m : Alien.MODULE.getModules()) {
            if (verified >= 3) {
               break;
            }

            StringBuilder sb = new StringBuilder("\u00a77[" + m.getName() + "=" + (m.isOn() ? "\u00a7aON" : "\u00a7cOFF") + "\u00a77]");
            int settingCount = 0;

            for (Setting s : m.getSettings()) {
               if (settingCount >= 3) {
                  break;
               }

               String val = "";
               if (s instanceof BooleanSetting bs) {
                  val = String.valueOf(bs.getValue());
               } else if (s instanceof SliderSetting ss) {
                  val = String.valueOf(ss.getValue());
               } else {
                  val = s.getClass().getSimpleName();
               }

               sb.append(" ").append(s.getName()).append("=").append(val);
               settingCount++;
            }

            this.sendChatMessage(sb.toString());
            verified++;
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
