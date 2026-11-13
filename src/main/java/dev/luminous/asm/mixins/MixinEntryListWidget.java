package dev.luminous.asm.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntryListWidget.class})
public abstract class MixinEntryListWidget {
   @Inject(
      method = {"drawMenuListBackground(Lnet/minecraft/client/gui/DrawContext;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$makeMultiplayerListTransparent(DrawContext context, CallbackInfo ci) {
      if (((Object)this) instanceof MultiplayerServerListWidget) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawHeaderAndFooterSeparators(Lnet/minecraft/client/gui/DrawContext;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$hideMultiplayerListSeparators(DrawContext context, CallbackInfo ci) {
      if (((Object)this) instanceof MultiplayerServerListWidget) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawSelectionHighlight(Lnet/minecraft/client/gui/DrawContext;IIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void alien$drawTransparentMultiplayerSelection(
      DrawContext context, int y, int rowWidth, int rowHeight, int outerColor, int innerColor, CallbackInfo ci
   ) {
      if (((Object)this) instanceof MultiplayerServerListWidget) {
         EntryListWidget<?> self = (EntryListWidget<?>)(Object)this;
         int left = self.getX() + (self.getWidth() - rowWidth) / 2;
         int right = self.getX() + (self.getWidth() + rowWidth) / 2;
         context.fill(left, y - 2, right, y + rowHeight + 2, 1719977983);
         context.fill(left + 1, y - 1, right - 1, y + rowHeight + 1, 856692768);
         ci.cancel();
      }
   }
}
