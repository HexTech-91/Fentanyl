package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.ElytraUpdateEvent;
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
import dev.luminous.asm.accessors.IVec3d;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.combat.Follower;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ElytraFly extends Module {
   public static ElytraFly INSTANCE;
   public final EnumSetting<ElytraFly.Mode> mode = this.add(new EnumSetting<>("\u6a21\u5f0f", ElytraFly.Mode.Control));
   public final BooleanSetting infiniteDura = this.add(new BooleanSetting("InfiniteDura", false));
   public final BooleanSetting packet;
   private final SliderSetting packetDelay;
   private final BooleanSetting setFlag;
   private final BooleanSetting firework;
   public final BindSetting fireWork;
   public final BooleanSetting packetInteract;
   public final BooleanSetting inventory;
   public final BooleanSetting onlyOne;
   private final BooleanSetting usingPause;
   public final BooleanSetting autoJump;
   public final SliderSetting upPitch;
   public final SliderSetting upFactor;
   public final SliderSetting downFactor;
   public final SliderSetting speed;
   public final BooleanSetting speedLimit;
   public final SliderSetting maxSpeed;
   public final BooleanSetting noDrag;
   public final BooleanSetting speedRamp;
   public final SliderSetting rampPerSecond;
   public final SliderSetting rampMax;
   public final BooleanSetting autoReplaceElytra;
   public final SliderSetting replaceDurability;
   public final BooleanSetting autoAvoid;
   public final SliderSetting avoidDistance;
   public final Timer fireworkTimer;
   private final BooleanSetting autoStop;
   private final BooleanSetting sprint;
   private final SliderSetting pitch;
   private final BooleanSetting instantFly;
   private final BooleanSetting checkSpeed;
   public final SliderSetting minSpeed;
   private final SliderSetting delay;
   private final SliderSetting timeout;
   private final SliderSetting sneakDownSpeed;
   private final SliderSetting boost;
   private final BooleanSetting freeze;
   private final BooleanSetting motionStop;
   private final SliderSetting infiniteMaxSpeed;
   private final SliderSetting infiniteMinSpeed;
   private final SliderSetting infiniteMaxHeight;
   public final BooleanSetting releaseSneak;
   private final BooleanSetting armorUnbreaking;
   private final SliderSetting armorFakeDelay;
   private final BooleanSetting armorPressSneak;
   private final SliderSetting armorReleaseDelay;
   private final BooleanSetting armorInventorySwap;
   private final SliderSetting armorPacketDelay;
   private final SliderSetting armorFallSpeed;
   private final BooleanSetting armorDeBug;
   private final BooleanSetting armorStand;
   public final BooleanSetting speedBypass;
   public final SliderSetting bypassStart;
   public final SliderSetting bypassMax;
   public final SliderSetting bypassUp;
   public final SliderSetting bypassBudget;
   public final BooleanSetting bypassDebug;
   public final BooleanSetting bypassAdapt;
   private final BooleanSetting turtleRamp;
   private final SliderSetting rampStep;
   private final SliderSetting rampMaxSetting;
   private final BooleanSetting rampReset;
   private final SliderSetting rampPerLevel;
   private final BooleanSetting rampTurtleOnly;
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
   private double speedBeforeSlow;
   private int armorPacketTimer;
   private int armorReleaseTimer;
   private int armorFireworkPackets;
   private int armorFireworkTimer;
   private int armorFakeTimer;
   private int armorFakeSlot;
   public final BooleanSetting 缓慢自适应;
   private final SliderSetting 缓慢速度;
   private final SliderSetting 正常速度;
   private boolean 先前缓慢状态;
   private final BooleanSetting autoChase;
   private final SliderSetting chaseHeight;
   private final SliderSetting chaseRange;
   private final SliderSetting chaseHorizontalSpeed;
   private Entity chaseTarget;
   private boolean isHovering;
   private boolean serverLookUp;
   private final Timer instantFlyTimer;
   private final Timer speedRampTimer;
   private float currentRampSpeed;
   private boolean wasFlying;
   boolean prev;
   float prePitch;
   private boolean hasElytra;
   float yaw;
   float rotationPitch;
   boolean flying;
   int packetDelayInt;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;

   public ElytraFly() {
      super("ElytraFly", Module.Category.Movement);
      this.infiniteDura.setChinese("\u65e0\u9650\u8010\u4e45");
      this.packet = this.add(new BooleanSetting("Packet", false).setParent());
      this.packet.setChinese("\u53d1\u5305");
      this.packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, this.packet::isOpen));
      this.packetDelay.setChinese("\u53d1\u5305\u5ef6\u8fdf");
      this.setFlag = this.add(new BooleanSetting("SetFlag", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.setFlag.setChinese("\u8bbe\u7f6e\u6807\u5fd7");
      this.firework = this.add(new BooleanSetting("Firework", false).setParent());
      this.firework.setChinese("\u70df\u82b1");
      this.fireWork = this.add(new BindSetting("FireWork", -1, this.firework::isOpen));
      this.fireWork.setChinese("\u70df\u82b1\u6309\u952e");
      this.packetInteract = this.add(new BooleanSetting("PacketInteract", true, this.firework::isOpen));
      this.packetInteract.setChinese("\u53d1\u5305\u4ea4\u4e92");
      this.inventory = this.add(new BooleanSetting("Inventory", true, this.firework::isOpen));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.onlyOne = this.add(new BooleanSetting("OnlyOne", true, this.firework::isOpen));
      this.onlyOne.setChinese("\u4ec5\u4e00\u4e2a");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, this.firework::isOpen));
      this.usingPause.setChinese("\u4f7f\u7528\u6682\u505c");
      this.autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.autoJump.setChinese("\u81ea\u52a8\u8df3\u8dc3");
      this.upPitch = this.add(new SliderSetting("UpPitch", 0.0, 0.0, 90.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.upPitch.setChinese("\u5411\u4e0a\u4ef0\u89d2");
      this.upFactor = this.add(new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.upFactor.setChinese("\u4e0a\u5347\u56e0\u5b50");
      this.downFactor = this.add(new SliderSetting("DownFactor", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.downFactor.setChinese("\u4e0b\u843d\u901f\u5ea6");
      this.speed = this.add(new SliderSetting("Speed", 1.0, 0.1F, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.speed.setChinese("\u901f\u5ea6");
      this.speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.speedLimit.setChinese("\u901f\u5ea6\u9650\u5236");
      this.maxSpeed = this.add(
         new SliderSetting("MaxSpeed", 2.5, 0.1F, 10.0, () -> this.speedLimit.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
      );
      this.maxSpeed.setChinese("\u6700\u5927\u901f\u5ea6");
      this.noDrag = this.add(new BooleanSetting("NoDrag", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.noDrag.setChinese("\u65e0\u963b\u529b");
      this.speedRamp = this.add(new BooleanSetting("SpeedRamp", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.speedRamp.setChinese("\u901f\u5ea6\u9012\u589e");
      this.rampPerSecond = this.add(
         new SliderSetting("RampPerSecond", 0.1, 0.01, 2.0, 0.01, () -> this.speedRamp.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
      );
      this.rampPerSecond.setChinese("\u6bcf\u79d2\u9012\u589e");
      this.rampMax = this.add(
         new SliderSetting("RampMax", 5.0, 0.1, 10.0, 0.1, () -> this.speedRamp.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
      );
      this.rampMax.setChinese("\u9012\u589e\u4e0a\u9650");
      this.autoReplaceElytra = this.add(new BooleanSetting("AutoReplaceElytra", false));
      this.autoReplaceElytra.setChinese("\u81ea\u52a8\u66ff\u6362\u9798\u7fc5");
      this.replaceDurability = this.add(new SliderSetting("ReplaceDurability", 5.0, 1.0, 50.0, 1.0, () -> this.autoReplaceElytra.getValue()));
      this.replaceDurability.setChinese("\u66ff\u6362\u8010\u4e45\u9608\u503c");
      this.autoAvoid = this.add(new BooleanSetting("AutoAvoid", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.autoAvoid.setChinese("\u81ea\u52a8\u907f\u969c");
      this.avoidDistance = this.add(
         new SliderSetting("AvoidDistance", 5.0, 2.0, 20.0, 0.5, () -> this.autoAvoid.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
      );
      this.avoidDistance.setChinese("\u907f\u969c\u68c0\u6d4b\u8ddd\u79bb");
      this.fireworkTimer = new Timer();
      this.autoStop = this.add(new BooleanSetting("AutoStop", true));
      this.autoStop.setChinese("\u81ea\u52a8\u505c\u6b62");
      this.sprint = this.add(new BooleanSetting("Sprint", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.sprint.setChinese("\u51b2\u523a");
      this.pitch = this.add(new SliderSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> this.mode.is(ElytraFly.Mode.Bounce)));
      this.pitch.setChinese("\u4ef0\u89d2");
      this.instantFly = this.add(new BooleanSetting("InstantFly", true, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.instantFly.setChinese("\u81ea\u52a8\u542f\u52a8");
      this.checkSpeed = this.add(new BooleanSetting("CheckSpeed", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.checkSpeed.setChinese("\u68c0\u67e5\u901f\u5ea6");
      this.minSpeed = this.add(new SliderSetting("MinSpeed", 70.0, 0.1, 200.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.minSpeed.setChinese("\u6700\u4f4e\u901f\u5ea6");
      this.delay = this.add(new SliderSetting("Delay", 1000.0, 0.0, 20000.0, 50.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.timeout = this.add(new SliderSetting("Timeout", 0.0, 0.1, 1.0, 0.1, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
      this.timeout.setChinese("\u8d85\u65f6");
      this.sneakDownSpeed = this.add(new SliderSetting("SneakDownSpeed", 1.0, 0.1, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
      this.sneakDownSpeed.setChinese("\u4e0b\u964d\u901f\u5ea6");
      this.boost = this.add(new SliderSetting("Boost", 1.0, 0.1, 4.0, () -> this.mode.getValue() == ElytraFly.Mode.Boost));
      this.boost.setChinese("\u52a0\u901f");
      this.freeze = this.add(new BooleanSetting("Freeze", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
      this.freeze.setChinese("\u51bb\u7ed3");
      this.motionStop = this.add(new BooleanSetting("MotionStop", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
      this.motionStop.setChinese("\u8fd0\u52a8\u505c\u6b62");
      this.infiniteMaxSpeed = this.add(new SliderSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.infiniteMaxSpeed.setChinese("\u65e0\u9650\u6700\u5927\u901f\u5ea6");
      this.infiniteMinSpeed = this.add(new SliderSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.infiniteMinSpeed.setChinese("\u65e0\u9650\u6700\u4f4e\u901f\u5ea6");
      this.infiniteMaxHeight = this.add(new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> this.mode.getValue() == ElytraFly.Mode.Pitch));
      this.infiniteMaxHeight.setChinese("\u65e0\u9650\u6700\u5927\u9ad8\u5ea6");
      this.releaseSneak = this.add(new BooleanSetting("ReleaseSneak", false));
      this.releaseSneak.setChinese("\u91ca\u653e\u6f5c\u884c");
      this.armorUnbreaking = this.add(new BooleanSetting("ArmorUnbreaking", true, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorUnbreaking.setChinese("\u7532\u98de\u8010\u4e45");
      this.armorFakeDelay = this.add(new SliderSetting("ArmorFakeDelay", 0.0, 0.0, 20.0, 1.0, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorFakeDelay.setChinese("\u7532\u98de\u4f2a\u88c5\u5ef6\u8fdf");
      this.armorPressSneak = this.add(new BooleanSetting("ArmorPressSneak", true, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorPressSneak.setChinese("\u7532\u98de\u81ea\u52a8\u6f5c\u884c");
      this.armorReleaseDelay = this.add(new SliderSetting("ArmorReleaseDelay", 0.0, 0.0, 20.0, 1.0, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorReleaseDelay.setChinese("\u7532\u98de\u6f5c\u884c\u5ef6\u8fdf");
      this.armorInventorySwap = this.add(new BooleanSetting("ArmorInventorySwap", true, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorInventorySwap.setChinese("\u7532\u98de\u7269\u54c1\u680f\u4ea4\u6362");
      this.armorPacketDelay = this.add(new SliderSetting("ArmorPacketDelay", 0.0, 0.0, 20.0, 1.0, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorPacketDelay.setChinese("\u7532\u98de\u4ea4\u6362\u5ef6\u8fdf");
      this.armorFallSpeed = this.add(new SliderSetting("ArmorFallSpeed", 0.01, 0.0, 2.0, 0.01, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorFallSpeed.setChinese("\u7532\u98de\u4e0b\u843d\u901f\u5ea6");
      this.armorDeBug = this.add(new BooleanSetting("ArmorDeBug", false, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorDeBug.setChinese("\u7532\u98de\u8c03\u8bd5");
      this.armorStand = this.add(new BooleanSetting("ArmorStand", true, () -> this.mode.is(ElytraFly.Mode.Armor)));
      this.armorStand.setChinese("\u7ad9\u98de");
      this.speedBypass = this.add(new BooleanSetting("SpeedBypass", false, () -> this.mode.getValue() == ElytraFly.Mode.Control).setParent());
      this.speedBypass.setChinese("\u901f\u5ea6\u7ed5\u8fc7");
      this.bypassStart = this.add(new SliderSetting("BypassStart", 0.5, 0.01, 10.0, 0.01, this.speedBypass::isOpen).setSuffix("b/t"));
      this.bypassStart.setChinese("\u8d77\u59cb\u901f\u5ea6");
      this.bypassMax = this.add(new SliderSetting("BypassMax", 3.0, 0.01, 50.0, 0.01, this.speedBypass::isOpen).setSuffix("b/t"));
      this.bypassMax.setChinese("\u6700\u5927\u901f\u5ea6");
      this.bypassUp = this.add(new SliderSetting("BypassUp", 2.0, 0.001, 10.0, 0.001, this.speedBypass::isOpen).setSuffix("/s"));
      this.bypassUp.setChinese("\u63d0\u901f\u901f\u7387");
      this.bypassBudget = this.add(new SliderSetting("BypassBudget", 10000.0, 10.0, 10000.0, 1.0, this.speedBypass::isOpen).setSuffix("vl"));
      this.bypassBudget.setChinese("\u9884\u7b97");
      this.bypassDebug = this.add(new BooleanSetting("BypassDebug", false, this.speedBypass::isOpen));
      this.bypassDebug.setChinese("\u8c03\u8bd5");
      this.bypassAdapt = this.add(new BooleanSetting("BypassAdapt", false, this.speedBypass::isOpen));
      this.bypassAdapt.setChinese("\u81ea\u9002\u5e94");
      this.turtleRamp = this.add(new BooleanSetting("TurtleRamp", true).setParent());
      this.turtleRamp.setChinese("\u7f13\u901f\u8865\u507f");
      this.rampStep = this.add(new SliderSetting("RampStep", 0.05, 0.01, 1.0, 0.01, this.turtleRamp::isOpen));
      this.rampStep.setChinese("\u6bcf\u6b21\u589e\u91cf");
      this.rampMaxSetting = this.add(new SliderSetting("RampMax", 3.0, 1.0, 20.0, 0.1, this.turtleRamp::isOpen));
      this.rampMaxSetting.setChinese("\u6700\u5927\u500d\u7387");
      this.rampReset = this.add(new BooleanSetting("RampReset", true, this.turtleRamp::isOpen));
      this.rampReset.setChinese("\u91cd\u7f6e");
      this.rampPerLevel = this.add(new SliderSetting("RampPerLevel", 0.15, 0.0, 1.0, 0.05, this.turtleRamp::isOpen));
      this.rampPerLevel.setChinese("\u6bcf\u7ea7\u589e\u91cf");
      this.rampTurtleOnly = this.add(new BooleanSetting("RampTurtleOnly", true, this.turtleRamp::isOpen));
      this.rampTurtleOnly.setChinese("\u4ec5\u6d77\u9f9f");
      this.armorPacketTimer = 0;
      this.armorReleaseTimer = 0;
      this.armorFireworkPackets = 0;
      this.armorFireworkTimer = 0;
      this.armorFakeTimer = 0;
      this.armorFakeSlot = 0;
      this.缓慢自适应 = this.add(new BooleanSetting("\u7f13\u6162\u81ea\u9002\u5e94", true).setParent());
      this.缓慢自适应.setChinese("\u7f13\u6162\u81ea\u9002\u5e94");
      this.缓慢速度 = this.add(new SliderSetting("\u7f13\u6162\u901f\u5ea6", 0.3, 0.1, 2.0, 0.1, this.缓慢自适应::isOpen));
      this.缓慢速度.setChinese("\u7f13\u6162\u901f\u5ea6");
      this.正常速度 = this.add(new SliderSetting("\u6b63\u5e38\u901f\u5ea6", 1.8, 1.0, 10.0, 0.1, this.缓慢自适应::isOpen));
      this.正常速度.setChinese("\u6b63\u5e38\u901f\u5ea6");
      this.先前缓慢状态 = false;
      this.autoChase = this.add(new BooleanSetting("AutoChase", false, () -> this.mode.is(ElytraFly.Mode.Freeze)));
      this.autoChase.setChinese("\u81ea\u52a8\u8ffd\u51fb");
      this.chaseHeight = this.add(new SliderSetting("ChaseHeight", 9.0, 1.0, 20.0, 1.0, () -> this.autoChase.getValue() && this.mode.is(ElytraFly.Mode.Freeze)));
      this.chaseHeight.setChinese("\u8ffd\u51fb\u9ad8\u5ea6");
      this.chaseRange = this.add(new SliderSetting("ChaseRange", 4.6, 1.0, 10.0, 0.1, () -> this.autoChase.getValue() && this.mode.is(ElytraFly.Mode.Freeze)));
      this.chaseRange.setChinese("\u8ffd\u51fb\u8303\u56f4");
      this.chaseHorizontalSpeed = this.add(
         new SliderSetting("ChaseHorizontalSpeed", 2.4, 0.1, 5.0, 0.1, () -> this.autoChase.getValue() && this.mode.is(ElytraFly.Mode.Freeze))
      );
      this.chaseHorizontalSpeed.setChinese("\u8ffd\u51fb\u6c34\u5e73\u901f\u5ea6");
      this.chaseTarget = null;
      this.isHovering = false;
      this.serverLookUp = false;
      this.instantFlyTimer = new Timer();
      this.speedRampTimer = new Timer();
      this.currentRampSpeed = 0.0F;
      this.wasFlying = false;
      this.hasElytra = false;
      this.yaw = 0.0F;
      this.rotationPitch = 0.0F;
      this.flying = false;
      this.packetDelayInt = 0;
      this.setChinese("\u9798\u7fc5\u98de\u884c");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ElytraFly.FireWorkTweak());
   }

   public void fireBoost() {
      this.off();
   }

   public void off() {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.onlyOne.getValue()) {
            for (Entity var2 : Alien.THREAD.getEntities()) {
               FireworkRocketEntity var3;
               if (var2 instanceof FireworkRocketEntity && ((IFireworkRocketEntity)(var3 = (FireworkRocketEntity)var2)).getShooter() == mc.player) {
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
            int var4;
            if (this.inventory.getValue() && (var4 = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(var4, mc.player.getInventory().selectedSlot);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               InventoryUtil.inventorySwap(var4, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            } else {
               int var5 = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
               if (var5 != -1) {
                  int var6 = mc.player.getInventory().selectedSlot;
                  InventoryUtil.switchToSlot(var5);
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

   private void offFireworkArmor() {
      if (this.firework.getValue() && this.hasElytra && !mc.player.isOnGround() && this.armorFireworkTimer <= 0) {
         if (!this.fireworkTimer.passed(this.delay.getValueInt())) {
            return;
         }

         this.fireworkTimer.reset();
         int var1;
         if (this.armorInventorySwap.getValue() && (var1 = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
            InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            this.armorFireworkPackets++;
            this.armorFireworkTimer = this.armorFakeDelay.getValueInt();
            return;
         }

         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(var0 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var0, mc.player.getYaw(), mc.player.getPitch()));
            this.armorFireworkPackets++;
            this.armorFireworkTimer = this.armorFakeDelay.getValueInt();
         }
      }

      if (this.armorFireworkTimer > 0) {
         this.armorFireworkTimer--;
      }
   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed()
         || mc.options.backKey.isPressed()
         || mc.options.leftKey.isPressed()
         || mc.options.rightKey.isPressed()
         || mc.options.jumpKey.isPressed();
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
      ItemStack var1;
      if (!var0.isTouchingWater()
         && !var0.hasStatusEffect(StatusEffects.LEVITATION)
         && (var1 = var0.getEquippedStack(EquipmentSlot.CHEST)).isOf(Items.ELYTRA)
         && ElytraItem.isUsable(var1)) {
         var0.startFallFlying();
         return true;
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
         this.chaseTarget = null;
         this.isHovering = false;
         this.serverLookUp = false;
         this.currentRampSpeed = 0.0F;
         this.wasFlying = false;
         this.speedBeforeSlow = this.speed.getValue();
         this.先前缓慢状态 = false;
         this.speedRampTimer.reset();
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         if (this.releaseSneak.getValue()) {
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }

         if (this.serverLookUp) {
            mc.getNetworkHandler().sendPacket(new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
            this.serverLookUp = false;
         }

         this.chaseTarget = null;
         this.isHovering = false;
      }
   }

   private void boost() {
      if (this.hasElytra && this.isFallFlying()) {
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
         if (this.mode.is(ElytraFly.Mode.Rotation)) {
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
         } else if (this.mode.is(ElytraFly.Mode.Pitch) && this.isFallFlying()) {
            var1.setPitch(this.infinitePitch);
         } else if (this.mode.is(ElytraFly.Mode.Bounce) && this.isFallFlying()) {
            var1.setPitch(this.pitch.getValueFloat());
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.getInfinitePitch();
      this.updateRampFactor();
      boolean var2 = mc.player.hasStatusEffect(StatusEffects.SLOWNESS);
      if (var2 && !this.先前缓慢状态) {
         this.speedBeforeSlow = this.speed.getValue();
         this.speed.setValue(0.3);
      } else if (!var2 && this.先前缓慢状态) {
         this.speed.setValue(1.8);
      }

      this.先前缓慢状态 = var2;
      this.flying = false;
      if (this.mode.is(ElytraFly.Mode.Armor)) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1 || mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
         float var3 = Sprint.getSprintYaw(mc.player.getYaw());
         float var4 = mc.player.getPitch();
         mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), var3, var4, mc.player.isOnGround()));
         var2 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
            && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
         if (var2 && !mc.player.isFallFlying() && !mc.player.isOnGround()) {
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
         }

         if (!mc.player.isOnGround() && this.hasElytra) {
            if (this.armorPressSneak.getValue()) {
               mc.getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
               this.armorReleaseTimer = this.armorReleaseDelay.getValueInt();
            }

            this.armorPacketTimer++;
            if (this.armorPacketTimer > this.armorPacketDelay.getValueInt()) {
               int var5 = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
               if (var5 == -1) {
                  return;
               }

               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
               if (!mc.player.isOnGround()) {
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                  mc.player.startFallFlying();
               }

               this.offFireworkArmor();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var5, 0, SlotActionType.PICKUP, mc.player);
               this.armorPacketTimer = 0;
            }
         }

         if (this.armorReleaseTimer > 0) {
            this.armorReleaseTimer--;
            if (this.armorReleaseTimer == 0) {
               mc.getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
         }
      }

      int var20;
      ItemStack var21;
      if (this.autoReplaceElytra.getValue()
         && this.isFallFlying()
         && (var21 = mc.player.getEquippedStack(EquipmentSlot.CHEST)).getItem() == Items.ELYTRA
         && var21.getMaxDamage() - var21.getDamage() <= this.replaceDurability.getValueInt()
         && (var20 = InventoryUtil.findItemInventorySlot(Items.ELYTRA)) != -1) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var20, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var20, 0, SlotActionType.PICKUP, mc.player);
         mc.getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         if (this.setFlag.getValue()) {
            mc.player.startFallFlying();
         }
      }

      if (this.packet.getValue()) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      } else {
         this.hasElytra = false;

         for (ItemStack var6 : mc.player.getArmorItems()) {
            if (var6.getItem() instanceof ElytraItem) {
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

         if (this.mode.is(ElytraFly.Mode.Bounce)) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(0);
            return;
         }
      }

      double var23 = mc.player.getX() - mc.player.prevX;
      double var7 = mc.player.getY() - mc.player.prevY;
      double var9 = mc.player.getZ() - mc.player.prevZ;
      double var11 = Math.sqrt(var23 * var23 + var9 * var9 + var7 * var7) / 1000.0;
      double var13 = 1.388888888888889E-5;
      float var15 = Alien.TIMER.get();
      double var16 = var11 / var13 * var15;
      if (this.mode.getValue() == ElytraFly.Mode.Boost) {
         this.boost();
      }

      if (this.packet.getValue()) {
         if (!mc.player.isOnGround()) {
            this.packetDelayInt++;
            if (!(this.packetDelayInt <= this.packetDelay.getValue())) {
               int var18 = InventoryUtil.findItem(Items.ELYTRA);
               if (var18 != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, var18, SlotActionType.SWAP, mc.player);
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                  mc.player.startFallFlying();
                  if ((!this.checkSpeed.getValue() || var16 <= this.minSpeed.getValue())
                     && this.firework.getValue()
                     && this.fireworkTimer.passed(this.delay.getValueInt())
                     && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
                     && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                     && this.isFallFlying()) {
                     this.off();
                     this.fireworkTimer.reset();
                  }

                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, var18, SlotActionType.SWAP, mc.player);
                  this.packetDelayInt = 0;
               } else {
                  var18 = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                  if (var18 != -1) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var18, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.getNetworkHandler()
                        .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     if ((!this.checkSpeed.getValue() || var16 <= this.minSpeed.getValue())
                        && this.firework.getValue()
                        && this.fireworkTimer.passed(this.delay.getValueInt())
                        && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
                        && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                        && this.isFallFlying()) {
                        this.off();
                        this.fireworkTimer.reset();
                     }

                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var18, 0, SlotActionType.PICKUP, mc.player);
                     this.packetDelayInt = 0;
                  }
               }
            }
         }
      } else {
         if ((!this.checkSpeed.getValue() || var16 <= this.minSpeed.getValue())
            && this.firework.getValue()
            && this.fireworkTimer.passed(this.delay.getValueInt())
            && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
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
   public void onElytraUpdate(ElytraUpdateEvent var1) {
      if (this.mode.is(ElytraFly.Mode.Armor) && this.armorStand.getValue()) {
         var1.cancel();
      }
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
      if (!nullCheck() && this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
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

   @EventListener
   private void onPacketSend(PacketEvent.Send var1) {
      ClientCommandC2SPacket var2;
      Packet var3;
      if (!nullCheck()
         && this.mode.is(ElytraFly.Mode.Bounce)
         && this.hasElytra
         && (var3 = var1.getPacket()) instanceof ClientCommandC2SPacket
         && (var2 = (ClientCommandC2SPacket)var3).getMode().equals(net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
         && !this.sprint.getValue()) {
         mc.player.setSprinting(true);
      }
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive var1) {
      if (this.speedRamp.getValue() && var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.currentRampSpeed = 0.0F;
         this.wasFlying = false;
         this.speedRampTimer.reset();
      }

      if (!nullCheck() && this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra && var1.getPacket() instanceof PlayerPositionLookS2CPacket) {
         mc.player.stopFallFlying();
      }
   }

   @EventListener
   public void travel(TravelEvent var1) {
      if (!nullCheck() && !AntiCheat.INSTANCE.movementSync()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
            if (var1.isPre()) {
               this.prev = true;
               this.prePitch = mc.player.getPitch();
               mc.player.setPitch(this.pitch.getValueFloat());
            } else if (this.prev) {
               this.prev = false;
               mc.player.setPitch(this.prePitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Pitch) && this.isFallFlying()) {
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

   @EventListener
   public void onMove(TravelEvent var1) {
      if (!nullCheck() && this.hasElytra && this.isFallFlying() && !var1.isPost()) {
         if (this.mode.is(ElytraFly.Mode.Freeze) && this.autoChase.getValue()) {
            this.handleAutoChase(var1);
         } else {
            if (this.mode.is(ElytraFly.Mode.Control)) {
               float var2 = (float)(this.speed.getValueFloat() * this.rampFactor);
               boolean var3 = Follower.INSTANCE != null && Follower.INSTANCE.shouldDrive();
               boolean var4 = Follower.INSTANCE != null && Follower.INSTANCE.shouldVerticalDrive();
               if (this.speedRamp.getValue()) {
                  boolean var5 = this.isFallFlying() && MovementUtil.isMoving();
                  if (var5 && !mc.player.isOnGround()) {
                     if (!this.wasFlying) {
                        this.currentRampSpeed = 0.0F;
                        this.speedRampTimer.reset();
                     }

                     if (this.speedRampTimer.passed(1000L)) {
                        this.speedRampTimer.reset();
                        this.currentRampSpeed = this.currentRampSpeed + this.rampPerSecond.getValueFloat();
                        if (this.currentRampSpeed > this.rampMax.getValueFloat()) {
                           this.currentRampSpeed = this.rampMax.getValueFloat();
                        }
                     }
                  } else {
                     this.currentRampSpeed = 0.0F;
                     this.speedRampTimer.reset();
                  }

                  this.wasFlying = var5 && !mc.player.isOnGround();
                  var2 += this.currentRampSpeed;
               }

               Vec3d var22;
               if (this.autoAvoid.getValue()
                  && MovementUtil.isMoving()
                  && mc.world
                        .raycast(
                           new RaycastContext(
                              var22 = mc.player.getEyePos(),
                              var22.add(Vec3d.fromPolar(0.0F, mc.player.getYaw()).multiply(this.avoidDistance.getValue())),
                              ShapeType.COLLIDER,
                              FluidHandling.NONE,
                              mc.player
                           )
                        )
                        .getType()
                     == Type.BLOCK) {
                  float[] var24 = new float[]{-45.0F, 45.0F, -90.0F, 90.0F, -30.0F, 30.0F, -60.0F, 60.0F, -15.0F, 15.0F};
                  float[] var25 = new float[]{0.0F, -10.0F, 10.0F, -20.0F, 20.0F};
                  boolean var8 = false;

                  label167:
                  for (float var12 : var25) {
                     for (float var16 : var24) {
                        float var17 = mc.player.getYaw() + var16;
                        Vec3d var18 = Vec3d.fromPolar(var12, var17).multiply(this.avoidDistance.getValue());
                        BlockHitResult var19 = mc.world.raycast(new RaycastContext(var22, var22.add(var18), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                        if (var19.getType() != Type.BLOCK) {
                           float var20 = var17 * (float) (Math.PI / 180.0);
                           this.setX(-MathHelper.sin(var20) * var2);
                           this.setZ(MathHelper.cos(var20) * var2);
                           var8 = true;
                           break label167;
                        }
                     }
                  }

                  if (!var8) {
                     this.setY(this.upFactor.getValue());
                  }

                  var1.cancel();
                  mc.player.move(MovementType.SELF, mc.player.getVelocity());
                  return;
               }

               if (this.firework.getValue()) {
                  if (var4) {
                     this.setY(Follower.INSTANCE.getVerticalDrive());
                  } else if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
                     this.setY(0.0);
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (mc.player.input.jumping) {
                     this.setY(this.upFactor.getValue());
                  } else {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  double[] var23 = var3 ? this.directionSpeedByYaw(var2, Follower.INSTANCE.getDriveYaw()) : MovementUtil.directionSpeed(var2);
                  this.setX(var23[0]);
                  this.setZ(var23[1]);
               } else {
                  Vec3d var6 = this.getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
                  double var7 = Math.sqrt(var6.x * var6.x + var6.z * var6.z);
                  double var9 = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (var4) {
                     this.setY(Follower.INSTANCE.getVerticalDrive());
                  } else if (mc.player.input.sneaking) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (!mc.player.input.jumping) {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  if (mc.player.input.jumping && !var4) {
                     if (var9 > this.upFactor.getValue() / this.upFactor.getMax()) {
                        double var11 = var9 * 0.01325;
                        this.setY(this.getY() + var11 * 3.2);
                        this.setX(this.getX() - var6.x * var11 / var7);
                        this.setZ(this.getZ() - var6.z * var11 / var7);
                     } else {
                        double[] var27 = var3 ? this.directionSpeedByYaw(var2, Follower.INSTANCE.getDriveYaw()) : MovementUtil.directionSpeed(var2);
                        this.setX(var27[0]);
                        this.setZ(var27[1]);
                     }
                  }

                  if (var7 > 0.0 && !var3) {
                     this.setX(this.getX() + (var6.x / var7 * var9 - this.getX()) * 0.1);
                     this.setZ(this.getZ() + (var6.z / var7 * var9 - this.getZ()) * 0.1);
                  }

                  if (!mc.player.input.jumping || var3) {
                     double[] var28 = var3 ? this.directionSpeedByYaw(var2, Follower.INSTANCE.getDriveYaw()) : MovementUtil.directionSpeed(var2);
                     this.setX(var28[0]);
                     this.setZ(var28[1]);
                  }

                  if (!this.noDrag.getValue()) {
                     this.setY(this.getY() * 0.99F);
                     this.setX(this.getX() * 0.98F);
                     this.setZ(this.getZ() * 0.99F);
                  }

                  double var29 = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (this.speedLimit.getValue() && var29 > this.maxSpeed.getValue()) {
                     this.setX(this.getX() * this.maxSpeed.getValue() / var29);
                     this.setZ(this.getZ() * this.maxSpeed.getValue() / var29);
                  }

                  var1.cancel();
                  mc.player.move(MovementType.SELF, mc.player.getVelocity());
               }
            }

            if (this.mode.is(ElytraFly.Mode.Armor) && this.hasElytra && !mc.player.isOnGround()) {
               if (!this.wantToMove()) {
                  Vec3d var21 = mc.player.getVelocity();
                  ((IVec3d)var21).setX(0.0);
                  ((IVec3d)var21).setZ(0.0);
                  ((IVec3d)var21).setY(-this.armorFallSpeed.getValue());
               }

               var1.cancel();
               mc.player.move(MovementType.SELF, mc.player.getVelocity());
            }

            if (this.mode.is(ElytraFly.Mode.Freeze)
               && !this.autoChase.getValue()
               && !MovementUtil.isMoving()
               && !mc.options.jumpKey.isPressed()
               && !mc.options.sneakKey.isPressed()) {
               var1.cancel();
            }

            if (this.mode.is(ElytraFly.Mode.Rotation)
               && this.freeze.getValue()
               && !MovementUtil.isMoving()
               && !mc.options.jumpKey.isPressed()
               && !mc.options.sneakKey.isPressed()) {
               var1.cancel();
            }
         }
      }
   }

   private double[] directionSpeedByYaw(double var1, float var3) {
      double var4 = Math.sin(Math.toRadians(var3 + 90.0F));
      double var6 = Math.cos(Math.toRadians(var3 + 90.0F));
      return new double[]{var1 * var6, var1 * var4};
   }

   private void handleAutoChase(TravelEvent var1) {
      double var2 = 0.0;
      this.chaseTarget = this.findNearestEnemy();
      if (this.chaseTarget == null) {
         if (this.serverLookUp) {
            mc.getNetworkHandler().sendPacket(new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
            this.serverLookUp = false;
         }

         this.sendMsg(Formatting.RED + "[AutoChase] \u6ca1\u6709\u53ef\u653b\u51fb\u7684\u76ee\u6807\uff01");
      } else {
         double var4 = this.chaseTarget.getX();
         double var6 = this.chaseTarget.getY() + this.chaseHeight.getValue();
         double var8 = this.chaseTarget.getZ();
         double var10 = var4 - mc.player.getX();
         double var12 = var8 - mc.player.getZ();
         double var14 = Math.sqrt(var10 * var10 + var12 * var12);
         double var16 = var6 - mc.player.getY();
         boolean var18 = var16 <= 1.0 && var14 <= this.chaseRange.getValue();
         if (var18) {
            if (this.serverLookUp) {
               mc.getNetworkHandler().sendPacket(new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
               this.serverLookUp = false;
            }

            this.isHovering = true;
            var1.cancel();
            this.setX(0.0);
            this.setZ(0.0);
            this.setY(0.0);
            mc.player.move(MovementType.SELF, mc.player.getVelocity());
         } else {
            if (this.isHovering) {
               this.isHovering = false;
               this.sendMsg(Formatting.YELLOW + "[AutoChase] \u76ee\u6807\u79fb\u52a8\uff0c\u91cd\u65b0\u8ffd\u51fb");
            }

            var1.cancel();
            if (var16 > 1.0) {
               if (!this.serverLookUp) {
                  float var19 = mc.player.getYaw();
                  mc.getNetworkHandler().sendPacket(new LookAndOnGround(var19, -90.0F, mc.player.isOnGround()));
                  this.serverLookUp = true;
               }

               if (this.fireworkTimer.passed(this.delay.getValueInt())) {
                  if (this.hasFirework()) {
                     this.off();
                     this.fireworkTimer.reset();
                  } else {
                     this.sendMsg(Formatting.YELLOW + "[AutoChase] \u80cc\u5305\u6ca1\u6709\u70df\u82b1\uff01");
                  }
               }
            } else {
               if (this.serverLookUp) {
                  mc.getNetworkHandler().sendPacket(new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
                  this.serverLookUp = false;
               }

               double var25 = this.chaseHorizontalSpeed.getValue();
               if (var14 > 1.0) {
                  double var21 = var10 / var14;
                  double var23 = var12 / var14;
                  this.setX(var21 * var25);
                  this.setZ(var23 * var25);
               } else {
                  this.setX(0.0);
                  this.setZ(0.0);
               }

               if (Math.abs(var16) > 0.5) {
                  this.setY(var16 > 0.0 ? 0.2 : -0.2);
               } else {
                  this.setY(0.0);
               }
            }

            double var26 = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
            if (var2 > this.maxSpeed.getValue()) {
               this.setX(this.getX() * this.maxSpeed.getValue() / var26);
               this.setZ(this.getZ() * this.maxSpeed.getValue() / var26);
            }

            mc.player.move(MovementType.SELF, mc.player.getVelocity());
         }
      }
   }

   private Entity findNearestEnemy() {
      double var1 = Double.MAX_VALUE;
      PlayerEntity var3 = null;

      for (Entity var5 : mc.world.getEntities()) {
         PlayerEntity var6;
         if (var5 instanceof PlayerEntity
            && (var6 = (PlayerEntity)var5) != mc.player
            && !var6.isSpectator()
            && var6.isAlive()
            && !Alien.FRIEND.isFriend(var6.getName().getString())) {
            double var7 = mc.player.squaredDistanceTo(var6);
            if (var7 < var1) {
               var1 = var7;
               var3 = var6;
            }
         }
      }

      return var3;
   }

   private boolean hasFirework() {
      return InventoryUtil.findItem(Items.FIREWORK_ROCKET) != -1;
   }

   private void sendMsg(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(var1), false);
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
      return mc.player.isFallFlying()
         || this.packet.getValue() && this.hasElytra && !mc.player.isOnGround()
         || this.mode.is(ElytraFly.Mode.Armor) && this.hasElytra && !mc.player.isOnGround()
         || this.flying;
   }

   public static void updateSlowSpeedGlobal() {
      if (INSTANCE != null && Module.mc != null && Module.mc.player != null) {
         if (Module.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            INSTANCE.speed.setValue(0.3);
         } else {
            INSTANCE.speed.setValue(1.8);
         }
      }
   }

   private double bypassFloor() {
      return this.bypassStart.getValue();
   }

   private double updateRampFactor() {
      if (this.turtleRamp.getValue() && !Module.nullCheck()) {
         StatusEffectInstance var1 = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
         boolean var2 = var1 != null;
         if (var2 && this.rampTurtleOnly.getValue()) {
            var2 = mc.player.hasStatusEffect(StatusEffects.RESISTANCE);
         }

         this.rampActive = var2;
         if (var2) {
            int var3 = var1.getAmplifier();
            if (var3 != this.lastSlowAmplifier) {
               this.lastSlowAmplifier = var3;
               this.rampFactor = 1.0;
            }

            this.rampFactor = this.rampFactor + this.rampStep.getValue() + this.rampPerLevel.getValue() * (var3 + 1);
            if (this.rampFactor > this.rampMaxSetting.getValue()) {
               this.rampFactor = this.rampMaxSetting.getValue();
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
      if (this.speedBypass.getValue() && !Module.nullCheck()) {
         if (this.bypassDebug.getValue() && ++this.bypassDbgTick % 20 == 0 && mc.player.isFallFlying()) {
            double var2 = mc.player.getX() - this.bypassDbgX;
            double var4 = mc.player.getZ() - this.bypassDbgZ;
            double var6 = Math.sqrt(var2 * var2 + var4 * var4);
            CommandManager.sendMessage(
               String.format(
                  "\u00a77[\u901f\u5ea6\u7ed5\u8fc7] \u00a7fspd=\u00a7a%.2f\u00a7f tgt=\u00a7b%.2f\u00a7f cap=\u00a73%.2f\u00a7f vl=\u00a7e%.0f\u00a7f pitch=\u00a7d%.1f",
                  var6,
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
            double var8 = this.bypassFloor();
            if (this.bypassSpeed < var8) {
               this.bypassSpeed = var8;
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

            double var10 = Math.max(var9, var8);
            if (this.bypassCap > 0.0) {
               var10 = Math.min(var10, Math.max(var8, this.bypassCap));
            }

            if (!this.bypassPrimed) {
               this.bypassSpeed = var10;
               this.bypassVL = 0.0;
               this.bypassPrimed = true;
            } else {
               this.bypassSpeed = Math.min(this.bypassSpeed + this.bypassUp.getValue() / 20.0, var10);
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
      if (this.speedBypass.getValue() && !Module.nullCheck() && !this.rampActive && mc.player.isFallFlying()) {
         double var2 = var1.getX();
         double var4;
         double var6 = Math.sqrt(var2 * var2 + (var4 = var1.getZ()) * var4);
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
                  var1.setX(var2 * var18);
                  var1.setZ(var4 * var18);
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
         if (!Module.nullCheck() && !var1.isPost() && (!ElytraFly.this.inventory.getValue() || EntityUtil.inInventory())) {
            if (Wrapper.mc.currentScreen == null) {
               if (ElytraFly.this.fireWork.isPressed()) {
                  if (!this.press
                     && ElytraFly.this.fireworkTimer.passed(ElytraFly.this.delay.getValueInt())
                     && (!Wrapper.mc.player.isUsingItem() || !ElytraFly.this.usingPause.getValue())
                     && ElytraFly.this.isFallFlying()) {
                     ElytraFly.this.off();
                     ElytraFly.this.fireworkTimer.reset();
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

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch,
      Armor;
   }
}
