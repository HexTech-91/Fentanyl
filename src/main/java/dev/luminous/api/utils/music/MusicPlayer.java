package dev.luminous.api.utils.music;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.lwjgl.openal.AL10;

public class MusicPlayer {
   private static MusicPlayer INSTANCE;
   private final ExecutorService playbackExecutor = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "Luminous-MusicPlayer");
      var1.setDaemon(true);
      return var1;
   });
   private final AtomicInteger playGeneration = new AtomicInteger(0);
   private int source = -1;
   private final AtomicBoolean playing = new AtomicBoolean(false);
   private final AtomicBoolean paused = new AtomicBoolean(false);
   private final AtomicBoolean stopped = new AtomicBoolean(false);
   private volatile SongInfo currentSong;
   private final List<SongInfo> playQueue = new CopyOnWriteArrayList<>();
   private volatile int queueIndex = -1;
   private volatile boolean autoPlayNext = true;
   private Consumer<SongInfo> onSongFinished;
   private Consumer<SongInfo> onSongSkipped;
   private Runnable onQueueExhausted;
   private Function<String, String> urlResolver;
   private static final int BUFFER_COUNT = 16;
   private volatile long songStartedAtMillis = 0L;
   private volatile long pausedAtMillis = 0L;

   private MusicPlayer() {
   }

   public double getPlaybackSeconds() {
      if (this.songStartedAtMillis <= 0L) {
         return -1.0;
      } else {
         long var1 = this.playing.get() && !this.paused.get() ? System.currentTimeMillis() : this.pausedAtMillis;
         if (var1 <= 0L) {
            var1 = System.currentTimeMillis();
         }

         return Math.max(0.0, (var1 - this.songStartedAtMillis) / 1000.0 - 0.4);
      }
   }

   public static synchronized MusicPlayer getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new MusicPlayer();
      }

      return INSTANCE;
   }

   private boolean isSourceValid() {
      try {
         return this.source >= 0 && AL10.alIsSource(this.source);
      } catch (Exception var2) {
         return false;
      }
   }

   private void ensureSource() {
      if (!this.isSourceValid()) {
         this.source = AL10.alGenSources();
         if (this.source > 0) {
            AL10.alSourcei(this.source, 514, 1);
            AL10.alSource3f(this.source, 4100, 0.0F, 0.0F, 0.0F);
            AL10.alSourcef(this.source, 4106, 0.8F);
         }
      }
   }

   private void stopSource() {
      this.playing.set(false);
      this.paused.set(false);
      this.songStartedAtMillis = 0L;
      this.pausedAtMillis = 0L;
      if (this.isSourceValid()) {
         try {
            AL10.alSourceStop(this.source);
            int var1 = AL10.alGetSourcei(this.source, 4117);

            for (int var2 = 0; var2 < var1; var2++) {
               AL10.alSourceUnqueueBuffers(this.source);
            }
         } catch (Exception var3) {
         }
      }
   }

   public CompletableFuture<Void> playQueue(List<SongInfo> var1, int var2) {
      this.playGeneration.incrementAndGet();
      this.stopped.set(false);
      this.playQueue.clear();
      this.playQueue.addAll(var1);
      this.queueIndex = Math.max(0, Math.min(var2, var1.size() - 1));
      return this.schedulePlayCurrent();
   }

   public CompletableFuture<Void> play(SongInfo var1) {
      this.playGeneration.incrementAndGet();
      this.stopped.set(false);
      this.playQueue.clear();
      this.playQueue.add(var1);
      this.queueIndex = 0;
      this.currentSong = var1;
      return this.schedulePlayCurrent();
   }

   public CompletableFuture<Void> playNext() {
      if (this.queueIndex + 1 < this.playQueue.size()) {
         this.playGeneration.incrementAndGet();
         this.stopped.set(false);
         this.queueIndex++;
         return this.schedulePlayCurrent();
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   public CompletableFuture<Void> playPrevious() {
      if (this.queueIndex > 0) {
         this.playGeneration.incrementAndGet();
         this.stopped.set(false);
         this.queueIndex--;
         return this.schedulePlayCurrent();
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   private CompletableFuture<Void> schedulePlayCurrent() {
      int var1 = this.playGeneration.get();

      try {
         return CompletableFuture.runAsync(() -> {
            if (var1 == this.playGeneration.get()) {
               this.playCurrentInQueue(var1);
            }
         }, this.playbackExecutor);
      } catch (RejectedExecutionException var3) {
         return CompletableFuture.completedFuture(null);
      }
   }

   private void playCurrentInQueue(int var1) {
      while (true) {
         label119: {
            if (this.queueIndex >= 0 && this.queueIndex < this.playQueue.size()) {
               if (var1 != this.playGeneration.get()) {
                  return;
               }

               SongInfo var2 = this.playQueue.get(this.queueIndex);
               if (!var2.isPlayable()) {
                  if (var2.vip) {
                     System.out.println("[MusicPlayer] Skipping VIP: " + var2.getDisplayName());
                     if (this.onSongSkipped != null) {
                        try {
                           this.onSongSkipped.accept(var2);
                        } catch (Exception var6) {
                        }
                     }
                     break label119;
                  }

                  System.out.println("[MusicPlayer] Resolving URL for: " + var2.getDisplayName());
                  String var3 = this.resolveSongUrl(var2);
                  if (var3 == null || var3.isEmpty()) {
                     this.playQueue.set(this.queueIndex, var2.markAsVip());
                     System.out.println("[MusicPlayer] Skipping unresolvable: " + var2.getDisplayName());
                     if (this.onSongSkipped != null) {
                        try {
                           this.onSongSkipped.accept(var2);
                        } catch (Exception var5) {
                        }
                     }
                     break label119;
                  }

                  SongInfo var4 = new SongInfo(var2.id, var2.name, var2.artist, var2.album, var3, var2.picUrl, var2.duration);
                  this.playQueue.set(this.queueIndex, var4);
               }
            }

            if (this.queueIndex >= 0 && this.queueIndex < this.playQueue.size()) {
               SongInfo var11 = this.playQueue.get(this.queueIndex);
               if (!var11.isPlayable()) {
                  this.currentSong = null;
                  return;
               }

               this.stopSource();
               this.currentSong = var11;

               try {
                  this.playInternal(var11, var1);
               } catch (Exception var10) {
                  System.err.println("[MusicPlayer] Error playing " + var11.getDisplayName() + ": " + var10.getMessage());
                  this.playQueue.set(this.queueIndex, var11.markAsVip());
                  if (this.onSongSkipped != null) {
                     try {
                        this.onSongSkipped.accept(var11);
                     } catch (Exception var9) {
                     }
                  }
               }

               if (var1 != this.playGeneration.get()) {
                  return;
               }

               if (this.stopped.get()) {
                  return;
               }

               this.playing.set(false);
               this.paused.set(false);
               this.pausedAtMillis = 0L;
               if (this.onSongFinished != null) {
                  try {
                     this.onSongFinished.accept(var11);
                  } catch (Exception var8) {
                  }
               }

               if (this.autoPlayNext && this.queueIndex + 1 < this.playQueue.size()) {
                  this.queueIndex++;
                  this.playCurrentInQueue(var1);
               } else if (this.autoPlayNext && this.onQueueExhausted != null) {
                  try {
                     this.onQueueExhausted.run();
                  } catch (Exception var7) {
                  }
               } else if (!this.autoPlayNext && var1 == this.playGeneration.get() && !this.stopped.get()) {
                  System.out.println("[MusicPlayer] AutoNext off \u2014 repeating: " + var11.getDisplayName());
                  this.playCurrentInQueue(var1);
               }

               return;
            }

            this.currentSong = null;
            return;
         }

         if (this.queueIndex + 1 >= this.playQueue.size()) {
            this.currentSong = null;
            return;
         }

         this.queueIndex++;
      }
   }

   private String resolveSongUrl(SongInfo var1) {
      if (this.urlResolver != null && var1.id != null && !var1.id.equals("0")) {
         try {
            return this.urlResolver.apply(var1.id);
         } catch (Exception var3) {
            System.err.println("[MusicPlayer] URL resolution failed for " + var1.getDisplayName() + ": " + var3.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private void playInternal(SongInfo var1, int var2) throws Exception {
      this.ensureSource();
      String var3 = var1.url;
      if (var3 != null && !var3.isEmpty()) {
         System.out.println("[MusicPlayer] Playing: " + var1.getDisplayName());
         URL var4 = URI.create(var3).toURL();
         HttpURLConnection var5 = (HttpURLConnection)var4.openConnection();
         var5.setRequestMethod("GET");
         var5.setConnectTimeout(8000);
         var5.setReadTimeout(15000);
         var5.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
         var5.setInstanceFollowRedirects(true);
         InputStream var6 = null;

         try {
            int var7 = var5.getResponseCode();
            if (var7 != 200) {
               throw new RuntimeException("HTTP " + var7 + " for: " + var1.getDisplayName());
            }

            var6 = var5.getInputStream();
            this.streamMP3(var6, var2);
         } finally {
            this.playing.set(false);
            if (var6 != null) {
               try {
                  var6.close();
               } catch (Exception var13) {
               }
            }

            var5.disconnect();
         }
      } else {
         throw new RuntimeException("No URL for: " + var1.getDisplayName());
      }
   }

   private void streamMP3(InputStream var1, int var2) throws Exception {
      BufferedInputStream var3 = new BufferedInputStream(var1, 131072);
      Bitstream var4 = new Bitstream(var3);
      Decoder var5 = new Decoder();
      int[] var6 = new int[16];

      for (int var7 = 0; var7 < 16; var7++) {
         var6[var7] = AL10.alGenBuffers();
      }

      int var42 = -1;
      int var8 = -1;
      boolean var9 = true;
      int var10 = 0;
      int var11 = 0;
      int var12 = 0;
      int var13 = 0;
      this.playing.set(true);
      this.paused.set(false);

      while (this.playing.get() && !this.stopped.get()) {
         if (var2 != this.playGeneration.get()) {
            System.out.println("[MusicPlayer] Superseded by newer play request, stopping");
            break;
         }

         if (this.paused.get()) {
            try {
               Thread.sleep(50L);
            } catch (InterruptedException var37) {
               break;
            }
         } else {
            if (!this.isSourceValid()) {
               System.out.println("[MusicPlayer] Source invalidated, attempting recovery...");
               this.source = -1;
               this.ensureSource();
               if (!this.isSourceValid()) {
                  if (++var12 > 50) {
                     System.out.println("[MusicPlayer] Context recovery failed after 50 attempts, stopping");
                     break;
                  }

                  try {
                     Thread.sleep(100L);
                     continue;
                  } catch (InterruptedException var38) {
                     break;
                  }
               }

               System.out.println("[MusicPlayer] Source recovered after context change");
               var11 = 0;
               var10 = 0;
               boolean var14 = false;

               for (int var15 = 0; var15 < 16; var15++) {
                  var6[var15] = AL10.alGenBuffers();
               }
            }

            var12 = 0;

            Header var45;
            try {
               var45 = var4.readFrame();
            } catch (Exception var39) {
               break;
            }

            if (var45 == null) {
               break;
            }

            try {
               SampleBuffer var47 = (SampleBuffer)var5.decodeFrame(var45, var4);
               if (var9) {
                  var42 = var47.getChannelCount();
                  var8 = var47.getSampleFrequency();
                  if (var42 < 1 || var42 > 2) {
                     var42 = 2;
                  }

                  if (var8 < 8000) {
                     var8 = 44100;
                  }

                  var9 = false;
                  System.out.println("[MusicPlayer] MP3: " + var42 + "ch, " + var8 + "Hz, 16 buffers");
               }

               short[] var16 = var47.getBuffer();
               int var17 = var47.getBufferLength();
               if (var17 > 0) {
                  ByteBuffer var18 = ByteBuffer.allocateDirect(var17 * 2);
                  var18.order(ByteOrder.nativeOrder());

                  for (int var19 = 0; var19 < var17; var19++) {
                     var18.putShort(var16[var19]);
                  }

                  var18.flip();
                  int var50 = var47.getChannelCount();
                  int var20 = var50 == 2 ? 4355 : 4353;
                  AL10.alBufferData(var6[var10], var20, var18, var8);
                  AL10.alSourceQueueBuffers(this.source, var6[var10]);
                  if (++var11 == 1) {
                     this.songStartedAtMillis = System.currentTimeMillis();
                     this.pausedAtMillis = 0L;
                     AL10.alSourcePlay(this.source);
                  }

                  var10 = (var10 + 1) % 16;
                  var13 = 0;

                  while (true) {
                     if (var11 >= 16 && this.playing.get() && !this.stopped.get() && var2 == this.playGeneration.get() && this.isSourceValid()) {
                        int var21 = AL10.alGetSourcei(this.source, 4118);
                        if (var21 > 0) {
                           for (int var52 = 0; var52 < var21; var52++) {
                              AL10.alSourceUnqueueBuffers(this.source);
                           }

                           var11 -= var21;
                           if (var11 < 0) {
                              var11 = 0;
                           }

                           var13 = 0;
                           continue;
                        }

                        int var22 = AL10.alGetSourcei(this.source, 4112);
                        if (var22 == 4116) {
                           int var23 = AL10.alGetSourcei(this.source, 4117);
                           System.out.println("[MusicPlayer] Source stopped with " + var23 + " queued, force-unqueueing");

                           for (int var24 = 0; var24 < var23; var24++) {
                              AL10.alSourceUnqueueBuffers(this.source);
                           }

                           var11 = 0;
                        } else if (++var13 > 200) {
                           System.out.println("[MusicPlayer] Stuck for too long, resetting source");
                           if (this.isSourceValid()) {
                              AL10.alSourceStop(this.source);
                              int var53 = AL10.alGetSourcei(this.source, 4117);

                              for (int var54 = 0; var54 < var53; var54++) {
                                 AL10.alSourceUnqueueBuffers(this.source);
                              }
                           }

                           var11 = 0;
                        } else {
                           try {
                              Thread.sleep(10L);
                              continue;
                           } catch (InterruptedException var40) {
                           }
                        }
                     }

                     if (this.isSourceValid() && var11 > 0 && this.playing.get() && !this.paused.get()) {
                        int var51 = AL10.alGetSourcei(this.source, 4112);
                        if (var51 != 4114 && var51 != 4115) {
                           AL10.alSourcePlay(this.source);
                        }
                     }
                     break;
                  }
               }
            } finally {
               var4.closeFrame();
            }
         }
      }

      long var46 = System.currentTimeMillis();

      while (this.playing.get() && !this.stopped.get() && this.isSourceValid() && var2 == this.playGeneration.get()) {
         if (this.paused.get()) {
            try {
               Thread.sleep(50L);
            } catch (InterruptedException var35) {
               break;
            }
         } else {
            int var48 = AL10.alGetSourcei(this.source, 4112);
            int var49 = AL10.alGetSourcei(this.source, 4117);
            if (var48 == 4116 || var49 == 0) {
               break;
            }

            if (System.currentTimeMillis() - var46 > 10000L) {
               System.out.println("[MusicPlayer] Wait for buffers timeout, proceeding to next song");
               break;
            }

            try {
               Thread.sleep(50L);
            } catch (InterruptedException var36) {
               break;
            }
         }
      }

      this.cleanup(var6);

      try {
         var4.close();
      } catch (Exception var34) {
      }
   }

   private void cleanup(int[] var1) {
      if (this.isSourceValid()) {
         try {
            AL10.alSourceStop(this.source);
            int var2 = AL10.alGetSourcei(this.source, 4117);

            for (int var3 = 0; var3 < var2; var3++) {
               AL10.alSourceUnqueueBuffers(this.source);
            }
         } catch (Exception var8) {
         }
      }

      for (int var5 : var1) {
         try {
            if (var5 > 0 && AL10.alIsBuffer(var5)) {
               AL10.alDeleteBuffers(var5);
            }
         } catch (Exception var7) {
         }
      }
   }

   public void stop() {
      this.playGeneration.incrementAndGet();
      this.stopped.set(true);
      this.paused.set(false);
      this.playing.set(false);

      try {
         this.playbackExecutor.submit(this::stopSource);
      } catch (RejectedExecutionException var2) {
      }
   }

   public void stopAll() {
      this.stop();
      this.playQueue.clear();
      this.queueIndex = -1;
   }

   public void pause() {
      if (this.isSourceValid() && this.playing.get() && !this.paused.get()) {
         try {
            AL10.alSourcePause(this.source);
         } catch (Exception var2) {
         }

         this.paused.set(true);
         this.pausedAtMillis = System.currentTimeMillis();
      }
   }

   public void resume() {
      if (this.isSourceValid() && this.paused.get() && !this.stopped.get()) {
         try {
            AL10.alSourcePlay(this.source);
         } catch (Exception var2) {
         }

         if (this.songStartedAtMillis > 0L && this.pausedAtMillis > 0L) {
            this.songStartedAtMillis = this.songStartedAtMillis + (System.currentTimeMillis() - this.pausedAtMillis);
         }

         this.pausedAtMillis = 0L;
         this.paused.set(false);
      }
   }

   public boolean isPaused() {
      return this.paused.get();
   }

   public void setVolume(float var1) {
      if (this.isSourceValid()) {
         try {
            AL10.alSourcef(this.source, 4106, Math.max(0.0F, Math.min(1.0F, var1)));
         } catch (Exception var3) {
         }
      }
   }

   public SongInfo getCurrentSong() {
      return this.currentSong;
   }

   public boolean isPlaying() {
      return this.playing.get();
   }

   public boolean hasAutoPlayNext() {
      return this.autoPlayNext;
   }

   public void setAutoPlayNext(boolean var1) {
      this.autoPlayNext = var1;
   }

   public void setOnSongFinished(Consumer<SongInfo> var1) {
      this.onSongFinished = var1;
   }

   public void setOnSongSkipped(Consumer<SongInfo> var1) {
      this.onSongSkipped = var1;
   }

   public void setOnQueueExhausted(Runnable var1) {
      this.onQueueExhausted = var1;
   }

   public void setUrlResolver(Function<String, String> var1) {
      this.urlResolver = var1;
   }

   public int getQueueSize() {
      return this.playQueue.size();
   }

   public int getQueueIndex() {
      return this.queueIndex;
   }

   public List<SongInfo> getPlayQueue() {
      return new ArrayList<>(this.playQueue);
   }

   public void updateQueueSong(int var1, SongInfo var2) {
      if (var1 >= 0 && var1 < this.playQueue.size()) {
         this.playQueue.set(var1, var2);
      }
   }

   public void appendToQueue(List<SongInfo> var1) {
      if (var1 != null && !var1.isEmpty()) {
         this.playQueue.addAll(var1);
      }
   }

   public void destroy() {
      this.stopAll();
      this.playbackExecutor.shutdownNow();
      if (this.source >= 0) {
         try {
            AL10.alDeleteSources(this.source);
         } catch (Exception var2) {
         }

         this.source = -1;
      }
   }
}
