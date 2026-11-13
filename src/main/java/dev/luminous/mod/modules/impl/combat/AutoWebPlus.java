package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoWebPlus extends Module {
   private static final double POSITION_EPSILON = 1.0E-4;
   private static final double MAX_PREDICTED_MOTION = 1.2;
   public static AutoWebPlus INSTANCE;
   public static boolean force = false;
   public static boolean ignore = false;
   private static boolean swapTransaction;
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500));
   private final SliderSetting blocksPer;
   private final SliderSetting maxWebs;
   private final SliderSetting offset;
   private final SliderSetting placeRange;
   private final SliderSetting targetRange;
   private final SliderSetting predictTicks;
   private final BooleanSetting feet;
   private final BooleanSetting feetExtend;
   private final BooleanSetting face;
   private final BooleanSetting down;
   private final BooleanSetting eatingPause;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting selfAvoid;
   private final BooleanSetting render;
   private final ColorSetting renderFill;
   private final ColorSetting renderOutline;
   private final SliderSetting renderLineWidth;
   private final SliderSetting renderTime;
   private final SliderSetting renderHeight;
   private final Timer timer;
   private final Set<BlockPos> placedList;
   private final Map<BlockPos, Long> renderPositions;
   private long lastPlacementScan;
   private volatile boolean refillPause;
   private PlayerEntity currentTarget;
   private Vec3d predictedPos;
   public Vec3d directionVec;

   public AutoWebPlus() {
      super("AutoWebPlus", "\u9884\u6d4b\u76ee\u6807\u79fb\u52a8\u5e76\u6301\u7eed\u653e\u7f6e\u8718\u86db\u7f51", Module.Category.Combat);
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 2, 1, 10));
      this.blocksPer.setChinese("\u6bcfTick\u653e\u7f6e");
      this.maxWebs = this.add(new SliderSetting("MaxWebs", 3.0, 1.0, 8.0, 1.0));
      this.maxWebs.setChinese("\u6700\u5927\u8718\u86db\u7f51");
      this.offset = this.add(new SliderSetting("Offset", 0.2, 0.0, 0.3, 0.01));
      this.offset.setChinese("\u504f\u79fb\u91cf");
      this.placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.5, 6.0, 0.1));
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.5, 8.0, 0.1));
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 3.0, 0.0, 10.0, 1.0));
      this.predictTicks.setChinese("\u9884\u6d4b\u91cf");
      this.feet = this.add(new BooleanSetting("Feet", true));
      this.feet.setChinese("\u811a\u4e0b\u653e\u7f6e");
      this.feetExtend = this.add(new BooleanSetting("FeetExtend", true));
      this.feetExtend.setChinese("\u5ef6\u4f38\u811a\u4e0b");
      this.face = this.add(new BooleanSetting("Face", true));
      this.face.setChinese("\u5934\u90e8\u653e\u7f6e");
      this.down = this.add(new BooleanSetting("Down", true));
      this.down.setChinese("\u843d\u70b9\u653e\u7f6e");
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true));
      this.eatingPause.setChinese("\u8fdb\u98df\u6682\u505c");
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
      this.inventorySwap.setChinese("\u7269\u54c1\u680f\u4ea4\u6362");
      this.selfAvoid = this.add(new BooleanSetting("SelfAvoid", true));
      this.render = this.add(new BooleanSetting("Render", true));
      this.renderFill = this.add(new ColorSetting("RenderFill", new Color(120, 210, 255, 70)).injectBoolean(true));
      this.renderOutline = this.add(new ColorSetting("RenderOutline", new Color(210, 245, 255, 220)).injectBoolean(true));
      this.renderLineWidth = this.add(new SliderSetting("RenderLineWidth", 1.5, 0.1, 5.0, 0.1));
      this.renderTime = this.add(new SliderSetting("RenderTime", 450.0, 50.0, 2000.0, 10.0).setSuffix("ms"));
      this.renderHeight = this.add(new SliderSetting("RenderHeight", 1.0, 0.05, 1.0, 0.05));
      this.selfAvoid.setChinese("\u81ea\u6211\u907f\u8ba9");
      this.timer = new Timer();
      this.placedList = new LinkedHashSet<>();
      this.renderPositions = new LinkedHashMap<>();
      this.setChinese("\u81ea\u52a8\u8718\u86db\u7f51+");
      INSTANCE = this;
      this.render.setChinese("\u6e32\u67d3");
      this.renderFill.setChinese("\u586b\u5145\u989c\u8272");
      this.renderOutline.setChinese("\u8f6e\u5ed3\u989c\u8272");
      this.renderLineWidth.setChinese("\u8f6e\u5ed3\u7c97\u7ec6");
      this.renderTime.setChinese("\u663e\u793a\u65f6\u95f4");
      this.renderHeight.setChinese("\u6e32\u67d3\u9ad8\u5ea6");
   }

   public static boolean isSwapTransaction() {
      return swapTransaction;
   }

   public void setRefillPause(boolean paused) {
      this.refillPause = paused;
      if (paused) {
         force = false;
         this.currentTarget = null;
         this.predictedPos = null;
         this.directionVec = null;
      }
   }

   public boolean isRefillPaused() {
      return this.refillPause;
   }

   @Override
   public String getInfo() {
      return this.currentTarget == null ? null : this.currentTarget.getGameProfile().getName();
   }

   @Override
   public void onEnable() {
      this.resetRuntimeState();
   }

   @Override
   public void onDisable() {
      this.resetRuntimeState();
      force = false;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && !event.isPost()) {
         this.runPlacementCycle();
      }
   }

   private void runPlacementCycle() {
      if (!nullCheck()) {
         if (this.refillPause) {
            force = false;
         } else {
            long now = System.currentTimeMillis();
            if (now - this.lastPlacementScan >= 10L) {
               this.lastPlacementScan = now;
               if (!PearlTracker.isCombatSwapLocked()) {
                  this.currentTarget = this.findTarget();
                  if (this.currentTarget == null) {
                     this.directionVec = null;
                     this.predictedPos = null;
                     force = false;
                  } else if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
                     if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                        if (!this.eatingPause.getValue() || !mc.player.isUsingItem()) {
                           int webSlot = this.findWebSlot();
                           if (webSlot != -1) {
                              if (this.timer.passed(this.placeDelay.getValueInt())) {
                                 force = false;
                                 this.placedList.clear();
                                 this.directionVec = null;
                                 this.predictedPos = this.predictTarget(this.currentTarget);
                                 List<Box> path = this.buildPath(this.currentTarget, this.predictedPos);
                                 List<Box> coverage = this.expandPath(path);
                                 LinkedHashSet<BlockPos> candidates = this.buildCandidates(path);
                                 int limit = Math.min(this.blocksPer.getValueInt(), this.maxWebs.getValueInt());
                                 List<BlockPos> placements = new ArrayList<>(limit);

                                 for (BlockPos candidate : candidates) {
                                    if (placements.size() >= limit) {
                                       break;
                                    }

                                    if (this.canPlaceWeb(candidate, coverage)) {
                                       placements.add(candidate);
                                    }
                                 }

                                 if (!placements.isEmpty()) {
                                    int oldSlot = mc.player.getInventory().selectedSlot;
                                    swapTransaction = true;

                                    try {
                                       this.beginSwap(webSlot, oldSlot);

                                       try {
                                          for (BlockPos placement : placements) {
                                             BlockUtil.placeBlock(placement, true);
                                             this.placedList.add(placement);
                                             this.renderPositions.put(placement.toImmutable(), System.currentTimeMillis());
                                             this.directionVec = Vec3d.ofCenter(placement);
                                          }
                                       } finally {
                                          this.endSwap(webSlot, oldSlot);
                                       }
                                    } finally {
                                       swapTransaction = false;
                                    }

                                    this.timer.reset();
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
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (!nullCheck()) {
         if (this.timer.passed(this.placeDelay.getValueInt())) {
            this.runPlacementCycle();
         }

         if (!this.render.getValue()) {
            this.renderPositions.clear();
         } else {
            long now = System.currentTimeMillis();
            long duration = Math.max(1L, (long)this.renderTime.getValueInt());
            this.renderPositions.entrySet().removeIf(entryx -> now - (Long)entryx.getValue() >= duration);

            for (Entry<BlockPos, Long> entry : this.renderPositions.entrySet()) {
               float fade = 1.0F - MathHelper.clamp((float)(now - entry.getValue()) / (float)duration, 0.0F, 1.0F);
               BlockPos pos = entry.getKey();
               Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + this.renderHeight.getValue(), pos.getZ() + 1.0);
               if (this.renderFill.booleanValue) {
                  Color fill = this.renderFill.getValue();
                  Render3DUtil.drawFill(event.matrixStack, box, ColorUtil.injectAlpha(fill, Math.max(0, (int)(fill.getAlpha() * fade))));
               }

               if (this.renderOutline.booleanValue) {
                  Color outline = this.renderOutline.getValue();
                  Render3DUtil.drawBox(
                     event.matrixStack,
                     box,
                     ColorUtil.injectAlpha(outline, Math.max(0, (int)(outline.getAlpha() * fade))),
                     this.renderLineWidth.getValueFloat()
                  );
               }
            }
         }
      }
   }

   private PlayerEntity findTarget() {
      double range = this.targetRange.getValue();
      if (this.isValidTarget(this.currentTarget, range + 0.75)) {
         return this.currentTarget;
      } else {
         PlayerEntity closest = null;
         double closestDistance = range * range;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (this.isValidTarget(player, range)) {
               double distance = mc.player.squaredDistanceTo(player);
               if (distance < closestDistance) {
                  closest = player;
                  closestDistance = distance;
               }
            }
         }

         return closest;
      }
   }

   private boolean isValidTarget(PlayerEntity player, double range) {
      return player != null
         && player != mc.player
         && player.isAlive()
         && !player.isSpectator()
         && !Alien.FRIEND.isFriend(player)
         && mc.player.squaredDistanceTo(player) <= range * range;
   }

   private Vec3d predictTarget(PlayerEntity target) {
      int ticks = MathHelper.clamp(this.predictTicks.getValueInt(), 0, 10);
      Vec3d current = target.getPos();
      if (ticks <= 0) {
         return current;
      } else {
         Vec3d measured = current.subtract(new Vec3d(target.prevX, target.prevY, target.prevZ));
         Vec3d velocity = target.getVelocity();
         double measuredHorizontal = measured.x * measured.x + measured.z * measured.z;
         double velocityHorizontal = velocity.x * velocity.x + velocity.z * velocity.z;
         double motionX = velocityHorizontal > measuredHorizontal ? velocity.x : measured.x;
         double motionZ = velocityHorizontal > measuredHorizontal ? velocity.z : measured.z;
         double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
         if (horizontalSpeed > 1.2) {
            double scale = 1.2 / horizontalSpeed;
            motionX *= scale;
            motionZ *= scale;
         }

         double motionY = target.isOnGround() ? 0.0 : MathHelper.clamp(measured.y, -0.6, 0.6);
         Vec3d motion = new Vec3d(motionX, motionY, motionZ);
         Vec3d predicted = current;
         Box baseBox = target.getBoundingBox();

         for (int tick = 0; tick < ticks; tick++) {
            Vec3d moved = this.resolveMovement(target, baseBox, current, predicted, motion);
            if (moved.squaredDistanceTo(predicted) < 1.0E-8) {
               break;
            }

            predicted = moved;
         }

         return predicted;
      }
   }

   private Vec3d resolveMovement(PlayerEntity target, Box baseBox, Vec3d origin, Vec3d position, Vec3d motion) {
      Vec3d direct = position.add(motion);
      if (this.canOccupy(target, baseBox, origin, direct)) {
         return direct;
      } else {
         Vec3d resolved = position;
         Vec3d vertical = position.add(0.0, motion.y, 0.0);
         if (this.canOccupy(target, baseBox, origin, vertical)) {
            resolved = vertical;
         }

         Vec3d xMove = resolved.add(motion.x, 0.0, 0.0);
         if (this.canOccupy(target, baseBox, origin, xMove)) {
            resolved = xMove;
         }

         Vec3d zMove = resolved.add(0.0, 0.0, motion.z);
         if (this.canOccupy(target, baseBox, origin, zMove)) {
            resolved = zMove;
         }

         return resolved;
      }
   }

   private boolean canOccupy(PlayerEntity target, Box baseBox, Vec3d origin, Vec3d position) {
      return !BlockUtil.canCollide(target, baseBox.offset(position.subtract(origin)));
   }

   private List<Box> buildPath(PlayerEntity target, Vec3d destination) {
      Vec3d origin = target.getPos();
      Box baseBox = target.getBoundingBox();
      double distance = origin.distanceTo(destination);
      List<Box> path = new ArrayList<>();
      path.add(baseBox);
      if (distance < 1.0E-4) {
         return path;
      } else {
         int samples = Math.min(12, Math.max(1, (int)Math.ceil(distance / 0.35)));

         for (int i = 1; i <= samples; i++) {
            Vec3d sample = origin.lerp(destination, (double)i / samples);
            path.add(baseBox.offset(sample.subtract(origin)));
         }

         return path;
      }
   }

   private List<Box> expandPath(List<Box> path) {
      double horizontal = this.offset.getValue();
      List<Box> expanded = new ArrayList<>(path.size());

      for (Box box : path) {
         expanded.add(box.expand(horizontal, 0.15, horizontal));
      }

      return expanded;
   }

   private LinkedHashSet<BlockPos> buildCandidates(List<Box> path) {
      LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();
      List<Box> prioritized = new ArrayList<>(path.size());
      prioritized.add(path.getFirst());
      if (path.size() > 1) {
         prioritized.add(path.getLast());
      }

      for (int i = 1; i < path.size() - 1; i++) {
         prioritized.add(path.get(i));
      }

      if (this.feet.getValue()) {
         for (Box box : prioritized) {
            this.addLayerCenter(candidates, box, MathHelper.floor(box.minY + 0.05));
         }

         if (this.feetExtend.getValue()) {
            for (Box box : prioritized) {
               this.addLayerExtensions(candidates, box, MathHelper.floor(box.minY + 0.05));
            }
         }
      }

      if (this.face.getValue()) {
         for (Box box : prioritized) {
            this.addLayerCenter(candidates, box, MathHelper.floor(box.maxY - 0.05));
         }

         for (Box box : prioritized) {
            this.addLayerExtensions(candidates, box, MathHelper.floor(box.maxY - 0.05));
         }
      }

      if (this.down.getValue()) {
         for (Box box : prioritized) {
            this.addLayerCenter(candidates, box, MathHelper.floor(box.minY - 0.05));
         }
      }

      return candidates;
   }

   private void addLayerCenter(LinkedHashSet<BlockPos> candidates, Box box, int y) {
      double centerX = (box.minX + box.maxX) * 0.5;
      double centerZ = (box.minZ + box.maxZ) * 0.5;
      candidates.add(BlockPos.ofFloored(centerX, y, centerZ));
   }

   private void addLayerExtensions(LinkedHashSet<BlockPos> candidates, Box originalBox, int y) {
      Box box = originalBox.expand(this.offset.getValue(), 0.0, this.offset.getValue());
      int minX = MathHelper.floor(box.minX + 1.0E-4);
      int maxX = MathHelper.floor(box.maxX - 1.0E-4);
      int minZ = MathHelper.floor(box.minZ + 1.0E-4);
      int maxZ = MathHelper.floor(box.maxZ - 1.0E-4);

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            candidates.add(new BlockPos(x, y, z));
         }
      }
   }

   private boolean canPlaceWeb(BlockPos pos, List<Box> coverage) {
      if (this.placedList.contains(pos) || mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
         return false;
      } else if (!mc.world.getBlockState(pos).isReplaceable() || !mc.world.getFluidState(pos).isEmpty()) {
         return false;
      } else if (!this.intersectsPath(pos, coverage)) {
         return false;
      } else if (this.selfAvoid.getValue() && new Box(pos).intersects(mc.player.getBoundingBox())) {
         return false;
      } else {
         return BlockUtil.hasCrystal(pos) ? false : BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null;
      }
   }

   private boolean intersectsPath(BlockPos pos, List<Box> coverage) {
      Box blockBox = new Box(pos);

      for (Box targetBox : coverage) {
         if (blockBox.intersects(targetBox)) {
            return true;
         }
      }

      return false;
   }

   private void beginSwap(int webSlot, int oldSlot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(webSlot, oldSlot);
      } else if (webSlot != oldSlot) {
         InventoryUtil.switchToSlot(webSlot);
      }
   }

   private void endSwap(int webSlot, int oldSlot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(webSlot, oldSlot);
         EntityUtil.syncInventory();
      } else if (webSlot != oldSlot) {
         InventoryUtil.switchToSlot(oldSlot);
      }
   }

   private int findWebSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
   }

   private void resetRuntimeState() {
      this.placedList.clear();
      this.renderPositions.clear();
      this.lastPlacementScan = 0L;
      this.currentTarget = null;
      this.predictedPos = null;
      this.directionVec = null;
   }
}
