package dev.luminous.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class LogoutSpots extends Module {
   private final ColorSetting box;
   private final ColorSetting fill;
   private final ColorSetting text;
   private final ColorSetting chamsFill;
   private final ColorSetting chamsLine;
   final Map<UUID, PlayerEntity> playerCache;
   final Map<UUID, ModelPlayer> logoutCache;
   final Map<UUID, Long> logoutTime;
   private final BooleanSetting beam;
   private final SliderSetting beamTime;
   private final BooleanSetting health;
   private final BooleanSetting totem;
   private final BooleanSetting message;
   public static LogoutSpots INSTANCE;

   public LogoutSpots() {
      super("LogoutSpots", Module.Category.Render);
      INSTANCE = this;
      this.box = this.add(new ColorSetting("Box", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.box.setChinese("\u76d2\u5b50");
      this.fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.fill.setChinese("\u586b\u5145");
      this.text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255)).injectBoolean(true));
      this.text.setChinese("\u6587\u672c");
      this.chamsFill = this.add(new ColorSetting("ChamsFill", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.chamsFill.setChinese("\u6a21\u578b\u586b\u5145");
      this.chamsLine = this.add(new ColorSetting("ChamsLine", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.chamsLine.setChinese("\u6a21\u578b\u8fb9\u6846");
      this.playerCache = Maps.newConcurrentMap();
      this.logoutCache = Maps.newConcurrentMap();
      this.logoutTime = Maps.newConcurrentMap();
      this.beam = this.add(new BooleanSetting("Beam", true));
      this.beam.setChinese("\u5149\u67f1");
      this.beamTime = this.add(new SliderSetting("BeamTime", 15.0, 3.0, 60.0, 1.0, this.beam::getValue).setSuffix("s"));
      this.beamTime.setChinese("\u5149\u67f1\u65f6\u95f4");
      this.health = this.add(new BooleanSetting("Health", true));
      this.health.setChinese("\u751f\u547d");
      this.totem = this.add(new BooleanSetting("Totem", true));
      this.totem.setChinese("\u56fe\u817e");
      this.message = this.add(new BooleanSetting("Message", true));
      this.message.setChinese("\u6d88\u606f");
      this.setChinese("\u9000\u51fa\u8bb0\u5f55");
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive var1) {
      if (!nullCheck()) {
         if (var1.getPacket() instanceof PlayerListS2CPacket var2) {
            if (var2.getActions().contains(Action.ADD_PLAYER)) {
               for (Entry var5 : var2.getPlayerAdditionEntries()) {
                  if (var5.gameMode() != GameMode.SPECTATOR) {
                     for (UUID var7 : this.logoutCache.keySet()) {
                        if (var7.equals(var5.profile().getId())) {
                           PlayerEntity var8 = this.logoutCache.get(var7).player;
                           if (this.message.getValue()) {
                              mc.execute(
                                 () -> this.sendMessage(
                                    "\u00a7f"
                                       + var8.getName().getString()
                                       + " \u00a7rLogged back at \u00a7f"
                                       + var8.getBlockX()
                                       + ", "
                                       + var8.getBlockY()
                                       + ", "
                                       + var8.getBlockZ()
                                 )
                              );
                           }

                           this.logoutCache.remove(var7);
                           this.logoutTime.remove(var7);
                        }
                     }
                  }
               }
            }
         } else if (var1.getPacket() instanceof PlayerRemoveS2CPacket var3) {
            List<UUID> var14;
            try {
               var14 = var3.profileIds();
            } catch (Throwable var11) {
               throw new MatchException(var11.toString(), var11);
            }

            for (UUID var16 : var14) {
               for (UUID var18 : this.playerCache.keySet()) {
                  if (var18.equals(var16)) {
                     PlayerEntity var9 = this.playerCache.get(var18);
                     if (!this.logoutCache.containsKey(var18) && var9 != null) {
                        ModelPlayer var10 = new ModelPlayer(var9);
                        if (this.message.getValue()) {
                           mc.execute(
                              () -> this.sendMessage(
                                 "\u00a7f"
                                    + var9.getName().getString()
                                    + " \u00a7rLogged out at \u00a7f"
                                    + var9.getBlockX()
                                    + ", "
                                    + var9.getBlockY()
                                    + ", "
                                    + var9.getBlockZ()
                              )
                           );
                        }

                        this.logoutCache.put(var18, var10);
                        this.logoutTime.put(var18, System.currentTimeMillis());
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.playerCache.clear();
      this.logoutCache.clear();
      this.logoutTime.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.playerCache.clear();

      for (Entity var3 : Alien.THREAD.threadSafeEntityList) {
         if (var3 instanceof AbstractClientPlayerEntity var4 && !var4.equals(mc.player)) {
            this.playerCache.put(var4.getGameProfile().getId(), var4);
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      this.logoutTime.entrySet().removeIf(var1x -> {
         UUID var2 = var1x.getKey();
         return !this.logoutCache.containsKey(var2) || System.currentTimeMillis() - var1x.getValue() > (long)(this.beamTime.getValueFloat() * 1000.0F);
      });

      for (ModelPlayer var3 : this.logoutCache.values()) {
         PlayerEntity var4 = var3.player;
         if (this.beam.getValue() && this.logoutTime.containsKey(var4.getGameProfile().getId())) {
            Box var5 = new Box(var4.getX() - 0.15, Math.max(var4.getY(), -64.0), var4.getZ() - 0.15, var4.getX() + 0.15, 320.0, var4.getZ() + 0.15);
            Render3DUtil.drawFill(var1, var5, this.box.getValue());
         }

         Box var6 = ((IEntity)var4).getDimensions().getBoxAt(var4.getPos());
         if (this.box.booleanValue) {
            Render3DUtil.drawBox(var1, var6, this.box.getValue());
         }

         if (this.fill.booleanValue) {
            Render3DUtil.drawFill(var1, var6, this.fill.getValue());
         }

         if (this.chamsFill.booleanValue || this.chamsLine.booleanValue) {
            var3.render(var1, this.chamsFill, this.chamsLine);
         }

         if (this.text.booleanValue) {
            Render3DUtil.drawText3D(
               var4.getName().getString()
                  + (this.health.getValue() ? TextRadar.getHealthColor(var4) + " " + round2(var4.getHealth() + var4.getAbsorptionAmount()) : "")
                  + (this.totem.getValue() && Alien.POP.getPop(var4) > 0 ? TextRadar.getPopColor(Alien.POP.getPop(var4)) + " -" + Alien.POP.getPop(var4) : ""),
               new Vec3d(var4.getX(), ((IEntity)var4).getDimensions().getBoxAt(var4.getPos()).maxY + 0.5, var4.getZ()),
               this.text.getValue()
            );
         }
      }
   }

   public static float round2(double var0) {
      BigDecimal var2 = new BigDecimal(var0);
      var2 = var2.setScale(1, RoundingMode.HALF_UP);
      return var2.floatValue();
   }

   public Map getLogoutCache() {
      return this.logoutCache;
   }
}
