package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public class AutoPot extends Module {
   public static AutoPot INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting speed;
   private final BooleanSetting resistance;
   private final BooleanSetting strength;
   private final BooleanSetting slowFalling;
   private final BooleanSetting usingPause;
   private final BooleanSetting onlyGround;
   private final BooleanSetting inventory;
   private final BindSetting speedKey;
   private final BindSetting strengthKey;
   private final BindSetting resistanceKey;
   private final Timer delayTimer;
   private boolean throwing;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;

   public AutoPot() {
      super("AutoPot", Module.Category.Player);
      this.delay.setChinese("\u5ef6\u8fdf");
      this.speed = this.add(new BooleanSetting("Speed", false));
      this.speed.setChinese("\u901f\u5ea6");
      this.resistance = this.add(new BooleanSetting("Resistance", false));
      this.resistance.setChinese("\u6297\u6027");
      this.strength = this.add(new BooleanSetting("Strength", false));
      this.strength.setChinese("\u529b\u91cf");
      this.slowFalling = this.add(new BooleanSetting("SlowFalling", false));
      this.slowFalling.setChinese("\u7f13\u964d");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.usingPause.setChinese("\u4f7f\u7528\u6682\u505c");
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", false));
      this.onlyGround.setChinese("\u4ec5\u5730\u9762");
      this.inventory = this.add(new BooleanSetting("Inventory", true));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.speedKey = this.add(new BindSetting("SpeedKey", -1));
      this.speedKey.setChinese("\u901f\u5ea6\u952e");
      this.strengthKey = this.add(new BindSetting("StrengthKey", -1));
      this.strengthKey.setChinese("\u529b\u91cf\u952e");
      this.resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
      this.resistanceKey.setChinese("\u6297\u6027\u952e");
      this.delayTimer = new Timer();
      this.throwing = false;
      this.setChinese("\u81ea\u52a8\u836f\u6c34");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoPot.AutoPotTick());
   }

   public static int findPotionInventorySlot(StatusEffect targetEffect) {
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findPotion(StatusEffect targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!this.inventory.getValue() || EntityUtil.inInventory())
         && this.delayTimer.passedMs(this.delay.getValue() * 1000.0)
         && (
            !this.onlyGround.getValue()
               || (mc.player.isOnGround() || Alien.PLAYER.isInWeb(mc.player)) && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))
         )) {
         if (!this.hasNearbyEnemy()) {
            return;
         }

         if (this.resistance.getValue()
            && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
            this.throwing = this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value());
            if (this.isThrow()) {
               this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
               return;
            }
         }

         if (this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            this.throwing = this.checkThrow((StatusEffect)StatusEffects.SPEED.value());
            if (this.isThrow()) {
               this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
               return;
            }
         }

         if (this.strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            this.throwing = this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value());
            if (this.isThrow()) {
               this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
               return;
            }
         }

         if (this.slowFalling.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            this.throwing = this.checkThrow((StatusEffect)StatusEffects.SLOW_FALLING.value());
            if (this.isThrow()) {
               this.throwPotion((StatusEffect)StatusEffects.SLOW_FALLING.value());
            }
         }
      }
   }

   private boolean hasNearbyEnemy() {
      List<PlayerEntity> players = mc.world
         .getEntitiesByClass(
            PlayerEntity.class,
            new Box(
               mc.player.getX() - 16.0,
               mc.player.getY() - 16.0,
               mc.player.getZ() - 16.0,
               mc.player.getX() + 16.0,
               mc.player.getY() + 16.0,
               mc.player.getZ() + 16.0
            ),
            player -> player != mc.player && !Alien.FRIEND.isFriend(player)
         );
      return !players.isEmpty();
   }

   public void throwPotion(StatusEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      } else if ((newSlot = findPotion(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.switchToSlot(oldSlot);
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow(StatusEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem()
            ? false
            : findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   public class AutoPotTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost() && (!AutoPot.this.inventory.getValue() || EntityUtil.inInventory())) {
            if (Wrapper.mc.currentScreen == null) {
               if (AutoPot.this.resistanceKey.isPressed()) {
                  if (!AutoPot.this.turtlePress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) {
                     if (AutoPot.this.hasNearbyEnemy()) {
                        AutoPot.this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
                        AutoPot.this.turtlePress = true;
                     }

                     return;
                  }
               } else {
                  AutoPot.this.turtlePress = false;
               }

               if (AutoPot.this.strengthKey.isPressed()) {
                  if (!AutoPot.this.strengthPress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value())) {
                     if (AutoPot.this.hasNearbyEnemy()) {
                        AutoPot.this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
                        AutoPot.this.strengthPress = true;
                     }

                     return;
                  }
               } else {
                  AutoPot.this.strengthPress = false;
               }

               if (AutoPot.this.speedKey.isPressed()) {
                  if (!AutoPot.this.speedPress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.SPEED.value()) && AutoPot.this.hasNearbyEnemy()) {
                     AutoPot.this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
                     AutoPot.this.speedPress = true;
                  }
               } else {
                  AutoPot.this.speedPress = false;
               }
            } else {
               AutoPot.this.speedPress = false;
               AutoPot.this.turtlePress = false;
               AutoPot.this.strengthPress = false;
            }
         }
      }
   }
}
