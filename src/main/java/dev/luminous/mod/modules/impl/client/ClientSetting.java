package dev.luminous.mod.modules.impl.client;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;

public class ClientSetting extends Module {
   public static final Animation animation = new Animation();
   public static ClientSetting INSTANCE;
   public final EnumSetting<ClientSetting.Page> page = this.add(new EnumSetting<>("Page", ClientSetting.Page.Game));
   public final BooleanSetting lowVersion = this.add(new BooleanSetting("1.12", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting crawl = this.add(new BooleanSetting("Crawl", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting rotations = this.add(new BooleanSetting("ShowRotations", true, () -> this.page.is(ClientSetting.Page.Game)).setParent());
   public final BooleanSetting sync = this.add(new BooleanSetting("Sync", false, () -> this.page.is(ClientSetting.Page.Game) && this.rotations.isOpen()));
   public final BooleanSetting titleFix = this.add(new BooleanSetting("TitleFix", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting fuckFPSLimit = this.add(new BooleanSetting("FuckFPSLimit", true, () -> this.page.is(ClientSetting.Page.Game)));
   private final BooleanSetting portalGui = this.add(new BooleanSetting("BlockTickNausea", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting optimizedCalc = this.add(new BooleanSetting("OptimizedCalc", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting mioCompatible = this.add(new BooleanSetting("MioCompatible", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final StringSetting prefix = this.add(new StringSetting("Prefix", ";", () -> this.page.is(ClientSetting.Page.Misc)));
   public final BooleanSetting chinese = this.add(new BooleanSetting("Chinese", true, () -> this.page.is(ClientSetting.Page.Misc)).injectTask(() -> {
      if (mc.currentScreen instanceof ClickGuiScreen) {
         ClickGuiScreen.rebuild();
      }
   }));
   public final BooleanSetting titleOverride = this.add(new BooleanSetting("TitleOverride", true, () -> this.page.is(ClientSetting.Page.Misc)).setParent());
   public final StringSetting windowTitle = this.add(
      new StringSetting("WindowTitle", "\u5b99\u65afClient", () -> this.page.is(ClientSetting.Page.Misc) && this.titleOverride.isOpen())
   );
   public final BooleanSetting debug = this.add(new BooleanSetting("DebugException", true, () -> this.page.is(ClientSetting.Page.Misc)));
   public final BooleanSetting caughtException = this.add(new BooleanSetting("CaughtException", false, () -> this.page.is(ClientSetting.Page.Misc)).setParent());
   public final BooleanSetting log = this.add(new BooleanSetting("Log", true, () -> this.page.is(ClientSetting.Page.Misc) && this.caughtException.isOpen()));
   private final BooleanSetting hotbar = this.add(new BooleanSetting("HotbarAnim", true, () -> this.page.is(ClientSetting.Page.Gui)));
   public final SliderSetting hotbarTime = this.add(new SliderSetting("HotbarTime", 100, 0, 1000, () -> this.page.is(ClientSetting.Page.Gui)));
   public final EnumSetting<Easing> animEase = this.add(new EnumSetting<>("AnimEase", Easing.CubicInOut, () -> this.page.is(ClientSetting.Page.Gui)));
   public final BooleanSetting darkening = this.add(new BooleanSetting("Darkening", true, () -> this.page.is(ClientSetting.Page.Gui)));
   public final StringSetting hackName = this.add(
      new StringSetting("Notification", "[\u5b99\u65afClient]", () -> this.page.getValue() == ClientSetting.Page.Notification)
   );
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 38, 38), () -> this.page.getValue() == ClientSetting.Page.Notification));
   public final EnumSetting<ClientSetting.Style> messageStyle = this.add(
      new EnumSetting<>("Style", ClientSetting.Style.Mio, () -> this.page.getValue() == ClientSetting.Page.Notification)
   );
   public final BooleanSetting toggle = this.add(
      new BooleanSetting("ModuleToggle", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
   );
   public final BooleanSetting onlyOne = this.add(
      new BooleanSetting("OnlyOne", false, () -> this.page.getValue() == ClientSetting.Page.Notification && this.toggle.isOpen())
   );
   public final BooleanSetting banner = this.add(
      new BooleanSetting("ToggleBanner", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
   );
   public final EnumSetting<ClientSetting.BannerStyle> bannerStyle = this.add(
      new EnumSetting<>("BannerStyle", ClientSetting.BannerStyle.iOS, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final EnumSetting<ClientSetting.StackDir> bannerStack = this.add(
      new EnumSetting<>("BannerStack", ClientSetting.StackDir.Down, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final SliderSetting bannerFade = this.add(
      new SliderSetting("BannerFade", 160, 0, 1000, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final SliderSetting bannerHold = this.add(
      new SliderSetting("BannerHold", 1000, 0, 3000, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final BooleanSetting bannerSound = this.add(
      new BooleanSetting("BannerSound", true, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen()).setParent()
   );
   public final SliderSetting bannerSoundPitch = this.add(
      new SliderSetting(
         "BannerPitch",
         1.0,
         0.5,
         2.0,
         0.05,
         () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen() && this.bannerSound.getValue()
      )
   );
   public final BooleanSetting keepHistory = this.add(new BooleanSetting("KeepHistory", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting infiniteChat = this.add(new BooleanSetting("InfiniteChat", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting hideIndicator = this.add(new BooleanSetting("HideIndicator", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 300, 0, 1000, () -> this.page.getValue() == ClientSetting.Page.ChatHud)
   );
   public final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final SliderSetting animateOffset = this.add(new SliderSetting("Offset", -40, -200, 100, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting loadGuiKey = this.add(new BooleanSetting("LoadGuiKey", true, () -> this.page.is(ClientSetting.Page.Gui)));

   public ClientSetting() {
      super("ClientSetting", Module.Category.Client);
      this.setChinese("\u5ba2\u6237\u7aef\u8bbe\u7f6e");
      INSTANCE = this;
      this.crawl.setChinese("\u722c\u884c");
      this.rotations.setChinese("\u663e\u793a\u65cb\u8f6c");
      this.sync.setChinese("\u540c\u6b65");
      this.titleFix.setChinese("\u6807\u9898\u4fee\u590d");
      this.fuckFPSLimit.setChinese("\u89e3\u9664\u5e27\u7387\u9650\u5236");
      this.portalGui.setChinese("\u963b\u6b62Tick\u6076\u5fc3");
      this.optimizedCalc.setChinese("\u4f18\u5316\u8ba1\u7b97");
      this.mioCompatible.setChinese("Mio\u517c\u5bb9");
      this.chinese.setChinese("\u4e2d\u6587");
      this.titleOverride.setChinese("\u6807\u9898\u8986\u76d6");
      this.debug.setChinese("\u8c03\u8bd5\u5f02\u5e38");
      this.caughtException.setChinese("\u6355\u83b7\u5f02\u5e38");
      this.log.setChinese("\u65e5\u5fd7");
      this.hotbar.setChinese("\u5feb\u6377\u680f\u52a8\u753b");
      this.hotbarTime.setChinese("\u5feb\u6377\u680f\u52a8\u753b\u65f6\u95f4");
      this.darkening.setChinese("\u6697\u5316");
      this.toggle.setChinese("\u6a21\u5757\u5207\u6362");
      this.onlyOne.setChinese("\u4ec5\u4e00\u6761");
      this.banner.setChinese("\u5207\u6362\u6a2a\u5e45");
      this.bannerFade.setChinese("\u6a2a\u5e45\u6e10\u9690");
      this.bannerHold.setChinese("\u6a2a\u5e45\u505c\u7559");
      this.bannerSound.setChinese("\u6a2a\u5e45\u58f0\u97f3");
      this.bannerSoundPitch.setChinese("\u6a2a\u5e45\u97f3\u8c03");
      this.keepHistory.setChinese("\u4fdd\u7559\u5386\u53f2");
      this.infiniteChat.setChinese("\u65e0\u9650\u804a\u5929");
      this.hideIndicator.setChinese("\u9690\u85cf\u6307\u793a\u5668");
      this.animationTime.setChinese("\u52a8\u753b\u65f6\u95f4");
      this.animateOffset.setChinese("\u504f\u79fb");
      this.fade.setChinese("\u6e10\u9690");
      this.loadGuiKey.setChinese("\u52a0\u8f7dUI\u5feb\u6377\u952e");
   }

   public boolean portalGui() {
      return this.isOn() && this.portalGui.getValue();
   }

   public boolean hotbar() {
      return this.isOn() && this.hotbar.getValue();
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }

   public static enum BannerStyle {
      iOS,
      Classic;
   }

   public static enum Page {
      Game,
      Gui,
      Misc,
      Notification,
      ChatHud;
   }

   public static enum StackDir {
      Down,
      Up;
   }

   public static enum Style {
      Mio,
      Debug,
      Lowercase,
      Normal,
      Future,
      Earth,
      Moon,
      Melon,
      Chinese,
      None;
   }
}
