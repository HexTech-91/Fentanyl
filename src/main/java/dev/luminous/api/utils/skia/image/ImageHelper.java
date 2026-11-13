package dev.luminous.api.utils.skia.image;

import io.github.humbleui.skija.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ImageHelper {
   private final Map<String, Image> images = new HashMap<>();
   private final Map<Integer, Image> textures = new HashMap<>();

   public boolean load(int texture, float width, float height, Object origin) {
      return this.textures.containsKey(texture);
   }

   public boolean load(File file) {
      if (file != null && file.exists()) {
         String key = file.getAbsolutePath();
         if (this.images.containsKey(key)) {
            return true;
         } else {
            try {
               byte[] bytes = Files.readAllBytes(file.toPath());
               Image image = Image.makeDeferredFromEncodedBytes(bytes);
               if (image == null) {
                  return false;
               } else {
                  this.images.put(key, image);
                  return true;
               }
            } catch (IOException var5) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean loadResource(String path) {
      if (path == null || path.isBlank()) {
         return false;
      } else if (this.images.containsKey(path)) {
         return true;
      } else {
         String normalized = path.startsWith("/") ? path.substring(1) : path;

         try {
            boolean var6;
            try (InputStream inputStream = this.openResource(path, normalized)) {
               if (inputStream == null) {
                  return false;
               }

               byte[] bytes = inputStream.readAllBytes();
               Image image = Image.makeDeferredFromEncodedBytes(bytes);
               if (image == null) {
                  return false;
               }

               this.images.put(path, image);
               var6 = true;
            }

            return var6;
         } catch (IOException var9) {
            return false;
         }
      }
   }

   private InputStream openResource(String rawPath, String normalizedPath) {
      InputStream inputStream = ImageHelper.class.getClassLoader().getResourceAsStream(rawPath);
      if (inputStream != null) {
         return inputStream;
      } else {
         inputStream = ImageHelper.class.getClassLoader().getResourceAsStream(normalizedPath);
         return inputStream != null ? inputStream : ImageHelper.class.getResourceAsStream(rawPath.startsWith("/") ? rawPath : "/" + rawPath);
      }
   }

   public Image get(String key) {
      return this.images.get(key);
   }

   public Image get(int texture) {
      return this.textures.get(texture);
   }
}
