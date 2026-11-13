package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class TeammateTaunt extends Module {
   private static final String DEFAULT_MESSAGE = "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253pop\u4e86";
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final StringSetting customMessage = this.add(
      new StringSetting("CustomMessage", "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253pop\u4e86")
   );
   private final SliderSetting range = this.add(new SliderSetting("Range", 16.0, 1.0, 128.0, 1.0));
   private final SliderSetting sendDelay = this.add(new SliderSetting("SendDelay", 0.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final SliderSetting suffixCount = this.add(new SliderSetting("SuffixCount", 0.0, 0.0, 20.0, 1.0));
   private final BooleanSetting privateMode = this.add(new BooleanSetting("PrivateMode", false));
   private final SliderSetting privateDelay = this.add(new SliderSetting("PrivateDelay", 0.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final Random random = new Random();
   private final ArrayDeque<TeammateTaunt.Pending> pending = new ArrayDeque<>();
   private long nextPublicSendAt;
   private long nextPrivateSendAt;

   public TeammateTaunt() {
      super("TeammateTaunt", Module.Category.Misc);
      this.customMessage.setChinese("\u81ea\u5b9a\u4e49\u5632\u8bbd\u8bed");
      this.range.setChinese("\u8303\u56f4");
      this.sendDelay.setChinese("\u53d1\u9001\u5ef6\u8fdf");
      this.suffixCount.setChinese("\u540e\u7f00\u6570\u91cf");
      this.privateMode.setChinese("\u79c1\u804a\u6a21\u5f0f");
      this.privateDelay.setChinese("\u79c1\u4fe1\u5ef6\u8fdf");
      this.setChinese("\u5632\u8bbd\u961f\u53cb");
   }

   @EventListener
   public void onTotem(TotemEvent var1) {
      PlayerEntity var2 = var1.getPlayer();
      if (var2 != null && mc.player != null && mc.world != null && var2 != mc.player && Alien.FRIEND.isFriend(var2)) {
         if (!(mc.player.distanceTo(var2) > this.range.getValueFloat())) {
            String var3 = var2.getName().getString();
            boolean var4 = this.privateMode.getValue();
            this.pending.addLast(new TeammateTaunt.Pending(var3, var4));
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      long var2 = System.currentTimeMillis();
      Iterator var4 = this.pending.iterator();

      while (var4.hasNext()) {
         TeammateTaunt.Pending var5 = (TeammateTaunt.Pending)var4.next();
         long var6 = var5.whisper ? this.nextPrivateSendAt : this.nextPublicSendAt;
         if (var2 >= var6) {
            if (this.sendTaunt(var5.player, var5.whisper)) {
               var4.remove();
               long var8 = (long)((var5.whisper ? this.privateDelay.getValue() : this.sendDelay.getValue()) * 1000.0);
               if (var5.whisper) {
                  this.nextPrivateSendAt = var2 + var8;
               } else {
                  this.nextPublicSendAt = var2 + var8;
               }
            }

            return;
         }
      }
   }

   private boolean sendTaunt(String var1, boolean var2) {
      if (mc.getNetworkHandler() != null && var1 != null && !var1.isEmpty()) {
         String var4 = this.customMessage.getValue();
         if (var4 == null || var4.trim().isEmpty()) {
            var4 = "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253pop\u4e86";
         }

         int var3;
         if ((var3 = this.suffixCount.getValueInt()) > 0) {
            var4 = var4 + " " + this.randomString(var3);
         }

         if (var2) {
            mc.getNetworkHandler().sendCommand("w " + var1 + " " + var4);
         } else {
            mc.getNetworkHandler().sendChatMessage(var1.concat(" ").concat(var4));
         }

         return true;
      } else {
         return false;
      }
   }

   private String randomString(int var1) {
      StringBuilder var2 = new StringBuilder(var1);

      for (int var3 = 0; var3 < var1; var3++) {
         var2.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length()))
         );
      }

      return var2.toString();
   }

   @Override
   public void onDisable() {
      this.pending.clear();
      this.nextPublicSendAt = 0L;
      this.nextPrivateSendAt = 0L;
   }

   private record Pending(String player, boolean whisper) {
   }
}
