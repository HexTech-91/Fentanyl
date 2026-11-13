package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StringButton extends Button {
   private final StringSetting setting;
   public boolean isListening;
   public String currentString = "";
   private TextFieldWidget textField;

   public StringButton(StringSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + this.width + 7.0F, this.y + this.height - 0.5F, !hovered ? defaultColor : hoverColor);
      float textY = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (this.isListening && this.textField != null) {
         this.textField.setX((int)(this.x + 2.3F));
         this.textField.setY((int)textY - 1);
         this.textField.setWidth((int)(this.width + 7.0F - 2.3F - 2.0F));
         this.textField.render(context, mouseX, mouseY, partialTicks);
      } else if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString("Reset Default", this.x + 2.3F, textY, enableTextColor);
      } else {
         this.drawString(
            this.setting.getDisplayName() + " " + Formatting.GRAY + this.setting.getValue(),
            this.x + 2.3F,
            textY,
            this.getState() ? enableTextColor : defaultTextColor
         );
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.setting.setValue(this.setting.getDefaultValue());
         sound();
      } else if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.toggle();
         sound();
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.toggle();
      }
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening && this.textField != null) {
         this.textField.charTyped(typedChar, keyCode);
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening && this.textField != null) {
         this.textField.keyPressed(key, 0, 0);
      }
   }

   private void enterString(String string) {
      if (!string.isEmpty()) {
         this.setting.setValue(string);
      }

      this.onMouseClick();
   }

   @Override
   public void toggle() {
      if (this.isListening) {
         this.isListening = false;
         if (this.textField != null) {
            this.currentString = this.textField.getText();
            this.enterString(this.currentString);
            this.textField.setFocused(false);
            this.textField = null;
         }

         ClickGuiScreen.setActiveTextField(null);
      } else {
         this.currentString = this.setting.getValue();
         this.isListening = true;
         this.textField = new TextFieldWidget(mc.textRenderer, 0, 0, 100, 12, Text.literal(""));
         this.textField.setMaxLength(256);
         this.textField.setText(this.currentString);
         this.textField.setFocused(true);
         ClickGuiScreen.setActiveTextField(this.textField);
      }
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   public static String getIdleSign() {
      return (System.currentTimeMillis() % 1000L > 500L ? "|" : "") + " ";
   }

   public static String removeLastChar(String str) {
      String out = "";
      if (str != null && !str.isEmpty()) {
         out = str.substring(0, str.length() - 1);
      }

      return out;
   }
}
