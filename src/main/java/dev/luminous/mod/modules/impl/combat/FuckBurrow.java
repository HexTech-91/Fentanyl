package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FuckBurrow extends Module {
   public static FuckBurrow INSTANCE;
   private final EnumSetting<FuckBurrow.Mode> mode = this.add(new EnumSetting<>("Mode", FuckBurrow.Mode.LADDER));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100.0, 0.0, 500.0, 1.0));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("EatingPause", true));
   private final Timer timer = new Timer();
   private PlayerEntity target;

   public FuckBurrow() {
      super("FuckBurrow", Module.Category.Combat);
      this.mode.setChinese("\u6a21\u5f0f");
      this.range.setChinese("\u8303\u56f4");
      this.delay.setChinese("\u5ef6\u8fdf");
      this.rotate.setChinese("\u65cb\u8f6c");
      this.inventorySwap.setChinese("\u7269\u54c1\u680f\u4ea4\u6362");
      this.usingPause.setChinese("\u8fdb\u98df\u6682\u505c");
      this.setChinese("\u53cd\u57cb\u811a");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
         if (this.mode.getValue() == FuckBurrow.Mode.LADDER) {
            if (this.timer.passed(this.delay.getValueInt())) {
               this.target = CombatUtil.getClosestEnemy(this.range.getValue());
               if (this.target != null) {
                  Block var2 = Blocks.LADDER;
                  int var3 = this.getSlot(var2);
                  if (var3 != -1) {
                     BlockPos var4 = this.target.getBlockPos();
                     if (BlockUtil.canReplace(var4)) {
                        for (Direction var8 : Direction.values()) {
                           if (var8.getVector().getY() == 0) {
                              BlockPos var9 = var4.offset(var8);
                              if (BlockUtil.isStrictDirection(var9, var8.getOpposite()) && !BlockUtil.canReplace(var9) && BlockUtil.canClick(var9)) {
                                 double var10 = mc.player
                                    .getEyePos()
                                    .squaredDistanceTo(
                                       var9.toCenterPos()
                                          .add(
                                             new Vec3d(
                                                var8.getOpposite().getVector().getX() * 0.5,
                                                var8.getOpposite().getVector().getY() * 0.5,
                                                var8.getOpposite().getVector().getZ() * 0.5
                                             )
                                          )
                                    );
                                 if (!(var10 > this.range.getValue())) {
                                    Box var12 = getBox(var8, var4);
                                    if (var12 == null || !hasEntity(var12)) {
                                       if (this.inventorySwap.getValue()) {
                                          int var13 = mc.player.getInventory().selectedSlot;
                                          InventoryUtil.inventorySwap(var3, var13);
                                          BlockUtil.placedPos.add(var4);
                                          BlockUtil.clickBlock(var9, var8.getOpposite(), this.rotate.getValue());
                                          InventoryUtil.inventorySwap(var3, var13);
                                          EntityUtil.syncInventory();
                                       } else {
                                          this.placeLadderSilent(var3, var9, var8.getOpposite());
                                       }

                                       this.timer.reset();
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
      }
   }

   private void placeLadderSilent(int var1, BlockPos var2, Direction var3) {
      int var4 = mc.player.getInventory().selectedSlot;
      if (var4 != var1) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(var1));
      }

      BlockUtil.placedPos.add(var2.offset(var3));
      BlockUtil.clickBlock(var2, var3, this.rotate.getValue(), Hand.MAIN_HAND, true);
      if (var4 != var1) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(var4));
      }
   }

   private int getSlot(Block var1) {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(var1) : InventoryUtil.findBlock(var1);
   }

   private static Box getBox(Direction var0, BlockPos var1) {
      double var2 = 0.1875;
      double var4 = var0.getVector().getX() * 0.5 + var1.getX() + 0.5;
      double var6 = var1.getY();
      double var8 = var0.getVector().getZ() * 0.5 + var1.getZ() + 0.5;
      if (var0.getVector().getX() < 0) {
         return new Box(var4, var6, var8, var4 + var2, var6 + 1.0, var8 + 1.0);
      } else if (var0.getVector().getX() > 0) {
         return new Box(var4 - var2, var6, var8, var4, var6 + 1.0, var8 + 1.0);
      } else if (var0.getVector().getZ() < 0) {
         return new Box(var4, var6, var8, var4 + 1.0, var6 + 1.0, var8 + var2);
      } else {
         return var0.getVector().getZ() > 0 ? new Box(var4, var6, var8 - var2, var4 + 1.0, var6 + 1.0, var8) : null;
      }
   }

   public static boolean hasEntity(Box var0) {
      for (Entity var2 : BlockUtil.getEntities(var0)) {
         if (var2.isAlive()
            && !(var2 instanceof ItemEntity)
            && !(var2 instanceof ExperienceOrbEntity)
            && !(var2 instanceof ExperienceBottleEntity)
            && !(var2 instanceof ArrowEntity)
            && !(var2 instanceof ItemFrameEntity)) {
            return true;
         }
      }

      return false;
   }

   private static enum Mode {
      LADDER("\u68af\u5b50"),
      COBWEB("\u8718\u86db\u7f51"),
      BUTTON("\u6309\u94ae"),
      SIGN("\u544a\u793a\u724c"),
      TRAPDOOR("\u6d3b\u677f\u95e8");

      private final String chinese;

      private Mode(String nullxx) {
         this.chinese = nullxx;
      }

      @Override
      public String toString() {
         return this.chinese;
      }
   }
}
