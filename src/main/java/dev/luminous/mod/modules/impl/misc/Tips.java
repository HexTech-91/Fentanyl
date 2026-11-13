package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

public class Tips extends Module {
   public static Tips INSTANCE;
   public final BooleanSetting visualRange = this.add(new BooleanSetting("VisualRange", false).setParent());
   public final BooleanSetting friends;
   public final BooleanSetting popCounter;
   public final BooleanSetting deathCoords;
   public final BooleanSetting serverLag;
   public final BooleanSetting lagBack;
   public final BooleanSetting potion;
   public final BooleanSetting resistanceLevelCheck;
   private final SliderSetting yOffset;
   final DecimalFormat df;
   final int color;
   private final Timer lagTimer;
   private final Timer lagBackTimer;
   int turtles;

   public Tips() {
      super("Tips", Module.Category.Misc);
      this.visualRange.setChinese("\u53ef\u89c6\u8303\u56f4");
      this.friends = this.add(new BooleanSetting("Friends", false, this.visualRange::isOpen));
      this.friends.setChinese("\u597d\u53cb");
      this.popCounter = this.add(new BooleanSetting("PopCounter", true));
      this.popCounter.setChinese("\u56fe\u817e\u8ba1\u6570");
      this.deathCoords = this.add(new BooleanSetting("DeathCoords", true));
      this.deathCoords.setChinese("\u6b7b\u4ea1\u5750\u6807");
      this.serverLag = this.add(new BooleanSetting("ServerLag", true));
      this.serverLag.setChinese("\u670d\u52a1\u5668\u5ef6\u8fdf");
      this.lagBack = this.add(new BooleanSetting("LagBack", true));
      this.lagBack.setChinese("\u56de\u5f39");
      this.potion = this.add(new BooleanSetting("Potion", true).setParent());
      this.potion.setChinese("\u836f\u6c34");
      this.resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, this.potion::isOpen));
      this.resistanceLevelCheck.setChinese("\u6297\u6027\u7b49\u7ea7\u68c0\u67e5");
      this.yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, this.potion::isOpen));
      this.yOffset.setChinese("Y\u8f74\u504f\u79fb");
      this.df = new DecimalFormat("0.0");
      this.color = new Color(190, 0, 0).getRGB();
      this.lagTimer = new Timer();
      this.lagBackTimer = new Timer();
      this.turtles = 0;
      this.setChinese("\u63d0\u793a");
      INSTANCE = this;
   }

   @EventListener
   public void onAddEntity(EntitySpawnEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "\u00a7f entered your visual range.",
               event.getEntity().getId() + 777
            );
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "\u00a7f left your visual range.", event.getEntity().getId() + 777
            );
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.potion.getValue()) {
         this.turtles = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
      }
   }

   @EventListener
   public void onPacketEvent(PacketEvent.Receive event) {
      this.lagTimer.reset();
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagBackTimer.reset();
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.serverLag.getValue() && this.lagTimer.passedS(1.4)) {
         String text = "Server not responding (" + this.df.format(this.lagTimer.getMs() / 1000.0) + "s)";
         drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 19, this.color, true);
      }

      if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.5)) {
         String text = "Lagback (" + this.df.format((1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
         drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 28, this.color, true);
      }

      if (this.potion.getValue()) {
         StringBuilder stringBuilder = new StringBuilder();
         if (this.turtles > 0) {
            stringBuilder.append("\u00a7e").append(this.turtles);
         }

         if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
            && (!this.resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("\u00a79").append(mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("\u00a74").append(mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("\u00a7b").append(mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
         }

         drawContext.drawText(
            mc.textRenderer,
            stringBuilder.toString(),
            mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(stringBuilder.toString()) / 2,
            mc.getWindow().getScaledHeight() / 2 + 9 - this.yOffset.getValueInt(),
            -1,
            true
         );
      }
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (this.popCounter.getValue()) {
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = Alien.POP.popContainer.get(player.getName().getString());
            if (l_Count == 1) {
               if (player.equals(mc.player)) {
                  this.sendMessage("\u00a7fYou\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
               } else {
                  this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
               }
            } else if (player.equals(mc.player)) {
               this.sendMessage("\u00a7fYou\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
            } else {
               this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("\u00a7fYou\u00a7r died.", player.getId());
         } else {
            this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died.", player.getId());
         }
      }

      if (this.deathCoords.getValue() && player == mc.player) {
         this.sendMessage("\u00a74You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
      }
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      if (this.popCounter.getValue()) {
         PlayerEntity player = event.getPlayer();
         int l_Count = 1;
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = Alien.POP.popContainer.get(player.getName().getString());
         }

         if (l_Count == 1) {
            if (player.equals(mc.player)) {
               this.sendMessage("\u00a7fYou\u00a7r popped \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
            } else {
               this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rpopped \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("\u00a7fYou\u00a7r popped \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
         } else {
            this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rhas popped \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
         }
      }
   }

   public void sendMessage(String message, int id) {
      if (!nullCheck()) {
         if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
            CommandManager.sendMessageId("\u00a7f[\u00a73" + this.getName() + "\u00a7f] " + message, id);
            return;
         }

         CommandManager.sendMessageId(message, id);
      }
   }
}
