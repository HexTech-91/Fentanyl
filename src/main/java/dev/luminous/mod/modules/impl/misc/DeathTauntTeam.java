package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayDeque;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class DeathTauntTeam extends Module {
   private static final String DEFAULT_MESSAGE = "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253\u6b7b\u4e86";
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final StringSetting customMessage = this.add(
      new StringSetting("CustomMessage", "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253\u6b7b\u4e86")
   );
   private final SliderSetting range = this.add(new SliderSetting("Range", 16.0, 1.0, 128.0, 1.0));
   private final SliderSetting sendDelay = this.add(new SliderSetting("SendDelay", 1.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final SliderSetting suffixCount = this.add(new SliderSetting("SuffixCount", 0.0, 0.0, 20.0, 1.0));
   private final BooleanSetting privateMode = this.add(new BooleanSetting("PrivateMode", false));
   private final SliderSetting privateDelay = this.add(new SliderSetting("PrivateDelay", 1.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final Random random = new Random();
   private final ArrayDeque<DeathTauntTeam.Pending> publicQueue = new ArrayDeque<>();
   private final ArrayDeque<DeathTauntTeam.Pending> privateQueue = new ArrayDeque<>();
   private long nextPublicSendAt;
   private long nextPrivateSendAt;

   public DeathTauntTeam() {
      super("DeathTauntTeam", Module.Category.Misc);
      this.customMessage.setChinese("\u81ea\u5b9a\u4e49\u5632\u8bbd\u8bed");
      this.range.setChinese("\u8303\u56f4");
      this.sendDelay.setChinese("\u53d1\u9001\u5ef6\u8fdf");
      this.suffixCount.setChinese("\u540e\u7f00\u6570\u91cf");
      this.privateMode.setChinese("\u79c1\u804a\u6a21\u5f0f");
      this.privateDelay.setChinese("\u79c1\u4fe1\u5ef6\u8fdf");
      this.setChinese("\u6b7b\u4ea1\u5632\u8bbd\u961f\u53cb");
   }

   @EventListener
   public void onDeath(DeathEvent var1) {
      PlayerEntity var2 = var1.getPlayer();
      if (var2 != null && mc.player != null && mc.world != null && var2 != mc.player && Alien.FRIEND.isFriend(var2)) {
         if (!(mc.player.distanceTo(var2) > this.range.getValue())) {
            String var3 = var2.getName().getString();
            if (var3 != null && !var3.trim().isEmpty()) {
               long var4 = System.currentTimeMillis();
               if (this.privateMode.getValue()) {
                  long var6 = this.delayMillis(this.privateDelay.getValue());
                  long var8 = Math.max(var4, this.nextPrivateSendAt);
                  this.privateQueue.addLast(new DeathTauntTeam.Pending(var3, var8));
                  this.nextPrivateSendAt = var8 + var6;
               } else {
                  long var10 = this.delayMillis(this.sendDelay.getValue());
                  long var11 = Math.max(var4, this.nextPublicSendAt);
                  this.publicQueue.addLast(new DeathTauntTeam.Pending(var3, var11));
                  this.nextPublicSendAt = var11 + var10;
               }
            }
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      long var2 = System.currentTimeMillis();
      if (!this.publicQueue.isEmpty() && this.publicQueue.peekFirst().due <= var2) {
         this.sendTaunt(this.publicQueue.removeFirst().player, false);
      }

      if (!this.privateQueue.isEmpty() && this.privateQueue.peekFirst().due <= var2) {
         this.sendTaunt(this.privateQueue.removeFirst().player, true);
      }
   }

   private void sendTaunt(String var1, boolean var2) {
      if (mc.getNetworkHandler() != null) {
         String var3 = this.customMessage.getValue();
         if (var3 == null || var3.trim().isEmpty()) {
            var3 = "\u961f\u53cb\u4f60\u662f\u5e9f\u7269\u5417\u600e\u4e48\u88ab\u6253\u6b7b\u4e86";
         }

         int var4 = this.suffixCount.getValueInt();
         if (var4 > 0) {
            var3 = var3 + " " + this.randomString(var4);
         }

         if (var2) {
            mc.getNetworkHandler().sendCommand("w " + var1 + " " + var3);
         } else {
            mc.getNetworkHandler().sendChatMessage(var1 + " " + var3);
         }
      }
   }

   private long delayMillis(double var1) {
      return Math.max(0L, Math.round(var1 * 1000.0));
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
      this.publicQueue.clear();
      this.privateQueue.clear();
      this.nextPublicSendAt = 0L;
      this.nextPrivateSendAt = 0L;
   }

   private record Pending(String player, long due) {
   }
}
