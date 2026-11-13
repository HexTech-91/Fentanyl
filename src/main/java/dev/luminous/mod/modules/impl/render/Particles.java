package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.PerformanceLimiter;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Particles extends Module {
   private final BooleanSetting fireFlies = this.add(new BooleanSetting("FireFlies", true).setParent());
   private final SliderSetting ffCount = this.add(new SliderSetting("FFCount", 30, 20, 200, this.fireFlies::isOpen));
   private final SliderSetting ffSize = this.add(new SliderSetting("FFSize", 1.0, 0.1, 2.0, 0.05, this.fireFlies::isOpen));
   private final EnumSetting<Particles.Mode> mode = this.add(new EnumSetting<>("Mode", Particles.Mode.SnowFlake));
   private final SliderSetting count = this.add(new SliderSetting("Count", 100, 20, 800, () -> this.mode.getValue() != Particles.Mode.Off));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.1, 6.0, 0.1, () -> this.mode.getValue() != Particles.Mode.Off));
   private final EnumSetting<Particles.ColorMode> colorMode = this.add(
      new EnumSetting<>("ColorMode", Particles.ColorMode.Sync, () -> this.mode.getValue() != Particles.Mode.Off)
   );
   private final ColorSetting color = this.add(
      new ColorSetting("Color", new Color(3649978), () -> this.mode.getValue() != Particles.Mode.Off && this.colorMode.getValue() == Particles.ColorMode.Custom)
   );
   private final EnumSetting<Particles.Physics> physics = this.add(
      new EnumSetting<>("Physics", Particles.Physics.Fly, () -> this.mode.getValue() != Particles.Mode.Off)
   );
   private final ArrayList<Particles.ParticleBase> fireFlyParticles = new ArrayList<>();
   private final ArrayList<Particles.ParticleBase> particles = new ArrayList<>();
   private final Random random = new Random();
   private final Identifier textureFirefly = Identifier.of("alien", "textures/particles/firefly.png");
   private final Identifier textureSnowflake = Identifier.of("alien", "textures/particles/snowflake.png");
   private final Identifier textureStar = Identifier.of("alien", "textures/particles/star.png");
   private final Identifier textureHeart = Identifier.of("alien", "textures/particles/heart.png");
   private final Identifier textureDollar = Identifier.of("alien", "textures/particles/dollar.png");

   public Particles() {
      super("Particles", Module.Category.Render);
      this.setChinese("\u7c92\u5b50");
   }

   @Override
   public void onEnable() {
      this.fireFlyParticles.clear();
      this.particles.clear();
   }

   @Override
   public void onDisable() {
      this.fireFlyParticles.clear();
      this.particles.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         this.fireFlyParticles.removeIf(Particles.ParticleBase::tick);
         this.particles.removeIf(Particles.ParticleBase::tick);
         this.spawnFireflies();
         this.spawnParticles();
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (!nullCheck()) {
         float var2 = mc.getRenderTickCounter().getTickDelta(true);
         Vec3d var3 = mc.gameRenderer.getCamera().getPos();
         if (this.fireFlies.getValue() && !this.fireFlyParticles.isEmpty()) {
            var1.push();
            var1.translate(-var3.x, -var3.y, -var3.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, this.textureFirefly);
            BufferBuilder var4 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Particles.ParticleBase var6 : this.fireFlyParticles) {
               var6.render(var1, var4, var2, (float)this.ffSize.getValue(), this.getColor(var6.age * 10), true);
            }

            Render3DUtil.endBuilding(var4);
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            var1.pop();
         }

         if (this.mode.getValue() != Particles.Mode.Off && !this.particles.isEmpty()) {
            Identifier var8 = this.getModeTexture();
            if (var8 == null) {
               return;
            }

            var1.push();
            var1.translate(-var3.x, -var3.y, -var3.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 1, 0);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, var8);
            BufferBuilder var9 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Particles.ParticleBase var7 : this.particles) {
               var7.render(var1, var9, var2, (float)this.size.getValue(), this.getColor(var7.age * 2), false);
            }

            Render3DUtil.endBuilding(var9);
            RenderSystem.depthMask(true);
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            var1.pop();
         }
      }
   }

   private void spawnFireflies() {
      if (this.fireFlies.getValue()) {
         int var1 = PerformanceLimiter.clampFireflies(this.ffCount.getValueInt());

         for (int var2 = this.fireFlyParticles.size(); var2 < var1; var2++) {
            this.fireFlyParticles
               .add(
                  new Particles.FireFly(
                     (float)(mc.player.getX() + this.random(-25.0F, 25.0F)),
                     (float)(mc.player.getY() + this.random(2.0F, 15.0F)),
                     (float)(mc.player.getZ() + this.random(-25.0F, 25.0F)),
                     this.random(-0.2F, 0.2F),
                     this.random(-0.1F, 0.1F),
                     this.random(-0.2F, 0.2F)
                  )
               );
         }
      }
   }

   private void spawnParticles() {
      if (this.mode.getValue() != Particles.Mode.Off) {
         int var1 = PerformanceLimiter.clampParticles(this.count.getValueInt());
         boolean var2 = this.physics.getValue() == Particles.Physics.Drop;

         for (int var3 = this.particles.size(); var3 < var1; var3++) {
            this.particles
               .add(
                  new Particles.ParticleBase(
                     (float)(mc.player.getX() + this.random(-48.0F, 48.0F)),
                     (float)(mc.player.getY() + this.random(2.0F, 48.0F)),
                     (float)(mc.player.getZ() + this.random(-48.0F, 48.0F)),
                     var2 ? 0.0F : this.random(-0.4F, 0.4F),
                     var2 ? this.random(-0.2F, -0.05F) : this.random(-0.1F, 0.1F),
                     var2 ? 0.0F : this.random(-0.4F, 0.4F)
                  )
               );
         }
      }
   }

   private Identifier getModeTexture() {
      return switch ((Particles.Mode)this.mode.getValue()) {
         case Off -> null;
         case SnowFlake -> this.textureSnowflake;
         case Stars -> this.textureStar;
         case Hearts -> this.textureHeart;
         case Dollars -> this.textureDollar;
         case Bloom -> this.textureFirefly;
      };
   }

   private int getColor(int var1) {
      if (this.colorMode.getValue() == Particles.ColorMode.Sync) {
         return HUD.INSTANCE != null ? HUD.INSTANCE.getColor(var1) : -1;
      } else {
         return this.color.getValue().getRGB();
      }
   }

   private float random(float var1, float var2) {
      return var1 + (var2 - var1) * this.random.nextFloat();
   }

   public static enum ColorMode {
      Custom,
      Sync;
   }

   public class FireFly extends Particles.ParticleBase {
      private final List<Particles.TrailPoint> trail = new ArrayList<>();

      public FireFly(float nullx, float nullxx, float nullxxx, float nullxxxx, float nullxxxxx, float nullxxxxxx) {
         super(nullx, nullxx, nullxxx, nullxxxx, nullxxxxx, nullxxxxxx);
      }

      @Override
      public boolean tick() {
         if (Wrapper.mc.player.squaredDistanceTo(this.posX, this.posY, this.posZ) > 100.0) {
            this.age -= 4;
         } else if (!Wrapper.mc.world.getBlockState(new BlockPos((int)this.posX, (int)this.posY, (int)this.posZ)).isAir()) {
            this.age -= 8;
         } else {
            this.age--;
         }

         if (this.age < 0) {
            return true;
         } else {
            this.prevposX = this.posX;
            this.prevposY = this.posY;
            this.prevposZ = this.posZ;
            this.posX = this.posX + this.motionX;
            this.posY = this.posY + this.motionY;
            this.posZ = this.posZ + this.motionZ;
            this.trail.add(new Particles.TrailPoint(new Vec3d(this.posX, this.posY, this.posZ), this.age));
            if (this.trail.size() > 20) {
               this.trail.remove(0);
            }

            this.motionX *= 0.99F;
            this.motionY *= 0.99F;
            this.motionZ *= 0.99F;
            return false;
         }
      }

      @Override
      public void render(MatrixStack var1, BufferBuilder var2, float var3, float var4, int var5, boolean var6) {
         int var7 = this.trail.size();
         if (var7 > 1) {
            for (int var8 = 0; var8 < var7; var8++) {
               Particles.TrailPoint var9 = this.trail.get(var8);
               float var10 = (float)var8 / var7;
               int var11 = (int)(255.0F * ((float)this.age / this.maxAge) * var10);
               super.renderAt(var1, var2, var9.pos, var4, ColorUtil.injectAlpha(var5, var11), true);
            }
         }

         super.render(var1, var2, var3, var4, var5, true);
      }
   }

   public static enum Mode {
      Off,
      SnowFlake,
      Stars,
      Hearts,
      Dollars,
      Bloom;
   }

   public class ParticleBase {
      protected float prevposX;
      protected float prevposY;
      protected float prevposZ;
      protected float posX;
      protected float posY;
      protected float posZ;
      protected float motionX;
      protected float motionY;
      protected float motionZ;
      protected int age;
      protected int maxAge;

      public ParticleBase(float nullx, float nullxx, float nullxxx, float nullxxxx, float nullxxxxx, float nullxxxxxx) {
         this.posX = nullx;
         this.posY = nullxx;
         this.posZ = nullxxx;
         this.prevposX = nullx;
         this.prevposY = nullxx;
         this.prevposZ = nullxxx;
         this.motionX = nullxxxx;
         this.motionY = nullxxxxx;
         this.motionZ = nullxxxxxx;
         this.age = (int)Particles.this.random(100.0F, 300.0F);
         this.maxAge = this.age;
      }

      public boolean tick() {
         if (Wrapper.mc.player.squaredDistanceTo(this.posX, this.posY, this.posZ) > 4096.0) {
            this.age -= 8;
         } else {
            this.age--;
         }

         if (this.age < 0) {
            return true;
         } else {
            this.prevposX = this.posX;
            this.prevposY = this.posY;
            this.prevposZ = this.posZ;
            this.posX = this.posX + this.motionX;
            this.posY = this.posY + this.motionY;
            this.posZ = this.posZ + this.motionZ;
            this.motionX *= 0.9F;
            if (Particles.this.physics.getValue() == Particles.Physics.Fly) {
               this.motionY *= 0.9F;
            }

            this.motionZ *= 0.9F;
            this.motionY -= 0.001F;
            return false;
         }
      }

      public void render(MatrixStack var1, BufferBuilder var2, float var3, float var4, int var5, boolean var6) {
         Vec3d var7 = new Vec3d(
            MathUtil.interpolate(this.prevposX, this.posX, var3),
            MathUtil.interpolate(this.prevposY, this.posY, var3),
            MathUtil.interpolate(this.prevposZ, this.posZ, var3)
         );
         this.renderAt(var1, var2, var7, var4, ColorUtil.injectAlpha(var5, (int)(255.0F * ((float)this.age / this.maxAge))), var6);
      }

      protected void renderAt(MatrixStack var1, BufferBuilder var2, Vec3d var3, float var4, int var5, boolean var6) {
         var1.push();
         var1.translate(var3.x, var3.y, var3.z);
         float var7 = Wrapper.mc.gameRenderer.getCamera().getYaw();
         float var8 = Wrapper.mc.gameRenderer.getCamera().getPitch();
         boolean var9 = Wrapper.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT;
         var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-var7));
         var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var8 * (var9 ? -1.0F : 1.0F)));
         Matrix4f var10 = var1.peek().getPositionMatrix();
         var2.vertex(var10, 0.0F, -var4, 0.0F).texture(0.0F, 1.0F).color(var5);
         var2.vertex(var10, -var4, -var4, 0.0F).texture(1.0F, 1.0F).color(var5);
         var2.vertex(var10, -var4, 0.0F, 0.0F).texture(1.0F, 0.0F).color(var5);
         var2.vertex(var10, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(var5);
         var1.pop();
      }
   }

   public static enum Physics {
      Drop,
      Fly;
   }

   private record TrailPoint(Vec3d pos, int age) {
   }
}
