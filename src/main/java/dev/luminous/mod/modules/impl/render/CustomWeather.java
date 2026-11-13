package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.WeatherRenderEvent;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome.Precipitation;

public class CustomWeather extends Module {
   public static CustomWeather INSTANCE;
   private static final Identifier RAIN = Identifier.of("minecraft", "textures/environment/rain.png");
   private static final Identifier SNOW = Identifier.of("minecraft", "textures/environment/snow.png");
   private final EnumSetting<CustomWeather.PrecipitationType> precipitation = this.add(new EnumSetting<>("Precipitation", CustomWeather.PrecipitationType.Snow));
   private final SliderSetting height = this.add(new SliderSetting("WeatherHeight", 0.0, -64.0, 320.0));
   private final SliderSetting strength = this.add(new SliderSetting("Strength", 0.8, 0.0, 2.0));
   private final ColorSetting weatherColor = this.add(new ColorSetting("WeatherColor", Color.WHITE));
   private final SliderSetting expandSize = this.add(new SliderSetting("ExpandSize", 5.0, 1.0, 10.0));
   private final SliderSetting snowFallingSpeedMultiplier = this.add(new SliderSetting("SnowFallingSpeedMultiplier", 1.0, 0.0, 10.0));
   private int ticks;
   private static final float[] weatherXCoords = new float[1024];
   private static final float[] weatherYCoords = new float[1024];

   public CustomWeather() {
      super("CustomWeather", Module.Category.Render);
      this.setChinese("\u81ea\u5b9a\u4e49\u5929\u6c14");
      this.precipitation.setChinese("\u964d\u6c34\u7c7b\u578b");
      this.height.setChinese("\u5929\u6c14\u9ad8\u5ea6");
      this.strength.setChinese("\u5f3a\u5ea6");
      this.weatherColor.setChinese("\u5929\u6c14\u989c\u8272");
      this.expandSize.setChinese("\u6269\u6563\u8303\u56f4");
      this.snowFallingSpeedMultiplier.setChinese("\u96ea\u82b1\u4e0b\u843d\u901f\u5ea6");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.ticks = 0;
   }

   @EventListener
   public void onWeather(WeatherRenderEvent var1) {
      if (!nullCheck()) {
         if (this.precipitation.is(CustomWeather.PrecipitationType.Both)) {
            this.render(var1, CustomWeather.PrecipitationType.Rain);
            this.render(var1, CustomWeather.PrecipitationType.Snow);
         } else {
            this.render(var1, this.precipitation.getValue());
         }

         var1.cancel();
      }
   }

