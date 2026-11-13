package dev.luminous.api.utils.render;

import dev.luminous.api.utils.misc.ShulkerColor;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class EnderChestOverlayRenderer {
   public static final int CELL = 18;
   private static final int PAD = 3;
   private static final float BORDER = 1.5F;
   public static final Color BORDER_COLOR = new Color(0, 220, 220, 220);
   public static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 40);
   private static final List<EnderChestOverlayRenderer.Pop> POPS = new ArrayList<>();

   private EnderChestOverlayRenderer() {
   }

   public static int panelWidth(int var0) {
      return var0 * 18 + 6;
   }

   public static int panelHeight(int var0) {
      return var0 * 18 + 6;
   }

   public static void layout(int var0, int var1, double var2, double var4, int var6, int var7, int[] var8) {
      int var9 = panelWidth(var6);
      int var10 = panelHeight(var7);
      int var11 = (int)Math.round(var2 - var9 / 2.0);
      int var12 = (int)Math.round(var4 - var10 / 2.0);
      var11 = Math.max(0, Math.min(var11, Math.max(0, var0 - var9)));
      var12 = Math.max(0, Math.min(var12, Math.max(0, var1 - var10)));
      var8[0] = var11;
      var8[1] = var12;
      var8[2] = var9;
      var8[3] = var10;
   }

   public static void slotCentre(int var0, int var1, int var2, int var3, float[] var4) {
      int var5 = var2 % var3;
      int var6 = var2 / var3;
      var4[0] = var0 + 3 + var5 * 18 + 9.0F;
      var4[1] = var1 + 3 + var6 * 18 + 9.0F;
   }

   public static int slotAt(double var0, double var2, int var4, int var5, int var6, int var7) {
      double var8 = var0 - (var4 + 3);
      double var10 = var2 - (var5 + 3);
      if (!(var8 < 0.0) && !(var10 < 0.0)) {
         int var12 = (int)(var8 / 18.0);
         int var13 = (int)(var10 / 18.0);
         return var12 >= 0 && var12 < var6 && var13 >= 0 && var13 < var7 ? var13 * var6 + var12 : -1;
      } else {
         return -1;
      }
   }

   public static void spawnPop(float var0, float var1, Color var2) {
      POPS.add(new EnderChestOverlayRenderer.Pop(var0, var1, var2, System.currentTimeMillis()));
   }

   public static void clearPops() {
      POPS.clear();
   }

   public static void markTakenUnlessPartial(ScreenHandler var0, int var1, int var2, int var3, int var4, int var5) {
      if (var0 != null && var1 >= 0 && var1 < var0.slots.size()) {
         ItemStack var6 = ((Slot)var0.slots.get(var1)).getStack();
         if (var6 != null && !var6.isEmpty()) {
            if (var2 != 1 || var6.getCount() <= 1) {
               float[] var7 = new float[2];
               slotCentre(var3, var4, var1, var5, var7);
               Color var8 = BORDER_COLOR;
               ShulkerColor var9 = ShulkerColor.of(var6);
               if (var9 != null) {
                  var8 = colorHint(var9);
               }

               spawnPop(var7[0], var7[1], var8);
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

   public static void render(DrawContext var0, ScreenHandler var1, int var2, int var3, int var4, int var5, ItemStack var6) {
      MatrixStack var7 = var0.getMatrices();
      int var8 = panelWidth(var2);
      int var9 = panelHeight(var3);
      Render2DUtil.drawRect(var7, (float)var4, (float)var5, (float)var8, (float)var9, BACKGROUND_COLOR);
      int var10 = BORDER_COLOR.getRGB();
      Render2DUtil.drawRect(var7, var4, var5, var8, 1.5F, var10);
      Render2DUtil.drawRect(var7, var4, var5 + var9 - 1.5F, var8, 1.5F, var10);
      Render2DUtil.drawRect(var7, var4, var5, 1.5F, var9, var10);
      Render2DUtil.drawRect(var7, var4 + var8 - 1.5F, var5, 1.5F, var9, var10);
      Color var11 = new Color(255, 255, 255, 26);

      for (int var12 = 1; var12 < var2; var12++) {
         float var13 = var4 + 3 + var12 * 18;
         Render2DUtil.drawRect(var7, var13, var5 + 3, 0.5F, var3 * 18, var11.getRGB());
      }

      for (int var21 = 1; var21 < var3; var21++) {
         float var23 = var5 + 3 + var21 * 18;
         Render2DUtil.drawRect(var7, var4 + 3, var23, var2 * 18, 0.5F, var11.getRGB());
      }

      int var22 = Math.min(var2 * var3, var1.slots.size());
      float[] var24 = new float[2];

      for (int var14 = 0; var14 < var22; var14++) {
         ItemStack var15 = ((Slot)var1.slots.get(var14)).getStack();
         if (var15 != null && !var15.isEmpty()) {
            slotCentre(var4, var5, var14, var2, var24);
            float var16 = var24[0] - 8.0F;
            float var17 = var24[1] - 8.0F;
            var0.drawItem(var15, (int)var16, (int)var17);
            if (var15.getCount() > 1) {
               String var18 = String.valueOf(var15.getCount());
               MinecraftClient var19 = MinecraftClient.getInstance();
               int var20 = var19.textRenderer.getWidth(var18);
               var0.drawText(var19.textRenderer, var18, (int)(var16 + 17.0F - var20), (int)(var17 + 9.0F), 16777215, true);
            }
         }
      }

      if (var6 != null && !var6.isEmpty()) {
         MinecraftClient var25 = MinecraftClient.getInstance();
         var0.drawItemWithoutEntity(var6, (int)var25.mouse.getX(), (int)var25.mouse.getY());
      }

      renderPops(var7);
   }

   public static void previewSlot(ScreenHandler var0, int var1) {
      if (var0 != null && var1 >= 0 && var1 < var0.slots.size()) {
         ItemStack var2 = ((Slot)var0.slots.get(var1)).getStack();
         if (var2 != null && !var2.isEmpty() && ShulkerColor.isAnyShulker(var2)) {
            ShulkerViewer.openContainer(var2, new ItemStack[27], false);
         }
      }
   }

   private static void renderPops(MatrixStack var0) {
      if (!POPS.isEmpty()) {
         long var1 = System.currentTimeMillis();
         var0.push();
         Iterator var3 = POPS.iterator();

         while (var3.hasNext()) {
            EnderChestOverlayRenderer.Pop var4 = (EnderChestOverlayRenderer.Pop)var3.next();
            float var5 = (float)(var1 - var4.startMs) / 620.0F;
            if (var5 >= 1.0F) {
               var3.remove();
            } else {
               float var6 = 2.0F + 13.0F * var5;
               int var7 = (int)(200.0F * (1.0F - var5) * (1.0F - var5));
               if (var7 > 2) {
                  Color var8 = new Color(var4.tint.getRed(), var4.tint.getGreen(), var4.tint.getBlue(), Math.min(var7, 255));
                  Render2DUtil.drawCircle(var0, var4.x, var4.y, var6, var8, 48);
                  int var9 = (int)(160.0F * (1.0F - var5));
                  if (var9 > 2) {
                     Color var10 = new Color(255, 255, 255, Math.min(var9, 255));
                     Render2DUtil.drawCircle(var0, var4.x, var4.y, Math.max(0.5F, 1.5F * (1.0F - var5)), var10, 24);
                  }
               }
            }
         }

         var0.pop();
      }
   }

   private static final class Pop {
      static final float LIFETIME_MS = 620.0F;
      final float x;
      final float y;
      final Color tint;
      final long startMs;
      final float maxRadius = 13.0F;

      Pop(float var1, float var2, Color var3, long var4) {
         this.x = var1;
         this.y = var2;
         this.tint = var3 == null ? new Color(0, 220, 220) : var3;
         this.startMs = var4;
      }
   }
}
