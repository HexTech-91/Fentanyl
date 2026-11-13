package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class PhaseESP extends Module {
   public static PhaseESP INSTANCE;
   private final SliderSetting getDistance = this.add(new SliderSetting("GetDistance", 0.1, 0.0, 1.0, 0.1));
   private final SliderSetting bevelDistance;
   private final ColorSetting safeFill;
   private final ColorSetting safeBox;
   private final ColorSetting semiSafeFill;
   private final ColorSetting semiSafeBox;
   private final ColorSetting unsafeFill;
   private final ColorSetting unsafeBox;
   List<BlockPos> safe;
   List<BlockPos> semiSafe;
   List<BlockPos> unsafe;
   int[] offsets;

   public PhaseESP() {
      super("PhaseESP", Module.Category.Render);
      this.getDistance.setChinese("\u8ddd\u79bb");
      this.bevelDistance = this.add(new SliderSetting("BevelDistance", 0.2, 0.0, 1.0, 0.1));
      this.bevelDistance.setChinese("\u659c\u9762\u8ddd\u79bb");
      this.safeFill = this.add(new ColorSetting("SafeFill", new Color(0, 255, 0, 50)).injectBoolean(true));
      this.safeFill.setChinese("\u5b89\u5168\u586b\u5145");
      this.safeBox = this.add(new ColorSetting("SafeBox", new Color(0, 255, 0, 100)).injectBoolean(true));
      this.safeBox.setChinese("\u5b89\u5168\u8fb9\u6846");
      this.semiSafeFill = this.add(new ColorSetting("SemiSafeFill", new Color(244, 255, 0, 50)).injectBoolean(true));
      this.semiSafeFill.setChinese("\u534a\u5b89\u5168\u586b\u5145");
      this.semiSafeBox = this.add(new ColorSetting("SemiSafeBox", new Color(244, 255, 0, 100)).injectBoolean(true));
      this.semiSafeBox.setChinese("\u534a\u5b89\u5168\u8fb9\u6846");
      this.unsafeFill = this.add(new ColorSetting("UnsafeFill", new Color(148, 0, 0, 50)).injectBoolean(true));
      this.unsafeFill.setChinese("\u4e0d\u5b89\u5168\u586b\u5145");
      this.unsafeBox = this.add(new ColorSetting("UnsafeBox", new Color(148, 0, 0, 100)).injectBoolean(true));
      this.unsafeBox.setChinese("\u4e0d\u5b89\u5168\u8fb9\u6846");
      this.safe = new ArrayList<>();
      this.semiSafe = new ArrayList<>();
      this.unsafe = new ArrayList<>();
      this.offsets = new int[]{1, 0, -1};
      this.setChinese("\u7a7f\u5899\u663e\u793a");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.safe.clear();
      this.semiSafe.clear();
      this.unsafe.clear();

      for (int x : this.offsets) {
         for (int z : this.offsets) {
            BlockPos pos = mc.player.getBlockPos().add(x, 0, z);
            double d = mc.player.getPos().distanceTo(pos.toBottomCenterPos());
            double d2 = x != 0 && z != 0 ? this.bevelDistance.getValue() + 1.0 : this.getDistance.getValue() + 0.8;
            if (d <= d2) {
               BlockState blockState = mc.world.getBlockState(pos);
               BlockPos downPos = pos.down();
               if (blockState.getBlock() == Blocks.BEDROCK) {
                  Block downBlock = mc.world.getBlockState(downPos).getBlock();
                  if (downBlock == Blocks.BEDROCK) {
                     this.safe.add(pos);
                  } else {
                     this.unsafe.add(pos);
                  }
               } else if (blockState.isFullCube(mc.world, pos)) {
                  Block downBlock = mc.world.getBlockState(downPos).getBlock();
                  if (downBlock == Blocks.BEDROCK) {
                     this.semiSafe.add(pos);
                  } else {
                     this.unsafe.add(pos);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      this.draw(matrixStack, this.safe, this.safeFill, this.safeBox);
      this.draw(matrixStack, this.unsafe, this.unsafeFill, this.unsafeBox);
      this.draw(matrixStack, this.semiSafe, this.semiSafeFill, this.semiSafeBox);
   }

   private void draw(MatrixStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting box) {
      for (BlockPos pos : list) {
         Box espBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY(), pos.getZ() + 1);
         if (fill.booleanValue) {
            Render3DUtil.drawFill(matrixStack, espBox, fill.getValue());
         }

         if (box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, espBox, box.getValue());
         }
      }
   }

   public static enum Type {
      None,
      Air,
      Normal,
      Bedrock;
   }
}
