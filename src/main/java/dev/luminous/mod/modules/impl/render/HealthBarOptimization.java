package dev.luminous.mod.modules.impl.render;

import dev.luminous.mod.modules.Module;

public class HealthBarOptimization extends Module {
   public static HealthBarOptimization INSTANCE;

   public HealthBarOptimization() {
      super("HealthBarOptimization", "Uses Minecraft's vanilla hardcore heart rendering", Module.Category.Render);
      this.setChinese("\u8840\u6761\u4f18\u5316");
      INSTANCE = this;
   }
}
