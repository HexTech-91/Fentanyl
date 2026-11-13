package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public class Turtle extends Module {
   public static Turtle INSTANCE;
   public final BooleanSetting lockOnHotbar = this.add(new BooleanSetting("LockOnHotbar", true));

   public Turtle() {
      super("Turtle", Module.Category.HUD);
      this.setChinese("神龟");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      Alien.GUI.turtleHud.draw(drawContext, tickDelta, null);
   }
}
