package dev.luminous.mod.gui.windows;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class WindowsScreen extends Screen {
   private static WindowsScreen INSTANCE;
   private final ArrayList<WindowBase> windows = new ArrayList<>();
   public int x;
   public int y;
   public int width = 200;
   public int height = 200;
   public boolean drag;

   public WindowsScreen() {
      super(Text.literal("Windows"));
      INSTANCE = this;
      this.windows.add(new ItemSelectWindow());
   }

   public WindowsScreen(WindowBase window) {
      super(Text.literal("Windows"));
      INSTANCE = this;
      this.windows.add(window);
   }

   public static WindowsScreen getInstance() {
      return INSTANCE;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + this.width, this.y + this.height, new Color(0, 0, 0, 200).getRGB());
      this.windows.forEach(window -> window.drawScreen(context, this.x, this.y, this.width, this.height, mouseX, mouseY, delta));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.windows.forEach(window -> window.mouseClicked((int)mouseX, (int)mouseY, this.x, this.y, this.width, this.height, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.windows.forEach(window -> window.mouseReleased((int)mouseX, (int)mouseY, this.x, this.y, this.width, this.height, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.windows.forEach(window -> window.mouseScrolled((int)mouseX, (int)mouseY, this.x, this.y, this.width, this.height, verticalAmount));
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.windows.forEach(window -> window.keyPressed(keyCode, this.x, this.y, this.width, this.height));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      this.windows.forEach(window -> window.charTyped(chr, this.x, this.y, this.width, this.height));
      return super.charTyped(chr, modifiers);
   }
}
