package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.skia.Skia;
import dev.luminous.api.utils.skia.context.SkiaContext;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import io.github.humbleui.skija.Font;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EnemyInfo extends Module implements Wrapper {
   public static EnemyInfo INSTANCE;
   public final BooleanSetting showHealthBar = this.add(new BooleanSetting("HealthBar", true));
   public final BooleanSetting showTotems;
   public final BooleanSetting showDistance;
   public final BooleanSetting skipFriends;
   public final SliderSetting enemyOffsetX;
   public final SliderSetting enemyOffsetY;
   public final BooleanSetting enableHitAnimation;
   public final SliderSetting shakeIntensity;
   public final SliderSetting shakeDuration;
   public final ColorSetting healthBarColor;
   public final ColorSetting hitColor;
   public final SliderSetting maxDistance;
   private final Map<UUID, EnemyInfo.EnemyRenderState> renderStates;
   public final SliderSetting backgroundAlpha;
   public final SliderSetting size;
   public SliderSetting ezHold;
   public SliderSetting deathFade;

   public EnemyInfo() {
      super("EnemyInfo", Module.Category.Render);
      this.showHealthBar.setChinese("\u8840\u6761");
      this.showTotems = this.add(new BooleanSetting("Totems", true));
      this.showTotems.setChinese("\u56fe\u817e\u6570");
      this.showDistance = this.add(new BooleanSetting("Distance", true));
      this.showDistance.setChinese("\u8ddd\u79bb");
      this.skipFriends = this.add(new BooleanSetting("SkipFriends", true));
      this.skipFriends.setChinese("\u8df3\u8fc7\u597d\u53cb");
      this.enemyOffsetX = this.add(new SliderSetting("EnemyOffsetX", 0, -200, 200));
      this.enemyOffsetX.setChinese("\u654c\u4ebaX\u504f\u79fb");
      this.enemyOffsetY = this.add(new SliderSetting("EnemyOffsetY", 0, -200, 200));
      this.enemyOffsetY.setChinese("\u654c\u4ebaY\u504f\u79fb");
      this.enableHitAnimation = this.add(new BooleanSetting("HitAnimation", true));
      this.enableHitAnimation.setChinese("\u53d7\u51fb\u52a8\u753b");
      this.shakeIntensity = this.add(new SliderSetting("ShakeIntensity", 3.0, 0.5, 10.0, 0.1));
      this.shakeIntensity.setChinese("\u6296\u52a8\u5f3a\u5ea6");
      this.shakeDuration = this.add(new SliderSetting("ShakeDuration", 200.0, 50.0, 1000.0, 10.0));
      this.shakeDuration.setChinese("\u6296\u52a8\u65f6\u957f");
      this.healthBarColor = this.add(new ColorSetting("HealthBarColor", new Color(108, 181, 255, 255)));
      this.healthBarColor.setChinese("\u8840\u6761\u989c\u8272");
      this.hitColor = this.add(new ColorSetting("HitColor", new Color(255, 0, 0, 100)));
      this.hitColor.setChinese("\u53d7\u51fb\u989c\u8272");
      this.maxDistance = this.add(new SliderSetting("MaxDistance", 20, 5, 100));
      this.maxDistance.setChinese("\u6700\u5927\u8ddd\u79bb");
      this.backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 232, 0, 255));
      this.backgroundAlpha.setChinese("\u80cc\u666f\u900f\u660e\u5ea6");
      this.size = this.add(new SliderSetting("Size", 1.0, 0.5, 2.0, 0.05));
      this.size.setChinese("\u5927\u5c0f");
      this.ezHold = this.add(new SliderSetting("EzHoldMs", 1000.0, 100.0, 10000.0, 100.0));
      this.ezHold.setChinese("\u51fb\u6740\u4fdd\u6301\u65f6\u957f");
      this.deathFade = this.add(new SliderSetting("DeathFadeMs", 1000.0, 100.0, 5000.0, 100.0));
      this.deathFade.setChinese("\u6b7b\u4ea1\u6de1\u51fa\u65f6\u957f");
      this.renderStates = new HashMap<>();
      this.setChinese("\u654c\u4eba\u4fe1\u606f");
      INSTANCE = this;
      this.drawn.setValue(true);
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (!nullCheck() && this.isOn()) {
         if (mc.getWindow() != null) {
            SkiaContext.createSurface(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         }

         this.renderEnemyList(var2);
      }
   }

   private void renderEnemyList(float var1) {
      long var2 = System.currentTimeMillis();
      List<PlayerEntity> var4 = this.getEnemyList();
      HashSet<UUID> var5 = new HashSet<>();

      for (PlayerEntity var7 : var4) {
         var5.add(var7.getUuid());
         EnemyInfo.EnemyRenderState var8 = this.renderStates.computeIfAbsent(var7.getUuid(), var0 -> new EnemyInfo.EnemyRenderState());
         if (!var8.seenOnce || var8.disappearing) {
            var8.seenOnce = true;
            var8.disappearing = false;
            var8.appearStartMs = var2;
         }

         var8.lastSeenMs = var2;
      }

      for (Entry var10 : this.renderStates.entrySet()) {
         if (!var5.contains(var10.getKey())) {
            EnemyInfo.EnemyRenderState var11 = (EnemyInfo.EnemyRenderState)var10.getValue();
            if (!var11.disappearing) {
               var11.disappearing = true;
               var11.disappearStartMs = var2;
            }
         }
      }

      SkiaContext.draw(var6 -> {
         for (PlayerEntity var8x : var4) {
            this.renderEnemy(var8x, var1, var2);
         }

         for (Entry var13 : this.renderStates.entrySet()) {
            UUID var9 = (UUID)var13.getKey();
            EnemyInfo.EnemyRenderState var10x = (EnemyInfo.EnemyRenderState)var13.getValue();
            if (var10x.disappearing && !var5.contains(var9)) {
               PlayerEntity var11x = this.findPlayerByUuid(var9);
               if (var11x != null) {
                  this.renderEnemy(var11x, var1, var2);
               } else if (var10x.hasLastBounds) {
                  if (var10x.deathStartMs == 0L) {
                     if (LogoutSpots.INSTANCE != null && LogoutSpots.INSTANCE.getLogoutCache().containsKey(var9)) {
                        var10x.deathStartMs = var2;
                        var10x.ezText = "EZ LOG";
                     } else {
                        var10x.deathStartMs = var2;
                        var10x.ezText = "EZ RUN";
                     }
                  }

                  this.renderEnemyAtLastBounds(var10x, var2);
               }
            }
         }
      });
      this.renderStates.entrySet().removeIf(var2x -> {
         if (DeathFx.inWindow(var2x.getValue().deathStartMs, var2, INSTANCE.ezHold.getValue(), INSTANCE.deathFade.getValue())) {
            return false;
         } else {
            EnemyInfo.EnemyRenderState var3 = var2x.getValue();
            return !var3.disappearing ? false : var2 - var3.disappearStartMs >= 260L;
         }
      });
   }

   private List<PlayerEntity> getEnemyList() {
      ArrayList<PlayerEntity> var1 = new ArrayList<>();
      if (mc.world != null && mc.player != null) {
         for (Entity var3 : mc.world.getEntities()) {
            if (var3 instanceof PlayerEntity var4
               && var4 != mc.player
               && (!this.skipFriends.getValue() || !Alien.FRIEND.isFriend(var4))
               && !(mc.player.distanceTo(var4) > this.maxDistance.getValue())) {
               var1.add(var4);
            }
         }

         var1.sort((var0, var1x) -> Double.compare(mc.player.distanceTo(var0), mc.player.distanceTo(var1x)));
         return var1;
      } else {
         return var1;
      }
   }

   private void renderEnemy(PlayerEntity var1, float var2, long var3) {
      EnemyInfo.EnemyRenderState var5 = this.renderStates.computeIfAbsent(var1.getUuid(), var0 -> new EnemyInfo.EnemyRenderState());
      this.updateState(var5, var1);
      float[] var6 = this.getProjectedBounds(var1, var2);
      if (var6 != null) {
         int var7 = this.calculateComponentWidth(var1);
         int var8 = this.calculateComponentHeight(var1);
         int var9 = Math.round(var6[2]) + this.enemyOffsetX.getValueInt() + 6;
         int var10 = Math.round(var6[1] + (var6[3] - var6[1]) * 0.5F) + this.enemyOffsetY.getValueInt() - var8 / 2;
         float var11 = this.getDistanceScale(var1, var2);
         var5.hasLastBounds = true;
         var5.lastX = var9;
         var5.lastY = var10;
         var5.lastW = var7;
         var5.lastH = var8;
         var5.lastDistanceScale = var11;
         var5.lastNameLine = this.buildNameLine(var1);
         var5.lastHpLine = this.buildHealthLine(var1);
         var5.lastTotemLine = String.format(Locale.ROOT, "Pop: %d", Alien.POP.getPop(var1.getName().getString()));
         if (var1 instanceof AbstractClientPlayerEntity var12) {
            var5.lastSkin = this.resolveSkinTexture(var12, var5);
         }

         var5.lastMaxHealth = var1.getMaxHealth() + var1.getAbsorptionAmount();
         this.drawComponent(var1, var5, var9, var10, var7, var8, var11);
      }
   }

   private void renderEnemyAtLastBounds(EnemyInfo.EnemyRenderState var1, long var2) {
      if (var1.hasLastBounds) {
         this.drawComponent(null, var1, var1.lastX, var1.lastY, var1.lastW, var1.lastH, var1.lastDistanceScale);
      }
   }

   private void updateState(EnemyInfo.EnemyRenderState var1, PlayerEntity var2) {
      if (var2.getHealth() > 0.0F) {
         var1.deathStartMs = 0L;
      } else if (var1.deathStartMs == 0L) {
         var1.deathStartMs = System.currentTimeMillis();
      }

      float var3 = var2.getHealth() + var2.getAbsorptionAmount();
      if (var1.displayedHealth == 0.0F) {
         var1.displayedHealth = var3;
      } else {
         var1.displayedHealth = var1.displayedHealth + (var3 - var1.displayedHealth) * 0.2F;
      }

      if (this.enableHitAnimation.getValue() && var3 < var1.lastHealth) {
         var1.lastHitTime = System.currentTimeMillis();
      }

      var1.lastHealth = var3;
      long var4 = System.currentTimeMillis() - var1.lastHitTime;
      if (var4 < this.shakeDuration.getValue()) {
         var1.hitAnimation = 1.0F - (float)var4 / (float)this.shakeDuration.getValue();
      } else {
         var1.hitAnimation = 0.0F;
      }
   }

   private int calculateComponentWidth(PlayerEntity var1) {
      String var2 = this.buildNameLine(var1);
      String var3 = this.buildHealthLine(var1);
      String var4 = String.format(Locale.ROOT, "Pop: %d", Alien.POP.getPop(var1.getName().getString()));
      Font var5 = Skia.getDefaultFont(var2, 10.5F, true);
      Font var6 = Skia.getDefaultFont(var3, 7.5F, false);
      float var7 = Math.max(Skia.getStringWidth(var2, var5), Skia.getStringWidth(var3, var6));
      if (this.showTotems.getValue()) {
         var7 = Math.max(var7, Skia.getStringWidth(var4, var6));
      }

      if (this.showHealthBar.getValue()) {
         var7 = Math.max(var7, 72.0F);
      }

      return Math.round(55.0F + var7);
   }

   private int calculateComponentHeight(PlayerEntity var1) {
      String var2 = this.buildNameLine(var1);
      String var3 = this.buildHealthLine(var1);
      Font var4 = Skia.getDefaultFont(var2, 10.5F, true);
      Font var5 = Skia.getDefaultFont(var3, 7.5F, false);
      float var6 = Skia.getFontHeight(var4);
      if (this.showTotems.getValue()) {
         var6 += 0.0F + Skia.getFontHeight(var5);
      }

      var6 += 0.0F + Skia.getFontHeight(var5);
      if (this.showHealthBar.getValue()) {
         var6 += 5.0F;
      }

      return Math.round(12.0F + Math.max(34.0F, var6));
   }

   private void drawComponent(PlayerEntity var1, EnemyInfo.EnemyRenderState var2, int var3, int var4, int var5, int var6, float var7) {
      long var8 = System.currentTimeMillis();
      float var10 = var2.disappearing ? getDisappearScale(var2, var8) : getAppearScale(var2, var8);
      float var11 = var2.disappearing ? getDisappearAlpha(var2, var8) : 1.0F;
      var11 *= DeathFx.alphaFactor(var2.deathStartMs, var8, this.ezHold.getValue(), this.deathFade.getValue());
      float var12 = Math.max(0.28F, 0.82F * var7) * var10 * this.size.getValueFloat();
      float var13 = var3 + var5 * 0.5F;
      float var14 = var4 + var6 * 0.5F;
      if (Skia.getCanvas() != null && !(var12 <= 0.001F) && !(var11 <= 0.001F)) {
         Skia.getCanvas().save();
         Skia.getCanvas().translate(var13, var14);
         Skia.getCanvas().scale(var12, var12);
         Skia.getCanvas().translate(-var13, -var14);
         int var15 = MathHelper.clamp(Math.round(this.backgroundAlpha.getValueInt() * var11), 0, 255);
         int var16 = MathHelper.clamp(Math.round(20.0F * var11), 0, 255);
         Skia.drawRoundedRect(var3, var4, var5, var6, 7.0F, new Color(8, 8, 10, var15));
         Skia.drawRoundedRectOutline(var3, var4, var5, var6, 7.0F, 1.0F, new Color(255, 255, 255, var16));
         int var17 = var3 + 6;
         int var18 = var4 + (var6 - 34) / 2;
         if (var2.deathStartMs != 0L) {
            DeathFx.pushLayer(var11, var17, var18, 34, 34);
         }

         this.drawEnemyHead(var1, var17, var18, var2);
         int var19 = var17 + 34 + 9;
         if (var2.deathStartMs != 0L) {
            Skia.getCanvas().restore();
            int var20 = var3 + var5 - 6 - var19;
            if (var2.ezText != null) {
               DeathFx.drawCardText(var19, var4, var20, var6, var11, var2.ezText);
            } else {
               DeathFx.drawCard(var19, var4, var20, var6, var11);
            }

            Skia.getCanvas().restore();
         } else {
            String var41 = var1 != null ? this.buildNameLine(var1) : var2.lastNameLine;
            String var21 = var1 != null ? this.buildHealthLine(var1) : var2.lastHpLine;
            Font var22 = Skia.getDefaultFont(var41, 10.5F, true);
            Font var23 = Skia.getDefaultFont(var21, 7.5F, false);
            float var24 = Skia.getFontHeight(var22);
            float var25 = Skia.getFontHeight(var23);
            float var26 = this.showTotems.getValue() ? var25 : 0.0F;
            float var27 = var24 + (this.showTotems.getValue() ? 0.0F + var26 : 0.0F) + 0.0F + var25 + (this.showHealthBar.getValue() ? 5.0F : 0.0F);
            float var28 = var4 + (var6 - var27) / 2.0F;
            float var29 = var28 + var24 + 0.0F;
            float var30 = this.showTotems.getValue() ? var29 + var26 + 0.0F : var28 + var24 + 0.0F;
            float var31 = var30 + var25 + 1.0F;
            int var32 = MathHelper.clamp(Math.round(255.0F * var11), 0, 255);
            int var33 = MathHelper.clamp(Math.round(220.0F * var11), 0, 255);
            int var34 = MathHelper.clamp(Math.round(235.0F * var11), 0, 255);
            Skia.drawText(var41, var19, var28, new Color(248, 248, 248, var32), var22);
            if (this.showTotems.getValue()) {
               String var35 = var1 != null
                  ? String.format(Locale.ROOT, "Pop: %d", Alien.POP.getPop(var1.getName().getString()))
                  : (var2.lastTotemLine == null ? "" : var2.lastTotemLine);
               Skia.drawText(var35, var19, var29, new Color(180, 205, 225, var33), var23);
            }

            if (this.showHealthBar.getValue()) {
               float var42 = var1 != null ? var1.getMaxHealth() + var1.getAbsorptionAmount() : Math.max(1.0F, var2.lastMaxHealth);
               if (var42 > 0.0F) {
                  Skia.drawText(var21, var19, var30, new Color(196, 216, 232, var34), var23);
                  int var36 = MathHelper.clamp(Math.round(235.0F * var11), 0, 255);
                  Skia.drawRoundedRect(var19, var31, 72.0F, 4.0F, 2.0F, new Color(36, 52, 70, var36));
                  float var37 = Math.max(0.0F, Math.min(var2.displayedHealth, var42));
                  int var38 = Math.round(72.0F * (var37 / var42));
                  if (var38 > 0) {
                     int var39 = MathHelper.clamp(Math.round(255.0F * var11), 0, 255);
                     Skia.drawRoundedRect(var19, var31, var38, 4.0F, 2.0F, new Color(137, 214, 255, var39));
                  }
               }
            }

            Skia.getCanvas().restore();
         }
      }
   }

   private void drawEnemyHead(PlayerEntity var1, int var2, int var3, EnemyInfo.EnemyRenderState var4) {
      int var5 = var2;
      int var6 = var3;
      int var7 = 34;
      if (this.enableHitAnimation.getValue() && var4.hitAnimation > 0.0F) {
         float var8 = (float)Math.max(0.7F, 1.0 - this.shakeIntensity.getValue() / 100.0 * var4.hitAnimation);
         var7 = Math.round(34.0F * var8);
         var5 = var2 + (34 - var7) / 2;
         var6 = var3 + (34 - var7) / 2;
         var5 += Math.round((float)(this.shakeIntensity.getValue() * var4.hitAnimation * 0.5 * Math.sin(System.currentTimeMillis() * 0.001 * 45.0)));
         var6 += Math.round((float)(this.shakeIntensity.getValue() * var4.hitAnimation * 0.5 * Math.cos(System.currentTimeMillis() * 0.001 * 57.0)));
      }

      Skia.drawRoundedRect(var5, var6, var7, var7, 3.5F, new Color(18, 18, 20, 255));
      Object var19 = null;
      if (var1 instanceof AbstractClientPlayerEntity var9) {
         var19 = this.resolveSkinTexture(var9, var4);
         var4.lastSkin = (Identifier)var19;
         Skia.save();
         Skia.clip(var5, var6, var7, var7, 3.5F);
         boolean var10 = this.drawHeadLayer(var4, (Identifier)var19, var5, var6, var7, 8.0F, 8.0F);

         try {
            Class var11 = Class.forName("net.minecraft.client.render.entity.model.PlayerModelPart");
            Field var12 = var11.getField("HAT");
            Object var13 = var12.get(null);
            Method var14 = AbstractClientPlayerEntity.class.getMethod("isPartVisible", var11);
            boolean var15 = (Boolean)var14.invoke(var9, var13);
            if (var15) {
               this.drawHeadLayer(var4, (Identifier)var19, var5, var6, var7, 40.0F, 8.0F);
            }
         } catch (Exception var16) {
         }

         if (!var10) {
            this.drawFallbackHead(var5, var6, var7);
         }

         Skia.restore();
      } else if (var4.lastSkin != null) {
         Skia.save();
         Skia.clip(var5, var6, var7, var7, 3.5F);
         boolean var22 = this.drawHeadLayer(var4, var4.lastSkin, var5, var6, var7, 8.0F, 8.0F);
         if (!var22) {
            this.drawFallbackHead(var5, var6, var7);
         }

         Skia.restore();
      } else {
         this.drawFallbackHead(var5, var6, var7);
      }

      if (this.enableHitAnimation.getValue() && var4.hitAnimation > 0.0F) {
         Color var21 = this.hitColor.getValue();
         int var23 = Math.min(255, Math.round(var21.getAlpha() * var4.hitAnimation));
         Skia.drawRoundedRect(var5, var6, var7, var7, 3.5F, new Color(var21.getRed(), var21.getGreen(), var21.getBlue(), var23));
      }
   }

   private boolean drawHeadLayer(EnemyInfo.EnemyRenderState var1, Identifier var2, int var3, int var4, int var5, float var6, float var7) {
      if (var1.lastSkinUrl != null && !var1.lastSkinUrl.isBlank()) {
         boolean var8 = Skia.drawUrlTextureRegion(var1.lastSkinUrl, var3, var4, var5, var5, var6, var7, 8.0F, 8.0F);
         if (var8) {
            return true;
         }
      }

      return Skia.drawTextureRegion(var2, var3, var4, var5, var5, var6, var7, 8.0F, 8.0F);
   }

   private Identifier resolveSkinTexture(AbstractClientPlayerEntity var1, EnemyInfo.EnemyRenderState var2) {
      if (mc.getNetworkHandler() != null) {
         PlayerListEntry var3 = mc.getNetworkHandler().getPlayerListEntry(var1.getUuid());
         if (var3 != null) {
            try {
               SkinTextures var4 = var3.getSkinTextures();
               if (var4 != null) {
                  try {
                     String var5 = var4.textureUrl();
                     if (var5 != null) {
                        var2.lastSkinUrl = var5;
                     }
                  } catch (Throwable var8) {
                  }

                  Identifier var11 = var4.texture();
                  if (var11 != null) {
                     return var11;
                  }
               }
            } catch (Throwable var9) {
            }
         }
      }

      try {
         try {
            String var10 = var1.getSkinTextures().textureUrl();
            if (var10 != null) {
               var2.lastSkinUrl = var10;
            }
         } catch (Throwable var6) {
         }

         return var1.getSkinTextures().texture();
      } catch (Throwable var7) {
         return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
      }
   }

   private void drawFallbackHead(int var1, int var2, int var3) {
      Skia.drawRoundedRect(var1, var2, var3, var3, 3.5F, new Color(40, 42, 48, 255));
      Skia.drawRoundedRect(var1 + 2, var2 + 2, var3 - 4, var3 - 4, 2.5F, new Color(82, 89, 102, 255));
      Skia.drawRoundedRect(var1 + 4, var2 + 5, var3 - 8, var3 - 8, 2.0F, new Color(180, 150, 125, 255));
      Skia.drawCircle(var1 + var3 * 0.35F, var2 + var3 * 0.42F, 1.0F, Color.BLACK);
      Skia.drawCircle(var1 + var3 * 0.65F, var2 + var3 * 0.42F, 1.0F, Color.BLACK);
   }

   private float[] getProjectedBounds(Entity var1, float var2) {
      if (mc.player != null && mc.getWindow() != null && var1 != null) {
         Box var3 = var1.getBoundingBox();
         double var4 = var1.prevX + (var1.getX() - var1.prevX) * var2;
         double var6 = var1.prevY + (var1.getY() - var1.prevY) * var2;
         double var8 = var1.prevZ + (var1.getZ() - var1.prevZ) * var2;
         double var10 = var4 - var1.getX();
         double var12 = var6 - var1.getY();
         double var14 = var8 - var1.getZ();
         Vec3d[] var16 = new Vec3d[]{
            new Vec3d(var3.minX + var10, var3.minY + var12, var3.minZ + var14),
            new Vec3d(var3.minX + var10, var3.minY + var12, var3.maxZ + var14),
            new Vec3d(var3.minX + var10, var3.maxY + var12, var3.minZ + var14),
            new Vec3d(var3.minX + var10, var3.maxY + var12, var3.maxZ + var14),
            new Vec3d(var3.maxX + var10, var3.minY + var12, var3.minZ + var14),
            new Vec3d(var3.maxX + var10, var3.minY + var12, var3.maxZ + var14),
            new Vec3d(var3.maxX + var10, var3.maxY + var12, var3.minZ + var14),
            new Vec3d(var3.maxX + var10, var3.maxY + var12, var3.maxZ + var14)
         };
         float var17 = Float.MAX_VALUE;
         float var18 = Float.MAX_VALUE;
         float var19 = -Float.MAX_VALUE;
         float var20 = -Float.MAX_VALUE;
         boolean var21 = false;

         for (Vec3d var25 : var16) {
            Vec3d var26 = TextUtil.worldSpaceToScreenSpace(var25);
            if (!(var26.z <= 0.0) && !(var26.z >= 1.0)) {
               var21 = true;
               var17 = Math.min(var17, (float)var26.x);
               var18 = Math.min(var18, (float)var26.y);
               var19 = Math.max(var19, (float)var26.x);
               var20 = Math.max(var20, (float)var26.y);
            }
         }

         return !var21 ? null : new float[]{var17, var18, var19, var20};
      } else {
         return null;
      }
   }

   private float getDistanceScale(Entity var1, float var2) {
      if (mc.cameraEntity != null && var1 != null) {
         double var3 = var1.prevX + (var1.getX() - var1.prevX) * var2;
         double var5 = var1.prevY + (var1.getY() - var1.prevY) * var2;
         double var7 = var1.prevZ + (var1.getZ() - var1.prevZ) * var2;
         Vec3d var9 = new Vec3d(var3, var5 + var1.getBoundingBox().getLengthY() + 0.3, var7);
         float var10 = 1.0F - MathHelper.sqrt((float)mc.cameraEntity.squaredDistanceTo(var9)) * 0.01F;
         return Math.max(var10, 0.34146342F);
      } else {
         return 1.0F;
      }
   }

   private String buildNameLine(PlayerEntity var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.getName().getString();
         return this.showDistance.getValue() && mc.player != null ? var2 + " [" + String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(var1)) + "]" : var2;
      }
   }

   private String buildHealthLine(PlayerEntity var1) {
      return var1 == null ? "" : String.format(Locale.ROOT, "%.1f HP", var1.getHealth() + var1.getAbsorptionAmount());
   }

   private PlayerEntity findPlayerByUuid(UUID var1) {
      if (mc.world != null && var1 != null) {
         try {
            Method var2 = mc.world.getClass().getMethod("getPlayerByUuid", UUID.class);
            Object var3 = var2.invoke(mc.world, var1);
            if (var3 instanceof PlayerEntity) {
               return (PlayerEntity)var3;
            }
         } catch (Throwable var4) {
         }

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            if (var1.equals(var6.getUuid())) {
               return var6;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static float easeOutCubic(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1 * var1;
   }

   private static float easeInCubic(float var0) {
      return var0 * var0 * var0;
   }

   private static float easeOutElastic(float var0) {
      if (var0 <= 0.0F) {
         return 0.0F;
      } else if (var0 >= 1.0F) {
         return 1.0F;
      } else {
         float var1 = 0.55F;
         return (float)(Math.pow(2.0, -10.0 * var0) * Math.sin((var0 - var1 / 4.0F) * (Math.PI * 2) / var1) + 1.0);
      }
   }

   private static float getAppearScale(EnemyInfo.EnemyRenderState var0, long var1) {
      if (!var0.seenOnce) {
         return 0.0F;
      } else {
         long var3 = Math.max(0L, var1 - var0.appearStartMs);
         float var5 = MathHelper.clamp((float)var3 / 320.0F, 0.0F, 1.0F);
         float var6 = easeOutElastic(var5);
         return MathHelper.clamp(var6, 0.0F, 1.15F);
      }
   }

   private static float getDisappearScale(EnemyInfo.EnemyRenderState var0, long var1) {
      if (DeathFx.inWindow(var0.deathStartMs, var1, INSTANCE.ezHold.getValue(), INSTANCE.deathFade.getValue())) {
         return 1.0F;
      } else {
         long var3 = Math.max(0L, var1 - var0.disappearStartMs);
         float var5 = MathHelper.clamp((float)var3 / 260.0F, 0.0F, 1.0F);
         if (var5 <= 0.35F) {
            return 1.0F + 0.12F * easeOutCubic(var5 / 0.35F);
         } else {
            float var6 = (var5 - 0.35F) / 0.65F;
            return 1.12F * (1.0F - easeInCubic(var6));
         }
      }
   }

   private static float getDisappearAlpha(EnemyInfo.EnemyRenderState var0, long var1) {
      if (DeathFx.inWindow(var0.deathStartMs, var1, INSTANCE.ezHold.getValue(), INSTANCE.deathFade.getValue())) {
         return 1.0F;
      } else {
         long var3 = Math.max(0L, var1 - var0.disappearStartMs);
         float var5 = MathHelper.clamp((float)var3 / 260.0F, 0.0F, 1.0F);
         float var6 = var5 <= 0.25F ? 0.0F : (var5 - 0.25F) / 0.75F;
         return 1.0F - easeInCubic(MathHelper.clamp(var6, 0.0F, 1.0F));
      }
   }

   static class EnemyRenderState {
      private float displayedHealth;
      private float lastHealth;
      private long lastHitTime;
      private float hitAnimation;
      private boolean seenOnce;
      private long appearStartMs;
      private boolean disappearing;
      private long disappearStartMs;
      private long lastSeenMs;
      private boolean hasLastBounds;
      private int lastX;
      private int lastY;
      private int lastW;
      private int lastH;
      private float lastDistanceScale = 1.0F;
      private float lastMaxHealth = 20.0F;
      private String lastNameLine = "";
      private String lastHpLine = "";
      private String lastTotemLine = "";
      private Identifier lastSkin;
      private String lastSkinUrl;
      public long deathStartMs;
      public String ezText;
   }
}
