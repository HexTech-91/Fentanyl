package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public class PlayerDoll extends Module {
   public static PlayerDoll INSTANCE;

   public PlayerDoll() {
      super("PlayerDoll", Module.Category.HUD);
      this.setChinese("玩家玩偶");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (nullCheck()) {
         return;
      }

      float x = Alien.GUI.playerDoll.getX();
      float y = Alien.GUI.playerDoll.getY();
      float width = Alien.GUI.playerDoll.getWidth();
      float height = Alien.GUI.playerDoll.getHeight();
      float dollHeight = Math.max(height * 0.7F, 10.0F);
      float size = dollHeight / 1.8F;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      matrices.translate(centerX, centerY, 50.0);
      matrices.scale(size, size, -size);
      matrices.translate(0.0F, 0.9F, 0.0F);
      matrices.multiply(new Quaternionf().rotateZ((float)Math.PI));
      DiffuseLighting.method_34742();
      EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      dispatcher.setRotation(new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.PI));
      dispatcher.setRenderShadows(false);
      dispatcher.render(mc.player, 0.0, 0.0, 0.0, 0.0F, tickDelta, matrices, drawContext.getVertexConsumers(), 15728880);
      drawContext.draw();
      dispatcher.setRenderShadows(true);
      matrices.pop();
      DiffuseLighting.enableGuiDepthLighting();
   }
}
