package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;

public class MaceGodPlus extends Module {
   public static MaceGodPlus INSTANCE;
   private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", 6.0, 1.0, 20.0, 0.1).setChinese("\u653b\u51fb\u8ddd\u79bb"));
   private final SliderSetting voidY = this.add(new SliderSetting("VoidY", -70.0, -500.0, 320.0, 1.0).setChinese("\u865a\u7a7a\u6df1\u5ea6"));
   private final SliderSetting apt = this.add(new SliderSetting("APT", 1.0, 1.0, 20.0, 1.0).setChinese("\u653b\u51fb\u6b21\u6570"));
   private final BooleanSetting autoMace = this.add(new BooleanSetting("AutoMace", true).setChinese("\u81ea\u52a8\u6362\u91cd\u9524"));
   private final BooleanSetting mobs = this.add(new BooleanSetting("Mobs", false).setChinese("\u653b\u51fb\u602a\u7269"));
   private final BooleanSetting disableInteract = this.add(new BooleanSetting("DisableInteract", true).setChinese("\u5173\u95ed\u4ea4\u4e92\u589e\u5f3a"));
   private final BooleanSetting autoCrystalSync = this.add(new BooleanSetting("AutoCrystalSync", false).setChinese("\u8054\u52a8\u6c34\u6676\u5149\u73af"));
   private int prevSlot = -1;
   private boolean switchedByMe = false;
   private boolean interactDisabledByMe = false;
   private boolean crystalEnabledByMe = false;

   public MaceGodPlus() {
      super("MaceGodPlus", "1-tick void teleport, attack, restore for massive Mace fall damage.", Module.Category.Combat);
      this.setChinese("\u865a\u7a7a\u9524plus");
      INSTANCE = this;
   }

   public static boolean shouldDisableNoFall() {
      return INSTANCE != null && INSTANCE.isOn();
   }

   @Override
   public void onEnable() {
      if (!Module.nullCheck()) {
         this.switchedByMe = false;
         this.prevSlot = -1;
         if (this.disableInteract.getValue() && InteractTweaks.INSTANCE != null && InteractTweaks.INSTANCE.isOn()) {
            InteractTweaks.INSTANCE.disable();
            this.interactDisabledByMe = true;
         }

         if (this.autoMace.getValue()) {
            this.switchToMace();
         }

         if (this.autoCrystalSync.getValue()) {
            AutoCrystal ac = AutoCrystal.INSTANCE;
            if (ac != null && !ac.isOn()) {
               ac.enable();
               this.crystalEnabledByMe = true;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.interactDisabledByMe && InteractTweaks.INSTANCE != null && !InteractTweaks.INSTANCE.isOn()) {
         InteractTweaks.INSTANCE.enable();
      }

      this.interactDisabledByMe = false;
      this.restoreSlot();
      if (this.autoCrystalSync.getValue() && this.crystalEnabledByMe) {
         AutoCrystal ac = AutoCrystal.INSTANCE;
         if (ac != null && ac.isOn()) {
            ac.disable();
         }

         this.crystalEnabledByMe = false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!event.isPost()) {
         if (!nullCheck() && mc.getNetworkHandler() != null) {
            Entity target = this.findTarget();
            if (target == null) {
               if (this.autoMace.getValue()) {
                  this.switchToMace();
               }
            } else {
               if (this.autoMace.getValue()) {
                  this.switchToMace();
               }

               double px = mc.player.getX();
               double py = mc.player.getY();
               double pz = mc.player.getZ();
               double vy = this.voidY.getValue();
               int attacks = (int)this.apt.getValue();

               for (int i = 0; i < attacks; i++) {
                  this.sendPos(px, vy, pz);
                  this.attack(target);
               }

               this.sendPos(px, py, pz);
            }
         }
      }
   }

   private void attack(Entity target) {
      mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
      mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void sendPos(double x, double y, double z) {
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, false));
   }

   private void switchToMace() {
      if (!Module.nullCheck()) {
         int maceSlot = this.findMaceSlot();
         if (maceSlot != -1) {
            if (!this.switchedByMe) {
               this.prevSlot = mc.player.getInventory().selectedSlot;
               this.switchedByMe = true;
            }

            if (mc.player.getInventory().selectedSlot != maceSlot) {
               mc.player.getInventory().selectedSlot = maceSlot;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
            }
         }
      }
   }

   private void restoreSlot() {
      if (this.switchedByMe
         && this.prevSlot != -1
         && mc.player != null
         && mc.getNetworkHandler() != null
         && mc.player.getInventory().selectedSlot != this.prevSlot) {
         mc.player.getInventory().selectedSlot = this.prevSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.prevSlot));
      }

      this.switchedByMe = false;
      this.prevSlot = -1;
   }

   private int findMaceSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
            return i;
         }
      }

      return -1;
   }

   private Entity findTarget() {
      double range = this.attackRange.getValue();
      double rangeSq = range * range;
      PlayerEntity playerTarget = null;
      double playerDist = Double.MAX_VALUE;

      for (PlayerEntity p : mc.world.getPlayers()) {
         if (p != mc.player && p.isAlive() && !p.isDead() && !Alien.FRIEND.isFriend(p)) {
            double d = mc.player.squaredDistanceTo(p);
            if (d <= rangeSq && d < playerDist) {
               playerDist = d;
               playerTarget = p;
            }
         }
      }

      if (playerTarget != null) {
         return playerTarget;
      } else if (!this.mobs.getValue()) {
         return null;
      } else {
         Entity mobTarget = null;
         double mobDist = Double.MAX_VALUE;

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof MobEntity mob && !(e instanceof AnimalEntity) && mob.isAlive() && !mob.isDead()) {
               double d = mc.player.squaredDistanceTo(e);
               if (d <= rangeSq && d < mobDist) {
                  mobDist = d;
                  mobTarget = e;
               }
            }
         }

         return mobTarget;
      }
   }
}
