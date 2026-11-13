package dev.luminous.mod.modules.impl.client;

import dev.luminous.api.utils.render.AnimatedGifTexture;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class GuiAnimation extends Module {
   private static final int DEFAULT_SIZE = 100;
   private static final int GIF_SIZE = 240;
   private static final int SCREEN_MARGIN = 8;
   private static final Identifier GIF_RESOURCE = Identifier.of("alienclient", "textures/gui/gui_animation/7412E55C114AE121F30DC4869B63D0ED.gif");
   private static final Identifier FALLBACK_TEXTURE = Identifier.of("alienclient", "textures/gui/gui_animation/7412E55C114AE121F30DC4869B63D0ED.png");
   private static final Identifier DYNAMIC_TEXTURE = Identifier.of("alienclient", "dynamic/gui_animation_7412E55C114AE121F30DC4869B63D0ED");
   public static GuiAnimation INSTANCE;
   private final BooleanSetting customPosition = this.add(new BooleanSetting("CustomPosition", false));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1920, () -> this.customPosition.getValue()));
   private final SliderSetting y = this.add(new SliderSetting("Y", 0, 0, 1080, () -> this.customPosition.getValue()));
   private final AnimatedGifTexture animation = new AnimatedGifTexture(GIF_RESOURCE, FALLBACK_TEXTURE, DYNAMIC_TEXTURE);

   public GuiAnimation() {
      super("GuiAnimation", Module.Category.Client);
      this.setChinese("GUI\u52a8\u56fe");
      this.customPosition.setChinese("\u81ea\u5b9a\u4e49\u4f4d\u7f6e");
      this.x.setChinese("X\u5750\u6807");
      this.y.setChinese("Y\u5750\u6807");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (var1 != null && !mc.options.hudHidden) {
         int var3 = var1.getScaledWindowWidth();
         int var4 = var1.getScaledWindowHeight();
         int var5 = Math.min(100, Math.min(var3, var4));
         int var6 = this.customPosition.getValue() ? this.x.getValueInt() : var3 - var5 - 8;
         int var7 = this.customPosition.getValue() ? this.y.getValueInt() : var4 - var5 - 8;
         var6 = Math.max(0, Math.min(var6, Math.max(0, var3 - var5)));
         var7 = Math.max(0, Math.min(var7, Math.max(0, var4 - var5)));
         var1.drawTexture(this.animation.getTexture(), var6, var7, var5, var5, 0.0F, 0.0F, 240, 240, 240, 240);
      }
   }
}
