package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4d;

public class TwoDESP extends Module {
   public static TwoDESP INSTANCE;
   private final EnumSetting<TwoDESP.分页> page = this.add(new EnumSetting<>("Page", TwoDESP.分页.目标));
   public final ColorSetting armorDurabilityColor;
   public final ColorSetting highHealthColor;
   public final ColorSetting midHealthColor;
   public final ColorSetting lowHealthColor;
   private final BooleanSetting outline;
   private final BooleanSetting renderHealth;
   private final BooleanSetting renderArmorDurability;
   private final BooleanSetting renderPotionEffects;
   private final SliderSetting durabilityScale;
   private final BooleanSetting renderItemName;
   private final BooleanSetting renderItemCount;
   public final ColorSetting itemCountColor;
   public final ColorSetting itemNameColor;
   private final BooleanSetting customFont;
   private final BooleanSetting players;
   private final BooleanSetting friends;
   private final BooleanSetting crystals;
   private final BooleanSetting animals;
   private final BooleanSetting monsters;
   private final BooleanSetting ambient;
   private final BooleanSetting other;
   private final ColorSetting playerBoxColor;
   private final ColorSetting friendBoxColor;
   private final ColorSetting crystalBoxColor;
   private final ColorSetting animalBoxColor;
   private final ColorSetting monsterBoxColor;
   private final ColorSetting ambientBoxColor;
   private final ColorSetting otherBoxColor;

