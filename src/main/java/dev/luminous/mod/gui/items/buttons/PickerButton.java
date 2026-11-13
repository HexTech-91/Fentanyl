package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class PickerButton extends Button {
   private final ColorSetting setting;
   private final Animation toggleAnimation = new Animation();
   private final Animation openAnimation = new Animation();
   private boolean open;
   private boolean draggingHue;
   private boolean draggingSatBr;
   private float selectedHue;
   private float selectedSat;
   private float selectedBr;
   private float pickerHue;
   private float pickerSat;
   private float pickerBr;
   private boolean draggingRainbow = false;
   private float rainbowX = 0.0F;
   private final Animation rainbowSpeedAnimation = new Animation();
   private final Animation rainbowSaturationAnimation = new Animation();
   private final Animation rainbowBrightnessAnimation = new Animation();
   private final Animation rainbowAlphaAnimation = new Animation();
   private boolean draggingRainbowSpeed = false;
   private boolean draggingRainbowSaturation = false;
   private boolean draggingRainbowBrightness = false;
   private boolean draggingRainbowAlpha = false;
   private final Animation speedAnimation = new Animation();
   private final Animation saturationAnimation = new Animation();
   private final Animation brightnessAnimation = new Animation();
   private final Animation alphaAnimation = new Animation();
   private boolean draggingSpeed = false;
   private boolean draggingSaturation = false;
   private boolean draggingBrightness = false;
   private boolean draggingAlpha = false;

   public PickerButton(ColorSetting var1) {
      super(var1.getDisplayName());
      this.setting = var1;
      if (this.setting.getValue() != null && (this.setting.getValue().getAlpha() != 255 || this.setting.rainbowAlpha >= 0.9999)) {
         this.setting.rainbowAlpha = this.setting.getValue().getAlpha() / 255.0;
      }

      double var2;
      this.toggleAnimation.from = var2 = var1.getValue() != null ? 1.0 : 0.0;
      this.toggleAnimation.to = var2;
      float[] var4 = Color.RGBtoHSB(this.setting.getValue().getRed(), this.setting.getValue().getGreen(), this.setting.getValue().getBlue(), null);
      this.selectedHue = var4[0];
      this.selectedSat = var4[1];
      this.selectedBr = var4[2];
      this.pickerHue = this.selectedHue;
      this.pickerSat = this.selectedSat;
      this.pickerBr = this.selectedBr;
   }

   @Override
   public int getHeight() {
      return this.open ? this.height + 95 + 75 : this.height;
   }

   @Override
   public void drawScreen(DrawContext var1, int var2, int var3, float var4) {
      this.dragRainbow(var2, var3);
      this.dragRainbowSpeed(var2, var3);
      this.dragRainbowSaturation(var2, var3);
      this.dragRainbowBrightness(var2, var3);
      this.dragRainbowAlpha(var2, var3);
      this.dragSpeed(var2, var3);
      this.dragSaturation(var2, var3);
      this.dragBrightness(var2, var3);
      this.dragAlpha(var2, var3);
      boolean var5 = this.isHovering(var2, var3);
      double var6 = this.toggleAnimation.get(this.getState() ? 1.0 : 0.0, 160L, Easing.CubicInOut);
      double var8 = this.getColorDelay();
      int var10 = var5 ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
      Color var11 = new Color(var5 ? hoverColor : defaultColor, true);
      float var12 = this.width + 7.0F;
      float var13 = this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         if (var6 >= 0.999) {
            Render2DUtil.drawLutRect(
               var1.getMatrices(),
               this.x,
               this.y,
               var12,
               var13,
               ClickGui.getInstance().getSpectrumLutId(),
               ClickGui.getInstance().getSpectrumLutHeight(),
               var10
            );
         } else {
            Render2DUtil.rect(var1.getMatrices(), this.x, this.y, this.x + var12, this.y + var13, var11.getRGB());
            int var14 = (int)Math.round(var10 * var6);
            if (var14 > 0) {
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  this.x,
                  this.y,
                  var12,
                  var13,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var14
               );
            }
         }
      } else {
         Color var44 = ClickGui.getInstance().getColor(var8);
         Color var15 = new Color(var44.getRed(), var44.getGreen(), var44.getBlue(), var10);
         Color var16 = ColorUtil.fadeColor(var11, var15, var6);
         Render2DUtil.rect(var1.getMatrices(), this.x, this.y, this.x + var12, this.y + this.height - 0.5F, var16.getRGB());
      }

      float var45 = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (var5 && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString("Reset Default", this.x + 2.3F, var45, enableTextColor);
      } else {
         this.drawString(this.setting.getDisplayName(), this.x + 2.3F, var45, this.getState() ? enableTextColor : defaultTextColor);
      }

      float var46 = 8.0F;
      float var47 = this.x + this.width - var46 + 2.0F;
      float var17 = this.y + (this.height - 0.5F - var46) / 2.0F;
      Render2DUtil.rect(var1.getMatrices(), var47, var17, var47 + var46, var17 + var46, this.setting.getValue().getRGB());
      if (this.open) {
         double var18 = this.openAnimation.get(1.0, 150L, Easing.CubicInOut);
         float var20 = this.x + 1.0F;
         float var21 = this.y + this.height + 1.0F;
         float var22 = this.width + 5.0F;
         float var23 = 95.0F;
         float var24 = var23 * (float)var18;
         Render2DUtil.rect(var1.getMatrices(), var20, var21, var20 + var22, var21 + var24, new Color(20, 20, 20, 220).getRGB());
         if (var18 > 0.01F) {
            float var25 = Math.min(var22 * 0.65F, var24);
            float var26 = var20 + 3.0F;
            float var27 = var21 + 12.0F;
            this.drawSatBrRect(var1, var26, var27, var25, var25, this.pickerHue);
            float var28 = var26 + this.pickerSat * var25;
            float var29 = var27 + (1.0F - this.pickerBr) * var25;
            float var30 = 3.0F;
            Render2DUtil.drawCircle(var1.getMatrices(), var28, var29, var30, Color.WHITE, 12);
            Render2DUtil.drawCircle(var1.getMatrices(), var28, var29, var30 - 1.0F, Color.BLACK, 12);
            float var32 = var21 + 3.0F;
            float var33 = var22 - 6.0F;
            float var34 = 7.0F;
            this.drawHueBarHorizontal(var1, var26, var32, var33, var34);
            float var35 = var26 + this.pickerHue * var33;
            float var36 = 2.0F;
            Render2DUtil.rect(var1.getMatrices(), var35 - 2.0F, var32 - 1.0F, var35 + 2.0F, var32 + var34 + 1.0F, -1);
            if (this.setting.injectBoolean) {
               float var37 = var27 + var25 + 5.0F;
               float var38 = 12.0F;
               float var39 = 8.0F;
               Render2DUtil.rect(
                  var1.getMatrices(),
                  var20 + 3.0F,
                  var37,
                  var20 + 3.0F + var39,
                  var37 + var39,
                  this.setting.booleanValue ? this.setting.getValue().getRGB() : new Color(60, 60, 60).getRGB()
               );
               this.drawString(
                  this.label("Rainbow", "\u5f69\u8679"),
                  var20 + 3.0F + var39 + 4.0F,
                  var37 + (var39 - this.getFontHeight()) / 2.0F,
                  new Color(200, 200, 200).getRGB()
               );
               if (this.setting.booleanValue && var18 > 0.9F) {
                  float var40 = var37 + var38 + 2.0F;
                  float var41 = 10.0F;
                  float var42 = var22 - 6.0F;
                  float var43 = 40.0F;
                  this.drawRainbowSlider(
                     var1,
                     this.label("Hue", "\u8272\u76f8"),
                     var20 + 3.0F,
                     var40,
                     var43,
                     var42,
                     var41,
                     this.setting.rainbowSpeed,
                     this.draggingRainbowSpeed,
                     this.rainbowSpeedAnimation,
                     var2,
                     var3
                  );
                  this.drawRainbowSlider(
                     var1,
                     this.label("Sat", "\u9971\u548c\u5ea6"),
                     var20 + 3.0F,
                     var40 + var41 + 2.0F,
                     var43,
                     var42,
                     var41,
                     this.setting.rainbowSaturation,
                     this.draggingRainbowSaturation,
                     this.rainbowSaturationAnimation,
                     var2,
                     var3
                  );
                  this.drawRainbowSlider(
                     var1,
                     this.label("Br", "\u4eae\u5ea6"),
                     var20 + 3.0F,
                     var40 + (var41 + 2.0F) * 2.0F,
                     var43,
                     var42,
                     var41,
                     this.setting.rainbowBrightness,
                     this.draggingRainbowBrightness,
                     this.rainbowBrightnessAnimation,
                     var2,
                     var3
                  );
                  this.drawAlphaSlider(
                     var1,
                     this.label("Alpha", "\u900f\u660e\u5ea6"),
                     var20 + 3.0F,
                     var40 + (var41 + 2.0F) * 3.0F,
                     var43,
                     var42,
                     var41,
                     this.setting.rainbowAlpha,
                     this.draggingRainbowAlpha,
                     this.rainbowAlphaAnimation,
                     var2,
                     var3
                  );
               }
            } else {
               float var48 = var27 + var25 + 5.0F;
               float var49 = 10.0F;
               float var50 = var22 - 6.0F;
               float var51 = 40.0F;
               this.drawSlider(
                  var1,
                  this.label("Speed", "\u901f\u5ea6"),
                  var20 + 3.0F,
                  var48,
                  var51,
                  var50,
                  var49,
                  this.setting.rainbowSpeed,
                  this.draggingSpeed,
                  this.speedAnimation,
                  var2,
                  var3
               );
               this.drawSlider(
                  var1,
                  this.label("Sat", "\u9971\u548c\u5ea6"),
                  var20 + 3.0F,
                  var48 + var49 + 2.0F,
                  var51,
                  var50,
                  var49,
                  this.setting.rainbowSaturation,
                  this.draggingSaturation,
                  this.saturationAnimation,
                  var2,
                  var3
               );
               this.drawSlider(
                  var1,
                  this.label("Br", "\u4eae\u5ea6"),
                  var20 + 3.0F,
                  var48 + (var49 + 2.0F) * 2.0F,
                  var51,
                  var50,
                  var49,
                  this.setting.rainbowBrightness,
                  this.draggingBrightness,
                  this.brightnessAnimation,
                  var2,
                  var3
               );
               this.drawAlphaSlider(
                  var1,
                  this.label("Alpha", "\u900f\u660e\u5ea6"),
                  var20 + 3.0F,
                  var48 + (var49 + 2.0F) * 3.0F,
                  var51,
                  var50,
                  var49,
                  this.setting.rainbowAlpha,
                  this.draggingAlpha,
                  this.alphaAnimation,
                  var2,
                  var3
               );
            }
         }
      }
   }

   private void drawSatBrRect(DrawContext var1, float var2, float var3, float var4, float var5, float var6) {
      for (int var7 = 0; var7 < var4; var7++) {
         for (int var8 = 0; var8 < var5; var8++) {
            float var9 = var7 / var4;
            float var10 = 1.0F - var8 / var5;
            Color var11 = Color.getHSBColor(var6, var9, var10);
            Render2DUtil.rect(var1.getMatrices(), var2 + var7, var3 + var8, var2 + var7 + 1.0F, var3 + var8 + 1.0F, var11.getRGB());
         }
      }
   }

   private void drawHueBar(DrawContext var1, float var2, float var3, float var4, float var5) {
      for (int var6 = 0; var6 < var5; var6++) {
         float var7 = var6 / var5;
         Color var8 = Color.getHSBColor(var7, 1.0F, 1.0F);
         Render2DUtil.rect(var1.getMatrices(), var2, var3 + var6, var2 + var4, var3 + var6 + 1.0F, var8.getRGB());
      }
   }

   private void drawHueBarHorizontal(DrawContext var1, float var2, float var3, float var4, float var5) {
      for (int var6 = 0; var6 < var4; var6++) {
         float var7 = var6 / Math.max(1.0F, var4 - 1.0F);
         Color var8 = Color.getHSBColor(var7, 1.0F, 1.0F);
         Render2DUtil.rect(var1.getMatrices(), var2 + var6, var3, var2 + var6 + 1.0F, var3 + var5, var8.getRGB());
      }
   }

   private void drawRainbowSlider(
      DrawContext var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      double var8,
      boolean var10,
      Animation var11,
      int var12,
      int var13
   ) {
      this.drawString(var2, var3, var4 + (var7 - this.getFontHeight()) / 2.0F, new Color(180, 180, 180).getRGB());
      float var14 = var3 + var5 + 4.0F;
      Render2DUtil.rect(var1.getMatrices(), var14, var4, var14 + var6, var4 + var7, new Color(40, 40, 40).getRGB());
      double var15 = var11.get(var8, 200L, Easing.CubicInOut);
      float var17 = (float)(var15 * var6);
      Render2DUtil.rect(var1.getMatrices(), var14, var4, var14 + var17, var4 + var7, ColorUtil.injectAlpha(this.setting.getValue(), 200).getRGB());
      float var18 = var14 + var17 - 2.0F;
      Render2DUtil.rect(var1.getMatrices(), var18, var4, var18 + 4.0F, var4 + var7, -1);
      if (var10) {
         double var19 = var12 - var14;
         double var21 = MathHelper.clamp(var19 / var6, 0.0, 1.0);
         if (var2.equals("Hue") || var2.equals("\u8272\u76f8")) {
            this.setting.rainbowSpeed = var21;
         } else if (var2.equals("Sat") || var2.equals("\u9971\u548c\u5ea6")) {
            this.setting.rainbowSaturation = var21;
         } else if (var2.equals("Br") || var2.equals("\u4eae\u5ea6")) {
            this.setting.rainbowBrightness = var21;
         } else if (var2.equals("Alpha") || var2.equals("\u900f\u660e\u5ea6")) {
            this.applyAlpha(var21);
         }
      }
   }

   private void drawAlphaSlider(
      DrawContext var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      double var8,
      boolean var10,
      Animation var11,
      int var12,
      int var13
   ) {
      this.drawString(var2, var3, var4 + (var7 - this.getFontHeight()) / 2.0F, new Color(200, 200, 200).getRGB());
      float var14 = var3 + var5 + 4.0F;
      byte var15 = 4;
      int var16 = new Color(52, 52, 52).getRGB();
      int var17 = new Color(92, 92, 92).getRGB();

      for (byte var18 = 0; var18 < var6; var18 += var15) {
         for (byte var19 = 0; var19 < var7; var19 += var15) {
            boolean var20 = (var18 / var15 + var19 / var15) % 2 == 0;
            float var21 = Math.min(var14 + var18 + var15, var14 + var6);
            float var22 = Math.min(var4 + var19 + var15, var4 + var7);
            Render2DUtil.rect(var1.getMatrices(), var14 + var18, var4 + var19, var21, var22, var20 ? var17 : var16);
         }
      }

      Color var23 = this.setting.getValue();
      if (var23 != null) {
         for (int var24 = 0; var24 < var6; var24++) {
            int var26 = (int)Math.round((double)var24 / Math.max(1.0F, var6 - 1.0F) * 255.0);
            Render2DUtil.rect(
               var1.getMatrices(),
               var14 + var24,
               var4,
               var14 + var24 + 1.0F,
               var4 + var7,
               new Color(var23.getRed(), var23.getGreen(), var23.getBlue(), var26).getRGB()
            );
         }
      }

      double var25 = MathHelper.clamp(var8, 0.0, 1.0);
      float var27 = var14 + (float)(var25 * var6) - 2.5F;
      Render2DUtil.rect(var1.getMatrices(), var27 - 1.0F, var4 - 1.0F, var27 + 6.0F, var4 + var7 + 1.0F, new Color(255, 255, 255, 220).getRGB());
      Render2DUtil.rect(var1.getMatrices(), var27, var4, var27 + 4.0F, var4 + var7, new Color(35, 35, 35, 230).getRGB());
      String var28 = Integer.toString((int)Math.round(var25 * 100.0)) + "%";
      this.drawString(var28, var14 + var6 - this.getWidth(var28) - 3.0F, var4 + (var7 - this.getFontHeight()) / 2.0F, Color.WHITE.getRGB());
      if (var10) {
         this.updateAlphaFromMouse(var12, var14, var6);
      }
   }

   private void updateAlphaFromMouse(int var1, float var2, float var3) {
      double var4 = MathHelper.clamp((var1 - var2) / var3, 0.0, 1.0);
      this.applyAlpha(var4);
   }

   private void drawSlider(
      DrawContext var1,
      String var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      double var8,
      boolean var10,
      Animation var11,
      int var12,
      int var13
   ) {
      this.drawString(var2, var3, var4 + (var7 - this.getFontHeight()) / 2.0F, new Color(180, 180, 180).getRGB());
      float var14 = var3 + var5 + 4.0F;
      Render2DUtil.rect(var1.getMatrices(), var14, var4, var14 + var6, var4 + var7, new Color(40, 40, 40).getRGB());
      double var15 = var11.get(var8, 200L, Easing.CubicInOut);
      float var17 = (float)(var15 * var6);
      Render2DUtil.rect(var1.getMatrices(), var14, var4, var14 + var17, var4 + var7, ColorUtil.injectAlpha(this.setting.getValue(), 200).getRGB());
      float var18 = var14 + var17 - 2.0F;
      Render2DUtil.rect(var1.getMatrices(), var18, var4, var18 + 4.0F, var4 + var7, -1);
      if (var10) {
         double var19 = var12 - var14;
         double var21 = MathHelper.clamp(var19 / var6, 0.0, 1.0);
         if (var2.equals("Speed") || var2.equals("\u901f\u5ea6")) {
            this.setting.rainbowSpeed = var21;
         } else if (var2.equals("Sat") || var2.equals("\u9971\u548c\u5ea6")) {
            this.setting.rainbowSaturation = var21;
         } else if (var2.equals("Br") || var2.equals("\u4eae\u5ea6")) {
            this.setting.rainbowBrightness = var21;
         } else if (var2.equals("Alpha") || var2.equals("\u900f\u660e\u5ea6")) {
            this.applyAlpha(var21);
         }
      }
   }

   private void applyPickerColor() {
      Color var1 = Color.getHSBColor(this.pickerHue, this.pickerSat, this.pickerBr);
      double var2 = MathHelper.clamp(this.setting.rainbowAlpha, 0.0, 1.0);
      int var4 = (int)Math.round(var2 * 255.0);
      this.setting.setValue(new Color(var1.getRed(), var1.getGreen(), var1.getBlue(), var4));
   }

   private void applyAlpha(double var1) {
      double var3 = MathHelper.clamp(var1, 0.0, 1.0);
      this.setting.rainbowAlpha = var3;
      Color var5 = this.setting.getValue();
      if (var5 != null) {
         this.setting.setValue(new Color(var5.getRed(), var5.getGreen(), var5.getBlue(), (int)Math.round(var3 * 255.0)));
      }
   }

   private void dragRainbow(int var1, int var2) {
      if (this.open && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var3 = this.y + this.height + 1.0F;
         float var4 = this.width + 5.0F;
         float var5 = 95.0F;
         float var6 = Math.min(var4 * 0.65F, var5);
         float var7 = this.x + 1.0F + 3.0F;
         float var8 = var3 + 12.0F;
         float var10 = var3 + 3.0F;
         float var11 = var4 - 6.0F;
         float var12 = 7.0F;
         if (var2 >= var8 && var2 <= var8 + var6 && var1 >= var7 && var1 <= var7 + var6) {
            this.pickerSat = MathHelper.clamp((var1 - var7) / var6, 0.0F, 1.0F);
            this.pickerBr = 1.0F - MathHelper.clamp((var2 - var8) / var6, 0.0F, 1.0F);
            this.applyPickerColor();
            this.setting.setSync(false);
            if (this.setting.injectBoolean) {
               this.setting.setRainbow(false);
            }
         }

         if (var2 >= var10 && var2 <= var10 + var12 && var1 >= var7 && var1 <= var7 + var11) {
            this.pickerHue = MathHelper.clamp((var1 - var7) / var11, 0.0F, 1.0F);
            this.applyPickerColor();
            this.setting.setSync(false);
            if (this.setting.injectBoolean) {
               this.setting.setRainbow(false);
            }
         }
      }
   }

   private void dragRainbowSpeed(int var1, int var2) {
      if (this.open && this.draggingRainbowSpeed && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var11 + 12.0F + 2.0F;
         float var13 = var6 - 6.0F;
         float var14 = 40.0F;
         float var15 = var9 + var14 + 4.0F;
         float var16 = 10.0F;
         this.setting.rainbowSpeed = MathHelper.clamp((var1 - var15) / var13, 0.0, 1.0);
      }
   }

   private void dragRainbowSaturation(int var1, int var2) {
      if (this.open && this.draggingRainbowSaturation && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var11 + 12.0F + 2.0F;
         float var13 = 10.0F;
         float var14 = var6 - 6.0F;
         float var15 = 40.0F;
         float var16 = var9 + var15 + 4.0F;
         this.setting.rainbowSaturation = MathHelper.clamp((var1 - var16) / var14, 0.0, 1.0);
      }
   }

   private void dragRainbowBrightness(int var1, int var2) {
      if (this.open && this.draggingRainbowBrightness && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var11 + 12.0F + 2.0F;
         float var13 = 10.0F;
         float var14 = var6 - 6.0F;
         float var15 = 40.0F;
         float var16 = var9 + var15 + 4.0F;
         this.setting.rainbowBrightness = MathHelper.clamp((var1 - var16) / var14, 0.0, 1.0);
      }
   }

   private void dragRainbowAlpha(int var1, int var2) {
      if (this.open && this.draggingRainbowAlpha && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var11 + 12.0F + 2.0F;
         float var13 = 10.0F;
         float var14 = var6 - 6.0F;
         float var15 = 40.0F;
         float var16 = var9 + var15 + 4.0F;
         double var3 = MathHelper.clamp((var1 - var16) / var14, 0.0, 1.0);
         this.applyAlpha(var3);
      }
   }

   private void dragSpeed(int var1, int var2) {
      if (this.open && this.draggingSpeed && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var6 - 6.0F;
         float var13 = 40.0F;
         float var14 = var9 + var13 + 4.0F;
         this.setting.rainbowSpeed = MathHelper.clamp((var1 - var14) / var12, 0.0, 1.0);
      }
   }

   private void dragSaturation(int var1, int var2) {
      if (this.open && this.draggingSaturation && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var6 - 6.0F;
         float var13 = 40.0F;
         float var14 = var9 + var13 + 4.0F;
         this.setting.rainbowSaturation = MathHelper.clamp((var1 - var14) / var12, 0.0, 1.0);
      }
   }

   private void dragBrightness(int var1, int var2) {
      if (this.open && this.draggingBrightness && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var6 - 6.0F;
         float var13 = 40.0F;
         float var14 = var9 + var13 + 4.0F;
         this.setting.rainbowBrightness = MathHelper.clamp((var1 - var14) / var12, 0.0, 1.0);
      }
   }

   private void dragAlpha(int var1, int var2) {
      if (this.open && this.draggingAlpha && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         float var5 = this.y + this.height + 1.0F;
         float var6 = this.width + 5.0F;
         float var7 = 95.0F;
         float var8 = Math.min(var6 * 0.65F, var7);
         float var9 = this.x + 1.0F + 3.0F;
         float var10 = var5 + 12.0F;
         float var11 = var10 + var8 + 5.0F;
         float var12 = var6 - 6.0F;
         float var13 = 40.0F;
         float var14 = var9 + var13 + 4.0F;
         double var3 = MathHelper.clamp((var1 - var14) / var12, 0.0, 1.0);
         this.applyAlpha(var3);
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (var3 == 0 && this.isHovering(var1, var2) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.setting.setValue(this.setting.getDefaultValue());
         this.setting.rainbow = this.setting.getDefaultRainbow();
         this.setting.rainbowAlpha = this.setting.getValue().getAlpha() / 255.0;
         if (this.setting.injectBoolean) {
            this.setting.booleanValue = this.setting.getDefaultBooleanValue();
         }

         float[] var22 = Color.RGBtoHSB(this.setting.getValue().getRed(), this.setting.getValue().getGreen(), this.setting.getValue().getBlue(), null);
         this.selectedHue = var22[0];
         this.selectedSat = var22[1];
         this.selectedBr = var22[2];
         this.pickerHue = this.selectedHue;
         this.pickerSat = this.selectedSat;
         this.pickerBr = this.selectedBr;
         sound();
      } else {
         super.mouseClicked(var1, var2, var3);
         if (var3 == 1 && this.isHovering(var1, var2)) {
            boolean var4 = this.open = !this.open;
            if (this.open) {
               float[] var5 = Color.RGBtoHSB(this.setting.getValue().getRed(), this.setting.getValue().getGreen(), this.setting.getValue().getBlue(), null);
               this.selectedHue = var5[0];
               this.selectedSat = var5[1];
               this.selectedBr = var5[2];
               this.pickerHue = this.selectedHue;
               this.pickerSat = this.selectedSat;
               this.pickerBr = this.selectedBr;
            } else {
               this.applyPickerColor();
               this.setting.setSync(false);
            }

            sound();
         }

         if (this.open) {
            float var21 = this.y + this.height + 1.0F;
            float var23 = this.width + 5.0F;
            float var6 = 95.0F;
            float var7 = Math.min(var23 * 0.65F, var6);
            float var8 = this.x + 1.0F + 3.0F;
            float var9 = var21 + 12.0F;
            float var11 = var21 + 3.0F;
            float var12 = var23 - 6.0F;
            float var13 = 7.0F;
            if (var3 == 0) {
               this.draggingRainbow = false;
               this.draggingRainbowSpeed = false;
               this.draggingRainbowSaturation = false;
               this.draggingRainbowBrightness = false;
               this.draggingRainbowAlpha = false;
               this.draggingSpeed = false;
               this.draggingSaturation = false;
               this.draggingBrightness = false;
               this.draggingAlpha = false;
               if (var2 >= var9 && var2 <= var9 + var7 && var1 >= var8 && var1 <= var8 + var7) {
                  this.pickerSat = MathHelper.clamp((var1 - var8) / var7, 0.0F, 1.0F);
                  this.pickerBr = 1.0F - MathHelper.clamp((var2 - var9) / var7, 0.0F, 1.0F);
                  this.applyPickerColor();
                  this.setting.setSync(false);
                  if (this.setting.injectBoolean) {
                     this.setting.setRainbow(false);
                  }
               }

               if (var2 >= var11 && var2 <= var11 + var13 && var1 >= var8 && var1 <= var8 + var12) {
                  this.pickerHue = MathHelper.clamp((var1 - var8) / var12, 0.0F, 1.0F);
                  this.applyPickerColor();
                  this.setting.setSync(false);
                  if (this.setting.injectBoolean) {
                     this.setting.setRainbow(false);
                  }
               }

               if (this.setting.injectBoolean) {
                  float var14 = 8.0F;
                  float var15 = var9 + var7 + 5.0F;
                  if (var1 >= var8
                     && var1 <= var8 + var14 + this.getWidth(this.label("Rainbow", "\u5f69\u8679")) + 4.0F
                     && var2 >= var15
                     && var2 <= var15 + var14) {
                     this.setting.booleanValue = !this.setting.booleanValue;
                     this.setting.setRainbow(this.setting.booleanValue);
                     sound();
                  }

                  float var16 = 10.0F;
                  float var17 = var15 + 12.0F + 2.0F;
                  float var18 = var23 - 6.0F;
                  float var19 = 40.0F;
                  float var20 = var8 + var19 + 4.0F;
                  if (var2 >= var17 && var2 <= var17 + var16 && var1 >= var20 && var1 <= var20 + var18) {
                     this.draggingRainbowSpeed = true;
                     this.rainbowX = var1 - var20;
                  }

                  if (var2 >= var17 + var16 + 2.0F && var2 <= var17 + (var16 + 2.0F) * 2.0F && var1 >= var20 && var1 <= var20 + var18) {
                     this.draggingRainbowSaturation = true;
                  }

                  if (var2 >= var17 + (var16 + 2.0F) * 2.0F && var2 <= var17 + (var16 + 2.0F) * 3.0F && var1 >= var20 && var1 <= var20 + var18) {
                     this.draggingRainbowBrightness = true;
                  }

                  if (var2 >= var17 + (var16 + 2.0F) * 3.0F && var2 <= var17 + (var16 + 2.0F) * 4.0F && var1 >= var20 && var1 <= var20 + var18) {
                     this.draggingRainbowAlpha = true;
                     this.updateAlphaFromMouse(var1, var20, var18);
                  }
               }
            } else {
               float var24 = var9 + var7 + 5.0F;
               float var25 = 10.0F;
               float var26 = var23 - 6.0F;
               float var27 = 40.0F;
               float var28 = var8 + var27 + 4.0F;
               if (var2 >= var24 && var2 <= var24 + var25 && var1 >= var28 && var1 <= var28 + var26) {
                  this.draggingSpeed = true;
               }

               if (var2 >= var24 + var25 + 2.0F && var2 <= var24 + (var25 + 2.0F) * 2.0F && var1 >= var28 && var1 <= var28 + var26) {
                  this.draggingSaturation = true;
               }

               if (var2 >= var24 + (var25 + 2.0F) * 2.0F && var2 <= var24 + (var25 + 2.0F) * 3.0F && var1 >= var28 && var1 <= var28 + var26) {
                  this.draggingBrightness = true;
               }

               if (var2 >= var24 + (var25 + 2.0F) * 3.0F && var2 <= var24 + (var25 + 2.0F) * 4.0F && var1 >= var28 && var1 <= var28 + var26) {
                  this.draggingAlpha = true;
                  this.updateAlphaFromMouse(var1, var28, var26);
               }
            }
         }
      }
   }

   @Override
   public void mouseReleased(int var1, int var2, int var3) {
      this.draggingRainbow = false;
      this.draggingRainbowSpeed = false;
      this.draggingRainbowSaturation = false;
      this.draggingRainbowBrightness = false;
      this.draggingRainbowAlpha = false;
      this.draggingSpeed = false;
      this.draggingSaturation = false;
      this.draggingBrightness = false;
      this.draggingAlpha = false;
   }

   public boolean isPickerOpen() {
      return this.open;
   }

   public void drawPickerOverlay(DrawContext var1, int var2, int var3) {
      if (this.open) {
         double var4 = this.openAnimation.get(1.0, 150L, Easing.CubicInOut);
         float var6 = this.x + 1.0F;
         float var7 = this.y + this.height + 1.0F;
         float var8 = this.width + 5.0F;
         float var9 = 95.0F;
         float var10 = var9 * (float)var4;
         Render2DUtil.rect(var1.getMatrices(), var6, var7, var6 + var8, var7 + var10, new Color(20, 20, 20, 220).getRGB());
         if (var4 > 0.01F) {
            float var11 = Math.min(var8 * 0.65F, var10);
            float var12 = var6 + 3.0F;
            float var13 = var7 + 12.0F;
            this.drawSatBrRect(var1, var12, var13, var11, var11, this.pickerHue);
            float var14 = var12 + this.pickerSat * var11;
            float var15 = var13 + (1.0F - this.pickerBr) * var11;
            float var16 = 3.0F;
            Render2DUtil.drawCircle(var1.getMatrices(), var14, var15, var16, Color.WHITE, 12);
            Render2DUtil.drawCircle(var1.getMatrices(), var14, var15, var16 - 1.0F, Color.BLACK, 12);
            float var18 = var7 + 3.0F;
            float var19 = var8 - 6.0F;
            float var20 = 7.0F;
            this.drawHueBarHorizontal(var1, var12, var18, var19, var20);
            float var21 = var12 + this.pickerHue * var19;
            float var22 = 2.0F;
            Render2DUtil.rect(var1.getMatrices(), var21 - 2.0F, var18 - 1.0F, var21 + 2.0F, var18 + var20 + 1.0F, -1);
            if (this.setting.injectBoolean) {
               float var23 = var13 + var11 + 5.0F;
               float var24 = 12.0F;
               float var25 = 8.0F;
               Render2DUtil.rect(
                  var1.getMatrices(),
                  var6 + 3.0F,
                  var23,
                  var6 + 3.0F + var25,
                  var23 + var25,
                  this.setting.booleanValue ? this.setting.getValue().getRGB() : new Color(60, 60, 60).getRGB()
               );
               this.drawString(
                  this.label("Rainbow", "\u5f69\u8679"),
                  var6 + 3.0F + var25 + 4.0F,
                  var23 + (var25 - this.getFontHeight()) / 2.0F,
                  new Color(200, 200, 200).getRGB()
               );
               if (this.setting.booleanValue && var4 > 0.9F) {
                  float var26 = var23 + var24 + 2.0F;
                  float var27 = 10.0F;
                  float var28 = var8 - 6.0F;
                  float var29 = 40.0F;
                  this.drawRainbowSlider(
                     var1,
                     this.label("Hue", "\u8272\u76f8"),
                     var6 + 3.0F,
                     var26,
                     var29,
                     var28,
                     var27,
                     this.setting.rainbowSpeed,
                     this.draggingRainbowSpeed,
                     this.rainbowSpeedAnimation,
                     var2,
                     var3
                  );
                  this.drawRainbowSlider(
                     var1,
                     this.label("Sat", "\u9971\u548c\u5ea6"),
                     var6 + 3.0F,
                     var26 + var27 + 2.0F,
                     var29,
                     var28,
                     var27,
                     this.setting.rainbowSaturation,
                     this.draggingRainbowSaturation,
                     this.rainbowSaturationAnimation,
                     var2,
                     var3
                  );
                  this.drawRainbowSlider(
                     var1,
                     this.label("Br", "\u4eae\u5ea6"),
                     var6 + 3.0F,
                     var26 + (var27 + 2.0F) * 2.0F,
                     var29,
                     var28,
                     var27,
                     this.setting.rainbowBrightness,
                     this.draggingRainbowBrightness,
                     this.rainbowBrightnessAnimation,
                     var2,
                     var3
                  );
                  this.drawAlphaSlider(
                     var1,
                     this.label("Alpha", "\u900f\u660e\u5ea6"),
                     var6 + 3.0F,
                     var26 + (var27 + 2.0F) * 3.0F,
                     var29,
                     var28,
                     var27,
                     this.setting.rainbowAlpha,
                     this.draggingRainbowAlpha,
                     this.rainbowAlphaAnimation,
                     var2,
                     var3
                  );
               }
            } else {
               float var30 = var13 + var11 + 5.0F;
               float var31 = 10.0F;
               float var32 = var8 - 6.0F;
               float var33 = 40.0F;
               this.drawSlider(
                  var1,
                  this.label("Speed", "\u901f\u5ea6"),
                  var6 + 3.0F,
                  var30,
                  var33,
                  var32,
                  var31,
                  this.setting.rainbowSpeed,
                  this.draggingSpeed,
                  this.speedAnimation,
                  var2,
                  var3
               );
               this.drawSlider(
                  var1,
                  this.label("Sat", "\u9971\u548c\u5ea6"),
                  var6 + 3.0F,
                  var30 + var31 + 2.0F,
                  var33,
                  var32,
                  var31,
                  this.setting.rainbowSaturation,
                  this.draggingSaturation,
                  this.saturationAnimation,
                  var2,
                  var3
               );
               this.drawSlider(
                  var1,
                  this.label("Br", "\u4eae\u5ea6"),
                  var6 + 3.0F,
                  var30 + (var31 + 2.0F) * 2.0F,
                  var33,
                  var32,
                  var31,
                  this.setting.rainbowBrightness,
                  this.draggingBrightness,
                  this.brightnessAnimation,
                  var2,
                  var3
               );
               this.drawAlphaSlider(
                  var1,
                  this.label("Alpha", "\u900f\u660e\u5ea6"),
                  var6 + 3.0F,
                  var30 + (var31 + 2.0F) * 3.0F,
                  var33,
                  var32,
                  var31,
                  this.setting.rainbowAlpha,
                  this.draggingAlpha,
                  this.alphaAnimation,
                  var2,
                  var3
               );
            }
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void toggle() {
      this.setting.setRainbow(!this.setting.rainbow);
   }

   @Override
   public void onMouseClick() {
   }

   @Override
   public boolean getState() {
      return this.setting.rainbow;
   }

   private String label(String var1, String var2) {
      return ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue() ? var2 : var1;
   }
}
