package dev.luminous.mod.gui.windows.impl;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.windows.WindowBase;
import dev.luminous.mod.modules.impl.survival.AutoRide;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;

public class RideAnimalSelectWindow extends WindowBase {
   private static final Map<String, String> NAMES = Map.of(
      "minecraft:horse",
      "\u9a6c",
      "minecraft:donkey",
      "\u9a74",
      "minecraft:mule",
      "\u9ab4\u5b50",
      "minecraft:llama",
      "\u7f8a\u9a7c",
      "minecraft:trader_llama",
      "\u884c\u5546\u7f8a\u9a7c",
      "minecraft:camel",
      "\u9a86\u9a7c",
      "minecraft:pig",
      "\u732a",
      "minecraft:strider",
      "\u70bd\u8db3\u517d",
      "minecraft:skeleton_horse",
      "\u9ab7\u9ac5\u9a6c",
      "minecraft:zombie_horse",
      "\u50f5\u5c38\u9a6c"
   );
   private final AutoRide module;
   private final List<String> animals = new ArrayList<>(AutoRide.RIDEABLE);
   private String search = "";
   private int scroll;
   private int maxScroll;

   public RideAnimalSelectWindow(AutoRide var1) {
      super("RideAnimals");
      this.module = var1;
      this.animals.sort(Comparator.comparing(NAMES::get));
   }

   private List<String> filtered() {
      if (this.search.trim().isEmpty()) {
         return this.animals;
      } else {
         String var1 = this.search.toLowerCase(Locale.ROOT);
         ArrayList var2 = new ArrayList();

         for (String var4 : this.animals) {
            if (NAMES.get(var4).contains(var1) || var4.contains(var1)) {
               var2.add(var4);
            }
         }

         return var2;
      }
   }

   @Override
   public void drawScreen(DrawContext var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8) {
      Item.context = var1;
      int var9 = var2 + 5;
      int var10 = var3 + 5;
      int var11 = var4 - 10;
      Render2DUtil.rect(var1.getMatrices(), var9, var10, var9 + var11, var10 + 14, new Color(35, 35, 35, 240).getRGB());
      this.drawString(
         this.search.isEmpty() ? "\u641c\u7d22\u52a8\u7269..." : this.search,
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
         String var18 = (String)var15.get(var17);
         int var19 = var12 + (var17 - this.scroll) * var14;
         boolean var20 = this.module.isSelected(var18);
         if (var20) {
            Render2DUtil.rect(var1.getMatrices(), var9, var19, var9 + var11, var19 + var14, new Color(85, 45, 130, 210).getRGB());
         }

         this.drawString((var20 ? "[x] " : "[ ] ") + NAMES.get(var18), var9 + 3, var19 + 2, Color.WHITE.getRGB());
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
            this.module.toggle((String)var11.get(var12));
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

      if (var1 == 256 && AutoRide.mc.currentScreen != null) {
         AutoRide.mc.setScreen(null);
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
