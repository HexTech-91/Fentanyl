package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.TotemParticleEvent;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.text.Text;

public class TotemParticle extends Module {
   public static TotemParticle INSTANCE;
   public final SliderSetting velocityXZ = this.add(new SliderSetting("VelocityXZ", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
   public final SliderSetting velocityY;
   final Random random;
   public final EnumSetting<TotemParticle.TextMode> textMode = this.add(new EnumSetting<>("TextMode", TotemParticle.TextMode.Off));
   private final SliderSetting textScale;
   private final SliderSetting textLife;
   private final BooleanSetting rainbow;
   private final ColorSetting color;
   private final ColorSetting color2;
   private final List<TotemParticle.PopText> popTexts = new ArrayList<>();

   public TotemParticle() {
      super("TotemParticle", Module.Category.Render);
      this.velocityXZ.setChinese("\u6c34\u5e73\u901f\u5ea6");
      this.velocityY = this.add(new SliderSetting("VelocityY", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
      this.velocityY.setChinese("\u5782\u76f4\u901f\u5ea6");
      this.random = new Random();
      this.textMode.setChinese("\u6587\u5b57\u6a21\u5f0f");
      this.textScale = this.add(new SliderSetting("TextScale", 2.0, 0.5, 6.0, 0.1, () -> this.textMode.getValue() != TotemParticle.TextMode.Off));
      this.textScale.setChinese("\u6587\u5b57\u5927\u5c0f");
      this.textLife = this.add(new SliderSetting("TextLife", 1.2, 0.3, 4.0, 0.1, () -> this.textMode.getValue() != TotemParticle.TextMode.Off).setSuffix("s"));
      this.textLife.setChinese("\u6587\u5b57\u65f6\u957f");
      this.rainbow = this.add(new BooleanSetting("Rainbow", true, () -> this.textMode.getValue() != TotemParticle.TextMode.Off));
      this.rainbow.setChinese("\u5f69\u8679");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
      this.color.setChinese("\u989c\u8272");
      this.color2 = this.add(new ColorSetting("Color2", new Color(0, 0, 0, 255)));
      this.color2.setChinese("\u989c\u82722");
      this.setChinese("\u81ea\u5b9a\u4e49\u56fe\u817e\u7c92\u5b50");
      INSTANCE = this;
   }

   @EventListener
   public void idk(TotemParticleEvent var1) {
      var1.cancel();
      var1.velocityZ = var1.velocityZ * (this.velocityXZ.getValue() / 100.0);
      var1.velocityX = var1.velocityX * (this.velocityXZ.getValue() / 100.0);
      var1.velocityY = var1.velocityY * (this.velocityY.getValue() / 100.0);
      var1.color = ColorUtil.fadeColor(this.color.getValue(), this.color2.getValue(), this.random.nextDouble());
   }

   public void recordPop(double var1, double var3, double var5, double var7, double var9, double var11) {
      if (this.popTexts.size() > 80) {
         this.popTexts.remove(0);
      }

      this.popTexts.add(new TotemParticle.PopText(var1, var3, var5, var7, var9, var11, (float)(this.random.nextDouble() * 360.0)));
   }

   @EventListener
   public void onRender3D(Render3DEvent var1) {
      if (this.textMode.getValue() != TotemParticle.TextMode.Off && !this.popTexts.isEmpty()) {
         String var2 = this.textMode.getValue() == TotemParticle.TextMode.Ez ? "ez" : "pop";
         long var3 = System.currentTimeMillis();
         long var5 = (long)(this.textLife.getValueFloat() * 1000.0F);
         boolean var7 = this.rainbow.getValue();
         Iterator var8 = this.popTexts.iterator();

         while (var8.hasNext()) {
            TotemParticle.PopText var9 = (TotemParticle.PopText)var8.next();
            long var10 = var3 - var9.born;
            if (var10 > var5) {
               var8.remove();
            } else {
               float var12 = 0.05F;
               var9.vy -= 0.04 * var12 * 20.0;
               var9.vx *= 0.98;
               var9.vy *= 0.98;
               var9.vz *= 0.98;
               var9.x = var9.x + var9.vx * var12;
               var9.y = var9.y + var9.vy * var12;
               var9.z = var9.z + var9.vz * var12;
               float var13 = (float)var10 / (float)var5;
               int var14 = (int)(255.0F * (1.0F - var13 * var13));
               var14 = Math.max(0, Math.min(255, var14));
               Color var15;
               if (var7) {
                  float var16 = (float)((System.currentTimeMillis() * 0.15 + var9.hueOffset) % 360.0);
                  var15 = ColorUtil.hslToColor(var16, 85.0F, 60.0F, var14 / 255.0F);
               } else {
                  Color var18 = ColorUtil.fadeColor(this.color.getValue(), this.color2.getValue(), this.random.nextDouble());
                  var15 = new Color(var18.getRed(), var18.getGreen(), var18.getBlue(), var14);
               }

               Render3DUtil.drawText3D(Text.literal(var2), var9.x, var9.y, var9.z, 0.0, 0.0, this.textScale.getValueFloat(), var15.getRGB());
            }
         }
      }
   }

   private static class PopText {
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      long born;
      float hueOffset;

      PopText(double var1, double var3, double var5, double var7, double var9, double var11, float var13) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         this.vx = var7;
         this.vy = var9;
         this.vz = var11;
         this.born = System.currentTimeMillis();
         this.hueOffset = var13;
      }
   }

   public static enum TextMode {
      Off,
      Ez,
      Pop;
   }
}
