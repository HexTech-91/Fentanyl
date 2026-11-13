package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.hit.BlockHitResult;

public class PacketLogger extends Module {
   public static PacketLogger INSTANCE;
   private final BooleanSetting moveFull = this.add(new BooleanSetting("MoveFull", true));
   private final BooleanSetting movePos;
   private final BooleanSetting moveLook;
   private final BooleanSetting moveGround;
   private final BooleanSetting vehicleMove;
   private final BooleanSetting playerAction;
   private final BooleanSetting updateSlot;
   private final BooleanSetting handSwing;
   private final BooleanSetting pong;
   private final BooleanSetting interactEntity;
   private final BooleanSetting interactBlock;
   private final BooleanSetting interactItem;
   private final BooleanSetting closeScreen;
   private final BooleanSetting command;
   private final BooleanSetting status;
   private final BooleanSetting clickSlot;
   private final BooleanSetting pickInventory;
   private final BooleanSetting teleportConfirm;
   private final BooleanSetting s2cVelocity;

   public PacketLogger() {
      super("PacketLogger", Module.Category.Misc);
      this.moveFull.setChinese("\u5b8c\u6574\u79fb\u52a8");
      this.movePos = this.add(new BooleanSetting("MovePos", true));
      this.movePos.setChinese("\u4f4d\u7f6e\u79fb\u52a8");
      this.moveLook = this.add(new BooleanSetting("MoveLook", true));
      this.moveLook.setChinese("\u89c6\u89d2\u79fb\u52a8");
      this.moveGround = this.add(new BooleanSetting("MoveGround", true));
      this.moveGround.setChinese("\u5730\u9762\u79fb\u52a8");
      this.vehicleMove = this.add(new BooleanSetting("VehicleMove", true));
      this.vehicleMove.setChinese("\u8f7d\u5177\u79fb\u52a8");
      this.playerAction = this.add(new BooleanSetting("PlayerAction", true));
      this.playerAction.setChinese("\u73a9\u5bb6\u52a8\u4f5c");
      this.updateSlot = this.add(new BooleanSetting("UpdateSlot", true));
      this.updateSlot.setChinese("\u5207\u6362\u69fd\u4f4d");
      this.handSwing = this.add(new BooleanSetting("HandSwing", true));
      this.handSwing.setChinese("\u6325\u52a8\u624b\u81c2");
      this.pong = this.add(new BooleanSetting("Pong", true));
      this.pong.setChinese("Pong\u54cd\u5e94");
      this.interactEntity = this.add(new BooleanSetting("InteractEntity", true));
      this.interactEntity.setChinese("\u5b9e\u4f53\u4ea4\u4e92");
      this.interactBlock = this.add(new BooleanSetting("InteractBlock", true));
      this.interactBlock.setChinese("\u65b9\u5757\u4ea4\u4e92");
      this.interactItem = this.add(new BooleanSetting("InteractItem", true));
      this.interactItem.setChinese("\u7269\u54c1\u4ea4\u4e92");
      this.closeScreen = this.add(new BooleanSetting("CloseScreen", true));
      this.closeScreen.setChinese("\u5173\u95ed\u754c\u9762");
      this.command = this.add(new BooleanSetting("Command", true));
      this.command.setChinese("\u5ba2\u6237\u7aef\u6307\u4ee4");
      this.status = this.add(new BooleanSetting("Status", true));
      this.status.setChinese("\u5ba2\u6237\u7aef\u72b6\u6001");
      this.clickSlot = this.add(new BooleanSetting("ClickSlot", true));
      this.clickSlot.setChinese("\u70b9\u51fb\u69fd\u4f4d");
      this.pickInventory = this.add(new BooleanSetting("PickInventory", true));
      this.pickInventory.setChinese("\u62fe\u53d6\u7269\u54c1");
      this.teleportConfirm = this.add(new BooleanSetting("TeleportConfirm", true));
      this.teleportConfirm.setChinese("\u4f20\u9001\u786e\u8ba4");
      this.s2cVelocity = this.add(new BooleanSetting("S2cVelocity", true));
      this.s2cVelocity.setChinese("\u670d\u52a1\u7aef\u901f\u5ea6");
      this.setChinese("\u6570\u636e\u5305\u8bb0\u5f55");
      INSTANCE = this;
   }

