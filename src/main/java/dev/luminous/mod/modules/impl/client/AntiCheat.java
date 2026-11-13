package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.MovementSync;
import dev.luminous.mod.modules.settings.enums.Placement;
import dev.luminous.mod.modules.settings.enums.SnapBack;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class AntiCheat extends Module {
   public static AntiCheat INSTANCE;
   public final EnumSetting<AntiCheat.Page> page = this.add(new EnumSetting<>("\u9875\u9762", AntiCheat.Page.General));
   public final BooleanSetting attackCDFix = this.add(new BooleanSetting("AttackCDFix", false, () -> this.page.is(AntiCheat.Page.General)));
   public final BooleanSetting multiPlace;
   public final BooleanSetting packetPlace;
   public final BooleanSetting attackRotate;
   public final BooleanSetting invSwapBypass;
   public final BooleanSetting priorHotbar;
   public final SliderSetting ieRange;
   public final SliderSetting boxSize;
   public final SliderSetting attackDelay;
   public final BooleanSetting noBadSlot;
   public final EnumSetting<Placement> placement;
   public final BooleanSetting upDirectionLimit;
   public final EnumSetting<SwingSide> interactSwing;
   public final EnumSetting<SwingSide> attackSwing;
   public final BooleanSetting grimRotation;
   public final EnumSetting<SnapBack> snapBackEnum;
   public final BooleanSetting look;
   public final SliderSetting rotateTime;
   public final BooleanSetting random;
   public final SliderSetting steps;
   public final BooleanSetting serverSide;
   public final BooleanSetting fullPackets;
   public final BooleanSetting force;
   public final BooleanSetting forceSync;
   public final BooleanSetting interactRotation;
   public final BooleanSetting detectDouble;
   public final SliderSetting doubleMineTimeout;
   public final SliderSetting minTimeout;
   public final SliderSetting breakTimeout;
   public final BooleanSetting ignoreArmorStand;
   public final BooleanSetting closeScreen;
   public final BooleanSetting inventorySync;
   public final EnumSetting<AntiCheat.Motion> motion;
   public final SliderSetting predictTicks;
   public final SliderSetting simulation;
   public final SliderSetting maxMotionY;
   public final BooleanSetting step;
   public final BooleanSetting doubleStep;
   public final BooleanSetting jump;
   public final BooleanSetting inBlockPause;

   public AntiCheat() {
      super("AntiCheat", Module.Category.Client);
      this.attackCDFix.setChinese("\u771f\u5b9e\u653b\u51fb\u51b7\u5374");
      this.multiPlace = this.add(new BooleanSetting("MultiPlace", true, () -> this.page.is(AntiCheat.Page.General)));
      this.multiPlace.setChinese("\u591a\u91cd\u653e\u7f6e");
      this.packetPlace = this.add(new BooleanSetting("PacketPlace", true, () -> this.page.is(AntiCheat.Page.General)));
      this.packetPlace.setChinese("\u53d1\u5305\u653e\u7f6e");
      this.attackRotate = this.add(new BooleanSetting("AttackRotate", false, () -> this.page.is(AntiCheat.Page.General)));
      this.attackRotate.setChinese("\u653b\u51fb\u65cb\u8f6c");
      this.invSwapBypass = this.add(new BooleanSetting("InvSwapBypass", false, () -> this.page.is(AntiCheat.Page.General)));
      this.invSwapBypass.setChinese("\u62fe\u53d6\u4ea4\u6362");
      this.priorHotbar = this.add(new BooleanSetting("PriorHotbar", false, () -> this.page.is(AntiCheat.Page.General)));
      this.priorHotbar.setChinese("\u4f18\u5148\u70ed\u952e\u680f");
      this.ieRange = this.add(new SliderSetting("IeRange", 3.0, 0.0, 8.0, 0.1, () -> this.page.is(AntiCheat.Page.General)));
      this.ieRange.setChinese("\u5b9e\u4f53\u4ea4\u4e92\u8303\u56f4");
      this.boxSize = this.add(new SliderSetting("BoxSize", 0.6, 0.0, 1.0, 0.01, () -> this.page.is(AntiCheat.Page.General)));
      this.boxSize.setChinese("\u78b0\u649e\u7bb1\u5927\u5c0f");
      this.attackDelay = this.add(new SliderSetting("AttackDelay", 0.2, 0.0, 1.0, 0.01, () -> this.page.is(AntiCheat.Page.General)).setSuffix("s"));
      this.attackDelay.setChinese("\u6316\u6398\u5ef6\u8fdf");
      this.noBadSlot = this.add(new BooleanSetting("NoBadSlot", false, () -> this.page.is(AntiCheat.Page.General)));
      this.noBadSlot.setChinese("\u65e0\u574f\u69fd\u4f4d");
      this.placement = this.add(new EnumSetting<>("\u653e\u7f6e\u65b9\u5f0f", Placement.Vanilla, () -> this.page.is(AntiCheat.Page.General)));
      this.upDirectionLimit = this.add(
         new BooleanSetting("UpDirectionLimit", true, () -> this.page.is(AntiCheat.Page.General) && this.placement.is(Placement.NCP))
      );
      this.upDirectionLimit.setChinese("\u5411\u4e0a\u65b9\u5411\u9650\u5236");
      this.interactSwing = this.add(new EnumSetting<>("\u4ea4\u4e92\u6325\u52a8", SwingSide.All, () -> this.page.is(AntiCheat.Page.General)));
      this.attackSwing = this.add(new EnumSetting<>("\u653b\u51fb\u6325\u52a8", SwingSide.All, () -> this.page.is(AntiCheat.Page.General)));
      this.grimRotation = this.add(new BooleanSetting("GrimRotation", false, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.grimRotation.setChinese("Grim\u65cb\u8f6c");
      this.snapBackEnum = this.add(new EnumSetting<>("\u56de\u5f39", SnapBack.None, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.look = this.add(new BooleanSetting("Look", true, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.look.setChinese("\u89c6\u89d2");
      this.rotateTime = this.add(new SliderSetting("RotateTime", 0.5, 0.0, 1.0, 0.01, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.rotateTime.setChinese("\u89c6\u89d2\u65f6\u95f4");
      this.random = this.add(new BooleanSetting("Random", true, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.random.setChinese("\u968f\u673a");
      this.steps = this.add(new SliderSetting("Steps", 0.6, 0.0, 1.0, 0.01, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.steps.setChinese("\u6b65\u957f");
      this.serverSide = this.add(new BooleanSetting("ServerSide", false, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.serverSide.setChinese("\u670d\u52a1\u7aef");
      this.fullPackets = this.add(new BooleanSetting("FullPackets", false, () -> this.page.is(AntiCheat.Page.Rotation)).setParent());
      this.fullPackets.setChinese("\u5b8c\u6574\u5305");
      this.force = this.add(new BooleanSetting("Force", false, () -> this.page.is(AntiCheat.Page.Rotation) && this.fullPackets.isOpen()));
      this.force.setChinese("\u59cb\u7ec8\u53d1\u9001");
      this.forceSync = this.add(new BooleanSetting("ForceSync", true, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.forceSync.setChinese("\u5f3a\u5236\u540c\u6b65");
      this.interactRotation = this.add(new BooleanSetting("InteractRotation", false, () -> this.page.is(AntiCheat.Page.Rotation)));
      this.interactRotation.setChinese("\u4ea4\u4e92\u65cb\u8f6c");
      this.detectDouble = this.add(new BooleanSetting("DetectDouble", true, () -> this.page.is(AntiCheat.Page.Misc)));
      this.detectDouble.setChinese("\u68c0\u6d4b\u53cc\u6316");
      this.doubleMineTimeout = this.add(new SliderSetting("DoubleMineTimeout", 2.0, 0.0, 3.0, 0.1, () -> this.page.is(AntiCheat.Page.Misc)).setSuffix("*"));
      this.doubleMineTimeout.setChinese("\u53cc\u6316\u8d85\u65f6");
      this.minTimeout = this.add(new SliderSetting("MinTimeout", 2.0, 0.0, 10.0, 0.1, () -> this.page.is(AntiCheat.Page.Misc)).setSuffix("s"));
      this.minTimeout.setChinese("\u6700\u5c0f\u8d85\u65f6");
      this.breakTimeout = this.add(new SliderSetting("BreakTimeout", 1.5, 0.0, 3.0, 0.1, () -> this.page.is(AntiCheat.Page.Misc)).setSuffix("*"));
      this.breakTimeout.setChinese("\u6316\u6398\u5931\u8d25");
      this.ignoreArmorStand = this.add(new BooleanSetting("IgnoreArmorStand", false, () -> this.page.is(AntiCheat.Page.Misc)));
      this.ignoreArmorStand.setChinese("\u5ffd\u7565\u76d4\u7532\u67b6");
      this.closeScreen = this.add(new BooleanSetting("CloseScreen", false, () -> this.page.is(AntiCheat.Page.Misc)));
      this.closeScreen.setChinese("\u5173\u95ed\u754c\u9762");
      this.inventorySync = this.add(new BooleanSetting("InventorySync", false, () -> this.page.is(AntiCheat.Page.Misc)));
      this.inventorySync.setChinese("\u80cc\u5305\u540c\u6b65");
      this.motion = this.add(new EnumSetting<>("\u8fd0\u52a8\u65b9\u5f0f", AntiCheat.Motion.Position, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 4, 0, 10, () -> this.page.getValue() == AntiCheat.Page.Predict).setSuffix("ticks"));
      this.predictTicks.setChinese("\u9884\u6d4b");
      this.simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.simulation.setChinese("\u6a21\u62df");
      this.maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.maxMotionY.setChinese("\u6700\u5927Y\u8f74\u8fd0\u52a8");
      this.step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.step.setChinese("\u6b65\u8fdb");
      this.doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.doubleStep.setChinese("\u53cc\u6b65\u8fdb");
      this.jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.jump.setChinese("\u8df3\u8dc3");
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AntiCheat.Page.Predict));
      this.inBlockPause.setChinese("\u65b9\u5757\u5185\u6682\u505c");
      this.setChinese("\u53cd\u4f5c\u5f0a\u9009\u9879");
      INSTANCE = this;
   }

   public boolean movementSync() {
      return MovementSync.INSTANCE.isOn();
   }

   public static double getOffset() {
      return INSTANCE != null ? INSTANCE.boxSize.getValue() / 2.0 : 0.3;
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }

   public static enum Motion {
      Velocity,
      Position;
   }

   public static enum Page {
      General,
      Rotation,
      Misc,
      Predict;
   }
}
