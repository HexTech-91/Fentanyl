package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;
   private final BooleanSetting addMessageEnabled = this.add(new BooleanSetting("AddMessageEnabled", false));
   private final StringSetting addMessage;
   private final BooleanSetting removeMessageEnabled;
   private final StringSetting removeMessage;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.addMessageEnabled.setChinese("\u6dfb\u52a0\u65f6\u53d1\u9001\u79c1\u4fe1");
      this.addMessage = this.add(new StringSetting("AddMessage", ""));
      this.addMessage.setChinese("\u6dfb\u52a0\u79c1\u4fe1\u5185\u5bb9");
      this.removeMessageEnabled = this.add(new BooleanSetting("RemoveMessageEnabled", false));
      this.removeMessageEnabled.setChinese("\u5220\u9664\u65f6\u53d1\u9001\u79c1\u4fe1");
      this.removeMessage = this.add(new StringSetting("RemoveMessage", ""));
      this.removeMessage.setChinese("\u5220\u9664\u79c1\u4fe1\u5185\u5bb9");
      this.setChinese("\u597d\u53cb");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
            String name = player.getGameProfile().getName();
            boolean wasFriend = Alien.FRIEND.isFriend(name);
            Alien.FRIEND.friend(player);
            if (!wasFriend && this.addMessageEnabled.getValue()) {
               String msg = this.addMessage.getValue().trim();
               if (!msg.isEmpty()) {
                  mc.getNetworkHandler().sendCommand("w " + name + " " + msg);
               }
            } else if (wasFriend && this.removeMessageEnabled.getValue()) {
               String msg = this.removeMessage.getValue().trim();
               if (!msg.isEmpty()) {
                  mc.getNetworkHandler().sendCommand("w " + name + " " + msg);
               }
            }
         }

         this.disable();
      }
   }
}
