package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.DamageUtils;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.api.utils.world.PearlTrajectoryUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.enums.PlaceFirstMode;
import dev.luminous.mod.modules.settings.enums.RaytraceMode;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoCrystalPlusPlus extends Module {
   public static AutoCrystalPlusPlus INSTANCE;
   public volatile BlockPos crystalPos;
   public final Timer lastBreakTimer = new Timer();
   private final EnumSetting<AutoCrystalPlusPlus.Page> page = this.add(new EnumSetting<>("Page", AutoCrystalPlusPlus.Page.General));
   final Animation animation;
   final DecimalFormat df;
   private final Timer baseTimer;
   private final Timer placeTimer;
   private final Timer breakTimer;
   private final Timer noPosTimer;
   private final Timer switchTimer;
   private final Timer calcDelay;
   private final Timer renderRetryTimer;
   private final BindSetting pause;
   private final BooleanSetting preferAnchor;
   private final BooleanSetting breakOnlyHasCrystal;
   private final BooleanSetting eatingPause;
   private final BooleanSetting antiPearl;
   private final SliderSetting antiPearlLandingRange;
   private final SliderSetting antiPearlBlastRange;
   private final SliderSetting antiPearlLeadTicks;
   private final SliderSetting antiPearlAttempts;
   private final SliderSetting antiPearlRetryDelay;
   private final SliderSetting switchCooldown;
   private final SliderSetting targetRange;
   private final SliderSetting updateDelay;
   private final BooleanSetting invincibilityTimeout;
   private final SliderSetting invincTimeout;
   private final BooleanSetting selfInvincibility;
   private final BooleanSetting rotate;
   private final BooleanSetting onPlace;
   private final BooleanSetting onBreak;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final SliderSetting minDamage;
   private final SliderSetting maxSelf;
   private final SliderSetting reserve;
   private final BooleanSetting balance;
   private final SliderSetting balanceOffset;
   private final BooleanSetting place;
   public final SliderSetting placeRange;
   private final SliderSetting placeDelay;
   private final EnumSetting<AutoCrystalPlusPlus.SwapMode> autoSwap;
   private final EnumSetting<PlaceFirstMode> placeFirst;
   private final BooleanSetting afterBreak;
   private final BooleanSetting forcePlace;
   private final BooleanSetting facePlace;
   private final SliderSetting facePlaceHealth;
   private final SliderSetting facePlaceArmor;
   private final BooleanSetting breakSetting;
   public final SliderSetting breakRange;
   private final SliderSetting breakDelay;
   private final SliderSetting minAge;
   private final BooleanSetting breakRemove;
   private final BooleanSetting onAdd;
   private final BooleanSetting resetCD;
   private final SliderSetting hitFactor;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting interactOnRender;
   private final SliderSetting wallRange;
   private final EnumSetting<SwingSide> swingMode;
   private final ColorSetting text;
   private final EnumSetting<AutoCrystalPlusPlus.TargetESP> mode;
   private final SliderSetting animationTime;
   private final EnumSetting<Easing> ease;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final ColorSetting hitColor;
   private final ColorSetting hitOutlineColor;
   private final BooleanSetting render;
   private final BooleanSetting sync;
   private final BooleanSetting shrink;
   private final ColorSetting box;
   private final SliderSetting lineWidth;
   private final ColorSetting fill;
   private final SliderSetting sliderSpeed;
   private final SliderSetting startFadeTime;
   private final SliderSetting fadeSpeed;
   private final SliderSetting attackVecStep;
   private final BooleanSetting thread;
   private final BooleanSetting doCrystal;
   private final SliderSetting selfPredict;
   private final SliderSetting predictTicks;
   private final SliderSetting simulation;
   private final SliderSetting maxMotionY;
   private final BooleanSetting step;
   private final BooleanSetting doubleStep;
   private final BooleanSetting jump;
   private final BooleanSetting inBlockPause;
   private final BooleanSetting terrainIgnore;
   private final EnumSetting<RaytraceMode> raytraceMode;
   private final BooleanSetting basePlace;
   private final SliderSetting baseMin;
   private final SliderSetting baseMax;
   private final SliderSetting overrideMax;
   private final BooleanSetting baseBalance;
   private final BooleanSetting onlyBelow;
   private final BooleanSetting inventory;
   private final BooleanSetting detectMining;
   private final SliderSetting delay;
   private final BooleanSetting ignoreMine;
   private final SliderSetting constantProgress;
   private final BooleanSetting antiSurround;
   private final SliderSetting miningProgress;
   private final SliderSetting antiSurroundMax;
   private final BooleanSetting slowPlace;
   private final SliderSetting slowDelay;
   private final SliderSetting slowMinDamage;
   private final BooleanSetting rangeOverride;
   private final SliderSetting rangeOverrideDistance;
   private final SliderSetting rangeOverrideDamage;
   private final BooleanSetting lethalOverride;
   private final SliderSetting forceMaxHealth;
   private final SliderSetting forceMin;
   private final BooleanSetting armorBreaker;
   private final SliderSetting maxDurable;
   private final SliderSetting armorBreakerDamage;
   private final BooleanSetting forceWeb;
   public final BooleanSetting airPlace;
   public final BooleanSetting replace;
   private final SliderSetting hurtTime;
   private final SliderSetting waitHurt;
   private final SliderSetting syncTimeout;
   private final Timer syncTimer;
   private final Timer pendingPlaceTimer;
   public volatile PlayerEntity displayTarget;
   public float breakDamage;
   public float tempDamage;
   public volatile float lastDamage;
   public volatile Vec3d directionVec;
   double currentFade;
   private EndCrystalEntity tempBreakCrystal;
   private volatile EndCrystalEntity breakCrystal;
   private volatile EndCrystalEntity pendingBreakCrystal;
   private BlockPos tempPos;
   private BlockPos syncPos;
   private volatile BlockPos lastPlacedPos;
   private volatile BlockPos pendingPlacePos;
   private volatile long lastValidCrystalPosAt;
   private volatile int lastBreakEntityId;
   private volatile long lastBreakPacketAt;
   private Vec3d placeVec3d;
   private Vec3d curVec3d;
   int lastSlot;
   BlockPos tempBasePos;
   volatile BlockPos basePos;
   private boolean facePlacing;
   private final Map<UUID, Long> playerJoinTime;
   private long enableTime;
   private final Set<Integer> attackedIds;
   private static final long FOOD_USE_GRACE_MS = 250L;
   private static final long BREAK_PACKET_FLOOR_MS = 20L;
   private static final long SAME_CRYSTAL_RETRY_MS = 100L;
   private long foodUseProtectionUntil;
   private boolean refillPause;
   private final Map<Integer, AutoCrystalPlusPlus.AntiPearlDefense> antiPearlDefenses;
   private BlockPos antiPearlPendingPos;
   private long antiPearlPendingUntil;
   private int antiPearlPendingId;

   public AutoCrystalPlusPlus() {
      super("AutoCrystalPlusPlus", Module.Category.Combat);
      this.page.setChinese("\u9875\u9762");
      this.animation = new Animation();
      this.df = new DecimalFormat("0.0");
      this.baseTimer = new Timer();
      this.placeTimer = new Timer();
      this.breakTimer = new Timer();
      this.noPosTimer = new Timer();
      this.switchTimer = new Timer();
      this.calcDelay = new Timer();
      this.renderRetryTimer = new Timer();
      this.pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(AutoCrystalPlusPlus.Page.Check)));
      this.pause.setChinese("\u6682\u505c");
      this.preferAnchor = this.add(new BooleanSetting("PreferAnchor", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check));
      this.preferAnchor.setChinese("\u4f18\u5148\u951a");
      this.breakOnlyHasCrystal = this.add(new BooleanSetting("BreakOnlyHasCrystal", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check));
      this.breakOnlyHasCrystal.setChinese("\u4ec5\u624b\u6301");
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check));
      this.eatingPause.setChinese("\u8fdb\u98df\u6682\u505c");
      this.antiPearl = this.add(new BooleanSetting("AntiPearl", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check).setParent());
      this.antiPearl.setChinese("\u53cd\u73cd\u73e0");
      this.antiPearlLandingRange = this.add(
         new SliderSetting(
            "AntiPearlLandingRange", 6.0, 2.0, 12.0, 0.5, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.antiPearl.isOpen()
         )
      );
      this.antiPearlLandingRange.setChinese("\u73cd\u73e0\u843d\u70b9\u8303\u56f4");
      this.antiPearlBlastRange = this.add(
         new SliderSetting("AntiPearlBlastRange", 4.5, 2.0, 6.0, 0.25, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.antiPearl.isOpen())
      );
      this.antiPearlBlastRange.setChinese("\u8f68\u8ff9\u7206\u70b8\u8303\u56f4");
      this.antiPearlLeadTicks = this.add(
         new SliderSetting("AntiPearlLeadTicks", 4.0, 1.0, 10.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.antiPearl.isOpen())
      );
      this.antiPearlLeadTicks.setChinese("\u63d0\u524d\u5f15\u7206Tick");
      this.antiPearlAttempts = this.add(
         new SliderSetting("AntiPearlAttempts", 3.0, 1.0, 5.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.antiPearl.isOpen())
      );
      this.antiPearlAttempts.setChinese("\u6700\u5927\u62e6\u622a\u6b21\u6570");
      this.antiPearlRetryDelay = this.add(
         new SliderSetting(
               "AntiPearlRetryDelay", 75.0, 50.0, 250.0, 5.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.antiPearl.isOpen()
            )
            .setSuffix("ms")
      );
      this.antiPearlRetryDelay.setChinese("\u518d\u6b21\u62e6\u622a\u5ef6\u8fdf");
      this.switchCooldown = this.add(
         new SliderSetting("SwitchCooldown", 100, 0, 1000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check).setSuffix("ms")
      );
      this.switchCooldown.setChinese("\u5207\u6362\u51b7\u5374");
      this.targetRange = this.add(
         new SliderSetting("TargetRange", 12.0, 0.0, 20.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check).setSuffix("m")
      );
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check).setSuffix("ms"));
      this.updateDelay.setChinese("\u66f4\u65b0\u5ef6\u8fdf");
      this.invincibilityTimeout = this.add(
         new BooleanSetting("InvincibilityTimeout", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check).setParent()
      );
      this.invincibilityTimeout.setChinese("\u65e0\u654c\u68c0\u6d4b");
      this.invincTimeout = this.add(
         new SliderSetting("InvincTimeout", 5000, 0, 20000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.invincibilityTimeout.isOpen())
            .setSuffix("ms")
      );
      this.invincTimeout.setChinese("\u65e0\u654c\u65f6\u95f4");
      this.selfInvincibility = this.add(
         new BooleanSetting("SelfInvincibility", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Check && this.invincibilityTimeout.isOpen())
      );
      this.selfInvincibility.setChinese("\u81ea\u8eab\u65e0\u654c");
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation).setParent());
      this.rotate.setChinese("\u8f6c\u5934");
      this.onPlace = this.add(new BooleanSetting("OnPlace", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation));
      this.onPlace.setChinese("\u653e\u7f6e\u65f6\u8f6c\u5934");
      this.onBreak = this.add(new BooleanSetting("OnBreak", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation));
      this.onBreak.setChinese("\u5f15\u7206\u65f6\u8f6c\u5934");
      this.yawStep = this.add(
         new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation).setParent()
      );
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(
         new BooleanSetting(
            "WhenElytra", true, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation && this.yawStep.isOpen()
         )
      );
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(
         new SliderSetting(
            "Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation
         )
      );
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(
         new BooleanSetting("CheckFov", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation)
      );
      this.checkFov.setChinese("\u4ec5\u89c6\u7ebf\u5185");
      this.fov = this.add(
         new SliderSetting(
            "\u89c6\u91ce",
            20.0,
            0.0,
            360.0,
            0.1,
            () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation
         )
      );
      this.priority = this.add(
         new SliderSetting(
            "Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystalPlusPlus.Page.Rotation
         )
      );
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.minDamage = this.add(new SliderSetting("MinDamage", 5.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setSuffix("dmg"));
      this.minDamage.setChinese("\u6700\u5c0f\u4f24\u5bb3");
      this.maxSelf = this.add(new SliderSetting("MaxSelf", 12.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setSuffix("dmg"));
      this.maxSelf.setChinese("\u6700\u5927\u81ea\u4f24");
      this.reserve = this.add(new SliderSetting("Reserve", 2.0, 0.0, 10.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setSuffix("hp"));
      this.reserve.setChinese("\u8840\u91cf\u9884\u7559");
      this.balance = this.add(new BooleanSetting("Balance", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setParent());
      this.balance.setChinese("\u5e73\u8861");
      this.balanceOffset = this.add(
         new SliderSetting("BalanceOffset", 0.0, -20.0, 20.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.balance.isOpen())
            .setSuffix("hp")
      );
      this.balanceOffset.setChinese("\u5e73\u8861\u504f\u79fb");
      this.place = this.add(new BooleanSetting("Place", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setParent());
      this.place.setChinese("\u653e\u7f6e");
      this.placeRange = this.add(
         new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen())
            .setSuffix("m")
      );
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.placeDelay = this.add(
         new SliderSetting("PlaceDelay", 300, 0, 1000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen()).setSuffix("ms")
      );
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.autoSwap = this.add(
         new EnumSetting<>("AutoSwap", AutoCrystalPlusPlus.SwapMode.None, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen())
      );
      this.autoSwap.setChinese("\u81ea\u52a8\u5207\u6362");
      this.placeFirst = this.add(
         new EnumSetting<>("PlaceFirst", PlaceFirstMode.NONE, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen())
      );
      this.placeFirst.setChinese("\u653e\u7f6e\u4f18\u5148");
      this.afterBreak = this.add(new BooleanSetting("AfterBreak", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen()));
      this.afterBreak.setChinese("\u5f15\u7206\u540e\u653e\u7f6e");
      this.forcePlace = this.add(new BooleanSetting("ForcePlace", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen()));
      this.forcePlace.setChinese("\u5f3a\u5236\u653e\u7f6e");
      this.facePlace = this.add(
         new BooleanSetting("FacePlace", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen()).setParent()
      );
      this.facePlace.setChinese("\u6253\u8138\u653e\u7f6e");
      this.facePlaceHealth = this.add(
         new SliderSetting(
               "FacePlaceHealth",
               10.0,
               0.0,
               36.0,
               0.1,
               () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen() && this.facePlace.isOpen()
            )
            .setSuffix("hp")
      );
      this.facePlaceHealth.setChinese("\u6253\u8138\u8840\u91cf");
      this.facePlaceArmor = this.add(
         new SliderSetting(
               "FacePlaceArmor", 30, 0, 100, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.place.isOpen() && this.facePlace.isOpen()
            )
            .setSuffix("%")
      );
      this.facePlaceArmor.setChinese("\u6253\u8138\u8010\u4e45");
      this.breakSetting = this.add(new BooleanSetting("BreakSetting", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setParent());
      this.breakSetting.setChinese("\u5f15\u7206");
      this.breakRange = this.add(
         new SliderSetting("BreakRange", 4.0, 0.0, 6.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen())
            .setSuffix("m")
      );
      this.breakRange.setChinese("\u5f15\u7206\u8303\u56f4");
      this.breakDelay = this.add(
         new SliderSetting("BreakDelay", 300, 0, 1000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen())
            .setSuffix("ms")
      );
      this.breakDelay.setChinese("\u5f15\u7206\u5ef6\u8fdf");
      this.minAge = this.add(
         new SliderSetting("MinAge", 0, 0, 20, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen()).setSuffix("tick")
      );
      this.minAge.setChinese("\u6700\u5c0f\u5e74\u9f84");
      this.breakRemove = this.add(
         new BooleanSetting("BreakRemove", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen())
      );
      this.breakRemove.setChinese("\u79fb\u9664");
      this.onAdd = this.add(new BooleanSetting("OnAdd", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen()));
      this.onAdd.setChinese("\u751f\u6210\u65f6\u6253");
      this.resetCD = this.add(new BooleanSetting("ResetCD", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen()));
      this.resetCD.setChinese("\u91cd\u7f6e\u653b\u51fbCD");
      this.hitFactor = this.add(
         new SliderSetting("HitFactor", 0, 0, 10, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General && this.breakSetting.isOpen())
      );
      this.hitFactor.setChinese("\u591a\u5305\u653b\u51fb");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General));
      this.timing.setChinese("\u65f6\u673a");
      this.interactOnRender = this.add(new BooleanSetting("InteractOnRender", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General));
      this.interactOnRender.setChinese("\u6e32\u67d3\u65f6\u4ea4\u4e92");
      this.wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.0, 6.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General).setSuffix("m"));
      this.wallRange.setChinese("\u7a7f\u5899\u8303\u56f4");
      this.swingMode = this.add(new EnumSetting<>("SwingMode", SwingSide.All, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.General));
      this.swingMode.setChinese("\u6325\u624b");
      this.text = this.add(new ColorSetting("Text", new Color(-1), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render).injectBoolean(true));
      this.text.setChinese("\u6587\u5b57");
      this.mode = this.add(new EnumSetting<>("Mode", AutoCrystalPlusPlus.TargetESP.Fill, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.mode.setChinese("\u76ee\u6807\u6e32\u67d3");
      this.animationTime = this.add(new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.animationTime.setChinese("\u52a8\u753b\u65f6\u95f4");
      this.ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.ease.setChinese("\u7f13\u52a8");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.color.setChinese("\u76ee\u6807\u989c\u8272");
      this.outlineColor = this.add(
         new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render)
      );
      this.outlineColor.setChinese("\u76ee\u6807\u8f6e\u5ed3\u989c\u8272");
      this.hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.hitColor.setChinese("\u51fb\u4e2d\u989c\u8272");
      this.hitOutlineColor = this.add(
         new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render)
      );
      this.hitOutlineColor.setChinese("\u51fb\u4e2d\u8f6e\u5ed3\u989c\u8272");
      this.render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render));
      this.render.setChinese("\u6e32\u67d3");
      this.sync = this.add(new BooleanSetting("Sync", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue()));
      this.sync.setChinese("\u540c\u6b65");
      this.shrink = this.add(new BooleanSetting("Shrink", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue()));
      this.shrink.setChinese("\u6536\u7f29");
      this.box = this.add(
         new ColorSetting("Box", new Color(255, 255, 255, 255), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
            .injectBoolean(true)
      );
      this.box.setChinese("\u8fb9\u6846");
      this.lineWidth = this.add(
         new SliderSetting("LineWidth", 1.5, 0.01, 3.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
      );
      this.lineWidth.setChinese("\u7ebf\u5bbd");
      this.fill = this.add(
         new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
            .injectBoolean(true)
      );
      this.fill.setChinese("\u586b\u5145");
      this.sliderSpeed = this.add(
         new SliderSetting("SliderSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
      );
      this.sliderSpeed.setChinese("\u6ed1\u52a8\u901f\u5ea6");
      this.startFadeTime = this.add(
         new SliderSetting("StartFadeTime", 0.3, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
            .setSuffix("s")
      );
      this.startFadeTime.setChinese("\u5f00\u59cb\u6e10\u9690");
      this.fadeSpeed = this.add(
         new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Render && this.render.getValue())
      );
      this.fadeSpeed.setChinese("\u6e10\u9690\u901f\u5ea6");
      this.attackVecStep = this.add(new SliderSetting("AttackVecStep", 0.1, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.attackVecStep.setChinese("\u653b\u51fb\u5411\u91cf\u6b65\u957f");
      this.thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.thread.setChinese("\u591a\u7ebf\u7a0b");
      this.doCrystal = this.add(new BooleanSetting("DoCrystal", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.doCrystal.setChinese("\u8ba1\u7b97\u4e2d\u4ea4\u4e92");
      this.selfPredict = this.add(new SliderSetting("SelfPredict", 0, 0, 20, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc).setSuffix("ticks"));
      this.selfPredict.setChinese("\u81ea\u8eab\u9884\u6d4b");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 4, 0, 20, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc).setSuffix("ticks"));
      this.predictTicks.setChinese("\u9884\u6d4b");
      this.simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.simulation.setChinese("\u6a21\u62df");
      this.maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.maxMotionY.setChinese("\u6700\u5927Y\u901f\u5ea6");
      this.step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.step.setChinese("\u8de8\u6b65");
      this.doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.doubleStep.setChinese("\u53cc\u8de8\u6b65");
      this.jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.jump.setChinese("\u8df3\u8dc3");
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.inBlockPause.setChinese("\u65b9\u5757\u5185\u6682\u505c");
      this.terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.terrainIgnore.setChinese("\u5ffd\u7565\u5730\u5f62");
      this.raytraceMode = this.add(new EnumSetting<>("RaytraceMode", RaytraceMode.FAST, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Calc));
      this.raytraceMode.setChinese("\u5c04\u7ebf\u7cbe\u5ea6");
      this.basePlace = this.add(new BooleanSetting("BasePlace", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base));
      this.basePlace.setChinese("\u5e95\u5ea7\u653e\u7f6e");
      this.baseMin = this.add(new SliderSetting("BaseMin", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base).setSuffix("hp"));
      this.baseMin.setChinese("\u5e95\u5ea7\u6700\u5c0f");
      this.baseMax = this.add(new SliderSetting("BaseMax", 12.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base).setSuffix("hp"));
      this.baseMax.setChinese("\u5e95\u5ea7\u6700\u5927");
      this.overrideMax = this.add(
         new SliderSetting("OverrideMax", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base).setSuffix("hp")
      );
      this.overrideMax.setChinese("\u8986\u76d6\u4e0a\u9650");
      this.baseBalance = this.add(new BooleanSetting("BaseBalance", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base));
      this.baseBalance.setChinese("\u5e95\u5ea7\u5e73\u8861");
      this.onlyBelow = this.add(new BooleanSetting("OnlyBelow", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base));
      this.onlyBelow.setChinese("\u4ec5\u4e0b\u65b9");
      this.inventory = this.add(new BooleanSetting("Inventory", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base));
      this.detectMining.setChinese("\u68c0\u6d4b\u6316\u6398");
      this.delay = this.add(new SliderSetting("Delay", 3000, 0, 10000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base).setSuffix("ms"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.ignoreMine = this.add(new BooleanSetting("IgnoreMine", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.ignoreMine.setChinese("\u5ffd\u7565\u6316\u6398");
      this.constantProgress = this.add(
         new SliderSetting("ConstantProgress", 90.0, 0.0, 100.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.ignoreMine.isOpen())
            .setSuffix("%")
      );
      this.constantProgress.setChinese("\u8fdb\u5ea6");
      this.antiSurround = this.add(new BooleanSetting("AntiSurround", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.antiSurround.setChinese("\u53cd\u5305\u56f4");
      this.miningProgress = this.add(
         new SliderSetting("MiningProgress", 90.0, 0.0, 100.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.antiSurround.isOpen())
            .setSuffix("%")
      );
      this.miningProgress.setChinese("\u6316\u6398\u8fdb\u5ea6");
      this.antiSurroundMax = this.add(
         new SliderSetting("AntiSurroundMax", 5.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.antiSurround.isOpen())
            .setSuffix("dmg")
      );
      this.antiSurroundMax.setChinese("\u4f4e\u4e8e\u6b64\u4f24\u5bb3");
      this.slowPlace = this.add(new BooleanSetting("SlowPlace", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.slowPlace.setChinese("\u8d85\u65f6");
      this.slowDelay = this.add(
         new SliderSetting("SlowDelay", 600, 0, 2000, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.slowPlace.isOpen()).setSuffix("ms")
      );
      this.slowDelay.setChinese("\u8d85\u65f6\u5ef6\u8fdf");
      this.slowMinDamage = this.add(
         new SliderSetting("SlowMinDamage", 1.5, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.slowPlace.isOpen())
            .setSuffix("dmg")
      );
      this.slowMinDamage.setChinese("\u8d85\u65f6\u6700\u5c0f");
      this.rangeOverride = this.add(new BooleanSetting("RangeOverride", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.rangeOverride.setChinese("\u8fdc\u8ddd\u79bb\u6301\u7eed\u653e\u7f6e");
      this.rangeOverrideDistance = this.add(
         new SliderSetting(
               "RangeOverrideDistance", 4.0, 2.0, 8.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.rangeOverride.isOpen()
            )
            .setSuffix("m")
      );
      this.rangeOverrideDistance.setChinese("\u8fdc\u8ddd\u79bb\u9608\u503c");
      this.rangeOverrideDamage = this.add(
         new SliderSetting(
               "RangeOverrideDamage", 2.0, 0.0, 12.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.rangeOverride.isOpen()
            )
            .setSuffix("dmg")
      );
      this.rangeOverrideDamage.setChinese("\u8fdc\u8ddd\u79bb\u6700\u5c0f\u4f24\u5bb3");
      this.lethalOverride = this.add(new BooleanSetting("LethalOverride", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.lethalOverride.setChinese("\u65a9\u6740\u8986\u76d6");
      this.forceMaxHealth = this.add(
         new SliderSetting("ForceMaxHealth", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.lethalOverride.isOpen())
            .setSuffix("health")
      );
      this.forceMaxHealth.setChinese("\u4f4e\u4e8e\u8840\u91cf");
      this.forceMin = this.add(
         new SliderSetting("ForceMin", 1.5, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.lethalOverride.isOpen())
            .setSuffix("dmg")
      );
      this.forceMin.setChinese("\u5f3a\u5236\u6700\u5c0f");
      this.armorBreaker = this.add(new BooleanSetting("ArmorBreaker", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.armorBreaker.setChinese("\u7834\u7532");
      this.maxDurable = this.add(
         new SliderSetting("MaxDurable", 8, 0, 100, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.armorBreaker.isOpen()).setSuffix("%")
      );
      this.maxDurable.setChinese("\u6700\u5927\u8010\u4e45");
      this.armorBreakerDamage = this.add(
         new SliderSetting("ArmorBreakerDamage", 3.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.armorBreaker.isOpen())
            .setSuffix("dmg")
      );
      this.armorBreakerDamage.setChinese("\u7834\u7532\u6700\u5c0f");
      this.forceWeb = this.add(new BooleanSetting("ForceWeb", true, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc).setParent());
      this.forceWeb.setChinese("\u8718\u86db\u7f51\u91cd\u7f6e");
      this.airPlace = this.add(new BooleanSetting("AirPlace", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Base && this.basePlace.getValue()));
      this.airPlace.setChinese("\u7a7a\u653e");
      this.replace = this.add(new BooleanSetting("Replace", false, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc && this.forceWeb.isOpen()));
      this.replace.setChinese("\u66ff\u6362");
      this.hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc));
      this.hurtTime.setChinese("\u53d7\u4f24\u65f6\u95f4");
      this.waitHurt = this.add(new SliderSetting("WaitHurt", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc));
      this.waitHurt.setChinese("\u7b49\u5f85\u53d7\u4f24");
      this.syncTimeout = this.add(new SliderSetting("SyncTimeout", 500.0, 0.0, 2000.0, 10.0, () -> this.page.getValue() == AutoCrystalPlusPlus.Page.Misc));
      this.syncTimeout.setChinese("\u7b49\u5f85\u8d85\u65f6");
      this.syncTimer = new Timer();
      this.pendingPlaceTimer = new Timer();
      this.directionVec = null;
      this.currentFade = 0.0;
      this.lastBreakEntityId = -1;
      this.facePlacing = false;
      this.playerJoinTime = new ConcurrentHashMap<>();
      this.enableTime = 0L;
      this.attackedIds = new HashSet<>();
      this.antiPearlDefenses = new HashMap<>();
      this.antiPearlPendingId = -1;
      this.setChinese("\u81ea\u52a8\u6c34\u6676++");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoCrystalPlusPlus.CrystalRender());
   }

   @Override
   public String getInfo() {
      return this.displayTarget != null && this.lastDamage > 0.0F ? this.df.format(this.lastDamage) : null;
   }

   @Override
   public void onDisable() {
      this.crystalPos = null;
      this.tempPos = null;
      this.basePos = null;
      this.tempBasePos = null;
      this.breakCrystal = null;
      this.pendingBreakCrystal = null;
      this.syncPos = null;
      this.lastPlacedPos = null;
      this.pendingPlacePos = null;
      this.lastValidCrystalPosAt = 0L;
      this.lastBreakEntityId = -1;
      this.lastBreakPacketAt = 0L;
      this.directionVec = null;
      this.antiPearlPendingPos = null;
      this.antiPearlPendingUntil = 0L;
      this.antiPearlPendingId = -1;
      this.antiPearlDefenses.clear();
   }

   @Override
   public void onEnable() {
      this.crystalPos = null;
      this.tempPos = null;
      this.tempBreakCrystal = null;
      this.basePos = null;
      this.tempBasePos = null;
      this.breakCrystal = null;
      this.pendingBreakCrystal = null;
      this.syncPos = null;
      this.lastPlacedPos = null;
      this.pendingPlacePos = null;
      this.lastValidCrystalPosAt = 0L;
      this.lastBreakEntityId = -1;
      this.lastBreakPacketAt = 0L;
      this.directionVec = null;
      this.displayTarget = null;
      this.syncTimer.reset();
      this.pendingPlaceTimer.reset();
      this.renderRetryTimer.reset();
      this.lastBreakTimer.reset();
      this.breakTimer.reset();
      this.enableTime = System.currentTimeMillis();
      this.foodUseProtectionUntil = 0L;
      this.attackedIds.clear();
      this.antiPearlPendingPos = null;
      this.antiPearlPendingUntil = 0L;
      this.antiPearlPendingId = -1;
      this.antiPearlDefenses.clear();
   }

   public boolean isFoodUseProtected() {
      if (mc.player == null) {
         return false;
      } else {
         long now = System.currentTimeMillis();
         boolean holdingFood = mc.player.getMainHandStack().contains(DataComponentTypes.FOOD) || mc.player.getOffHandStack().contains(DataComponentTypes.FOOD);
         boolean foodIntent = holdingFood && mc.options.useKey.isPressed();
         boolean activelyUsingFood = mc.player.isUsingItem() && mc.player.getActiveItem().contains(DataComponentTypes.FOOD);
         if (foodIntent || activelyUsingFood) {
            this.foodUseProtectionUntil = now + 250L;
         }

         return foodIntent || activelyUsingFood || now < this.foodUseProtectionUntil;
      }
   }

   public void setRefillPause(boolean paused) {
      this.refillPause = paused;
   }

   public boolean isRefillPaused() {
      return this.refillPause;
   }

   public void onThread() {
      if (!this.isOff() && this.thread.getValue()) {
         this.updateCrystalPos();
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && (!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
         if (this.handleAntiPearl()) {
            return;
         }

         if (!this.thread.getValue()) {
            this.updateCrystalPos();
         }

         if (!this.shouldReturn()) {
            this.doInteract();
            BlockPos basePos = this.basePos;
            if (this.basePlace.getValue() && basePos != null && this.canPlaceBase(basePos)) {
               this.doPlace(basePos);
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck()) {
         if (!this.shouldReturn() && this.renderRetryTimer.passedMs(20.0)) {
            this.renderRetryTimer.reset();
            this.retryPendingBreak();
            this.retrySmoothPlace();
            if (this.interactOnRender.getValue()) {
               this.doInteract();
               BlockPos basePos = this.basePos;
               if (this.basePlace.getValue() && basePos != null && this.canPlaceBase(basePos)) {
                  this.doPlace(basePos);
               }
            }
         }

         if (this.displayTarget != null && !this.noPosTimer.passed(500L)) {
            this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget, this.mode.getValue());
         }
      }
   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity, AutoCrystalPlusPlus.TargetESP mode) {
      switch (mode) {
         case Box:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               true,
               true
            );
            break;
         case Fill:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               false,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, this.displayTarget, this.color.getValue());
      }
   }

   private void doInteract() {
      BlockPos crystalPos = this.crystalPos;
      this.retryPendingBreak();
      if (this.placeFirst.getValue() != PlaceFirstMode.NONE && crystalPos != null) {
         this.doPlace(crystalPos, this.rotate.getValue() && this.onPlace.getValue());
      }

      if (crystalPos != null) {
         this.doBreak(crystalPos);
      }

      EndCrystalEntity breakCrystal = this.breakCrystal;
      if (breakCrystal != null) {
         if (breakCrystal.isAlive() && !breakCrystal.isRemoved()) {
            this.doBreak(breakCrystal);
         } else if (this.breakCrystal == breakCrystal) {
            this.breakCrystal = null;
         }
      }

      if ((this.placeFirst.getValue() == PlaceFirstMode.NONE || this.placeFirst.getValue() == PlaceFirstMode.STRONG) && crystalPos != null) {
         this.doPlace(crystalPos, this.rotate.getValue() && this.onPlace.getValue());
      }
   }

   private void retryPendingBreak() {
      EndCrystalEntity pendingCrystal = this.pendingBreakCrystal;
      if (pendingCrystal != null) {
         if (pendingCrystal.isAlive() && !pendingCrystal.isRemoved()) {
            this.doBreak(pendingCrystal);
         } else {
            this.pendingBreakCrystal = null;
         }
      }
   }

   private void retrySmoothPlace() {
      BlockPos pos = this.crystalPos;
      if (pos != null && this.place.getValue()) {
         this.doPlace(pos, false);
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue()
         && this.shouldYawStep()
         && this.directionVec != null
         && this.displayTarget != null
         && !this.noPosTimer.passed(1000L)
         && !this.shouldReturn()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener(
      priority = -199
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled() && event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet && this.lastSlot != packet.getSelectedSlot()) {
         this.lastSlot = packet.getSelectedSlot();
         if (!AutoWebPlus.isSwapTransaction()) {
            this.switchTimer.reset();
         }
      }
   }

   public Vec3d getAttackVec(Vec3d feetPos) {
      return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), feetPos, this.breakRange.getValue(), 2.0, this.attackVecStep.getValue());
   }

   private Vec3d getPlaceVec(Vec3d feetPos) {
      return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), feetPos, this.placeRange.getValue(), 2.0, this.attackVecStep.getValue());
   }

   private boolean canBreakCrystal(EndCrystalEntity crystal) {
      if (crystal != null && crystal.isAlive()) {
         Vec3d attackVec = this.getAttackVec(crystal.getPos());
         return attackVec != null && (mc.player.canSee(crystal) || mc.player.getEyePos().distanceTo(attackVec) <= this.wallRange.getValue());
      } else {
         return false;
      }
   }

   private boolean isBaseSelfDamageAllowed(float selfDamage) {
      double configuredMax = this.baseMax.getValue();
      double effectiveMax = configuredMax <= 0.0 ? this.maxSelf.getValue() : configuredMax;
      return selfDamage <= effectiveMax;
   }

   private void updateCrystalPos() {
      if (this.calcDelay.passedMs(this.updateDelay.getValue())) {
         this.calcDelay.reset();
         BlockPos previousPos = this.crystalPos;
         float previousDamage = this.lastDamage;
         this.calcCrystalPos();
         CombatUtil.modifyPos = null;
         CombatUtil.modifyBlockState = null;
         long now = System.currentTimeMillis();
         if (this.tempPos != null) {
            this.lastValidCrystalPosAt = now;
         } else if (this.tempBasePos == null && previousPos != null && now - this.lastValidCrystalPosAt <= 150L && this.canRetainCrystalPos(previousPos)) {
            this.tempPos = previousPos;
            this.tempDamage = previousDamage;
         }

         this.basePos = this.tempBasePos;
         this.lastDamage = this.tempDamage;
         this.breakCrystal = this.tempBreakCrystal;
         this.crystalPos = this.tempPos;
      }
   }

   private boolean canRetainCrystalPos(BlockPos pos) {
      PlayerEntity target = this.displayTarget;
      if (target != null && target.isAlive() && !(mc.player.squaredDistanceTo(target) > this.targetRange.getValue() * this.targetRange.getValue())) {
         Vec3d attackVec = this.getPlaceVec(pos.toBottomCenterPos());
         return attackVec != null && !this.behindWall(pos, attackVec) && this.canPlaceCrystal(pos, true, false);
      } else {
         return false;
      }
   }

   private boolean shouldReturn() {
      if (PearlTracker.isCombatSwapLocked()) {
         this.lastBreakTimer.reset();
         return true;
      } else if (this.antiPearlPendingPos != null && System.currentTimeMillis() <= this.antiPearlPendingUntil) {
         this.lastBreakTimer.reset();
         return true;
      } else if (this.refillPause) {
         this.lastBreakTimer.reset();
         return true;
      } else if (this.selfInvincibility.getValue() && System.currentTimeMillis() - this.enableTime < this.invincTimeout.getValue()) {
         this.lastBreakTimer.reset();
         return true;
      } else if ((!this.eatingPause.getValue() || !this.isFoodUseProtected()) && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())) {
         if (this.preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            this.lastBreakTimer.reset();
            return true;
         } else if (this.pause.isPressed()) {
            this.lastBreakTimer.reset();
            return true;
         } else {
            return false;
         }
      } else {
         this.lastBreakTimer.reset();
         return true;
      }
   }

   private void calcCrystalPos() {
      if (!nullCheck()) {
         if (this.breakOnlyHasCrystal.getValue()
            && !mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL)
            && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)
            && !this.hasCrystal()) {
            this.tempPos = null;
            this.tempBreakCrystal = null;
            this.lastBreakTimer.reset();
         } else {
            boolean shouldReturn = this.shouldReturn();
            boolean needBasePlace = this.basePlace.getValue() && this.baseTimer.passedMs(this.delay.getValue()) && this.getBlock() != -1;
            this.tempBreakCrystal = null;
            this.breakDamage = 0.0F;
            this.tempPos = null;
            this.tempDamage = 0.0F;
            this.tempBasePos = null;
            float baseDamage = 0.0F;
            ArrayList<PlayerEntityPredict> list = new ArrayList<>();

            for (PlayerEntity target : CombatUtil.getEnemies(this.targetRange.getValueFloat())) {
               if (this.invincibilityTimeout.getValue()) {
                  UUID uuid = target.getUuid();
                  long now = System.currentTimeMillis();
                  this.playerJoinTime.putIfAbsent(uuid, now);
                  if (now - this.playerJoinTime.get(uuid) < this.invincTimeout.getValue()) {
                     continue;
                  }
               }

               if (target.hurtTime <= this.hurtTime.getValueInt()) {
                  list.add(
                     new PlayerEntityPredict(
                        target,
                        this.maxMotionY.getValue(),
                        this.predictTicks.getValueInt(),
                        this.simulation.getValueInt(),
                        this.step.getValue(),
                        this.doubleStep.getValue(),
                        this.jump.getValue(),
                        this.inBlockPause.getValue()
                     )
                  );
               }
            }

            PlayerEntityPredict self = new PlayerEntityPredict(
               mc.player,
               this.maxMotionY.getValue(),
               this.selfPredict.getValueInt(),
               this.simulation.getValueInt(),
               this.step.getValue(),
               this.doubleStep.getValue(),
               this.jump.getValue(),
               this.inBlockPause.getValue()
            );
            if (list.isEmpty()) {
               this.lastBreakTimer.reset();
            } else {
               for (Entity entity : Alien.THREAD.getEntities()) {
                  if (entity instanceof EndCrystalEntity crystal && entity.age >= this.minAge.getValueInt() && this.canBreakCrystal(crystal)) {
                     float selfDamage = this.calculateDamage(crystal.getPos(), self.player, self.predict);

                     for (PlayerEntityPredict pap : list) {
                        float damage = this.calculateDamage(crystal.getPos(), pap.player, pap.predict);
                        if (damage > this.breakDamage
                           && !(selfDamage > this.maxSelf.getValue())
                           && (
                              !(this.reserve.getValue() > 0.0)
                                 || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                           )
                           && (
                              !(damage < EntityUtil.getHealth(pap.player))
                                 || !(damage < this.getDamage(pap.player))
                                    && (
                                       !this.balance.getValue()
                                          || (
                                             this.getDamage(pap.player) == this.forceMin.getValue()
                                                ? !(damage < selfDamage - 2.5)
                                                : !(damage < selfDamage + this.balanceOffset.getValue())
                                          )
                                    )
                           )) {
                           this.breakDamage = damage;
                           this.tempBreakCrystal = crystal;
                           this.displayTarget = pap.player;
                        }
                     }
                  }
               }

               if (this.doCrystal.getValue() && !this.thread.getValue() && this.tempBreakCrystal != null && !shouldReturn) {
                  this.doBreak(this.tempBreakCrystal);
                  this.tempBreakCrystal = null;
               }

               for (PlayerEntityPredict papx : list) {
                  BlockPos feetPos = BlockPos.ofFloored(papx.predict.getPos());
                  BlockPos crystalPos = feetPos.up();
                  if ((BlockUtil.getBlock(feetPos) == Blocks.OBSIDIAN || BlockUtil.getBlock(feetPos) == Blocks.BEDROCK)
                     && this.canPlaceCrystal(crystalPos, true, false)
                     && this.canTouch(feetPos)) {
                     Vec3d attackVec = this.getPlaceVec(crystalPos.toBottomCenterPos());
                     if (attackVec != null && !this.behindWall(crystalPos, attackVec)) {
                        float selfDamage = this.calculateDamage(crystalPos, self.player, self.predict);
                        float damage = this.calculateDamage(crystalPos, papx.player, papx.predict);
                        float adjustedDamage = damage + 2.0F;
                        if (adjustedDamage > this.tempDamage
                           && !(selfDamage > this.maxSelf.getValue())
                           && (
                              this.reserve.getValue() <= 0.0
                                 || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                           )
                           && (
                              damage >= EntityUtil.getHealth(papx.player)
                                 || damage >= this.getDamage(papx.player)
                                 || this.balance.getValue()
                                    && (
                                       this.getDamage(papx.player) == this.forceMin.getValue()
                                          ? damage >= selfDamage - 2.5
                                          : damage >= selfDamage + this.balanceOffset.getValue()
                                    )
                           )) {
                           this.displayTarget = papx.player;
                           this.tempPos = crystalPos;
                           this.tempBasePos = null;
                           this.tempDamage = adjustedDamage;
                        }
                     }
                  }
               }

               this.facePlacing = false;
               if (this.facePlace.getValue() && this.place.getValue()) {
                  for (PlayerEntityPredict papxx : list) {
                     if (EntityUtil.getHealth(papxx.player) <= this.facePlaceHealth.getValue()
                        && EntityUtil.isArmorLow(papxx.player, this.facePlaceArmor.getValueInt())) {
                        BlockPos feetPos = BlockPos.ofFloored(papxx.predict.getPos());
                        BlockPos facePos = feetPos.up(2);
                        if (this.canPlaceCrystal(facePos, true, false) && this.canTouch(facePos.down())) {
                           Vec3d attackVec = this.getPlaceVec(facePos.toBottomCenterPos());
                           if (attackVec != null && !this.behindWall(facePos, attackVec)) {
                              float selfDamage = this.calculateDamage(facePos, self.player, self.predict);
                              float damage = this.calculateDamage(facePos, papxx.player, papxx.predict);
                              float adjustedDamage = damage + 3.0F;
                              if (adjustedDamage > this.tempDamage
                                 && !(selfDamage > this.maxSelf.getValue())
                                 && (
                                    this.reserve.getValue() <= 0.0
                                       || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                 )
                                 && (
                                    damage >= EntityUtil.getHealth(papxx.player)
                                       || damage >= this.getDamage(papxx.player)
                                       || this.balance.getValue()
                                          && (
                                             this.getDamage(papxx.player) == this.forceMin.getValue()
                                                ? damage >= selfDamage - 2.5
                                                : damage >= selfDamage + this.balanceOffset.getValue()
                                          )
                                 )) {
                                 this.displayTarget = papxx.player;
                                 this.tempPos = facePos;
                                 this.tempBasePos = null;
                                 this.tempDamage = adjustedDamage;
                                 this.facePlacing = true;
                              }
                           }
                        }
                     }
                  }
               }

               for (BlockPos pos : BlockUtil.getSphere(this.placeRange.getValueFloat())) {
                  boolean base = false;
                  CombatUtil.modifyPos = null;
                  CombatUtil.modifyBlockState = null;
                  if (needBasePlace && this.canPlaceBase(pos.down())) {
                     CombatUtil.modifyPos = pos.down();
                     CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                     base = true;
                  }

                  if (!base || !Alien.BREAK.isMining(pos.down()) || !this.detectMining.getValue()) {
                     Vec3d attackVec = this.getPlaceVec(pos.toBottomCenterPos());
                     boolean validCrystalSpace = base ? this.canPlaceCrystalWithBase(pos, true, false) : this.canPlaceCrystal(pos, true, false);
                     boolean canReachBase = base || this.canTouch(pos.down());
                     if (attackVec != null && !this.behindWall(pos, attackVec) && canReachBase && validCrystalSpace) {
                        float selfDamage = base
                           ? this.calculateBaseDamage(pos, self.player, self.predict)
                           : this.calculateDamage(pos, self.player, self.predict);

                        for (PlayerEntityPredict papxxx : list) {
                           if (!base || !this.onlyBelow.getValue() || pos.down().getY() <= Math.floor(papxxx.predict.getY())) {
                              float damage = base
                                 ? this.calculateBaseDamage(pos, papxxx.player, papxxx.predict)
                                 : this.calculateDamage(pos, papxxx.player, papxxx.predict);
                              if (base) {
                                 if (this.tempDamage <= this.overrideMax.getValue()
                                    && damage > this.tempDamage
                                    && damage > baseDamage
                                    && this.isBaseSelfDamageAllowed(selfDamage)
                                    && (
                                       !(this.reserve.getValue() > 0.0)
                                          || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                    )
                                    && (
                                       !(damage < EntityUtil.getHealth(papxxx.player))
                                          || !(damage < this.baseMin.getValue()) && (!this.baseBalance.getValue() || !(damage < selfDamage))
                                    )) {
                                    this.displayTarget = papxxx.player;
                                    baseDamage = damage;
                                    this.tempBasePos = pos.down();
                                    this.tempPos = null;
                                 }
                              } else if (damage > this.tempDamage
                                 && (damage >= baseDamage || this.tempDamage > this.overrideMax.getValue())
                                 && !(selfDamage > this.maxSelf.getValue())
                                 && (
                                    !(this.reserve.getValue() > 0.0)
                                       || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                 )
                                 && (
                                    !(damage < EntityUtil.getHealth(papxxx.player))
                                       || !(damage < this.getDamage(papxxx.player))
                                          && (
                                             !this.balance.getValue()
                                                || (
                                                   this.getDamage(papxxx.player) == this.forceMin.getValue()
                                                      ? !(damage < selfDamage - 2.5)
                                                      : !(damage < selfDamage + this.balanceOffset.getValue())
                                                )
                                          )
                                 )) {
                                 this.displayTarget = papxxx.player;
                                 this.tempPos = pos;
                                 this.tempBasePos = null;
                                 this.tempDamage = damage;
                              }
                           }
                        }
                     }
                  }
               }

               CombatUtil.modifyPos = null;
               CombatUtil.modifyBlockState = null;
               if (this.antiSurround.getValue()
                  && PacketMine.getBreakPos() != null
                  && PacketMine.progress >= this.miningProgress.getValueFloat()
                  && !BlockUtil.hasEntity(PacketMine.getBreakPos(), false)
                  && this.tempDamage <= this.antiSurroundMax.getValueFloat()) {
                  for (PlayerEntityPredict papxxxx : list) {
                     BlockPos pos = new BlockPosX(papxxxx.player.getPos().add(0.0, 0.5, 0.0));
                     if (!BlockUtil.canCollide(papxxxx.player, new Box(pos))) {
                        for (Direction i : Direction.values()) {
                           if (i != Direction.DOWN && i != Direction.UP) {
                              BlockPos offsetPos = pos.offset(i);
                              if (offsetPos.equals(PacketMine.getBreakPos())) {
                                 for (Direction direction : Direction.values()) {
                                    if (direction != Direction.DOWN
                                       && direction != Direction.UP
                                       && this.canPlaceCrystal(offsetPos.offset(direction), false, false)) {
                                       float selfDamage = this.calculateDamage(offsetPos.offset(direction), self.player, self.predict);
                                       if (selfDamage < this.maxSelf.getValue()
                                          && (
                                             !(this.reserve.getValue() > 0.0)
                                                || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                          )) {
                                          this.tempPos = offsetPos.offset(direction);
                                          if (this.doCrystal.getValue() && !this.thread.getValue() && this.tempPos != null && !shouldReturn) {
                                             this.doCrystal(this.tempPos);
                                          }

                                          return;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (this.doCrystal.getValue() && !this.thread.getValue() && this.tempPos != null && !shouldReturn) {
               this.doCrystal(this.tempPos);
            }
         }
      }
   }

   @EventListener
   private void onEntity(EntitySpawnedEvent event) {
      if (event.getEntity() instanceof EndCrystalEntity crystal) {
         BlockPos crystalBlockPos = crystal.getBlockPos();
         if (this.antiPearlPendingPos != null && this.antiPearlPendingPos.equals(crystalBlockPos) && System.currentTimeMillis() <= this.antiPearlPendingUntil) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            AutoCrystalPlusPlus.AntiPearlDefense defense = this.antiPearlDefenses
               .computeIfAbsent(this.antiPearlPendingId, id -> new AutoCrystalPlusPlus.AntiPearlDefense());
            defense.attempts++;
            defense.retryAt = System.currentTimeMillis() + this.antiPearlRetryDelay.getValueInt();
            this.antiPearlPendingPos = null;
            this.antiPearlPendingUntil = 0L;
            this.antiPearlPendingId = -1;
            this.pendingPlacePos = null;
            return;
         }

         BlockPos pending = this.pendingPlacePos;
         if (pending != null && pending.equals(crystalBlockPos)) {
            this.pendingPlacePos = null;
         }

         if (this.onAdd.getValue() && crystalBlockPos.equals(this.lastPlacedPos)) {
            this.pendingBreakCrystal = crystal;
            this.doBreak(crystal);
         }
      }
   }

   private boolean handleAntiPearl() {
      if (this.antiPearl.getValue() && !this.refillPause && !PearlTracker.isCombatSwapLocked()) {
         if (this.eatingPause.getValue() && this.isFoodUseProtected()) {
            return false;
         } else {
            long now = System.currentTimeMillis();
            if (this.antiPearlPendingPos != null) {
               if (now <= this.antiPearlPendingUntil) {
                  return true;
               }

               AutoCrystalPlusPlus.AntiPearlDefense defense = this.antiPearlDefenses
                  .computeIfAbsent(this.antiPearlPendingId, id -> new AutoCrystalPlusPlus.AntiPearlDefense());
               defense.retryAt = now + this.antiPearlRetryDelay.getValueInt();
               this.antiPearlPendingPos = null;
               this.antiPearlPendingUntil = 0L;
               this.antiPearlPendingId = -1;
            }

            this.antiPearlDefenses.keySet().removeIf(id -> mc.world.getEntityById(id) == null);
            EnderPearlEntity threat = null;
            PearlTrajectoryUtil.Prediction threatPrediction = null;
            double landingRangeSq = this.antiPearlLandingRange.getValue() * this.antiPearlLandingRange.getValue();

            for (EnderPearlEntity pearl : mc.world.getEntitiesByClass(EnderPearlEntity.class, mc.player.getBoundingBox().expand(96.0), Entity::isAlive)) {
               AutoCrystalPlusPlus.AntiPearlDefense defense = this.antiPearlDefenses.get(pearl.getId());
               if (defense == null || defense.attempts < this.antiPearlAttempts.getValueInt() && now >= defense.retryAt) {
                  PlayerEntity owner = PearlTrajectoryUtil.resolveOwner(pearl);
                  if (owner != null && owner != mc.player && !Alien.FRIEND.isFriend(owner) && !(owner.squaredDistanceTo(mc.player) < 225.0)) {
                     PearlTrajectoryUtil.Prediction prediction = PearlTrajectoryUtil.predict(pearl);
                     if (prediction != null && prediction.collided()) {
                        double dx = prediction.landingPos().x - mc.player.getX();
                        double dz = prediction.landingPos().z - mc.player.getZ();
                        if (!(dx * dx + dz * dz > landingRangeSq) && (threat == null || prediction.ticks() < threatPrediction.ticks())) {
                           threat = pearl;
                           threatPrediction = prediction;
                        }
                     }
                  }
               }
            }

            if (threat != null && threatPrediction != null) {
               EndCrystalEntity existingCrystal = this.findAntiPearlExistingCrystal(threatPrediction);
               if (existingCrystal != null) {
                  mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(existingCrystal, mc.player.isSneaking()));
                  mc.player.resetLastAttackedTicks();
                  EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                  AutoCrystalPlusPlus.AntiPearlDefense defense = this.antiPearlDefenses
                     .computeIfAbsent(threat.getId(), id -> new AutoCrystalPlusPlus.AntiPearlDefense());
                  defense.attempts++;
                  defense.retryAt = now + this.antiPearlRetryDelay.getValueInt();
                  return true;
               } else {
                  BlockPos defensePos = this.findAntiPearlCrystalPos(threatPrediction);
                  if (defensePos != null && this.forceAntiPearlPlace(defensePos)) {
                     this.antiPearlPendingPos = defensePos.toImmutable();
                     this.antiPearlPendingUntil = now + Math.max(150L, Math.min(300L, this.getAntiPearlNetworkTicks() * 50L + 50L));
                     this.antiPearlPendingId = threat.getId();
                     return true;
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private EndCrystalEntity findAntiPearlExistingCrystal(PearlTrajectoryUtil.Prediction prediction) {
      EndCrystalEntity best = null;
      double bestScore = Double.MAX_VALUE;
      double maxBlastDistanceSq = this.antiPearlBlastRange.getValue() * this.antiPearlBlastRange.getValue();
      int detonationTick = Math.max(1, this.getAntiPearlNetworkTicks() / 2);
      int lastTick = Math.min(prediction.path().size() - 1, detonationTick + 2);

      for (EndCrystalEntity crystal : mc.world
         .getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(this.breakRange.getValue()), Entity::isAlive)) {
         if (this.canBreakCrystal(crystal)) {
            for (int i = 1; i <= lastTick; i++) {
               Vec3d point = prediction.path().get(i);
               double distanceSq = crystal.getPos().squaredDistanceTo(point);
               if (!(distanceSq > maxBlastDistanceSq)) {
                  double score = this.getAntiPearlPushScore(crystal.getPos(), point, distanceSq, i, detonationTick);
                  if (score < bestScore) {
                     bestScore = score;
                     best = crystal;
                  }
               }
            }
         }
      }

      return best;
   }

   private BlockPos findAntiPearlCrystalPos(PearlTrajectoryUtil.Prediction prediction) {
      BlockPos bestPos = null;
      double bestScore = Double.MAX_VALUE;
      double maxBlastDistanceSq = this.antiPearlBlastRange.getValue() * this.antiPearlBlastRange.getValue();
      int detonationTick = Math.max(this.antiPearlLeadTicks.getValueInt(), this.getAntiPearlNetworkTicks());
      int firstTick = Math.max(1, detonationTick - 1);
      int lastTick = Math.min(prediction.path().size() - 1, detonationTick + 3);

      for (BlockPos pos : BlockUtil.getSphere(this.placeRange.getValueFloat())) {
         if (this.canPlaceCrystal(pos, false, true)) {
            Vec3d explosionPos = Vec3d.ofBottomCenter(pos);

            for (int i = firstTick; i <= lastTick; i++) {
               Vec3d point = prediction.path().get(i);
               double distanceSq = explosionPos.squaredDistanceTo(point);
               if (!(distanceSq > maxBlastDistanceSq)) {
                  double score = this.getAntiPearlPushScore(explosionPos, point, distanceSq, i, detonationTick);
                  if (score < bestScore) {
                     bestScore = score;
                     bestPos = pos.toImmutable();
                  }
               }
            }
         }
      }

      return bestPos;
   }

   private double getAntiPearlPushScore(Vec3d explosionPos, Vec3d pearlPos, double distanceSq, int tick, int detonationTick) {
      Vec3d away = new Vec3d(pearlPos.x - mc.player.getX(), 0.0, pearlPos.z - mc.player.getZ());
      Vec3d push = new Vec3d(pearlPos.x - explosionPos.x, 0.0, pearlPos.z - explosionPos.z);
      double alignment = away.lengthSquared() > 1.0E-4 && push.lengthSquared() > 1.0E-4 ? away.normalize().dotProduct(push.normalize()) : 0.0;
      return alignment < -0.15 ? Double.MAX_VALUE : distanceSq + (1.0 - alignment) * 4.0 + Math.abs(tick - detonationTick) * 0.2;
   }

   private int getAntiPearlNetworkTicks() {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         int latency = entry == null ? 100 : entry.getLatency();
         return Math.max(2, Math.min(10, (int)Math.ceil(latency * 1.5 / 50.0)));
      } else {
         return 4;
      }
   }

   private boolean forceAntiPearlPlace(BlockPos pos) {
      if (!this.canPlaceCrystal(pos, false, true)) {
         return false;
      } else if (!mc.player.getOffHandStack().isOf(Items.END_CRYSTAL) && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
         int crystalSlot = InventoryUtil.findItemInventorySlotFromZero(Items.END_CRYSTAL);
         if (crystalSlot == -1) {
            return false;
         } else {
            int selectedSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.inventorySwap(crystalSlot, selectedSlot);

            try {
               this.placeCrystal(pos);
            } finally {
               InventoryUtil.inventorySwap(crystalSlot, selectedSlot);
               EntityUtil.syncInventory();
            }

            return true;
         }
      } else {
         this.placeCrystal(pos);
         return true;
      }
   }

   @EventListener
   private void onEntityRemoved(RemoveEntityEvent event) {
      if (event.getEntity() instanceof EndCrystalEntity crystal) {
         this.attackedIds.remove(crystal.getId());
         EndCrystalEntity pendingCrystal = this.pendingBreakCrystal;
         if (pendingCrystal != null && pendingCrystal.getId() == crystal.getId()) {
            this.pendingBreakCrystal = null;
         }

         if (this.lastBreakEntityId == crystal.getId()) {
            this.lastBreakEntityId = -1;
         }

         EndCrystalEntity selectedCrystal = this.breakCrystal;
         if (selectedCrystal != null && selectedCrystal.getId() == crystal.getId()) {
            this.breakCrystal = null;
         }

         if (this.lastPlacedPos != null && this.lastPlacedPos.equals(crystal.getBlockPos())) {
            this.pendingPlacePos = null;
         }
      }
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      BlockPos obsPos = pos.down();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN)
         && BlockUtil.getClickSideStrict(obsPos) != null
         && this.canPlaceCrystalSpace(pos, ignoreCrystal, ignoreItem);
   }

   private boolean canPlaceCrystalWithBase(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      return this.canPlaceCrystalSpace(pos, ignoreCrystal, ignoreItem);
   }

   private boolean canPlaceCrystalSpace(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      BlockPos boost2 = pos.up();
      return this.noEntityBlockCrystal(pos, ignoreCrystal, ignoreItem)
         && this.noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
         && (mc.world.isAir(pos) || BlockUtil.getBlock(pos) == Blocks.COBWEB || BlockUtil.hasCrystal(pos) && BlockUtil.getBlock(pos) == Blocks.FIRE)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2) || BlockUtil.getBlock(boost2) == Blocks.COBWEB);
   }

   private boolean canPlaceBase(BlockPos pos) {
      if (pos != null && BlockUtil.canReplace(pos) && !BlockUtil.hasEntity(pos, false)) {
         return !this.airPlace.getValue() && !BlockUtil.allowAirPlace()
            ? BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null
            : BlockUtil.getClickSide(pos) != null;
      } else {
         return false;
      }
   }

   private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity.isAlive()
            && (!(entity instanceof ItemEntity) || !ignoreItem)
            && (
               !(entity instanceof EndCrystalEntity)
                  || !ignoreCrystal
                  || this.getAttackVec(entity.getPos()) == null
                  || !mc.player.canSee(entity) && !(mc.player.getEyePos().distanceTo(entity.getPos()) <= this.wallRange.getValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public boolean behindWall(BlockPos pos, Vec3d attackVec) {
      Vec3d[] samplePoints = this.getRaytraceSamplePoints(pos);

      for (Vec3d crystalEyePos : samplePoints) {
         HitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), crystalEyePos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (result == null || result.getType() == Type.MISS) {
            return false;
         }

         if (mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue()) {
            return true;
         }
      }

      return mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue();
   }

   private Vec3d[] getRaytraceSamplePoints(BlockPos pos) {
      switch ((RaytraceMode)this.raytraceMode.getValue()) {
         case FAST:
         default:
            return new Vec3d[]{new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5)};
         case X2:
            return new Vec3d[]{
               new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5)
            };
         case X4:
            return new Vec3d[]{
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.0, pos.getZ() + 0.3),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.0, pos.getZ() + 0.7),
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.7, pos.getZ() + 0.7),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.7, pos.getZ() + 0.3)
            };
         case X8:
            return new Vec3d[]{
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.0, pos.getZ() + 0.3),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.0, pos.getZ() + 0.3),
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.0, pos.getZ() + 0.7),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.0, pos.getZ() + 0.7),
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.7, pos.getZ() + 0.3),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.7, pos.getZ() + 0.3),
               new Vec3d(pos.getX() + 0.3, pos.getY() + 1.7, pos.getZ() + 0.7),
               new Vec3d(pos.getX() + 0.7, pos.getY() + 1.7, pos.getZ() + 0.7)
            };
      }
   }

   private boolean canTouch(BlockPos pos) {
      Direction side = BlockUtil.getClickSideStrict(pos);
      return side != null && this.getFaceHitVec(pos, side).distanceTo(mc.player.getEyePos()) <= this.placeRange.getValue();
   }

   private Vec3d getFaceHitVec(BlockPos pos, Direction side) {
      Vec3d eye = mc.player.getEyePos();
      double minX = pos.getX() + 0.001;
      double minY = pos.getY() + 0.001;
      double minZ = pos.getZ() + 0.001;
      double maxX = pos.getX() + 0.999;
      double maxY = pos.getY() + 0.999;
      double maxZ = pos.getZ() + 0.999;
      double x = Math.max(minX, Math.min(maxX, eye.x));
      double y = Math.max(minY, Math.min(maxY, eye.y));
      double z = Math.max(minZ, Math.min(maxZ, eye.z));

      return switch (side) {
         case DOWN -> new Vec3d(x, pos.getY(), z);
         case UP -> new Vec3d(x, pos.getY() + 1.0, z);
         case NORTH -> new Vec3d(x, y, pos.getZ());
         case SOUTH -> new Vec3d(x, y, pos.getZ() + 1.0);
         case WEST -> new Vec3d(pos.getX(), y, z);
         case EAST -> new Vec3d(pos.getX() + 1.0, y, z);
         default -> throw new MatchException(null, null);
      };
   }

   private void doCrystal(BlockPos pos) {
      if (this.placeFirst.getValue() != PlaceFirstMode.NONE) {
         this.doPlace(pos, this.rotate.getValue() && this.onPlace.getValue());
      }

      this.doBreak(pos);
      if (this.placeFirst.getValue() == PlaceFirstMode.NONE || this.placeFirst.getValue() == PlaceFirstMode.STRONG) {
         this.doPlace(pos, this.rotate.getValue() && this.onPlace.getValue());
      }
   }

   private void doPlace(BlockPos pos) {
      if (this.baseTimer.passed((long)this.delay.getValue()) && this.canPlaceBase(pos) && (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos))) {
         int block = this.getBlock();
         if (block != -1) {
            int old = mc.player.getInventory().selectedSlot;
            this.baseSwap(block);
            if (!this.airPlace.getValue() && !BlockUtil.allowAirPlace()) {
               BlockUtil.placeBlock(pos, this.rotate.getValue(), AntiCheat.INSTANCE.packetPlace.getValue());
            } else {
               BlockUtil.airPlace(pos, this.rotate.getValue(), Hand.MAIN_HAND, AntiCheat.INSTANCE.packetPlace.getValue());
            }

            if (this.inventory.getValue()) {
               this.baseSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.baseSwap(old);
            }

            this.resetCadenceTimer(this.baseTimer, this.delay.getValue());
         }
      }
   }

   public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
      return this.calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
   }

   public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
      if (this.ignoreMine.getValue()
         && PacketMine.getBreakPos() != null
         && mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()
         && PacketMine.progress >= this.constantProgress.getValue() / 100.0) {
         CombatUtil.modifyPos = PacketMine.getBreakPos();
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
      }

      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float damage = ExplosionUtil.calculateDamage(pos, player, predict, 6.0F);
      CombatUtil.modifyPos = null;
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   public float calculateBaseDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float damage = DamageUtils.overridingExplosionDamage(
         player, predict, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), 12.0F, pos.down(), Blocks.OBSIDIAN.getDefaultState()
      );
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   private double getDamage(PlayerEntity target) {
      boolean miningObsidian = PacketMine.INSTANCE.obsidian.isPressed();
      if (!miningObsidian && this.rangeOverride.getValue() && mc.player.distanceTo(target) >= this.rangeOverrideDistance.getValue()) {
         return Math.min(this.minDamage.getValue(), this.rangeOverrideDamage.getValue());
      } else if (!miningObsidian && this.slowPlace.getValue() && this.lastBreakTimer.passed((long)this.slowDelay.getValue())) {
         return this.slowMinDamage.getValue();
      } else if (this.lethalOverride.getValue() && EntityUtil.getHealth(target) <= this.forceMaxHealth.getValue() && !miningObsidian) {
         return this.forceMin.getValue();
      } else {
         if (this.armorBreaker.getValue()) {
            for (ItemStack armor : target.getInventory().armor) {
               if (!armor.isEmpty() && !(EntityUtil.getDamagePercent(armor) > this.maxDurable.getValue())) {
                  return this.armorBreakerDamage.getValue();
               }
            }
         }

         return this.minDamage.getValue();
      }
   }

   private boolean shouldYawStep() {
      return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying()))
         && this.yawStep.getValue()
         && !Velocity.INSTANCE.noRotation();
   }

   public boolean hasCrystal() {
      return this.autoSwap.getValue() != AutoCrystalPlusPlus.SwapMode.None && this.getCrystal() != -1;
   }

   private void doBreak(EndCrystalEntity entity) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (this.refillPause) {
            return;
         }

         if (this.eatingPause.getValue() && this.isFoodUseProtected()) {
            return;
         }

         if (this.canBreakCrystal(entity)
            && (
               this.displayTarget == null || this.displayTarget.hurtTime <= this.waitHurt.getValueInt() || this.syncTimer.passedMs(this.syncTimeout.getValue())
            )
            && entity.age >= this.minAge.getValueInt()) {
            if (this.shouldYawStep() || this.breakTimer.passed((long)this.breakDelay.getValue())) {
               long now = System.currentTimeMillis();
               long minimumPacketInterval = entity.getId() == this.lastBreakEntityId ? 100L : 20L;
               if (now - this.lastBreakPacketAt < minimumPacketInterval) {
                  return;
               }

               if (this.rotate.getValue() && this.onBreak.getValue()) {
                  Vec3d attackVec = this.getAttackVec(entity.getPos());
                  Vec3d targetVec = attackVec == null ? entity.getPos() : attackVec;
                  if (this.shouldYawStep()) {
                     this.directionVec = targetVec;
                  } else {
                     Alien.ROTATION.lookAt(targetVec);
                  }
               }

               if (!this.breakTimer.passed((long)this.breakDelay.getValue())) {
                  if (this.forcePlace.getValue() && this.crystalPos != null) {
                     this.doPlace(this.crystalPos, false);
                  }
               } else {
                  this.animation.to = 1.0;
                  this.animation.from = 1.0;
                  this.resetCadenceTimer(this.breakTimer, this.breakDelay.getValue());
                  this.lastBreakEntityId = entity.getId();
                  this.lastBreakPacketAt = now;
                  this.lastBreakTimer.reset();
                  this.syncTimer.reset();
                  this.pendingBreakCrystal = entity;
                  this.syncPos = entity.getBlockPos();
                  mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                  if (this.hitFactor.getValueInt() > 0 && !this.attackedIds.contains(entity.getId())) {
                     this.attackedIds.add(entity.getId());

                     for (int i = 1; i <= this.hitFactor.getValueInt(); i++) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                     }
                  }

                  if (this.resetCD.getValue()) {
                     mc.player.resetLastAttackedTicks();
                  }

                  EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                  if (this.breakRemove.getValue()) {
                     mc.world.removeEntity(entity.getId(), RemovalReason.KILLED);
                  }

                  BlockPos crystalPos = this.crystalPos;
                  if (crystalPos != null
                     && this.displayTarget != null
                     && this.lastDamage >= this.getDamage(this.displayTarget)
                     && this.afterBreak.getValue()
                     && (
                        !this.rotate.getValue()
                           || !this.shouldYawStep()
                           || !this.checkFov.getValue()
                           || Alien.ROTATION.inFov(entity.getPos(), this.fov.getValueFloat())
                     )) {
                     this.doPlace(crystalPos, false, true);
                  }

                  if (this.forceWeb.getValue() && AutoWebPlus.INSTANCE.isOn()) {
                     AutoWebPlus.force = true;
                  }

                  if (this.rotate.getValue() && !this.shouldYawStep()) {
                     Alien.ROTATION.snapBack();
                  }
               }
            } else if (this.forcePlace.getValue() && this.crystalPos != null) {
               this.doPlace(this.crystalPos, false);
            }
         }
      }
   }

   private void doBreak(BlockPos pos) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (!this.refillPause) {
            if (!this.eatingPause.getValue() || !this.isFoodUseProtected()) {
               Box crystalBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
               EndCrystalEntity closestCrystal = null;
               double closestDistance = Double.MAX_VALUE;

               for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, crystalBox, Entity::isAlive)) {
                  if (crystal.age >= this.minAge.getValueInt() && this.canBreakCrystal(crystal)) {
                     double distance = mc.player.getEyePos().squaredDistanceTo(crystal.getPos());
                     if (distance < closestDistance) {
                        closestCrystal = crystal;
                        closestDistance = distance;
                     }
                  }
               }

               if (closestCrystal != null) {
                  this.doBreak(closestCrystal);
               } else if (this.forcePlace.getValue() && this.crystalPos != null) {
                  this.doPlace(this.crystalPos, false);
               }
            }
         }
      }
   }

   private void doPlace(BlockPos pos, boolean rotate) {
      this.doPlace(pos, rotate, false);
   }

   private void doPlace(BlockPos pos, boolean rotate, boolean ignoreCrystal) {
      this.noPosTimer.reset();
      if (this.place.getValue()) {
         if (this.refillPause) {
            return;
         }

         if (this.eatingPause.getValue() && this.isFoodUseProtected()) {
            return;
         }

         if (!this.canPlaceCrystal(pos, ignoreCrystal, false)) {
            return;
         }

         if (this.isPlacementPending(pos)) {
            return;
         }

         if ((
               mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL)
                  || mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)
                  || this.hasCrystal()
            )
            && this.placeTimer.passed((long)this.placeDelay.getValue())) {
            if (rotate) {
               BlockPos obsPos = pos.down();
               Direction facing = BlockUtil.getClickSide(obsPos);
               if (facing == null) {
                  return;
               }

               Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
               if (facing != Direction.UP && facing != Direction.DOWN) {
                  vec = vec.add(0.0, 0.45, 0.0);
               }

               if (!this.faceVector(vec)) {
                  return;
               }
            }

            if (!mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)) {
               int old = mc.player.getInventory().selectedSlot;
               int crystal = this.getCrystal();
               if (crystal == -1) {
                  return;
               }

               this.doSwap(crystal);
               this.placeCrystal(pos);
               if (this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Silent) {
                  this.doSwap(old);
               } else if (this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Inventory) {
                  this.doSwap(crystal);
                  EntityUtil.syncInventory();
               }
            } else {
               this.placeCrystal(pos);
            }

            if (rotate && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Silent || this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Normal) {
         InventoryUtil.switchToSlot(slot);
      } else if (this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Inventory) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      }
   }

   private void baseSwap(int slot) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(slot);
      } else {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private int getCrystal() {
      if (this.autoSwap.getValue() != AutoCrystalPlusPlus.SwapMode.Silent && this.autoSwap.getValue() != AutoCrystalPlusPlus.SwapMode.Normal) {
         return this.autoSwap.getValue() == AutoCrystalPlusPlus.SwapMode.Inventory ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : -1;
      } else {
         return InventoryUtil.findItem(Items.END_CRYSTAL);
      }
   }

   private void placeCrystal(BlockPos pos) {
      boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
      BlockPos obsPos = pos.down();
      Direction bestSide = BlockUtil.getClickSideStrict(obsPos);
      if (bestSide != null) {
         Vec3d fallbackHitVec = this.getFaceHitVec(obsPos, bestSide);
         double fallbackDistance = fallbackHitVec.distanceTo(mc.player.getEyePos());
         Vec3d bestHitVec = fallbackDistance <= this.placeRange.getValue() ? fallbackHitVec : null;
         double bestDist = bestHitVec == null ? Double.MAX_VALUE : fallbackDistance;
         double[] xzOffsets = new double[]{0.3, 0.7};
         double[] yOffsets = new double[]{0.3, 0.7, 0.9};

         for (double xOff : xzOffsets) {
            for (double yOff : yOffsets) {
               for (double zOff : xzOffsets) {
                  Vec3d samplePoint = new Vec3d(obsPos.getX() + xOff, obsPos.getY() + yOff, obsPos.getZ() + zOff);
                  if (!(samplePoint.distanceTo(mc.player.getEyePos()) > this.placeRange.getValue())) {
                     BlockHitResult result = mc.world
                        .raycast(new RaycastContext(mc.player.getEyePos(), samplePoint, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                     if (result != null && result.getType() == Type.BLOCK && result.getBlockPos().equals(obsPos)) {
                        Direction candidateSide = result.getSide();
                        double dist = result.getPos().distanceTo(mc.player.getEyePos());
                        if (BlockUtil.isStrictDirection(obsPos, candidateSide) && dist < bestDist) {
                           bestHitVec = result.getPos();
                           bestSide = candidateSide;
                           bestDist = dist;
                        }
                     }
                  }
               }
            }
         }

         if (bestHitVec != null) {
            Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            BlockHitResult hitResult = new BlockHitResult(bestHitVec, bestSide, obsPos, false);
            sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, hitResult, id));
            EntityUtil.swingHand(hand, this.swingMode.getValue());
            this.lastPlacedPos = pos;
            this.pendingPlacePos = pos.toImmutable();
            this.pendingPlaceTimer.reset();
            this.syncPos = pos;
            this.resetCadenceTimer(this.placeTimer, this.placeDelay.getValue());
         }
      }
   }

   private boolean isPlacementPending(BlockPos pos) {
      BlockPos pending = this.pendingPlacePos;
      if (pending == null) {
         return false;
      } else if (BlockUtil.hasCrystal(pending)) {
         this.pendingPlacePos = null;
         return false;
      } else {
         long delay = Math.max(20L, (long)this.placeDelay.getValue());
         if (this.pendingPlaceTimer.passed(delay)) {
            this.pendingPlacePos = null;
            return false;
         } else {
            return pending.equals(pos);
         }
      }
   }

   private void resetCadenceTimer(Timer timer, double delay) {
      long delayMs = Math.max(0L, (long)delay);
      long elapsed = timer.getMs();
      timer.reset();
      if (delayMs != 0L) {
         long overshoot = elapsed - delayMs;
         if (overshoot > 0L && overshoot < delayMs) {
            timer.setMs(overshoot);
         }
      }
   }

   private boolean faceVector(Vec3d directionVec) {
      if (directionVec == null) {
         return false;
      } else if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   private static final class AntiPearlDefense {
      private int attempts;
      private long retryAt;
   }

   private class CrystalRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         BlockPos cpos = AutoCrystalPlusPlus.this.sync.getValue() && AutoCrystalPlusPlus.this.crystalPos != null
            ? AutoCrystalPlusPlus.this.syncPos
            : AutoCrystalPlusPlus.this.crystalPos;
         if (cpos != null) {
            AutoCrystalPlusPlus.this.placeVec3d = cpos.down().toCenterPos();
         }

         if (AutoCrystalPlusPlus.this.placeVec3d != null) {
            if (AutoCrystalPlusPlus.this.fadeSpeed.getValue() >= 1.0) {
               AutoCrystalPlusPlus.this.currentFade = AutoCrystalPlusPlus.this.noPosTimer
                     .passed((long)(AutoCrystalPlusPlus.this.startFadeTime.getValue() * 1000.0))
                  ? 0.0
                  : 0.5;
            } else {
               AutoCrystalPlusPlus.this.currentFade = AnimateUtil.animate(
                  AutoCrystalPlusPlus.this.currentFade,
                  AutoCrystalPlusPlus.this.noPosTimer.passed((long)(AutoCrystalPlusPlus.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                  AutoCrystalPlusPlus.this.fadeSpeed.getValue() / 10.0
               );
            }

            if (AutoCrystalPlusPlus.this.currentFade == 0.0) {
               AutoCrystalPlusPlus.this.curVec3d = null;
            } else {
               if (AutoCrystalPlusPlus.this.curVec3d != null && !(AutoCrystalPlusPlus.this.sliderSpeed.getValue() >= 1.0)) {
                  AutoCrystalPlusPlus.this.curVec3d = new Vec3d(
                     AnimateUtil.animate(
                        AutoCrystalPlusPlus.this.curVec3d.x, AutoCrystalPlusPlus.this.placeVec3d.x, AutoCrystalPlusPlus.this.sliderSpeed.getValue() / 10.0
                     ),
                     AnimateUtil.animate(
                        AutoCrystalPlusPlus.this.curVec3d.y, AutoCrystalPlusPlus.this.placeVec3d.y, AutoCrystalPlusPlus.this.sliderSpeed.getValue() / 10.0
                     ),
                     AnimateUtil.animate(
                        AutoCrystalPlusPlus.this.curVec3d.z, AutoCrystalPlusPlus.this.placeVec3d.z, AutoCrystalPlusPlus.this.sliderSpeed.getValue() / 10.0
                     )
                  );
               } else {
                  AutoCrystalPlusPlus.this.curVec3d = AutoCrystalPlusPlus.this.placeVec3d;
               }

               if (AutoCrystalPlusPlus.this.render.getValue()) {
                  Box cbox = new Box(AutoCrystalPlusPlus.this.curVec3d, AutoCrystalPlusPlus.this.curVec3d);
                  if (AutoCrystalPlusPlus.this.shrink.getValue()) {
                     cbox = cbox.expand(AutoCrystalPlusPlus.this.currentFade);
                  } else {
                     cbox = cbox.expand(0.5);
                  }

                  MatrixStack matrixStack = event.matrixStack;
                  if (AutoCrystalPlusPlus.this.fill.booleanValue) {
                     Render3DUtil.drawFill(
                        matrixStack,
                        cbox,
                        ColorUtil.injectAlpha(
                           AutoCrystalPlusPlus.this.fill.getValue(),
                           (int)(AutoCrystalPlusPlus.this.fill.getValue().getAlpha() * AutoCrystalPlusPlus.this.currentFade * 2.0)
                        )
                     );
                  }

                  if (AutoCrystalPlusPlus.this.box.booleanValue) {
                     Render3DUtil.drawBox(
                        matrixStack,
                        cbox,
                        ColorUtil.injectAlpha(
                           AutoCrystalPlusPlus.this.box.getValue(),
                           (int)(AutoCrystalPlusPlus.this.box.getValue().getAlpha() * AutoCrystalPlusPlus.this.currentFade * 2.0)
                        ),
                        AutoCrystalPlusPlus.this.lineWidth.getValueFloat()
                     );
                  }
               }

               if (AutoCrystalPlusPlus.this.text.booleanValue
                  && AutoCrystalPlusPlus.this.lastDamage > 0.0F
                  && !AutoCrystalPlusPlus.this.noPosTimer.passed((long)(AutoCrystalPlusPlus.this.startFadeTime.getValue() * 1000.0))) {
                  Render3DUtil.drawText3D(
                     AutoCrystalPlusPlus.this.df.format(AutoCrystalPlusPlus.this.lastDamage),
                     AutoCrystalPlusPlus.this.curVec3d,
                     AutoCrystalPlusPlus.this.text.getValue()
                  );
               }
            }
         }
      }
   }

   private static enum Page {
      General,
      Base,
      Misc,
      Rotation,
      Check,
      Calc,
      Render;
   }

   private static enum SwapMode {
      None,
      Normal,
      Silent,
      Inventory;
   }

   public static enum TargetESP {
      Box,
      Fill,
      Jello,
      ThunderHack,
      None;
   }
}
