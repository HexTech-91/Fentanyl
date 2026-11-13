package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.Vec3d;

public class AutoHelp extends Module {
   private final Timer timer = new Timer();
   private final List<String> onlineFriends = new ArrayList<>();
   private final Set<String> sentFriends = new HashSet<>();
   private final StringSetting prefixText = this.add(new StringSetting("PrefixText", "\u6551\u6551\u6211\uff01QWQ"));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 1.0, 10.0, 1.0).setSuffix("\u79d2"));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));

   public AutoHelp() {
      super("AutoHelp", Module.Category.Misc);
      this.setChinese("\u81ea\u52a8\u6c42\u52a9");
      FeatureTranslations.localizeModule(this);
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.sentFriends.clear();
         this.refreshOnlineFriends();
         if (this.onlineFriends.isEmpty()) {
            this.sendMessage("\u6ca1\u6709\u5728\u7ebf\u597d\u53cb");
            if (this.autoDisable.getValue()) {
               this.disable();
            }
         } else {
            this.timer.reset();
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      double distanceFromSpawn = mc.player.getPos().distanceTo(new Vec3d(0.0, 0.0, 0.0));
      if (distanceFromSpawn > 4000.0) {
         this.sendMessage("\u8ddd\u79bb\u51fa\u751f\u70b9\u8d85\u8fc74000\u683c\uff0c\u81ea\u52a8\u5173\u95ed");
         this.disable();
      } else if (this.timer.passedS(this.delay.getValue())) {
         this.timer.reset();
         this.refreshOnlineFriends();
         if (this.onlineFriends.isEmpty()) {
            if (this.autoDisable.getValue()) {
               this.disable();
            }
         } else {
            String coordinates = String.format("%.0f, %.0f, %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            String message = this.prefixText.getValue() + " \u5f53\u524d\u5750\u6807 " + coordinates;
            String friendName = this.onlineFriends.get(0);
            mc.getNetworkHandler().sendChatCommand("tell " + friendName + " " + message);
            this.sentFriends.add(friendName);
         }
      }
   }

   private void refreshOnlineFriends() {
      this.onlineFriends.clear();
      if (mc.getNetworkHandler() != null && mc.player != null) {
         String selfName = mc.player.getGameProfile().getName();

         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (name != null && !name.equals(selfName) && Alien.FRIEND.isFriend(name) && !this.sentFriends.contains(name)) {
               this.onlineFriends.add(name);
            }
         }
      }
   }
}
