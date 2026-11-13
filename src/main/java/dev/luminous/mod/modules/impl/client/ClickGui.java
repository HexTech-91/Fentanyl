package dev.luminous.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.ResizeEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.gui.tclick.TClickGuiScreen;
import dev.luminous.mod.gui.tclick.core.TFonts;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   private boolean guiWasOpen = false;
   public final EnumSetting<ClickGui.Style> style = this.add(new EnumSetting<>("Style", ClickGui.Style.Static).injectTask(this::updateStyle));
   public final EnumSetting<ClickGui.BackgroundStyle> backgroundStyle = this.add(
      new EnumSetting<>("BackgroundStyle", ClickGui.BackgroundStyle.Dark).injectTask(this::updateBackgroundStyle)
   );
   public final BooleanSetting disableNotification = this.add(new BooleanSetting("DisableNotification", false));
   public final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
   public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1.6, 0.0, 2.0, 0.1, this.sound::isOpen));
   public final BooleanSetting hoverSound = this.add(new BooleanSetting("HoverSound", true, this.sound::isOpen));
   public final SliderSetting hoverSoundPitch = this.add(new SliderSetting("HoverSoundPitch", 1.0, 0.5, 2.0, 0.1, this.hoverSound::isOpen));
   public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
   public final SliderSetting moduleButtonHeight = this.add(new SliderSetting("ModuleButtonHeight", 13, 10, 25).injectTask(this::applyHeights));
   public final SliderSetting moduleButtonWidth = this.add(new SliderSetting("ModuleButtonWidth", 93, 60, 160).injectTask(this::applyHeights));
   public final SliderSetting categoryWidth = this.add(new SliderSetting("CategoryWidth", 95, 60, 200).injectTask(this::applyHeights));
   public final SliderSetting categoryBarHeight = this.add(new SliderSetting("CategoryBarHeight", 17, 8, 30).injectTask(this::applyHeights));
   public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -5.0, 8.0, 1.0));
   public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset", 1.0, -5.0, 8.0, 1.0));
   public final SliderSetting alpha = this.add(new SliderSetting("Alpha", 150, 0, 255));
   public final SliderSetting hoverAlpha = this.add(new SliderSetting("HoverAlpha", 220, 0, 255));
   public final SliderSetting topAlpha = this.add(new SliderSetting("TopAlpha", 210, 0, 255));
   public final SliderSetting backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 236, 0, 255));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true).setParent());
   public final SliderSetting length = this.add(new SliderSetting("Length", 400, 0, 1000, this.fade::isOpen));
   public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.BackInOut, this.fade::isOpen));
   public final BooleanSetting scrollAnim = this.add(new BooleanSetting("ScrollAnim", true).setParent());
   public final SliderSetting scrollAnimLength = this.add(new SliderSetting("ScrollAnimLength", 220, 1, 1000, this.scrollAnim::isOpen));
   public final EnumSetting<Easing> scrollAnimEasing = this.add(new EnumSetting<>("ScrollAnimEasing", Easing.SineOut, this.scrollAnim::isOpen));
   public final BooleanSetting mouseMove = this.add(new BooleanSetting("MouseMove", true).setParent());
   public final SliderSetting mouseMoveStrength = this.add(new SliderSetting("MouseMoveStrength", 16.0, 0.0, 30.0, 0.5, this.mouseMove::isOpen));
   public final SliderSetting mouseMoveSmooth = this.add(new SliderSetting("MouseMoveSmooth", 10.0, 0.0, 30.0, 0.5, this.mouseMove::isOpen));
   public final BooleanSetting walkShake = this.add(new BooleanSetting("WalkShake", true).setParent());
   public final SliderSetting walkShakeStrength = this.add(new SliderSetting("WalkShakeStrength", 8.0, 0.0, 20.0, 0.5, this.walkShake::isOpen));
   public final SliderSetting walkShakeSpeed = this.add(new SliderSetting("WalkShakeSpeed", 12.0, 0.0, 30.0, 0.5, this.walkShake::isOpen));
   public final SliderSetting walkShakeSmooth = this.add(new SliderSetting("WalkShakeSmooth", 14.0, 0.0, 30.0, 0.5, this.walkShake::isOpen));
   public final SliderSetting walkShakeMax = this.add(new SliderSetting("WalkShakeMax", 8.0, 0.0, 30.0, 0.5, this.walkShake::isOpen));
   public final BooleanSetting snow = this.add(new BooleanSetting("Snow", true).setParent());
   public final EnumSetting<ClickGui.SnowShape> snowShape = this.add(new EnumSetting<>("SnowShape", ClickGui.SnowShape.Snowflake, this.snow::isOpen));
   public final SliderSetting snowAmount = this.add(new SliderSetting("SnowAmount", 60, 0, 600, this.snow::isOpen));
   public final SliderSetting snowSpeed = this.add(new SliderSetting("SnowSpeed", 38.0, 1.0, 160.0, 1.0, this.snow::isOpen));
   public final SliderSetting snowSize = this.add(new SliderSetting("SnowSize", 1.8, 0.5, 8.0, 0.1, this.snow::isOpen));
   public final SliderSetting snowAlpha = this.add(new SliderSetting("SnowAlpha", 160, 0, 255, this.snow::isOpen));
   public final SliderSetting snowWind = this.add(new SliderSetting("SnowWind", 10.0, -80.0, 80.0, 1.0, this.snow::isOpen));
   public final BooleanSetting tips = this.add(new BooleanSetting("Tips", true));
   public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false).setParent().injectTask(this::keyCodec));
   public final BooleanSetting line = this.add(new BooleanSetting("Line", true, this.elements::isOpen));
   public final ColorSetting gear = this.add(new ColorSetting("Gear", -1, this.elements::isOpen).injectBoolean(false));
   public final EnumSetting<ClickGui.ExpandIcon> expandIcon = this.add(new EnumSetting<>("ExpandIcon", ClickGui.ExpandIcon.PlusMinus, this.elements::isOpen));
   public final BooleanSetting recenterLayoutButton = this.add(
      new BooleanSetting("RecenterLayout", false, this.elements::isOpen).injectTask(this::recenterLayoutCodec)
   );
   public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent().injectTask(this::elementCodec));
   public final BooleanSetting modernColors = this.add(new BooleanSetting("ModernColors", false).setParent());
   public final ColorSetting modernAccentColor = this.add(
      new ColorSetting("ModernAccentColor", new Color(241, 155, 200, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernAccentMutedColor = this.add(
      new ColorSetting("ModernAccentMutedColor", new Color(109, 100, 116, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernPrimaryTextColor = this.add(
      new ColorSetting("ModernPrimaryTextColor", new Color(255, 255, 255, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernMutedTextColor = this.add(
      new ColorSetting("ModernMutedTextColor", new Color(183, 183, 192, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernOnAccentTextColor = this.add(
      new ColorSetting("ModernOnAccentTextColor", new Color(23, 23, 28, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernPanelColor = this.add(new ColorSetting("ModernPanelColor", new Color(20, 20, 28, 236).getRGB(), this.modernColors::isOpen));
   public final ColorSetting modernPanelLayerColor = this.add(
      new ColorSetting("ModernPanelLayerColor", new Color(28, 28, 36, 236).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernPanelDeepColor = this.add(
      new ColorSetting("ModernPanelDeepColor", new Color(18, 18, 24, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernControlColor = this.add(
      new ColorSetting("ModernControlColor", new Color(38, 38, 44, 216).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernControlMutedColor = this.add(
      new ColorSetting("ModernControlMutedColor", new Color(94, 94, 102, 255).getRGB(), this.modernColors::isOpen)
   );
   public final ColorSetting modernTrackColor = this.add(new ColorSetting("ModernTrackColor", new Color(78, 78, 86, 180).getRGB(), this.modernColors::isOpen));
   public final ColorSetting modernHoverColor = this.add(new ColorSetting("ModernHoverColor", new Color(255, 255, 255, 32).getRGB(), this.modernColors::isOpen));
   public final ColorSetting modernIdleFillColor = this.add(new ColorSetting("ModernIdleFillColor", new Color(0, 0, 0, 24).getRGB(), this.modernColors::isOpen));
   public final ColorSetting modernSeparatorColor = this.add(
      new ColorSetting("ModernSeparatorColor", new Color(255, 255, 255, 28).getRGB(), this.modernColors::isOpen)
   );
   public final SliderSetting modernBackdropAlpha = this.add(new SliderSetting("ModernBackdropAlpha", 100.0, 0.0, 255.0, 5.0, this.modernColors::isOpen));
   public final EnumSetting<ClickGui.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", ClickGui.ColorMode.Custom, this.colors::isOpen));
   public final SliderSetting rainbowSpeed = this.add(
      new SliderSetting(
         "RainbowSpeed",
         1.0,
         1.0,
         10.0,
         0.1,
         () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)
      )
   );
   public final SliderSetting saturation = this.add(
      new SliderSetting(
         "Saturation",
         220.0,
         1.0,
         255.0,
         () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)
      )
   );
   public final SliderSetting rainbowDelay = this.add(
      new SliderSetting(
         "Delay",
         50,
         0,
         1000,
         () -> this.colors.isOpen() && (this.colorMode.getValue() == ClickGui.ColorMode.Rainbow || this.colorMode.getValue() == ClickGui.ColorMode.Spectrum)
      )
   );
   public final ColorSetting color = this.add(
      new ColorSetting("FirstColor", new Color(0, 120, 212), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Custom)
   );
   public final ColorSetting secondColor = this.add(
      new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse)
         .injectBoolean(true)
   );
   public final SliderSetting pulseSpeed = this.add(
      new SliderSetting("PulseSpeed", 1.0, 0.0, 8.0, 0.1, () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse)
   );
   public final SliderSetting pulseCounter = this.add(
      new SliderSetting("Counter", 10, 1, 50, () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Pulse)
   );
   public final ColorSetting activeColor = this.add(
      new ColorSetting("ActiveColor", new Color(0, 120, 212), () -> this.colors.isOpen() && this.colorMode.getValue() == ClickGui.ColorMode.Custom)
   );
   public final ColorSetting hoverColor = this.add(new ColorSetting("HoverColor", new Color(50, 50, 50, 200), this.colors::isOpen));
   public final ColorSetting defaultColor = this.add(new ColorSetting("DefaultColor", new Color(30, 30, 30, 236), this.colors::isOpen));
   public final ColorSetting defaultTextColor = this.add(new ColorSetting("DefaultTextColor", new Color(220, 220, 220), this.colors::isOpen));
   public final ColorSetting enableTextColor = this.add(new ColorSetting("EnableTextColor", new Color(255, 255, 255), this.colors::isOpen));
   public final ColorSetting backGround = this.add(new ColorSetting("BackGround", new Color(30, 30, 30, 236), this.colors::isOpen).injectBoolean(true));
   public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(255, 0, 0, 80)).defaultRainbow(true).injectBoolean(true));
   public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(255, 0, 0, 50), () -> this.tint.booleanValue));
   public double alphaValue;
   private boolean fading = false;
   private final Animation animation = new Animation();
   public static String key = "";
   private int lastLayoutWidth = -1;
   private static final Identifier SPECTRUM_LUT_ID = Identifier.of("luminous", "clickgui_spectrum_lut");
   private NativeImage spectrumLutImage;
   private NativeImageBackedTexture spectrumLutTexture;
   private int spectrumLutHeight = -1;
   private double lastRainbowSpeed = -1.0;
   private double lastRainbowDelay = -1.0;
   private float lastSaturation = -1.0F;
   private int lastScaledHeight = -1;
   public final EnumSetting guiMode;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.setChinese("\u70b9\u51fb\u754c\u9762");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ClickGui.FadeOut());
      this.disableNotification.setChinese("\u5173\u95ed\u901a\u77e5");
      this.sound.setChinese("\u58f0\u97f3");
      this.soundPitch.setChinese("\u58f0\u97f3\u97f3\u8c03");
      this.hoverSound.setChinese("\u60ac\u505c\u58f0\u97f3");
      this.hoverSoundPitch.setChinese("\u60ac\u505c\u97f3\u8c03");
      this.guiSound.setChinese("GUI\u58f0\u97f3");
      this.moduleButtonHeight.setChinese("\u6a21\u5757\u6309\u94ae\u9ad8\u5ea6");
      this.moduleButtonWidth.setChinese("\u6a21\u5757\u6309\u94ae\u5bbd\u5ea6");
      this.categoryWidth.setChinese("\u5206\u7c7b\u5bbd\u5ea6");
      this.categoryBarHeight.setChinese("\u5206\u7c7b\u680f\u9ad8\u5ea6");
      this.textOffset.setChinese("\u6587\u5b57\u504f\u79fb");
      this.titleOffset.setChinese("\u6807\u9898\u504f\u79fb");
      this.alpha.setChinese("\u900f\u660e\u5ea6");
      this.hoverAlpha.setChinese("\u60ac\u505c\u900f\u660e\u5ea6");
      this.topAlpha.setChinese("\u9876\u90e8\u900f\u660e\u5ea6");
      this.backgroundAlpha.setChinese("\u80cc\u666f\u900f\u660e\u5ea6");
      this.fade.setChinese("\u6e10\u9690");
      this.length.setChinese("\u957f\u5ea6");
      this.scrollAnim.setChinese("\u6eda\u52a8\u52a8\u753b");
      this.scrollAnimLength.setChinese("\u6eda\u52a8\u52a8\u753b\u957f\u5ea6");
      this.mouseMove.setChinese("\u9f20\u6807\u79fb\u52a8");
      this.mouseMoveStrength.setChinese("\u9f20\u6807\u79fb\u52a8\u5f3a\u5ea6");
      this.mouseMoveSmooth.setChinese("\u9f20\u6807\u79fb\u52a8\u5e73\u6ed1");
      this.walkShake.setChinese("\u8d70\u8def\u6296\u52a8");
      this.walkShakeStrength.setChinese("\u8d70\u8def\u6296\u52a8\u5f3a\u5ea6");
      this.walkShakeSpeed.setChinese("\u8d70\u8def\u6296\u52a8\u901f\u5ea6");
      this.walkShakeSmooth.setChinese("\u8d70\u8def\u6296\u52a8\u5e73\u6ed1");
      this.walkShakeMax.setChinese("\u8d70\u8def\u6296\u52a8\u6700\u5927");
      this.snow.setChinese("\u96ea\u82b1");
      this.snowAmount.setChinese("\u96ea\u82b1\u6570\u91cf");
      this.snowSpeed.setChinese("\u96ea\u82b1\u901f\u5ea6");
      this.snowSize.setChinese("\u96ea\u82b1\u5927\u5c0f");
      this.snowAlpha.setChinese("\u96ea\u82b1\u900f\u660e\u5ea6");
      this.snowWind.setChinese("\u96ea\u82b1\u98ce\u5411");
      this.tips.setChinese("\u63d0\u793a");
      this.elements.setChinese("\u5143\u7d20");
      this.line.setChinese("\u7ebf\u6761");
      this.recenterLayoutButton.setChinese("\u91cd\u7f6e\u5e03\u5c40");
      this.colors.setChinese("\u989c\u8272");
      this.modernColors.setChinese("\u7b2c\u4e8c\u754c\u9762\u989c\u8272");
      this.modernAccentColor.setChinese("\u4e3b\u8272");
      this.modernAccentMutedColor.setChinese("\u4e3b\u8272(\u6697)");
      this.modernPrimaryTextColor.setChinese("\u4e3b\u6587\u5b57");
      this.modernMutedTextColor.setChinese("\u6b21\u6587\u5b57");
      this.modernOnAccentTextColor.setChinese("\u4e3b\u8272\u4e0a\u6587\u5b57");
      this.modernPanelColor.setChinese("\u9762\u677f");
      this.modernPanelLayerColor.setChinese("\u9762\u677f\u5206\u5c42");
      this.modernPanelDeepColor.setChinese("\u9762\u677f\u6df1\u8272");
      this.modernControlColor.setChinese("\u63a7\u4ef6");
      this.modernControlMutedColor.setChinese("\u63a7\u4ef6(\u6697)");
      this.modernTrackColor.setChinese("\u6ed1\u8f68");
      this.modernHoverColor.setChinese("\u60ac\u505c");
      this.modernIdleFillColor.setChinese("\u9759\u6b62\u586b\u5145");
      this.modernSeparatorColor.setChinese("\u5206\u9694\u7ebf");
      this.modernBackdropAlpha.setChinese("\u80cc\u666f\u906e\u7f69\u6d53\u5ea6");
      this.pulseSpeed.setChinese("\u8109\u51b2\u901f\u5ea6");
      this.pulseCounter.setChinese("\u8ba1\u6570\u5668");
      this.guiMode = this.add(new EnumSetting<>("GuiMode", ClickGui$GuiMode.Legacy));
   }

   public boolean isThunderSetting(Setting var1) {
      return var1 == this.guiMode
         || var1 == this.style
         || var1 == this.backgroundStyle
         || var1 == this.snow
         || var1 == this.elements
         || var1 == this.colors
         || var1 == this.modernColors;
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void keyCodec() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void elementCodec() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   public void recenterLayoutCodec() {
      this.recenterLayoutButton.setValueWithoutTask(false);
      this.recenterLayout();
   }

   private void applyHeights() {
      ArrayList var1 = ClickGuiScreen.getInstance().getComponents();
      int var2 = this.categoryWidth.getValueInt();
      int var3 = this.moduleButtonWidth.getValueInt();
      int var4 = Math.max(var2, var3);
      boolean var5 = this.lastLayoutWidth != var4;
      this.lastLayoutWidth = var4;
      int var6 = var4 + 1;
      int var7 = var1.size();
      int var8 = 10;
      int var9 = 4;
      if (mc != null && mc.getWindow() != null) {
         int var10 = mc.getWindow().getScaledWidth();
         int var11 = mc.getWindow().getScaledHeight();
         int var12 = var7 * var4 + (var7 - 1);
         var8 = Math.round(((float)var10 - var12) / 2.0F);
         var9 = Math.round(var11 / 6.0F);
      }

      boolean var20 = true;
      int var21 = Math.round(((float)var4 - var3) / 2.0F);
      int var22 = var8 + var21;

      for (int var13 = 0; var13 < var1.size(); var13++) {
         Component var14 = (Component)var1.get(var13);
         if (var14.getX() != var22 || var14.getY() != var9) {
            var20 = false;
            break;
         }

         var22 += var6;
      }

      boolean var23 = var5 && mc != null && mc.currentScreen instanceof ClickGuiScreen;
      int var24 = this.categoryBarHeight.getValueInt() + 5;
      int var15 = var8 + var21;

      for (int var16 = 0; var16 < var1.size(); var16++) {
         Component var17 = (Component)var1.get(var16);
         var17.setWidth(var3);
         var17.setHeight(var24);
         if (var20 || var23) {
            var17.setX(var15);
            var17.setY(var9);
            var15 += var6;
         }

         for (Item var19 : var17.getItems()) {
            var19.setHeight(this.moduleButtonHeight.getValueInt());
         }
      }
   }

   private void recenterLayout() {
      ArrayList var1 = ClickGuiScreen.getInstance().getComponents();
      int var2 = this.categoryWidth.getValueInt();
      int var3 = this.moduleButtonWidth.getValueInt();
      int var4 = Math.max(var2, var3);
      int var5 = var4 + 1;
      int var6 = var1.size();
      if (mc != null && mc.getWindow() != null) {
         int var7 = mc.getWindow().getScaledWidth();
         int var8 = mc.getWindow().getScaledHeight();
         int var9 = var6 * var4 + (var6 - 1);
         int var10 = Math.round(((float)var7 - var9) / 2.0F);
         int var11 = Math.round(var8 / 6.0F);
         int var12 = this.categoryBarHeight.getValueInt() + 5;
         int var13 = Math.round(((float)var4 - var3) / 2.0F);
         int var14 = var10 + var13;

         for (int var15 = 0; var15 < var1.size(); var15++) {
            Component var16 = (Component)var1.get(var15);
            var16.setWidth(var3);
            var16.setHeight(var12);
            var16.setX(var14);
            var16.setY(var11);
            var14 += var5;

            for (Item var18 : var16.getItems()) {
               var18.setHeight(this.moduleButtonHeight.getValueInt());
            }
         }
      }
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.updateColor();
         if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
         }

         if (this.guiMode.getValue() == ClickGui$GuiMode.Thunder) {
            TFonts.init();
            mc.setScreen(TClickGuiScreen.getInstance());
         } else {
            mc.setScreen(ClickGuiScreen.getInstance());
         }
      }
   }

   @Override
   public void onDisable() {
      Alien.CONFIG.save();
      if (this.fade.getValue() && this.alphaValue > 0.0) {
         this.fading = true;
      } else if (mc.currentScreen instanceof ClickGuiScreen) {
         mc.currentScreen.close();
      }

      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (mc != null && this.spectrumLutTexture != null) {
         mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
         this.spectrumLutTexture = null;
      }

      if (this.spectrumLutImage != null) {
         this.spectrumLutImage.close();
         this.spectrumLutImage = null;
      }

      this.spectrumLutHeight = -1;
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      this.updateColor();
      Screen var2 = mc.currentScreen;
      if (!(var2 instanceof ClickGuiScreen) && !(var2 instanceof TClickGuiScreen)) {
         if (this.guiWasOpen) {
            Alien.CONFIG.save();
            this.guiWasOpen = false;
         }

         this.disable();
      } else {
         this.guiWasOpen = true;
      }
   }

   @EventListener
   public void onResize(ResizeEvent var1) {
      if (mc != null && mc.currentScreen instanceof ClickGuiScreen) {
         this.recenterLayout();
      }
   }

   public void updateColor() {
      Button.hoverColor = this.hoverColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultColor = this.defaultColor.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public Identifier getSpectrumLutId() {
      return SPECTRUM_LUT_ID;
   }

   public int getSpectrumLutHeight() {
      return this.spectrumLutHeight;
   }

   public void updateSpectrumLut(int var1) {
      if (var1 > 0 && mc != null) {
         long var2 = System.currentTimeMillis();
         double var4 = this.rainbowSpeed.getValue();
         double var6 = this.rainbowDelay.getValue();
         float var8 = this.saturation.getValueFloat() / 255.0F;
         if (var4 == this.lastRainbowSpeed && var6 == this.lastRainbowDelay && var8 == this.lastSaturation) {
            boolean var17 = false;
         } else {
            boolean var9 = true;
         }

         boolean var18 = this.lastScaledHeight != var1;
         if (var18 || this.spectrumLutTexture == null || this.spectrumLutImage == null) {
            this.recreateSpectrumLut(var1);
            boolean var10 = true;
         }

         if (this.spectrumLutTexture != null && this.spectrumLutImage != null && this.spectrumLutHeight > 0) {
            this.lastRainbowSpeed = var4;
            this.lastRainbowDelay = var6;
            this.lastSaturation = var8;
            this.lastScaledHeight = var1;
            int var19 = this.spectrumLutHeight;

            for (int var11 = 0; var11 < var19; var11++) {
               double var12 = var11 * 0.25;
               double var14 = (var2 * var4 + var12 * var6) / 20.0;
               int var16 = Color.getHSBColor((float)(var14 % 360.0 / 360.0), var8, 1.0F).getRGB();
               this.spectrumLutImage.setColor(0, var11, argbToAbgr(var16));
            }

            if (RenderSystem.isOnRenderThread()) {
               this.spectrumLutTexture.upload();
            } else {
               RenderSystem.recordRenderCall(() -> this.spectrumLutTexture.upload());
            }
         }
      }
   }

   private void recreateSpectrumLut(int var1) {
      if (mc != null) {
         if (this.spectrumLutTexture != null) {
            mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
            this.spectrumLutTexture = null;
         }

         if (this.spectrumLutImage != null) {
            this.spectrumLutImage.close();
            this.spectrumLutImage = null;
         }

         this.spectrumLutHeight = var1;
         this.spectrumLutImage = new NativeImage(Format.RGBA, 1, var1, false);
         this.spectrumLutTexture = new NativeImageBackedTexture(this.spectrumLutImage);
         if (RenderSystem.isOnRenderThread()) {
            this.spectrumLutTexture.upload();
            mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
         } else {
            RenderSystem.recordRenderCall(() -> {
               this.spectrumLutTexture.upload();
               mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
            });
         }
      }
   }

   private static int argbToAbgr(int var0) {
      int var1 = var0 >> 24 & 0xFF;
      int var2 = var0 >> 16 & 0xFF;
      int var3 = var0 >> 8 & 0xFF;
      int var4 = var0 & 0xFF;
      return var1 << 24 | var4 << 16 | var3 << 8 | var2;
   }

   public Color getColor() {
      return this.getColor(0.0);
   }

   public Color getColor(double var1) {
      return this.getModeColor(this.color.getValue(), var1);
   }

   public Color getActiveColor() {
      return this.getActiveColor(0.0);
   }

   public Color getActiveColor(double var1) {
      return this.getModeColor(this.activeColor.getValue(), var1);
   }

   private Color getModeColor(Color var1, double var2) {
      return this.colorMode.getValue() == ClickGui.ColorMode.Custom ? var1 : this.dynamicColor(var2);
   }

   private Color dynamicColor(double var1) {
      if (this.colorMode.getValue() == ClickGui.ColorMode.Pulse) {
         return this.secondColor.booleanValue
            ? ColorUtil.pulseColor(this.color.getValue(), this.secondColor.getValue(), var1, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
            : ColorUtil.pulseColor(this.color.getValue(), var1, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() != ClickGui.ColorMode.Rainbow && this.colorMode.getValue() != ClickGui.ColorMode.Spectrum) {
         return this.color.getValue();
      } else {
         double var3 = (System.currentTimeMillis() * this.rainbowSpeed.getValue() + var1 * this.rainbowDelay.getValue()) / 20.0;
         return Color.getHSBColor((float)(var3 % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
      }
   }

   public void updateBackgroundStyle() {
      ClickGui.BackgroundStyle var1 = this.backgroundStyle.getValue();
      if (var1 == null) {
         var1 = ClickGui.BackgroundStyle.Dark;
      }

      if (var1 == ClickGui.BackgroundStyle.Dark) {
         this.backGround.setValue(new Color(30, 30, 30, 120));
         this.backGround.booleanValue = true;
         this.backgroundAlpha.setValue(120.0);
      } else if (var1 == ClickGui.BackgroundStyle.Transparent) {
         this.backGround.booleanValue = false;
         this.defaultColor.setValue(new Color(0, 0, 0, 50));
         this.backgroundAlpha.setValue(0.0);
      }
   }

   public void updateStyle() {
      ClickGui.Style var1 = this.style.getValue();
      if (var1 != ClickGui.Style.Spectrum && var1 != ClickGui.Style.LightPurple) {
         var1 = ClickGui.Style.Static;
         if (this.style.getValue() != ClickGui.Style.Static) {
            this.style.setValue(ClickGui.Style.Static);
         }
      }

      this.color.setValue(new Color(120, 180, 235));
      this.activeColor.setValue(new Color(120, 180, 235));
      this.hoverColor.setValue(new Color(130, 130, 130, 110));
      this.defaultColor.setValue(new Color(30, 30, 30, 80));
      this.defaultTextColor.setValue(new Color(225, 225, 228));
      this.enableTextColor.setValue(new Color(255, 255, 255));
      this.backGround.setValue(new Color(30, 30, 30, 120));
      this.backGround.booleanValue = true;
      this.color.rainbow = false;
      this.activeColor.rainbow = false;
      this.tint.rainbow = false;
      this.endColor.rainbow = false;
      this.alpha.setValue(120.0);
      this.hoverAlpha.setValue(220.0);
      this.topAlpha.setValue(190.0);
      this.backgroundAlpha.setValue(120.0);
      if (var1 == ClickGui.Style.LightPurple) {
         this.colors.setValueWithoutTask(false);
         this.colors.setOpen(false);
         this.colorMode.setValue(ClickGui.ColorMode.Custom);
         this.color.setValue(new Color(200, 160, 255));
         this.activeColor.setValue(new Color(200, 160, 255));
         this.hoverColor.setValue(new Color(255, 255, 255, 200));
         this.defaultColor.setValue(new Color(30, 30, 30, 18));
         this.defaultTextColor.setValue(new Color(216, 186, 255));
         this.enableTextColor.setValue(new Color(216, 186, 255));
         this.backGround.setValue(new Color(30, 30, 30));
         this.backGround.booleanValue = true;
         this.color.rainbow = false;
         this.activeColor.rainbow = false;
         this.tint.rainbow = false;
         this.endColor.rainbow = false;
         this.alpha.setValue(200.0);
         this.hoverAlpha.setValue(235.0);
         this.topAlpha.setValue(235.0);
         this.backgroundAlpha.setValue(200.0);
         this.tint.booleanValue = true;
         this.tint.setValue(new Color(200, 160, 255, 36));
         this.endColor.setValue(new Color(200, 160, 255, 50));
         this.secondColor.booleanValue = false;
         this.updateBackgroundStyle();
      } else if (var1 == ClickGui.Style.Static) {
         this.colors.setValueWithoutTask(false);
         this.colors.setOpen(false);
         this.colorMode.setValue(ClickGui.ColorMode.Custom);
         this.tint.booleanValue = true;
         this.tint.rainbow = false;
         this.tint.setValue(new Color(0, 120, 212, 36));
         this.endColor.rainbow = false;
         this.endColor.setValue(new Color(0, 0, 255, 50));
         this.secondColor.booleanValue = false;
         this.updateBackgroundStyle();
      } else {
         this.colors.setValueWithoutTask(true);
         this.colors.setOpen(true);
         if (var1 == ClickGui.Style.Spectrum) {
            this.colorMode.setValue(ClickGui.ColorMode.Spectrum);
            this.rainbowSpeed.setValue(1.0);
            this.saturation.setValue(210.0);
            this.rainbowDelay.setValue(50.0);
            this.tint.booleanValue = true;
            this.tint.rainbow = true;
            this.endColor.rainbow = false;
            this.endColor.setValue(new Color(255, 255, 255, 50));
            this.secondColor.booleanValue = false;
            this.updateBackgroundStyle();
         }
      }
   }

   public Color dynamicColor(int var1) {
      return this.dynamicColor((double)var1);
   }

   public static enum BackgroundStyle {
      Dark,
      Transparent;
   }

   public static enum ColorMode {
      Custom,
      Pulse,
      Rainbow,
      Spectrum;
   }

   public static enum ExpandIcon {
      PlusMinus,
      Chevron,
      Gear;
   }

   public class FadeOut {
      @EventListener(
         priority = -99999
      )
      public void onRender2D(Render2DEvent var1) {
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation
                  .get(ClickGui.this.isOn() ? 1.0 : 0.0, ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
            }

            if (ClickGui.this.alphaValue > 0.0) {
               Screen var2 = Wrapper.mc.currentScreen;
               if (!(var2 instanceof ClickGuiScreen)) {
                  var1.drawContext.getMatrices().push();
                  var1.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
                  ClickGuiScreen.getInstance().render(var1.drawContext, 0, 0, var1.tickDelta);
                  var1.drawContext.getMatrices().pop();
               }
            }

            if (ClickGui.this.fading && ClickGui.this.alphaValue <= 0.0) {
               ClickGui.this.fading = false;
               if (Wrapper.mc.currentScreen instanceof ClickGuiScreen) {
                  Wrapper.mc.currentScreen.close();
               }
            }
         } else {
            ClickGui.this.alphaValue = 1.0;
         }
      }
   }

   public static enum SnowShape {
      Snowflake,
      Circle;
   }

   public static enum Style {
      Static,
      Spectrum,
      LightPurple;
   }
}
