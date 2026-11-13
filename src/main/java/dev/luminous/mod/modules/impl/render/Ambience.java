package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class Ambience extends Module {
   public static Ambience INSTANCE;
   public final ColorSetting filter = this.add(new ColorSetting("Filter", new Color(255, 255, 255, 20)).injectBoolean(false));
   public final ColorSetting worldColor;
   public final BooleanSetting customTime;
   public final SliderSetting time;
   public final ColorSetting fog;
   public final ColorSetting sky;
   public final ColorSetting cloud;
   public final ColorSetting dimensionColor;
   public final BooleanSetting fogDistance;
   public final SliderSetting fogStart;
   public final SliderSetting fogEnd;
   public final BooleanSetting fullBright;
   public final BooleanSetting forceOverworld;
   public final BooleanSetting customLuminance;
   public final SliderSetting luminance;
   long oldTime;

   public Ambience() {
      super("Ambience", "Custom ambience", Module.Category.Render);
      this.filter.setChinese("\u6ee4\u955c");
      this.worldColor = this.add(new ColorSetting("WorldColor", new Color(-1, true)).injectBoolean(true));
      this.worldColor.setChinese("\u4e16\u754c\u989c\u8272");
      this.customTime = this.add(new BooleanSetting("CustomTime", false).setParent());
      this.customTime.setChinese("\u81ea\u5b9a\u4e49\u65f6\u95f4");
      this.time = this.add(new SliderSetting("Time", 0, 0, 24000, this.customTime::isOpen));
      this.time.setChinese("\u65f6\u95f4");
      this.fog = this.add(new ColorSetting("Fog", new Color(13401557)).injectBoolean(false));
      this.fog.setChinese("\u96fe\u989c\u8272");
      this.sky = this.add(new ColorSetting("Sky", new Color(0)).injectBoolean(false));
      this.sky.setChinese("\u5929\u7a7a\u989c\u8272");
      this.cloud = this.add(new ColorSetting("Cloud", new Color(0)).injectBoolean(false));
      this.cloud.setChinese("\u4e91\u989c\u8272");
      this.dimensionColor = this.add(new ColorSetting("DimensionColor", new Color(100, 150, 255)).injectBoolean(false));
      this.dimensionColor.setChinese("\u7ef4\u5ea6\u989c\u8272");
      this.fogDistance = this.add(new BooleanSetting("FogDistance", false).setParent());
      this.fogDistance.setChinese("\u96fe\u8ddd\u79bb");
      this.fogStart = this.add(new SliderSetting("FogStart", 50, 0, 1000, this.fogDistance::isOpen));
      this.fogStart.setChinese("\u96fe\u8d77\u59cb");
      this.fogEnd = this.add(new SliderSetting("FogEnd", 100, 0, 1000, this.fogDistance::isOpen));
      this.fogEnd.setChinese("\u96fe\u7ed3\u675f");
      this.fullBright = this.add(new BooleanSetting("FullBright", false));
      this.fullBright.setChinese("\u5168\u4eae");
      this.forceOverworld = this.add(new BooleanSetting("ForceOverworld", false));
      this.forceOverworld.setChinese("\u5f3a\u5236\u4e3b\u4e16\u754c");
      this.customLuminance = this.add(new BooleanSetting("CustomLuminance", false).setParent().injectTask(() -> {
         if (!nullCheck()) {
            mc.worldRenderer.reload();
         }
      }));
      this.customLuminance.setChinese("\u81ea\u5b9a\u4e49\u4eae\u5ea6");
      this.luminance = this.add(new SliderSetting("Luminance", 15, 0, 15, this.customLuminance::isOpen).injectTask(() -> {
         if (!nullCheck() && this.customLuminance.getValue()) {
            mc.worldRenderer.reload();
         }
      }));
      this.luminance.setChinese("\u4eae\u5ea6");
      this.setChinese("\u81ea\u5b9a\u4e49\u73af\u5883");
      INSTANCE = this;
   }

   @EventListener
   public void onRender2D(PreRender2DEvent event) {
      if (this.filter.booleanValue) {
         event.drawContext.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), this.filter.getValue().getRGB());
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.customTime.getValue()) {
         mc.world.setTimeOfDay((long)this.time.getValue());
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.oldTime = mc.world.getTimeOfDay();
         if (this.customTime.getValue()) {
            mc.world.setTimeOfDay((long)this.time.getValue());
         }
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         mc.world.setTimeOfDay(this.oldTime);
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.getValue()) {
         this.oldTime = ((WorldTimeUpdateS2CPacket)event.getPacket()).getTime();
         event.cancel();
      }
   }
}
