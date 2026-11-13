package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class TotemSound extends Module {
   public static TotemSound INSTANCE;
   private static final Identifier CUSTOM_ID = Identifier.of("minecraft", "alien.totemsound.pop");
   private static final Identifier VANILLA_TOTEM = Identifier.of("minecraft", "item.totem.use");
   private final BooleanSetting playSound = this.add(new BooleanSetting("PlaySound", true));
   private long lastEnemyTotemAt = 0L;
   private static final long MATCH_WINDOW_MS = 250L;

   public TotemSound() {
      super("TotemSound", Module.Category.Misc);
      this.setChinese("\u7206\u56fe\u817e\u97f3\u6548");
      INSTANCE = this;
      this.playSound.setChinese("\u64ad\u653e\u97f3\u6548");
   }

   @EventListener
   public void onTotem(TotemEvent var1) {
      PlayerEntity var2 = var1.getPlayer();
      if (var2 != null) {
         if (MinecraftClient.getInstance().player != null) {
            if (!var2.equals(MinecraftClient.getInstance().player)) {
               if (Alien.FRIEND == null || !Alien.FRIEND.isFriend(var2)) {
                  this.lastEnemyTotemAt = System.currentTimeMillis();
               }
            }
         }
      }
   }

   @EventListener
   public void onPlaySound(PlaySoundEvent var1) {
      if (this.playSound.getValue()) {
         if (this.lastEnemyTotemAt != 0L) {
            if (System.currentTimeMillis() - this.lastEnemyTotemAt <= 250L) {
               SoundInstance var2 = var1.sound;
               if (var2 != null) {
                  Identifier var3 = var2.getId();
                  if (var3 != null && var3.equals(VANILLA_TOTEM)) {
                     var1.cancel();
                     this.lastEnemyTotemAt = 0L;
                     this.playTotemSound();
                  }
               }
            }
         }
      }
   }

   private void playTotemSound() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.getSoundManager() != null) {
         var1.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(CUSTOM_ID), 1.0F));
      }
   }
}
