package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.asm.accessors.IFireworkRocketEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFlyPlusPlusPlus extends Module {
   public static ElytraFlyPlusPlusPlus INSTANCE;
   public final EnumSetting<ElytraFlyPlusPlusPlus.Mode> mode = this.add(new EnumSetting<>("Mode", ElytraFlyPlusPlusPlus.Mode.Control));
   public final BooleanSetting infiniteDura = this.add(new BooleanSetting("InfiniteDura", false));
   public final BooleanSetting packet = this.add(new BooleanSetting("Packet", false).setParent());
   private final SliderSetting packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, this.packet::isOpen));
   private final BooleanSetting setFlag = this.add(new BooleanSetting("SetFlag", false, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final BooleanSetting firework = this.add(new BooleanSetting("Firework", false).setParent());
   public final BindSetting fireWork = this.add(new BindSetting("FireWorkBind", -1, this.firework::isOpen));
   public final BooleanSetting packetInteract = this.add(new BooleanSetting("PacketInteract", true, this.firework::isOpen));
   public final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, this.firework::isOpen));
   public final BooleanSetting onlyOne = this.add(new BooleanSetting("OnlyOne", true, this.firework::isOpen));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, this.firework::isOpen));
   public final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   public final SliderSetting upPitch = this.add(new SliderSetting("UpPitch", 0.0, 0.0, 90.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control));
   public final SliderSetting upFactor = this.add(
      new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control)
   );
   public final SliderSetting downFactor = this.add(
      new SliderSetting("FallSpeed", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control)
   );
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 2.1, 0.1F, 10.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control));
   public final BooleanSetting speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control));
   public final SliderSetting maxSpeed = this.add(
      new SliderSetting("MaxSpeed", 3.0, 0.1F, 10.0, () -> this.speedLimit.getValue() && this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control)
   );
   public final BooleanSetting noDrag = this.add(new BooleanSetting("NoDrag", true, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control));
   public final BooleanSetting bypass = this.add(
      new BooleanSetting("SpeedBypass", true, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control).setParent()
   );
   public final SliderSetting bypassStart = this.add(new SliderSetting("BypassStart", 1.47, 0.01, 10.0, 0.01, this.bypass::isOpen).setSuffix("b/t"));
   public final SliderSetting bypassMax = this.add(new SliderSetting("BypassMax", 3.06, 0.01, 50.0, 0.01, this.bypass::isOpen).setSuffix("b/t"));
   public final SliderSetting bypassUp = this.add(new SliderSetting("BypassUp", 2.318, 0.001, 10.0, 0.001, this.bypass::isOpen).setSuffix("/s"));
   public final SliderSetting bypassBudget = this.add(new SliderSetting("BypassBudget", 1000.0, 10.0, 10000.0, 1.0, this.bypass::isOpen).setSuffix("vl"));
   public final BooleanSetting bypassDebug = this.add(new BooleanSetting("BypassDebug", false, this.bypass::isOpen));
   public final BooleanSetting bypassAdapt = this.add(new BooleanSetting("BypassAdapt", false, this.bypass::isOpen));
   private final BooleanSetting turtleRamp = this.add(new BooleanSetting("TurtleRamp", true).setParent());
   private final SliderSetting rampStep = this.add(new SliderSetting("RampStep", 0.05, 0.01, 1.0, 0.01, this.turtleRamp::isOpen));
   private final SliderSetting rampMax = this.add(new SliderSetting("RampMax", 3.0, 1.0, 20.0, 0.1, this.turtleRamp::isOpen));
   private final BooleanSetting rampReset = this.add(new BooleanSetting("RampReset", true, this.turtleRamp::isOpen));
   private final SliderSetting rampPerLevel = this.add(new SliderSetting("RampPerLevel", 0.15, 0.0, 1.0, 0.05, this.turtleRamp::isOpen));
   private final BooleanSetting rampTurtleOnly = this.add(new BooleanSetting("RampTurtleOnly", true, this.turtleRamp::isOpen));
   private double rampFactor = 1.0;
   private int lastSlowAmplifier = -1;
   private boolean rampActive;
   private double bypassSpeed;
   private double bypassVL;
   private double bypassLastH;
   private double bypassCap = -1.0;
   private double bypassCapMaxSeen = -1.0;
   private long bypassCapMs;
   private boolean bypassPrimed;
   private double bypassDbgX;
   private double bypassDbgZ;
   private int bypassDbgTick;
   public final Timer fireworkTimer = new Timer();
   private final BooleanSetting autoStop = this.add(new BooleanSetting("AutoStop", false));
   private final BooleanSetting sprint = this.add(new BooleanSetting("Sprint", true, () -> this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final SliderSetting pitch = this.add(new SliderSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final BooleanSetting instantFly = this.add(new BooleanSetting("AutoStart", false, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final BooleanSetting checkSpeed = this.add(new BooleanSetting("CheckSpeed", false, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   public final SliderSetting minSpeed = this.add(new SliderSetting("MinSpeed", 63.5, 0.1, 200.0, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 1000.0, 0.0, 20000.0, 50.0, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final SliderSetting timeout = this.add(new SliderSetting("Timeout", 0.2, 0.1, 1.0, 0.1, () -> !this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)));
   private final SliderSetting sneakDownSpeed = this.add(
      new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control)
   );
   private final SliderSetting boost = this.add(new SliderSetting("Boost", 0.8, 0.1, 4.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Boost));
   private final BooleanSetting freeze = this.add(new BooleanSetting("Freeze", false, () -> this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation)));
   private final BooleanSetting motionStop = this.add(new BooleanSetting("MotionStop", false, () -> this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation)));
   private final SliderSetting infiniteMaxSpeed = this.add(
      new SliderSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Pitch)
   );
   private final SliderSetting infiniteMinSpeed = this.add(
      new SliderSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Pitch)
   );
   private final SliderSetting infiniteMaxHeight = this.add(
      new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Pitch)
   );
   public final BooleanSetting releaseSneak = this.add(new BooleanSetting("ReleaseSneak", false));
   private final Timer instantFlyTimer = new Timer();
   boolean prev;
   float prePitch;
   private boolean hasElytra = false;
   float yaw = 0.0F;
   float rotationPitch = 0.0F;
   boolean flying = false;
   int packetDelayInt = 0;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;

   public ElytraFlyPlusPlusPlus() {
      super("ElytraFlyPlusPlusPlus", Module.Category.Movement);
      this.setChinese("\u9798\u7fc5\u98de\u884c+++");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ElytraFlyPlusPlusPlus.FireWorkTweak());
   }

   public void off() {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.onlyOne.getValue()) {
            for (Entity var3 : Alien.THREAD.getEntities()) {
               FireworkRocketEntity var4;
               if (var3 instanceof FireworkRocketEntity && ((IFireworkRocketEntity)(var4 = (FireworkRocketEntity)var3)).getShooter() == mc.player) {
                  return;
               }
            }
         }

         INSTANCE.fireworkTimer.reset();
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (this.packetInteract.getValue()) {
               sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            } else {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         } else {
            int var1;
            if (this.inventory.getValue() && (var1 = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            } else {
               var1 = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
               if (var1 != -1) {
                  int var6 = mc.player.getInventory().selectedSlot;
                  InventoryUtil.switchToSlot(var1);
                  if (this.packetInteract.getValue()) {
                     sendSequencedPacket(
                        var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                     );
                  } else {
                     mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  }

                  InventoryUtil.switchToSlot(var6);
               }
            }
         }
      }
   }

   public static boolean recastElytra(ClientPlayerEntity var0) {
      if (checkConditions(var0) && ignoreGround(var0)) {
         var0.networkHandler.sendPacket(new ClientCommandC2SPacket(var0, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         if (INSTANCE.setFlag.getValue()) {
            mc.player.startFallFlying();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity var0) {
      ItemStack var1 = var0.getEquippedStack(EquipmentSlot.CHEST);
      return !var0.getAbilities().flying && !var0.hasVehicle() && !var0.isClimbing() && var1.isOf(Items.ELYTRA) && ElytraItem.isUsable(var1);
   }

   private static boolean ignoreGround(ClientPlayerEntity var0) {
      if (!var0.isTouchingWater() && !var0.hasStatusEffect(StatusEffects.LEVITATION)) {
         ItemStack var1 = var0.getEquippedStack(EquipmentSlot.CHEST);
         if (var1.isOf(Items.ELYTRA) && ElytraItem.isUsable(var1)) {
            var0.startFallFlying();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.hasElytra = false;
         this.yaw = mc.player.getYaw();
         this.rotationPitch = mc.player.getPitch();
         this.bypassCap = -1.0;
         this.bypassPrimed = false;
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         if (this.releaseSneak.getValue()) {
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }
      }
   }

   private void boost() {
      if (this.hasElytra) {
         if (!this.isFallFlying()) {
            return;
         }

         float var1 = (float)Math.toRadians(mc.player.getYaw());
         if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity(-MathHelper.sin(var1) * this.boost.getValueFloat() / 10.0F, 0.0, MathHelper.cos(var1) * this.boost.getValueFloat() / 10.0F);
         }
      }
   }

   @EventListener(
      priority = -9999
   )
   public void onRotation(UpdateRotateEvent var1) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation)) {
            if (this.isFallFlying()) {
               if (MovementUtil.isMoving()) {
                  if (mc.options.jumpKey.isPressed()) {
                     this.rotationPitch = -45.0F;
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.rotationPitch = 45.0F;
                  } else {
                     this.rotationPitch = -1.9F;
                     if (this.motionStop.getValue()) {
                        this.setY(0.0);
                     }
                  }
               } else if (mc.options.jumpKey.isPressed()) {
                  this.rotationPitch = -89.0F;
               } else if (mc.options.sneakKey.isPressed()) {
                  this.rotationPitch = 89.0F;
               } else if (this.motionStop.getValue()) {
                  this.setY(0.0);
               }

               if (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) {
                  this.yaw = Sprint.getSprintYaw(mc.player.getYaw());
               } else if (this.motionStop.getValue()) {
                  this.setX(0.0);
                  this.setZ(0.0);
               }

               var1.setYaw(this.yaw);
               var1.setPitch(this.rotationPitch);
            }
         } else if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Pitch)) {
            if (this.isFallFlying()) {
               var1.setPitch(this.infinitePitch);
            }
         } else if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce) && this.isFallFlying()) {
            var1.setPitch(this.pitch.getValueFloat());
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.getInfinitePitch();
      this.flying = false;
      if (this.packet.getValue()) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      } else {
         this.hasElytra = false;

         for (ItemStack var3 : mc.player.getArmorItems()) {
            if (var3.getItem() instanceof ElytraItem) {
               this.hasElytra = true;
               break;
            }
         }

         if (this.infiniteDura.getValue() && !mc.player.isOnGround() && this.hasElytra) {
            this.flying = true;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (this.setFlag.getValue()) {
               mc.player.startFallFlying();
            }
         }

         if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(0);
            return;
         }
      }

      double var16 = mc.player.getX() - mc.player.prevX;
      double var4 = mc.player.getY() - mc.player.prevY;
      double var6 = mc.player.getZ() - mc.player.prevZ;
      double var8 = Math.sqrt(var16 * var16 + var6 * var6 + var4 * var4) / 1000.0;
      double var10 = 1.388888888888889E-5;
      float var12 = Alien.TIMER.get();
      double var13 = var8 / var10 * var12;
      if (this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Boost) {
         this.boost();
      }

      if (this.packet.getValue()) {
         if (!mc.player.isOnGround()) {
            this.packetDelayInt++;
            if (!(this.packetDelayInt <= this.packetDelay.getValue())) {
               int var15 = InventoryUtil.findItem(Items.ELYTRA);
               if (var15 != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, var15, SlotActionType.SWAP, mc.player);
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                  mc.player.startFallFlying();
                  if ((!this.checkSpeed.getValue() || var13 <= this.minSpeed.getValue())
                     && this.firework.getValue()
                     && this.fireworkTimer.passed(this.delay.getValueInt())
                     && (MovementUtil.isMoving() || this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation) && mc.options.jumpKey.isPressed())
                     && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                     && this.isFallFlying()) {
                     this.off();
                     this.fireworkTimer.reset();
                  }

                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, var15, SlotActionType.SWAP, mc.player);
                  this.packetDelayInt = 0;
               } else {
                  var15 = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                  if (var15 != -1) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var15, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     if ((!this.checkSpeed.getValue() || var13 <= this.minSpeed.getValue())
                        && this.firework.getValue()
                        && this.fireworkTimer.passed(this.delay.getValueInt())
                        && (MovementUtil.isMoving() || this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation) && mc.options.jumpKey.isPressed())
                        && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                        && this.isFallFlying()) {
                        this.off();
                        this.fireworkTimer.reset();
                     }

                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var15, 0, SlotActionType.PICKUP, mc.player);
                     this.packetDelayInt = 0;
                  }
               }
            }
         }
      } else {
         if ((!this.checkSpeed.getValue() || var13 <= this.minSpeed.getValue())
            && this.firework.getValue()
            && this.fireworkTimer.passed(this.delay.getValueInt())
            && (MovementUtil.isMoving() || this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation) && mc.options.jumpKey.isPressed())
            && (!mc.player.isUsingItem() || !this.usingPause.getValue())
            && this.isFallFlying()) {
            this.off();
            this.fireworkTimer.reset();
         }

         if (!this.isFallFlying() && this.hasElytra) {
            this.fireworkTimer.setMs(99999999L);
            if (!mc.player.isOnGround() && this.instantFly.getValue() && mc.player.getVelocity().getY() < 0.0 && !this.infiniteDura.getValue()) {
               if (!this.instantFlyTimer.passed((long)(1000.0 * this.timeout.getValue()))) {
                  return;
               }

               this.instantFlyTimer.reset();
               mc.getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               if (this.setFlag.getValue()) {
                  mc.player.startFallFlying();
               }
            }
         }
      }
   }

   protected final Vec3d getRotationVector(float var1, float var2) {
      float var3 = var1 * (float) (Math.PI / 180.0);
      float var4 = -var2 * (float) (Math.PI / 180.0);
      float var5 = MathHelper.cos(var4);
      float var6 = MathHelper.sin(var4);
      float var7 = MathHelper.cos(var3);
      float var8 = MathHelper.sin(var3);
      return new Vec3d(var6 * var7, -var8, var5 * var7);
   }

   public Vec3d getRotationVec(float var1) {
      return this.getRotationVector(-this.upPitch.getValueFloat(), mc.player.getYaw(var1));
   }

   @EventListener
   private void onPlayerMove(MoveEvent var1) {
      if (this.autoStop.getValue() && this.isFallFlying()) {
         int var2 = (int)(mc.player.getX() / 16.0);
         int var3 = (int)(mc.player.getZ() / 16.0);
         if (!mc.world.getChunkManager().isChunkLoaded(var2, var3)) {
            var1.cancel();
         }
      }
   }

   @EventListener
   private void onTick(ClientTickEvent var1) {
      if (!nullCheck()) {
         if (!var1.isPost()) {
            this.updateRampFactor();
         }

         if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce) && this.hasElytra) {
            if (this.autoJump.getValue()) {
               mc.options.jumpKey.setPressed(true);
            }

            if (var1.isPost()) {
               if (!this.isFallFlying()) {
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               }

               if (checkConditions(mc.player) && !this.sprint.getValue()) {
                  if (this.isFallFlying()) {
                     mc.player.setSprinting(mc.player.isOnGround());
                  } else {
                     mc.player.setSprinting(true);
                  }
               }
            } else if (checkConditions(mc.player) && this.sprint.getValue()) {
               mc.player.setSprinting(true);
            }
         }
      }
   }

   @EventListener
   private void onPacketSend(PacketEvent.Send var1) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce)
            && this.hasElytra
            && var1.getPacket() instanceof ClientCommandC2SPacket
            && ((ClientCommandC2SPacket)var1.getPacket()).getMode().equals(net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            && !this.sprint.getValue()) {
            mc.player.setSprinting(true);
         }
      }
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive var1) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce) && this.hasElytra && var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
            mc.player.stopFallFlying();
         }
      }
   }

   @EventListener
   public void travel(TravelEvent var1) {
      if (!nullCheck()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Bounce) && this.hasElytra) {
               if (var1.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.pitch.getValueFloat());
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            } else if (this.mode.is(ElytraFlyPlusPlusPlus.Mode.Pitch) && this.isFallFlying()) {
               if (var1.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.lastInfinitePitch);
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            }
         }
      }
   }

   @EventListener
   public void onMove(TravelEvent var1) {
      if (!nullCheck() && this.hasElytra && this.isFallFlying() && !var1.isPost()) {
         if ((this.mode.is(ElytraFlyPlusPlusPlus.Mode.Freeze) || this.mode.is(ElytraFlyPlusPlusPlus.Mode.Rotation) && this.freeze.getValue())
            && !MovementUtil.isMoving()
            && !mc.options.jumpKey.isPressed()
            && !mc.options.sneakKey.isPressed()) {
            var1.cancel();
         } else {
            if (this.mode.getValue() == ElytraFlyPlusPlusPlus.Mode.Control) {
               if (this.firework.getValue()) {
                  if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
                     this.setY(0.0);
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (mc.player.input.jumping) {
                     this.setY(this.upFactor.getValue());
                  } else {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  double[] var2 = MovementUtil.directionSpeed(this.speed.getValue() * this.rampFactor);
                  this.setX(var2[0]);
                  this.setZ(var2[1]);
               } else {
                  Vec3d var4 = this.getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
                  double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
                  double var7 = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (mc.player.input.sneaking) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (!mc.player.input.jumping) {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  if (mc.player.input.jumping) {
                     if (var7 > this.upFactor.getValue() / this.upFactor.getMax()) {
                        double var10 = var7 * 0.01325;
                        this.setY(this.getY() + var10 * 3.2);
                        this.setX(this.getX() - var4.x * var10 / var5);
                        this.setZ(this.getZ() - var4.z * var10 / var5);
                     } else {
                        double[] var9 = MovementUtil.directionSpeed(this.speed.getValue() * this.rampFactor);
                        this.setX(var9[0]);
                        this.setZ(var9[1]);
                     }
                  }

                  if (var5 > 0.0) {
                     this.setX(this.getX() + (var4.x / var5 * var7 - this.getX()) * 0.1);
                     this.setZ(this.getZ() + (var4.z / var5 * var7 - this.getZ()) * 0.1);
                  }

                  if (!mc.player.input.jumping) {
                     double[] var12 = MovementUtil.directionSpeed(this.speed.getValue() * this.rampFactor);
                     this.setX(var12[0]);
                     this.setZ(var12[1]);
                  }

                  if (!this.noDrag.getValue()) {
                     this.setY(this.getY() * 0.99F);
                     this.setX(this.getX() * 0.98F);
                     this.setZ(this.getZ() * 0.99F);
                  }

                  double var11 = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (this.speedLimit.getValue() && var11 > this.maxSpeed.getValue()) {
                     this.setX(this.getX() * this.maxSpeed.getValue() / var11);
                     this.setZ(this.getZ() * this.maxSpeed.getValue() / var11);
                  }

                  var1.cancel();
                  mc.player.move(MovementType.SELF, mc.player.getVelocity());
               }
            }
         }
      }
   }

   private double getX() {
      return MovementUtil.getMotionX();
   }

   private void setX(double var1) {
      MovementUtil.setMotionX(var1);
   }

   private double getY() {
      return MovementUtil.getMotionY();
   }

   private void setY(double var1) {
      MovementUtil.setMotionY(var1);
   }

   private double getZ() {
      return MovementUtil.getMotionZ();
   }

   private void setZ(double var1) {
      MovementUtil.setMotionZ(var1);
   }

   private void getInfinitePitch() {
      this.lastInfinitePitch = this.infinitePitch;
      double var1 = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      if (mc.player.getY() < this.infiniteMaxHeight.getValue()) {
         if (var1 * 72.0 < this.infiniteMinSpeed.getValue() && !this.down) {
            this.down = true;
         }

         if (var1 * 72.0 > this.infiniteMaxSpeed.getValue() && this.down) {
            this.down = false;
         }
      } else {
         this.down = true;
      }

      this.infinitePitch = this.down ? (this.infinitePitch += 3.0F) : (this.infinitePitch -= 3.0F);
      this.infinitePitch = MathUtil.clamp(this.infinitePitch, -40.0F, 40.0F);
   }

   public boolean isFallFlying() {
      return mc.player.isFallFlying() || this.packet.getValue() && this.hasElytra && !mc.player.isOnGround() || this.flying;
   }

   private double bypassFloor() {
      return this.bypassStart.getValue();
   }

   private double updateRampFactor() {
      if (this.turtleRamp.getValue() && !Module.nullCheck()) {
         StatusEffectInstance var2 = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
         boolean var1 = var2 != null;
         if (var1 && this.rampTurtleOnly.getValue()) {
            var1 = mc.player.hasStatusEffect(StatusEffects.RESISTANCE);
         }

         this.rampActive = var1;
         if (var1) {
            int var4 = var2.getAmplifier();
            if (var4 != this.lastSlowAmplifier) {
               this.lastSlowAmplifier = var4;
               this.rampFactor = 1.0;
            }

            this.rampFactor = this.rampFactor + (this.rampStep.getValue() + this.rampPerLevel.getValue() * (var4 + 1));
            if (this.rampFactor > this.rampMax.getValue()) {
               this.rampFactor = this.rampMax.getValue();
            }

            return this.rampFactor;
         } else {
            this.lastSlowAmplifier = -1;
            if (this.rampReset.getValue()) {
               this.rampFactor = 1.0;
            }

            return this.rampFactor;
         }
      } else {
         this.rampFactor = 1.0;
         this.rampActive = false;
         return 1.0;
      }
   }

   @EventListener
   public void onBypassUpdate(UpdateEvent var1) {
      if (this.bypass.getValue() && !Module.nullCheck()) {
         if (this.bypassDebug.getValue() && ++this.bypassDbgTick % 20 == 0 && mc.player.isFallFlying()) {
            double var6 = mc.player.getX() - this.bypassDbgX;
            double var4 = mc.player.getZ() - this.bypassDbgZ;
            double var2 = Math.sqrt(var6 * var6 + var4 * var4);
            CommandManager.sendMessage(
               String.format(
                  "\u00a77[Bypass] \u00a7fspd=\u00a7a%.2f\u00a7f tgt=\u00a7b%.2f\u00a7f cap=\u00a73%.2f\u00a7f vl=\u00a7e%.0f\u00a7f pitch=\u00a7d%.1f",
                  var2,
                  this.bypassSpeed,
                  this.bypassCap,
                  this.bypassVL,
                  mc.player.getPitch()
               )
            );
         }

         this.bypassDbgX = mc.player.getX();
         this.bypassDbgZ = mc.player.getZ();
         if (!mc.player.isFallFlying()) {
            this.bypassSpeed = this.bypassFloor();
            this.bypassPrimed = false;
         } else {
            double var10 = this.bypassFloor();
            if (this.bypassSpeed < var10) {
               this.bypassSpeed = var10;
            }

            double var9;
            if ((var9 = this.bypassMax.getValue()) != this.bypassCapMaxSeen) {
               this.bypassCapMaxSeen = var9;
               this.bypassCap = -1.0;
            } else if (this.bypassCap > 0.0 && System.currentTimeMillis() - this.bypassCapMs > 10000L) {
               this.bypassCap = Math.min(var9, this.bypassCap + 0.01);
               if (this.bypassCap >= var9) {
                  this.bypassCap = -1.0;
               }
            }

            double var8 = Math.max(var9, var10);
            if (this.bypassCap > 0.0) {
               var8 = Math.min(var8, Math.max(var10, this.bypassCap));
            }

            if (!this.bypassPrimed) {
               this.bypassSpeed = var8;
               this.bypassVL = 0.0;
               this.bypassPrimed = true;
            } else {
               this.bypassSpeed = Math.min(this.bypassSpeed + this.bypassUp.getValue() / 20.0, var8);
            }
         }
      }
   }

   @EventListener
   public void onBypassPacket(PacketEvent.Receive var1) {
      if (var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
         double var2 = this.bypassFloor();
         if (!this.bypassAdapt.getValue()) {
            this.bypassCap = -1.0;
         } else if (this.bypassSpeed > var2) {
            double var4 = Math.max(var2, this.bypassSpeed * 0.98);
            double var6 = Math.max(var2, Math.max(this.bypassStart.getValue(), this.bypassMax.getValue() * 0.3));
            this.bypassCap = this.bypassCap > 0.0 ? Math.min(this.bypassCap, var4) : var4;
            this.bypassCap = Math.max(this.bypassCap, var6);
            this.bypassCapMs = System.currentTimeMillis();
         }

         this.bypassSpeed = var2;
         this.bypassVL = 0.0;
      }
   }

   @EventListener
   public void onBypassMove(MoveEvent var1) {
      if (this.bypass.getValue() && !Module.nullCheck() && !this.rampActive && mc.player.isFallFlying()) {
         double var4 = var1.getX();
         double var2;
         double var6 = Math.sqrt(var4 * var4 + (var2 = var1.getZ()) * var2);
         if (var6 < 1.0E-4) {
            this.bypassLastH = 0.0;
         } else {
            double var8 = this.bypassLastH * 0.99;
            float var10 = mc.player.getPitch();
            if (var10 > 0.0F) {
               var8 += this.bypassLastH * 0.04 * Math.sin(Math.toRadians(var10));
            }

            double var11 = Math.min(this.bypassSpeed, this.bypassLastH + this.bypassUp.getValue() / 20.0);
            double var13 = var6;
            boolean var15 = false;
            if (var11 > var6 && var10 >= 0.0F && this.bypassVL < this.bypassBudget.getValue()) {
               double var16 = var8 + (this.bypassBudget.getValue() - this.bypassVL) / 100.0;
               var13 = Math.min(var11, var16);
               if (var13 > var6) {
                  if (var13 > var8) {
                     this.bypassVL += (var13 - var8) * 100.0;
                  }

                  var15 = true;
                  double var18 = var13 / var6;
                  var1.setX(var4 * var18);
                  var1.setZ(var2 * var18);
               } else {
                  var13 = var6;
               }
            }

            if (!var15) {
               this.bypassVL = Math.max(0.0, this.bypassVL * 0.95);
            }

            this.bypassLastH = var13;
         }
      }
   }

   public class FireWorkTweak {
      boolean press;

      @EventListener
      public void onTick(ClientTickEvent var1) {
         if (!Module.nullCheck() && !var1.isPost()) {
            if (!ElytraFlyPlusPlusPlus.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.currentScreen == null) {
                  if (ElytraFlyPlusPlusPlus.this.fireWork.isPressed()) {
                     if (!this.press
                        && ElytraFlyPlusPlusPlus.this.fireworkTimer.passed(ElytraFlyPlusPlusPlus.this.delay.getValueInt())
                        && (!Wrapper.mc.player.isUsingItem() || !ElytraFlyPlusPlusPlus.this.usingPause.getValue())
                        && ElytraFlyPlusPlusPlus.this.isFallFlying()) {
                        ElytraFlyPlusPlusPlus.this.off();
                        ElytraFlyPlusPlusPlus.this.fireworkTimer.reset();
                     }

                     this.press = true;
                  } else {
                     this.press = false;
                  }
               } else {
                  this.press = false;
               }
            }
         }
      }
   }

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch;
   }
}
