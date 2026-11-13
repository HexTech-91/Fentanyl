package dev.luminous.mod.modules.impl.hud;

import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class NotificationHud extends Module {
   public static NotificationHud INSTANCE;
   private static final HashMap<Integer, FontRenderer> fontCache = new HashMap<>();
   public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.05));
   public final SliderSetting x = this.add(new SliderSetting("X", 10, 0, 1920));
   public final SliderSetting y = this.add(new SliderSetting("Y", 10, 0, 1080));
   private final ColorSetting enableColor = this.add(new ColorSetting("Enable Color", new Color(48, 226, 178, 255)));
   private final ColorSetting disableColor = this.add(new ColorSetting("Disable Color", new Color(220, 50, 50, 255)));
   private final ColorSetting infoColor = this.add(new ColorSetting("Info Color", new Color(99, 150, 145, 255)));
   private final ColorSetting bgColor = this.add(new ColorSetting("Bg Color", new Color(20, 20, 20, 180)));
   private final ColorSetting titleColor = this.add(new ColorSetting("Title Color", new Color(255, 255, 255, 255)));
   private final ColorSetting msgColor = this.add(new ColorSetting("Msg Color", new Color(181, 181, 181, 255)));
   private final SliderSetting textOffset = this.add(new SliderSetting("Text Offset", 0.0, -10.0, 10.0, 1.0));
   private final List<NotificationHud.Notification> notifications = new CopyOnWriteArrayList<>();

   public NotificationHud() {
      super("NotificationHud", "Module toggle message notification HUD", Module.Category.HUD);
      this.setChinese("\u901a\u77e5\u6d88\u606f");
      INSTANCE = this;
   }

   public static float getCardWidth() {
      return 170.0F * INSTANCE.scale.getValueFloat();
   }

   public static float getCardHeight() {
      return 36.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getAccentWidth() {
      return 2.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getPaddingLeft() {
      return 10.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getLineGap() {
      return 2.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getCardSpacing() {
      return 4.0F * INSTANCE.scale.getValueFloat();
   }

   private static float getTitleFontSize() {
      return 8.5F * INSTANCE.scale.getValueFloat();
   }

   private static float getMsgFontSize() {
      return 9.0F * INSTANCE.scale.getValueFloat();
   }

   private static FontRenderer getNotificationFont(float var0) {
      int var1 = Math.round(var0);
      FontRenderer var2 = fontCache.get(var1);
      if (var2 == null) {
         try {
            var2 = FontManager.assets(var0, "default", 0, "msyh");
         } catch (Exception var4) {
            var2 = FontManager.ui;
         }

         fontCache.put(var1, var2);
      }

      return var2;
   }

   public static void send(String var0, String var1, NotificationHud.NotificationType var2) {
      if (INSTANCE != null && INSTANCE.isOn()) {
         INSTANCE.notifications.add(new NotificationHud.Notification(var0, var1, var2));

         while (INSTANCE.notifications.size() > 12) {
            INSTANCE.notifications.remove(0);
         }
      }
   }

   public static void onToggle(Module var0, boolean var1) {
      if (INSTANCE != null && INSTANCE.isOn() && !Module.nullCheck()) {
         send(
            var0.getCategory().name(),
            var0.getDisplayName() + (var1 ? " Enabled" : " Disabled"),
            var1 ? NotificationHud.NotificationType.ENABLE : NotificationHud.NotificationType.DISABLE
         );
      }
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (!this.notifications.isEmpty()) {
         float var3 = this.y.getValueInt();

         for (NotificationHud.Notification var5 : this.notifications) {
            var5.updateY(var3);
            if (!var5.isDead) {
               var3 += (getCardHeight() + getCardSpacing()) * var5.getHeightProgress();
            }
         }

         for (NotificationHud.Notification var7 : this.notifications) {
            var7.render(var1);
         }

         this.notifications.removeIf(var0 -> var0.isDead);
      }
   }

   private static class Notification {
      private final String title;
      private final String message;
      private final NotificationHud.NotificationType type;
      private final long startTime;
      private float renderY = -1.0F;
      private float heightProgress = 1.0F;
      public boolean isDead = false;

      public Notification(String var1, String var2, NotificationHud.NotificationType var3) {
         this.title = var1;
         this.message = var2;
         this.type = var3;
         this.startTime = System.currentTimeMillis();
      }

      public float getHeightProgress() {
         return this.heightProgress;
      }

      public void updateY(float var1) {
         if (this.renderY < 0.0F) {
            this.renderY = var1;
         }

         this.renderY = AnimateUtil.fast(var1, this.renderY, 16.0F);
      }

      public void render(DrawContext var1) {
         if (NotificationHud.INSTANCE != null) {
            long var2 = System.currentTimeMillis() - this.startTime;
            long var4 = 300L;
            long var6 = 500L;
            long var8 = 1500L;
            long var10 = var8 + 200L;
            long var12 = var8 + 200L + 300L;
            if (var2 > var12) {
               this.isDead = true;
            } else {
               int var14 = NotificationHud.INSTANCE.x.getValueInt();
               int var15 = Math.round(this.renderY);
               if (var2 <= var4) {
                  float var16 = (float)var2 / 300.0F;
                  float var17 = (float)Easing.CubicOut.ease(var16);
                  this.heightProgress = var17;
                  this.renderStage1(var1, var14, var15, var17);
               } else if (var2 <= var6) {
                  float var18 = (float)(var2 - var4) / 200.0F;
                  float var21 = (float)Easing.CubicOut.ease(var18);
                  this.heightProgress = 1.0F;
                  this.renderStage2(var1, var14, var15, var21);
               } else if (var2 <= var8) {
                  this.heightProgress = 1.0F;
                  this.renderStage3(var1, var14, var15);
               } else if (var2 <= var10) {
                  float var19 = (float)(var2 - var8) / 200.0F;
                  float var22 = (float)Easing.CubicOut.ease(var19);
                  this.heightProgress = 1.0F;
                  this.renderStage2(var1, var14, var15, 1.0F - var22);
               } else {
                  float var20 = (float)(var2 - var10) / 300.0F;
                  float var23 = (float)Easing.CubicOut.ease(var20);
                  this.heightProgress = 1.0F - var23;
                  this.renderStage1(var1, var14, var15, 1.0F - var23);
               }
            }
         }
      }

      private void renderStage1(DrawContext var1, int var2, int var3, float var4) {
         Color var5 = this.getAccentColor();
         MatrixStack var6 = var1.getMatrices();
         float var7 = NotificationHud.getCardWidth();
         float var8 = NotificationHud.getCardHeight();
         float var9 = var7 * var4;
         Render2DUtil.drawRect(var6, (float)var2, (float)var3, var9, var8, var5);
      }

      private void renderStage2(DrawContext var1, int var2, int var3, float var4) {
         Color var5 = NotificationHud.INSTANCE.bgColor.getValue();
         Color var6 = this.getAccentColor();
         MatrixStack var7 = var1.getMatrices();
         float var8 = NotificationHud.getCardWidth();
         float var9 = NotificationHud.getCardHeight();
         float var10 = NotificationHud.getAccentWidth();
         Render2DUtil.drawHudBase(var7, (float)var2, (float)var3, var8, var9);
         int var11 = (int)(255.0F * var4);
         if (var11 > 4) {
            this.drawNotifText(var1, var2, var3, var11);
         }

         float var12 = var2 + (var8 - var10) * var4;
         float var13 = var8 - (var12 - var2);
         Render2DUtil.drawRect(var7, var12, (float)var3, var13, var9, var6);
      }

      private void renderStage3(DrawContext var1, int var2, int var3) {
         Color var4 = NotificationHud.INSTANCE.bgColor.getValue();
         Color var5 = this.getAccentColor();
         MatrixStack var6 = var1.getMatrices();
         float var7 = NotificationHud.getCardWidth();
         float var8 = NotificationHud.getCardHeight();
         float var9 = NotificationHud.getAccentWidth();
         Render2DUtil.drawHudBase(var6, (float)var2, (float)var3, var7, var8);
         Render2DUtil.drawRect(var6, var2 + var7 - var9, (float)var3, var9, var8, var5);
         this.drawNotifText(var1, var2, var3, 255);
      }

      private void drawNotifText(DrawContext var1, int var2, int var3, int var4) {
         int var5 = NotificationHud.INSTANCE.textOffset.getValueInt();
         float var6 = NotificationHud.getTitleFontSize();
         float var7 = NotificationHud.getMsgFontSize();
         FontRenderer var8 = NotificationHud.getNotificationFont(var6);
         FontRenderer var9 = NotificationHud.getNotificationFont(var7);
         float var10 = var8.getFontHeight("A");
         float var11 = var9.getFontHeight("A");
         float var12 = NotificationHud.getLineGap();
         float var13 = var10 + var12 + var11;
         float var14 = NotificationHud.getCardHeight();
         float var15 = NotificationHud.getPaddingLeft();
         float var16 = var3 + (var14 - var13) * 0.5F + var5;
         float var17 = var2 + var15;
         Color var18 = NotificationHud.INSTANCE.titleColor.getValue();
         Color var19 = NotificationHud.INSTANCE.msgColor.getValue();
         float var20 = var4 / 255.0F * (var18.getAlpha() / 255.0F);
         float var21 = var4 / 255.0F * (var19.getAlpha() / 255.0F);
         MatrixStack var22 = var1.getMatrices();
         var8.drawString(
            var22, this.title, var17, Math.round(var16), var18.getRed() / 255.0F, var18.getGreen() / 255.0F, var18.getBlue() / 255.0F, var20, false
         );
         var9.drawString(
            var22,
            this.message,
            var17,
            Math.round(var16 + var10 + var12),
            var19.getRed() / 255.0F,
            var19.getGreen() / 255.0F,
            var19.getBlue() / 255.0F,
            var21,
            false
         );
      }

      private Color getAccentColor() {
         if (NotificationHud.INSTANCE == null) {
            return new Color(100, 200, 100);
         } else {
            switch (this.type) {
               case ENABLE:
                  return NotificationHud.INSTANCE.enableColor.getValue();
               case DISABLE:
                  return NotificationHud.INSTANCE.disableColor.getValue();
               default:
                  return NotificationHud.INSTANCE.infoColor.getValue();
            }
         }
      }
   }

   public static enum NotificationType {
      ENABLE,
      DISABLE,
      INFO;
   }
}
