package dev.luminous.mod.modules.impl.movement;

import dev.luminous.mod.modules.Module;

public class TPLowerBedrock extends Module {
   public TPLowerBedrock() {
      super("TPLowerBedrock", "Teleports you to lower bedrock", Module.Category.Movement);
      this.setChinese("TP\u57fa\u5ca9\u4e0b\u5c42");
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         mc.getNetworkHandler().sendChatMessage(";tp ~ -70 ~");
         this.disable();
      }
   }
}
