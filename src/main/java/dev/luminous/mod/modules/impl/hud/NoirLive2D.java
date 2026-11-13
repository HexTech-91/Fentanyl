package dev.luminous.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.live2d.CubismNativeModel;
import dev.luminous.api.live2d.CubismNativeRenderer;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class NoirLive2D extends Module {
   public static NoirLive2D INSTANCE;
   private static final Identifier PREVIEW_TEXTURE = Identifier.of("forever", "live2d/noir/preview.png");
   private static final Identifier MODEL_JSON = Identifier.of("forever", "live2d/noir/noir.model3.json");
   private static final Identifier MOC_FILE = Identifier.of("forever", "live2d/noir/noir.moc3");
   private static final Identifier MODEL_TEXTURE = Identifier.of("forever", "live2d/noir/noir.2048/texture_00.png");
   private static final float PREVIEW_ASPECT = 0.99666667F;
   private static final Set<String> HIDDEN_HAIR_DRAWABLES = Set.of();
   private static final Set<String> UNSUPPORTED_PARAMETERS = Set.of("ParamBodyAngleX");
   private final SliderSetting size = this.add(new SliderSetting("Height", 220.0, 80.0, 600.0, 1.0));
   private final SliderSetting swayStrength = this.add(new SliderSetting("Sway", 3.0, 0.0, 3.0, 0.05));
   private final SliderSetting smoothing = this.add(new SliderSetting("Smoothing", 0.1, 0.02, 0.6, 0.01));
   private final BooleanSetting followPitch = this.add(new BooleanSetting("FollowPitch", true));
   private final EnumSetting<NoirLive2D.FormMode> form = this.add(new EnumSetting<>("Form", NoirLive2D.FormMode.Normal));
   private final BooleanSetting deathAni = this.add(new BooleanSetting("DealthAni", true));
   private final SliderSetting deathKeepTime = this.add(new SliderSetting("DealthKeepTime", 2.5, 0.1, 10.0, 0.1, this.deathAni::getValue).setSuffix("s"));
   private final BooleanSetting popAni = this.add(new BooleanSetting("PopAni", true));
   private final SliderSetting popKeepTime = this.add(new SliderSetting("PopKeepTime", 1.5, 0.1, 10.0, 0.1, this.popAni::getValue).setSuffix("s"));
   private final BooleanSetting selfPopAni = this.add(new BooleanSetting("SelfPopAni", true));
   private final SliderSetting selfPopKeepTime = this.add(new SliderSetting("SelfPopKeepTime", 1.5, 0.1, 10.0, 0.1, this.selfPopAni::getValue).setSuffix("s"));
   private final BooleanSetting hurtAni = this.add(new BooleanSetting("HurtAni", true));
   private final SliderSetting hurtKeepTime = this.add(new SliderSetting("HurtKeepTime", 0.6, 0.1, 5.0, 0.05, this.hurtAni::getValue).setSuffix("s"));
   private final BooleanSetting clickAni = this.add(new BooleanSetting("ClickAni", true));
   private final BooleanSetting showRuntimeHint = this.add(new BooleanSetting("RuntimeHint", true));
   public final SliderSetting x = this.add(new SliderSetting("X", 20, 0, 1920));
   public final SliderSetting y = this.add(new SliderSetting("Y", 140, 0, 1080));
   private boolean firstFrame = true;
   private float lastYaw;
   private float lastPitch;
   private float angleX;
   private float angleY;
   private float angleZ;
   private float bodyAngleX;
   private float springX;
   private float springVelocityX;
   private float springY;
   private float springVelocityY;
   private float earSpring;
   private float earVelocity;
   private long nextBlinkAt = System.currentTimeMillis() + 1800L;
   private long blinkStartedAt = -1L;
   private long deathExpressionUntil;
   private long popExpressionUntil;
   private long selfPopExpressionUntil;
   private long hurtExpressionUntil;
   private long clickExpressionUntil;
   private float lastHealth = -1.0F;
   private long lastHurtAt;
   private float hurtWeight;
   private float winceWeight;
   private float clickWeight;
   private float tearsWeight;
   private float dizzyWeight;
   private float eyeCloseWeight;
   private float formWeight;
   private long lastExpressionUpdateMs = System.currentTimeMillis();
   private CubismNativeModel nativeModel;
   private CubismNativeRenderer nativeRenderer;
   private boolean nativeLoadAttempted;
   private String nativeStatus = "not loaded";

   public NoirLive2D() {
      super("NoirLive2D", "Live2D desktop character rendered on the HUD", Module.Category.HUD);
      this.setChinese("NoirLive2D");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.firstFrame = true;
      this.lastExpressionUpdateMs = System.currentTimeMillis();
      this.loadNativeRuntime();
   }

   @Override
   public void onDisable() {
      if (this.nativeModel != null) {
         this.nativeModel.close();
         this.nativeModel = null;
      }

      this.nativeRenderer = null;
      this.nativeLoadAttempted = false;
      this.clearExpressionTimers();
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (mc.player != null && mc.world != null) {
         this.loadNativeRuntime();
         this.updateTracking(var2);
         this.updateHurtState();
         int var3 = this.x.getValueInt();
         int var4 = this.y.getValueInt();
         int var5 = this.getElementWidth();
         int var6 = this.getElementHeight();
         float var7 = var3 + this.springX * 0.55F;
         float var8 = var4 + this.springY * 0.3F + this.idleBob();
         float var9 = this.angleZ * 0.22F;
         float var10 = 1.0F + Math.abs(this.springX) * 9.0E-4F;
         float var11 = 1.0F - Math.abs(this.springX) * 4.5E-4F;
         MatrixStack var12 = var1.getMatrices();
         var12.push();
         if (this.nativeModel != null && this.nativeRenderer != null) {
            var12.translate(var3, var4, 0.0F);

            try {
               this.renderNative(var1, var5, var6);
            } catch (Throwable var14) {
               this.disableNativeRuntime(var14);
               var12.translate(-var3, -var4, 0.0F);
               var12.translate(var7 + var5 / 2.0F, var8 + var6 / 2.0F, 0.0F);
               var12.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var9));
               var12.scale(var10, var11, 1.0F);
               var12.translate(-var5 / 2.0F, -var6 / 2.0F, 0.0F);
               this.renderPreview(var1, var5, var6);
            }
         } else {
            var12.translate(var7 + var5 / 2.0F, var8 + var6 / 2.0F, 0.0F);
            var12.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var9));
            var12.scale(var10, var11, 1.0F);
            var12.translate(-var5 / 2.0F, -var6 / 2.0F, 0.0F);
            this.renderPreview(var1, var5, var6);
         }

         var12.pop();
         if (this.showRuntimeHint.getValue() && this.nativeModel == null) {
            this.renderRuntimeHint(var1, var3, var4, var5, var6);
         }
      }
   }

   private void renderNative(DrawContext var1, int var2, int var3) {
      this.applyIdleAnimation();
      this.applyExpressions();
      this.setParam("ParamAngleX", this.angleX);
      this.setParam("ParamAngleY", this.angleY);
      this.setParam("ParamAngleZ", this.angleZ);
      this.setParam("ParamBodyAngleX", this.bodyAngleX);
      this.nativeModel.update();
      this.nativeRenderer.setFlipV(true);
      this.nativeRenderer.setSkipMaskedDrawables(false);
      this.nativeRenderer.setFrontBangsFix(false);
      this.nativeRenderer.setFrontBangDrawables(Set.of());
      this.nativeRenderer.setHiddenDrawables(HIDDEN_HAIR_DRAWABLES);
      this.nativeRenderer.render(var1, this.nativeModel, 0.0F, 0.0F, var2, var3, 1.0F);
   }

   private void setParam(String var1, float var2) {
      if (this.nativeModel != null && !UNSUPPORTED_PARAMETERS.contains(var1)) {
         this.nativeModel.setParameter(var1, var2);
      }
   }

   private void applyIdleAnimation() {
      long var1 = System.currentTimeMillis();
      double var3 = var1 / 1000.0;
      float var5 = 1.0F;
      if (this.blinkStartedAt < 0L && var1 >= this.nextBlinkAt) {
         this.blinkStartedAt = var1;
      }

      if (this.blinkStartedAt >= 0L) {
         float var6 = MathHelper.clamp((float)(var1 - this.blinkStartedAt) / 180.0F, 0.0F, 1.0F);
         var5 = var6 < 0.5F ? 1.0F - var6 * 2.0F : (var6 - 0.5F) * 2.0F;
         if (var6 >= 1.0F) {
            this.blinkStartedAt = -1L;
            this.nextBlinkAt = var1 + 2200L + (long)(Math.sin(var3 * 1.7) * 500.0 + 650.0);
         }
      }

      float var14 = (float)((Math.sin(var3 * 2.1) + 1.0) * 0.5);
      this.earVelocity = this.earVelocity + (this.angleZ * 0.55F + this.angleX * 0.18F - this.earSpring) * 0.16F;
      this.earVelocity *= 0.74F;
      this.earSpring = this.earSpring + this.earVelocity;
      float var7 = MathHelper.clamp(this.earSpring, -12.0F, 12.0F);
      float var8 = this.angleX * 0.09F + (float)Math.sin(var3 * 2.8) * 1.2F;
      float var9 = this.angleY * 0.08F + (float)Math.sin(var3 * 2.1 + 1.0) * 0.8F;
      this.updateWince(var1);
      float var10 = MathHelper.clamp(var5 * (1.0F - this.winceWeight * 0.55F), 0.0F, 1.0F);
      float var11 = MathHelper.clamp(var5 * (1.0F - this.winceWeight * 0.88F), 0.0F, 1.0F);
      this.setParam("ParamEyeLOpen", var10);
      this.setParam("ParamEyeROpen", var11);
      this.setParam("ParamEyeLSmile", this.winceWeight * 0.7F);
      this.setParam("ParamEyeRSmile", this.winceWeight * 0.15F);
      this.setParam("ParamBreath", var14);
      this.setParam("ParamEyeBallX", MathHelper.clamp(this.angleX / 30.0F, -1.0F, 1.0F));
      this.setParam("ParamEyeBallY", MathHelper.clamp(this.angleY / 20.0F, -1.0F, 1.0F));
      this.setParam("Param10", var7);
      this.setParam("Param14", -var7);
      this.setParam("Param11", var7 * 0.35F);
      this.setParam("Param12", var7 * 0.2F);
      this.setParam("Param15", -var7 * 0.35F);
      this.setParam("Param16", -var7 * 0.2F);

      for (String var13 : new String[]{
         "Param20", "Param21", "Param24", "Param25", "Param26", "Param27", "Param28", "Param29", "Param30", "Param31", "Param32", "Param33"
      }) {
         this.setParam(var13, var8);
      }

      for (String var18 : new String[]{"Param22", "Param23", "Param34", "Param35", "Param36", "Param37"}) {
         this.setParam(var18, var9);
      }
   }

   private void applyExpressions() {
      long var1 = System.currentTimeMillis();
      float var3 = Math.min(0.1F, Math.max(0.0F, (float)(var1 - this.lastExpressionUpdateMs) / 1000.0F));
      this.lastExpressionUpdateMs = var1;
      boolean var4 = this.deathAni.getValue() && var1 < this.deathExpressionUntil;
      boolean var5 = this.selfPopAni.getValue() && var1 < this.selfPopExpressionUntil;
      boolean var6 = this.popAni.getValue() && var1 < this.popExpressionUntil;
      boolean var20 = this.hurtAni.getValue() && var1 < this.hurtExpressionUntil;
      boolean var21 = this.clickAni.getValue() && var1 < this.clickExpressionUntil;
      if (var4) {
         var5 = false;
         var6 = false;
         var20 = false;
      } else if (var5) {
         var6 = false;
         var20 = false;
      } else if (var6) {
         var20 = false;
      }

      this.tearsWeight = this.approachExpressionWeight(this.tearsWeight, var4 ? 1.0F : 0.0F, var3, 0.12F, 0.2F);
      this.dizzyWeight = this.approachExpressionWeight(this.dizzyWeight, var5 ? 1.0F : 0.0F, var3, 0.12F, 0.2F);
      this.eyeCloseWeight = this.approachExpressionWeight(this.eyeCloseWeight, var6 ? 1.0F : 0.0F, var3, 0.12F, 0.2F);
      this.hurtWeight = this.approachExpressionWeight(this.hurtWeight, var20 ? 1.0F : 0.0F, var3, 0.06F, 0.18F);
      this.clickWeight = this.approachExpressionWeight(this.clickWeight, var21 ? 1.0F : 0.0F, var3, 0.1F, 0.25F);
      this.formWeight = this.approachExpressionWeight(this.formWeight, this.form.getValue().target, var3, 0.1F, 0.2F);
      this.setParam("Param17", this.tearsWeight);
      this.setParam("ParamCheek", Math.max(this.tearsWeight, this.hurtWeight * 0.8F));
      this.setParam("Param39", this.dizzyWeight);
      this.setParam("Param40", -this.eyeCloseWeight);
      this.setParam("Param41", this.formWeight);
      this.setParam("Param20", this.hurtWeight * 10.0F);
      this.setParam("Param21", -this.hurtWeight * 10.0F);

      if (this.clickWeight > 0.01F) {
         this.setParam("ParamMouthOpenY", this.clickWeight * 0.8F);
         this.setParam("ParamMouthForm", 1.0F - this.clickWeight * 1.4F);
      }
   }

   private void updateWince(long var1) {
      long var3 = this.hurtAni.getValue() ? this.hurtExpressionUntil : 0L;
      boolean var5 = var1 < var3;
      if (!var5) {
         this.winceWeight = this.lerp(this.winceWeight, 0.0F, 0.18F);
         return;
      }

      float var6 = MathHelper.clamp((float)(var3 - var1) / 260.0F, 0.0F, 1.0F);
      float var7 = 1.0F - var6;
      float var8 = 0.75F + 0.25F * (float)Math.sin(var1 / 42.0);
      this.winceWeight = MathHelper.clamp(var7 * var8, 0.0F, 1.0F);
   }

   private float approachExpressionWeight(float var1, float var2, float var3, float var4, float var5) {
      float var6 = var2 > var1 ? var4 : var5;
      return var6 <= 0.0F ? var2 : this.lerp(var1, var2, MathHelper.clamp(var3 / var6, 0.0F, 1.0F));
   }

   @EventListener
   public void onDeath(DeathEvent var1) {
      if (this.isSelf(var1.getPlayer()) && this.deathAni.getValue()) {
         this.deathExpressionUntil = this.keepUntil(this.deathKeepTime);
      }
   }

   @EventListener
   public void onTotemPop(TotemEvent var1) {
      if (this.isSelf(var1.getPlayer())) {
         if (this.selfPopAni.getValue()) {
            this.selfPopExpressionUntil = this.keepUntil(this.selfPopKeepTime);
         }
      } else if (this.popAni.getValue()) {
         this.popExpressionUntil = this.keepUntil(this.popKeepTime);
      }
   }

   private void updateHurtState() {
      if (mc.player == null) {
         this.lastHealth = -1.0F;
         return;
      }

      float var1 = mc.player.getHealth();
      if (this.lastHealth < 0.0F) {
         this.lastHealth = var1;
         return;
      }

      long var2 = System.currentTimeMillis();
      if (var1 < this.lastHealth - 0.01F && var2 - this.lastHurtAt > 120L && var1 > 0.0F) {
         this.lastHurtAt = var2;
         if (this.hurtAni.getValue()) {
            this.hurtExpressionUntil = this.keepUntil(this.hurtKeepTime);
         }
      }

      this.lastHealth = var1;
   }

   public void reactToClick(double var1, double var3, int var5, int var6) {
      if (!this.clickAni.getValue()) {
         return;
      }

      this.clickExpressionUntil = System.currentTimeMillis() + 1200L;
      float var7 = (float)(var1 / Math.max(1, var5) - 0.5);
      this.angleX = MathHelper.clamp(this.angleX + var7 * 14.0F, -30.0F, 30.0F);
      this.springX += var7 * 9.0F;
      this.springY -= 3.0F;
   }

   private long keepUntil(SliderSetting var1) {
      return System.currentTimeMillis() + Math.round(var1.getValue() * 1000.0);
   }

   private boolean isSelf(PlayerEntity var1) {
      return var1 != null && mc.player != null
         ? var1.getUuid().equals(mc.player.getUuid()) || var1.getName().getString().equalsIgnoreCase(mc.player.getName().getString())
         : false;
   }

   private void clearExpressionTimers() {
      this.deathExpressionUntil = 0L;
      this.popExpressionUntil = 0L;
      this.selfPopExpressionUntil = 0L;
      this.hurtExpressionUntil = 0L;
      this.clickExpressionUntil = 0L;
      this.lastHealth = -1.0F;
      this.hurtWeight = 0.0F;
      this.winceWeight = 0.0F;
      this.clickWeight = 0.0F;
   }

   private void renderPreview(DrawContext var1, int var2, int var3) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      var1.drawTexture(PREVIEW_TEXTURE, 0, 0, 0.0F, 0.0F, var2, var3, var2, var3);
      RenderSystem.disableBlend();
   }

   private void loadNativeRuntime() {
      if (!this.nativeLoadAttempted && this.nativeModel == null && mc != null && mc.getResourceManager() != null) {
         this.nativeLoadAttempted = true;

         try {
            this.nativeModel = CubismNativeModel.load(mc, MOC_FILE, mc.runDirectory.toPath());
            this.nativeRenderer = new CubismNativeRenderer(new Identifier[]{MODEL_TEXTURE});
            this.nativeRenderer.preloadTextures(mc);
            this.nativeStatus = "native cubism loaded";
         } catch (Throwable var2) {
            this.nativeStatus = var2.getClass().getSimpleName() + ": " + var2.getMessage();
            this.nativeModel = null;
            this.nativeRenderer = null;
            System.err.println("[NoirLive2D] Cubism native runtime unavailable: " + this.nativeStatus);
         }
      }
   }

   private void disableNativeRuntime(Throwable var1) {
      this.nativeStatus = var1.getClass().getSimpleName() + ": " + var1.getMessage();
      if (this.nativeModel != null) {
         this.nativeModel.close();
      }

      this.nativeModel = null;
      this.nativeRenderer = null;
      this.nativeLoadAttempted = true;
      System.err.println("[NoirLive2D] Cubism native renderer failed: " + this.nativeStatus);
   }

   private void updateTracking(float var1) {
      float var2 = mc.player.getYaw();
      float var3 = mc.player.getPitch();
      if (this.firstFrame) {
         this.lastYaw = var2;
         this.lastPitch = var3;
         this.firstFrame = false;
      } else {
         float var4 = Math.max(0.01F, var1 / 20.0F);
         float var5 = MathHelper.wrapDegrees(var2 - this.lastYaw);
         float var6 = var3 - this.lastPitch;
         this.lastYaw = var2;
         this.lastPitch = var3;
         float var7 = this.swayStrength.getValueFloat();
         float var8 = MathHelper.clamp(-var5 / var4 * 0.018F * var7, -30.0F, 30.0F);
         float var9 = this.followPitch.getValue() ? MathHelper.clamp(-var6 / var4 * 0.014F * var7, -20.0F, 20.0F) : 0.0F;
         float var10 = MathHelper.clamp(var5 / var4 * 0.01F * var7, -18.0F, 18.0F);
         float var11 = this.smoothing.getValueFloat();
         this.angleX = this.lerp(this.angleX, var8, var11);
         this.angleY = this.lerp(this.angleY, var9, var11);
         this.angleZ = this.lerp(this.angleZ, var10, var11 * 0.75F);
         this.bodyAngleX = this.lerp(this.bodyAngleX, this.angleX * 0.35F, var11 * 0.55F);
         this.springX = this.updateSpring(this.springX, this.angleX + this.bodyAngleX, true);
         this.springY = this.updateSpring(this.springY, this.angleY, false);
      }
   }

   private float updateSpring(float var1, float var2, boolean var3) {
      float var4 = var3 ? 0.12F : 0.08F;
      float var5 = var3 ? 0.78F : 0.82F;
      if (var3) {
         this.springVelocityX += (var2 - var1) * var4;
         this.springVelocityX *= var5;
         return var1 + this.springVelocityX;
      } else {
         this.springVelocityY += (var2 - var1) * var4;
         this.springVelocityY *= var5;
         return var1 + this.springVelocityY;
      }
   }

   private void renderRuntimeHint(DrawContext var1, int var2, int var3, int var4, int var5) {
      byte var6 = 28;
      int var7 = var3 + var5 - var6 - 3;
      Color var8 = new Color(12, 12, 14, 125);
      Color var9 = new Color(245, 245, 245, 210);
      Render2DUtil.drawRoundedRect(var1.getMatrices(), var2 + 4, var7, var4 - 8, var6, 4.0F, var8);
      TextUtil.drawString(var1, "Cubism native fallback", var2 + 10, var7 + 5, var9.getRGB(), false);
      TextUtil.drawString(var1, this.nativeStatus, var2 + 10, var7 + 16, new Color(210, 210, 210, 170).getRGB(), false);
   }

   private float idleBob() {
      return (float)Math.sin(System.currentTimeMillis() / 650.0) * 1.5F;
   }

   private float lerp(float var1, float var2, float var3) {
      return var1 + (var2 - var1) * MathHelper.clamp(var3, 0.0F, 1.0F);
   }

   public int getElementWidth() {
      return Math.max(1, Math.round(this.getElementHeight() * 0.99666667F));
   }

   public int getElementHeight() {
      return this.size.getValueInt();
   }

   public float getParamAngleX() {
      return this.angleX;
   }

   public float getParamAngleY() {
      return this.angleY;
   }

   public float getParamAngleZ() {
      return this.angleZ;
   }

   public float getParamBodyAngleX() {
      return this.bodyAngleX;
   }

   private static enum FormMode {
      Normal(0.0F),
      White(1.0F);

      private final float target;

      private FormMode(float nullxx) {
         this.target = nullxx;
      }
   }
}
