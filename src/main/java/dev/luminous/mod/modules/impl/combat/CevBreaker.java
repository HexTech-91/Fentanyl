package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CevBreaker extends Module {
   public static CevBreaker INSTANCE;
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 0.0, 8.0, 0.1));
   private final SliderSetting breakRange;
   private final SliderSetting delay;
   private final BooleanSetting rotate;
   private final BooleanSetting ground;
   private final BooleanSetting inventory;
   private final BooleanSetting top;
   private final BooleanSetting bevel;
   private final Timer timer;
   private PlayerEntity target;

   public CevBreaker() {
      super("CevBreaker", Module.Category.Combat);
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.breakRange = this.add(new SliderSetting("BreakRange", 5.0, 0.0, 8.0, 0.1));
      this.breakRange.setChinese("\u6316\u6398\u8303\u56f4");
      this.delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.rotate = this.add(new BooleanSetting("Rotate", true));
      this.rotate.setChinese("\u8f6c\u5934");
      this.ground = this.add(new BooleanSetting("Ground", true));
      this.ground.setChinese("\u5730\u9762");
      this.inventory = this.add(new BooleanSetting("Inventory", true));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.top = this.add(new BooleanSetting("Top", false));
      this.top.setChinese("\u9876\u90e8");
      this.bevel = this.add(new BooleanSetting("Bevel", true));
      this.bevel.setChinese("\u659c\u9762");
      this.timer = new Timer();
      this.target = null;
      this.setChinese("\u81ea\u52a8\u70b8\u5934");
      INSTANCE = this;
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      return mc.world.isAir(pos)
         && BlockUtil.noEntityBlockCrystal(pos, false)
         && BlockUtil.noEntityBlockCrystal(pos.up(), false)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(pos.up()));
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!this.inventory.getValue() || EntityUtil.inInventory()) && (!this.ground.getValue() || mc.player.isOnGround())) {
         PacketMine.INSTANCE.crystal.setValue(true);
         if ((this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue())) != null) {
            BlockPos targetPos = EntityUtil.getEntityPos(this.target);
            if (PacketMine.getBreakPos() != null) {
               for (Direction facing : Direction.values()) {
                  if (facing != Direction.DOWN && (facing == Direction.UP ? this.top.getValue() : this.bevel.getValue())) {
                     BlockPos pos = targetPos.up(1).offset(facing);
                     if (!(pos.up().toCenterPos().distanceTo(mc.player.getPos()) > this.breakRange.getValue())
                        && PacketMine.getBreakPos().equals(targetPos.up(1).offset(facing))) {
                        if (canPlaceCrystal(targetPos.up(2).offset(facing))) {
                           if (mc.world.isAir(pos)) {
                              if (BlockUtil.canPlace(pos)) {
                                 if (!this.timer.passedMs(this.delay.getValue())) {
                                    return;
                                 }

                                 this.placeBlock(pos);
                                 this.timer.reset();
                                 return;
                              }
                           } else if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                              PacketMine.INSTANCE.mine(pos);
                              this.timer.reset();
                              return;
                           }
                        } else if (BlockUtil.hasCrystal(targetPos.up(2).offset(facing))) {
                           if (mc.world.isAir(pos)) {
                              return;
                           }

                           if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                              PacketMine.INSTANCE.mine(pos);
                              this.timer.reset();
                              return;
                           }
                        }
                     }
                  }
               }
            }

            for (Direction facingx : Direction.values()) {
               if (facingx != Direction.DOWN && (facingx == Direction.UP ? this.top.getValue() : this.bevel.getValue())) {
                  BlockPos pos = targetPos.up(1).offset(facingx);
                  if (!(pos.up().toCenterPos().distanceTo(mc.player.getPos()) > this.breakRange.getValue())) {
                     if (canPlaceCrystal(targetPos.up(2).offset(facingx))) {
                        if (mc.world.isAir(pos)) {
                           if (BlockUtil.canPlace(pos)) {
                              if (!this.timer.passedMs(this.delay.getValue())) {
                                 return;
                              }

                              this.placeBlock(pos);
                              this.timer.reset();
                              break;
                           }
                        } else if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                           PacketMine.INSTANCE.mine(pos);
                           this.timer.reset();
                           break;
                        }
                     } else if (BlockUtil.hasCrystal(targetPos.up(2).offset(facingx))) {
                        if (mc.world.isAir(pos)) {
                           break;
                        }

                        if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                           PacketMine.INSTANCE.mine(pos);
                           this.timer.reset();
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private void placeBlock(BlockPos pos) {
      int block;
      if ((block = this.getBlock()) != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         if (BlockUtil.canPlace(pos)) {
            if (BlockUtil.allowAirPlace()) {
               this.doSwap(block);
               BlockUtil.placedPos.add(pos);
               BlockUtil.airPlace(pos, this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(oldSlot);
               }

               return;
            }

            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            this.doSwap(block);
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }
}
