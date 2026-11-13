package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Chase extends Module {
   private static final double MAX_ERROR = 5.0;
   private static final double MAX_TRACK_RANGE = 128.0;
   private static final int THROW_COOLDOWN_TICKS = 12;
   private final BooleanSetting prioritizeEnemy = this.add(new BooleanSetting("PrioritizeEnemy", true));
   private final Set<Integer> seenPearls = new HashSet<>();
   private int cooldown;
   private boolean initialized;

   public Chase() {
      super("Chase", "Predicts enemy ender pearls and throws toward their trajectory", Module.Category.Combat);
      this.setChinese("\u73cd\u73e0\u8ffd\u8e2a");
      this.prioritizeEnemy.setChinese("\u4f18\u5148\u9501\u5b9a\u654c\u4eba");
   }

   @Override
   public void onEnable() {
      this.seenPearls.clear();
      this.cooldown = 0;
      this.initialized = false;
   }

   @Override
   public void onDisable() {
      this.seenPearls.clear();
      this.initialized = false;
   }

   @EventListener
   public void onTick(TickEvent var1) {
      if (var1.isPost() && !nullCheck()) {
         if (this.cooldown > 0) {
            this.cooldown--;
         }

         List<EnderPearlEntity> var2 = this.getPearls();
         HashSet var3 = new HashSet();

         for (EnderPearlEntity var5 : var2) {
            var3.add(var5.getId());
         }

         this.seenPearls.retainAll(var3);
         if (!this.initialized) {
            this.seenPearls.addAll(var3);
            this.initialized = true;
         } else {
            ArrayList<Chase.TargetPearl> var10 = new ArrayList<>();

            for (EnderPearlEntity var6 : var2) {
               int var7 = var6.getId();
               if (!this.seenPearls.contains(var7)) {
                  Entity var8 = var6.getOwner();
                  if (var8 != null) {
                     this.seenPearls.add(var7);
                     if (var8 instanceof PlayerEntity var9
                        && var9 != mc.player
                        && var9.isAlive()
                        && !Alien.FRIEND.isFriend(var9)
                        && !(mc.player.getPos().distanceTo(var9.getPos()) > 128.0)
                        && this.isInView(var9)) {
                        var10.add(new Chase.TargetPearl(var6, var9, this.isMarkedEnemy(var9), this.getHealth(var9)));
                     }
                  }
               }
            }

            if (this.cooldown <= 0 && !var10.isEmpty()) {
               var10.sort(
                  Comparator.comparing(Chase.TargetPearl::marked)
                     .reversed()
                     .thenComparingDouble(Chase.TargetPearl::health)
                     .thenComparingDouble(var0 -> mc.player.squaredDistanceTo(var0.player()))
               );
               this.tryChase(((Chase.TargetPearl)var10.get(0)).pearl());
            }
         }
      }
   }

   private List<EnderPearlEntity> getPearls() {
      ArrayList var1 = new ArrayList();

      for (Entity var3 : Alien.THREAD.getEntities()) {
         if (var3 instanceof EnderPearlEntity var4 && var4.isAlive()) {
            var1.add(var4);
         }
      }

      return var1;
   }

   private boolean isInView(PlayerEntity var1) {
      Vec3d var2 = mc.player.getEyePos();
      Vec3d var3 = var1.getEyePos().subtract(var2);
      if (var3.lengthSquared() < 1.0E-6) {
         return true;
      } else {
         double var4 = mc.player.getRotationVec(1.0F).normalize().dotProduct(var3.normalize());
         return var4 > 0.15 && mc.player.canSee(var1);
      }
   }

   private boolean isMarkedEnemy(PlayerEntity var1) {
      if (this.prioritizeEnemy.getValue() && EnemyMark.INSTANCE != null && EnemyMark.INSTANCE.isOn()) {
         String var2 = EnemyMark.INSTANCE.attackName.getValue().trim();
         return !var2.isEmpty() && var1.getGameProfile().getName().equalsIgnoreCase(var2);
      } else {
         return false;
      }
   }

   private double getHealth(PlayerEntity var1) {
      return var1.getHealth() + var1.getAbsorptionAmount();
   }

   private void tryChase(EnderPearlEntity var1) {
      int var2 = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL);
      if (var2 != -1) {
         List var3 = this.predictEntityPearl(var1);
         if (var3.size() >= 3) {
            Vec3d var4 = (Vec3d)var3.get(var3.size() / 2);
            Chase.Aim var5 = this.findAim(var4);
            if (var5 != null && !(var5.error() > 5.0)) {
               this.throwPearl(var2, var5.yaw(), var5.pitch());
               this.cooldown = 12;
            }
         }
      }
   }

   private List<Vec3d> predictEntityPearl(EnderPearlEntity var1) {
      ArrayList var2 = new ArrayList();
      Vec3d var3 = var1.getPos();
      Vec3d var4 = var1.getVelocity();
      var2.add(var3);

      for (int var5 = 0; var5 < 120; var5++) {
         Vec3d var6 = var3.add(var4);
         var2.add(var6);
         if (this.hitsBlock(var3, var6) || var6.y <= -65.0) {
            break;
         }

         var4 = new Vec3d(var4.x * 0.99, var4.y * 0.99 - 0.03, var4.z * 0.99);
         var3 = var6;
      }

      return var2;
   }

   private Chase.Aim findAim(Vec3d var1) {
      Vec3d var2 = mc.player.getEyePos();
      double var3 = var1.x - var2.x;
      double var5 = var1.z - var2.z;
      float var7 = (float)(Math.toDegrees(Math.atan2(var5, var3)) - 90.0);
      Chase.Aim var8 = null;

      for (float var9 = -12.0F; var9 <= 12.0F; var9 += 4.0F) {
         for (float var10 = -85.0F; var10 <= 85.0F; var10 += 3.0F) {
            float var11 = var7 + var9;
            double var12 = this.predictOwnPearlError(var11, var10, var1);
            if (var8 == null || var12 < var8.error()) {
               var8 = new Chase.Aim(var11, var10, var12);
            }
         }
      }

      if (var8 == null) {
         return null;
      } else {
         Chase.Aim var14 = var8;

         for (float var15 = var8.yaw() - 3.0F; var15 <= var8.yaw() + 3.0F; var15++) {
            for (float var16 = var8.pitch() - 3.0F; var16 <= var8.pitch() + 3.0F; var16++) {
               double var17 = this.predictOwnPearlError(var15, var16, var1);
               if (var17 < var14.error()) {
                  var14 = new Chase.Aim(var15, var16, var17);
               }
            }
         }

         return var14;
      }
   }

   private double predictOwnPearlError(float var1, float var2, Vec3d var3) {
      Vec3d var4 = mc.player.getEyePos().add(new Vec3d(0.0, -0.1, 0.0));
      double var5 = Math.toRadians(var1);
      double var7 = Math.toRadians(var2);
      Vec3d var9 = new Vec3d(
         -Math.sin(var5) * Math.cos(var7) * 1.5 + mc.player.getVelocity().x,
         -Math.sin(var7) * 1.5 + mc.player.getVelocity().y,
         Math.cos(var5) * Math.cos(var7) * 1.5 + mc.player.getVelocity().z
      );
      double var10 = var4.distanceTo(var3);

      for (int var12 = 0; var12 < 120; var12++) {
         Vec3d var13 = var4.add(var9);
         var10 = Math.min(var10, this.distanceToSegment(var3, var4, var13));
         if (this.hitsBlock(var4, var13) || var13.y <= -65.0 || var10 < 0.25) {
            break;
         }

         var9 = new Vec3d(var9.x * 0.99, var9.y * 0.99 - 0.03, var9.z * 0.99);
         var4 = var13;
      }

      return var10;
   }

   private boolean hitsBlock(Vec3d var1, Vec3d var2) {
      return mc.world.raycast(new RaycastContext(var1, var2, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType() == Type.BLOCK;
   }

   private double distanceToSegment(Vec3d var1, Vec3d var2, Vec3d var3) {
      Vec3d var4 = var3.subtract(var2);
      double var5 = var4.lengthSquared();
      if (var5 < 1.0E-8) {
         return var1.distanceTo(var2);
      } else {
         double var7 = Math.max(0.0, Math.min(1.0, var1.subtract(var2).dotProduct(var4) / var5));
         return var1.distanceTo(var2.add(var4.multiply(var7)));
      }
   }

   private void throwPearl(int var1, float var2, float var3) {
      int var4 = mc.player.getInventory().selectedSlot;
      boolean var5 = var1 != var4 && var1 != var4 + 36;
      if (var5) {
         InventoryUtil.inventorySwap(var1, var4);
      }

      Alien.ROTATION.snapAt(var2, var3);
      sendSequencedPacket(var2x -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var2x, var2, var3));
      EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
      Alien.ROTATION.snapBack();
      if (var5) {
         InventoryUtil.inventorySwap(var1, var4);
         EntityUtil.syncInventory();
      }
   }

   private record Aim(float yaw, float pitch, double error) {
   }

   private record TargetPearl(EnderPearlEntity pearl, PlayerEntity player, boolean marked, double health) {
   }
}
