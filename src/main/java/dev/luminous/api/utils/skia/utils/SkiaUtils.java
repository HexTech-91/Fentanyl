package dev.luminous.api.utils.skia.utils;

import io.github.humbleui.skija.Data;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SkiaUtils {
   public static Optional<byte[]> convertToBytes(String path) {
      try {
         Optional var2;
         try (InputStream inputStream = getResourceAsStream(path)) {
            if (inputStream == null) {
               return Optional.empty();
            }

            var2 = Optional.of(inputStream.readAllBytes());
         }

         return var2;
      } catch (IOException var6) {
         return Optional.empty();
      }
   }

   public static Optional<Data> convertToData(String path) {
      return convertToBytes(path).map(Data::makeFromBytes);
   }

   public static InputStream getResourceAsStream(String path) {
      if (path != null && !path.isBlank()) {
         InputStream stream = SkiaUtils.class.getResourceAsStream(path);
         if (stream != null) {
            return stream;
         } else {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            stream = SkiaUtils.class.getClassLoader().getResourceAsStream(normalized);
            if (stream != null) {
               return stream;
            } else {
               return !path.startsWith("/") ? SkiaUtils.class.getResourceAsStream("/" + path) : null;
            }
         }
      } else {
         return null;
      }
   }
}
