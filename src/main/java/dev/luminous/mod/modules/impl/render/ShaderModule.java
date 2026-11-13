package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.asm.accessors.IGameRenderer;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ShaderModule extends Module {
   public static ShaderModule INSTANCE;
   private final EnumSetting<ShaderModule.Page> page = this.add(new EnumSetting<>("Page", ShaderModule.Page.Shader));
   public final EnumSetting<ShaderManager.Shader> mode;
   public final SliderSetting speed;
   public final ColorSetting fill;
   public final SliderSetting maxSample;
   public final SliderSetting divider;
   public final SliderSetting radius;
   public final SliderSetting smoothness;
   public final SliderSetting alpha;
   public final SliderSetting maxRange;
   public final SliderSetting pulseSpeed;
   public final ColorSetting pulse;
   public final SliderSetting step;
   public final SliderSetting octaves;
   public final ColorSetting smoke1;
   public final ColorSetting smoke2;
   public final ColorSetting smoke3;
   public final ColorSetting smoke4;
   private final BooleanSetting hands;
   private final BooleanSetting self;
   private final BooleanSetting players;
   private final BooleanSetting friends;
   private final BooleanSetting crystals;
   private final BooleanSetting creatures;
   private final BooleanSetting monsters;
   private final BooleanSetting ambients;
   private final BooleanSetting items;
   private final BooleanSetting others;

   public ShaderModule() {
      super("Shader", Module.Category.Render);
      this.page.setChinese("\u9875\u9762");
      this.mode = this.add(new EnumSetting<>("Mode", ShaderManager.Shader.Solid, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.mode.setChinese("\u6a21\u5f0f");
      this.speed = this.add(new SliderSetting("Speed", 4.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.speed.setChinese("\u901f\u5ea6");
      this.fill = this.add(new ColorSetting("Color", () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.fill.setChinese("\u989c\u8272");
      this.maxSample = this.add(new SliderSetting("MaxSample", 10.0, 0.0, 20.0, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.maxSample.setChinese("\u6700\u5927\u91c7\u6837");
      this.divider = this.add(new SliderSetting("Divider", 150.0, 0.0, 300.0, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.divider.setChinese("\u5206\u9694");
      this.radius = this.add(new SliderSetting("Radius", 2.0, 0.0, 6.0, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.radius.setChinese("\u534a\u5f84");
      this.smoothness = this.add(new SliderSetting("Smoothness", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.smoothness.setChinese("\u5e73\u6ed1\u5ea6");
      this.alpha = this.add(new SliderSetting("GlowAlpha", 255, 0, 255, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.alpha.setChinese("\u53d1\u5149\u900f\u660e\u5ea6");
      this.maxRange = this.add(new SliderSetting("MaxRange", 64, 16, 512, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.maxRange.setChinese("\u6700\u5927\u8303\u56f4");
      this.pulseSpeed = this.add(new SliderSetting("PulseSize", 10.0, 0.0, 200.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.pulseSpeed.setChinese("\u8109\u51b2\u5927\u5c0f");
      this.pulse = this.add(new ColorSetting("Pulse", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.pulse.setChinese("\u8109\u51b2");
      this.step = this.add(new SliderSetting("Step", 0.5, 0.0, 4.0, 0.01F, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.step.setChinese("\u6b65\u957f");
      this.octaves = this.add(new SliderSetting("Octaves", 10, 5, 30, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.octaves.setChinese("\u516b\u5ea6");
      this.smoke1 = this.add(new ColorSetting("Color1", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke1.setChinese("\u989c\u82721");
      this.smoke2 = this.add(new ColorSetting("Color2", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke2.setChinese("\u989c\u82722");
      this.smoke3 = this.add(new ColorSetting("Color3", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke3.setChinese("\u989c\u82723");
      this.smoke4 = this.add(new ColorSetting("Color4", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke4.setChinese("\u989c\u82724");
      this.hands = this.add(new BooleanSetting("Hands", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.hands.setChinese("\u624b");
      this.self = this.add(new BooleanSetting("Self", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.self.setChinese("\u81ea\u5df1");
      this.players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.players.setChinese("\u73a9\u5bb6");
      this.friends = this.add(new BooleanSetting("Friends", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.friends.setChinese("\u597d\u53cb");
      this.crystals = this.add(new BooleanSetting("Crystals", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.crystals.setChinese("\u672b\u5f71\u6c34\u6676");
      this.creatures = this.add(new BooleanSetting("Creatures", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.creatures.setChinese("\u751f\u7269");
      this.monsters = this.add(new BooleanSetting("Monsters", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.monsters.setChinese("\u602a\u7269");
      this.ambients = this.add(new BooleanSetting("Ambients", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.ambients.setChinese("\u73af\u5883");
      this.items = this.add(new BooleanSetting("Items", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.items.setChinese("\u7269\u54c1");
      this.others = this.add(new BooleanSetting("Others", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.others.setChinese("\u5176\u4ed6");
      this.setChinese("\u7740\u8272\u5668");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public boolean shouldRender(Entity entity) {
      if (entity == null) {
         return false;
      } else if (mc.player == null) {
         return false;
      } else if (MathHelper.sqrt((float)mc.player.squaredDistanceTo(entity.getPos())) > this.maxRange.getValue()) {
         return false;
      } else {
         Objects.requireNonNull(entity);
         if (entity instanceof PlayerEntity playerEntity) {
            if (entity == mc.player) {
               return this.self.getValue();
            } else {
               return Alien.FRIEND.isFriend(playerEntity) ? this.friends.getValue() : this.players.getValue();
            }
         } else if (entity instanceof EndCrystalEntity) {
            return this.crystals.getValue();
         } else if (entity instanceof ItemEntity) {
            return this.items.getValue();
         } else {
            return switch (entity.getType().getSpawnGroup()) {
               case CREATURE, WATER_CREATURE -> this.creatures.getValue();
               case MONSTER -> this.monsters.getValue();
               case AMBIENT, WATER_AMBIENT -> this.ambients.getValue();
               default -> this.others.getValue();
            };
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.hands.getValue()) {
         Alien.SHADER
            .renderShader(
               () -> ((IGameRenderer)mc.gameRenderer)
                  .IRenderHand(mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickDelta(true), matrixStack.peek().getPositionMatrix()),
               this.mode.getValue()
            );
      }
   }

   @Override
   public void onToggle() {
      Alien.SHADER.reloadShaders();
   }

   @Override
   public void onLogin() {
      Alien.SHADER.reloadShaders();
   }

   private static enum Page {
      Shader,
      Target,
      Color;
   }
}
