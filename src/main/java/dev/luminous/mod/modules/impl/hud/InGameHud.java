package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class InGameHud extends Module {
   public static InGameHud INSTANCE;

   public InGameHud() {
      super("InGameHud", Module.Category.HUD);
      this.setChinese("在游戏界面");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      Alien.GUI.inGameHud.draw(drawContext, tickDelta, Alien.GUI.getColor());
   }
}
