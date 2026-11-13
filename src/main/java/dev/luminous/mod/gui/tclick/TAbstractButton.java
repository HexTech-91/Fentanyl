package dev.luminous.mod.gui.tclick;

import net.minecraft.client.gui.DrawContext;

public class TAbstractButton {
   public float x;
   public float y;
   public float width;
   public float height;
   public float target_offset;
   public float offsetY;

   public void init() {
   }

   public float getX() {
      return this.x;
   }

   public void setX(float var1) {
      this.x = var1;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float var1) {
      this.y = var1 + this.offsetY;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float var1) {
      this.width = var1;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float var1) {
      this.height = var1;
   }

   public void setTargetOffset(float var1) {
      this.target_offset = var1;
   }

   public void setOffset(float var1) {
      this.offsetY = var1;
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
   }

   public void mouseReleased(int var1, int var2, int var3) {
   }

   public void mouseClicked(int var1, int var2, int var3) {
   }

   public void keyTyped(int var1) {
   }

   public void onGuiClosed() {
   }

   public void tick() {
   }

   public void charTyped(char var1, int var2) {
   }
}
