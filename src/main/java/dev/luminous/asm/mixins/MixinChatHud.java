package dev.luminous.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import dev.luminous.api.interfaces.IChatHudHook;
import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.asm.accessors.IChatHud;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.misc.ChatCensor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public abstract class MixinChatHud implements IChatHudHook {
   @Unique
   private int nextMessageId = 0;
   @Unique
   private boolean nextSync;
   @Unique
   private int chatLineIndex;
   @Final
   @Shadow
   private List<Visible> visibleMessages;
   @Shadow
   @Final
   private List<ChatHudLine> messages;

   @Inject(
      method = {"<init>(Lnet/minecraft/client/MinecraftClient;)V"},
      at = {@At("TAIL")}
   )
   public void onInit(MinecraftClient var1, CallbackInfo var2) {
      ((IChatHud)this).setMessages(new CopyOnWriteArrayList<>());
      ((IChatHud)this).setVisibleMessages(new CopyOnWriteArrayList<>());
   }

   @Override
   public void alienClient$addMessage(Text var1, int var2) {
      this.nextMessageId = var2;
      this.nextSync = true;
      this.addMessage(var1);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   @Override
   public void alienClient$addMessage(Text var1) {
      this.nextSync = true;
      this.addMessage(var1);
      this.nextSync = false;
   }

   @Override
   public void alienClient$addMessageOutSync(Text var1, int var2) {
      this.nextMessageId = var2;
      this.addMessage(var1);
      this.nextMessageId = 0;
   }

   @Inject(
      method = {"addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine var1, CallbackInfo var2) {
      IChatHudLineHook var3 = (IChatHudLineHook)(Object)this.visibleMessages.getFirst();
      if (var3 != null) {
         var3.alienClient$setMessageId(this.nextMessageId);
         var3.alienClient$setSync(this.nextSync);
         var3.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLine(ChatHudLine var1, CallbackInfo var2) {
      IChatHudLineHook var3 = (IChatHudLineHook)(Object)this.messages.getFirst();
      if (var3 != null) {
         var3.alienClient$setMessageId(this.nextMessageId);
         var3.alienClient$setSync(this.nextSync);
         var3.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}
   )
   private void onAddMessage(Text var1, MessageSignatureData var2, MessageIndicator var3, CallbackInfo var4) {
      if (this.nextMessageId != 0) {
         this.visibleMessages.removeIf(var1x -> ((IChatHudLineHook)(Object)var1x).alienClient$getMessageId() == this.nextMessageId);
         this.messages.removeIf(var1x -> ((IChatHudLineHook)(Object)var1x).alienClient$getMessageId() == this.nextMessageId);
      }
   }

   @Redirect(
      method = {"addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;size()I",
         ordinal = 2,
         remap = false
      ),
      require = 0
   )
   public int chatLinesSize(List<Visible> var1) {
      return ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? -2147483647 : var1.size();
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
      ),
      require = 0
   )
   private int drawStringWithShadow(DrawContext var1, TextRenderer var2, OrderedText var3, int var4, int var5, int var6) {
      IChatHudLineHook var7 = (IChatHudLineHook)(Object)this.visibleMessages.get(this.chatLineIndex);
      if (var7 != null) {
         FadeUtils var8 = var7.alienClient$getFade();
         double var9 = var8 == null ? 0.0 : var8.ease(ClientSetting.INSTANCE.ease.getValue());
         double var11 = 1.0 - var9;
         var4 += (int)(var11 * ClientSetting.INSTANCE.animateOffset.getValue());
         double var13 = Math.max(10.0, (var6 >> 24 & 0xFF) * var9);
         return var7.alienClient$getSync()
            ? var1.drawTextWithShadow(
               var2,
               var3,
               var4,
               var5,
               ColorUtil.injectAlpha(ClientSetting.INSTANCE.color.getValue(), ClientSetting.INSTANCE.fade.getValue() ? (int)var13 : var6 >> 24 & 0xFF).getRGB()
            )
            : var1.drawTextWithShadow(
               var2, var3, var4, var5, ColorUtil.injectAlpha(var6, ClientSetting.INSTANCE.fade.getValue() ? (int)var13 : var6 >> 24 & 0xFF)
            );
      } else {
         return var1.drawTextWithShadow(var2, var3, var4, var5, var6);
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I"
      )}
   )
   public void getChatLineIndex(CallbackInfo var1, @Local(ordinal = 13) int var2) {
      this.chatLineIndex = var2;
   }

   @ModifyVariable(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = @At("STORE")
   )
   private MessageIndicator removeMessageIndicator(MessageIndicator var1) {
      return ClientSetting.INSTANCE.hideIndicator.getValue() ? null : var1;
   }

   @Shadow
   public abstract void addMessage(Text var1);

   @Shadow
   public int getWidth() {
      return 0;
   }

   @ModifyVariable(
      method = {"addMessage(Lnet/minecraft/text/Text;)V"},
      at = @At("HEAD"),
      index = 1,
      argsOnly = true
   )
   private Text alienClient$colorFriendName(Text var1) {
      return ChatCensor.censorText(var1);
   }
}
