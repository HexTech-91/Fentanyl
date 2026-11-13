package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.AmbientOcclusionEvent;
import dev.luminous.api.events.impl.ChunkOcclusionEvent;
import dev.luminous.api.events.impl.RenderBlockEntityEvent;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class Xray extends Module {
   public static Xray INSTANCE;
   public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false).injectTask(this::openGui));
   private static final ThreadLocal<Mutable> EXPOSED_POS = ThreadLocal.withInitial(Mutable::new);

   public Xray() {
      super("Xray", Module.Category.Survival);
      this.edit.setChinese("\u7f16\u8f91");
      this.setChinese("\u77ff\u7269\u900f\u89c6");
      INSTANCE = this;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.XRAY)));
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         mc.worldRenderer.reload();
      }
   }

   @Override
   public void onDisable() {
      mc.worldRenderer.reload();
   }

   public boolean isBlocked(Block var1) {
      return !Alien.XRAY.inWhitelist(var1.getTranslationKey());
   }

   @EventListener
   private void onRenderBlockEntity(RenderBlockEntityEvent var1) {
      if (this.isBlocked(var1.blockEntity.getCachedState().getBlock())) {
         var1.cancel();
      }
   }

   @EventListener
   private void onChunkOcclusion(ChunkOcclusionEvent var1) {
      var1.cancel();
   }

   @EventListener
   private void onAmbientOcclusion(AmbientOcclusionEvent var1) {
      var1.lightLevel = 1.0F;
   }

   public static boolean shouldBlock(BlockState var0) {
      return INSTANCE.isOn() && INSTANCE.isBlocked(var0.getBlock());
   }

   public boolean modifyDrawSide(BlockState var1, BlockView var2, BlockPos var3, Direction var4, boolean var5) {
      if (!var5 && !this.isBlocked(var1.getBlock())) {
         BlockPos var6 = var3.offset(var4);
         BlockState var7 = var2.getBlockState(var6);
         return var7.getCullingFace(var2, var6, var4.getOpposite()) != VoxelShapes.fullCube() || var7.getBlock() != var1.getBlock() || isExposed(var6);
      } else {
         return var5;
      }
   }

   public static boolean isExposed(BlockPos var0) {
      for (Direction var4 : Direction.values()) {
         if (!mc.world.getBlockState(EXPOSED_POS.get().set(var0, var4)).isOpaque()) {
            return true;
         }
      }

      return false;
   }
}
