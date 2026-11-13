package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.mod.commands.Command;
import java.util.List;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class PingCommand extends Command {
   private long sendTime;

   public PingCommand() {
      super("ping", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      this.sendTime = System.currentTimeMillis();
      mc.getNetworkHandler().sendChatCommand("chat ");
      Alien.EVENT_BUS.subscribe(this);
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof GameMessageS2CPacket packet
         && (
            packet.content().getString().contains("chat.use")
               || packet.content().getString().contains("\u547d\u4ee4")
               || packet.content().getString().contains("Bad command")
               || packet.content().getString().contains("No such command")
               || packet.content().getString().contains("<--[HERE]")
               || packet.content().getString().contains("Unknown")
               || packet.content().getString().contains("\u5e2e\u52a9")
               || packet.content().getString().contains("\u6267\u884c\u9519\u8bef")
         )) {
         this.sendChatMessage("ping: " + (System.currentTimeMillis() - this.sendTime) + "ms");
         Alien.EVENT_BUS.unsubscribe(this);
      }
   }
}
