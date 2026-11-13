package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.combat.Surround;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class Step extends Module {
   private final EnumSetting<Step.Mode> mode = this.add(new EnumSetting<>("Mode", Step.Mode.Vanilla));
   private final SliderSetting height;
   private final BooleanSetting useTimer;
   private final BooleanSetting fast;
   private final BooleanSetting onlyMoving;
   private final BooleanSetting surroundPause;
   private final BooleanSetting inWebPause;
   private final BooleanSetting inBlockPause;
   private final BooleanSetting sneakingPause;
   private final BooleanSetting pathingPause;
   boolean timer;
   int packets;

   public Step() {
      super("Step", "Steps up blocks.", Module.Category.Movement);
      this.mode.setChinese("\u6a21\u5f0f");
      this.height = this.add(new SliderSetting("Height", 1.0, 0.0, 5.0, 0.5));
      this.height.setChinese("\u9ad8\u5ea6");
      this.useTimer = this.add(new BooleanSetting("Timer", true, () -> this.mode.getValue() == Step.Mode.OldNCP || this.mode.getValue() == Step.Mode.NCP));
      this.fast = this.add(new BooleanSetting("Fast", true, () -> this.mode.getValue() == Step.Mode.NCP && this.useTimer.getValue()));
      this.fast.setChinese("\u5feb\u901f");
      this.onlyMoving = this.add(new BooleanSetting("OnlyMoving", true));
      this.onlyMoving.setChinese("\u4ec5\u79fb\u52a8");
      this.surroundPause = this.add(new BooleanSetting("SurroundPause", true));
      this.surroundPause.setChinese("\u5305\u56f4\u6682\u505c");
      this.inWebPause = this.add(new BooleanSetting("InWebPause", true));
      this.inWebPause.setChinese("\u7f51\u4e2d\u6682\u505c");
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true));
      this.inBlockPause.setChinese("\u65b9\u5757\u5185\u6682\u505c");
      this.sneakingPause = this.add(new BooleanSetting("SneakingPause", true));
      this.sneakingPause.setChinese("\u6f5c\u884c\u6682\u505c");
      this.pathingPause = this.add(new BooleanSetting("PathingPause", true));
      this.pathingPause.setChinese("\u5bfb\u8def\u6682\u505c");
      this.packets = 0;
      this.setChinese("\u6b65\u884c\u8f85\u52a9");
      this.useTimer.setChinese("\u8ba1\u65f6\u5668\u52a0\u901f");
   }

   public static void setStepHeight(float v) {
      mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT).setBaseValue(v);
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         setStepHeight(0.6F);
         Alien.TIMER.reset();
      }
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.pathingPause.getValue() && BaritoneUtil.isActive()
         || this.sneakingPause.getValue() && mc.player.isSneaking()
         || this.inBlockPause.getValue() && EntityUtil.isInsideBlock()
         || mc.player.isInLava()
         || mc.player.isTouchingWater()
         || this.inWebPause.getValue() && Alien.PLAYER.isInWeb(mc.player)
         || !mc.player.isOnGround()
         || this.onlyMoving.getValue() && !MovementUtil.isMoving()
         || this.surroundPause.getValue() && (Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
         setStepHeight(0.6F);
      } else {
         setStepHeight(this.height.getValueFloat());
      }
   }

   @EventListener
   public void onStep(TickEvent event) {
      if (event.isPost()) {
         this.packets--;
      } else {
         if (this.timer && this.packets <= 0) {
            Alien.TIMER.reset();
            this.timer = false;
         }

         boolean strict = this.mode.getValue() == Step.Mode.NCP;
         if (this.mode.getValue().equals(Step.Mode.OldNCP) || strict) {
            double stepHeight = mc.player.getY() - mc.player.prevY;
            if (stepHeight <= 0.75 || stepHeight > this.height.getValue()) {
               return;
            }

            double[] offsets = this.getOffset(stepHeight);
            if (offsets != null && offsets.length > 1) {
               if (this.useTimer.getValue()) {
                  Alien.TIMER.set((float)this.getTimer(stepHeight));
                  this.timer = true;
                  this.packets = 2;
               }

               for (double offset : offsets) {
                  mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.prevX, mc.player.prevY + offset, mc.player.prevZ, false));
               }
            }
         }
      }
   }

   public double getTimer(double height) {
      if (height > 0.6 && height <= 1.0) {
         return !this.fast.getValue() && this.mode.getValue() == Step.Mode.NCP ? 0.3333333333333333 : 0.5;
      } else {
         double[] offsets = this.getOffset(height);
         return offsets == null ? 1.0 : 1.0 / offsets.length;
      }
   }

   public double[] getOffset(double height) {
      boolean strict = this.mode.getValue() == Step.Mode.NCP;
      if (height == 0.75) {
         return strict ? new double[]{0.42, 0.753, 0.75} : new double[]{0.42, 0.753};
      } else if (height == 0.8125) {
         return strict ? new double[]{0.39, 0.7, 0.8125} : new double[]{0.39, 0.7};
      } else if (height == 0.875) {
         return strict ? new double[]{0.39, 0.7, 0.875} : new double[]{0.39, 0.7};
      } else if (height == 1.0) {
         return strict ? new double[]{0.42, 0.753, 1.0} : new double[]{0.42, 0.753};
      } else if (height == 1.5) {
         return new double[]{0.42, 0.75, 1.0, 1.16, 1.23, 1.2};
      } else if (height == 2.0) {
         return new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
      } else {
         return height == 2.5 ? new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907} : null;
      }
   }

   public static enum Mode {
      Vanilla,
      OldNCP,
      NCP;
   }
}
