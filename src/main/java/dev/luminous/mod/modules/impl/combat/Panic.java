package dev.luminous.mod.modules.impl.combat;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Panic extends Module {
   private final BooleanSetting restoreOnDisable;
   private final StringSetting whitelist;
   private final List<Module> disabledModules = new ArrayList<>();

   public Panic() {
      super("Panic", "\u7d27\u6025\u505c\u6b62\u6240\u6709\u529f\u80fd", Module.Category.Survival);
      this.setChinese("\u7d27\u6025\u505c\u6b62\u6240\u6709\u529f\u80fd");
      this.restoreOnDisable = this.add(new BooleanSetting("RestoreOnDisable", true));
      this.restoreOnDisable.setChinese("\u6062\u590d\u542f\u7528");
      this.whitelist = this.add(new StringSetting("Whitelist", "ClickGui,HUD,Offhand"));
      this.whitelist.setChinese("\u767d\u540d\u5355");
   }

   @Override
   public void onEnable() {
      this.disabledModules.clear();
      List<Module> var1 = this.getAllModules();
      String[] var2 = this.whitelist.getValue().split(",");
      ArrayList var3 = new ArrayList();

      for (String var7 : var2) {
         var3.add(var7.trim().toLowerCase());
      }

      for (Module var9 : var1) {
         if (var9 != this && !this.shouldWhitelist(var9, var3) && var9.isOn()) {
            this.disabledModules.add(var9);
            var9.disable();
         }
      }

      this.sendMessage("\u00a7c\u5df2\u7d27\u6025\u505c\u6b62 " + this.disabledModules.size() + " \u4e2a\u529f\u80fd");
   }

   @Override
   public void onDisable() {
      if (this.restoreOnDisable.getValue() && !this.disabledModules.isEmpty()) {
         int var1 = 0;

         for (Module var3 : this.disabledModules) {
            if (var3.isOff()) {
               var3.enable();
               var1++;
            }
         }

         this.sendMessage("\u00a7a\u5df2\u6062\u590d " + var1 + " \u4e2a\u529f\u80fd");
      }

      this.disabledModules.clear();
   }

   @Override
   public String getInfo() {
      return this.disabledModules.isEmpty() ? "" : this.disabledModules.size() + "";
   }

   private List<Module> getAllModules() {
      ArrayList var2 = new ArrayList();

      for (String var6 : new String[]{
         "AutoKit",
         "Bot",
         "Fonts",
         "NoTerrainScreen",
         "AutoCrystal",
         "Ambience",
         "AntiHunger",
         "AntiVoid",
         "AutoWalk",
         "VClip",
         "ExtraTab",
         "AntiWeak",
         "BedCrafter",
         "Friend",
         "AspectRatio",
         "ChunkESP",
         "Aura",
         "PistonCrystal",
         "AutoAnchor",
         "PhaseESP",
         "AutoArmor",
         "Breaker",
         "AutoLog",
         "AutoEZ",
         "SelfTrap",
         "Sorter",
         "AutoMend",
         "AutoPot",
         "AutoPush",
         "Offhand",
         "Nuker",
         "AutoTrap",
         "AutoWeb",
         "AutoTotemWhisper",
         "Blink",
         "ChorusControl",
         "BlockStrafe",
         "FastSwim",
         "Blocker",
         "Quiver",
         "BowBomb",
         "BreakESP",
         "Burrow",
         "Punctuation",
         "MaceSpoof",
         "CameraClip",
         "ClickGui",
         "InfiniteTrident",
         "ColorsModule",
         "AutoRegear",
         "AntiPhase",
         "Clip",
         "AntiCheat",
         "ItemsCounter",
         "Fov",
         "Criticals",
         "CevBreaker",
         "Crosshair",
         "Chams",
         "AntiPacket",
         "AutoReconnect",
         "ESP",
         "HoleESP",
         "MovementSync",
         "ElytraFly",
         "PacketLogger",
         "TeleportLogger",
         "SkinFlicker",
         "EntityControl",
         "NameTags",
         "ShulkerViewer",
         "PingSpoof",
         "FakePlayer",
         "Spammer",
         "MotionCamera",
         "HighLight",
         "FastFall",
         "FastWeb",
         "Flatten",
         "Fly",
         "Yaw",
         "Freecam",
         "FreeLook",
         "TimerModule",
         "Tips",
         "ClientSetting",
         "TextRadar",
         "HUD",
         "NoResourcePack",
         "RocketExtend",
         "HoleSnap",
         "LogoutSpots",
         "AutoTool",
         "Trajectories",
         "KillEffect",
         "AutoPearl",
         "AntiEffects",
         "NoFall",
         "NoRender",
         "NoSlow",
         "NoSound",
         "AirPlace",
         "Xray",
         "PacketEat",
         "PacketFly",
         "PacketMine",
         "PacketControl",
         "Phase",
         "PlaceRender",
         "InteractTweaks",
         "PopChams",
         "Replenish",
         "ServerLagger",
         "Scaffold",
         "ShaderModule",
         "AntiCrawl",
         "AntiRegear",
         "SafeWalk",
         "NoJumpDelay",
         "Speed",
         "Sprint",
         "Strafe",
         "Step",
         "Surround",
         "TotemParticle",
         "Velocity",
         "ViewModel",
         "XCarry",
         "Zoom"
      }) {
         Module var7 = this.getModuleInstance(var6);
         if (var7 != null) {
            var2.add(var7);
         }
      }

      return var2;
   }

   private Module getModuleInstance(String var1) {
      try {
         for (String var6 : new String[]{
            "dev.luminous.mod.modules.impl.combat.",
            "dev.luminous.mod.modules.impl.movement.",
            "dev.luminous.mod.modules.impl.player.",
            "dev.luminous.mod.modules.impl.render.",
            "dev.luminous.mod.modules.impl.misc.",
            "dev.luminous.mod.modules.impl.client.",
            "dev.luminous.mod.modules.impl.exploit."
         }) {
            try {
               String var7 = var6 + var1;
               Class var8 = Class.forName(var7);
               Field var9 = var8.getDeclaredField("INSTANCE");
               var9.setAccessible(true);
               Object var10 = var9.get(null);
               if (var10 instanceof Module) {
                  return (Module)var10;
               }
            } catch (NoSuchFieldException | ClassNotFoundException var11) {
            }
         }
      } catch (Exception var12) {
      }

      return null;
   }

   private boolean shouldWhitelist(Module var1, List<String> var2) {
      if (var1 == null) {
         return false;
      } else {
         String var3 = var1.getName().toLowerCase();

         for (String var5 : var2) {
            if (var3.equals(var5.toLowerCase())) {
               return true;
            }
         }

         return false;
      }
   }

   public void panicNow() {
      if (!this.isOn()) {
         this.enable();
      }
   }

   public void restoreNow() {
      if (this.isOn()) {
         this.disable();
      }
   }
}
