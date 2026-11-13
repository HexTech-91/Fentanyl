package dev.luminous.mod.gui.fonts;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class FontRenderer implements Closeable {
   private static final Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   private static final ExecutorService ASYNC_WORKER = Executors.newCachedThreadPool();
   private final Object2ObjectMap<Identifier, ObjectList<FontRenderer.DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap();
   private final float originalSize;
   private final ObjectList<GlyphMap> maps = new ObjectArrayList();
   private final Char2ObjectArrayMap<Glyph> allGlyphs = new Char2ObjectArrayMap();
   private final int charsPerPage;
   private final int padding;
   private final String prebakeGlyphs;
   private int scaleMul = 0;
   private Font font;
   private Font secondFont;
   private int previousGameScale = -1;
   private Future<Void> prebakeGlyphsFuture;
   private boolean initialized;

   public FontRenderer(Font var1, float var2, int var3, int var4, @Nullable String var5) {
      this.originalSize = var2;
      this.charsPerPage = var3;
      this.padding = var4;
      this.prebakeGlyphs = var5;
      this.init(var1, var2);
   }

   public FontRenderer(Font var1, Font var2, float var3, int var4, int var5, @Nullable String var6) {
      this(var1, var3, var4, var5, var6);
      this.secondFont = var2.deriveFont(var3 * this.scaleMul);
   }

   public FontRenderer(Font var1, float var2) {
      this(var1, var2, 256, 5, null);
   }

   public FontRenderer(Font var1, Font var2, float var3) {
      this(var1, var2, var3, 256, 5, null);
   }

   private static int floorNearestMulN(int var0, int var1) {
      return var1 * (int)Math.floor((double)var0 / var1);
   }

   public static String stripControlCodes(String var0) {
      char[] var1 = var0.toCharArray();
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1.length; var3++) {
         char var4 = var1[var3];
         if (var4 == 167) {
            var3++;
         } else {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   @Contract(
      value = "-> new",
      pure = true
   )
   @NotNull
   public static Identifier randomIdentifier() {
      return Identifier.of("frogclient", "temp/" + randomString());
   }

   private static String randomString() {
      return IntStream.range(0, 32).mapToObj(var0 -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
   }

   @Contract(
      value = "_ -> new",
      pure = true
   )
   @NotNull
   public static int[] RGBIntToRGB(int var0) {
      int var1 = var0 >> 16 & 0xFF;
      int var2 = var0 >> 8 & 0xFF;
      int var3 = var0 & 0xFF;
      return new int[]{var1, var2, var3};
   }

   public static double roundToDecimal(double var0, int var2) {
      if (var2 == 0) {
         return Math.floor(var0);
      } else {
         double var3 = Math.pow(10.0, var2);
         return Math.round(var0 * var3) / var3;
      }
   }

   private void sizeCheck() {
      int var1 = (int)Wrapper.mc.getWindow().getScaleFactor();
      if (var1 != this.previousGameScale) {
         this.close();
         this.init(this.font, this.originalSize);
         if (this.secondFont != null) {
            this.secondFont = this.secondFont.deriveFont(this.originalSize * this.scaleMul);
         }
      }
   }

   private void init(Font var1, float var2) {
      if (this.initialized) {
         throw new IllegalStateException("Double call to init()");
      } else {
         this.initialized = true;
         this.scaleMul = this.previousGameScale = (int)Wrapper.mc.getWindow().getScaleFactor();
         this.font = var1.deriveFont(var2 * this.scaleMul);
         if (this.prebakeGlyphs != null && !this.prebakeGlyphs.isEmpty()) {
            this.prebakeGlyphsFuture = this.prebake();
         }
      }
   }

   private Future<Void> prebake() {
      return ASYNC_WORKER.submit(() -> {
         for (char var4 : this.prebakeGlyphs.toCharArray()) {
            if (Thread.interrupted()) {
               break;
            }

            this.locateGlyph1(var4);
         }

         return null;
      });
   }

   private GlyphMap generateMap(char var1, char var2) {
      GlyphMap var3 = this.secondFont != null
         ? new GlyphMap(var1, var2, this.font, this.secondFont, randomIdentifier(), this.padding)
         : new GlyphMap(var1, var2, this.font, randomIdentifier(), this.padding);
      this.maps.add(var3);
      return var3;
   }

   private Glyph locateGlyph0(char var1) {
      ObjectListIterator var2 = this.maps.iterator();

      while (var2.hasNext()) {
         GlyphMap var3 = (GlyphMap)var2.next();
         if (var3.contains(var1)) {
            return var3.getGlyph(var1);
         }
      }

      int var5 = floorNearestMulN(var1, this.charsPerPage);
      GlyphMap var4 = this.generateMap((char)var5, (char)(var5 + this.charsPerPage));
      return var4.getGlyph(var1);
   }

   @Nullable
   private Glyph locateGlyph1(char var1) {
      return (Glyph)this.allGlyphs.computeIfAbsent(var1, this::locateGlyph0);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, int var7) {
      float var8 = (var7 >> 16 & 0xFF) / 255.0F;
      float var9 = (var7 >> 8 & 0xFF) / 255.0F;
      float var10 = (var7 & 0xFF) / 255.0F;
      float var11 = (var7 >> 24 & 0xFF) / 255.0F;
      this.drawString(var1, var2, (float)var3, (float)var5, var8, var9, var10, var11);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, Color var7) {
      this.drawString(var1, var2, (float)var3, (float)var5, var7.getRed() / 255.0F, var7.getGreen() / 255.0F, var7.getBlue() / 255.0F, var7.getAlpha());
   }

   public void drawString(MatrixStack var1, String var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.drawString(var1, var2, var3, var4, var5, var6, var7, var8, false);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, int var7, boolean var8) {
      float var9 = (var7 >> 16 & 0xFF) / 255.0F;
      float var10 = (var7 >> 8 & 0xFF) / 255.0F;
      float var11 = (var7 & 0xFF) / 255.0F;
      float var12 = (var7 >> 24 & 0xFF) / 255.0F;
      this.drawString(var1, var2, (float)var3, (float)var5, var9, var10, var11, var12, var8);
   }

   public void drawString(MatrixStack var1, String var2, double var3, double var5, Color var7, boolean var8) {
      this.drawString(var1, var2, (float)var3, (float)var5, var7.getRed() / 255.0F, var7.getGreen() / 255.0F, var7.getBlue() / 255.0F, var7.getAlpha(), var8);
   }

   public void drawStringWithShadow(MatrixStack var1, String var2, double var3, double var5, int var7) {
      float var8 = (var7 >> 16 & 0xFF) / 255.0F;
      float var9 = (var7 >> 8 & 0xFF) / 255.0F;
      float var10 = (var7 & 0xFF) / 255.0F;
      float var11 = (var7 >> 24 & 0xFF) / 255.0F;
      this.drawStringWithShadow(var1, var2, (float)var3, (float)var5, var8, var9, var10, var11);
   }

   public void drawStringWithShadow(MatrixStack var1, String var2, double var3, double var5, Color var7) {
      this.drawStringWithShadow(
         var1, var2, (float)var3, (float)var5, var7.getRed() / 255.0F, var7.getGreen() / 255.0F, var7.getBlue() / 255.0F, var7.getAlpha()
      );
   }

   public void drawStringWithShadow(MatrixStack var1, String var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.drawString(var1, var2, var3, var4, var5, var6, var7, var8, true);
   }

   public void drawString(MatrixStack var1, String var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9) {
      if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
         try {
            this.prebakeGlyphsFuture.get();
         } catch (ExecutionException | InterruptedException var45) {
         }
      }

      this.sizeCheck();
      float var10 = var5;
      float var11 = var6;
      float var12 = var7;
      float var13 = var5;
      float var14 = var6;
      float var15 = var7;
      boolean var16 = this.useClickGuiRainbowByY();
      if (var16) {
         Color var17 = ClickGui.getInstance().getActiveColor(var4 / 10.0);
         var10 = var17.getRed() / 255.0F;
         var11 = var17.getGreen() / 255.0F;
         var12 = var17.getBlue() / 255.0F;
         var13 = var10;
         var14 = var11;
         var15 = var12;
      }

      var1.push();
      var1.translate(roundToDecimal(var3, 1), roundToDecimal(var4, 1), 0.0);
      var1.scale(1.0F / this.scaleMul, 1.0F / this.scaleMul, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      GL11.glTexParameteri(3553, 10241, 9729);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      Matrix4f var47 = var1.peek().getPositionMatrix();
      char[] var18 = var2.toCharArray();
      float var19 = 0.0F;
      float var20 = 0.0F;
      boolean var21 = false;
      int var22 = 0;
      Object2ObjectMap var23 = this.GLYPH_PAGE_CACHE;
      synchronized (var23) {
         for (int var25 = 0; var25 < var18.length; var25++) {
            char var26 = var18[var25];
            if (var21) {
               var21 = false;
               char var27 = Character.toUpperCase(var26);
               if (colorCodes.containsKey(var27)) {
                  int var28 = colorCodes.get(var27);
                  int[] var29 = RGBIntToRGB(var28);
                  var13 = var29[0] / 255.0F;
                  var14 = var29[1] / 255.0F;
                  var15 = var29[2] / 255.0F;
               } else if (var27 == 'R') {
                  var13 = var10;
                  var14 = var11;
                  var15 = var12;
               }
            } else if (var26 == 167) {
               var21 = true;
            } else if (var26 == '\n') {
               var20 += this.getStringHeight(var2.substring(var22, var25)) * this.scaleMul;
               var19 = 0.0F;
               var22 = var25 + 1;
               if (var16) {
                  Color var50 = ClickGui.getInstance().getActiveColor((var4 + var20) / 10.0);
                  var10 = var50.getRed() / 255.0F;
                  var11 = var50.getGreen() / 255.0F;
                  var12 = var50.getBlue() / 255.0F;
                  var13 = var10;
                  var14 = var11;
                  var15 = var12;
               }
            } else {
               Glyph var51 = this.locateGlyph1(var26);
               if (var51 != null) {
                  if (var51.value() != ' ') {
                     Identifier var53 = var51.owner().bindToTexture;
                     if (var9) {
                        FontRenderer.DrawEntry var55 = new FontRenderer.DrawEntry(var19 + 1.0F, var20 + 1.0F, 0.0F, 0.0F, 0.0F, var51);
                        ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(var53, var0 -> new ObjectArrayList())).add(var55);
                     }

                     FontRenderer.DrawEntry var56 = new FontRenderer.DrawEntry(var19, var20, var13, var14, var15, var51);
                     ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(var53, var0 -> new ObjectArrayList())).add(var56);
                  }

                  var19 += var51.width();
               }
            }
         }

         ObjectIterator var48 = this.GLYPH_PAGE_CACHE.keySet().iterator();

         while (var48.hasNext()) {
            Identifier var49 = (Identifier)var48.next();
            RenderSystem.setShaderTexture(0, var49);
            List<FontRenderer.DrawEntry> var52 = (List<FontRenderer.DrawEntry>)(Object)this.GLYPH_PAGE_CACHE.get(var49);
            BufferBuilder var54 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (FontRenderer.DrawEntry var30 : var52) {
               float var31 = var30.atX;
               float var32 = var30.atY;
               float var33 = var30.r;
               float var34 = var30.g;
               float var35 = var30.elementCodec;
               Glyph var36 = var30.toDraw;
               GlyphMap var37 = var36.owner();
               float var38 = var36.width();
               float var39 = var36.height();
               float var40 = (float)var36.u() / var37.width;
               float var41 = (float)var36.v() / var37.height;
               float var42 = (float)(var36.u() + var36.width()) / var37.width;
               float var43 = (float)(var36.v() + var36.height()) / var37.height;
               var54.vertex(var47, var31 + 0.0F, var32 + var39, 0.0F).texture(var40, var43).color(var33, var34, var35, var8);
               var54.vertex(var47, var31 + var38, var32 + var39, 0.0F).texture(var42, var43).color(var33, var34, var35, var8);
               var54.vertex(var47, var31 + var38, var32 + 0.0F, 0.0F).texture(var42, var41).color(var33, var34, var35, var8);
               var54.vertex(var47, var31 + 0.0F, var32 + 0.0F, 0.0F).texture(var40, var41).color(var33, var34, var35, var8);
            }

            Render3DUtil.endBuilding(var54);
         }

         this.GLYPH_PAGE_CACHE.clear();
      }

      var1.pop();
   }

   public void drawCenteredString(MatrixStack var1, String var2, double var3, double var5, int var7) {
      float var8 = (var7 >> 16 & 0xFF) / 255.0F;
      float var9 = (var7 >> 8 & 0xFF) / 255.0F;
      float var10 = (var7 & 0xFF) / 255.0F;
      float var11 = (var7 >> 24 & 0xFF) / 255.0F;
      this.drawStringWithShadow(var1, var2, (float)(var3 - this.getWidth(var2) / 2.0F), (float)var5, var8, var9, var10, var11);
   }

   public void drawCenteredString(MatrixStack var1, String var2, double var3, double var5, Color var7) {
      this.drawStringWithShadow(
         var1,
         var2,
         (float)(var3 - this.getWidth(var2) / 2.0F),
         (float)var5,
         var7.getRed() / 255.0F,
         var7.getGreen() / 255.0F,
         var7.getBlue() / 255.0F,
         var7.getAlpha() / 255.0F
      );
   }

   public void drawCenteredString(MatrixStack var1, String var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.drawStringWithShadow(var1, var2, var3 - this.getWidth(var2) / 2.0F, var4, var5, var6, var7, var8);
   }

   public float getWidth(String var1) {
      char[] var2 = stripControlCodes(var1).toCharArray();
      float var3 = 0.0F;
      float var4 = 0.0F;

      for (char var8 : var2) {
         if (var8 == '\n') {
            var4 = Math.max(var3, var4);
            var3 = 0.0F;
         } else {
            Glyph var9 = this.locateGlyph1(var8);
            var3 += var9 == null ? 0.0F : (float)var9.width() / this.scaleMul;
         }
      }

      return Math.max(var3, var4);
   }

   public float getStringHeight(String var1) {
      char[] var2 = stripControlCodes(var1).toCharArray();
      if (var2.length == 0) {
         var2 = new char[]{' '};
      }

      float var3 = 0.0F;
      float var4 = 0.0F;

      for (char var8 : var2) {
         if (var8 == '\n') {
            if (var3 == 0.0F) {
               var3 = this.locateGlyph1(' ') == null ? 0.0F : (float)Objects.requireNonNull(this.locateGlyph1(' ')).height() / this.scaleMul;
            }

            var4 += var3;
            var3 = 0.0F;
         } else {
            Glyph var9 = this.locateGlyph1(var8);
            var3 = Math.max(var9 == null ? 0.0F : (float)var9.height() / this.scaleMul, var3);
         }
      }

      return var3 + var4;
   }

   @Override
   public void close() {
      try {
         if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone() && !this.prebakeGlyphsFuture.isCancelled()) {
            this.prebakeGlyphsFuture.cancel(true);
            this.prebakeGlyphsFuture.get();
            this.prebakeGlyphsFuture = null;
         }

         ObjectListIterator var1 = this.maps.iterator();

         while (var1.hasNext()) {
            GlyphMap var2 = (GlyphMap)var1.next();
            var2.destroy();
         }

         this.maps.clear();
         this.allGlyphs.clear();
         this.initialized = false;
      } catch (Exception var3) {
      }
   }

   public float getFontHeight(String var1) {
      return this.getStringHeight(var1);
   }

   public float getFontHeight() {
      return this.getStringHeight("A");
   }

   public void drawGradientString(MatrixStack var1, String var2, float var3, float var4) {
      this.drawString(var1, var2, var3, var4, 255.0F, 255.0F, 255.0F, 255.0F);
   }

   public void drawGradientCenteredString(MatrixStack var1, String var2, float var3, float var4) {
      this.drawGradientString(var1, var2, var3 - this.getWidth(var2) / 2.0F, var4);
   }

   private boolean useClickGuiRainbowByY() {
      if (Wrapper.mc == null || Wrapper.mc.currentScreen == null) {
         return false;
      } else if (!(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) {
         return false;
      } else {
         ClickGui var1 = ClickGui.getInstance();
         return var1 == null ? false : var1.colors.getValue() && var1.colorMode.getValue() == ClickGui.ColorMode.Rainbow;
      }
   }

   record DrawEntry(float atX, float atY, float r, float g, float elementCodec, Glyph toDraw) {
   }
}
