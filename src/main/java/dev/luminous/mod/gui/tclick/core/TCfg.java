package dev.luminous.mod.gui.tclick.core;

public final class TCfg {
   public static int moduleWidth = 100;
   public static int moduleHeight = 14;
   public static int catHeight = 300;
   public static int gradientMode = 1;
   public static boolean gearEnabled = true;
   public static int gearScale = 40;
   public static int textSide = 0;
   public static boolean descriptions = true;
   public static boolean tips = true;
   public static boolean clickGuiBlur = true;
   public static int scrollMode = 1;
   public static boolean closeAnimation = true;
   public static boolean soundEnabled = true;
   public static float soundVolume = 1.0F;
   public static TImageMode imageMode = TImageMode.None;

   private TCfg() {
   }

   public static int getModuleWidth() {
      return moduleWidth;
   }

   public static int getModuleHeight() {
      return moduleHeight;
   }

   public static int getCatHeight() {
      return catHeight;
   }

   public static int getGradientMode() {
      return gradientMode;
   }

   public static boolean isGearEnabled() {
      return gearEnabled;
   }

   public static int getGearScale() {
      return gearScale;
   }

   public static int getTextSide() {
      return textSide;
   }

   public static boolean isDescriptions() {
      return descriptions;
   }

   public static boolean isTips() {
      return tips;
   }

   public static boolean isClickGuiBlur() {
      return clickGuiBlur;
   }

   public static int getScrollMode() {
      return scrollMode;
   }

   public static boolean isCloseAnimation() {
      return closeAnimation;
   }

   public static boolean isSoundEnabled() {
      return soundEnabled;
   }

   public static float getSoundVolume() {
      return soundVolume;
   }
}
