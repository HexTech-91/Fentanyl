package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.entity.player.PlayerEntity;

public class PopEz extends Module {
   public static PopEz INSTANCE;
   private final SliderSetting randomChars = this.add(new SliderSetting("RandomChars", 3.0, 0.0, 20.0, 1.0));
   private final BooleanSetting privateMode = this.add(new BooleanSetting("PrivateMode", false));
   public final BooleanSetting slowSend;
   public final SliderSetting slowSendDelay;
   public final BooleanSetting enableCustomMsg;
   public final StringSetting customMsgFormat;
   private final Random random;
   private final Timer timer;
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final Map<Integer, Integer> popQueue;

   public PopEz() {
      super("PopEz", Module.Category.Misc);
      this.randomChars.setChinese("\u968f\u673a\u5b57\u7b26");
      this.privateMode.setChinese("\u79c1\u4fe1\u6a21\u5f0f");
      this.slowSend = this.add(new BooleanSetting("SlowSend", false));
      this.slowSend.setChinese("\u6162\u901f\u53d1\u9001");
      this.slowSendDelay = this.add(new SliderSetting("SlowSendDelay", 3.2, 3.0, 10.0, 0.1).setSuffix("s"));
      this.slowSendDelay.setChinese("\u6162\u901f\u53d1\u9001\u5ef6\u8fdf");
      this.enableCustomMsg = this.add(new BooleanSetting("EnableCustomMsg", true));
      this.enableCustomMsg.setChinese("\u542f\u7528\u81ea\u5b9a\u4e49\u6d88\u606f");
      this.customMsgFormat = this.add(new StringSetting("CustomMsgFormat", "{player}\u7206\u4e86{count}\u4e2a\u56fe\u817e"));
      this.customMsgFormat.setChinese("\u81ea\u5b9a\u4e49\u6d88\u606f");
      this.random = new Random();
      this.timer = new Timer();
      this.popQueue = new HashMap<>();
      this.setChinese("Pop\u5632\u8bbd");
      INSTANCE = this;
   }

   @EventListener
   public void onTotem(TotemEvent var1) {
      PlayerEntity var2 = var1.getPlayer();
      if (var2 != null && !var2.equals(mc.player) && !Alien.FRIEND.isFriend(var2)) {
         int var3 = 1;
         String var4 = var2.getName().getString();
         if (Alien.POP.popContainer.containsKey(var4)) {
            var3 = Alien.POP.popContainer.get(var4);
         }

         if (this.slowSend.getValue()) {
            this.popQueue.put(var2.getId(), var3);
         } else {
            String var5 = this.buildMessage(var2, var3);
            this.sendChatMessage(var4, var5);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (this.slowSend.getValue() && !this.popQueue.isEmpty() && this.timer.passedS(this.slowSendDelay.getValue())) {
         this.timer.reset();
         Entry var2 = this.popQueue.entrySet().iterator().next();
         int var3 = (Integer)var2.getKey();
         int var4 = (Integer)var2.getValue();
         PlayerEntity var5 = null;

         for (PlayerEntity var7 : mc.world.getPlayers()) {
            if (var7.getId() == var3) {
               var5 = var7;
               break;
            }
         }

         if (var5 != null) {
            String var8 = this.buildMessage(var5, var4);
            this.sendChatMessage(var5.getName().getString(), var8);
         }

         this.popQueue.remove(var3);
      }
   }

   private String buildMessage(PlayerEntity var1, int var2) {
      if (this.enableCustomMsg.getValue()) {
         String var3 = this.customMsgFormat.getValue();
         return var3.replace("{player}", var1.getName().getString()).replace("{count}", String.valueOf(var2));
      } else {
         return var1.getName().getString() + " has popped " + var2 + " totem" + (var2 == 1 ? "" : "s") + ".";
      }
   }

   private void sendChatMessage(String var1, String var2) {
      if (mc.player != null && mc.player.networkHandler != null) {
         String var3 = this.generateRandomString(this.randomChars.getValueInt());
         if (!var3.isEmpty()) {
            var2 = var2 + " " + var3;
         }

         if (this.privateMode.getValue() && var1 != null && !var1.isEmpty()) {
            mc.player.networkHandler.sendCommand("w " + var1 + " " + var2);
         } else {
            mc.player.networkHandler.sendChatMessage(var2);
         }
      }
   }

   private String generateRandomString(int var1) {
      if (var1 <= 0) {
         return "";
      } else {
         StringBuilder var2 = new StringBuilder(var1);

         for (int var3 = 0; var3 < var1; var3++) {
            int var4 = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
            var2.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(var4));
         }

         return var2.toString();
      }
   }

   @Override
   public void onDisable() {
      this.popQueue.clear();
   }
}
