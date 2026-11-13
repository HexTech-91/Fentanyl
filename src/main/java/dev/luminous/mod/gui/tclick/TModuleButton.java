package dev.luminous.mod.gui.tclick;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.i18n.LangUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.gui.tclick.core.TAnim;
import dev.luminous.mod.gui.tclick.core.TAssets;
import dev.luminous.mod.gui.tclick.core.TCfg;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TGearAnimation;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.gui.tclick.core.TRect;
import dev.luminous.mod.gui.tclick.core.TSounds;
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
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;

public class TModuleButton extends TAbstractButton {
   private final List<TAbstractElement> elements;
   public final Module module;
   private boolean open;
   private boolean hovered;
   private boolean prevHovered;
   private float animation;
   private float animation2;
   float category_animation = 0.0F;
   int ticksOpened;
   private final TGearAnimation gearAnimation = new TGearAnimation();
   private boolean binding = false;
   private boolean holdbind = false;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public TModuleButton(Module var1) {
      this.module = var1;
      this.elements = new ArrayList<>();

      for (Setting var3 : var1.getSettings()) {
         ClickGui var4;
         if (!(var1 instanceof ClickGui) || (var4 = (ClickGui)var1).isThunderSetting(var3)) {
            if (var3 instanceof BooleanSetting var5) {
               if (!var5.getName().equalsIgnoreCase("Drawn")) {
                  this.elements.add((TAbstractElement)(var5.hasParent() ? new TBooleanParentElement(var3) : new TBooleanElement(var3)));
               }
            } else if (var3 instanceof SliderSetting) {
               this.elements.add(new TSliderElement(var3));
            } else if (var3 instanceof ColorSetting) {
               this.elements.add(new TColorPickerElement(var3));
            } else if (var3 instanceof EnumSetting) {
               this.elements.add(new TModeElement(var3));
            } else if (var3 instanceof StringSetting) {
               this.elements.add(new TStringElement(var3));
            } else if (var3 instanceof BindSetting) {
               this.elements.add(new TBindElement(var3));
            }
         }
      }
   }

