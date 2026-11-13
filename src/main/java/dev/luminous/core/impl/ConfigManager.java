package dev.luminous.core.impl;

import com.google.common.base.Splitter;
import dev.luminous.Alien;
import dev.luminous.core.Manager;
import dev.luminous.mod.gui.hud.Tab;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class ConfigManager extends Manager {
   public static File options = getFile("options.txt");

   private final Hashtable<String, String> settings = new Hashtable<>();
   private final Map<String, String> keyAliases = new HashMap<>();
   private boolean aliasesBuilt = false;
   private String keyComparison = "";

   public ConfigManager() {
      this.read();
   }

   public int getReadCount() {
      return this.settings.size();
   }

   public String getKeyComparison() {
      return this.keyComparison;
   }

   private void buildAliases() {
      if (!this.aliasesBuilt) {
         this.aliasesBuilt = true;
         File aliasFile = getFile("cfg" + File.separator + "aliases.txt");
         if (aliasFile.exists()) {
            try {
               for (String line : IOUtils.readLines(new FileInputStream(aliasFile), StandardCharsets.UTF_8)) {
                  line = line.trim();
                  if (!line.isEmpty() && !line.startsWith("#")) {
                     int eq = line.indexOf(61);
                     if (eq > 0) {
                        String oldKey = line.substring(0, eq).trim();
                        String newKey = line.substring(eq + 1).trim();
                        if (!oldKey.isEmpty() && !newKey.isEmpty()) {
                           this.keyAliases.put(oldKey, newKey);
                        }
                     }
                  }
               }

               System.out.println("[Alien] Loaded " + this.keyAliases.size() + " key aliases from aliases.txt");
            } catch (Exception var15) {
               System.out.println("[Alien] Failed to load aliases.txt: " + var15.getMessage());
            }
         }

         Map<String, ArrayList<String>> settingNameIndex = new HashMap<>();
         Map<String, String> allCurrentKeys = new HashMap<>();

         for (Module module : Alien.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String key = module.getName() + "_" + setting.getName();
               allCurrentKeys.put(key, "");
               settingNameIndex.computeIfAbsent(setting.getName(), k -> new ArrayList<>()).add(key);
            }
         }

         int autoAliases = 0;

         for (String cfgKey : this.settings.keySet()) {
            if (!allCurrentKeys.containsKey(cfgKey) && !this.keyAliases.containsKey(cfgKey)) {
               int lastUnderscore = cfgKey.lastIndexOf(95);
               if (lastUnderscore > 0) {
                  String settingName = cfgKey.substring(lastUnderscore + 1);
                  if (!settingName.equals("state") && !settingName.equals("hold") && !settingName.equals("Sync") && !settingName.equals("Boolean")) {
                     ArrayList<String> candidates = settingNameIndex.get(settingName);
                     if (candidates != null && candidates.size() == 1) {
                        this.keyAliases.put(cfgKey, candidates.get(0));
                        autoAliases++;
                     } else if (candidates != null && candidates.size() > 1) {
                        String oldModuleName = cfgKey.substring(0, lastUnderscore);

                        for (String candidate : candidates) {
                           int candLastUnderscore = candidate.lastIndexOf(95);
                           String candModuleName = candidate.substring(0, candLastUnderscore);
                           if (oldModuleName.equalsIgnoreCase(candModuleName)) {
                              this.keyAliases.put(cfgKey, candidate);
                              autoAliases++;
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (autoAliases > 0) {
            System.out.println("[Alien] Auto-built " + autoAliases + " key aliases (total: " + this.keyAliases.size() + ")");
         }
      }
   }

   private String resolveKey(String key) {
      if (this.settings.containsKey(key)) {
         return key;
      } else {
         String aliased = this.keyAliases.get(key);
         return aliased != null ? aliased : key;
      }
   }

   public int load() {
      int loaded = 0;
      int missing = 0;
      int aliased = 0;
      int sampleLogged = 0;
      this.buildAliases();
      System.out.println("[Alien] load() starting, settings hashtable has " + this.settings.size() + " entries, " + this.keyAliases.size() + " aliases");
      StringBuilder cmp = new StringBuilder();
      int keySample = 0;
      System.out.println("[Alien] === First 5 cfg keys ===");
      cmp.append("\u00a7eCFG keys: ");

      for (String k : this.settings.keySet()) {
         if (keySample >= 5) {
            break;
         }

         System.out.println("[Alien]   cfg: '" + k + "'");
         if (keySample > 0) {
            cmp.append(", ");
         }

         cmp.append(k);
         keySample++;
      }

      cmp.append(" \u00a77| \u00a7eExpected: ");
      int expectedSample = 0;
      System.out.println("[Alien] === First 5 expected keys ===");

      label139:
      for (Module module : Alien.MODULE.getModules()) {
         Iterator var10 = module.getSettings().iterator();

         while (true) {
            if (var10.hasNext()) {
               Setting setting = (Setting)var10.next();
               if (expectedSample < 5) {
                  String line = module.getName() + "_" + setting.getName();
                  boolean found = this.settings.containsKey(line) || this.keyAliases.values().contains(line) || this.keyAliases.containsKey(line);
                  System.out.println("[Alien]   expected: '" + line + "' (found=" + found + ")");
                  if (expectedSample > 0) {
                     cmp.append(", ");
                  }

                  cmp.append(line).append("(").append(found ? "Y" : "N").append(")");
                  expectedSample++;
                  continue;
               }
            }

            if (expectedSample >= 5) {
               break label139;
            }
            break;
         }
      }

      System.out.println("[Alien] === End key comparison ===");
      this.keyComparison = cmp.toString();

      for (Module module : Alien.MODULE.getModules()) {
         for (Setting setting : module.getSettings()) {
            String line = module.getName() + "_" + setting.getName();
            String resolvedKey = this.resolveKey(line);
            boolean found = this.settings.containsKey(resolvedKey);
            boolean isAliased = !resolvedKey.equals(line);

            try {
               if (setting instanceof BooleanSetting s) {
                  boolean val = this.getBoolean(resolvedKey, s.getDefaultValue());
                  s.setValueWithoutTask(val);
                  if (sampleLogged < 10) {
                     System.out.println("[Alien] Load " + line + (isAliased ? " (via " + resolvedKey + ")" : "") + " = " + val + " (found=" + found + ")");
                     sampleLogged++;
                  }
               } else if (setting instanceof SliderSetting sx) {
                  float val = this.getFloat(resolvedKey, (float)sx.getDefaultValue());
                  sx.setValue(val);
                  if (sampleLogged < 10) {
                     System.out.println("[Alien] Load " + line + (isAliased ? " (via " + resolvedKey + ")" : "") + " = " + val + " (found=" + found + ")");
                     sampleLogged++;
                  }
               } else if (setting instanceof BindSetting sxx) {
                  if (setting == module.getBindSetting() && module instanceof ClickGui && !this.getBoolean("ClientSetting_LoadGuiKey", true)) {
                     sxx.setValue(sxx.getDefaultValue());
                     sxx.setHoldEnable(false);
                  } else {
                     sxx.setValue(this.getInt(resolvedKey, sxx.getDefaultValue()));
                     sxx.setHoldEnable(this.getBoolean(resolvedKey + "_hold"));
                  }
               } else if (setting instanceof EnumSetting<?> sxxx) {
                  sxxx.loadSetting(this.getString(resolvedKey));
               } else if (setting instanceof ColorSetting sxxxx) {
                  sxxxx.setValue(new Color(this.getInt(resolvedKey, sxxxx.getDefaultValue().getRGB()), true));
                  sxxxx.setSync(this.getBoolean(resolvedKey + "Sync", sxxxx.getDefaultSync()));
                  sxxxx.rainbow = this.getBoolean(resolvedKey + "Rainbow", sxxxx.getDefaultRainbow());
                  sxxxx.rainbowSpeed = this.getFloat(resolvedKey + "RainbowSpeed", (float)sxxxx.rainbowSpeed);
                  sxxxx.rainbowSaturation = this.getFloat(resolvedKey + "RainbowSaturation", (float)sxxxx.rainbowSaturation);
                  sxxxx.rainbowBrightness = this.getFloat(resolvedKey + "RainbowBrightness", (float)sxxxx.rainbowBrightness);
                  sxxxx.rainbowAlpha = this.getFloat(resolvedKey + "RainbowAlpha", (float)sxxxx.rainbowAlpha);
                  if (sxxxx.injectBoolean) {
                     sxxxx.booleanValue = this.getBoolean(resolvedKey + "Boolean", sxxxx.getDefaultBooleanValue());
                  }
               } else if (setting instanceof StringSetting sxxxxx) {
                  sxxxxx.setValue(this.getString(resolvedKey, sxxxxx.getDefaultValue()));
               }
            } catch (Exception var23) {
               System.out.println("[Alien] Error loading setting " + line + ": " + var23.getMessage());
            }

            if (found) {
               loaded++;
               if (isAliased) {
                  aliased++;
               }
            } else if (++missing <= 10) {
               System.out.println("[Alien] Config key not found: " + line);
            }
         }

         module.setState(this.getBoolean(module.getName() + "_state", module instanceof HUD));
      }

      System.out.println("[Alien] Loaded " + loaded + " settings (" + aliased + " via aliases), " + missing + " keys not found in config");
      return loaded;
   }

   public void save() {
      try {
         StringBuilder sb = new StringBuilder();
         int totalSettings = 0;

         for (Module module : Alien.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();
               if (setting instanceof BooleanSetting s) {
                  sb.append(line).append(":").append(s.getValue()).append("\n");
                  totalSettings++;
               } else if (setting instanceof SliderSetting sx) {
                  sb.append(line).append(":").append(sx.getValue()).append("\n");
                  totalSettings++;
               } else if (setting instanceof BindSetting sxx) {
                  sb.append(line).append(":").append(sxx.getValue()).append("\n");
                  sb.append(line).append("_hold:").append(sxx.isHoldEnable()).append("\n");
                  totalSettings++;
               } else if (setting instanceof EnumSetting<?> sxxx) {
                  sb.append(line).append(":").append(sxxx.getValue().name()).append("\n");
                  totalSettings++;
               } else if (setting instanceof ColorSetting sxxxx) {
                  sb.append(line).append(":").append(sxxxx.getValue().getRGB()).append("\n");
                  sb.append(line).append("Sync:").append(sxxxx.sync).append("\n");
                  sb.append(line).append("Rainbow:").append(sxxxx.rainbow).append("\n");
                  sb.append(line).append("RainbowSpeed:").append(sxxxx.rainbowSpeed).append("\n");
                  sb.append(line).append("RainbowSaturation:").append(sxxxx.rainbowSaturation).append("\n");
                  sb.append(line).append("RainbowBrightness:").append(sxxxx.rainbowBrightness).append("\n");
                  sb.append(line).append("RainbowAlpha:").append(sxxxx.rainbowAlpha).append("\n");
                  if (sxxxx.injectBoolean) {
                     sb.append(line).append("Boolean:").append(sxxxx.booleanValue).append("\n");
                  }

                  totalSettings++;
               } else if (setting instanceof StringSetting sxxxxx) {
                  sb.append(line).append(":").append(sxxxxx.getValue()).append("\n");
                  totalSettings++;
               }
            }

            sb.append(module.getName()).append("_state:").append(module.isOn()).append("\n");
            totalSettings++;
         }

         if (Alien.GUI != null) {
            totalSettings += this.appendPosition(sb, "armor", Alien.GUI.armorHud);
            totalSettings += this.appendPosition(sb, "totem", Alien.GUI.totemHud);
            totalSettings += this.appendPosition(sb, "turtle", Alien.GUI.turtleHud);
            totalSettings += this.appendPosition(sb, "ingamehud", Alien.GUI.inGameHud);
            totalSettings += this.appendPosition(sb, "playerinfo", Alien.GUI.playerInfo);
            totalSettings += this.appendPosition(sb, "radar", Alien.GUI.radar);
            totalSettings += this.appendPosition(sb, "potion", Alien.GUI.potionHud);
            totalSettings += this.appendPosition(sb, "watermark", Alien.GUI.waterMark);
            totalSettings += this.appendPosition(sb, "playerdoll", Alien.GUI.playerDoll);
            sb.append("playerdoll_w:").append(Alien.GUI.playerDoll.getWidth()).append("\n");
            sb.append("playerdoll_h:").append(Alien.GUI.playerDoll.getHeight()).append("\n");
            totalSettings += 2;
            totalSettings += this.appendPosition(sb, "inventoryviewer", Alien.GUI.inventoryViewer);
         }

         byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
         Files.write(options.toPath(), data);
         String verifyContent = new String(Files.readAllBytes(options.toPath()), StandardCharsets.UTF_8);
         int verifyLines = verifyContent.split("\n").length;
         System.out
            .println("[Alien] Saved " + totalSettings + " settings to " + options.getAbsolutePath() + " (" + data.length + " bytes, " + verifyLines + " lines)");
      } catch (Exception var14) {
         var14.printStackTrace();
         System.out.println("[Alien] Failed to save settings: " + var14.getMessage());
      }
   }

   private int appendPosition(StringBuilder sb, String key, Tab element) {
      sb.append(key).append("_x:").append(element.getX()).append("\n");
      sb.append(key).append("_y:").append(element.getY()).append("\n");
      return 2;
   }

   public void read() {
      Splitter COLON_SPLITTER = Splitter.on(':');

      try {
         if (!options.exists()) {
            System.out.println("[Alien] Config file not found: " + options.getPath());
            return;
         }

         int count = 0;
         int skipped = 0;

         for (String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
            try {
               if (!s.isEmpty() && !s.startsWith("#")) {
                  s = s.trim();
                  if (!s.isEmpty()) {
                     Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
                     String key = iterator.next();
                     String value = iterator.next();
                     key = key.trim();
                     value = value.trim();
                     if (!key.isEmpty() && !value.isEmpty()) {
                        this.settings.put(key, value);
                        count++;
                     } else {
                        skipped++;
                     }
                  }
               }
            } catch (Exception var9) {
               if (++skipped <= 5) {
                  System.out.println("[Alien] Skipping bad option: " + s);
               }
            }
         }

         System.out.println("[Alien] Read " + count + " settings from " + options.getAbsolutePath() + " (skipped " + skipped + ")");
      } catch (Exception var10) {
         var10.printStackTrace();
         System.out.println("[Alien] Failed to load settings: " + var10.getMessage());
      }
   }

   public int getInt(String setting, int defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isInteger(s) ? Integer.parseInt(s) : defaultValue;
   }

   public float getFloat(String setting, float defaultValue) {
      String s = this.settings.get(setting);
      return s != null && this.isFloat(s) ? Float.parseFloat(s) : defaultValue;
   }

   public boolean getBoolean(String setting) {
      String s = this.settings.get(setting);
      return Boolean.parseBoolean(s);
   }

   public boolean getBoolean(String setting, boolean defaultValue) {
      if (this.settings.get(setting) != null) {
         String s = this.settings.get(setting);
         return Boolean.parseBoolean(s);
      } else {
         return defaultValue;
      }
   }

   public String getString(String setting) {
      return this.settings.get(setting);
   }

   public String getString(String setting, String defaultValue) {
      return this.settings.get(setting) == null ? defaultValue : this.settings.get(setting);
   }

   public boolean isInteger(String str) {
      Pattern pattern = Pattern.compile("^[-+]?[\\d]+$");
      return pattern.matcher(str).matches();
   }

   public boolean isFloat(String str) {
      String pattern = "^[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?$";
      return str.matches(pattern);
   }
}
