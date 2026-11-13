package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.entity.CopyPlayerEntity;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class PopChams extends Module {
   public static PopChams INSTANCE;
   public final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   private final ColorSetting fill;
   private final ColorSetting line;
   final CopyOnWriteArrayList<PopChams.Person> popList;
   private final BooleanSetting alpha;
   private final BooleanSetting forceSneak;
   private final BooleanSetting noSelf;
   private final BooleanSetting noLimb;
   private final SliderSetting fadeTime;
   private final SliderSetting yOffset;
   private final SliderSetting scale;
   private final SliderSetting yaw;

   public PopChams() {
      super("PopChams", Module.Category.Render);
      this.ease.setChinese("\u7f13\u52a8");
      this.fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.fill.setChinese("\u586b\u5145");
      this.line = this.add(new ColorSetting("Line", new Color(255, 255, 255, 100)).injectBoolean(true));
      this.line.setChinese("\u7ebf\u6761");
      this.popList = new CopyOnWriteArrayList<>();
      this.alpha = this.add(new BooleanSetting("Alpha", true));
      this.alpha.setChinese("\u900f\u660e\u5ea6");
      this.forceSneak = this.add(new BooleanSetting("ForceSneak", false));
      this.forceSneak.setChinese("\u5f3a\u5236\u6f5c\u884c");
      this.noSelf = this.add(new BooleanSetting("NoSelf", true));
      this.noSelf.setChinese("\u6392\u9664\u81ea\u8eab");
      this.noLimb = this.add(new BooleanSetting("NoLimb", true));
      this.noLimb.setChinese("\u65e0\u80a2\u4f53");
      this.fadeTime = this.add(new SliderSetting("FadeTime", 300, 0, 1000));
      this.fadeTime.setChinese("\u6de1\u51fa\u65f6\u95f4");
      this.yOffset = this.add(new SliderSetting("YOffset", 0.0, -10.0, 10.0, 0.01));
      this.yOffset.setChinese("Y\u504f\u79fb");
      this.scale = this.add(new SliderSetting("Scale", 1.0, 0.0, 2.0, 0.01));
      this.scale.setChinese("\u7f29\u653e");
      this.yaw = this.add(new SliderSetting("Yaw", 0.0, 0.0, 720.0, 0.01));
      this.yaw.setChinese("\u504f\u822a");
      this.setChinese("\u7206\u56fe\u817e\u4e0a\u8272");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      RenderSystem.depthMask(false);
      this.popList.removeIf(person -> person.render(matrixStack));
      RenderSystem.depthMask(true);
   }

   @EventListener
   private void onTotemPop(TotemEvent event) {
      if (!this.noSelf.getValue() || !event.getPlayer().equals(mc.player)) {
         this.popList.add(new PopChams.Person(new CopyPlayerEntity(event.getPlayer())));
      }
   }

   private class Person {
      public final ModelPlayer modelPlayer;
      final Animation animation;

      public Person(PlayerEntity player) {
         this.modelPlayer = new ModelPlayer(player);
         this.animation = new Animation();
      }

      public boolean render(MatrixStack matrixStack) {
         double animation = this.animation.get(1.0, PopChams.this.fadeTime.getValueInt(), PopChams.this.ease.getValue());
         if (animation >= 1.0) {
            return true;
         } else {
            this.modelPlayer
               .render(
                  matrixStack,
                  PopChams.this.fill,
                  PopChams.this.line,
                  PopChams.this.alpha.getValue() ? 1.0 - animation : 1.0,
                  PopChams.this.yOffset.getValue() * animation,
                  1.0 + (PopChams.this.scale.getValue() - 1.0) * animation,
                  PopChams.this.yaw.getValue() * animation,
                  PopChams.this.noLimb.getValue(),
                  PopChams.this.forceSneak.getValue()
               );
            return false;
         }
      }
   }
}
