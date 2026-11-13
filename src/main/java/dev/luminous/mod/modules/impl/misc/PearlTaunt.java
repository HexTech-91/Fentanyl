package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;

public final class PearlTaunt extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final StringSetting message = this.add(
      new StringSetting("Message", "{player}\u662f\u73cd\u73e0\u8dd1\u8def\u5927\u86c7\uff0c\u6211\u5df2\u5413\u54ed")
   );
   private final SliderSetting delay = this.add(new SliderSetting("SendDelay", 0.0, 0.0, 30.0, 0.1).setSuffix("s"));
   private final SliderSetting suffix = this.add(new SliderSetting("Suffix", 0.0, 0.0, 20.0, 1.0));
   private final BooleanSetting privateMode = this.add(new BooleanSetting("Private", false));
   private final StringSetting privateMessage = this.add(
      new StringSetting("PrivateMessage", "\u7adf\u7136\u662f\u73cd\u73e0\u8dd1\u8def\u5927\u86c7\u6211\u5413\u54ed\u4e86", this.privateMode::getValue)
   );
   private final Random random = new Random();
   private final Set<Integer> visiblePearls = new HashSet<>();
   private final Map<Integer, String> owners = new HashMap<>();
   private final Map<String, Long> pending = new HashMap<>();
   private boolean primed;

   public PearlTaunt() {
      super("PearlTaunt", "Taunts players when their ender pearl disappears from view.", Module.Category.Misc);
      this.message.setChinese("\u5632\u8bbd\u8bed");
      this.delay.setChinese("\u53d1\u9001\u5ef6\u8fdf");
      this.suffix.setChinese("\u540e\u7f00\u6570\u91cf");
      this.privateMode.setChinese("\u79c1\u4fe1\u6a21\u5f0f");
      this.privateMessage.setChinese("\u79c1\u4fe1\u5632\u8bbd\u8bed");
      this.setChinese("\u73cd\u73e0\u5632\u8bbd");
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (!nullCheck() && mc.world != null && mc.player != null && Alien.THREAD != null) {
         try {
            HashSet var2 = new HashSet();

            for (Entity var4 : Alien.THREAD.getEntities()) {
               if (var4 instanceof EnderPearlEntity var5
                  && var5.isAlive()
                  && var5.getOwner() instanceof PlayerEntity var7
                  && var7 != mc.player
                  && !(mc.player.squaredDistanceTo(var7) > 4096.0)
                  && mc.player.canSee(var7)) {
                  int var8 = var5.getId();
                  var2.add(var8);
                  this.owners.put(var8, var7.getGameProfile().getName());
               }
            }

            if (this.primed) {
               for (Integer var12 : this.visiblePearls) {
                  if (!var2.contains(var12)) {
                     String var13 = this.owners.remove(var12);
                     if (var13 != null) {
                        this.pending.putIfAbsent(var13, System.currentTimeMillis());
                     }
                  }
               }
            }

            this.visiblePearls.clear();
            this.visiblePearls.addAll(var2);
            this.primed = true;
            long var11 = System.currentTimeMillis();
            Iterator var14 = this.pending.entrySet().iterator();

            while (var14.hasNext()) {
               Entry var15 = (Entry)var14.next();
               if (var11 - (Long)var15.getValue() >= (long)(this.delay.getValue() * 1000.0)) {
                  this.send((String)var15.getKey());
                  var14.remove();
               }
            }
         } catch (RuntimeException var9) {
         }
      }
   }

   private void send(String var1) {
      if (mc.player != null && mc.player.networkHandler != null && var1 != null && !var1.isBlank()) {
         if (this.privateMode.getValue()) {
            mc.player.networkHandler.sendChatMessage("/w " + var1 + " " + this.privateMessage.getValue());
         } else {
            String var2 = this.message.getValue().replace("{player}", var1).replace("\u67d0\u67d0", var1);
            if (!this.message.getValue().contains("{player}") && !this.message.getValue().contains("\u67d0\u67d0")) {
               var2 = var1 + " " + var2;
            }

            String var3 = this.randomChars(this.suffix.getValueInt());
            if (!var3.isEmpty()) {
               var2 = var2 + " " + var3;
            }

            mc.player.networkHandler.sendChatMessage(var2);
         }
      }
   }

   private String randomChars(int var1) {
      StringBuilder var2 = new StringBuilder(Math.max(0, var1));

      for (int var3 = 0; var3 < var1; var3++) {
         var2.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length()))
         );
      }

      return var2.toString();
   }

   @Override
   public void onDisable() {
      this.visiblePearls.clear();
      this.owners.clear();
      this.pending.clear();
      this.primed = false;
   }
}
