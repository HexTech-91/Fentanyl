package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class InventoryViewer extends Module {
   public static InventoryViewer INSTANCE;

   public InventoryViewer() {
      super("InventoryViewer", Module.Category.HUD);
      this.setChinese("背包物品");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      Alien.GUI.inventoryViewer.draw(drawContext, tickDelta, null);
   }
}
