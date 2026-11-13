package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerModule extends Module {
   public static PlayerModule INSTANCE;
   public final SliderSetting x = this.add(new SliderSetting("X", 100, 0, 1500));
   public final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.25, 3.0, 0.05));
   public final SliderSetting yaw = this.add(new SliderSetting("Yaw", 180.0, 0.0, 360.0, 1.0));
   public final SliderSetting pitch = this.add(new SliderSetting("Pitch", 0.0, -35.0, 35.0, 1.0));
   public final BooleanSetting showInClickGui = this.add(new BooleanSetting("ShowInClickGui", true));
   public final BooleanSetting showInGame = this.add(new BooleanSetting("ShowInGame", false));

   public PlayerModule() {
      super("PlayerModule", Module.Category.Client);
      this.setChinese("\u73a9\u5bb6\u6a21\u5757");
      this.x.setChinese("X\u5750\u6807");
      this.y.setChinese("Y\u5750\u6807");
      this.scale.setChinese("\u7f29\u653e");
      this.yaw.setChinese("\u504f\u822a");
      this.pitch.setChinese("\u4fef\u4ef0");
      this.showInClickGui.setChinese("GUI\u663e\u793a");
      this.showInGame.setChinese("\u6e38\u620f\u5185\u663e\u793a");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      if (mc.player != null) {
         boolean inGui = mc.currentScreen != null;
         boolean shouldRender = inGui && this.showInClickGui.getValue() || !inGui && this.showInGame.getValue();
         if (shouldRender) {
            float sc = this.scale.getValueFloat();
            int w = Math.max(34, Math.round(58.0F * sc));
            int h = Math.max(50, Math.round(82.0F * sc));
            int rx = this.x.getValueInt();
            int ry = this.y.getValueInt();
            int size = Math.max(18, Math.round(32.0F * sc));
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateY((float)Math.toRadians(this.yaw.getValueFloat()));
            Quaternionf lastRotation = new Quaternionf().rotateX((float)Math.toRadians(this.pitch.getValueFloat()));
            rotation.mul(lastRotation);
            Vector3f position = new Vector3f(0.0F, mc.player.getHeight() / 2.0F, 0.0F);
            InventoryScreen.drawEntity(context, rx + w / 2, ry + h - 4, size, position, rotation, lastRotation, mc.player);
         }
      }
   }
}
