package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TAnim;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.gui.tclick.core.TSounds;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class TBooleanElement extends TAbstractElement {
   private final BooleanSetting booleanSetting;
   float animation = 0.0F;
   float animation2 = 0.0F;

   public TBooleanElement(Setting var1) {
      super(var1);
      this.booleanSetting = (BooleanSetting)var1;
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      TDraw.drawRound(
         var1.getMatrices(),
         this.x + this.width - 21.0F,
         this.y + this.height / 2.0F - 4.0F,
         15.0F,
         8.0F,
         1.0F,
         7.0F * this.animation > 4.0F ? TColor.getColor(0) : new Color(687865855, true)
      );
      this.animation = TAnim.fast(this.animation, this.booleanSetting.getValue() ? 1.0F : 0.0F, 20.0F);
      this.animation2 = TAnim.fast(this.animation2, this.booleanSetting.getValue() ? 1.0F : 0.0F, 8.0F);
      TDraw.drawRound(
         var1.getMatrices(), this.x + this.width - 20.0F + 7.0F * this.animation, this.y + this.height / 2.0F - 3.0F, 6.0F, 6.0F, 1.0F, new Color(-1)
      );
      TDraw.drawRound(
         var1.getMatrices(), this.x + this.width - 20.0F + 7.0F * this.animation2, this.y + this.height / 2.0F - 3.0F, 6.0F, 6.0F, 1.0F, new Color(-1)
      );
      if (7.0F * this.animation > 4.0F) {
         TFonts.sf_bold_mini.drawString(var1.getMatrices(), "v", this.x + this.width - 19.0F, this.y + this.height / 2.0F - 2.0F, new Color(-1).getRGB());
      } else {
         TFonts.sf_bold_mini.drawString(var1.getMatrices(), "x", this.x + this.width - 12.0F, this.y + this.height / 2.0F - 2.0F, new Color(-1).getRGB());
      }

      if (TDraw.isHovered(var2, var3, this.x + this.width - 21.0F, this.y + this.height / 2.0F - 4.0F, 15.0, 8.0)) {
         TClickGuiScreen.anyHovered = true;
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221188));
         }
      }

      TFonts.sf_medium_mini
         .drawString(
            var1.getMatrices(),
            TI18n.setting(this.booleanSetting.getName()),
            (this.booleanSetting.hasParent() ? 2.0F : 0.0F) + this.x + 6.0F,
            this.y + this.height / 2.0F - 3.0F + 2.0F,
            new Color(-1).getRGB()
         );
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.hovered && var3 == 0) {
         this.booleanSetting.setValue(!this.booleanSetting.getValue());
         TSounds.playBoolean();
      }

      super.mouseClicked(var1, var2, var3);
   }
}
