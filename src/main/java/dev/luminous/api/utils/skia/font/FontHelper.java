package dev.luminous.api.utils.skia.font;

import dev.luminous.api.utils.skia.utils.SkiaUtils;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FontHelper {
   private static final Map<String, Typeface> CACHE = new HashMap<>();

   public static Font load(String font, float size) {
      return new Font(getTypeface(font), size);
   }

   public static void preloadFonts(String... fonts) {
      for (String font : fonts) {
         getTypeface(font);
      }
   }

   public static Font loadSystem(String path, float size) {
      return new Font(getSystemTypeface(path), size);
   }

   private static Typeface getTypeface(String font) {
      return CACHE.computeIfAbsent(font, FontHelper::loadTypeface);
   }

   private static Typeface loadTypeface(String font) {
      Optional<Data> direct = SkiaUtils.convertToData("/assets/alien/fonts/" + font);
      if (direct.isPresent()) {
         return Typeface.makeFromData(direct.get());
      } else {
         Optional<Data> fallbackMinecraft = SkiaUtils.convertToData("/assets/minecraft/font/font.ttf");
         if (fallbackMinecraft.isPresent()) {
            return Typeface.makeFromData(fallbackMinecraft.get());
         } else {
            throw new IllegalArgumentException("Font not found: " + font);
         }
      }
   }

   private static Typeface getSystemTypeface(String path) {
      return CACHE.computeIfAbsent("system:" + path, key -> {
         File file = new File(path);
         if (!file.exists()) {
            throw new IllegalArgumentException("System font not found: " + path);
         } else {
            return Typeface.makeFromFile(path);
         }
      });
   }
}
