package dev.luminous.mod.gui.tclick.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class TSounds {
   public static boolean enabled = true;
   public static float volume = 1.0F;
   public static final Identifier SCROLL_ID = Identifier.of("bullwwclient", "scroll");
   public static final Identifier SWIPEIN_ID = Identifier.of("bullwwclient", "swipein");
   public static final Identifier SWIPEOUT_ID = Identifier.of("bullwwclient", "swipeout");
   public static final Identifier BOOLEAN_ID = Identifier.of("bullwwclient", "boolean");
   public static final Identifier ENABLE_ID = Identifier.of("bullwwclient", "enable");
   public static final Identifier DISABLE_ID = Identifier.of("bullwwclient", "disable");
   public static final Identifier KEYPRESS_ID = Identifier.of("bullwwclient", "keypress");
   public static SoundEvent SCROLL;
   public static SoundEvent SWIPEIN;
   public static SoundEvent SWIPEOUT;
   public static SoundEvent BOOLEAN;
   public static SoundEvent ENABLE;
   public static SoundEvent DISABLE;
   public static SoundEvent KEYPRESS;
   private static boolean registered = false;
   private static long lastScroll = 0L;

   private TSounds() {
   }

   public static synchronized void register() {
      if (!registered) {
         try {
            SCROLL = SoundEvent.of(SCROLL_ID);
            SWIPEIN = SoundEvent.of(SWIPEIN_ID);
            SWIPEOUT = SoundEvent.of(SWIPEOUT_ID);
            BOOLEAN = SoundEvent.of(BOOLEAN_ID);
            ENABLE = SoundEvent.of(ENABLE_ID);
            DISABLE = SoundEvent.of(DISABLE_ID);
            KEYPRESS = SoundEvent.of(KEYPRESS_ID);
            Registry.register(Registries.SOUND_EVENT, SCROLL_ID, SCROLL);
            Registry.register(Registries.SOUND_EVENT, SWIPEIN_ID, SWIPEIN);
            Registry.register(Registries.SOUND_EVENT, SWIPEOUT_ID, SWIPEOUT);
            Registry.register(Registries.SOUND_EVENT, BOOLEAN_ID, BOOLEAN);
            Registry.register(Registries.SOUND_EVENT, ENABLE_ID, ENABLE);
            Registry.register(Registries.SOUND_EVENT, DISABLE_ID, DISABLE);
            Registry.register(Registries.SOUND_EVENT, KEYPRESS_ID, KEYPRESS);
            registered = true;
         } catch (Throwable var1) {
         }
      }
   }

   public static void playScroll() {
      if (enabled) {
         long var0 = System.currentTimeMillis();
         if (var0 - lastScroll >= 50L) {
            lastScroll = var0;
            play(KEYPRESS);
         }
      }
   }

   public static void playSlider() {
      if (enabled) {
         play(SCROLL);
      }
   }

   public static void playBoolean() {
      if (enabled) {
         play(BOOLEAN);
      }
   }

   public static void playSwipeIn() {
      if (enabled) {
         play(SWIPEIN);
      }
   }

   public static void playSwipeOut() {
      if (enabled) {
         play(SWIPEOUT);
      }
   }

   public static void playEnable() {
      if (enabled) {
         play(ENABLE);
      }
   }

   public static void playDisable() {
      if (enabled) {
         play(DISABLE);
      }
   }

   private static void play(SoundEvent var0) {
      try {
         if (!registered) {
            register();
         }

         if (var0 == null) {
            return;
         }

         MinecraftClient var1 = MinecraftClient.getInstance();
         if (var1.player != null && var1.world != null) {
            var1.world.playSound(var1.player, var1.player.getBlockPos(), var0, SoundCategory.BLOCKS, volume, 1.0F);
         }
      } catch (Throwable var2) {
      }
   }
}
