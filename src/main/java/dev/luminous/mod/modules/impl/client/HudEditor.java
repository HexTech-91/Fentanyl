package dev.luminous.mod.modules.impl.client;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.tclick.TClickGuiScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

public class HudEditor extends Module {
   public static HudEditor INSTANCE;

   public final EnumSetting<HudEditor.HudStyle> hudStyle = this.add(new EnumSetting<>("HudStyle", HudEditor.HudStyle.Blurry));
   public final ColorSetting blurColor = this.add(
      new ColorSetting("BlurColor", new Color(0xFF000E25, true), () -> this.hudStyle.is(HudEditor.HudStyle.Blurry))
   );
   public final BooleanSetting glow = this.add(new BooleanSetting("Light", true));
   public final SliderSetting hudRound = this.add(new SliderSetting("HudRound", 2.0, 1.0, 15.0, 0.1));
   public final SliderSetting alpha = this.add(
      new SliderSetting("Alpha", 0.9, 0.0, 1.0, 0.05, () -> this.hudStyle.is(HudEditor.HudStyle.Glowing))
   );
   public final SliderSetting blend = this.add(
      new SliderSetting("Blend", 10.0, 1.0, 15.0, 0.1, () -> this.hudStyle.is(HudEditor.HudStyle.Glowing))
   );
   public final SliderSetting outline = this.add(
      new SliderSetting("Outline", 0.5, 0.0, 2.5, 0.1, () -> this.hudStyle.is(HudEditor.HudStyle.Glowing))
   );
   public final SliderSetting glow1 = this.add(
      new SliderSetting("Glow", 0.5, 0.0, 1.0, 0.05, () -> this.hudStyle.is(HudEditor.HudStyle.Glowing))
   );
   public final SliderSetting blurOpacity = this.add(
      new SliderSetting("BlurOpacity", 0.55, 0.0, 1.0, 0.05, () -> this.hudStyle.is(HudEditor.HudStyle.Blurry))
   );
   public final SliderSetting blurStrength = this.add(
      new SliderSetting("BlurStrength", 20.0, 5.0, 50.0, 1.0, () -> this.hudStyle.is(HudEditor.HudStyle.Blurry))
   );

   public HudEditor() {
      super("HudEditor", "HudEditor", Module.Category.Client);
      this.setChinese("界面编辑器");
      INSTANCE = this;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof TClickGuiScreen)) {
         this.disable();
      }
   }

   public enum HudStyle {
      Blurry,
      Glowing;
   }
}
