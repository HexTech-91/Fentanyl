package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.HealthBarOptimization;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class MixinInGameHud {
   @Final
   @Shadow
   private MinecraftClient client;
   @Unique
   final Color minColor = new Color(196, 0, 0);
   @Unique
   final Color maxColor = new Color(0, 227, 0);

   @Inject(
      method = {"renderPortalOverlay(Lnet/minecraft/client/gui/DrawContext;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderPortalOverlay(DrawContext var1, float var2, CallbackInfo var3) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderStatusEffectOverlay(CallbackInfo var1) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) {
         var1.cancel();
      }
   }

   @Redirect(
      method = {"renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/WorldProperties;isHardcore()Z"
      ),
      require = 0
   )
   private boolean useHardcoreHearts(WorldProperties var1) {
      return HealthBarOptimization.INSTANCE != null && HealthBarOptimization.INSTANCE.isOn() || var1.isHardcore();
   }

   @Inject(
      method = {"renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("TAIL")}
   )
   private void onRenderMainHud(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      PlayerEntity var4;
      if (HUD.INSTANCE.isOn() && HUD.INSTANCE.armor.getValue() && (var4 = this.getCameraPlayer()) != null) {
         int var5 = var1.getScaledWindowWidth() / 2 + 91;
         int var6 = var1.getScaledWindowHeight() - 28 - HUD.INSTANCE.armorOffset.getValueInt();
         if (this.client.interactionManager.hasStatusBars()) {
            var6 -= 16;
         }

         int var7;
         if ((var7 = this.getHeartCount(this.getRiddenEntity())) == 0) {
            var6 -= 10;
         }

         int var8 = var4.getMaxAir();
         int var9 = Math.min(var4.getAir(), var8);
         if (var4.isSubmergedIn(FluidTags.WATER) || var9 < var8) {
            int var10 = this.getHeartRows(var7) - 1;
            var6 += var10 * 10;
         }

         for (ItemStack var11 : this.client.player.getInventory().armor) {
            var5 -= 20;
            if (!var11.isEmpty()) {
               var1.getMatrices().push();
               int var12 = EntityUtil.getDamagePercent(var11);
               var1.drawItem(var11, var5, var6);
               var1.drawItemInSlot(this.client.textRenderer, var11, var5, var6);
               if (HUD.INSTANCE.durability.getValue()) {
                  if (HUD.INSTANCE.font.getValue()) {
                     FontManager.small
                        .drawString(
                           var1.getMatrices(),
                           var12 + "%",
                           var5 + 1,
                           var6 - FontManager.small.getFontHeight() / 2.0F,
                           ColorUtil.fadeColor(this.minColor, this.maxColor, var12 / 100.0F).getRGB(),
                           HUD.INSTANCE.shadow.getValue()
                        );
                  } else {
                     TextUtil.drawStringScale(
                        var1,
                        var12 + "%",
                        var5 + 2,
                        var6 - 2.25F,
                        ColorUtil.fadeColor(this.minColor, this.maxColor, var12 / 100.0F).getRGB(),
                        0.5F,
                        HUD.INSTANCE.shadow.getValue()
                     );
                  }
               }

               var1.getMatrices().pop();
            }
         }
      }
   }

   @Shadow
   private int getHeartRows(int var1) {
      return 0;
   }

   @Shadow
   private int getHeartCount(LivingEntity var1) {
      return 0;
   }

   @Shadow
   private PlayerEntity getCameraPlayer() {
      return null;
   }

   @Shadow
   private LivingEntity getRiddenEntity() {
      return null;
   }

   @Shadow
   public abstract void render(DrawContext var1, RenderTickCounter var2);

   @Inject(
      at = {@At("HEAD")},
      method = {"render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"}
   )
   public void renderStart(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      Alien.EVENT_BUS.post(PreRender2DEvent.get(var1));
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"}
   )
   public void renderHook(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      Alien.MODULE.onRender2D(var1);
   }

   @Inject(
      method = {"clear()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"
      )},
      cancellable = true
   )
   private void onClear(CallbackInfo var1) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
         var1.cancel();
      }
   }

   @ModifyArg(
      method = {"renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V",
         ordinal = 1
      ),
      index = 1
   )
   private int selectedSlotX(int var1) {
      if (ClientSetting.INSTANCE.hotbar()) {
         double var2 = ClientSetting.animation.get(var1, ClientSetting.INSTANCE.hotbarTime.getValueInt(), ClientSetting.INSTANCE.animEase.getValue());
         return (int)var2;
      } else {
         return var1;
      }
   }

   @Inject(
      method = {"renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderCrosshairBegin(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (Crosshair.INSTANCE.isOn()) {
         Crosshair.INSTANCE.draw(var1);
         var3.cancel();
      }
   }
}
