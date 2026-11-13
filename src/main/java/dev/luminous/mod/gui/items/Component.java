package dev.luminous.mod.gui.items;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.HudEditor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class Component extends Mod {
   private final List<ModuleButton> items = new ArrayList<>();
   private final Module.Category category;
   public boolean drag;
   protected DrawContext context;
   private int x;
   private int y;
   private float animX;
   private float animY;
   private float mouseMoveOffsetX;
   private float mouseMoveOffsetY;
   private final Animation xAnimation = new Animation();
   private final Animation yAnimation = new Animation();
   private final Animation openAnimation = new Animation();
   private int x2;
   private int y2;
   private int width;
   private int height;
   private boolean open;
   private boolean hidden = false;

   public Component(String var1, Module.Category var2, int var3, int var4, boolean var5) {
      super(var1);
      this.category = var2;
      this.setX(var3);
      this.setY(var4);
      this.animX = this.x;
      this.animY = this.y;
      if (ClickGui.getInstance() != null) {
         this.setWidth(ClickGui.getInstance().moduleButtonWidth.getValueInt());
         this.setHeight(ClickGui.getInstance().categoryBarHeight.getValueInt() + 5);
      } else {
         this.setWidth(93);
         this.setHeight(18);
      }

      this.open = var5;
      this.setupItems();
   }

   public void setupItems() {
   }

   private void drag(int var1, int var2) {
      if (this.drag) {
         this.x = this.x2 + var1;
         this.y = this.y2 + var2;
         this.animX = this.x;
         this.animY = this.y;
      }
   }

   private void updatePosition() {
      if (ClickGui.getInstance().scrollAnim.getValue() && !this.drag) {
         int var1 = Math.max(1, ClickGui.getInstance().scrollAnimLength.getValueInt());
         Easing var2 = ClickGui.getInstance().scrollAnimEasing.getValue();
         this.animX = (float)this.xAnimation.get(this.x, var1, var2);
         this.animY = (float)this.yAnimation.get(this.y, var1, var2);
      } else {
         this.animX = this.x;
         this.animY = this.y;
         this.xAnimation.from = this.x;
         this.xAnimation.to = this.x;
         this.yAnimation.from = this.y;
         this.yAnimation.to = this.y;
      }
   }

   protected double getColorDelay() {
      return this.getY() / 10.0;
   }

   private boolean isHudEditorHidden() {
      boolean on = HudEditor.INSTANCE != null && HudEditor.INSTANCE.isOn();
      if (this.category == null) {
         return false;
      }

      return this.category == Module.Category.HUD ? !on : on;
   }

   public void drawScreen(DrawContext var1, int var2, int var3, float var4) {
      if (this.isHudEditorHidden()) {
         return;
      }

      this.context = var1;
      this.drag(var2, var3);
      this.updatePosition();
      int var5 = this.getX();
      int var6 = this.getY();
      float var7 = this.getTotalItemHeight() - 2.0F;
      double var8 = this.openAnimation.get(this.open ? 1.0 : 0.0, 200L, Easing.CubicInOut);
      float var10 = (float)(var7 * var8);
      int var11 = ClickGui.getInstance().categoryWidth.getValueInt();
      float var12 = var5;
      float var13 = var6;
      float var14 = this.width;
      float var15 = this.height - 5.0F;
      double var16 = this.getColorDelay();
      int var18 = Math.min(205, ClickGui.getInstance().topAlpha.getValueInt());
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         Render2DUtil.drawLutRect(
            var1.getMatrices(), var12, var13, var14, var15, ClickGui.getInstance().getSpectrumLutId(), ClickGui.getInstance().getSpectrumLutHeight(), var18
         );
      } else {
         Color var19 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var16), var18);
         Render2DUtil.drawRect(var1.getMatrices(), var12, var13, var14, var15, var19);
      }

      Render2DUtil.drawRectWithOutline(
         var1.getMatrices(), var12, var13, var14, var15, new Color(0, 0, 0, 0), new Color(ClickGui.getInstance().hoverColor.getValue().getRGB())
      );
      if (var8 > 0.01) {
         if (ClickGui.getInstance().backGround.booleanValue) {
            Render2DUtil.drawRect(
               var1.getMatrices(),
               (float)var5,
               (float)var6 + this.height - 5.0F,
               (float)this.width,
               var6 + this.height + var10 - ((float)var6 + this.height - 5.0F),
               ColorUtil.injectAlpha(ClickGui.getInstance().backGround.getValue(), ClickGui.getInstance().backgroundAlpha.getValueInt())
            );
            Render2DUtil.drawRectWithOutline(
               var1.getMatrices(),
               var5,
               (float)var6 + this.height - 5.0F,
               this.width,
               var6 + this.height + var10 - ((float)var6 + this.height - 5.0F),
               new Color(0, 0, 0, 0),
               new Color(ClickGui.getInstance().hoverColor.getValue().getRGB())
            );
         }

         if (ClickGui.getInstance().line.getValue()) {
            float var37 = (float)var6 + this.height - 5.0F;
            float var20 = var6 + this.height + var10;
            double var21 = 0.25;
            float var23 = var5 + 0.2F;
            float var24 = var5 + this.width;
            int var25 = ClickGui.getInstance().topAlpha.getValueInt();
            if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
               float var26 = 1.0F;
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  var23,
                  var37,
                  var26,
                  var20 - var37,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var25
               );
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  var24 - var26,
                  var37,
                  var26,
                  var20 - var37,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var25
               );
            } else {
               int var44 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var37 * var21).getRGB(), var25);
               int var27 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var20 * var21).getRGB(), var25);
               Render2DUtil.verticalGradient(var1.getMatrices(), var23, var37, var23 + 1.0F, var20, var44, var27);
               Render2DUtil.verticalGradient(var1.getMatrices(), var24 - 1.0F, var37, var24, var20, var44, var27);
            }

            int var45 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var20 * var21).getRGB(), var25);
            Render2DUtil.drawLine(var1.getMatrices(), var5, var20, var5 + this.width, var20, var45);
         }
      }

      float var38 = var13 + (var15 - FontManager.icon.getFontHeight()) / 2.0F;
      String var39 = this.category != null ? this.category.getIcon() : "F";
      FontManager.icon.drawString(var1.getMatrices(), var39, var12 + 6.0F, var38, Button.enableTextColor);
      float var40 = FontManager.isCustomFontEnabled() ? FontManager.ui.getFontHeight() : 9.0F;
      float var22 = var13 + (var15 - var40) / 2.0F + ClickGui.getInstance().titleOffset.getValueInt();
      this.drawString(this.getName(), var12 + 20.0F, var22, Button.enableTextColor);
      if (var8 > 0.01) {
         int var41 = var5 - 1;
         int var42 = (int)((float)var6 + this.height - 6.0F);
         int var43 = var5 + this.width + 1;
         int var46 = (int)Math.round((double)(var6 + this.height) + var10) + 1;
         var1.enableScissor(var41, var42, var43, var46);
         float var47 = this.getY() + this.getHeight() - 3.0F;
         Module.Category var28 = null;
         boolean var29 = true;

         for (ModuleButton var31 : this.getItems()) {
            if (!var31.isHidden()) {
               Module.Category var32 = var31.getModule() != null ? var31.getModule().getCategory() : null;
               if (this.category == null && var32 != null && (var28 == null || var28 != var32)) {
                  if (!var29) {
                     float var33 = var47 + 1.0F;
                     Render2DUtil.drawLine(var1.getMatrices(), var5 + 2.0F, var33, var5 + this.width - 2.0F, var33, 587202815);
                     var47 += 4.0F;
                  }

                  float var48 = var47 + (FontManager.ui.getFontHeight() - 2.0F) / 2.0F;
                  String var34 = var32.getIcon();
                  FontManager.icon.drawString(var1.getMatrices(), var34, var5 + 4.0F, var48, Button.enableTextColor);
                  this.drawString(this.catName(var32), var5 + 18.0F, var48, Button.defaultTextColor);
                  var47 += FontManager.ui.getFontHeight() + 3.0F;
               }

               var28 = var32;
               var29 = false;
               var31.setLocation(var5 + 2.0F, var47);
               var31.setWidth(this.getWidth() - 4);
               if (!(var31.itemHeight > 0.0) && !var31.subOpen) {
                  var31.drawScreen(var1, var2, var3, var4);
               } else {
                  int var49 = (int)var31.x - 1;
                  int var50 = (int)var31.y - 1;
                  int var35 = (int)(var31.x + var31.getWidth() + 1.0F);
                  int var36 = (int)(var47 + var31.getButtonHeight() + 1.5F + var31.itemHeight) + 1;
                  var1.enableScissor(var49, var50, var35, var36);
                  var31.drawScreen(var1, var2, var3, var4);
                  var1.disableScissor();
               }

               var47 += var31.getButtonHeight() + 1.5F + (float)var31.itemHeight;
            }
         }

         var1.disableScissor();
      }
   }

   public void mouseClicked(int var1, int var2, int var3) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (var3 == 0 && this.isHovering(var1, var2)) {
         if (ClickGui.getInstance().mouseMove.getValue()) {
            this.setX(this.getX());
            this.setY(this.getY());
            this.animX = this.x;
            this.animY = this.y;
            this.xAnimation.from = this.x;
            this.xAnimation.to = this.x;
            this.yAnimation.from = this.y;
            this.yAnimation.to = this.y;
         }

         this.x2 = this.getX() - var1;
         this.y2 = this.getY() - var2;
         ClickGuiScreen.getInstance().getComponents().forEach(var0 -> {
            if (var0.drag) {
               var0.drag = false;
            }
         });
         this.drag = true;
      } else if (var3 == 1 && this.isHovering(var1, var2)) {
         this.open = !this.open;
         Item.sound();
      } else if (this.open) {
         this.getItems().forEach(var3x -> var3x.mouseClicked(var1, var2, var3));
      }
   }

   public void mouseReleased(int var1, int var2, int var3) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (var3 == 0) {
         this.drag = false;
      }

      if (this.open) {
         this.getItems().forEach(var3x -> var3x.mouseReleased(var1, var2, var3));
      }
   }

   public void onKeyTyped(char var1, int var2) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (this.open) {
         this.getItems().forEach(var2x -> var2x.onKeyTyped(var1, var2));
      }
   }

   public void onKeyPressed(int var1) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (this.open) {
         this.getItems().forEach(var1x -> var1x.onKeyPressed(var1));
      }
   }

   public void addButton(ModuleButton var1) {
      this.items.add(var1);
   }

   public int getX() {
      return (int)(this.animX + this.mouseMoveOffsetX);
   }

   public int getTargetX() {
      return this.x;
   }

   public void setX(int var1) {
      this.x = var1;
   }

   public int getY() {
      return (int)(this.animY + this.mouseMoveOffsetY);
   }

   public int getTargetY() {
      return this.y;
   }

   public void setY(int var1) {
      this.y = var1;
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

   public void setInitialPosition(int var1, int var2) {
      this.x = var1;
      this.y = var2;
      this.animX = var1;
      this.animY = var2;
      this.xAnimation.from = var1;
      this.xAnimation.to = var1;
      this.yAnimation.from = var2;
      this.yAnimation.to = var2;
   }

   public void setOpenInitial(boolean var1) {
      this.open = var1;
      this.openAnimation.from = var1 ? 1.0 : 0.0;
      this.openAnimation.to = var1 ? 1.0 : 0.0;
   }

   public boolean isOpen() {
      return this.open;
   }

   public final List<ModuleButton> getItems() {
      return this.items;
   }

   public void setMouseMoveOffset(float var1, float var2) {
      this.mouseMoveOffsetX = var1;
      this.mouseMoveOffsetY = var2;
   }

   private boolean isHovering(int var1, int var2) {
      int var3 = ClickGui.getInstance().categoryWidth.getValueInt();
      int var4 = Math.round(this.getX() + ((float)this.getWidth() - var3) / 2.0F);
      int var5 = this.getY();
      int var6 = this.getHeight() - 5;
      return var1 >= var4 && var1 <= var4 + var3 && var2 >= var5 && var2 <= var5 + var6;
   }

   private String catName(Module.Category var1) {
      switch (var1) {
         case Combat:
            return "\u6218\u6597";
         case Misc:
            return "\u6742\u9879";
         case Render:
            return "\u6e32\u67d3";
         case Movement:
            return "\u79fb\u52a8";
         case Player:
            return "\u73a9\u5bb6";
         case Client:
            return "\u5ba2\u6237\u7aef";
         case HUD:
            return "\u754c\u9762";
         default:
            return var1.toString();
      }
   }

   private float getTotalItemHeight() {
      float var1 = 0.0F;

      for (ModuleButton var3 : this.getItems()) {
         var3.update();
         double var4 = var3.animation.get(var3.subOpen ? 1.0 : 0.0, 200L, Easing.CubicInOut);
         var3.itemHeight = var3.getVisibleItemHeight() * var4;
         var1 += var3.getButtonHeight() + 1.5F + (float)var3.itemHeight;
      }

      if (this.category == null) {
         int var8 = 0;
         Module.Category var9 = null;

         for (ModuleButton var5 : this.getItems()) {
            Module.Category var6 = var5.getModule() != null ? var5.getModule().getCategory() : null;
            if (var6 != null && (var9 == null || var9 != var6)) {
               var8++;
            }

            var9 = var6;
         }

         var1 += var8 * 4;
         var1 += var8 * (FontManager.ui.getFontHeight() + 3.0F);
      }

      return var1;
   }

   protected void drawString(String var1, double var2, double var4, Color var6) {
      this.drawString(var1, var2, var4, var6.hashCode());
   }

   protected void drawString(String var1, double var2, double var4, int var6) {
      boolean var7 = FontManager.isShadowEnabled();
      if (FontManager.isCustomFontEnabled()) {
         FontManager.ui.drawString(this.context.getMatrices(), var1, (int)var2, (int)var4, var6, var7);
      } else {
         this.context.drawText(mc.textRenderer, var1, (int)var2, (int)var4, var6, var7);
      }
   }
}
