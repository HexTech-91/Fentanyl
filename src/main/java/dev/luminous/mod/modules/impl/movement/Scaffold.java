package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class Scaffold extends Module {
   private static Vec3d lastVec3d;
   public final SliderSetting rotateTime = this.add(new SliderSetting("RotateTime", 1000.0, 0.0, 3000.0, 10.0));
   private final BooleanSetting tower;
   private final BooleanSetting packetPlace;
   private final BooleanSetting safeWalk;
   private final BooleanSetting rotate;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final BooleanSetting render;
   public final ColorSetting color;
   public final ColorSetting outlineColor;
   public final SliderSetting sliderSpeed;
   private final BooleanSetting esp;
   private final BooleanSetting fill;
   private final BooleanSetting outline;
   private final Timer timer;
   private final Timer towerTimer;
   private Vec3d vec;
   private BlockPos pos;

   public Scaffold() {
      super("Scaffold", Module.Category.Movement);
      this.rotateTime.setChinese("\u4fdd\u6301\u65cb\u8f6c");
      this.tower = this.add(new BooleanSetting("Tower", true));
      this.tower.setChinese("\u642d\u9ad8");
      this.packetPlace = this.add(new BooleanSetting("PacketPlace", false));
      this.packetPlace.setChinese("\u53d1\u5305\u653e\u7f6e");
      this.safeWalk = this.add(new BooleanSetting("SafeWalk", false));
      this.safeWalk.setChinese("\u8fb9\u7f18\u884c\u8d70");
      this.rotate = this.add(new BooleanSetting("Rotate", true).setParent());
      this.rotate.setChinese("\u65cb\u8f6c");
      this.yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.rotate.isOpen()).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(new BooleanSetting("WhenElytra", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.rotate.isOpen()));
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(new BooleanSetting("CheckFov", true, () -> this.rotate.isOpen() && this.yawStep.isOpen()).setParent());
      this.checkFov.setChinese("\u4ec5\u770b\u65b9\u5757");
      this.fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.isOpen() && this.rotate.isOpen() && this.yawStep.isOpen()));
      this.fov.setChinese("\u89c6\u91ce");
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen()));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.render = this.add(new BooleanSetting("Render", true).setParent());
      this.render.setChinese("\u6e32\u67d3");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100), this.render::isOpen));
      this.color.setChinese("\u989c\u8272");
      this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 100), this.render::isOpen));
      this.outlineColor.setChinese("\u8f6e\u5ed3\u989c\u8272");
      this.sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.2, 0.01, 1.0, 0.01, this.render::isOpen));
      this.sliderSpeed.setChinese("\u6ed1\u52a8\u901f\u5ea6");
      this.esp = this.add(new BooleanSetting("ESP", true, this.render::isOpen));
      this.fill = this.add(new BooleanSetting("Fill", true, this.render::isOpen));
      this.fill.setChinese("\u586b\u5145");
      this.outline = this.add(new BooleanSetting("Outline", true, this.render::isOpen));
      this.outline.setChinese("\u8fb9\u6846");
      this.timer = new Timer();
      this.towerTimer = new Timer();
      this.setChinese("\u81ea\u52a8\u642d\u8def");
      this.esp.setChinese("\u9ad8\u4eae\u663e\u793a");
   }

   @EventListener(
      priority = -100
   )
   public void onMove(MoveEvent event) {
      if (this.safeWalk.getValue()) {
         SafeWalk.INSTANCE.onMove(event);
      }
   }

   @EventListener
   public void onRotation(RotationEvent event) {
      if (this.rotate.getValue() && !this.timer.passed(this.rotateTime.getValueInt()) && this.vec != null) {
         event.setTarget(this.vec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @Override
   public void onEnable() {
      lastVec3d = null;
      this.pos = null;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.render.getValue()) {
         if (this.esp.getValue()) {
            GL11.glEnable(3042);
            double temp = 0.01;

            for (double i = 0.0; i < 0.8; i += temp) {
               HoleSnap.drawCircle(
                  matrixStack,
                  ColorUtil.injectAlpha(this.color.getValue(), (int)Math.min(this.color.getValue().getAlpha() * 2 / (0.8 / temp), 255.0)),
                  i,
                  new Vec3d(
                     MathUtil.interpolate(mc.player.lastRenderX, mc.player.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                     MathUtil.interpolate(mc.player.lastRenderY, mc.player.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                     MathUtil.interpolate(mc.player.lastRenderZ, mc.player.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                  ),
                  5
               );
            }

            GL11.glDisable(3042);
         }

         if (this.pos != null) {
            Vec3d cur = this.pos.toCenterPos();
            if (lastVec3d == null) {
               lastVec3d = cur;
            } else {
               lastVec3d = new Vec3d(
                  AnimateUtil.animate(lastVec3d.getX(), cur.x, this.sliderSpeed.getValue()),
                  AnimateUtil.animate(lastVec3d.getY(), cur.y, this.sliderSpeed.getValue()),
                  AnimateUtil.animate(lastVec3d.getZ(), cur.z, this.sliderSpeed.getValue())
               );
            }

            Render3DUtil.draw3DBox(
               matrixStack,
               new Box(lastVec3d.add(0.5, 0.5, 0.5), lastVec3d.add(-0.5, -0.5, -0.5)),
               ColorUtil.injectAlpha(this.color.getValue(), this.color.getValue().getAlpha()),
               ColorUtil.injectAlpha(this.outlineColor.getValue(), this.outlineColor.getValue().getAlpha()),
               this.outline.getValue(),
               this.fill.getValue()
            );
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      int block = InventoryUtil.findBlock();
      if (block != -1) {
         BlockPos placePos = mc.player.getBlockPos().down();
         if (BlockUtil.clientCanPlace(placePos, false)) {
            int old = mc.player.getInventory().selectedSlot;
            if (BlockUtil.getPlaceSide(placePos) == null) {
               double distance = 1000.0;
               BlockPos bestPos = null;

               for (Direction i : Direction.values()) {
                  if (i != Direction.UP
                     && BlockUtil.canPlace(placePos.offset(i))
                     && (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < distance)) {
                     bestPos = placePos.offset(i);
                     distance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                  }
               }

               if (bestPos == null) {
                  return;
               }

               placePos = bestPos;
            }

            if (this.rotate.getValue()) {
               Direction side = BlockUtil.getPlaceSide(placePos);
               this.vec = placePos.offset(side)
                  .toCenterPos()
                  .add(side.getOpposite().getVector().getX() * 0.5, side.getOpposite().getVector().getY() * 0.5, side.getOpposite().getVector().getZ() * 0.5);
               this.timer.reset();
               if (!this.faceVector(this.vec)) {
                  return;
               }
            }

            InventoryUtil.switchToSlot(block);
            BlockUtil.placeBlock(placePos, false, this.packetPlace.getValue());
            InventoryUtil.switchToSlot(old);
            if (this.rotate.getValue()) {
               Alien.ROTATION.snapBack();
            }

            this.pos = placePos;
            if (this.tower.getValue() && mc.options.jumpKey.isPressed() && !MovementUtil.isMoving()) {
               MovementUtil.setMotionY(0.42);
               MovementUtil.setMotionX(0.0);
               MovementUtil.setMotionZ(0.0);
               if (this.towerTimer.passed(1500L)) {
                  MovementUtil.setMotionY(-0.28);
                  this.towerTimer.reset();
               }
            } else {
               this.towerTimer.reset();
            }
         }
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   private boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }
}
