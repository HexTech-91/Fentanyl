package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
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

public class AutoAnchor extends Module {
   public static AutoAnchor INSTANCE;
   static Vec3d placeVec3d;
   static Vec3d curVec3d;
   public final EnumSetting<AutoAnchor.Page> page = this.add(new EnumSetting<>("Page", AutoAnchor.Page.General));
   public final SliderSetting range;
   public final SliderSetting targetRange;
   public final SliderSetting minDamage;
   public final SliderSetting breakMin;
   public final SliderSetting headDamage;
   private final SliderSetting selfPredict;
   private final SliderSetting predictTicks;
   private final SliderSetting simulation;
   private final SliderSetting maxMotionY;
   private final BooleanSetting step;
   private final BooleanSetting doubleStep;
   private final BooleanSetting jump;
   private final BooleanSetting inBlockPause;
   final ArrayList<BlockPos> chargeList;
   private final BooleanSetting assist;
   private final BooleanSetting obsidian;
   private final BooleanSetting checkMine;
   private final SliderSetting assistRange;
   private final SliderSetting assistDamage;
   private final SliderSetting delay;
   private final BooleanSetting preferCrystal;
   private final BooleanSetting thread;
   private final BooleanSetting light;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting spam;
   private final BooleanSetting mineSpam;
   private final BooleanSetting spamPlace;
   private final BooleanSetting inSpam;
   private final BooleanSetting usingPause;
   private final EnumSetting<SwingSide> swingMode;
   private final EnumSetting<Timing> timing;
   private final SliderSetting placeDelay;
   private final SliderSetting fillDelay;
   private final SliderSetting breakDelay;
   private final SliderSetting spamDelay;
   private final SliderSetting updateDelay;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting noSuicide;
   private final BooleanSetting smart;
   private final BooleanSetting terrainIgnore;
   private final SliderSetting minPrefer;
   private final SliderSetting maxSelfDamage;
   private final EnumSetting<Aura.TargetESP> mode;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final BooleanSetting render;
   private final BooleanSetting shrink;
   private final ColorSetting box;
   private final ColorSetting fill;
   private final SliderSetting sliderSpeed;
   private final SliderSetting startFadeTime;
   private final SliderSetting fadeSpeed;
   private final Timer delayTimer;
   private final Timer calcTimer;
   private final Timer noPosTimer;
   private final Timer assistTimer;
   public Vec3d directionVec;
   public PlayerEntity displayTarget;
   public BlockPos currentPos;
   public BlockPos tempPos;
   public double lastDamage;
   double fade;
   BlockPos assistPos;

