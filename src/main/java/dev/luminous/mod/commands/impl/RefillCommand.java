package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.combat.Refill;
import java.util.List;

public class RefillCommand extends Command {
   public RefillCommand() {
      super("refill", "\u7ba1\u7406\u81ea\u52a8\u8865\u7ed9\u5957\u88c5");
   }

   @Override
   public void runCommand(String[] var1) {
      Refill var2 = (Refill)Alien.MODULE.getModuleByName("Refill");
      if (var2 == null) {
         this.sendChatMessage("\u00a7cRefill \u6a21\u5757\u672a\u52a0\u8f7d\uff01");
      } else {
         var2.onCommand(var1);
      }
   }

   @Override
   public String[] getAutocorrect(int var1, List<String> var2) {
      return var1 == 1 ? new String[]{"save", "remove", "use"} : null;
   }
}
