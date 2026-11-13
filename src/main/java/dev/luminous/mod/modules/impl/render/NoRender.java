package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.ParticleEvent;
import dev.luminous.api.events.impl.RenderEntityEvent;
import dev.luminous.api.events.impl.TickEntityEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.ElderGuardianAppearanceParticle;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.client.particle.FireworksSparkParticle.FireworkParticle;
import net.minecraft.client.particle.FireworksSparkParticle.Flash;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;

public class NoRender extends Module {
   public static NoRender INSTANCE;
   public final BooleanSetting fastItem = this.add(new BooleanSetting("FastItem", false).setParent());
   public final BooleanSetting castShadow;
   public final BooleanSetting renderSidesOfItems;
   public final BooleanSetting potionsIcon;
   public final BooleanSetting weather;
   public final BooleanSetting invisible;
   public final BooleanSetting lightsUpdate;
   public final BooleanSetting potions;
   public final BooleanSetting xp;
   public final BooleanSetting arrows;
   public final BooleanSetting eggs;
   public final BooleanSetting item;
   public final BooleanSetting armorParts;
   public final BooleanSetting armorTrim;
   public final BooleanSetting armorGlint;
   public final BooleanSetting hurtCam;
   public final BooleanSetting fireOverlay;
   public final BooleanSetting waterOverlay;
   public final BooleanSetting blockOverlay;
   public final BooleanSetting portal;
   public final BooleanSetting totem;
   public final BooleanSetting nausea;
   public final BooleanSetting blindness;
   public final BooleanSetting fog;
   public final BooleanSetting darkness;
   public final BooleanSetting fireEntity;
   public final BooleanSetting antiTitle;
   public final BooleanSetting antiPlayerCollision;
   public final BooleanSetting effect;
   public final BooleanSetting elderGuardian;
   public final BooleanSetting explosions;
   public final BooleanSetting campFire;
   public final BooleanSetting fireworks;
   public final BooleanSetting guiToast;

