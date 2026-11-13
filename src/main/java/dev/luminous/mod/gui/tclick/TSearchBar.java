package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

public class TSearchBar extends TAbstractButton {
   public static String moduleName = "";
   public static boolean listening;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      TDraw.drawGuiBase(
         var1.getMatrices(),
         this.x + 4.0F,
         this.y + 1.0F,
         this.width - 8.0F,
         this.height - 2.0F,
         1.0F,
         TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height) ? 0.8F : 0.0F
      );
      if (!listening) {
         drawGradientString(var1, "Search...", this.x + 7.0F, this.y + this.height / 2.0F - 3.0F);
      } else {
         drawGradientString(var1, moduleName + (mc.player != null && mc.player.age / 10 % 2 != 0 ? "_" : " "), this.x + 7.0F, this.y + 5.0F);
      }

      if (TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height)) {
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221186));
         }

         TClickGuiScreen.anyHovered = true;
      }
   }

   private static void drawGradientString(DrawContext var0, String var1, float var2, float var3) {
      char[] var4 = var1.toCharArray();
      float var5 = var2;

      for (int var6 = 0; var6 < var4.length; var6++) {
         String var7 = String.valueOf(var4[var6]);
         Color var8 = ClickGui.getInstance().getColor(var6 * 2);
         TFonts.sf_medium.drawString(var0.getMatrices(), var7, var5, var3, var8.getRGB());
         var5 += TFonts.sf_medium.getWidth(var7);
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      super.mouseClicked(var1, var2, var3);
      boolean var4 = TDraw.isHovered(var1, var2, this.x, this.y, this.width, this.height);
      if (var4) {
         listening = true;
      } else {
         moduleName = "";
         listening = false;
      }
   }

   @Override
   public void charTyped(char var1, int var2) {
      if (StringHelper.isValidChar(var1) && listening) {
         moduleName = moduleName + var1;
      }
   }

   @Override
   public void keyTyped(int var1) {
      super.keyTyped(var1);
      if (var1 != 70 || !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341) && !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 345)) {
         if (listening) {
            switch (var1) {
               case 32:
                  moduleName = moduleName + " ";
                  break;
               case 256:
               case 257:
                  listening = false;
                  moduleName = "";
                  break;
               case 259:
                  moduleName = TSliderElement.removeLastChar(moduleName);
            }
         }
      } else {
         listening = !listening;
      }
   }
}