   public TwoDESP() {
      super(
         "TwoDESP",
         "2D\u900f\u89c6\u663e\u793a\u5b9e\u4f53\u6846\u3001\u8840\u91cf\u3001\u88c5\u5907\u8010\u4e45\u548c\u836f\u6c34\u6548\u679c",
         Module.Category.Render
      );
      this.page.setChinese("\u9875\u9762");
      this.armorDurabilityColor = this.add(new ColorSetting("armorDurabilityColor", new Color(3145472), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.armorDurabilityColor.setChinese("\u88c5\u5907\u8010\u4e45\u989c\u8272");
      this.highHealthColor = this.add(new ColorSetting("highHealthColor", new Color(0, 255, 0, 255), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.highHealthColor.setChinese("\u9ad8\u8840\u91cf\u989c\u8272");
      this.midHealthColor = this.add(new ColorSetting("midHealthColor", new Color(255, 255, 0, 255), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.midHealthColor.setChinese("\u4e2d\u8840\u91cf\u989c\u8272");
      this.lowHealthColor = this.add(new ColorSetting("lowHealthColor", new Color(255, 0, 0, 255), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.lowHealthColor.setChinese("\u4f4e\u8840\u91cf\u989c\u8272");
      this.outline = this.add(new BooleanSetting("outline", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.outline.setChinese("\u63cf\u8fb9");
      this.renderHealth = this.add(new BooleanSetting("renderHealth", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.renderHealth.setChinese("\u6e32\u67d3\u8840\u91cf");
      this.renderArmorDurability = this.add(new BooleanSetting("renderArmorDurability", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.renderArmorDurability.setChinese("\u6e32\u67d3\u88c5\u5907\u8010\u4e45");
      this.renderPotionEffects = this.add(new BooleanSetting("renderPotionEffects", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.renderPotionEffects.setChinese("\u6e32\u67d3\u836f\u6c34\u6548\u679c");
      this.durabilityScale = this.add(new SliderSetting("durabilityScale", 1.0, 0.0, 2.0, 0.1, () -> this.renderArmorDurability.getValue()));
      this.durabilityScale.setChinese("\u8010\u4e45\u7f29\u653e");
      this.renderItemName = this.add(new BooleanSetting("renderItemName", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.renderItemName.setChinese("\u6e32\u67d3\u7269\u54c1\u540d");
      this.renderItemCount = this.add(
         new BooleanSetting("renderItemCount", true, () -> this.page.getValue() == TwoDESP.分页.设置 && this.renderItemName.getValue())
      );
      this.renderItemCount.setChinese("\u6e32\u67d3\u7269\u54c1\u6570\u91cf");
      this.itemCountColor = this.add(
         new ColorSetting("itemCountColor", new Color(255, 255, 0, 255), () -> this.page.getValue() == TwoDESP.分页.颜色 && this.renderItemCount.getValue())
      );
      this.itemCountColor.setChinese("\u7269\u54c1\u6570\u91cf\u989c\u8272");
      this.itemNameColor = this.add(
         new ColorSetting("itemNameColor", new Color(255, 255, 255, 255), () -> this.page.getValue() == TwoDESP.分页.颜色 && this.renderItemName.getValue())
      );
      this.itemNameColor.setChinese("\u7269\u54c1\u540d\u989c\u8272");
      this.customFont = this.add(new BooleanSetting("customFont", true, () -> this.page.getValue() == TwoDESP.分页.设置));
      this.customFont.setChinese("\u81ea\u5b9a\u4e49\u5b57\u4f53");
      this.players = this.add(new BooleanSetting("players", true, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.players.setChinese("\u73a9\u5bb6");
      this.friends = this.add(new BooleanSetting("friends", true, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.friends.setChinese("\u597d\u53cb");
      this.crystals = this.add(new BooleanSetting("crystals", true, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.crystals.setChinese("\u6c34\u6676");
      this.animals = this.add(new BooleanSetting("animals", false, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.animals.setChinese("\u751f\u7269");
      this.monsters = this.add(new BooleanSetting("monsters", false, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.monsters.setChinese("\u602a\u7269");
      this.ambient = this.add(new BooleanSetting("ambient", false, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.ambient.setChinese("\u73af\u5883\u751f\u7269");
      this.other = this.add(new BooleanSetting("other", false, () -> this.page.getValue() == TwoDESP.分页.目标));
      this.other.setChinese("\u5176\u4ed6");
      this.playerBoxColor = this.add(new ColorSetting("playerBoxColor", new Color(16749056), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.playerBoxColor.setChinese("\u73a9\u5bb6\u6846\u989c\u8272");
      this.friendBoxColor = this.add(new ColorSetting("friendBoxColor", new Color(3211008), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.friendBoxColor.setChinese("\u597d\u53cb\u6846\u989c\u8272");
      this.crystalBoxColor = this.add(new ColorSetting("crystalBoxColor", new Color(48127), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.crystalBoxColor.setChinese("\u6c34\u6676\u6846\u989c\u8272");
      this.animalBoxColor = this.add(new ColorSetting("animalBoxColor", new Color(10527910), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.animalBoxColor.setChinese("\u751f\u7269\u6846\u989c\u8272");
      this.monsterBoxColor = this.add(new ColorSetting("monsterBoxColor", new Color(16711680), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.monsterBoxColor.setChinese("\u602a\u7269\u6846\u989c\u8272");
      this.ambientBoxColor = this.add(new ColorSetting("ambientBoxColor", new Color(8061183), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.ambientBoxColor.setChinese("\u73af\u5883\u751f\u7269\u6846\u989c\u8272");
      this.otherBoxColor = this.add(new ColorSetting("otherBoxColor", new Color(16711778), () -> this.page.getValue() == TwoDESP.分页.颜色));
      this.otherBoxColor.setChinese("\u5176\u4ed6\u6846\u989c\u8272");
      this.setChinese("2D\u900f\u89c6");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (!nullCheck()) {
         Matrix4f var3 = var1.getMatrices().peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder var4 = null;
         boolean var5 = false;

         for (Entity var7 : Alien.THREAD.threadSafeEntityList) {
            if (this.应该渲染(var7)) {
               if (var4 == null) {
                  var4 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
               }

               if (this.绘制框(var4, var7, var3, var1)) {
                  var5 = true;
               }
            }
         }

         if (var5 && var4 != null) {
            BufferRenderer.drawWithGlobalProgram(var4.end());
         }

         RenderSystem.disableBlend();

         for (Entity var9 : Alien.THREAD.threadSafeEntityList) {
            if (this.应该渲染(var9)) {
               this.绘制文字(var9, var1);
            }
         }
      }
   }

   public boolean 应该渲染(Entity var1) {
      if (var1 == null) {
         return false;
      } else if (mc.player == null) {
         return false;
      } else if (var1 instanceof PlayerEntity) {
         if (var1 == mc.player && mc.options.getPerspective().isFirstPerson()) {
            return false;
         } else {
            return Alien.FRIEND.isFriend((PlayerEntity)var1) ? this.friends.getValue() : this.players.getValue();
         }
      } else if (var1 instanceof EndCrystalEntity) {
         return this.crystals.getValue();
      } else {
         return switch (var1.getType().getSpawnGroup()) {
            case CREATURE, WATER_CREATURE -> this.animals.getValue();
            case MONSTER -> this.monsters.getValue();
            case AMBIENT, WATER_AMBIENT -> this.ambient.getValue();
            default -> this.other.getValue();
         };
      }
   }

   public Color 获取实体颜色(Entity var1) {
      if (var1 == null) {
         return new Color(-1);
      } else if (var1 instanceof PlayerEntity) {
         return Alien.FRIEND.isFriend((PlayerEntity)var1) ? this.friendBoxColor.getValue() : this.playerBoxColor.getValue();
      } else if (var1 instanceof EndCrystalEntity) {
         return this.crystalBoxColor.getValue();
      } else {
         return switch (var1.getType().getSpawnGroup()) {
            case CREATURE, WATER_CREATURE -> this.animalBoxColor.getValue();
            case MONSTER -> this.monsterBoxColor.getValue();
            case AMBIENT, WATER_AMBIENT -> this.ambientBoxColor.getValue();
            default -> this.otherBoxColor.getValue();
         };
      }
   }

   public boolean 绘制框(BufferBuilder var1, Entity var2, Matrix4f var3, DrawContext var4) {
      double var6 = MathUtil.interpolate(var2.lastRenderX, var2.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double var8 = MathUtil.interpolate(var2.lastRenderY, var2.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double var10 = MathUtil.interpolate(var2.lastRenderZ, var2.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
      Box var12 = var2.getBoundingBox();
      Box var13 = new Box(
         var12.minX - var2.getX() + var6 - 0.05,
         var12.minY - var2.getY() + var8,
         var12.minZ - var2.getZ() + var10 - 0.05,
         var12.maxX - var2.getX() + var6 + 0.05,
         var12.maxY - var2.getY() + var8 + 0.15,
         var12.maxZ - var2.getZ() + var10 + 0.05
      );
      Vec3d[] var14 = new Vec3d[]{
         new Vec3d(var13.minX, var13.minY, var13.minZ),
         new Vec3d(var13.minX, var13.maxY, var13.minZ),
         new Vec3d(var13.maxX, var13.minY, var13.minZ),
         new Vec3d(var13.maxX, var13.maxY, var13.minZ),
         new Vec3d(var13.minX, var13.minY, var13.maxZ),
         new Vec3d(var13.minX, var13.maxY, var13.maxZ),
         new Vec3d(var13.maxX, var13.minY, var13.maxZ),
         new Vec3d(var13.maxX, var13.maxY, var13.maxZ)
      };
      Color var15 = this.获取实体颜色(var2);
      Vector4d var16 = null;

      for (Vec3d var20 : var14) {
         var20 = TextUtil.worldSpaceToScreenSpace(var20);
         if (var20.z > 0.0 && var20.z < 1.0) {
            if (var16 == null) {
               var16 = new Vector4d(var20.x, var20.y, var20.z, 0.0);
            }

            var16.x = Math.min(var20.x, var16.x);
            var16.y = Math.min(var20.y, var16.y);
            var16.z = Math.max(var20.x, var16.z);
            var16.w = Math.max(var20.y, var16.w);
         }
      }

      if (var16 == null) {
         return false;
      } else {
         double var36 = var16.x;
         double var37 = var16.y;
         double var21 = var16.z;
         double var23 = var16.w;
         if (this.outline.getValue()) {
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 1.0), (float)var37, 1.5F, (float)(var23 - var37 + 0.5), Color.BLACK);
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 1.0), (float)(var37 - 0.5), (float)(var21 - var36 + 1.5), 1.5F, Color.BLACK);
            this.绘制矩形到缓冲区(var1, var3, (float)(var21 - 1.0), (float)var37, 1.5F, (float)(var23 - var37 + 0.5), Color.BLACK);
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 1.0), (float)(var23 - 1.0), (float)(var21 - var36 + 1.5), 1.5F, Color.BLACK);
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 0.5), (float)var37, 0.5F, (float)(var23 - var37), var15);
            this.绘制矩形到缓冲区(var1, var3, (float)var36, (float)(var23 - 0.5), (float)(var21 - var36), 0.5F, var15);
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 0.5), (float)var37, (float)(var21 - var36 + 0.5), 0.5F, var15);
            this.绘制矩形到缓冲区(var1, var3, (float)(var21 - 0.5), (float)var37, 0.5F, (float)(var23 - var37), var15);
         }

         LivingEntity var5;
         if (var2 instanceof LivingEntity && (var5 = (LivingEntity)var2).getHealth() != 0.0F && this.renderHealth.getValue()) {
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 4.0), (float)var37, 1.0F, (float)(var23 - var37), Color.BLACK);
            Color var25 = this.获取血量颜色(var5.getHealth());
            float var26 = (float)(var23 - (var23 + (var37 - var23) * var5.getHealth() / var5.getMaxHealth()));
            this.绘制矩形到缓冲区(var1, var3, (float)(var36 - 4.0), (float)(var23 - var26), 1.0F, var26, var25);
         }

         if (var2 instanceof PlayerEntity var39 && this.renderArmorDurability.getValue()) {
            double var40 = (var23 - var37) / 4.0;
            ArrayList<ItemStack> var28 = new ArrayList<>();
            var28.add((ItemStack)var39.getInventory().armor.get(3));
            var28.add((ItemStack)var39.getInventory().armor.get(2));
            var28.add((ItemStack)var39.getInventory().armor.get(1));
            var28.add((ItemStack)var39.getInventory().armor.get(0));
            int var29 = -1;

            for (ItemStack var31 : var28) {
               var29++;
               if (!var31.isEmpty()) {
                  float var32 = var31.getMaxDamage() - var31.getDamage();
                  int var33 = (int)(var32 / var31.getMaxDamage() * 100.0F);
                  double var34 = var40 * (var33 / 100) * this.durabilityScale.getValueFloat();
                  var34 = Math.min(var40, var34);
                  this.绘制矩形到缓冲区(
                     var1,
                     var3,
                     (float)(var21 + 1.5),
                     (float)((float)var37 + var40 * var29 + 1.2 * (var29 + 1)),
                     1.5F,
                     (float)var34,
                     this.armorDurabilityColor.getValue()
                  );
               }
            }
         }

         return true;
      }
   }

   private void 绘制矩形到缓冲区(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5, float var6, Color var7) {
      float var8 = var7.getAlpha() / 255.0F;
      float var9 = var7.getRed() / 255.0F;
      float var10 = var7.getGreen() / 255.0F;
      float var11 = var7.getBlue() / 255.0F;
      var1.vertex(var2, var3, var4, 0.0F).color(var9, var10, var11, var8);
      var1.vertex(var2, var3, var4 + var6, 0.0F).color(var9, var10, var11, var8);
      var1.vertex(var2, var3 + var5, var4 + var6, 0.0F).color(var9, var10, var11, var8);
      var1.vertex(var2, var3 + var5, var4, 0.0F).color(var9, var10, var11, var8);
   }

   public void 绘制文字(Entity var1, DrawContext var2) {
      double var3 = MathUtil.interpolate(var1.lastRenderX, var1.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double var5 = MathUtil.interpolate(var1.lastRenderY, var1.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
      double var7 = MathUtil.interpolate(var1.lastRenderZ, var1.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
      Box var9 = var1.getBoundingBox();
      Box var10 = new Box(
         var9.minX - var1.getX() + var3 - 0.05,
         var9.minY - var1.getY() + var5,
         var9.minZ - var1.getZ() + var7 - 0.05,
         var9.maxX - var1.getX() + var3 + 0.05,
         var9.maxY - var1.getY() + var5 + 0.15,
         var9.maxZ - var1.getZ() + var7 + 0.05
      );
      Vec3d[] var11 = new Vec3d[]{
         new Vec3d(var10.minX, var10.minY, var10.minZ),
         new Vec3d(var10.minX, var10.maxY, var10.minZ),
         new Vec3d(var10.maxX, var10.minY, var10.minZ),
         new Vec3d(var10.maxX, var10.maxY, var10.minZ),
         new Vec3d(var10.minX, var10.minY, var10.maxZ),
         new Vec3d(var10.minX, var10.maxY, var10.maxZ),
         new Vec3d(var10.maxX, var10.minY, var10.maxZ),
         new Vec3d(var10.maxX, var10.maxY, var10.maxZ)
      };
      Vector4d var12 = null;

      for (Vec3d var16 : var11) {
         var16 = TextUtil.worldSpaceToScreenSpace(var16);
         if (var16.z > 0.0 && var16.z < 1.0) {
            if (var12 == null) {
               var12 = new Vector4d(var16.x, var16.y, var16.z, 0.0);
            }

            var12.x = Math.min(var16.x, var12.x);
            var12.y = Math.min(var16.y, var12.y);
            var12.z = Math.max(var16.x, var12.z);
            var12.w = Math.max(var16.y, var12.w);
         }
      }

      if (var12 != null) {
         double var33 = var12.x;
         double var34 = var12.y;
         double var17 = var12.z;
         double var19 = var12.w;
         if (var1 instanceof ItemEntity var21 && this.renderItemName.getValue()) {
            float var22 = (float)((var17 - var33) / 2.0);
            float var23 = FontManager.ui.getWidth(var21.getDisplayName().getString());
            float var24 = (float)(var33 + var22 - var23 / 2.0F);
            int var25 = var21.getStack().getCount();
            var2.drawText(mc.textRenderer, var21.getDisplayName().getString(), (int)var24, (int)(var34 - 10.0), this.itemNameColor.getValue().getRGB(), false);
            if (this.renderItemCount.getValue()) {
               var2.drawText(
                  mc.textRenderer,
                  " x" + var25,
                  (int)(var24 + mc.textRenderer.getWidth(var21.getDisplayName().getString() + " ")),
                  (int)var34 - 10,
                  this.itemCountColor.getValue().getRGB(),
                  false
               );
            }
         }

         if (var1 instanceof PlayerEntity var36) {
            if (this.renderArmorDurability.getValue()) {
               double var37 = (var19 - var34) / 4.0;
               ArrayList<ItemStack> var40 = new ArrayList<>();
               var40.add((ItemStack)var36.getInventory().armor.get(3));
               var40.add((ItemStack)var36.getInventory().armor.get(2));
               var40.add((ItemStack)var36.getInventory().armor.get(1));
               var40.add((ItemStack)var36.getInventory().armor.get(0));
               int var42 = -1;

               for (ItemStack var27 : var40) {
                  var42++;
                  if (!var27.isEmpty()) {
                     float var28 = var27.getMaxDamage() - var27.getDamage();
                     int var29 = (int)(var28 / var27.getMaxDamage() * 100.0F);
                     double var30 = var37 * (var29 / 100.0) * this.durabilityScale.getValueFloat();
                     var30 = Math.min(var37, var30);
                     var2.drawItem(var27, (int)(var17 + 4.0), (int)(var34 + var37 * var42 + 1.2 * (var42 + 1) + var30 / 2.0));
                  }
               }
            }

            if (this.renderPotionEffects.getValue()) {
               float var38 = (float)var34 - 20.0F;

               for (StatusEffectInstance var41 : var36.getStatusEffects()) {
                  StatusEffect var44 = (StatusEffect)var41.getEffectType().value();
                  String var45 = var44.getName().getString();
                  int var46 = var41.getDuration();
                  String var43;
                  if (var46 >= 72000) {
                     int var47 = var46 / 72000;
                     int var52 = var46 % 72000 / 1200;
                     var43 = String.format("%dh%dm", var47, var52);
                  } else if (var46 >= 1200) {
                     int var48 = var46 / 1200;
                     int var53 = var46 % 1200 / 20;
                     var43 = String.format("%dm%ds", var48, var53);
                  } else {
                     int var49 = var46 / 20;
                     var43 = String.format("%ds", var49);
                  }

                  String var50 = var45 + " " + var43;
                  int var54 = var44.getColor();
                  float var31 = this.customFont.getValue() ? FontManager.ui.getWidth(var50) : TextUtil.getWidth(var50);
                  float var32 = (float)(var33 + (var17 - var33) / 2.0 - var31 / 2.0);
                  TextUtil.drawString(var2, var50, var32, var38, var54, this.customFont.getValue(), false);
                  var38 -= 12.0F;
               }
            }
         }
      }
   }

   public Color 获取血量颜色(float var1) {
      if (var1 >= 20.0F) {
         return this.highHealthColor.getValue();
      } else {
         return 20.0F > var1 && var1 > 10.0F ? this.midHealthColor.getValue() : this.lowHealthColor.getValue();
      }
   }

   public static enum 分页 {
      设置,
      目标,
      颜色;
   }
}
