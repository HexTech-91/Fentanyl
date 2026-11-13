package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DamageNumber extends Module {
   private final EnumSetting<DamageNumber.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", DamageNumber.ColorMode.Sync));
   private final ColorSetting startColor = this.add(
      new ColorSetting("StartColor", new Color(255, 90, 90, 255), () -> this.colorMode.getValue() == DamageNumber.ColorMode.Custom)
   );
   private final ColorSetting endColor = this.add(
      new ColorSetting("EndColor", new Color(255, 220, 120, 40), () -> this.colorMode.getValue() == DamageNumber.ColorMode.Custom)
   );
   private final BooleanSetting onlyPlayers = this.add(new BooleanSetting("OnlyPlayers", true));
   private final SliderSetting lifeTime = this.add(new SliderSetting("LifeTime", 900.0, 200.0, 3000.0, 50.0));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.1, 0.4, 3.0, 0.05));
   private final SliderSetting rise = this.add(new SliderSetting("Rise", 0.9, 0.1, 2.5, 0.05));
   private final SliderSetting random = this.add(new SliderSetting("RandomOffset", 0.18, 0.0, 0.6, 0.01));
   private final SliderSetting range = this.add(new SliderSetting("TrackRange", 12.0, 2.0, 24.0, 0.5));
   private final Map<Integer, DamageNumber.TargetHealth> trackedTargets = new HashMap<>();
   private final List<DamageNumber.DamagePopup> popups = new ArrayList<>();
   private final DecimalFormat format = new DecimalFormat("0.0");

   public DamageNumber() {
      super("DamageNumber", Module.Category.Render);
      this.setChinese("\u4f24\u5bb3\u6570\u5b57");
      FeatureTranslations.localizeModule(this);
   }

   @Override
   public void onEnable() {
      this.trackedTargets.clear();
      this.popups.clear();
   }

   @Override
   public void onDisable() {
      this.trackedTargets.clear();
      this.popups.clear();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck() && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
         if (Criticals.getInteractType(packet) == InteractType.ATTACK) {
            if (Criticals.getEntity(packet) instanceof LivingEntity living) {
               if (!this.onlyPlayers.getValue() || living instanceof PlayerEntity) {
                  this.trackedTargets.put(living.getId(), new DamageNumber.TargetHealth(living, this.getHealth(living), new Timer()));
               }
            }
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         this.popups.removeIf(popup -> popup.timer.passedMs((long)this.lifeTime.getValue()));
         Iterator<Entry<Integer, DamageNumber.TargetHealth>> iterator = this.trackedTargets.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Integer, DamageNumber.TargetHealth> entry = iterator.next();
            DamageNumber.TargetHealth target = entry.getValue();
            LivingEntity living = target.entity;
            if (living == null || !living.isAlive() || living.isRemoved() || mc.player.distanceTo(living) > this.range.getValue()) {
               iterator.remove();
            } else if (target.timer.passedMs(1400.0)) {
               iterator.remove();
            } else {
               float currentHealth = this.getHealth(living);
               float delta = target.lastHealth - currentHealth;
               if (delta > 0.05F) {
                  this.popups.add(new DamageNumber.DamagePopup(this.getPopupPos(living), delta, new Timer()));
                  target.lastHealth = currentHealth;
                  target.timer.reset();
               } else if (currentHealth > target.lastHealth) {
                  target.lastHealth = currentHealth;
                  target.timer.reset();
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck() && !this.popups.isEmpty()) {
         long duration = (long)this.lifeTime.getValue();

         for (DamageNumber.DamagePopup popup : this.popups) {
            float progress = MathHelper.clamp((float)popup.timer.getMs() / (float)duration, 0.0F, 1.0F);
            Vec3d pos = popup.pos.add(0.0, this.rise.getValue() * progress, 0.0);
            Color color = this.getPopupColor(progress);
            Render3DUtil.drawText3D(Text.of(this.format.format(popup.damage)), pos.x, pos.y, pos.z, 0.0, 0.0, this.scale.getValue(), color.getRGB());
         }
      }
   }

   private Vec3d getPopupPos(LivingEntity living) {
      double xOffset = (Math.random() - 0.5) * this.random.getValue();
      double zOffset = (Math.random() - 0.5) * this.random.getValue();
      return new Vec3d(living.getX() + xOffset, living.getY() + living.getHeight() + 0.3, living.getZ() + zOffset);
   }

   private float getHealth(LivingEntity entity) {
      return entity.getHealth() + entity.getAbsorptionAmount();
   }

   private Color getPopupColor(float progress) {
      Color start = this.colorMode.getValue() == DamageNumber.ColorMode.Sync ? this.getHudColor(0) : this.startColor.getValue();
      Color end = this.colorMode.getValue() == DamageNumber.ColorMode.Sync ? this.getHudColor(180) : this.endColor.getValue();
      return ColorUtil.fadeColor(start, end, progress);
   }

   private Color getHudColor(int offset) {
      return HUD.INSTANCE != null ? new Color(HUD.INSTANCE.getColor(offset), true) : Color.WHITE;
   }

   private static enum ColorMode {
      Sync,
      Custom;
   }

   private record DamagePopup(Vec3d pos, float damage, Timer timer) {
   }

   private static class TargetHealth {
      private final LivingEntity entity;
      private float lastHealth;
      private final Timer timer;

      private TargetHealth(LivingEntity entity, float lastHealth, Timer timer) {
         this.entity = entity;
         this.lastHealth = lastHealth;
         this.timer = timer;
      }
   }
}
