package dev.luminous.mod.modules.impl.hud;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

public class Radar extends Module {
   public static Radar INSTANCE;
   public final SliderSetting size = this.add(new SliderSetting("Size", 80, 20, 300));
   private final ColorSetting color2 = this.add(new ColorSetting("Color", new Color(0xFF101010)));
   private final ColorSetting color3 = this.add(new ColorSetting("PlayerColor", new Color(0xC59B9B9B)));

   public Radar() {
      super("Radar", Module.Category.HUD);
      this.setChinese("雷达");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      Render2DUtil.drawHudBase(
         context.getMatrices(), (float)this.getPosX(), (float)this.getPosY(), (float)this.size.getValue(), (float)this.size.getValue(), HudEditor.INSTANCE.hudRound.getValueFloat()
      );
      if (HudEditor.INSTANCE.hudStyle.is(HudEditor.HudStyle.Blurry)) {
         Render2DUtil.drawRectDumbWay(
            context.getMatrices(),
            (float)this.getPosX(),
            (float)(this.getPosY() + (this.size.getValue() / 2.0F + 0.25F)),
            (float)(this.getPosX() + this.size.getValue()),
            (float)(this.getPosY() + (this.size.getValue() / 2.0F) - 0.25F),
            new Color(0x54FFFFFF, true)
         );
         Render2DUtil.drawRectDumbWay(
            context.getMatrices(),
            (float)(this.getPosX() + (this.size.getValue() / 2.0F - 0.25F)),
            this.getPosY() - 0.5F,
            (float)(this.getPosX() + (this.size.getValue() / 2.0F) + 0.25F),
            (float)(this.getPosY() + this.size.getValue() - 1.0),
            new Color(0x54FFFFFF, true)
         );
      } else {
         Render2DUtil.draw2DGradientRect(
            context.getMatrices(),
            (float)(this.getPosX() + (this.size.getValue() / 2.0F - 0.5)),
            (float)(this.getPosY() + 3.5),
            (float)(this.getPosX() + (this.size.getValue() / 2.0F + 0.2)),
            (float)(this.getPosY() + this.size.getValue() - 3.5),
            this.color2.getValue(),
            this.color2.getValue(),
            this.color2.getValue(),
            this.color2.getValue()
         );
         Render2DUtil.draw2DGradientRect(
            context.getMatrices(),
            this.getPosX() + 3.5F,
            (float)(this.getPosY() + (this.size.getValue() / 2.0F - 0.2F)),
            (float)(this.getPosX() + this.size.getValue() - 3.5),
            (float)(this.getPosY() + (this.size.getValue() / 2.0F + 0.5F)),
            this.color2.getValue(),
            this.color2.getValue(),
            this.color2.getValue(),
            this.color2.getValue()
         );
      }

      if (mc.world == null) {
         return;
      }

      for (PlayerEntity entityPlayer : mc.world.getPlayers()) {
         if (entityPlayer == mc.player) {
            continue;
         }

         float posX = (float)(entityPlayer.prevX + (entityPlayer.prevX - entityPlayer.getX()) * Render3DUtil.getTickDelta() - mc.player.getX()) * 2.0F;
         float posZ = (float)(entityPlayer.prevZ + (entityPlayer.prevZ - entityPlayer.getZ()) * Render3DUtil.getTickDelta() - mc.player.getZ()) * 2.0F;
         float cos = (float)Math.cos(mc.player.getYaw(Render3DUtil.getTickDelta()) * 0.017453292);
         float sin = (float)Math.sin(mc.player.getYaw(Render3DUtil.getTickDelta()) * 0.017453292);
         float rotY = -(posZ * cos - posX * sin);
         float rotX = -(posX * cos + posZ * sin);
         if (rotY > this.size.getValue() / 2.0F - 6.0F) {
            rotY = (float)(this.size.getValue() / 2.0F - 6.0F);
         } else if (rotY < -(this.size.getValue() / 2.0F - 8.0F)) {
            rotY = (float)(-(this.size.getValue() / 2.0F - 8.0F));
         }

         if (rotX > this.size.getValue() / 2.0F - 5.0F) {
            rotX = (float)(this.size.getValue() / 2.0F - 5.0F);
         } else if (rotX < -(this.size.getValue() / 2.0F - 5.0F)) {
            rotX = (float)(-(this.size.getValue() / 2.0F - 5.0F));
         }

         Render2DUtil.drawRound(
            context.getMatrices(),
            (float)(this.getPosX() + this.size.getValue() / 2.0F + rotX - 2.0),
            (float)(this.getPosY() + this.size.getValue() / 2.0F + rotY - 2.0),
            4.0F,
            4.0F,
            2.0F,
            this.color3.getValue()
         );
      }
   }

   private int getPosX() {
      return Alien.GUI.radar.getX();
   }

   private int getPosY() {
      return Alien.GUI.radar.getY();
   }
}
