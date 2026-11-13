package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AutoCrossbowAim extends Module {
   public static AutoCrossbowAim INSTANCE;
   private static final int BURST_SIZE = 5;
   private static final double ARROW_SPEED = 3.15;
   private static final double ARROW_GRAVITY = 0.05;
   private final SliderSetting range = this.add(new SliderSetting("AimRange", 32.0, 1.0, 128.0, 0.5));
   private final SliderSetting stationaryTime = this.add(new SliderSetting("StationaryTime", 500.0, 100.0, 2000.0, 50.0));
   private final SliderSetting movementThreshold = this.add(new SliderSetting("MovementThreshold", 0.04, 0.01, 0.3, 0.01));
   private final SliderSetting shotDelay = this.add(new SliderSetting("ShotDelay", 50.0, 0.0, 500.0, 10.0));
   private final SliderSetting effectWait = this.add(new SliderSetting("EffectWait", 2000.0, 500.0, 5000.0, 100.0));
   private final Map<UUID, AutoCrossbowAim.MotionState> motionStates = new HashMap<>();
   private final Timer shotTimer = new Timer();
   private final Timer burstTimer = new Timer();
   private PlayerEntity target;
   private UUID burstTarget;
   private int shotsFired;
   private boolean waitingForEffect;
   private ChargedProjectilesComponent persistentProjectiles;

   public AutoCrossbowAim() {
      super("AutoCrossbowAim", "Silently shoots stationary mace users with slow-falling arrows", Module.Category.Combat);
      this.setChinese("\u81ea\u52a8\u5f29\u7784\u51c6");
      this.range.setChinese("\u5c04\u7a0b");
      this.stationaryTime.setChinese("\u9759\u6b62\u65f6\u95f4");
      this.movementThreshold.setChinese("\u79fb\u52a8\u9608\u503c");
      this.shotDelay.setChinese("\u5c04\u51fb\u95f4\u9694");
      this.effectWait.setChinese("\u6548\u679c\u786e\u8ba4\u65f6\u95f4");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.resetBurst();
      this.motionStates.clear();
      this.shotTimer.reset();
   }

   @Override
   public void onDisable() {
      this.resetBurst();
      this.motionStates.clear();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         this.updateMotionStates();
         this.maintainCrossbowState();
         this.target = this.findTarget();
         if (this.target == null && this.waitingForEffect && this.burstTarget != null) {
            PlayerEntity waitingTarget = mc.world.getPlayerByUuid(this.burstTarget);
            if (waitingTarget != null && waitingTarget.isAlive()) {
               this.target = waitingTarget;
            }
         }

         if (this.target == null) {
            RotationManager.directionVec = null;
            this.resetBurst();
         } else {
            UUID targetId = this.target.getUuid();
            if (!targetId.equals(this.burstTarget)) {
               this.resetBurst();
               this.burstTarget = targetId;
            }

            if (this.target.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
               RotationManager.directionVec = null;
               this.shotsFired = 5;
               this.waitingForEffect = true;
               this.burstTimer.reset();
            } else if (!this.isStationary(this.target)) {
               RotationManager.directionVec = null;
            } else {
               Alien.ROTATION.rotationTo(this.target.getEyePos());
               if (this.waitingForEffect) {
                  if (!this.burstTimer.passedMs(this.effectWait.getValue()) && this.target.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                     return;
                  }

                  this.shotsFired = 0;
                  this.waitingForEffect = false;
               }

               if (this.shotsFired < 5 && this.shotTimer.passedMs(this.shotDelay.getValue())) {
                  int crossbowSlot = this.findSlowFallingCrossbow();
                  if (crossbowSlot != -1) {
                     this.fireSilently(crossbowSlot, this.target);
                     this.shotsFired++;
                     this.shotTimer.reset();
                     if (this.shotsFired == 5) {
                        this.waitingForEffect = true;
                        this.burstTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      PlayerEntity popped = event.getPlayer();
      if (popped != null && this.burstTarget != null && popped.getUuid().equals(this.burstTarget)) {
         this.shotsFired = 0;
         this.waitingForEffect = false;
         this.burstTimer.reset();
         this.shotTimer.reset();
      }
   }

   private void updateMotionStates() {
      long now = System.currentTimeMillis();
      double thresholdSquared = this.movementThreshold.getValue() * this.movementThreshold.getValue();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player) {
            AutoCrossbowAim.MotionState state = this.motionStates.get(player.getUuid());
            Vec3d position = player.getPos();
            if (state == null) {
               this.motionStates.put(player.getUuid(), new AutoCrossbowAim.MotionState(position, now));
            } else {
               if (state.position.squaredDistanceTo(position) > thresholdSquared) {
                  state.stationarySince = now;
               }

               state.position = position;
            }
         }
      }

      this.motionStates.keySet().removeIf(id -> mc.world.getPlayerByUuid(id) == null);
   }

   private PlayerEntity findTarget() {
      PlayerEntity bestStationary = null;
      PlayerEntity bestMoving = null;
      double stationaryDistance = this.range.getValue();
      double movingDistance = this.range.getValue();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (this.isEligible(player)) {
            double distance = mc.player.distanceTo(player);
            if (this.isStationary(player) && distance < stationaryDistance) {
               stationaryDistance = distance;
               bestStationary = player;
            } else if (!this.isStationary(player) && distance < movingDistance) {
               movingDistance = distance;
               bestMoving = player;
            }
         }
      }

      return bestStationary != null ? bestStationary : bestMoving;
   }

   private boolean isEligible(PlayerEntity player) {
      if (player == mc.player || !player.isAlive() || player.isSpectator()) {
         return false;
      } else if (Alien.FRIEND.isFriend(player)) {
         return false;
      } else {
         return !player.getMainHandStack().isOf(Items.MACE) && !player.getOffHandStack().isOf(Items.MACE)
            ? false
            : mc.player.distanceTo(player) <= this.range.getValue();
      }
   }

   private boolean isStationary(PlayerEntity player) {
      AutoCrossbowAim.MotionState state = this.motionStates.get(player.getUuid());
      return state != null && System.currentTimeMillis() - state.stationarySince >= this.stationaryTime.getValue();
   }

   private int findSlowFallingCrossbow() {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.getItem() instanceof CrossbowItem) {
            return slot;
         }
      }

      return -1;
   }

   private void maintainCrossbowState() {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (stack.getItem() instanceof CrossbowItem) {
            ChargedProjectilesComponent charged = (ChargedProjectilesComponent)stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (charged != null && !charged.isEmpty()) {
               this.persistentProjectiles = charged;
            }
         }
      }

      if (this.persistentProjectiles != null) {
         for (int slotx = 0; slotx < 36; slotx++) {
            ItemStack stack = mc.player.getInventory().getStack(slotx);
            if (stack.getItem() instanceof CrossbowItem) {
               ChargedProjectilesComponent charged = (ChargedProjectilesComponent)stack.get(DataComponentTypes.CHARGED_PROJECTILES);
               if (charged == null || charged.isEmpty()) {
                  stack.set(DataComponentTypes.CHARGED_PROJECTILES, this.persistentProjectiles);
               }
            }
         }
      }
   }

   private boolean hasSlowFallingArrowInInventory() {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         PotionContentsComponent potion = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (potion != null && this.hasSlowFallingEffect(potion)) {
            return true;
         }
      }

      return false;
   }

   private boolean containsSlowFallingArrow(ItemStack crossbow) {
      ChargedProjectilesComponent charged = (ChargedProjectilesComponent)crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
      if (charged != null && !charged.isEmpty()) {
         for (ItemStack projectile : charged.getProjectiles()) {
            PotionContentsComponent potion = (PotionContentsComponent)projectile.get(DataComponentTypes.POTION_CONTENTS);
            if (potion != null && this.hasSlowFallingEffect(potion)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean hasSlowFallingEffect(PotionContentsComponent potion) {
      for (StatusEffectInstance effect : potion.getEffects()) {
         if (effect.getEffectType().value() == StatusEffects.SLOW_FALLING.value()) {
            return true;
         }
      }

      return false;
   }

   private void fireSilently(int crossbowSlot, PlayerEntity player) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      float[] rotations = this.getBallisticRotation(player);
      if (crossbowSlot < 9) {
         InventoryUtil.switchToSlot(crossbowSlot);
      } else {
         InventoryUtil.inventorySwap(crossbowSlot, oldSlot);
      }

      mc.itemUseCooldown = 0;
      sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotations[0], rotations[1]));
      if (crossbowSlot < 9) {
         InventoryUtil.switchToSlot(oldSlot);
      } else {
         InventoryUtil.inventorySwap(crossbowSlot, oldSlot);
         EntityUtil.syncInventory();
      }
   }

   private float[] getBallisticRotation(PlayerEntity player) {
      Vec3d start = mc.player.getEyePos();
      Vec3d aim = player.getPos().add(0.0, player.getHeight() * 0.55, 0.0);
      float[] direct = RotationManager.getRotation(start, aim);
      double horizontal = Math.hypot(aim.x - start.x, aim.z - start.z);
      double vertical = aim.y - start.y;
      double speedSquared = 9.9225;
      double discriminant = speedSquared * speedSquared - 0.05 * (0.05 * horizontal * horizontal + 2.0 * vertical * speedSquared);
      if (!(horizontal < 0.001) && !(discriminant < 0.0)) {
         double angle = Math.atan((speedSquared - Math.sqrt(discriminant)) / (0.05 * horizontal));
         return new float[]{direct[0], (float)(-Math.toDegrees(angle))};
      } else {
         return direct;
      }
   }

   private void resetBurst() {
      this.target = null;
      this.burstTarget = null;
      this.shotsFired = 0;
      this.waitingForEffect = false;
      this.burstTimer.reset();
   }

   @Override
   public String getInfo() {
      if (this.target == null) {
         return null;
      } else {
         return this.waitingForEffect ? "WAIT" : this.shotsFired + "/5";
      }
   }

   private static final class MotionState {
      private Vec3d position;
      private long stationarySince;

      private MotionState(Vec3d position, long stationarySince) {
         this.position = position;
         this.stationarySince = stationarySince;
      }
   }
}
