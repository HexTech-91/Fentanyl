package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ForceEnchantedApple extends Module {
   public static ForceEnchantedApple INSTANCE;
   private final Set<Module> pausedModules = new HashSet<>();

   public ForceEnchantedApple() {
      super("ForceEnchantedApple", Module.Category.Combat);
      this.setChinese("\u5f3a\u5236\u91d1\u82f9\u679c");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.pausedModules.clear();
   }

   @Override
   public void onDisable() {
      this.restorePausedModules();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck() && Alien.MODULE != null) {
         if (this.isUsingGoldenApple()) {
            this.pauseCombatModules();
         } else {
            this.restorePausedModules();
         }
      }
   }

   private void pauseCombatModules() {
      for (Module var2 : Alien.MODULE.getModules()) {
         if (var2 != this && var2.getCategory() == Module.Category.Combat && this.isBlockedModule(var2) && !this.isObsidianException(var2) && var2.isOn()) {
            this.pausedModules.add(var2);
            var2.disable();
         }
      }
   }

   private boolean isBlockedModule(Module var1) {
      String var2 = var1.getName().toLowerCase(Locale.ROOT);
      return var2.contains("crystal")
         || var2.contains("anchor")
         || var2.contains("piston")
         || var2.contains("web")
         || var2.contains("surround")
         || var2.contains("selftrap")
         || var2.contains("selfsurround");
   }

   private void restorePausedModules() {
      if (!this.pausedModules.isEmpty()) {
         Module[] var1 = this.pausedModules.toArray(new Module[0]);
         this.pausedModules.clear();

         for (Module var5 : var1) {
            if (!var5.isOn()) {
               var5.enable();
            }
         }
      }
   }

   private boolean isObsidianException(Module var1) {
      String var2 = var1.getName().toLowerCase(Locale.ROOT);
      return var2.contains("obsidian") || var2.contains("burrow") || var2.equals("packetmine") || var2.equals("packetmineplus");
   }

   private boolean isUsingGoldenApple() {
      if (!mc.player.isUsingItem()) {
         return false;
      } else {
         ItemStack var1 = mc.player.getMainHandStack();
         ItemStack var2 = mc.player.getOffHandStack();
         return this.isGoldenApple(var1) || this.isGoldenApple(var2);
      }
   }

   private boolean isGoldenApple(ItemStack var1) {
      return var1.getItem() == Items.GOLDEN_APPLE || var1.getItem() == Items.POTION;
   }
}
