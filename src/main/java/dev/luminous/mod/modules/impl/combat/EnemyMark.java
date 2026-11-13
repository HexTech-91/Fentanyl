package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;

public class EnemyMark extends Module {
   public static EnemyMark INSTANCE;
   public final StringSetting attackName = this.add(
      new StringSetting(new String(new char[]{'C', 'u', 's', 't', 'o', 'm', 'A', 't', 't', 'a', 'c', 'k', 'N', 'a', 'm', 'e'}), new String())
   );

   public EnemyMark() {
      super(new String(new char[]{'E', 'n', 'e', 'm', 'y', 'M', 'a', 'r', 'k'}), Module.Category.Combat);
      this.setChinese(new String(new char[]{'\u4ec7', '\u4eba', '\u6807', '\u8bb0'}));
      this.attackName.setChinese(new String(new char[]{'\u81ea', '\u5b9a', '\u4e49', '\u653b', '\u51fb', '\u540d', '\u79f0'}));
      INSTANCE = this;
   }

   public static boolean isMarked(PlayerEntity var0) {
      return var0 == null ? false : isMarkedName(var0.getName().getString());
   }

   public static boolean isMarkedName(String var0) {
      if (INSTANCE != null && var0 != null && !var0.isBlank()) {
         for (String var2 : parseNames(INSTANCE.attackName.getValue())) {
            if (var2.equalsIgnoreCase(var0)) {
               removeFriend(var0);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static int markNames(String var0) {
      if (INSTANCE == null) {
         return 0;
      } else {
         ArrayList<String> var1 = parseNames(INSTANCE.attackName.getValue());
         int var2 = 0;

         for (String var4 : parseNames(var0)) {
            boolean var5 = false;

            for (String var7 : var1) {
               if (var7.equalsIgnoreCase(var4)) {
                  var5 = true;
                  break;
               }
            }

            removeFriend(var4);
            if (!var5) {
               var1.add(var4);
               var2++;
            }
         }

         INSTANCE.attackName.setValue(joinNames(var1));
         return var2;
      }
   }

   public static void clearMarks() {
      if (INSTANCE != null) {
         INSTANCE.attackName.setValue(new String());
      }
   }

   public static List<String> getMarkedNames() {
      return (List<String>)(INSTANCE == null ? List.of() : new ArrayList<>(parseNames(INSTANCE.attackName.getValue())));
   }

   public static PlayerEntity getPriorityTarget(double var0) {
      if (INSTANCE != null && INSTANCE.isOn() && mc.player != null && mc.world != null) {
         double var2 = var0 * var0;
         PlayerEntity var4 = null;
         double var5 = Double.MAX_VALUE;
         double var7 = Double.MAX_VALUE;

         for (PlayerEntity var10 : mc.world.getPlayers()) {
            if (var10 != null && var10 != mc.player && var10.isAlive() && isMarked(var10)) {
               double var11 = var10.getX() - mc.player.getX();
               double var13 = var10.getY() - mc.player.getY();
               double var15 = var10.getZ() - mc.player.getZ();
               double var17 = var11 * var11 + var13 * var13 + var15 * var15;
               if (!(var17 > var2) && !Alien.FRIEND.isFriend(var10)) {
                  double var19 = var10.getHealth() + var10.getAbsorptionAmount();
                  if (var4 == null || var19 < var5 || var19 == var5 && var17 < var7) {
                     var4 = var10;
                     var5 = var19;
                     var7 = var17;
                  }
               }
            }
         }

         return var4;
      } else {
         return null;
      }
   }

   private static ArrayList<String> parseNames(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         StringBuilder var2 = new StringBuilder();

         for (int var3 = 0; var3 < var0.length(); var3++) {
            char var4 = var0.charAt(var3);
            if (var4 != ',' && var4 != ';' && var4 != ' ' && var4 != '\t' && var4 != '\n') {
               var2.append(var4);
            } else {
               addToken(var1, var2);
            }
         }

         addToken(var1, var2);
         return var1;
      }
   }

   private static void addToken(ArrayList<String> var0, StringBuilder var1) {
      String var2 = var1.toString().trim();
      var1.setLength(0);
      if (!var2.isEmpty()) {
         var0.add(var2);
      }
   }

   private static String joinNames(List<String> var0) {
      StringBuilder var1 = new StringBuilder();

      for (String var3 : var0) {
         if (var1.length() > 0) {
            var1.append(',');
         }

         var1.append(var3);
      }

      return var1.toString();
   }

   private static void removeFriend(String var0) {
      if (Alien.FRIEND != null && var0 != null) {
         for (String var2 : new ArrayList<>(Alien.FRIEND.friendList)) {
            if (var2.equalsIgnoreCase(var0)) {
               Alien.FRIEND.remove(var2);
            }
         }
      }
   }
}
