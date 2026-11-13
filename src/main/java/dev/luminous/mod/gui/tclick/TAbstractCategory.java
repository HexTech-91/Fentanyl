package dev.luminous.mod.gui.tclick;

import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class TAbstractCategory {
   private String name;
   public float animationY;
   protected float prevTargetX;
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected float sx;
   protected float sy;
   private float prevX;
   private float prevY;
   protected boolean hovered;
   public boolean dragging;
   public float moduleOffset;
   private boolean open;

   public TAbstractCategory(String var1, float var2, float var3, float var4, float var5) {
      this.name = var1;
      this.x = var2;
      this.y = var3;
      this.width = var4;
      this.height = var5;
      this.open = false;
   }

   public void init() {
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
      this.hovered = TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height);
      this.animationY = (float)interpolate(this.y, this.animationY, 0.05);
      if (this.dragging) {
         this.prevTargetX = this.x;
         this.x = this.prevX + var2;
         this.y = this.prevY + var3;
      } else {
         this.prevTargetX = this.x;
      }
   }

   public void mouseClicked(int var1, int var2, int var3) {
      if (this.hovered && var3 == 0) {
         this.dragging = true;
         this.prevX = this.x - var1;
         this.prevY = this.y - var2;
      }
   }

   public static double interpolate(double var0, double var2, double var4) {
      return var2 + (var0 - var2) * var4;
   }

   public void mouseReleased(int var1, int var2, int var3) {
      if (var3 == 0) {
         this.dragging = false;
      }
   }

   public boolean keyTyped(int var1) {
      return true;
   }

   public void charTyped(char var1, int var2) {
   }

   public void onClose() {
   }

   public void setOpen(boolean var1) {
      this.open = var1;
   }

   public String getName() {
      return this.name;
   }

   public boolean isOpen() {
      return this.open;
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
      this.y = var1;
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

   public void setModuleOffset(float var1, float var2, float var3) {
      if (TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height + 1000.0F)) {
         this.moduleOffset += var1;
      }
   }

   public void tick() {
   }

   public void hudClicked(Module var1) {
   }

   public void savePos() {
      this.sx = this.x;
      this.sy = this.y;
   }

   public void restorePos() {
      this.x = this.sx;
      this.y = this.sy;
   }
}
