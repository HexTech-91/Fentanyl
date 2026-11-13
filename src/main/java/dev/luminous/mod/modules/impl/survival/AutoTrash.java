package dev.luminous.mod.modules.impl.survival;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.TrashItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class AutoTrash extends Module {
   public static AutoTrash INSTANCE;
   private final BooleanSetting editItems = this.add(new BooleanSetting("CustomDropItems", false).injectTask(this::openSelector));
   private final SliderSetting delay = this.add(new SliderSetting("DropDelay", 250.0, 50.0, 5000.0, 25.0).setSuffix("ms"));
   private final Set<String> selectedItems = new HashSet<>();
   private final Timer timer = new Timer();

   public AutoTrash() {
      super("AutoTrash", "Drops only the selected inventory items", Module.Category.Survival);
      this.editItems.setChinese("\u81ea\u5b9a\u4e49\u4e22\u5f03\u7269\u54c1");
      this.delay.setChinese("\u4e22\u5f03\u5ef6\u8fdf");
      this.setChinese("\u81ea\u52a8\u4e22\u5783\u573e");
      INSTANCE = this;
   }

   private void openSelector() {
      this.editItems.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new TrashItemSelectWindow(this)));
      }
   }

   public boolean isSelected(Item var1) {
      return this.selectedItems.contains(idOf(var1));
   }

   public void toggle(Item var1) {
      String var2 = idOf(var1);
      if (!this.selectedItems.add(var2)) {
         this.selectedItems.remove(var2);
      }
   }

   private static String idOf(Item var0) {
      Identifier var1 = Registries.ITEM.getId(var0);
      return var1 == null ? "" : var1.toString();
   }

   @EventListener
   public void onTick(TickEvent var1) {
      if (!nullCheck() && this.timer.passedMs(this.delay.getValue())) {
         int var2 = this.findSelectedSlot();
         if (var2 != -1) {
            int var3 = var2 < 9 ? var2 + 36 : var2;
            int var4 = mc.player.playerScreenHandler.syncId;
            mc.interactionManager.clickSlot(var4, var3, 1, SlotActionType.THROW, mc.player);
            this.timer.reset();
         }
      }
   }

   private int findSelectedSlot() {
      for (int var1 = 0; var1 < 36; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (!var2.isEmpty() && this.isSelected(var2.getItem())) {
            return var1;
         }
      }

      return -1;
   }
}
