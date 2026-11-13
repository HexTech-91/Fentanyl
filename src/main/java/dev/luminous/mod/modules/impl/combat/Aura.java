package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.exploit.MaceSpoof;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class Aura extends Module {
   public static Aura INSTANCE;
   public static Entity target;
   public final EnumSetting<Aura.Page> page = this.add(new EnumSetting<>("Page", Aura.Page.General));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1F, 14.0, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Aura.Cooldown> cooldownMode = this.add(
      new EnumSetting<>("CooldownMode", Aura.Cooldown.Delay, () -> this.page.getValue() == Aura.Page.General)
   );
   private final BooleanSetting reset = this.add(
      new BooleanSetting("Reset", true, () -> this.page.getValue() == Aura.Page.General && this.cooldownMode.is(Aura.Cooldown.Delay))
   );
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<>("Swing", SwingSide.All, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 1.1F, 0.0, 1.2F, 0.01, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting whileEating = this.add(new BooleanSetting("WhileUsing", true, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting weaponOnly = this.add(new BooleanSetting("WeaponOnly", true, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting ghostHandMace = this.add(
      new BooleanSetting("GhostHandMace", false, () -> this.page.getValue() == Aura.Page.General).setChinese("\u9b3c\u624b\u91cd\u9524")
   );
   private final EnumSetting<Timing> timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == Aura.Page.Target).setParent());
   private final BooleanSetting armorLow = this.add(
      new BooleanSetting("ArmorLow", true, () -> this.page.getValue() == Aura.Page.Target && this.Players.isOpen())
   );
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, () -> this.page.getValue() == Aura.Page.Target));
   private final EnumSetting<Aura.TargetMode> targetMode = this.add(
      new EnumSetting<>("Filter", Aura.TargetMode.DISTANCE, () -> this.page.getValue() == Aura.Page.Target)
   );
   private final EnumSetting<Aura.TargetESP> mode = this.add(
      new EnumSetting<>("TargetESP", Aura.TargetESP.Fill, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting outlineColor = this.add(
      new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting hitOutlineColor = this.add(
      new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final Animation animation = new Animation();
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Aura.Page.Rotate));
   private final BooleanSetting yawStep = this.add(
      new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == Aura.Page.Rotate).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Aura.Page.Rotate)
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting fov = this.add(
      new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.getValue() && this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final Timer tick = new Timer();
   public Vec3d directionVec = null;

   public Aura() {
      super("Aura", Module.Category.Combat);
      this.setChinese("\u6740\u622e\u5149\u73af");
      INSTANCE = this;
   }

   public static void doRender(MatrixStack var0, float var1, Entity var2, Color var3, Color var4, Aura.TargetESP var5) {
      switch (var5) {
         case Fill:
            Render3DUtil.draw3DBox(
               var0,
               ((IEntity)var2)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var2.lastRenderX, var2.getX(), (double)var1),
                        MathUtil.interpolate(var2.lastRenderY, var2.getY(), (double)var1),
                        MathUtil.interpolate(var2.lastRenderZ, var2.getZ(), (double)var1)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               var3,
               var4,
               false,
               true
            );
            break;
         case Box:
            Render3DUtil.draw3DBox(
               var0,
               ((IEntity)var2)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var2.lastRenderX, var2.getX(), (double)var1),
                        MathUtil.interpolate(var2.lastRenderY, var2.getY(), (double)var1),
                        MathUtil.interpolate(var2.lastRenderZ, var2.getZ(), (double)var1)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               var3,
               var4,
               true,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(var0, var2, var3);
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(var0, target, var3);
      }
   }

   public static float getAttackCooldownProgressPerTick() {
      return (float)(1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (target != null) {
         this.doRender(var1, mc.getRenderTickCounter().getTickDelta(true), target, this.mode.getValue());
      }
   }

   public void doRender(MatrixStack var1, float var2, Entity var3, Aura.TargetESP var4) {
      switch (var4) {
         case Fill:
            Render3DUtil.draw3DBox(
               var1,
               ((IEntity)var3)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var3.lastRenderX, var3.getX(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderY, var3.getY(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderZ, var3.getZ(), (double)var2)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               false,
               true
            );
            break;
         case Box:
            Render3DUtil.draw3DBox(
               var1,
               ((IEntity)var3)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var3.lastRenderX, var3.getX(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderY, var3.getY(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderZ, var3.getZ(), (double)var2)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               true,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(var1, var3, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(var1, target, this.color.getValue());
      }
   }

   @Override
   public String getInfo() {
      return target == null ? null : target.getName().getString();
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !var1.isPost()) && (!this.timing.is(Timing.Post) || !var1.isPre())) {
            if (this.weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player)) {
               target = null;
            } else if (this.whileEating.getValue()
               && !this.weaponOnly.getValue()
               && MaceSpoof.INSTANCE.isOn()
               && MaceSpoof.INSTANCE.swap.getValue()
               && mc.player.getMainHandStack().get(DataComponentTypes.FOOD) == null
               && !EntityUtil.isHoldingWeapon(mc.player)) {
               target = null;
            } else {
               target = this.getTarget(this.range.getValueFloat());
               if (target == null) {
                  target = this.getTarget(this.targetRange.getValueFloat());
               } else {
                  this.doAura();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent var1) {
      if (target != null && this.rotate.getValue() && this.shouldYawStep()) {
         this.directionVec = this.getAttackVec(target);
         var1.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send var1) {
      Packet var2;
      if (this.reset.getValue()
         && (
            (var2 = var1.getPacket()) instanceof HandSwingC2SPacket
               || var2 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType((PlayerInteractEntityC2SPacket)var2) == InteractType.ATTACK
         )) {
         this.tick.reset();
      }
   }

   private boolean check() {
      if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return false;
      } else {
         int var1 = (int)(this.tick.getMs() / 50L);
         if (this.cooldownMode.getValue() == Aura.Cooldown.Vanilla) {
            var1 = ((ILivingEntity)mc.player).getLastAttackedTicks();
         }

         double var2 = this.cooldown.getValue();
         if (mc.player.getMainHandStack().getItem() == Items.MACE && MaceSpoof.INSTANCE.isOn() && MaceSpoof.INSTANCE.auraDelay.getValue() >= 0.0) {
            var2 = MaceSpoof.INSTANCE.auraDelay.getValue();
         }

         if (!(Math.max((int)(var1 * Alien.SERVER.getTPSFactor()) / getAttackCooldownProgressPerTick(), 0.0F) >= var2)) {
            return false;
         } else {
            return target instanceof LivingEntity var5 && var5.hurtTime > this.hurtTime.getValue()
               ? false
               : this.whileEating.getValue() || !EntityUtil.isUsingItem();
         }
      }
   }

   private void doAura() {
      if (this.check()) {
         if (!this.rotate.getValue() || this.faceVector(this.getAttackVec(target))) {
            int var1 = -1;
            int var2 = -1;
            boolean var3 = this.ghostHandMace.getValue() && TPMace.INSTANCE != null && TPMace.INSTANCE.isOn();
            if (var3) {
               var1 = this.findMaceSlot();
               if (var1 != -1 && var1 != mc.player.getInventory().selectedSlot) {
                  var2 = mc.player.getInventory().selectedSlot;
                  InventoryUtil.switchToSlot(var1);
               } else {
                  var3 = false;
               }
            }

            this.animation.to = 1.0;
            this.animation.from = 1.0;
            if (var3) {
               mc.interactionManager.attackEntity(mc.player, target);
            } else {
               mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
               mc.player.resetLastAttackedTicks();
            }

            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            if (var3 && var2 != -1) {
               InventoryUtil.switchToSlot(var2);
            }

            this.tick.reset();
            if (this.rotate.getValue() && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   private int findMaceSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getStack(var1).getItem() == Items.MACE) {
            return var1;
         }
      }

      return -1;
   }

   private Vec3d getAttackVec(Entity var1) {
      return MathUtil.getClosestPointToBox(mc.player.getEyePos(), var1.getBoundingBox());
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   public boolean faceVector(Vec3d var1) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(var1);
         return true;
      } else {
         this.directionVec = var1;
         return Alien.ROTATION.inFov(var1, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   public Entity getTarget(double var1) {
      Entity var3 = null;
      boolean var4 = false;
      double var5 = var1;
      double var7 = 36.0;

      for (Entity var10 : Alien.THREAD.getEntities()) {
         if (this.isEnemy(var10)) {
            Vec3d var11 = this.getAttackVec(var10);
            if (!(mc.player.getEyePos().distanceTo(var11) > var1)
               && (mc.player.canSee(var10) || !(mc.player.getEyePos().distanceTo(var11) > this.wallRange.getValue()))
               && CombatUtil.isValid(var10)) {
               if (var3 == null) {
                  var3 = var10;
                  var4 = CombatUtil.isEnemy(var10);
                  var5 = mc.player.getEyePos().distanceTo(var11);
                  var7 = EntityUtil.getHealth(var10);
               } else {
                  boolean var12 = CombatUtil.isEnemy(var10);
                  if (var12 && !var4) {
                     var3 = var10;
                     var4 = true;
                     var5 = mc.player.getEyePos().distanceTo(var11);
                     var7 = EntityUtil.getHealth(var10);
                  } else if (var12 || !var4) {
                     if (this.armorLow.getValue() && var10 instanceof PlayerEntity && EntityUtil.isArmorLow((PlayerEntity)var10, 10)) {
                        var3 = var10;
                        break;
                     }

                     if (this.targetMode.getValue() == Aura.TargetMode.HEALTH && EntityUtil.getHealth(var10) < var7) {
                        var3 = var10;
                        var7 = EntityUtil.getHealth(var10);
                     } else if (this.targetMode.getValue() == Aura.TargetMode.DISTANCE && mc.player.getEyePos().distanceTo(var11) < var5) {
                        var3 = var10;
                        var5 = mc.player.getEyePos().distanceTo(var11);
                     }
                  }
               }
            }
         }
      }

      return var3;
   }

   private boolean isEnemy(Entity var1) {
      if (var1 instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (var1 instanceof PlayerEntity) {
         return this.Players.getValue();
      } else if (var1 instanceof VillagerEntity || var1 instanceof WanderingTraderEntity) {
         return this.Villagers.getValue();
      } else if (var1 instanceof AnimalEntity) {
         return this.Animals.getValue();
      } else {
         return var1 instanceof MobEntity ? this.Mobs.getValue() : false;
      }
   }

   public static enum Cooldown {
      Vanilla,
      Delay;
   }

   public static enum Mode {
      Mace,
      Axe,
      Sword;
   }

   public static enum Page {
      General,
      Rotate,
      Target,
      Render;
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack,
      None;
   }

   private static enum TargetMode {
      DISTANCE,
      HEALTH;
   }
}
