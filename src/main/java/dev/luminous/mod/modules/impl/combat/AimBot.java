package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;

public class AimBot extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 10, 0, 32)).setChinese("\u8303\u56f4");
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 1, 0, 100)).setChinese("\u6b65\u957f");

   public AimBot() {
      super("AimBot", Module.Category.Combat);
      this.setChinese("\u7784\u51c6\u673a\u5668\u4eba");
   }

   @EventListener
   private void onRotate(RotationEvent event) {
      PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
      if (target != null) {
         if (mc.player.hasVehicle()) {
            Alien.ROTATION.lookAt(target.getEyePos());
         } else {
            event.setTarget(target.getEyePos(), this.steps.getValueFloat(), 10.0F);
         }
      }
   }
}
