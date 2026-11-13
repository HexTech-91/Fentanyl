package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class PearlPredict extends Module {
   private final BooleanSetting self = this.add(new BooleanSetting("Self", true));
   private final ColorSetting selfColor;
   private final BooleanSetting others;
   private final ColorSetting othersColor;
   private final BooleanSetting preview;
   private final ColorSetting outlineColor;
   private final SliderSetting clearTime;
   private final ConcurrentHashMap<Integer, PearlPredict.PearlSpot> spots;

   public PearlPredict() {
      super("PearlPredict", Module.Category.Render);
      this.self.setChinese("\u81ea\u5df1");
      this.selfColor = this.add(new ColorSetting("SelfColor", new Color(0, 255, 0, 100)));
      this.selfColor.setChinese("\u81ea\u5df1\u989c\u8272");
      this.others = this.add(new BooleanSetting("Others", true));
      this.others.setChinese("\u4ed6\u4eba");
      this.othersColor = this.add(new ColorSetting("OthersColor", new Color(255, 0, 0, 100)));
      this.othersColor.setChinese("\u4ed6\u4eba\u989c\u8272");
      this.preview = this.add(new BooleanSetting("Preview", true));
      this.preview.setChinese("\u9884\u89c8");
      this.outlineColor = this.add(new ColorSetting("Outline", new Color(255, 255, 255, 180)));
      this.outlineColor.setChinese("\u8f6e\u5ed3\u989c\u8272");
      this.clearTime = this.add(new SliderSetting("ClearTime", 10.0, 1.0, 30.0, 0.5).setSuffix("s"));
      this.clearTime.setChinese("\u6e05\u9664\u65f6\u95f4");
      this.spots = new ConcurrentHashMap<>();
      this.setChinese("\u73cd\u73e0\u843d\u70b9\u9884\u6d4b");
   }

   @Override
   public void onDisable() {
      this.spots.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         for (Entry<Integer, PearlPredict.PearlSpot> entry : this.spots.entrySet()) {
            PearlPredict.PearlSpot spot = entry.getValue();
            if (spot.pearl != null && !spot.pearl.isRemoved()) {
               Vec3d landing = this.simulatePearl(spot.pearl.getPos(), spot.pearl.getVelocity());
               if (landing != null) {
                  spot.pos = landing;
                  spot.landed = false;
               }
            } else if (!spot.landed) {
               spot.landed = true;
               spot.timer.reset();
            }
         }

         this.spots.values().removeIf(s -> s.landed && s.timer.passedS(this.clearTime.getValue()));
      }
   }

   @EventListener
   public void onEntitySpawn(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         if (event.getEntity() instanceof EnderPearlEntity pearl) {
            Entity owner = pearl.getOwner();
            boolean isSelf = owner == mc.player;
            boolean isFriend = owner instanceof PlayerEntity && Alien.FRIEND.isFriend((PlayerEntity)owner);
            if (!isSelf || this.self.getValue()) {
               if (isSelf || isFriend || this.others.getValue()) {
                  Vec3d landing = this.simulatePearl(pearl.getPos(), pearl.getVelocity());
                  if (landing != null) {
                     String name = isSelf ? "You" : (owner != null ? owner.getName().getString() : "Unknown");
                     Color color = !isSelf && !isFriend ? this.othersColor.getValue() : this.selfColor.getValue();
                     this.spots.put(pearl.getId(), new PearlPredict.PearlSpot(pearl, landing, name, color, new Timer()));
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck()) {
         for (PearlPredict.PearlSpot spot : this.spots.values()) {
            this.renderBeam(matrixStack, spot.pos, spot.color);
         }

         if (this.preview.getValue()) {
            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();
            if (mainHand.getItem() instanceof EnderPearlItem || offHand.getItem() instanceof EnderPearlItem) {
               Vec3d landing = this.simulateOwnThrow();
               if (landing != null) {
                  this.renderBeam(matrixStack, landing, this.selfColor.getValue());
               }
            }
         }
      }
   }

   private void renderBeam(MatrixStack matrixStack, Vec3d pos, Color color) {
      Render3DUtil.draw3DBox(
         matrixStack, new Box(pos.x - 0.3, -60.0, pos.z - 0.3, pos.x + 0.3, 320.0, pos.z + 0.3), color, this.outlineColor.getValue(), true, true, 2.0F
      );
   }

   private Vec3d simulatePearl(Vec3d pos, Vec3d velocity) {
      double x = pos.x;
      double y = pos.y;
      double z = pos.z;
      double mx = velocity.x;
      double my = velocity.y;
      double mz = velocity.z;

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         int floorX = x + mx >= 0.0 ? (int)(x + mx) : (int)(x + mx) - 1;
         int floorY = y + my >= 0.0 ? (int)(y + my) : (int)(y + my) - 1;
         int floorZ = z + mz >= 0.0 ? (int)(z + mz) : (int)(z + mz) - 1;
         if (mc.world.getBlockState(new BlockPos(floorX, floorY, floorZ)).getBlock() == Blocks.WATER) {
            mx *= 0.8;
            my *= 0.8;
            mz *= 0.8;
         } else {
            mx *= 0.99;
            my *= 0.99;
            mz *= 0.99;
         }

         x += mx;
         y += my;
         z += mz;
         my -= 0.03;
         Vec3d currentPos = new Vec3d(x, y, z);
         BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, currentPos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (bhr != null && bhr.getType() != Type.MISS) {
            return bhr.getPos();
         }

         if (y <= -65.0) {
            break;
         }
      }

      return null;
   }

   private Vec3d simulateOwnThrow() {
      double x = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * mc.getRenderTickCounter().getTickDelta(true);
      double y = mc.player.prevY + (mc.player.getY() - mc.player.prevY) * mc.getRenderTickCounter().getTickDelta(true);
      double z = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * mc.getRenderTickCounter().getTickDelta(true);
      y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      x -= MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
      z -= MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      double maxDist = 0.4;
      double motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI) * maxDist;
      double motionY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI) * maxDist;
      double motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI) * maxDist;
      double len = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
      motionX /= len;
      motionY /= len;
      motionZ /= len;
      motionX *= 1.5;
      motionY *= 1.5;
      motionZ *= 1.5;
      motionX += mc.player.getVelocity().x;
      motionY += mc.player.getVelocity().y;
      motionZ += mc.player.getVelocity().z;

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         int floorX = x + motionX >= 0.0 ? (int)(x + motionX) : (int)(x + motionX) - 1;
         int floorY = y + motionY >= 0.0 ? (int)(y + motionY) : (int)(y + motionY) - 1;
         int floorZ = z + motionZ >= 0.0 ? (int)(z + motionZ) : (int)(z + motionZ) - 1;
         if (mc.world.getBlockState(new BlockPos(floorX, floorY, floorZ)).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         x += motionX;
         y += motionY;
         z += motionZ;
         motionY -= 0.03;
         Vec3d currentPos = new Vec3d(x, y, z);
         BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, currentPos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (bhr != null && bhr.getType() != Type.MISS) {
            return bhr.getPos();
         }

         if (y <= -65.0) {
            return null;
         }
      }

      return null;
   }

   private static class PearlSpot {
      final EnderPearlEntity pearl;
      Vec3d pos;
      final String name;
      final Color color;
      final Timer timer;
      boolean landed;

      PearlSpot(EnderPearlEntity pearl, Vec3d pos, String name, Color color, Timer timer) {
         this.pearl = pearl;
         this.pos = pos;
         this.name = name;
         this.color = color;
         this.timer = timer;
      }
   }
}
