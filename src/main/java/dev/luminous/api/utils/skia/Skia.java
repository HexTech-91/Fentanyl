package dev.luminous.api.utils.skia;

import dev.luminous.Alien;
import dev.luminous.api.utils.skia.context.SkiaContext;
import dev.luminous.api.utils.skia.font.Fonts;
import dev.luminous.api.utils.skia.image.ImageHelper;
import dev.luminous.asm.accessors.IAbstractTexture;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.lang.Character.UnicodeScript;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Skia {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final ImageHelper IMAGE_HELPER = new ImageHelper();
   private static final ImageFilter SHADOW_FILTER = ImageFilter.makeBlur(3.0F, 3.0F, FilterTileMode.DECAL);
   private static final Map<Identifier, Image> TEXTURE_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Image> URL_CACHE = new ConcurrentHashMap<>();

   public static Canvas getCanvas() {
      return SkiaContext.getCanvas();
   }

   public static void save() {
      if (getCanvas() != null) {
         getCanvas().save();
      }
   }

   public static void restore() {
      if (getCanvas() != null) {
         getCanvas().restore();
      }
   }

   public static void clip(float x, float y, float width, float height, float radius) {
      if (getCanvas() != null) {
         getCanvas().clipRRect(RRect.makeXYWH(x, y, width, height, radius), ClipMode.INTERSECT, true);
      }
   }

   public static void drawRect(float x, float y, float width, float height, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            getCanvas().drawRect(Rect.makeXYWH(x, y, width, height), paint);
         }
      }
   }

   public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         }
      }
   }

   public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float strokeWidth, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(strokeWidth);
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         }
      }
   }

   public static void drawCircle(float x, float y, float radius, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            getCanvas().drawCircle(x, y, radius, paint);
         }
      }
   }

   public static void drawLine(float x, float y, float endX, float endY, float width, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(width);
            paint.setAntiAlias(true);
            getCanvas().drawLine(x, y, endX, endY, paint);
         }
      }
   }

   public static void drawArcStroke(float cx, float cy, float radius, float startDeg, float sweepDeg, float strokeWidth, Color color) {
      if (getCanvas() != null && sweepDeg != 0.0F) {
         int segments = Math.max(12, (int)(Math.abs(sweepDeg) / 8.0F));
         float step = sweepDeg / segments;
         float prevX = 0.0F;
         float prevY = 0.0F;

         for (int i = 0; i <= segments; i++) {
            float angle = (float)Math.toRadians(startDeg + step * i);
            float x = cx + (float)Math.cos(angle) * radius;
            float y = cy + (float)Math.sin(angle) * radius;
            if (i > 0) {
               drawLine(prevX, prevY, x, y, strokeWidth, color);
            }

            prevX = x;
            prevY = y;
         }
      }
   }

   public static void drawRoundedGuiBlur(float x, float y, float width, float height, float radius, float alpha) {
      if (Alien.BLUR != null) {
         Alien.BLUR.applyBlur(14.0F * alpha, x, y, width, height);
      }
   }

   public static void drawRoundedGuiBlur(float x, float y, float width, float height, float radius) {
      drawRoundedGuiBlur(x, y, width, height, radius, 1.0F);
   }

   public static void drawShadow(float x, float y, float width, float height, float radius) {
      drawShadow(x, y, width, height, radius, new Color(0, 0, 0, 90));
   }

   public static void drawShadow(float x, float y, float width, float height, float radius, Color color) {
      if (getCanvas() != null) {
         try (Paint paint = getPaint(color)) {
            paint.setImageFilter(SHADOW_FILTER);
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         }
      }
   }

   public static void drawImage(File file, float x, float y, float width, float height) {
      if (getCanvas() != null && file != null && IMAGE_HELPER.load(file)) {
         getCanvas().drawImageRect(IMAGE_HELPER.get(file.getAbsolutePath()), Rect.makeXYWH(x, y, width, height));
      }
   }

   public static void drawResourceImage(String resourcePath, float x, float y, float width, float height) {
      if (getCanvas() != null && resourcePath != null && IMAGE_HELPER.loadResource(resourcePath)) {
         getCanvas().drawImageRect(IMAGE_HELPER.get(resourcePath), Rect.makeXYWH(x, y, width, height));
      }
   }

   public static boolean drawTextureRegion(Identifier textureId, float x, float y, float width, float height, float u, float v, float texWidth, float texHeight) {
      if (getCanvas() != null && textureId != null) {
         Image image = loadMinecraftTexture(textureId);
         if (image == null) {
            return false;
         } else {
            getCanvas().drawImageRect(image, Rect.makeXYWH(u, v, texWidth, texHeight), Rect.makeXYWH(x, y, width, height));
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean drawUrlTextureRegion(String url, float x, float y, float width, float height, float u, float v, float texWidth, float texHeight) {
      if (getCanvas() != null && url != null && !url.isBlank()) {
         Image image = loadUrlImage(url);
         if (image == null) {
            return false;
         } else {
            getCanvas().drawImageRect(image, Rect.makeXYWH(u, v, texWidth, texHeight), Rect.makeXYWH(x, y, width, height));
            return true;
         }
      } else {
         return false;
      }
   }

   public static void invalidateTexture(Identifier textureId) {
      if (textureId != null) {
         TEXTURE_CACHE.remove(textureId);
      }
   }

   public static void invalidateUrlTexture(String url) {
      if (url != null) {
         URL_CACHE.remove(url);
      }
   }

   public static void drawRoundedImage(File file, float x, float y, float width, float height, float radius) {
      if (getCanvas() != null) {
         save();
         clip(x, y, width, height, radius);
         drawImage(file, x, y, width, height);
         restore();
      }
   }

   public static void drawText(String text, float x, float y, Color color, Font font) {
      if (getCanvas() != null && text != null) {
         try (Paint paint = getPaint(color)) {
            getCanvas().drawString(stripFormatting(text), x, y - font.getMetrics().getAscent(), font, paint);
         }
      }
   }

   public static float getStringWidth(String text, Font font) {
      return text != null && font != null ? font.measureTextWidth(stripFormatting(text)) : 0.0F;
   }

   public static float getFontHeight(Font font) {
      if (font == null) {
         return 9.0F;
      } else {
         FontMetrics metrics = font.getMetrics();
         return metrics.getDescent() - metrics.getAscent();
      }
   }

   public static String getLimitText(String text, Font font, float width) {
      if (text != null && !text.isEmpty() && !(getStringWidth(text, font) <= width)) {
         String value = text;
         float dots = getStringWidth("...", font);

         while (!value.isEmpty() && getStringWidth(value, font) + dots > width) {
            value = value.substring(0, value.length() - 1);
         }

         return value + "...";
      } else {
         return text;
      }
   }

   public static Font getDefaultFont(float size, boolean bold) {
      return bold ? Fonts.getProductSansMedium(size) : Fonts.getMiSans(size);
   }

   public static Font getDefaultFont(String text, float size, boolean bold) {
      if (containsCjk(text)) {
         return bold ? Fonts.getCjkBold(size) : Fonts.getCjkRegular(size);
      } else {
         return getDefaultFont(size, bold);
      }
   }

   public static Paint getPaint(Color color) {
      Paint paint = new Paint();
      paint.setARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
      paint.setAntiAlias(true);
      return paint;
   }

   private static String stripFormatting(String text) {
      return text == null ? "" : text.replaceAll("\u00a7.", "");
   }

   private static Image loadMinecraftTexture(Identifier textureId) {
      try {
         Image cached = TEXTURE_CACHE.get(textureId);
         if (cached != null) {
            return cached;
         } else {
            Image resourceImage = tryLoadResourceTexture(textureId);
            if (resourceImage != null) {
               TEXTURE_CACHE.put(textureId, resourceImage);
               return resourceImage;
            } else {
               MC.getTextureManager().bindTexture(textureId);
               AbstractTexture texture = MC.getTextureManager().getTexture(textureId);
               Image image = tryLoadNativeTexture(texture);
               if (image != null) {
                  TEXTURE_CACHE.put(textureId, image);
                  return image;
               } else {
                  int glTextureId = GL11.glGetInteger(32873);
                  if (glTextureId <= 0) {
                     glTextureId = getGlTextureId(texture);
                  }

                  if (glTextureId <= 0) {
                     return null;
                  } else {
                     int previousBinding = GL11.glGetInteger(32873);
                     GL11.glBindTexture(3553, glTextureId);

                     Image rasterImage;
                     try {
                        int width = GL11.glGetTexLevelParameteri(3553, 0, 4096);
                        int height = GL11.glGetTexLevelParameteri(3553, 0, 4097);
                        if (width <= 0 || height <= 0) {
                           return null;
                        }

                        int rowBytes = width * 4;
                        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
                        GL11.glGetTexImage(3553, 0, 6408, 5121, buffer);
                        byte[] flipped = flipTextureRows(buffer, width, height);
                        if (!isAllZero(flipped)) {
                           rasterImage = Image.makeRasterFromBytes(
                              new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), flipped, rowBytes
                           );
                           if (rasterImage != null) {
                              TEXTURE_CACHE.put(textureId, rasterImage);
                           }

                           return rasterImage;
                        }

                        rasterImage = null;
                     } finally {
                        GL11.glBindTexture(3553, previousBinding);
                     }

                     return rasterImage;
                  }
               }
            }
         }
      } catch (Throwable var18) {
         return null;
      }
   }

   private static Image loadUrlImage(String url) {
      try {
         Image cached = URL_CACHE.get(url);
         if (cached != null) {
            return cached;
         } else {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Tide-Skia");

            Image var7;
            try (InputStream in = conn.getInputStream()) {
               byte[] bytes = in.readAllBytes();
               Image image = Image.makeDeferredFromEncodedBytes(bytes);
               if (image != null) {
                  URL_CACHE.put(url, image);
               }

               var7 = image;
            }

            return var7;
         }
      } catch (Throwable var10) {
         return null;
      }
   }

   private static Image tryLoadResourceTexture(Identifier textureId) {
      try {
         Optional<Resource> resource = MC.getResourceManager().getResource(textureId);
         if (resource.isEmpty()) {
            return null;
         } else {
            try (InputStream inputStream = resource.get().getInputStream()) {
               byte[] bytes = inputStream.readAllBytes();
               Image image = Image.makeDeferredFromEncodedBytes(bytes);
               return image != null ? image : null;
            }
         }
      } catch (Throwable var8) {
         return null;
      }
   }

   private static Image tryLoadNativeTexture(AbstractTexture texture) {
      if (texture == null) {
         return null;
      } else {
         try {
            Image image = extractNativeImage(texture, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            if (image != null) {
               return image;
            }
         } catch (Throwable var2) {
         }

         return null;
      }
   }

   private static Image extractNativeImage(Object value, Set<Object> visited, int depth) {
      if (value != null && depth <= 7 && !visited.contains(value)) {
         visited.add(value);
         if (value instanceof NativeImage nativeImage) {
            return nativeImageToSkiaImage(nativeImage);
         } else {
            if (value instanceof NativeImageBackedTexture nativeTexture) {
               NativeImage nativeImage = nativeTexture.getImage();
               if (nativeImage != null) {
                  return nativeImageToSkiaImage(nativeImage);
               }
            }

            for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
               for (Field field : type.getDeclaredFields()) {
                  try {
                     field.setAccessible(true);
                     Object child = field.get(value);
                     Image image = extractNativeImage(child, visited, depth + 1);
                     if (image != null) {
                        return image;
                     }
                  } catch (Throwable var11) {
                  }
               }

               for (Method method : type.getDeclaredMethods()) {
                  if (method.getParameterCount() == 0 && method.getReturnType() != void.class && method.getDeclaringClass() != Object.class) {
                     String methodName = method.getName().toLowerCase();
                     if (methodName.contains("image")
                        || methodName.contains("native")
                        || methodName.contains("pixel")
                        || methodName.contains("buffer")
                        || methodName.contains("content")
                        || methodName.contains("texture")
                        || methodName.contains("data")) {
                        try {
                           method.setAccessible(true);
                           Object child = method.invoke(value);
                           Image image = extractNativeImage(child, visited, depth + 1);
                           if (image != null) {
                              return image;
                           }
                        } catch (Throwable var12) {
                        }
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static Image nativeImageToSkiaImage(NativeImage nativeImage) {
      int width = nativeImage.getWidth();
      int height = nativeImage.getHeight();
      int rowBytes = width * 4;
      byte[] pixels = new byte[rowBytes * height];
      int index = 0;

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int color = nativeImage.getColor(x, y);
            pixels[index++] = (byte)(color & 0xFF);
            pixels[index++] = (byte)(color >> 8 & 0xFF);
            pixels[index++] = (byte)(color >> 16 & 0xFF);
            pixels[index++] = (byte)(color >> 24 & 0xFF);
         }
      }

      return Image.makeRasterFromBytes(new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), pixels, rowBytes);
   }

   private static byte[] flipTextureRows(ByteBuffer buffer, int width, int height) {
      int rowBytes = width * 4;
      byte[] source = new byte[rowBytes * height];
      buffer.rewind();
      buffer.get(source);
      byte[] flipped = new byte[source.length];

      for (int row = 0; row < height; row++) {
         int srcPos = row * rowBytes;
         int dstPos = (height - 1 - row) * rowBytes;
         System.arraycopy(source, srcPos, flipped, dstPos, rowBytes);
      }

      return flipped;
   }

   private static boolean isAllZero(byte[] pixels) {
      if (pixels != null && pixels.length != 0) {
         for (byte pixel : pixels) {
            if (pixel != 0) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static int getGlTextureId(AbstractTexture texture) {
      try {
         return ((IAbstractTexture)texture).getGlId();
      } catch (Throwable var2) {
         return -1;
      }
   }

   private static boolean containsCjk(String text) {
      if (text != null && !text.isEmpty()) {
         for (int i = 0; i < text.length(); i++) {
            UnicodeScript script = UnicodeScript.of(text.charAt(i));
            if (script == UnicodeScript.HAN || script == UnicodeScript.HIRAGANA || script == UnicodeScript.KATAKANA || script == UnicodeScript.HANGUL) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
