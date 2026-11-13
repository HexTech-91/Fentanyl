package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.network.PlayerListEntry;

public class Spammer extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   public final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", false));
   final StringSetting message;
   private final Random random;
   private final SliderSetting randoms;
   private final SliderSetting delay;
   private final BooleanSetting tellMode;
   private final BooleanSetting autoDisable;
   private final Timer timer;

   public Spammer() {
      super("Spammer", Module.Category.Misc);
      this.checkSelf.setChinese("\u68c0\u67e5\u81ea\u5df1");
      this.message = this.add(
         new StringSetting(
            "\u6d88\u606f",
            "\u6700\u5f3a\u5916\u6302Alien\u793e\u533a\u7248\u514d\u8d39\u8bd5\u7528 \u7fa4\ud835\udff1\ud835\udff4\ud835\udff5\ud835\udfed\ud835\udff5\ud835\udfed\ud835\udff1\ud835\udff2\ud835\udfed"
         )
      );
      this.random = new Random();
      this.randoms = this.add(new SliderSetting("Randoms", 3.0, 0.0, 20.0, 1.0));
      this.randoms.setChinese("\u968f\u673a\u5b57\u7b26");
      this.delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 60.0, 0.1).setSuffix("s"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.tellMode = this.add(new BooleanSetting("TellMode", false));
      this.tellMode.setChinese("\u968f\u673a\u79c1\u804a");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.timer = new Timer();
      this.setChinese("\u81ea\u52a8\u5237\u5c4f");
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedS(this.delay.getValue())) {
         this.timer.reset();
         String randomString = this.generateRandomString(this.randoms.getValueInt());
         if (!randomString.isEmpty()) {
            randomString = " " + randomString;
         }

         if (this.tellMode.getValue()) {
            Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();
            List<PlayerListEntry> list = new ArrayList<>(players);
            int size = list.size();
            if (size == 0) {
               return;
            }

            PlayerListEntry playerListEntry = list.get(this.random.nextInt(size));

            for (int i = 0;
               this.checkSelf.getValue() && Objects.equals(playerListEntry.getProfile().getName(), mc.player.getGameProfile().getName());
               playerListEntry = list.get(this.random.nextInt(size))
            ) {
               if (i > 50) {
                  return;
               }

               i++;
            }

            mc.getNetworkHandler().sendChatCommand("tell " + playerListEntry.getProfile().getName() + " " + this.message.getValue() + randomString);
         } else if (this.message.getValue().startsWith("/")) {
            mc.getNetworkHandler().sendCommand(this.message.getValue().replaceFirst("/", "") + randomString);
         } else {
            mc.getNetworkHandler().sendChatMessage(this.message.getValue() + randomString);
         }
      }
   }

   private String generateRandomString(int LENGTH) {
      StringBuilder sb = new StringBuilder(LENGTH);

      for (int i = 0; i < LENGTH; i++) {
         int index = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(index));
      }

      return sb.toString();
   }
}
