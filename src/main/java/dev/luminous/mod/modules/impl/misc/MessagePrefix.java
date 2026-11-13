package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

public final class MessagePrefix extends Module {
   public static MessagePrefix INSTANCE;
   private final StringSetting customPrefix = this.add(new StringSetting("CustomPrefix", "^"));
   private boolean rewriting;

   public MessagePrefix() {
      super("MessagePrefix", Module.Category.Misc);
      this.setChinese("\u6d88\u606f\u524d\u7f00");
      this.customPrefix.setChinese("\u81ea\u5b9a\u4e49\u524d\u7f00");
      INSTANCE = this;
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send var1) {
      if (!this.rewriting && !Module.nullCheck()) {
         Packet var2 = var1.getPacket();
         if (!(var2 instanceof ChatMessageC2SPacket)) {
            return;
         }

         ChatMessageC2SPacket var3 = (ChatMessageC2SPacket)var2;
         String var4 = var3.chatMessage();
         if (var4 != null && !var4.isBlank() && !var4.startsWith("/")) {
            String var5 = this.customPrefix.getValue();
            if (var5 != null && !var5.isBlank() && !var4.startsWith(var5)) {
               var1.cancel();
               this.rewriting = true;

               try {
                  Wrapper.mc.player.networkHandler.sendChatMessage(var5 + var4);
               } finally {
                  this.rewriting = false;
               }

               return;
            }
         }
      }
   }
}
