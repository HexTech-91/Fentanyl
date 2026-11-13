package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.tclick.TClickGuiScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import dev.luminous.mod.modules.impl.client.FakeCoords;
import dev.luminous.mod.modules.impl.client.Fonts;
import dev.luminous.mod.modules.impl.client.Friends;
import dev.luminous.mod.modules.impl.client.GUICharacter;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.client.HudEditor;
import dev.luminous.mod.modules.impl.client.ItemsCounter;
import dev.luminous.mod.modules.impl.client.PlayerModule;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.impl.client.WinRateAdvisor;
import dev.luminous.mod.modules.impl.combat.AimBot;
import dev.luminous.mod.modules.impl.combat.AntiCrawl;
import dev.luminous.mod.modules.impl.combat.AntiPhase;
import dev.luminous.mod.modules.impl.combat.AntiPistonCrystal;
import dev.luminous.mod.modules.impl.combat.AntiPush;
import dev.luminous.mod.modules.impl.combat.AntiRegear;
import dev.luminous.mod.modules.impl.combat.AntiWeak;
import dev.luminous.mod.modules.impl.combat.Aura;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoButton;
import dev.luminous.mod.modules.impl.combat.AutoCrossbowAim;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.AutoMaceMinus;
import dev.luminous.mod.modules.impl.combat.AutoMend;
import dev.luminous.mod.modules.impl.combat.AutoPush;
import dev.luminous.mod.modules.impl.combat.AutoRegear;
import dev.luminous.mod.modules.impl.combat.AutoTotemWhisper;
import dev.luminous.mod.modules.impl.combat.AutoTrap;
import dev.luminous.mod.modules.impl.combat.AutoTrapDoor;
import dev.luminous.mod.modules.impl.combat.AutoWeb;
import dev.luminous.mod.modules.impl.combat.Blocker;
import dev.luminous.mod.modules.impl.combat.Breaker;
import dev.luminous.mod.modules.impl.combat.Burrow;
import dev.luminous.mod.modules.impl.combat.CevBreaker;
import dev.luminous.mod.modules.impl.combat.Chase;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.combat.CrossbowRapidFire;
import dev.luminous.mod.modules.impl.combat.ElytraChaser;
import dev.luminous.mod.modules.impl.combat.EnemyMark;
import dev.luminous.mod.modules.impl.combat.Follower;
import dev.luminous.mod.modules.impl.combat.ForceEnchantedApple;
import dev.luminous.mod.modules.impl.combat.FuckBurrow;
import dev.luminous.mod.modules.impl.combat.IQBoost;
import dev.luminous.mod.modules.impl.combat.MaceGodPlus;
import dev.luminous.mod.modules.impl.combat.Offhand;
import dev.luminous.mod.modules.impl.combat.Panic;
import dev.luminous.mod.modules.impl.combat.PearlBlocker;
import dev.luminous.mod.modules.impl.combat.PistonCrystal;
import dev.luminous.mod.modules.impl.combat.Refill;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.combat.SpearHunter;
import dev.luminous.mod.modules.impl.combat.Surround;
import dev.luminous.mod.modules.impl.combat.TPMace;
import dev.luminous.mod.modules.impl.combat.VoidHammer;
import dev.luminous.mod.modules.impl.exploit.AntiHunger;
import dev.luminous.mod.modules.impl.exploit.AntiPacket;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.impl.exploit.ChorusControl;
import dev.luminous.mod.modules.impl.exploit.ChunkESP;
import dev.luminous.mod.modules.impl.exploit.Clip;
import dev.luminous.mod.modules.impl.exploit.InfiniteTrident;
import dev.luminous.mod.modules.impl.exploit.MaceSpoof;
import dev.luminous.mod.modules.impl.exploit.NoResourcePack;
import dev.luminous.mod.modules.impl.exploit.PacketControl;
import dev.luminous.mod.modules.impl.exploit.Phase;
import dev.luminous.mod.modules.impl.exploit.PingSpoof;
import dev.luminous.mod.modules.impl.exploit.RocketExtend;
import dev.luminous.mod.modules.impl.exploit.ServerLagger;
import dev.luminous.mod.modules.impl.exploit.SkinFlicker;
import dev.luminous.mod.modules.impl.exploit.TeleportLogger;
import dev.luminous.mod.modules.impl.exploit.WindChargeMace;
import dev.luminous.mod.modules.impl.exploit.XCarry;
import dev.luminous.mod.modules.impl.hud.Armor;
import dev.luminous.mod.modules.impl.hud.InGameHud;
import dev.luminous.mod.modules.impl.hud.InventoryViewer;
import dev.luminous.mod.modules.impl.hud.NoirLive2D;
import dev.luminous.mod.modules.impl.hud.NotificationHud;
import dev.luminous.mod.modules.impl.hud.PlayerDoll;
import dev.luminous.mod.modules.impl.hud.PotionHud;
import dev.luminous.mod.modules.impl.hud.Radar;
import dev.luminous.mod.modules.impl.hud.Totem;
import dev.luminous.mod.modules.impl.hud.Turtle;
import dev.luminous.mod.modules.impl.hud.WaterMark;
import dev.luminous.mod.modules.impl.misc.AutoDropShulker;
import dev.luminous.mod.modules.impl.misc.AutoEZ;
import dev.luminous.mod.modules.impl.misc.AutoEnderChestPlus;
import dev.luminous.mod.modules.impl.misc.AutoFlirt;
import dev.luminous.mod.modules.impl.misc.AutoFuck;
import dev.luminous.mod.modules.impl.misc.AutoHelp;
import dev.luminous.mod.modules.impl.misc.AutoKit;
import dev.luminous.mod.modules.impl.misc.AutoKouZi;
import dev.luminous.mod.modules.impl.misc.AutoLog;
import dev.luminous.mod.modules.impl.misc.AutoLogin;
import dev.luminous.mod.modules.impl.misc.AutoReconnect;
import dev.luminous.mod.modules.impl.misc.AutoSanZiJing;
import dev.luminous.mod.modules.impl.misc.AutoSpammer;
import dev.luminous.mod.modules.impl.misc.AutoWither;
import dev.luminous.mod.modules.impl.misc.BedCrafter;
import dev.luminous.mod.modules.impl.misc.ChatCensor;
import dev.luminous.mod.modules.impl.misc.DeathTauntTeam;
import dev.luminous.mod.modules.impl.misc.ExtraTab;
import dev.luminous.mod.modules.impl.misc.FakeFPS;
import dev.luminous.mod.modules.impl.misc.FakePlayer;
import dev.luminous.mod.modules.impl.misc.FanqieReader;
import dev.luminous.mod.modules.impl.misc.Friend;
import dev.luminous.mod.modules.impl.misc.KillEffect;
import dev.luminous.mod.modules.impl.misc.LogEz;
import dev.luminous.mod.modules.impl.misc.MessagePrefix;
import dev.luminous.mod.modules.impl.misc.MessageSuffix;
import dev.luminous.mod.modules.impl.misc.MioFriendSync;
import dev.luminous.mod.modules.impl.misc.Music;
import dev.luminous.mod.modules.impl.misc.NoSound;
import dev.luminous.mod.modules.impl.misc.NoTerrainScreen;
import dev.luminous.mod.modules.impl.misc.Nuker;
import dev.luminous.mod.modules.impl.misc.PacketLogger;
import dev.luminous.mod.modules.impl.misc.PearlTaunt;
import dev.luminous.mod.modules.impl.misc.PopEz;
import dev.luminous.mod.modules.impl.misc.Punctuation;
import dev.luminous.mod.modules.impl.misc.RussianPunctuation;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import dev.luminous.mod.modules.impl.misc.Spammer;
import dev.luminous.mod.modules.impl.misc.TeammateTaunt;
import dev.luminous.mod.modules.impl.misc.Teams;
import dev.luminous.mod.modules.impl.misc.Tips;
import dev.luminous.mod.modules.impl.misc.TotemSound;
import dev.luminous.mod.modules.impl.misc.Tp;
import dev.luminous.mod.modules.impl.misc.WallTaunt;
import dev.luminous.mod.modules.impl.movement.AntiVoid;
import dev.luminous.mod.modules.impl.movement.AutoFollow;
import dev.luminous.mod.modules.impl.movement.AutoTame;
import dev.luminous.mod.modules.impl.movement.AutoWalk;
import dev.luminous.mod.modules.impl.movement.BlockStrafe;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.FastFall;
import dev.luminous.mod.modules.impl.movement.FastSwim;
import dev.luminous.mod.modules.impl.movement.FastWeb;
import dev.luminous.mod.modules.impl.movement.Flatten;
import dev.luminous.mod.modules.impl.movement.Fly;
import dev.luminous.mod.modules.impl.movement.GrimChase;
import dev.luminous.mod.modules.impl.movement.HighJumpPlus;
import dev.luminous.mod.modules.impl.movement.HoleSnap;
import dev.luminous.mod.modules.impl.movement.LongJump;
import dev.luminous.mod.modules.impl.movement.MovementSync;
import dev.luminous.mod.modules.impl.movement.NoJumpDelay;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.PacketFly;
import dev.luminous.mod.modules.impl.movement.SafeWalk;
import dev.luminous.mod.modules.impl.movement.Scaffold;
import dev.luminous.mod.modules.impl.movement.Speed;
import dev.luminous.mod.modules.impl.movement.Sprint;
import dev.luminous.mod.modules.impl.movement.Step;
import dev.luminous.mod.modules.impl.movement.Strafe;
import dev.luminous.mod.modules.impl.movement.TPLowerBedrock;
import dev.luminous.mod.modules.impl.movement.TpChase;
import dev.luminous.mod.modules.impl.movement.VClip;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.impl.player.AntiEffects;
import dev.luminous.mod.modules.impl.player.AutoArmor;
import dev.luminous.mod.modules.impl.player.AutoDropArmor;
import dev.luminous.mod.modules.impl.player.AutoNetherPortal;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.impl.player.AutoPot;
import dev.luminous.mod.modules.impl.player.AutoRotate;
import dev.luminous.mod.modules.impl.player.AutoSuicide;
import dev.luminous.mod.modules.impl.player.AutoTool;
import dev.luminous.mod.modules.impl.player.FreeLook;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import dev.luminous.mod.modules.impl.player.Macro;
import dev.luminous.mod.modules.impl.player.NoFall;
import dev.luminous.mod.modules.impl.player.PacketEat;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.player.Replenish;
import dev.luminous.mod.modules.impl.player.Sorter;
import dev.luminous.mod.modules.impl.player.TimerModule;
import dev.luminous.mod.modules.impl.player.Yaw;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.AspectRatio;
import dev.luminous.mod.modules.impl.render.AttackSlash;
import dev.luminous.mod.modules.impl.render.BedrockClip;
import dev.luminous.mod.modules.impl.render.BreakESP;
import dev.luminous.mod.modules.impl.render.CameraClip;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.CircularStatusHud;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.CrystalRender;
import dev.luminous.mod.modules.impl.render.CustomWeather;
import dev.luminous.mod.modules.impl.render.DamageNumber;
import dev.luminous.mod.modules.impl.render.ESP;
import dev.luminous.mod.modules.impl.render.EnemyInfo;
import dev.luminous.mod.modules.impl.render.EveryonePig;
import dev.luminous.mod.modules.impl.render.FireFlies;
import dev.luminous.mod.modules.impl.render.Fov;
import dev.luminous.mod.modules.impl.render.HealthBarOptimization;
import dev.luminous.mod.modules.impl.render.HealthWarning;
import dev.luminous.mod.modules.impl.render.HighLight;
import dev.luminous.mod.modules.impl.render.HoleESP;
import dev.luminous.mod.modules.impl.render.JumpCircle;
import dev.luminous.mod.modules.impl.render.Keystrokes;
import dev.luminous.mod.modules.impl.render.LogoutSpots;
import dev.luminous.mod.modules.impl.render.LowArmorWarning;
import dev.luminous.mod.modules.impl.render.LowHealthWarning;
import dev.luminous.mod.modules.impl.render.MaceEffect;
import dev.luminous.mod.modules.impl.render.MotionCamera;
import dev.luminous.mod.modules.impl.render.NameTags;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.Particles;
import dev.luminous.mod.modules.impl.render.PearlPredict;
import dev.luminous.mod.modules.impl.render.PhaseESP;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.impl.render.PlayerInfo;
import dev.luminous.mod.modules.impl.render.PopChams;
import dev.luminous.mod.modules.impl.render.PotionHUD;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import dev.luminous.mod.modules.impl.render.Skybox;
import dev.luminous.mod.modules.impl.render.SpearFix;
import dev.luminous.mod.modules.impl.render.TargetESP;
import dev.luminous.mod.modules.impl.render.TotemParticle;
import dev.luminous.mod.modules.impl.render.Trajectories;
import dev.luminous.mod.modules.impl.render.TwoDESP;
import dev.luminous.mod.modules.impl.render.ViewModel;
import dev.luminous.mod.modules.impl.render.Xray;
import dev.luminous.mod.modules.impl.render.Zoom;
import dev.luminous.mod.modules.impl.survival.AutoRide;
import dev.luminous.mod.modules.impl.survival.AutoTrash;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.opengl.GL11;

