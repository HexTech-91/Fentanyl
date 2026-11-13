package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import java.util.HashSet;
import java.util.Set;

public class Friends extends Module {
   public static Friends INSTANCE;
   private final Set<String> tempUnfriended = new HashSet<>();

   public Friends() {
      super("Friends", Module.Category.Client);
      this.setChinese("\u597d\u53cb\u7ba1\u7406");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      int count = this.tempUnfriended.size();
      return count > 0 ? "\u4e0b\u597d\u53cb:" + count : "";
   }

   public Set<String> getTempUnfriended() {
      return this.tempUnfriended;
   }

   public void upFriend(String name) {
      this.tempUnfriended.remove(name);
      if (!Alien.FRIEND.friendList.contains(name)) {
         Alien.FRIEND.friend(name);
      }

      this.sendMessage("\u00a7a[\u4e0a\u597d\u53cb] \u00a7f" + name + " \u00a7a\u5df2\u6062\u590d\u4e3a\u597d\u53cb");
   }

   public void downFriend(String name) {
      this.tempUnfriended.add(name);
      Alien.FRIEND.friend(name);
      this.sendMessage("\u00a7c[\u4e0b\u597d\u53cb] \u00a7f" + name + " \u00a7c\u5df2\u4e34\u65f6\u4e0b\u6389");
   }

   @Override
   public void onDisable() {
      this.restoreAllFriends();
   }

   public void restoreAllFriends() {
      for (String name : this.tempUnfriended) {
         if (!Alien.FRIEND.friendList.contains(name)) {
            Alien.FRIEND.friend(name);
         }
      }

      if (!this.tempUnfriended.isEmpty()) {
         this.sendMessage("\u00a7a\u5df2\u6062\u590d " + this.tempUnfriended.size() + " \u4e2a\u4e34\u65f6\u4e0b\u6389\u7684\u597d\u53cb");
      }

      this.tempUnfriended.clear();
   }
}
