package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiPush extends Module {
   public static AntiPush INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 10.0, 0.5));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
   private final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true));
   private final BooleanSetting skipSelf = this.add(new BooleanSetting("SkipSelf", true));
   private final BooleanSetting skipFriend = this.add(new BooleanSetting("SkipFriend", true));
   private final SliderSetting friendRange = this.add(new SliderSetting("FriendRange", 4.0, 1.0, 8.0, 0.5));
   private final SliderSetting mineTimeout = this.add(new SliderSetting("MineTimeout", 1000, 100, 5000));
   private final Timer timer = new Timer();
   private BlockPos targetPos;
   private long mineStartAt;

   public AntiPush() {
      super("AntiPush", "\u53cd\u6d3b\u585e\u63a8\u4eba", Module.Category.Combat);
      this.range.setChinese("\u68c0\u6d4b\u8303\u56f4");
      this.mine.setChinese("\u53d1\u5305\u6316\u6398");
      this.surround.setChinese("\u5305\u56f4\u81ea\u5df1");
      this.crystal.setChinese("\u6c34\u6676\u7206\u70b8");
      this.skipSelf.setChinese("\u8df3\u8fc7\u81ea\u5df1\u653e\u7684");
      this.skipFriend.setChinese("\u8df3\u8fc7\u597d\u53cb\u653e\u7684");
      this.friendRange.setChinese("\u597d\u53cb\u68c0\u6d4b\u8303\u56f4");
      this.mineTimeout.setChinese("\u6316\u6398\u8d85\u65f6");
      this.targetPos = null;
      this.setChinese("\u53cd\u6d3b\u585e\u63a8\u4eba");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.targetPos = null;
   }

   @Override
   public void onDisable() {
      this.targetPos = null;
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck() && !var1.isPost()) {
         BlockPos var2 = this.findPiston();
         if (var2 != null) {
            if (this.mine.getValue()) {
               if (!var2.equals(this.targetPos)) {
                  this.targetPos = var2;
                  this.mineStartAt = System.currentTimeMillis();
               }

               PacketMine.INSTANCE.mine(var2);
            }

            if (this.isAir(var2)) {
               this.targetPos = null;
               return;
            }

            if (System.currentTimeMillis() - this.mineStartAt > this.mineTimeout.getValue()) {
               this.handleStuck(var2);
            }
         } else {
            this.targetPos = null;
         }
      }
   }

   private void handleStuck(BlockPos var1) {
      if (!this.crystal.getValue() || !this.tryCrystalBoom(var1)) {
         if (this.surround.getValue() && Surround.INSTANCE != null) {
            if (!Surround.INSTANCE.isOn()) {
               Surround.INSTANCE.enable();
            }

            this.surroundSelf();
         }
      }
   }

   private void surroundSelf() {
      int var1 = (int)Math.floor(mc.player.getX());
      int var2 = (int)Math.floor(mc.player.getY());
      int var3 = (int)Math.floor(mc.player.getZ());
      BlockPosX var4 = new BlockPosX(var1, var2 + 1, var3);
      if (this.canSurround(var4)) {
         Surround.INSTANCE.doSurround(var4);
      } else {
         BlockPosX var5 = new BlockPosX(var1, var2, var3);
         if (this.canSurround(var5)) {
            Surround.INSTANCE.doSurround(var5);
         }

         Surround.INSTANCE.doSurround(var4);
      }
   }

   private boolean canSurround(BlockPos var1) {
      for (Direction var5 : Direction.values()) {
         if (var5 != Direction.UP && var5 != Direction.DOWN) {
            BlockPos var6 = var1.offset(var5);
            if (BlockUtil.getPlaceSide(var6) != null) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean tryCrystalBoom(BlockPos var1) {
      for (Direction var5 : Direction.values()) {
         BlockPos var6 = var1.offset(var5);
         if (this.canPlaceCrystal(var6)) {
            BlockUtil.placeBlock(var6, false);
            CombatUtil.attackCrystal(var6, false, true);
            return true;
         }
      }

      BlockPos var7 = var1.up();
      if (this.canPlaceCrystal(var7)) {
         BlockUtil.placeBlock(var7, false);
         CombatUtil.attackCrystal(var7, false, true);
         return true;
      } else {
         return false;
      }
   }

   private boolean canPlaceCrystal(BlockPos var1) {
      if (!mc.world.isAir(var1)) {
         return false;
      } else {
         BlockPos var2 = var1.down();
         Block var3 = mc.world.getBlockState(var2).getBlock();
         return var3 == Blocks.BEDROCK || var3 == Blocks.OBSIDIAN;
      }
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
      BlockState var2 = mc.world.getBlockState(var1);
      Block var3 = var2.getBlock();
      return var3 instanceof PistonBlock ? true : var3 == Blocks.REDSTONE_BLOCK;
   }

   private boolean isAir(BlockPos var1) {
      return mc.world.isAir(var1);
   }
}
