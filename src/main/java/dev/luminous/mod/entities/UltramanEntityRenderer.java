package dev.luminous.mod.entities;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.util.Identifier;

public class UltramanEntityRenderer extends MobEntityRenderer<GiantEntity, PlayerEntityModel<GiantEntity>> {
   private static final Identifier TIGA_TEXTURE = Identifier.of("alien", "textures/entity/ultraman.png");

   public UltramanEntityRenderer(Context context) {
      super(context, createPlayerModel(), 0.5F);
   }

   private static PlayerEntityModel<GiantEntity> createPlayerModel() {
      ModelData modelData = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
      ModelPart root = TexturedModelData.of(modelData, 64, 64).createModel();
      return new PlayerEntityModel(root, false);
   }

   public Identifier getTexture(GiantEntity entity) {
      return TIGA_TEXTURE;
   }

   protected void scale(GiantEntity entity, MatrixStack matrices, float amount) {
      matrices.scale(6.0F, 6.0F, 6.0F);
   }

   public void render(GiantEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
      ((PlayerEntityModel)this.model).setAngles(entity, 0.0F, 0.0F, entity.age + tickDelta, entity.headYaw, entity.getPitch());
      super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
   }
}
