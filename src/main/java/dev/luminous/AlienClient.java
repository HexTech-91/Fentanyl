package dev.luminous;

import dev.luminous.mod.entities.UltramanEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;

public class AlienClient implements ClientModInitializer {
   public void onInitializeClient() {
      EntityRendererRegistry.register(EntityType.GIANT, UltramanEntityRenderer::new);
   }
}
