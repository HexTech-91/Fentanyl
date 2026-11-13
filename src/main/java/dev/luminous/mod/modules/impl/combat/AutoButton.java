package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.move.MoveUtils;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.rotation.Rotation;
import dev.luminous.api.utils.rotation.RotationUtils;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.ButtonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoButton extends Module {
   public static AutoButton INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("\u6beb\u79d2"));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 8.0).setSuffix("\u7c73"));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.5, 0.0, 6.0).setSuffix("\u7c73"));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting ghostHand = this.add(new BooleanSetting("GhostHand", true));
   private final BooleanSetting movementFix = this.add(new BooleanSetting("MovementFix", true));
   private final Timer timer = new Timer();

   public AutoButton() {
      super("AutoButton", Module.Category.Combat);
      this.setChinese("\u81ea\u52a8\u6309\u94ae");
      INSTANCE = this;
      FeatureTranslations.localizeModule(this);
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passed((long)this.delay.getValue())) {
         if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
            PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
            if (target != null) {
               BlockPos pos = EntityUtil.getEntityPos(target, true);
               if (pos.getY() < 320) {
                  if (!this.shouldSkipPlace(pos)) {
                     int buttonSlot = this.getButtonSlot();
                     if (buttonSlot != -1) {
                        int oldSlot = mc.player.getInventory().selectedSlot;
                        this.doSwap(buttonSlot);
                        if (this.movementFix.getValue()) {
                           Direction side = BlockUtil.allowAirPlace() ? BlockUtil.getClickSide(pos) : BlockUtil.getPlaceSide(pos, this.placeRange.getValue());
                           if (side != null) {
                              Vec3d hitVec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
                              Rotation rotation = RotationUtils.calculate(hitVec);
                              MoveUtils.fixMovement(rotation.getYaw());
                           }
                        }

                        BlockUtil.placeBlock(pos, true);
                        if (this.inventorySwap.getValue()) {
                           this.doSwap(buttonSlot);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(oldSlot);
                        }

                        this.timer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldSkipPlace(BlockPos pos) {
      if (!BlockUtil.canReplace(pos)) {
         return true;
      } else {
         if (this.ghostHand.getValue()) {
            if (this.hasEntityIgnoringPlayers(pos)) {
               return true;
            }
         } else if (BlockUtil.hasEntity(pos, false)) {
            return true;
         }

         return this.placeRange.getValue() > 0.0 && mc.player.getEyePos().distanceTo(pos.toCenterPos()) > this.placeRange.getValue()
            ? true
            : BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) == null && !BlockUtil.allowAirPlace();
      }
   }

   private boolean hasEntityIgnoringPlayers(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != null
            && entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && !(entity instanceof PlayerEntity)) {
            return true;
         }
      }

      return false;
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getButtonSlot() {
      return this.inventorySwap.getValue() ? InventoryUtil.findClassInventorySlot(ButtonBlock.class) : InventoryUtil.findClass(ButtonBlock.class);
   }
}
