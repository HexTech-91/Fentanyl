package dev.luminous.core.impl;

import dev.luminous.core.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;

public class FriendManager extends Manager {
   private static final List<String> BUILT_IN_FRIENDS = List.of();
   private static final List<String> LOCKED_FRIENDS = List.of();
   private static final int MAX_BACKUPS = 20;
   public final ArrayList<String> friendList = new ArrayList<>();
   private boolean ignoringFriends;

   public FriendManager() {
      this.read();
   }

   public boolean isFriend(String name) {
      String normalized = normalizeName(name);
      if (this.ignoringFriends || normalized == null) {
         return false;
      } else {
         return BUILT_IN_FRIENDS.contains(normalized) || this.containsFriend(normalized);
      }
   }

   public boolean isFriend(PlayerEntity entity) {
      return this.isFriend(entity.getGameProfile().getName());
   }

   public void setIgnoringFriends(boolean ignoringFriends) {
      this.ignoringFriends = ignoringFriends;
   }

   public void remove(String name) {
      String normalized = normalizeName(name);
      if (normalized != null && !LOCKED_FRIENDS.contains(normalized)) {
         boolean removed = this.friendList.removeIf(friend -> friend.equalsIgnoreCase(normalized));
         if (removed) {
            this.save();
         }
      }
   }

   public void add(String name) {
      String trimmed = normalizeName(name);
      if (trimmed != null && !trimmed.isBlank() && !this.containsFriend(trimmed)) {
         this.friendList.add(trimmed);
         this.save();
      }
   }

   public void friend(PlayerEntity entity) {
      this.friend(entity.getGameProfile().getName());
   }

   public void friend(String name) {
      String trimmed = normalizeName(name);
      if (trimmed != null && !trimmed.isBlank()) {
         if (this.containsFriend(trimmed)) {
            this.friendList.removeIf(friend -> friend.equalsIgnoreCase(trimmed));
         } else {
            this.friendList.add(trimmed);
         }

         this.save();
      }
   }

   private boolean containsFriend(String name) {
      for (String friend : this.friendList) {
         if (friend.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   private static String normalizeName(String name) {
      if (name == null) {
         return null;
      } else {
         String trimmed = name.trim();
         if (trimmed.length() >= 2
            && (trimmed.startsWith("\"") && trimmed.endsWith("\"") || trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
         }

         return trimmed;
      }
   }

   public void clear() {
      if (!this.friendList.isEmpty()) {
         this.friendList.clear();
         this.save();
      }
   }

   public void read() {
      try {
         File friendFile = getFile("friends.txt");
         this.friendList.clear();
         if (friendFile.exists()) {
            for (String name : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
               this.addWithoutSaving(name);
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public void save() {
      try {
         File friendFile = getFile("friends.txt");
         String content = this.serializeFriends();
         String existing = friendFile.exists() ? Files.readString(friendFile.toPath(), StandardCharsets.UTF_8) : null;
         if (!content.equals(existing)) {
            this.backup(friendFile.toPath());
            this.writeFriends(friendFile.toPath(), content);
         }
      } catch (Exception var4) {
         var4.printStackTrace();
         System.out.println("[Alien] Failed to save friends");
      }
   }

   private boolean addWithoutSaving(String name) {
      String trimmed = normalizeName(name);
      if (trimmed != null && !trimmed.isBlank()) {
         if (this.containsFriend(trimmed)) {
            return false;
         } else {
            this.friendList.add(trimmed);
            return true;
         }
      } else {
         return false;
      }
   }

   private String serializeFriends() {
      StringBuilder output = new StringBuilder();

      for (String friend : this.friendList) {
         output.append(friend).append(System.lineSeparator());
      }

      return output.toString();
   }

   private void writeFriends(Path friendFile, String content) throws Exception {
      Files.createDirectories(friendFile.getParent());
      Path temporaryFile = friendFile.resolveSibling("friends.txt.alien.tmp");
      Files.writeString(
         temporaryFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
      );

      try {
         Files.move(temporaryFile, friendFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException var5) {
         Files.move(temporaryFile, friendFile, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private void backup(Path friendFile) throws Exception {
      if (Files.exists(friendFile) && Files.size(friendFile) != 0L) {
         Path backupDirectory = friendFile.getParent().resolve("friend-backups");
         Files.createDirectories(backupDirectory);
         Path backupFile = backupDirectory.resolve("friends-" + System.currentTimeMillis() + ".txt");
         Files.copy(friendFile, backupFile, StandardCopyOption.COPY_ATTRIBUTES);

         try (Stream<Path> backups = Files.list(backupDirectory)) {
            for (Path path : backups.filter(pathx -> pathx.getFileName().toString().startsWith("friends-"))
               .sorted(Comparator.comparingLong(this::lastModified).reversed())
               .skip(20L)
               .toList()) {
               Files.deleteIfExists(path);
            }
         }
      }
   }

   private long lastModified(Path path) {
      try {
         return Files.getLastModifiedTime(path).toMillis();
      } catch (Exception var3) {
         return 0L;
      }
   }
}
