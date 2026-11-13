package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoLogin extends Module {
   public static AutoLogin INSTANCE;
   private final BooleanSetting sendCommand = this.add(new BooleanSetting("\u53d1\u9001\u6307\u4ee4", true));
   private final StringSetting loginCommand = this.add(new StringSetting("\u767b\u5f55\u6307\u4ee4", "login password123"));
   private final SliderSetting clickDelay = this.add(new SliderSetting("\u70b9\u51fb\u5ef6\u8fdf", 500.0, 0.0, 2000.0, 1.0).setSuffix("ms"));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("\u81ea\u52a8\u5173\u95ed", true));
   private AutoLogin.State state = AutoLogin.State.WAIT_BEFORE_SCROLL;
   private boolean commandSent = false;
   private int targetSlot = -1;
   private int scrollIndex = 0;
   private List<Integer> scrollSequence;
   private final Timer loginTimer = new Timer();
   private final Timer scrollTimer = new Timer();
   private final Timer clickTimer = new Timer();
   private long guiOpenedTime = 0L;

   public AutoLogin() {
      super("AutoLogin", Module.Category.Misc);
      this.setChinese("\u81ea\u52a8\u767b\u5f55");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onLogin() {
      if (this.isOn()) {
         this.reset();
         if (this.sendCommand.getValue()) {
            this.commandSent = false;
            this.loginTimer.reset();
         } else {
            this.commandSent = true;
         }

         this.state = AutoLogin.State.WAIT_BEFORE_SCROLL;
         this.loginTimer.reset();
         this.scrollTimer.reset();
         this.clickTimer.reset();
         this.targetSlot = this.findCompassInHotbar();
         if (this.targetSlot == -1) {
            this.state = AutoLogin.State.WAIT_BEFORE_SCROLL;
         } else {
            this.buildScrollSequence(this.targetSlot);
            this.scrollIndex = 0;
         }
      }
   }

   private void reset() {
      this.state = AutoLogin.State.WAIT_BEFORE_SCROLL;
      this.commandSent = false;
      this.targetSlot = -1;
      this.scrollSequence = null;
      this.scrollIndex = 0;
      this.loginTimer.reset();
      this.scrollTimer.reset();
      this.clickTimer.reset();
      this.guiOpenedTime = 0L;
   }

   private void buildScrollSequence(int target) {
      this.scrollSequence = new ArrayList<>();

      for (int i = 0; i <= 8; i++) {
         this.scrollSequence.add(i);
      }

      for (int i = 7; i >= 0; i--) {
         this.scrollSequence.add(i);
      }

      for (int i = 1; i <= target; i++) {
         this.scrollSequence.add(i);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!this.commandSent && this.sendCommand.getValue() && this.loginTimer.passedS(1.0)) {
            mc.player.networkHandler.sendCommand(this.loginCommand.getValue());
            this.commandSent = true;
         }

         switch (this.state) {
            case WAIT_BEFORE_SCROLL:
               if (this.loginTimer.passedS(0.5) && this.targetSlot != -1) {
                  this.state = AutoLogin.State.SCROLLING;
                  this.scrollTimer.reset();
               }
               break;
            case SCROLLING:
               if (this.scrollSequence == null || this.scrollIndex >= this.scrollSequence.size()) {
                  this.state = AutoLogin.State.READY_TO_OPEN;
               } else if (this.scrollTimer.passedMs(50.0)) {
                  int slot = this.scrollSequence.get(this.scrollIndex);
                  if (mc.player.getInventory().selectedSlot != slot) {
                     mc.player.getInventory().selectedSlot = slot;
                     mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                  }

                  this.scrollIndex++;
                  this.scrollTimer.reset();
               }
               break;
            case READY_TO_OPEN:
               if (mc.player.getInventory().selectedSlot != this.targetSlot) {
                  mc.player.getInventory().selectedSlot = this.targetSlot;
                  mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.targetSlot));
               }

               this.state = AutoLogin.State.OPENING_GUI;
               break;
            case OPENING_GUI:
               if (mc.currentScreen == null) {
                  mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
               } else {
                  this.state = AutoLogin.State.TAKING_FROM_GUI;
                  this.guiOpenedTime = System.currentTimeMillis();
                  this.clickTimer.reset();
               }
               break;
            case TAKING_FROM_GUI:
               if (!(mc.currentScreen instanceof InventoryScreen)
                  && !(mc.player.currentScreenHandler instanceof PlayerScreenHandler)
                  && this.hasCompassInGui()
                  && this.clickTimer.passedMs((long)this.clickDelay.getValue())) {
                  this.takeOneCompassFromGui();
                  this.clickTimer.reset();
               }
         }
      }
   }

   private int findCompassInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.COMPASS) {
            return i;
         }
      }

      return -1;
   }

   private boolean hasCompassInGui() {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (handler == null) {
         return false;
      } else {
         for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().getItem() == Items.COMPASS) {
               return true;
            }
         }

         return false;
      }
   }

   private void takeOneCompassFromGui() {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (handler != null) {
         for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().getItem() == Items.COMPASS) {
               mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
               return;
            }
         }
      }
   }

   private static enum State {
      WAIT_BEFORE_SCROLL,
      SCROLLING,
      READY_TO_OPEN,
      OPENING_GUI,
      TAKING_FROM_GUI;
   }
}
