package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public class BooleanButton extends Button {
   private final BooleanSetting setting;
   private final Animation toggleAnimation = new Animation();

   public BooleanButton(BooleanSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      double initial = setting.getValue() ? 1.0 : 0.0;
      this.toggleAnimation.from = initial;
      this.toggleAnimation.to = initial;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double toggleProgress = this.toggleAnimation.get(this.getState() ? 1.0 : 0.0, 160L, Easing.CubicInOut);
      double baseDelay = this.getColorDelay();
      int accentA = hovered ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
      Color unpressedFill = new Color(hovered ? hoverColor : defaultColor, true);
      float w = this.width + 7.0F;
      float h = this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         if (toggleProgress >= 0.999) {
            Render2DUtil.drawLutRect(
               context.getMatrices(), this.x, this.y, w, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), accentA
            );
         } else {
            Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + h, unpressedFill.getRGB());
            int overlayA = (int)Math.round(accentA * toggleProgress);
            if (overlayA > 0) {
               Render2DUtil.drawLutRect(
                  context.getMatrices(),
                  this.x,
                  this.y,
                  w,
                  h,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  overlayA
               );
            }
         }
      } else {
         Color accent = ClickGui.getInstance().getColor(baseDelay);
         Color pressedFill = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), accentA);
         Color baseFill = ColorUtil.fadeColor(unpressedFill, pressedFill, toggleProgress);
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + w, this.y + this.height - 0.5F, baseFill.getRGB());
      }

      float textY = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString("Reset Default", this.x + 2.3F, textY, enableTextColor);
      } else {
         this.drawString(this.setting.getDisplayName(), this.x + 2.3F, textY, this.getState() ? enableTextColor : defaultTextColor);
      }

      if (this.setting.hasParent()) {
         this.drawString(this.setting.isOpen() ? "-" : "+", this.x + this.width - 1.0F, textY, ClickGui.getInstance().gear.getValue());
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         if (this.setting.hasParent()) {
            boolean resetChildren = false;

            for (Component component : ClickGuiScreen.getInstance().getComponents()) {
               for (ModuleButton moduleButton : component.getItems()) {
                  if (moduleButton.getModule().getSettings().contains(this.setting)) {
                     moduleButton.resetChildSettingsToDefault(this.setting);
                     resetChildren = true;
                     break;
                  }
               }

               if (resetChildren) {
                  break;
               }
            }
         }

         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else {
         super.mouseClicked(mouseX, mouseY, mouseButton);
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            sound();
            this.setting.setOpen(!this.setting.isOpen());
         }
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}
