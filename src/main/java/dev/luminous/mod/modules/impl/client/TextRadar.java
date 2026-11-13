package dev.luminous.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final ColorSetting friend = this.add(new ColorSetting("Friend").injectBoolean(true));
   private final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
   public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
   private final BooleanSetting getDistance = this.add(new BooleanSetting("Distance", true));
   private final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));
   private final BooleanSetting direction = this.add(new BooleanSetting("Direction", true));
   private final BooleanSetting gradient = this.add(new BooleanSetting("Gradient", true));
   private final SliderSetting maxRange = this.add(new SliderSetting("MaxRange", 50.0, 10.0, 200.0, 1.0));
   private final SliderSetting iconSize = this.add(new SliderSetting("IconSize", 12.0, 8.0, 24.0, 1.0));
   private final BooleanSetting showSpeed;
   private final BooleanSetting showSlowness;
   private final BooleanSetting showStrength;
   private final BooleanSetting showWeakness;
   private final BooleanSetting showResistance;
   private final BooleanSetting showFireRes;
   private final BooleanSetting showAbsorption;
   private final BooleanSetting showRegeneration;
   private final BooleanSetting showInvisibility;
   private final BooleanSetting showJumpBoost;
   private final BooleanSetting showPoison;
   private final BooleanSetting showWither;

   public TextRadar() {
      super("TextRadar", Module.Category.Client);
      this.iconSize.setChinese("\u56fe\u6807\u5927\u5c0f");
      this.showSpeed = this.add(new BooleanSetting("ShowSpeed", true));
      this.showSlowness = this.add(new BooleanSetting("ShowSlowness", true));
      this.showStrength = this.add(new BooleanSetting("ShowStrength", true));
      this.showWeakness = this.add(new BooleanSetting("ShowWeakness", true));
      this.showResistance = this.add(new BooleanSetting("ShowResistance", true));
      this.showFireRes = this.add(new BooleanSetting("ShowFireRes", true));
      this.showAbsorption = this.add(new BooleanSetting("ShowAbsorption", true));
      this.showRegeneration = this.add(new BooleanSetting("ShowRegeneration", true));
      this.showInvisibility = this.add(new BooleanSetting("ShowInvisibility", true));
      this.showJumpBoost = this.add(new BooleanSetting("ShowJumpBoost", true));
      this.showPoison = this.add(new BooleanSetting("ShowPoison", true));
      this.showWither = this.add(new BooleanSetting("ShowWither", true));
      this.setChinese("\u6587\u5b57\u96f7\u8fbe");
      INSTANCE = this;
      this.font.setChinese("\u5b57\u4f53");
      this.shadow.setChinese("\u9634\u5f71");
      this.x.setChinese("X\u5750\u6807");
      this.y.setChinese("Y\u5750\u6807");
      this.doubleBlank.setChinese("\u53cc\u884c");
      this.health.setChinese("\u8840\u91cf");
      this.pops.setChinese("\u7206\u5934");
      this.red.setChinese("\u7ea2\u8272");
      this.getDistance.setChinese("\u8ddd\u79bb");
      this.effects.setChinese("\u836f\u6c34\u6548\u679c");
      this.direction.setChinese("\u65b9\u5411");
      this.gradient.setChinese("\u9ed1\u767d\u6e10\u53d8");
      this.maxRange.setChinese("\u6e10\u53d8\u8303\u56f4");
      this.showSpeed.setChinese("\u901f\u5ea6");
      this.showSlowness.setChinese("\u7f13\u6162");
      this.showStrength.setChinese("\u529b\u91cf");
      this.showWeakness.setChinese("\u865a\u5f31");
      this.showResistance.setChinese("\u6297\u6027");
      this.showFireRes.setChinese("\u9632\u706b");
      this.showAbsorption.setChinese("\u5438\u6536");
      this.showRegeneration.setChinese("\u751f\u547d\u6062\u590d");
      this.showInvisibility.setChinese("\u9690\u8eab");
      this.showJumpBoost.setChinese("\u8df3\u8dc3\u63d0\u5347");
      this.showPoison.setChinese("\u4e2d\u6bd2");
      this.showWither.setChinese("\u51cb\u96f6");
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int currentY = this.y.getValueInt();
      ArrayList<AbstractClientPlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
      players.sort(Comparator.comparingDouble(player -> mc.player.distanceTo(player)));

      for (PlayerEntity playerEntity : players) {
         if (playerEntity != mc.player) {
            StringBuilder stringBuilder = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            float dist = mc.player.distanceTo(playerEntity);
            boolean isFriend = Alien.FRIEND.isFriend(playerEntity);
            int displayColor;
            if (isFriend && this.friend.booleanValue) {
               displayColor = this.friend.getValue().getRGB();
            } else if (this.gradient.getValue()) {
               float factor = Math.max(0.0F, Math.min(1.0F, 1.0F - dist / this.maxRange.getValueFloat()));
               int gray = (int)(factor * 255.0F);
               displayColor = new Color(gray, gray, gray).getRGB();
            } else {
               displayColor = this.color.getValue().getRGB();
            }

            if (this.health.getValue()) {
               stringBuilder.append(getHealthColor(playerEntity));
               stringBuilder.append(this.df.format(playerEntity.getHealth() + playerEntity.getAbsorptionAmount()));
               stringBuilder.append(blank);
            }

            stringBuilder.append(Formatting.RESET);
            stringBuilder.append(playerEntity.getName().getString());
            if (this.direction.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(Formatting.GRAY);
               stringBuilder.append(this.getDirection(playerEntity));
            }

            if (this.getDistance.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(Formatting.WHITE);
               stringBuilder.append(this.df.format(dist));
               stringBuilder.append("m");
            }

            if (this.effects.getValue()) {
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.SLOWNESS, this.showSlowness.getValue(), Formatting.GRAY);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.SPEED, this.showSpeed.getValue(), Formatting.AQUA);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.STRENGTH, this.showStrength.getValue(), Formatting.DARK_RED);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.WEAKNESS, this.showWeakness.getValue(), Formatting.DARK_GRAY);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.RESISTANCE, this.showResistance.getValue(), Formatting.BLUE);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.FIRE_RESISTANCE, this.showFireRes.getValue(), Formatting.GOLD);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.ABSORPTION, this.showAbsorption.getValue(), Formatting.YELLOW);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.REGENERATION, this.showRegeneration.getValue(), Formatting.LIGHT_PURPLE);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.INVISIBILITY, this.showInvisibility.getValue(), Formatting.WHITE);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.JUMP_BOOST, this.showJumpBoost.getValue(), Formatting.GREEN);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.POISON, this.showPoison.getValue(), Formatting.DARK_GREEN);
               this.appendEffectText(playerEntity, stringBuilder, blank, StatusEffects.WITHER, this.showWither.getValue(), Formatting.DARK_PURPLE);
            }

            int totemPopped;
            if (this.pops.getValue() && (totemPopped = Alien.POP.getPop(playerEntity)) > 0) {
               stringBuilder.append(blank);
               stringBuilder.append(getPopColor(totemPopped));
               stringBuilder.append("-");
               stringBuilder.append(totemPopped);
            }

            if (!isFriend || this.friend.booleanValue) {
               String text = stringBuilder.toString();
               float textWidth;
               if (this.font.getValue()) {
                  FontManager.ui.drawString(drawContext.getMatrices(), text, this.x.getValueInt(), currentY, displayColor, this.shadow.getValue());
                  textWidth = FontManager.ui.getWidth(text);
               } else {
                  drawContext.drawText(mc.textRenderer, text, this.x.getValueInt(), currentY, displayColor, this.shadow.getValue());
                  textWidth = mc.textRenderer.getWidth(text);
               }

               if (this.effects.getValue()) {
                  float iconX = this.x.getValueInt() + textWidth + 2.0F;
                  int iconSz = this.iconSize.getValueInt();
                  StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();
                  if (spriteManager != null) {
                     iconX = this.renderEffectIcons(drawContext, playerEntity, spriteManager, iconX, currentY, iconSz);
                  }
               }

               float fontHeight = this.font.getValue() ? FontManager.ui.getFontHeight() : 9.0F;
               currentY += (int)fontHeight;
            }
         }
      }
   }

   private float renderEffectIcons(DrawContext ctx, PlayerEntity player, StatusEffectSpriteManager sm, float startX, int y, int size) {
      float cx = startX;

      for (StatusEffectInstance effect : player.getStatusEffects()) {
         RegistryEntry<StatusEffect> type = effect.getEffectType();
         if (this.shouldShowEffect(type)) {
            Sprite sprite = sm.getSprite(type);
            if (sprite != null) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               ctx.drawSprite((int)cx, y, 0, size, size, sprite);
               cx += size + 1;
            }
         }
      }

      return cx;
   }

   private boolean shouldShowEffect(RegistryEntry<StatusEffect> effect) {
      if (effect == StatusEffects.SPEED) {
         return this.showSpeed.getValue();
      } else if (effect == StatusEffects.SLOWNESS) {
         return this.showSlowness.getValue();
      } else if (effect == StatusEffects.STRENGTH) {
         return this.showStrength.getValue();
      } else if (effect == StatusEffects.WEAKNESS) {
         return this.showWeakness.getValue();
      } else if (effect == StatusEffects.RESISTANCE) {
         return this.showResistance.getValue();
      } else if (effect == StatusEffects.FIRE_RESISTANCE) {
         return this.showFireRes.getValue();
      } else if (effect == StatusEffects.ABSORPTION) {
         return this.showAbsorption.getValue();
      } else if (effect == StatusEffects.REGENERATION) {
         return this.showRegeneration.getValue();
      } else if (effect == StatusEffects.INVISIBILITY) {
         return this.showInvisibility.getValue();
      } else if (effect == StatusEffects.JUMP_BOOST) {
         return this.showJumpBoost.getValue();
      } else if (effect == StatusEffects.POISON) {
         return this.showPoison.getValue();
      } else {
         return effect == StatusEffects.WITHER ? this.showWither.getValue() : false;
      }
   }

   private void appendEffectText(PlayerEntity player, StringBuilder sb, String blank, RegistryEntry<StatusEffect> effect, boolean show, Formatting color) {
      if (show) {
         if (player.hasStatusEffect(effect)) {
            StatusEffectInstance instance = player.getStatusEffect(effect);
            sb.append(blank);
            sb.append(color);
            sb.append("Lv.");
            sb.append(instance.getAmplifier() + 1);
            sb.append(" ");
            sb.append(instance.getDuration() / 20 + 1);
            sb.append("s");
         }
      }
   }

   private String getDirection(PlayerEntity player) {
      double dx = player.getX() - mc.player.getX();
      double dz = player.getZ() - mc.player.getZ();
      double angle = Math.toDegrees(Math.atan2(dz, dx));
      if (angle < 0.0) {
         angle += 360.0;
      }

      if (angle >= 337.5 || angle < 22.5) {
         return "S";
      } else if (angle >= 22.5 && angle < 67.5) {
         return "SW";
      } else if (angle >= 67.5 && angle < 112.5) {
         return "W";
      } else if (angle >= 112.5 && angle < 157.5) {
         return "NW";
      } else if (angle >= 157.5 && angle < 202.5) {
         return "N";
      } else if (angle >= 202.5 && angle < 247.5) {
         return "NE";
      } else {
         return angle >= 247.5 && angle < 292.5 ? "E" : "SE";
      }
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = player.getHealth() + player.getAbsorptionAmount();
      if (health > 18.0) {
         return Formatting.GREEN;
      } else if (health > 16.0) {
         return Formatting.DARK_GREEN;
      } else if (health > 12.0) {
         return Formatting.YELLOW;
      } else if (health > 8.0) {
         return Formatting.GOLD;
      } else {
         return health > 4.0 ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static Formatting getPopColor(int totems) {
      if (INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (totems > 10) {
         return Formatting.DARK_RED;
      } else if (totems > 8) {
         return Formatting.RED;
      } else if (totems > 6) {
         return Formatting.GOLD;
      } else if (totems > 4) {
         return Formatting.YELLOW;
      } else {
         return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }
}
