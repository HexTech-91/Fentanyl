package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;

public class AutoMaceMinus extends Module {
   public static AutoMaceMinus INSTANCE;
   private final Timer cooldown = new Timer();
   private final Timer flyDuration = new Timer();
   private final Timer antiKickTimer = new Timer();
   private final Timer antiKick2Timer = new Timer();
   private boolean forceUp = false;
   private boolean waitingForLagback = false;
   private boolean isFlying = false;
   private final Timer fallTimer = new Timer();
   private boolean fallPhase = false;
   public final SliderSetting groundDistance = this.add(new SliderSetting("GroundDistance", 1.0, 1.0, 10.0, 0.1));
   public final SliderSetting rubberbandDelay = this.add(new SliderSetting("RubberbandDelay", 300.0, 0.0, 1000.0, 1.0));
   public final SliderSetting speedConfig = this.add(new SliderSetting("Speed", 2.5, 0.1, 10.0, 0.1));
   public final SliderSetting vspeedConfig = this.add(new SliderSetting("VerticalSpeed", 1.0, 0.1, 5.0, 0.1));
   public final BooleanSetting antiKickConfig = this.add(new BooleanSetting("AntiKick", true).setParent());
   public final BooleanSetting up = this.add(new BooleanSetting("Up", true, this.antiKickConfig::isOpen));
   public final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false));
   public final BooleanSetting landDisable = this.add(new BooleanSetting("LandDisable", true));
   public final BooleanSetting waterDisable = this.add(new BooleanSetting("WaterDisable", true));
   public final BooleanSetting slowFallDisable = this.add(new BooleanSetting("SlowFallDisable", true));

   public AutoMaceMinus() {
      super(
         "AutoMace-",
         "\u63a5\u8fd1\u5730\u9762\u65f6\u81ea\u52a8\u98de\u884c\u5e76\u5f3a\u5236\u4e0a\u5347\u76f4\u5230\u6a61\u80f6\u5708",
         Module.Category.Combat
      );
      this.setChinese("\u81ea\u52a8\u91cd\u9524-");
      INSTANCE = this;
      this.groundDistance.setChinese("\u5730\u9762\u8ddd\u79bb");
      this.rubberbandDelay.setChinese("\u6a61\u80f6\u5708\u5ef6\u8fdf");
      this.speedConfig.setChinese("\u6c34\u5e73\u901f\u5ea6");
      this.vspeedConfig.setChinese("\u5782\u76f4\u901f\u5ea6");
      this.antiKickConfig.setChinese("\u53cd\u8e22\u51fa");
      this.up.setChinese("\u5411\u4e0a");
      this.allowSneak.setChinese("\u5141\u8bb8\u6f5c\u884c");
      this.landDisable.setChinese("\u843d\u5730\u5173\u95ed");
      this.waterDisable.setChinese("\u6c34\u4e2d\u5173\u95ed");
      this.slowFallDisable.setChinese("\u7f13\u964d\u5173\u95ed");
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (nullCheck()) {
         this.disable();
      } else {
         this.cooldown.setMs(100L);
         this.forceUp = false;
         this.waitingForLagback = false;
         this.isFlying = false;
         this.fallPhase = false;
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.releaseFly();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost()) {
            if (this.isFlying) {
               if (this.landDisable.getValue() && mc.player.isOnGround()) {
                  this.disable();
                  return;
               }

               if (this.waterDisable.getValue() && mc.player.isTouchingWater()) {
                  this.disable();
                  return;
               }

               if (this.slowFallDisable.getValue() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                  this.sendMessage("\u00a7c\u68c0\u6d4b\u5230\u7f13\u964d\u6548\u679c\uff0c\u81ea\u52a8\u5173\u95ed");
                  this.disable();
                  return;
               }

               if (this.forceUp) {
                  this.sendFakePackets();
               }
            } else if (this.cooldown.passedMs(this.自适应延迟(100L)) && !this.waitingForLagback) {
               if (this.slowFallDisable.getValue() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                  return;
               }

               if (this.isNearGround() && !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0) {
                  this.进入飞行循环();
               }
            }
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.isFlying) {
            if (this.antiKickTimer.passed(3900L) && this.antiKickConfig.getValue() && !mc.player.isOnGround()) {
               mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), false));
               this.antiKickTimer.reset();
            } else if (this.antiKick2Timer.passed(4000L) && this.antiKickConfig.getValue() && !mc.player.isOnGround() && this.up.getValue()) {
               mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.04, mc.player.getZ(), false));
               this.antiKick2Timer.reset();
            }

            double[] move = MovementUtil.directionSpeed(this.speedConfig.getValueFloat());
            if (move[0] == 0.0 && move[1] == 0.0) {
               float yaw = (float)Math.toRadians(mc.player.getYaw());
               move[0] = -Math.sin(yaw) * this.speedConfig.getValueFloat();
               move[1] = Math.cos(yaw) * this.speedConfig.getValueFloat();
            }

            MovementUtil.setMotionX(move[0]);
            MovementUtil.setMotionZ(move[1]);
            if (this.forceUp) {
               MovementUtil.setMotionY(0.0);
            }
         }
      }
   }

   @EventListener(
      priority = -100
   )
   public void keyboard(KeyboardInputEvent event) {
      if (!this.allowSneak.getValue()) {
         mc.player.input.sneaking = false;
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.isFlying && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.releaseFly();
            this.waitingForLagback = false;
            this.cooldown.reset();
         }
      }
   }

   private void sendFakePackets() {
      double[] move = MovementUtil.directionSpeed(this.speedConfig.getValueFloat());
      float yaw = (float)Math.toRadians(mc.player.getYaw());
      if (move[0] == 0.0 && move[1] == 0.0) {
         move[0] = -Math.sin(yaw);
         move[1] = Math.cos(yaw);
      }

      double fakeX = mc.player.getX() + move[0] * 8.0;
      double fakeY = mc.player.getY() + 10.0;
      double fakeZ = mc.player.getZ() + move[1] * 8.0;
      mc.player.networkHandler.sendPacket(new Full(fakeX, fakeY, fakeZ, mc.player.getYaw(), mc.player.getPitch(), false));
      mc.player.networkHandler.sendPacket(new OnGroundOnly(false));
   }

   private void enableFly() {
      this.isFlying = true;
      this.antiKickTimer.reset();
      this.antiKick2Timer.reset();
   }

   private void releaseFly() {
      this.forceUp = false;
      this.isFlying = false;
      this.fallPhase = false;
      if (mc.player != null) {
         MovementUtil.setMotionY(-0.1);
      }
   }

   private void 进入飞行循环() {
      this.enableFly();
      this.flyDuration.reset();
      this.fallTimer.reset();
      this.forceUp = true;
      this.fallPhase = true;
      this.waitingForLagback = true;
      this.cooldown.reset();
      this.sendFakePackets();
   }

   private long 自适应延迟(long baseMs) {
      float timer = Alien.TIMER.get();
      return timer <= 1.0F ? baseMs : (long)((float)baseMs / timer);
   }

   private boolean isNearGround() {
      double feetY = mc.player.getY();
      int playerX = mc.player.getBlockX();
      int playerZ = mc.player.getBlockZ();
      int startY = (int)Math.floor(feetY);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int y = startY; y >= startY - 15; y--) {
               BlockPos pos = new BlockPos(playerX + dx, y, playerZ + dz);
               BlockState state = mc.world.getBlockState(pos);
               if (state.getBlock() == Blocks.WATER || !state.getFluidState().isEmpty()) {
                  double waterSurface = y + state.getFluidState().getHeight();
                  double dist = feetY - waterSurface;
                  if (dist >= -0.6 && dist <= 2.5) {
                     return true;
                  }
                  break;
               }

               if (state.blocksMovement()) {
                  double blockTop = y + 1.0;
                  double dist = feetY - blockTop;
                  if (state.getBlock() == Blocks.COBWEB && dist >= -0.6 && dist <= 2.5) {
                     return true;
                  }

                  if (dist >= -0.6 && dist <= this.groundDistance.getValue()) {
                     return true;
                  }

                  if (!(dist < -0.6)) {
                     break;
                  }
               }
            }
         }
      }

      return false;
   }
}
