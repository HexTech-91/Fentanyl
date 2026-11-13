package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class TPMace extends Module {
   public static TPMace INSTANCE;
   private final EnumSetting<TPMace.TPDirection> tpDirection = this.add(new EnumSetting<>("Direction", TPMace.TPDirection.VOID).setChinese("\u65b9\u5411"));
   private final SliderSetting voidDepth = this.add(
      new SliderSetting("VoidDepth", 100.0, 10.0, 300.0, 10.0).setSuffix("m").setChinese("\u865a\u7a7a\u6df1\u5ea6")
   );
   private final SliderSetting lagHeight = this.add(new SliderSetting("LagHeight", 1.0, 0.3, 3.0, 0.1).setSuffix("m").setChinese("\u653b\u51fb\u9ad8\u5ea6"));
   private final BooleanSetting vclip = this.add(new BooleanSetting("VClip", true).setChinese("\u5782\u76f4\u4f20\u9001"));
   private final SliderSetting clipJump = this.add(
      new SliderSetting("ClipJump", 256.0, 50.0, 1000.0, 10.0, this.vclip::getValue).setSuffix("m").setChinese("\u63a9\u62a4\u8df3\u9ad8\u5ea6")
   );
   private final SliderSetting voidTicks = this.add(new SliderSetting("VoidTicks", 0.0, 0.0, 10.0, 1.0).setSuffix("t").setChinese("\u865a\u7a7a\u7b49\u5f85"));
   private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 0.8, 0.1, 1.0, 0.05).setChinese("\u653b\u51fb\u51b7\u5374"));
   private final BooleanSetting requireMace = this.add(new BooleanSetting("RequireMace", true).setChinese("\u9700\u8981\u91cd\u9524"));
   private final BooleanSetting healthVoid;
   private final SliderSetting hvThreshold1;
   private final SliderSetting hvThreshold2;
   private final SliderSetting hvThreshold3;
   private final SliderSetting hvThreshold4;
   private final SliderSetting hvDepth1;
   private final SliderSetting hvDepth2;
   private final SliderSetting hvDepth3;
   private final SliderSetting hvDepth4;
   private final SliderSetting hvDepth5;
   private final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, this.requireMace::getValue).setChinese("\u81ea\u52a8\u5207\u6362"));
   private final BooleanSetting playersOnly = this.add(new BooleanSetting("PlayersOnly", true).setChinese("\u4ec5\u73a9\u5bb6"));
   private final BooleanSetting autoAura = this.add(new BooleanSetting("AutoAura", true).setChinese("\u81ea\u52a8\u5149\u73af"));
   private final BooleanSetting debug = this.add(new BooleanSetting("Debug", false).setChinese("\u8c03\u8bd5"));
   private final BooleanSetting storeCooldown;
   private TPMace.Phase phase;
   private int voidTicksRemaining;
   private boolean forcingLanding;
   private double snapX;
   private double snapY;
   private double snapZ;
   private double computedVoidY;
   private double lagY;
   private boolean sessionBurrowed;
   private boolean sessionGoUp;
   private int savedSlot;
   public static Entity target;
   private boolean auraWasOn;
   private int tickCounter;
   private boolean attackUnlocked;
   public int storedCount;
   public int storeAccumTicks;
   private final EnumSetting<TPMace.SwapMode> ghostHand;
   private final BooleanSetting whileEating;
   private int silentRestoreSlot = -1;

   public TPMace() {
      super("TPMace", "Void TP mace loop: never lands until disabled.", Module.Category.Combat);
      BooleanSetting var1 = this.add(new BooleanSetting("StoreCooldown", true).setChinese("\u50a8\u5b58\u51b7\u5374"));
      this.ghostHand = this.add(new EnumSetting<>("GhostHand", TPMace.SwapMode.Silent));
      this.ghostHand.setChinese("\u9759\u9ed8\u7528\u9524");
      this.whileEating = this.add(new BooleanSetting("WhileEating", true, () -> this.ghostHand.getValue() != TPMace.SwapMode.None));
      this.whileEating.setChinese("\u8fdb\u98df\u65f6\u4e5f\u51fa\u624b");
      this.storeCooldown = var1;
      this.healthVoid = this.add(new BooleanSetting("HealthVoid", false).setChinese("\u6839\u636e\u8840\u91cf\u9009\u865a\u7a7a"));
      this.hvThreshold1 = this.add(new SliderSetting("Threshold1", 32.0, 0.0, 60.0, 1.0, this.healthVoid::getValue).setChinese("\u8840\u91cf\u9608\u503c1"));
      this.hvThreshold2 = this.add(new SliderSetting("Threshold2", 24.0, 0.0, 60.0, 1.0, this.healthVoid::getValue).setChinese("\u8840\u91cf\u9608\u503c2"));
      this.hvThreshold3 = this.add(new SliderSetting("Threshold3", 16.0, 0.0, 60.0, 1.0, this.healthVoid::getValue).setChinese("\u8840\u91cf\u9608\u503c3"));
      this.hvThreshold4 = this.add(new SliderSetting("Threshold4", 8.0, 0.0, 60.0, 1.0, this.healthVoid::getValue).setChinese("\u8840\u91cf\u9608\u503c4"));
      this.hvDepth1 = this.add(
         new SliderSetting("Depth1", 180.0, 10.0, 300.0, 10.0, this.healthVoid::getValue).setSuffix("m").setChinese("\u8840\u91cf\u6df1\u5ea61")
      );
      this.hvDepth2 = this.add(
         new SliderSetting("Depth2", 140.0, 10.0, 300.0, 10.0, this.healthVoid::getValue).setSuffix("m").setChinese("\u8840\u91cf\u6df1\u5ea62")
      );
      this.hvDepth3 = this.add(
         new SliderSetting("Depth3", 100.0, 10.0, 300.0, 10.0, this.healthVoid::getValue).setSuffix("m").setChinese("\u8840\u91cf\u6df1\u5ea63")
      );
      this.hvDepth4 = this.add(
         new SliderSetting("Depth4", 60.0, 10.0, 300.0, 10.0, this.healthVoid::getValue).setSuffix("m").setChinese("\u8840\u91cf\u6df1\u5ea64")
      );
      this.hvDepth5 = this.add(
         new SliderSetting("Depth5", 30.0, 10.0, 300.0, 10.0, this.healthVoid::getValue).setSuffix("m").setChinese("\u8840\u91cf\u6df1\u5ea65")
      );
      this.phase = TPMace.Phase.IDLE;
      this.voidTicksRemaining = 0;
      this.forcingLanding = false;
      this.sessionBurrowed = false;
      this.sessionGoUp = false;
      this.savedSlot = -1;
      this.auraWasOn = false;
      this.tickCounter = 0;
      this.attackUnlocked = false;
      this.storedCount = 0;
      this.storeAccumTicks = 0;
      this.setChinese("\u865a\u7a7a\u91cd\u9524");
      INSTANCE = this;
   }

   public static boolean shouldDisableNoFall() {
      return INSTANCE != null && INSTANCE.isOn();
   }

   public static boolean shouldDisableCriticals() {
      return false;
   }

   public static void storeCooldownTick() {
      if (INSTANCE != null
         && INSTANCE.storeCooldown.getValue()
         && !INSTANCE.isOn()
         && mc != null
         && mc.player != null
         && ++INSTANCE.storeAccumTicks >= (int)Aura.getAttackCooldownProgressPerTick()) {
         INSTANCE.storeAccumTicks = 0;
         if (INSTANCE.storedCount < 100) {
            INSTANCE.storedCount++;
         }
      }
   }

   @Override
   public String getInfo() {
      if (mc != null && mc.player != null && mc.world != null) {
         double var1 = mc.player.fallDistance;
         switch (this.phase) {
            case VOID_SENT:
               if (this.voidTicksRemaining > 0) {
                  return String.format("\u00a7cVOID\u00a7r %.0ffall \u00a77(%dt)", var1, this.voidTicksRemaining);
               }

               return String.format("\u00a7cVOID\u00a7r %.0ffall", var1);
            case ATTACKING:
               return String.format("\u00a76ATK\u00a7r %.0ffall", var1);
            case HOLD:
               return String.format("\u00a7eHOLD\u00a7r %.0ffall", var1);
            default:
               return target != null ? String.format("idle | %s", target.getName().getString()) : "idle";
         }
      } else {
         return "off";
      }
   }

   @Override
   public void onEnable() {
      this.phase = TPMace.Phase.IDLE;
      this.voidTicksRemaining = 0;
      this.forcingLanding = false;
      this.tickCounter = 0;
      target = null;
      this.savedSlot = -1;
      this.attackUnlocked = false;
      this.sessionBurrowed = false;
      this.sessionGoUp = false;
      this.auraWasOn = Aura.INSTANCE != null && Aura.INSTANCE.isOn();
   }

   @Override
   public void onDisable() {
      if (this.phase != TPMace.Phase.IDLE && mc != null && mc.player != null && mc.getNetworkHandler() != null) {
         this.forcingLanding = true;

         try {
            mc.player.setPosition(this.snapX, this.snapY, this.snapZ);
            mc.player.setOnGround(true);
            mc.player.fallDistance = 0.0F;
            mc.player.setVelocity(0.0, 0.0, 0.0);
            this.sendPos(this.snapX, this.snapY, this.snapZ, true);
         } finally {
            this.forcingLanding = false;
         }
      }

      this.phase = TPMace.Phase.IDLE;
      this.voidTicksRemaining = 0;
      target = null;
      this.attackUnlocked = false;
      if (this.autoAura.getValue() && !this.auraWasOn && Aura.INSTANCE != null && Aura.INSTANCE.isOn()) {
         Aura.INSTANCE.disable();
      }

      if (this.savedSlot != -1 && mc != null && mc.player != null) {
         this.restoreSlot(this.savedSlot);
         this.savedSlot = -1;
      }
   }

   @Override
   public void onLogout() {
      this.onDisable();
   }

   @EventListener(
      priority = 250
   )
   public void onPacketSend(PacketEvent.Send var1) {
      Packet var2;
      if (!nullCheck() && this.phase != TPMace.Phase.IDLE && !this.forcingLanding && (var2 = var1.getPacket()) instanceof PlayerMoveC2SPacket) {
         PlayerMoveC2SPacket var3 = (PlayerMoveC2SPacket)var2;
         ((IPlayerMoveC2SPacket)var3).setOnGround(false);
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive var1) {
      Packet var2;
      if (!nullCheck() && this.debug.getValue() && this.phase != TPMace.Phase.IDLE && (var2 = var1.getPacket()) instanceof PlayerPositionLookS2CPacket) {
         PlayerPositionLookS2CPacket var3 = (PlayerPositionLookS2CPacket)var2;
         CommandManager.sendMessage(String.format("\u00a7c[TPMace] \u6a61\u76ae\u7b4b! phase=%s \u670d\u52a1\u5668\u8bf4Y=%.2f", this.phase, var3.getY()));
      }
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (this.silentRestoreSlot != -1) {
         InventoryUtil.switchToSlot(this.silentRestoreSlot);
         this.silentRestoreSlot = -1;
      }

      if (!nullCheck() && !var1.isPost()) {
         this.tickCounter++;
         switch (this.phase) {
            case IDLE:
               this.tickIdle();
               break;
            case VOID_SENT:
               this.tickVoidWait();
               break;
            case ATTACKING:
               this.tickAttack();
               break;
            case HOLD:
               this.tickHold();
         }
      }
   }

   private boolean isBurrowed() {
      if (mc.player != null && mc.world != null) {
         Box var1 = mc.player.getBoundingBox(EntityPose.STANDING);
         BlockPos var2 = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());

         for (int var3 = -1; var3 <= 1; var3++) {
            for (int var4 = 0; var4 <= 2; var4++) {
               for (int var5 = -1; var5 <= 1; var5++) {
                  BlockPos var8 = var2.add(var3, var4, var5);
                  BlockState var9 = mc.world.getBlockState(var8);
                  VoxelShape var7;
                  if (!var9.isAir() && !(var7 = var9.getCollisionShape(mc.world, var8)).isEmpty() && var1.intersects(var7.getBoundingBox().offset(var8))) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean hasHeadroom() {
      if (mc.player != null && mc.world != null) {
         double var1 = mc.player.getWidth() / 2.0;
         double var3 = mc.player.getY() + mc.player.getHeight();
         Box var5 = new Box(mc.player.getX() - var1, var3, mc.player.getZ() - var1, mc.player.getX() + var1, var3 + 1.0, mc.player.getZ() + var1);
         return mc.world.isSpaceEmpty(mc.player, var5);
      } else {
         return false;
      }
   }

   private double findClearY(double var1) {
      double var3 = mc.player.getWidth() / 2.0;
      double var5 = mc.player.getHeight();

      for (double var7 = 0.0; var7 <= 3.0; var7 += 0.1) {
         double var9 = var1 + var7;
         Box var11 = new Box(this.snapX - var3, var9, this.snapZ - var3, this.snapX + var3, var9 + var5, this.snapZ + var3);
         if (mc.world.isSpaceEmpty(mc.player, var11)) {
            return var9;
         }
      }

      return var1 + Math.max(this.lagHeight.getValue() * 3.0, 2.0);
   }

   private void tickIdle() {
      boolean var1 = this.isBurrowed();
      if (mc.player.isOnGround() || var1) {
         target = this.findTarget();
         this.trySwitchToMace();
         boolean var2 = this.tpDirection.is(TPMace.TPDirection.HIGH) ? this.hasHeadroom() : false;
         if (this.debug.getValue()) {
            CommandManager.sendMessage(
               String.format(
                  "\u00a7a[TPMace] \u542f\u52a8\u5faa\u73af: burrow=%b goUp=%b target=%s", var1, var2, target != null ? target.getName().getString() : "\u65e0"
               )
            );
         }

         this.snapX = mc.player.getX();
         this.snapY = mc.player.getY();
         this.snapZ = mc.player.getZ();
         this.sessionBurrowed = var1;
         this.sessionGoUp = var2;
         double var3 = this.lagY = var1 ? this.findClearY(this.snapY) : this.snapY + this.lagHeight.getValue();
         if (this.debug.getValue() && var1) {
            CommandManager.sendMessage(
               String.format("\u00a7b[TPMace] burrow\u653b\u51fb\u4f4d\u7f6e: snapY=%.2f -> lagY(\u8131\u79bb\u65b9\u5757)=%.2f", this.snapY, this.lagY)
            );
         }

         this.sendPos(this.snapX, this.snapY, this.snapZ, true);
         this.enterVoid();
      } else if (this.debug.getValue()) {
         CommandManager.sendMessage("\u00a77[TPMace] \u672a\u542f\u52a8: \u4e0d\u5728\u5730\u9762\u4e14\u672a\u68c0\u6d4b\u5230burrow");
      }
   }

   private void enterVoid() {
      double var1 = mc.player.getY();
      boolean var3 = this.sessionGoUp;
      double var4 = this.computedVoidY = var3 ? this.snapY + this.getVoidDepthForTarget() : mc.world.getBottomY() - this.getVoidDepthForTarget();
      if (this.vclip.getValue()) {
         double var6 = var3 ? var1 - this.clipJump.getValue() : var1 + this.clipJump.getValue();
         this.sendPos(this.snapX, var6, this.snapZ, false);
      }

      this.sendPos(this.snapX, this.computedVoidY, this.snapZ, false);
      if (this.voidTicks.getValue() <= 0.0) {
         this.sendPos(this.snapX, this.lagY, this.snapZ, false);
         mc.player.setPosition(this.snapX, this.lagY, this.snapZ);
         mc.player.setOnGround(false);
         mc.player.fallDistance = (float)Math.abs(this.snapY - this.computedVoidY);
         mc.player.setVelocity(0.0, 0.0, 0.0);
         if (this.debug.getValue()) {
            CommandManager.sendMessage(
               String.format(
                  "\u00a7b[TPMace] \u5408\u5e76\u8df3\u8dc3(\u540ctick %s+\u56de\u7a0b) burrow=%b goUp=%b: snapY=%.2f -> extremeY=%.2f -> lagY=%.2f fallDist=%.1f",
                  var3 ? "\u4e0a" : "\u4e0b",
                  this.sessionBurrowed,
                  var3,
                  this.snapY,
                  this.computedVoidY,
                  this.lagY,
                  mc.player.fallDistance
               )
            );
         }

         this.phase = TPMace.Phase.ATTACKING;
      } else {
         this.voidTicksRemaining = (int)this.voidTicks.getValue();
         this.phase = TPMace.Phase.VOID_SENT;
         mc.player.setPosition(this.snapX, this.computedVoidY, this.snapZ);
         mc.player.setOnGround(false);
         mc.player.fallDistance = (float)Math.abs(this.snapY - this.computedVoidY);
         mc.player.setVelocity(0.0, var3 ? 1.0 : -1.0, 0.0);
         if (this.debug.getValue()) {
            CommandManager.sendMessage(
               String.format(
                  "\u00a7b[TPMace] enterVoid(%s) burrow=%b goUp=%b: snapY=%.2f -> extremeY=%.2f fallDist=%.1f",
                  var3 ? "\u4e0a\u51b2" : "\u4e0b\u6f5c",
                  this.sessionBurrowed,
                  var3,
                  this.snapY,
                  this.computedVoidY,
                  mc.player.fallDistance
               )
            );
         }
      }
   }

   private void tickVoidWait() {
      if (this.voidTicksRemaining > 0) {
         this.sendPos(this.snapX, this.computedVoidY, this.snapZ, false);
         mc.player.setPosition(this.snapX, this.computedVoidY, this.snapZ);
         mc.player.setOnGround(false);
         mc.player.fallDistance = (float)Math.abs(this.snapY - this.computedVoidY);
         mc.player.setVelocity(0.0, this.sessionGoUp ? 1.0 : -1.0, 0.0);
         this.voidTicksRemaining--;
      } else {
         this.phase = TPMace.Phase.ATTACKING;
      }
   }

   private void tickAttack() {
      this.sendPos(this.snapX, this.lagY, this.snapZ, false);
      mc.player.setPosition(this.snapX, this.lagY, this.snapZ);
      mc.player.setOnGround(false);
      mc.player.fallDistance = (float)Math.abs(this.snapY - this.computedVoidY);
      mc.player.setVelocity(0.0, 0.0, 0.0);
      if (!this.attackUnlocked) {
         if (this.storedCount > 0) {
            this.storedCount--;
            this.attackUnlocked = true;
            this.enableAura();
            if (this.debug.getValue()) {
               CommandManager.sendMessage(
                  String.format(
                     "\u00a7b[TPMace] \u6d88\u8017\u50a8\u5b58\u51b7\u5374\uff0c\u5269\u4f59 %d \u6b21\uff0c\u7acb\u5373\u51fa\u624b", this.storedCount
                  )
               );
            }
         } else {
            float var1 = this.getCooldownProgress();
            if (var1 < this.cooldown.getValueFloat()) {
               if (this.debug.getValue()) {
                  CommandManager.sendMessage(
                     String.format("\u00a77[TPMace] \u7b49\u51b7\u5374(%.2f/%.2f)\uff0c\u672c\u8f6e\u53ea\u8df3\u4e0d\u6253", var1, this.cooldown.getValue())
                  );
               }

               this.continueLoop();
               return;
            }

            this.attackUnlocked = true;
            this.enableAura();
            if (this.debug.getValue()) {
               CommandManager.sendMessage(String.format("\u00a7a[TPMace] \u51b7\u5374\u5230\u4f4d(%.2f)\uff0c\u5df2\u6253\u5f00Aura", var1));
            }
         }
      }

      if (this.savedSlot != -1) {
         this.restoreSlot(this.savedSlot);
         this.savedSlot = -1;
      }

      this.continueLoop();
   }

   private void enableAura() {
      if (this.ghostHand.getValue() == TPMace.SwapMode.None || target == null || !this.attackWithMace(target)) {
         if (this.autoAura.getValue() && Aura.INSTANCE != null && !Aura.INSTANCE.isOn()) {
            Aura.INSTANCE.enable();
         }
      }
   }

   private void tickHold() {
      this.sendPos(this.snapX, this.lagY, this.snapZ, false);
      mc.player.setPosition(this.snapX, this.lagY, this.snapZ);
      mc.player.setOnGround(false);
      mc.player.setVelocity(0.0, 0.0, 0.0);
      this.continueLoop();
   }

   private void continueLoop() {
      target = this.findTarget();
      if (target != null) {
         this.trySwitchToMace();
      }

      this.enterVoid();
   }

   private Entity findTarget() {
      Entity var1 = Aura.target;
      if (var1 == null || var1 == mc.player) {
         return null;
      } else if (!var1.isAlive() || var1.isRemoved()) {
         return null;
      } else if (this.playersOnly.getValue() && !(var1 instanceof PlayerEntity)) {
         return null;
      } else {
         return var1 instanceof PlayerEntity && Alien.FRIEND.isFriend((PlayerEntity)var1) ? null : var1;
      }
   }

   private double getVoidDepthForTarget() {
      if (this.healthVoid.getValue() && target != null && target instanceof LivingEntity) {
         float var1 = ((LivingEntity)target).getAbsorptionAmount() + ((LivingEntity)target).getHealth();
         if (var1 >= this.hvThreshold1.getValueFloat()) {
            return this.hvDepth1.getValue();
         } else if (var1 >= this.hvThreshold2.getValueFloat()) {
            return this.hvDepth2.getValue();
         } else if (var1 >= this.hvThreshold3.getValueFloat()) {
            return this.hvDepth3.getValue();
         } else {
            return var1 >= this.hvThreshold4.getValueFloat() ? this.hvDepth4.getValue() : this.hvDepth5.getValue();
         }
      } else {
         return this.voidDepth.getValue();
      }
   }

   private float getCooldownProgress() {
      int var1 = ((ILivingEntity)mc.player).getLastAttackedTicks();
      var1 = (int)(var1 * Alien.SERVER.getTPSFactor());
      return Math.max(var1 / Aura.getAttackCooldownProgressPerTick(), 0.0F);
   }

   private void trySwitchToMace() {
      if (mc.player != null) {
         if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
            if (this.findMaceSlot() != -1 || this.findAnyMaceSlot() != -1) {
               if (this.ghostHand.getValue() == TPMace.SwapMode.None) {
                  if (this.requireMace.getValue() && this.autoSwitch.getValue()) {
                     int var1 = this.findMaceSlot();
                     if (var1 != -1) {
                        if (this.savedSlot == -1) {
                           this.savedSlot = mc.player.getInventory().selectedSlot;
                        }

                        InventoryUtil.switchToSlot(var1);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean attackWithMace(Entity var1) {
      if (mc.player != null && var1 != null && var1.isAlive()) {
         if (mc.player.getMainHandStack().isOf(Items.MACE)) {
            mc.interactionManager.attackEntity(mc.player, var1);
            return true;
         } else if (this.ghostHand.getValue() == TPMace.SwapMode.None) {
            return false;
         } else if (!this.whileEating.getValue() && EntityUtil.isUsingItem()) {
            return false;
         } else {
            int var2 = this.resolveMaceSlot();
            if (var2 == -1) {
               return false;
            } else {
               int var3 = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(var2);
               mc.interactionManager.attackEntity(mc.player, var1);
               EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
               if (this.ghostHand.getValue() == TPMace.SwapMode.Silent) {
                  InventoryUtil.switchToSlot(var3);
               } else {
                  this.silentRestoreSlot = var3;
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private int resolveMaceSlot() {
      int var1 = this.findMaceSlot();
      if (var1 != -1) {
         return var1;
      } else if (this.ghostHand.getValue() != TPMace.SwapMode.Inventory) {
         return -1;
      } else {
         int var2 = this.findAnyMaceSlot();
         if (var2 == -1) {
            return -1;
         } else {
            int var3 = mc.player.getInventory().selectedSlot;
            InventoryUtil.inventorySwap(var2, var3);
            EntityUtil.syncInventory();
            return var3;
         }
      }
   }

   private int findAnyMaceSlot() {
      for (int var1 = 0; var1 < 36; var1++) {
         if (mc.player.getInventory().getStack(var1).isOf(Items.MACE)) {
            return var1;
         }
      }

      return -1;
   }

   private int findMaceSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getStack(var1).isOf(Items.MACE)) {
            return var1;
         }
      }

      return -1;
   }

   private void restoreSlot(int var1) {
      if (var1 >= 0 && var1 <= 8 && var1 != mc.player.getInventory().selectedSlot) {
         InventoryUtil.switchToSlot(var1);
      }
   }

   private void sendPos(double var1, double var3, double var5, boolean var7) {
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(var1, var3, var5, var7));
   }

   private static enum Phase {
      IDLE,
      VOID_SENT,
      ATTACKING,
      HOLD;
   }

   private static enum SwapMode {
      None,
      Normal,
      Silent,
      Inventory;
   }

   private static enum TPDirection {
      VOID,
      HIGH;
   }
}
