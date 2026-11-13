package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

public class TStringElement extends TAbstractElement {
   private final StringSetting stringSetting;
   public boolean listening;
   private String currentString = "";

   public TStringElement(Setting var1) {
      super(var1);
      this.stringSetting = (StringSetting)var1;
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      TDraw.drawRect(var1.getMatrices(), this.x + 5.0F, this.y + 2.0F, this.width - 11.0F, 10.0F, new Color(-1811939328, true));
      MinecraftClient var5 = MinecraftClient.getInstance();
      TFonts.sf_medium_mini
         .drawString(
            var1.getMatrices(),
            this.listening ? this.currentString + (var5.player != null && var5.player.age % 5 != 0 ? "" : "_") : this.stringSetting.getValue(),
            this.x + 6.0F,
            this.y + this.height / 2.0F,
            -1
         );
      if (TDraw.isHovered(var2, var3, this.x + 5.0F, this.y + 2.0F, this.width - 11.0F, 10.0)) {
         TClickGuiScreen.anyHovered = true;
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(var5.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221186));
         }
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.hovered && var3 == 0) {
         boolean var4 = this.listening = !this.listening;
      }

      if (this.listening) {
         this.currentString = this.stringSetting.getValue();
      }

      super.mouseClicked(var1, var2, var3);
   }

   @Override
   public void charTyped(char var1, int var2) {
      if (StringHelper.isValidChar(var1)) {
         this.currentString = this.currentString + var1;
      }
   }

   @Override
   public void keyTyped(int var1) {
      if (this.listening) {
         switch (var1) {
            case 32:
               this.currentString = this.currentString + " ";
               break;
            case 256:
               this.currentString = "";
               this.listening = false;
               break;
            case 257:
               this.stringSetting
                  .setValue(this.currentString != null && !this.currentString.isEmpty() ? this.currentString : this.stringSetting.getDefaultValue());
               this.currentString = "";
               this.listening = !this.listening;
               break;
            case 259:
               this.currentString = TSliderElement.removeLastChar(this.currentString);
         }
      }
   }

   @Override
   public void onClose() {
      this.listening = false;
      this.currentString = "";
   }
}
