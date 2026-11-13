package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.impl.combat.EnemyMark;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   final ColorSetting colorConfig = this.add(new ColorSetting("ColorConfig", new Color(255, 255, 255)));
   final ColorSetting friendConfig;
   final ColorSetting invisibleConfig;
   final ColorSetting died;
   final ColorSetting sneakingConfig;
   final ColorSetting rectConfig;
   final BooleanSetting armorConfig;
   final BooleanSetting drawItemConfig;
   final SliderSetting offsetConfig;
   final BooleanSetting enchantmentsConfig;
   final BooleanSetting durabilityConfig;
   final BooleanSetting forceBarConfig;
   final BooleanSetting itemNameConfig;
   final BooleanSetting entityIdConfig;
   final BooleanSetting gamemodeConfig;
   final BooleanSetting pingConfig;
   final BooleanSetting healthConfig;
   final BooleanSetting totemsConfig;
   final SliderSetting scaleConfig;
   final BooleanSetting factorConfig;
   final SliderSetting scalingConfig;
   final SliderSetting distanceConfig;
   final SliderSetting heightConfig;
   final DecimalFormat df;

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      this.colorConfig.setChinese("\u989c\u8272");
      this.friendConfig = this.add(new ColorSetting("FriendConfig", new Color(155, 155, 255)).injectBoolean(true));
      this.friendConfig.setChinese("\u597d\u53cb");
      this.invisibleConfig = this.add(new ColorSetting("InvisibleConfig", new Color(200, 200, 200)).injectBoolean(true));
      this.invisibleConfig.setChinese("\u9690\u8eab");
      this.died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
      this.died.setChinese("\u6b7b\u4ea1");
      this.sneakingConfig = this.add(new ColorSetting("SneakingConfig", new Color(200, 200, 0)).injectBoolean(true));
      this.sneakingConfig.setChinese("\u6f5c\u884c");
      this.rectConfig = this.add(new ColorSetting("RectConfig", new Color(0, 0, 0, 100)).injectBoolean(true));
      this.rectConfig.setChinese("\u77e9\u5f62");
      this.armorConfig = this.add(new BooleanSetting("ArmorConfig", true).setParent());
      this.armorConfig.setChinese("\u76d4\u7532");
      this.drawItemConfig = this.add(new BooleanSetting("DrawItemConfig", true, this.armorConfig::isOpen));
      this.drawItemConfig.setChinese("\u7ed8\u5236\u7269\u54c1");
      this.offsetConfig = this.add(new SliderSetting("OffsetConfig", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
      this.offsetConfig.setChinese("\u504f\u79fb");
      this.enchantmentsConfig = this.add(new BooleanSetting("EnchantmentsConfig", true));
      this.enchantmentsConfig.setChinese("\u9644\u9b54");
      this.durabilityConfig = this.add(new BooleanSetting("DurabilityConfig", true).setParent());
      this.durabilityConfig.setChinese("\u8010\u4e45");
      this.forceBarConfig = this.add(new BooleanSetting("ForceBarConfig", true, this.durabilityConfig::isOpen));
      this.forceBarConfig.setChinese("\u5f3a\u5236\u6761");
      this.itemNameConfig = this.add(new BooleanSetting("ItemNameConfig", false));
      this.itemNameConfig.setChinese("\u7269\u54c1\u540d");
      this.entityIdConfig = this.add(new BooleanSetting("EntityIdConfig", false));
      this.entityIdConfig.setChinese("\u5b9e\u4f53ID");
      this.gamemodeConfig = this.add(new BooleanSetting("GamemodeConfig", false));
      this.gamemodeConfig.setChinese("\u6e38\u620f\u6a21\u5f0f");
      this.pingConfig = this.add(new BooleanSetting("PingConfig", true));
      this.pingConfig.setChinese("\u5ef6\u8fdf");
      this.healthConfig = this.add(new BooleanSetting("HealthConfig", true));
      this.healthConfig.setChinese("\u751f\u547d");
      this.totemsConfig = this.add(new BooleanSetting("TotemsConfig", false));
      this.totemsConfig.setChinese("\u56fe\u817e");
      this.scaleConfig = this.add(new SliderSetting("ScaleConfig", 1.0, 0.0, 3.0, 0.1));
      this.scaleConfig.setChinese("\u7f29\u653e");
      this.factorConfig = this.add(new BooleanSetting("FactorConfig", true).setParent());
      this.factorConfig.setChinese("\u56e0\u5b50");
      this.scalingConfig = this.add(new SliderSetting("ScalingConfig", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
      this.scalingConfig.setChinese("\u7f29\u653e\u6bd4\u4f8b");
      this.distanceConfig = this.add(new SliderSetting("DistanceConfig", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
      this.distanceConfig.setChinese("\u8ddd\u79bb");
      this.heightConfig = this.add(new SliderSetting("HeightConfig", 0.0, -3.0, 3.0, 0.01));
      this.heightConfig.setChinese("\u9ad8\u5ea6");
      this.df = new DecimalFormat("0.0");
      this.setChinese("\u540d\u5b57\u6807\u7b7e");
      INSTANCE = this;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Camera camera = mc.gameRenderer.getCamera();
         RenderSystem.enableBlend();
         GL11.glDepthFunc(519);
         MatrixStack matrixStack = new MatrixStack();

         for (Entity entity : Alien.THREAD.threadSafeEntityList) {
            if (entity instanceof PlayerEntity playerEntity
               && (this.died.booleanValue || playerEntity.isAlive())
               && (playerEntity != mc.player || !mc.options.getPerspective().isFirstPerson())
               && (this.invisibleConfig.booleanValue || !playerEntity.isInvisible())) {
               String info = this.getNametagInfo(playerEntity);
               Vec3d renderPosition = MathUtil.getRenderPosition(playerEntity, event.tickDelta);
               double x = renderPosition.getX();
               double y = renderPosition.getY();
               double z = renderPosition.getZ();
               int width = mc.textRenderer.getWidth(info);
               float hwidth = width / 2.0F;
               this.renderInfo(info, hwidth, playerEntity, x, y, z, camera, matrixStack);
            }
         }

         GL11.glDepthFunc(515);
         RenderSystem.disableBlend();
      }
   }

   private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
      Vec3d pos = camera.getPos();
      double eyeY = y + entity.getHeight() + (entity.isSneaking() ? 0.4F : 0.43F) + this.heightConfig.getValueFloat();
      float scale = (float)(
         -0.025F * this.scaleConfig.getValueFloat()
            + (
               this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()
                  ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025F * this.scalingConfig.getValueFloat()
                  : 0.0
            )
      );
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (scale / -0.025F - 1.0F) / 4.0F, z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);
      if (this.rectConfig.booleanValue) {
         float f = -width - 2.0F;
         Objects.requireNonNull(mc.textRenderer);
         Render2DUtil.drawRect(matrices, f, -1.0F, width * 2.0F + 3.0F, 10.0F, this.rectConfig.getValue());
      }

      this.drawWithShadow(matrices, info, -width, 0.0F, this.getNametagColor(entity));
      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity);
      }

      matrices.pop();
   }

   private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
      Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.textRenderer.draw(info, x, y, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      mc.textRenderer.draw(info, x, y, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      CopyOnWriteArrayList<ItemStack> displayItems = new CopyOnWriteArrayList<>();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach(armorStack -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int n11 = 0;

      for (ItemStack itemStack : displayItems) {
         x -= 8.0F;
         if (itemStack.getEnchantments().getSize() > n11) {
            n11 = itemStack.getEnchantments().getSize();
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for (ItemStack stack : displayItems) {
         GL11.glDepthFunc(519);
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stack, x, y + 1.0F);
         }

         this.renderItemOverlay(matrixStack, stack, x, y + 2.5F);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stack, x + 2.0F, y + 11.5F);
         }

         if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stack, x + 2.0F, y + 7.0F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
         GL11.glDepthFunc(515);
      }

      ItemStack itemStackx = player.getMainHandStack();
      if (!itemStackx.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, itemStackx, y - 4.5F + this.enchantOffset(n11));
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      DiffuseLighting.disableGuiDepthLighting();
      BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, 0);
      Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 16711680, OverlayTexture.DEFAULT_UV, model);
      i.draw();
      DiffuseLighting.enableGuiDepthLighting();
      matrixStack.pop();
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      if (stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         this.drawWithShadow(matrixStack, string, x + 17.0F - mc.textRenderer.getWidth(string), y + 9.0F, -1);
      }

      if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
         int i = stack.getItemBarStep();
         int j = stack.getItemBarColor();
         float k = x + 2.0F;
         float l = y + 13.0F;
         Render2DUtil.drawRect(matrixStack, k, l, 13.0F, 2.0F, -16777216);
         Render2DUtil.drawRect(matrixStack, k, l, i, 1.0F, j | 0xFF000000);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamage();
         int durability = (int)((float)(n - n2) / n * 100.0F);
         this.drawWithShadow(
            matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
         );
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float n2 = 0.0F;

         for (RegistryEntry enchantment : enchants.getEnchantments()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = ((Enchantment)enchantment.value()).toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
               enchantString.append("\u00a7cVan");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("\u00a7cBind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 -= 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         float var3;
         return var3 = value - (n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y) {
      String itemName = itemStack.getName().getString();
      float width = mc.textRenderer.getWidth(itemName) / 4.0F;
      this.drawWithShadow(matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append(Formatting.GOLD);
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append(Formatting.GRAY);
            info.append("[I] ");
         } else {
            info.append(Formatting.BOLD);
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(this.getEntityPing(player));
         info.append("ms ");
         info.append(Formatting.RESET);
      }

      info.append(player.getName().getString());
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = player.getHealth() + player.getAbsorptionAmount();
         Formatting hcolor = health > 18.0
            ? Formatting.GREEN
            : (
               health > 16.0
                  ? Formatting.DARK_GREEN
                  : (health > 12.0 ? Formatting.YELLOW : (health > 8.0 ? Formatting.GOLD : (health > 4.0 ? Formatting.RED : Formatting.DARK_RED)))
            );
         String phealth = this.df.format(health);
         info.append(hcolor);
         info.append(phealth);
         info.append(" ");
      }

      int totems;
      if (this.totemsConfig.getValue() && (totems = Alien.POP.getPop(player)) > 0) {
         Formatting c = TextRadar.getPopColor(totems);
         info.append(c);
         info.append(-totems);
         info.append(" ");
      }

      return info.toString().trim();
   }

   private String getEntityPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) {
         return "\u00a77-1";
      } else {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         if (playerListEntry == null) {
            return "\u00a77-1";
         } else {
            int ping = playerListEntry.getLatency();
            Formatting color = ping >= 200 ? Formatting.RED : (ping >= 100 ? Formatting.YELLOW : Formatting.GREEN);
            return color.toString() + ping;
         }
      }
   }

   private int getNametagColor(PlayerEntity player) {
      if (EnemyMark.isMarked(player)) {
         return -65536;
      } else if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isSneaking()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }
}
