package dev.luminous.mod.commands.impl;

import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.combat.EnemyMark;
import java.util.List;

public class CrCommand extends Command {
   public CrCommand() {
      super(
         new String(new char[]{'c', 'r'}),
         new String(
            new char[]{
               '[',
               'n',
               'a',
               'm',
               'e',
               ']',
               ' ',
               '(',
               'm',
               'u',
               'l',
               't',
               'i',
               'p',
               'l',
               'e',
               ' ',
               'n',
               'a',
               'm',
               'e',
               's',
               ' ',
               's',
               'e',
               'p',
               'a',
               'r',
               'a',
               't',
               'e',
               'd',
               ' ',
               'b',
               'y',
               ' ',
               's',
               'p',
               'a',
               'c',
               'e',
               ' ',
               'o',
               'r',
               ' ',
               'c',
               'o',
               'm',
               'm',
               'a',
               ')'
            }
         )
      );
   }

   @Override
   public void runCommand(String[] var1) {
      if (var1.length == 0) {
         this.sendUsage();
      } else if (var1.length == 1 && var1[0].equalsIgnoreCase(new String(new char[]{'r', 'e', 's', 'e', 't'}))) {
         EnemyMark.clearMarks();
         this.sendChatMessage(new String(new char[]{'E', 'n', 'e', 'm', 'y', ' ', 'm', 'a', 'r', 'k', 's', ' ', 'c', 'l', 'e', 'a', 'r', 'e', 'd'}));
      } else if (var1.length == 1 && var1[0].equalsIgnoreCase(new String(new char[]{'l', 'i', 's', 't'}))) {
         List var7 = EnemyMark.getMarkedNames();
         this.sendChatMessage(
            var7.isEmpty()
               ? new String(new char[]{'E', 'n', 'e', 'm', 'y', ' ', 'm', 'a', 'r', 'k', 's', ':', ' ', 'e', 'm', 'p', 't', 'y'})
               : new String(new char[]{'E', 'n', 'e', 'm', 'y', ' ', 'm', 'a', 'r', 'k', 's', ':', ' '}) + String.join(new String(new char[]{',', ' '}), var7)
         );
      } else {
         int var2 = 0;

         for (String var6 : var1) {
            var2 += EnemyMark.markNames(var6);
         }

         this.sendChatMessage(new String(new char[]{'E', 'n', 'e', 'm', 'y', ' ', 'm', 'a', 'r', 'k', 's', ' ', 'a', 'd', 'd', 'e', 'd', ':', ' '}) + var2);
      }
   }

   @Override
   public String[] getAutocorrect(int var1, List<String> var2) {
      return null;
   }
}
