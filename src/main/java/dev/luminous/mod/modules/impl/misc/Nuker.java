package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Nuker extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
   private final BooleanSetting down;
   private final BooleanSetting sand;
   private final SliderSetting breaks;

   public Nuker() {
      super("Nuker", Module.Category.Misc);
      this.range.setChinese("\u8303\u56f4");
      this.down = this.add(new BooleanSetting("Down", false));
      this.down.setChinese("\u5411\u4e0b\u6316\u6398");
      this.sand = this.add(new BooleanSetting("Sand", false));
      this.sand.setChinese("\u6c99\u5b50");
      this.breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, this.sand::getValue));
      this.breaks.setChinese("\u6316\u6398\u6b21\u6570");
      this.setChinese("\u8303\u56f4\u6316\u6398");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || mc.world.isAir(PacketMine.getBreakPos())) {
         if (this.sand.getValue()) {
            if (!mc.player.isOnGround()) {
               return;
            }

            int b = 0;

            for (BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
               if (Blocks.SAND == mc.world.getBlockState(sand).getBlock() || Blocks.RED_SAND == mc.world.getBlockState(sand).getBlock()) {
                  Direction side = BlockUtil.getClickSideStrict(sand);
                  if (side != null) {
                     PlaceRender.INSTANCE.create(sand);
                     Alien.ROTATION.snapAt(sand.toCenterPos());
                     sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                     sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                     sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                     Alien.ROTATION.snapBack();
                     if (++b >= this.breaks.getValue()) {
                        return;
                     }
                  }
               }
            }
         } else {
            BlockPos pos = this.getBlock();
            if (pos != null) {
               PacketMine.INSTANCE.mine(pos);
            }
         }
      }
   }

   private BlockPos getBlock() {
      BlockPos down = null;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
         if (!mc.world.isAir(pos) && !PacketMine.unbreakable(pos) && BlockUtil.getClickSideStrict(pos) != null) {
            if (!(pos.getY() < mc.player.getY())) {
               return pos;
            }

            if (down == null && this.down.getValue()) {
               down = pos;
            }
         }
      }

      return down;
   }
}
