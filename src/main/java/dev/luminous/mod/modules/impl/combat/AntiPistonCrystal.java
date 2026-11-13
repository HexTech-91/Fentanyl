package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiPistonCrystal extends Module {
   public static AntiPistonCrystal INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 10.0, 0.5));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 1.0, 6.0, 0.5));
   private final BooleanSetting blockPiston = this.add(new BooleanSetting("BlockPiston", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting sticky = this.add(new BooleanSetting("Sticky", true));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting skipSelf = this.add(new BooleanSetting("SkipSelf", true));
   private final BooleanSetting skipFriend = this.add(new BooleanSetting("SkipFriend", true));
   private final SliderSetting friendRange = this.add(new SliderSetting("FriendRange", 4.0, 1.0, 8.0, 0.5));
   private final Timer timer = new Timer();
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500));
   private final BooleanSetting placeCrystal = this.add(new BooleanSetting("PlaceCrystal", true));
   private final SliderSetting crystalRange = this.add(new SliderSetting("CrystalRange", 4.0, 1.0, 6.0, 1.0));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final Timer crystalTimer = new Timer();

   public AntiPistonCrystal() {
      super("AntiPistonCrystal", "\u53cd\u6d3b\u585e\u6c34\u6676", Module.Category.Combat);
      this.range.setChinese("\u68c0\u6d4b\u8303\u56f4");
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.blockPiston.setChinese("\u5835\u585e\u6d3b\u585e");
      this.mine.setChinese("\u6316\u6398\u6d3b\u585e");
      this.sticky.setChinese("\u7c98\u6027\u6d3b\u585e");
      this.inventorySwap.setChinese("\u7269\u54c1\u680f\u4ea4\u6362");
      this.skipSelf.setChinese("\u8df3\u8fc7\u81ea\u5df1\u653e\u7684");
      this.skipFriend.setChinese("\u8df3\u8fc7\u597d\u53cb\u653e\u7684");
      this.friendRange.setChinese("\u597d\u53cb\u68c0\u6d4b\u8303\u56f4");
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.placeCrystal.setChinese("\u6c34\u6676\u7206\u70b8");
      this.crystalRange.setChinese("\u6c34\u6676\u641c\u7d22\u8303\u56f4");
      this.rotate.setChinese("\u8f6c\u5934");
      this.setChinese("\u53cd\u6d3b\u585e\u6c34\u6676");
      INSTANCE = this;
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck() && !var1.isPost()) {
         if (this.timer.passed(this.placeDelay.getValueInt())) {
            BlockPos var2 = this.findPiston();
            if (var2 != null) {
               if (this.blockPiston.getValue()) {
                  this.blockPistonFront(var2);
               }

               if (this.mine.getValue()) {
                  PacketMine.INSTANCE.mine(var2);
               }

               if (this.placeCrystal.getValue() && this.crystalTimer.passed(500L)) {
                  this.explodePiston(var2);
               }

               this.timer.reset();
            }
         }
      }
   }

   private void explodePiston(BlockPos var1) {
      int var2 = this.findCrystal();
      if (var2 != -1) {
         BlockPos var3 = this.findBestCrystalPos(var1);
         if (var3 != null) {
            boolean var4 = this.rotate.getValue();
            int var5 = mc.player.getInventory().selectedSlot;
            this.doSwap(var2);
            BlockUtil.placeCrystal(var3, var4);
            if (this.inventorySwap.getValue()) {
               this.doSwap(var2);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(var5);
            }

            CombatUtil.attackCrystal(var3, var4, false);
            this.crystalTimer.reset();
         }
      }
   }

   private BlockPos findBestCrystalPos(BlockPos var1) {
      int var2 = this.crystalRange.getValueInt();
      BlockPos var3 = null;
      double var4 = Double.MAX_VALUE;

      for (int var6 = -var2; var6 <= var2; var6++) {
         for (int var7 = -var2; var7 <= var2; var7++) {
            for (int var8 = -var2; var8 <= var2; var8++) {
               BlockPos var9 = var1.add(var6, var7, var8);
               if (BlockUtil.canPlaceCrystal(var9)) {
                  double var10 = mc.player.getEyePos().squaredDistanceTo(var9.toCenterPos());
                  if (var10 < var4) {
                     var3 = var9;
                     var4 = var10;
                  }
               }
            }
         }
      }

      return var3;
   }

   private int findCrystal() {
      return this.inventorySwap.getValue() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
   }

   private void blockPistonFront(BlockPos var1) {
      Direction var2 = this.getPistonFacing(var1);
      if (var2 != null) {
         BlockPos var3 = var1.offset(var2);
         if (this.isAir(var3)) {
            if (this.inPlaceRange(var3)) {
               int var4 = this.findObsidian();
               if (var4 != -1) {
                  int var5 = mc.player.getInventory().selectedSlot;
                  this.doSwap(var4);
                  BlockUtil.placeBlock(var3, false, false);
                  if (this.inventorySwap.getValue()) {
                     this.doSwap(var4);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(var5);
                  }
               }
            }
         }
      }
   }

   private Direction getPistonFacing(BlockPos var1) {
      BlockState var2 = mc.world.getBlockState(var1);
      return !(var2.getBlock() instanceof PistonBlock) ? null : (Direction)var2.get(FacingBlock.FACING);
   }

   private BlockPos findPiston() {
      int var1 = (int)Math.ceil(this.range.getValue());
      BlockPosX var2 = new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      BlockPos var3 = null;
      double var4 = Double.MAX_VALUE;

      for (int var6 = -var1; var6 <= var1; var6++) {
         for (int var7 = -var1; var7 <= var1; var7++) {
            for (int var8 = -var1; var8 <= var1; var8++) {
               BlockPos var9 = var2.add(var6, var7, var8);
               if (this.isPiston(var9)
                  && (!this.skipSelf.getValue() || !BlockUtil.placedPos.contains(var9))
                  && (!this.skipFriend.getValue() || !this.isFriendNearby(var9))) {
                  double var10 = mc.player.getEyePos().squaredDistanceTo(var9.toCenterPos());
                  if (var10 < var4) {
                     var3 = var9;
                     var4 = var10;
                  }
               }
            }
         }
      }

      return var3;
   }

   private boolean isFriendNearby(BlockPos var1) {
      double var2 = this.friendRange.getValue() * this.friendRange.getValue();

      for (PlayerEntity var5 : mc.world.getPlayers()) {
         if (var5 != mc.player && Alien.FRIEND.isFriend(var5)) {
            double var6 = var5.getX() - (var1.getX() + 0.5);
            double var8 = var5.getY() - (var1.getY() + 0.5);
            double var10 = var5.getZ() - (var1.getZ() + 0.5);
            if (var6 * var6 + var8 * var8 + var10 * var10 <= var2) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isPiston(BlockPos var1) {
      Block var2 = mc.world.getBlockState(var1).getBlock();
      return var2 instanceof PistonBlock;
   }

   private boolean isAir(BlockPos var1) {
      return mc.world.isAir(var1);
   }

   private boolean inPlaceRange(BlockPos var1) {
      double var2 = mc.player.getEyePos().squaredDistanceTo(var1.toCenterPos());
      double var4 = this.placeRange.getValue();
      return var2 <= var4 * var4;
   }

   private int findObsidian() {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.BEDROCK) : InventoryUtil.findBlock(Blocks.BEDROCK);
   }

   private void doSwap(int var1) {
      if (var1 >= 0) {
         if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
         } else {
            InventoryUtil.switchToSlot(var1);
         }
      }
   }
}
