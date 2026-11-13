package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.music.MusicPlayer;
import dev.luminous.api.utils.music.NetEaseAuth;
import dev.luminous.api.utils.music.SongInfo;
import dev.luminous.api.utils.render.TextUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.DrawContext;

public final class LyricsHUD implements Wrapper {
   private static final long CACHE_TTL = 600000L;
   private static final float BASE_OFFSET = 76.0F;
   private static final float LINE_GAP = 24.0F;
   private static final long FADE_MS = 900L;
   private static final float FADE_RISE = 10.0F;
   private static final long MOVE_MS = 280L;
   private static final Map<String, LyricsHUD.CachedLrc> CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Long> FAILED = new ConcurrentHashMap<>();
   private static String animSongId;
   private static int currIndex = -1;
   private static int fadingIndex = -1;
   private static long fadingStartAt;
   private static long currStartAt;

   private LyricsHUD() {
   }

   public static void render(DrawContext var0) {
      MusicPlayer var1 = MusicPlayer.getInstance();
      SongInfo var2 = var1.getCurrentSong();
      if (var2 == null || var2.id == null) {
         resetAnim();
      } else if (var1.isPlaying() && !var1.isPaused()) {
         double var3 = var1.getPlaybackSeconds();
         if (!(var3 < 0.0)) {
            List var5 = timelineFor(var2.id);
            if (var5 != null && !var5.isEmpty()) {
               int var6 = -1;
               long var7 = (long)(var3 * 1000.0);

               for (int var9 = 0; var9 < var5.size() && ((long[])var5.get(var9))[0] <= var7; var9++) {
                  var6 = (int)((long[])var5.get(var9))[1];
               }

               long var24 = System.currentTimeMillis();
               if (!var2.id.equals(animSongId)) {
                  animSongId = var2.id;
                  currIndex = -1;
                  fadingIndex = -1;
               }

               if (var6 != currIndex) {
                  if (currIndex >= 0) {
                     fadingIndex = currIndex;
                     fadingStartAt = var24;
                  }

                  currIndex = var6;
                  currStartAt = var24;
               }

               if (currIndex >= 0) {
                  String var11 = NetEaseAuth.LRC_TEXTS.get(currIndex);
                  float var12 = TextUtil.getHeight();
                  int var13 = mc.getWindow().getFramebufferWidth();
                  int var14 = mc.getWindow().getScaledHeight();
                  float var15 = Math.min(1.0F, (float)(var24 - currStartAt) / 280.0F);
                  float var16 = 1.0F - (1.0F - var15) * (1.0F - var15);
                  float var17 = var14 - 76.0F - var12 + 24.0F * (1.0F - var16);
                  drawLine(var0, var11, var13, var17, 1.0F);
                  int var18 = -1;

                  for (int var19 = 0; var19 < var5.size(); var19++) {
                     if ((int)((long[])var5.get(var19))[1] == currIndex && var19 + 1 < var5.size()) {
                        var18 = var19 + 1;
                        break;
                     }
                  }

                  if (var18 >= 0) {
                     String var25 = NetEaseAuth.LRC_TEXTS.get((int)((long[])var5.get(var18))[1]);
                     float var20 = var14 - 76.0F - var12 + 24.0F;
                     drawLine(var0, var25, var13, var20, 0.45F);
                  }

                  if (fadingIndex >= 0) {
                     long var26 = var24 - fadingStartAt;
                     if (var26 < 900L) {
                        String var21 = NetEaseAuth.LRC_TEXTS.get(fadingIndex);
                        float var22 = (float)var26 / 900.0F;
                        float var23 = var14 - 76.0F - var12 - 10.0F * var22;
                        drawLine(var0, var21, var13, var23, 1.0F - var22);
                     } else {
                        fadingIndex = -1;
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawLine(DrawContext var0, String var1, int var2, float var3, float var4) {
      if (var1 != null && !var1.isEmpty() && !(var4 <= 0.01F)) {
         float var5 = TextUtil.getWidth(var1);
         float var6 = TextUtil.getHeight();
         float var7 = (var2 - var5) / 2.0F;
         byte var8 = 4;
         int var9 = (int)(64.0F * var4);
         if (var9 > 0) {
            var0.fill((int)var7 - var8, (int)var3 - var8, (int)(var7 + var5) + var8, (int)(var3 + var6) + var8, var9);
         }

         int var10 = (int)(255.0F * var4);
         int var11 = var10 << 24 | 16777215;
         TextUtil.drawString(var0, var1, var7, var3, var11, true, false);
      }
   }

   private static void resetAnim() {
      animSongId = null;
      currIndex = -1;
      fadingIndex = -1;
   }

   private static List<long[]> timelineFor(String var0) {
      LyricsHUD.CachedLrc var1 = CACHE.get(var0);
      long var2 = System.currentTimeMillis();
      if (var1 != null && var2 - var1.fetchedAt() < 600000L) {
         return var1.timeline();
      } else {
         if (var1 != null) {
            CACHE.remove(var0);
         }

         Long var4 = FAILED.get(var0);
         if (var4 != null) {
            if (var2 - var4 < 600000L) {
               return null;
            }

            FAILED.remove(var0);
         }

         CACHE.put(var0, new LyricsHUD.CachedLrc(List.of(), var2));
         Thread var5 = new Thread(() -> {
            try {
               String var1x = NetEaseAuth.lyric(var0);
               int var2x = var1x.indexOf("\"lrc\"");
               if (var2x < 0) {
                  FAILED.put(var0, System.currentTimeMillis());
                  return;
               }

               String var3 = NetEaseAuth.extractString(var1x.substring(var2x), "lyric");
               if (var3 == null || var3.isBlank()) {
                  FAILED.put(var0, System.currentTimeMillis());
                  return;
               }

               List var4x = NetEaseAuth.parseLrc(var3);
               CACHE.put(var0, new LyricsHUD.CachedLrc(var4x, System.currentTimeMillis()));
               if (var4x.isEmpty()) {
                  FAILED.put(var0, System.currentTimeMillis());
               }
            } catch (Exception var5x) {
               FAILED.put(var0, System.currentTimeMillis());
            }
         }, "Luminous-Lyric-Fetch");
         var5.setDaemon(true);
         var5.start();
         return null;
      }
   }

   private record CachedLrc(List<long[]> timeline, long fetchedAt) {
   }
}
