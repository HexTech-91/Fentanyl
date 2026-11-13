package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class AutoDropShulker extends Module {
   private final EnumSetting<AutoDropShulker.Mode> mode = this.add(new EnumSetting<>("\u6a21\u5f0f", AutoDropShulker.Mode.All));
   private final SliderSetting tasksPerTick = this.add(new SliderSetting("\u6bcfTick\u4efb\u52a1\u6570", 2.0, 1.0, 9.0, 1.0));
   private final SliderSetting delay = this.add(new SliderSetting("\u5ef6\u8fdf", 0.1, 0.0, 1.0, 0.01).setSuffix("s"));
   private final Timer timer = new Timer();

   public AutoDropShulker() {
      super("AutoDropShulker", Module.Category.Misc);
      this.setChinese("\u81ea\u52a8\u4e22\u76d2");
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.timer.passedS(this.delay.getValue())) {
            this.timer.reset();
            int tasks = (int)this.tasksPerTick.getValue();
            int executed = 0;

            for (int pass = 0; pass < tasks; pass++) {
               int slot = this.findBestSlotToDrop();
               if (slot == -1) {
                  break;
               }

               int syncId = mc.player.playerScreenHandler.syncId;
               int screenSlot = slot < 9 ? slot + 36 : slot;
               mc.interactionManager.clickSlot(syncId, screenSlot, 1, SlotActionType.THROW, mc.player);
               executed++;
               if (slot == mc.player.getInventory().selectedSlot) {
               }
            }
         }
      }
   }

   private int findBestSlotToDrop() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && this.isShulkerBox(stack) && this.shouldDrop(stack)) {
            return i;
         }
      }

      return -1;
   }

   private boolean isShulkerBox(ItemStack stack) {
      return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
   }

   private boolean shouldDrop(ItemStack stack) {
      return switch ((AutoDropShulker.Mode)this.mode.getValue()) {
         case All -> true;
         case EmptyOnly -> this.isCompletelyEmpty(stack);
         case NonEssential -> !this.containsRequiredItems(stack);
      };
   }

   private boolean containsRequiredItems(ItemStack shulkerBox) {
      ItemStack[] contents = this.getShulkerContents(shulkerBox);
      if (contents == null) {
         return false;
      } else {
         for (ItemStack stack : contents) {
            if (stack != null && !stack.isEmpty()) {
               if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                  return true;
               }

               if (stack.getItem() == Items.END_CRYSTAL) {
                  return true;
               }

               if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
                  PotionContentsComponent comp = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
                  if (comp != null) {
                     for (StatusEffectInstance effect : comp.getEffects()) {
                        Identifier id = Registries.STATUS_EFFECT.getId((StatusEffect)effect.getEffectType().value());
                        if (id != null && id.toString().equals("minecraft:turtle_master") && effect.getAmplifier() == 6) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean isCompletelyEmpty(ItemStack shulkerBox) {
      ItemStack[] contents = this.getShulkerContents(shulkerBox);
      if (contents == null) {
         return true;
      } else {
         for (ItemStack stack : contents) {
            if (stack != null && !stack.isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   private ItemStack[] getShulkerContents(ItemStack shulkerBox) {
      if (!this.isShulkerBox(shulkerBox)) {
         return null;
      } else {
         ContainerComponent container = (ContainerComponent)shulkerBox.get(DataComponentTypes.CONTAINER);
         if (container != null) {
            return container.stream().toArray(ItemStack[]::new);
         } else {
            NbtComponent nbt = (NbtComponent)shulkerBox.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (nbt != null && nbt.contains("Items")) {
               NbtList list = nbt.getNbt().getList("Items", 10);
               ItemStack[] items = new ItemStack[27];

               for (int i = 0; i < list.size(); i++) {
                  NbtCompound compound = list.getCompound(i);
                  int slot = compound.getByte("Slot");
                  if (slot >= 0 && slot < 27) {
                     items[slot] = ItemStack.fromNbtOrEmpty(mc.player.getRegistryManager(), compound);
                  }
               }

               return items;
            } else {
               return null;
            }
         }
      }
   }

   public static enum Mode {
      All,
      EmptyOnly,
      NonEssential;
   }
}
