package dev.luminous.mod.gui.tclick;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.tclick.core.TAnim;
import dev.luminous.mod.gui.tclick.core.TCfg;
import dev.luminous.mod.gui.tclick.core.TColor;
import dev.luminous.mod.gui.tclick.core.TDraw;
import dev.luminous.mod.gui.tclick.core.TEaseOutBack;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.gui.tclick.core.TImageMode;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ItemsCounter;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.impl.hud.NotificationHud;
import dev.luminous.mod.modules.impl.render.Keystrokes;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TClickGuiScreen extends Screen {
   public static List<TAbstractCategory> windows;
   public static boolean anyHovered;
   private boolean firstOpen;
   private float scrollY;
   private float closeAnimation;
   private float prevYaw;
   private float prevPitch;
   private float closeDirectionX;
   private float closeDirectionY;
   public static boolean close = false;
   public static String currentDescription = "";
   public static TResetDialog activeDialog;
   private static TClickGuiScreen INSTANCE = new TClickGuiScreen();
   private final Animation openAnimation = new Animation();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public TEaseOutBack imageAnimation = new TEaseOutBack(6);
   public static boolean imageDirection = true;

   public TClickGuiScreen() {
      super(Text.of("NewClickGUI"));
      windows = new ArrayList<>();
      this.firstOpen = true;
      this.setInstance();
   }

   public static TClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new TClickGuiScreen();
      }

      imageDirection = true;
      return INSTANCE;
   }

   public static void replayImageAnimation() {
      imageDirection = true;
      if (INSTANCE != null) {
         INSTANCE.imageAnimation.reset();
      }
   }

   public static TClickGuiScreen getClickGui() {
      windows.forEach(TAbstractCategory::init);
      return getInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void clearCloseState() {
      close = false;
      this.closeAnimation = 7.0F;
      this.closeDirectionX = 0.0F;
      this.closeDirectionY = 0.0F;
      activeDialog = null;
   }

   public void close() {
      windows.forEach(TAbstractCategory::onClose);
      this.clearCloseState();
      TDraw.globalAlpha = 1.0F;
      super.close();
   }

   private static float centeredStartX(int var0, int var1) {
      float var2 = TCfg.moduleWidth + 2;
      float var3 = var2 * var1 - 2.0F;
      return Math.max(2.0F, (var0 - var3) / 2.0F);
   }

   private static boolean isHudEditorOnly(Module var0) {
      return var0 instanceof NotificationHud || var0 instanceof ItemsCounter || var0 instanceof Keystrokes || var0 instanceof TextRadar;
   }

   private boolean stripOffScreen() {
      if (windows.isEmpty()) {
         return true;
      } else {
         int var1 = mc.getWindow().getScaledWidth();
         int var2 = mc.getWindow().getScaledHeight();
         float var3 = TCfg.moduleWidth + 2;
         float var4 = windows.getFirst().getX();
         float var5 = var4 + var3 * windows.size();
         float var6 = windows.getFirst().getY();
         return var4 < 0.0F || var5 > var1 || var6 < 0.0F || var6 > var2 - 40.0F;
      }
   }

   protected void init() {
      TFonts.init();
      this.clearCloseState();
      this.openAnimation.from = 0.0;
      this.openAnimation.to = 0.0;
      if (this.firstOpen) {
         byte var1 = 18;
         int var2 = mc.getWindow().getScaledWidth();
         float var3 = TCfg.moduleWidth + 2;
         float var4 = centeredStartX(var2, Module.Category.values().length);
         float var5 = 0.0F;

         for (Module.Category var9 : Module.Category.values()) {
            ArrayList var10 = new ArrayList();

            for (Module var12 : Alien.MODULE.getModules()) {
               if (var12.getCategory() == var9 && !isHudEditorOnly(var12)) {
                  var10.add(var12);
               }
            }

            TCategory var19 = new TCategory(var9, var10, var4 + var5, 20.0F, TCfg.moduleWidth, var1);
            var19.setOpen(true);
            windows.add(var19);
            var5 += var3;
         }

         this.firstOpen = false;
      } else if (this.stripOffScreen()) {
         int var13 = mc.getWindow().getScaledWidth();
         float var14 = TCfg.moduleWidth + 2;
         float var15 = centeredStartX(var13, windows.size());
         float var16 = 0.0F;

         for (TAbstractCategory var18 : windows) {
            var18.setX(var15 + var16);
            var18.setY(20.0F);
            var16 += var14;
         }
      }

      windows.forEach(TAbstractCategory::init);
   }

   public boolean shouldPause() {
      return false;
   }

   public void tick() {
      windows.forEach(TAbstractCategory::tick);
      this.imageAnimation.update(imageDirection);
      if (close) {
         if (mc.player != null) {
            if (mc.player.getPitch() > this.prevPitch) {
               this.closeDirectionY = (this.prevPitch - mc.player.getPitch()) * 300.0F;
            }

            if (mc.player.getPitch() < this.prevPitch) {
               this.closeDirectionY = (this.prevPitch - mc.player.getPitch()) * 300.0F;
            }

            if (mc.player.getYaw() > this.prevYaw) {
               this.closeDirectionX = (this.prevYaw - mc.player.getYaw()) * 300.0F;
            }

            if (mc.player.getYaw() < this.prevYaw) {
               this.closeDirectionX = (this.prevYaw - mc.player.getYaw()) * 300.0F;
            }
         }

         if (this.closeDirectionX < 1.0F && this.closeDirectionY < 1.0F && this.closeAnimation > 2.0F) {
            this.closeDirectionY = -3000.0F;
         }

         this.closeAnimation++;
         if (this.closeAnimation > 6.0F) {
            close = false;
            windows.forEach(TAbstractCategory::restorePos);
            this.close();
         }
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      TFonts.init();
      float var5 = 1.0F;

      try {
         ClickGui var6 = ClickGui.getInstance();
         if (var6 != null && var6.fade.getValue()) {
            var5 = (float)this.openAnimation.get(1.0, var6.length.getValueInt(), var6.easing.getValue());
         } else {
            this.openAnimation.from = 1.0;
            this.openAnimation.to = 1.0;
         }
      } catch (Throwable var10) {
      }

      if (var5 < 0.0F) {
         var5 = 0.0F;
      }

      if (var5 > 1.0F) {
         var5 = 1.0F;
      }

      TDraw.globalAlpha = var5;
      float var11 = 0.92F + 0.08F * var5;
      float var7 = (1.0F - var5) * 20.0F;
      TDraw.drawRect(context.getMatrices(), 0.0F, 0.0F, context.getScaledWindowWidth(), context.getScaledWindowHeight(), new Color(0, 0, 0, 60));
      if (TCfg.imageMode != TImageMode.None) {
         RenderSystem.setShaderTexture(0, TCfg.imageMode.file);
         TDraw.renderTexture(
            context.getMatrices(),
            context.getScaledWindowWidth() - TCfg.imageMode.fileWidth * this.imageAnimation.getAnimationd(),
            context.getScaledWindowHeight() - TCfg.imageMode.fileHeight,
            TCfg.imageMode.fileWidth,
            TCfg.imageMode.fileHeight,
            0.0F,
            0.0F,
            TCfg.imageMode.fileWidth,
            TCfg.imageMode.fileHeight,
            TCfg.imageMode.fileWidth,
            TCfg.imageMode.fileHeight
         );
      }

      anyHovered = false;
      if (this.closeAnimation <= 6.0F) {
         windows.forEach(var1 -> {
            var1.setX(var1.getX() + this.closeDirectionX * TAnim.deltaTime());
            var1.setY(var1.getY() + this.closeDirectionY * TAnim.deltaTime());
         });
      }

      if (mc.player == null) {
         this.renderBackground(context, mouseX, mouseY, delta);
      }

      if (TCfg.scrollMode == 1) {
         for (TAbstractCategory var9 : windows) {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 264)) {
               var9.setY(var9.getY() + 2.0F);
            }

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 265)) {
               var9.setY(var9.getY() - 2.0F);
            }

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 262)) {
               var9.setX(var9.getX() + 2.0F);
            }

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 263)) {
               var9.setX(var9.getX() - 2.0F);
            }

            if (this.scrollY != 0.0F) {
               var9.setY(var9.getY() + this.scrollY);
            }
         }
      } else {
         for (TAbstractCategory var13 : windows) {
            if (this.scrollY != 0.0F) {
               var13.setModuleOffset(this.scrollY, mouseX, mouseY);
            }
         }
      }

      this.scrollY = 0.0F;
      if (Alien.GUI != null) {
         Alien.GUI.onUpdate(mouseX, mouseY);
         Alien.GUI.draw(mouseX, mouseY, context, delta);
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, var7, 0.0F);
      context.getMatrices().scale(var11, var11, 1.0F);
      windows.forEach(var4 -> var4.render(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
      if (!Objects.equals(currentDescription, "") && TCfg.descriptions) {
         TDraw.drawHudBase(context.getMatrices(), mouseX + 7, mouseY + 5, TFonts.sf_medium.getWidth(currentDescription) + 6.0F, 11.0F, 1.0F, 1.0F, false);
         TFonts.sf_medium.drawString(context.getMatrices(), currentDescription, (float)(mouseX + 10), (float)(mouseY + 8), TColor.getColor(0).getRGB());
         currentDescription = "";
      }

      if (TCfg.tips && !close) {
         TFonts.sf_medium
            .drawString(
               context.getMatrices(),
               "Left Mouse Click to enable module\nRight Mouse Click to open module settings\nMiddle Mouse Click to bind module\nCtrl + F to start searching\nDrag n Drop config there to load\nShift + Left Mouse Click to change module visibility in Array list\nMiddle Mouse Click on slider to enter value from keyboard\nDelete + Left Mouse Click on module to reset",
               5.0,
               (double)(mc.getWindow().getScaledHeight() - 80),
               TColor.getColor(0).getRGB()
            );
      }

      if (!anyHovered && GLFW.glfwGetPlatform() != 393219) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
      }

      if (activeDialog != null) {
         activeDialog.render(context, mouseX, mouseY, delta);
      }

      TDraw.globalAlpha = 1.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int var9 = (int)(verticalAmount * 5.0);
      long var10 = mc.getWindow().getHandle();
      boolean var12 = InputUtil.isKeyPressed(var10, 340) || InputUtil.isKeyPressed(var10, 344);
      if (var12) {
         float var13 = var9;

         for (TAbstractCategory var15 : windows) {
            var15.setX(var15.getX() + var13);
         }
      } else {
         this.scrollY += var9;
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         ClickGuiScreen.clicked = true;
      }

      if (activeDialog != null) {
         activeDialog.mouseClicked((int)mouseX, (int)mouseY, button);
         return true;
      } else {
         windows.forEach(TAbstractCategory::onClose);
         windows.forEach(var5 -> {
            var5.mouseClicked((int)mouseX, (int)mouseY, button);
            windows.forEach(var1 -> {
               if (var5.dragging && var5 != var1) {
                  var1.dragging = false;
               }
            });
         });
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         ClickGuiScreen.clicked = false;
      }

      if (activeDialog != null) {
         return true;
      } else {
         windows.forEach(var5 -> var5.mouseReleased((int)mouseX, (int)mouseY, button));
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (activeDialog != null) {
         return true;
      } else {
         windows.forEach(var2 -> var2.charTyped(chr, modifiers));
         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (activeDialog != null) {
         activeDialog.keyTyped(keyCode);
         return true;
      } else {
         windows.forEach(var1 -> var1.keyTyped(keyCode));
         if (keyCode == 256) {
            if (mc.player == null || !TCfg.closeAnimation) {
               super.keyPressed(keyCode, scanCode, modifiers);
               return true;
            } else if (close) {
               return true;
            } else {
               windows.forEach(TAbstractCategory::savePos);
               imageDirection = false;
               this.imageAnimation.reset();
               this.closeDirectionX = 0.0F;
               this.closeDirectionY = 0.0F;
               close = true;
               mc.mouse.lockCursor();
               this.closeAnimation = 0.0F;
               if (mc.player != null) {
                  this.prevYaw = mc.player.getYaw();
                  this.prevPitch = mc.player.getPitch();
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }
}
