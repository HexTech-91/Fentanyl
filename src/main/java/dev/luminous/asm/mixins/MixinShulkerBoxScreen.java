package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.combat.AutoRegear;
import dev.luminous.mod.modules.impl.combat.Refill;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ShulkerBoxScreen.class})
public abstract class MixinShulkerBoxScreen extends HandledScreen<ShulkerBoxScreenHandler> {
   protected MixinShulkerBoxScreen(ShulkerBoxScreenHandler var1, PlayerInventory var2, Text var3) {
      super(var1, var2, var3);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$renderKitOverlay(DrawContext var1, int var2, int var3, float var4, CallbackInfo var5) {
      if (Refill.isOverlayActive()) {
         Refill.renderOverlay(var1);
         var5.cancel();
      } else {
         if (AutoRegear.isOverlayActive()) {
            AutoRegear.renderOverlay(var1);
            var5.cancel();
         }
      }
   }
}
