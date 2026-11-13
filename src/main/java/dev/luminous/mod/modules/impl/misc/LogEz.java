package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class LogEz extends Module {
   private final StringSetting message = this.add(new StringSetting("Message", "Ezlog %player%"));
   private final SliderSetting range = this.add(new SliderSetting("Range", 12.0, 0.0, 64.0, 0.1));
   private final Map<UUID, LogEz.LoggedPlayer> playerCache = new ConcurrentHashMap<>();
   public static LogEz INSTANCE;

   public LogEz() {
      super("LogEz", Module.Category.Misc);
      this.setChinese("\u9000\u670d\u5632\u8bbd");
      this.message.setChinese("\u6d88\u606f");
      this.range.setChinese("\u8303\u56f4");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         this.playerCache.clear();

         for (PlayerEntity var3 : mc.world.getPlayers()) {
            if (var3 != null && var3 != mc.player) {
               this.playerCache.put(var3.getUuid(), new LogEz.LoggedPlayer(var3.getName().getString(), var3.getPos()));
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive var1) {
      if (!nullCheck() && mc.player != null && mc.player.networkHandler != null) {
         if (var1.getPacket() instanceof PlayerRemoveS2CPacket var2) {
            for (Object var5 : var2.profileIds()) {
               if (var5 instanceof UUID var6) {
                  PlayerEntity var7 = null;

                  for (PlayerEntity var9 : mc.world.getPlayers()) {
                     if (var9.getUuid().equals(var6)) {
                        var7 = var9;
                        break;
                     }
                  }

                  if (var7 == null || var7 != mc.player) {
                     LogEz.LoggedPlayer var16 = this.playerCache.get(var6);
                     String var17 = var7 != null ? var7.getName().getString() : (var16 != null ? var16.name : null);
                     if (var17 != null && (Alien.FRIEND == null || !Alien.FRIEND.isFriend(var17))) {
                        double var10 = Double.MAX_VALUE;
                        if (var7 != null) {
                           var10 = mc.player.getPos().distanceTo(var7.getPos());
                        } else {
                           if (var16 == null) {
                              continue;
                           }

                           var10 = mc.player.getPos().distanceTo(var16.lastPos);
                        }

                        double var12 = this.range.getValue();
                        if (!(var12 > 0.0) || !(Math.sqrt(var10) > var12)) {
                           String var14 = this.message.getValue().replace("%player%", var17).trim();
                           if (!var14.isEmpty()) {
                              mc.player.networkHandler.sendChatMessage(var14);
                           }

                           this.playerCache.remove(var6);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.playerCache.clear();
   }

   @Override
   public void onLogout() {
      this.playerCache.clear();
   }

   private static class LoggedPlayer {
      private final String name;
      private final Vec3d lastPos;

      private LoggedPlayer(String var1, Vec3d var2) {
         this.name = var1;
         this.lastPos = var2;
      }
   }
}
