package dev.luminous.core.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.tclick.TClickGuiScreen;
import dev.luminous.mod.gui.elements.ArmorElements;
import dev.luminous.mod.gui.elements.InGameHudElements;
import dev.luminous.mod.gui.elements.InventoryViewerElements;
import dev.luminous.mod.gui.elements.NoirLive2DElements;
import dev.luminous.mod.gui.elements.NotificationElements;
import dev.luminous.mod.gui.elements.PlayerDollElements;
import dev.luminous.mod.gui.elements.PlayerInfoElements;
import dev.luminous.mod.gui.elements.PotionElements;
import dev.luminous.mod.gui.elements.RadarElements;
import dev.luminous.mod.gui.elements.TotemElements;
import dev.luminous.mod.gui.elements.TurtleElements;
import dev.luminous.mod.gui.elements.WaterMarkElements;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.HudEditor;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class HudManager implements Wrapper {
   public final ArmorElements armorHud;
   public final TotemElements totemHud;
   public final TurtleElements turtleHud;
   public final InGameHudElements inGameHud;
   public final PlayerInfoElements playerInfo;
   public final RadarElements radar;
   public final PotionElements potionHud;
   public final WaterMarkElements waterMark;
   public final PlayerDollElements playerDoll;
   public final InventoryViewerElements inventoryViewer;
   public final NotificationElements notificationHud;
   public final NoirLive2DElements noirLive2D;
   public static Tab currentGrabbed = null;
   private int lastMouseX = 0;
   private int lastMouseY = 0;
   private int mouseX;
   private int mouseY;

   public int getMouseX() {
      return this.mouseX;
   }

   public int getMouseY() {
      return this.mouseY;
   }

   public HudManager() {
      this.armorHud = new ArmorElements();
      this.totemHud = new TotemElements();
      this.turtleHud = new TurtleElements();
      this.inGameHud = new InGameHudElements();
      this.playerInfo = new PlayerInfoElements();
      this.radar = new RadarElements();
      this.potionHud = new PotionElements();
      this.waterMark = new WaterMarkElements();
      this.playerDoll = new PlayerDollElements();
      this.inventoryViewer = new InventoryViewerElements();
      this.notificationHud = new NotificationElements();
      this.noirLive2D = new NoirLive2DElements();
   }

   public Color getColor() {
      return ClickGui.getInstance().getColor();
   }

   public void onUpdate(int mouseX, int mouseY) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (this.isClickGuiOpen() && HudEditor.INSTANCE.isOn()) {
         this.armorHud.update(mouseX, mouseY);
         this.totemHud.update(mouseX, mouseY);
         this.turtleHud.update(mouseX, mouseY);
         this.inGameHud.update(mouseX, mouseY);
         this.playerInfo.update(mouseX, mouseY);
         this.radar.update(mouseX, mouseY);
         this.potionHud.update(mouseX, mouseY);
         this.waterMark.update(mouseX, mouseY);
         this.playerDoll.update(mouseX, mouseY);
         this.inventoryViewer.update(mouseX, mouseY);
         this.notificationHud.update(mouseX, mouseY);
         this.noirLive2D.update(mouseX, mouseY);
      }
   }

   public void draw(int x, int y, DrawContext drawContext, float tickDelta) {
      if (!HudEditor.INSTANCE.isOn()) {
         currentGrabbed = null;
         return;
      }

      MatrixStack matrixStack = drawContext.getMatrices();
      boolean mouseClicked = ClickGuiScreen.clicked;
      this.mouseX = x;
      this.mouseY = y;
      if (!mouseClicked) {
         currentGrabbed = null;
      }

      if (currentGrabbed != null) {
         currentGrabbed.moveWindow(this.lastMouseX - this.mouseX, this.lastMouseY - this.mouseY);
      }

      this.lastMouseX = this.mouseX;
      this.lastMouseY = this.mouseY;
      RenderSystem.enableCull();
      matrixStack.push();
      this.armorHud.draw(drawContext, tickDelta, this.getColor());
      this.totemHud.draw(drawContext, tickDelta, this.getColor());
      this.turtleHud.draw(drawContext, tickDelta, this.getColor());
      this.inGameHud.draw(drawContext, tickDelta, this.getColor());
      this.playerInfo.draw(drawContext, tickDelta, this.getColor());
      this.radar.draw(drawContext, tickDelta, this.getColor());
      this.potionHud.draw(drawContext, tickDelta, this.getColor());
      this.waterMark.draw(drawContext, tickDelta, this.getColor());
      this.playerDoll.draw(drawContext, tickDelta, this.getColor());
      this.inventoryViewer.draw(drawContext, tickDelta, this.getColor());
      this.notificationHud.draw(drawContext, tickDelta, this.getColor());
      this.noirLive2D.draw(drawContext, tickDelta, this.getColor());
      matrixStack.pop();
   }

   public boolean isClickGuiOpen() {
      return mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof TClickGuiScreen;
   }

   public void onGlobalClick(double mouseX, double mouseY, int button) {
      if (HudEditor.INSTANCE == null || !HudEditor.INSTANCE.isOn() || this.isClickGuiOpen()) {
         return;
      }

      this.noirLive2D.mouseClicked(mouseX, mouseY, button);
   }
}
