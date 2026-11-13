package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;

public class AutoSpammer extends Module {
   public static AutoSpammer INSTANCE;
   private final Timer timer = new Timer();
   private int index = 0;
   private int sentCount = 0;
   private List<String> lines = new ArrayList<>();
   private String currentFile = "";
   private final Random random = new Random();
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 2.0, 0.1, 30.0, 0.1).setSuffix("s"));
   private final StringSetting fileName;
   private final BooleanSetting whisperMode;
   private final StringSetting targetPlayer;
   private final BooleanSetting autoDisable;
   private final BooleanSetting randomMode;

   public AutoSpammer() {
      super(
         "AutoSpammer",
         "\u81ea\u52a8\u8bfb\u53d6\u6263\u5b57\u6587\u4ef6\u5939\u4e2d\u7684txt\u6587\u6863\u5e76\u53d1\u9001\u5230\u804a\u5929\u680f",
         Module.Category.Misc
      );
      this.delay.setChinese("\u53d1\u9001\u5ef6\u8fdf");
      this.fileName = this.add(new StringSetting("FileName", ""));
      this.fileName.setChinese("\u6587\u4ef6\u540d");
      this.whisperMode = this.add(new BooleanSetting("WhisperMode", false));
      this.whisperMode.setChinese("\u79c1\u804a\u6a21\u5f0f");
      this.targetPlayer = this.add(new StringSetting("Target", ""));
      this.targetPlayer.setChinese("\u76ee\u6807\u73a9\u5bb6");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.randomMode = this.add(new BooleanSetting("RandomMode", false));
      this.randomMode.setChinese("\u968f\u673a\u53d1\u9001");
      this.setChinese("\u81ea\u52a8\u6263\u5b57");
      INSTANCE = this;
   }

   private static File getSpamFolder() {
      return new File(MinecraftClient.getInstance().runDirectory.getPath() + File.separator + "Alien" + File.separator + "\u6263\u5b57");
   }

   private boolean loadFile(String name) {
      if (name != null && !name.trim().isEmpty()) {
         try {
            File folder = getSpamFolder();
            if (!folder.exists()) {
               folder.mkdirs();
            }

            File file = new File(folder, name.endsWith(".txt") ? name : name + ".txt");
            if (!file.exists()) {
               return false;
            } else {
               this.lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
               this.lines.removeIf(line -> line.trim().isEmpty());
               this.currentFile = name;
               return !this.lines.isEmpty();
            }
         } catch (Exception var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.index = 0;
      this.sentCount = 0;
      this.timer.reset();
      String name = this.fileName.getValue().trim();
      if (!name.isEmpty()) {
         if (!this.loadFile(name)) {
            this.sendMessage("\u00a7c\u6587\u4ef6\u4e0d\u5b58\u5728: " + name + "\uff0c\u8bf7\u653e\u5165 Alien/\u6263\u5b57/ \u76ee\u5f55");
            this.disable();
            return;
         }

         this.sendMessage("\u00a7a\u5df2\u52a0\u8f7d: \u00a7f" + this.currentFile + " \u00a7a(" + this.lines.size() + "\u884c)");
      } else {
         List<String> files = scanFiles();
         if (!files.isEmpty()) {
            String first = files.get(0);
            if (this.loadFile(first)) {
               this.fileName.setValue(first);
               this.sendMessage("\u00a7a\u5df2\u52a0\u8f7d: \u00a7f" + first + " \u00a7a(" + this.lines.size() + "\u884c)");
            }
         }
      }

      if (this.lines.isEmpty()) {
         this.sendMessage(
            "\u00a7c\u8bf7\u5c06txt\u6587\u4ef6\u653e\u5165 Alien/\u6263\u5b57/ \u76ee\u5f55\uff0c\u5e76\u5728FileName\u4e2d\u8f93\u5165\u6587\u4ef6\u540d"
         );
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      this.index = 0;
      this.sentCount = 0;
      this.lines.clear();
      this.currentFile = "";
   }

   @Override
   public String getInfo() {
      if (this.lines.isEmpty()) {
         List<String> files = scanFiles();
         return files.isEmpty() ? "\u65e0\u6587\u4ef6" : "\u6587\u4ef6:" + files.size() + "\u4e2a";
      } else {
         String mode = this.randomMode.getValue() ? "\u968f\u673a" : "";
         return mode + this.sentCount + "/" + this.lines.size();
      }
   }

   public static List<String> scanFiles() {
      List<String> files = new ArrayList<>();

      try {
         File folder = getSpamFolder();
         if (folder.exists() && folder.isDirectory()) {
            File[] txtFiles = folder.listFiles((dir, fname) -> fname.endsWith(".txt"));
            if (txtFiles != null) {
               for (File f : txtFiles) {
                  files.add(f.getName().replace(".txt", ""));
               }
            }
         }
      } catch (Exception var7) {
      }

      return files;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         if (this.lines.isEmpty()) {
            if (this.autoDisable.getValue()) {
               this.disable();
            }
         } else {
            if (this.timer.passedS(this.delay.getValue())) {
               this.timer.reset();
               String line;
               if (this.randomMode.getValue()) {
                  line = this.lines.get(this.random.nextInt(this.lines.size()));
               } else {
                  line = this.lines.get(this.index);
                  this.index = (this.index + 1) % this.lines.size();
               }

               if (line == null || line.trim().isEmpty()) {
                  return;
               }

               if (this.whisperMode.getValue()) {
                  String target = this.targetPlayer.getValue().trim();
                  if (!target.isEmpty()) {
                     mc.getNetworkHandler().sendCommand("w " + target + " " + line);
                  } else {
                     mc.getNetworkHandler().sendChatMessage(line);
                  }
               } else {
                  mc.getNetworkHandler().sendChatMessage(line);
               }

               this.sentCount++;
               if (this.sentCount >= this.lines.size() && this.autoDisable.getValue()) {
                  this.disable();
               }
            }
         }
      }
   }
}
