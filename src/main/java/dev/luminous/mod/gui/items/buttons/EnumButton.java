package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;

public class EnumButton extends Button {
   public final EnumSetting<?> setting;
   private boolean open;

   public EnumButton(EnumSetting<?> setting) {
      super(setting.getDisplayName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      double baseDelay = this.getColorDelay();
      float w = this.width + 7.0F;
      float h = this.height - 0.5F;
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum && this.getState()) {
         int keyCodec = hovered ? ClickGui.getInstance().hoverAlpha.getValueInt() : ClickGui.getInstance().alpha.getValueInt();
         Render2DUtil.drawLutRect(
            context.getMatrices(), this.x, this.y, w, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), keyCodec
         );
      } else {
         Color color = ClickGui.getInstance().getColor(baseDelay);
         Render2DUtil.rect(
            context.getMatrices(),
            this.x,
            this.y,
            this.x + w,
            this.y + this.height - 0.5F,
            this.getState()
               ? (
                  !hovered
                     ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB()
                     : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()
               )
               : (!hovered ? defaultColor : hoverColor)
         );
      }

      float textY = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString(
            this.setting.getName().equalsIgnoreCase("Page") ? "\u91cd\u7f6e\u9875\u9762" : "\u6062\u590d\u9ed8\u8ba4", this.x + 2.3F, textY, enableTextColor
         );
      } else {
         this.drawString(
            this.setting.getDisplayName()
               + " "
               + Formatting.GRAY
               + (
                  FeatureTranslations.translateEnum(this.setting.getValue()).equalsIgnoreCase("ABC")
                     ? "ABC"
                     : FeatureTranslations.translateEnum(this.setting.getValue())
               ),
            this.x + 2.3F,
            textY,
            this.getState() ? enableTextColor : defaultTextColor
         );
      }

      if (this.open) {
         int y = (int)this.y;

         for (Enum e : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
            String s = FeatureTranslations.translateEnum(e);
            float optionY = this.getCenteredTextY((float)y + this.height + 1.0F, 11.0F);
            this.drawString(
               s,
               this.width / 2.0F - this.getWidth(s) / 2.0F + 2.0F + this.x,
               optionY,
               FeatureTranslations.translateEnum(this.setting.getValue()).equals(s) ? enableTextColor : defaultTextColor
            );
            y += 11;
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton != 0 || !this.isHovering(mouseX, mouseY) || !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         super.mouseClicked(mouseX, mouseY, mouseButton);
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            sound();
         } else if (mouseButton == 0 && this.open) {
            int y = (int)this.y;

            for (Enum o : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
               if (mouseX > this.x && mouseX < this.x + this.width && mouseY >= y + this.height + 1 && mouseY < y + this.height + 11 + 1) {
                  this.setting.setEnumValue(String.valueOf(o));
                  sound();
               }

               y += 11;
            }
         }
      } else if (!this.setting.getName().equalsIgnoreCase("Page")) {
         Enum defaultValue = this.setting.getDefaultValue();
         this.setting.setEnumValue(defaultValue.name());
         sound();
      } else {
         boolean resetPage = false;

         for (Component component : ClickGuiScreen.getInstance().getComponents()) {
            for (ModuleButton moduleButton : component.getItems()) {
               if (moduleButton.getModule().getSettings().contains(this.setting)) {
                  moduleButton.resetPageSettingsToDefault(this.setting);
                  resetPage = true;
                  break;
               }
            }

            if (resetPage) {
               break;
            }
         }

         sound();
      }
   }

   @Override
   public int getHeight() {
      return super.getHeight() + (this.open ? 11 * ((Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()).length : 0);
   }

   @Override
   public void toggle() {
      this.setting.increaseEnum();
   }

   @Override
   public boolean getState() {
      return true;
   }
}
