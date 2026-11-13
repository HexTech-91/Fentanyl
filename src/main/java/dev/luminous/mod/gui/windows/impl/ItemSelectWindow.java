package dev.luminous.mod.gui.windows.impl;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.windows.WindowBase;
import dev.luminous.mod.modules.Module;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class ItemSelectWindow extends WindowBase {
   private final List<Module> displayedModules = new ArrayList<>();
   private float scrollOffset = 0.0F;
   private float maxScroll = 0.0F;
   private boolean isDraggingScroll = false;
   private float dragStartY = 0.0F;
   private float dragStartOffset = 0.0F;
   private Module.Category currentCategory = null;
   private int selectedIndex = 0;
   private final Manager manager;
   private final ArrayList<String> whitelist = new ArrayList<>();

   public ItemSelectWindow() {
      super("ItemSelect");
      this.manager = null;
      this.selectCategory(Module.Category.Combat);
   }

   public ItemSelectWindow(Manager manager) {
      super("ItemSelect");
      this.manager = manager;
      if (manager != null) {
         this.whitelist.addAll(manager.getList());
      }

      this.selectCategory(Module.Category.Combat);
   }

   private void selectCategory(Module.Category category) {
      this.currentCategory = category;
      this.displayedModules.clear();
      this.scrollOffset = 0.0F;

      for (Module module : Alien.MODULE.getModules()) {
         if (module.getCategory() == category) {
            this.displayedModules.add(module);
         }
      }

      this.displayedModules.sort(Comparator.comparing(Mod::getName));
   }

   @Override
   public void drawScreen(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
      if (this.currentCategory != null) {
         Item.context = context;
         int padding = 4;
         int innerX = x + padding;
         int innerY = y + padding;
         int innerWidth = width - padding * 2;
         int innerHeight = height - padding * 2;
         int categoryTabHeight = 14;
         int searchBarHeight = 12;
         int categoryX = innerX;
         int categoryY = innerY;

         for (Module.Category cat : Module.Category.values()) {
            int catWidth = this.getCategoryWidth(cat);
            boolean isSelected = cat == this.currentCategory;
            Render2DUtil.rect(
               context.getMatrices(),
               categoryX,
               categoryY,
               categoryX + catWidth,
               categoryY + categoryTabHeight,
               isSelected ? -1 : new Color(60, 60, 60).getRGB()
            );
            this.drawString(
               cat.toString(), categoryX + 2.0F, categoryY + (categoryTabHeight - this.getFontHeight()) / 2.0F, isSelected ? new Color(0, 0, 0).getRGB() : -1
            );
            categoryX += catWidth + 1;
         }

         int listY = categoryY + categoryTabHeight + 2;
         int listHeight = innerY + innerHeight - listY;
         int itemHeight = this.getFontHeight() + 4;
         int maxVisibleItems = Math.max(0, listHeight / itemHeight);
         int totalItems = this.displayedModules.size();
         this.maxScroll = Math.max(0.0F, (float)(totalItems - maxVisibleItems));
         if (this.scrollOffset > this.maxScroll) {
            this.scrollOffset = this.maxScroll;
         }

         if (this.scrollOffset < 0.0F) {
            this.scrollOffset = 0.0F;
         }

         int startIndex = (int)this.scrollOffset;
         int endIndex = Math.min(totalItems, startIndex + maxVisibleItems + 1);
         Render2DUtil.rect(context.getMatrices(), innerX, listY, innerX + innerWidth, listY + listHeight, new Color(20, 20, 20, 220).getRGB());

         for (int i = startIndex; i < endIndex; i++) {
            if (i >= 0 && i < totalItems) {
               Module module = this.displayedModules.get(i);
               int itemY = (int)(listY + (i - this.scrollOffset) * itemHeight);
               boolean isSelected = i == this.selectedIndex;
               if (this.isHovering(mouseX, mouseY, innerX, itemY, innerWidth, itemHeight)) {
                  Render2DUtil.rect(context.getMatrices(), innerX, itemY, innerX + innerWidth, itemY + itemHeight, new Color(80, 80, 80, 200).getRGB());
                  this.selectedIndex = i;
               } else if (isSelected) {
                  Render2DUtil.rect(context.getMatrices(), innerX, itemY, innerX + innerWidth, itemY + itemHeight, new Color(60, 60, 60, 200).getRGB());
               }

               this.drawString(
                  module.getName(), innerX + 4.0F, itemY + (itemHeight - this.getFontHeight()) / 2.0F, module.isOn() ? -1 : new Color(160, 160, 160).getRGB()
               );
            }
         }

         if (this.maxScroll > 0.0F) {
            int scrollBarX = innerX + innerWidth - 4;
            int scrollBarW = 3;
            float scrollBarH = Math.max(20.0F, listHeight * ((float)maxVisibleItems / totalItems));
            float scrollBarY = listY + (listHeight - scrollBarH) * (this.scrollOffset / this.maxScroll);
            Render2DUtil.rect(
               context.getMatrices(), scrollBarX, scrollBarY, scrollBarX + scrollBarW, scrollBarY + scrollBarH, new Color(100, 100, 100).getRGB()
            );
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int x, int y, int width, int height, int button) {
      int padding = 4;
      int innerX = x + padding;
      int innerY = y + padding;
      int innerWidth = width - padding * 2;
      int innerHeight = height - padding * 2;
      int categoryTabHeight = 14;
      int categoryX = innerX;
      int categoryY = innerY;

      for (Module.Category cat : Module.Category.values()) {
         int catWidth = this.getCategoryWidth(cat);
         if (mouseX >= categoryX && mouseX <= categoryX + catWidth && mouseY >= categoryY && mouseY <= categoryY + categoryTabHeight) {
            this.selectCategory(cat);
            this.selectedIndex = 0;
            return;
         }

         categoryX += catWidth + 1;
      }

      int listY = categoryY + categoryTabHeight + 2;
      int listHeight = innerY + innerHeight - listY;
      int itemHeight = this.getFontHeight() + 4;
      if (mouseX >= innerX && mouseX <= innerX + innerWidth && mouseY >= listY && mouseY <= listY + listHeight) {
         int relativeIndex = (int)((mouseY - listY) / itemHeight + this.scrollOffset);
         if (relativeIndex >= 0 && relativeIndex < this.displayedModules.size()) {
            this.selectedIndex = relativeIndex;
            Module module = this.displayedModules.get(relativeIndex);
            module.toggle();
         }
      }
   }

   @Override
   public void mouseScrolled(int mouseX, int mouseY, int x, int y, int width, int height, double verticalAmount) {
      int padding = 4;
      int innerY = y + padding;
      int innerHeight = height - padding * 2;
      int categoryTabHeight = 14;
      int listY = innerY + categoryTabHeight + 2;
      int listHeight = innerY + innerHeight - listY;
      if (mouseX >= x + padding && mouseX <= x + width - padding && mouseY >= listY && mouseY <= listY + listHeight) {
         this.scrollOffset = (float)(this.scrollOffset - verticalAmount);
         this.scrollOffset = Math.max(0.0F, Math.min(this.scrollOffset, this.maxScroll));
      }
   }

   private int getCategoryWidth(Module.Category category) {
      return this.getWidth(category.toString()) + 4;
   }

   @Override
   protected int getWidth(String text) {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getWidth(text) : mc.textRenderer.getWidth(text);
   }

   @Override
   protected int getFontHeight() {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getFontHeight() : 9;
   }
}
