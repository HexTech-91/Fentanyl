package dev.luminous.mod.gui.elements;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.HudManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.hud.InGameHud;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;

public class InGameHudElements extends Tab {
   private final KeyBinding keybindUp;
   private final KeyBinding keybindDown;
   private final KeyBinding keybindLeft;
   private final KeyBinding keybindRight;
   int index = 0;
   int indexMods = 0;
   boolean isCategoryMenuOpen = false;
   Module.Category[] categories;
   ArrayList<Module> modules = new ArrayList<>();

   public InGameHudElements() {
      this.keybindUp = new KeyBinding("key.tabup", 265, "key.categories.aoba");
      this.keybindDown = new KeyBinding("key.tabdown", 264, "key.categories.aoba");
      this.keybindLeft = new KeyBinding("key.tableft", 263, "key.categories.aoba");
      this.keybindRight = new KeyBinding("key.tabright", 262, "key.categories.aoba");
      this.categories = Module.Category.values();
      this.x = Alien.CONFIG.getInt("ingamehud_x", 0);
      this.y = Alien.CONFIG.getInt("ingamehud_y", 30);
      this.width = 150;
      this.height = 30;
   }

   @Override
   public void update(double mouseX, double mouseY) {
      if (HudManager.currentGrabbed == null && InGameHud.INSTANCE.isOn()) {
         if (mouseX >= this.x && mouseX <= this.x + this.width) {
            if (mouseY >= this.y && mouseY <= this.y + this.height) {
               if (ClickGuiScreen.clicked) {
                  HudManager.currentGrabbed = this;
               }
            }
         }

         if (this.keybindUp.isPressed()) {
            if (!this.isCategoryMenuOpen) {
               if (this.index == 0) {
                  this.index = this.categories.length - 1;
               } else {
                  this.index--;
               }
            } else if (this.indexMods == 0) {
               this.indexMods = this.modules.size() - 1;
            } else {
               this.indexMods--;
            }

            this.keybindUp.setPressed(false);
         } else if (this.keybindDown.isPressed()) {
            if (!this.isCategoryMenuOpen) {
               this.index = (this.index + 1) % this.categories.length;
            } else {
               this.indexMods = (this.indexMods + 1) % this.modules.size();
            }

            this.keybindDown.setPressed(false);
         } else if (this.keybindRight.isPressed()) {
            if (!this.isCategoryMenuOpen && this.x != -this.width) {
               this.isCategoryMenuOpen = true;
               if (this.modules.isEmpty()) {
                  for (Module module : Alien.MODULE.getModules()) {
                     if (module.getCategory() == this.categories[this.index]) {
                        this.modules.add(module);
                     }
                  }
               }
            } else {
               this.modules.get(this.indexMods).toggle();
            }

            this.keybindRight.setPressed(false);
         } else if (this.keybindLeft.isPressed()) {
            if (this.isCategoryMenuOpen) {
               this.indexMods = 0;
               this.modules.clear();
               this.isCategoryMenuOpen = false;
            }

            this.keybindLeft.setPressed(false);
         }
      }
   }

   @Override
   public void draw(DrawContext drawContext, float partialTicks, Color color) {
      MatrixStack matrixStack = drawContext.getMatrices();
      matrixStack.push();
      if (InGameHud.INSTANCE.isOn()) {
         Render2DUtil.drawRectOutline(matrixStack, this.x, this.y, this.width, this.height * this.categories.length, new Color(30, 30, 30), 0.4F);

         for (int i = 0; i < this.categories.length; i++) {
            if (this.index == i) {
               TextUtil.drawString(drawContext, "> §f" + this.categories[i].name(), (float)(this.x + 8), (float)(this.y + this.height * i + 8), color.getRGB());
            } else {
               TextUtil.drawString(drawContext, this.categories[i].name(), this.x + 8, this.y + this.height * i + 8, 16777215);
            }
         }

         if (this.isCategoryMenuOpen) {
            Render2DUtil.drawRectOutline(
               matrixStack, this.x + this.width, this.y + this.height * this.index, 165.0F, this.height * this.modules.size(), new Color(30, 30, 30), 0.4F
            );

            for (int ix = 0; ix < this.modules.size(); ix++) {
               if (this.indexMods == ix) {
                  if (this.modules.get(ix).isOn()) {
                     TextUtil.drawString(
                        drawContext,
                        "> " + this.modules.get(ix).getName(),
                        this.x + this.width + 5,
                        this.y + ix * this.height + this.index * this.height + 8,
                        color.getRGB()
                     );
                  } else {
                     TextUtil.drawString(
                        drawContext,
                        "> §f" + this.modules.get(ix).getName(),
                        this.x + this.width + 5,
                        this.y + ix * this.height + this.index * this.height + 8,
                        color.getRGB()
                     );
                  }
               } else {
                  TextUtil.drawString(
                     drawContext,
                     this.modules.get(ix).getName(),
                     this.x + this.width + 5,
                     this.y + ix * this.height + this.index * this.height + 8,
                     this.modules.get(ix).isOn() ? color.getRGB() : 16777215
                  );
               }
            }
         }
      }

      matrixStack.pop();
   }
}
