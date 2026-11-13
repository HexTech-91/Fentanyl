package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FastWeb extends Module {
   public static FastWeb INSTANCE;
   public final EnumSetting<FastWeb.Mode> mode = this.add(new EnumSetting<>("Mode", FastWeb.Mode.Vanilla));
   public final BooleanSetting onlySneak;
   public final BooleanSetting grim;
   public final BooleanSetting abortPacket;
   public final SliderSetting xZSlow;
   public final SliderSetting ySlow;
   private final SliderSetting fastSpeed;
   private boolean work;

   public FastWeb() {
      super("FastWeb", "So you don't need to keep timer on keybind", Module.Category.Movement);
      this.mode.setChinese("\u6a21\u5f0f");
      this.onlySneak = this.add(new BooleanSetting("OnlySneak", true));
      this.onlySneak.setChinese("\u4ec5\u6f5c\u884c");
      this.grim = this.add(new BooleanSetting("Grim", false).setParent());
      this.abortPacket = this.add(new BooleanSetting("AbortPacket", true, this.grim::isOpen));
      this.abortPacket.setChinese("\u4e2d\u6b62\u53d1\u5305");
      this.xZSlow = this.add(new SliderSetting("XZSlow", 25.0, 0.0, 100.0, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom).setSuffix("%"));
      this.xZSlow.setChinese("\u6c34\u5e73\u901f\u5ea6");
      this.ySlow = this.add(new SliderSetting("YSlow", 100.0, 0.0, 100.0, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom).setSuffix("%"));
      this.ySlow.setChinese("\u5782\u76f4\u901f\u5ea6");
      this.fastSpeed = this.add(
         new SliderSetting("FastSpeed", 3.0, 0.0, 8.0, () -> this.mode.getValue() == FastWeb.Mode.Vanilla || this.mode.getValue() == FastWeb.Mode.Strict)
      );
      this.fastSpeed.setChinese("\u901f\u5ea6");
      this.work = false;
      this.setChinese("\u8718\u86db\u7f51\u52a0\u901f");
      INSTANCE = this;
      this.grim.setChinese("Grim\u6a21\u5f0f");
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.work = !mc.player.isOnGround() && (mc.options.sneakKey.isPressed() || !this.onlySneak.getValue()) && Alien.PLAYER.isInWeb(mc.player);
      if (this.work && this.mode.is(FastWeb.Mode.Vanilla)) {
         MovementUtil.setMotionY(-this.fastSpeed.getValue());
      }

      if (this.grim.getValue() && (mc.options.sneakKey.isPressed() || !this.onlySneak.getValue())) {
         for (BlockPos pos : this.getIntersectingWebs()) {
            if (this.abortPacket.getValue()) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
         }
      }
   }

   @EventListener(
      priority = -100
   )
   public void onTimer(TimerEvent event) {
      if (this.work && this.mode.getValue() == FastWeb.Mode.Strict) {
         event.set(this.fastSpeed.getValueFloat());
      }
   }

   public List<BlockPos> getIntersectingWebs() {
      int radius = 2;
      List<BlockPos> blocks = new ArrayList<>();

      for (int x = radius; x > -radius; x--) {
         for (int y = radius; y > -radius; y--) {
            for (int z = radius; z > -radius; z--) {
               BlockPos blockPos = BlockPos.ofFloored(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
               if (!(mc.player.getPos().distanceTo(blockPos.toCenterPos()) > 1.0) || !(mc.player.getEyePos().distanceTo(blockPos.toCenterPos()) > 1.0)) {
                  BlockState state = mc.world.getBlockState(blockPos);
                  if (state.getBlock() instanceof CobwebBlock) {
                     blocks.add(blockPos);
                  }
               }
            }
         }
      }

      return blocks;
   }

   public static enum Mode {
      Vanilla,
      Strict,
      Custom,
      Ignore;
   }
}
