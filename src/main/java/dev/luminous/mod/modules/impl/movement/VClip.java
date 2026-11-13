package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;

public class VClip extends Module {
   private final EnumSetting<VClip.Mode> mode = this.add(new EnumSetting<>("\u6a21\u5f0f", VClip.Mode.Jump));
   private final BooleanSetting strict = this.add(
      new BooleanSetting(
         "Strict",
         false,
         () -> this.mode.getValue() == VClip.Mode.Teleport || this.mode.getValue() == VClip.Mode.Teleport2 || this.mode.getValue() == VClip.Mode.TeleportMax
      )
   );
   private final BooleanSetting safety;
   private final BooleanSetting maxUp;
   private final SliderSetting maxCheckHeight;
   private final SliderSetting blockThreshold;

   public VClip() {
      super("VClip", Module.Category.Movement);
      this.strict.setChinese("\u4e25\u683c");
      this.safety = this.add(
         new BooleanSetting(
            "Safety",
            true,
            () -> this.mode.getValue() == VClip.Mode.Teleport || this.mode.getValue() == VClip.Mode.Teleport2 || this.mode.getValue() == VClip.Mode.TeleportMax
         )
      );
      this.safety.setChinese("\u5b89\u5168");
      this.maxUp = this.add(
         new BooleanSetting("MaxUp", true, () -> this.mode.getValue() == VClip.Mode.TeleportMax || this.mode.getValue() == VClip.Mode.EscapeTrap)
      );
      this.maxUp.setChinese("\u6700\u5927\u5411\u4e0a");
      this.maxCheckHeight = this.add(new SliderSetting("MaxCheckHeight", 10.0, 1.0, 30.0, 1.0, () -> this.mode.getValue() == VClip.Mode.TeleportMax));
      this.maxCheckHeight.setChinese("\u6700\u5927\u68c0\u67e5\u9ad8\u5ea6");
      this.blockThreshold = this.add(new SliderSetting("BlockThreshold", 3.0, 1.0, 10.0, 1.0, () -> this.mode.getValue() == VClip.Mode.TeleportMax));
      this.blockThreshold.setChinese("\u65b9\u5757\u9608\u503c");
      this.setChinese("\u7eb5\u5411\u7a7f\u5899");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.strict.getValue() && !this.checkStrictCondition()) {
         this.disable();
      } else {
         this.disable();
         switch ((VClip.Mode)this.mode.getValue()) {
            case Glitch:
               this.executeGlitch();
               break;
            case Teleport:
               this.executeTeleport(3);
               break;
            case Teleport2:
               this.executeTeleport(4);
               break;
            case Jump:
               this.executeJump();
               break;
            case TeleportMax:
               this.executeTeleportMax();
               break;
            case EscapeTrap:
               this.executeEscapeTrap();
         }
      }
   }

   private boolean checkStrictCondition() {
      if (mc.player != null && mc.world != null) {
         BlockPos playerPos = mc.player.getBlockPos();
         BlockPos headPos = playerPos.up(1);
         BlockPos headTopPos = playerPos.up(2);
         boolean hasHeadBlock = !this.isPositionSafe(headPos);
         boolean hasHeadTopBlock = !this.isPositionSafe(headTopPos);
         return hasHeadBlock || hasHeadTopBlock;
      } else {
         return false;
      }
   }

   private void executeTeleport(int distance) {
      if (mc.player != null && mc.world != null) {
         if (!this.safety.getValue() || this.isTeleportSafe(distance)) {
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + distance, mc.player.getZ());
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
         }
      }
   }

   private void executeTeleportMax() {
      VClip.TeleportDecision decision = this.makeTeleportDecision();
      switch (decision.type) {
         case USE_TELEPORT:
            if (!this.safety.getValue() || this.isTeleportSafe(3)) {
               this.executeTeleport(3);
            }
            break;
         case USE_TELEPORT2:
            if (!this.safety.getValue() || this.isTeleportSafe(4)) {
               this.executeTeleport(4);
            }
         case ABORT_TRAP:
         case ABORT_UNSAFE:
      }
   }

   private VClip.TeleportDecision makeTeleportDecision() {
      if (mc.player != null && mc.world != null) {
         VClip.BlockAnalysis analysis = this.analyzeBlocksAbove();
         if (analysis.consecutiveBlocks >= this.blockThreshold.getValueInt()) {
            return new VClip.TeleportDecision(
               VClip.DecisionType.ABORT_TRAP,
               "\u68c0\u6d4b\u5230\u591a\u5c42\u65b9\u5757\u9677\u9631 (" + analysis.consecutiveBlocks + "\u5c42\u8fde\u7eed\u65b9\u5757)"
            );
         } else {
            boolean teleportSafe = !this.safety.getValue() || this.isTeleportSafe(3);
            boolean teleport2Safe = !this.safety.getValue() || this.isTeleportSafe(4);
            if (analysis.hasAntiStep) {
               if (teleport2Safe) {
                  return new VClip.TeleportDecision(VClip.DecisionType.USE_TELEPORT2, "\u68c0\u6d4b\u5230AntiStep\u65b9\u5757");
               } else {
                  return teleportSafe
                     ? new VClip.TeleportDecision(VClip.DecisionType.USE_TELEPORT, "Teleport2\u4e0d\u5b89\u5168\uff0c\u56de\u9000\u5230Teleport")
                     : new VClip.TeleportDecision(VClip.DecisionType.ABORT_UNSAFE, "\u4e24\u79cd\u6a21\u5f0f\u90fd\u4e0d\u5b89\u5168");
               }
            } else if (teleportSafe) {
               return new VClip.TeleportDecision(VClip.DecisionType.USE_TELEPORT, "\u65e0AntiStep\uff0c\u4f7f\u7528Teleport");
            } else {
               return teleport2Safe
                  ? new VClip.TeleportDecision(VClip.DecisionType.USE_TELEPORT2, "Teleport\u4e0d\u5b89\u5168\uff0c\u4f7f\u7528Teleport2")
                  : new VClip.TeleportDecision(VClip.DecisionType.ABORT_UNSAFE, "\u4e24\u79cd\u6a21\u5f0f\u90fd\u4e0d\u5b89\u5168");
            }
         }
      } else {
         return new VClip.TeleportDecision(VClip.DecisionType.ABORT_UNSAFE, "\u73a9\u5bb6\u6216\u4e16\u754c\u4e3anull");
      }
   }

   private VClip.BlockAnalysis analyzeBlocksAbove() {
      int playerX = mc.player.getBlockX();
      int playerY = mc.player.getBlockY();
      int playerZ = mc.player.getBlockZ();
      int consecutiveBlocks = 0;
      boolean hasAntiStep = false;
      int maxHeight = this.maxCheckHeight.getValueInt();

      for (int offset = 1; offset <= maxHeight; offset++) {
         BlockPos pos = new BlockPos(playerX, playerY + offset, playerZ);
         BlockState state = mc.world.getBlockState(pos);
         if (!this.isPositionSafe(pos)) {
            consecutiveBlocks++;
            if (offset == 3) {
               hasAntiStep = true;
            }
         } else {
            consecutiveBlocks = 0;
         }
      }

      return new VClip.BlockAnalysis(consecutiveBlocks, hasAntiStep);
   }

   private boolean isTeleportSafe(int distance) {
      if (mc.player != null && mc.world != null) {
         int targetX = mc.player.getBlockX();
         int targetY = (int)Math.floor(mc.player.getY() + distance);
         int targetZ = mc.player.getBlockZ();
         BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
         return this.isPositionSafe(targetPos);
      } else {
         return false;
      }
   }

   private boolean isPositionSafe(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return state.isAir() || state.getCollisionShape(mc.world, pos) == null || state.getCollisionShape(mc.world, pos).isEmpty();
      }
   }

   private void executeJump() {
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
      mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
   }

   private void executeGlitch() {
      double posX = mc.player.getX();
      double posY = Math.round(mc.player.getY());
      double posZ = mc.player.getZ();
      boolean onGround = mc.player.isOnGround();
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
      double halfY = 0.005;
      posY -= halfY;
      mc.player.setPosition(posX, posY, posZ);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
      posY -= halfY * 300.0;
      mc.player.setPosition(posX, posY, posZ);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(posX, posY, posZ, onGround));
   }

   private void executeEscapeTrap() {
      if (mc.player != null && mc.world != null) {
         BlockPos playerPos = mc.player.getBlockPos();
         BlockPos headPos = playerPos.up(2);
         ArrayList<BlockPos> candidatePositions = new ArrayList<>();

         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || z != 0) {
                  BlockPos checkPos = headPos.add(x, 0, z);
                  if (this.hasBlock(checkPos)) {
                     candidatePositions.add(checkPos.up());
                  }
               }
            }
         }

         if (this.hasBlock(headPos)) {
            candidatePositions.add(headPos.up());
         }

         for (BlockPos pos : candidatePositions) {
            if (!this.hasBlock(pos) && !this.hasBlock(pos.up()) && !this.hasBlock(pos.up(2))) {
               this.teleport(pos);
               return;
            }
         }

         if (this.maxUp.getValue()) {
            this.tryTeleportUp(4);
         }
      }
   }

   private boolean tryTeleportUp(int distance) {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (this.strict.getValue() && !this.checkStrictCondition()) {
         return false;
      } else if (this.safety.getValue() && !this.isTeleportSafe(distance)) {
         return false;
      } else {
         mc.player.setPosition(mc.player.getX(), mc.player.getY() + distance, mc.player.getZ());
         mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
         return true;
      }
   }

   private boolean hasBlock(BlockPos pos) {
      return mc.world != null && !mc.world.isAir(pos);
   }

   private void teleport(BlockPos pos) {
      if (!this.hasBlock(pos) && !this.hasBlock(pos.up()) && !this.hasBlock(pos.up(2))) {
         double x = pos.getX() + 0.5;
         double y = pos.getY();
         double z = pos.getZ() + 0.5;
         mc.player.setPosition(x, y, z);
         mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, true));
      }
   }

   private static class BlockAnalysis {
      final int consecutiveBlocks;
      final boolean hasAntiStep;

      BlockAnalysis(int consecutiveBlocks, boolean hasAntiStep) {
         this.consecutiveBlocks = consecutiveBlocks;
         this.hasAntiStep = hasAntiStep;
      }
   }

   private static enum DecisionType {
      USE_TELEPORT,
      USE_TELEPORT2,
      ABORT_TRAP,
      ABORT_UNSAFE;
   }

   public static enum Mode {
      Glitch,
      Teleport,
      Teleport2,
      Jump,
      TeleportMax,
      EscapeTrap;
   }

   private static class TeleportDecision {
      final VClip.DecisionType type;
      final String reason;

      TeleportDecision(VClip.DecisionType type, String reason) {
         this.type = type;
         this.reason = reason;
      }
   }
}
