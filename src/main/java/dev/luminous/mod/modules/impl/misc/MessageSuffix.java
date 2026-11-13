package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

public final class MessageSuffix extends Module {
   public static MessageSuffix INSTANCE;
   private final StringSetting customSuffix = this.add(new StringSetting("CustomSuffix", "|\u5b99\u65afClient"));
   private boolean rewriting;

   public MessageSuffix() {
      super("MessageSuffix", Module.Category.Misc);
      this.setChinese("\u6d88\u606f\u540e\u7f00");
      this.customSuffix.setChinese("\u81ea\u5b9a\u4e49\u540e\u7f00");
      INSTANCE = this;
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send var1) {
      if (!this.rewriting && !nullCheck()) {
         if (var1.getPacket() instanceof ChatMessageC2SPacket var3) {
            String var4 = var3.chatMessage();
            if (var4 != null && !var4.isBlank() && !var4.startsWith("/")) {
               String var5 = this.customSuffix.getValue();
               if (var5 != null && !var5.isBlank()) {
                  String var6 = " " + var5;
                  if (!var4.endsWith(var6) && !var4.endsWith(var5)) {
                     var1.cancel();
                     this.rewriting = true;

                     try {
                        mc.player.networkHandler.sendChatMessage(var4 + var6);
                     } finally {
                        this.rewriting = false;
                     }
                  }
               }
            }
         }
      }
   }
}
