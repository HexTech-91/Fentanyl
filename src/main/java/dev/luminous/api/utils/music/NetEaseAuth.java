package dev.luminous.api.utils.music;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class NetEaseAuth {
   private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
   private static final String IV = "0102030405060708";
   private static final String RSA_MODULUS_HEX = "e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
   private static final String BASE = "https://music.163.com";
   private static final SecureRandom RANDOM = new SecureRandom();
   public static final List<String> LRC_TEXTS = new ArrayList<>();

   private static String rsaEncrypt(String var0) throws Exception {
      BigInteger var1 = new BigInteger(
         "e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7",
         16
      );
      RSAPublicKeySpec var2 = new RSAPublicKeySpec(var1, BigInteger.valueOf(65537L));
      KeyFactory var3 = KeyFactory.getInstance("RSA");
      PublicKey var4 = var3.generatePublic(var2);
      Cipher var5 = Cipher.getInstance("RSA/ECB/NoPadding");
      var5.init(1, var4);
      String var6 = new StringBuilder(var0).reverse().toString();
      StringBuilder var7 = new StringBuilder();

      for (byte var11 : var6.getBytes(StandardCharsets.UTF_8)) {
         var7.append(String.format("%02x", var11));
      }

      StringBuilder var12 = new StringBuilder();

      for (int var13 = 0; var13 < 128 - var7.length(); var13++) {
         var12.append('0');
      }

      var12.append((CharSequence)var7);
      byte[] var14 = var5.doFinal(hexToBytes(var12.toString()));
      return bytesToHex(var14).toUpperCase();
   }

   private static byte[] hexToBytes(String var0) {
      int var1 = var0.length();
      byte[] var2 = new byte[var1 / 2];

      for (byte var3 = 0; var3 < var1; var3 += 2) {
         var2[var3 / 2] = (byte)((Character.digit(var0.charAt(var3), 16) << 4) + Character.digit(var0.charAt(var3 + 1), 16));
      }

      return var2;
   }

   private static String bytesToHex(byte[] var0) {
      StringBuilder var1 = new StringBuilder(var0.length * 2);

      for (byte var5 : var0) {
         var1.append(String.format("%02x", var5));
      }

      return var1.toString();
   }

   private static String randomHex(int var0) {
      StringBuilder var1 = new StringBuilder(var0);
      String var2 = "0123456789abcdef";

      for (int var3 = 0; var3 < var0; var3++) {
         var1.append(var2.charAt(RANDOM.nextInt(var2.length())));
      }

      return var1.toString();
   }

   private static String aesEncrypt(String var0, String var1) throws Exception {
      Cipher var2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
      var2.init(1, new SecretKeySpec(var1.getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec("0102030405060708".getBytes(StandardCharsets.UTF_8)));
      return Base64.getEncoder().encodeToString(var2.doFinal(var0.getBytes(StandardCharsets.UTF_8)));
   }

   private static String weapiForm(String var0) throws Exception {
      String var1 = randomHex(16);
      String var2 = aesEncrypt(var0, "0CoJUm6Qyw8W8jud");
      String var3 = aesEncrypt(var2, var1);
      String var4 = rsaEncrypt(var1);
      return "params=" + URLEncoder.encode(var3, StandardCharsets.UTF_8) + "&encSecKey=" + URLEncoder.encode(var4, StandardCharsets.UTF_8);
   }

   private static String post(String var0, Map<String, String> var1, String var2) throws Exception {
      URL var3 = URI.create("https://music.163.com" + var0).toURL();
      HttpURLConnection var4 = (HttpURLConnection)var3.openConnection();
      var4.setRequestMethod("POST");
      var4.setDoOutput(true);
      var4.setConnectTimeout(8000);
      var4.setReadTimeout(15000);
      var4.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
      var4.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      var4.setRequestProperty("Referer", "https://music.163.com");
      var4.setRequestProperty("Origin", "https://music.163.com");
      StringBuilder var5 = new StringBuilder();

      for (Entry var7 : var1.entrySet()) {
         if (var5.length() > 0) {
            var5.append("; ");
         }

         var5.append((String)var7.getKey()).append('=').append((String)var7.getValue());
      }

      if (var5.length() > 0) {
         var4.setRequestProperty("Cookie", var5.toString());
      }

      byte[] var15 = weapiForm(var2).getBytes(StandardCharsets.UTF_8);

      String var11;
      try {
         var4.getOutputStream().write(var15);
         int var16 = var4.getResponseCode();
         BufferedReader var8 = new BufferedReader(new InputStreamReader(var16 >= 400 ? var4.getErrorStream() : var4.getInputStream(), StandardCharsets.UTF_8));
         StringBuilder var9 = new StringBuilder(2048);

         String var10;
         while ((var10 = var8.readLine()) != null) {
            var9.append(var10);
         }

         var8.close();
         var11 = var9.toString();
      } finally {
         var4.disconnect();
      }

      return var11;
   }

   private static String get(String var0, Map<String, String> var1) throws Exception {
      URL var2 = URI.create(var0).toURL();
      HttpURLConnection var3 = (HttpURLConnection)var2.openConnection();
      var3.setRequestMethod("GET");
      var3.setConnectTimeout(8000);
      var3.setReadTimeout(15000);
      var3.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
      var3.setRequestProperty("Referer", "https://music.163.com");
      StringBuilder var4 = new StringBuilder();

      for (Entry var6 : var1.entrySet()) {
         if (var4.length() > 0) {
            var4.append("; ");
         }

         var4.append((String)var6.getKey()).append('=').append((String)var6.getValue());
      }

      if (var4.length() > 0) {
         var3.setRequestProperty("Cookie", var4.toString());
      }

      String var8;
      try {
         BufferedReader var12 = new BufferedReader(new InputStreamReader(var3.getInputStream(), StandardCharsets.UTF_8));
         StringBuilder var13 = new StringBuilder(2048);

         String var7;
         while ((var7 = var12.readLine()) != null) {
            var13.append(var7);
         }

         var12.close();
         var8 = var13.toString();
      } finally {
         var3.disconnect();
      }

      return var8;
   }

   public static String qrKey() throws Exception {
      String var0 = post("/weapi/login/qrcode/unikey", new HashMap<>(), "{\"type\":1}");
      String var1 = extractString(var0, "unikey");
      if (var1 == null) {
         throw new RuntimeException("no unikey in response: " + var0);
      } else {
         return var1;
      }
   }

   public static NetEaseAuth.QrPoll qrStateWithCookies(String var0) throws Exception {
      URL var1 = URI.create("https://music.163.com/weapi/login/qrcode/client/login").toURL();
      HttpURLConnection var2 = (HttpURLConnection)var1.openConnection();
      var2.setRequestMethod("POST");
      var2.setDoOutput(true);
      var2.setConnectTimeout(8000);
      var2.setReadTimeout(15000);
      var2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
      var2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      var2.setRequestProperty("Referer", "https://music.163.com");
      var2.setRequestProperty("Origin", "https://music.163.com");
      byte[] var3 = weapiForm("{\"key\":\"" + var0 + "\",\"type\":1}").getBytes(StandardCharsets.UTF_8);

      NetEaseAuth.QrPoll var19;
      try {
         var2.getOutputStream().write(var3);
         BufferedReader var4 = new BufferedReader(
            new InputStreamReader(var2.getResponseCode() >= 400 ? var2.getErrorStream() : var2.getInputStream(), StandardCharsets.UTF_8)
         );
         StringBuilder var5 = new StringBuilder(1024);

         String var6;
         while ((var6 = var4.readLine()) != null) {
            var5.append(var6);
         }

         var4.close();
         String var7 = var5.toString();
         String var8 = extractString(var7, "code");
         int var9 = var8 != null ? Integer.parseInt(var8) : 801;
         String var10 = null;
         if (var9 == 803) {
            ArrayList<String> var11 = new ArrayList<>();

            for (Entry var13 : var2.getHeaderFields().entrySet()) {
               if (var13.getKey() != null && ((String)var13.getKey()).equalsIgnoreCase("set-cookie") && var13.getValue() != null) {
                  var11.addAll((Collection)var13.getValue());
               }
            }

            if (!var11.isEmpty()) {
               StringBuilder var20 = new StringBuilder();

               for (String var14 : var11) {
                  String var15 = var14.split(";", 2)[0].trim();
                  if (var20.length() > 0) {
                     var20.append("; ");
                  }

                  var20.append(var15);
               }

               var10 = var20.toString();
            }

            System.out.println("[NetEaseAuth] 803 hit, Set-Cookie entries: " + var11.size() + ", MUSIC_U: " + (var10 != null && var10.contains("MUSIC_U=")));
         }

         var19 = new NetEaseAuth.QrPoll(var9, var10);
      } finally {
         var2.disconnect();
      }

      return var19;
   }

   public static String qrImageUrl(String var0) {
      return "https://music.163.com/login?keychain=1&key=" + urlEscape(var0);
   }

   public static String musicUFromCookie(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for (String var4 : var0.split(";")) {
            String var5 = var4.trim();
            if (var5.startsWith("MUSIC_U=") && var5.length() > 8) {
               return var5.substring(8);
            }
         }

         return null;
      }
   }

   public static String accountInfo(String var0) throws Exception {
      HashMap var1 = new HashMap();
      var1.put("MUSIC_U", var0);
      return post("/weapi/w/nuser/account/get", var1, "{}");
   }

   public static String dailyRecommend(String var0) throws Exception {
      HashMap var1 = new HashMap();
      var1.put("MUSIC_U", var0);
      return post("/weapi/v3/discovery/recommend/songs", var1, "{\"limit\":30,\"offset\":0,\"total\":true}");
   }

   public static String lyric(String var0) throws Exception {
      String var1 = "https://music.163.com/api/song/lyric?lv=-1&kv=-1&tv=-1&id=" + urlEscape(var0);
      return get(var1, new HashMap<>());
   }

   public static List<long[]> parseLrc(String var0) {
      List<long[]> var1 = new ArrayList<>();
      LRC_TEXTS.clear();
      if (var0 != null && !var0.isEmpty()) {
         String var2 = NetEaseAPI.cleanJsonString(var0);

         for (String var6 : var2.replace("\r", "").split("\n")) {
            String var7 = var6.trim();
            if (!var7.isEmpty() && var7.startsWith("[")) {
               List<Long> var8 = new ArrayList<>();
               int var9 = 0;

               while (var9 < var7.length() && var7.charAt(var9) == '[') {
                  int var10 = var7.indexOf(93, var9);
                  if (var10 < 0) {
                     break;
                  }

                  long var11 = parseLrcTag(var7.substring(1, var10));
                  if (var11 >= 0L) {
                     var8.add(var11);
                  }

                  var9 = var10 + 1;
               }

               if (!var8.isEmpty() && var9 < var7.length()) {
                  String var15 = var7.substring(var9).trim();
                  if (!var15.isEmpty()) {
                     int var16 = LRC_TEXTS.size();
                     LRC_TEXTS.add(var15);

                     for (long var13 : var8) {
                        var1.add(new long[]{var13, var16});
                     }
                  }
               }
            }
         }

         var1.sort((var0x, var1x) -> Long.compare(var0x[0], var1x[0]));
         return var1;
      } else {
         return var1;
      }
   }

   private static long parseLrcTag(String var0) {
      if (!var0.isEmpty() && !Character.isLetter(var0.charAt(0))) {
         try {
            String[] var1 = var0.split(":");
            if (var1.length < 2) {
               return -1L;
            } else {
               long var2 = Long.parseLong(var1[0].trim());
               double var4 = Double.parseDouble(var1[1].trim());
               return var2 * 60000L + (long)(var4 * 1000.0);
            }
         } catch (NumberFormatException var6) {
            return -1L;
         }
      } else {
         return -1L;
      }
   }

   public static String likedSongs(String var0, long var1) throws Exception {
      HashMap var3 = new HashMap();
      var3.put("MUSIC_U", var0);
      String var4 = "{\"id\":" + var1 + ",\"n\":1000,\"s\":8}";
      return post("/weapi/v6/playlist/detail", var3, var4);
   }

   public static List<SongInfo> parseSongs(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 != null && !var0.isEmpty()) {
         int var2 = 0;
         int var3 = -1;

         for (int var4 = 0; var4 < var0.length() && var1.size() < 100; var4++) {
            char var5 = var0.charAt(var4);
            if (var5 == '{') {
               if (var2 == 0) {
                  var3 = var4;
               }

               var2++;
            } else if (var5 == '}') {
               if (--var2 == 0 && var3 >= 0) {
                  String var6 = var0.substring(var3, var4 + 1);
                  String var7 = extractString(var6, "id");
                  String var8 = extractString(var6, "name");
                  if (var8 == null) {
                     var8 = extractString(var6, "title");
                  }

                  if (var7 != null && var8 != null && var7.matches("\\d+") && !var7.equals("0")) {
                     String var9 = extractString(var6, "artistName");
                     if (var9 == null) {
                        var9 = extractString(var6, "artist");
                     }

                     if (var9 == null) {
                        var9 = extractArFromT(var6);
                     }

                     if (var9 == null) {
                        var9 = "\u672a\u77e5\u6b4c\u624b";
                     }

                     long var10 = 0L;
                     String var12 = extractString(var6, "duration");
                     if (var12 != null) {
                        try {
                           var10 = Long.parseLong(var12);
                        } catch (NumberFormatException var14) {
                        }
                     }

                     var1.add(new SongInfo(var7, NetEaseAPI.cleanJsonString(var8), NetEaseAPI.cleanJsonString(var9), "", null, null, var10));
                  }

                  var3 = -1;
               }
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static String extractArFromT(String var0) {
      int var1 = var0.indexOf("\"ar\"");
      if (var1 < 0) {
         var1 = var0.indexOf("\"artists\"");
      }

      if (var1 < 0) {
         return null;
      } else {
         String var2 = var0.substring(var1);
         int var3 = var2.indexOf("\"name\"");
         return var3 < 0 ? null : extractString(var2.substring(var3), "name");
      }
   }

   public static String extractString(String var0, String var1) {
      int var2 = var0.indexOf("\"" + var1 + "\"");
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

               while (var5 < var0.length() && (Character.isDigit(var0.charAt(var5)) || var0.charAt(var5) == '.')) {
                  var5++;
               }

               return var5 > var4 ? var0.substring(var4, var5) : null;
            }
         }
      }
   }

   private static String urlEscape(String var0) {
      try {
         return URLEncoder.encode(var0, StandardCharsets.UTF_8);
      } catch (Exception var2) {
         return var0;
      }
   }

   public static class QrPoll {
      public final int state;
      public final String cookies;

      public QrPoll(int var1, String var2) {
         this.state = var1;
         this.cookies = var2;
      }
   }
}
