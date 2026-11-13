package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
   public static Chams INSTANCE;
   public final BooleanSetting crystal = this.add(new BooleanSetting("Crystals", true).setParent());
   public final BooleanSetting custom;
   public final BooleanSetting depth;
   public final BooleanSetting chamsTexture;
   public final ColorSetting fill;
   public final ColorSetting line;
   public final ColorSetting core;
   public final ColorSetting outerFrame;
   public final ColorSetting innerFrame;
   public final BooleanSetting glint;
   public final BooleanSetting texture;
   public final BooleanSetting spinSync;
   public final SliderSetting scale;
   public final SliderSetting spinValue;
   public final SliderSetting bounceHeight;
   public final SliderSetting floatValue;
   public final SliderSetting floatOffset;
   public final BooleanSetting throughWall;
   private final BooleanSetting Crystals;
   private final BooleanSetting Players;
   private final BooleanSetting Mobs;
   private final BooleanSetting Animals;
   private final BooleanSetting Villagers;
   private final BooleanSetting Slimes;
   public final ColorSetting hand;
   public int age;

   public Chams() {
      super("Chams", Module.Category.Render);
      this.crystal.setChinese("\u6c34\u6676");
      this.custom = this.add(new BooleanSetting("Custom", false, this.crystal::isOpen).setParent());
      this.custom.setChinese("\u81ea\u5b9a\u4e49");
      this.depth = this.add(new BooleanSetting("Depth", false, () -> this.crystal.isOpen() && this.custom.isOpen()));
      this.depth.setChinese("\u6df1\u5ea6");
      this.chamsTexture = this.add(new BooleanSetting("ChamsTexture", true, () -> this.crystal.isOpen() && this.custom.isOpen()));
      this.chamsTexture.setChinese("\u4e0a\u8272\u7eb9\u7406");
      this.fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.crystal.isOpen() && this.custom.isOpen()).injectBoolean(false));
      this.fill.setChinese("\u586b\u5145");
      this.line = this.add(new ColorSetting("Line", new Color(255, 255, 255, 100), () -> this.crystal.isOpen() && this.custom.isOpen()).injectBoolean(false));
      this.line.setChinese("\u7ebf\u6761");
      this.core = this.add(new ColorSetting("Core", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
      this.core.setChinese("\u6838\u5fc3");
      this.outerFrame = this.add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
      this.outerFrame.setChinese("\u5916\u6846");
      this.innerFrame = this.add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
      this.innerFrame.setChinese("\u5185\u6846");
      this.glint = this.add(new BooleanSetting("Glint", true, this.crystal::isOpen));
      this.glint.setChinese("\u5149\u6548");
      this.texture = this.add(new BooleanSetting("Texture", true, this.crystal::isOpen));
      this.texture.setChinese("\u7eb9\u7406");
      this.spinSync = this.add(new BooleanSetting("SpinSync", false, this.crystal::isOpen));
      this.spinSync.setChinese("\u65cb\u8f6c\u540c\u6b65");
      this.scale = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
      this.scale.setChinese("\u7f29\u653e");
      this.spinValue = this.add(new SliderSetting("SpinValue", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
      this.spinValue.setChinese("\u65cb\u8f6c\u901f\u5ea6");
      this.bounceHeight = this.add(new SliderSetting("BounceHeight", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
      this.bounceHeight.setChinese("\u5f39\u8df3\u9ad8\u5ea6");
      this.floatValue = this.add(new SliderSetting("FloatValue", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
      this.floatValue.setChinese("\u5f39\u8df3\u901f\u5ea6");
      this.floatOffset = this.add(new SliderSetting("FloatOffset", 0.0, -1.0, 1.0, 0.01, this.crystal::isOpen));
      this.floatOffset.setChinese("Y\u504f\u79fb");
      this.throughWall = this.add(new BooleanSetting("ThroughWall", false).setParent());
      this.throughWall.setChinese("\u7a7f\u5899\u900f\u89c6");
      this.Crystals = this.add(new BooleanSetting("Crystal", true, this.throughWall::isOpen));
      this.Crystals.setChinese("\u6c34\u6676");
      this.Players = this.add(new BooleanSetting("Players", true, this.throughWall::isOpen));
      this.Players.setChinese("\u73a9\u5bb6");
      this.Mobs = this.add(new BooleanSetting("Mobs", true, this.throughWall::isOpen));
      this.Mobs.setChinese("\u602a\u7269");
      this.Animals = this.add(new BooleanSetting("Animals", true, this.throughWall::isOpen));
      this.Animals.setChinese("\u52a8\u7269");
      this.Villagers = this.add(new BooleanSetting("Villagers", true, this.throughWall::isOpen));
      this.Villagers.setChinese("\u6751\u6c11");
      this.Slimes = this.add(new BooleanSetting("Slimes", true, this.throughWall::isOpen));
      this.Slimes.setChinese("\u53f2\u83b1\u59c6");
      this.hand = this.add(new ColorSetting("Hand", -1).injectBoolean(true));
      this.hand.setChinese("\u624b\u90e8");
      this.setChinese("\u6a21\u578b\u4e0a\u8272");
      INSTANCE = this;
   }

   public boolean customCrystal() {
      return this.isOn() && this.crystal.getValue();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.age++;
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onRender3D(Render3DEvent event) {
      if (this.hand.booleanValue) {
         Color color = this.hand.getValue();
         RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
      }
   }

   public boolean chams(Entity entity) {
      if (entity instanceof EndCrystalEntity) {
         return this.Crystals.getValue();
      } else if (entity instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (entity instanceof PlayerEntity) {
         return this.Players.getValue();
      } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
         return this.Villagers.getValue();
      } else if (entity instanceof AnimalEntity) {
         return this.Animals.getValue();
      } else {
         return entity instanceof MobEntity ? this.Mobs.getValue() : false;
      }
   }
}
