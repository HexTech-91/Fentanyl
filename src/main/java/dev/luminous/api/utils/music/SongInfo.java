package dev.luminous.api.utils.music;

public class SongInfo {
   public final String id;
   public final String name;
   public final String artist;
   public final String album;
   public final String url;
   public final String picUrl;
   public final long duration;
   public final boolean vip;

   public SongInfo(String var1, String var2, String var3, String var4, String var5, String var6, long var7) {
      this(var1, var2, var3, var4, var5, var6, var7, false);
   }

   public SongInfo(String var1, String var2, String var3, String var4, String var5, String var6, long var7, boolean var9) {
      this.id = var1;
      this.name = var2;
      this.artist = var3;
      this.album = var4;
      this.url = var5;
      this.picUrl = var6;
      this.duration = var7;
      this.vip = var9;
   }

   public SongInfo markAsVip() {
      return this.vip ? this : new SongInfo(this.id, this.name, this.artist, this.album, this.url, this.picUrl, this.duration, true);
   }

   public boolean isPlayable() {
      return !this.vip && this.url != null && !this.url.isEmpty();
   }

   public String getDisplayName() {
      return this.name + " - " + this.artist;
   }

   @Override
   public String toString() {
      return this.name + " - " + this.artist + " [" + this.album + "]" + (this.vip ? " [VIP]" : "");
   }
}
