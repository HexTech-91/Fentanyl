package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PlayerInfo extends Module implements Wrapper {
   public static PlayerInfo INSTANCE;
   public final BooleanSetting showHealth = this.add(new BooleanSetting("Health", true));
   public final BooleanSetting showHealthBar = this.add(new BooleanSetting("HealthBar", true));
   public final BooleanSetting showTotems = this.add(new BooleanSetting("Totems", true));
   public final ColorSetting healthBarColor = this.add(new ColorSetting("HealthBarColor", new Color(255, 85, 85, 255)));
   public final ColorSetting islandColor = this.add(new ColorSetting("IslandColor", new Color(0, 0, 0, 255)));
   public final SliderSetting posX = this.add(new SliderSetting("PosX", 50, 0, 2000));
   public final SliderSetting posY = this.add(new SliderSetting("PosY", 50, 0, 2000));
   public BooleanSetting resetTotems = this.add(new BooleanSetting("Reset", false));
   private boolean hasFireResistance;
   private static final int HEAD_SIZE = 20;
   private static final int PADDING = 6;
   private static final int COMPONENT_SPACING = 3;
   private static final int HEALTH_BAR_HEIGHT = 4;
   private static final int HEALTH_BAR_RADIUS = 2;
   private int componentWidth;
   private int componentHeight;
   private int x;
   private int y;
   private boolean isDragging;
   private boolean wasLeftMouseDown;
   private double dragOffsetX;
   private double dragOffsetY;
   private boolean lastHadTotemEffects;

   public PlayerInfo() {
      super("PlayerInfo", Module.Category.Render);
      this.resetTotems.injectTask(() -> {
         if (mc.player != null) {
            Alien.POP.popContainer.remove(mc.player.getName().getString());
         }
      });
      this.isDragging = false;
      this.wasLeftMouseDown = false;
      this.lastHadTotemEffects = false;
      this.setChinese("\u73a9\u5bb6\u4fe1\u606f");
      INSTANCE = this;
      this.drawn.setValue(true);
      this.enable();
      this.x = (int)this.posX.getValue();
      this.y = (int)this.posY.getValue();
      FeatureTranslations.localizeModule(this);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (!nullCheck() && this.isOn()) {
         this.checkTotemPops();
         this.x = (int)this.posX.getValue();
         this.y = (int)this.posY.getValue();
         this.calculateDimensions();
         this.handleMouseDrag();
         this.drawComponent(var1);
      }
   }

   private void checkTotemPops() {
      this.checkTotemPops(false);
   }

   private ItemStack getArmorItem(int var1) {
      return mc.player != null && mc.player.getInventory() != null ? mc.player.getInventory().getArmorStack(var1) : ItemStack.EMPTY;
   }

   private int getArmorPercentage(int var1) {
      if (mc.player != null && mc.player.getInventory() != null) {
         if (mc.player.getInventory().getArmorStack(var1) == null) {
            return 0;
         } else {
            int var2 = mc.player.getInventory().getArmorStack(var1).getMaxDamage();
            int var3 = mc.player.getInventory().getArmorStack(var1).getDamage();
            return var2 <= 0 ? 0 : Math.max(0, Math.min(100, (int)((1.0 - (double)var3 / var2) * 100.0)));
         }
      } else {
         return 0;
      }
   }

   private void checkTotemPops(boolean var1) {
   }

   public void onTotemPop() {
   }

   private void calculateDimensions() {
      TextRenderer var1 = mc.textRenderer;
      int var2 = 0;
      int var3 = 1;
      if (this.showHealth.getValue()) {
         var3++;
      }

      if (this.showTotems.getValue()) {
         var3++;
      }

      String var4 = mc.player.getName().getString();
      var2 = Math.max(var2, var1.getWidth(var4));
      if (this.showHealth.getValue()) {
         String var5 = String.format("\u751f\u547d: %.1f/%.0f", mc.player.getHealth() + mc.player.getAbsorptionAmount(), mc.player.getMaxHealth());
         var2 = Math.max(var2, var1.getWidth(var5));
      }

      if (this.showTotems.getValue()) {
         int var8 = mc.player != null ? Alien.POP.getPop(mc.player.getName().getString()) : 0;
         String var6 = String.format("\u56fe\u817e: %d", var8);
         var2 = Math.max(var2, var1.getWidth(var6));
      }

      this.componentWidth = 32 + var2 + 6;
      Objects.requireNonNull(var1);
      this.componentHeight = 6 + var3 * 12 - 3 + 6 + (this.showHealthBar.getValue() ? 7 : 0);
   }

   private void drawComponent(DrawContext var1) {
      MatrixStack var4 = var1.getMatrices();
      TextRenderer var5 = mc.textRenderer;
      Color var6 = this.islandColor.getValue();
      byte var7 = 8;
      Render2DUtil.drawRoundedRect(var4, this.x, this.y, this.componentWidth, this.componentHeight, var7, var6);
      this.drawPlayerHead(var1, this.x + 6, this.y + (this.componentHeight - 20) / 2);
      int var8 = this.y + 6;
      int var9 = this.x + 6 + 20 + 6;
      int var10 = 16777215;
      var1.drawText(var5, Text.literal(mc.player.getName().getString()), var9, var8, var10, false);
      Objects.requireNonNull(var5);
      var8 += 12;
      if (this.showHealth.getValue()) {
         float var3 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         float var2 = mc.player.getMaxHealth();
         String var11 = String.format("\u751f\u547d: %.1f", var3);
         int var12 = 16777215;
         var1.drawText(var5, Text.literal(var11), var9, var8, var12, false);
         Objects.requireNonNull(var5);
         var8 += 12;
      }

      if (this.showTotems.getValue()) {
         int var25 = mc.player != null ? Alien.POP.getPop(mc.player.getName().getString()) : 0;
         String var26 = String.format("\u56fe\u817e: %d", var25);
         int var13 = 16777215;
         var1.drawText(var5, Text.literal(var26), var9, var8, var13, false);
         Objects.requireNonNull(var5);
         var8 += 12;
      }

      if (this.showHealthBar.getValue()) {
         float var22 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         float var21 = mc.player.getMaxHealth();
         int var28 = var8 + 3 - 2;
         int var14 = this.x + 6;
         int var15 = this.componentWidth - 12;
         Render2DUtil.drawRoundedRect(var4, var14, var28, var15, 4.0F, 2.0F, new Color(-13421773));
         float var16 = Math.min(var22, 20.0F);
         int var17 = (int)(var15 * (var16 / 20.0F));
         Color var18 = this.healthBarColor.getValue();
         if (var17 > 0) {
            Render2DUtil.drawRoundedRect(var4, var14, var28, var17, 4.0F, 2.0F, var18);
         }

         String var19 = String.format("%.1f", var22);
         float var27;
         if (var17 > 0 && (var27 = var5.getWidth(var19) * 0.5F) <= Math.max(var17 - 4, 0)) {
            var9 = var14 + (var17 - (int)var27) / 2;
            Objects.requireNonNull(var5);
            int var20 = var28 + 0;
            var4.push();
            var4.scale(0.5F, 0.5F, 1.0F);
            var1.drawText(var5, Text.literal(var19), (int)(var9 / 0.5F), (int)(var20 / 0.5F), 16777215, false);
            var4.pop();
         }
      }
   }

   private void drawPlayerHead(DrawContext var1, int var2, int var3) {
      if (mc.player != null) {
         ClientPlayerEntity var4 = mc.player;
         Identifier var5 = var4.getSkinTextures().texture();
         var1.drawTexture(var5, var2, var3, 20, 20, 8.0F, 8.0F, 8, 8, 64, 64);

         try {
            Class var6 = Class.forName("net.minecraft.client.render.entity.model.PlayerModelPart");
            Field var7 = var6.getField("HAT");
            Object var8 = var7.get(null);
            Method var9 = AbstractClientPlayerEntity.class.getMethod("isPartVisible", var6);
            boolean var10 = (Boolean)var9.invoke(var4, var8);
            if (var10) {
               var1.drawTexture(var5, var2, var3, 20, 20, 40.0F, 8.0F, 8, 8, 64, 64);
            }
         } catch (Exception var11) {
         }
      }
   }

   private void handleMouseDrag() {
      if (mc.mouse != null) {
         double var1 = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
         double var3 = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
         boolean var5 = var1 >= this.x && var1 <= this.x + this.componentWidth && var3 >= this.y && var3 <= this.y + this.componentHeight;
         boolean var6 = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1;
         boolean var7 = var6 && !this.wasLeftMouseDown;
         boolean var8 = mc.currentScreen instanceof ClickGuiScreen;
         if (!var8) {
            this.isDragging = false;
            this.wasLeftMouseDown = var6;
         } else {
            if (var6) {
               if (var7 && var5) {
                  this.isDragging = true;
                  this.dragOffsetX = var1 - this.x;
                  this.dragOffsetY = var3 - this.y;
               }
            } else {
               this.isDragging = false;
            }

            if (this.isDragging) {
               this.x = (int)(var1 - this.dragOffsetX);
               this.y = (int)(var3 - this.dragOffsetY);
               this.x = Math.max(0, Math.min(this.x, mc.getWindow().getScaledWidth() - this.componentWidth));
               this.y = Math.max(0, Math.min(this.y, mc.getWindow().getScaledHeight() - this.componentHeight));
               this.posX.setValue(this.x);
               this.posY.setValue(this.y);
            }

            this.wasLeftMouseDown = var6;
         }
      }
   }

   private int getHealthColor(float var1, float var2) {
      if (var2 <= 0.0F) {
         return 16733525;
      } else {
         float var3 = var1 / var2;
         if (var3 > 0.6F) {
            return 5635925;
         } else {
            return var3 > 0.3F ? 16777045 : 16733525;
         }
      }
   }

   private void drawGlowEffect(DrawContext var1) {
      MatrixStack var2 = var1.getMatrices();
      Color var3 = new Color(30, 30, 35, 40);
      int var4 = this.componentWidth + 8;
      int var5 = this.componentHeight + 8;
      int var6 = this.x - 4;
      int var7 = this.y - 4;
      int var8 = var3.getAlpha();
      Color var9 = new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), var8);
      byte var10 = 20;
      int var11 = (int)(var8 * 0.1F);
      Color var12 = new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), var11);
      Render2DUtil.drawRoundedRect(var2, var6 - var10, var7 - var10, var4 + var10 * 2, var5 + var10 * 2, var10 / 2, var12);
      int var13 = (int)(var8 * 0.3F);
      Color var14 = new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), var13);
      Render2DUtil.drawRoundedRect(var2, var6 - 3, var7 - 3, var4 + 6, var5 + 6, 8.0F, var14);
      int var15 = (int)(var8 * 0.6F);
      Color var16 = new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), var15);
      Render2DUtil.drawRoundedRect(var2, var6 - 1, var7 - 1, var4 + 2, var5 + 2, 6.0F, var16);
      Render2DUtil.drawRoundedRect(var2, var6, var7, var4, var5, 4.0F, var9);
      byte var17 = 25;
      Color var18 = new Color(30, 30, 35, var17);
      Render2DUtil.drawRoundedRect(var2, var6 - 2, var7 - 2, var4 + 4, var5 + 4, 6.0F, var18);
      byte var19 = 3;

      for (int var20 = 0; var20 < var19; var20++) {
         float var21 = (float)(var20 * 2 * Math.PI / var19);
         int var22 = var6 + var4 / 2 + (int)(Math.cos(var21) * (var4 / 2 + 5));
         int var23 = var7 + var5 / 2 + (int)(Math.sin(var21) * (var5 / 2 + 5));
         byte var24 = 3;
         byte var25 = 15;
         Color var26 = new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), var25);
         Render2DUtil.drawRoundedRect(var2, var22 - var24, var23 - var24, var24 * 2, var24 * 2, var24, var26);
      }
   }
}
