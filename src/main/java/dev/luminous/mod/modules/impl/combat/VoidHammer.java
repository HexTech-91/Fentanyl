package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

public class VoidHammer extends Module {
   public static VoidHammer INSTANCE;
   public final EnumSetting<VoidHammer.Mode> mode = this.add(new EnumSetting<>("Mode", VoidHammer.Mode.VoidTP));
   public final BooleanSetting requireMace;
   public final BooleanSetting swing;
   public final BooleanSetting rotate;
   public final BooleanSetting lockView;
   public final SliderSetting voidY;
   public final SliderSetting timerSpeed;
   public final SliderSetting fallTime;
   public final SliderSetting attackPackets;
   public final SliderSetting cycleDelay;
   public final SliderSetting attackRange;
   public final SliderSetting targetRange;
   public final BooleanSetting autoRestart;
   public final SliderSetting vcHeight;
   public final SliderSetting vcBase;
   public final SliderSetting vcSteps;
   public final SliderSetting vcAttackRange;
   public final SliderSetting vcMinFall;
   public final SliderSetting vcAttackDelay;
   public final SliderSetting vcLoopDelay;
   public final BooleanSetting vcAutoSwitch;
   public final BooleanSetting vcPlayerOnly;
   public final SliderSetting tfVoidY;
   public final SliderSetting tfTimerSpeed;
   public final SliderSetting tfFallTime;
   public final SliderSetting tfAttackPackets;
   public final SliderSetting tfCycleDelay;
   public final SliderSetting tfFallHeight;
   public final SliderSetting tfAttackRange;
   public final SliderSetting tfWallRange;
   private VoidHammer.Phase phase;
   private PlayerEntity target;
   private double originX;
   private double originY;
   private double originZ;
   private double groundY;
   private long phaseStartMs;
   private int packetsSent;
   private float savedYaw;
   private float savedPitch;
   private VoidHammer.VcPhase vcPhase;
   private Entity vcLockedTarget;
   private long vcLastAttack;
   private long vcLandedAt;
   private boolean vcSendingSelf;
   private VoidHammer.TfPhase tfPhase;
   private double tfOrigX;
   private double tfOrigY;
   private double tfOrigZ;
   private float tfOrigFallDist;
   private boolean tfTimerActive;
   private final Timer tfFallTimer;
   private final Timer tfCooldownTimer;