   private void render(WeatherRenderEvent var1, CustomWeather.PrecipitationType var2) {
      LightmapTextureManager var3 = var1.lightmapTextureManager;
      double var4 = var1.cameraX;
      double var6 = var1.cameraY;
      double var8 = var1.cameraZ;
      float var10 = var1.tickDelta;
      float var11 = this.strength.getValueFloat();
      float var12 = this.weatherColor.getValue().getRed() / 255.0F;
      float var13 = this.weatherColor.getValue().getBlue() / 255.0F;
      float var14 = this.weatherColor.getValue().getGreen() / 255.0F;
      var3.enable();
      int var15 = MathHelper.floor(var4);
      int var16 = MathHelper.floor(var6);
      int var17 = MathHelper.floor(var8);
      Tessellator var18 = Tessellator.getInstance();
      BufferBuilder var19 = var18.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
      RenderSystem.setShader(GameRenderer::getParticleProgram);
      Mutable var20 = new Mutable();
      int var21 = this.expandSize.getValueInt();
      byte var22 = -1;
      float var23 = this.ticks + var10;

      for (int var24 = var17 - var21; var24 <= var17 + var21; var24++) {
         for (int var25 = var15 - var21; var25 <= var15 + var21; var25++) {
            int var26 = (var24 - var17 + 16) * 32 + var25 - var15 + 16;
            if (var26 >= 0 && var26 <= 1023) {
               double var27 = weatherXCoords[var26] * 0.5;
               double var29 = weatherYCoords[var26] * 0.5;
               var20.set(var25, var6, var24);
               int var31 = this.height.getValueInt();
               int var32 = var16 - var21;
               int var33 = var16 + var21;
               if (var32 < var31) {
                  var32 = var31;
               }

               if (var33 < var31) {
                  var33 = var31;
               }

               int var34 = Math.max(var31, var16);
               if (var32 != var33) {
                  Random var35 = Random.create((long)var25 * var25 * 3121L + var25 * 45238971L ^ (long)var24 * var24 * 418711L + var24 * 13761L);
                  var20.set(var25, var32, var24);
                  Precipitation var36 = var2.toMC();
                  if (var36 == Precipitation.RAIN) {
                     if (var22 != 0) {
                        if (var22 >= 0) {
                           Render3DUtil.endBuilding(var19);
                        }

                        var22 = 0;
                        RenderSystem.setShaderTexture(0, RAIN);
                        var18.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
                     }

                     int var37 = this.ticks + var25 * var25 * 3121 + var25 * 45238971 + var24 * var24 * 418711 + var24 * 13761 & 31;
                     float var38 = -(var37 + var10) / 32.0F * (3.0F + var35.nextFloat());
                     double var39 = var25 + 0.5 - var4;
                     double var41 = var24 + 0.5 - var8;
                     float var43 = (float)Math.sqrt(var39 * var39 + var41 * var41) / var21;
                     float var44 = ((1.0F - var43 * var43) * 0.5F + 0.5F) * var11;
                     var20.set(var25, var34, var24);
                     int var45 = WorldRenderer.getLightmapCoordinates(mc.world, var20);
                     var19.vertex((float)(var25 - var4 - var27 + 0.5), (float)(var33 - var6), (float)(var24 - var8 - var29 + 0.5))
                        .texture(0.0F, var32 * 0.25F + var38)
                        .color(var12, var14, var13, var44)
                        .light(var45);
                     var19.vertex((float)(var25 - var4 + var27 + 0.5), (float)(var33 - var6), (float)(var24 - var8 + var29 + 0.5))
                        .texture(1.0F, var32 * 0.25F + var38)
                        .color(var12, var14, var13, var44)
                        .light(var45);
                     var19.vertex((float)(var25 - var4 + var27 + 0.5), (float)(var32 - var6), (float)(var24 - var8 + var29 + 0.5))
                        .texture(1.0F, var33 * 0.25F + var38)
                        .color(var12, var14, var13, var44)
                        .light(var45);
                     var19.vertex((float)(var25 - var4 - var27 + 0.5), (float)(var32 - var6), (float)(var24 - var8 - var29 + 0.5))
                        .texture(0.0F, var33 * 0.25F + var38)
                        .color(var12, var14, var13, var44)
                        .light(var45);
                  } else if (var36 == Precipitation.SNOW) {
                     if (var22 != 1) {
                        if (var22 == 0) {
                           Render3DUtil.endBuilding(var19);
                        }

                        var22 = 1;
                        RenderSystem.setShaderTexture(0, SNOW);
                        var18.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
                     }

                     float var51 = -((this.ticks & 511) + var10) / 512.0F;
                     float var52 = (float)(var35.nextDouble() + var23 * 0.01 * (float)var35.nextGaussian());
                     float var53 = (float)(var35.nextDouble() + var23 * (float)var35.nextGaussian() * 0.001) * this.snowFallingSpeedMultiplier.getValueInt();
                     double var40 = var25 + 0.5 - var4;
                     double var42 = var24 + 0.5 - var8;
                     float var54 = (float)Math.sqrt(var40 * var40 + var42 * var42) / var21;
                     float var55 = ((1.0F - var54 * var54) * 0.3F + 0.5F) * var11;
                     var20.set(var25, var34, var24);
                     int var46 = WorldRenderer.getLightmapCoordinates(mc.world, var20);
                     int var47 = var46 >> 16 & 65535;
                     int var48 = var46 & 65535;
                     int var49 = (var47 * 3 + 240) / 4;
                     int var50 = (var48 * 3 + 240) / 4;
                     var19.vertex((float)(var25 - var4 - var27 + 0.5), (float)(var33 - var6), (float)(var24 - var8 - var29 + 0.5))
                        .texture(0.0F + var52, var32 * 0.25F + var51 + var53)
                        .color(var12, var14, var13, var55)
                        .light(var50, var49);
                     var19.vertex((float)(var25 - var4 + var27 + 0.5), (float)(var33 - var6), (float)(var24 - var8 + var29 + 0.5))
                        .texture(1.0F + var52, var32 * 0.25F + var51 + var53)
                        .color(var12, var14, var13, var55)
                        .light(var50, var49);
                     var19.vertex((float)(var25 - var4 + var27 + 0.5), (float)(var32 - var6), (float)(var24 - var8 + var29 + 0.5))
                        .texture(1.0F + var52, var33 * 0.25F + var51 + var53)
                        .color(var12, var14, var13, var55)
                        .light(var50, var49);
                     var19.vertex((float)(var25 - var4 - var27 + 0.5), (float)(var32 - var6), (float)(var24 - var8 - var29 + 0.5))
                        .texture(0.0F + var52, var33 * 0.25F + var51 + var53)
                        .color(var12, var14, var13, var55)
                        .light(var50, var49);
                  }
               }
            }
         }
      }

      if (var22 >= 0) {
         Render3DUtil.endBuilding(var19);
      }

      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      var3.disable();
   }

   @EventListener
   public void onTick(TickEvent var1) {
      if (!var1.isPost() && !nullCheck()) {
         this.ticks++;
      }
   }

   static {
      for (int var0 = 0; var0 < 32; var0++) {
         for (int var1 = 0; var1 < 32; var1++) {
            float var2 = var1 - 16;
            float var3 = var0 - 16;
            float var4 = MathHelper.sqrt(var2 * var2 + var3 * var3);
            weatherXCoords[var0 << 5 | var1] = -var3 / var4;
            weatherYCoords[var0 << 5 | var1] = var2 / var4;
         }
      }
   }

   public static enum PrecipitationType {
      None,
      Rain,
      Snow,
      Both;

      public Precipitation toMC() {
         return switch (this) {
            case None -> Precipitation.NONE;
            case Rain -> Precipitation.RAIN;
            case Snow, Both -> Precipitation.SNOW;
         };
      }
   }
}
