package dev.luminous.mod.gui;

import dev.luminous.api.utils.music.MusicPlayer;
import dev.luminous.api.utils.music.MusicStore;
import dev.luminous.api.utils.music.NetEaseAPI;
import dev.luminous.api.utils.music.NetEaseAuth;
import dev.luminous.api.utils.music.QrCode;
import dev.luminous.api.utils.music.SongInfo;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.gui.fonts.GlyphMap;
import dev.luminous.mod.modules.impl.misc.Music;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MusicScreen extends Screen {
   private static final int HEADER_H = 26;
   private static final int FOOTER_H = 34;
   private static final int ROW_H = 20;
   private final MinecraftClient client = MinecraftClient.getInstance();
   private TextFieldWidget searchField;
   private MusicScreen.View view = MusicScreen.View.SEARCH;
   private final List<SongInfo> rows = new ArrayList<>();
   private String status = "";
   private boolean loading;
   private int requestVersion;
   private int scroll;
   private int selectedRow = -1;
   private boolean qrVisible;
   private String qrKey;
   private int qrState;
   private long qrPollAt;
   private final Map<String, Identifier> qrTextures = new HashMap<>();
   private String accountName;
   private int px;
   private int py;
   private int pw;
   private int ph;
   private int listY;
   private int listH;
   private final List<int[]> tabBoxes = new ArrayList<>();
   private int accountBoxX;
   private int accountBoxW;
   private final Map<Integer, Integer> rowHeartBoxes = new HashMap<>();
   private int lastRowW;
   private int footerX;
   private int footerY;
   private int lyricsToggleX = -1;
   private int lyricsToggleY = -1;
   private boolean lyricsToggleOn;
   private int qrCloseX = -1;
   private int qrCloseY = -1;

   public MusicScreen() {
      super(Text.literal("Music Player"));
   }

   protected void init() {
      this.px = 18;
      this.py = 18;
      this.pw = Math.max(340, this.width - 36);
      this.ph = Math.max(200, this.height - 36);
      this.listY = this.py + 26 + 26;
      this.listH = this.ph - 26 - 26 - 34;
      this.searchField = new TextFieldWidget(this.textRenderer, this.px + 8, this.py + 26 + 5, this.pw - 108, 16, Text.literal("Search"));
      this.searchField.setMaxLength(80);
      this.addDrawableChild(this.searchField);
      if (MusicStore.getMusicU() != null) {
         this.accountName = MusicStore.getNickname();
      }

      if (this.rows.isEmpty()) {
         this.status = "\u8f93\u5165\u6b4c\u540d\u641c\u7d22\u5168\u7f51\u97f3\u4e50\uff0c\u6216\u767b\u5f55\u540e\u770b\u6bcf\u65e5\u63a8\u8350";
      }
   }

   private void search() {
      String var1 = this.searchField.getText().trim();
      if (!var1.isEmpty()) {
         this.switchView(MusicScreen.View.SEARCH, "\u641c\u7d22\u4e2d...");
         int var2 = ++this.requestVersion;
         Thread var3 = new Thread(() -> {
            try {
               List var3x = NetEaseAPI.search(var1, 30);
               this.client.execute(() -> this.acceptRows(var2, var3x, "\u6ca1\u6709\u641c\u5230\u7ed3\u679c"));
            } catch (Exception var4) {
               this.client.execute(() -> this.acceptError(var2, "\u641c\u7d22\u5931\u8d25: " + rootMessage(var4)));
            }
         }, "Luminous-Music-Search");
         var3.setDaemon(true);
         var3.start();
      }
   }

   private void loadDaily() {
      String var1 = MusicStore.getMusicU();
      if (var1 == null) {
         this.openQr();
         this.status = "\u6bcf\u65e5\u63a8\u8350\u9700\u8981\u767b\u5f55\uff0c\u8bf7\u626b\u7801";
      } else {
         this.switchView(MusicScreen.View.DAILY, "\u83b7\u53d6\u6bcf\u65e5\u63a8\u8350...");
         int var2 = ++this.requestVersion;
         Thread var3 = new Thread(() -> {
            try {
               String var3x = NetEaseAuth.dailyRecommend(var1);
               String var4 = NetEaseAuth.extractString(var3x, "code");
               List var5 = NetEaseAuth.parseSongs(var3x);
               if (var5.isEmpty() && var4 != null && !var4.equals("200")) {
                  this.client.execute(() -> {
                     if (var2 == this.requestVersion) {
                        this.loading = false;
                        this.status = "\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u70b9\u53f3\u4e0a\u89d2\u91cd\u65b0\u626b\u7801";
                     }
                  });
                  return;
               }

               this.client.execute(() -> this.acceptRows(var2, var5, "\u6bcf\u65e5\u63a8\u8350\u83b7\u53d6\u5931\u8d25"));
            } catch (Exception var6) {
               this.client.execute(() -> this.acceptError(var2, "\u6bcf\u65e5\u63a8\u8350\u5931\u8d25: " + rootMessage(var6)));
            }
         }, "Luminous-Music-Daily");
         var3.setDaemon(true);
         var3.start();
      }
   }

   private void loadFavourites() {
      this.switchView(MusicScreen.View.FAVOURITE, "");
      List var1 = MusicStore.favourites();
      this.rows.clear();
      this.rows.addAll(var1);
      this.scroll = 0;
      this.selectedRow = -1;
      this.loading = false;
      this.status = var1.isEmpty()
         ? "\u8fd8\u6ca1\u6709\u6536\u85cf\u7684\u6b4c\u66f2\uff0c\u64ad\u653e\u65f6\u70b9\u2661\u6536\u85cf"
         : "\u5171 " + var1.size() + " \u9996\u6536\u85cf";
      String var2 = MusicStore.getMusicU();
      long var3 = MusicStore.getUserId();
      if (var2 != null && var3 > 0L) {
         this.loading = true;
         if (this.status.startsWith("\u5171 ")) {
            this.status = "\u6b63\u5728\u540c\u6b65\u7f51\u6613\u4e91\u6536\u85cf...";
         }

         int var5 = ++this.requestVersion;
         Thread var6 = new Thread(
            () -> {
               try {
                  List<SongInfo> var5x = NetEaseAuth.parseSongs(NetEaseAuth.likedSongs(var2, var3));
                  this.client
                     .execute(
                        () -> {
                           if (var5 == this.requestVersion && this.view == MusicScreen.View.FAVOURITE) {
                              this.loading = false;

                              for (SongInfo var4x : var5x) {
                                 boolean var5xx = false;

                                 for (SongInfo var7 : this.rows) {
                                    if (var7.id.equals(var4x.id)) {
                                       var5xx = true;
                                       break;
                                    }
                                 }

                                 if (!var5xx) {
                                    this.rows.add(var4x);
                                 }
                              }

                              this.status = this.rows.isEmpty()
                                 ? "\u8fd8\u6ca1\u6709\u6536\u85cf\u7684\u6b4c\u66f2\uff0c\u64ad\u653e\u65f6\u70b9\u2661\u6536\u85cf"
                                 : "\u5171 " + this.rows.size() + " \u9996\uff08\u542b\u7f51\u6613\u4e91\u6536\u85cf\uff09";
                           }
                        }
                     );
               } catch (Exception var6x) {
                  this.client.execute(() -> {
                     if (var5 == this.requestVersion) {
                        this.loading = false;
                        if (!this.rows.isEmpty()) {
                           this.status = "\u7f51\u6613\u4e91\u6536\u85cf\u540c\u6b65\u5931\u8d25\uff0c\u5df2\u663e\u793a\u672c\u5730\u6536\u85cf";
                        }
                     }
                  });
               }
            },
            "Luminous-Music-Liked"
         );
         var6.setDaemon(true);
         var6.start();
      }
   }

   private void switchView(MusicScreen.View var1, String var2) {
      this.view = var1;
      this.rows.clear();
      this.scroll = 0;
      this.selectedRow = -1;
      this.loading = !var2.isEmpty();
      this.status = var2;
   }

   private void acceptRows(int var1, List<SongInfo> var2, String var3) {
      if (var1 == this.requestVersion) {
         this.loading = false;
         this.rows.clear();
         if (var2 != null) {
            this.rows.addAll(var2);
         }

         this.status = this.rows.isEmpty() ? var3 : "\u5171 " + this.rows.size() + " \u9996";
         this.scroll = 0;
      }
   }

   private void acceptError(int var1, String var2) {
      if (var1 == this.requestVersion) {
         this.loading = false;
         this.status = var2;
      }
   }

   private void playRow(int var1) {
      if (var1 >= 0 && var1 < this.rows.size()) {
         this.selectedRow = var1;
         Music var2 = Music.INSTANCE;
         if (var2 != null) {
            ArrayList var3 = new ArrayList<>(this.rows);
            SongInfo var4 = (SongInfo)var3.get(var1);
            if (!var4.isPlayable()) {
               this.status = "\u89e3\u6790\u64ad\u653e\u94fe\u63a5...";
               Thread var5 = new Thread(
                  () -> {
                     try {
                        String var5x = NetEaseAPI.getSongUrl(var4.id);
                        SongInfo var6 = var5x != null && !var5x.isEmpty()
                           ? new SongInfo(var4.id, var4.name, var4.artist, var4.album, var5x, var4.picUrl, var4.duration)
                           : var4.markAsVip();
                        var3.set(var1, var6);
                        this.client
                           .execute(
                              () -> {
                                 this.rows.set(var1, var6);
                                 var2.wirePlayer();
                                 MusicPlayer.getInstance().playQueue(new ArrayList<>(this.rows), var1);
                                 this.status = var6.vip
                                    ? "\u8be5\u6b4c\u9700\u8981 VIP / \u65e0\u7248\u6743\uff0c\u5df2\u8df3\u8fc7"
                                    : "\u6b63\u5728\u64ad\u653e: " + var6.getDisplayName();
                              }
                           );
                     } catch (Exception var7) {
                        this.client.execute(() -> this.status = "\u53d6\u94fe\u63a5\u5931\u8d25: " + rootMessage(var7));
                     }
                  },
                  "Luminous-Music-Resolve"
               );
               var5.setDaemon(true);
               var5.start();
            } else {
               var2.wirePlayer();
               MusicPlayer.getInstance().playQueue(var3, var1);
               this.status = "\u6b63\u5728\u64ad\u653e: " + var4.getDisplayName();
            }
         }
      }
   }

   private void openQr() {
      this.qrVisible = true;
      this.qrState = 801;
      this.qrPollAt = 0L;
      Thread var1 = new Thread(() -> {
         try {
            String var1x = NetEaseAuth.qrKey();
            this.qrKey = var1x;
            String var2 = "https://music.163.com/login?codekey=" + var1x;
            BufferedImage var3 = QrCode.encode(var2, 132);
            Identifier var4 = Identifier.of("luminous", "music_qr_login");
            GlyphMap.registerBufferedImageTexture(var4, var3);
            this.client.execute(() -> this.qrTextures.put("qr", var4));
         } catch (Exception var5) {
            this.client.execute(() -> this.status = "\u83b7\u53d6\u4e8c\u7ef4\u7801\u5931\u8d25: " + rootMessage(var5));
         }
      }, "Luminous-Music-QR");
      var1.setDaemon(true);
      var1.start();
   }

   private void pollQr() {
      if (this.qrVisible && this.qrKey != null && System.currentTimeMillis() >= this.qrPollAt) {
         this.qrPollAt = System.currentTimeMillis() + 2000L;
         String var1 = this.qrKey;
         Thread var2 = new Thread(
            () -> {
               try {
                  NetEaseAuth.QrPoll var2x = NetEaseAuth.qrStateWithCookies(var1);
                  int var3 = var2x.state;
                  if (var3 == 803 && var2x.cookies != null) {
                     String var4 = NetEaseAuth.musicUFromCookie(var2x.cookies);
                     if (var4 != null) {
                        String var5 = NetEaseAuth.accountInfo(var4);
                        String var6 = NetEaseAuth.extractString(var5, "nickname");
                        String var7 = NetEaseAuth.extractString(var5, "userId");
                        String var8 = var6 == null ? "\u7f51\u6613\u4e91\u7528\u6237" : var6;
                        long var9 = 0L;
                        if (var7 != null) {
                           try {
                              var9 = Long.parseLong(var7);
                           } catch (NumberFormatException var12) {
                           }
                        }

                        MusicStore.saveLogin(var4, var8, var9);
                        this.client.execute(() -> {
                           this.qrVisible = false;
                           this.qrKey = null;
                           this.qrTextures.clear();
                           this.accountName = var8;
                           this.status = "\u00a7a\u767b\u5f55\u6210\u529f: " + var8;
                           if (this.view == MusicScreen.View.DAILY) {
                              this.loadDaily();
                           } else if (this.view == MusicScreen.View.FAVOURITE) {
                              this.loadFavourites();
                           }
                        });
                        this.client
                           .execute(
                              () -> CommandManager.sendChatMessageWidthIdNoSync(
                                 "\u00a7f[\u00a7b\u97f3\u4e50\u64ad\u653e\u5668\u00a7f] \u00a7a\u767b\u5f55\u6210\u529f: \u00a7f" + var8, -1
                              )
                           );
                        return;
                     }
                  }

                  this.client.execute(() -> this.qrState = var3);
               } catch (Exception var13) {
               }
            },
            "Luminous-Music-QRPoll"
         );
         var2.setDaemon(true);
         var2.start();
      }
   }

   public void renderBackground(DrawContext var1, int var2, int var3, float var4) {
      var1.fill(0, 0, this.width, this.height, -1877995501);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -1307504365);
      context.fill(this.px, this.py, this.px + this.pw, this.py + this.ph, -166916849);
      this.drawTabs(context, mouseX, mouseY);
      this.drawSearchRow(context, mouseX, mouseY);
      this.drawRows(context, mouseX, mouseY);
      this.drawFooter(context, mouseX, mouseY);
      if (!this.status.isBlank()) {
         String var5 = this.loading ? "\u00a7b" : "\u00a77";
         context.drawTextWithShadow(this.textRenderer, this.truncate(var5 + this.status, this.pw - 16), this.px + 8, this.py + this.ph - 12, -1);
      }

      if (this.qrVisible) {
         this.drawQrDialog(context, mouseX, mouseY);
      }

      super.render(context, mouseX, mouseY, delta);
   }

   private void drawTabs(DrawContext var1, int var2, int var3) {
      int var4 = this.px + 8;
      this.tabBoxes.clear();
      var4 = this.drawTab(var1, var4, this.py + 7, "\u641c\u7d22", this.view == MusicScreen.View.SEARCH, var2, var3, MusicScreen.View.SEARCH);
      var4 = this.drawTab(var1, var4, this.py + 7, "\u6bcf\u65e5\u63a8\u8350", this.view == MusicScreen.View.DAILY, var2, var3, MusicScreen.View.DAILY);
      var4 = this.drawTab(
         var1, var4, this.py + 7, "\u6211\u559c\u6b22\u7684\u97f3\u4e50", this.view == MusicScreen.View.FAVOURITE, var2, var3, MusicScreen.View.FAVOURITE
      );
      String var5 = this.accountName != null ? "\u00a7a\u2713 " + this.accountName : "\u626b\u7801\u767b\u5f55";
      int var6 = this.textRenderer.getWidth(var5) + 10;
      int var7 = this.px + this.pw - var6 - 8;
      boolean var8 = inside(var2, var3, var7, this.py + 4, var6, 14);
      var1.fill(var7, this.py + 4, var7 + var6, this.py + 18, var8 ? -14013904 : -15000801);
      var1.drawTextWithShadow(this.textRenderer, var5, var7 + 5, this.py + 8, -1);
      this.accountBoxX = var7;
      this.accountBoxW = var6;
   }

   private int drawTab(DrawContext var1, int var2, int var3, String var4, boolean var5, int var6, int var7, MusicScreen.View var8) {
      int var9 = this.textRenderer.getWidth(var4) + 12;
      boolean var10 = inside(var6, var7, var2, var3 - 2, var9, 15);
      int var11 = var5 ? -13738870 : (var10 ? -14540248 : -15329766);
      var1.fill(var2, var3 - 2, var2 + var9, var3 + 13, var11);
      var1.drawTextWithShadow(this.textRenderer, var4, var2 + 6, var3 + 2, var5 ? -1 : -4137755);
      this.tabBoxes.add(new int[]{var2, var3 - 2, var9, 15, var8.ordinal()});
      return var2 + var9 + 4;
   }

   private void drawSearchRow(DrawContext var1, int var2, int var3) {
      int var4 = this.px + this.pw - 46;
      boolean var5 = inside(var2, var3, var4, this.py + 26 + 5, 38, 16);
      var1.fill(var4, this.py + 26 + 5, var4 + 38, this.py + 26 + 21, var5 ? -13738870 : -15000801);
      String var6 = "\u641c\u7d22";
      var1.drawTextWithShadow(this.textRenderer, var6, var4 + (38 - this.textRenderer.getWidth(var6)) / 2, this.py + 26 + 9, -1);
   }

   private void drawRows(DrawContext var1, int var2, int var3) {
      var1.fill(this.px + 4, this.listY - 2, this.px + this.pw - 4, this.listY + this.listH + 2, -15724525);
      if (!this.rows.isEmpty()) {
         int var4 = this.rows.size() * 20;
         this.scroll = clampScroll(this.scroll, var4, this.listH);
         int var5 = this.scroll / 20;
         int var6 = -(this.scroll % 20);
         int var7 = this.pw - 16;

         for (int var8 = var5; var8 < this.rows.size(); var8++) {
            int var9 = this.listY + var6 + (var8 - var5) * 20;
            if (var9 + 20 >= this.listY && var9 <= this.listY + this.listH) {
               SongInfo var10 = this.rows.get(var8);
               boolean var11 = inside(var2, var3, this.px + 6, var9, var7, 19);
               boolean var12 = var8 == this.selectedRow;
               if (var12) {
                  var1.fill(this.px + 6, var9, this.px + 6 + var7, var9 + 20 - 1, -14536641);
               } else if (var11) {
                  var1.fill(this.px + 6, var9, this.px + 6 + var7, var9 + 20 - 1, -15000800);
               }

               String var13 = var12 ? "\u00a7b\u25b6" : "\u00a77" + (var8 + 1);
               var1.drawTextWithShadow(this.textRenderer, var13, this.px + 10, var9 + 6, -1);
               var1.drawTextWithShadow(this.textRenderer, this.truncate(var10.name, var7 - 150), this.px + 34, var9 + 6, var11 ? -1 : -2209137);
               if (!var10.artist.isEmpty()) {
                  var1.drawTextWithShadow(this.textRenderer, this.truncate(var10.artist, 110), this.px + this.pw - 150, var9 + 6, -4137755);
               }

               if (var10.vip) {
                  var1.drawTextWithShadow(this.textRenderer, "VIP", this.px + this.pw - 40, var9 + 6, -3675862);
               }

               boolean var14 = MusicStore.isFavourite(var10.id);
               String var15 = var14 ? "\u00a7c\u2665" : "\u00a77\u2661";
               int var16 = this.px + this.pw - 62;
               var1.drawTextWithShadow(this.textRenderer, var15, var16, var9 + 6, -1);
               this.rowHeartBoxes.put(var8, var16);
            }
         }

         this.lastRowW = var7;
      }
   }

   private void drawFooter(DrawContext var1, int var2, int var3) {
      int var4 = this.py + this.ph - 34 - 6;
      MusicPlayer var5 = MusicPlayer.getInstance();
      SongInfo var6 = var5.getCurrentSong();
      String var7 = var6 == null ? "\u672a\u5728\u64ad\u653e" : var6.getDisplayName();
      var1.drawTextWithShadow(this.textRenderer, this.truncate("\u00a7b\u266a \u00a7f" + var7, this.pw - 240), this.px + 8, var4 + 6, -1);
      Music var8 = Music.INSTANCE;
      boolean var9 = var8 != null && var8.lyrics.getValue();
      String var10 = "\u8bcd";
      byte var11 = 22;
      int var12 = this.px + this.pw - 96 - 8 - var11;
      boolean var13 = inside(var2, var3, var12, var4, var11, 16);
      var1.fill(var12, var4, var12 + var11, var4 + 16, var9 ? -13738870 : (var13 ? -14540248 : -15000801));
      var1.drawTextWithShadow(this.textRenderer, var10, var12 + (var11 - this.textRenderer.getWidth(var10)) / 2, var4 + 4, var9 ? -1 : -4137755);
      this.lyricsToggleX = var12;
      this.lyricsToggleY = var4;
      this.lyricsToggleOn = var9;
      int var14 = this.px + this.pw - 96;
      this.drawControl(var1, var14, var4, 28, "\u23ee", var2, var3);
      this.drawControl(var1, var14 + 32, var4, 28, var5.isPlaying() && !var5.isPaused() ? "\u23f8" : "\u25b6", var2, var3);
      this.drawControl(var1, var14 + 64, var4, 28, "\u23ed", var2, var3);
      this.footerX = var14;
      this.footerY = var4;
   }

   private void drawControl(DrawContext var1, int var2, int var3, int var4, String var5, int var6, int var7) {
      boolean var8 = inside(var6, var7, var2, var3, var4, 16);
      var1.fill(var2, var3, var2 + var4, var3 + 16, var8 ? -13738870 : -15000801);
      var1.drawTextWithShadow(this.textRenderer, var5, var2 + (var4 - this.textRenderer.getWidth(var5)) / 2, var3 + 4, -1);
   }

   private void drawQrDialog(DrawContext var1, int var2, int var3) {
      var1.fill(0, 0, this.width, this.height, -1879048192);
      short var4 = 150;
      short var5 = 190;
      int var6 = (this.width - var4) / 2;
      int var7 = (this.height - var5) / 2;
      var1.fill(var6, var7, var6 + var4, var7 + var5, -166719469);
      var1.fill(var6, var7, var6 + var4, var7 + 16, -15000801);
      var1.drawTextWithShadow(this.textRenderer, "\u626b\u7801\u767b\u5f55", var6 + 8, var7 + 4, -1);
      var1.drawTextWithShadow(this.textRenderer, "\u00d7", var6 + var4 - 10, var7 + 3, -4137755);
      this.qrCloseX = var6 + var4 - 12;
      this.qrCloseY = var7;
      Identifier var8 = this.qrTextures.get("qr");
      if (var8 != null) {
         var1.drawTexture(var8, var6 + (var4 - 120) / 2, var7 + 22, 120, 0.0F, 0.0F, 132, 132, 132, 132);
      } else {
         var1.drawTextWithShadow(this.textRenderer, "\u751f\u6210\u4e8c\u7ef4\u7801\u4e2d...", var6 + 30, var7 + 70, -4137755);
      }
      String var9 = switch (this.qrState) {
         case 800 -> "\u00a7c\u4e8c\u7ef4\u7801\u5df2\u8fc7\u671f\uff0c\u70b9\u51fb\u5237\u65b0";
         default -> "\u00a77\u8bf7\u7528\u7f51\u6613\u4e91\u97f3\u4e50 APP \u626b\u7801";
         case 802 -> "\u00a7a\u5df2\u626b\u7801\uff0c\u8bf7\u5728\u624b\u673a\u786e\u8ba4";
         case 803 -> "\u00a7a\u767b\u5f55\u6210\u529f";
      };
      var1.drawTextWithShadow(this.textRenderer, var9, var6 + (var4 - this.textRenderer.getWidth(var9)) / 2, var7 + var5 - 40, -1);
      if (this.qrState == 800) {
         String var10 = "\u5237\u65b0";
         var1.fill(var6 + var4 / 2 - 25, var7 + var5 - 24, var6 + var4 / 2 + 25, var7 + var5 - 10, -13738870);
         var1.drawTextWithShadow(this.textRenderer, var10, var6 + var4 / 2 - this.textRenderer.getWidth(var10) / 2, var7 + var5 - 21, -1);
      }

      this.pollQr();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         int var6 = (int)mouseX;
         int var7 = (int)mouseY;
         if (this.qrVisible) {
            if (inside(var6, var7, this.qrCloseX, this.qrCloseY, 12, 12)) {
               this.qrVisible = false;
               return true;
            } else {
               short var15 = 150;
               short var17 = 190;
               int var19 = (this.width - var15) / 2;
               int var11 = (this.height - var17) / 2;
               if (this.qrState == 800 && inside(var6, var7, var19 + var15 / 2 - 25, var11 + var17 - 24, 50, 14)) {
                  this.openQr();
                  return true;
               } else {
                  return true;
               }
            }
         } else if (inside(var6, var7, this.accountBoxX, this.py + 4, this.accountBoxW, 14)) {
            if (MusicStore.getMusicU() != null) {
               MusicStore.clearLogin();
               this.accountName = null;
               this.status = "\u5df2\u9000\u51fa\u767b\u5f55";
            } else {
               this.openQr();
            }

            return true;
         } else {
            for (int[] var9 : this.tabBoxes) {
               if (inside(var6, var7, var9[0], var9[1], var9[2], var9[3])) {
                  MusicScreen.View var10 = MusicScreen.View.values()[var9[4]];
                  if (var10 != this.view) {
                     if (var10 == MusicScreen.View.SEARCH) {
                        this.switchView(MusicScreen.View.SEARCH, "\u8f93\u5165\u6b4c\u540d\u641c\u7d22");
                     } else if (var10 == MusicScreen.View.DAILY) {
                        this.loadDaily();
                     } else {
                        this.loadFavourites();
                     }
                  }

                  return true;
               }
            }

            if (inside(var6, var7, this.px + this.pw - 46, this.py + 26 + 5, 38, 16)) {
               this.search();
               return true;
            } else {
               if (inside(var6, var7, this.px + 6, this.listY, this.pw - 12, this.listH)) {
                  int var12 = this.rowAt(var7);
                  if (var12 >= 0 && var12 < this.rows.size()) {
                     Integer var16 = this.rowHeartBoxes.get(var12);
                     if (var16 != null && var6 >= var16 - 2 && var6 <= var16 + 10) {
                        SongInfo var18 = this.rows.get(var12);
                        MusicStore.toggleFavourite(var18);
                        this.status = MusicStore.isFavourite(var18.id) ? "\u5df2\u6536\u85cf: " + var18.name : "\u5df2\u53d6\u6d88\u6536\u85cf";
                        return true;
                     }

                     this.playRow(var12);
                     return true;
                  }
               }

               if (inside(var6, var7, this.lyricsToggleX, this.lyricsToggleY, 22, 16)) {
                  Music var14 = Music.INSTANCE;
                  if (var14 != null) {
                     var14.lyrics.setValue(!this.lyricsToggleOn);
                     this.status = var14.lyrics.getValue() ? "\u6b4c\u8bcd\u5df2\u5f00\u542f" : "\u6b4c\u8bcd\u5df2\u5173\u95ed";
                  }

                  return true;
               } else if (!inside(var6, var7, this.footerX, this.footerY, 96, 16)) {
                  return super.mouseClicked(mouseX, mouseY, button);
               } else {
                  MusicPlayer var13 = MusicPlayer.getInstance();
                  if (inside(var6, var7, this.footerX, this.footerY, 28, 16)) {
                     var13.playPrevious();
                     this.status = "\u4e0a\u4e00\u9996";
                  } else if (inside(var6, var7, this.footerX + 32, this.footerY, 28, 16)) {
                     if (var13.isPlaying() && !var13.isPaused()) {
                        var13.pause();
                        this.status = "\u5df2\u6682\u505c";
                     } else {
                        var13.resume();
                        this.status = "\u7ee7\u7eed\u64ad\u653e";
                     }
                  } else {
                     var13.playNext();
                     this.status = "\u4e0b\u4e00\u9996";
                  }

                  return true;
               }
            }
         }
      }
   }

   private int rowAt(int var1) {
      int var2 = this.scroll + (var1 - this.listY);
      return var2 < 0 ? -1 : var2 / 20;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (inside(mouseX, mouseY, this.px + 4, this.listY - 2, this.pw - 8, this.listH + 4)) {
         int var9 = (int)(-verticalAmount * 24.0);
         this.scroll = clampScroll(this.scroll + var9, this.rows.size() * 20, this.listH);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         if (this.qrVisible) {
            this.qrVisible = false;
            return true;
         } else {
            this.close();
            return true;
         }
      } else if ((keyCode == 257 || keyCode == 335) && this.searchField.isFocused()) {
         this.search();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      super.close();
   }

   private String truncate(String var1, int var2) {
      if (this.textRenderer.getWidth(var1) <= var2) {
         return var1;
      } else {
         String var3 = "...";
         int var4 = var1.length();

         while (var4 > 0 && this.textRenderer.getWidth(var1.substring(0, var4) + var3) > var2) {
            var4--;
         }

         return var1.substring(0, var4) + var3;
      }
   }

   private static boolean inside(double var0, double var2, int var4, int var5, int var6, int var7) {
      return var6 > 0 && var0 >= var4 && var0 < var4 + var6 && var2 >= var5 && var2 < var5 + var7;
   }

   private static int clampScroll(int var0, int var1, int var2) {
      return Math.max(0, Math.min(var0, Math.max(0, var1 - var2)));
   }

   private static String rootMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null) {
         var1 = var1.getCause();
      }

      return var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getMessage();
   }

   private static enum View {
      SEARCH,
      DAILY,
      FAVOURITE;
   }
}