   public AutoAnchor() {
      super("AutoAnchor", Module.Category.Combat);
      this.page.setChinese("\u9875\u9762");
      this.range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("m"));
      this.range.setChinese("\u8303\u56f4");
      this.targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1, 12.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("m"));
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.minDamage = this.add(new SliderSetting("MinDamage", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg"));
      this.minDamage.setChinese("\u6700\u5c0f\u4f24\u5bb3");
      this.breakMin = this.add(new SliderSetting("BreakMin", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg"));
      this.breakMin.setChinese("\u7206\u70b8\u6700\u5c0f");
      this.headDamage = this.add(new SliderSetting("HeadDamage", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg"));
      this.headDamage.setChinese("\u5f3a\u5236\u7206\u5934");
      this.selfPredict = this.add(new SliderSetting("SelfPredict", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict).setSuffix("ticks"));
      this.selfPredict.setChinese("\u81ea\u8eab\u9884\u6d4b");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict).setSuffix("ticks"));
      this.predictTicks.setChinese("\u9884\u6d4b");
      this.simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.simulation.setChinese("\u6a21\u62df");
      this.maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.maxMotionY.setChinese("\u6700\u5927Y\u901f\u5ea6");
      this.step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.step.setChinese("\u8de8\u6b65");
      this.doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.doubleStep.setChinese("\u53cc\u8de8\u6b65");
      this.jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.jump.setChinese("\u8df3\u8dc3");
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AutoAnchor.Page.Predict));
      this.inBlockPause.setChinese("\u65b9\u5757\u5185\u6682\u505c");
      this.chargeList = new ArrayList<>();
      this.assist = this.add(new BooleanSetting("Assist", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.assist.setChinese("\u8f85\u52a9");
      this.obsidian = this.add(new BooleanSetting("Obsidian", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.obsidian.setChinese("\u9ed1\u66dc\u77f3");
      this.checkMine = this.add(new BooleanSetting("CheckMine", false, () -> this.page.getValue() == AutoAnchor.Page.Assist));
      this.checkMine.setChinese("\u68c0\u6d4b\u6316\u6398");
      this.assistRange = this.add(new SliderSetting("AssistRange", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("m"));
      this.assistRange.setChinese("\u8f85\u52a9\u8303\u56f4");
      this.assistDamage = this.add(new SliderSetting("AssistDamage", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("h"));
      this.assistDamage.setChinese("\u8f85\u52a9\u4f24\u5bb3");
      this.delay = this.add(new SliderSetting("Delay", 0.1, 0.0, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("s"));
      this.delay.setChinese("\u8f85\u52a9\u5ef6\u8fdf");
      this.preferCrystal = this.add(new BooleanSetting("PreferCrystal", false, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.preferCrystal.setChinese("\u4f18\u5148\u6c34\u6676");
      this.thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.thread.setChinese("\u591a\u7ebf\u7a0b");
      this.light = this.add(new BooleanSetting("Light", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.light.setChinese("\u4f4eCPU");
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.inventorySwap.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.breakCrystal = this.add(new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent());
      this.breakCrystal.setChinese("\u6253\u6c34\u6676");
      this.spam = this.add(new BooleanSetting("Spam", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent());
      this.spam.setChinese("\u8fde\u7206");
      this.mineSpam = this.add(new BooleanSetting("MineSpam", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spam.isOpen()));
      this.mineSpam.setChinese("\u4ec5\u6316\u6398\u65f6");
      this.spamPlace = this.add(new BooleanSetting("SpamPlace", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent());
      this.spamPlace.setChinese("\u5feb\u901f");
      this.inSpam = this.add(new BooleanSetting("InSpam", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spamPlace.isOpen()));
      this.inSpam.setChinese("\u8fde\u7206\u65f6");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.usingPause.setChinese("\u4f7f\u7528\u4e2d\u6682\u505c");
      this.swingMode = this.add(new EnumSetting<>("SwingMode", SwingSide.All, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.swingMode.setChinese("\u6325\u624b");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == AutoAnchor.Page.General));
      this.timing.setChinese("\u65f6\u673a");
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms"));
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.fillDelay = this.add(new SliderSetting("FillDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms"));
      this.fillDelay.setChinese("\u586b\u5145\u5ef6\u8fdf");
      this.breakDelay = this.add(new SliderSetting("BreakDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms"));
      this.breakDelay.setChinese("\u5f15\u7206\u5ef6\u8fdf");
      this.spamDelay = this.add(new SliderSetting("SpamDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms"));
      this.spamDelay.setChinese("\u8fde\u7206\u5ef6\u8fdf");
      this.updateDelay = this.add(
         new SliderSetting("UpdateDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
      );
      this.updateDelay.setChinese("\u66f4\u65b0\u5ef6\u8fdf");
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoAnchor.Page.Rotate).setParent());
      this.rotate.setChinese("\u8f6c\u5934");
      this.yawStep = this.add(new BooleanSetting("YawStep", true, () -> this.rotate.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(
         new BooleanSetting("WhenElytra", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
      );
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(
         new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
      );
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(
         new BooleanSetting("CheckFov", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
      );
      this.checkFov.setChinese("\u4ec5\u89c6\u7ebf\u5185");
      this.fov = this.add(
         new SliderSetting(
            "\u89c6\u91ce",
            20.0,
            0.0,
            360.0,
            0.1,
            () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoAnchor.Page.Rotate
         )
      );
      this.priority = this.add(
         new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
      );
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.noSuicide = this.add(new BooleanSetting("NoSuicide", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.noSuicide.setChinese("\u9632\u81ea\u6740");
      this.smart = this.add(new BooleanSetting("Smart", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.smart.setChinese("\u667a\u80fd");
      this.terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
      this.terrainIgnore.setChinese("\u5ffd\u7565\u5730\u5f62");
      this.minPrefer = this.add(new SliderSetting("MinPrefer", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg"));
      this.minPrefer.setChinese("\u4f18\u5148");
      this.maxSelfDamage = this.add(
         new SliderSetting("MaxSelfDamage", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
      );
      this.maxSelfDamage.setChinese("\u6700\u5927\u81ea\u4f24");
      this.mode = this.add(new EnumSetting<>("Mode", Aura.TargetESP.Jello, () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.mode.setChinese("\u76ee\u6807\u6e32\u67d3");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.color.setChinese("\u76ee\u6807\u989c\u8272");
      this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.outlineColor.setChinese("\u76ee\u6807\u8f6e\u5ed3\u989c\u8272");
      this.render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == AutoAnchor.Page.Render));
      this.render.setChinese("\u6e32\u67d3");
      this.shrink = this.add(new BooleanSetting("Shrink", true, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()));
      this.shrink.setChinese("\u6536\u7f29");
      this.box = this.add(
         new ColorSetting("Box", new Color(255, 255, 255, 255), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
            .injectBoolean(true)
      );
      this.box.setChinese("\u8fb9\u6846");
      this.fill = this.add(
         new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
            .injectBoolean(true)
      );
      this.fill.setChinese("\u586b\u5145");
      this.sliderSpeed = this.add(
         new SliderSetting("SliderSpeed", 0.2, 0.0, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
      );
      this.sliderSpeed.setChinese("\u6ed1\u52a8\u901f\u5ea6");
      this.startFadeTime = this.add(
         new SliderSetting("StartFadeTime", 0.3, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()).setSuffix("s")
      );
      this.startFadeTime.setChinese("\u5f00\u59cb\u6e10\u9690");
      this.fadeSpeed = this.add(
         new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
      );
      this.fadeSpeed.setChinese("\u6e10\u9690\u901f\u5ea6");
      this.delayTimer = new Timer();
      this.calcTimer = new Timer();
      this.noPosTimer = new Timer();
      this.assistTimer = new Timer();
      this.directionVec = null;
      this.fade = 0.0;
      this.setChinese("\u91cd\u751f\u951a\u5149\u73af");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoAnchor.AnchorRender());
   }

   public static boolean canSee(Vec3d from, Vec3d to) {
      HitResult result = mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   @Override
   public String getInfo() {
      return this.displayTarget != null && this.currentPos != null ? this.displayTarget.getName().getString() : null;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.displayTarget != null && this.currentPos != null) {
         Aura.doRender(
            matrixStack,
            mc.getRenderTickCounter().getTickDelta(true),
            this.displayTarget,
            this.color.getValue(),
            this.outlineColor.getValue(),
            this.mode.getValue()
         );
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.currentPos != null && this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @Override
   public void onDisable() {
      this.tempPos = null;
      this.currentPos = null;
   }

   public void onThread() {
      if (!this.isOff() && !nullCheck() && this.thread.getValue()) {
         if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.currentPos = null;
            return;
         }

         if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
            this.currentPos = null;
            return;
         }

         int anchor = this.inventorySwap.getValue()
            ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR)
            : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
         int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
         int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
         if (anchor == -1) {
            this.currentPos = null;
            return;
         }

         if (glowstone == -1) {
            this.currentPos = null;
            return;
         }

         if (unBlock == -1) {
            this.currentPos = null;
            return;
         }

         if (mc.player.isSneaking()) {
            this.currentPos = null;
            return;
         }

         if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            this.currentPos = null;
            return;
         }

         this.calc();
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && (!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
         int anchor = this.inventorySwap.getValue()
            ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR)
            : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
         int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
         int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
         int old = mc.player.getInventory().selectedSlot;
         if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.currentPos = null;
         } else if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null) {
            this.currentPos = null;
         } else if (anchor == -1) {
            this.currentPos = null;
         } else if (glowstone == -1) {
            this.currentPos = null;
         } else if (unBlock == -1) {
            this.currentPos = null;
         } else if (mc.player.isSneaking()) {
            this.currentPos = null;
         } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            this.currentPos = null;
         } else if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
            if (this.assist.getValue()) {
               this.onAssist();
            }

            if (!this.thread.getValue()) {
               this.calc();
            }

            BlockPos pos = this.currentPos;
            if (pos != null) {
               if (this.breakCrystal.getValue()) {
                  CombatUtil.attackCrystal(new BlockPos(pos), this.rotate.getValue(), false);
               }

               boolean shouldSpam = this.spam.getValue() && (!this.mineSpam.getValue() || Alien.BREAK.isMining(pos));
               if (shouldSpam) {
                  if (!this.delayTimer.passed((long)this.spamDelay.getValueFloat())) {
                     return;
                  }

                  this.delayTimer.reset();
                  if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                     this.placeBlock(pos, this.rotate.getValue(), anchor);
                  }

                  if (!this.chargeList.contains(pos)) {
                     this.delayTimer.reset();
                     this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                     this.chargeList.add(pos);
                  }

                  this.chargeList.remove(pos);
                  this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                  if (this.spamPlace.getValue() && this.inSpam.getValue()) {
                     if (this.shouldYawStep() && this.checkFov.getValue()) {
                        Direction side = BlockUtil.getClickSide(pos);
                        Vec3d directionVec = new Vec3d(
                           pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                           pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                           pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                        );
                        if (Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                           this.placeBlock(pos, this.rotate.getValue(), anchor);
                           CombatUtil.modifyPos = null;
                        }
                     } else {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                        CombatUtil.modifyPos = null;
                     }
                  }
               } else if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                  if (!this.delayTimer.passed((long)this.placeDelay.getValueFloat())) {
                     return;
                  }

                  this.delayTimer.reset();
                  this.placeBlock(pos, this.rotate.getValue(), anchor);
               } else if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                  if (!this.chargeList.contains(pos)) {
                     if (!this.delayTimer.passed((long)this.fillDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                     this.chargeList.add(pos);
                  } else {
                     if (!this.delayTimer.passed((long)this.breakDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     this.chargeList.remove(pos);
                     this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                     if (this.spamPlace.getValue()) {
                        if (this.shouldYawStep() && this.checkFov.getValue()) {
                           Direction side = BlockUtil.getClickSide(pos);
                           Vec3d directionVec = new Vec3d(
                              pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                              pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                              pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                           );
                           if (Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                              CombatUtil.modifyPos = pos;
                              CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                              this.placeBlock(pos, this.rotate.getValue(), anchor);
                              CombatUtil.modifyPos = null;
                           }
                        } else {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                           this.placeBlock(pos, this.rotate.getValue(), anchor);
                           CombatUtil.modifyPos = null;
                        }
                     }
                  }
               }

               if (!this.inventorySwap.getValue()) {
                  this.doSwap(old);
               }
            }
         }
      }
   }

   private void calc() {
      if (!nullCheck()) {
         if (this.calcTimer.passed((long)this.updateDelay.getValueFloat())) {
            this.calcTimer.reset();
            PlayerEntityPredict selfPredict = new PlayerEntityPredict(
               mc.player,
               this.maxMotionY.getValue(),
               this.selfPredict.getValueInt(),
               this.simulation.getValueInt(),
               this.step.getValue(),
               this.doubleStep.getValue(),
               this.jump.getValue(),
               this.inBlockPause.getValue()
            );
            this.tempPos = null;
            double placeDamage = this.minDamage.getValue();
            double breakDamage = this.breakMin.getValue();
            boolean anchorFound = false;
            List<PlayerEntity> enemies = CombatUtil.getEnemies(this.targetRange.getValue());
            ArrayList<PlayerEntityPredict> list = new ArrayList<>();

            for (PlayerEntity player : enemies) {
               list.add(
                  new PlayerEntityPredict(
                     player,
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

            for (PlayerEntityPredict pap : list) {
               BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
               double selfDamage;
               double damage;
               if ((
                     BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())
                        || BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR && BlockUtil.getClickSideStrict(pos) != null
                  )
                  && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                  && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                  && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) > this.headDamage.getValueFloat()
                  && (!this.smart.getValue() || !(selfDamage > damage))) {
                  this.lastDamage = damage;
                  this.displayTarget = pap.player;
                  this.tempPos = pos;
                  break;
               }
            }

            if (this.tempPos == null) {
               for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat() + 1.0F, mc.player.getEyePos())) {
                  for (PlayerEntityPredict papx : list) {
                     if (this.light.getValue()) {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        boolean skip = !canSee(pos.toCenterPos(), papx.predict.getPos());
                        CombatUtil.modifyPos = null;
                        if (skip) {
                           continue;
                        }
                     }

                     if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                        if (!anchorFound && BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                           boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                           CombatUtil.modifyPos = null;
                           if (!skip) {
                              double damage = this.getAnchorDamage(pos, papx.player, papx.predict);
                              double selfDamage;
                              if (damage >= placeDamage
                                 && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage)
                                 && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                                 && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                                 && (!this.smart.getValue() || !(selfDamage > damage))) {
                                 this.lastDamage = damage;
                                 this.displayTarget = papx.player;
                                 placeDamage = damage;
                                 this.tempPos = pos;
                              }
                           }
                        }
                     } else {
                        double damage = this.getAnchorDamage(pos, papx.player, papx.predict);
                        if (BlockUtil.getClickSideStrict(pos) != null && damage >= breakDamage) {
                           if (damage >= this.minPrefer.getValue()) {
                              anchorFound = true;
                           }

                           double selfDamage;
                           if ((anchorFound || !(damage < placeDamage))
                              && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage)
                              && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                              && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                              && (!this.smart.getValue() || !(selfDamage > damage))) {
                              this.lastDamage = damage;
                              this.displayTarget = papx.player;
                              breakDamage = damage;
                              this.tempPos = pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         this.currentPos = this.tempPos;
      }
   }

   public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      double damage = ExplosionUtil.anchorDamage(anchorPos, target, predict);
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   public void placeBlock(BlockPos pos, boolean rotate, int slot) {
      if (BlockUtil.allowAirPlace()) {
         this.airPlace(pos, rotate, slot);
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side != null) {
            this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
         }
      }
   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
      if (pos != null) {
         Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
         );
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   public void airPlace(BlockPos pos, boolean rotate, int slot) {
      if (pos != null) {
         Direction side = BlockUtil.getClickSide(pos);
         Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
         );
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   public boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   public void onAssist() {
      this.assistPos = null;
      int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
      int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
      int old = mc.player.getInventory().selectedSlot;
      if (anchor != -1) {
         if (this.obsidian.getValue()) {
            anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
               return;
            }
         }

         if (glowstone != -1
            && !mc.player.isSneaking()
            && (!this.usingPause.getValue() || !mc.player.isUsingItem())
            && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())
            && this.assistTimer.passed((long)(this.delay.getValueFloat() * 1000.0F))) {
            this.assistTimer.reset();
            ArrayList<PlayerEntityPredict> list = new ArrayList<>();

            for (PlayerEntity player : CombatUtil.getEnemies(this.assistRange.getValue())) {
               list.add(
                  new PlayerEntityPredict(
                     player,
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

            double bestDamage = this.assistDamage.getValue();

            for (PlayerEntityPredict pap : list) {
               BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
               if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                  return;
               }

               if (BlockUtil.clientCanPlace(pos, false)) {
                  double damage = this.getAnchorDamage(pos, pap.player, pap.predict);
                  if (damage >= bestDamage) {
                     bestDamage = damage;
                     this.assistPos = pos;
                  }
               }

               for (Direction i : Direction.values()) {
                  if (i != Direction.UP && i != Direction.DOWN && BlockUtil.clientCanPlace(pos.offset(i), false)) {
                     double damage = this.getAnchorDamage(pos.offset(i), pap.player, pap.predict);
                     if (damage >= bestDamage) {
                        bestDamage = damage;
                        this.assistPos = pos.offset(i);
                     }
                  }
               }
            }

            BlockPos placePos;
            if (this.assistPos != null
               && BlockUtil.getPlaceSide(this.assistPos, this.range.getValue()) == null
               && (placePos = this.getHelper(this.assistPos)) != null) {
               this.doSwap(anchor);
               BlockUtil.placeBlock(placePos, this.rotate.getValue());
               if (this.inventorySwap.getValue()) {
                  this.doSwap(anchor);
               } else {
                  this.doSwap(old);
               }
            }
         }
      }
   }

   public BlockPos getHelper(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
            && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
            && BlockUtil.canPlace(pos.offset(i))) {
            return pos.offset(i);
         }
      }

      return null;
   }

   public class AnchorRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
         if (currentPos != null) {
            AutoAnchor.this.noPosTimer.reset();
            AutoAnchor.placeVec3d = currentPos.toCenterPos();
         }

         if (AutoAnchor.placeVec3d != null) {
            if (AutoAnchor.this.fadeSpeed.getValue() >= 1.0) {
               AutoAnchor.this.fade = AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5;
            } else {
               AutoAnchor.this.fade = AnimateUtil.animate(
                  AutoAnchor.this.fade,
                  AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                  AutoAnchor.this.fadeSpeed.getValue() / 10.0
               );
            }

            if (AutoAnchor.this.fade == 0.0) {
               AutoAnchor.curVec3d = null;
            } else {
               if (AutoAnchor.curVec3d != null && !(AutoAnchor.this.sliderSpeed.getValue() >= 1.0)) {
                  AutoAnchor.curVec3d = new Vec3d(
                     AnimateUtil.animate(AutoAnchor.curVec3d.x, AutoAnchor.placeVec3d.x, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoAnchor.curVec3d.y, AutoAnchor.placeVec3d.y, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoAnchor.curVec3d.z, AutoAnchor.placeVec3d.z, AutoAnchor.this.sliderSpeed.getValue() / 10.0)
                  );
               } else {
                  AutoAnchor.curVec3d = AutoAnchor.placeVec3d;
               }

               if (AutoAnchor.this.render.getValue()) {
                  Box cbox = new Box(AutoAnchor.curVec3d, AutoAnchor.curVec3d);
                  if (AutoAnchor.this.shrink.getValue()) {
                     cbox = cbox.expand(AutoAnchor.this.fade);
                  } else {
                     cbox = cbox.expand(0.5);
                  }

                  if (AutoAnchor.this.fill.booleanValue) {
                     event.drawFill(
                        cbox,
                        ColorUtil.injectAlpha(AutoAnchor.this.fill.getValue(), (int)(AutoAnchor.this.fill.getValue().getAlpha() * AutoAnchor.this.fade * 2.0))
                     );
                  }

                  if (AutoAnchor.this.box.booleanValue) {
                     event.drawBox(
                        cbox,
                        ColorUtil.injectAlpha(AutoAnchor.this.box.getValue(), (int)(AutoAnchor.this.box.getValue().getAlpha() * AutoAnchor.this.fade * 2.0))
                     );
                  }
               }
            }
         }
      }
   }

   public static enum Page {
      General,
      Interact,
      Predict,
      Rotate,
      Assist,
      Render;
   }
}
