package dev.luminous.mod.gui.items;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class Item extends Mod {
   public static DrawContext context;
   protected float x;
   protected float y;
   protected int width;
   protected int height;
   private boolean hidden;
   private final Animation visibleHeightAnimation = new Animation();

   public Item(String var1) {
      super(var1);
   }

   public void setLocation(float var1, float var2) {
      this.x = var1;
      this.y = var2;
   }

   public static void sound() {
      if (ClickGui.getInstance().sound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager()
            .play(
               PositionedSoundInstance.master((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.5F, ClickGui.getInstance().soundPitch.getValueFloat())
            );
      }
   }

   public static void hoverSound() {
      if (ClickGui.getInstance().sound.getValue() && ClickGui.getInstance().hoverSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager()
            .play(
               PositionedSoundInstance.master(
                  (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.8F, ClickGui.getInstance().hoverSoundPitch.getValueFloat()
               )
            );
      }
   }

   public void drawScreen(DrawContext var1, int var2, int var3, float var4) {
   }

   public void mouseClicked(int var1, int var2, int var3) {
   }

   public void mouseReleased(int var1, int var2, int var3) {
   }

   public void update() {
   }

   public void onKeyTyped(char var1, int var2) {
   }

   public void onKeyPressed(int var1) {
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int var1) {
      this.width = var1;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int var1) {
      this.height = var1;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean var1) {
      this.hidden = var1;
   }

   public double getVisibleHeight() {
      double var1 = this.hidden ? 0.0 : this.getHeight();
      return this.visibleHeightAnimation.get(var1, 200L, Easing.CubicInOut);
   }

   protected double getColorDelay() {
      return this.y / 10.0;
   }

   protected void drawString(String var1, double var2, double var4, Color var6) {
      this.drawString(var1, var2, var4, var6.hashCode());
   }

   protected void drawString(String var1, double var2, double var4, int var6) {
      boolean var7 = FontManager.isShadowEnabled();
      if (FontManager.isCustomFontEnabled()) {
         FontManager.ui.drawString(context.getMatrices(), var1, (int)var2, (int)var4, var6, var7);
      } else {
         context.drawText(mc.textRenderer, var1, (int)var2, (int)var4, var6, var7);
      }
   }

   protected int getFontHeight() {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getFontHeight() : 9;
   }

   protected float getCenteredTextY(float var1, float var2) {
      return var1 + (var2 - this.getFontHeight()) / 2.0F + ClickGui.getInstance().textOffset.getValueInt();
   }

   protected int getWidth(String var1) {
      return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getWidth(var1) : mc.textRenderer.getWidth(var1);
   }
}
