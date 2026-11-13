package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.RotationAxis;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList<>();
   public boolean subOpen;
   public double itemHeight;
   private boolean wasHovered;
   public final Animation animation = new Animation();
   private final Animation hoverAnimation = new Animation();
   private final Animation toggleAnimation = new Animation();
   private final HashMap<BooleanSetting, Animation> parentOpenAnimations = new HashMap<>();

   public ModuleButton(Module var1) {
      super(var1.getName());
      this.module = var1;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList var1 = new ArrayList();

      for (Setting var3 : this.module.getSettings()) {
         if (var3 instanceof BooleanSetting var4) {
            var1.add(new BooleanButton(var4));
         }

         if (var3 instanceof BindSetting var5) {
            var1.add(new BindButton(var5));
         }

         if (var3 instanceof StringSetting var6) {
            var1.add(new StringButton(var6));
         }

         if (var3 instanceof SliderSetting var7) {
            var1.add(new SliderButton(var7));
         }

         if (var3 instanceof EnumSetting var8) {
            var1.add(new EnumButton(var8));
         }

         if (var3 instanceof ColorSetting var9) {
            var1.add(new PickerButton(var9));
         }
      }

      this.items = var1;
   }

   public List<Item> getItems() {
      return this.items;
   }

   @Override
   public void update() {
      for (Item var2 : this.items) {
         var2.update();
      }
   }

   @Override
   public void drawScreen(DrawContext var1, int var2, int var3, float var4) {
      boolean var5 = this.isHovering(var2, var3);
      if (var5 && !this.wasHovered) {
         hoverSound();
      }

      this.wasHovered = var5;
      boolean var6 = this.getState();
      double var7 = this.hoverAnimation.get(var5 ? 1.0 : 0.0, 100L, Easing.CubicInOut);
      double var9 = this.toggleAnimation.get(var6 ? 1.0 : 0.0, 160L, Easing.CubicInOut);
      double var11 = this.getColorDelay();
      Color var13 = ClickGui.getInstance().defaultColor.getValue();
      Color var14 = ClickGui.getInstance().hoverColor.getValue();
      Color var15 = new Color(var13.getRed(), var13.getGreen(), var13.getBlue(), var13.getAlpha());
      int var16 = ClickGui.getInstance().alpha.getValueInt();
      int var17 = ClickGui.getInstance().hoverAlpha.getValueInt();
      int var18 = Math.max(0, Math.min(255, (int)Math.round(var16 + (var17 - var16) * var7)));
      Color var19 = new Color(var14.getRed(), var14.getGreen(), var14.getBlue(), var17);
      Color var20 = ColorUtil.fadeColor(var15, var19, var7);
      float var21 = this.height - 0.5F;
      float var22 = Math.min(10.0F, Math.min((float)this.width, var21) / 2.0F);
      if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
         if (var9 >= 0.999) {
            Render2DUtil.drawLutRect(
               var1.getMatrices(),
               this.x,
               this.y,
               this.width,
               var21,
               ClickGui.getInstance().getSpectrumLutId(),
               ClickGui.getInstance().getSpectrumLutHeight(),
               var18
            );
         } else {
            Render2DUtil.drawRect(var1.getMatrices(), this.x, this.y, (float)this.width, var21, var20);
            int var23 = (int)Math.round(var18 * var9);
            if (var23 > 0) {
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  this.x,
                  this.y,
                  this.width,
                  var21,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var23
               );
            }
         }
      } else {
         Color var77 = ClickGui.getInstance().getActiveColor(var11);
         Color var24 = new Color(var77.getRed(), var77.getGreen(), var77.getBlue(), var18);
         Color var25 = ColorUtil.fadeColor(var20, var24, var9);
         Render2DUtil.drawRect(var1.getMatrices(), this.x, this.y, (float)this.width, var21, var25);
      }

      float var78 = this.getCenteredTextY(this.y, this.height - 0.5F);
      if (var5 && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.drawString("Reset Default", this.x + 2.3F, var78, enableTextColor);
      } else {
         this.drawString(this.module.getDisplayName(), this.x + 2.3F, var78, this.getState() ? enableTextColor : defaultTextColor);
      }

      if (ClickGui.getInstance().gear.booleanValue) {
         if (!this.subOpen && !(this.itemHeight > 0.0)) {
            boolean var80 = false;
         } else {
            boolean var79 = true;
         }

         switch ((ClickGui.ExpandIcon)ClickGui.getInstance().expandIcon.getValue()) {
            case PlusMinus:
               this.drawString(this.subOpen ? "-" : "+", this.x + this.width - 8.0F, var78, ClickGui.getInstance().gear.getValue().getRGB());
               break;
            case Chevron:
               this.drawString(this.subOpen ? "v" : ">", this.x + this.width - 8.0F, var78, ClickGui.getInstance().gear.getValue().getRGB());
               break;
            case Gear:
               int var81 = ClickGui.getInstance().gear.getValue().getRGB();
               float var84 = FontManager.icon.getWidth("d");
               float var26 = FontManager.icon.getFontHeight("d");
               float var27 = this.x + this.width - var22;
               float var28 = this.y + var21 / 2.0F;
               float var29 = var27 - var84 / 2.0F;
               float var30 = var28 - var26 / 2.0F;
               int var31 = this.getItemHeight();
               float var32 = var31 <= 0 ? 0.0F : (float)Math.min(1.0, Math.max(0.0, this.itemHeight / var31));
               if (var32 > 0.001F) {
                  float var33 = (float)(System.currentTimeMillis() % 2000L) / 2000.0F * 360.0F * var32;
                  var1.getMatrices().push();
                  var1.getMatrices().translate(var27, var28, 0.0F);
                  var1.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var33));
                  FontManager.icon.drawString(var1.getMatrices(), "d", -var84 / 2.0F, -var26 / 2.0F, var81);
                  var1.getMatrices().pop();
               } else {
                  FontManager.icon.drawString(var1.getMatrices(), "d", var29, var30, var81);
               }
         }
      }

      if (this.subOpen || this.itemHeight > 0.0) {
         double var47 = this.getVisibleItemHeight();
         var7 = Math.max(0.0, Math.min(this.itemHeight, var47));
         float var49 = var47 <= 0.0 ? 0.0F : (float)(var7 / var47);
         if (this.subOpen && var49 < 0.9F) {
            for (Item var51 : this.items) {
               if (var51 instanceof SliderButton) {
                  ((SliderButton)var51).resetFillAnimation();
               }
            }
         }

         float var50 = (1.0F - var49) * 6.0F;
         if (ClickGui.getInstance().line.getValue() && var7 > 0.01) {
            float var52 = this.y + this.height - 0.5F;
            float var12 = (float)(this.y + this.height + var7 - 0.5);
            float var56 = (float)(this.y + this.height + var7 - 0.7F);
            double var58 = 0.25;
            float var61 = this.x + 0.6F;
            float var63 = this.x + this.width - 0.6F;
            var18 = Math.min(160, ClickGui.getInstance().topAlpha.getValueInt());
            if (ClickGui.getInstance().colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
               float var67 = 1.0F;
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  var61,
                  var52,
                  var67,
                  var12 - var52,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var18
               );
               Render2DUtil.drawLutRect(
                  var1.getMatrices(),
                  var63 - var67,
                  var52,
                  var67,
                  var12 - var52,
                  ClickGui.getInstance().getSpectrumLutId(),
                  ClickGui.getInstance().getSpectrumLutHeight(),
                  var18
               );
            } else {
               int var68 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var52 * var58).getRGB(), var18);
               int var72 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var12 * var58).getRGB(), var18);
               Render2DUtil.verticalGradient(var1.getMatrices(), var61, var52, var61 + 1.0F, var12, var68, var72);
               Render2DUtil.verticalGradient(var1.getMatrices(), var63 - 1.0F, var52, var63, var12, var68, var72);
            }

            int var69 = ColorUtil.injectAlpha(ClickGui.getInstance().getColor(var12 * var58).getRGB(), var18);
            Render2DUtil.drawLine(var1.getMatrices(), var61, var12, var63, var56, var69);
         }

         float var53 = this.height + 2;
         List var55 = this.module.getSettings();
         int var57 = 0;

         while (var57 < this.items.size() && var57 < var55.size()) {
            Setting var59 = (Setting)var55.get(var57);
            Item var60 = this.items.get(var57);
            var60.setHeight(this.height);
            double var62 = var60.getVisibleHeight();
            if (var62 <= 0.01 && var60.isHidden()) {
               var57++;
            } else {
               var60.setLocation(this.x + 1.0F, this.y + var53 + var50);
               var60.setWidth(this.width - 9);
               if (var59 instanceof BooleanSetting && ((BooleanSetting)var59).hasParent()) {
                  var60.drawScreen(var1, var2, var3, var4);
                  float var54 = var53 + (float)(var62 + 2.0);
                  BooleanSetting var66 = (BooleanSetting)var59;
                  double var71 = this.getParentOpenProgress(var66);
                  int var75 = var57 + 1;

                  while (var75 < var55.size() && this.isChildOf(var66, (Setting)var55.get(var75))) {
                     var75++;
                  }

                  double var76 = 0.0;

                  for (int var82 = var57 + 1; var82 < var75 && var82 < this.items.size(); var82++) {
                     if (this.isVisibleWithParentOpen(var66, (Setting)var55.get(var82))) {
                        Item var85 = this.items.get(var82);
                        var85.setHeight(this.height);
                        double var86 = var85.getVisibleHeight();
                        if (!(var86 <= 0.01) || !var85.isHidden()) {
                           var76 += var86 + 2.0;
                        }
                     }
                  }

                  double var83 = var76 * var71;
                  if (var83 > 0.01) {
                     float var87 = (1.0F - (float)var71) * 6.0F;
                     float var88 = this.y + var54 + var50 + var87;
                     float var89 = this.x + 2.0F;
                     int var90 = this.width - 11;
                     int var91 = (int)var89 - 1;
                     int var92 = (int)var88 - 1;
                     int var93 = (int)(var89 + var90 + 8.0F) + 1;
                     int var94 = (int)Math.round(var88 + var83) + 1;
                     var1.enableScissor(var91, var92, var93, var94);
                     float var34 = var88;

                     for (int var35 = var57 + 1; var35 < var75 && var35 < this.items.size(); var35++) {
                        if (this.isVisibleWithParentOpen(var66, (Setting)var55.get(var35))) {
                           Item var36 = this.items.get(var35);
                           var36.setHeight(this.height);
                           double var37 = var36.getVisibleHeight();
                           if (!(var37 <= 0.01) || !var36.isHidden()) {
                              var36.setLocation(var89, var34);
                              var36.setWidth(var90);
                              if (var37 < var36.getHeight() - 0.01) {
                                 int var39 = (int)var36.getX() - 1;
                                 int var40 = (int)var36.getY() - 1;
                                 int var41 = (int)(var36.getX() + var36.getWidth() + 7.0F) + 1;
                                 int var42 = (int)(var36.getY() + var37) + 1;
                                 int var43 = Math.max(var91, var39);
                                 int var44 = Math.max(var92, var40);
                                 int var45 = Math.min(var93, var41);
                                 int var46 = Math.min(var94, var42);
                                 if (var45 > var43 && var46 > var44) {
                                    var1.enableScissor(var43, var44, var45, var46);
                                    var36.drawScreen(var1, var2, var3, var4);
                                    var1.disableScissor();
                                 }
                              } else {
                                 var36.drawScreen(var1, var2, var3, var4);
                              }

                              var34 += (float)(var37 + 2.0);
                           }
                        }
                     }

                     var1.disableScissor();
                  }

                  var53 = var54 + (float)var83;
                  var57 = var75;
               } else {
                  if (var62 < var60.getHeight() - 0.01) {
                     var18 = (int)var60.getX() - 1;
                     int var70 = (int)var60.getY() - 1;
                     int var73 = (int)(var60.getX() + var60.getWidth() + 7.0F) + 1;
                     int var74 = (int)(var60.getY() + var62) + 1;
                     var1.enableScissor(var18, var70, var73, var74);
                     var60.drawScreen(var1, var2, var3, var4);
                     var1.disableScissor();
                  } else {
                     var60.drawScreen(var1, var2, var3, var4);
                  }

                  var53 += (float)(var62 + 2.0);
                  var57++;
               }
            }
         }
      }
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      boolean hovered = this.isHovering(var1, var2);
      if (var3 == 0 && hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         this.resetSettingsToDefault();
         sound();
      } else {
         super.mouseClicked(var1, var2, var3);
         if (!this.items.isEmpty()) {
            if (var3 == 1 && hovered) {
               this.subOpen = !this.subOpen;
               if (this.subOpen) {
                  for (Item var5 : this.items) {
                     if (var5 instanceof SliderButton) {
                        ((SliderButton)var5).resetFillAnimation();
                     }
                  }
               }

               sound();
            }

            if (this.subOpen) {
               for (Item var7 : this.items) {
                  if (!var7.isHidden()) {
                     var7.mouseClicked(var1, var2, var3);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onKeyTyped(char var1, int var2) {
      super.onKeyTyped(var1, var2);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item var4 : this.items) {
            if (!var4.isHidden()) {
               var4.onKeyTyped(var1, var2);
            }
         }
      }
   }

   @Override
   public void onKeyPressed(int var1) {
      super.onKeyPressed(var1);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item var3 : this.items) {
            if (!var3.isHidden()) {
               var3.onKeyPressed(var1);
            }
         }
      }
   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public double getVisibleItemHeight() {
      double var1 = 3.0;
      List var3 = this.module.getSettings();
      int var4 = 0;

      while (var4 < this.items.size() && var4 < var3.size()) {
         Setting var5 = (Setting)var3.get(var4);
         Item var6 = this.items.get(var4);
         var6.setHeight(this.height);
         double var7 = var6.getVisibleHeight();
         if (var7 <= 0.01 && var6.isHidden()) {
            var4++;
         } else {
            var1 += var7 + 2.0;
            if (var5 instanceof BooleanSetting && ((BooleanSetting)var5).hasParent()) {
               BooleanSetting var9 = (BooleanSetting)var5;
               double var10 = this.getParentOpenProgress(var9);
               int var12 = var4 + 1;

               while (var12 < var3.size() && this.isChildOf(var9, (Setting)var3.get(var12))) {
                  var12++;
               }

               double var13 = 0.0;

               for (int var15 = var4 + 1; var15 < var12 && var15 < this.items.size(); var15++) {
                  if (this.isVisibleWithParentOpen(var9, (Setting)var3.get(var15))) {
                     Item var16 = this.items.get(var15);
                     var16.setHeight(this.height);
                     double var17 = var16.getVisibleHeight();
                     if (!(var17 <= 0.01) || !var16.isHidden()) {
                        var13 += var17 + 2.0;
                     }
                  }
               }

               var1 += var13 * var10;
               var4 = var12;
            } else {
               var4++;
            }
         }
      }

      return var1;
   }

   public int getItemHeight() {
      return (int)Math.round(this.getVisibleItemHeight());
   }

   private void resetSettingsToDefault() {
      for (Setting var2 : this.module.getSettings()) {
         this.resetSettingToDefault(var2);
      }
   }

   public void resetChildSettingsToDefault(BooleanSetting var1) {
      boolean var2 = var1.isOpen();

      for (Setting var4 : this.module.getSettings()) {
         if (var4 != var1) {
            var1.setOpen(true);
            boolean var5 = var4.isVisible();
            var1.setOpen(false);
            boolean var6 = var4.isVisible();
            var1.setOpen(var2);
            if (var5 && !var6) {
               if (var4 instanceof BooleanSetting var7 && var7.hasParent()) {
                  this.resetChildSettingsToDefault(var7);
               }

               this.resetSettingToDefault(var4);
            }
         }
      }
   }

   public void resetPageSettingsToDefault(EnumSetting<?> var1) {
      Enum var2 = var1.getValue();
      Enum[] var3 = (Enum[])var2.getDeclaringClass().getEnumConstants();
      String var4 = var2.name();
      var1.setEnumValue(var4);

      for (Setting var6 : this.module.getSettings()) {
         if (var6 != var1) {
            var1.setEnumValue(var4);
            boolean var7 = var6.isVisible();
            if (var7) {
               boolean var8 = true;

               for (Enum var12 : var3) {
                  var1.setEnumValue(var12.name());
                  if (!var6.isVisible()) {
                     var8 = false;
                     break;
                  }
               }

               var1.setEnumValue(var4);
               if (!var8) {
                  if (var6 instanceof BooleanSetting var13 && var13.hasParent()) {
                     this.resetChildSettingsToDefault(var13);
                  }

                  this.resetSettingToDefault(var6);
               }
            }
         }
      }

      var1.setEnumValue(var4);
   }

   private double getParentOpenProgress(BooleanSetting var1) {
      Animation var2 = this.parentOpenAnimations.get(var1);
      if (var2 == null) {
         var2 = new Animation();
         this.parentOpenAnimations.put(var1, var2);
      }

      return var2.get(var1.isOpen() ? 1.0 : 0.0, 200L, Easing.CubicInOut);
   }

   private boolean isChildOf(BooleanSetting var1, Setting var2) {
      boolean var3 = var1.isOpen();
      var1.setOpen(true);
      boolean var4 = var2.isVisible();
      var1.setOpen(false);
      boolean var5 = var2.isVisible();
      var1.setOpen(var3);
      return var4 && !var5;
   }

   private boolean isVisibleWithParentOpen(BooleanSetting var1, Setting var2) {
      boolean var3 = var1.isOpen();
      var1.setOpen(true);
      boolean var4 = var2.isVisible();
      var1.setOpen(var3);
      return var4;
   }

   private void resetSettingToDefault(Setting var1) {
      if (var1 instanceof BooleanSetting var2) {
         var2.setValue(var2.getDefaultValue());
         if (var2.hasParent()) {
            var2.setOpen(false);
         }
      } else if (var1 instanceof SliderSetting var3) {
         var3.setValue(var3.getDefaultValue());
      } else if (var1 instanceof StringSetting var4) {
         var4.setValue(var4.getDefaultValue());
      } else if (var1 instanceof BindSetting var5) {
         var5.setValue(var5.getDefaultValue());
         var5.setHoldEnable(false);
         var5.setPressed(false);
         var5.holding = false;
      } else if (var1 instanceof EnumSetting var6) {
         Enum var8 = var6.getDefaultValue();
         var6.setEnumValue(var8.name());
      } else if (var1 instanceof ColorSetting var7) {
         var7.setValue(var7.getDefaultValue());
         var7.rainbow = var7.getDefaultRainbow();
         if (var7.injectBoolean) {
            var7.booleanValue = var7.getDefaultBooleanValue();
         }
      }
   }

   @Override
   public int getHeight() {
      if (this.subOpen) {
         int var1 = super.getHeight();

         for (Item var3 : this.items) {
            if (!var3.isHidden()) {
               var1 += var3.getHeight() + 1;
            }
         }

         return var1 + 2;
      } else {
         return super.getHeight();
      }
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isOn();
   }
}
