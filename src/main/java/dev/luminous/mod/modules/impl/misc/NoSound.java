package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.util.ArrayList;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class NoSound extends Module {
   static final ArrayList<SoundEvent> armor = new ArrayList<>();
   public static NoSound INSTANCE;
   private final BooleanSetting equip = this.add(new BooleanSetting("Equip", true));
   private final BooleanSetting explode;
   private final BooleanSetting attack;
   private final BooleanSetting teleport;
   private final BooleanSetting throwConfig;
   private final BooleanSetting potion;
   private final BooleanSetting elytra;

   public NoSound() {
      super("NoSound", Module.Category.Misc);
      this.equip.setChinese("\u76d4\u7532\u7a7f\u6234");
      this.explode = this.add(new BooleanSetting("Explode", true));
      this.explode.setChinese("\u7206\u70b8");
      this.attack = this.add(new BooleanSetting("Attack", true));
      this.attack.setChinese("\u653b\u51fb");
      this.teleport = this.add(new BooleanSetting("Teleport", true));
      this.teleport.setChinese("\u4f20\u9001");
      this.throwConfig = this.add(new BooleanSetting("ThrowConfig", true));
      this.throwConfig.setChinese("\u6295\u63b7");
      this.potion = this.add(new BooleanSetting("Potion", true));
      this.potion.setChinese("\u836f\u6c34");
      this.elytra = this.add(new BooleanSetting("Elytra", true));
      this.elytra.setChinese("\u9798\u7fc5");
      this.setChinese("\u53bb\u9664\u58f0\u97f3");
      INSTANCE = this;
   }

   @EventListener
   public void onPlaySound(PlaySoundEvent event) {
      if (this.equip.getValue()) {
         for (SoundEvent se : armor) {
            if (event.sound.getId() == se.getId()) {
               event.cancel();
               return;
            }
         }
      }

      if (!this.explode.getValue()
         || event.sound.getId() != ((SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value()).getId()
            && event.sound.getId() != SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE.getId()) {
         if (this.attack.getValue()
            && (
               event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_WEAK.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG.getId()
            )) {
            event.cancel();
         }

         if (this.teleport.getValue() && event.sound.getId() == SoundEvents.ENTITY_PLAYER_TELEPORT.getId()) {
            event.cancel();
         }

         if (this.potion.getValue() && event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_BREAK.getId()) {
            event.cancel();
         }

         if (this.elytra.getValue() && event.sound.getId() == SoundEvents.ITEM_ELYTRA_FLYING.getId()) {
            event.cancel();
         }

         if (this.throwConfig.getValue()
            && (
               event.sound.getId() == SoundEvents.ENTITY_ENDER_PEARL_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_EGG_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_SNOWBALL_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_THROW.getId()
            )) {
            event.cancel();
         }
      } else {
         event.cancel();
      }
   }

   static {
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_TURTLE.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_IRON.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value());
      armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value());
   }
}
