package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.util.Identifier;

public class Skin extends Module {
   public static Skin INSTANCE;
   private final StringSetting name = this.add(new StringSetting("Name", "Notch"));
   private final EnumSetting<Skin.Model> model;
   private final BooleanSetting cape;
   private final EnumSetting<Skin.CapeType> capeType;
   private final ExecutorService executorService;
   private Future<?> future;
   private Skin.Type type;
   private String skinName;

   public Skin() {
      super("Skin", "\u66f4\u6539\u672c\u5730\u76ae\u80a4\u548c\u62ab\u98ce.", Module.Category.Client);
      this.name.setChinese("\u540d\u79f0");
      this.model = this.add(new EnumSetting<>("Model", Skin.Model.AUTO));
      this.model.setChinese("\u6a21\u578b");
      this.cape = this.add(new BooleanSetting("Cape", true));
      this.cape.setChinese("\u62ab\u98ce");
      this.capeType = this.add(new EnumSetting<>("CapeType", Skin.CapeType.MELON));
      this.capeType.setChinese("\u62ab\u98ce\u7c7b\u578b");
      this.executorService = Executors.newSingleThreadExecutor(runnable -> {
         Thread thread = new Thread(runnable, "Xigua-SkinFetcher");
         thread.setDaemon(true);
         return thread;
      });
      this.type = Skin.Type.IDLE;
      this.skinName = null;
      this.setChinese("\u76ae\u80a4");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.loadSkin();
   }

   @Override
   public void onDisable() {
      if (this.future != null) {
         this.future.cancel(true);
      }

      this.executorService.shutdownNow();
      this.skinName = null;
      this.type = Skin.Type.IDLE;
   }

   @Override
   public String getInfo() {
      return switch (this.type) {
         case IDLE -> "";
         case LOADING -> "...";
         case SUCCESS -> this.skinName;
         case FAILED -> "Failed";
      };
   }

   public boolean isSkinLoaded() {
      return this.isOn() && this.skinName != null;
   }

   public Identifier getSkinTexture() {
      return this.skinName == null ? null : Identifier.of("xigua", "skin/" + this.skinName);
   }

   public boolean isCapeEnabled() {
      return this.isOn() && this.cape.getValue();
   }

   public Identifier getCapeTexture() {
      String capeFile = this.capeType.getValue().text;
      return Identifier.of("xigua", "cape/" + capeFile);
   }

   private void loadSkin() {
      String skinNameInput = this.name.getValue().trim();
      if (skinNameInput.isEmpty()) {
         this.type = Skin.Type.FAILED;
      } else {
         this.type = Skin.Type.LOADING;
         this.future = this.executorService.submit(() -> {
            try {
               this.skinName = skinNameInput;
               this.type = Skin.Type.SUCCESS;
            } catch (Exception var3) {
               this.type = Skin.Type.FAILED;
            }
         });
      }
   }

   public static enum CapeType {
      MELON("melon_cape.png"),
      VAPE("vape_cape.png"),
      CHICKEN("chicken_cape.png"),
      BADLION("badlion_cape.png"),
      CREEPER("creeper_cape.png"),
      DRAGON("dragon_cape.png"),
      ELAINA("elaina_cape.png"),
      FDP("fdp_cape.png"),
      LUNAR_LIGHT("lunar_light_cape.png"),
      LUNAR_DARK("lunar_dark_cape.png"),
      MOJANG("mojang_cape.png"),
      NOVOLINE("novoline_cape.png"),
      SAGIRI("sagiri_cape.png");

      public final String text;

      private CapeType(String text) {
         this.text = text;
      }

      public String getChinese() {
         return switch (this) {
            case MELON -> "\u751c\u74dc";
            case VAPE -> "Vape";
            case CHICKEN -> "\u5c0f\u9e21";
            case BADLION -> "BadLion";
            case CREEPER -> "\u82e6\u529b\u6015";
            case DRAGON -> "\u9f99";
            case ELAINA -> "\u4f0a\u857e\u5a1c";
            case FDP -> "FDP";
            case LUNAR_LIGHT -> "\u6708\u7403\u4eae";
            case LUNAR_DARK -> "\u6708\u7403\u6697";
            case MOJANG -> "Mojang";
            case NOVOLINE -> "NovoLine";
            case SAGIRI -> "\u7eb1\u96fe";
         };
      }
   }

   public static enum Model {
      AUTO,
      DEFAULT,
      SLIM;

      public String getChinese() {
         return switch (this) {
            case AUTO -> "\u81ea\u52a8";
            case DEFAULT -> "\u9ed8\u8ba4";
            case SLIM -> "\u7ea4\u7ec6";
         };
      }
   }

   private static enum Type {
      IDLE,
      LOADING,
      SUCCESS,
      FAILED;
   }
}
