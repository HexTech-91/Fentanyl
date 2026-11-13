package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class Crosshair extends Module {
   public static Crosshair INSTANCE;
   public final SliderSetting length = this.add(new SliderSetting("Length", 1.4, 0.0, 20.0, 0.1));
   public final SliderSetting thickness;
   public final SliderSetting interval;
   private final ColorSetting color;

   public Crosshair() {
      super("Crosshair", Module.Category.Render);
      this.length.setChinese("\u957f\u5ea6");
      this.thickness = this.add(new SliderSetting("Thickness", 0.8, 0.0, 20.0, 0.1));
      this.thickness.setChinese("\u539a\u5ea6");
      this.interval = this.add(new SliderSetting("Interval", 1.0, 0.0, 20.0, 0.1));
      this.interval.setChinese("\u95f4\u9694");
      this.color = this.add(new ColorSetting("Color"));
      this.color.setChinese("\u989c\u8272");
      this.setChinese("\u51c6\u661f");
      INSTANCE = this;
   }

   public void draw(DrawContext context) {
      MatrixStack matrixStack = context.getMatrices();
      float centerX = mc.getWindow().getScaledWidth() / 2.0F;
      float centerY = mc.getWindow().getScaledHeight() / 2.0F;
      Render2DUtil.drawRect(
         matrixStack,
         centerX - this.thickness.getValueFloat() / 2.0F,
         centerY - this.length.getValueFloat() - this.interval.getValueFloat(),
         this.thickness.getValueFloat(),
         this.length.getValueFloat(),
         this.color.getValue()
      );
      Render2DUtil.drawRect(
         matrixStack,
         centerX - this.thickness.getValueFloat() / 2.0F,
         centerY + this.interval.getValueFloat(),
         this.thickness.getValueFloat(),
         this.length.getValueFloat(),
         this.color.getValue()
      );
      Render2DUtil.drawRect(
         matrixStack,
         centerX + this.interval.getValueFloat(),
         centerY - this.thickness.getValueFloat() / 2.0F,
         this.length.getValueFloat(),
         this.thickness.getValueFloat(),
         this.color.getValue()
      );
      Render2DUtil.drawRect(
         matrixStack,
         centerX - this.interval.getValueFloat() - this.length.getValueFloat(),
         centerY - this.thickness.getValueFloat() / 2.0F,
         this.length.getValueFloat(),
         this.thickness.getValueFloat(),
         this.color.getValue()
      );
   }
}
