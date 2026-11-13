package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.modules.Module;
import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

public class ChatCensor extends Module {
   public static ChatCensor INSTANCE;
   private static final List<String> BAD_WORDS = List.of("\u50bb", "\u903c", "\u64cd", "\u6b7b", "\u5988", "\u91ce", "\u7239", "\u7238");
   private static final String REPLACE = "*";

   public ChatCensor() {
      super("ChatCensor", "Censors bad words in vanilla server chat", Module.Category.Misc);
      this.setChinese("\u5fc3\u6001\u5766\u514b");
      INSTANCE = this;
   }

   public static boolean hasBadWord(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (String var2 : BAD_WORDS) {
            if (var0.contains(var2)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static String censor(String var0) {
      if (!hasBadWord(var0)) {
         return var0;
      } else {
         String var1 = var0;

         for (Object var4 : BAD_WORDS) {
            String var5 = (String)var4;
            if (var1.contains(var5)) {
               var1 = var1.replace(var5, "*".repeat(var5.length()));
            }
         }

         return var1;
      }
   }

   public static Text censorText(Text var0) {
      if (INSTANCE != null && !INSTANCE.isOff() && var0 != null) {
         String var1 = var0.getString();
         return !hasBadWord(var1) ? var0 : rebuild(var0);
      } else {
         return var0;
      }
   }

   private static Text rebuild(Text var0) {
      TextContent var1 = var0.getContent();
      MutableText var2 = MutableText.of(var1);
      if (var1 instanceof PlainTextContent var3) {
         String var4 = var3.string();
         String var5 = censor(var4);
         if (!var5.equals(var4)) {
            var2 = MutableText.of(PlainTextContent.of(var5));
         }
      }

      var2.setStyle(var0.getStyle());

      for (Object var8 : var0.getSiblings()) {
         var2.append(censorText((Text)var8));
      }

      return var2;
   }
}
