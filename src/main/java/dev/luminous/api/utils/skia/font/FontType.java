package dev.luminous.api.utils.skia.font;

public enum FontType {
   TTF,
   OTF,
   UNKNOWN;

   public static FontType fromString(String value) {
      String var1 = value == null ? "" : value.toLowerCase();

      return switch (var1) {
         case "ttf" -> TTF;
         case "otf" -> OTF;
         default -> UNKNOWN;
      };
   }
}
