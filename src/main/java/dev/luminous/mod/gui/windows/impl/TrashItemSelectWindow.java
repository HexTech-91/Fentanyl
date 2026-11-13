package dev.luminous.mod.gui.windows.impl;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.windows.WindowBase;
import dev.luminous.mod.modules.impl.survival.AutoTrash;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public class TrashItemSelectWindow extends WindowBase {
   private final AutoTrash module;
   private final List<Item> items = new ArrayList<>();
   private String search = "";
   private int scroll;
   private int maxScroll;

   public TrashItemSelectWindow(AutoTrash var1) {
      super("TrashItems");
      this.module = var1;

      for (Item var3 : Registries.ITEM) {
         this.items.add(var3);
      }

      this.items.sort(Comparator.comparing(var0 -> var0.getName().getString(), String.CASE_INSENSITIVE_ORDER));
   }

   private List<Item> filtered() {
      String var1 = this.search.trim().toLowerCase();
      if (var1.isEmpty()) {
         return this.items;
      } else {
         ArrayList var2 = new ArrayList();

         for (Item var4 : this.items) {
            if (var4.getName().getString().toLowerCase().contains(var1)) {
               var2.add(var4);
            }
         }

         return var2;
      }
   }

   @Override
   public void drawScreen(DrawContext var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8) {
      dev.luminous.mod.gui.items.Item.context = var1;
      int var9 = var2 + 5;
      int var10 = var3 + 5;
      int var11 = var4 - 10;
      Render2DUtil.rect(var1.getMatrices(), var9, var10, var9 + var11, var10 + 14, new Color(35, 35, 35, 240).getRGB());
      this.drawString(
         this.search.isEmpty() ? "\u641c\u7d22\u7269\u54c1..." : this.search,
         var9 + 4,
         var10 + 3,
         this.search.isEmpty() ? new Color(150, 150, 150).getRGB() : Color.WHITE.getRGB()
      );
      int var12 = var10 + 17;
      int var13 = var5 - 22;
      int var14 = this.getFontHeight() + 4;
      List var15 = this.filtered();
      int var16 = Math.max(1, var13 / var14);
      this.maxScroll = Math.max(0, var15.size() - var16);
      this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll));
      Render2DUtil.rect(var1.getMatrices(), var9, var12, var9 + var11, var12 + var13, new Color(15, 15, 15, 230).getRGB());

      for (int var17 = this.scroll; var17 < Math.min(var15.size(), this.scroll + var16 + 1); var17++) {
         Item var18 = (Item)var15.get(var17);
         int var19 = var12 + (var17 - this.scroll) * var14;
         boolean var20 = this.module.isSelected(var18);
         if (var20) {
            Render2DUtil.rect(var1.getMatrices(), var9, var19, var9 + var11, var19 + var14, new Color(85, 45, 130, 210).getRGB());
         }

         this.drawString((var20 ? "[\u2713] " : "[ ] ") + var18.getName().getString(), var9 + 3, var19 + 2, Color.WHITE.getRGB());
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var3 + 5;
      int var9 = var4 + 22;
      int var10 = this.getFontHeight() + 4;
      if (var1 >= var8 && var1 <= var3 + var5 - 5 && var2 >= var9 && var2 <= var4 + var6 - 2) {
         List var11 = this.filtered();
         int var12 = this.scroll + (var2 - var9) / var10;
         if (var12 >= 0 && var12 < var11.size()) {
            this.module.toggle((Item)var11.get(var12));
         }
      }
   }

   @Override
   public void mouseScrolled(int var1, int var2, int var3, int var4, int var5, int var6, double var7) {
      this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll - (int)Math.signum(var7)));
   }

   @Override
   public void keyPressed(int var1, int var2, int var3, int var4, int var5) {
      if (var1 == 259 && !this.search.isEmpty()) {
         this.search = this.search.substring(0, this.search.length() - 1);
      }

      if (var1 == 256 && AutoTrash.mc.currentScreen != null) {
         AutoTrash.mc.setScreen(null);
      }
   }

   @Override
   public void charTyped(char var1, int var2, int var3, int var4, int var5) {
      if (Character.isLetterOrDigit(var1) || Character.isWhitespace(var1) || var1 >= 12288) {
         this.search = this.search + var1;
      }
   }

   @Override
   protected int getFontHeight() {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getFontHeight() : 9;
   }
}
