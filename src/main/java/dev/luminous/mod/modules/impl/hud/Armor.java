package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public class Armor extends Module {
   public static Armor INSTANCE;
   public final BooleanSetting lockOnHotbar = this.add(new BooleanSetting("LockOnHotbar", true));

   public Armor() {
      super("Armor", Module.Category.HUD);
      this.setChinese("盔甲");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      Alien.GUI.armorHud.draw(drawContext, tickDelta, null);
   }
}
