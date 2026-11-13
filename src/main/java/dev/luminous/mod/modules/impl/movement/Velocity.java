package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;

public class Velocity extends Module {
   public static Velocity INSTANCE;
   private final EnumSetting<Velocity.Mode> mode = this.add(new EnumSetting<>("Mode", Velocity.Mode.Plain));
   private final SliderSetting lagPause;
   public final BooleanSetting ignorePearlLag;
   private final SliderSetting phaseTime;
   public final BooleanSetting noRotation;
   public final BooleanSetting flagInWall;
   public final BooleanSetting whenPushOutOfBlocks;
   public final BooleanSetting staticSetting;
   public final BooleanSetting cancelAll;
   private final SliderSetting horizontal;
   private final SliderSetting vertical;
   public final BooleanSetting whileLiquid;
   public final BooleanSetting whileElytra;
   public final BooleanSetting noClimb;
   public final BooleanSetting waterPush;
   public final BooleanSetting entityPush;
   public final BooleanSetting blockPush;
   public final BooleanSetting fishBob;
   public final Timer pearlTimer;
   private final Timer lagBackTimer;
   private boolean flag;
   static boolean pushOutOfBlocks = false;

   public Velocity() {
      super("Velocity", Module.Category.Movement);
      this.mode.setChinese("\u6a21\u5f0f");
      this.lagPause = this.add(new SliderSetting("LagPause", 50, 0, 500, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)));
      this.lagPause.setChinese("\u5ef6\u8fdf\u6682\u505c");
      this.ignorePearlLag = this.add(
         new BooleanSetting("IgnorePearlLag", true, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
      );
      this.ignorePearlLag.setChinese("\u5ffd\u7565\u73cd\u73e0\u5ef6\u8fdf");
      this.phaseTime = this.add(
         new SliderSetting(
            "PhaseTime", 250, 0, 1000, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.ignorePearlLag.isOpen()
         )
      );
      this.phaseTime.setChinese("Phase\u65f6\u95f4");
      this.noRotation = this.add(new BooleanSetting("NoRotation", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)));
      this.noRotation.setChinese("\u65e0\u65cb\u8f6c");
      this.flagInWall = this.add(
         new BooleanSetting("FlagInWall", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
      );
      this.flagInWall.setChinese("\u5899\u5185\u6807\u5fd7");
      this.whenPushOutOfBlocks = this.add(
         new BooleanSetting(
            "WhenPushOutOfBlocks", false, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.flagInWall.isOpen()
         )
      );
      this.whenPushOutOfBlocks.setChinese("\u63a8\u51fa\u65f6");
      this.staticSetting = this.add(new BooleanSetting("StaticSetting", false, () -> this.mode.is(Velocity.Mode.Grim)));
      this.staticSetting.setChinese("\u9759\u6001");
      this.cancelAll = this.add(new BooleanSetting("CancelAll", false, () -> !this.mode.is(Velocity.Mode.None)));
      this.cancelAll.setChinese("\u53d6\u6d88\u5168\u90e8");
      this.horizontal = this.add(new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue()));
      this.horizontal.setChinese("\u6c34\u5e73");
      this.vertical = this.add(new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue()));
      this.vertical.setChinese("\u5782\u76f4");
      this.whileLiquid = this.add(new BooleanSetting("WhileLiquid", false));
      this.whileLiquid.setChinese("\u6db2\u4f53\u4e2d");
      this.whileElytra = this.add(new BooleanSetting("WhileElytra", false));
      this.whileElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.noClimb = this.add(new BooleanSetting("NoClimb", false));
      this.noClimb.setChinese("\u65e0\u6500\u722c");
      this.waterPush = this.add(new BooleanSetting("WaterPush", false));
      this.waterPush.setChinese("\u65e0\u6c34\u63a8\u52a8");
      this.entityPush = this.add(new BooleanSetting("EntityPush", true));
      this.entityPush.setChinese("\u65e0\u5b9e\u4f53\u63a8\u52a8");
      this.blockPush = this.add(new BooleanSetting("BlockPush", true));
      this.blockPush.setChinese("\u65e0\u65b9\u5757\u63a8\u52a8");
      this.fishBob = this.add(new BooleanSetting("FishBob", true));
      this.fishBob.setChinese("\u65e0\u9c7c\u94a9");
      this.pearlTimer = new Timer();
      this.lagBackTimer = new Timer();
      this.setChinese("\u53cd\u51fb\u9000");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.is(Velocity.Mode.None)
         ? null
         : this.mode.getValue().name()
            + ", "
            + (this.cancelAll.getValue() ? "Cancel" : this.horizontal.getValueInt() + "%, " + this.vertical.getValueInt() + "%");
   }

   @EventListener
   public void onRotate(UpdateRotateEvent var1) {
      if (this.noRotation()) {
         var1.setRotation(Alien.ROTATION.rotationYaw, 89.0F);
      }
   }

   public boolean noRotation() {
      return this.isOn() && (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && EntityUtil.isInsideBlock() && this.noRotation.getValue();
   }

   @EventListener
   public void onVelocity(EntityVelocityUpdateEvent var1) {
      if (!nullCheck()
         && var1.getEntity() == mc.player
         && !this.mode.is(Velocity.Mode.None)
         && (!mc.player.isInFluid() || this.whileLiquid.getValue())
         && (!mc.player.isFallFlying() || this.whileElytra.getValue())) {
         switch ((Velocity.Mode)this.mode.getValue()) {
            case Plain:
            default:
               break;
            case Grim:
               if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                  return;
               }

               if (!EntityUtil.isInsideBlock() && this.getPos() == null && (!this.staticSetting.getValue() || !MovementUtil.isStatic())) {
                  return;
               }

               if (var1.getX() != 0.0 || var1.getZ() != 0.0) {
                  this.flag = true;
               }
               break;
            case Wall:
               if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                  return;
               }

               if (!EntityUtil.isInsideBlock()) {
                  return;
               }

               if (var1.getX() != 0.0 || var1.getZ() != 0.0) {
                  this.flag = true;
               }
         }

         if (this.cancelAll.getValue()) {
            var1.cancel();
         } else {
            double var2 = this.horizontal.getValueInt() / 100.0;
            double var4 = this.vertical.getValueInt() / 100.0;
            var1.setX(var1.getX() * var2);
            var1.setZ(var1.getZ() * var2);
            var1.setY(var1.getY() * var4);
         }
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive var1) {
      if (var1.getPacket() instanceof PlayerPositionLookS2CPacket && (!this.ignorePearlLag.getValue() || this.pearlTimer.passed(this.phaseTime.getValueInt()))) {
         this.lagBackTimer.reset();
      }

      if (!nullCheck()
         && (!mc.player.isInFluid() || this.whileLiquid.getValue())
         && this.fishBob.getValue()
         && var1.getPacket() instanceof EntityStatusS2CPacket var2
         && var2.getStatus() == 31
         && var2.getEntity(mc.world) instanceof FishingBobberEntity var3
         && var3.getHookedEntity() == mc.player) {
         var1.setCancelled(true);
      }
   }

   @EventListener
   public void onUpdate(TickEvent var1) {
      if (!nullCheck() && !var1.isPost() && (!mc.player.isInFluid() || this.whileLiquid.getValue())) {
         if (this.flagInWall.getValue()) {
            pushOutOfBlocks = false;
            pushOutOfBlocks(mc.player.getX() - mc.player.getWidth() * 0.35, mc.player.getZ() + mc.player.getWidth() * 0.35);
            pushOutOfBlocks(mc.player.getX() - mc.player.getWidth() * 0.35, mc.player.getZ() - mc.player.getWidth() * 0.35);
            pushOutOfBlocks(mc.player.getX() + mc.player.getWidth() * 0.35, mc.player.getZ() - mc.player.getWidth() * 0.35);
            pushOutOfBlocks(mc.player.getX() + mc.player.getWidth() * 0.35, mc.player.getZ() + mc.player.getWidth() * 0.35);
         }

         if ((!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) && this.flag) {
            if (this.lagBackTimer.passedMs(this.lagPause.getValue())
               && (this.flagInWall.getValue() && (!pushOutOfBlocks || this.whenPushOutOfBlocks.getValue()) || !EntityUtil.isInsideBlock())) {
               mc.getNetworkHandler()
                  .sendPacket(
                     new Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), Alien.ROTATION.rotationYaw, Alien.ROTATION.rotationPitch, mc.player.isOnGround()
                     )
                  );
               BlockPos var2 = this.getPos();
               if (var2 != null) {
                  mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, var2, mc.player.getHorizontalFacing().getOpposite()));
               }
            }

            this.flag = false;
         }
      }
   }

   public BlockPos getPos() {
      return mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.OBSIDIAN ? mc.player.getBlockPos().down() : null;
   }

   private static void pushOutOfBlocks(double var0, double var2) {
      BlockPos var4 = BlockPos.ofFloored(var0, mc.player.getY(), var2);
      if (wouldCollideAt(var4)) {
         double var5 = var0 - var4.getX();
         double var7 = var2 - var4.getZ();
         Direction var9 = null;
         double var10 = Double.MAX_VALUE;
         Direction[] var12 = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for (Direction var16 : var12) {
            double var17 = var16.getAxis().choose(var5, 0.0, var7);
            double var19 = var16.getDirection() == AxisDirection.POSITIVE ? 1.0 - var17 : var17;
            if (var19 < var10 && !wouldCollideAt(var4.offset(var16))) {
               var10 = var19;
               var9 = var16;
            }
         }

         if (var9 != null) {
            pushOutOfBlocks = true;
         }
      }
   }

   private static boolean wouldCollideAt(BlockPos var0) {
      Box var1 = mc.player.getBoundingBox();
      Box var2 = new Box(var0.getX(), var1.minY, var0.getZ(), var0.getX() + 1.0, var1.maxY, var0.getZ() + 1.0).contract(1.0E-7);
      return mc.player.getWorld().canCollide(mc.player, var2);
   }

   public static enum Mode {
      Plain,
      Grim,
      Wall,
      None;
   }
}
