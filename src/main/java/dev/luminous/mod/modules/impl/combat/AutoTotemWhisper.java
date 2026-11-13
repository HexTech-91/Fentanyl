package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;

public class AutoTotemWhisper extends Module {
   private final BooleanSetting enable = this.add(new BooleanSetting("Enable", true));
   private final SliderSetting popWindow;
   private final SliderSetting popThreshold;
   private final SliderSetting minTotems;
   private final StringSetting targetPlayer;
   private final StringSetting message;
   private final BindSetting panicKey;
   private long lastPopTime;
   private int recentPops;
   private boolean keyWasPressed;

   public AutoTotemWhisper() {
      super(
         "AutoTotemWhisper",
         "\u77ed\u65f6\u95f4\u5185\u8fde\u7eed\u7206\u56fe\u817e\u65f6\u81ea\u52a8\u5411\u6307\u5b9a\u73a9\u5bb6\u53d1\u9001\u79c1\u4fe1\u6c42\u6551",
         Module.Category.Combat
      );
      this.enable.setChinese("\u542f\u7528");
      this.popWindow = this.add(new SliderSetting("PopWindow", 5.0, 1.0, 30.0, 0.5));
      this.popWindow.setChinese("\u7206\u56fe\u817e\u7a97\u53e3");
      this.popThreshold = this.add(new SliderSetting("PopThreshold", 3.0, 1.0, 10.0, 1.0));
      this.popThreshold.setChinese("\u7206\u56fe\u817e\u9608\u503c");
      this.minTotems = this.add(new SliderSetting("MinTotems", 2.0, 1.0, 2.0, 1.0));
      this.minTotems.setChinese("\u6700\u5c11\u56fe\u817e\u6570");
      this.targetPlayer = this.add(new StringSetting("TargetPlayer", ""));
      this.targetPlayer.setChinese("\u76ee\u6807\u73a9\u5bb6");
      this.message = this.add(new StringSetting("Message", "!pull"));
      this.message.setChinese("\u79c1\u4fe1\u5185\u5bb9");
      this.panicKey = this.add(new BindSetting("PanicKey", -1));
      this.panicKey.setChinese("\u7d27\u6025\u53d1\u9001\u952e");
      this.setChinese("\u81ea\u52a8\u56fe\u817e\u6c42\u6551");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.enable.getValue()) {
         if (!nullCheck()) {
            if (this.panicKey.isPressed()) {
               if (!this.keyWasPressed) {
                  this.sendWhisper();
                  this.recentPops = 0;
                  this.keyWasPressed = true;
               }
            } else {
               this.keyWasPressed = false;
            }
         }
      }
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      if (this.enable.getValue()) {
         if (!nullCheck()) {
            if (event.getPlayer() == mc.player) {
               long now = System.currentTimeMillis();
               long windowMs = (long)(this.popWindow.getValueFloat() * 1000.0F);
               if (now - this.lastPopTime > windowMs) {
                  this.recentPops = 0;
               }

               this.recentPops++;
               this.lastPopTime = now;
               if (this.recentPops >= this.popThreshold.getValueInt()) {
                  int totemCount = this.countTotems();
                  if (totemCount <= this.minTotems.getValueInt()) {
                     this.sendWhisper();
                     this.recentPops = 0;
                  }
               }
            }
         }
      }
   }

   private int countTotems() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = mc.player.getOffHandStack();
      if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
         count += offhand.getCount();
      }

      PlayerScreenHandler ix = mc.player.playerScreenHandler;
      if (ix instanceof PlayerScreenHandler) {
         PlayerScreenHandler handler = ix;

         for (int ixx = 1; ixx <= 4; ixx++) {
            ItemStack stack = handler.getSlot(ixx).getStack();
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
               count += stack.getCount();
            }
         }
      }

      return count;
   }

   private void sendWhisper() {
      if (mc.getNetworkHandler() != null) {
         String target = this.targetPlayer.getValue().trim();
         if (!target.isEmpty()) {
            String msg = this.message.getValue().trim();
            if (!msg.isEmpty()) {
               mc.getNetworkHandler().sendCommand("w " + target + " " + msg);
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.recentPops = 0;
      this.lastPopTime = 0L;
      this.keyWasPressed = false;
   }
}
