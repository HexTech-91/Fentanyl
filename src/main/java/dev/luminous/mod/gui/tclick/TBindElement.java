package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class TBindElement extends TAbstractElement {
   private final BindSetting bindSetting;
   public boolean isListening;

   public TBindElement(Setting var1) {
      super(var1);
      this.bindSetting = (BindSetting)var1;
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      TFonts.sf_medium_mini
         .drawString(
            var1.getMatrices(), TI18n.setting(this.bindSetting.getName()), this.x + 6.0F, this.y + this.height / 2.0F - 3.0F + 2.0F, new Color(-1).getRGB()
         );
      float var5 = TFonts.sf_medium_mini.getWidth(this.isListening ? "..." : this.bindSetting.getKeyString());
      TDraw.drawRect(var1.getMatrices(), this.x + (this.width - var5 - 11.0F), this.y + 2.0F, var5 + 4.0F, 10.0F, new Color(-1811939328, true));
      TFonts.sf_medium_mini
         .drawString(
            var1.getMatrices(),
            this.isListening ? "..." : this.bindSetting.getKeyString(),
            this.x + (this.width - var5 - 9.0F),
            this.y + this.height / 2.0F - 1.0F,
            new Color(-1).getRGB()
         );
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.isListening) {
         this.bindSetting.setValue(-var3 - 2);
         this.isListening = false;
      } else if (this.hovered && var3 == 0) {
         this.isListening = !this.isListening;
      }

      super.mouseClicked(var1, var2, var3);
   }

   @Override
   public void keyTyped(int var1) {
      if (this.isListening) {
         if (var1 != 256 && var1 != 261) {
            this.bindSetting.setValue(var1);
         } else {
            this.bindSetting.setValue(-1);
         }

         this.isListening = false;
      }
   }
}
