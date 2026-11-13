package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class TimerModule extends Module {
   public static TimerModule INSTANCE;
   public final SliderSetting multiplier = this.add(new SliderSetting("Multiplier", 1.0, 0.1, 5.0, 0.01));
   public final BindSetting boostKey;
   public final SliderSetting boost;
   final DecimalFormat df;
   private final BooleanSetting tickShift;
   private final SliderSetting shiftTimer;
   private final SliderSetting accumulate;
   private final SliderSetting minAccumulate;
   private final BooleanSetting smooth;
   private final EnumSetting<Easing> ease;
   private final BooleanSetting reset;
   private final BooleanSetting indicator;
   private final ColorSetting work;
   private final ColorSetting charging;
   private final SliderSetting yOffset;
   private final Timer timer;
   private final Timer timer2;
   private final FadeUtils end;
   long lastMs;
   boolean moving;

   public TimerModule() {
      super("Timer", Module.Category.Player);
      this.multiplier.setChinese("\u901f\u5ea6");
      this.boostKey = this.add(new BindSetting("BoostKey", -1));
      this.boostKey.setChinese("\u6309\u4f4f\u952e");
      this.boost = this.add(new SliderSetting("Boost", 1.0, 0.1, 10.0, 0.01));
      this.boost.setChinese("\u6309\u952e\u65f6");
      this.df = new DecimalFormat("0.0");
      this.tickShift = this.add(new BooleanSetting("TickShift", true).setParent());
      this.shiftTimer = this.add(new SliderSetting("ShiftTimer", 2.0, 1.0, 10.0, 0.1, this.tickShift::isOpen));
      this.shiftTimer.setChinese("\u52a0\u901fTimer");
      this.accumulate = this.add(new SliderSetting("Accumulate", 2000.0, 1.0, 10000.0, 50.0, this.tickShift::isOpen).setSuffix("ms"));
      this.accumulate.setChinese("\u5145\u80fd");
      this.minAccumulate = this.add(new SliderSetting("MinAccumulate", 500.0, 1.0, 10000.0, 50.0, this.tickShift::isOpen).setSuffix("ms"));
      this.minAccumulate.setChinese("\u6700\u5c0f\u5145\u80fd");
      this.smooth = this.add(new BooleanSetting("Smooth", true, this.tickShift::isOpen).setParent());
      this.smooth.setChinese("\u5e73\u6ed1");
      this.ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.smooth.isOpen() && this.tickShift.isOpen()));
      this.ease.setChinese("\u7f13\u52a8");
      this.reset = this.add(new BooleanSetting("Reset", true, this.tickShift::isOpen));
      this.reset.setChinese("\u91cd\u7f6e");
      this.indicator = this.add(new BooleanSetting("Indicator", true, this.tickShift::isOpen).setParent());
      this.indicator.setChinese("\u6307\u793a\u5668");
      this.work = this.add(new ColorSetting("Work", new Color(0, 255, 0), () -> this.indicator.isOpen() && this.tickShift.isOpen()));
      this.work.setChinese("\u5b8c\u6210");
      this.charging = this.add(new ColorSetting("Charging", new Color(255, 0, 0), () -> this.indicator.isOpen() && this.tickShift.isOpen()));
      this.charging.setChinese("\u5145\u80fd\u4e2d");
      this.yOffset = this.add(new SliderSetting("YOffset", 0.0, -200.0, 200.0, 1.0, () -> this.indicator.isOpen() && this.tickShift.isOpen()));
      this.yOffset.setChinese("Y\u504f\u79fb");
      this.timer = new Timer();
      this.timer2 = new Timer();
      this.end = new FadeUtils(500L);
      this.lastMs = 0L;
      this.moving = false;
      this.setChinese("\u65f6\u95f4\u52a0\u901f");
      INSTANCE = this;
      this.tickShift.setChinese("Tick\u53d8\u901f");
   }

   @Override
   public void onDisable() {
      Alien.TIMER.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      Alien.TIMER.tryReset();
   }

   @Override
   public void onEnable() {
      Alien.TIMER.reset();
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.tickShift.getValue()) {
         this.timer.setMs(Math.min(Math.max(0L, this.timer.getMs()), (long)this.accumulate.getValueInt()));
         if (MovementUtil.isMoving() && !EntityUtil.isInsideBlock()) {
            if (!this.moving) {
               if (this.timer.passedMs(this.minAccumulate.getValue())) {
                  this.timer2.reset();
                  this.lastMs = this.timer.getMs();
               } else {
                  this.lastMs = 0L;
               }

               this.moving = true;
            }

            this.timer.reset();
            if (this.timer2.passed(this.lastMs)) {
               Alien.TIMER.reset();
            } else if (this.smooth.getValue()) {
               double timer = Alien.TIMER.getDefault()
                  + (1.0 - this.end.ease(this.ease.getValue())) * (this.shiftTimer.getValueFloat() - 1.0F) * (this.lastMs / this.accumulate.getValue());
               Alien.TIMER.set((float)Math.max((double)Alien.TIMER.getDefault(), timer));
            } else {
               Alien.TIMER.set(this.shiftTimer.getValueFloat());
            }
         } else {
            if (this.moving) {
               Alien.TIMER.reset();
               if (this.reset.getValue()) {
                  this.timer.reset();
               } else {
                  this.timer.setMs(Math.max(this.lastMs - this.timer2.getMs(), 0L));
               }

               this.moving = false;
            }

            this.end.setLength(this.timer.getMs());
            this.end.reset();
         }

         if (this.indicator.getValue()) {
            double current = this.moving ? Math.max(this.lastMs - this.timer2.getMs(), 0L) : this.timer.getMs();
            boolean completed = this.moving && current > 0.0 || current >= this.minAccumulate.getValueInt();
            double max = this.accumulate.getValue();
            String text = this.df.format(current / max * 100.0) + "%";
            drawContext.drawText(
               mc.textRenderer,
               text,
               mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2,
               mc.getWindow().getScaledHeight() / 2 + 9 - this.yOffset.getValueInt(),
               completed ? this.work.getValue().getRGB() : this.charging.getValue().getRGB(),
               true
            );
         }
      }
   }

   @Override
   public String getInfo() {
      if (!this.tickShift.getValue()) {
         return null;
      } else {
         double current = this.moving ? Math.max(this.lastMs - this.timer2.getMs(), 0L) : this.timer.getMs();
         double max = this.accumulate.getValue();
         double value = Math.min(current / max * 100.0, 100.0);
         return this.df.format(value) + "%";
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lastMs = 0L;
      }
   }
}
