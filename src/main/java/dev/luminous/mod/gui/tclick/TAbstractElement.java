package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public abstract class TAbstractElement {
   protected Setting setting;
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected float offsetY;
   protected boolean hovered;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public TAbstractElement(Setting var1) {
      this.setting = var1;
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
      this.hovered = TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height);
   }

   public void init() {
   }

   public void mouseClicked(int var1, int var2, int var3) {
      if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 261) && var3 == 2 && this.hovered) {
         resetSetting(this.setting);
      }
   }

   public void mouseReleased(int var1, int var2, int var3) {
   }

   public void keyTyped(int var1) {
   }

   public void onClose() {
   }

   public Setting getSetting() {
      return this.setting;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setX(float var1) {
      this.x = var1;
   }

   public void setY(float var1) {
      this.y = var1 + this.offsetY;
   }

   public void setWidth(float var1) {
      this.width = var1;
   }

   public void setHeight(float var1) {
      this.height = var1;
   }

   public void setOffsetY(float var1) {
      this.offsetY = var1;
   }

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   public void charTyped(char var1, int var2) {
   }

   public static void resetSetting(Setting var0) {
      if (var0 instanceof BooleanSetting var1) {
         var1.setValue(var1.getDefaultValue());
      } else if (var0 instanceof SliderSetting var2) {
         var2.setValue(var2.getDefaultValue());
      } else if (var0 instanceof StringSetting var3) {
         var3.setValue(var3.getDefaultValue());
      } else if (var0 instanceof EnumSetting var4) {
         var4.setValue(var4.getDefaultValue());
      } else if (var0 instanceof ColorSetting var5) {
         var5.setValue(var5.getDefaultValue());
      } else if (var0 instanceof BindSetting var6) {
         var6.setValue(var6.getDefaultValue());
      }
   }
}
