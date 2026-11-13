package dev.luminous.mod.gui.tclick.core;

import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.impl.client.Fonts;
import java.util.LinkedHashSet;

public final class TFonts {
   public static TFont settings;
   public static TFont modules;
   public static TFont categories;
   public static TFont sf_bold;
   public static TFont sf_medium;
   public static TFont sf_medium_mini;
   public static TFont sf_medium_modules;
   public static TFont sf_bold_mini;
   public static TFont sf_bold_micro;
   public static TFont icons;
   public static TFont mid_icons;

   private TFonts() {
   }

   public static void init() {
      if (!ready()) {
         try {
            settings = wrap(create(6.0F, "comfortaa"));
            modules = wrap(create(7.5F, "comfortaa"));
            categories = wrap(create(9.0F, "comfortaa"));
            sf_bold = wrap(create(8.0F, "sf_bold"));
            sf_medium = wrap(create(8.0F, "sf_medium"));
            sf_medium_mini = wrap(create(6.0F, "sf_medium"));
            sf_medium_modules = wrap(create(7.0F, "sf_medium"));
            sf_bold_mini = wrap(create(7.0F, "sf_bold"));
            sf_bold_micro = wrap(create(6.0F, "sf_bold"));
            icons = wrap(create(10.0F, "icons"));
            mid_icons = wrap(create(23.0F, "icons"));
            fillFallbacks();
         } catch (Throwable var1) {
            fillFallbacks();
         }
      }
   }

   public static void setSettingFontScale(int var0) {
      FontRenderer var1 = create(Math.max(1.0F, var0 / 2.0F), "sf_medium");
      if (var1 != null) {
         sf_medium_mini = wrap(var1);
      }
   }

   public static void setModulesFontScale(int var0) {
      FontRenderer var1 = create(Math.max(1.0F, var0 / 2.0F), "sf_medium");
      if (var1 != null) {
         sf_medium_modules = wrap(var1);
      }
   }

   public static boolean ready() {
      return settings != null
         && modules != null
         && categories != null
         && sf_bold != null
         && sf_medium != null
         && sf_medium_mini != null
         && sf_medium_modules != null
         && sf_bold_mini != null
         && sf_bold_micro != null
         && icons != null
         && mid_icons != null;
   }

   private static TFont wrap(FontRenderer var0) {
      FontRenderer var1 = var0 != null ? var0 : FontManager.ui;
      return var1 == null ? null : new TFont(var1);
   }

   private static FontRenderer create(float var0, String var1) {
      try {
         String var2 = var1;
         String var3 = "msyh";
         int var4 = 0;
         if (Fonts.INSTANCE != null && !"icons".equals(var1)) {
            String var5 = Fonts.INSTANCE.font.getValue();
            if (var5 != null && !var5.isEmpty() && !var5.equals("default") && !var5.equals("icons")) {
               var2 = var5;
            }

            String var6 = Fonts.INSTANCE.alternate.getValue();
            if (var6 != null && !var6.isEmpty() && !var6.equals("null")) {
               var3 = var6;
            }

            var4 = Fonts.INSTANCE.style.getValue().get();
         }

         return FontManager.hasResourceFont(var2) ? FontManager.assets(var0, var2, var4, var3) : FontManager.create((int)var0, var2, var4, var3);
      } catch (Throwable var7) {
         return null;
      }
   }

   public static void rebuild() {
      try {
         LinkedHashSet<FontRenderer> var0 = new LinkedHashSet<>();

         for (TFont var4 : new TFont[]{
            settings, modules, categories, sf_bold, sf_medium, sf_medium_mini, sf_medium_modules, sf_bold_mini, sf_bold_micro, icons, mid_icons
         }) {
            if (var4 != null && var4.base() != null) {
               var0.add(var4.base());
            }
         }

         var0.remove(FontManager.ui);
         var0.remove(FontManager.small);
         var0.remove(FontManager.icon);
         settings = null;
         modules = null;
         categories = null;
         sf_bold = null;
         sf_medium = null;
         sf_medium_mini = null;
         sf_medium_modules = null;
         sf_bold_mini = null;
         sf_bold_micro = null;
         icons = null;
         mid_icons = null;
         init();

         for (FontRenderer var8 : var0) {
            try {
               var8.close();
            } catch (Throwable var5) {
            }
         }
      } catch (Throwable var6) {
      }
   }

   private static void fillFallbacks() {
      if (FontManager.ui != null) {
         TFont var0 = new TFont(FontManager.ui);
         if (settings == null) {
            settings = var0;
         }

         if (modules == null) {
            modules = var0;
         }

         if (categories == null) {
            categories = var0;
         }

         if (sf_bold == null) {
            sf_bold = var0;
         }

         if (sf_medium == null) {
            sf_medium = var0;
         }

         if (sf_medium_mini == null) {
            sf_medium_mini = var0;
         }

         if (sf_medium_modules == null) {
            sf_medium_modules = var0;
         }

         if (sf_bold_mini == null) {
            sf_bold_mini = var0;
         }

         if (sf_bold_micro == null) {
            sf_bold_micro = var0;
         }

         if (icons == null) {
            icons = var0;
         }

         if (mid_icons == null) {
            mid_icons = var0;
         }
      }
   }
}
