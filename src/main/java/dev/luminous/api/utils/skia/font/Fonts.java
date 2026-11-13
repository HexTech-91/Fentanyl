package dev.luminous.api.utils.skia.font;

import io.github.humbleui.skija.Font;

public class Fonts {
   private static final String MISANS = "MiSans-Regular.ttf";
   private static final String PRODUCT_SANS_MEDIUM = "productsans-medium.ttf";
   private static final String CLASSIC = "classic.ttf";
   private static final String ICON = "icon.ttf";
   private static final String WINDOWS_CJK_REGULAR = "C:\\Windows\\Fonts\\msyh.ttc";
   private static final String WINDOWS_CJK_BOLD = "C:\\Windows\\Fonts\\msyhbd.ttc";

   public static void loadAll() {
      FontHelper.preloadFonts("MiSans-Regular.ttf", "productsans-medium.ttf", "icon.ttf");
   }

   public static Font getMiSans(float size) {
      return FontHelper.load("MiSans-Regular.ttf", size);
   }

   public static Font getProductSansMedium(float size) {
      return FontHelper.load("productsans-medium.ttf", size);
   }

   public static Font getClassic(float size) {
      return FontHelper.load("classic.ttf", size);
   }

   public static Font getCjkRegular(float size) {
      try {
         return FontHelper.loadSystem("C:\\Windows\\Fonts\\msyh.ttc", size);
      } catch (Exception var2) {
         return getMiSans(size);
      }
   }

   public static Font getCjkBold(float size) {
      try {
         return FontHelper.loadSystem("C:\\Windows\\Fonts\\msyhbd.ttc", size);
      } catch (Exception var2) {
         return getProductSansMedium(size);
      }
   }

   public static Font getNavenIcon(float size) {
      return FontHelper.load("icon.ttf", size);
   }
}
