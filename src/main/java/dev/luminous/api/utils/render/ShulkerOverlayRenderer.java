package dev.luminous.api.utils.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class ShulkerOverlayRenderer {
   public static final int CELL = 18;
   public static final int COLUMNS = 9;
   public static final int ROWS = 3;
   private static final int PAD = 3;
   private static final float BORDER = 1.5F;
   public static final Color BORDER_COLOR = new Color(0, 220, 220, 220);
   public static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 40);
   private static final List<ShulkerOverlayRenderer.Pop> POPS = new ArrayList<>();
   private static Map<Integer, Integer> lastCounts;
   private static int lastSyncId = Integer.MIN_VALUE;
   private static final int[] LAST_PANEL = new int[4];
   private static boolean panelValid = false;

   private ShulkerOverlayRenderer() {
   }

   public static int panelWidth() {
      return 168;
   }

   public static int panelHeight() {
      return 60;
   }

   public static void layout(int var0, int var1, double var2, double var4, int[] var6) {
      int var7 = panelWidth();
      int var8 = panelHeight();
      int var9 = (int)Math.round(var2 - var7 / 2.0);
      int var10 = (int)Math.round(var4 - var8 / 2.0);
      var9 = Math.max(0, Math.min(var9, Math.max(0, var0 - var7)));
      var10 = Math.max(0, Math.min(var10, Math.max(0, var1 - var8)));
      var6[0] = var9;
      var6[1] = var10;
      var6[2] = var7;
      var6[3] = var8;
   }

   public static void slotCentre(int var0, int var1, int var2, float[] var3) {
      int var4 = var2 % 9;
      int var5 = var2 / 9;
      var3[0] = var0 + 3 + var4 * 18 + 9.0F;
      var3[1] = var1 + 3 + var5 * 18 + 9.0F;
   }

   public static int slotAt(double var0, double var2, int var4, int var5) {
      double var6 = var0 - (var4 + 3);
      double var8 = var2 - (var5 + 3);
      if (!(var6 < 0.0) && !(var8 < 0.0)) {
         int var10 = (int)(var6 / 18.0);
         int var11 = (int)(var8 / 18.0);
         return var10 >= 0 && var10 < 9 && var11 >= 0 && var11 < 3 ? var11 * 9 + var10 : -1;
      } else {
         return -1;
      }
   }

   public static void spawnPop(float var0, float var1, Color var2) {
      POPS.add(new ShulkerOverlayRenderer.Pop(var0, var1, var2, System.currentTimeMillis()));
   }

   public static void clearPops() {
      POPS.clear();
      lastCounts = null;
      lastSyncId = Integer.MIN_VALUE;
      panelValid = false;
   }

   public static int[] lastPanel() {
      return panelValid ? LAST_PANEL : null;
   }

   private static void detectTakenSlots(ShulkerBoxScreenHandler var0, int var1, int var2) {
      HashMap var3 = new HashMap();

      for (int var4 = 0; var4 < var0.slots.size() && var4 < 27; var4++) {
         ItemStack var5 = ((Slot)var0.slots.get(var4)).getStack();
         if (var5 != null && !var5.isEmpty()) {
            var3.put(var4, var5.getCount());
         }
      }

      Map<Integer, Integer> var9 = lastCounts;
      int var10 = lastSyncId;
      lastCounts = var3;
      lastSyncId = var0.syncId;
      if (var9 != null && var10 == var0.syncId) {
         for (Integer var7 : var9.keySet()) {
            if (!var3.containsKey(var7)) {
               float[] var8 = new float[2];
               slotCentre(var1, var2, var7, var8);
               spawnPop(var8[0], var8[1], BORDER_COLOR);
            }
         }
      }
   }

   public static void render(DrawContext var0, ShulkerBoxScreenHandler var1, int var2, int var3, ItemStack var4) {
      detectTakenSlots(var1, var2, var3);
      MatrixStack var5 = var0.getMatrices();
      int var6 = panelWidth();
      int var7 = panelHeight();
      LAST_PANEL[0] = var2;
      LAST_PANEL[1] = var3;
      LAST_PANEL[2] = var6;
      LAST_PANEL[3] = var7;
      panelValid = true;
      Render2DUtil.drawRect(var5, (float)var2, (float)var3, (float)var6, (float)var7, BACKGROUND_COLOR);
      int var8 = BORDER_COLOR.getRGB();
      Render2DUtil.drawRect(var5, var2, var3, var6, 1.5F, var8);
      Render2DUtil.drawRect(var5, var2, var3 + var7 - 1.5F, var6, 1.5F, var8);
      Render2DUtil.drawRect(var5, var2, var3, 1.5F, var7, var8);
      Render2DUtil.drawRect(var5, var2 + var6 - 1.5F, var3, 1.5F, var7, var8);
      Color var9 = new Color(255, 255, 255, 26);

      for (int var10 = 1; var10 < 9; var10++) {
         float var11 = var2 + 3 + var10 * 18;
         Render2DUtil.drawRect(var5, var11, var3 + 3, 0.5F, 54.0F, var9.getRGB());
      }

      for (int var19 = 1; var19 < 3; var19++) {
         float var21 = var3 + 3 + var19 * 18;
         Render2DUtil.drawRect(var5, var2 + 3, var21, 162.0F, 0.5F, var9.getRGB());
      }

      int var20 = Math.min(27, var1.slots.size());
      float[] var22 = new float[2];

      for (int var12 = 0; var12 < var20; var12++) {
         ItemStack var13 = ((Slot)var1.slots.get(var12)).getStack();
         if (var13 != null && !var13.isEmpty()) {
            slotCentre(var2, var3, var12, var22);
            float var14 = var22[0] - 8.0F;
            float var15 = var22[1] - 8.0F;
            var0.drawItem(var13, (int)var14, (int)var15);
            if (var13.getCount() > 1) {
               String var16 = String.valueOf(var13.getCount());
               MinecraftClient var17 = MinecraftClient.getInstance();
               int var18 = var17.textRenderer.getWidth(var16);
               var0.drawText(var17.textRenderer, var16, (int)(var14 + 17.0F - var18), (int)(var15 + 9.0F), 16777215, true);
            }
         }
      }

      if (var4 != null && !var4.isEmpty()) {
         MinecraftClient var23 = MinecraftClient.getInstance();
         var0.drawItemWithoutEntity(var4, (int)var23.mouse.getX(), (int)var23.mouse.getY());
      }

      renderPops(var5);
   }

   private static void renderPops(MatrixStack var0) {
      if (!POPS.isEmpty()) {
         long var1 = System.currentTimeMillis();
         var0.push();
         Iterator var3 = POPS.iterator();

         while (var3.hasNext()) {
            ShulkerOverlayRenderer.Pop var4 = (ShulkerOverlayRenderer.Pop)var3.next();
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
