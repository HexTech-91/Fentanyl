package dev.luminous.api.utils.i18n;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import net.minecraft.client.MinecraftClient;

public final class LangUtil {
   private LangUtil() {
   }

   public static boolean isChinese() {
      try {
         String var0 = MinecraftClient.getInstance().options.fullscreenKey.getTranslationKey();
         return var0 != null && var0.toLowerCase().startsWith("zh");
      } catch (Throwable var1) {
         return true;
      }
   }

   public static String ui(String var0) {
      return var0;
   }

   public static String setting(String var0) {
      return FeatureTranslations.translateSetting(var0);
   }

   public static String enumValue(Enum<?> var0) {
      return FeatureTranslations.translateEnum(var0);
   }

   public static String category(Module.Category var0) {
      switch (var0) {
         case Combat:
            return "\u6218\u6597";
         case Misc:
            return "\u6742\u9879";
         case Render:
            return "\u6e32\u67d3";
         case Movement:
            return "\u79fb\u52a8";
         case Player:
            return "\u73a9\u5bb6";
         case Client:
            return "\u5ba2\u6237\u7aef";
         case HUD:
            return "\u754c\u9762";
         default:
            return var0.toString();
      }
   }
}
