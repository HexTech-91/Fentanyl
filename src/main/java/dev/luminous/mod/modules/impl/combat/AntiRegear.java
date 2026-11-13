package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PlaceBlockEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;
   public final List<BlockPos> safe = Collections.synchronizedList(new ArrayList<>());
   private final EnumSetting<AntiRegear.Mode> mode = this.add(new EnumSetting<>("Mode", AntiRegear.Mode.Mine)).setChinese("\u6a21\u5f0f");
   private final SliderSetting safeRange = this.add(new SliderSetting("SafeRange", 2.0, 0.0, 8.0, 0.1));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 8.0, 0.1));
   private final SliderSetting mineRange = this.add(new SliderSetting("MineRange", 5.0, 0.0, 8.0, 0.1, () -> this.mode.is(AntiRegear.Mode.Both)))
      .setChinese("\u6316\u6398\u8ddd\u79bb");
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 200, 0, 1000, () -> !this.mode.is(AntiRegear.Mode.Mine))).setChinese("\u5ef6\u8fdf");
   private final BooleanSetting noScreen = this.add(new BooleanSetting("NoScreen", false, () -> !this.mode.is(AntiRegear.Mode.Mine)))
      .setChinese("\u65e0\u754c\u9762");
   private final Timer resetTimer = new Timer();
   private final Timer openTimer = new Timer();
   private boolean opened = false;

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.setChinese("\u53cd\u8865\u7ed9");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.opened = false;
   }

   @EventListener
   private void onScreenUpdate(PacketEvent.Receive var1) {
      if (!nullCheck() && this.noScreen.getValue() && this.opened) {
         if (var1.getPacket() instanceof OpenScreenS2CPacket var3 && var3.getScreenHandlerType() == ScreenHandlerType.SHULKER_BOX) {
            mc.executeSync(() -> mc.setScreen(null));
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent var1) {
      if (PacketMine.getBreakPos() == null || !(mc.world.getBlockState(PacketMine.getBreakPos()).getBlock() instanceof ShulkerBoxBlock)) {
         this.safe.removeIf(var0 -> !(mc.world.getBlockState(var0).getBlock() instanceof ShulkerBoxBlock));
         if (!AutoRegear.INSTANCE.isOn()) {
            ShulkerBoxBlockEntity var2 = this.getBlock();
            if (var2 != null) {
               if (!this.mode.is(AntiRegear.Mode.Mine)
                  && (
                     !this.mode.is(AntiRegear.Mode.Both)
                        || !(mc.player.squaredDistanceTo(var2.getPos().toCenterPos()) < this.mineRange.getValue() * this.mineRange.getValue())
                  )) {
                  if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) && !this.opened) {
                     if (!this.openTimer.passedMs(this.delay.getValue())) {
                        return;
                     }

                     this.openTimer.reset();
                     BlockState var7 = mc.world.getBlockState(var2.getPos());
                     if (!(var7.getBlock() instanceof ShulkerBoxBlock)) {
                        return;
                     }

                     Direction var8 = (Direction)var7.get(Properties.FACING);
                     if (BlockUtil.getBlock(var2.getPos().offset(var8)) == Blocks.AIR && BlockUtil.getClickSideStrict(var2.getPos()) != null) {
                        BlockUtil.clickBlock(var2.getPos(), BlockUtil.getClickSideStrict(var2.getPos()), true);
                        this.opened = true;
                        this.resetTimer.reset();
                     }
                  } else if (this.opened && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var3) {
                     for (int var6 = 0; var6 < 27; var6++) {
                        Slot var5 = var3.getSlot(var6);
                        if (var5 != null && !var5.getStack().isEmpty()) {
                           dropSlot(var3.syncId, var5.id);
                        }
                     }

                     mc.player.closeScreen();
                     this.safe.add(var2.getPos());
                  } else if (this.resetTimer.passedMs(this.delay.getValue() * 0.5)) {
                     this.opened = false;
                  }
               } else {
                  PacketMine.INSTANCE.mine(var2.getPos());
               }
            }
         }
      }
   }

   private static void dropSlot(int var0, int var1) {
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(var0, var1, 1, SlotActionType.THROW, mc.player);
      }
   }

   @EventListener
   public void onPlace(PlaceBlockEvent var1) {
      if (var1.block instanceof ShulkerBoxBlock) {
         this.safe.add(var1.blockPos);
      }
   }

   public boolean isControllingShulker() {
      return this.isOn() && (this.opened || mc.player != null && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler);
   }

   private ShulkerBoxBlockEntity getBlock() {
      for (BlockEntity var2 : BlockUtil.getTileEntities()) {
         if (var2 instanceof ShulkerBoxBlockEntity) {
            double var3 = MathHelper.sqrt((float)mc.player.squaredDistanceTo(var2.getPos().toCenterPos()));
            if (!(var3 <= this.safeRange.getValue()) && !(var3 > this.range.getValue()) && !this.safe.contains(var2.getPos())) {
               return (ShulkerBoxBlockEntity)var2;
            }
         }
      }

      return null;
   }

   static enum Mode {
      Mine,
      Throw,
      Both;
   }
}
