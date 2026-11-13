package dev.luminous.asm.mixins;

import dev.luminous.api.utils.render.EnderChestOverlayRenderer;
import dev.luminous.api.utils.render.ShulkerOverlayRenderer;
import dev.luminous.mod.modules.impl.combat.AutoRegear;
import dev.luminous.mod.modules.impl.combat.Refill;
import dev.luminous.mod.modules.impl.misc.AutoEnderChestPlus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HandledScreen.class})
public abstract class MixinEnderChestSilent<T extends ScreenHandler> extends Screen {
   @Unique
   private static final int ECHEST_COLUMNS = 9;
   @Unique
   private static final int ECHEST_ROWS = 3;
   @Unique
   private final int[] alien$panel = new int[4];

   protected MixinEnderChestSilent(Text var1) {
      super(var1);
   }

   @Unique
   private GenericContainerScreenHandler alien$enderChestHandler() {
      if (!AutoEnderChestPlus.isOverlayActive()) {
         return null;
      } else if (((HandledScreen<?>)(Object)this).getScreenHandler() instanceof GenericContainerScreenHandler var3) {
         return var3.getRows() == 3 ? var3 : null;
      } else {
         return null;
      }
   }

   @Unique
   private ShulkerBoxScreenHandler alien$shulkerPanelHandler() {
      if (!ShulkerBoxScreen.class.isInstance(this)) {
         return null;
      } else if (((HandledScreen<?>)(Object)this).getScreenHandler() instanceof ShulkerBoxScreenHandler var3) {
         return !Refill.isOverlayActive() && !AutoRegear.isOverlayActive() ? null : var3;
      } else {
         return null;
      }
   }

   @Unique
   private boolean alien$panelActive() {
      return this.alien$enderChestHandler() != null || this.alien$shulkerPanelHandler() != null;
   }

   @Unique
   private int[] alien$layout() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      int var2 = var1.getWindow().getScaledWidth();
      int var3 = var1.getWindow().getScaledHeight();
      double var4 = var2 / 2.0;
      double var6 = var3 / 2.0;
      AutoEnderChestPlus var8 = AutoEnderChestPlus.INSTANCE;
      if (var8 != null) {
         var4 = var8.overlayAnchorX();
         var6 = var8.overlayAnchorY();
      }

      EnderChestOverlayRenderer.layout(var2, var3, var4, var6, 9, 3, this.alien$panel);
      return this.alien$panel;
   }

   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$noBackground(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      if (this.alien$panelActive()) {
         var5.cancel();
      }
   }

   @Inject(
      method = {"keyPressed(III)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$escapeCloses(int var1, int var2, int var3, CallbackInfoReturnable<Boolean> var4) {
      if (var1 == 256) {
         if (this.alien$panelActive()) {
            MinecraftClient var5 = MinecraftClient.getInstance();
            if (var5.player != null) {
               var5.player.closeHandledScreen();
            }

            var4.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$renderSilent(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      GenericContainerScreenHandler var6 = this.alien$enderChestHandler();
      if (var6 != null) {
         if (!ShulkerBoxScreen.class.isInstance(this)) {
            int[] var7 = this.alien$layout();
            ItemStack var8 = var6.getCursorStack();
            EnderChestOverlayRenderer.render(var1, var6, 9, 3, var7[0], var7[1], var8);
            var5.cancel();
         }
      }
   }

   @Inject(
      method = {"mouseClicked(DDI)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$clickSilent(double var1, double var3, int var5, CallbackInfoReturnable<Boolean> var6) {
      GenericContainerScreenHandler var7 = this.alien$enderChestHandler();
      if (var7 == null) {
         ShulkerBoxScreenHandler var14 = this.alien$shulkerPanelHandler();
         if (var14 != null) {
            this.alien$clickShulkerPanel(var14, var1, var3, var5, var6);
         }
      } else {
         int[] var8 = this.alien$layout();
         int var9 = EnderChestOverlayRenderer.slotAt(var1, var3, var8[0], var8[1], 9, 3);
         if (var9 < 0) {
            var6.setReturnValue(true);
         } else if (var5 == 2) {
            EnderChestOverlayRenderer.previewSlot(var7, var9);
            var6.setReturnValue(true);
         } else {
            MinecraftClient var10 = MinecraftClient.getInstance();
            if (var10.player == null) {
               var6.setReturnValue(true);
            } else {
               SlotActionType var11;
               byte var12;
               if (var5 == 0 && var10.player.isSneaking()) {
                  var11 = SlotActionType.QUICK_MOVE;
                  var12 = 0;
               } else if (var5 == 0) {
                  var11 = SlotActionType.PICKUP;
                  var12 = 0;
               } else {
                  if (var5 != 1) {
                     var6.setReturnValue(true);
                     return;
                  }

                  var11 = SlotActionType.PICKUP;
                  var12 = 1;
               }

               boolean var13 = var11 == SlotActionType.QUICK_MOVE;
               EnderChestOverlayRenderer.markTakenUnlessPartial(var7, var9, var13 ? -1 : var12, var8[0], var8[1], 9);
               var10.interactionManager.clickSlot(var7.syncId, var9, var12, var11, var10.player);
               var6.setReturnValue(true);
            }
         }
      }
   }

   @Unique
   private void alien$clickShulkerPanel(ShulkerBoxScreenHandler var1, double var2, double var4, int var6, CallbackInfoReturnable<Boolean> var7) {
      int[] var8 = ShulkerOverlayRenderer.lastPanel();
      if (var8 != null) {
         int var9 = ShulkerOverlayRenderer.slotAt(var2, var4, var8[0], var8[1]);
         if (var9 < 0) {
            var7.setReturnValue(true);
         } else {
            MinecraftClient var10 = MinecraftClient.getInstance();
            if (var10.player == null) {
               var7.setReturnValue(true);
            } else {
               SlotActionType var11;
               byte var12;
               if (var6 == 0 && var10.player.isSneaking()) {
                  var11 = SlotActionType.QUICK_MOVE;
                  var12 = 0;
               } else if (var6 == 0) {
                  var11 = SlotActionType.PICKUP;
                  var12 = 0;
               } else {
                  if (var6 != 1) {
                     var7.setReturnValue(true);
                     return;
                  }

                  var11 = SlotActionType.PICKUP;
                  var12 = 1;
               }

               var10.interactionManager.clickSlot(var1.syncId, var9, var12, var11, var10.player);
               var7.setReturnValue(true);
            }
         }
      }
   }
}
