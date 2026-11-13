package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.handler.PacketEncoderException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class MixinClientConnection {
   @Inject(
      at = {@At("HEAD")},
      method = {"handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V"},
      cancellable = true
   )
   private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      PacketEvent.Receive event = new PacketEvent.Receive(packet);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      PacketEvent.Send event = new PacketEvent.Send(packet);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("RETURN")}
   )
   private void onSendPacketPost(Packet<?> packet, CallbackInfo info) {
      PacketEvent.Sent event = new PacketEvent.Sent(packet);
      Alien.EVENT_BUS.post(event);
   }

   @Inject(
      method = {"exceptionCaught(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
      if (isMalformedPortedSpearChat(throwable)) {
         ci.cancel();
      } else if (!(throwable instanceof TimeoutException)
         && !(throwable instanceof PacketEncoderException)
         && ClientSetting.INSTANCE.caughtException.getValue()) {
         if (ClientSetting.INSTANCE.log.getValue()) {
            CommandManager.sendMessage("\u00a74Caught exception \u00a77" + throwable.getMessage());
         }

         ci.cancel();
      }
   }

   private static boolean isMalformedPortedSpearChat(Throwable throwable) {
      if (!(throwable instanceof DecoderException)) {
         return false;
      } else {
         boolean var1 = false;
         boolean var2 = false;

         for (Throwable var3 = throwable; var3 != null; var3 = var3.getCause()) {
            String var4 = var3.getMessage();
            if (var4 != null) {
               var1 |= var4.contains("clientbound/minecraft:system_chat");
               var2 |= var4.contains("minecraft:lunge") || var4.contains("was speared") || var4.contains("spear");
            }
         }

         return var1 && var2;
      }
   }
}
