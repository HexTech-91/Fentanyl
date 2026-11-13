package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.asm.accessors.IEnderDragonEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class EveryonePig extends Module {
   public static EveryonePig INSTANCE;
   public final EnumSetting<EveryonePig.Target> target;
   public final EnumSetting<EveryonePig.AnimalModel> animal;
   public final BooleanSetting firstPersonSelf;
   public final BooleanSetting useFriendDifferent;
   public final EnumSetting<EveryonePig.AnimalModel> friendAnimal;
   public final EnumSetting<EveryonePig.AnimalModel> nonFriendAnimal;
   private final Map<UUID, LivingEntity> entityCache = new HashMap<>();

   public EveryonePig() {
      super("EveryoneAnimal", Module.Category.Render);
      this.setChinese("\u5168\u5458\u53d8\u52a8\u7269");
      INSTANCE = this;
      this.animal = this.add(new EnumSetting<>("Animal", EveryonePig.AnimalModel.Pig));
      this.animal.setChinese("\u52a8\u7269\u6a21\u578b");
      this.target = this.add(new EnumSetting<>("Target", EveryonePig.Target.Both));
      this.target.setChinese("\u76ee\u6807");
      this.firstPersonSelf = this.add(new BooleanSetting("FirstPersonSelf", false));
      this.firstPersonSelf.setChinese("\u7b2c\u4e00\u4eba\u79f0\u81ea\u5df1");
      this.useFriendDifferent = this.add(new BooleanSetting("UseFriendDifferent", false));
      this.useFriendDifferent.setChinese("\u597d\u53cb\u4e0d\u540c\u52a8\u7269");
      this.friendAnimal = this.add(new EnumSetting<>("FriendAnimal", EveryonePig.AnimalModel.Pig));
      this.friendAnimal.setChinese("\u597d\u53cb\u52a8\u7269\u6a21\u578b");
      this.nonFriendAnimal = this.add(new EnumSetting<>("NonFriendAnimal", EveryonePig.AnimalModel.Wolf));
      this.nonFriendAnimal.setChinese("\u975e\u597d\u53cb\u52a8\u7269\u6a21\u578b");
   }

   @Override
   public void onDisable() {
      this.entityCache.clear();
   }

   public boolean shouldReplace(PlayerEntity var1) {
      return var1 == mc.player
         ? this.target.getValue() == EveryonePig.Target.Self || this.target.getValue() == EveryonePig.Target.Both
         : this.target.getValue() == EveryonePig.Target.Others || this.target.getValue() == EveryonePig.Target.Both;
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (!nullCheck()) {
         float var2 = mc.getRenderTickCounter().getTickDelta(true);

         for (PlayerEntity var4 : mc.world.getPlayers()) {
            if (var4 != null
               && !var4.isDead()
               && var4.isAlive()
               && this.shouldReplace(var4)
               && (var4 != mc.player || !mc.options.getPerspective().isFirstPerson() || this.firstPersonSelf.getValue())) {
               LivingEntity var5 = this.getAnimalEntity(var4);
               double var6 = MathHelper.lerp(var2, var4.prevX, var4.getX());
               double var8 = MathHelper.lerp(var2, var4.prevY, var4.getY());
               double var10 = MathHelper.lerp(var2, var4.prevZ, var4.getZ());
               float var12 = MathHelper.lerpAngleDegrees(var2, var4.prevYaw, var4.getYaw());
               float var13 = MathHelper.lerpAngleDegrees(var2, var4.prevBodyYaw, var4.bodyYaw);
               var5.setPos(var6, var8, var10);
               var5.prevX = var6;
               var5.prevY = var8;
               var5.prevZ = var10;
               var5.setYaw(var12);
               var5.prevYaw = var12;
               var5.setPitch(var4.getPitch());
               var5.prevPitch = var4.prevPitch;
               var5.bodyYaw = var13;
               var5.prevBodyYaw = var13;
               var5.prevHeadYaw = var5.headYaw = MathHelper.lerpAngleDegrees(var2, var4.prevHeadYaw, var4.headYaw);
               var5.limbAnimator.setSpeed(var4.limbAnimator.getSpeed());
               var5.age = var4.age;
               var5.setSneaking(var4.isSneaking());
               var5.setPose(var4.getPose());
               if (this.getModelForPlayer(var4) == EveryonePig.AnimalModel.EnderDragon && var5 instanceof EnderDragonEntity var14) {
                  this.animateDragonWings(var14);
               }

               double var21 = mc.getEntityRenderDispatcher().camera.getPos().getX();
               double var16 = mc.getEntityRenderDispatcher().camera.getPos().getY();
               double var18 = mc.getEntityRenderDispatcher().camera.getPos().getZ();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               GL11.glDisable(32823);
               GL11.glPolygonOffset(0.0F, 0.0F);
               var1.push();
               if (this.getModelForPlayer(var4) == EveryonePig.AnimalModel.EnderDragon) {
                  var1.translate(var6 - var21, var8 - var16, var10 - var18);
                  var1.scale(0.3F, 0.3F, 0.3F);
                  var1.translate(-(var6 - var21), -(var8 - var16), -(var10 - var18));
               }

               Immediate var20 = mc.getBufferBuilders().getEntityVertexConsumers();
               mc.getEntityRenderDispatcher().render(var5, var6 - var21, var8 - var16, var10 - var18, var13, var2, var1, var20, 15728880);
               var20.draw();
               var1.pop();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
            }
         }

         this.entityCache.keySet().removeIf(var0 -> mc.world.getPlayers().stream().noneMatch(var1x -> var1x.getUuid().equals(var0)));
      }
   }

   private void animateDragonWings(EnderDragonEntity var1) {
      IEnderDragonEntity var2 = (IEnderDragonEntity)var1;
      float var3 = var2.getWingPosition();
      var2.setPrevWingPosition(var3);
      double var4 = System.currentTimeMillis() % 7000L / 7000.0 * Math.PI * 2.0;
      float var6 = (float)(Math.sin(var4) * 1.5);
      float var7 = var2.getWingPosition();
      var2.setWingPosition(var7 + (var6 - var7) * 0.05F);
   }

   private EveryonePig.AnimalModel getModelForPlayer(PlayerEntity var1) {
      if (!this.useFriendDifferent.getValue()) {
         return this.animal.getValue();
      } else {
         return Alien.FRIEND.isFriend(var1) ? this.friendAnimal.getValue() : this.nonFriendAnimal.getValue();
      }
   }

   private LivingEntity getAnimalEntity(PlayerEntity var1) {
      UUID var2 = var1.getUuid();
      EveryonePig.AnimalModel var3 = this.getModelForPlayer(var1);
      LivingEntity var4 = this.entityCache.get(var2);
      if (var4 != null && !this.matchesCurrentModel(var4, var3)) {
         this.entityCache.remove(var2);
         var4 = null;
      }

      if (var4 == null) {
         var4 = this.createAnimal(var2, var3);
         this.entityCache.put(var2, var4);
      }

      return var4;
   }

   private boolean matchesCurrentModel(LivingEntity var1, EveryonePig.AnimalModel var2) {
      switch (var2) {
         case Pig:
            return var1 instanceof PigEntity;
         case EnderDragon:
            return var1 instanceof EnderDragonEntity;
         case Wolf:
            return var1 instanceof WolfEntity;
         case Panda:
            return var1 instanceof PandaEntity;
         case Ultraman:
            return var1 instanceof GiantEntity;
         case PolarBear:
            return var1 instanceof RabbitEntity;
         case Cat:
            return var1 instanceof CatEntity;
         case Creeper:
            return var1 instanceof SheepEntity;
         case Zombie:
            return var1 instanceof ZombieEntity;
         case Villager:
            return var1 instanceof VillagerEntity;
         case IronGolem:
            return var1 instanceof MagmaCubeEntity;
         case Cow:
            return var1 instanceof CowEntity;
         case Sheep:
            return var1 instanceof BlazeEntity;
         default:
            throw new IllegalStateException("Unknown animal model");
      }
   }

   private LivingEntity createAnimal(UUID var1, EveryonePig.AnimalModel var2) {
      switch (var2) {
         case Pig:
            PigEntity var15 = new PigEntity(EntityType.PIG, Wrapper.mc.world);
            var15.setId(-Math.abs(var1.hashCode()));
            var15.setSilent(true);
            var15.setNoGravity(true);
            return var15;
         case EnderDragon:
            EnderDragonEntity var14 = new EnderDragonEntity(EntityType.ENDER_DRAGON, Wrapper.mc.world);
            var14.setId(-Math.abs(var1.hashCode()));
            var14.setSilent(true);
            var14.setNoGravity(true);
            return var14;
         case Wolf:
            WolfEntity var13 = new WolfEntity(EntityType.WOLF, Wrapper.mc.world);
            var13.setId(-Math.abs(var1.hashCode()));
            var13.setSilent(true);
            var13.setNoGravity(true);
            return var13;
         case Panda:
            PandaEntity var12 = new PandaEntity(EntityType.PANDA, Wrapper.mc.world);
            var12.setId(-Math.abs(var1.hashCode()));
            var12.setSilent(true);
            var12.setNoGravity(true);
            return var12;
         case Ultraman:
            GiantEntity var11 = new GiantEntity(EntityType.GIANT, Wrapper.mc.world);
            var11.setId(-Math.abs(var1.hashCode()));
            var11.setSilent(true);
            var11.setNoGravity(true);
            return var11;
         case PolarBear:
            RabbitEntity var10 = new RabbitEntity(EntityType.RABBIT, Wrapper.mc.world);
            var10.setId(-Math.abs(var1.hashCode()));
            var10.setSilent(true);
            var10.setNoGravity(true);
            return var10;
         case Cat:
            CatEntity var9 = new CatEntity(EntityType.CAT, Wrapper.mc.world);
            var9.setId(-Math.abs(var1.hashCode()));
            var9.setSilent(true);
            var9.setNoGravity(true);
            return var9;
         case Creeper:
            SheepEntity var8 = new SheepEntity(EntityType.SHEEP, Wrapper.mc.world);
            var8.setId(-Math.abs(var1.hashCode()));
            var8.setSilent(true);
            var8.setNoGravity(true);
            return var8;
         case Zombie:
            ZombieEntity var7 = new ZombieEntity(EntityType.ZOMBIE, Wrapper.mc.world);
            var7.setId(-Math.abs(var1.hashCode()));
            var7.setSilent(true);
            var7.setNoGravity(true);
            return var7;
         case Villager:
            VillagerEntity var6 = new VillagerEntity(EntityType.VILLAGER, Wrapper.mc.world);
            var6.setId(-Math.abs(var1.hashCode()));
            var6.setSilent(true);
            var6.setNoGravity(true);
            return var6;
         case IronGolem:
            MagmaCubeEntity var5 = new MagmaCubeEntity(EntityType.MAGMA_CUBE, Wrapper.mc.world);
            var5.setId(-Math.abs(var1.hashCode()));
            var5.setSilent(true);
            var5.setNoGravity(true);
            return var5;
         case Cow:
            CowEntity var4 = new CowEntity(EntityType.COW, Wrapper.mc.world);
            var4.setId(-Math.abs(var1.hashCode()));
            var4.setSilent(true);
            var4.setNoGravity(true);
            return var4;
         case Sheep:
            BlazeEntity var3 = new BlazeEntity(EntityType.BLAZE, Wrapper.mc.world);
            var3.setId(-Math.abs(var1.hashCode()));
            var3.setSilent(true);
            var3.setNoGravity(true);
            return var3;
         default:
            throw new IllegalStateException("Unknown animal model");
      }
   }

   public static enum AnimalModel {
      Pig,
      EnderDragon,
      Wolf,
      Panda,
      Ultraman,
      PolarBear,
      Cat,
      Creeper,
      Zombie,
      Villager,
      IronGolem,
      Cow,
      Sheep;
   }

   public static enum Target {
      Self,
      Others,
      Both;
   }
}
