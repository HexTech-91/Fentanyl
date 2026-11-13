package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

public class NoFall extends Module {
   private final EnumSetting<NoFall.NoFallMode> mode = this.add(new EnumSetting<>("Mode", NoFall.NoFallMode.Packet));
   private final SliderSetting distance;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.mode.setChinese("\u6a21\u5f0f");
      this.distance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));
      this.distance.setChinese("\u8ddd\u79bb");
      this.setChinese("\u6ca1\u6709\u6454\u843d\u4f24\u5bb3");
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && this.mode.is(NoFall.NoFallMode.Grim) && this.checkFalling()) {
         mc.getNetworkHandler()
            .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false));
         mc.player.onLanding();
      }
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > mc.player.getSafeFallDistance() && !mc.player.isOnGround() && !mc.player.isFallFlying();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         for (ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() == Items.ELYTRA) {
               return;
            }
         }

         if (this.mode.is(NoFall.NoFallMode.Packet)
            && event.getPacket() instanceof PlayerMoveC2SPacket packet
            && mc.player.fallDistance >= (float)this.distance.getValue()
            && !BowBomb.send) {
            ((IPlayerMoveC2SPacket)packet).setOnGround(true);
         }
      }
   }

   public static enum NoFallMode {
      Packet,
      Grim;
   }
}