public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList<>();
   private final ArrayList<ModuleManager.ToggleBanner> toggleBanners = new ArrayList<>();
   private boolean notificationMouseWasDown = false;
   private boolean interactiveToggleInBanner = false;

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.addModule(new Panic());
      this.addModule(new AutoKit());
      this.addModule(new AutoLogin());
      this.addModule(new AutoEnderChestPlus());
      this.addModule(new EnemyMark());
      this.addModule(new HealthWarning());
      this.addModule(new Fonts());
      this.addModule(new NoTerrainScreen());
      this.addModule(new WindChargeMace());
      this.addModule(new Music());
      this.addModule(new FanqieReader());
      this.addModule(new Ambience());
      this.addModule(new Skybox());
      this.addModule(new AutoTrash());
      this.addModule(new AutoRide());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new AutoTame());
      this.addModule(new AutoWalk());
      this.addModule(new VClip());
      this.addModule(new ExtraTab());
      this.addModule(new AntiWeak());
      this.addModule(new BedCrafter());
      this.addModule(new Friend());
      this.addModule(new AspectRatio());
      this.addModule(new ChunkESP());
      this.addModule(new Aura());
      this.addModule(new PistonCrystal());
      this.addModule(new AntiPistonCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new PhaseESP());
      this.addModule(new AutoArmor());
      this.addModule(new AutoDropArmor());
      this.addModule(new Breaker());
      this.addModule(new AutoLog());
      this.addModule(new AutoEZ());
      this.addModule(new ChatCensor());
      this.addModule(new PopEz());
      this.addModule(new SelfTrap());
      this.addModule(new Sorter());
      this.addModule(new AutoMend());
      this.addModule(new AutoMaceMinus());
      this.addModule(new AutoPot());
      this.addModule(new AutoRotate());
      this.addModule(new AutoPush());
      this.addModule(new AntiPush());
      this.addModule(new Offhand());
      this.addModule(new Nuker());
      this.addModule(new AutoTrap());
      this.addModule(new AutoTotemWhisper());
      this.addModule(new CrossbowRapidFire());
      this.addModule(new AutoCrossbowAim());
      this.addModule(new Blink());
      this.addModule(new ChorusControl());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new Blocker());
      this.addModule(new BowBomb());
      this.addModule(new BreakESP());
      this.addModule(new Burrow());
      this.addModule(new Punctuation());
      this.addModule(new MaceSpoof());
      this.addModule(new CameraClip());
      this.addModule(new ClickGui());
      this.addModule(new InfiniteTrident());
      this.addModule(new ColorsModule());
      this.addModule(new AutoRegear());
      this.addModule(new AntiPhase());
      this.addModule(new Clip());
      this.addModule(new AntiCheat());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new Criticals());
      this.addModule(new CevBreaker());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new LogEz());
      this.addModule(new ESP());
      this.addModule(new HoleESP());
      this.addModule(new TargetESP());
      this.addModule(new MovementSync());
      this.addModule(new ElytraFly());
      this.addModule(new PacketLogger());
      this.addModule(new TeleportLogger());
      this.addModule(new SkinFlicker());
      this.addModule(new MaceEffect());
      this.addModule(new EveryonePig());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new FakeFPS());
      this.addModule(new Spammer());
      this.addModule(new AutoSpammer());
      this.addModule(new AutoSanZiJing());
      this.addModule(new MotionCamera());
      this.addModule(new HighLight());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new Yaw());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new Macro());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new Tp());
      this.addModule(new ClientSetting());
      this.addModule(new TextRadar());
      this.addModule(new Friends());
      this.addModule(new GUICharacter());
      this.addModule(new HUD());
      this.addModule(new NoResourcePack());
      this.addModule(new PlayerModule());
      this.addModule(new RocketExtend());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new LongJump());
      this.addModule(new TpChase());
      this.addModule(new HighJumpPlus());
      this.addModule(new AutoTool());
      this.addModule(new AutoDropShulker());
      this.addModule(new Trajectories());
      this.addModule(new TwoDESP());
      this.addModule(new EnemyInfo());
      this.addModule(new CustomWeather());
      this.addModule(new KillEffect());
      this.addModule(new AttackSlash());
      this.addModule(new JumpCircle());
      this.addModule(new AutoPearl());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new NoSound());
      this.addModule(new AirPlace());
      this.addModule(new Xray());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new PacketMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new CrystalRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new PotionHUD());
      this.addModule(new Replenish());
      this.addModule(new ServerLagger());
      this.addModule(new Scaffold());
      this.addModule(new ShaderModule());
      this.addModule(new AntiCrawl());
      this.addModule(new AntiRegear());
      this.addModule(new SafeWalk());
      this.addModule(new NoJumpDelay());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Surround());
      this.addModule(new VoidHammer());
      this.addModule(new TotemParticle());
      this.addModule(new FireFlies());
      this.addModule(new Particles());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new Zoom());
      this.modules.sort(Comparator.comparing(Mod::getName));
      this.addModule(new Chase());
      this.addModule(new AutoWither());
      this.addModule(new WinRateAdvisor());
      this.addModule(new AutoHelp());
      this.addModule(new AutoKouZi());
      this.addModule(new AutoFuck());
      this.addModule(new AutoSuicide());
      this.addModule(new AutoFlirt());
      this.addModule(new Keystrokes());
      this.addModule(new CircularStatusHud());
      this.addModule(new SpearFix());
      this.addModule(new HealthBarOptimization());
      this.addModule(new PlayerInfo());
      this.addModule(new DamageNumber());
      this.addModule(new MioFriendSync());
      this.addModule(new AutoButton());
      this.addModule(new Follower());
      this.addModule(new ElytraChaser());
      this.addModule(new ForceEnchantedApple());
      this.addModule(new AutoCrystal());
      this.addModule(new Refill());
      this.addModule(new AutoWeb());
      this.addModule(new TeammateTaunt());
      this.addModule(new AutoFollow());
      this.addModule(new AutoNetherPortal());
      this.addModule(new FakeCoords());
      this.addModule(new DeathTauntTeam());
      this.addModule(new FuckBurrow());
      this.addModule(new WallTaunt());
      this.addModule(new PearlTaunt());
      this.addModule(new MessageSuffix());
      this.addModule(new MessagePrefix());
      this.addModule(new TPMace());
      this.addModule(new AutoTrapDoor());
      this.addModule(new IQBoost());
      this.addModule(new PearlPredict());
      this.addModule(new TPLowerBedrock());
      this.addModule(new MaceGodPlus());
      this.addModule(new AimBot());
      this.addModule(new SpearHunter());
      this.addModule(new RussianPunctuation());
      this.addModule(new NotificationHud());
      this.addModule(new NoirLive2D());
      this.addModule(new HudEditor());
      this.addModule(new Armor());
      this.addModule(new Totem());
      this.addModule(new Turtle());
      this.addModule(new InGameHud());
      this.addModule(new InventoryViewer());
      this.addModule(new PlayerDoll());
      this.addModule(new Radar());
      this.addModule(new PotionHud());
      this.addModule(new dev.luminous.mod.modules.impl.misc.PotionDisplay());
      this.addModule(new WaterMark());
      this.addModule(new dev.luminous.mod.modules.impl.hud.PlayerInfo());
      this.addModule(new Teams());
      this.addModule(new GrimChase());
      this.addModule(new LowArmorWarning());
      this.addModule(new LowHealthWarning());
      this.addModule(new PearlBlocker());
      this.addModule(new TotemSound());
      this.addModule(new BedrockClip());
   }

   public void onKeyReleased(int var1) {
      if (var1 != -1 && var1 != 0) {
         this.handleKeyEvent(var1, false);
      }
   }

   public void onKeyPressed(int var1) {
      if (var1 != -1 && var1 != 0) {
         this.handleKeyEvent(var1, true);
      }
   }

   private void handleKeyEvent(int var1, boolean var2) {
      for (Module var4 : this.modules) {
         BindSetting var5 = var4.getBindSetting();
         if (var5.getValue() == var1) {
            if (var2) {
               boolean var6 = mc.currentScreen == null;
               if (!var6 && var4 instanceof ClickGui) {
                  var6 = mc.currentScreen instanceof ClickGuiScreen;
               }

               if (var6) {
                  var4.toggle();
                  var5.holding = true;
               }
            } else if (!var2 && var5.isHoldEnable() && var5.holding) {
               var4.toggle();
               var5.holding = false;
            }
         }

         for (Setting var7 : var4.getSettings()) {
            BindSetting var8;
            if (var7 instanceof BindSetting && (var8 = (BindSetting)var7).getValue() == var1) {
               var8.setPressed(var2);
            }
         }
      }
   }

   public void onLogin() {
      for (Module var2 : this.modules) {
         if (var2.isOn()) {
            var2.onLogin();
         }
      }
   }

   public void onLogout() {
      for (Module var2 : this.modules) {
         if (var2.isOn()) {
            var2.onLogout();
         }
      }
   }

   public void onRender2D(DrawContext var1) {
      boolean editing = (mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof TClickGuiScreen)
         && HudEditor.INSTANCE != null
         && HudEditor.INSTANCE.isOn();

      for (Module var3 : this.modules) {
         if (var3.isOn()) {
            if (editing && isHudElementModule(var3)) {
               continue;
            }

            try {
               var3.onRender2D(var1, mc.getRenderTickCounter().getTickDelta(true));
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("\u00a74An error has occurred (" + var3.getName() + " [onRender2D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render2DEvent.get(var1, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("\u00a74An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      this.renderToggleBanners(var1);
   }

   private static boolean isHudElementModule(Module module) {
      return module.getClass().getPackageName().equals("dev.luminous.mod.modules.impl.hud");
   }

   public void render3D(MatrixStack var1) {
      GL11.glEnable(2848);

      for (Module var3 : this.modules) {
         if (var3.isOn()) {
            try {
               var3.onRender3D(var1);
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("\u00a74An error has occurred (" + var3.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render3DEvent.get(var1, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("\u00a74An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }

   public void showToggleBanner(Module var1, boolean var2) {
   }

   private void renderToggleBanners(DrawContext var1) {
   }

   private void renderPotionListLegacy(DrawContext var1) {
      if (mc.player != null) {
         byte var2 = 14;
         int var3 = var2 + 2;
         int var4 = var2 + 92;
         byte var5 = 14;
         byte var6 = 6;
         int var7 = 0;

         for (StatusEffectInstance var9 : mc.player.getStatusEffects()) {
            String var10 = ((StatusEffect)var9.getEffectType().value()).getName().getString();
            int var11 = var9.getDuration();
            int var12 = Math.max(0, var11 / 20);
            int var13 = var12 / 60;
            int var14 = var12 % 60;
            String var15 = String.format("%d:%02d", var13, var14);
            String var16 = var10 + " " + var15;
            int var17 = FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getWidth(var16) : (int)TextUtil.getWidth(var16);
            int var18 = var17 + var6 * 2;
            int var19 = var4 + var7 * (var5 + 4);
            short var20 = 180;
            Render2DUtil.drawRoundedRect(var1.getMatrices(), var3, var19, var18, var5, 4.0F, new Color(255, 255, 255, var20));
            Render2DUtil.drawRoundedStroke(var1.getMatrices(), var3, var19, var18, var5, 4.0F, new Color(220, 224, 230, 160), 48);
            int var21 = var3 + var6;
            double var22 = var19 + (var5 - (FontManager.isCustomFontEnabled() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
            TextUtil.drawString(var1, var16, var21, var22, new Color(30, 30, 30).getRGB(), FontManager.isCustomFontEnabled());
            if (++var7 >= 5) {
               break;
            }
         }
      }
   }

   private void renderPotionBanners(DrawContext var1) {
   }

   private String formatDuration(int var1) {
      int var2 = Math.max(0, var1 / 20);
      int var3 = var2 / 60;
      int var4 = var2 % 60;
      return String.format("%d:%02d", var3, var4);
   }

   private Module getModuleByDisplayName(String var1) {
      for (Module var3 : this.modules) {
         if (var3.getDisplayName().equalsIgnoreCase(var1) || var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public void addModule(Module var1) {
      this.modules.add(var1);
   }

   public Module getModuleByName(String var1) {
      for (Module var3 : this.modules) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   private static class ToggleBanner {
      public final String name;
      public final boolean enabled;
      public final long start;

      public ToggleBanner(String var1, boolean var2, long var3) {
         this.name = var1;
         this.enabled = var2;
         this.start = var3;
      }
   }
}