   public VoidHammer() {
      super("VoidHammer", Module.Category.Combat);
      this.mode.setChinese("\u6a21\u5f0f");
      this.requireMace = this.add(new BooleanSetting("RequireMace", true));
      this.requireMace.setChinese("\u9700\u8981\u91cd\u9524");
      this.swing = this.add(new BooleanSetting("Swing", true));
      this.swing.setChinese("\u6325\u52a8\u624b\u81c2");
      this.rotate = this.add(new BooleanSetting("Rotate", false));
      this.rotate.setChinese("\u65cb\u8f6c\u89c6\u89d2");
      this.lockView = this.add(new BooleanSetting("LockView", true));
      this.lockView.setChinese("\u56fa\u5b9a\u89c6\u89d2");
      this.voidY = this.add(new SliderSetting("VoidY", -70.0, -128.0, -1.0, 1.0, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.voidY.setChinese("\u865a\u7a7aY\u5750\u6807");
      this.timerSpeed = this.add(new SliderSetting("TimerSpeed", 5.2, 1.0, 20.0, 0.1, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.timerSpeed.setChinese("Timer\u901f\u5ea6");
      this.fallTime = this.add(new SliderSetting("FallTime", 150.0, 50.0, 500.0, 1.0, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.fallTime.setChinese("\u5760\u843d\u65f6\u95f4");
      this.attackPackets = this.add(new SliderSetting("AttackPackets", 3.0, 1.0, 10.0, 1.0, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.attackPackets.setChinese("\u653b\u51fb\u5305\u6570\u91cf");
      this.cycleDelay = this.add(new SliderSetting("CycleDelay", 0.0, 0.0, 20.0, 1.0, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.cycleDelay.setChinese("\u5faa\u73af\u5ef6\u8fdf");
      this.attackRange = this.add(new SliderSetting("AttackRange", 6.0, 3.0, 10.0, 0.1, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.attackRange.setChinese("\u653b\u51fb\u8ddd\u79bb");
      this.targetRange = this.add(new SliderSetting("TargetRange", 10.0, 5.0, 30.0, 1.0, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.autoRestart = this.add(new BooleanSetting("AutoRestart", true, () -> this.mode.is(VoidHammer.Mode.VoidTP)));
      this.autoRestart.setChinese("\u547d\u4e2d\u91cd\u542f");
      this.vcHeight = this.add(new SliderSetting("Height", 8.0, 3.0, 15.0, 0.5, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcHeight.setChinese("\u9ad8\u5ea6");
      this.vcBase = this.add(new SliderSetting("Base", 2.36, 1.0, 5.0, 0.01, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcBase.setChinese("\u5e95\u6570");
      this.vcSteps = this.add(new SliderSetting("MaceSteps", 0.5, 0.1, 2.0, 0.01, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcSteps.setChinese("\u6b65\u6570");
      this.vcAttackRange = this.add(new SliderSetting("AttackRange", 6.0, 1.0, 7.0, 0.1, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcAttackRange.setChinese("\u653b\u51fb\u8ddd\u79bb");
      this.vcMinFall = this.add(new SliderSetting("MinFall", 1.5, 0.0, 6.0, 0.1, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcMinFall.setChinese("\u6700\u5c0f\u4e0b\u843d");
      this.vcAttackDelay = this.add(new SliderSetting("AttackDelay", 0.0, 0.0, 1000.0, 10.0, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcAttackDelay.setChinese("\u653b\u51fb\u5ef6\u8fdf");
      this.vcLoopDelay = this.add(new SliderSetting("LoopDelay", 0.0, 0.0, 1000.0, 10.0, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcLoopDelay.setChinese("\u5faa\u73af\u5ef6\u8fdf");
      this.vcAutoSwitch = this.add(new BooleanSetting("AutoSwitch", false, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcAutoSwitch.setChinese("\u81ea\u52a8\u5207\u6362");
      this.vcPlayerOnly = this.add(new BooleanSetting("PlayerOnly", true, () -> this.mode.is(VoidHammer.Mode.VClip)));
      this.vcPlayerOnly.setChinese("\u4ec5\u73a9\u5bb6");
      this.tfVoidY = this.add(new SliderSetting("VoidY", -70.0, -128.0, 0.0, 1.0, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfVoidY.setChinese("\u865a\u7a7aY\u5750\u6807");
      this.tfTimerSpeed = this.add(new SliderSetting("TimerSpeed", 5.2, 1.0, 10.0, 0.1, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfTimerSpeed.setChinese("Timer\u901f\u5ea6");
      this.tfFallTime = this.add(new SliderSetting("FallTime", 150.0, 50.0, 500.0, 10.0, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfFallTime.setChinese("\u5760\u843d\u65f6\u95f4");
      this.tfAttackPackets = this.add(new SliderSetting("AttackPackets", 3.0, 1.0, 10.0, 1.0, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfAttackPackets.setChinese("\u653b\u51fb\u5305\u6570\u91cf");
      this.tfCycleDelay = this.add(new SliderSetting("CycleDelay", 0.0, 0.0, 500.0, 10.0, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfCycleDelay.setChinese("\u5faa\u73af\u5ef6\u8fdf");
      this.tfFallHeight = this.add(new SliderSetting("FallHeight", 120.0, 10.0, 300.0, 5.0, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfFallHeight.setChinese("\u5760\u843d\u9ad8\u5ea6");
      this.tfAttackRange = this.add(new SliderSetting("AttackRange", 6.0, 1.0, 10.0, 0.1, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfAttackRange.setChinese("\u653b\u51fb\u8ddd\u79bb");
      this.tfWallRange = this.add(new SliderSetting("WallRange", 5.5, 0.0, 8.0, 0.1, () -> this.mode.is(VoidHammer.Mode.TimerFall)));
      this.tfWallRange.setChinese("\u7a7f\u5899\u8ddd\u79bb");
      this.phase = VoidHammer.Phase.INIT;
      this.vcPhase = VoidHammer.VcPhase.WAITING;
      this.tfPhase = VoidHammer.TfPhase.IDLE;
      this.tfFallTimer = new Timer();
      this.tfCooldownTimer = new Timer();
      this.setChinese("\u865a\u7a7a\u9524");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.phase = VoidHammer.Phase.INIT;
      this.packetsSent = 0;
      this.phaseStartMs = System.currentTimeMillis();
      this.target = null;
      this.vcPhase = VoidHammer.VcPhase.WAITING;
      this.vcLockedTarget = null;
      this.vcLastAttack = 0L;
      this.vcLandedAt = 0L;
      this.vcSendingSelf = false;
      this.tfPhase = VoidHammer.TfPhase.IDLE;
      this.tfTimerActive = false;
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         if (this.phase == VoidHammer.Phase.FALL || this.phase == VoidHammer.Phase.STRIKE_TP) {
            this.emergencyReturnVoid();
         }

         if (this.tfPhase == VoidHammer.TfPhase.TELEPORT_VOID
            || this.tfPhase == VoidHammer.TfPhase.WAITING_FALL
            || this.tfPhase == VoidHammer.TfPhase.TELEPORT_BACK) {
            this.emergencyReturnTimerVoid();
         }
      }

      this.target = null;
      this.vcLockedTarget = null;
      if (this.tfTimerActive) {
         Alien.TIMER.reset();
         this.tfTimerActive = false;
      }

      Alien.TIMER.reset();
      RotationManager.fixYaw = 0.0F;
      RotationManager.fixPitch = 0.0F;
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck()) {
         if (!var1.isPost()) {
            switch ((VoidHammer.Mode)this.mode.getValue()) {
               case VoidTP:
                  this.onTickVoidTP();
                  break;
               case VClip:
                  this.onTickVClip();
                  break;
               case TimerFall:
                  this.onTickTimerFall();
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive var1) {
      if (var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
         mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround()));
      }

      EntityDamageS2CPacket var2;
      Packet var4;
      if (this.mode.is(VoidHammer.Mode.VoidTP)
         && this.autoRestart.getValue()
         && (var4 = var1.getPacket()) instanceof EntityDamageS2CPacket
         && mc.world.getEntityById((var2 = (EntityDamageS2CPacket)var4).entityId()) == this.target
         && (this.phase == VoidHammer.Phase.ATTACK || this.phase == VoidHammer.Phase.COOLDOWN)) {
         if (this.rotate.getValue()) {
            Alien.ROTATION.snapBack();
         }

         if (this.lockView.getValue()) {
            RotationManager.fixYaw = 0.0F;
            RotationManager.fixPitch = 0.0F;
         }

         this.phase = VoidHammer.Phase.INIT;
         this.phaseStartMs = System.currentTimeMillis();
      }
   }

   @EventListener
   public void onTimer(TimerEvent var1) {
      if (this.mode.is(VoidHammer.Mode.TimerFall) && this.tfTimerActive) {
         var1.set(this.tfTimerSpeed.getValueFloat());
      }
   }

   private void onTickVoidTP() {
      if (!this.checkVoidPrerequisites()) {
         if (this.phase != VoidHammer.Phase.FALL && this.phase != VoidHammer.Phase.STRIKE_TP) {
            this.phase = VoidHammer.Phase.INIT;
         } else {
            this.emergencyReturnVoid();
         }
      } else {
         long var1 = (System.currentTimeMillis() - this.phaseStartMs) / 50L;
         switch (this.phase) {
            case INIT:
               this.originX = mc.player.getX();
               this.originY = mc.player.getY();
               this.originZ = mc.player.getZ();
               this.groundY = this.findGroundY((int)this.originX, (int)this.originY, (int)this.originZ);
               this.target = this.findTargetVoid();
               if (this.target == null) {
                  this.phase = VoidHammer.Phase.COOLDOWN;
                  this.phaseStartMs = System.currentTimeMillis();
               } else {
                  this.phase = VoidHammer.Phase.VOID_TP;
                  this.phaseStartMs = System.currentTimeMillis();
               }
               break;
            case VOID_TP:
               if (this.lockView.getValue()) {
                  this.savedYaw = mc.player.getYaw();
                  this.savedPitch = mc.player.getPitch();
                  RotationManager.fixYaw = this.savedYaw;
                  RotationManager.fixPitch = this.savedPitch;
               }

               mc.player.networkHandler.sendPacket(new PositionAndOnGround(this.originX, this.voidY.getValue(), this.originZ, false));
               Alien.TIMER.set(this.timerSpeed.getValueFloat());
               this.phase = VoidHammer.Phase.FALL;
               this.phaseStartMs = System.currentTimeMillis();
               break;
            case FALL:
               Alien.TIMER.reset();
               if (this.lockView.getValue()) {
                  RotationManager.fixYaw = 0.0F;
                  RotationManager.fixPitch = 0.0F;
               }

               mc.player.networkHandler.sendPacket(new PositionAndOnGround(this.originX, this.groundY, this.originZ, true));
               if (this.rotate.getValue() && this.target != null && this.target.isAlive()) {
                  Alien.ROTATION.lookAt(MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.target.getBoundingBox()));
               }

               this.phase = VoidHammer.Phase.ATTACK;
               this.packetsSent = 0;
               break;
            case STRIKE_TP:
               if (this.lockView.getValue()) {
                  RotationManager.fixYaw = 0.0F;
                  RotationManager.fixPitch = 0.0F;
               }

               mc.player.networkHandler.sendPacket(new PositionAndOnGround(this.originX, this.groundY, this.originZ, true));
               if (this.rotate.getValue() && this.target != null && this.target.isAlive()) {
                  Alien.ROTATION.lookAt(MathUtil.getClosestPointToBox(mc.player.getEyePos(), this.target.getBoundingBox()));
               }

               this.phase = VoidHammer.Phase.ATTACK;
               this.packetsSent = 0;
               break;
            case ATTACK:
               if (this.target == null || !this.target.isAlive() || this.target.getHealth() <= 0.0F) {
                  this.finishVoid();
                  return;
               }

               if (this.packetsSent < (int)this.attackPackets.getValue()) {
                  int var3 = this.getMaceSlot();
                  if (var3 != -1 && this.requireMace.getValue()) {
                     int var4 = mc.player.getInventory().selectedSlot;
                     mc.player.getInventory().selectedSlot = var3;
                     mc.interactionManager.attackEntity(mc.player, this.target);
                     if (this.swing.getValue()) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                     }

                     mc.player.getInventory().selectedSlot = var4;
                  } else {
                     mc.interactionManager.attackEntity(mc.player, this.target);
                     if (this.swing.getValue()) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  }

                  this.packetsSent++;
               }

               if (this.packetsSent >= (int)this.attackPackets.getValue()) {
                  this.finishVoid();
               }
               break;
            case COOLDOWN:
               if (var1 >= (long)this.cycleDelay.getValue()) {
                  this.phase = VoidHammer.Phase.INIT;
                  this.phaseStartMs = System.currentTimeMillis();
               }
         }
      }
   }

   private void emergencyReturnVoid() {
      Alien.TIMER.reset();
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(this.originX, this.groundY, this.originZ, true));
      }

      if (this.lockView.getValue()) {
         RotationManager.fixYaw = 0.0F;
         RotationManager.fixPitch = 0.0F;
      }

      this.phase = VoidHammer.Phase.COOLDOWN;
      this.phaseStartMs = System.currentTimeMillis();
   }

   private void finishVoid() {
      if (this.rotate.getValue()) {
         Alien.ROTATION.snapBack();
      }

      if (this.lockView.getValue()) {
         RotationManager.fixYaw = 0.0F;
         RotationManager.fixPitch = 0.0F;
      }

      this.phase = VoidHammer.Phase.COOLDOWN;
      this.phaseStartMs = System.currentTimeMillis();
   }

   private boolean checkVoidPrerequisites() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else {
         return Aura.INSTANCE != null && Aura.INSTANCE.isOn() ? !this.requireMace.getValue() || this.getMaceSlot() != -1 : false;
      }
   }

   private PlayerEntity findTargetVoid() {
      Entity var1;
      if (Aura.INSTANCE != null && Aura.INSTANCE.isOn() && (var1 = Aura.INSTANCE.getTarget(this.targetRange.getValueFloat())) instanceof PlayerEntity) {
         PlayerEntity var9 = (PlayerEntity)var1;
         return var9;
      } else {
         PlayerEntity var2 = null;
         double var3 = Double.MAX_VALUE;

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            double var7;
            if (var6 != mc.player
               && var6.isAlive()
               && !(var6.getHealth() <= 0.0F)
               && !((var7 = mc.player.squaredDistanceTo(var6)) > MathHelper.square(this.targetRange.getValue()))
               && var7 < var3) {
               var3 = var7;
               var2 = var6;
            }
         }

         return var2;
      }
   }

   private double findGroundY(int var1, int var2, int var3) {
      Mutable var4 = new Mutable(var1, var2, var3);

      while (var4.getY() > -64) {
         if (mc.world.getBlockState(var4).isSolidBlock(mc.world, var4)) {
            return var4.getY() + 1.0;
         }

         var4.setY(var4.getY() - 1);
      }

      return this.originY + 1.0;
   }

   private void onTickVClip() {
      if (this.canVcRun()) {
         if (this.vcPhase == VoidHammer.VcPhase.WAITING) {
            if (System.currentTimeMillis() - this.vcLandedAt >= (long)this.vcLoopDelay.getValue()) {
               this.vcLockedTarget = this.getVcAuraTarget();
               if (this.vcLockedTarget != null) {
                  this.vcPhase = VoidHammer.VcPhase.FALLING;
               }
            }
         } else {
            this.tryVcAttack();
         }
      }
   }

   private boolean canVcRun() {
      if (Aura.INSTANCE != null && Aura.INSTANCE.isOn()) {
         return !this.requireMace.getValue()
               || mc.player.getMainHandStack().getItem() == Items.MACE
               || this.vcAutoSwitch.getValue() && this.findVcMaceSlot() != -1
            ? this.getVcTarget() != null
            : false;
      } else {
         return false;
      }
   }

   private void tryVcAttack() {
      Entity var1 = this.getVcTarget();
      if (var1 != null) {
         if (!(mc.player.getEyePos().distanceTo(MathUtil.getClosestPointToBox(mc.player.getEyePos(), var1.getBoundingBox())) > this.vcAttackRange.getValue())) {
            long var2 = System.currentTimeMillis();
            if (var2 - this.vcLastAttack >= (long)this.vcAttackDelay.getValue()) {
               if (this.rotate.getValue()) {
                  Alien.ROTATION.lookAt(MathUtil.getClosestPointToBox(mc.player.getEyePos(), var1.getBoundingBox()));
               }

               int var4 = mc.player.getInventory().selectedSlot;
               boolean var5 = this.switchVcToMace();
               if (!this.requireMace.getValue() || mc.player.getMainHandStack().getItem() == Items.MACE) {
                  if (this.lockView.getValue()) {
                     RotationManager.fixYaw = mc.player.getYaw();
                     RotationManager.fixPitch = mc.player.getPitch();
                  }

                  this.doVcSpoof();
                  this.vcSendingSelf = true;

                  try {
                     mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
                     mc.player.resetLastAttackedTicks();
                     if (this.swing.getValue()) {
                        EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
                     }
                  } finally {
                     this.vcSendingSelf = false;
                  }

                  if (var5) {
                     mc.player.getInventory().selectedSlot = var4;
                  }

                  if (this.rotate.getValue()) {
                     Alien.ROTATION.snapBack();
                  }

                  if (this.lockView.getValue()) {
                     RotationManager.fixYaw = 0.0F;
                     RotationManager.fixPitch = 0.0F;
                  }

                  this.vcLastAttack = var2;
                  this.vcPhase = VoidHammer.VcPhase.WAITING;
                  this.vcLandedAt = var2;
               }
            }
         }
      }
   }

   private void doVcSpoof() {
      double var1 = mc.player.getX();
      double var3 = mc.player.getY();
      double var5 = mc.player.getZ();
      double var7 = this.vcHeight.getValue();

      for (int var9 = 0; var9 < 4; var9++) {
         mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, var3, var5, false));
      }

      double var15 = Math.max(1.1, Math.min(1000.0, this.vcBase.getValue()));
      double var11 = Math.max(0.1, Math.min(5.0, this.vcSteps.getValue()));

      for (double var13 = 0.0; Math.pow(var15, var13) < var7; var13 += var11) {
         mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, var3 + Math.min(var7, Math.pow(var15, var13)), var5, false));
      }

      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, var3, var5, false));
      mc.player.fallDistance = (float)var7;
      mc.player.setVelocity(0.0, -0.08, 0.0);
   }

   private Entity getVcTarget() {
      return this.isValidVcTarget(this.vcLockedTarget) ? this.vcLockedTarget : this.getVcAuraTarget();
   }

   private Entity getVcAuraTarget() {
      Entity var1 = Aura.INSTANCE != null ? Aura.target : null;
      return this.isValidVcTarget(var1) ? var1 : null;
   }

   private boolean isValidVcTarget(Entity var1) {
      if (var1 != null && var1 != mc.player && var1.isAlive()) {
         LivingEntity var2;
         return var1 instanceof LivingEntity && (var2 = (LivingEntity)var1).isDead() ? false : !this.vcPlayerOnly.getValue() || var1 instanceof PlayerEntity;
      } else {
         return false;
      }
   }

   private boolean switchVcToMace() {
      if (this.vcAutoSwitch.getValue() && mc.player.getMainHandStack().getItem() != Items.MACE) {
         int var1 = this.findVcMaceSlot();
         if (var1 != -1 && var1 != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = var1;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int findVcMaceSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getStack(var1).getItem() == Items.MACE) {
            return var1;
         }
      }

      return -1;
   }

   private void onTickTimerFall() {
      if (this.requireMace.getValue() && mc.player.getMainHandStack().getItem() != Items.MACE) {
         if (this.tfTimerActive) {
            Alien.TIMER.reset();
            this.tfTimerActive = false;
         }

         this.tfPhase = VoidHammer.TfPhase.IDLE;
      } else {
         switch (this.tfPhase) {
            case IDLE:
               this.runTimerFallOneTick();
               break;
            case TELEPORT_VOID:
            case WAITING_FALL:
            case TELEPORT_BACK:
               this.emergencyReturnTimerVoid();
               break;
            case ATTACKING:
               Entity var1 = this.getTfTarget();
               if (var1 == null) {
                  this.tfCooldownTimer.reset();
                  this.tfPhase = VoidHammer.TfPhase.COOLDOWN;
                  return;
               }

               if (this.rotate.getValue()) {
                  Alien.ROTATION.lookAt(MathUtil.getClosestPointToBox(mc.player.getEyePos(), var1.getBoundingBox()));
               }

               int var2 = this.tfAttackPackets.getValueInt();

               for (int var3 = 0; var3 < var2; var3++) {
                  mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
               }

               if (this.swing.getValue()) {
                  EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
               }

               if (this.rotate.getValue()) {
                  Alien.ROTATION.snapBack();
               }

               this.tfCooldownTimer.reset();
               this.tfPhase = VoidHammer.TfPhase.COOLDOWN;
               break;
            case COOLDOWN:
               if (this.tfCooldownTimer.passedMs(this.tfCycleDelay.getValue())) {
                  this.tfPhase = VoidHammer.TfPhase.IDLE;
               }
         }
      }
   }

   private void runTimerFallOneTick() {
      Entity var1 = this.getTfTarget();
      if (var1 != null) {
         this.tfOrigX = mc.player.getX();
         this.tfOrigY = mc.player.getY();
         this.tfOrigZ = mc.player.getZ();
         this.tfOrigFallDist = mc.player.fallDistance;
         if (this.lockView.getValue()) {
            RotationManager.fixYaw = mc.player.getYaw();
            RotationManager.fixPitch = mc.player.getPitch();
         }

         boolean var2 = false;

         try {
            double var3 = this.tfVoidY.getValue();
            Alien.TIMER.set(this.tfTimerSpeed.getValueFloat());
            this.tfTimerActive = true;
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(this.tfOrigX, var3, this.tfOrigZ, false));
            var2 = true;
            mc.player.setPosition(this.tfOrigX, var3, this.tfOrigZ);
            mc.player.fallDistance = this.tfFallHeight.getValueFloat();
         } finally {
            if (var2) {
               mc.getNetworkHandler().sendPacket(new PositionAndOnGround(this.tfOrigX, this.tfOrigY, this.tfOrigZ, false));
               mc.player.setPosition(this.tfOrigX, this.tfOrigY, this.tfOrigZ);
            }

            mc.player.fallDistance = this.tfOrigFallDist;
            if (this.tfTimerActive) {
               Alien.TIMER.reset();
               this.tfTimerActive = false;
            }

            if (this.lockView.getValue()) {
               RotationManager.fixYaw = 0.0F;
               RotationManager.fixPitch = 0.0F;
            }
         }

         this.tfPhase = VoidHammer.TfPhase.ATTACKING;
      }
   }

   private void emergencyReturnTimerVoid() {
      if (this.tfTimerActive) {
         Alien.TIMER.reset();
         this.tfTimerActive = false;
      }

      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(this.tfOrigX, this.tfOrigY, this.tfOrigZ, false));
         mc.player.setPosition(this.tfOrigX, this.tfOrigY, this.tfOrigZ);
         mc.player.fallDistance = this.tfOrigFallDist;
      }

      RotationManager.fixYaw = 0.0F;
      RotationManager.fixPitch = 0.0F;
      this.tfCooldownTimer.reset();
      this.tfPhase = VoidHammer.TfPhase.COOLDOWN;
   }

   private Entity getTfTarget() {
      if (Aura.INSTANCE != null && Aura.INSTANCE.isOn()) {
         Entity var1 = Aura.target;
         if (!this.isValidTfTarget(var1)) {
            return null;
         } else {
            Vec3d var2 = MathUtil.getClosestPointToBox(mc.player.getEyePos(), var1.getBoundingBox());
            double var3 = mc.player.getEyePos().distanceTo(var2);
            return (mc.player.canSee(var1) ? !(var3 > this.tfAttackRange.getValue()) : !(var3 > this.tfWallRange.getValue())) ? var1 : null;
         }
      } else {
         return null;
      }
   }

   private boolean isValidTfTarget(Entity var1) {
      if (var1 != null && var1 != mc.player && var1.isAlive()) {
         PlayerEntity var2;
         LivingEntity var3;
         return var1 instanceof LivingEntity && (var3 = (LivingEntity)var1).isDead()
            ? false
            : !(var1 instanceof PlayerEntity) || !Alien.FRIEND.isFriend(var2 = (PlayerEntity)var1);
      } else {
         return false;
      }
   }

   private int getMaceSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getStack(var1).getItem() == Items.MACE) {
            return var1;
         }
      }

      return -1;
   }

   public static enum Mode {
      VoidTP,
      VClip,
      TimerFall;
   }

   private static enum Phase {
      INIT,
      VOID_TP,
      FALL,
      STRIKE_TP,
      ATTACK,
      COOLDOWN;
   }

   private static enum TfPhase {
      IDLE,
      TELEPORT_VOID,
      WAITING_FALL,
      TELEPORT_BACK,
      ATTACKING,
      COOLDOWN;
   }

   private static enum VcPhase {
      WAITING,
      FALLING;
   }
}
