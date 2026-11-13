package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class FastFall extends Module {
   private final EnumSetting<FastFall.Mode> mode = this.add(new EnumSetting<>("Mode", FastFall.Mode.Fast));
   private final BooleanSetting noLag;
   private final BooleanSetting useTimerSetting;
   private final SliderSetting timer;
   private final BooleanSetting anchor;
   private final SliderSetting height;
   private final Timer lagTimer;
   boolean onGround;
   private boolean useTimer;

   public FastFall() {
      super("FastFall", "Miyagi son simulator", Module.Category.Movement);
      this.mode.setChinese("\u6a21\u5f0f");
      this.noLag = this.add(new BooleanSetting("NoLag", true, () -> this.mode.getValue() == FastFall.Mode.Fast));
      this.noLag.setChinese("\u65e0\u5ef6\u8fdf");
      this.useTimerSetting = this.add(new BooleanSetting("UseTimerSetting", false));
      this.useTimerSetting.setChinese("\u4f7f\u7528Timer");
      this.timer = this.add(new SliderSetting("Timer", 2.5, 1.0, 8.0, 0.1, this.useTimerSetting::getValue));
      this.anchor = this.add(new BooleanSetting("Anchor", true));
      this.anchor.setChinese("\u5b9a\u8eab");
      this.height = this.add(new SliderSetting("Height", 10.0, 1.0, 20.0, 0.5));
      this.height.setChinese("\u9ad8\u5ea6");
      this.lagTimer = new Timer();
      this.onGround = false;
      this.setChinese("\u5feb\u901f\u5760\u843d");
      this.timer.setChinese("\u8ba1\u65f6\u5668\u52a0\u901f");
   }

   @Override
   public void onDisable() {
      this.useTimer = false;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener(
      priority = -100
   )
   public void onMove(MoveEvent event) {
      if (!nullCheck()
         && mc.player.isOnGround()
         && this.anchor.getValue()
         && this.traceDown() != 0
         && this.traceDown() <= this.height.getValue()
         && this.trace()) {
         event.setX(event.getX() * 0.05);
         event.setZ(event.getZ() * 0.05);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!(this.height.getValue() > 0.0) || !(this.traceDown() > this.height.getValue()))
         && !mc.player.isInsideWall()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.isHoldingOntoLadder()
         && this.lagTimer.passed(1000L)
         && !mc.player.isFallFlying()
         && !Fly.INSTANCE.isOn()
         && !nullCheck()
         && !Alien.PLAYER.isInWeb(mc.player)) {
         if (mc.player.isOnGround() && this.mode.getValue() == FastFall.Mode.Fast) {
            MovementUtil.setMotionY(MovementUtil.getMotionY() - (this.noLag.getValue() ? 0.62F : 1.0F));
         }

         if (this.useTimerSetting.getValue()) {
            if (!mc.player.isOnGround()) {
               if (this.onGround) {
                  this.useTimer = true;
               }

               if (MovementUtil.getMotionY() >= 0.0) {
                  this.useTimer = false;
               }

               this.onGround = false;
            } else {
               this.useTimer = false;
               MovementUtil.setMotionY(-0.08);
               this.onGround = true;
            }
         } else {
            this.useTimer = false;
         }
      }
   }

   @EventListener
   public void onTimer(TimerEvent event) {
      if (!nullCheck() && !mc.player.isOnGround() && this.useTimer) {
         event.set(this.timer.getValueFloat());
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!nullCheck() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagTimer.reset();
      }
   }

   private int traceDown() {
      int retval = 0;
      int y = (int)Math.round(mc.player.getY()) - 1;

      for (int tracey = y; tracey >= 0; tracey--) {
         HitResult trace = mc.world
            .raycast(
               new RaycastContext(mc.player.getPos(), new Vec3d(mc.player.getX(), tracey, mc.player.getZ()), ShapeType.COLLIDER, FluidHandling.NONE, mc.player)
            );
         if (trace != null && trace.getType() == Type.BLOCK) {
            return retval;
         }

         retval++;
      }

      return retval;
   }

   private boolean trace() {
      Box bbox = mc.player.getBoundingBox();
      Vec3d basepos = bbox.getCenter();
      double minX = bbox.minX;
      double minZ = bbox.minZ;
      double maxX = bbox.maxX;
      double maxZ = bbox.maxZ;
      Map<Vec3d, Vec3d> positions = new HashMap<>();
      positions.put(basepos, new Vec3d(basepos.x, basepos.y - 1.0, basepos.z));
      positions.put(new Vec3d(minX, basepos.y, minZ), new Vec3d(minX, basepos.y - 1.0, minZ));
      positions.put(new Vec3d(maxX, basepos.y, minZ), new Vec3d(maxX, basepos.y - 1.0, minZ));
      positions.put(new Vec3d(minX, basepos.y, maxZ), new Vec3d(minX, basepos.y - 1.0, maxZ));
      positions.put(new Vec3d(maxX, basepos.y, maxZ), new Vec3d(maxX, basepos.y - 1.0, maxZ));

      for (Vec3d key : positions.keySet()) {
         RaycastContext context = new RaycastContext(key, positions.get(key), ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         BlockHitResult result = mc.world.raycast(context);
         if (result != null && result.getType() == Type.BLOCK) {
            return false;
         }
      }

      BlockState state = mc.world.getBlockState(new BlockPosX(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ()));
      return state.isAir();
   }

   private static enum Mode {
      Fast,
      None;
   }
}