   public NoRender() {
      super("NoRender", "Disables all overlays and potion effects.", Module.Category.Render);
      this.fastItem.setChinese("2D\u7269\u54c1");
      this.castShadow = this.add(new BooleanSetting("CastShadow", true, this.fastItem::isOpen));
      this.castShadow.setChinese("\u6295\u5c04\u9634\u5f71");
      this.renderSidesOfItems = this.add(new BooleanSetting("RenderSidesOfItems", false, this.fastItem::isOpen));
      this.renderSidesOfItems.setChinese("\u6e32\u67d3\u7269\u54c1\u4fa7\u9762");
      this.potionsIcon = this.add(new BooleanSetting("PotionsIcon", false));
      this.potionsIcon.setChinese("\u836f\u6c34\u56fe\u6807");
      this.weather = this.add(new BooleanSetting("Weather", true));
      this.weather.setChinese("\u5929\u6c14");
      this.invisible = this.add(new BooleanSetting("Invisible", false));
      this.invisible.setChinese("\u9690\u8eab");
      this.lightsUpdate = this.add(new BooleanSetting("LightsUpdate", false));
      this.lightsUpdate.setChinese("\u5149\u7167\u66f4\u65b0");
      this.potions = this.add(new BooleanSetting("Potions", true));
      this.potions.setChinese("\u836f\u6c34");
      this.xp = this.add(new BooleanSetting("Xp", true));
      this.xp.setChinese("\u7ecf\u9a8c");
      this.arrows = this.add(new BooleanSetting("Arrows", false));
      this.arrows.setChinese("\u7bad\u77e2");
      this.eggs = this.add(new BooleanSetting("Eggs", false));
      this.eggs.setChinese("\u9e21\u86cb");
      this.item = this.add(new BooleanSetting("Item", false));
      this.item.setChinese("\u7269\u54c1");
      this.armorParts = this.add(new BooleanSetting("ArmorParts", false));
      this.armorParts.setChinese("\u76d4\u7532\u90e8\u4ef6");
      this.armorTrim = this.add(new BooleanSetting("ArmorTrim", false));
      this.armorTrim.setChinese("\u76d4\u7532\u7eb9\u9970");
      this.armorGlint = this.add(new BooleanSetting("ArmorGlint", false));
      this.armorGlint.setChinese("\u76d4\u7532\u5149\u6548");
      this.hurtCam = this.add(new BooleanSetting("HurtCam", true));
      this.hurtCam.setChinese("\u4f24\u5bb3\u6296\u52a8");
      this.fireOverlay = this.add(new BooleanSetting("FireOverlay", true));
      this.fireOverlay.setChinese("\u706b\u7130\u8986\u76d6");
      this.waterOverlay = this.add(new BooleanSetting("WaterOverlay", true));
      this.waterOverlay.setChinese("\u6c34\u4e0b\u8986\u76d6");
      this.blockOverlay = this.add(new BooleanSetting("BlockOverlay", true));
      this.blockOverlay.setChinese("\u65b9\u5757\u8986\u76d6");
      this.portal = this.add(new BooleanSetting("Portal", true));
      this.portal.setChinese("\u4f20\u9001\u95e8");
      this.totem = this.add(new BooleanSetting("Totem", true));
      this.totem.setChinese("\u56fe\u817e");
      this.nausea = this.add(new BooleanSetting("Nausea", true));
      this.nausea.setChinese("\u53cd\u80c3");
      this.blindness = this.add(new BooleanSetting("Blindness", true));
      this.blindness.setChinese("\u5931\u660e");
      this.fog = this.add(new BooleanSetting("Fog", false));
      this.fog.setChinese("\u96fe");
      this.darkness = this.add(new BooleanSetting("Darkness", true));
      this.darkness.setChinese("\u9ed1\u6697");
      this.fireEntity = this.add(new BooleanSetting("FireEntity", true));
      this.fireEntity.setChinese("\u5b9e\u4f53\u706b\u7130");
      this.antiTitle = this.add(new BooleanSetting("AntiTitle", false));
      this.antiTitle.setChinese("\u6807\u9898");
      this.antiPlayerCollision = this.add(new BooleanSetting("AntiPlayerCollision", true));
      this.antiPlayerCollision.setChinese("\u73a9\u5bb6\u78b0\u649e");
      this.effect = this.add(new BooleanSetting("Effect", true));
      this.effect.setChinese("\u6548\u679c");
      this.elderGuardian = this.add(new BooleanSetting("ElderGuardian", false));
      this.elderGuardian.setChinese("\u5b88\u536b\u8005");
      this.explosions = this.add(new BooleanSetting("Explosions", true));
      this.explosions.setChinese("\u7206\u70b8");
      this.campFire = this.add(new BooleanSetting("CampFire", false));
      this.campFire.setChinese("\u8425\u706b");
      this.fireworks = this.add(new BooleanSetting("Fireworks", false));
      this.fireworks.setChinese("\u70df\u82b1");
      this.guiToast = this.add(new BooleanSetting("GuiToast", false));
      this.guiToast.setChinese("\u754c\u9762\u63d0\u793a");
      this.setChinese("\u7981\u7528\u6e32\u67d3");
      INSTANCE = this;
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof TitleS2CPacket && this.antiTitle.getValue()) {
         event.setCancelled(true);
      }
   }

   @EventListener
   public void onRender(TickEntityEvent event) {
      this.cancelEvent(event.getEntity(), event);
   }

   @EventListener
   public void onRender(RenderEntityEvent event) {
      this.cancelEvent(event.getEntity(), event);
   }

   private void cancelEvent(Entity entity, Event event) {
      if (entity instanceof PotionEntity && this.potions.getValue()) {
         event.cancel();
      } else if (entity instanceof ExperienceBottleEntity && this.xp.getValue()) {
         event.cancel();
      } else if (entity instanceof ArrowEntity && this.arrows.getValue()) {
         event.cancel();
      } else if (entity instanceof EggEntity && this.eggs.getValue()) {
         event.cancel();
      } else if (entity instanceof ItemEntity && this.item.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onParticle(ParticleEvent event) {
      if (this.elderGuardian.getValue() && event.particle instanceof ElderGuardianAppearanceParticle) {
         event.cancel();
      } else if (this.explosions.getValue() && event.particle instanceof ExplosionLargeParticle) {
         event.cancel();
      } else if (this.campFire.getValue() && event.particle instanceof CampfireSmokeParticle) {
         event.cancel();
      } else if (!this.fireworks.getValue() || !(event.particle instanceof FireworkParticle) && !(event.particle instanceof Flash)) {
         if (this.effect.getValue() && event.particle instanceof SpellParticle) {
            event.cancel();
         }
      } else {
         event.cancel();
      }
   }
}
