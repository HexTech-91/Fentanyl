package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AutoFollow extends Module {
   public static AutoFollow INSTANCE;
   private static final double SEARCH_RANGE = 64.0;
   private static final double BEHIND_DISTANCE = 0.3;
   private static final double STOP_DISTANCE = 0.12;
   private static final double FOLLOW_SPEED = 0.16;
   private final StringSetting targetName = this.add(new StringSetting("TargetName", ""));
   private PlayerEntity target;

   public AutoFollow() {
      super("AutoFollow", Module.Category.Movement);
      this.setChinese("\u81ea\u52a8\u8ddf\u8e2a");
      this.targetName.setChinese("\u81ea\u5b9a\u4e49\u8ddf\u8e2a\u540d\u79f0");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.target = null;
   }

   @Override
   public String getInfo() {
      return this.target == null ? null : this.target.getName().getString();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target != null) {
            double var2 = Math.toRadians(this.target.getYaw());
            double var4 = this.target.getX() + Math.sin(var2) * 0.3;
            double var6 = this.target.getZ() - Math.cos(var2) * 0.3;
            double var8 = var4 - mc.player.getX();
            double var10 = var6 - mc.player.getZ();
            double var12 = Math.sqrt(var8 * var8 + var10 * var10);
            Vec3d var14 = mc.player.getVelocity();
            if (var12 <= 0.12) {
               mc.player.setVelocity(0.0, var14.y, 0.0);
            } else {
               double var15 = Math.min(0.16, var12);
               mc.player.setVelocity(var8 / var12 * var15, var14.y, var10 / var12 * var15);
            }
         }
      }
   }

   private PlayerEntity findTarget() {
      String var1 = this.targetName.getValue().trim();
      PlayerEntity var2 = null;
      PlayerEntity var3 = null;
      double var4 = 4096.0;
      double var6 = var4;

      for (PlayerEntity var9 : mc.world.getPlayers()) {
         if (this.isValid(var9)) {
            double var10 = mc.player.squaredDistanceTo(var9);
            if (!(var10 > var4)) {
               String var12 = var9.getName().getString();
               if (!var1.isEmpty() && var1.equalsIgnoreCase(var12) && var10 <= var6) {
                  var6 = var10;
                  var3 = var9;
               }

               if (var10 <= var4) {
                  var4 = var10;
                  var2 = var9;
               }
            }
         }
      }

      return var3 != null ? var3 : var2;
   }

   private boolean isValid(PlayerEntity var1) {
      return var1 != null
         && var1 != mc.player
         && var1.isAlive()
         && var1.getHealth() > 0.0F
         && !var1.isSpectator()
         && mc.player.squaredDistanceTo(var1) <= 4096.0;
   }
}