   private void logPacket(String msg, Object... args) {
      String s = String.format(msg, args);
      CommandManager.sendMessage(s);
   }

   @EventListener(
      priority = 999999
   )
   public void velocity(EntityVelocityUpdateEvent event) {
      if (this.s2cVelocity.getValue() && event.getEntity() == mc.player) {
         this.logPacket("S2C Velocity, x: %s, y: %s, z: %s, isExplosion: %s", event.getX(), event.getY(), event.getZ(), event.isExplosion());
      }
   }

   @EventListener(
      priority = -999999
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof Full packet && this.moveFull.getValue()) {
         StringBuilder builder = new StringBuilder();
         builder.append("PlayerMove Full - ");
         if (packet.changesPosition()) {
            builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         if (packet.changesLook()) {
            builder.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
         }

         builder.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builder.toString());
      }

      if (event.getPacket() instanceof PositionAndOnGround packet && this.movePos.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove PosGround - ");
         if (packet.changesPosition()) {
            builderx.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof LookAndOnGround packet && this.moveLook.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove LookGround - ");
         if (packet.changesLook()) {
            builderx.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof OnGroundOnly packet && this.moveGround.getValue()) {
         String s = "PlayerMove Ground - onground: " + packet.isOnGround();
         this.logPacket(s);
      }

      if (event.getPacket() instanceof VehicleMoveC2SPacket packet && this.vehicleMove.getValue()) {
         this.logPacket(
            "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch()
         );
      }

      if (event.getPacket() instanceof PlayerActionC2SPacket packet && this.playerAction.getValue() && packet.getDirection() != null) {
         this.logPacket(
            "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString()
         );
      }

      if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet && this.updateSlot.getValue()) {
         this.logPacket("UpdateSlot - slot: %d", packet.getSelectedSlot());
      }

      if (event.getPacket() instanceof HandSwingC2SPacket packet && this.handSwing.getValue()) {
         this.logPacket("HandSwing - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof CommonPongC2SPacket packet && this.pong.getValue()) {
         this.logPacket("Pong - %d", packet.getParameter());
      }

      if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && this.interactEntity.getValue()) {
         this.logPacket("InteractEntity - Entity: %s, id: %s", Criticals.getEntity(packet).getName().getString(), Criticals.getEntity(packet).getId());
      }

      if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet && this.interactBlock.getValue()) {
         BlockHitResult blockHitResult = packet.getBlockHitResult();
         this.logPacket(
            "InteractBlock - pos: %s, dir: %s, hand: %s",
            blockHitResult.getBlockPos().toShortString(),
            blockHitResult.getSide().name(),
            packet.getHand().name()
         );
      }

      if (event.getPacket() instanceof PlayerInteractItemC2SPacket packet && this.interactItem.getValue()) {
         this.logPacket("InteractItem - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof CloseHandledScreenC2SPacket packet && this.closeScreen.getValue()) {
         this.logPacket("CloseScreen - id: %s", packet.getSyncId());
      }

      if (event.getPacket() instanceof ClientCommandC2SPacket packet && this.command.getValue()) {
         this.logPacket("ClientCommand - mode: %s", packet.getMode().name());
      }

      if (event.getPacket() instanceof ClientStatusC2SPacket packet && this.status.getValue()) {
         this.logPacket("ClientStatus - mode: %s", packet.getMode().name());
      }

      if (event.getPacket() instanceof ClickSlotC2SPacket packet && this.clickSlot.getValue()) {
         this.logPacket(
            "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.getActionType().name(), packet.getSlot(), packet.getButton(), packet.getSyncId()
         );
      }

      if (event.getPacket() instanceof PickFromInventoryC2SPacket packet && this.pickInventory.getValue()) {
         this.logPacket("PickInventory - slot: %s", packet.getSlot());
      }

      if (event.getPacket() instanceof TeleportConfirmC2SPacket packet && this.teleportConfirm.getValue()) {
         this.logPacket("TeleportConfirm - id: %s", packet.getTeleportId());
      }
   }
}
