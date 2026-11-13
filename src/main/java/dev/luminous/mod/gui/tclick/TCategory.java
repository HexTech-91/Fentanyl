package dev.luminous.mod.gui.tclick;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.mod.gui.tclick.core.TAnim;
import dev.luminous.mod.gui.tclick.core.TAssets;
import dev.luminous.mod.gui.tclick.core.TCfg;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TI18n;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HudEditor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;

public class TCategory extends TAbstractCategory {
   private final Module.Category category;
   private final Identifier ICON;
   private boolean scrollHover;
   private final List<TAbstractButton> buttons = new ArrayList<>();
   public float catHeight;

   public TCategory(Module.Category var1, ArrayList<Module> var2, float var3, float var4, float var5, float var6) {
      super(var1.name(), var3, var4, var5, var6);
      this.category = var1;
      this.ICON = iconFor(var1);
      if (var1 == Module.Category.Client) {
         this.buttons.add(new TSearchBar());
      }

      var2.forEach(var1x -> this.buttons.add(new TModuleButton(var1x)));
   }

   public TCategory(String var1, Identifier var2, ArrayList<Module> var3, float var4, float var5, float var6, float var7) {
      super(var1, var4, var5, var6, var7);
      this.category = null;
      this.ICON = var2;
      var3.forEach(var1x -> this.buttons.add(new TModuleButton(var1x)));
   }

   private static Identifier iconFor(Module.Category var0) {
      switch (var0) {
         case Combat:
            return TAssets.COMBAT;
         case Misc:
            return TAssets.MISC;
         case Render:
            return TAssets.RENDER;
         case Movement:
            return TAssets.MOVEMENT;
         case Player:
            return TAssets.PLAYER;
         case Exploit:
            return TAssets.STOCK;
         case HUD:
            return TAssets.HUD;
         default:
            return TAssets.CLIENT;
      }
   }

   @Override
   public void init() {
      this.buttons.forEach(TAbstractButton::init);
   }

   private boolean isHudEditorHidden() {
      boolean on = HudEditor.INSTANCE != null && HudEditor.INSTANCE.isOn();
      if (this.category == null) {
         return false;
      }

      return this.category == Module.Category.HUD ? !on : on;
   }

