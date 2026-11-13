package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TAnim;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.gui.tclick.core.TSounds;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class TModeElement extends TAbstractElement {
   private final EnumSetting enumSetting;
   private boolean open;
   private double wheight;
   private String prevMode;
   private float animation;
   private float animation2;

   public TModeElement(Setting var1) {
      super(var1);
      this.enumSetting = (EnumSetting)var1;
      this.prevMode = this.currentEnumName();
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      this.animation = TAnim.fast(this.animation, this.open ? 0.0F : 1.0F, 15.0F);
      this.animation2 = TAnim.fast(this.animation2, 1.0F, 10.0F);
      MatrixStack var5 = var1.getMatrices();
      String var6 = this.currentEnumName();
      TFonts.sf_medium_mini
         .drawString(var5, TI18n.setting(this.enumSetting.getName()), (double)(this.x + 6.0F), this.y + this.wheight / 2.0 - 3.0 + 3.0, new Color(-1).getRGB());
      String var7 = this.displayOf(var6);
      String var8 = this.displayOf(this.prevMode);
      if (this.animation2 < 0.99 && !Objects.equals(var6, this.prevMode)) {
         TFonts.sf_medium_mini
            .drawString(
               var5,
               var8,
               (double)((int)(this.x + this.width - 18.0F - TFonts.sf_medium_mini.getWidth(var8))),
               3.0 + (this.y + this.wheight / 2.0 - 3.0) - this.animation2 * 5.0F,
               TDraw.applyOpacity(new Color(-1), this.animation2)
            );
         TFonts.sf_medium_mini
            .drawString(
               var5,
               var7,
               (double)(this.x + this.width - 18.0F - TFonts.sf_medium_mini.getWidth(var7)),
               3.0 + (this.y + this.wheight / 2.0 - 3.0) - this.animation2 * 5.0F + 5.0,
               TDraw.applyOpacity(new Color(-1), 1.0F - this.animation2)
            );
      } else {
         TFonts.sf_medium_mini
            .drawString(
               var5,
               var7,
               (double)(this.x + this.width - 18.0F - TFonts.sf_medium_mini.getWidth(var7)),
               3.0 + (this.y + this.wheight / 2.0 - 3.0),
               new Color(-1).getRGB()
            );
      }

      if (this.open) {
         Color var9 = TColor.getColor(0);
         double var10 = 0.0;
         Enum[] var12 = this.modes();

         for (int var13 = 0; var13 <= var12.length - 1; var13++) {
            String var14 = var12[var13].name();
            String var15 = TI18n.enumValue(var12[var13]);
            TFonts.sf_medium_mini
               .drawString(
                  var5,
                  var15,
                  (double)(this.x + this.width / 2.0F - TFonts.sf_medium_mini.getWidth(var15) / 2.0F),
                  this.y + this.wheight + 2.0 + var10,
                  var6.equalsIgnoreCase(var14) ? var9.getRGB() : new Color(-1).getRGB()
               );
            var10 += 12.0;
         }
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (TDraw.isHovered(var1, var2, this.x, this.y, this.width, this.wheight)) {
         if (var3 == 0) {
            this.prevMode = this.currentEnumName();
            this.animation2 = 0.0F;
            this.enumSetting.increaseEnum();
            TSounds.playBoolean();
         } else {
            boolean var4 = this.open = !this.open;
            if (this.open) {
               TSounds.playSwipeIn();
            } else {
               TSounds.playSwipeOut();
            }
         }
      }

      if (this.open) {
         double var8 = 0.0;
         Enum[] var6 = this.modes();

         for (int var7 = 0; var7 <= var6.length - 1; var7++) {
            if (TDraw.isHovered(var1, var2, this.x, this.y + this.wheight + var8, this.width, 12.0) && var3 == 0) {
               this.prevMode = this.currentEnumName();
               this.animation2 = 0.0F;
               this.enumSetting.setValue(var6[var7]);
               TSounds.playBoolean();
            }

            var8 += 12.0;
         }
      }

      super.mouseClicked(var1, var2, var3);
   }

   public void setWHeight(double var1) {
      this.wheight = var1;
   }

   public boolean isOpen() {
      return this.open;
   }

   private String currentEnumName() {
      return this.enumSetting.getValue().name();
   }

   private String displayOf(String var1) {
      for (Enum var5 : this.modes()) {
         if (var5.name().equalsIgnoreCase(var1)) {
            return TI18n.enumValue(var5);
         }
      }

      return var1;
   }

   private Enum<?>[] modes() {
      return (Enum<?>[])this.enumSetting.getValue().getDeclaringClass().getEnumConstants();
   }
}
