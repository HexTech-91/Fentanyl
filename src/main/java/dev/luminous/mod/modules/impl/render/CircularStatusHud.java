package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;

public class CircularStatusHud extends Module {
   public static CircularStatusHud INSTANCE;
   private float currentFade = 1.0F;
   private long lastRenderTime = 0L;
   public final BooleanSetting hideVanillaCrosshair = this.add(new BooleanSetting("HideVanillaCrosshair", true));
   private final EnumSetting<CircularStatusHud.CrosshairMode> crosshairMode = this.add(
      new EnumSetting<>("CrosshairMode", CircularStatusHud.CrosshairMode.SolidCircle, () -> this.hideVanillaCrosshair.getValue())
   );
   private final SliderSetting crosshairSize = this.add(
      new SliderSetting(
         "CrosshairSize",
         4.0,
         1.0,
         20.0,
         0.5,
         () -> this.hideVanillaCrosshair.getValue() && this.crosshairMode.getValue() != CircularStatusHud.CrosshairMode.None
      )
   );
   private final ColorSetting crosshairColor = this.add(new ColorSetting("CrosshairColor", new Color(255, 255, 255, 255)));
   private final SliderSetting crosshairThick = this.add(
      new SliderSetting(
         "CrosshairThick",
         1.5,
         0.5,
         5.0,
         0.1,
         () -> this.hideVanillaCrosshair.getValue() && this.crosshairMode.getValue() == CircularStatusHud.CrosshairMode.Arcs
      )
   );
   private final SliderSetting crosshairGap = this.add(
      new SliderSetting(
         "CrosshairGap",
         30.0,
         0.0,
         80.0,
         1.0,
         () -> this.hideVanillaCrosshair.getValue() && this.crosshairMode.getValue() == CircularStatusHud.CrosshairMode.Arcs
      )
   );
   private final SliderSetting radius = this.add(new SliderSetting("Radius", 25.0, 10.0, 100.0, 1.0));
   private final SliderSetting thickness = this.add(new SliderSetting("Thickness", 4.0, 1.0, 20.0, 0.5));
   private final SliderSetting gap = this.add(new SliderSetting("GapDegree", 10.0, 0.0, 30.0, 1.0));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.25, 3.0, 0.05));
   private final SliderSetting globalAlpha = this.add(new SliderSetting("GlobalAlpha", 1.0, 0.1, 1.0, 0.01));
   private final SliderSetting fadeRange = this.add(new SliderSetting("FadeRange", 20.0, 0.0, 120.0, 1.0).setSuffix("m"));
   private final ColorSetting bgColor = this.add(new ColorSetting("BgColor", new Color(40, 40, 40, 150)));
   private final ColorSetting healthColor = this.add(new ColorSetting("HealthColor", new Color(255, 50, 50, 255)));
   private final ColorSetting absorbColor = this.add(new ColorSetting("AbsorbColor", new Color(255, 200, 0, 255)));
   private final ColorSetting turtleColor = this.add(new ColorSetting("TurtleColor", new Color(0, 200, 150, 255)));

   public CircularStatusHud() {
      super("CircularStatus", Module.Category.Render);
      this.setChinese("\u73af\u5f62\u72b6\u6001\u680f");
      INSTANCE = this;
   }

   public static boolean shouldHideVanillaCrosshair() {
      return INSTANCE != null && INSTANCE.isOn() && INSTANCE.hideVanillaCrosshair.getValue();
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      this.renderElement(var1, var2);
   }

   public void renderElement(DrawContext var1, float var2) {
      if (mc.player != null && mc.getWindow() != null) {
         int var3 = mc.getWindow().getScaledWidth() / 2;
         int var4 = mc.getWindow().getScaledHeight() / 2;
         if (this.hideVanillaCrosshair.getValue() && this.crosshairMode.getValue() != CircularStatusHud.CrosshairMode.None) {
            float var5 = this.crosshairSize.getValueFloat();
            if (this.crosshairMode.getValue() == CircularStatusHud.CrosshairMode.Square) {
               float var24 = var5 / 2.0F;
               var1.fill(
                  (int)(var3 - var24), (int)(var4 - var24), (int)(var3 - var24 + var5), (int)(var4 - var24 + var5), this.crosshairColor.getValue().getRGB()
               );
            } else if (this.crosshairMode.getValue() == CircularStatusHud.CrosshairMode.SolidCircle) {
               float var23 = var5 / 4.0F;
               float var25 = var5 / 2.0F;
               this.drawArc(var1, var3, var4, var23, var25, 0.0F, 360.0F, this.crosshairColor.getValue(), 1.0F);
            } else if (this.crosshairMode.getValue() == CircularStatusHud.CrosshairMode.Arcs) {
               float var6 = this.crosshairThick.getValueFloat();
               float var7 = this.crosshairGap.getValueFloat();
               float var8 = 90.0F - var7;
               if (var8 > 0.0F) {
                  for (float var12 : new float[]{0.0F, 90.0F, 180.0F, 270.0F}) {
                     float var13 = var12 - var8 / 2.0F;
                     this.drawArc(var1, var3, var4, var5, var6, var13, var8, this.crosshairColor.getValue(), 1.0F);
                  }
               }
            }
         }

         long var22 = System.currentTimeMillis();
         if (this.lastRenderTime == 0L) {
            this.lastRenderTime = var22;
         }

         long var26 = var22 - this.lastRenderTime;
         this.lastRenderTime = var22;
         float var27 = (float)var26 / 300.0F;
         this.currentFade = this.hasPlayerInRange() ? Math.min(1.0F, this.currentFade + var27) : Math.max(0.0F, this.currentFade - var27);
         if (!(this.currentFade <= 0.0F)) {
            float var28 = this.size.getValueFloat();
            float var29 = this.radius.getValueFloat() * var28;
            float var30 = this.thickness.getValueFloat() * var28;
            float var31 = this.gap.getValueFloat();
            float var14 = 120.0F - var31;
            float var15 = this.currentFade * this.globalAlpha.getValueFloat();
            float var16 = clamp(mc.player.getHealth() / 20.0F);
            float var17 = clamp(mc.player.getAbsorptionAmount() / 16.0F);
            float var18 = 0.0F;
            StatusEffectInstance var19 = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
            if (var19 != null && var19.getAmplifier() >= 2) {
               int var20 = var19.getDuration();
               int var21 = Math.round(var20 / 400.0F * 100.0F);
               var18 = clamp(var21 / 100.0F);
            }

            if (this.bgColor.getValue().getAlpha() > 0) {
               this.drawArc(var1, var3, var4, var29, var30, 270.0F - var31 / 2.0F, -var14, this.bgColor.getValue(), var15);
               this.drawArc(var1, var3, var4, var29, var30, 30.0F - var31 / 2.0F, -var14, this.bgColor.getValue(), var15);
               this.drawArc(var1, var3, var4, var29, var30, 30.0F + var31 / 2.0F, var14, this.bgColor.getValue(), var15);
            }

            if (var16 > 0.0F) {
               this.drawArc(var1, var3, var4, var29, var30, 270.0F - var31 / 2.0F, -var14 * var16, this.healthColor.getValue(), var15);
            }

            if (var17 > 0.0F) {
               this.drawArc(var1, var3, var4, var29, var30, 30.0F - var31 / 2.0F, -var14 * var17, this.absorbColor.getValue(), var15);
            }

            if (var18 > 0.0F) {
               this.drawArc(var1, var3, var4, var29, var30, 30.0F + var31 / 2.0F, var14 * var18, this.turtleColor.getValue(), var15);
            }
         }
      }
   }

   private void drawArc(DrawContext var1, float var2, float var3, float var4, float var5, float var6, float var7, Color var8, float var9) {
      if (!(Math.abs(var7) < 0.1F)) {
         float var10 = clamp(var8.getAlpha() / 255.0F * var9);
         if (!(var10 <= 0.01F)) {
            float var11 = Math.max(0.5F, var4 + var5 / 2.0F);
            float var12 = Math.max(0.0F, var4 - var5 / 2.0F);
            float var13 = var11 - var12;
            if (!(var13 <= 0.0F)) {
               int var14 = var8.getRed();
               int var15 = var8.getGreen();
               int var16 = var8.getBlue();
               float var17 = Math.abs(var7) * (float) (Math.PI / 180.0);
               int var18 = (int)Math.ceil(var17 * var11);
               var18 = Math.max(128, Math.min(2048, var18));
               float var19 = Math.min(2.0F, Math.max(0.5F, var5 * 0.5F));
               float var20 = Math.min(0.35F, var19 / var13);
               float var21 = Math.abs(var7) >= 359.0F ? 0.0F : Math.min(0.25F, 3.0F / Math.abs(var7));
               Matrix4f var22 = var1.getMatrices().peek().getPositionMatrix();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
               BufferBuilder var23 = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
               byte var24 = 5;

               for (int var25 = 0; var25 < var24; var25++) {
                  float var26 = (float)var25 / var24;
                  float var27 = (float)(var25 + 1) / var24;
                  float var28 = var12 + var13 * var26;
                  float var29 = var12 + var13 * var27;
                  float var30 = radialAlpha(var26, var20);
                  float var31 = radialAlpha(var27, var20);
                  if (var25 > 0) {
                     int var32 = packColor(var10 * var30 * endAlpha(1.0F, var21), var14, var15, var16);
                     double var33 = Math.toRadians(var6 + var7);
                     double var35 = Math.toRadians(var6);
                     var23.vertex(var22, var2 + (float)Math.cos(var33) * var28, var3 + (float)Math.sin(var33) * var28, 0.0F).color(var32);
                     var23.vertex(var22, var2 + (float)Math.cos(var35) * var28, var3 + (float)Math.sin(var35) * var28, 0.0F).color(var32);
                  }

                  for (int var40 = 0; var40 <= var18; var40++) {
                     float var41 = (float)var40 / var18;
                     double var34 = Math.toRadians(var6 + var7 * var41);
                     float var36 = (float)Math.cos(var34);
                     float var37 = (float)Math.sin(var34);
                     float var38 = endAlpha(var41, var21);
                     var23.vertex(var22, var2 + var36 * var28, var3 + var37 * var28, 0.0F).color(packColor(var10 * var30 * var38, var14, var15, var16));
                     var23.vertex(var22, var2 + var36 * var29, var3 + var37 * var29, 0.0F).color(packColor(var10 * var31 * var38, var14, var15, var16));
                  }
               }

               BufferRenderer.drawWithGlobalProgram(var23.end());
               RenderSystem.enableCull();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   private static float radialAlpha(float var0, float var1) {
      if (var1 <= 0.0F) {
         return 1.0F;
      } else if (var0 <= var1) {
         return var0 / var1;
      } else {
         return var0 >= 1.0F - var1 ? (1.0F - var0) / var1 : 1.0F;
      }
   }

   private static float endAlpha(float var0, float var1) {
      return var1 <= 0.0F ? 1.0F : clamp(Math.min(var0, 1.0F - var0) / var1);
   }

   private static int packColor(float var0, int var1, int var2, int var3) {
      return (int)(clamp(var0) * 255.0F) << 24 | var1 << 16 | var2 << 8 | var3;
   }

   private boolean hasPlayerInRange() {
      if (mc.world != null && mc.player != null) {
         float var1 = this.fadeRange.getValueFloat();
         if (var1 <= 0.0F) {
            return true;
         } else {
            double var2 = (double)var1 * var1;

            for (PlayerEntity var5 : mc.world.getPlayers()) {
               if (var5 != mc.player && var5.isAlive() && !(var5.squaredDistanceTo(mc.player) > var2)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static float clamp(float var0) {
      return Math.max(0.0F, Math.min(1.0F, var0));
   }

   public String getElementName() {
      return this.getDisplayName();
   }

   public Module getModule() {
      return this;
   }

   public int getElementX() {
      return mc.getWindow() == null ? 0 : mc.getWindow().getScaledWidth() / 2 - this.getElementWidth() / 2;
   }

   public void setElementX(int var1) {
   }

   public int getElementY() {
      return mc.getWindow() == null ? 0 : mc.getWindow().getScaledHeight() / 2 - this.getElementHeight() / 2;
   }

   public void setElementY(int var1) {
   }

   public int getElementWidth() {
      return (int)(this.radius.getValueFloat() * 2.0F * this.size.getValueFloat());
   }

   public int getElementHeight() {
      return (int)(this.radius.getValueFloat() * 2.0F * this.size.getValueFloat());
   }

   public boolean isElementVisible() {
      return this.isOn();
   }

   public boolean isDraggable() {
      return false;
   }

   public static enum CrosshairMode {
      None,
      Square,
      Arcs,
      SolidCircle;
   }
}
