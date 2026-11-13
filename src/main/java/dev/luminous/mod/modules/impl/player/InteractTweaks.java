package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class InteractTweaks extends Module {
   public static InteractTweaks INSTANCE;
   public final BooleanSetting noEntityTrace = this.add(new BooleanSetting("NoEntityTrace", true).setParent());
   public final BooleanSetting onlyPickaxe;
   public final BooleanSetting multiTask;
   public final BooleanSetting respawn;
   public final BooleanSetting ghostHand;
   private final BooleanSetting noAbort;
   private final BooleanSetting noReset;
   private final BooleanSetting noDelay;
   private final BooleanSetting noInteract;
   private final BooleanSetting pickaxeSwitch;
   private final BooleanSetting allowSword;
   private final BooleanSetting allowPickaxe;
   private final BooleanSetting reach;
   public final SliderSetting blockRange;
   public final SliderSetting entityRange;
   private final SliderSetting delay;
   public boolean isActive;
   boolean swapped;
   int lastSlot;

   public InteractTweaks() {
      super("InteractTweaks", Module.Category.Player);
      this.noEntityTrace.setChinese("\u65e0\u5b9e\u4f53\u8ffd\u8e2a");
      this.onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, this.noEntityTrace::isOpen));
      this.onlyPickaxe.setChinese("\u4ec5\u9550\u5b50");
      this.multiTask = this.add(new BooleanSetting("MultiTask", true));
      this.multiTask.setChinese("\u591a\u4efb\u52a1");
      this.respawn = this.add(new BooleanSetting("Respawn", true));
      this.respawn.setChinese("\u91cd\u751f");
      this.ghostHand = this.add(new BooleanSetting("GhostHand", false));
      this.ghostHand.setChinese("\u5ffd\u7565\u57fa\u5ca9");
      this.noAbort = this.add(new BooleanSetting("NoAbort", false));
      this.noAbort.setChinese("\u65e0\u6316\u6398\u4e2d\u6b62");
      this.noReset = this.add(new BooleanSetting("NoReset", false));
      this.noReset.setChinese("\u65e0\u6316\u6398\u91cd\u7f6e");
      this.noDelay = this.add(new BooleanSetting("NoDelay", false));
      this.noDelay.setChinese("\u65e0\u6316\u6398\u5ef6\u8fdf");
      this.noInteract = this.add(new BooleanSetting("NoInteract", false));
      this.noInteract.setChinese("\u65e0\u4ea4\u4e92");
      this.pickaxeSwitch = this.add(new BooleanSetting("PickaxeSwitch", false).setParent());
      this.pickaxeSwitch.setChinese("\u5207\u6362\u8fdb\u98df");
      this.allowSword = this.add(new BooleanSetting("AllowSword", true, this.pickaxeSwitch::isOpen));
      this.allowSword.setChinese("\u5251");
      this.allowPickaxe = this.add(new BooleanSetting("AllowPickaxe", true, this.pickaxeSwitch::isOpen));
      this.allowPickaxe.setChinese("\u9550");
      this.reach = this.add(new BooleanSetting("Reach", false));
      this.reach.setChinese("\u8303\u56f4");
      this.blockRange = this.add(new SliderSetting("BlockRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
      this.blockRange.setChinese("\u65b9\u5757\u8303\u56f4");
      this.entityRange = this.add(new SliderSetting("EntityRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
      this.entityRange.setChinese("\u5b9e\u4f53\u8303\u56f4");
      this.delay = this.add(new SliderSetting("Delay", 4.0, 0.0, 4.0, 1.0));
      this.delay.setChinese("\u4f7f\u7528\u5ef6\u8fdf");
      this.swapped = false;
      this.lastSlot = 0;
      this.setChinese("\u4ea4\u4e92\u8c03\u6574");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.respawn.getValue() && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }

      if (mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
         mc.itemUseCooldown = 0;
      }

      if (this.pickaxeSwitch.getValue()) {
         boolean isCrystalOrMace = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getMainHandStack().getItem() == Items.MACE;
         if ((!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem) || !this.allowPickaxe.getValue())
            && (!(mc.player.getMainHandStack().getItem() instanceof SwordItem) || !this.allowSword.getValue())
            && !isCrystalOrMace
            && mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE
            && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
            this.swapped = false;
            return;
         }

         int gapple = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
         if (gapple == -1) {
            gapple = InventoryUtil.findItem(Items.GOLDEN_APPLE);
         }

         if (gapple == -1) {
            if (this.swapped) {
               InventoryUtil.switchToSlot(this.lastSlot);
               this.swapped = false;
            }

            return;
         }

         if (mc.options.useKey.isPressed()) {
            if ((
                  mc.player.getMainHandStack().getItem() instanceof PickaxeItem && this.allowPickaxe.getValue()
                     || mc.player.getMainHandStack().getItem() instanceof SwordItem && this.allowSword.getValue()
                     || isCrystalOrMace
               )
               && mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE
               && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
               this.lastSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(gapple);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.lastSlot);
            this.swapped = false;
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (!nullCheck() && this.noInteract.getValue() && event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
         Block var4 = mc.world.getBlockState(packet.getBlockHitResult().getBlockPos()).getBlock();
         if (!mc.player.isSneaking() && (var4 instanceof ChestBlock || var4 instanceof EnderChestBlock || var4 instanceof AnvilBlock)) {
            event.cancel();
         }
      }
   }

   @Override
   public void onDisable() {
      this.isActive = false;
   }

   public boolean reach() {
      return this.isOn() && this.reach.getValue();
   }

   public boolean noAbort() {
      return this.isOn() && this.noAbort.getValue() && !mc.options.useKey.isPressed();
   }

   public boolean noReset() {
      return this.isOn() && this.noReset.getValue();
   }

   public boolean noDelay() {
      return this.isOn() && this.noDelay.getValue();
   }

   public boolean multiTask() {
      return this.isOn() && this.multiTask.getValue();
   }

   public boolean noEntityTrace() {
      if (!this.isOff() && this.noEntityTrace.getValue()) {
         return !this.onlyPickaxe.getValue()
            ? true
            : mc.player.getMainHandStack().getItem() instanceof PickaxeItem
               || mc.player.isUsingItem() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem);
      } else {
         return false;
      }
   }

   public boolean ghostHand() {
      return this.isOn() && this.ghostHand.getValue() && !mc.options.useKey.isPressed() && !mc.options.sneakKey.isPressed();
   }

   public boolean isEatingGapple() {
      return false;
   }
}
