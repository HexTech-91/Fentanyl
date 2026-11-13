package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.gui.tclick.core.TSounds;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class TSliderElement extends TAbstractElement {
   private final SliderSetting sliderSetting;
   private final float min;
   private final float max;
   private float animation;
   private float prevValue;
   private boolean dragging;
   private boolean listening;
   private String typedValue = "";

   public TSliderElement(Setting var1) {
      super(var1);
      this.sliderSetting = (SliderSetting)var1;
      this.min = (float)this.sliderSetting.getMin();
      this.max = (float)this.sliderSetting.getMax();
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      this.animation = TDraw.scrollAnimate(this.animation, (float)((this.sliderSetting.getValue() - this.min) / (this.max - this.min)), 0.4F);
      MatrixStack var5 = var1.getMatrices();
      String var6 = this.format(this.sliderSetting.getValue());
      if (!this.dragging) {
         TFonts.sf_medium_mini.drawString(var5, TI18n.setting(this.sliderSetting.getName()), this.x + 6.0F, this.y + 4.0F, new Color(-1).getRGB());
         if (this.listening) {
            String var7 = this.typedValue.isEmpty() ? var6 : this.typedValue;
            float var8 = TFonts.sf_medium_mini.getWidth(var7);
            float var9 = this.x + this.width - 6.0F - var8;
            TDraw.drawRound(var5, var9 - 4.0F, this.y + 2.0F, var8 + 8.0F, 11.0F, 3.0F, new Color(1090519039, true));
            boolean var10 = MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.age % 10 < 5;
            TFonts.sf_medium_mini.drawString(var5, var7 + (var10 ? "_" : ""), var9, this.y + 5.0F, TColor.getColor(0).getRGB());
         } else {
            TFonts.sf_medium_mini
               .drawString(var5, var6, (float)((int)(this.x + this.width - 6.0F - TFonts.sf_medium_mini.getWidth(var6))), this.y + 5.0F, new Color(-1).getRGB());
         }
      } else {
         String var11 = this.format(this.sliderSetting.getMin());
         String var12 = this.format(this.sliderSetting.getMax());
         if (this.animation > 0.2F) {
            TFonts.sf_medium_mini.drawString(var5, var11, this.x + 6.0F, this.y + 4.0F, new Color(-1).getRGB());
         }

         if (this.animation < 0.8F) {
            TFonts.sf_medium_mini
               .drawString(var5, var12, this.x + this.width - TFonts.sf_medium_mini.getWidth(var12) - 6.0F, this.y + 4.0F, new Color(-1).getRGB());
         }

         TFonts.sf_medium_mini
            .drawString(
               var5,
               var6,
               this.animation > 0.2F
                  ? (
                     this.animation < 0.8F
                        ? this.x + 6.0F + (this.width - 14.0F) * this.animation - TFonts.sf_medium_mini.getWidth(var6) / 2.0F
                        : this.x + this.width - TFonts.sf_medium_mini.getWidth(var12) - 6.0F
                  )
                  : this.x + 6.0F,
               this.y + 4.0F,
               new Color(-1).getRGB()
            );
      }

      TDraw.drawRect(var5, this.x + 6.0F, this.y + this.height - 6.0F, this.width - 12.0F, 2.0F, new Color(687865855, true));
      TDraw.draw2DGradientRect(
         var5,
         this.x + 6.0F,
         this.y + this.height - 6.0F,
         this.x + 6.0F + (this.width - 12.0F) * this.animation,
         this.y + this.height - 4.0F,
         TColor.getColor(180),
         TColor.getColor(180),
         TColor.getColor(0),
         TColor.getColor(0)
      );
      TDraw.drawRect(var5, this.x + 6.0F + (this.width - 14.0F) * this.animation, this.y + this.height - 7.5F, 2.0F, 5.0F, new Color(-1973791));
      this.animation = MathHelper.clamp(this.animation, 0.0F, 1.0F);
      if (this.dragging) {
         this.setValue(var2, this.x + 7.0F, this.width - 14.0F);
      }

      if (TDraw.isHovered(var2, var3, this.x + 6.0F, this.y + this.height - 7.0F, this.width - 12.0F, 3.0)) {
         TClickGuiScreen.anyHovered = true;
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221189));
         }
      }
   }

   private void setValue(int var1, double var2, double var4) {
      float var6 = TDraw.interpolateFloat(this.min, this.max, MathHelper.clamp((var1 - var2) / var4, 0.0, 1.0));
      this.sliderSetting.setValue(var6);
      if (var6 != this.prevValue) {
         TSounds.playSlider();
      }

      this.prevValue = var6;
   }

   public static String removeLastChar(String var0) {
      String var1 = "";
      if (var0 != null && !var0.isEmpty()) {
         var1 = var0.substring(0, var0.length() - 1);
      }

      return var1;
   }

   private String format(double var1) {
      return this.sliderSetting.getIncrement() >= 1.0 ? String.valueOf((int)var1) : String.valueOf((float)var1);
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.hovered && var3 == 2) {
         super.mouseClicked(var1, var2, var3);
         this.listening = true;
         this.typedValue = "";
      } else {
         if (var3 == 0 && this.hovered) {
            this.dragging = true;
         }

         super.mouseClicked(var1, var2, var3);
      }
   }

   @Override
   public void onClose() {
      this.listening = false;
      this.typedValue = "";
      this.dragging = false;
   }

   @Override
   public void mouseReleased(int var1, int var2, int var3) {
      this.dragging = false;
   }

   @Override
   public void keyTyped(int var1) {
      if (this.listening) {
         switch (var1) {
            case 256:
               this.listening = false;
               this.typedValue = "";
               break;
            case 257:
            case 335:
               this.applyTyped();
               break;
            case 259:
               this.typedValue = removeLastChar(this.typedValue);
         }
      }
   }

   @Override
   public void charTyped(char var1, int var2) {
      if (this.listening && (var1 >= '0' && var1 <= '9' || var1 == '.')) {
         if (var1 == '.' && this.typedValue.indexOf(46) >= 0) {
            return;
         }

         this.typedValue = this.typedValue + var1;
      }
   }

   private void applyTyped() {
      try {
         double var1 = Double.parseDouble(this.typedValue);
         double var3 = MathHelper.clamp(var1, this.min, this.max);
         this.sliderSetting.setValue(var3);
      } catch (Throwable var5) {
      }

      this.listening = false;
      this.typedValue = "";
   }

   public boolean isListening() {
      return this.listening;
   }
}
