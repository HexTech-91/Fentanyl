package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ESP extends Module {
   public static ESP INSTANCE;
   public final EnumSetting<ESP.RenderMode> mode = this.add(new EnumSetting<>("Mode", ESP.RenderMode.ThreeD));
   private final BooleanSetting twoDOutline;
   private final BooleanSetting twoDHealth;
   private final ColorSetting hHealth;
   private final ColorSetting mHealth;
   private final ColorSetting lHealth;
   private final BooleanSetting twoDArmorDura;
   private final ColorSetting armorDuraColor;
   private final SliderSetting duraScale;
   public final BooleanSetting target;
   public final BooleanSetting self;
   private final ColorSetting endPortalFill;
   private final ColorSetting endPortalOutline;
   private final ColorSetting itemFill;
   private final ColorSetting itemOutline;
   private final ColorSetting playerFill;
   private final ColorSetting playerOutline;
   private final ColorSetting chestFill;
   private final ColorSetting chestOutline;
   private final ColorSetting enderChestFill;
   private final ColorSetting enderChestOutline;
   private final ColorSetting shulkerBoxFill;
   private final ColorSetting shulkerBoxOutline;
   public final BooleanSetting item;
   public final BooleanSetting customName;
   public final BooleanSetting count;
   private final ColorSetting text;
   public final BooleanSetting pearl;

   public ESP() {
      super("ESP", Module.Category.Survival);
      this.mode.setChinese("\u6a21\u5f0f");
      this.twoDOutline = this.add(new BooleanSetting("TwoDOutline", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.twoDOutline.setChinese("2D\u8f6e\u5ed3");
      this.twoDHealth = this.add(new BooleanSetting("TwoDHealth", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.twoDHealth.setChinese("2D\u8840\u91cf");
      this.hHealth = this.add(new ColorSetting("HHealth", new Color(0, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.hHealth.setChinese("\u9ad8\u8840\u91cf");
      this.mHealth = this.add(new ColorSetting("MHealth", new Color(255, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.mHealth.setChinese("\u4e2d\u8840\u91cf");
      this.lHealth = this.add(new ColorSetting("LHealth", new Color(255, 0, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.lHealth.setChinese("\u4f4e\u8840\u91cf");
      this.twoDArmorDura = this.add(new BooleanSetting("TwoDArmorDura", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
      this.twoDArmorDura.setChinese("2D\u62a4\u7532\u8010\u4e45");
      this.armorDuraColor = this.add(
         new ColorSetting("ArmorDuraColor", new Color(3145472), () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue())
      );
      this.armorDuraColor.setChinese("\u62a4\u7532\u8010\u4e45\u989c\u8272");
      this.duraScale = this.add(
         new SliderSetting("DuraScale", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue())
      );
      this.duraScale.setChinese("\u8010\u4e45\u7f29\u653e");
      this.target = this.add(new BooleanSetting("Target", true).setParent());
      this.target.setChinese("\u76ee\u6807");
      this.self = this.add(new BooleanSetting("Self", false, this.target::isOpen));
      this.self.setChinese("\u81ea\u8eab");
      this.endPortalFill = this.add(
         new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.endPortalFill.setChinese("\u672b\u5730\u4f20\u9001\u95e8\u586b\u5145");
      this.endPortalOutline = this.add(
         new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.endPortalOutline.setChinese("\u672b\u5730\u4f20\u9001\u95e8\u8f6e\u5ed3");
      this.itemFill = this.add(new ColorSetting("ItemFill", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
      this.itemFill.setChinese("\u7269\u54c1\u586b\u5145");
      this.itemOutline = this.add(new ColorSetting("ItemOutline", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
      this.itemOutline.setChinese("\u7269\u54c1\u8f6e\u5ed3");
      this.playerFill = this.add(new ColorSetting("PlayerFill", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
      this.playerFill.setChinese("\u73a9\u5bb6\u586b\u5145");
      this.playerOutline = this.add(new ColorSetting("PlayerOutline", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
      this.playerOutline.setChinese("\u73a9\u5bb6\u8f6e\u5ed3");
      this.chestFill = this.add(
         new ColorSetting("ChestFill", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.chestFill.setChinese("\u7bb1\u5b50\u586b\u5145");
      this.chestOutline = this.add(
         new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.chestOutline.setChinese("\u7bb1\u5b50\u8f6e\u5ed3");
      this.enderChestFill = this.add(
         new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.enderChestFill.setChinese("\u672b\u5f71\u7bb1\u586b\u5145");
      this.enderChestOutline = this.add(
         new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.enderChestOutline.setChinese("\u672b\u5f71\u7bb1\u8f6e\u5ed3");
      this.shulkerBoxFill = this.add(
         new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.shulkerBoxFill.setChinese("\u6f5c\u5f71\u76d2\u586b\u5145");
      this.shulkerBoxOutline = this.add(
         new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
            .injectBoolean(false)
      );
      this.shulkerBoxOutline.setChinese("\u6f5c\u5f71\u76d2\u8f6e\u5ed3");
      this.item = this.add(new BooleanSetting("Item", false).setParent());
      this.item.setChinese("\u7269\u54c1\u540d\u79f0");
      this.customName = this.add(new BooleanSetting("CustomName", false, this.item::isOpen));
      this.customName.setChinese("\u81ea\u5b9a\u4e49\u540d\u79f0");
      this.count = this.add(new BooleanSetting("Count", true, this.item::isOpen));
      this.count.setChinese("\u6570\u91cf");
      this.text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255), this.item::isOpen));
      this.text.setChinese("\u6587\u672c");
      this.pearl = this.add(new BooleanSetting("Pearl", true));
      this.pearl.setChinese("\u672b\u5f71\u73cd\u73e0\u4e3b\u4eba");
      this.setChinese("\u900f\u89c6");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (this.mode.getValue() != ESP.RenderMode.TwoD) {
         if (this.item.getValue()) {
            for (Entity var3 : Alien.THREAD.threadSafeEntityList) {
               if (var3 instanceof ItemEntity var4) {
                  int var5 = var4.getStack().getCount();
                  String var6 = this.count.getValue() && var5 > 1 ? " x" + var5 : "";
                  String var7 = (this.customName.getValue() ? var4.getStack().getName() : var4.getStack().getItem().getName()).getString();
                  Render3DUtil.drawText3D(
                     var7 + var6,
                     ((IEntity)var4)
                        .getDimensions()
                        .getBoxAt(
                           new Vec3d(
                              MathUtil.interpolate(var4.lastRenderX, var4.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(var4.lastRenderY, var4.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(var4.lastRenderZ, var4.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                           )
                        )
                        .expand(0.0, 0.1, 0.0)
                        .getCenter()
                        .add(0.0, 0.5, 0.0),
                     this.text.getValue()
                  );
               }
            }
         }

         if (this.target.getValue()) {
            if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
               for (Entity var10 : Alien.THREAD.threadSafeEntityList) {
                  if (!(var10 instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                     if ((var10 != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson())
                        && var10 instanceof PlayerEntity
                        && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                        Color var13 = this.playerFill.getValue();
                        Render3DUtil.draw3DBox(
                           var1,
                           ((IEntity)var10)
                              .getDimensions()
                              .getBoxAt(
                                 new Vec3d(
                                    MathUtil.interpolate(var10.lastRenderX, var10.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                    MathUtil.interpolate(var10.lastRenderY, var10.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                    MathUtil.interpolate(var10.lastRenderZ, var10.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                                 )
                              )
                              .expand(0.0, 0.1, 0.0),
                           var13,
                           this.playerOutline.getValue(),
                           this.playerOutline.booleanValue,
                           this.playerFill.booleanValue
                        );
                     }
                  } else {
                     Color var12 = this.itemFill.getValue();
                     Render3DUtil.draw3DBox(
                        var1,
                        ((IEntity)var10)
                           .getDimensions()
                           .getBoxAt(
                              new Vec3d(
                                 MathUtil.interpolate(var10.lastRenderX, var10.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                 MathUtil.interpolate(var10.lastRenderY, var10.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                 MathUtil.interpolate(var10.lastRenderZ, var10.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                              )
                           ),
                        var12,
                        this.itemOutline.getValue(),
                        this.itemOutline.booleanValue,
                        this.itemFill.booleanValue
                     );
                  }
               }
            }

            for (BlockEntity var11 : BlockUtil.getTileEntities()) {
               if (!(var11 instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
                  if (!(var11 instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                     if (!(var11 instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                        if (var11 instanceof EndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                           Box var17 = new Box(var11.getPos());
                           Render3DUtil.draw3DBox(
                              var1,
                              var17,
                              this.endPortalFill.getValue(),
                              this.endPortalOutline.getValue(),
                              this.endPortalOutline.booleanValue,
                              this.endPortalFill.booleanValue
                           );
                        }
                     } else {
                        Box var16 = new Box(var11.getPos());
                        Render3DUtil.draw3DBox(
                           var1,
                           var16,
                           this.shulkerBoxFill.getValue(),
                           this.shulkerBoxOutline.getValue(),
                           this.shulkerBoxOutline.booleanValue,
                           this.shulkerBoxFill.booleanValue
                        );
                     }
                  } else {
                     Box var15 = new Box(var11.getPos());
                     Render3DUtil.draw3DBox(
                        var1,
                        var15,
                        this.enderChestFill.getValue(),
                        this.enderChestOutline.getValue(),
                        this.enderChestOutline.booleanValue,
                        this.enderChestFill.booleanValue
                     );
                  }
               } else {
                  Box var14 = new Box(var11.getPos());
                  Render3DUtil.draw3DBox(
                     var1, var14, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue
                  );
               }
            }
         }
      }
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (this.mode.getValue() == ESP.RenderMode.TwoD && !nullCheck()) {
         if (this.target.getValue()) {
            for (Entity var4 : Alien.THREAD.threadSafeEntityList) {
               if (var4 instanceof ItemEntity) {
                  boolean var6 = this.itemOutline.booleanValue;
                  boolean var5 = this.itemFill.booleanValue;
                  if (var6 || var5) {
                     this.draw2DForEntity(var1.getMatrices(), var4, this.itemOutline.getValue(), this.itemFill.getValue(), var6, var5, var2, false);
                  }
               } else if ((var4 != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson()) && var4 instanceof PlayerEntity) {
                  boolean var10 = this.playerOutline.booleanValue;
                  boolean var9 = this.playerFill.booleanValue;
                  if (var10 || var9) {
                     this.draw2DForEntity(var1.getMatrices(), var4, this.playerOutline.getValue(), this.playerFill.getValue(), var10, var9, var2, true);
                  }
               }
            }
         }

         if (this.item.getValue()) {
            for (Entity var8 : Alien.THREAD.threadSafeEntityList) {
               if (var8 instanceof ItemEntity) {
                  this.draw2DItemText(var1, (ItemEntity)var8, var2);
               }
            }
         }
      }
   }

   private void draw2DForEntity(MatrixStack var1, Entity var2, Color var3, Color var4, boolean var5, boolean var6, float var7, boolean var8) {
      double[] var9 = this.getEntity2DBox(var2, var7);
      if (var9 != null) {
         float var10 = (float)var9[0];
         float var11 = (float)var9[1];
         float var12 = (float)var9[2];
         float var13 = (float)var9[3];
         if (!(var12 - var10 <= 0.0F) && !(var13 - var11 <= 0.0F)) {
            if (var6) {
               Render2DUtil.drawRect(var1, var10, var11, var12 - var10, var13 - var11, var4);
            }

            if (var5) {
               this.draw2DBox(var1, var10, var11, var12, var13, var3);
            }

            LivingEntity var14;
            if (this.twoDHealth.getValue() && var2 instanceof LivingEntity && (var14 = (LivingEntity)var2).getMaxHealth() > 0.0F && var14.getHealth() > 0.0F) {
               this.draw2DHealthBar(var1, var10, var11, var13, var14);
            }

            if (var8 && this.twoDArmorDura.getValue() && var2 instanceof PlayerEntity) {
               this.draw2DArmorDura(var1, var12, var11, var13, (PlayerEntity)var2);
            }
         }
      }
   }

   private void draw2DBox(MatrixStack var1, float var2, float var3, float var4, float var5, Color var6) {
      if (this.twoDOutline.getValue()) {
         Render2DUtil.drawRect(var1, var2 - 1.0F, var3, 1.5F, var5 - var3 + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(var1, var2 - 1.0F, var3 - 0.5F, var4 - var2 + 1.5F, 1.5F, Color.BLACK);
         Render2DUtil.drawRect(var1, var4 - 1.0F, var3, 1.5F, var5 - var3 + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(var1, var2 - 1.0F, var5 - 1.0F, var4 - var2 + 1.5F, 1.5F, Color.BLACK);
      }

      Render2DUtil.drawRect(var1, var2 - 0.5F, var3, 0.5F, var5 - var3, var6);
      Render2DUtil.drawRect(var1, var2, var5 - 0.5F, var4 - var2, 0.5F, var6);
      Render2DUtil.drawRect(var1, var2 - 0.5F, var3, var4 - var2 + 0.5F, 0.5F, var6);
      Render2DUtil.drawRect(var1, var4 - 0.5F, var3, 0.5F, var5 - var3, var6);
   }

   private void draw2DHealthBar(MatrixStack var1, float var2, float var3, float var4, LivingEntity var5) {
      float var6 = var4 - var3;
      float var7 = var5.getHealth();
      float var8 = var5.getMaxHealth();
      float var9 = Math.max(0.0F, Math.min(1.0F, var7 / var8));
      Render2DUtil.drawRect(var1, var2 - 4.0F, var3, 1.0F, var6, Color.BLACK);
      Render2DUtil.drawRect(var1, var2 - 4.0F, var4 - var6 * var9, 1.0F, var6 * var9, this.getHealthColor(var7));
   }

   private Color getHealthColor(float var1) {
      if (var1 >= 20.0F) {
         return this.hHealth.getValue();
      } else {
         return var1 > 10.0F ? this.mHealth.getValue() : this.lHealth.getValue();
      }
   }

   private void draw2DArmorDura(MatrixStack var1, float var2, float var3, float var4, PlayerEntity var5) {
      float var6 = var4 - var3;
      float var7 = var6 / 4.0F;
      ArrayList var8 = new ArrayList();
      var8.add((ItemStack)var5.getInventory().armor.get(3));
      var8.add((ItemStack)var5.getInventory().armor.get(2));
      var8.add((ItemStack)var5.getInventory().armor.get(1));
      var8.add((ItemStack)var5.getInventory().armor.get(0));

      for (int var9 = 0; var9 < var8.size(); var9++) {
         ItemStack var10 = (ItemStack)var8.get(var9);
         if (!var10.isEmpty() && var10.getMaxDamage() > 0) {
            float var11 = (float)(var10.getMaxDamage() - var10.getDamage()) / var10.getMaxDamage();
            var11 = Math.max(0.0F, Math.min(1.0F, var11));
            float var12 = var3 + var7 * var9;
            float var13 = var12 + var7;
            float var14 = var7 * var11 * this.duraScale.getValueFloat();
            var14 = Math.min(var7, var14);
            Render2DUtil.drawRect(var1, var2 + 1.5F, var13 - var14, 1.5F, var14, this.armorDuraColor.getValue());
         }
      }
   }

   private double[] getEntity2DBox(Entity var1, float var2) {
      double var4 = MathUtil.interpolate(var1.lastRenderX, var1.getX(), (double)var2);
      double var6 = MathUtil.interpolate(var1.lastRenderY, var1.getY(), (double)var2);
      double var8 = MathUtil.interpolate(var1.lastRenderZ, var1.getZ(), (double)var2);
      Box var10 = var1.getBoundingBox();
      Box var11 = new Box(
         var10.minX - var1.getX() + var4 - 0.05,
         var10.minY - var1.getY() + var6,
         var10.minZ - var1.getZ() + var8 - 0.05,
         var10.maxX - var1.getX() + var4 + 0.05,
         var10.maxY - var1.getY() + var6 + 0.15,
         var10.maxZ - var1.getZ() + var8 + 0.05
      );
      Vec3d[] var12 = new Vec3d[]{
         new Vec3d(var11.minX, var11.minY, var11.minZ),
         new Vec3d(var11.minX, var11.maxY, var11.minZ),
         new Vec3d(var11.maxX, var11.minY, var11.minZ),
         new Vec3d(var11.maxX, var11.maxY, var11.minZ),
         new Vec3d(var11.minX, var11.minY, var11.maxZ),
         new Vec3d(var11.minX, var11.maxY, var11.maxZ),
         new Vec3d(var11.maxX, var11.minY, var11.maxZ),
         new Vec3d(var11.maxX, var11.maxY, var11.maxZ)
      };
      double var13 = Double.POSITIVE_INFINITY;
      double var15 = Double.POSITIVE_INFINITY;
      double var17 = Double.NEGATIVE_INFINITY;
      double var19 = Double.NEGATIVE_INFINITY;
      boolean var21 = false;

      for (Vec3d var25 : var12) {
         Vec3d var26 = TextUtil.worldSpaceToScreenSpace(var25);
         if (var26.z > 0.0 && var26.z < 1.0) {
            var21 = true;
            var13 = Math.min(var26.x, var13);
            var15 = Math.min(var26.y, var15);
            var17 = Math.max(var26.x, var17);
            var19 = Math.max(var26.y, var19);
         }
      }

      double[] var3;
      if (!var21) {
         var3 = null;
      } else {
         double[] var27 = new double[]{var13, var15, var17, 0.0};
         var3 = var27;
         var27[3] = var19;
      }

      return var3;
   }

   private void draw2DItemText(DrawContext var1, ItemEntity var2, float var3) {
      double[] var4 = this.getEntity2DBox(var2, var3);
      if (var4 != null) {
         int var5 = var2.getStack().getCount();
         String var6 = this.count.getValue() && var5 > 1 ? " x" + var5 : "";
         String var7 = (this.customName.getValue() ? var2.getStack().getName() : var2.getStack().getItem().getName()).getString();
         String var8 = var7 + var6;
         int var9 = mc.textRenderer.getWidth(var8);
         float var10 = (float)(var4[0] + (var4[2] - var4[0]) / 2.0 - var9 / 2.0);
         var1.drawText(mc.textRenderer, var8, (int)var10, (int)(var4[1] - 10.0), this.text.getValue().getRGB(), false);
      }
   }

   @EventListener
   public void onReceivePacket(EntitySpawnedEvent var1) {
      Entity var2;
      if (!nullCheck() && this.pearl.getValue() && (var2 = var1.getEntity()) instanceof EnderPearlEntity) {
         EnderPearlEntity var3 = (EnderPearlEntity)var2;
         if (var3.getOwner() != null) {
            var3.setCustomName(var3.getOwner().getName());
            var3.setCustomNameVisible(true);
         } else {
            mc.world
               .getPlayers()
               .stream()
               .min(Comparator.comparingDouble(var1x -> var1x.getPos().distanceTo(new Vec3d(var3.getX(), var3.getY(), var3.getZ()))))
               .ifPresent(var1x -> {
                  var3.setCustomName(var1x.getName());
                  var3.setCustomNameVisible(true);
               });
         }
      }
   }

   private static enum RenderMode {
      ThreeD,
      TwoD;
   }
}
