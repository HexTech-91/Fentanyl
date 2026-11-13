package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public class Totem extends Module {
   public static Totem INSTANCE;
   public final BooleanSetting lockOnHotbar = this.add(new BooleanSetting("LockOnHotbar", true));

   public Totem() {
      super("Totem", Module.Category.HUD);
      this.setChinese("图腾");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      Alien.GUI.totemHud.draw(drawContext, tickDelta, null);
   }
}
