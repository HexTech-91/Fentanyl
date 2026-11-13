package dev.luminous.api.utils.render;

import dev.luminous.api.utils.misc.ShulkerColor;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public final class KitOverlayRenderer {
   public static final int CELL = 18;
   private static final int PAD = 4;
   private static final int COLUMN_GAP = 6;
   private static final float BORDER = 1.5F;
   public static final Color BORDER_COLOR = new Color(0, 220, 220, 220);
   public static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 40);
   private static final Color CONTENT_BG = new Color(0, 0, 0, 30);
   private static final List<KitOverlayRenderer.Pop> POPS = new ArrayList<>();

   private KitOverlayRenderer() {
   }

   private static int columnWidth() {
      return 54;
   }

   private static int columnHeight() {
      return 74;
   }

   public static int panelWidth(int var0) {
      return var0 <= 0 ? 8 : var0 * columnWidth() + (var0 - 1) * 6 + 8;
   }

   public static int panelHeight() {
      return columnHeight() + 8;
   }

   public static void layout(int var0, int var1, double var2, double var4, int var6, int[] var7) {
      int var8 = panelWidth(var6);
      int var9 = panelHeight();
      int var10 = (int)Math.round(var2 - var8 / 2.0);
      int var11 = (int)Math.round(var4 - var9 / 2.0);
      byte var12 = 4;
      var10 = Math.max(var12, Math.min(var0 - var8 - var12, var10));
      var11 = Math.max(var12, Math.min(var1 - var9 - var12, var11));
      var7[0] = var10;
      var7[1] = var11;
      var7[2] = var8;
      var7[3] = var9;
   }

   public static void iconCentre(int var0, int var1, int var2, float[] var3) {
      int var4 = var0 + 4 + var2 * (columnWidth() + 6) + 9 + 1;
      int var5 = var1 + 4 + 9;
      var3[0] = var4;
      var3[1] = var5;
   }

   public static void contentCentre(int var0, int var1, int var2, int var3, float[] var4) {
      int var5 = var0 + 4 + var2 * (columnWidth() + 6);
      int var6 = var1 + 4 + 18 + 2;
      int var7 = var3 % 3;
      int var8 = var3 / 3;
      var4[0] = var5 + var7 * 18 + 9;
      var4[1] = var6 + var8 * 18 + 9;
   }

   public static void render(DrawContext var0, List<ItemStack> var1, int var2, int var3, int var4) {
      MatrixStack var5 = var0.getMatrices();
      int var6 = panelWidth(var2);
      int var7 = panelHeight();
      Render2DUtil.drawRect(var0, (float)var3, (float)var4, (float)var6, (float)var7, BACKGROUND_COLOR);
      Render2DUtil.drawRectWithOutline(var5, var3, var4, var6, var7, new Color(0, 0, 0, 0), BORDER_COLOR);
      ItemStack[] var8 = new ItemStack[27];
      float[] var9 = new float[2];

      for (int var10 = 0; var10 < var2 && var10 < var1.size(); var10++) {
         ItemStack var11 = (ItemStack)var1.get(var10);
         if (var11 != null && !var11.isEmpty()) {
            iconCentre(var3, var4, var10, var9);
            int var12 = (int)(var9[0] - 8.0F);
            int var13 = (int)(var9[1] - 8.0F);
            var0.drawItem(var11, var12, var13);
            ShulkerViewer.getItemsInContainerItem(var11, var8);
            int var14 = var3 + 4 + var10 * (columnWidth() + 6);
            int var15 = var4 + 4 + 18 + 2;
            Render2DUtil.drawRect(var5, (float)var14, (float)var15, 54.0F, 54.0F, CONTENT_BG);

            for (int var16 = 0; var16 < 9; var16++) {
               ItemStack var17 = var8[var16];
               if (var17 != null && !var17.isEmpty()) {
                  contentCentre(var3, var4, var10, var16, var9);
                  var0.drawItem(var17, (int)(var9[0] - 8.0F), (int)(var9[1] - 8.0F));
                  if (var17.getCount() > 1) {
                     MinecraftClient var18 = MinecraftClient.getInstance();
                     TextRenderer var19 = var18.textRenderer;
                     String var20 = String.valueOf(var17.getCount());
                     var0.drawText(var19, var20, (int)(var9[0] + 9.0F - var19.getWidth(var20)), (int)(var9[1] + 1.0F), 16777215, true);
                  }
               }
            }
         }
      }

      renderPops(var5);
   }

   public static void spawnPop(float var0, float var1, Color var2) {
      POPS.add(new KitOverlayRenderer.Pop(var0, var1, var2));
   }

   public static void clearPops() {
      POPS.clear();
   }

   public static void renderPops(MatrixStack var0) {
      if (!POPS.isEmpty()) {
         long var1 = System.currentTimeMillis();
         POPS.removeIf(var2 -> var2.isDead(var1));

         for (KitOverlayRenderer.Pop var4 : POPS) {
            float var5 = var4.progress(var1);
            float var6 = 2.0F + 13.0F * var5;
            int var7 = (int)(200.0F * (1.0F - var5) * (1.0F - var5));
            if (var7 > 0) {
               Color var8 = var4.tint;
               Render2DUtil.drawCircle(var0, var4.x, var4.y, var6, new Color(var8.getRed(), var8.getGreen(), var8.getBlue(), Math.min(255, var7)), 32);
               int var9 = (int)(160.0F * (1.0F - var5));
               if (var9 > 0) {
                  Render2DUtil.drawCircle(var0, var4.x, var4.y, var6 * 0.45F, new Color(255, 255, 255, Math.min(255, var9)), 24);
               }
            }
         }
      }
   }

   public static Color colorHint(ShulkerColor var0) {
      return switch (var0) {
         case White -> new Color(240, 240, 240);
         case Orange -> new Color(240, 150, 50);
         case Magenta -> new Color(200, 80, 200);
         case LightBlue -> new Color(90, 180, 240);
         case Yellow -> new Color(240, 220, 60);
         case Lime -> new Color(120, 220, 60);
         case Pink -> new Color(240, 150, 190);
         case Gray -> new Color(110, 110, 110);
         case LightGray -> new Color(170, 170, 170);
         case Cyan -> new Color(40, 200, 200);
         case Purple -> new Color(140, 70, 200);
         case Blue -> new Color(60, 90, 220);
         case Brown -> new Color(130, 90, 60);
         case Green -> new Color(70, 150, 60);
         case Red -> new Color(200, 60, 60);
         case Black -> new Color(50, 50, 50);
      };
   }

   private static final class Pop {
      static final float LIFETIME_MS = 620.0F;
      final float x;
      final float y;
      final Color tint;
      final long born;

      Pop(float var1, float var2, Color var3) {
         this.x = var1;
         this.y = var2;
         this.tint = var3;
         this.born = System.currentTimeMillis();
      }

      float progress(long var1) {
         return Math.min(1.0F, (float)(var1 - this.born) / 620.0F);
      }

      boolean isDead(long var1) {
         return var1 - this.born >= 620L;
      }
   }
}
