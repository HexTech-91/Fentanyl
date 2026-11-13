package dev.luminous.mod.modules.impl.player;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClickBlockEvent;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.CevBreaker;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;

public class PacketMine extends Module {
   public static PacketMine INSTANCE;
   public static BlockPos secondPos;
   public static double progress = 0.0;
   private final FadeUtils animationTime = new FadeUtils(1000L);
   private final FadeUtils secondAnim = new FadeUtils(1000L);
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final EnumSetting<PacketMine.Page> page = this.add(new EnumSetting<>("Page", PacketMine.Page.General));
   private final SliderSetting stopDelay;
   private final SliderSetting startDelay;
   private final SliderSetting damage;
   private final SliderSetting maxBreak;
   public final BooleanSetting noGhostHand;
   public final BooleanSetting noCollide;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting grimDisabler;
   private final BooleanSetting instant;
   private final BooleanSetting wait;
   private final BooleanSetting mineAir;
   private final BooleanSetting hotBar;
   private final BooleanSetting doubleBreak;
   public final BooleanSetting autoSwitch;
   private final SliderSetting start;
   private final SliderSetting timeOut;
   private final BooleanSetting setAir;
   private final BooleanSetting swing;
   private final BooleanSetting endSwing;
   public final SliderSetting range;
   private final EnumSetting<SwingSide> swingMode;
   private final BooleanSetting unbreakableCancel;
   private final BooleanSetting switchReset;
   private final BooleanSetting preferWeb;
   private final BooleanSetting preferHead;
   private final BooleanSetting farCancel;
   private final BooleanSetting onlyGround;
   private final BooleanSetting checkWeb;
   private final BooleanSetting checkGround;
   private final BooleanSetting smart;
   private final BooleanSetting usingPause;
   private final BooleanSetting allowOffhand;
   private final BooleanSetting bypassGround;
   private final SliderSetting bypassTime;
   private final BindSetting pause;
   private final BooleanSetting rotate;
   private final BooleanSetting endRotate;
   private final SliderSetting syncTime;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   public final BooleanSetting crystal;
   private final BooleanSetting onlyHeadBomber;
   private final BooleanSetting waitPlace;
   private final BooleanSetting spamPlace;
   private final BooleanSetting afterBreak;
   private final BooleanSetting checkDamage;
   private final SliderSetting crystalDamage;
   public final BindSetting obsidian;
   private final BindSetting enderChest;
   private final BooleanSetting placeRotate;
   private final BooleanSetting inventory;
   private final SliderSetting placeDelay;
   private final BooleanSetting checkDouble;
   private final EnumSetting<PacketMine.Animation> animation;
   private final EnumSetting<Easing> ease;
   private final EnumSetting<Easing> fadeEase;
   private final SliderSetting expandLine;
   private final ColorSetting startColor;
   private final ColorSetting startOutlineColor;
   private final ColorSetting endColor;
   private final ColorSetting endOutlineColor;
   private final ColorSetting doubleColor;
   private final ColorSetting doubleOutlineColor;
   private final BooleanSetting text;
   private final BooleanSetting box;
   private final BooleanSetting outline;
   private final BooleanSetting spinner;
   private final SliderSetting spinnerSize;
   private final SliderSetting spinnerThickness;
   private final SliderSetting spinnerSpeed;
   private final ColorSetting spinnerColor;
   private final Timer mineTimer;
   private final Timer sync;
   private final Timer secondTimer;
   private final Timer delayTimer;
   private final Timer placeTimer;
   private final Timer startTime;
   public static boolean ghost = false;
   public static boolean complete = false;
   int lastSlot;
   Vec3d directionVec;
   Runnable switchBack;
   BlockPos breakPos;
   boolean startPacket;
   int breakNumber;
   double breakFinalTime;
   double secondFinalTime;
   boolean sendGroundPacket;
   boolean swapped;
   int mainSlot;

