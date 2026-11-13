package dev.luminous.mod.gui.items;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.Friends;
import java.awt.Color;
import java.lang.Character.UnicodeScript;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;

public class FriendsComponent extends Component {
   private final FriendsComponent.Mode mode;

   public FriendsComponent(int x, int y, boolean open, FriendsComponent.Mode mode) {
      super(getDisplayName(mode), null, x, y, open);
      this.mode = mode;
      this.getItems().clear();
      this.setupItems();
   }

   private static String getDisplayName(FriendsComponent.Mode mode) {
      boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
      if (mode == FriendsComponent.Mode.PREMIUM) {
         return chinese ? "\u6b63\u7248\u597d\u53cb" : "Premium";
      } else {
         return chinese ? "\u79bb\u7ebf\u597d\u53cb" : "Offline";
      }
   }

   public static boolean isChineseName(String name) {
      if (name != null && !name.isEmpty()) {
         for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            UnicodeScript script = UnicodeScript.of(c);
            if (script == UnicodeScript.HAN) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public void setupItems() {
      if (Alien.FRIEND != null && Alien.FRIEND.friendList != null) {
         Set<String> tempUnfriended = (Set<String>)(Friends.INSTANCE != null ? Friends.INSTANCE.getTempUnfriended() : new HashSet<>());

         for (String friendName : Alien.FRIEND.friendList) {
            boolean isChinese = isChineseName(friendName);
            if ((this.mode != FriendsComponent.Mode.PREMIUM || !isChinese) && (this.mode != FriendsComponent.Mode.OFFLINE || isChinese)) {
               boolean isUp = !tempUnfriended.contains(friendName);
               this.addButton(new FriendsComponent.FriendButton(friendName, isUp));
            }
         }
      }
   }

   public static class FriendButton extends ModuleButton {
      private boolean isUp;
      private final String friendName;

      public FriendButton(String friendName, boolean isUp) {
         super(new FriendsComponent.FriendDummyModule(friendName));
         this.friendName = friendName;
         this.isUp = isUp;
      }

      @Override
      public void initSettings() {
      }

      @Override
      public boolean getState() {
         return this.isUp;
      }

      @Override
      public void toggle() {
         this.isUp = !this.isUp;
         if (Friends.INSTANCE != null) {
            if (this.isUp) {
               Friends.INSTANCE.upFriend(this.friendName);
            } else {
               Friends.INSTANCE.downFriend(this.friendName);
            }
         }
      }

      @Override
      public double getVisibleItemHeight() {
         return 0.0;
      }

      @Override
      public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
         boolean hovered = this.isHovering(mouseX, mouseY);
         float h = this.height - 0.5F;
         Color bgColor;
         if (this.isUp) {
            bgColor = hovered ? new Color(60, 200, 60, 200) : new Color(50, 170, 50, 160);
         } else {
            bgColor = hovered ? new Color(220, 70, 70, 200) : new Color(190, 50, 50, 160);
         }

         Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, (float)this.width, h, bgColor);
         float textY = this.getCenteredTextY(this.y, h);
         this.drawString(this.friendName, this.x + 2.3F, textY, this.isUp ? enableTextColor : defaultTextColor);
      }
   }

   private static class FriendDummyModule extends Module {
      public FriendDummyModule(String name) {
         super(name, Module.Category.Client);
      }
   }

   public static enum Mode {
      PREMIUM,
      OFFLINE;
   }
}
