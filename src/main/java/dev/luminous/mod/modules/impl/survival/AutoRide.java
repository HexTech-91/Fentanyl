package dev.luminous.mod.modules.impl.survival;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.RideAnimalSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class AutoRide extends Module {
   public static final Set<String> RIDEABLE = new HashSet<>(
      Arrays.asList(
         "minecraft:horse",
         "minecraft:donkey",
         "minecraft:mule",
         "minecraft:llama",
         "minecraft:trader_llama",
         "minecraft:camel",
         "minecraft:pig",
         "minecraft:strider",
         "minecraft:skeleton_horse",
         "minecraft:zombie_horse"
      )
   );
   public static AutoRide INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("RideRange", 16.0, 2.0, 48.0, 1.0).setSuffix("m"));
   private final BooleanSetting customAnimals = this.add(new BooleanSetting("CustomAnimals", false).injectTask(this::openSelector));
   private final BooleanSetting checkSaddle = this.add(new BooleanSetting("CheckSaddle", false));
   private final SliderSetting rideDelay = this.add(new SliderSetting("RideDelay", 500.0, 100.0, 5000.0, 25.0).setSuffix("ms"));
   private final BooleanSetting autoSaddle = this.add(new BooleanSetting("AutoSaddle", true));
   private final BooleanSetting forceRide = this.add(new BooleanSetting("ForceRide", false));
   private final Set<String> selectedAnimals = new HashSet<>();
   private Entity target;
   private long lastInteract;

   public AutoRide() {
      super("AutoRide", "Sprints to and mounts selected rideable animals", Module.Category.Survival);
      this.setChinese("\u81ea\u52a8\u9a91\u4e58");
      this.range.setChinese("\u81ea\u5b9a\u4e49\u8303\u56f4");
      this.customAnimals.setChinese("\u81ea\u5b9a\u4e49\u52a8\u7269");
      this.checkSaddle.setChinese("\u68c0\u67e5\u978d");
      this.rideDelay.setChinese("\u9a91\u4e58\u5ef6\u8fdf");
      this.autoSaddle.setChinese("\u81ea\u52a8\u653e\u978d");
      this.forceRide.setChinese("\u5f3a\u5236\u9a91\u4e58");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.stopMoving();
   }

   private void openSelector() {
      this.customAnimals.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new RideAnimalSelectWindow(this)));
      }
   }

   public boolean isSelected(String var1) {
      return this.selectedAnimals.contains(var1);
   }

   public void toggle(String var1) {
      if (!this.selectedAnimals.add(var1)) {
         this.selectedAnimals.remove(var1);
      }
   }

   public static String idOf(Entity var0) {
      Identifier var1 = EntityType.getId(var0.getType());
      return var1 == null ? "" : var1.toString();
   }

   private boolean valid(Entity var1) {
      String var2 = idOf(var1);
      return var1.isAlive() && !var1.isSpectator() && RIDEABLE.contains(var2) && (!this.customAnimals.getValue() || this.isSelected(var2));
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck()) {
         if (!this.forceRide.getValue() || mc.player.getVehicle() == null) {
            if (this.target == null || !this.valid(this.target) || mc.player.squaredDistanceTo(this.target) > this.range.getValue() * this.range.getValue()) {
               this.target = this.findTarget();
            }

            if (this.target == null) {
               this.stopMoving();
            } else if (this.forceRide.getValue() && mc.player.getVehicle() == null) {
               if (this.canMount()) {
                  this.mount();
               }
            } else if (mc.player.squaredDistanceTo(this.target.getPos()) > 5.0) {
               this.moveToward(this.target.getPos());
            } else {
               this.stopMoving();
               if (System.currentTimeMillis() - this.lastInteract >= this.rideDelay.getValueInt()) {
                  if (this.autoSaddle.getValue() && !this.hasSaddle()) {
                     this.useSaddle();
                  }

                  if (this.canMount()) {
                     this.mount();
                  }
               }
            }
         }
      }
   }

   private Entity findTarget() {
      Entity var1 = null;
      double var2 = this.range.getValue() * this.range.getValue();

      for (Entity var5 : Alien.THREAD.getEntities()) {
         if (this.valid(var5)) {
            double var6 = mc.player.squaredDistanceTo(var5);
            if (var6 < var2) {
               var2 = var6;
               var1 = var5;
            }
         }
      }

      return var1;
   }

   private void useSaddle() {
      Item var1 = (Item)Registries.ITEM.get(Identifier.of("minecraft", "saddle"));
      int var2 = var1 == null ? -1 : InventoryUtil.findItem(var1);
      if (var2 != -1) {
         int var3 = mc.player.getInventory().selectedSlot;
         InventoryUtil.switchToSlot(var2);
         mc.interactionManager.interactEntity(mc.player, this.target, Hand.MAIN_HAND);
         InventoryUtil.switchToSlot(var3);
      }
   }

   private void mount() {
      mc.interactionManager.interactEntity(mc.player, this.target, Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.lastInteract = System.currentTimeMillis();
   }

   private boolean canMount() {
      return !this.checkSaddle.getValue() || this.hasSaddle();
   }

   private boolean hasSaddle() {
      return this.target instanceof AbstractHorseEntity var1 && var1.isTame();
   }

   private void moveToward(Vec3d var1) {
      mc.player.setSprinting(true);
      mc.options.sneakKey.setPressed(false);
      mc.player.input.sneaking = false;
      double var2 = var1.x - mc.player.getX();
      double var4 = var1.z - mc.player.getZ();
      double var6 = Math.sqrt(var2 * var2 + var4 * var4);
      if (var6 > 0.05) {
         Vec3d var8 = mc.player.getVelocity();
         mc.player.setVelocity(var2 / var6 * Math.min(0.21, var6), var8.y, var4 / var6 * Math.min(0.21, var6));
      }
   }

   private void stopMoving() {
      if (mc.player != null) {
         Vec3d var1 = mc.player.getVelocity();
         mc.player.setVelocity(0.0, var1.y, 0.0);
         mc.player.setSprinting(false);
         mc.options.sneakKey.setPressed(false);
         mc.player.input.sneaking = false;
      }
   }
}
