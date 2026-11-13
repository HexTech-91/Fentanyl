package dev.luminous.mod.gui.items.buttons;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;

public class BindButton extends Button {
   private final BindSetting setting;
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getDisplayName());
      this.setting = setting;
   }

   private static void saveConfig() {
      if (Alien.CONFIG != null) {
         Alien.CONFIG.save();
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + this.width + 7.0F, this.y + this.height - 0.5F, !hovered ? defaultColor : hoverColor);
      float textY = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (this.isListening) {
         this.drawString("Listening... ", this.x + 2.3F, textY, enableTextColor);
      } else {
         String keyName = this.getKeyName();
         String modeText = this.setting.isHoldEnable() ? " Hold" : " Toggle";
         if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
            this.drawString(
               this.setting.getDisplayName() + " " + Formatting.GRAY + keyName + Formatting.YELLOW + modeText, this.x + 2.3F, textY, enableTextColor
            );
         } else {
            this.drawString(
               this.setting.getDisplayName() + " " + Formatting.GRAY + keyName + Formatting.DARK_GRAY + modeText,
               this.x + 2.3F,
               textY,
               this.getState() ? enableTextColor : defaultTextColor
            );
         }
      }
   }

   private String getKeyName() {
      return this.setting.getKeyString().equalsIgnoreCase("None") ? "None" : this.setting.getKeyString();
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isListening) {
         if (mouseButton == 1) {
            this.setting.setValue(-1);
         } else if (mouseButton > 1) {
            this.setting.setValue(-(mouseButton + 2));
         }

         if (mouseButton != 0) {
            this.isListening = false;
            saveConfig();
            sound();
         }
      } else if (mouseButton == 0 && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.setting.setHoldEnable(!this.setting.isHoldEnable());
         saveConfig();
         sound();
      } else if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.isListening = !this.isListening;
         sound();
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.setting.setValue(-1);
         saveConfig();
         sound();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         BindSetting bindSetting = this.setting;
         if (key != 256 && key != 261) {
            bindSetting.setValue(key);
         } else {
            bindSetting.setValue(-1);
         }

         this.isListening = false;
         saveConfig();
      }
   }

   @Override
   public void toggle() {
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }
}
