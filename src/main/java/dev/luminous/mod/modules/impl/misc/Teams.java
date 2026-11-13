package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class Teams extends Module {
   public static Teams INSTANCE;
   public final EnumSetting<Teams.Mode> mode = this.add(new EnumSetting<>("Mode", Teams.Mode.Scoreboard));

   public Teams() {
      super("Teams", Module.Category.Misc);
      this.setChinese("\u961f\u4f0d");
      INSTANCE = this;
   }

   public static boolean isSameTeam(Entity entity) {
      if (INSTANCE != null && INSTANCE.isOn() && entity instanceof PlayerEntity && mc.player != null) {
         PlayerEntity target = (PlayerEntity)entity;
         if (INSTANCE.mode.is(Teams.Mode.Color)) {
            return mc.player.getTeamColorValue() == target.getTeamColorValue();
         } else {
            return mc.player.getScoreboardTeam() != null && target.getScoreboardTeam() != null
               ? Objects.equals(mc.player.getScoreboardTeam(), target.getScoreboardTeam())
               : false;
         }
      } else {
         return false;
      }
   }

   public static enum Mode {
      Scoreboard,
      Color;
   }
}
