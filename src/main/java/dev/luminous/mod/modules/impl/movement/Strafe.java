package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.util.Objects;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Strafe extends Module {
   public static Strafe INSTANCE;
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", true));
   private final BooleanSetting slowCheck;
   public static Vec3d velocityOverride;

   public Strafe() {
      super("Strafe", "Modifies sprinting", Module.Category.Movement);
      this.airStop.setChinese("\u7a7a\u4e2d\u505c\u6b62");
      this.slowCheck = this.add(new BooleanSetting("SlowCheck", true));
      this.slowCheck.setChinese("\u7f13\u6162\u68c0\u6d4b");
      this.setChinese("\u7075\u6d3b\u79fb\u52a8");
      INSTANCE = this;
   }

   @EventListener
   public void onStrafe(MoveEvent event) {
      if (!BaritoneUtil.isActive()
         && !mc.player.isSneaking()
         && !Fly.INSTANCE.isOn()
         && !HoleSnap.INSTANCE.isOn()
         && !Speed.INSTANCE.isOn()
         && !mc.player.isFallFlying()
         && !EntityUtil.isInsideBlock()
         && !mc.player.isInLava()
         && !mc.player.isTouchingWater()
         && !mc.player.getAbilities().flying) {
         if (!MovementUtil.isMoving()) {
            if (this.airStop.getValue()) {
               MovementUtil.setMotionX(0.0);
               MovementUtil.setMotionZ(0.0);
            }
         } else {
            double[] dir = MovementUtil.directionSpeed(this.getBaseMoveSpeed());
            event.setX(dir[0]);
            event.setZ(dir[1]);
         }
      }
   }

   public double getBaseMoveSpeed() {
      double n = 0.2873;
      if (mc.player.hasStatusEffect(StatusEffects.SPEED) && (!this.slowCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SLOWNESS))) {
         n *= 1.0 + 0.2 * (Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() + 1);
      }

      return n;
   }
}
