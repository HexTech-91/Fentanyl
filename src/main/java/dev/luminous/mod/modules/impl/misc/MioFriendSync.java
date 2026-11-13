package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.SendMessageEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.lang.Character.UnicodeBlock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class MioFriendSync extends Module {
   public static MioFriendSync INSTANCE;
   public final StringSetting prefix = this.add(new StringSetting("Prefix", "."));
   private static final String CN_ADD_PREFIX = "\u5df2\u5c06";
   private static final String CN_ADD_SUFFIX = "\u6dfb\u52a0\u4e3a\u597d\u53cb";
   private static final String EN_ADD_PREFIX = "Added ";
   private static final String EN_ADD_SUFFIX = " to friends";
   private static final Pattern OUTGOING_ADD_PATTERN = Pattern.compile("^(?:[/!;,]|\\\\)?(?:friend|friends|f)\\s+add\\s+(.+)$", 2);

   public MioFriendSync() {
      super("MioFriendSync", Module.Category.Misc);
      this.setChinese("MIO\u597d\u53cb\u540c\u6b65");
      INSTANCE = this;
      FeatureTranslations.localizeModule(this);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.sendMessage("\u00a7a[MIO\u597d\u53cb\u540c\u6b65] \u5df2\u542f\u7528\uff0c\u5f00\u59cb\u540c\u6b65 MIO \u597d\u53cb");
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String var5 = packet.content().getString();
            String playerName = this.extractAddedFriendName(var5);
            if (playerName != null && !playerName.isEmpty() && !Alien.FRIEND.isFriend(playerName)) {
               this.sendAddFriendCommand(playerName);
            }
         }
      }
   }

   @EventListener
   public void onSendMessage(SendMessageEvent event) {
      if (mc.player != null) {
         String playerName = this.extractOutgoingFriendName(event.message);
         if (playerName != null && !playerName.isEmpty() && !Alien.FRIEND.isFriend(playerName)) {
            this.sendAddFriendCommand(playerName);
         }
      }
   }

   private String extractAddedFriendName(String message) {
      if (message != null && !message.isEmpty()) {
         int chineseStart = message.indexOf("\u5df2\u5c06");
         int chineseEnd = message.indexOf("\u6dfb\u52a0\u4e3a\u597d\u53cb");
         if (chineseStart >= 0 && chineseEnd > chineseStart + "\u5df2\u5c06".length()) {
            return message.substring(chineseStart + "\u5df2\u5c06".length(), chineseEnd).trim();
         } else {
            int englishStart = message.indexOf("Added ");
            int englishEnd = message.indexOf(" to friends");
            return englishStart >= 0 && englishEnd > englishStart + "Added ".length()
               ? message.substring(englishStart + "Added ".length(), englishEnd).trim()
               : null;
         }
      } else {
         return null;
      }
   }

   private void sendAddFriendCommand(String playerName) {
      String command;
      if (this.containsChinese(playerName)) {
         command = this.prefix.getValue() + "friend add \"" + playerName + "\"";
      } else {
         command = this.prefix.getValue() + "friend add " + playerName;
      }

      mc.player.networkHandler.sendChatMessage(command);
   }

   private String extractOutgoingFriendName(String message) {
      if (message == null) {
         return null;
      } else {
         Matcher matcher = OUTGOING_ADD_PATTERN.matcher(message.trim());
         if (!matcher.matches()) {
            return null;
         } else {
            String playerName = matcher.group(1).trim();
            if (playerName.length() >= 2 && playerName.startsWith("\"") && playerName.endsWith("\"")) {
               playerName = playerName.substring(1, playerName.length() - 1).trim();
            }

            return playerName;
         }
      }
   }

   private boolean containsChinese(String str) {
      if (str != null && !str.isEmpty()) {
         for (char c : str.toCharArray()) {
            if (this.isChinese(c)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isChinese(char c) {
      UnicodeBlock ub = UnicodeBlock.of(c);
      return ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
         || ub == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
         || ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
         || ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
         || ub == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
         || ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
         || ub == UnicodeBlock.GENERAL_PUNCTUATION;
   }
}
