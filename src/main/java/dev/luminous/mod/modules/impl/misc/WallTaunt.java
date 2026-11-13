package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;

public class WallTaunt extends Module {
   private static final String DEFAULT_PUBLIC = "\u68c0\u6d4b\u5230{player}\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11";
   private static final String DEFAULT_PRIVATE = "\u68c0\u6d4b\u5230\u4f60\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11";
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final SliderSetting range = this.add(new SliderSetting("Range", 16.0, 1.0, 128.0, 1.0));
   private final SliderSetting sendDelay = this.add(new SliderSetting("SendDelay", 1.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final SliderSetting suffixCount = this.add(new SliderSetting("SuffixCount", 0.0, 0.0, 20.0, 1.0));
   private final BooleanSetting ignoreFriends = this.add(new BooleanSetting("IgnoreFriends", false));
   private final BooleanSetting privateMode = this.add(new BooleanSetting("PrivateMode", false));
   private final StringSetting customMessage = this.add(
      new StringSetting("CustomMessage", "\u68c0\u6d4b\u5230{player}\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11")
   );
   private final StringSetting privateMessage = this.add(
      new StringSetting("PrivateMessage", "\u68c0\u6d4b\u5230\u4f60\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11")
   );
   private final Random random = new Random();
   private final Set<String> insidePlayers = new HashSet<>();
   private final ArrayDeque<WallTaunt.Pending> pending = new ArrayDeque<>();
   private long nextSendAt;

   public WallTaunt() {
      super("WallTaunt", Module.Category.Misc);
      this.range.setChinese("\u8303\u56f4");
      this.sendDelay.setChinese("\u81ea\u5b9a\u4e49\u53d1\u9001\u5ef6\u8fdf");
      this.suffixCount.setChinese("\u540e\u7f00");
      this.privateMode.setChinese("\u79c1\u4fe1\u6a21\u5f0f");
      this.customMessage.setChinese("\u81ea\u5b9a\u4e49\u5632\u8bbd\u8bed");
      this.privateMessage.setChinese("\u79c1\u4fe1\u81ea\u5b9a\u4e49\u5632\u8bbd\u8bed");
      this.setChinese("\u5361\u5899\u5632\u8bbd");
      this.ignoreFriends.setChinese("\u5ffd\u7565\u597d\u53cb");
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null && mc.player.networkHandler != null) {
         long var2 = System.currentTimeMillis();
         this.flushQueue(var2);
         HashSet var4 = new HashSet();

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            if (var6 != null && var6 != mc.player && var6.isAlive() && !var6.isDead() && !(mc.player.distanceTo(var6) > this.range.getValue())) {
               String var7 = var6.getGameProfile().getName();
               if (var7 != null && !var7.trim().isEmpty()) {
                  boolean var8 = Alien.FRIEND.isFriend(var6);
                  if ((!var8 || !this.ignoreFriends.getValue()) && this.isInsideBlock(var6)) {
                     String var9 = var6.getUuid().toString();
                     var4.add(var9);
                     if (!this.insidePlayers.contains(var9)) {
                        this.enqueue(var7, var8, this.privateMode.getValue(), var2);
                     }
                  }
               }
            }
         }

         this.insidePlayers.retainAll(var4);
         this.insidePlayers.addAll(var4);
      }
   }

   private boolean isInsideBlock(PlayerEntity var1) {
      return BlockUtil.canCollide(var1, var1.getBoundingBox().expand(0.05));
   }

   private void enqueue(String var1, boolean var2, boolean var3, long var4) {
      long var6 = Math.max(0L, Math.round((var3 ? this.sendDelay.getValue() : this.sendDelay.getValue()) * 1000.0));
      long var8 = Math.max(var4, this.nextSendAt);
      this.pending.addLast(new WallTaunt.Pending(var1, var2, var3, var8));
      this.nextSendAt = var8 + var6;
   }

   private void flushQueue(long var1) {
      while (!this.pending.isEmpty() && this.pending.peekFirst().due <= var1) {
         WallTaunt.Pending var3 = this.pending.removeFirst();
         this.sendTaunt(var3.player, var3.friend, var3.whisper);
      }
   }

   private void sendTaunt(String var1, boolean var2, boolean var3) {
      ClientPlayNetworkHandler var4 = mc.player == null ? null : mc.player.networkHandler;
      if (var4 != null) {
         String var5 = var3 ? this.privateMessage.getValue() : this.customMessage.getValue();
         if (var5 == null || var5.trim().isEmpty()) {
            var5 = var3
               ? "\u68c0\u6d4b\u5230\u4f60\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11"
               : "\u68c0\u6d4b\u5230{player}\u5728\u5361\u5899\uff0c\u6211\u5373\u5c06\u5927\u7b11";
         }

         String var6 = var2 && !var3 ? "\u597d\u53cb" + var1 : var1;
         var5 = var5.replace("{player}", var6);
         if (!var3 && !var5.contains(var6) && !var5.contains(var1)) {
            var5 = var6 + " " + var5;
         }

         int var7 = this.suffixCount.getValueInt();
         if (var7 > 0) {
            var5 = var5 + " " + this.randomString(var7);
         }

         if (var3) {
            var4.sendCommand("w " + var1 + " " + var5);
         } else {
            var4.sendChatMessage(var5);
         }
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
      this.insidePlayers.clear();
      this.pending.clear();
      this.nextSendAt = 0L;
   }

   private record Pending(String player, boolean friend, boolean whisper, long due) {
   }
}
