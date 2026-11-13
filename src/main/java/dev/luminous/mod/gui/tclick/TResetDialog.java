package dev.luminous.mod.gui.tclick;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.mod.gui.tclick.core.TAssets;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class TResetDialog {
   private static final float PANEL_W = 260.0F;
   private static final float PANEL_H = 130.0F;
   private static final float PICTURE = 64.0F;
   private static final float BUTTON_W = 90.0F;
   private static final float BUTTON_H = 24.0F;
   private static final float BUTTON_Y = 96.0F;
   private final String title;
   private final String question;
   private final String yesText;
   private final String noText;
   private final Runnable onYes;
   private final Runnable onNo;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public TResetDialog(String var1, String var2, String var3, String var4, Runnable var5, Runnable var6) {
      this.title = var1 == null ? "" : var1;
      this.question = var2 == null ? "" : var2;
      this.yesText = var3 == null ? "Yes" : var3;
      this.noText = var4 == null ? "No" : var4;
      this.onYes = var5;
      this.onNo = var6;
   }

   private static float panelX() {
      return mc.getWindow().getScaledWidth() / 2.0F - 130.0F;
   }

   private static float panelY() {
      return mc.getWindow().getScaledHeight() / 2.0F - 65.0F;
   }

   private static boolean yesHovered(int var0, int var1) {
      return TDraw.isHovered(var0, var1, panelX() + 30.0F, panelY() + 96.0F, 90.0, 24.0);
   }

   private static boolean noHovered(int var0, int var1) {
      return TDraw.isHovered(var0, var1, panelX() + 140.0F, panelY() + 96.0F, 90.0, 24.0);
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
      try {
         MatrixStack var5 = var1.getMatrices();
         int var6 = var1.getScaledWindowWidth();
         int var7 = var1.getScaledWindowHeight();
         TDraw.drawRect(var5, 0.0F, 0.0F, var6, var7, new Color(0, 0, 0, 140));
         float var8 = panelX();
         float var9 = panelY();
         TDraw.drawHudBase(var5, var8, var9, 260.0F, 130.0F, 8.0F, 1.0F, false);

         try {
            RenderSystem.setShaderTexture(0, TAssets.QUESTION);
            TDraw.renderTexture(var5, var8 + 14.0F, var9 + 33.0F - 6.0F, 64.0, 64.0, 0.0F, 0.0F, 64.0, 64.0, 64.0, 64.0);
         } catch (Throwable var17) {
         }

         float var10 = var8 + 165.0F;
         float var11 = 158.0F;
         TFonts.sf_medium.drawCenteredString(var5, this.title, var10, var9 + 14.0F, Color.WHITE.getRGB());
         float var12 = var9 + 32.0F;

         for (String var14 : wrap(this.question, var11)) {
            TFonts.sf_medium.drawCenteredString(var5, var14, var10, var12, new Color(-1275068417, true).getRGB());
            var12 += TFonts.sf_medium.getFontHeight() + 2.0F;
         }

         float var19 = panelX() + 30.0F;
         float var20 = panelX() + 140.0F;
         Color var15 = yesHovered(var2, var3) ? TColor.getColor(0) : TDraw.applyOpacity(TColor.getColor(0), 0.7F);
         Color var16 = noHovered(var2, var3) ? TColor.getColor(270) : TDraw.applyOpacity(TColor.getColor(270), 0.7F);
         TDraw.drawRound(var5, var19, var9 + 96.0F, 90.0F, 24.0F, 6.0F, var15);
         TDraw.drawRound(var5, var20, var9 + 96.0F, 90.0F, 24.0F, 6.0F, var16);
         TFonts.sf_medium.drawCenteredString(var5, this.yesText, var19 + 45.0F, var9 + 96.0F + 8.0F, Color.WHITE.getRGB());
         TFonts.sf_medium.drawCenteredString(var5, this.noText, var20 + 45.0F, var9 + 96.0F + 8.0F, Color.WHITE.getRGB());
      } catch (Throwable var18) {
      }
   }

   public boolean mouseClicked(int var1, int var2, int var3) {
      try {
         if (var3 == 0) {
            if (yesHovered(var1, var2)) {
               this.choose(true);
            } else if (noHovered(var1, var2)) {
               this.choose(false);
            }
         }
      } catch (Throwable var5) {
      }

      return true;
   }

   public boolean keyTyped(int var1) {
      try {
         if (var1 == 256) {
            this.choose(false);
         }
      } catch (Throwable var3) {
      }

      return true;
   }

   private void choose(boolean var1) {
      try {
         if (var1) {
            if (this.onYes != null) {
               this.onYes.run();
            }
         } else if (this.onNo != null) {
            this.onNo.run();
         }
      } catch (Throwable var3) {
      }

      if (TClickGuiScreen.activeDialog == this) {
         TClickGuiScreen.activeDialog = null;
      }
   }

   private static List<String> wrap(String var0, float var1) {
      ArrayList var2 = new ArrayList();
      if (var0 != null && !var0.isEmpty() && TFonts.sf_medium != null) {
         String[] var3 = var0.split(" ");
         StringBuilder var4 = new StringBuilder();

         for (String var8 : var3) {
            String var9 = var4.length() == 0 ? var8 : var4 + " " + var8;
            if (TFonts.sf_medium.getWidth(var9) > var1 && var4.length() > 0) {
               var2.add(var4.toString());
               var4 = new StringBuilder(var8);
            } else {
               var4 = new StringBuilder(var9);
            }
         }

         if (var4.length() > 0) {
            var2.add(var4.toString());
         }

         return var2;
      } else {
         if (var0 != null && !var0.isEmpty()) {
            var2.add(var0);
         }

         return var2;
      }
   }
}
