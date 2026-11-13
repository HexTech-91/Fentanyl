package dev.luminous;

import dev.luminous.api.events.eventbus.EventBus;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.BlurManager;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.core.impl.CleanerManager;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.core.impl.FPSManager;
import dev.luminous.core.impl.FriendManager;
import dev.luminous.core.impl.HoleManager;
import dev.luminous.core.impl.HudManager;
import dev.luminous.core.impl.ModuleManager;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.core.impl.PopManager;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.core.impl.ServerManager;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ThreadManager;
import dev.luminous.core.impl.TimerManager;
import dev.luminous.core.impl.TradeManager;
import dev.luminous.core.impl.XrayManager;
import dev.luminous.mod.features.SpearPort;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

public class Alien implements ModInitializer {
   public static final String NAME = "Fentanyl";
   public static final String VERSION = "1.0";
   public static final EventBus EVENT_BUS = new EventBus();
   public static HoleManager HOLE;
   public static PlayerManager PLAYER;
   public static TradeManager TRADE;
   public static CleanerManager CLEANER;
   public static XrayManager XRAY;
   public static ModuleManager MODULE;
   public static HudManager GUI;
   public static CommandManager COMMAND;
   public static ConfigManager CONFIG;
   public static RotationManager ROTATION;
   public static BreakManager BREAK;
   public static PopManager POP;
   public static FriendManager FRIEND;
   public static TimerManager TIMER;
   public static ShaderManager SHADER;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static ThreadManager THREAD;
   public static boolean loaded;
   public static long initTime;
   public static final Item NETHERITE_SPEAR = SpearPort.NETHERITE_SPEAR;

   public static String getPrefix() {
      return ClientSetting.INSTANCE.prefix.getValue();
   }

   public static void save() {
      System.out.println("[\u5b99\u65afClient] Saving");
      CONFIG.save();
      CLEANER.save();
      FRIEND.save();
      XRAY.save();
      TRADE.save();
      System.out.println("[\u5b99\u65afClient] Saved");
   }

   private void register() {
      EVENT_BUS.registerLambdaFactory((var0, var1) -> (Lookup)var0.invoke(null, var1, MethodHandles.lookup()));
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (loaded) {
            save();
         }
      }));
   }

   public void onInitialize() {
      this.register();
      Render2DUtil.initShaders();
      CONFIG = new ConfigManager();
      HOLE = new HoleManager();
      MODULE = new ModuleManager();
      GUI = new HudManager();
      COMMAND = new CommandManager();
      FRIEND = new FriendManager();
      XRAY = new XrayManager();
      CLEANER = new CleanerManager();
      TRADE = new TradeManager();
      ROTATION = new RotationManager();
      BREAK = new BreakManager();
      PLAYER = new PlayerManager();
      POP = new PopManager();
      TIMER = new TimerManager();
      SHADER = new ShaderManager();
      BLUR = new BlurManager();
      FPS = new FPSManager();
      SERVER = new ServerManager();
      THREAD = new ThreadManager();
      CONFIG.load();
      SpearPort.init();
      initTime = System.currentTimeMillis();
      loaded = true;
      EVENT_BUS.post(new InitEvent());
      File var1 = new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + "Alien".toLowerCase() + File.separator + "cfg");
      if (!var1.exists()) {
         var1.mkdirs();
      }
   }
}
