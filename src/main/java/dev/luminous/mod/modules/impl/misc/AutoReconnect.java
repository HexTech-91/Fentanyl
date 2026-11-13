package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.ServerConnectBeginEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import java.util.HashMap;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;

public class AutoReconnect extends Module {
   public static AutoReconnect INSTANCE;
   public final BooleanSetting rejoin = this.add(new BooleanSetting("Rejoin", true));
   public final SliderSetting delay;
   public final BooleanSetting autoLogin;
   public final SliderSetting afterLoginTime;
   public final BooleanSetting autoQueue;
   public final SliderSetting joinQueueDelay;
   final StringSetting password;
   public final BooleanSetting autoAnswer;
   public static boolean inQueueServer;
   private final Timer queueTimer;
   private final Timer timer;
   public Pair<ServerAddress, ServerInfo> lastServerConnection;
   private boolean login;
   final String[] abc;
   public static final HashMap<String, String> asks = new HashMap<String, String>() {
      {
         this.put("\u7ea2\u77f3\u706b\u628a", "15");
         this.put("\u732a\u88ab\u95ea\u7535", "\u50f5\u5c38\u732a\u4eba");
         this.put("\u5c0f\u7bb1\u5b50\u80fd", "27");
         this.put("\u5f00\u670d\u5e74\u4efd", "2020");
         this.put("\u5b9a\u4f4d\u672b\u5730\u9057\u8ff9", "0");
         this.put("\u722c\u884c\u8005\u88ab\u95ea\u7535", "\u9ad8\u538b\u722c\u884c\u8005");
         this.put("\u5927\u7bb1\u5b50\u80fd", "54");
         this.put("\u7f8a\u9a7c\u4f1a\u4e3b\u52a8", "\u4e0d\u4f1a");
         this.put("\u65e0\u9650\u6c34", "3");
         this.put("\u6316\u6398\u901f\u5ea6\u6700\u5feb", "\u91d1\u9550");
         this.put("\u51cb\u7075\u6b7b\u540e", "\u4e0b\u754c\u4e4b\u661f");
         this.put("\u82e6\u529b\u6015\u7684\u5b98\u65b9", "\u722c\u884c\u8005");
         this.put("\u5357\u74dc\u7684\u751f\u957f", "\u4e0d\u9700\u8981");
         this.put("\u5b9a\u4f4d\u672b\u5730", "0");
      }
   };

   public AutoReconnect() {
      super("AutoReconnect", Module.Category.Misc);
      this.rejoin.setChinese("\u91cd\u8fde");
      this.delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 20.0, 0.1).setSuffix("s"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.autoLogin = this.add(new BooleanSetting("AutoLogin", true));
      this.autoLogin.setChinese("\u81ea\u52a8\u767b\u5f55");
      this.afterLoginTime = this.add(new SliderSetting("AfterLoginTime", 3.0, 0.0, 10.0, 0.1).setSuffix("s"));
      this.afterLoginTime.setChinese("\u767b\u5f55\u540e\u7b49\u5f85");
      this.autoQueue = this.add(new BooleanSetting("AutoQueue", true));
      this.autoQueue.setChinese("\u81ea\u52a8\u6392\u961f");
      this.joinQueueDelay = this.add(new SliderSetting("JoinQueueDelay", 3.0, 0.0, 10.0, 0.1).setSuffix("s"));
      this.joinQueueDelay.setChinese("\u6392\u961f\u5ef6\u8fdf");
      this.password = this.add(new StringSetting("Password", "123456"));
      this.password.setChinese("\u5bc6\u7801");
      this.autoAnswer = this.add(new BooleanSetting("AutoAnswer", true));
      this.autoAnswer.setChinese("\u81ea\u52a8\u56de\u7b54");
      this.queueTimer = new Timer();
      this.timer = new Timer();
      this.login = false;
      this.abc = new String[]{"A", "B", "C"};
      this.setChinese("\u81ea\u52a8\u91cd\u8fde");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoReconnect.StaticListener());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.login && this.timer.passedS(this.afterLoginTime.getValue())) {
         mc.getNetworkHandler().sendChatCommand("login " + this.password.getValue());
         this.login = false;
      }

      if (this.autoQueue.getValue() && InventoryUtil.findItem(Items.COMPASS) != -1 && this.queueTimer.passedS(this.joinQueueDelay.getValue())) {
         InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.COMPASS));
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         this.queueTimer.reset();
      }

      if (nullCheck()) {
         inQueueServer = false;
      } else {
         inQueueServer = InventoryUtil.findItem(Items.COMPASS) != -1;
      }
   }

   @Override
   public void onLogin() {
      if (this.autoLogin.getValue()) {
         this.login = true;
         this.timer.reset();
      }
   }

   public boolean rejoin() {
      return this.isOn() && this.rejoin.getValue() && !AutoLog.loggedOut;
   }

   @Override
   public void onLogout() {
      inQueueServer = false;
   }

   @Override
   public void onDisable() {
      inQueueServer = false;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!nullCheck() && this.autoAnswer.getValue() && inQueueServer && e.getPacket() instanceof GameMessageS2CPacket packet) {
         for (String key : asks.keySet()) {
            if (packet.content().getString().contains(key)) {
               for (String s : this.abc) {
                  if (packet.content().getString().contains(s + "." + asks.get(key))) {
                     mc.getNetworkHandler().sendChatMessage(s.toLowerCase());
                     return;
                  }
               }
            }
         }
      }
   }

   private class StaticListener {
      @EventListener
      private void onGameJoined(ServerConnectBeginEvent event) {
         AutoReconnect.this.lastServerConnection = new ObjectObjectImmutablePair(event.address, event.info);
      }
   }
}
