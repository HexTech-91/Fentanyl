package dev.luminous.api.utils.music;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NetEaseAPI {
   private static final String API_BASE = "https://met.api.xiaoguan.fit/api";
   private static final String API_FALLBACK_1 = "https://api.mizuki.mysqil.com/api/meting";
   private static final String API_FALLBACK_2 = "https://api.injahow.cn/meting/";
   private static final int CONNECT_TIMEOUT = 8000;
   private static final int READ_TIMEOUT = 15000;
   public static boolean DEBUG = false;

   private static void log(String var0) {
      if (DEBUG) {
         System.out.println("[NetEaseAPI] " + var0);
      }
   }

   public static List<SongInfo> search(String var0, int var1) throws Exception {
      var1 = Math.min(var1, 30);
      String var2 = URLEncoder.encode(var0, StandardCharsets.UTF_8);
      String var3 = "https://met.api.xiaoguan.fit/api?server=netease&type=search&id=" + var2;
      log("Searching: " + var0 + " -> " + var3);
      String var4 = httpGet(var3);
      List var5 = parseSearchResult(var4, var1);
      log("Primary API returned " + var5.size() + " results");
      if (var5.isEmpty()) {
         var5 = tryFallbackSearch(var2, var1);
      }

      return var5;
   }

   private static List<SongInfo> tryFallbackSearch(String var0, int var1) {
      try {
         String var2 = "https://api.mizuki.mysqil.com/api/meting?server=netease&type=search&id=" + var0;
         log("Trying fallback 1: " + var2);
         String var3 = httpGet(var2);
         List var4 = parseSearchResult(var3, var1);
         if (!var4.isEmpty()) {
            log("Fallback 1 returned " + var4.size() + " results");
            return var4;
         }
      } catch (Exception var6) {
         log("Fallback 1 failed: " + var6.getMessage());
      }

      try {
         String var7 = "https://api.injahow.cn/meting/?server=netease&type=search&id=" + var0;
         log("Trying fallback 2: " + var7);
         String var8 = httpGet(var7);
         List var9 = parseSearchResult(var8, var1);
         if (!var9.isEmpty()) {
            log("Fallback 2 returned " + var9.size() + " results");
            return var9;
         }
      } catch (Exception var5) {
         log("Fallback 2 failed: " + var5.getMessage());
      }

      return new ArrayList<>();
   }

   public static String getSongUrl(String var0) {
      String var1 = tryGetUrl("https://met.api.xiaoguan.fit/api", var0);
      if (var1 != null) {
         return var1;
      } else {
         var1 = tryGetUrl("https://api.mizuki.mysqil.com/api/meting", var0);
         if (var1 != null) {
            return var1;
         } else {
            var1 = tryGetUrl("https://api.injahow.cn/meting/", var0);
            return var1 != null ? var1 : null;
         }
      }
   }

   private static String tryGetUrl(String var0, String var1) {
      try {
         String var2 = var0 + "?server=netease&type=url&id=" + var1 + "&br=320000";
         log("Getting URL: " + var2);
         URL var3 = URI.create(var2).toURL();
         HttpURLConnection var4 = (HttpURLConnection)var3.openConnection();
         var4.setInstanceFollowRedirects(false);
         var4.setRequestMethod("GET");
         var4.setConnectTimeout(8000);
         var4.setReadTimeout(15000);
         var4.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
         var4.setRequestProperty("Accept", "*/*");

         try {
            int var5 = var4.getResponseCode();
            log("URL response code: " + var5);
            if (var5 == 302 || var5 == 301 || var5 == 307 || var5 == 308) {
               String var6 = var4.getHeaderField("Location");
               log("Redirect to: " + var6);
               if (var6 != null && !var6.isEmpty()) {
                  if (var6.contains("music.126.net")
                     || var6.endsWith(".mp3")
                     || var6.contains("kuwo.cn")
                     || var6.contains("kugou.com")
                     || var6.contains("qq.com")) {
                     return var6;
                  }

                  if (var6.startsWith("http")) {
                     return var6;
                  }
               }
            }

            if (var5 == 200) {
               try (BufferedReader var25 = new BufferedReader(new InputStreamReader(var4.getInputStream(), StandardCharsets.UTF_8))) {
                  StringBuilder var7 = new StringBuilder(512);

                  String var8;
                  while ((var8 = var25.readLine()) != null) {
                     var7.append(var8);
                  }

                  String var9 = var7.toString();
                  if (!var9.isEmpty()) {
                     log("URL JSON response: " + (var9.length() > 200 ? var9.substring(0, 200) + "..." : var9));
                     String var10 = extractString(var9, "url");
                     if (var10 != null && !var10.isEmpty() && var10.startsWith("http")) {
                        return var10;
                     }
                  }
               }
            }

            if (var5 >= 400) {
               try (BufferedReader var26 = new BufferedReader(new InputStreamReader(var4.getErrorStream(), StandardCharsets.UTF_8))) {
                  StringBuilder var27 = new StringBuilder();

                  String var30;
                  while ((var30 = var26.readLine()) != null) {
                     var27.append(var30);
                  }

                  log("Error response: " + var27.toString());
               }
            }

            return null;
         } finally {
            var4.disconnect();
         }
      } catch (Exception var24) {
         log("Failed to get URL from " + var0 + ": " + var24.getMessage());
         return null;
      }
   }

   public static List<SongInfo> getPlaylist(String var0) throws Exception {
      if (var0.equalsIgnoreCase("daily")) {
         return getDailyRecommendation();
      } else if (var0.equalsIgnoreCase("hot")) {
         return getHotSongs();
      } else {
         String var1 = URLEncoder.encode(var0, StandardCharsets.UTF_8);
         String var2 = "https://met.api.xiaoguan.fit/api?server=netease&type=playlist&id=" + var1;
         String var3 = httpGet(var2);
         List var4 = parsePlaylistResult(var3);
         if (var4.isEmpty()) {
            try {
               String var5 = "https://api.mizuki.mysqil.com/api/meting?server=netease&type=playlist&id=" + var1;
               String var6 = httpGet(var5);
               var4 = parsePlaylistResult(var6);
            } catch (Exception var7) {
            }
         }

         return var4;
      }
   }

   private static List<SongInfo> getDailyRecommendation() throws Exception {
      return getPlaylist("3778678");
   }

   private static List<SongInfo> getHotSongs() throws Exception {
      return getPlaylist("3778678");
   }

   public static String getLyric(String var0) throws Exception {
      String var1 = "https://met.api.xiaoguan.fit/api?server=netease&type=lrc&id=" + var0;
      return httpGet(var1);
   }

   public static boolean testConnection() {
      try {
         String var0 = "https://met.api.xiaoguan.fit/api?server=netease&type=search&id=test";
         String var1 = httpGet(var0);
         return var1 != null && !var1.isEmpty() && var1.contains("[");
      } catch (Exception var2) {
         log("Connection test failed: " + var2.getMessage());
         return false;
      }
   }

   private static String httpGet(String var0) throws Exception {
      URL var1 = URI.create(var0).toURL();
      HttpURLConnection var2 = (HttpURLConnection)var1.openConnection();
      var2.setRequestMethod("GET");
      var2.setConnectTimeout(8000);
      var2.setReadTimeout(15000);
      var2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
      var2.setRequestProperty("Accept", "application/json, text/plain, */*");
      var2.setInstanceFollowRedirects(true);

      String var19;
      try {
         int var3 = var2.getResponseCode();
         if (var3 < 400) {
            try (BufferedReader var20 = new BufferedReader(new InputStreamReader(var2.getInputStream(), StandardCharsets.UTF_8))) {
               StringBuilder var21 = new StringBuilder(4096);

               String var22;
               while ((var22 = var20.readLine()) != null) {
                  var21.append(var22);
               }

               return var21.toString();
            }
         }

         log("HTTP " + var3 + " for " + var0);

         try (BufferedReader var4 = new BufferedReader(new InputStreamReader(var2.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder var5 = new StringBuilder();

            String var6;
            while ((var6 = var4.readLine()) != null) {
               var5.append(var6);
            }

            log("Error body: " + var5.toString());
         }

         var19 = "";
      } finally {
         var2.disconnect();
      }

      return var19;
   }

   private static List<SongInfo> parseSearchResult(String var0, int var1) {
      ArrayList var2 = new ArrayList(var1);
      if (var0 != null && !var0.isEmpty()) {
         try {
            int var3 = var0.indexOf(91);
            if (var3 < 0) {
               log("No array found in response, trying object parsing");
               return var2;
            }

            int var4 = 0;
            int var5 = -1;

            for (int var6 = var3; var6 < var0.length() && var2.size() < var1; var6++) {
               char var7 = var0.charAt(var6);
               if (var7 == '{') {
                  if (var4 == 0) {
                     var5 = var6;
                  }

                  var4++;
               } else if (var7 == '}') {
                  if (--var4 == 0 && var5 >= 0) {
                     SongInfo var8 = parseSongObject(var0.substring(var5, var6 + 1));
                     if (var8 != null) {
                        var2.add(var8);
                     }

                     var5 = -1;
                  }
               }
            }
         } catch (Exception var9) {
            log("Parse error: " + var9.getMessage());
         }

         return var2;
      } else {
         return var2;
      }
   }

   private static SongInfo parseSongObject(String var0) {
      try {
         String var1 = extractString(var0, "id");
         String var2 = extractString(var0, "title");
         String var3 = extractString(var0, "author");
         String var4 = extractString(var0, "pic");
         String var5 = extractString(var0, "url");
         if (var2 == null) {
            var2 = extractString(var0, "name");
            if (var2 == null) {
               return null;
            }

            var3 = extractString(var0, "artist");
            var4 = extractString(var0, "pic");
            var5 = extractString(var0, "url");
         }

         if (var1 == null && var2 != null && var5 != null) {
            int var6 = var5.lastIndexOf("id=");
            if (var6 >= 0) {
               var1 = var5.substring(var6 + 3);
               int var7 = var1.indexOf(38);
               if (var7 >= 0) {
                  var1 = var1.substring(0, var7);
               }
            }
         }

         if (var2 != null) {
            String var10 = cleanJsonString(var2);
            String var11 = var3 != null ? cleanJsonString(var3) : "\u672a\u77e5\u6b4c\u624b";
            String var8 = var5 != null && !var5.isEmpty() ? var5 : null;
            return new SongInfo(var1 != null ? var1 : "0", var10, var11, "", var8, var4, 0L);
         }
      } catch (Exception var9) {
         log("Song parse error: " + var9.getMessage());
      }

      return null;
   }

   private static String extractString(String var0, String var1) {
      int var2 = var0.indexOf("\"" + var1 + "\"");
      if (var2 < 0) {
         var2 = var0.indexOf("'" + var1 + "'");
      }

      if (var2 < 0) {
         return null;
      } else {
         int var3 = var0.indexOf(58, var2);
         if (var3 < 0) {
            return null;
         } else {
            int var4 = var3 + 1;

            while (var4 < var0.length() && Character.isWhitespace(var0.charAt(var4))) {
               var4++;
            }

            if (var4 >= var0.length()) {
               return null;
            } else if (var0.charAt(var4) == '"') {
               StringBuilder var8 = new StringBuilder();
               int var6 = var4 + 1;

               while (var6 < var0.length()) {
                  char var7 = var0.charAt(var6);
                  if (var7 == '\\' && var6 + 1 < var0.length()) {
                     var8.append(var0.charAt(var6 + 1));
                     var6 += 2;
                  } else {
                     if (var7 == '"') {
                        break;
                     }

                     var8.append(var7);
                     var6++;
                  }
               }

               return var8.toString();
            } else {
               int var5 = var4;

               while (
                  var5 < var0.length()
                     && (
                        Character.isDigit(var0.charAt(var5))
                           || var0.charAt(var5) == '.'
                           || var0.charAt(var5) == '-'
                           || var0.charAt(var5) == 'e'
                           || var0.charAt(var5) == 'E'
                     )
               ) {
                  var5++;
               }

               if (var5 > var4) {
                  return var0.substring(var4, var5);
               } else {
                  return var0.startsWith("null", var4) ? null : null;
               }
            }
         }
      }
   }

   static String cleanJsonString(String var0) {
      return var0 == null ? "" : var0.replace("\\/", "/").replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "");
   }

   private static List<SongInfo> parsePlaylistResult(String var0) {
      return parseSearchResult(var0, 50);
   }
}
