package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.asm.accessors.ISimpleRegistry;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

public class HUD extends Module {
   public static HUD INSTANCE;
   public final EnumSetting<HUD.Page> page = this.add(new EnumSetting<>("Page", HUD.Page.General));
   public final BooleanSetting renderingUp = this.add(new BooleanSetting("RenderingUp", false, () -> this.page.is(HUD.Page.General)));
   public final BooleanSetting font = this.add(new BooleanSetting("Font", true, () -> this.page.is(HUD.Page.General)));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true, () -> this.page.is(HUD.Page.General)));
   public final BooleanSetting lowerCase = this.add(new BooleanSetting("LowerCase", false, () -> this.page.is(HUD.Page.General)));
   public final BooleanSetting sort = this.add(new BooleanSetting("Sort", false, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting xOffset = this.add(new SliderSetting("XOffset", 0.0, 0.0, 50.0, 0.1, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.0, 0.0, 50.0, 0.1, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -10.0, 10.0, 0.1, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting interval = this.add(new SliderSetting("Interval", 0.0, 0.0, 15.0, 0.1, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting enableLength = this.add(new SliderSetting("EnableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting disableLength = this.add(new SliderSetting("DisableLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
   public final SliderSetting fadeLength = this.add(new SliderSetting("FadeLength", 200, 0, 1000, () -> this.page.is(HUD.Page.General)));
   public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.CircInOut, () -> this.page.is(HUD.Page.General)));
   public final BooleanSetting arrayList = this.add(new BooleanSetting("ArrayList", true, () -> this.page.is(HUD.Page.Element)).setParent());
   public final BooleanSetting listSort = this.add(new BooleanSetting("ListSort", true, () -> this.page.is(HUD.Page.Element) && this.arrayList.isOpen()));
   public final BooleanSetting armor = this.add(new BooleanSetting("Armor", true, () -> this.page.is(HUD.Page.Element)).setParent());
   public final SliderSetting armorOffset = this.add(
      new SliderSetting("ArmorOffset", 1.0, 0.0, 100.0, -1.0, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen())
   );
   public final BooleanSetting durability = this.add(new BooleanSetting("Durability", true, () -> this.page.is(HUD.Page.Element) && this.armor.isOpen()));
   public final BooleanSetting waterMark = this.add(new BooleanSetting("WaterMark", true, () -> this.page.is(HUD.Page.Element)).setParent());
   public final ColorSetting pulse = this.add(
      new ColorSetting("Pulse", new Color(79, 0, 0), () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen()).injectBoolean(true)
   );
   public final StringSetting waterMarkString = this.add(
      new StringSetting("Title", "%hackname% %version%", () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen())
   );
   public final SliderSetting waterMarkOffset = this.add(
      new SliderSetting("Offset", 1.0, 0.0, 100.0, -1.0, () -> this.page.is(HUD.Page.Element) && this.waterMark.isOpen())
   );
   public final BooleanSetting fps = this.add(new BooleanSetting("FPS", true, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting ping = this.add(new BooleanSetting("Ping", true, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting tps = this.add(new BooleanSetting("TPS", true, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting ip = this.add(new BooleanSetting("IP", false, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting time = this.add(new BooleanSetting("Time", false, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting speed = this.add(new BooleanSetting("Speed", true, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting brand = this.add(new BooleanSetting("Brand", false, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting potions = this.add(new BooleanSetting("Potions", true, () -> this.page.is(HUD.Page.Element)));
   public final BooleanSetting coords = this.add(new BooleanSetting("Coords", true, () -> this.page.is(HUD.Page.Element)).setParent());
   public final BooleanSetting colorSync = this.add(new BooleanSetting("ColorSync", true, () -> this.page.is(HUD.Page.Element) && this.coords.isOpen()));
   private final EnumSetting<HUD.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", HUD.ColorMode.Pulse, () -> this.page.is(HUD.Page.Color)));
   public final ColorSetting color = this.add(
      new ColorSetting("Color", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Custom)
   );
   private final SliderSetting rainbowSpeed = this.add(
      new SliderSetting("RainbowSpeed", 1.0, 1.0, 10.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow)
   );
   private final SliderSetting saturation = this.add(
      new SliderSetting("Saturation", 220.0, 1.0, 255.0, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow)
   );
   private final SliderSetting rainbowDelay = this.add(
      new SliderSetting("Delay", 220, 0, 1000, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Rainbow)
   );
   private final ColorSetting endColor = this.add(
      new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)
         .injectBoolean(true)
   );
   private final SliderSetting pulseSpeed = this.add(
      new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)
   );
   private final SliderSetting pulseCounter = this.add(
      new SliderSetting("Counter", 10, 1, 50, () -> this.page.is(HUD.Page.Color) && this.colorMode.getValue() == HUD.ColorMode.Pulse)
   );
   public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false, () -> this.page.is(HUD.Page.Color)).setParent());
   public final SliderSetting radius = this.add(new SliderSetting("Radius", 10.0, 0.0, 100.0, () -> this.page.is(HUD.Page.Color) && this.blur.isOpen()));
   private final BooleanSetting backGround = this.add(new BooleanSetting("BackGround", false, () -> this.page.is(HUD.Page.Color)).setParent());
   public final SliderSetting width = this.add(new SliderSetting("Width", 0.0, 0.0, 15.0, () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen()));
   private final ColorSetting bgColor = this.add(
      new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> this.page.is(HUD.Page.Color) && this.backGround.isOpen())
   );
   private final ColorSetting rect = this.add(new ColorSetting("Rect", new Color(208, 0, 0), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
   private final ColorSetting glow = this.add(new ColorSetting("Glow", new Color(208, 0, 100), () -> this.page.is(HUD.Page.Color)).injectBoolean(false));
   private final DecimalFormat decimal = new DecimalFormat("0.0");
   private final ArrayList<HUD.Info> infoList = new ArrayList<>();
   private final ArrayList<HUD.Info> moduleList = new ArrayList<>();

   public HUD() {
      super("HUD", Module.Category.Client);
      this.setChinese("\u754c\u9762");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new HUD.InitHandler());
      this.renderingUp.setChinese("\u4ece\u4e0a\u65b9\u6e32\u67d3");
      this.font.setChinese("\u5b57\u4f53");
      this.shadow.setChinese("\u9634\u5f71");
      this.lowerCase.setChinese("\u5c0f\u5199");
      this.sort.setChinese("\u6392\u5e8f");
      this.xOffset.setChinese("X\u504f\u79fb");
      this.yOffset.setChinese("Y\u504f\u79fb");
      this.textOffset.setChinese("\u6587\u5b57\u504f\u79fb");
      this.interval.setChinese("\u95f4\u8ddd");
      this.enableLength.setChinese("\u542f\u7528\u52a8\u753b\u957f\u5ea6");
      this.disableLength.setChinese("\u7981\u7528\u52a8\u753b\u957f\u5ea6");
      this.armor.setChinese("\u76d4\u7532");
      this.waterMarkOffset.setChinese("\u504f\u79fb");
      this.fps.setChinese("\u5e27\u7387");
      this.ping.setChinese("\u5ef6\u8fdf");
      this.tps.setChinese("TPS");
      this.time.setChinese("\u65f6\u95f4");
      this.speed.setChinese("\u901f\u5ea6");
      this.rainbowDelay.setChinese("\u5ef6\u8fdf");
      this.blur.setChinese("\u6a21\u7cca");
      this.width.setChinese("\u5bbd\u5ea6");

      for (StatusEffect var2 : Registries.STATUS_EFFECT) {
         try {
            RegistryEntry var3 = (RegistryEntry)((ISimpleRegistry)Registries.STATUS_EFFECT).getValueToEntry().get(var2);
            this.infoList.add(new HUD.Info(() -> {
               StatusEffectInstance var2x = mc.player.getStatusEffect(var3);
               if (var2x != null) {
                  String var3x = var2.getName().getString() + " " + (var2x.getAmplifier() + 1);
                  String var4x = getDuration(var2x);
                  return var3x + " \u00a7f" + var4x;
               } else {
                  return "";
               }
            }, () -> mc.player.hasStatusEffect(var3) && this.potions.getValue()));
         } catch (Exception var4) {
         }
      }

      this.infoList
         .add(
            new HUD.Info(
               () -> "ServerBrand \u00a7f" + (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "")),
               this.brand::getValue
            )
         );
      this.infoList
         .add(new HUD.Info(() -> "Server \u00a7f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address), this.ip::getValue));
      this.infoList.add(new HUD.Info(() -> "TPS \u00a7f" + Alien.SERVER.getTPS() + " [" + Alien.SERVER.getCurrentTPS() + "]", this.tps::getValue));
      this.infoList.add(new HUD.Info(() -> {
         double var1 = mc.player.getX() - mc.player.prevX;
         double var3x = mc.player.getZ() - mc.player.prevZ;
         double var5 = Math.sqrt(var1 * var1 + var3x * var3x) / 1000.0;
         double var7 = 1.388888888888889E-5;
         float var9 = Alien.TIMER.get();
         double var10 = var5 / var7 * var9;
         return String.format("Speed \u00a7f%skm/h", this.decimal.format(var10));
      }, this.speed::getValue));
      this.infoList.add(new HUD.Info(() -> "Time \u00a7f" + new SimpleDateFormat("h:mm keyCodec", Locale.ENGLISH).format(new Date()), this.time::getValue));
      this.infoList.add(new HUD.Info(() -> {
         PlayerListEntry var0 = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         String var1 = var0 == null ? "Unknown" : var0.getLatency() + "ms";
         return "Ping \u00a7f" + var1;
      }, this.ping::getValue));
      this.infoList.add(new HUD.Info(() -> "FPS \u00a7f" + Alien.FPS.getFps(), this.fps::getValue));
   }

   public static String getDuration(StatusEffectInstance var0) {
      if (var0.isInfinite()) {
         return "\u221e";
      } else {
         int var1 = var0.getDuration();
         int var2 = var1 / 1200;
         int var3 = var1 % 1200 / 20;
         return String.format("%d:%02d", var2, var3);
      }
   }

   @Override
   public void onRender2D(DrawContext var1, float var2) {
      if (this.waterMark.getValue()) {
         if (this.pulse.booleanValue) {
            TextUtil.drawStringPulse(
               var1,
               this.waterMarkString.getValue().replaceAll("%version%", "4.0").replaceAll("%hackname%", "\u5b99\u65afClient"),
               this.waterMarkOffset.getValueInt(),
               this.waterMarkOffset.getValueInt(),
               this.color.getValue(),
               this.pulse.getValue(),
               this.pulseSpeed.getValue(),
               this.pulseCounter.getValueInt(),
               this.font.getValue(),
               this.shadow.getValue()
            );
         } else {
            TextUtil.drawString(
               var1,
               this.waterMarkString.getValue().replaceAll("%version%", "4.0").replaceAll("%hackname%", "\u5b99\u65afClient"),
               this.waterMarkOffset.getValueInt(),
               this.waterMarkOffset.getValueInt(),
               this.color.getValue().getRGB(),
               this.font.getValue(),
               this.shadow.getValue()
            );
         }
      }

      int var3 = this.getHeight();
      if (this.coords.getValue()) {
         String var4 = getCoords();
         this.drawCoord(var1, var4, mc.getWindow().getScaledHeight() - var3 - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
      }

      HUD.Info.onRender(var1, this.infoList, this.renderingUp.getValue());
      if (this.arrayList.getValue()) {
         HUD.Info.onRender(var1, this.moduleList, !this.renderingUp.getValue());
      }
   }

   @EventListener(
      priority = -999
   )
   public void onUpdate(ClientTickEvent var1) {
      if (!nullCheck() && var1.isPost()) {
         HUD.Info.onUpdate(this.infoList, this.sort.getValue());
         if (this.arrayList.getValue()) {
            HUD.Info.onUpdate(this.moduleList, this.listSort.getValue());
         }
      }
   }

   private static String getCoords() {
      if (FakeCoords.INSTANCE != null && FakeCoords.INSTANCE.isOn()) {
         return FakeCoords.INSTANCE.getCoords();
      } else {
         boolean var0 = mc.world.getRegistryKey().equals(World.NETHER);
         int var1 = mc.player.getBlockX();
         int var2 = mc.player.getBlockY();
         int var3 = mc.player.getBlockZ();
         float var4 = !var0 ? 0.125F : 8.0F;
         int var5 = (int)(mc.player.getX() * var4);
         int var6 = (int)(mc.player.getZ() * var4);
         return "XYZ \u00a7f"
            + (
               var0
                  ? var1 + ", " + var2 + ", " + var3 + " \u00a77[\u00a7f" + var5 + ", " + var6 + "\u00a77]\u00a7f"
                  : var1 + ", " + var2 + ", " + var3 + "\u00a77 [\u00a7f" + var5 + ", " + var6 + "\u00a77]"
            );
      }
   }

   private int getWidth(String var1) {
      if (this.lowerCase.getValue()) {
         var1 = var1.toLowerCase();
      }

      return this.font.getValue() ? (int)FontManager.ui.getWidth(var1) : mc.textRenderer.getWidth(var1);
   }

   private int getHeight() {
      return this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
   }

   private void drawCoord(DrawContext var1, String var2, int var3) {
      if (this.colorSync.getValue()) {
         if (this.lowerCase.getValue()) {
            var2 = var2.toLowerCase();
         }

         TextUtil.drawString(var1, var2, 2.0, var3, this.getColor(20.0), this.font.getValue(), this.shadow.getValue());
      } else if (this.pulse.booleanValue) {
         TextUtil.drawStringPulse(
            var1,
            var2,
            2.0,
            var3,
            this.color.getValue(),
            this.pulse.getValue(),
            this.pulseSpeed.getValue(),
            this.pulseCounter.getValueInt(),
            this.font.getValue(),
            this.shadow.getValue()
         );
      } else {
         TextUtil.drawString(var1, var2, 2.0, var3, this.color.getValue().getRGB(), this.font.getValue(), this.shadow.getValue());
      }
   }

   public int getColor(double var1) {
      return this.colorMode.getValue() != HUD.ColorMode.Custom ? this.rainbow(var1).getRGB() : this.color.getValue().getRGB();
   }

   private Color rainbow(double var1) {
      if (this.colorMode.getValue() == HUD.ColorMode.Pulse) {
         return this.endColor.booleanValue
            ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), var1, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
            : ColorUtil.pulseColor(this.color.getValue(), var1, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.getValue() == HUD.ColorMode.Rainbow) {
         double var3 = Math.ceil((System.currentTimeMillis() * this.rainbowSpeed.getValue() + var1 * this.rainbowDelay.getValue()) / 20.0);
         return Color.getHSBColor((float)(var3 % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
      } else {
         return this.color.getValue();
      }
   }

   private int getFontHeight() {
      return this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;
   }

   public class Info {
      public final Callable<String> info;
      public String string;
      public final BooleanSupplier drawn;
      public double currentX = 0.0;
      public boolean isOn;
      public final Animation animation = new Animation();
      public final Animation fadeAnimation = new Animation();
      static double fontHeight;
      static double currentY;
      static int windowWidth;
      static boolean fromUp;
      static double counter;

      public Info(Callable<String> nullx, BooleanSupplier nullxx) {
         this.info = nullx;
         this.drawn = nullxx;

         try {
            this.string = this.info.call();
         } catch (Exception var5) {
         }
      }

      public static void onRender(DrawContext var0, List<HUD.Info> var1, boolean var2) {
         counter = 20.0;
         fromUp = var2;
         fontHeight = HUD.INSTANCE.getFontHeight();
         currentY = var2
            ? 1.0 + HUD.INSTANCE.yOffset.getValue()
            : Wrapper.mc.getWindow().getScaledHeight()
               - fontHeight
               - 1.0
               - (
                  Wrapper.mc.currentScreen instanceof ChatScreen && HUD.INSTANCE.yOffset.getValue() < 12.0
                     ? 12.0 - HUD.INSTANCE.yOffset.getValue() + HUD.INSTANCE.interval.getValue() / 2.0
                     : 0.0
               )
               - HUD.INSTANCE.yOffset.getValue();
         windowWidth = Wrapper.mc.getWindow().getScaledWidth();

         for (HUD.Info var4 : var1) {
            var4.draw(var0);
         }
      }

      public static void onUpdate(List<HUD.Info> var0, boolean var1) {
         for (HUD.Info var3 : var0) {
            var3.onUpdate();
         }

         if (var1) {
            var0.sort(Comparator.comparingInt(var0x -> var0x.string == null ? 0 : -HUD.INSTANCE.getWidth(var0x.string)));
         }
      }

      public void onUpdate() {
         this.isOn = this.drawn.getAsBoolean();
         if (this.isOn) {
            try {
               this.string = HUD.this.lowerCase.getValue() ? this.info.call().toLowerCase() : this.info.call();
            } catch (Exception var2) {
               var2.printStackTrace();
            }
         }
      }

      public void draw(DrawContext var1) {
         if (this.currentX > 0.0 || this.isOn) {
            this.currentX = this.animation
               .get(
                  this.isOn ? HUD.this.getWidth(this.string) + 1 : 0.0,
                  this.isOn ? HUD.this.enableLength.getValueInt() : HUD.this.disableLength.getValueInt(),
                  HUD.this.easing.getValue()
               );
            double var2 = this.currentX + HUD.this.xOffset.getValueFloat();
            double var4 = this.fadeAnimation.get(this.isOn ? 1.0 : 0.0, HUD.this.fadeLength.getValueInt(), HUD.this.easing.getValue());
            if (var4 > 0.04) {
               int var6 = ColorUtil.injectAlpha(
                  HUD.this.getColor(counter = counter + (fromUp ? var4 : -var4)), (int)(HUD.this.color.getValue().getAlpha() * var4)
               );
               if (HUD.this.blur.getValue()) {
                  Alien.BLUR
                     .applyBlur(
                        (float)(HUD.this.radius.getValue() * var4),
                        (float)(windowWidth - var2 - HUD.this.width.getValueFloat() / 2.0F),
                        (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F,
                        (float)var2 + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(),
                        (float)fontHeight + HUD.this.interval.getValueFloat()
                     );
               }

               if (HUD.this.backGround.getValue()) {
                  Render2DUtil.drawRect(
                     var1.getMatrices(),
                     (float)(windowWidth - var2 - HUD.this.width.getValueFloat() / 2.0F),
                     (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F,
                     (float)var2 + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(),
                     (float)fontHeight + HUD.this.interval.getValueFloat(),
                     ColorUtil.injectAlpha(
                        HUD.this.bgColor.rainbow ? var6 : HUD.this.bgColor.getValue().getRGB(), (int)(HUD.this.bgColor.getValue().getAlpha() * var4)
                     )
                  );
               }

               if (HUD.this.glow.booleanValue) {
                  Render2DUtil.drawGlow(
                     var1.getMatrices(),
                     (float)(windowWidth - var2 - HUD.this.width.getValueFloat() / 2.0F),
                     (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F,
                     (float)var2 + HUD.this.width.getValueFloat() - HUD.this.xOffset.getValueFloat(),
                     (float)fontHeight + HUD.this.interval.getValueFloat(),
                     ColorUtil.injectAlpha(HUD.this.glow.rainbow ? var6 : HUD.this.glow.getValue().getRGB(), (int)(HUD.this.glow.getValue().getAlpha() * var4))
                  );
               }

               TextUtil.drawString(
                  var1,
                  this.string,
                  windowWidth - var2,
                  currentY + HUD.this.textOffset.getValueFloat(),
                  var6,
                  HUD.this.font.getValue(),
                  HUD.this.shadow.getValue()
               );
               if (HUD.this.rect.booleanValue) {
                  Render2DUtil.drawRect(
                     var1.getMatrices(),
                     windowWidth + HUD.this.width.getValueFloat() / 2.0F - HUD.this.xOffset.getValueFloat(),
                     (float)currentY - 1.0F - HUD.this.interval.getValueFloat() / 2.0F,
                     1.0F,
                     (float)fontHeight + HUD.this.interval.getValueFloat(),
                     HUD.this.rect.rainbow ? var6 : ColorUtil.injectAlpha(HUD.this.rect.getValue(), (int)(HUD.this.rect.getValue().getAlpha() * var4)).getRGB()
                  );
               }

               currentY = currentY + (fromUp ? (fontHeight + HUD.this.interval.getValue()) * var4 : -(fontHeight + HUD.this.interval.getValue()) * var4);
            }
         }
      }
   }

   public class InitHandler {
      @EventListener
      public void onInit(InitEvent var1) {
         for (Module var3 : Alien.MODULE.getModules()) {
            HUD.this.moduleList.add(HUD.this.new Info(var3::getArrayName, () -> var3.isOn() && var3.drawn.getValue()));
         }

         Alien.EVENT_BUS.unsubscribe(this);
      }
   }

   public static enum Page {
      General,
      Element,
      Color;
   }
}
