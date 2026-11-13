package dev.luminous.api.utils.music;

import dev.luminous.core.Manager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MusicStore {
   private static final Object LOCK = new Object();
   private static final String FILE = "music.txt";

   private MusicStore() {
   }

   public static String getMusicU() {
      synchronized (LOCK) {
         for (String var2 : readLines()) {
            if (var2.startsWith("musicU=")) {
               String var3 = var2.substring(7).trim();
               return var3.isEmpty() ? null : var3;
            }
         }

         return null;
      }
   }

   public static String getNickname() {
      synchronized (LOCK) {
         for (String var2 : readLines()) {
            if (var2.startsWith("nickname=")) {
               return var2.substring(9).trim();
            }
         }

         return null;
      }
   }

   public static long getUserId() {
      synchronized (LOCK) {
         for (String var2 : readLines()) {
            if (var2.startsWith("userId=")) {
               long var10000;
               try {
                  var10000 = Long.parseLong(var2.substring(7).trim());
               } catch (NumberFormatException var5) {
                  return 0L;
               }

               return var10000;
            }
         }

         return 0L;
      }
   }

   public static void saveLogin(String var0, String var1, long var2) {
      synchronized (LOCK) {
         ArrayList var5 = new ArrayList();

         for (String var7 : readLines()) {
            if (!var7.startsWith("musicU=") && !var7.startsWith("nickname=") && !var7.startsWith("userId=")) {
               var5.add(var7);
            }
         }

         if (var0 != null) {
            var5.add("musicU=" + var0);
         }

         if (var1 != null && !var1.isEmpty()) {
            var5.add("nickname=" + var1);
         }

         if (var2 > 0L) {
            var5.add("userId=" + var2);
         }

         writeLines(var5);
      }
   }

   public static void clearLogin() {
      saveLogin(null, null, 0L);
   }

   public static boolean isFavourite(String var0) {
      synchronized (LOCK) {
         for (String var3 : readLines()) {
            if (var3.startsWith("fav=") && var3.substring(4).trim().equals(var0)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void toggleFavourite(SongInfo var0) {
      synchronized (LOCK) {
         List<String> var2 = readLines();
         ArrayList var3 = new ArrayList(var2);
         String var4 = "fav=" + var0.id;
         boolean var5 = var3.removeIf(var1 -> var1.equals(var4));
         if (!var5) {
            var3.add(var4);
            var3.add("favName=" + var0.id + var0.name + " - " + var0.artist);
         }

         writeLines(var3);
      }
   }

   public static List<SongInfo> favourites() {
      ArrayList var0 = new ArrayList();
      synchronized (LOCK) {
         List<String> var2 = readLines();

         for (String var4 : var2) {
            if (var4.startsWith("fav=")) {
               String var5 = var4.substring(4).trim();
               String var6 = "\u672a\u77e5\u6b4c\u66f2";

               for (String var8 : var2) {
                  if (var8.startsWith("favName=" + var5)) {
                     var6 = var8.substring(("favName=" + var5).length());
                     break;
                  }
               }

               var0.add(new SongInfo(var5, var6, "", "", null, null, 0L));
            }
         }

         return var0;
      }
   }

   private static List<String> readLines() {
      try {
         File var0 = Manager.getFile("music.txt");
         if (var0.exists()) {
            return new ArrayList<>(Files.readAllLines(var0.toPath(), StandardCharsets.UTF_8));
         }
      } catch (IOException var1) {
      }

      return new ArrayList<>();
   }

   private static void writeLines(List<String> var0) {
      try {
         Files.write(Manager.getFile("music.txt").toPath(), var0, StandardCharsets.UTF_8);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }
}
