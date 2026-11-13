package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class FireFlies extends Module {
   private final BooleanSetting darkImprint = this.add(new BooleanSetting("DarkImprint", false));
   private final BooleanSetting lighting = this.add(new BooleanSetting("Lighting", false));
   private final BooleanSetting rainbowColor = this.add(new BooleanSetting("RainbowColor", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4000.0, 500.0, 10000.0, 250.0));
   private final SliderSetting spawnDelay = this.add(new SliderSetting("SpawnDelay", 3.0, 1.0, 10.0, 0.5));
   private final SliderSetting animationSpeed = this.add(new SliderSetting("AnimationSpeed", 1.0, 0.2, 3.0, 0.1));
   private final List<FireFlies.FirePart> fireParts = new ArrayList<>();
   private final Identifier firePartTexture = Identifier.of("alien", "textures/fireflies/firepart.png");
   private final Random random = new Random();

   public FireFlies() {
      super("FireFlies", Module.Category.Render);
      this.setChinese("\u8424\u706b\u866b\u7c92\u5b50");
   }

   private long getMaxPartAliveTime() {
      float var1 = (float)this.animationSpeed.getValue();
      return (long)(6000.0F / var1);
   }

   private int getPartColor() {
      if (this.rainbowColor.getValue()) {
         float var1 = (float)this.rainbowSpeed.getValue();
         if (var1 < 100.0F) {
            var1 = 100.0F;
         }

         float var2 = (float)(System.currentTimeMillis() % (long)var1) / var1;
         return 0xFF000000 | Color.getHSBColor(var2, 1.0F, 1.0F).getRGB() & 16777215;
      } else {
         return 0xFF000000 | this.color.getValue().getRGB() & 16777215;
      }
   }

   private float getAnimationSpeed() {
      float var1 = (float)this.animationSpeed.getValue();
      return MathHelper.clamp(var1, 0.2F, 3.0F);
   }

   private double random(double var1, double var3) {
      return var1 + (var3 - var1) * this.random.nextDouble();
   }

   private Vec3d generateVecForPart(double var1, double var3) {
      Vec3d var5 = mc.player.getPos();
      Vec3d var6 = var5.add(this.random(-var1, var1), this.random(-var3 / 2.0, var3), this.random(-var1, var1));

      for (int var7 = 0; var7 < 30; var7++) {
         var6 = var5.add(this.random(-var1, var1), this.random(-var3 / 2.0, var3), this.random(-var1, var1));
      }

      return var6;
   }

   private void bindTexture(Identifier var1) {
      RenderSystem.setShaderTexture(0, var1);
   }

   private void drawBoundTexture(MatrixStack var1, float var2, float var3, float var4, float var5, int var6) {
      float var7 = (var6 >> 24 & 0xFF) / 255.0F;
      float var8 = (var6 >> 16 & 0xFF) / 255.0F;
      float var9 = (var6 >> 8 & 0xFF) / 255.0F;
      float var10 = (var6 & 0xFF) / 255.0F;
      Matrix4f var11 = var1.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      BufferBuilder var12 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      var12.vertex(var11, var2, var3, 0.0F).texture(0.0F, 0.0F).color(var8, var9, var10, var7);
      var12.vertex(var11, var2, var5, 0.0F).texture(0.0F, 1.0F).color(var8, var9, var10, var7);
      var12.vertex(var11, var4, var5, 0.0F).texture(1.0F, 1.0F).color(var8, var9, var10, var7);
      var12.vertex(var11, var4, var3, 0.0F).texture(1.0F, 0.0F).color(var8, var9, var10, var7);
      BufferRenderer.drawWithGlobalProgram(var12.end());
   }

   private void drawBillboard(MatrixStack var1, FireFlies.FirePart var2, float var3, int var4) {
      Vec3d var5 = var2.getRenderPosVec(var3);
      var1.push();
      var1.translate(var5.x, var5.y, var5.z);
      float var6 = mc.gameRenderer.getCamera().getYaw();
      float var7 = mc.gameRenderer.getCamera().getPitch();
      boolean var8 = mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT;
      var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-var6));
      var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var7 * (var8 ? -1.0F : 1.0F)));
      var1.scale(-0.1F, -0.1F, 0.1F);
      float var9 = 7.0F;
      this.bindTexture(this.firePartTexture);
      this.drawBoundTexture(var1, -var9 / 2.0F, -var9 / 2.0F, var9 / 2.0F, var9 / 2.0F, var4);
      if (this.lighting.getValue()) {
         var9 *= 3.0F;
         int var10 = this.darker(var4, 0.8F);
         int var11 = this.applyOpacity(var10, this.getAlpha(var4) / 7.0F / 255.0F);
         this.drawBoundTexture(var1, -var9 / 2.0F, -var9 / 2.0F, var9 / 2.0F, var9 / 2.0F, var11);
      }

      var1.pop();
   }

   @EventListener
   public void onUpdate(ClientTickEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.age == 1) {
            this.fireParts.forEach(FireFlies.FirePart::setToRemove);
         }

         this.fireParts.forEach(FireFlies.FirePart::updatePart);
         this.fireParts.removeIf(FireFlies.FirePart::isToRemove);
         int var2 = (int)(this.spawnDelay.getValue() + 1.0);
         if (var2 < 1) {
            var2 = 1;
         }

         if (mc.player.age % var2 == 0) {
            this.fireParts.add(new FireFlies.FirePart(this.generateVecForPart(10.0, 4.0), this.getMaxPartAliveTime()));
            this.fireParts.add(new FireFlies.FirePart(this.generateVecForPart(6.0, 5.0), this.getMaxPartAliveTime()));
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (mc.player != null && mc.world != null) {
         if (!this.fireParts.isEmpty()) {
            float var2 = mc.getRenderTickCounter().getTickDelta(true);
            var1.push();
            Vec3d var3 = mc.gameRenderer.getCamera().getPos();
            var1.translate(-var3.x, -var3.y, -var3.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            for (FireFlies.FirePart var5 : this.fireParts) {
               int var6 = this.getPartColor();
               float var7 = var5.getAlphaPC();
               int var8 = this.withAlpha(var6, (int)(var7 * 255.0F));
               if (this.darkImprint.getValue()) {
                  RenderSystem.blendFuncSeparate(770, 771, 1, 0);
                  this.drawSparkPartsList(var1, var6, var5, var2);
                  this.drawTrailPartsList(var1, var6, var5);
                  RenderSystem.blendFuncSeparate(770, 1, 1, 0);
               } else {
                  this.drawSparkPartsList(var1, var6, var5, var2);
                  this.drawTrailPartsList(var1, var6, var5);
               }

               this.drawBillboard(var1, var5, var2, var8);
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            var1.pop();
         }
      }
   }

   @Override
   public void onEnable() {
      this.fireParts.clear();
   }

   @Override
   public void onDisable() {
      this.fireParts.clear();
   }

   private void drawSparkPartsList(MatrixStack var1, int var2, FireFlies.FirePart var3, float var4) {
      if (var3.sparkParts.size() >= 2) {
         double var5 = mc.player.getPos().distanceTo(var3.getPosVec().add(0.0, 1.6, 0.0));
         float var7 = 1.0F - (float)((var5 - 3.0) / 10.0);
         var7 = MathHelper.clamp(var7, 0.0F, 1.0F);
         float var8 = 1.5F + 6.0F * var7;
         RenderSystem.lineWidth(var8);
         Matrix4f var9 = var1.peek().getPositionMatrix();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var10 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (FireFlies.SparkPart var12 : var3.sparkParts) {
            float var13 = (float)var12.timePC();
            Color var14 = new Color(255, 255, 255, 255);
            Color var15 = new Color(var2, true);
            int var16 = (int)(var14.getRed() + (var15.getRed() - var14.getRed()) * var13);
            int var17 = (int)(var14.getGreen() + (var15.getGreen() - var14.getGreen()) * var13);
            int var18 = (int)(var14.getBlue() + (var15.getBlue() - var14.getBlue()) * var13);
            int var19 = (int)(var14.getAlpha() + (var15.getAlpha() - var14.getAlpha()) * var13);
            float var20 = this.getAlpha(var2) / 255.0F * (1.0F - var13);
            int var21 = this.applyOpacity(var19 << 24 | var16 << 16 | var17 << 8 | var18, var20);
            float var22 = (var21 >> 24 & 0xFF) / 255.0F;
            float var23 = (var21 >> 16 & 0xFF) / 255.0F;
            float var24 = (var21 >> 8 & 0xFF) / 255.0F;
            float var25 = (var21 & 0xFF) / 255.0F;
            float var26 = (float)var12.getRenderPosX(var4);
            float var27 = (float)var12.getRenderPosY(var4);
            float var28 = (float)var12.getRenderPosZ(var4);
            var10.vertex(var9, var26, var27, var28).color(var23, var24, var25, var22);
            var10.vertex(var9, var26, var27 + 0.001F, var28).color(var23, var24, var25, var22);
         }

         BufferRenderer.drawWithGlobalProgram(var10.end());
         RenderSystem.lineWidth(1.0F);
      }
   }

   private void drawTrailPartsList(MatrixStack var1, int var2, FireFlies.FirePart var3) {
      if (var3.trailParts.size() >= 2) {
         double var4 = mc.player.getPos().distanceTo(var3.getPosVec().add(0.0, 1.6, 0.0));
         float var6 = 1.0F - (float)((var4 - 3.0) / 20.0);
         var6 = MathHelper.clamp(var6, 0.0F, 1.0F);
         float var7 = 1.0E-5F + 8.0F * var6;
         RenderSystem.lineWidth(var7);
         int var8 = 0;
         int var9 = var3.trailParts.size();
         Matrix4f var10 = var1.peek().getPositionMatrix();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var11 = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

         for (FireFlies.TrailPart var13 : var3.trailParts) {
            float var14 = (float)var8 / var9;
            var14 = (var14 > 0.5 ? 1.0F - var14 : var14) * 2.0F;
            var14 = MathHelper.clamp(var14, 0.0F, 1.0F);
            float var15 = this.getAlpha(var2) / 255.0F * var14;
            int var16 = this.applyOpacity(var2, var15);
            float var17 = (var16 >> 24 & 0xFF) / 255.0F;
            float var18 = (var16 >> 16 & 0xFF) / 255.0F;
            float var19 = (var16 >> 8 & 0xFF) / 255.0F;
            float var20 = (var16 & 0xFF) / 255.0F;
            var11.vertex(var10, (float)var13.x, (float)var13.y, (float)var13.z).color(var18, var19, var20, var17);
            var8++;
         }

         BufferRenderer.drawWithGlobalProgram(var11.end());
         RenderSystem.lineWidth(1.0F);
      }
   }

   private int applyOpacity(int var1, float var2) {
      var2 = MathHelper.clamp(var2, 0.0F, 1.0F);
      int var3 = (int)((var1 >> 24 & 0xFF) * var2);
      if (var3 < 0) {
         var3 = 0;
      }

      if (var3 > 255) {
         var3 = 255;
      }

      return var3 << 24 | var1 & 16777215;
   }

   private int withAlpha(int var1, int var2) {
      if (var2 < 0) {
         var2 = 0;
      }

      if (var2 > 255) {
         var2 = 255;
      }

      return var2 << 24 | var1 & 16777215;
   }

   private int darker(int var1, float var2) {
      int var3 = (int)((var1 >> 16 & 0xFF) * var2);
      int var4 = (int)((var1 >> 8 & 0xFF) * var2);
      int var5 = (int)((var1 & 0xFF) * var2);
      int var6 = var1 >> 24 & 0xFF;
      return var6 << 24 | var3 << 16 | var4 << 8 | var5;
   }

   private int getAlpha(int var1) {
      return var1 >> 24 & 0xFF;
   }

   private final class FirePart {
      private final List<FireFlies.TrailPart> trailParts = new ArrayList<>();
      private final List<FireFlies.SparkPart> sparkParts = new ArrayList<>();
      private Vec3d prevPos;
      private Vec3d pos;
      private float alphaPC;
      private float alphaTarget = 1.0F;
      private int msChangeSideRate = this.getMsChangeSideRate();
      private float moveYawSet = (float)FireFlies.this.random(0.0, 360.0);
      private float speed = (float)FireFlies.this.random(0.1, 0.25);
      private float yMotion = (float)FireFlies.this.random(-0.075, 0.1);
      private float moveYaw = this.moveYawSet;
      private final float maxAlive;
      private final long startTime;
      private long rateTimer;
      private boolean toRemove;

      public FirePart(Vec3d nullx, long nullxx) {
         this.pos = nullx;
         this.prevPos = nullx;
         this.maxAlive = (float)nullxx;
         this.startTime = System.currentTimeMillis();
         this.rateTimer = this.startTime;
      }

      public float getTimePC() {
         float var1 = FireFlies.this.getAnimationSpeed();
         return MathHelper.clamp((float)(System.currentTimeMillis() - this.startTime) * var1 / this.maxAlive, 0.0F, 1.0F);
      }

      public void setAlphaPCTo(float var1) {
         this.alphaTarget = var1;
      }

      public float getAlphaPC() {
         return this.alphaPC;
      }

      private void updateAlpha() {
         float var1 = 0.05F;
         float var2 = FireFlies.this.getAnimationSpeed();
         this.alphaPC = this.alphaPC + (this.alphaTarget - this.alphaPC) * (var1 * var2);
      }

      public Vec3d getPosVec() {
         return this.pos;
      }

      public Vec3d getRenderPosVec(float var1) {
         Vec3d var2 = this.getPosVec();
         return var2.add(-(this.prevPos.x - var2.x) * var1, -(this.prevPos.y - var2.y) * var1, -(this.prevPos.z - var2.z) * var1);
      }

      public void updatePart() {
         if (System.currentTimeMillis() - this.rateTimer >= this.msChangeSideRate) {
            this.msChangeSideRate = this.getMsChangeSideRate();
            this.rateTimer = System.currentTimeMillis();
            this.moveYawSet = (float)FireFlies.this.random(0.0, 360.0);
         }

         float var1 = 0.065F * FireFlies.this.getAnimationSpeed();
         this.moveYaw = this.moveYaw + (this.moveYawSet - this.moveYaw) * var1;
         float var2 = -((float)Math.sin(Math.toRadians(this.moveYaw))) * (this.speed /= 1.005F);
         float var3 = (float)Math.cos(Math.toRadians(this.moveYaw)) * this.speed;
         this.prevPos = this.pos;
         float var4 = 0.1F;
         Box var5 = new Box(
            this.pos.x - var4 / 2.0F, this.pos.y, this.pos.z - var4 / 2.0F, this.pos.x + var4 / 2.0F, this.pos.y + var4, this.pos.z + var4 / 2.0F
         );
         boolean var6 = StreamSupport.stream(Wrapper.mc.world.getBlockCollisions(Wrapper.mc.player, var5).spliterator(), false).findAny().isPresent();
         float var7 = var6 ? 0.3F : 1.0F;
         float var8 = FireFlies.this.getAnimationSpeed();
         this.pos = this.pos.add(var2 * var8 / var7, (this.yMotion /= 1.02F) * var8 / var7, var3 * var8 / var7);
         float var9 = this.getTimePC();
         if (var9 >= 1.0F) {
            this.setAlphaPCTo(0.0F);
         }

         this.updateAlpha();
         if (var9 >= 1.0F && this.alphaPC < 0.003921569F) {
            this.setToRemove();
         }

         this.trailParts.add(FireFlies.this.new TrailPart(this, 400L));
         if (!this.trailParts.isEmpty()) {
            this.trailParts.removeIf(FireFlies.TrailPart::toRemove);
         }

         for (int var10 = 0; var10 < 2; var10++) {
            this.sparkParts.add(FireFlies.this.new SparkPart(this, 300L));
         }

         this.sparkParts.forEach(FireFlies.SparkPart::motionSparkProcess);
         if (!this.sparkParts.isEmpty()) {
            this.sparkParts.removeIf(FireFlies.SparkPart::toRemove);
         }
      }

      public void setToRemove() {
         this.toRemove = true;
      }

      public boolean isToRemove() {
         return this.toRemove;
      }

      int getMsChangeSideRate() {
         float var1 = FireFlies.this.getAnimationSpeed();
         return (int)(FireFlies.this.random(300.5, 900.5) / var1);
      }
   }

   private final class SparkPart {
      private double posX;
      private double posY;
      private double posZ;
      private double prevPosX;
      private double prevPosY;
      private double prevPosZ;
      private final double speed = Math.random() / 30.0;
      private final double radianYaw = Math.random() * 360.0;
      private final double radianPitch = -90.0 + Math.random() * 180.0;
      private final long time;
      private final long maxTime;

      public SparkPart(FireFlies.FirePart nullx, long nullxx) {
         this.posX = nullx.pos.x;
         this.posY = nullx.pos.y;
         this.posZ = nullx.pos.z;
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         this.maxTime = nullxx;
         this.time = System.currentTimeMillis();
      }

      public boolean toRemove() {
         return System.currentTimeMillis() - this.time >= this.maxTime;
      }

      public double timePC() {
         return MathHelper.clamp((double)(System.currentTimeMillis() - this.time) / this.maxTime, 0.0, 1.0);
      }

      public void motionSparkProcess() {
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         double var1 = Math.toRadians(this.radianYaw);
         this.posX = this.posX + Math.sin(var1) * this.speed;
         this.posY = this.posY + Math.cos(Math.toRadians(this.radianPitch - 90.0)) * this.speed;
         this.posZ = this.posZ + Math.cos(var1) * this.speed;
      }

      public double getRenderPosX(float var1) {
         return this.prevPosX + (this.posX - this.prevPosX) * var1;
      }

      public double getRenderPosY(float var1) {
         return this.prevPosY + (this.posY - this.prevPosY) * var1;
      }

      public double getRenderPosZ(float var1) {
         return this.prevPosZ + (this.posZ - this.prevPosZ) * var1;
      }
   }

   private final class TrailPart {
      private final double x;
      private final double y;
      private final double z;
      private final long time;
      private final long maxTime;

      public TrailPart(FireFlies.FirePart nullx, long nullxx) {
         this.x = nullx.pos.x;
         this.y = nullx.pos.y;
         this.z = nullx.pos.z;
         this.time = System.currentTimeMillis();
         this.maxTime = nullxx;
      }

      public boolean toRemove() {
         return System.currentTimeMillis() - this.time >= this.maxTime;
      }
   }
}
