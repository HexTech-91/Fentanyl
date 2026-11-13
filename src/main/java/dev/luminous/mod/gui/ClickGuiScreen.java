package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ClickGuiScreen extends Screen {
   public static boolean clicked = false;
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList<>();
   private TextFieldWidget activeTextField;
   private float mouseMoveOffsetX;
   private float mouseMoveOffsetY;
   private float walkShakeOffsetX;
   private float walkShakeOffsetY;
   private long lastRenderTime = 0L;
   private static final long FRAME_INTERVAL = 8L;
   private float walkShakeTime;
   private boolean layoutCorrected = false;
   private final Random snowRandom = new Random();
   private final ArrayList<ClickGuiScreen.Snowflake> snowflakes = new ArrayList<>();
   private boolean needsRebuild = false;
   private static final int TOP_BUTTON_Y = 10;
   private static final int TOP_BUTTON_HEIGHT = 20;
   private static final int TOP_BUTTON_GAP = 6;
   private static final int MODE_BUTTON_WIDTH = 74;

   public ClickGuiScreen() {
      super(Text.literal("Frog"));
      this.setInstance();
      this.load();
   }

   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   public static void setActiveTextField(TextFieldWidget var0) {
      if (INSTANCE != null) {
         if (INSTANCE.activeTextField != null) {
            INSTANCE.remove(INSTANCE.activeTextField);
         }

         INSTANCE.activeTextField = var0;
         if (var0 != null) {
            INSTANCE.addDrawableChild(var0);
            INSTANCE.setFocused(var0);
         }
      }
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void load() {
      int var1 = ClickGui.getInstance() != null ? ClickGui.getInstance().categoryWidth.getValueInt() : 101;
      int var2 = ClickGui.getInstance() != null ? ClickGui.getInstance().moduleButtonWidth.getValueInt() : 93;
      int var3 = Math.max(var1, var2);
      int var4 = var3 + 1;
      int var5 = Module.Category.values().length;
      int var6 = 10;
      int var7 = 4;
      if (Wrapper.mc != null && Wrapper.mc.getWindow() != null) {
         int var8 = Wrapper.mc.getWindow().getScaledWidth();
         int var9 = Wrapper.mc.getWindow().getScaledHeight();
         int var10 = var5 * var3;
         var6 = Math.round(((float)var8 - var10) / 2.0F);
         var7 = Math.round(var9 / 6.0F);
         this.layoutCorrected = true;
      }

      int var14 = Math.round(((float)var3 - var2) / 2.0F);
      int var15 = var6 - var4;

      for (final Module.Category var13 : Module.Category.values()) {
         var15 += var4;
         this.components.add(new Component(var13.toString(), var13, var15 + var14, var7, true) {
            @Override
            public void setupItems() {
               for (Module var2x : Alien.MODULE.getModules()) {
                  if (var2x.getCategory().equals(var13)) {
                     this.addButton(new ModuleButton(var2x));
                  }
               }
            }
         });
      }

      this.components.forEach(var0 -> var0.getItems().sort(Comparator.comparing(Mod::getName)));
      this.loadPositions();
   }

   public static void rebuild() {
      if (INSTANCE != null) {
         INSTANCE.needsRebuild = true;
      }
   }

   public void renderBackground(DrawContext var1, int var2, int var3, float var4) {
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.needsRebuild) {
         this.needsRebuild = false;
         this.components.clear();
         this.load();
         this.loadPositions();
      }

      long var5 = System.currentTimeMillis();
      this.lastRenderTime = var5;
      float var7 = (float)ClickGui.getInstance().alphaValue;
      float var8 = 0.92F + 0.08F * var7;
      float var9 = (1.0F - var7) * 20.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var7);
      Item.context = context;
      this.renderBackground(context, mouseX, mouseY, delta);
      ClickGui var10 = ClickGui.getInstance();
      if (var10 != null && var10.colorMode.getValue() == ClickGui.ColorMode.Spectrum && var7 > 0.0F) {
         var10.updateSpectrumLut(context.getScaledWindowHeight());
      }

      if (!this.layoutCorrected && Wrapper.mc != null && Wrapper.mc.getWindow() != null && var10 != null) {
         int var11 = var10.categoryWidth.getValueInt();
         int var12 = var10.moduleButtonWidth.getValueInt();
         int var13 = Math.max(var11, var12);
         int var14 = var13 + 1;
         int var15 = this.components.size();
         if (var15 > 0) {
            int var16 = Wrapper.mc.getWindow().getScaledWidth();
            int var17 = Wrapper.mc.getWindow().getScaledHeight();
            int var18 = var15 * var13;
            int var19 = Math.round(((float)var16 - var18) / 2.0F);
            int var20 = Math.round(var17 / 6.0F);
            int var21 = Math.round(((float)var13 - var12) / 2.0F);
            int var100 = var19 - var14;

            for (Component var24 : this.components) {
               var100 += var14;
               var24.setX(var100 + var21);
               var24.setY(var20);
               var100--;
            }
         }

         this.layoutCorrected = true;
      }

      boolean var71 = false;

      for (Component var74 : this.components) {
         if (var74.drag) {
            var71 = true;
            break;
         }
      }

      float var73 = 0.0F;
      float var75 = 0.0F;
      if (var10 != null && var10.mouseMove.getValue() && !var71) {
         float var76 = var10.mouseMoveStrength.getValueFloat() * var7;
         float var78 = context.getScaledWindowWidth() / 2.0F;
         float var82 = context.getScaledWindowHeight() / 2.0F;
         float var85 = var78 <= 0.0F ? 0.0F : (mouseX - var78) / var78;
         float var88 = var82 <= 0.0F ? 0.0F : (mouseY - var82) / var82;
         var85 = Math.max(-1.0F, Math.min(1.0F, var85));
         var88 = Math.max(-1.0F, Math.min(1.0F, var88));
         var73 = var85 * var76;
         var75 = var88 * var76;
      }

      float var77 = var10 != null ? var10.mouseMoveSmooth.getValueFloat() : 0.0F;
      if (var77 <= 0.0F) {
         this.mouseMoveOffsetX = var73;
         this.mouseMoveOffsetY = var75;
      } else {
         float var79 = AnimateUtil.deltaTime() * var77;
         if (var79 < 0.0F) {
            var79 = 0.0F;
         }

         if (var79 > 0.35F) {
            var79 = 0.35F;
         }

         this.mouseMoveOffsetX = this.mouseMoveOffsetX + (var73 - this.mouseMoveOffsetX) * var79;
         this.mouseMoveOffsetY = this.mouseMoveOffsetY + (var75 - this.mouseMoveOffsetY) * var79;
      }

      float var80 = 0.0F;
      float var83 = 0.0F;
      float var87 = 0.0F;
      if (var10 != null && var10.walkShake.getValue() && !var71 && Wrapper.mc.player != null) {
         Vec3d var90 = Wrapper.mc.player.getVelocity();
         float var92 = (float)Math.sqrt(var90.x * var90.x + var90.z * var90.z);
         float var95 = var92 > 0.003F ? Math.min(1.0F, var92 * 18.0F) : 0.0F;
         if (var95 > 0.0F) {
            float var97 = AnimateUtil.deltaTime();
            float var101 = var10.walkShakeSpeed.getValueFloat();
            this.walkShakeTime += var97 * var101 * (0.4F + 0.6F * var95);
         }

         float var98 = var10.walkShakeStrength.getValueFloat() * var95 * var7;
         var87 = var10.walkShakeMax.getValueFloat() * var7;
         var80 = (float)Math.sin(this.walkShakeTime) * var98;
         var83 = (float)Math.cos(this.walkShakeTime * 2.0F) * var98 * 0.35F;
         var80 = Math.max(-var87, Math.min(var87, var80));
         var83 = Math.max(-var87, Math.min(var87, var83));
      }

      float var91 = var10 != null ? var10.walkShakeSmooth.getValueFloat() : 0.0F;
      if (var91 <= 0.0F) {
         this.walkShakeOffsetX = var80;
         this.walkShakeOffsetY = var83;
      } else {
         float var93 = AnimateUtil.deltaTime() * var91;
         if (var93 < 0.0F) {
            var93 = 0.0F;
         }

         if (var93 > 0.35F) {
            var93 = 0.35F;
         }

         this.walkShakeOffsetX = this.walkShakeOffsetX + (var80 - this.walkShakeOffsetX) * var93;
         this.walkShakeOffsetY = this.walkShakeOffsetY + (var83 - this.walkShakeOffsetY) * var93;
      }

      if (var87 > 0.0F) {
         this.walkShakeOffsetX = Math.max(-var87, Math.min(var87, this.walkShakeOffsetX));
         this.walkShakeOffsetY = Math.max(-var87, Math.min(var87, this.walkShakeOffsetY));
      }

      float var94 = this.mouseMoveOffsetX + this.walkShakeOffsetX;
      float var96 = this.mouseMoveOffsetY + this.walkShakeOffsetY;
      this.components.forEach(var2 -> var2.setMouseMoveOffset(var94, var96));
      int var99 = Integer.MAX_VALUE;
      int var102 = Integer.MAX_VALUE;
      int var103 = Integer.MIN_VALUE;
      int var104 = Integer.MIN_VALUE;

      for (Component var26 : this.components) {
         var99 = Math.min(var99, var26.getX());
         var102 = Math.min(var102, var26.getY());
         var103 = Math.max(var103, var26.getX() + var26.getWidth());
         var104 = Math.max(var104, var26.getY() + var26.getHeight());
      }

      byte var105 = 16;
      int var106 = Math.max(8, var99 - var105);
      int var27 = Math.max(6, var102 - var105);
      int var28 = Math.min(context.getScaledWindowWidth() - var106 - 8, var103 - var99 + var105 * 2);
      int var29 = Math.min(context.getScaledWindowHeight() - var27 - 6, var104 - var102 + var105 * 2 + 24);
      boolean var30 = mouseX >= var106 && mouseX <= var106 + var28 && mouseY >= var27 && mouseY <= var27 + var29;
      int var31 = var30 ? 242 : 227;
      this.renderSnow(context);
      if (Alien.GUI != null) {
         Alien.GUI.onUpdate(mouseX, mouseY);
         Alien.GUI.draw(mouseX, mouseY, context, delta);
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, var9, 0.0F);
      context.getMatrices().scale(var8, var8, 1.0F);
      this.components.forEach(var4 -> var4.drawScreen(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
      if (var10 != null) {
      }

      if (var10 != null && var10.tips.getValue()) {
         boolean var32 = FontManager.isCustomFontEnabled();
         boolean var33 = FontManager.isShadowEnabled();
         float var34 = var32 ? FontManager.ui.getFontHeight() : TextUtil.getHeight();
         float var35 = 6.0F;
         byte var36 = 5;
         float var37 = context.getScaledWindowHeight() - var35 - var34 * var36;
         byte var38 = 6;
         int var39 = Math.round(var37);
         boolean var40 = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
         String var41 = var40 ? "\u5de6\u952e\u62d6\u52a8 \u53f3\u952e\u5c55\u5f00/\u6298\u53e0" : "LMB drag, RMB expand/collapse";
         String var42 = var40
            ? "\u6eda\u8f6e\u662f\u4e0a\u4e0b\u79fb\u52a8 SHIFT+\u6eda\u8f6e\u662f\u5de6\u53f3\u79fb\u52a8"
            : "Scroll up/down, SHIFT+scroll left/right";
         String var43 = var40
            ? "SHIFT+\u5de6\u952e \u5feb\u6377\u952e\u6309 \u5207\u6362\u529f\u80fd(\u6309\u4f4f/\u677e\u5f00)\u89e6\u53d1"
            : "SHIFT+LMB: toggle hold/release";
         String var44 = var40 ? "SHIFT+\u5de6\u952e \u529f\u80fd\u6309\u94ae \u91cd\u7f6e\u8bbe\u7f6e" : "SHIFT+LMB: reset this setting";
         String var45 = var40 ? "\u6587\u672c\u8bbe\u7f6e \u53f3\u952e\u7f16\u8f91" : "RMB on String setting: edit";
         boolean var46 = var10.colorMode.getValue() == ClickGui.ColorMode.Spectrum;
         double var47 = var46 ? var39 * 0.25 : var39 / 10.0;
         Color var49 = var10.getActiveColor(var47);
         int var50 = ColorUtil.injectAlpha(var49, var31).getRGB();
         TextUtil.drawString(context, var41, var38, var39, var50, var32, var33);
         int var51 = (int)(var39 + var34);
         double var52 = var46 ? var51 * 0.25 : var51 / 10.0;
         Color var54 = var10.getActiveColor(var52);
         int var55 = ColorUtil.injectAlpha(var54, var31).getRGB();
         TextUtil.drawString(context, var42, var38, var51, var55, var32, var33);
         int var56 = (int)(var39 + var34 * 2.0F);
         double var57 = var46 ? var56 * 0.25 : var56 / 10.0;
         Color var59 = var10.getActiveColor(var57);
         int var60 = ColorUtil.injectAlpha(var59, var31).getRGB();
         TextUtil.drawString(context, var43, var38, var56, var60, var32, var33);
         int var61 = (int)(var39 + var34 * 3.0F);
         double var62 = var46 ? var61 * 0.25 : var61 / 10.0;
         Color var64 = var10.getActiveColor(var62);
         int var65 = ColorUtil.injectAlpha(var64, var31).getRGB();
         TextUtil.drawString(context, var44, var38, var61, var65, var32, var33);
         int var66 = (int)(var39 + var34 * 4.0F);
         double var67 = var46 ? var66 * 0.25 : var66 / 10.0;
         Color var69 = var10.getActiveColor(var67);
         int var70 = ColorUtil.injectAlpha(var69, var31).getRGB();
         TextUtil.drawString(context, var45, var38, var66, var70, var32, var33);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         clicked = true;
      }

      this.components.forEach(var5 -> var5.mouseClicked((int)mouseX, (int)mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         clicked = false;
      }

      this.components.forEach(var5 -> var5.mouseReleased((int)mouseX, (int)mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
         if (verticalAmount < 0.0) {
            this.components.forEach(var0 -> var0.setX(var0.getTargetX() - 15));
         } else if (verticalAmount > 0.0) {
            this.components.forEach(var0 -> var0.setX(var0.getTargetX() + 15));
         }
      } else if (verticalAmount < 0.0) {
         this.components.forEach(var0 -> var0.setY(var0.getTargetY() - 15));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(var0 -> var0.setY(var0.getTargetY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(var1 -> var1.onKeyPressed(keyCode));
      return this.activeTextField != null ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(var2 -> var2.onKeyTyped(chr, modifiers));
      return this.activeTextField != null ? true : super.charTyped(chr, modifiers);
   }

   public boolean shouldPause() {
      return false;
   }

   public void removed() {
      clicked = false;
      this.savePositions();
      super.removed();
   }

   private static File getPositionsFile() {
      return new File(Wrapper.mc.runDirectory, "Alien" + File.separator + "clickgui_positions.txt");
   }

   private void savePositions() {
      try {
         File var1 = getPositionsFile();
         var1.getParentFile().mkdirs();
         StringBuilder var2 = new StringBuilder();

         for (Component var4 : this.components) {
            var2.append(var4.getName())
               .append(":")
               .append(var4.getTargetX())
               .append(":")
               .append(var4.getTargetY())
               .append(":")
               .append(var4.isOpen())
               .append("\n");
         }

         Files.write(var1.toPath(), var2.toString().getBytes(StandardCharsets.UTF_8));
      } catch (Exception var5) {
         System.out.println("[Alien] Failed to save clickgui positions: " + var5.getMessage());
      }
   }

   private void loadPositions() {
      try {
         File var1 = getPositionsFile();
         if (!var1.exists()) {
            return;
         }

         for (String var3 : Files.readAllLines(var1.toPath(), StandardCharsets.UTF_8)) {
            if (!var3.isEmpty()) {
               String[] var4 = var3.split(":");
               if (var4.length >= 4) {
                  String var5 = var4[0];
                  int var6 = Integer.parseInt(var4[1]);
                  int var7 = Integer.parseInt(var4[2]);
                  boolean var8 = Boolean.parseBoolean(var4[3]);

                  for (Component var10 : this.components) {
                     if (var10.getName().equals(var5)) {
                        var10.setInitialPosition(var6, var7);
                        var10.setOpenInitial(var8);
                        break;
                     }
                  }
               }
            }
         }
      } catch (Exception var11) {
         System.out.println("[Alien] Failed to load clickgui positions: " + var11.getMessage());
      }
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }

   private void renderSnow(DrawContext var1) {
      ClickGui var2 = ClickGui.getInstance();
      if (var2 != null && var2.snow.getValue()) {
         float var3 = (float)var2.alphaValue;
         if (!(var3 <= 0.01F)) {
            int var4 = var1.getScaledWindowWidth();
            int var5 = var1.getScaledWindowHeight();
            int var6 = Math.max(0, var2.snowAmount.getValueInt());

            while (this.snowflakes.size() < var6) {
               this.snowflakes.add(this.spawnSnowflake(var4, var5, true));
            }

            while (this.snowflakes.size() > var6) {
               this.snowflakes.remove(this.snowflakes.size() - 1);
            }

            float var7 = AnimateUtil.deltaTime();
            if (var7 <= 0.0F) {
               var7 = 0.016F;
            }

            float var8 = var2.snowSpeed.getValueFloat();
            float var9 = var2.snowSize.getValueFloat();
            float var10 = var2.snowWind.getValueFloat();
            int var11 = (int)Math.round((double)var2.snowAlpha.getValueInt() * var3);
            var11 = Math.max(0, Math.min(255, var11));
            Color var12 = new Color(255, 255, 255, var11);
            int var13 = var12.getRGB();
            boolean var14 = var2.snowShape.getValue() == ClickGui.SnowShape.Circle;
            int var15 = this.snowflakes.size();
            float[] var16 = var14 ? new float[var15 * 3] : null;
            int[] var17 = var14 ? new int[var15] : null;
            float[] var18 = var14 ? null : new float[var15 * 2 * 4 * 4];
            int[] var19 = var14 ? null : new int[var15 * 2 * 4];
            int var20 = 0;
            int var21 = 0;

            for (int var22 = 0; var22 < var15; var22++) {
               ClickGuiScreen.Snowflake var23 = this.snowflakes.get(var22);
               var23.y = var23.y + var8 * var23.speedMul * var7;
               var23.x = var23.x + var10 * var7 + (float)Math.sin(var23.y * 0.02F + var23.phase) * var23.drift * var7;
               float var24 = Math.max(0.3F, var9 * var23.sizeMul);
               if (!(var23.y > var5 + var24 + 2.0F) && !(var23.x < -10.0F) && !(var23.x > var4 + 10.0F)) {
                  if (var14) {
                     var16[var20 * 3] = var23.x;
                     var16[var20 * 3 + 1] = var23.y;
                     var16[var20 * 3 + 2] = var24;
                     var17[var20] = var13;
                     var20++;
                  } else {
                     float var25 = var24 * 0.45F;
                     float var26 = 0.65F;
                     float var27 = var23.phase + var23.y * 0.01F;

                     for (int var28 = 0; var28 < 2; var28++) {
                        double var29 = var27 + var28 * Math.PI;
                        float var31 = (float)Math.cos(var29) * var24;
                        float var32 = (float)Math.sin(var29) * var24;
                        var18[var21 * 4] = var23.x - var31;
                        var18[var21 * 4 + 1] = var23.y - var32;
                        var18[var21 * 4 + 2] = var23.x + var31;
                        var18[var21 * 4 + 3] = var23.y + var32;
                        var19[var21] = var13;
                        var21++;
                        float var33 = var23.x + var31 * var26;
                        float var34 = var23.y + var32 * var26;
                        var18[var21 * 4] = var33;
                        var18[var21 * 4 + 1] = var34;
                        var18[var21 * 4 + 2] = var33 + (float)Math.cos(var29 + (Math.PI / 6)) * var25;
                        var18[var21 * 4 + 3] = var34 + (float)Math.sin(var29 + (Math.PI / 6)) * var25;
                        var19[var21] = var13;
                        var18[++var21 * 4] = var33;
                        var18[var21 * 4 + 1] = var34;
                        var18[var21 * 4 + 2] = var33 + (float)Math.cos(var29 - (Math.PI / 6)) * var25;
                        var18[var21 * 4 + 3] = var34 + (float)Math.sin(var29 - (Math.PI / 6)) * var25;
                        var19[var21] = var13;
                        var21++;
                     }
                  }
               } else {
                  this.snowflakes.set(var22, this.spawnSnowflake(var4, var5, false));
               }
            }

            if (var14 && var20 > 0) {
               Render2DUtil.drawCirclesBatch(var1.getMatrices(), var16, var17, 16);
            } else if (!var14 && var21 > 0) {
               Render2DUtil.drawLinesBatch(var1.getMatrices(), var18, var19);
            }
         }
      } else if (!this.snowflakes.isEmpty()) {
         this.snowflakes.clear();
      }
   }

   private void drawSnowflake(MatrixStack var1, float var2, float var3, float var4, int var5, float var6) {
      float var7 = var4 * 0.45F;
      float var8 = 0.65F;

      for (int var9 = 0; var9 < 2; var9++) {
         double var10 = var6 + var9 * Math.PI;
         float var12 = (float)Math.cos(var10) * var4;
         float var13 = (float)Math.sin(var10) * var4;
         Render2DUtil.drawLine(var1, var2 - var12, var3 - var13, var2 + var12, var3 + var13, var5);
         float var14 = var2 + var12 * var8;
         float var15 = var3 + var13 * var8;
         Render2DUtil.drawLine(
            var1, var14, var15, var14 + (float)Math.cos(var10 + (Math.PI / 6)) * var7, var15 + (float)Math.sin(var10 + (Math.PI / 6)) * var7, var5
         );
         Render2DUtil.drawLine(
            var1, var14, var15, var14 + (float)Math.cos(var10 - (Math.PI / 6)) * var7, var15 + (float)Math.sin(var10 - (Math.PI / 6)) * var7, var5
         );
      }
   }

   private ClickGuiScreen.Snowflake spawnSnowflake(int var1, int var2, boolean var3) {
      float var4 = this.snowRandom.nextInt(Math.max(1, var1));
      float var5 = var3 ? this.snowRandom.nextInt(Math.max(1, var2)) : -5.0F - this.snowRandom.nextFloat() * 20.0F;
      float var6 = this.snowRandom.nextFloat() * (float) (Math.PI * 2);
      float var7 = 10.0F + this.snowRandom.nextFloat() * 40.0F;
      float var8 = 0.55F + this.snowRandom.nextFloat() * 1.05F;
      float var9 = 0.6F + this.snowRandom.nextFloat() * 1.2F;
      return new ClickGuiScreen.Snowflake(var4, var5, var6, var7, var8, var9);
   }

   private static final class Snowflake {
      private float x;
      private float y;
      private final float phase;
      private final float drift;
      private final float speedMul;
      private final float sizeMul;

      private Snowflake(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.x = var1;
         this.y = var2;
         this.phase = var3;
         this.drift = var4;
         this.speedMul = var5;
         this.sizeMul = var6;
      }
   }
}
