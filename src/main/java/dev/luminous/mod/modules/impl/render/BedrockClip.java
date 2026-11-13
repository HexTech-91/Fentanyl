package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class BedrockClip extends Module {
   public static BedrockClip INSTANCE;
   private static final SoundEvent CLIP_SOUND = SoundEvent.of(Identifier.of("minecraft", "alien.bedrockclip.pop"));
   private static final Identifier TEX = Identifier.of("alien", "textures/bedrockclip/jiraya.png");
   private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 1.0, 32.0, 1.0).setChinese("\u68c0\u6d4b\u8303\u56f4"));
   private final SliderSetting alpha = this.add(new SliderSetting("Alpha", 1.0, 0.1, 1.0, 0.05).setChinese("\u900f\u660e\u5ea6"));
   private final SliderSetting size = this.add(new SliderSetting("Size", 2.0, 0.3, 3.0, 0.1).setChinese("\u5927\u5c0f"));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 1.2, 0.0, 3.0, 0.1).setChinese("\u9ad8\u5ea6\u504f\u79fb"));
   private final SliderSetting flashMs = this.add(new SliderSetting("FlashMs", 1000.0, 200.0, 3000.0, 100.0).setChinese("\u95ea\u73b0\u65f6\u957f(ms)"));
   private final SliderSetting minMs = this.add(new SliderSetting("MinStuckMs", 100.0, 0.0, 5000.0, 100.0).setChinese("\u6700\u5c11\u5361\u4f4f(ms)"));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setChinese("\u64ad\u653e\u97f3\u6548"));
   private final Map<UUID, Long> clipped = new HashMap<>();
   private final Set<UUID> stillClipped = new HashSet<>();
   private final Map<UUID, Long> stuckSince = new HashMap<>();

   public BedrockClip() {
      super("BedrockClip", Module.Category.Render);
      this.setChinese("\u5361\u5899\u68c0\u6d4b");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.clipped.clear();
      this.stillClipped.clear();
      this.stuckSince.clear();
   }

   @Override
   public void onDisable() {
      this.clipped.clear();
      this.stillClipped.clear();
      this.stuckSince.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (this.isOn()) {
            long var2 = System.currentTimeMillis();
            double var4 = this.range.getValue();
            double var6 = var4 * var4;

            for (PlayerEntity var9 : mc.world.getPlayers()) {
               if (var9 != mc.player && var9.isAlive() && (Alien.FRIEND == null || !Alien.FRIEND.isFriend(var9)) && !(mc.player.squaredDistanceTo(var9) > var6)
                  )
                {
                  boolean var10 = this.bboxIntersectsBedrock(var9);
                  if (var10) {
                     if (!this.stuckSince.containsKey(var9.getUuid())) {
                        this.stuckSince.put(var9.getUuid(), var2);
                     }

                     if (!this.stillClipped.contains(var9.getUuid()) && var2 - this.stuckSince.get(var9.getUuid()) >= (long)this.minMs.getValue()) {
                        this.stillClipped.add(var9.getUuid());
                        this.clipped.put(var9.getUuid(), var2);
                        if (this.sound.getValue()) {
                           try {
                              mc.getSoundManager().play(PositionedSoundInstance.master(CLIP_SOUND, 1.0F, 1.0F));
                           } catch (Throwable var12) {
                           }
                        }
                     }
                  } else {
                     this.stuckSince.remove(var9.getUuid());
                     this.stillClipped.remove(var9.getUuid());
                     this.clipped.remove(var9.getUuid());
                  }
               }
            }

            Iterator var13 = this.clipped.entrySet().iterator();

            while (var13.hasNext()) {
               Entry var14 = (Entry)var13.next();
               if (var2 - (Long)var14.getValue() > (long)this.flashMs.getValue()) {
                  var13.remove();
               }
            }
         }
      }
   }

   private boolean bboxIntersectsBedrock(Entity var1) {
      Box var2 = var1.getBoundingBox().expand(-0.01);
      int var3 = (int)Math.floor(var2.minX);
      int var4 = (int)Math.floor(var2.minY);
      int var5 = (int)Math.floor(var2.minZ);
      int var6 = (int)Math.floor(var2.maxX);
      int var7 = (int)Math.floor(var2.maxY);
      int var8 = (int)Math.floor(var2.maxZ);

      for (int var9 = var3; var9 <= var6; var9++) {
         for (int var10 = var4; var10 <= var7; var10++) {
            for (int var11 = var5; var11 <= var8; var11++) {
               if (mc.world.getBlockState(new BlockPos(var9, var10, var11)).isOf(Blocks.BEDROCK)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @EventListener
   public void onRender3D(Render3DEvent var1) {
      if (mc.player != null && mc.world != null && !this.clipped.isEmpty()) {
         long var2 = System.currentTimeMillis();

         for (PlayerEntity var5 : mc.world.getPlayers()) {
            Long var6 = this.clipped.get(var5.getUuid());
            if (var6 != null && var2 - var6 <= (long)this.flashMs.getValue()) {
               this.drawMarker(var1.matrixStack, var5.getX(), var5.getY() + this.yOffset.getValue(), var5.getZ(), (float)this.alpha.getValue(), var2 - var6);
            }
         }
      }
   }

   private void drawMarker(MatrixStack var1, double var2, double var4, double var6, float var8, long var9) {
      try {
         Vec3d var11 = mc.gameRenderer.getCamera().getPos();
         float var12 = mc.gameRenderer.getCamera().getYaw();
         float var13 = mc.gameRenderer.getCamera().getPitch();
         MatrixStack var14 = new MatrixStack();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         var14.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var13));
         var14.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var12 + 180.0F));
         var14.translate(var2 - var11.x, var4 - var11.y, var6 - var11.z);
         var14.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-var12));
         var14.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var13));
         double var15 = 0.0245 * this.size.getValue();
         var14.scale((float)var15, (float)var15, (float)var15);
         float var17 = 1.0F - Math.min(1.0F, (float)var9 / (float)this.flashMs.getValue());
         float var18 = var8 * var17;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var18);
         RenderSystem.setShaderTexture(0, TEX);
         this.drawTexture(var14, -32.0F, -32.0F, 64.0F, 64.0F);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } catch (Throwable var19) {
      }
   }

   private void drawTexture(MatrixStack var1, float var2, float var3, float var4, float var5) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      Matrix4f var6 = var1.peek().getPositionMatrix();
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      var7.vertex(var6, var2, var3 + var5, 0.0F).texture(0.0F, 0.0F);
      var7.vertex(var6, var2 + var4, var3 + var5, 0.0F).texture(1.0F, 0.0F);
      var7.vertex(var6, var2 + var4, var3, 0.0F).texture(1.0F, 1.0F);
      var7.vertex(var6, var2, var3, 0.0F).texture(0.0F, 1.0F);
      BufferRenderer.drawWithGlobalProgram(var7.end());
   }
}
