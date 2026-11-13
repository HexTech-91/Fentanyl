package dev.luminous.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class GUICharacter extends Module {
   public static GUICharacter INSTANCE;
   public final EnumSetting<GUICharacter.CharacterType> characterType = this.add(new EnumSetting<>("Character", GUICharacter.CharacterType.Character1));
   public final SliderSetting x = this.add(new SliderSetting("X", 100, 0, 1500));
   public final SliderSetting y = this.add(new SliderSetting("Y", 200, 0, 1000));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.1, 3.0, 0.1));
   public final BooleanSetting showInClickGui = this.add(new BooleanSetting("ShowInClickGui", true));
   public final BooleanSetting showInGame = this.add(new BooleanSetting("ShowInGame", false));

   public GUICharacter() {
      super("GUICharacter", Module.Category.Client);
      this.setChinese("GUI\u89d2\u8272");
      this.characterType.setChinese("\u89d2\u8272");
      this.x.setChinese("X\u5750\u6807");
      this.y.setChinese("Y\u5750\u6807");
      this.scale.setChinese("\u7f29\u653e");
      this.showInClickGui.setChinese("GUI\u663e\u793a");
      this.showInGame.setChinese("\u6e38\u620f\u5185\u663e\u793a");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      boolean inGui = mc.currentScreen != null;
      boolean shouldRender = inGui && this.showInClickGui.getValue() || !inGui && this.showInGame.getValue();
      if (shouldRender) {
         float sc = this.scale.getValueFloat();
         int w = Math.max(10, Math.round(100.0F * sc));
         int h = Math.max(10, Math.round(100.0F * sc));
         int rx = this.x.getValueInt();
         int ry = this.y.getValueInt();
         drawTexture(context, this.characterType.getValue().texture, rx, ry, w, h);
      }
   }

   private static void drawTexture(DrawContext context, Identifier texture, float x, float y, float width, float height) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.setShaderTexture(0, texture);
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 1.0F).color(255, 255, 255, 255);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(255, 255, 255, 255);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 0.0F).color(255, 255, 255, 255);
      bufferBuilder.vertex(matrix, x, y, 0.0F).texture(0.0F, 0.0F).color(255, 255, 255, 255);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static enum CharacterType {
      Character1("character_1.png"),
      Character2("character_2.png"),
      Character3("character_3.png"),
      Character4("character_4.png"),
      Character5("character_5.png"),
      Character7("character_7.png"),
      Character9("character_9.png"),
      Character10("character_10.png");

      private final Identifier texture;

      private CharacterType(String file) {
         this.texture = Identifier.of("alien", "textures/character/" + file);
      }
   }
}