   public PacketMine() {
      super("PacketMine", Module.Category.Player);
      this.page.setChinese("\u9875\u9762");
      this.stopDelay = this.add(new SliderSetting("StopDelay", 50.0, 0.0, 500.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
      this.stopDelay.setChinese("\u505c\u6b62\u5ef6\u8fdf");
      this.startDelay = this.add(new SliderSetting("StartDelay", 200.0, 0.0, 500.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
      this.startDelay.setChinese("\u5f00\u59cb\u5ef6\u8fdf");
      this.damage = this.add(new SliderSetting("Damage", 0.7F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General)));
      this.damage.setChinese("\u4f24\u5bb3");
      this.maxBreak = this.add(new SliderSetting("MaxBreak", 3.0, 0.0, 20.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
      this.maxBreak.setChinese("\u6700\u5927\u7834\u574f");
      this.noGhostHand = this.add(new BooleanSetting("1.21", false, () -> this.page.is(PacketMine.Page.General)));
      this.noCollide = this.add(new BooleanSetting("NoCollide", true, () -> this.page.is(PacketMine.Page.General)));
      this.noCollide.setChinese("\u65e0\u78b0\u649e");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == PacketMine.Page.General));
      this.timing.setChinese("\u65f6\u673a");
      this.grimDisabler = this.add(new BooleanSetting("GrimDisabler", false, () -> this.page.is(PacketMine.Page.General)));
      this.grimDisabler.setChinese("Grim\u7ed5\u8fc7");
      this.instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(PacketMine.Page.General)));
      this.instant.setChinese("\u77ac\u95f4");
      this.wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(PacketMine.Page.General)));
      this.wait.setChinese("\u7b49\u5f85");
      this.mineAir = this.add(
         new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(PacketMine.Page.General))
      );
      this.mineAir.setChinese("\u6316\u6398\u7a7a\u6c14");
      this.hotBar = this.add(new BooleanSetting("HotBar", false, () -> this.page.is(PacketMine.Page.General)));
      this.hotBar.setChinese("\u70ed\u952e\u680f\u4ea4\u6362");
      this.doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(PacketMine.Page.General))).setParent();
      this.doubleBreak.setChinese("\u53cc\u91cd\u7834\u574f");
      this.autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.autoSwitch.setChinese("\u81ea\u52a8\u5207\u6362");
      this.start = this.add(new SliderSetting("Start", 0.9F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.start.setChinese("\u5f00\u59cb");
      this.timeOut = this.add(new SliderSetting("TimeOut", 1.2F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen()));
      this.timeOut.setChinese("\u8d85\u65f6");
      this.setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(PacketMine.Page.General)));
      this.setAir.setChinese("\u8bbe\u4e3a\u7a7a\u6c14");
      this.swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(PacketMine.Page.General)));
      this.swing.setChinese("\u6325\u624b");
      this.endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(PacketMine.Page.General)));
      this.endSwing.setChinese("\u7ed3\u675f\u6325\u624b");
      this.range = this.add(new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1, () -> this.page.is(PacketMine.Page.General)));
      this.range.setChinese("\u8303\u56f4");
      this.swingMode = this.add(new EnumSetting<>("SwingMode", SwingSide.All, () -> this.page.is(PacketMine.Page.General)));
      this.swingMode.setChinese("\u6325\u624b\u4fa7");
      this.unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(PacketMine.Page.Check)));
      this.unbreakableCancel.setChinese("\u4e0d\u53ef\u7834\u574f\u53d6\u6d88");
      this.switchReset = this.add(new BooleanSetting("SwitchReset", false, () -> this.page.is(PacketMine.Page.Check)));
      this.switchReset.setChinese("\u5207\u6362\u91cd\u7f6e");
      this.preferWeb = this.add(new BooleanSetting("PreferWeb", true, () -> this.page.is(PacketMine.Page.Check)));
      this.preferWeb.setChinese("\u4f18\u5148\u8718\u86db\u7f51");
      this.preferHead = this.add(new BooleanSetting("PreferHead", true, () -> this.page.is(PacketMine.Page.Check)));
      this.preferHead.setChinese("\u4f18\u5148\u5934\u90e8");
      this.farCancel = this.add(new BooleanSetting("FarCancel", false, () -> this.page.is(PacketMine.Page.Check)));
      this.farCancel.setChinese("\u8fdc\u8ddd\u79bb\u53d6\u6d88");
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.onlyGround.setChinese("\u4ec5\u5730\u9762");
      this.checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(PacketMine.Page.Check)));
      this.checkWeb.setChinese("\u68c0\u6d4b\u8718\u86db\u7f51");
      this.checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.checkGround.setChinese("\u68c0\u6d4b\u5730\u9762");
      this.smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(PacketMine.Page.Check) && this.checkGround.getValue()));
      this.smart.setChinese("\u667a\u80fd");
      this.usingPause = this.add(new BooleanSetting("UsingPause", false, () -> this.page.is(PacketMine.Page.Check)).setParent());
      this.usingPause.setChinese("\u4f7f\u7528\u6682\u505c");
      this.allowOffhand = this.add(new BooleanSetting("AllowOffhand", true, () -> this.page.is(PacketMine.Page.Check) && this.usingPause.isOpen()));
      this.allowOffhand.setChinese("\u5141\u8bb8\u526f\u624b");
      this.bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(PacketMine.Page.Check)));
      this.bypassGround.setChinese("\u7ed5\u8fc7\u5730\u9762");
      this.bypassTime = this.add(new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(PacketMine.Page.Check)));
      this.bypassTime.setChinese("\u7ed5\u8fc7\u65f6\u95f4");
      this.pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(PacketMine.Page.Check)));
      this.pause.setChinese("\u6682\u505c");
      this.rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(PacketMine.Page.Rotation)));
      this.rotate.setChinese("\u5f00\u59cb\u65cb\u8f6c");
      this.endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(PacketMine.Page.Rotation)));
      this.endRotate.setChinese("\u7ed3\u675f\u65cb\u8f6c");
      this.syncTime = this.add(new SliderSetting("SyncTime", 300, 0, 1000, () -> this.page.is(PacketMine.Page.Rotation)));
      this.syncTime.setChinese("\u540c\u6b65");
      this.yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.is(PacketMine.Page.Rotation)).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(new BooleanSetting("WhenElytra", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(new BooleanSetting("CheckFov", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.checkFov.setChinese("\u4ec5\u770b\u65b9\u5757");
      this.fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.fov.setChinese("\u89c6\u91ce");
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen()));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.crystal = this.add(new BooleanSetting("Crystal", false, () -> this.page.is(PacketMine.Page.Place)).setParent());
      this.crystal.setChinese("\u6c34\u6676");
      this.onlyHeadBomber = this.add(new BooleanSetting("OnlyHeadBomber", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.onlyHeadBomber.setChinese("\u4ec5Cev");
      this.waitPlace = this.add(new BooleanSetting("WaitPlace", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.waitPlace.setChinese("\u7b49\u5f85\u653e\u7f6e");
      this.spamPlace = this.add(new BooleanSetting("SpamPlace", false, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.spamPlace.setChinese("\u8fde\u7eed\u653e\u7f6e");
      this.afterBreak = this.add(new BooleanSetting("AfterBreak", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.afterBreak.setChinese("\u7834\u574f\u540e");
      this.checkDamage = this.add(new BooleanSetting("CheckDamage", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
      this.checkDamage.setChinese("\u68c0\u6d4b\u8fdb\u5ea6");
      this.crystalDamage = this.add(
         new SliderSetting(
            "CrystalDamage", 0.9F, 0.0, 1.0, 0.01, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen() && this.checkDamage.getValue()
         )
      );
      this.crystalDamage.setChinese("\u8fdb\u5ea6");
      this.obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(PacketMine.Page.Place)));
      this.obsidian.setChinese("\u9ed1\u66dc\u77f3");
      this.enderChest = this.add(new BindSetting("EnderChest", -1, () -> this.page.is(PacketMine.Page.Place)));
      this.enderChest.setChinese("\u672b\u5f71\u7bb1");
      this.placeRotate = this.add(new BooleanSetting("PlaceRotate", true, () -> this.page.is(PacketMine.Page.Place)));
      this.placeRotate.setChinese("\u653e\u7f6e\u65cb\u8f6c");
      this.inventory = this.add(new BooleanSetting("Inventory", true, () -> this.page.is(PacketMine.Page.Place)));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> this.page.is(PacketMine.Page.Place)));
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(PacketMine.Page.Render)));
      this.checkDouble.setChinese("\u68c0\u6d4b\u53cc\u91cd");
      this.animation = this.add(new EnumSetting<>("Animation", PacketMine.Animation.Up, () -> this.page.is(PacketMine.Page.Render)));
      this.animation.setChinese("\u52a8\u753b");
      this.ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
      this.ease.setChinese("\u7f13\u52a8");
      this.fadeEase = this.add(new EnumSetting<>("FadeEase", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
      this.fadeEase.setChinese("\u6e10\u9690\u7f13\u52a8");
      this.expandLine = this.add(new SliderSetting("ExpandLine", 0.0, 0.0, 1.0, () -> this.page.is(PacketMine.Page.Render)));
      this.expandLine.setChinese("\u6269\u5c55\u7ebf");
      this.startColor = this.add(new ColorSetting("StartColor", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.startColor.setChinese("\u5f00\u59cb\u586b\u5145");
      this.startOutlineColor = this.add(new ColorSetting("StartOutlineColor", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.startOutlineColor.setChinese("\u5f00\u59cb\u8f6e\u5ed3");
      this.endColor = this.add(new ColorSetting("EndColor", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.endColor.setChinese("\u7ed3\u675f\u586b\u5145");
      this.endOutlineColor = this.add(new ColorSetting("EndOutlineColor", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
      this.endOutlineColor.setChinese("\u7ed3\u675f\u8f6e\u5ed3");
      this.doubleColor = this.add(
         new ColorSetting("DoubleColor", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render))
      );
      this.doubleColor.setChinese("\u53cc\u91cd\u586b\u5145");
      this.doubleOutlineColor = this.add(
         new ColorSetting("DoubleOutlineColor", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render))
      );
      this.doubleOutlineColor.setChinese("\u53cc\u91cd\u8f6e\u5ed3");
      this.text = this.add(new BooleanSetting("Text", true, () -> this.page.is(PacketMine.Page.Render)));
      this.text.setChinese("\u6587\u5b57");
      this.box = this.add(new BooleanSetting("Box", true, () -> this.page.is(PacketMine.Page.Render)));
      this.box.setChinese("\u8fb9\u6846");
      this.outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(PacketMine.Page.Render)));
      this.outline.setChinese("\u8f6e\u5ed3");
      this.spinner = this.add(new BooleanSetting("Spinner", true, () -> this.page.is(PacketMine.Page.Render)));
      this.spinner.setChinese("\u52a0\u8f7d\u52a8\u753b");
      this.spinnerSize = this.add(
         new SliderSetting("SpinnerSize", 0.66, 0.1, 2.0, 0.02, () -> this.page.is(PacketMine.Page.Render) && this.spinner.getValue())
      );
      this.spinnerSize.setChinese("\u52a0\u8f7d\u5927\u5c0f");
      this.spinnerThickness = this.add(
         new SliderSetting("SpinnerThickness", 3.0, 1.0, 10.0, 0.5, () -> this.page.is(PacketMine.Page.Render) && this.spinner.getValue())
      );
      this.spinnerThickness.setChinese("\u52a0\u8f7d\u7c97\u7ec6");
      this.spinnerSpeed = this.add(
         new SliderSetting("SpinnerSpeed", 1.0, 0.1, 5.0, 0.1, () -> this.page.is(PacketMine.Page.Render) && this.spinner.getValue())
      );
      this.spinnerSpeed.setChinese("\u52a0\u8f7d\u901f\u5ea6");
      this.spinnerColor = this.add(
         new ColorSetting("SpinnerColor", new Color(255, 255, 255), () -> this.page.is(PacketMine.Page.Render) && this.spinner.getValue())
      );
      this.spinnerColor.setChinese("\u52a0\u8f7d\u989c\u8272");
      this.mineTimer = new Timer();
      this.sync = new Timer();
      this.secondTimer = new Timer();
      this.delayTimer = new Timer();
      this.placeTimer = new Timer();
      this.startTime = new Timer();
      this.lastSlot = -1;
      this.directionVec = null;
      this.startPacket = false;
      this.breakNumber = 0;
      this.sendGroundPacket = false;
      this.swapped = false;
      this.mainSlot = 0;
      this.setChinese("\u53d1\u5305\u6316\u6398");
      INSTANCE = this;
   }

   public static BlockPos getBreakPos() {
      return INSTANCE.isOn() ? INSTANCE.breakPos : null;
   }

   @Override
   public String getInfo() {
      return progress >= 1.0 ? "Done" : this.df.format(progress * 100.0) + "%";
   }

   @EventListener
   public void onRotate(RotationEvent var1) {
      if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
         var1.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @Override
   public void onLogin() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
      secondPos = null;
   }

   @Override
   public void onDisable() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
   }

   private void autoSwitch() {
      if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
         int var1 = -1;
         if (secondPos != null) {
            float var2 = 1.0F;

            for (int var3 = 0; var3 < 9; var3++) {
               ItemStack var4 = mc.player.getInventory().getStack(var3);
               if (var4 != ItemStack.EMPTY) {
                  float var5 = EnchantmentHelper.getLevel(
                     (RegistryEntry)mc.world
                        .getRegistryManager()
                        .getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef())
                        .getOptional(Enchantments.EFFICIENCY)
                        .get(),
                     var4
                  );
                  float var6 = var4.getMiningSpeedMultiplier(mc.world.getBlockState(secondPos));
                  if (var5 + var6 > var2) {
                     var2 = var5 + var6;
                     var1 = var3;
                  }
               }
            }
         }

         if (var1 != -1
            && !mc.options.useKey.isPressed()
            && !mc.options.attackKey.isPressed()
            && !mc.player.isUsingItem()
            && this.secondTimer.passedMs(this.getBreakTime(secondPos, var1, this.start.getValue()))) {
            if (var1 != mc.player.getInventory().selectedSlot) {
               this.mainSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(var1);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.mainSlot);
            this.swapped = false;
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck()) {
         if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
            complete = true;
         }

         if (secondPos != null) {
            int var2 = this.getTool(secondPos);
            if (var2 == -1) {
               var2 = mc.player.getInventory().selectedSlot;
            }

            this.secondFinalTime = this.getBreakTime(secondPos, var2, 1.0);
            if (!this.isAir(secondPos) && !unbreakable(secondPos)) {
               double var3 = this.getBreakTime(secondPos, mc.player.getInventory().selectedSlot, 1.0);
               if (this.secondTimer.passedMs(var3 * this.timeOut.getValue())) {
                  secondPos = null;
               }
            } else {
               secondPos = null;
            }
         }

         if (this.switchBack != null && var1.isPre()) {
            this.switchBack.run();
            this.switchBack = null;
         }

         if ((!this.timing.is(Timing.Pre) || !var1.isPost()) && (!this.timing.is(Timing.Post) || !var1.isPre())) {
            if (mc.player.isDead()) {
               secondPos = null;
            }

            this.autoSwitch();
            if (mc.player.isCreative()) {
               this.startPacket = false;
               ghost = false;
               complete = false;
               this.breakNumber = 0;
               this.breakPos = null;
               progress = 0.0;
            } else if (this.breakPos == null) {
               this.breakNumber = 0;
               this.startPacket = false;
               ghost = false;
               complete = false;
               progress = 0.0;
            } else {
               int var7 = this.getTool(this.breakPos);
               if (var7 == -1) {
                  var7 = mc.player.getInventory().selectedSlot;
               }

               this.breakFinalTime = this.getBreakTime(this.breakPos, var7);
               progress = this.mineTimer.getMs() / this.breakFinalTime;
               if (this.isAir(this.breakPos)) {
                  this.breakNumber = 0;
               }

               if ((!(this.breakNumber > this.maxBreak.getValue() - 1.0) || !(this.maxBreak.getValue() > 0.0) || complete)
                  && (this.wait.getValue() || !this.isAir(this.breakPos) || this.instant.getValue())) {
                  if (unbreakable(this.breakPos)) {
                     if (this.unbreakableCancel.getValue()) {
                        this.breakPos = null;
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                     }

                     this.breakNumber = 0;
                  } else if (MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos())) > this.range.getValue()) {
                     if (this.farCancel.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.breakNumber = 0;
                        this.breakPos = null;
                     }
                  } else if ((
                        !this.usingPause.getValue() || !mc.player.isUsingItem() || this.allowOffhand.getValue() && mc.player.getActiveHand() != Hand.MAIN_HAND
                     )
                     && !this.pause.isPressed()
                     && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())
                     && (!this.breakPos.equals(AutoAnchor.INSTANCE.currentPos) || !(BlockUtil.getBlock(getBreakPos()) instanceof RespawnAnchorBlock))
                     && (this.hotBar.getValue() || EntityUtil.inInventory())) {
                     if (!this.isAir(this.breakPos)) {
                        if (this.canPlaceCrystal(this.breakPos.up()) && this.shouldCrystal()) {
                           if (this.placeTimer.passedMs(this.placeDelay.getValue())) {
                              if (this.checkDamage.getValue()) {
                                 if (this.mineTimer.getMs() / this.breakFinalTime >= this.crystalDamage.getValue() && !this.placeCrystal()) {
                                    return;
                                 }
                              } else if (!this.placeCrystal()) {
                                 return;
                              }
                           } else if (this.startPacket) {
                              return;
                           }
                        }
                     } else {
                        if (this.shouldCrystal()) {
                           for (Direction var6 : Direction.values()) {
                              CombatUtil.attackCrystal(this.breakPos.offset(var6), this.placeRotate.getValue(), true);
                           }
                        }

                        if (this.placeTimer.passedMs(this.placeDelay.getValue()) && BlockUtil.canPlace(this.breakPos) && mc.currentScreen == null) {
                           if (this.enderChest.isPressed()) {
                              int var9 = this.findBlock(Blocks.ENDER_CHEST);
                              if (var9 != -1) {
                                 int var14 = mc.player.getInventory().selectedSlot;
                                 this.doSwap(var9, var9);
                                 BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                 this.doSwap(var14, var9);
                                 this.placeTimer.reset();
                              }
                           } else if (this.obsidian.isPressed()) {
                              int var10 = this.findBlock(Blocks.OBSIDIAN);
                              if (var10 != -1) {
                                 boolean var15 = false;
                                 if (this.shouldCrystal()) {
                                    for (Entity var24 : BlockUtil.getEntities(new Box(this.breakPos.up()))) {
                                       if (var24 instanceof EndCrystalEntity) {
                                          var15 = true;
                                          break;
                                       }
                                    }
                                 }

                                 if (!var15 || this.spamPlace.getValue()) {
                                    int var21 = mc.player.getInventory().selectedSlot;
                                    this.doSwap(var10, var10);
                                    BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                    this.doSwap(var21, var10);
                                    this.placeTimer.reset();
                                 }
                              }
                           }
                        }

                        this.breakNumber = 0;
                     }

                     if (this.waitPlace.getValue()) {
                        for (Direction var25 : Direction.values()) {
                           if (this.breakPos.offset(var25).equals(AutoCrystal.INSTANCE.crystalPos)) {
                              if (AutoCrystal.INSTANCE.canPlaceCrystal(this.breakPos, false, false)) {
                                 return;
                              }
                              break;
                           }
                        }
                     }

                     if (this.delayTimer.passed((long)this.stopDelay.getValue())) {
                        if (this.startPacket) {
                           if (this.isAir(this.breakPos)) {
                              return;
                           }

                           if (this.onlyGround.getValue() && !mc.player.isOnGround()) {
                              return;
                           }

                           if (this.mineTimer.passed((long)this.breakFinalTime)) {
                              if (this.endRotate.getValue()
                                 && this.shouldYawStep()
                                 && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                                 return;
                              }

                              int var12 = mc.player.getInventory().selectedSlot;
                              boolean var17;
                              if (this.hotBar.getValue()) {
                                 var17 = var7 != var12;
                              } else {
                                 if (var7 < 9) {
                                    var7 += 36;
                                 }

                                 var17 = var12 + 36 != var7;
                              }

                              if (var17) {
                                 if (this.hotBar.getValue()) {
                                    InventoryUtil.switchToSlot(var7);
                                 } else {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var7, var12, SlotActionType.SWAP, mc.player);
                                 }
                              }

                              int var23 = var7;
                              this.switchBack = () -> {
                                 if (!this.endRotate.getValue()
                                    || this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                                    sendSequencedPacket(
                                       var1xx -> new PlayerActionC2SPacket(
                                          Action.STOP_DESTROY_BLOCK, this.breakPos, BlockUtil.getClickSide(this.breakPos), var1xx
                                       )
                                    );
                                    if (this.endSwing.getValue()) {
                                       EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                                    }

                                    if (var17) {
                                       if (this.hotBar.getValue()) {
                                          InventoryUtil.switchToSlot(var12);
                                       } else {
                                          mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var23, var12, SlotActionType.SWAP, mc.player);
                                          EntityUtil.syncInventory();
                                       }
                                    }

                                    this.breakNumber++;
                                    this.delayTimer.reset();
                                    this.startTime.reset();
                                    if (this.afterBreak.getValue() && this.shouldCrystal()) {
                                       for (Direction var7x : Direction.values()) {
                                          CombatUtil.attackCrystal(this.breakPos.offset(var7x), this.placeRotate.getValue(), true);
                                       }
                                    }

                                    if (this.setAir.getValue()) {
                                       mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                                    }

                                    if (this.endRotate.getValue() && !this.shouldYawStep()) {
                                       Alien.ROTATION.snapBack();
                                    }

                                    ghost = true;
                                 } else if (var17) {
                                    if (this.hotBar.getValue()) {
                                       InventoryUtil.switchToSlot(var12);
                                    } else {
                                       mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, var23, var12, SlotActionType.SWAP, mc.player);
                                       EntityUtil.syncInventory();
                                    }
                                 }
                              };
                              if (!this.noGhostHand.getValue()) {
                                 this.switchBack.run();
                                 this.switchBack = null;
                              }
                           }
                        } else {
                           if (!this.startTime.passed(this.startDelay.getValueInt())) {
                              return;
                           }

                           if (!this.mineAir.getValue() && this.isAir(this.breakPos)) {
                              return;
                           }

                           Direction var13 = BlockUtil.getClickSide(this.breakPos);
                           if (this.rotate.getValue()) {
                              Vec3i var18 = var13.getVector();
                              if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d(var18.getX() * 0.5, var18.getY() * 0.5, var18.getZ() * 0.5)))) {
                                 return;
                              }
                           }

                           this.mineTimer.reset();
                           this.animationTime.reset();
                           if (this.swing.getValue()) {
                              EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                           }

                           if (this.doubleBreak.getValue()) {
                              if (secondPos == null || this.isAir(secondPos)) {
                                 double var19 = this.getBreakTime(this.breakPos, var7, 1.0);
                                 this.secondAnim.reset();
                                 this.secondAnim.setLength((long)var19);
                                 this.secondTimer.reset();
                                 secondPos = this.breakPos;
                              }

                              this.doDoubleBreak(var13);
                           }

                           sendSequencedPacket(var2x -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, var13, var2x));
                           if (this.rotate.getValue() && !this.shouldYawStep()) {
                              Alien.ROTATION.snapBack();
                           }

                           this.startTime.reset();
                        }
                     }
                  }
               } else {
                  if (this.breakPos.equals(secondPos)) {
                     secondPos = null;
                  }

                  this.startPacket = false;
                  ghost = false;
                  complete = false;
                  this.breakNumber = 0;
                  this.breakPos = null;
               }
            }
         }
      }
   }

   private void breakBlock(BlockPos var1) {
      mc.world.getBlockState(var1).getBlock().onBreak(mc.world, var1, mc.world.getBlockState(var1), mc.player);
   }

   void doDoubleBreak(Direction var1) {
      sendSequencedPacket(var2 -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, var1, var2));
      sendSequencedPacket(var2 -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, var1, var2));
   }

   boolean placeCrystal() {
      int var1 = this.findCrystal();
      if (var1 != -1) {
         int var2 = mc.player.getInventory().selectedSlot;
         this.doSwap(var1, var1);
         BlockUtil.placeCrystal(this.breakPos.up(), this.placeRotate.getValue());
         this.doSwap(var2, var1);
         this.placeTimer.reset();
         return !this.waitPlace.getValue();
      } else {
         return true;
      }
   }

   @EventListener
   public void onAttackBlock(ClickBlockEvent var1) {
      if (!nullCheck() && !mc.player.isCreative()) {
         var1.cancel();
         BlockPos var2 = var1.getPos();
         if (!var2.equals(this.breakPos)
            && !unbreakable(var2)
            && (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB)
            && (this.breakPos == null || !this.preferHead.getValue() || !mc.player.isCrawling() || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos))
            && BlockUtil.getClickSideStrict(var2) != null
            && !(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(var2.toCenterPos())) > this.range.getValue())) {
            this.breakPos = var2;
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.mineTimer.reset();
            this.animationTime.reset();
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               Direction var3 = BlockUtil.getClickSide(this.breakPos);
               if (this.rotate.getValue()) {
                  Vec3i var4 = var3.getVector();
                  if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d(var4.getX() * 0.5, var4.getY() * 0.5, var4.getZ() * 0.5)))) {
                     return;
                  }
               }

               if (this.startTime.passed(this.startDelay.getValueInt())) {
                  if (this.swing.getValue()) {
                     EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                  }

                  if (this.doubleBreak.getValue()) {
                     if (secondPos == null || this.isAir(secondPos)) {
                        int var5 = this.getTool(this.breakPos);
                        if (var5 == -1) {
                           var5 = mc.player.getInventory().selectedSlot;
                        }

                        this.secondFinalTime = this.getBreakTime(this.breakPos, var5, 1.0);
                        this.secondAnim.reset();
                        this.secondAnim.setLength((long)this.secondFinalTime);
                        this.secondTimer.reset();
                        secondPos = this.breakPos;
                     }

                     this.doDoubleBreak(var3);
                  }

                  int var6 = this.getTool(this.breakPos);
                  if (var6 == -1) {
                     var6 = mc.player.getInventory().selectedSlot;
                  }

                  this.breakFinalTime = this.getBreakTime(this.breakPos, var6);
                  sendSequencedPacket(var2x -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, var3, var2x));
                  if (this.rotate.getValue() && !this.shouldYawStep()) {
                     Alien.ROTATION.snapBack();
                  }

                  this.startTime.reset();
               }
            }
         }
      }
   }

   public void mine(BlockPos var1) {
      if (!nullCheck()) {
         if (mc.player.isCreative()) {
            mc.interactionManager.attackBlock(var1, BlockUtil.getClickSide(var1));
         } else if (this.isOff()) {
            mc.interactionManager.attackBlock(var1, BlockUtil.getClickSide(var1));
         } else if (!var1.equals(this.breakPos)
            && !unbreakable(var1)
            && (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB)
            && (this.breakPos == null || !this.preferHead.getValue() || !mc.player.isCrawling() || !EntityUtil.getPlayerPos(true).up().equals(this.breakPos))
            && BlockUtil.getClickSideStrict(var1) != null
            && !(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(var1.toCenterPos())) > this.range.getValue())) {
            this.breakPos = var1;
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.mineTimer.reset();
            this.animationTime.reset();
         }
      }
   }

   boolean faceVector(Vec3d var1) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(var1);
         return true;
      } else {
         this.sync.reset();
         this.directionVec = var1;
         return Alien.ROTATION.inFov(var1, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (this.breakPos != null && mc.world.isAir(this.breakPos)) {
         complete = true;
      }

      if (!mc.player.isCreative()) {
         if (secondPos != null) {
            if (this.isAir(secondPos)) {
               secondPos = null;
               return;
            }

            if (!this.checkDouble.getValue() || !secondPos.equals(this.breakPos)) {
               this.secondAnim.setLength((long)this.secondFinalTime);
               double var2 = this.secondAnim.ease(this.ease.getValue());
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(var1, this.getFillBox(secondPos, var2), this.doubleColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(var1, this.getOutlineBox(secondPos, var2), this.doubleOutlineColor.getValue());
               }
            }
         }

         if (this.breakPos != null) {
            progress = this.mineTimer.getMs() / this.breakFinalTime;
            this.animationTime.setLength((long)this.breakFinalTime);
            double var4 = this.animationTime.ease(this.ease.getValue());
            if (unbreakable(this.breakPos)) {
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(var1, new Box(this.breakPos), this.startColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(var1, new Box(this.breakPos), this.startOutlineColor.getValue());
               }

               return;
            }

            if (this.box.getValue()) {
               Render3DUtil.drawFill(var1, this.getFillBox(this.breakPos, var4), this.getColor(this.animationTime.ease(this.fadeEase.getValue())));
            }

            if (this.outline.getValue()) {
               Render3DUtil.drawBox(var1, this.getOutlineBox(this.breakPos, var4), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue())));
            }

            if (this.spinner.getValue()) {
               this.drawSpinner(this.breakPos);
            }

            if (this.text.getValue()) {
               if (this.isAir(this.breakPos)) {
                  Render3DUtil.drawText3D("Waiting", this.breakPos.toCenterPos(), -1);
               } else if ((int)this.mineTimer.getMs() < this.breakFinalTime) {
                  Render3DUtil.drawText3D(this.df.format(progress * 100.0) + "%", this.breakPos.toCenterPos(), -1);
               } else {
                  Render3DUtil.drawText3D("100.0%", this.breakPos.toCenterPos(), -1);
               }
            }
         } else {
            progress = 0.0;
         }
      } else {
         progress = 0.0;
      }
   }

   private void drawSpinner(BlockPos pos) {
      double centerX = pos.toCenterPos().x;
      double centerY = pos.toCenterPos().y;
      double centerZ = pos.toCenterPos().z;
      float radius = (float)(this.spinnerSize.getValue() * 0.45);
      float thickness = (float)(this.spinnerThickness.getValue() * 0.02);
      double time = System.currentTimeMillis() / 1000.0 * this.spinnerSpeed.getValue();
      int segments = 48;
      float step = (float)(Math.PI * 2.0 / segments);
      float head = (float)(time * Math.PI * 2.0 % (Math.PI * 2.0));
      int r = this.spinnerColor.getValue().getRed();
      int g = this.spinnerColor.getValue().getGreen();
      int b = this.spinnerColor.getValue().getBlue();
      MatrixStack matrices = Render3DUtil.matrixFrom(centerX, centerY, centerZ);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      for (int i = 0; i < segments; i++) {
         float start = i * step;
         float end = start + step * 1.15F;
         float offset = (head - start + (float)(Math.PI * 2.0)) % (float)(Math.PI * 2.0);
         float fade = Math.max(0.0F, 1.0F - offset / 1.7F);
         fade = fade * fade;
         if (fade <= 0.02) {
            continue;
         }

         int alpha = (int)(fade * 255.0F);
         float innerStartX = MathHelper.cos(start) * (radius - thickness);
         float innerStartY = MathHelper.sin(start) * (radius - thickness);
         float outerStartX = MathHelper.cos(start) * radius;
         float outerStartY = MathHelper.sin(start) * radius;
         float innerEndX = MathHelper.cos(end) * (radius - thickness);
         float innerEndY = MathHelper.sin(end) * (radius - thickness);
         float outerEndX = MathHelper.cos(end) * radius;
         float outerEndY = MathHelper.sin(end) * radius;
         buffer.vertex(matrix, innerStartX, innerStartY, 0.0F).color(r, g, b, alpha);
         buffer.vertex(matrix, outerStartX, outerStartY, 0.0F).color(r, g, b, alpha);
         buffer.vertex(matrix, outerEndX, outerEndY, 0.0F).color(r, g, b, alpha);
         buffer.vertex(matrix, innerEndX, innerEndY, 0.0F).color(r, g, b, alpha);
      }

      Render3DUtil.endBuilding(buffer);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
   }

   private Box getFillBox(BlockPos var1, double var2) {
      switch ((PacketMine.Animation)this.animation.getValue()) {
         case Center:
            var2 = (1.0 - var2) / 2.0;
            return new Box(var1).shrink(var2, var2, var2).shrink(-var2, -var2, -var2);
         case Grow:
            var2 = (1.0 - var2) / 2.0;
            return new Box(var1).shrink(var2, 0.0, var2).shrink(-var2, 0.0, -var2);
         case Up:
            return new Box(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1, var1.getY() + var2, var1.getZ() + 1);
         case Down:
            return new Box(var1.getX(), var1.getY() + 1 - var2, var1.getZ(), var1.getX() + 1, var1.getY() + 1, var1.getZ() + 1);
         case Oscillation:
            return new Box(var1).shrink(var2, var2, var2).shrink(-var2, -var2, -var2);
         default:
            return new Box(var1);
      }
   }

   private Box getOutlineBox(BlockPos var1, double var2) {
      var2 = Math.min(var2 + this.expandLine.getValue(), 1.0);
      switch ((PacketMine.Animation)this.animation.getValue()) {
         case Center:
            var2 = (1.0 - var2) / 2.0;
            return new Box(var1).shrink(var2, var2, var2).shrink(-var2, -var2, -var2);
         case Grow:
            var2 = (1.0 - var2) / 2.0;
            return new Box(var1).shrink(var2, 0.0, var2).shrink(-var2, 0.0, -var2);
         case Up:
            return new Box(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1, var1.getY() + var2, var1.getZ() + 1);
         case Down:
            return new Box(var1.getX(), var1.getY() + 1 - var2, var1.getZ(), var1.getX() + 1, var1.getY() + 1, var1.getZ() + 1);
         case Oscillation:
            return new Box(var1).shrink(var2, var2, var2).shrink(-var2, -var2, -var2);
         default:
            return new Box(var1);
      }
   }

   @EventListener(
      priority = -200
   )
   public void onPacketSend(PacketEvent.Send var1) {
      if (!nullCheck() && !mc.player.isCreative()) {
         if (var1.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.bypassGround.getValue()
               && !mc.player.isFallFlying()
               && this.breakPos != null
               && !this.isAir(this.breakPos)
               && this.bypassTime.getValue() > 0.0
               && MathHelper.sqrt((float)this.breakPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= this.range.getValueFloat() + 2.0F) {
               double var4 = this.breakFinalTime - this.bypassTime.getValue();
               if (var4 <= 0.0 || this.mineTimer.passed((long)var4)) {
                  this.sendGroundPacket = true;
                  ((IPlayerMoveC2SPacket)var1.getPacket()).setOnGround(true);
               }
            } else {
               this.sendGroundPacket = false;
            }
         } else if (var1.getPacket() instanceof UpdateSelectedSlotC2SPacket var2) {
            if (var2.getSelectedSlot() != this.lastSlot) {
               this.lastSlot = var2.getSelectedSlot();
               if (this.switchReset.getValue()) {
                  this.startPacket = false;
                  ghost = false;
                  complete = false;
                  this.mineTimer.reset();
                  this.animationTime.reset();
               }
            }
         } else if (var1.getPacket() instanceof PlayerActionC2SPacket var3) {
            if (var3.getAction() == Action.START_DESTROY_BLOCK) {
               if (this.breakPos == null || !var3.getPos().equals(this.breakPos)) {
                  return;
               }

               if (this.grimDisabler.getValue()) {
                  mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, var3.getPos(), var3.getDirection()));
               }

               this.startPacket = true;
            } else if (var3.getAction() == Action.STOP_DESTROY_BLOCK) {
               if (this.breakPos == null || !var3.getPos().equals(this.breakPos)) {
                  return;
               }

               if (!this.instant.getValue()) {
                  this.startPacket = false;
                  ghost = false;
                  complete = false;
               }
            }
         }
      }
   }

   boolean canPlaceCrystal(BlockPos var1) {
      BlockPos var2 = var1.down();
      BlockPos var3 = var2.up();
      return (BlockUtil.getBlock(var2) == Blocks.BEDROCK || BlockUtil.getBlock(var2) == Blocks.OBSIDIAN)
         && BlockUtil.getClickSideStrict(var2) != null
         && this.noEntity(var3)
         && this.noEntity(var3.up())
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(var3.up()));
   }

   boolean noEntity(BlockPos var1) {
      for (Entity var3 : BlockUtil.getEntities(new Box(var1))) {
         if (!(var3 instanceof ItemEntity) && (!(var3 instanceof ArmorStandEntity) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue())) {
            return false;
         }
      }

      return true;
   }

   void doSwap(int var1, int var2) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(var1);
      } else {
         InventoryUtil.inventorySwap(var2, mc.player.getInventory().selectedSlot);
      }
   }

   int findCrystal() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
   }

   int findBlock(Block var1) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(var1) : InventoryUtil.findBlock(var1);
   }

   boolean shouldCrystal() {
      return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
   }

   public static double getBreakTime(BlockPos var0) {
      int var1 = INSTANCE.getTool(var0);
      if (var1 == -1) {
         var1 = mc.player.getInventory().selectedSlot;
      }

      return INSTANCE.getBreakTime(var0, var1);
   }

   double getBreakTime(BlockPos var1, int var2) {
      return this.getBreakTime(var1, var2, this.damage.getValue());
   }

   double getBreakTime(BlockPos var1, int var2, double var3) {
      return 1.0F / this.getBlockStrength(var1, mc.player.getInventory().getStack(var2)) / 20.0F * 1000.0F * var3;
   }

   float getBlockStrength(BlockPos var1, ItemStack var2) {
      BlockState var3 = mc.world.getBlockState(var1);
      float var4 = var3.getHardness(mc.world, var1);
      if (var4 < 0.0F) {
         return 0.0F;
      } else {
         float var5 = var3.isToolRequired() && !var2.isSuitableFor(var3) ? 100.0F : 30.0F;
         return this.getDigSpeed(var3, var2) / var4 / var5;
      }
   }

   float getDigSpeed(BlockState var1, ItemStack var2) {
      float var3 = this.getDestroySpeed(var1, var2);
      if (var3 > 1.0F) {
         int var4 = EnchantmentHelper.getLevel(
            (RegistryEntry)mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(),
            var2
         );
         if (var4 > 0 && !var2.isEmpty()) {
            var3 += (float)(StrictMath.pow(var4, 2.0) + 1.0);
         }
      }

      if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         var3 *= 1.0F + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         var3 *= switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      if (mc.player.isSubmergedInWater()) {
         var3 *= (float)mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();
      }

      boolean var5 = this.checkWeb.getValue() && Alien.PLAYER.isInWeb(mc.player) && mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
      if ((!mc.player.isOnGround() || var5)
         && INSTANCE.checkGround.getValue()
         && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || mc.player.isFallFlying() || var5)) {
         var3 /= 5.0F;
      }

      return var3 < 0.0F ? 0.0F : var3;
   }

   float getDestroySpeed(BlockState var1, ItemStack var2) {
      float var3 = 1.0F;
      if (var2 != null && !var2.isEmpty()) {
         var3 *= var2.getMiningSpeedMultiplier(var1);
      }

      return var3;
   }

   Color getColor(double var1) {
      int var3 = this.startColor.getValue().getRed();
      int var4 = this.startColor.getValue().getGreen();
      int var5 = this.startColor.getValue().getBlue();
      int var6 = this.startColor.getValue().getAlpha();
      int var7 = this.endColor.getValue().getRed();
      int var8 = this.endColor.getValue().getGreen();
      int var9 = this.endColor.getValue().getBlue();
      int var10 = this.endColor.getValue().getAlpha();
      return new Color(
         (int)(var3 + (var7 - var3) * var1), (int)(var4 + (var8 - var4) * var1), (int)(var5 + (var9 - var5) * var1), (int)(var6 + (var10 - var6) * var1)
      );
   }

   Color getOutlineColor(double var1) {
      int var3 = this.startOutlineColor.getValue().getRed();
      int var4 = this.startOutlineColor.getValue().getGreen();
      int var5 = this.startOutlineColor.getValue().getBlue();
      int var6 = this.startOutlineColor.getValue().getAlpha();
      int var7 = this.endOutlineColor.getValue().getRed();
      int var8 = this.endOutlineColor.getValue().getGreen();
      int var9 = this.endOutlineColor.getValue().getBlue();
      int var10 = this.endOutlineColor.getValue().getAlpha();
      return new Color(
         (int)(var3 + (var7 - var3) * var1), (int)(var4 + (var8 - var4) * var1), (int)(var5 + (var9 - var5) * var1), (int)(var6 + (var10 - var6) * var1)
      );
   }

   int getTool(BlockPos var1) {
      if (this.hotBar.getValue()) {
         int var8 = -1;
         float var9 = 1.0F;

         for (int var10 = 0; var10 < 9; var10++) {
            ItemStack var11 = mc.player.getInventory().getStack(var10);
            if (var11 != ItemStack.EMPTY) {
               float var12 = EnchantmentHelper.getLevel(
                  (RegistryEntry)mc.world
                     .getRegistryManager()
                     .getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef())
                     .getOptional(Enchantments.EFFICIENCY)
                     .get(),
                  var11
               );
               float var13 = var11.getMiningSpeedMultiplier(mc.world.getBlockState(var1));
               if (var12 + var13 > var9) {
                  var9 = var12 + var13;
                  var8 = var10;
               }
            }
         }

         return var8;
      } else {
         AtomicInteger var2 = new AtomicInteger();
         var2.set(-1);
         float var3 = 1.0F;

         for (Entry var5 : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (!(((ItemStack)var5.getValue()).getItem() instanceof AirBlockItem)) {
               float var6 = EnchantmentHelper.getLevel(
                  (RegistryEntry)mc.world
                     .getRegistryManager()
                     .getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef())
                     .getOptional(Enchantments.EFFICIENCY)
                     .get(),
                  (ItemStack)var5.getValue()
               );
               float var7 = ((ItemStack)var5.getValue()).getMiningSpeedMultiplier(mc.world.getBlockState(var1));
               if (var6 + var7 > var3) {
                  var3 = var6 + var7;
                  var2.set((Integer)var5.getKey());
               }
            }
         }

         return var2.get();
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   boolean isAir(BlockPos var1) {
      return mc.world.isAir(var1) || BlockUtil.getBlock(var1) == Blocks.FIRE && BlockUtil.hasCrystal(var1);
   }

   public static boolean unbreakable(BlockPos var0) {
      Block var1 = mc.world.getBlockState(var0).getBlock();
      return !(var1 instanceof AirBlock) && (var1.getHardness() == -1.0F || var1.getHardness() == 100.0F);
   }

   private static enum Animation {
      Center,
      Grow,
      Up,
      Down,
      Oscillation,
      None;
   }

   public static enum Page {
      General,
      Check,
      Place,
      Rotation,
      Render;
   }
}
