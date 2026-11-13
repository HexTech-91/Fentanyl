package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class SliderButton extends Button {
   private final double min;
   private final double max;
   private final double difference;
   public final SliderSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean drag = false;
   private final Animation valueAnimation = new Animation();

   public SliderButton(SliderSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max - this.min;
      double initial = this.partialMultiplier();
      this.valueAnimation.from = initial;
      this.valueAnimation.to = initial;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      boolean hovered = this.isHovering(mouseX, mouseY);
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + this.width + 7.0F, this.y + this.height - 0.5F, !hovered ? defaultColor : hoverColor);
      double baseDelay = this.getColorDelay();
      double fillProgress = this.valueAnimation.get(this.partialMultiplier(), 200L, Easing.CubicInOut);
      float filledX = (float)(this.x + (this.width + 7.0F) * fillProgress);
      float h = this.height - 0.5F;
      int keyCodec = !hovered ? ClickGui.getInstance().alpha.getValueInt() : ClickGui.getInstance().hoverAlpha.getValueInt();
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         float fillW = Math.max(0.0F, filledX - this.x);
         Render2DUtil.drawLutRect(
            context.getMatrices(), this.x, this.y, fillW, h, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), keyCodec
         );
      } else {
         Color color = ClickGui.getInstance().getColor(baseDelay);
         Render2DUtil.rect(context.getMatrices(), this.x, this.y, filledX, this.y + this.height - 0.5F, ColorUtil.injectAlpha(color, keyCodec).getRGB());
      }

      float textY = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (this.isListening) {
         this.drawString(this.currentString + StringButton.getIdleSign(), this.x + 2.3F, textY, this.getState() ? enableTextColor : defaultTextColor);
      } else if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString("Reset Default", this.x + 2.3F, textY, enableTextColor);
      } else {
         this.drawString(
            this.setting.getDisplayName() + " " + Formatting.GRAY + this.setting.getValueFloat() + this.setting.getSuffix(),
            this.x + 2.3F,
            textY,
            enableTextColor
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drag = false;
         this.isListening = false;
         this.currentString = "";
         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else if (this.isHovering(mouseX, mouseY)) {
         sound();
         if (mouseButton == 0) {
            if (this.isListening) {
               this.toggle();
            } else {
               this.setSettingFromX(mouseX);
               this.drag = true;
            }
         } else if (mouseButton == 1) {
            this.toggle();
         }
      }
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() + 8.0F && mouseY >= this.getY() && mouseY <= this.getY() + this.height - 1.0F;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         this.setString(this.currentString + typedChar);
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         switch (key) {
            case 86:
               if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
                  this.setString(this.currentString + SelectionManager.getClipboard(mc));
               }
               break;
            case 256:
               this.isListening = false;
               break;
            case 257:
            case 335:
               this.enterString();
               break;
            case 259:
               this.setString(StringButton.removeLastChar(this.currentString));
         }
      }
   }

   private void enterString() {
      if (!this.currentString.isEmpty() && this.isNumeric(this.currentString)) {
         this.setting.setValue(Double.parseDouble(this.currentString));
      } else {
         this.setting.setValue(this.setting.getDefaultValue());
      }

      this.onMouseClick();
   }

   public void setString(String newString) {
      this.currentString = newString;
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.drag && this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      } else {
         this.drag = false;
      }
   }

   @Override
   public void toggle() {
      this.setString(this.setting.getValueFloat() + "");
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   private void setSettingFromX(int mouseX) {
      double percent = (mouseX - this.x) / (this.width + 7.4);
      double result = this.setting.getMin() + this.difference * percent;
      result = Math.max(this.min, Math.min(result, this.max));
      this.setting.setValue(result);
   }

   private double part() {
      return this.setting.getValue() - this.min;
   }

   private double partialMultiplier() {
      return Math.max(0.0, Math.min(this.part() / this.difference, 1.0));
   }

   public void resetFillAnimation() {
      this.valueAnimation.from = 0.0;
      this.valueAnimation.to = 0.0;
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }
}
