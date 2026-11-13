package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.misc.Music;
import java.util.List;

public class MusicCommand extends Command {
   public MusicCommand() {
      super("music", "search <\u5173\u952e\u5b57> | top | queue | now | next | prev | pause | resume | stop | ping");
   }

   @Override
   public void runCommand(String[] var1) {
      Music var2 = (Music)Alien.MODULE.getModuleByName("Music");
      if (var2 == null) {
         this.sendChatMessage("\u00a7cMusic \u6a21\u5757\u672a\u52a0\u8f7d\uff01");
      } else {
         var2.onCommand(var1);
      }
   }

   @Override
   public String[] getAutocorrect(int var1, List<String> var2) {
      return var1 == 1 ? new String[]{"search", "top", "queue", "now", "next", "prev", "pause", "resume", "stop", "ping"} : null;
   }
}
