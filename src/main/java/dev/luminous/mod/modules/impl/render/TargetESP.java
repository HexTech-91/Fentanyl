package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.render.CaptureMark;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class TargetESP extends Module {
   public static TargetESP INSTANCE;
   public final BooleanSetting nurikZapen = this.add(new BooleanSetting("NurikZapen", true));
   public final BooleanSetting players;
   public final BooleanSetting skipFriends;
   public final BooleanSetting skipDead;
   public final BooleanSetting skipLogged;
   public final BooleanSetting autoTarget;
   public final SliderSetting range;
   public final SliderSetting targetCount;
   public final BooleanSetting rainbow;
   public final BooleanSetting colorCycle;
   public final BooleanSetting pastel;
   public final SliderSetting cycleSpeed;
   public final ColorSetting color;
   public static Entity autoTargetEntity;
   public static List<Entity> autoTargetEntities = new ArrayList<>();

   public TargetESP() {
      super("TargetESP", "\u5728\u76ee\u6807\u73a9\u5bb6\u8eab\u4e0a\u6e32\u67d3\u6253\u6ed1\u65cb\u8f6c\u6355\u83b7\u6807\u8bb0", Module.Category.Render);
      this.nurikZapen.setChinese("\u6253\u6ed1\u6807\u8bb0");
      this.players = this.add(new BooleanSetting("Players", true));
      this.players.setChinese("\u73a9\u5bb6");
      this.skipFriends = this.add(new BooleanSetting("SkipFriends", true));
      this.skipFriends.setChinese("\u8df3\u8fc7\u597d\u53cb");
      this.skipDead = this.add(new BooleanSetting("SkipDead", true));
      this.skipDead.setChinese("\u8df3\u8fc7\u6b7b\u4ea1");
      this.skipLogged = this.add(new BooleanSetting("SkipLogged", true));
      this.skipLogged.setChinese("\u8df3\u8fc7\u79bb\u7ebf");
      this.autoTarget = this.add(new BooleanSetting("AutoTarget", true));
      this.autoTarget.setChinese("\u81ea\u52a8\u76ee\u6807");
      this.range = this.add(new SliderSetting("Range", 20.0, 0.0, 50.0, 0.1));
      this.range.setChinese("\u8303\u56f4");
      this.targetCount = this.add(new SliderSetting("TargetCount", 1.0, 1.0, 10.0, 1.0));
      this.targetCount.setChinese("\u76ee\u6807\u6570\u91cf");
      this.rainbow = this.add(new BooleanSetting("Rainbow", false));
      this.rainbow.setChinese("\u5f69\u8679");
      this.colorCycle = this.add(new BooleanSetting("ColorCycle", false));
      this.colorCycle.setChinese("\u8272\u5f69\u5faa\u73af");
      this.pastel = this.add(new BooleanSetting("Pastel", true, () -> this.colorCycle.getValue()));
      this.pastel.setChinese("\u6de1\u8272");
      this.cycleSpeed = this.add(new SliderSetting("CycleSpeed", 5.0, 1.0, 10.0, 0.5, () -> this.colorCycle.getValue()).setSuffix("s"));
      this.cycleSpeed.setChinese("\u5faa\u73af\u901f\u5ea6");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
      this.color.setChinese("\u989c\u8272");
      this.setChinese("\u76ee\u6807ESP");
      INSTANCE = this;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (event.stage == Event.Stage.Pre) {
         if (!nullCheck()) {
            CaptureMark.tick();
            if (this.autoTarget.getValue()) {
               autoTargetEntities.clear();
               List<Entity> validEntities = new ArrayList<>();

               for (Entity entity : Alien.THREAD.getEntities()) {
                  if (entity instanceof PlayerEntity
                     && entity != mc.player
                     && this.players.getValue()
                     && (!this.skipFriends.getValue() || !Alien.FRIEND.isFriend((PlayerEntity)entity))) {
                     float distance = entity.distanceTo(mc.player);
                     if (this.range.getValue() <= 0.0 || distance <= this.range.getValue()) {
                        validEntities.add(entity);
                     }
                  }
               }

               validEntities.sort((e1, e2) -> Float.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
               int count = Math.min(this.targetCount.getValueInt(), validEntities.size());

               for (int i = 0; i < count; i++) {
                  autoTargetEntities.add(validEntities.get(i));
               }

               autoTargetEntity = autoTargetEntities.isEmpty() ? null : autoTargetEntities.get(0);
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck() && this.nurikZapen.getValue()) {
         if (this.autoTarget.getValue() && !autoTargetEntities.isEmpty()) {
            for (Entity entity : autoTargetEntities) {
               if (this.players.getValue()
                  && entity instanceof PlayerEntity
                  && (!this.skipDead.getValue() || !entity.isWet())
                  && (!this.skipFriends.getValue() || !Alien.FRIEND.isFriend((PlayerEntity)entity))
                  && (this.range.getValue() <= 0.0 || entity.distanceTo(mc.player) <= this.range.getValue())) {
                  if (this.colorCycle.getValue()) {
                     CaptureMark.renderGradient(entity, this.color.getValue(), this.getCycleHue(this.cycleSpeed.getValueFloat()), this.pastel.getValue());
                  } else {
                     CaptureMark.render(entity, this.color.getValue(), this.rainbow.getValue());
                  }
               }
            }
         }
      }
   }

   private float getCycleHue(float speedSeconds) {
      return (float)(System.currentTimeMillis() % (long)(speedSeconds * 1000.0F)) / (speedSeconds * 1000.0F);
   }
}
