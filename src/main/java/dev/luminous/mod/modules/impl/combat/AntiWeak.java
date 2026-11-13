package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;

public class AntiWeak extends Module {
   private final EnumSetting<AntiWeak.SwapMode> swapMode = this.add(new EnumSetting<>("SwapMode", AntiWeak.SwapMode.Inventory));
   private final SliderSetting delay;
   private final BooleanSetting onlyCrystal;
   private final Timer delayTimer;
   boolean ignore;
   private PlayerInteractEntityC2SPacket lastPacket;

   public AntiWeak() {
      super("AntiWeak", Module.Category.Combat);
      this.swapMode.setChinese("\u4ea4\u6362\u6a21\u5f0f");
      this.delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
      this.onlyCrystal.setChinese("\u4ec5\u6c34\u6676");
      this.delayTimer = new Timer();
      this.ignore = false;
      this.lastPacket = null;
      this.setChinese("\u53cd\u865a\u5f31");
   }

   @Override
   public String getInfo() {
      return this.swapMode.getValue().name();
   }

   @EventListener(
      priority = 200
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()
         && !event.isCancelled()
         && !this.ignore
         && mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null
         && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
         && this.delayTimer.passedMs(this.delay.getValue())
         && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet
         && Criticals.getInteractType(packet) == InteractType.ATTACK) {
         if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
            return;
         }

         this.lastPacket = packet;
         this.delayTimer.reset();
         this.ignore = true;
         this.doAnti();
         this.ignore = false;
         event.cancel();
      }
   }

   private void doAnti() {
      if (this.lastPacket != null) {
         int strong;
         if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
            strong = InventoryUtil.findClass(SwordItem.class);
         } else {
            strong = InventoryUtil.findClassInventorySlot(SwordItem.class);
         }

         if (strong != -1) {
            int old = mc.player.getInventory().selectedSlot;
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               InventoryUtil.switchToSlot(strong);
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
            }

            mc.getNetworkHandler().sendPacket(this.lastPacket);
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               if (this.swapMode.getValue() != AntiWeak.SwapMode.Normal) {
                  InventoryUtil.switchToSlot(old);
               }
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            }
         }
      }
   }

   public static enum SwapMode {
      Normal,
      Silent,
      Inventory;
   }
}
