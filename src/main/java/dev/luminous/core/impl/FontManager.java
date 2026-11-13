package dev.luminous.core.impl;

import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;

public class FontManager {
   public static FontRenderer ui;
   public static FontRenderer small;
   public static FontRenderer icon;

   public static void init() {
      try {
         ui = assets(8.0F, "default", 0);
         small = assets(6.0F, "default", 0);
         icon = assetsWithoutOffset(8.0F, "icon", 0);
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   private static InputStream fontStream(String var0) {
      ClassLoader var1 = FontManager.class.getClassLoader();
      InputStream var2 = var1.getResourceAsStream("assets/alienclient/font/" + var0 + ".ttf");
      if (var2 == null) {
         var2 = var1.getResourceAsStream("assets/bullwwclient/font/" + var0 + ".ttf");
      }

      return var2;
   }

   public static boolean hasResourceFont(String var0) {
      return FontManager.class.getClassLoader().getResource("assets/alienclient/font/" + var0 + ".ttf") != null
         || FontManager.class.getClassLoader().getResource("assets/bullwwclient/font/" + var0 + ".ttf") != null;
   }

   public static FontRenderer assets(float var0, String var1, int var2, String var3) throws IOException, FontFormatException {
      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(fontStream(var1))).deriveFont(var2, var0), getFont(var3, var2, (int)var0), var0) {
         @Override
         public void drawString(MatrixStack var1, String var2x, float var3x, float var4, float var5, float var6, float var7, float var8, boolean var9) {
            super.drawString(
               var1, var2x, var3x + Fonts.INSTANCE.translate.getValueInt(), var4 + Fonts.INSTANCE.shift.getValueInt(), var5, var6, var7, var8, var9
            );
         }
      };
   }

   public static FontRenderer assetsWithoutOffset(float var0, String var1, int var2) throws IOException, FontFormatException {
      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(fontStream(var1))).deriveFont(var2, var0), var0);
   }

   public static FontRenderer assets(float var0, String var1, int var2) throws IOException, FontFormatException {
      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(fontStream(var1))).deriveFont(var2, var0), var0) {
         @Override
         public void drawString(MatrixStack var1, String var2x, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9) {
            super.drawString(
               var1, var2x, var3 + Fonts.INSTANCE.translate.getValueInt(), var4 + Fonts.INSTANCE.shift.getValueInt(), var5, var6, var7, var8, var9
            );
         }
      };
   }

   public static FontRenderer create(int var0, String var1, int var2, String var3) {
      return new FontRenderer(getFont(var1, var2, var0), getFont(var3, var2, var0), var0) {
         @Override
         public void drawString(MatrixStack var1, String var2x, float var3x, float var4, float var5, float var6, float var7, float var8, boolean var9) {
            super.drawString(
               var1, var2x, var3x + Fonts.INSTANCE.translate.getValueInt(), var4 + Fonts.INSTANCE.shift.getValueInt(), var5, var6, var7, var8, var9
            );
         }
      };
   }

   public static FontRenderer create(int var0, String var1, int var2) {
      return new FontRenderer(getFont(var1, var2, var0), var0) {
         @Override
         public void drawString(MatrixStack var1, String var2x, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9) {
            super.drawString(
               var1, var2x, var3 + Fonts.INSTANCE.translate.getValueInt(), var4 + Fonts.INSTANCE.shift.getValueInt(), var5, var6, var7, var8, var9
            );
         }
      };
   }

   public static boolean isCustomFontEnabled() {
      return Fonts.INSTANCE != null && Fonts.INSTANCE.isOn();
   }

   public static boolean isShadowEnabled() {
      return Fonts.INSTANCE != null && Fonts.INSTANCE.shadow.getValue();
   }

   private static Font getFont(String var0, int var1, int var2) {
      File var3 = new File("C:\\Windows\\Fonts");

      try {
         for (File var7 : var3.listFiles()) {
            if (var7.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(var0)) {
               try {
                  return Font.createFont(0, var7).deriveFont(var1, var2);
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         }

         for (File var15 : var3.listFiles()) {
            if (var15.getName().startsWith(var0)) {
               try {
                  return Font.createFont(0, var15).deriveFont(var1, var2);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }
         }
      } catch (Exception var11) {
      }

      return new Font(null, var1, var2);
   }
}
