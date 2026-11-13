package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class Tp extends Module {
   public static Tp INSTANCE;
   private final StringSetting coords = this.add(new StringSetting("Coords", ""));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));

   public Tp() {
      super("Tp", Module.Category.Survival);
      this.coords.setChinese("\u5750\u6807");
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.setChinese("TP");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         String var1 = this.coords.getValue();
         if (var1 != null && !var1.trim().isEmpty()) {
            String[] var2 = parseCoords(var1);
            if (var2 == null) {
               this.sendMessage("\u00a7c\u5750\u6807\u683c\u5f0f\u9519\u8bef\uff0c\u5e94\u4e3a\uff1ax y z");
               this.setState(false);
            } else {
               Alien.COMMAND.command(new String[]{"tp", var2[0], var2[1], var2[2]});
               if (this.autoDisable.getValue()) {
                  this.setState(false);
               }
            }
         } else {
            this.sendMessage("\u00a7c\u8bf7\u5148\u586b\u5199\u5750\u6807\uff0c\u683c\u5f0f\uff1ax y z");
            this.setState(false);
         }
      }
   }

   private static String[] parseCoords(String var0) {
      String var1 = var0.trim().replace(",", " ").replace("\uff0c", " ").trim();
      String[] var2 = var1.split("\\s+");
      if (var2.length < 3) {
         return null;
      } else {
         for (int var3 = 0; var3 < 3; var3++) {
            String var4 = var2[var3];
            if (!var4.startsWith("~") && !var4.startsWith("^")) {
               try {
                  Double.parseDouble(var4);
               } catch (NumberFormatException var6) {
                  return null;
               }
            }
         }

         return new String[]{var2[0], var2[1], var2[2]};
      }
   }

   @Override
   public String getInfo() {
      String var1 = this.coords.getValue();
      return var1 != null && !var1.trim().isEmpty() ? var1.trim() : null;
   }
}
