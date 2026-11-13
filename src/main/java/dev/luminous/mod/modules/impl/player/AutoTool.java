package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.event.KeyEvent;
import net.minecraft.block.AirBlock;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting fish;
   private final BooleanSetting autoCast;
   private final SliderSetting ticksAutoCast;
   private final SliderSetting ticksCatch;
   private final SliderSetting ticksThrow;
   private final BooleanSetting splashDetection;
   private final SliderSetting splashDetectionRange;
   private boolean ticksEnabled;
   private int ticksToRightClick;
   private int ticksData;
   private int autoCastTimer;
   private boolean autoCastEnabled;
   private int autoCastCheckTimer;

   public AutoTool() {
      super("AutoTool", Module.Category.Player);
      this.mine.setChinese("\u6316\u6398");
      this.fish = this.add(new BooleanSetting("Fish", true).setParent());
      this.fish.setChinese("\u9493\u9c7c");
      this.autoCast = this.add(new BooleanSetting("AutoCast", true, this.fish::isOpen));
      this.autoCast.setChinese("\u81ea\u52a8\u629b\u7aff");
      this.ticksAutoCast = this.add(new SliderSetting("TicksAutoCast", 10, 0, 60, this.fish::isOpen));
      this.ticksAutoCast.setChinese("\u81ea\u52a8\u629b\u7aff\u5ef6\u8fdf");
      this.ticksCatch = this.add(new SliderSetting("TicksCatch", 6, 0, 60, this.fish::isOpen));
      this.ticksCatch.setChinese("\u6536\u6746\u5ef6\u8fdf");
      this.ticksThrow = this.add(new SliderSetting("TicksThrow", 14, 0, 60, this.fish::isOpen));
      this.ticksThrow.setChinese("\u629b\u7aff\u5ef6\u8fdf");
      this.splashDetection = this.add(new BooleanSetting("SplashDetection", false, this.fish::isOpen));
      this.splashDetection.setChinese("\u6c34\u82b1\u68c0\u6d4b");
      this.splashDetectionRange = this.add(new SliderSetting("SplashDetectionRange", 10, 0, 60, this.fish::isOpen));
      this.splashDetectionRange.setChinese("\u68c0\u6d4b\u8303\u56f4");
      this.setChinese("\u81ea\u52a8\u5de5\u5177");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.autoFish();
      this.autoTool();
   }

   public void autoTool() {
      if (this.mine.getValue() && mc.crosshairTarget instanceof BlockHitResult result) {
         BlockPos var4 = result.getBlockPos();
         if (!mc.world.isAir(var4)) {
            int tool = getTool(var4);
            if (tool != -1 && mc.options.attackKey.isPressed()) {
               mc.player.getInventory().selectedSlot = tool;
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.ticksEnabled = false;
      this.autoCastEnabled = false;
      this.autoCastCheckTimer = 0;
   }

   @EventListener
   private void onPlaySound(PlaySoundEvent event) {
      if (!nullCheck() && this.fish.getValue()) {
         SoundInstance p = event.sound;
         FishingBobberEntity b = mc.player.fishHook;
         if (b != null
            && p.getId().getPath().equals("entity.fishing_bobber.splash")
            && (
               !this.splashDetection.getValue()
                  || MathUtil.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= this.splashDetectionRange.getValue()
            )) {
            this.ticksEnabled = true;
            this.ticksToRightClick = this.ticksCatch.getValueInt();
            this.ticksData = 0;
         }
      }
   }

   public void autoFish() {
      if (this.fish.getValue()) {
         if (this.autoCastCheckTimer <= 0) {
            this.autoCastCheckTimer = 30;
            if (this.autoCast.getValue()
               && !this.ticksEnabled
               && !this.autoCastEnabled
               && mc.player.fishHook == null
               && mc.player.getMainHandStack().getItem() == Items.FISHING_ROD) {
               this.autoCastTimer = 0;
               this.autoCastEnabled = true;
            }
         } else {
            this.autoCastCheckTimer--;
         }

         if (this.autoCastEnabled) {
            this.autoCastTimer++;
            if (this.autoCastTimer > this.ticksAutoCast.getValue()) {
               this.autoCastEnabled = false;
               mc.doItemUse();
            }
         }

         if (this.ticksEnabled && this.ticksToRightClick <= 0) {
            if (this.ticksData == 0) {
               mc.doItemUse();
               this.ticksToRightClick = this.ticksThrow.getValueInt();
               this.ticksData = 1;
            } else if (this.ticksData == 1) {
               mc.doItemUse();
               this.ticksEnabled = false;
            }
         }

         this.ticksToRightClick--;
      }
   }

   @EventListener
   private void onKey(KeyEvent event) {
      if (mc.options.useKey.isPressed()) {
         this.ticksEnabled = false;
      }
   }

   public static int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            float digSpeed = EnchantmentHelper.getLevel(
               (RegistryEntry)mc.world
                  .getRegistryManager()
                  .getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef())
                  .getOptional(Enchantments.EFFICIENCY)
                  .get(),
               stack
            );
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
            if (mc.world.getBlockState(pos).getBlock() instanceof AirBlock) {
               return -1;
            }

            if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }
}
