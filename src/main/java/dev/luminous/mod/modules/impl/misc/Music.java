package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.music.MusicPlayer;
import dev.luminous.api.utils.music.NetEaseAPI;
import dev.luminous.api.utils.music.SongInfo;
import dev.luminous.mod.gui.MusicScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class Music extends Module {
   public static Music INSTANCE;
   public final SliderSetting volume = this.add(new SliderSetting("Volume", 0.8, 0.0, 1.0, 0.05));
   public final BooleanSetting autoNext = this.add(new BooleanSetting("AutoNext", true));
   public final BooleanSetting lyrics = this.add(new BooleanSetting("Lyrics", true));
   private boolean syncingVolume;
   private boolean pendingOpen;

   public Music() {
      super("Music", "\u97f3\u4e50\u64ad\u653e\u5668", Module.Category.Client);
      this.setChinese("\u97f3\u4e50\u64ad\u653e\u5668");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.pendingOpen = true;
      this.wirePlayer(MusicPlayer.getInstance());
      this.applyAutoNext();
      this.applyVolume();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (this.pendingOpen) {
         this.pendingOpen = false;
         if (!nullCheck()) {
            mc.setScreen(new MusicScreen());
         }
      }

      this.applyVolume();
      this.applyAutoNext();
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (this.lyrics.getValue()) {
         LyricsHUD.render(var1);
      }
   }

   @Override
   public void onDisable() {
   }

   @Override
   public String getInfo() {
      MusicPlayer var1 = MusicPlayer.getInstance();
      if (var1.isPlaying()) {
         SongInfo var2 = var1.getCurrentSong();
         return var2 != null ? var2.getDisplayName() : "Playing...";
      } else {
         return var1.getCurrentSong() != null ? "Paused" : "Idle";
      }
   }

   public void applyVolume() {
      MusicPlayer var1 = MusicPlayer.getInstance();
      if (!this.syncingVolume) {
         this.syncingVolume = true;

         try {
            var1.setVolume((float)this.volume.getValue());
         } finally {
            this.syncingVolume = false;
         }
      }
   }

   private void applyAutoNext() {
      MusicPlayer.getInstance().setAutoPlayNext(this.autoNext.getValue());
   }

   public void wirePlayer(MusicPlayer var1) {
      var1.setUrlResolver(NetEaseAPI::getSongUrl);
      var1.setOnSongSkipped(var1x -> this.sendMessage("\u00a77[music]\u00a7f \u8df3\u8fc7 (VIP/\u65e0\u7248\u6743): " + var1x.getDisplayName()));
      var1.setOnSongFinished(var2 -> {
         if (this.autoNext.getValue()) {
            List var3 = var1.getPlayQueue();
            int var4 = var1.getQueueIndex() + 1;
            if (var4 >= 0 && var4 < var3.size()) {
               this.sendMessage("\u00a7b[music]\u00a7f \u4e0b\u4e00\u9996: " + ((SongInfo)var3.get(var4)).getDisplayName());
            }
         }
      });
      var1.setOnQueueExhausted(() -> this.sendMessage("\u00a77[music]\u00a7f \u64ad\u653e\u961f\u5217\u64ad\u5b8c\u4e86\u3002"));
   }

   public void wirePlayer() {
      this.wirePlayer(MusicPlayer.getInstance());
   }

   public void onCommand(String[] var1) {
      MusicPlayer var2 = MusicPlayer.getInstance();
      String var3 = var1 != null && var1.length > 0 ? var1[0].toLowerCase() : "";
      switch (var3) {
         case "stop":
            var2.stopAll();
            this.sendMessage("\u00a7b[music]\u00a7f \u5df2\u505c\u6b62\u64ad\u653e\u3002");
            break;
         case "pause":
            var2.pause();
            this.sendMessage("\u00a7b[music]\u00a7f \u5df2\u6682\u505c\u3002");
            break;
         case "resume":
            var2.resume();
            this.sendMessage("\u00a7b[music]\u00a7f \u7ee7\u7eed\u64ad\u653e\u3002");
            break;
         case "next":
            var2.playNext();
            this.sendMessage("\u00a7b[music]\u00a7f \u4e0b\u4e00\u9996\u3002");
            break;
         case "prev":
            var2.playPrevious();
            this.sendMessage("\u00a7b[music]\u00a7f \u4e0a\u4e00\u9996\u3002");
            break;
         case "now":
            SongInfo var8 = var2.getCurrentSong();
            this.sendMessage(
               var8 != null
                  ? "\u00a7b[music]\u00a7f " + var8.getDisplayName()
                  : "\u00a7b[music]\u00a7f \u5f53\u524d\u6ca1\u6709\u64ad\u653e\u4e2d\u7684\u6b4c\u66f2\u3002"
            );
            break;
         case "queue":
            this.showQueue(var2);
            break;
         case "top":
            this.sendMessage("\u00a7b[music]\u00a7f \u83b7\u53d6\u70ed\u6b4c\u699c...");
            this.async(() -> {
               try {
                  List var2x = NetEaseAPI.getPlaylist("hot");
                  this.startPlaying(var2, var2x, "\u70ed\u6b4c\u699c");
               } catch (Exception var3x) {
                  throw new RuntimeException(var3x);
               }
            });
            break;
         case "ping":
            this.sendMessage("\u00a7b[music]\u00a7f \u6d4b\u8bd5 API \u8fde\u901a\u6027...");
            this.async(
               () -> {
                  try {
                     boolean var1x = NetEaseAPI.testConnection();
                     this.sendMessage(
                        var1x
                           ? "\u00a7a[music]\u00a7f API \u8fde\u901a\u6b63\u5e38\u3002"
                           : "\u00a7c[music]\u00a7f API \u4e0d\u53ef\u7528\uff0c\u7a0d\u540e\u518d\u8bd5\u3002"
                     );
                  } catch (Exception var2x) {
                     throw new RuntimeException(var2x);
                  }
               }
            );
            break;
         case "search":
            if (var1.length < 2) {
               this.printUsage();
               return;
            }

            StringBuilder var6 = new StringBuilder();

            for (int var7 = 1; var7 < var1.length; var7++) {
               if (var7 > 1) {
                  var6.append(' ');
               }

               var6.append(var1[var7]);
            }

            String var9 = var6.toString();
            this.sendMessage("\u00a7b[music]\u00a7f \u641c\u7d22\u4e2d: " + var9 + " ...");
            this.async(() -> {
               try {
                  List var3x = NetEaseAPI.search(var9, 10);
                  this.startPlaying(var2, var3x, var9);
               } catch (Exception var4) {
                  throw new RuntimeException(var4);
               }
            });
      }
   }

   private void showQueue(MusicPlayer var1) {
      List var2 = var1.getPlayQueue();
      if (var2.isEmpty()) {
         this.sendMessage("\u00a7b[music]\u00a7f \u961f\u5217\u662f\u7a7a\u7684\u3002");
      } else {
         int var3 = var1.getQueueIndex();
         int var4 = Math.min(var2.size(), 10);
         this.sendMessage("\u00a7b[music]\u00a7f \u961f\u5217 " + (var3 + 1) + "/" + var2.size() + ":");

         for (int var5 = var3 < 0 ? 0 : var3; var5 < var4; var5++) {
            String var6 = var5 == var3 ? "\u00a7a\u25b6 " : "\u00a77  ";
            this.sendMessage(var6 + (var5 + 1) + ". " + ((SongInfo)var2.get(var5)).getDisplayName() + (((SongInfo)var2.get(var5)).vip ? " \u00a78[VIP]" : ""));
         }

         if (var2.size() > var4) {
            this.sendMessage("\u00a77  ... \u8fd8\u6709 " + (var2.size() - var4) + " \u9996");
         }
      }
   }

   private void startPlaying(MusicPlayer var1, List<SongInfo> var2, String var3) {
      if (var2 != null && !var2.isEmpty()) {
         this.wirePlayer(var1);
         SongInfo var4 = (SongInfo)var2.get(0);
         if (!var4.isPlayable()) {
            String var5 = NetEaseAPI.getSongUrl(var4.id);
            if (var5 != null && !var5.isEmpty()) {
               var4 = new SongInfo(var4.id, var4.name, var4.artist, var4.album, var5, var4.picUrl, var4.duration);
               var2.set(0, var4);
            } else {
               var2.set(0, var4.markAsVip());
            }
         }

         var1.playQueue(var2, 0);
         this.sendMessage("\u00a7b[music]\u00a7f \u6b63\u5728\u64ad\u653e: " + var4.getDisplayName() + "\u00a77 (" + var3 + ", " + var2.size() + "\u9996)");
      } else {
         this.sendMessage("\u00a7c[music]\u00a7f \u6ca1\u6709\u641c\u5230\u7ed3\u679c\u3002");
      }
   }

   private void async(Runnable var1) {
      Thread var2 = new Thread(() -> {
         try {
            var1.run();
         } catch (Exception var3) {
            this.sendMessage("\u00a7c[music]\u00a7f \u8bf7\u6c42\u5931\u8d25: " + var3.getMessage());
         }
      }, "Luminous-Music-Net");
      var2.setDaemon(true);
      var2.start();
   }

   private void printUsage() {
      this.sendMessage(
         "\u00a7b[music]\u00a7f \u7528\u6cd5: .music search <\u5173\u952e\u5b57> | top | queue | now | next | prev | pause | resume | stop | ping"
      );
   }
}
