package dev.luminous.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClickableWidget.class})
public abstract class MixinButtonWidgetCustom {
   @Shadow
   public abstract int getX();

   @Shadow
   public abstract int getY();

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract int getHeight();

   @Shadow
   public abstract Text getMessage();

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void luminous$renderCustom(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (((Object)this) instanceof ButtonWidget) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
         int bgColor = hovered ? 1430203002 : 573778777;
         int borderColor = hovered ? 2143263999 : 2002732728;
         context.fill(x, y, x + w, y + h, bgColor);
         context.fill(x, y, x + w, y + 1, borderColor);
         context.fill(x, y + h - 1, x + w, y + h, borderColor);
         context.fill(x, y, x + 1, y + h, borderColor);
         context.fill(x + w - 1, y, x + w, y + h, borderColor);
         if (hovered) {
            context.fill(x, y, x + w, y + h, 585553151);
         }

         long var15 = System.currentTimeMillis();
         double var17 = var15 % 1400L / 1400.0;
         int var21 = Math.max(w / 8, 2);
         int var22 = (int)((w + var21) * var17 - var21);
         int var23 = Math.max(var22, x);
         int var24 = Math.min(var22 + var21, x + w);
         if (var23 < var24) {
            context.fill(var23, y, var24, y + h, 747621375);
         }

         MinecraftClient client = MinecraftClient.getInstance();
         int textColor = hovered ? -1 : -536870913;
         context.drawCenteredTextWithShadow(client.textRenderer, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
         ci.cancel();
      }
   }

   @Inject(
      method = {"playDownSound(Lnet/minecraft/client/sound/SoundManager;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void luminous$playMechanicalSound(SoundManager var1, CallbackInfo var2) {
      var1.play(PositionedSoundInstance.master(SoundEvents.BLOCK_LEVER_CLICK, 0.55F, 1.05F));
      var2.cancel();
   }
}
