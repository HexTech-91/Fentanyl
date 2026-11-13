package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class PlayerInfo extends Module {
   public static PlayerInfo INSTANCE;
   private final ColorSetting textColor = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255)));
   private final BooleanSetting customFont = this.add(new BooleanSetting("CustomFont", true));

   public PlayerInfo() {
      super("PlayerInfoHud", Module.Category.HUD);
      this.setChinese("玩家信息");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (nullCheck()) {
         return;
      }

      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      float xOffset = (float)Alien.GUI.playerInfo.getX();
      float yOffset = (float)Alien.GUI.playerInfo.getY();
      Render2DUtil.drawHudBase(matrices, xOffset, yOffset, 200.0F, 80.0F);
      Render2DUtil.drawHudBase(matrices, xOffset, yOffset, 200.0F, FontManager.ui.getFontHeight());
      FontManager.ui
         .drawString(
            matrices,
            "Player Info",
            (float)(xOffset + HudEditor.INSTANCE.hudRound.getValue() + 2.0),
            (float)(yOffset + 2.0),
            Color.WHITE.getRGB()
         );
      drawContext.drawTexture(mc.player.getSkinTextures().texture(), (int)(xOffset + 10.0F), (int)(yOffset + 25.0F), 40, 40, 8.0F, 8.0F, 8, 8, 64, 64);
      if (this.customFont.getValue()) {
         FontManager.ui
            .drawString(
               matrices,
               mc.player.getName().getString(),
               (float)(xOffset + (200.0F - FontManager.ui.getWidth(mc.player.getName().getString())) / 2.0),
               yOffset + 15.0F,
               this.textColor.getValue().getRGB(),
               false
            );
      } else {
         TextUtil.drawString(
            drawContext,
            mc.player.getName().getString(),
            (float)(xOffset + (200.0F - FontManager.ui.getWidth(mc.player.getName().getString())) / 2.0),
            yOffset + 15.0F,
            this.textColor.getValue().getRGB(),
            false
         );
      }

      TextUtil.drawString(
         drawContext,
         "Pos: " + mc.player.getBlockPos().getX() + " " + mc.player.getBlockPos().getY() + " " + mc.player.getBlockPos().getZ(),
         xOffset + 65.0F,
         yOffset + 30.0F,
         this.textColor.getValue().getRGB(),
         this.customFont.getValue()
      );
      TextUtil.drawString(
         drawContext,
         "NetherPos: " + mc.player.getBlockPos().getX() / 8 + " " + mc.player.getBlockPos().getY() / 8 + " " + mc.player.getBlockPos().getZ() / 8,
         xOffset + 65.0F,
         yOffset + 40.0F,
         this.textColor.getValue().getRGB(),
         this.customFont.getValue()
      );
      TextUtil.drawString(
         drawContext,
         "Pop: " + Alien.POP.getPop(mc.player.getName().getString()),
         xOffset + 65.0F,
         yOffset + 50.0F,
         this.textColor.getValue().getRGB(),
         this.customFont.getValue()
      );
      matrices.pop();
   }
}
