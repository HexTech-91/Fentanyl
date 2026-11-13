package dev.luminous.api.utils.entity;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.SwingSide;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class EntityUtil implements Wrapper {
   public static boolean isHoldingWeapon(PlayerEntity player) {
      return player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem;
   }

   public static boolean isInsideBlock() {
      return BlockUtil.getBlock(getPlayerPos(true)) == Blocks.COBWEB ? true : mc.world.canCollide(mc.player, mc.player.getBoundingBox());
   }

   public static int getDamagePercent(ItemStack stack) {
      return stack.getDamage() >= stack.getMaxDamage() ? 100 : (int)((double)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0);
   }

   public static boolean isArmorLow(PlayerEntity player, int durability) {
      for (ItemStack piece : player.getArmorItems()) {
         if (piece == null || piece.isEmpty()) {
            return true;
         }

         if (getDamagePercent(piece) < durability) {
            return true;
         }
      }

      return false;
   }

   public static float getHealth(Entity entity) {
      return entity instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : 0.0F;
   }

   public static BlockPos getEntityPos(Entity entity) {
      return new BlockPosX(entity.getPos());
   }

   public static BlockPos getPlayerPos(boolean fix) {
      return new BlockPosX(mc.player.getPos(), fix);
   }

   public static BlockPos getEntityPos(Entity entity, boolean fix) {
      return new BlockPosX(entity.getPos(), fix);
   }

   public static Vec3d getEyesPos() {
      return mc.player.getEyePos();
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      Vec3d testVec = pos.toCenterPos().add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
      BlockHitResult result = mc.world.raycast(new RaycastContext(getEyesPos(), testVec, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   public static void swingHand(Hand hand, SwingSide side) {
      switch (side) {
         case All:
            mc.player.swingHand(hand);
            break;
         case Client:
            mc.player.swingHand(hand, false);
            break;
         case Server:
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
      }
   }

   public static void syncInventory() {
      if (AntiCheat.INSTANCE.inventorySync.getValue()) {
         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }
}
