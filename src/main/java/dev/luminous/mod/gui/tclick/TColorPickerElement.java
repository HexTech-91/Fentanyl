package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class TColorPickerElement extends TAbstractElement {
   private final ColorSetting colorSetting;
   private float hue;
   private float saturation;
   private float brightness;
   private int alpha;
   private boolean afocused;
   private boolean hfocused;
   private boolean sbfocused;
   private float spos;
   private float bpos;
   private float hpos;
   private float apos;
   private Color prevColor;
   private boolean firstInit;
   private boolean extended;

   public TColorPickerElement(Setting var1) {
      super(var1);
      this.colorSetting = (ColorSetting)var1;
      this.prevColor = this.getColorSetting().getValue();
      this.updatePos();
      this.firstInit = true;
   }

   public ColorSetting getColorSetting() {
      return this.colorSetting;
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      MatrixStack var5 = var1.getMatrices();
      Color var6 = this.getColorSetting().getValue();
      boolean var7 = TDraw.isHovered(var2, var3, this.x, this.y + 5.0F, 90.0, 7.0);
      TFonts.sf_medium_mini.drawString(var5, TI18n.setting(this.colorSetting.getName()), this.x + 6.0F, this.y + 8.0F, new Color(-1).getRGB());
      TDraw.drawBlurredShadow(var5, this.x + this.width - 22.0F, this.y + 5.0F, 14.0F, 7.0F, var7 ? 6 : 10, var6);
      if (var7) {
         TDraw.drawRound(var5, this.x + this.width - 22.5F, this.y + 4.5F, 15.0F, 8.0F, 1.0F, var6);
      } else {
         TDraw.drawRound(var5, this.x + this.width - 22.0F, this.y + 5.0F, 14.0F, 7.0F, 1.0F, var6);
      }

      if (this.extended) {
         boolean var8 = TDraw.isHovered(var2, var3, this.x + 36.0F, this.y + 54.0F, 24.0, 7.0);
         boolean var9 = TDraw.isHovered(var2, var3, this.x + 9.0F, this.y + 54.0F, 24.0, 7.0);
         boolean var10 = TDraw.isHovered(var2, var3, this.x + 63.0F, this.y + 54.0F, 24.0, 7.0);
         boolean var11 = TDraw.isDark(TColor.copy_color);
         boolean var12 = TDraw.isDark(var6);
         TDraw.drawRect(var5, this.x + 9.0F, this.y + 54.0F, 24.0F, 7.0F, new Color(4342338));
         TFonts.sf_medium_mini
            .drawString(var5, "\u0421opy", this.x + 13.0F, this.y + 56.5F, var9 ? new Color(-1543503873, true).getRGB() : Color.WHITE.getRGB());
         TDraw.drawRect(var5, this.x + 36.0F, this.y + 54.0F, 24.0F, 7.0F, this.getColorSetting().rainbow ? var6 : new Color(4342338));
         TFonts.sf_medium_mini
            .drawString(
               var5, "RB", this.x + 44.0F, this.y + 56.5F, var8 ? new Color(-1543503873, true).getRGB() : (var12 ? Color.WHITE.getRGB() : Color.BLACK.getRGB())
            );
         TDraw.drawRect(var5, this.x + 63.0F, this.y + 54.0F, 24.0F, 7.0F, TColor.copy_color);
         TFonts.sf_medium_mini
            .drawString(
               var5,
               "Paste",
               this.x + 67.0F,
               this.y + 56.5F,
               var10 ? new Color(-1543503873, true).getRGB() : (var11 ? Color.WHITE.getRGB() : Color.BLACK.getRGB())
            );
         this.renderPicker(var5, var2, var3, var6);
      }
   }

   @Override
   public float getHeight() {
      return this.extended ? 66.0F : 15.0F;
   }

   private void renderPicker(MatrixStack var1, int var2, int var3, Color var4) {
      double var5 = this.x + 6.0F;
      double var7 = this.y + 16.0F;
      double var9 = this.width - 38.0F;
      double var11 = this.height - 30.0F;
      if (this.prevColor != this.getColorSetting().getValue()) {
         this.updatePos();
         this.prevColor = this.getColorSetting().getValue();
      }

      if (this.firstInit) {
         this.spos = (float)(var5 + var9 - (var9 - var9 * this.saturation));
         this.bpos = (float)(var7 + (var11 - var11 * this.brightness));
         this.hpos = (float)(var7 + var11 - 3.0 + (var11 - 3.0) * this.hue);
         this.apos = (float)(var7 + (var11 - 3.0 - (var11 - 3.0) * (this.alpha / 255.0F)));
         this.firstInit = false;
      }

      this.spos = TDraw.scrollAnimate(this.spos, (float)(var5 + var9 - (var9 - var9 * this.saturation)), 0.6F);
      this.bpos = TDraw.scrollAnimate(this.bpos, (float)(var7 + (var11 - var11 * this.brightness)), 0.6F);
      this.hpos = TDraw.scrollAnimate(this.hpos, (float)(var7 + var11 - 3.0 + (var11 - 3.0) * this.hue), 0.6F);
      this.apos = TDraw.scrollAnimate(this.apos, (float)(var7 + (var11 - 3.0 - (var11 - 3.0) * (this.alpha / 255.0F))), 0.6F);
      Color var13 = Color.getHSBColor(this.hue, 0.0F, 1.0F);
      Color var14 = Color.getHSBColor(this.hue, 1.0F, 1.0F);
      Color var15 = new Color(0, 0, 0, 0);
      Color var16 = new Color(0, 0, 0);
      TDraw.horizontalGradient(var1, (float)var5 + 2.0F, (float)var7, (float)(var5 + var9), (float)(var7 + var11), var13, var14);
      TDraw.verticalGradient(var1, (float)var5 + 2.0F, (float)var7, (float)(var5 + var9), (float)(var7 + var11), var15, var16);

      for (float var17 = 1.0F; var17 < var11 - 2.0; var17++) {
         float var18 = (float)(1.0 / (var11 / var17));
         TDraw.drawRect(var1, (float)(var5 + var9 + 4.0), (float)(var7 + var17), 8.0F, 1.0F, Color.getHSBColor(var18, 1.0F, 1.0F));
      }

      TDraw.drawRect(var1, (float)(var5 + var9 + 17.0), (float)(var7 + 1.0), 8.0F, (float)(var11 - 3.0), new Color(-1));
      TDraw.verticalGradient(
         var1,
         (float)(var5 + var9 + 17.0),
         (float)(var7 + 0.8F),
         (float)(var5 + var9 + 25.0),
         (float)(var7 + var11 - 2.0),
         new Color(var4.getRed(), var4.getGreen(), var4.getBlue(), 255),
         new Color(0, 0, 0, 0)
      );
      TDraw.drawRect(var1, (float)(var5 + var9 + 3.0), this.hpos + 0.5F, 10.0F, 1.0F, Color.WHITE);
      TDraw.drawRect(var1, (float)(var5 + var9 + 16.0), this.apos + 0.5F, 10.0F, 1.0F, Color.WHITE);
      TDraw.drawRound(var1, this.spos - 1.5F, this.bpos - 1.5F, 3.0F, 3.0F, 1.5F, new Color(-1));
      Color var19 = Color.getHSBColor(this.hue, this.saturation, this.brightness);
      if (this.sbfocused) {
         this.saturation = (float)(MathHelper.clamp((float)(var2 - var5), 0.0F, (float)var9) / var9);
         this.brightness = (float)((var11 - MathHelper.clamp((float)(var3 - var7), 0.0F, (float)var11)) / var11);
         var19 = Color.getHSBColor(this.hue, this.saturation, this.brightness);
         this.setColor(new Color(var19.getRed(), var19.getGreen(), var19.getBlue(), this.alpha));
      }

      if (this.hfocused) {
         this.hue = (float)(-((var11 - MathHelper.clamp((float)(var3 - var7), 0.0F, (float)var11)) / var11));
         var19 = Color.getHSBColor(this.hue, this.saturation, this.brightness);
         this.setColor(new Color(var19.getRed(), var19.getGreen(), var19.getBlue(), this.alpha));
      }

      if (this.afocused) {
         this.alpha = (int)((var11 - MathHelper.clamp((float)(var3 - var7), 0.0F, (float)var11)) / var11 * 255.0);
         this.setColor(new Color(var19.getRed(), var19.getGreen(), var19.getBlue(), this.alpha));
      }
   }

   private void updatePos() {
      Color var1 = this.getColorSetting().getValue();
      float[] var2 = Color.RGBtoHSB(var1.getRed(), var1.getGreen(), var1.getBlue(), null);
      this.hue = -1.0F + var2[0];
      this.saturation = var2[1];
      this.brightness = var2[2];
      this.alpha = var1.getAlpha();
   }

   private void setColor(Color var1) {
      this.getColorSetting().setValue(var1.getRGB());
      this.prevColor = var1;
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      double var4 = this.x + 4.0F;
      double var6 = this.y + 17.0F;
      double var8 = this.width - 34.0F;
      double var10 = this.height - 30.0F;
      boolean var12 = TDraw.isHovered(var1, var2, this.x + 36.0F, this.y + 54.0F, 24.0, 7.0);
      boolean var13 = TDraw.isHovered(var1, var2, this.x + 9.0F, this.y + 54.0F, 24.0, 7.0);
      boolean var14 = TDraw.isHovered(var1, var2, this.x + 63.0F, this.y + 54.0F, 24.0, 7.0);
      boolean var15 = TDraw.isHovered(var1, var2, this.x, this.y, 90.0, 11.0);
      if (var15) {
         boolean var16 = this.extended = !this.extended;
      }

      if (this.extended) {
         if (TDraw.isHovered(var1, var2, var4 + var8 + 17.0, var6, 8.0, var10) && var3 == 0) {
            this.afocused = true;
         } else if (TDraw.isHovered(var1, var2, var4 + var8 + 4.0, var6, 8.0, var10) && var3 == 0) {
            this.hfocused = true;
         } else if (TDraw.isHovered(var1, var2, var4, var6, var8, var10) && var3 == 0) {
            this.sbfocused = true;
         } else if (var12 && var3 == 0) {
            this.getColorSetting().setRainbow(!this.getColorSetting().rainbow);
         } else if (var13) {
            TColor.copy_color = this.getColorSetting().getValue();
         } else if (var14) {
            this.getColorSetting().setValue(TColor.copy_color.getRGB());
         }

         super.mouseClicked(var1, var2, var3);
      }
   }

   @Override
   public void mouseReleased(int var1, int var2, int var3) {
      this.hfocused = false;
      this.afocused = false;
      this.sbfocused = false;
   }

   @Override
   public void onClose() {
      this.hfocused = false;
      this.afocused = false;
      this.sbfocused = false;
   }
}