   @Override
   public void init() {
      this.elements.forEach(TAbstractElement::init);
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      this.hovered = TDraw.isHovered(var2, var3, this.x, this.y, this.width, this.height);
      this.animation = TAnim.fast(this.animation, this.module.isOn() ? 1.0F : 0.0F, 8.0F);
      this.animation2 = TAnim.fast(this.animation2, 1.0F, 10.0F);
      if (this.hovered) {
         if (!this.prevHovered) {
            TSounds.playScroll();
         }

         TClickGuiScreen.currentDescription = TI18n.ui(I18n.translate(this.module.getDescription(), new Object[0]));
      }

      this.prevHovered = this.hovered;
      float var5 = this.x + 5.0F;
      float var6 = this.y + this.height / 2.0F - 3.0F;
      this.offsetY = TAnim.fast(this.offsetY, this.target_offset, 20.0F);
      float var7 = 0.0F;
      if (this.isOpen()) {
         TDraw.drawGuiBase(
            var1.getMatrices(), this.x + 4.0F, this.y + 2.0F, this.width - 8.0F, this.height + (float)this.getElementsHeight(), 1.0F, TColor.alpha
         );
         TDraw.addWindow(
            var1.getMatrices(),
            new TRect(this.x + 1.0F, this.y + this.height - 2.0F, this.width + this.x - 2.0F, (float)(this.height + this.y + 1.0F + this.getElementsHeight()))
         );
         if (mc.player != null && TCfg.gearEnabled) {
            TDraw.addWindow(
               var1.getMatrices(),
               new TRect(this.x, this.y + this.height + 1.0F, this.width + this.x + 6.0F, (float)(this.height + this.y + 1.0F + this.getElementsHeight()))
            );
            float var8 = this.x + 4.0F + (this.width - 8.0F) / 2.0F;
            float var9 = this.y + 12.0F + (this.height + (float)this.getElementsHeight()) / 2.0F;
            int var10 = TCfg.gearScale;
            var1.getMatrices().push();
            var1.getMatrices().translate(var8, var9, 0.0F);
            var1.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.gearAnimation.getValue()));
            var1.getMatrices().translate(-var8, -var9, 0.0F);
            RenderSystem.setShaderTexture(0, TAssets.GEAR);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            TDraw.renderGradientTexture(
               var1.getMatrices(),
               var8 - var10 / 2.0F,
               var9 - var10 / 2.0F,
               var10,
               var10,
               0.0F,
               0.0F,
               var10,
               var10,
               var10,
               var10,
               TDraw.injectAlpha(TColor.getColor(270), 200),
               TDraw.injectAlpha(TColor.getColor(0), 200),
               TDraw.injectAlpha(TColor.getColor(180), 200),
               TDraw.injectAlpha(TColor.getColor(90), 200)
            );
            RenderSystem.disableBlend();
            var1.getMatrices().translate(var8, var9, 0.0F);
            var1.getMatrices()
               .multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)TDraw.interpolate(mc.player.age - 1, mc.player.age, TAnim.tickDelta()) * -4.0F));
            var1.getMatrices().translate(-var8, -var9, 0.0F);
            var1.getMatrices().pop();
            TDraw.popWindow();
         }

         if (TDraw.isHovered(var2, var3, this.x + 4.0F, this.y + this.height - 12.0F, this.width - 8.0F, this.height + (float)this.getElementsHeight())) {
            TDraw.drawBlurredShadow(var1.getMatrices(), var2 - 10, var3 - 10, 20.0F, 20.0F, 40, TColor.getColor(270));
         }

         for (TAbstractElement var12 : this.elements) {
            if (var12.isVisible()) {
               var12.setOffsetY(var7);
               var12.setX(this.x);
               var12.setY(this.y + this.height + 2.0F);
               var12.setWidth(this.width);
               var12.setHeight(13.0F);
               if (var12 instanceof TColorPickerElement var13) {
                  var12.setHeight(var13.getHeight());
               } else if (var12 instanceof TSliderElement) {
                  var12.setHeight(18.0F);
               }

               if (var12 instanceof TModeElement var14) {
                  var14.setWHeight(13.0);
                  if (var14.isOpen()) {
                     var12.setHeight(13 + this.modeCount(var12.getSetting()) * 12);
                  } else {
                     var12.setHeight(13.0F);
                  }
               }

               var7 += var12.getHeight();
            }
         }

         var1.getMatrices().push();
         TDraw.sizeAnimation(
            var1.getMatrices(),
            this.x + this.width / 2.0F + 6.0F,
            this.y + this.height / 2.0F - 12.0F,
            this.ticksOpened < 5 ? Math.clamp(this.category_animation / var7, 0.0F, 1.0F) : 1.0
         );
         this.elements.forEach(var4x -> {
            if (var4x.isVisible()) {
               var4x.render(var1, var2, var3, var4);
            }
         });
         var1.getMatrices().pop();
         TDraw.drawBlurredShadow(var1.getMatrices(), this.x + 3.0F, this.y + this.height, this.width - 6.0F, 3.0F, 13, TColor.getColor(1));
         if (!this.module.isOn()) {
            TDraw.draw2DGradientRect(
               var1.getMatrices(),
               this.x + 4.0F,
               this.y + this.height - 1.0F,
               this.x + 3.0F + this.width - 7.0F,
               3.0F + this.y + this.height,
               TDraw.applyOpacity(TColor.getColor(0), 0.0F),
               TColor.getColor(0),
               TDraw.applyOpacity(TColor.getColor(90), 0.0F),
               TColor.getColor(90)
            );
         }

         TDraw.popWindow();
      } else if (this.hovered) {
         TDraw.addWindow(var1.getMatrices(), this.x + 1.0F, this.y, this.x + this.width - 2.0F, this.y + this.height, 1.0);
         TDraw.drawBlurredShadow(var1.getMatrices(), var2 - 10, var3 - 10, 20.0F, 20.0F, 35, TColor.getColor(270));
         TDraw.popWindow();
      }

      this.category_animation = TAnim.fast(this.category_animation, var7, 20.0F);
      if (this.animation < 0.05) {
         TDraw.drawRect(
            var1.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, this.height - 2.0F, TDraw.applyOpacity(TColor.plateColor.darker(), 0.15F)
         );
      } else {
         switch (TCfg.gradientMode) {
            case 0:
               TDraw.draw2DGradientRect(
                  var1.getMatrices(),
                  this.x + 4.0F,
                  this.y + 1.0F,
                  this.x + 4.0F + this.width - 8.0F,
                  this.y + 1.0F + this.height - 2.0F,
                  TDraw.applyOpacity(TColor.getColor(270), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(0), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(270), this.animation),
                  TDraw.applyOpacity(TColor.getColor(0), this.animation)
               );
               break;
            case 1:
               TDraw.draw2DGradientRect(
                  var1.getMatrices(),
                  this.x + 4.0F,
                  this.y + 1.0F,
                  this.x + 4.0F + this.width - 8.0F,
                  this.y + 1.0F + this.height - 2.0F,
                  TDraw.applyOpacity(TColor.getColor(270), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(270), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(0), this.animation),
                  TDraw.applyOpacity(TColor.getColor(0), this.animation)
               );
               break;
            default:
               TDraw.draw2DGradientRect(
                  var1.getMatrices(),
                  this.x + 4.0F,
                  this.y + 1.0F,
                  this.x + 4.0F + this.width - 8.0F,
                  this.y + 1.0F + this.height - 2.0F,
                  TDraw.applyOpacity(TColor.getColor(270), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(0), this.animation * 2.0F),
                  TDraw.applyOpacity(TColor.getColor(180), this.animation),
                  TDraw.applyOpacity(TColor.getColor(90), this.animation)
               );
         }
      }

      if (!this.module.getBindSetting().getKeyString().equalsIgnoreCase("none") && !this.binding) {
         TFonts.sf_medium_modules
            .drawString(
               var1.getMatrices(),
               this.getSbind(),
               this.x + this.width - 11.0F - TFonts.sf_medium_modules.getWidth(this.getSbind()),
               this.y + 6.0F,
               (this.module.isOn() ? TColor.textColor2 : TColor.textColor).getRGB()
            );
      }

      if (this.binding) {
         TFonts.sf_medium_modules
            .drawString(
               var1.getMatrices(),
               this.holdbind ? Formatting.GRAY + "Toggle / " + Formatting.RESET + "Hold" : Formatting.RESET + "Toggle " + Formatting.GRAY + "/ Hold",
               this.x + this.width - 11.0F - TFonts.sf_medium_modules.getWidth("Toggle/Hold"),
               var6 + 2.0F,
               TDraw.applyOpacity(Color.WHITE.getRGB(), this.animation2)
            );
      }

      if (this.hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)) {
         TFonts.sf_medium_modules
            .drawString(
               var1.getMatrices(),
               "Drawn " + (this.module.drawn.getValue() ? Formatting.GREEN + "TRUE" : Formatting.RED + "FALSE"),
               var5 + 1.0F,
               var6 + 2.0F,
               (this.module.isOn() ? TColor.textColor2 : TColor.textColor).getRGB()
            );
      } else if (this.binding) {
         TFonts.sf_medium_modules
            .drawString(
               var1.getMatrices(),
               "PressKey",
               var5,
               var6 + 2.0F,
               this.module.isOn()
                  ? TDraw.applyOpacity(TColor.textColor2.getRGB(), this.animation2)
                  : TDraw.applyOpacity(TColor.textColor.getRGB(), this.animation2)
            );
      } else if (TCfg.textSide == 0) {
         TFonts.sf_medium_modules
            .drawString(
               var1.getMatrices(), this.module.getDisplayName(), var5 + 2.0F, var6 + 2.0F, (this.module.isOn() ? TColor.textColor2 : TColor.textColor).getRGB()
            );
      } else {
         TFonts.sf_medium_modules
            .drawCenteredString(
               var1.getMatrices(),
               this.module.getDisplayName(),
               var5 + this.getWidth() / 2.0F - 4.0F,
               var6 + 2.0F,
               (this.module.isOn() ? TColor.textColor2 : TColor.textColor).getRGB()
            );
      }
   }

   private String getSbind() {
      String var1 = this.module.getBindSetting().getKeyString();
      if (var1.equals("LEFT_CONTROL")) {
         var1 = "LCtrl";
      }

      if (var1.equals("RIGHT_CONTROL")) {
         var1 = "RCtrl";
      }

      if (var1.equals("LEFT_SHIFT")) {
         var1 = "LShift";
      }

      if (var1.equals("RIGHT_SHIFT")) {
         var1 = "RShift";
      }

      if (var1.equals("LEFT_ALT")) {
         var1 = "LAlt";
      }

      if (var1.equals("RIGHT_ALT")) {
         var1 = "RAlt";
      }

      return var1;
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.binding) {
         if (var1 > this.x + 56.0F && var1 < this.x + 67.0F && var2 > this.y && var2 < this.y + this.height) {
            this.holdbind = false;
            this.module.getBindSetting().setHoldEnable(false);
            return;
         }

         if (var1 > this.x + 78.0F && var1 < this.x + 88.0F && var2 > this.y && var2 < this.y + this.height) {
            this.holdbind = true;
            this.module.getBindSetting().setHoldEnable(true);
            return;
         }

         this.bindModule(var3);
         this.binding = false;
      }

      if (this.hovered) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && var3 == 0) {
            this.module.drawn.setValue(!this.module.drawn.getValue());
            return;
         }

         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 261) && var3 == 0) {
            this.openResetDialog();
            return;
         }

         if (var3 == 0) {
            this.module.toggle();
        } else if (var3 == 1) {
            this.setOpen(!this.isOpen());
            if (this.open) {
               TSounds.playSwipeIn();
            } else {
               TSounds.playSwipeOut();
               this.elements.forEach(TAbstractElement::onClose);
            }

            this.animation = 0.5F;
         } else if (var3 == 2) {
            this.animation2 = 0.0F;
            this.binding = !this.binding;
            boolean var4 = this.binding;
         }
      }

      if (this.open) {
         this.elements.forEach(var3x -> {
            if (var3x.isVisible()) {
               var3x.mouseClicked(var1, var2, var3);
            }
         });
      }
   }

   private void openResetDialog() {
      try {
         boolean var1 = LangUtil.isChinese();
         TClickGuiScreen.activeDialog = new TResetDialog(
            var1 ? "\u91cd\u7f6e\u6a21\u5757" : "Reset module",
            var1
               ? "\u786e\u5b9a\u8981\u91cd\u7f6e " + this.module.getName() + " \u5417\uff1f"
               : "Are you sure you want to reset " + this.module.getName() + "?",
            var1 ? "\u662f" : "Yes",
            var1 ? "\u5426" : "No",
            () -> {
               try {
                  if (this.module.isOn()) {
                     this.module.disable();
                  }

                  for (Setting var2x : this.module.getSettings()) {
                     TAbstractElement.resetSetting(var2x);
                  }
               } catch (Throwable var3) {
               }
            },
            () -> {}
         );
      } catch (Throwable var2) {
      }
   }

   @Override
   public void mouseReleased(int var1, int var2, int var3) {
      if (this.isOpen()) {
         this.elements.forEach(var3x -> var3x.mouseReleased(var1, var2, var3));
      }
   }

   @Override
   public void charTyped(char var1, int var2) {
      if (this.isOpen()) {
         for (TAbstractElement var4 : this.elements) {
            if (var4.isVisible()) {
               var4.charTyped(var1, var2);
            }
         }
      }
   }

   @Override
   public void keyTyped(int var1) {
      if (this.isOpen()) {
         for (TAbstractElement var3 : this.elements) {
            var3.keyTyped(var1);
         }
      }

      if (this.binding) {
         if (var1 != 256 && var1 != 261) {
            this.bindModule(var1);
            CommandManager.sendMessage(this.module.getName() + " bind changed to " + this.module.getBindSetting().getKeyString());
         } else {
            this.bindModule(-1);
            CommandManager.sendMessage("Removed bind from " + this.module.getName());
         }

         this.binding = false;
      }
   }

   private void bindModule(int var1) {
      this.module.getBindSetting().setValue(var1);
      this.module.getBindSetting().setHoldEnable(this.holdbind);
   }

   @Override
   public void onGuiClosed() {
      this.elements.forEach(TAbstractElement::onClose);
   }

   public List<TAbstractElement> getElements() {
      return this.elements;
   }

   public double getElementsHeight() {
      return this.category_animation;
   }

   private int modeCount(Setting var1) {
      EnumSetting var2;
      Enum var3;
      return var1 instanceof EnumSetting && (var3 = (var2 = (EnumSetting)var1).getValue()) instanceof Enum
         ? ((Enum[])var3.getDeclaringClass().getEnumConstants()).length
         : 0;
   }

   public double interp(double var1, double var3, float var5) {
      return var3 + (var1 - var3) * var5;
   }

   public boolean isOpen() {
      return this.open;
   }

   public void setOpen(boolean var1) {
      this.open = var1;
   }

   @Override
   public void tick() {
      if (this.isOpen()) {
         this.gearAnimation.tick();
         this.ticksOpened++;
      } else {
         this.ticksOpened = 0;
      }
   }
}
