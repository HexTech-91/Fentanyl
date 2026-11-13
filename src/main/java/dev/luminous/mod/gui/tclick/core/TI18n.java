package dev.luminous.mod.gui.tclick.core;

import dev.luminous.api.utils.i18n.LangUtil;
import dev.luminous.mod.modules.Module;

public final class TI18n {
   private TI18n() {
   }

   public static String setting(String var0) {
      try {
         String var1 = LangUtil.setting(var0);
         return var1 != null && !var1.isEmpty() ? var1 : var0;
      } catch (Throwable var2) {
         return var0;
      }
   }

   public static String enumValue(Enum<?> var0) {
      try {
         String var1 = LangUtil.enumValue(var0);
         return var1 != null && !var1.isEmpty() ? var1 : var0.name();
      } catch (Throwable var2) {
         return var0.name();
      }
   }

   public static String category(Module.Category var0) {
      try {
         String var1 = LangUtil.category(var0);
         return var1 != null && !var1.isEmpty() ? var1 : var0.name();
      } catch (Throwable var2) {
         return var0.name();
      }
   }

   public static String ui(String var0) {
      try {
         String var1 = LangUtil.ui(var0);
         return var1 != null && !var1.isEmpty() ? var1 : var0;
      } catch (Throwable var2) {
         return var0;
      }
   }
}