   @Override
   public void render(DrawContext var1, int var2, int var3, float var4) {
      if (this.isHudEditorHidden()) {
         return;
      }

      super.render(var1, var2, var3, var4);
      this.setWidth(TCfg.moduleWidth);
      this.scrollHover = TDraw.isHovered(var2, var3, this.getX(), this.getY() + this.height, this.width, this.catHeight + 20.0F);
      var1.getMatrices().push();
      boolean var5 = false;
      float var6 = TCfg.scrollMode != 1 && !(this.getButtonsHeight() < TCfg.catHeight) ? TCfg.catHeight : (float)this.getButtonsHeight();
      this.catHeight = TAnim.fast(this.catHeight, var6, 30.0F);
      Color var7 = TColor.getColor(270);
      Color var8 = TColor.getColor(0);
      Color var9 = TColor.getColor(180);
      Color var10 = TColor.getColor(90);
      if (this.isOpen()) {
         TDraw.drawHudBase(var1.getMatrices(), this.getX() + 3.0F, this.getY() + this.height - 6.0F, this.width - 6.0F, this.catHeight, 1.0F, false);
         if (TCfg.scrollMode != 1 && !(this.getButtonsHeight() < TCfg.catHeight)) {
            TDraw.addWindow(
               var1.getMatrices(),
               this.getX() + 3.0F,
               this.getY() + this.height - 6.0F,
               this.getX() + 3.0F + this.width - 6.0F,
               this.getY() + this.height - 6.0F + TCfg.catHeight,
               1.0
            );
            var5 = true;
         }

         TDraw.drawBlurredShadow(
            var1.getMatrices(), (int)this.getX() + 4, (int)(this.getY() + this.height - 6.0F), (int)this.width - 8, 8.0F, 7, new Color(0, 0, 0, 180)
         );

         for (TAbstractButton var12 : this.buttons) {
            if (!(var12 instanceof TModuleButton var13 && TSearchBar.listening && !matchesSearch(var13.module))) {
               if (var5 && this.buttons.getFirst().getY() + this.moduleOffset < this.getY() + this.height) {
                  var12.setY(this.getY() + this.height + this.moduleOffset);
               } else {
                  var12.setY(this.getY() + this.height);
                  this.moduleOffset = 0.0F;
               }

               var12.setX(this.getX() + 2.0F);
               var12.setWidth(this.width - 4.0F);
               var12.setHeight(TCfg.moduleHeight);
               var12.render(var1, var2, var3, var4);
            }
         }
      }

      if (var5) {
         TDraw.popWindow();
      }

      TDraw.drawHudBase(var1.getMatrices(), this.getX() + 2.0F, this.getY() - 5.0F, this.width - 4.0F, this.height, 1.0F, false);
      RenderSystem.setShaderTexture(0, this.ICON);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      TDraw.addWindow(var1.getMatrices(), this.getX() + 2.0F, this.getY() - 4.0F, this.getX() + 2.0F + this.width - 4.0F, this.getY() - 5.0F + this.height, 1.0);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      BufferBuilder var14 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 85.0F,
         this.getY() + (this.height - 24.0F) / 2.0F,
         12.0,
         12.0,
         0.0F,
         0.0F,
         12.0,
         12.0,
         12.0,
         12.0,
         var7.darker(),
         var8.darker(),
         var9.darker(),
         var10.darker()
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 75.0F,
         this.getY() + (this.height - 34.0F) / 2.0F,
         16.0,
         16.0,
         0.0F,
         0.0F,
         16.0,
         16.0,
         16.0,
         16.0,
         var7,
         var8,
         var9,
         var10
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 65.0F,
         this.getY() + (this.height - 20.0F) / 2.0F,
         12.0,
         12.0,
         0.0F,
         0.0F,
         12.0,
         12.0,
         12.0,
         12.0,
         var7.darker().darker(),
         var8.darker().darker(),
         var9.darker().darker(),
         var10.darker().darker()
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 55.0F,
         this.getY() + (this.height - 28.0F) / 2.0F,
         6.0,
         6.0,
         0.0F,
         0.0F,
         6.0,
         6.0,
         6.0,
         6.0,
         var7,
         var8,
         var9,
         var10
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 45.0F,
         this.getY() + (this.height - 17.0F) / 2.0F,
         17.0,
         17.0,
         0.0F,
         0.0F,
         17.0,
         17.0,
         17.0,
         17.0,
         var7,
         var8,
         var9,
         var10
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 35.0F,
         this.getY() + (this.height - 30.0F) / 2.0F,
         15.0,
         15.0,
         0.0F,
         0.0F,
         15.0,
         15.0,
         15.0,
         15.0,
         var7.darker().darker().darker(),
         var8.darker().darker().darker(),
         var9.darker().darker().darker(),
         var10.darker().darker().darker()
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 25.0F,
         this.getY() + (this.height - 21.0F) / 2.0F,
         8.0,
         8.0,
         0.0F,
         0.0F,
         8.0,
         8.0,
         8.0,
         8.0,
         var7,
         var8,
         var9,
         var10
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 15.0F,
         this.getY() + (this.height - 22.0F) / 2.0F,
         12.0,
         12.0,
         0.0F,
         0.0F,
         12.0,
         12.0,
         12.0,
         12.0,
         var7.darker().darker().darker(),
         var8.darker().darker().darker(),
         var9.darker().darker().darker(),
         var10.darker().darker().darker()
      );
      TDraw.renderGradientTextureInternal(
         var14,
         var1.getMatrices(),
         this.getX() + 5.0F,
         this.getY() + (this.height - 28.0F) / 2.0F,
         20.0,
         20.0,
         0.0F,
         0.0F,
         20.0,
         20.0,
         20.0,
         20.0,
         var7,
         var8,
         var9,
         var10
      );
      BufferRenderer.drawWithGlobalProgram(var14.end());
      RenderSystem.disableBlend();
      TDraw.popWindow();
      String var15 = this.category != null ? TI18n.category(this.category) : super.getName();
      TDraw.drawBlurredShadow(
         var1.getMatrices(),
         (int)this.getX() + (this.width - 4.0F) / 2.0F - TFonts.categories.getWidth(var15) / 2.0F,
         (int)this.getY() + (int)this.height / 2.0F - 10.0F,
         TFonts.categories.getWidth(var15) + 6.0F,
         13.0F,
         20,
         TDraw.injectAlpha(Color.black, 170)
      );
      TFonts.categories
         .drawCenteredString(
            var1.getMatrices(),
            var15,
            (int)this.getX() + 2.0F + (this.width - 4.0F) / 2.0F,
            (int)this.getY() + (int)this.height / 2.0F - 7.0F,
            new Color(-1).getRGB()
         );
      var1.getMatrices().pop();
      this.updatePosition();
   }

   @Override
   public void mouseClicked(int var1, int var2, int var3) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (var3 == 1 && this.hovered) {
         this.setOpen(!this.isOpen());
      }

      super.mouseClicked(var1, var2, var3);
      if (this.isOpen() && this.scrollHover) {
         this.buttons.forEach(var3x -> var3x.mouseClicked(var1, var2, var3));
      }
   }

   @Override
   public void mouseReleased(int var1, int var2, int var3) {
      if (this.isHudEditorHidden()) {
         return;
      }

      super.mouseReleased(var1, var2, var3);
      if (this.isOpen()) {
         this.buttons.forEach(var3x -> var3x.mouseReleased(var1, var2, var3));
      }
   }

   @Override
   public boolean keyTyped(int var1) {
      if (this.isHudEditorHidden()) {
         return false;
      }

      if (this.isOpen()) {
         for (TAbstractButton var3 : this.buttons) {
            var3.keyTyped(var1);
         }
      }

      return false;
   }

   @Override
   public void charTyped(char var1, int var2) {
      if (this.isHudEditorHidden()) {
         return;
      }

      if (this.isOpen()) {
         for (TAbstractButton var4 : this.buttons) {
            var4.charTyped(var1, var2);
         }
      }
   }

   @Override
   public void onClose() {
      super.onClose();
      this.buttons.forEach(TAbstractButton::onGuiClosed);
   }

   @Override
   public void tick() {
      this.buttons.forEach(TAbstractButton::tick);
   }

   private void updatePosition() {
      float var1 = 0.0F;
      float var2 = 0.0F;

      for (TAbstractButton var4 : this.buttons) {
         if (!(var4 instanceof TModuleButton var5) || !TSearchBar.listening || matchesSearch(var5.module)) {
            var4.setTargetOffset(var1);
            TModuleButton var6;
            if (var4 instanceof TModuleButton && (var6 = (TModuleButton)var4).isOpen()) {
               for (TAbstractElement var8 : var6.getElements()) {
                  if (var8.isVisible()) {
                     var1 += var8.getHeight();
                  }
               }

               var1 += 2.0F;
            }

            var1 += var4.getHeight() + var2;
         }
      }
   }

   @Override
   public void hudClicked(Module var1) {
      for (TAbstractButton var3 : this.buttons) {
         if (var3 instanceof TModuleButton var4 && var4.module == var1) {
            this.setOpen(true);
            var4.setOpen(true);
         }
      }
   }

   private static boolean matchesSearch(Module var0) {
      String var1 = TSearchBar.moduleName == null ? "" : TSearchBar.moduleName.toLowerCase();
      return var1.isEmpty() ? true : var0.getName().toLowerCase().contains(var1) || var0.getDisplayName().toLowerCase().contains(var1);
   }

   public double getButtonsHeight() {
      double var1 = 8.0;

      for (TAbstractButton var4 : this.buttons) {
         if (!(var4 instanceof TModuleButton var5 && TSearchBar.listening && !matchesSearch(var5.module))) {
            if (var4 instanceof TModuleButton var6) {
               if (var6.isOpen()) {
                  var1 += 2.0;
               }

               var1 += var6.getElementsHeight();
            }

            var1 += var4.getHeight();
         }
      }

      return var1;
   }
}
