package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.interfaces.IChatHudHook;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.commands.impl.AimCommand;
import dev.luminous.mod.commands.impl.BindCommand;
import dev.luminous.mod.commands.impl.BindsCommand;
import dev.luminous.mod.commands.impl.CleanerCommand;
import dev.luminous.mod.commands.impl.ClipCommand;
import dev.luminous.mod.commands.impl.CrCommand;
import dev.luminous.mod.commands.impl.EsuCommand;
import dev.luminous.mod.commands.impl.FakePlayerCommand;
import dev.luminous.mod.commands.impl.FriendCommand;
import dev.luminous.mod.commands.impl.GamemodeCommand;
import dev.luminous.mod.commands.impl.GcCommand;
import dev.luminous.mod.commands.impl.KitCommand;
import dev.luminous.mod.commands.impl.LoadCommand;
import dev.luminous.mod.commands.impl.MusicCommand;
import dev.luminous.mod.commands.impl.PeekCommand;
import dev.luminous.mod.commands.impl.PingCommand;
import dev.luminous.mod.commands.impl.PrefixCommand;
import dev.luminous.mod.commands.impl.RefillCommand;
import dev.luminous.mod.commands.impl.RejoinCommand;
import dev.luminous.mod.commands.impl.ReloadCommand;
import dev.luminous.mod.commands.impl.SaveCommand;
import dev.luminous.mod.commands.impl.TCommand;
import dev.luminous.mod.commands.impl.TeleportCommand;
import dev.luminous.mod.commands.impl.ToggleCommand;
import dev.luminous.mod.commands.impl.TradeCommand;
import dev.luminous.mod.commands.impl.WatermarkCommand;
import dev.luminous.mod.commands.impl.XrayCommand;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.HashMap;
import net.minecraft.text.Text;

public class CommandManager implements Wrapper {
   private final HashMap<String, Command> commands = new HashMap<>();

   public CommandManager() {
      this.init();
   }

   public void init() {
      this.registerCommand(new AimCommand());
      this.registerCommand(new BindCommand());
      this.registerCommand(new BindsCommand());
      this.registerCommand(new CleanerCommand());
      this.registerCommand(new ClipCommand());
      this.registerCommand(new EsuCommand());
      this.registerCommand(new FakePlayerCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new XrayCommand());
      this.registerCommand(new GamemodeCommand());
      this.registerCommand(new KitCommand());
      this.registerCommand(new LoadCommand());
      this.registerCommand(new PingCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(new RejoinCommand());
      this.registerCommand(new ReloadCommand());
      this.registerCommand(new SaveCommand());
      this.registerCommand(new TeleportCommand());
      this.registerCommand(new TCommand());
      this.registerCommand(new ToggleCommand());
      this.registerCommand(new TradeCommand());
      this.registerCommand(new WatermarkCommand());
      this.registerCommand(new PeekCommand());
      this.registerCommand(new GcCommand());
      this.registerCommand(new RefillCommand());
      this.registerCommand(new MusicCommand());
      this.registerCommand(new CrCommand());
   }

   public static void sendMessage(String var0) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
               mc.inGameHud.getChatHud().addMessage(Text.of(var0));
            } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
               mc.inGameHud.getChatHud().addMessage(Text.of("\u00a7f[\u00a7b" + ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f] " + var0));
            } else {
               ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f " + var0));
            }
         }
      });
   }

   public static void sendMessageId(String var0, int var1) {
      mc.execute(
         () -> {
            if (!Module.nullCheck()) {
               if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                  ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessage(Text.of(var0), var1);
               } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                  ((IChatHudHook)mc.inGameHud.getChatHud())
                     .alienClient$addMessage(Text.of("\u00a7f[\u00a7b" + ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f] " + var0), var1);
               } else {
                  ((IChatHudHook)mc.inGameHud.getChatHud())
                     .alienClient$addMessage(Text.of(ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f " + var0), var1);
               }
            }
         }
      );
   }

   public static void sendChatMessageWidthIdNoSync(String var0, int var1) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            ((IChatHudHook)mc.inGameHud.getChatHud()).alienClient$addMessageOutSync(Text.of("\u00a7f" + var0), var1);
         }
      });
   }

   private void registerCommand(Command var1) {
      this.commands.put(var1.getName(), var1);
   }

   public Command getCommandBySyntax(String var1) {
      return this.commands.get(var1);
   }

   public HashMap<String, Command> getCommands() {
      return this.commands;
   }

   public void command(String[] var1) {
      String head = var1[0];
      String prefix = Alien.getPrefix();
      if (head.startsWith(prefix)) {
         head = head.substring(prefix.length());
      } else if (head.startsWith(".")) {
         head = head.substring(1);
      }

      Command var2 = this.commands.get(head.toLowerCase());
      if (var2 == null) {
         sendMessage("\u00a74Invalid Command!");
      } else {
         String[] var3 = new String[var1.length - 1];
         System.arraycopy(var1, 1, var3, 0, var1.length - 1);
         if (var3.length == 1 && var3[0].equals("help")) {
            var2.sendUsage();
            return;
         }

         var2.runCommand(var3);
      }
   }
}
