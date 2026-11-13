package dev.luminous.mod.commands.impl;

import dev.luminous.api.utils.path.TPUtils;
import dev.luminous.mod.commands.Command;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class TeleportCommand extends Command {
   public TeleportCommand() {
      super("tp", "[x] [y] [z]");
   }

   @Override
   public void runCommand(String[] var1) {
      if (var1.length != 3) {
         this.sendUsage();
      } else {
         double var2;
         if (this.isNumeric(var1[0])) {
            var2 = Double.parseDouble(var1[0]);
         } else {
            if (!var1[0].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(var1[0].replace("~", ""))) {
               var2 = mc.player.getX() + Double.parseDouble(var1[0].replace("~", ""));
            } else {
               if (!var1[0].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               var2 = mc.player.getX();
            }
         }

         double var4;
         if (this.isNumeric(var1[1])) {
            var4 = Double.parseDouble(var1[1]);
         } else {
            if (!var1[1].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(var1[1].replace("~", ""))) {
               var4 = mc.player.getY() + Double.parseDouble(var1[1].replace("~", ""));
            } else {
               if (!var1[1].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               var4 = mc.player.getY();
            }
         }

         double var6;
         if (this.isNumeric(var1[2])) {
            var6 = Double.parseDouble(var1[2]);
         } else {
            if (!var1[2].startsWith("~")) {
               this.sendUsage();
               return;
            }

            if (this.isNumeric(var1[2].replace("~", ""))) {
               var6 = mc.player.getZ() + Double.parseDouble(var1[2].replace("~", ""));
            } else {
               if (!var1[2].replace("~", "").isEmpty()) {
                  this.sendUsage();
                  return;
               }

               var6 = mc.player.getZ();
            }
         }

         Vec3d var8 = new Vec3d(var2, var4, var6);
         Entity var9 = mc.player.getVehicle();
         if (var9 == null) {
            TPUtils.newTeleport(var8);
            mc.player.setPosition(var2, var4, var6);
         } else {
            double var10 = var2 - mc.player.getX();
            double var12 = var4 - mc.player.getY();
            double var14 = var6 - mc.player.getZ();
            var9.updatePosition(var9.getX() + var10, var9.getY() + var12, var9.getZ() + var14);
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(var9));
            var9.updatePassengerPosition(mc.player);
         }

         DecimalFormat var16 = new DecimalFormat("0.0");
         this.sendChatMessage("\u00a7fTeleported to \u00a7e" + var16.format(var2) + ", " + var16.format(var4) + ", " + var16.format(var6));
      }
   }

   private boolean isNumeric(String var1) {
      return var1.matches("-?\\d+(\\.\\d+)?");
   }

   @Override
   public String[] getAutocorrect(int var1, List<String> var2) {
      return new String[]{"~ "};
   }
}
