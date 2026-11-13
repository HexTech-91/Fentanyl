package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoPearl extends Module {
   public static AutoPearl INSTANCE;
   public static boolean throwing = false;
   public final BooleanSetting inventory = this.add(new BooleanSetting("Inventory", true));
   private final EnumSetting<Timing> timing;
   public final EnumSetting<SwingSide> interactSwing;
   private final BooleanSetting rotation;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting sync;

   public AutoPearl() {
      super("AutoPearl", Module.Category.Player);
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All));
      this.timing.setChinese("\u65f6\u673a");
      this.interactSwing = this.add(new EnumSetting<>("InteractSwing", SwingSide.All));
      this.interactSwing.setChinese("\u6325\u624b");
      this.rotation = this.add(new BooleanSetting("Rotation", true));
      this.rotation.setChinese("\u65cb\u8f6c");
      this.yawStep = this.add(new BooleanSetting("YawStep", false).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(new BooleanSetting("WhenElytra", true, this.yawStep::isOpen));
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, this.yawStep::isOpen));
      this.steps.setChinese("\u6b65\u957f");
      this.fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, this.yawStep::isOpen));
      this.fov.setChinese("\u89c6\u91ce");
      this.priority = this.add(new SliderSetting("Priority", 100, 0, 100, this.yawStep::isOpen));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.sync = this.add(new BooleanSetting("Sync", true, this.yawStep::isOpen));
      this.sync.setChinese("\u540c\u6b65");
      this.setChinese("\u6254\u73cd\u73e0");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (INSTANCE.inventory.getValue()) {
            if (InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) == -1) {
               this.disable();
               return;
            }
         } else if (InventoryUtil.findItem(Items.ENDER_PEARL) == -1) {
            this.disable();
            return;
         }

         if (!this.shouldYawStep()
            && (!this.inventory.getValue() || EntityUtil.inInventory())
            && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())) {
            this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
            this.disable();
         }
      }
   }

   @EventListener
   public void onUpdate(TickEvent event) {
      if (!nullCheck()
         && (!this.timing.is(Timing.Pre) || !event.isPost())
         && (!this.timing.is(Timing.Post) || !event.isPre())
         && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())) {
         if (!this.shouldYawStep()) {
            this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
            this.disable();
         } else if (Alien.ROTATION.inFov(mc.player.getYaw(), mc.player.getPitch(), this.fov.getValueFloat())) {
            if (this.sync.getValue()) {
               this.throwPearl(mc.player.getYaw(), mc.player.getPitch());
            } else {
               throwing = true;
               if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                  sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                  EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
               } else {
                  int pearl;
                  if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                     InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                     sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                     EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                     InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                     EntityUtil.syncInventory();
                  } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
                     int old = mc.player.getInventory().selectedSlot;
                     InventoryUtil.switchToSlot(pearl);
                     sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                     EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                     InventoryUtil.switchToSlot(old);
                  }
               }

               throwing = false;
            }

            this.disable();
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.shouldYawStep()) {
         event.setRotation(mc.player.getYaw(), mc.player.getPitch(), this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   public void throwPearl(float yaw, float pitch) {
      throwing = true;
      if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
         if (this.rotation.getValue()) {
            Alien.ROTATION.snapAt(yaw, pitch);
         }

         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
         EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
         if (this.rotation.getValue()) {
            Alien.ROTATION.snapBack();
         }
      } else {
         int pearl;
         if (this.inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapAt(yaw, pitch);
            }

            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapBack();
            }
         } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(pearl);
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapAt(yaw, pitch);
            }

            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            InventoryUtil.switchToSlot(old);
            if (this.rotation.getValue()) {
               Alien.ROTATION.snapBack();
            }
         }
      }

      throwing = false;
   }
}
