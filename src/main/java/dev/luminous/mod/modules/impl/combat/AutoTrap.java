package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoTrap extends Module {
   public static AutoTrap INSTANCE;
   public final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final EnumSetting<AutoTrap.TargetMode> targetMod;
   private final EnumSetting<AutoTrap.Mode> headMode;
   final ArrayList<BlockPos> trapList;
   final ArrayList<BlockPos> placeList;
   private final Timer timer;
   private final SliderSetting placeRange;
   private final SliderSetting blocksPer;
   public final SliderSetting predictTicks;
   private final BooleanSetting rotate;
   private final BooleanSetting autoDisable;
   private final SliderSetting range;
   private final BooleanSetting checkMine;
   private final BooleanSetting helper;
   private final BooleanSetting inventory;
   private final BooleanSetting onlyCrawling;
   private final BooleanSetting checkElytra;
   private final BooleanSetting extend;
   private final BooleanSetting antiStep;
   private final BooleanSetting onlyBreak;
   private final BooleanSetting head;
   private final BooleanSetting headExtend;
   private final BooleanSetting chestUp;
   private final BooleanSetting onlyBreaking;
   private final BooleanSetting chest;
   private final BooleanSetting onlyGround;
   private final BooleanSetting ignoreCrawling;
   private final BooleanSetting legs;
   private final BooleanSetting legAnchor;
   private final BooleanSetting down;
   private final BooleanSetting onlyHole;
   private final BooleanSetting breakCrystal;
   private final BooleanSetting usingPause;
   private final BooleanSetting selfGround;
   public PlayerEntity target;
   int progress;

   public AutoTrap() {
      super("AutoTrap", Module.Category.Combat);
      this.delay.setChinese("\u5ef6\u8fdf");
      this.targetMod = this.add(new EnumSetting<>("TargetMod", AutoTrap.TargetMode.Single));
      this.targetMod.setChinese("\u76ee\u6807\u6a21\u5f0f");
      this.headMode = this.add(new EnumSetting<>("HeadMode", AutoTrap.Mode.Anchor));
      this.headMode.setChinese("\u5934\u90e8\u65b9\u5757");
      this.trapList = new ArrayList<>();
      this.placeList = new ArrayList<>();
      this.timer = new Timer();
      this.placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 1.0, 6.0).setSuffix("m"));
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
      this.blocksPer.setChinese("\u6bcf\u6b21\u65b9\u5757");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0));
      this.predictTicks.setChinese("\u9884\u6d4b");
      this.rotate = this.add(new BooleanSetting("Rotate", true));
      this.rotate.setChinese("\u8f6c\u5934");
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.autoDisable.setChinese("\u81ea\u52a8\u5173\u95ed");
      this.range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0).setSuffix("m"));
      this.range.setChinese("\u8303\u56f4");
      this.checkMine = this.add(new BooleanSetting("CheckMine", false));
      this.checkMine.setChinese("\u68c0\u6d4b\u6316\u6398");
      this.helper = this.add(new BooleanSetting("Helper", true));
      this.helper.setChinese("\u8f85\u52a9");
      this.inventory = this.add(new BooleanSetting("Inventory", true));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.onlyCrawling = this.add(new BooleanSetting("OnlyCrawling", false));
      this.onlyCrawling.setChinese("\u4ec5\u8db4\u4e0b");
      this.checkElytra = this.add(new BooleanSetting("CheckElytra", false));
      this.checkElytra.setChinese("\u68c0\u6d4b\u9798\u7fc5");
      this.extend = this.add(new BooleanSetting("Extend", true));
      this.extend.setChinese("\u6269\u5c55");
      this.antiStep = this.add(new BooleanSetting("AntiStep", false));
      this.antiStep.setChinese("\u53cd\u8de8\u6b65");
      this.onlyBreak = this.add(new BooleanSetting("OnlyBreak", false, this.antiStep::getValue));
      this.onlyBreak.setChinese("\u4ec5\u6316\u6398");
      this.head = this.add(new BooleanSetting("Head", true));
      this.head.setChinese("\u5934\u90e8");
      this.headExtend = this.add(new BooleanSetting("HeadExtend", true));
      this.headExtend.setChinese("\u5934\u90e8\u6269\u5c55");
      this.chestUp = this.add(new BooleanSetting("ChestUp", true));
      this.chestUp.setChinese("\u80f8\u90e8\u4e0a\u65b9");
      this.onlyBreaking = this.add(new BooleanSetting("OnlyBreaking", false, this.chestUp::getValue));
      this.onlyBreaking.setChinese("\u4ec5\u6316\u6398\u65f6");
      this.chest = this.add(new BooleanSetting("Chest", true));
      this.chest.setChinese("\u80f8\u90e8");
      this.onlyGround = this.add(new BooleanSetting("OnlyGround", false, this.chest::getValue));
      this.onlyGround.setChinese("\u4ec5\u5730\u9762");
      this.ignoreCrawling = this.add(new BooleanSetting("IgnoreCrawling", false, this.chest::getValue));
      this.ignoreCrawling.setChinese("\u5ffd\u7565\u8db4\u4e0b");
      this.legs = this.add(new BooleanSetting("Legs", false));
      this.legs.setChinese("\u817f\u90e8");
      this.legAnchor = this.add(new BooleanSetting("LegAnchor", true));
      this.legAnchor.setChinese("\u817f\u90e8\u951a");
      this.down = this.add(new BooleanSetting("Down", false));
      this.down.setChinese("\u4e0b\u65b9");
      this.onlyHole = this.add(new BooleanSetting("OnlyHole", false));
      this.onlyHole.setChinese("\u4ec5\u5751\u5185");
      this.breakCrystal = this.add(new BooleanSetting("BreakCrystal", true));
      this.breakCrystal.setChinese("\u6253\u6c34\u6676");
      this.usingPause = this.add(new BooleanSetting("UsingPause", true));
      this.usingPause.setChinese("\u4f7f\u7528\u4e2d\u6682\u505c");
      this.selfGround = this.add(new BooleanSetting("SelfGround", true));
      this.selfGround.setChinese("\u81ea\u8eab\u5730\u9762");
      this.progress = 0;
      this.setChinese("\u81ea\u52a8\u56f0\u4f4f");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.trapList.clear();
      this.placeList.clear();
      this.progress = 0;
      this.target = null;
      if ((!this.selfGround.getValue() || mc.player.isOnGround())
         && (!this.inventory.getValue() || EntityUtil.inInventory())
         && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())
         && (!this.usingPause.getValue() || !mc.player.isUsingItem())
         && this.timer.passed((long)this.delay.getValue())) {
         if (this.targetMod.getValue() == AutoTrap.TargetMode.Single) {
            this.target = CombatUtil.getClosestEnemy(this.range.getValue());
            if (this.target == null) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }

               return;
            }

            this.trapTarget(this.target);
         } else if (this.targetMod.getValue() == AutoTrap.TargetMode.Multi) {
            boolean found = false;

            for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
               found = true;
               this.target = player;
               this.trapTarget(this.target);
            }

            if (!found) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }

               this.target = null;
            }
         }
      }
   }

   private void trapTarget(PlayerEntity target) {
      if ((!this.onlyHole.getValue() || Alien.HOLE.isHole(EntityUtil.getEntityPos(target)))
         && (
            !this.onlyCrawling.getValue()
               || target.isCrawling()
               || this.checkElytra.getValue()
                  && ((ItemStack)target.getInventory().armor.get(2)).getItem() instanceof ElytraItem
                  && (!(mc.player.getY() < target.getY() + 1.0) || target.isFallFlying())
         )) {
         Vec3d playerPos = this.predictTicks.getValue() > 0.0 ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
         this.doTrap(target, new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
      }
   }

   private void doTrap(PlayerEntity player, BlockPos pos) {
      if (pos != null && !this.trapList.contains(pos)) {
         this.trapList.add(pos);
         int headOffset = player.isCrawling() ? 1 : 2;
         int chestOffset = player.isCrawling() ? 0 : 1;
         if (this.legs.getValue()) {
            for (Direction i : Direction.values()) {
               if (i != Direction.DOWN && i != Direction.UP) {
                  BlockPos offsetPos = pos.offset(i);
                  this.tryPlaceBlock(offsetPos, this.legAnchor.getValue(), false, false);
                  if (BlockUtil.getPlaceSide(offsetPos) == null
                     && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                     && this.getHelper(offsetPos) != null) {
                     this.tryPlaceObsidian(this.getHelper(offsetPos));
                  }
               }
            }
         }

         if (this.headExtend.getValue()) {
            for (int x : new int[]{1, 0, -1}) {
               for (int z : new int[]{1, 0, -1}) {
                  BlockPos offsetPos = pos.add(z, 0, x);
                  if (this.checkEntity(new BlockPos(offsetPos))) {
                     this.tryPlaceBlock(
                        offsetPos.up(headOffset),
                        this.headMode.getValue() == AutoTrap.Mode.Anchor,
                        this.headMode.getValue() == AutoTrap.Mode.Concrete,
                        this.headMode.getValue() == AutoTrap.Mode.Web
                     );
                  }
               }
            }
         }

         if (this.head.getValue() && BlockUtil.clientCanPlace(pos.up(headOffset), this.breakCrystal.getValue())) {
            if (BlockUtil.getPlaceSide(pos.up(headOffset)) == null) {
               boolean trapChest = this.helper.getValue();
               if (this.getHelper(pos.up(headOffset)) != null) {
                  this.tryPlaceObsidian(this.getHelper(pos.up(headOffset)));
                  trapChest = false;
               }

               if (trapChest) {
                  for (Direction ix : Direction.values()) {
                     if (ix != Direction.DOWN && ix != Direction.UP) {
                        BlockPos offsetPos = pos.offset(ix).up(chestOffset);
                        if (BlockUtil.isStrictDirection(pos.offset(ix).up(), ix.getOpposite())
                           && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                           && BlockUtil.canPlace(offsetPos, this.placeRange.getValue(), this.breakCrystal.getValue())) {
                           this.tryPlaceObsidian(offsetPos);
                           trapChest = false;
                           break;
                        }
                     }
                  }

                  if (trapChest) {
                     for (Direction ixx : Direction.values()) {
                        if (ixx != Direction.DOWN && ixx != Direction.UP) {
                           BlockPos offsetPos = pos.offset(ixx).up(chestOffset);
                           if (BlockUtil.isStrictDirection(pos.offset(ixx).up(), ixx.getOpposite())
                              && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                              && BlockUtil.getPlaceSide(offsetPos) == null
                              && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                              && this.getHelper(offsetPos) != null) {
                              this.tryPlaceObsidian(this.getHelper(offsetPos));
                              trapChest = false;
                              break;
                           }
                        }
                     }

                     if (trapChest) {
                        for (Direction ixxx : Direction.values()) {
                           if (ixxx != Direction.DOWN && ixxx != Direction.UP) {
                              BlockPos offsetPos = pos.offset(ixxx).up(chestOffset);
                              if (BlockUtil.isStrictDirection(pos.offset(ixxx).up(), ixxx.getOpposite())
                                 && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                 && BlockUtil.getPlaceSide(offsetPos) == null
                                 && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                 && this.getHelper(offsetPos) != null
                                 && BlockUtil.getPlaceSide(offsetPos.down()) == null
                                 && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                                 && this.getHelper(offsetPos.down()) != null) {
                                 this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.tryPlaceBlock(
               pos.up(headOffset),
               this.headMode.getValue() == AutoTrap.Mode.Anchor,
               this.headMode.getValue() == AutoTrap.Mode.Concrete,
               this.headMode.getValue() == AutoTrap.Mode.Web
            );
         }

         if (this.antiStep.getValue() && (Alien.BREAK.isMining(pos.up(headOffset)) || !this.onlyBreak.getValue())) {
            if (BlockUtil.getPlaceSide(pos.up(3)) == null
               && BlockUtil.clientCanPlace(pos.up(3), this.breakCrystal.getValue())
               && this.getHelper(pos.up(3), Direction.DOWN) != null) {
               this.tryPlaceObsidian(this.getHelper(pos.up(3)));
            }

            this.tryPlaceObsidian(pos.up(3));
         }

         if (this.down.getValue()) {
            BlockPos offsetPos = pos.down();
            this.tryPlaceObsidian(offsetPos);
            if (BlockUtil.getPlaceSide(offsetPos) == null
               && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
               && this.getHelper(offsetPos) != null) {
               this.tryPlaceObsidian(this.getHelper(offsetPos));
            }
         }

         if (this.chestUp.getValue()) {
            for (Direction ixxxx : Direction.values()) {
               if (ixxxx != Direction.DOWN && ixxxx != Direction.UP) {
                  BlockPos offsetPos = pos.offset(ixxxx).up(headOffset);
                  if (!this.onlyBreaking.getValue() || Alien.BREAK.isMining(pos.up(headOffset))) {
                     this.tryPlaceObsidian(offsetPos);
                     if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                        if (this.getHelper(offsetPos) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos));
                        } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                           && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                           && this.getHelper(offsetPos.down()) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                        }
                     }
                  }
               }
            }
         }

         if (this.chest.getValue()
            && (!this.onlyGround.getValue() || this.target.isOnGround())
            && (!this.ignoreCrawling.getValue() || !this.target.isCrawling())) {
            for (Direction ixxxxx : Direction.values()) {
               if (ixxxxx != Direction.DOWN && ixxxxx != Direction.UP) {
                  BlockPos offsetPos = pos.offset(ixxxxx).up(chestOffset);
                  this.tryPlaceObsidian(offsetPos);
                  if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                     if (this.getHelper(offsetPos) != null) {
                        this.tryPlaceObsidian(this.getHelper(offsetPos));
                     } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                        && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                        && this.getHelper(offsetPos.down()) != null) {
                        this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                     }
                  }
               }
            }
         }

         if (this.extend.getValue()) {
            for (int x : new int[]{1, 0, -1}) {
               for (int zx : new int[]{1, 0, -1}) {
                  BlockPos offsetPos = pos.add(x, 0, zx);
                  if (this.checkEntity(new BlockPos(offsetPos))) {
                     this.doTrap(player, offsetPos);
                  }
               }
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   public BlockPos getHelper(BlockPos pos) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
               && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
               && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.offset(i);
            }
         }

         return null;
      }
   }

   public BlockPos getHelper(BlockPos pos, Direction ignore) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if (i != ignore
               && (!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
               && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
               && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.offset(i);
            }
         }

         return null;
      }
   }

   private boolean checkEntity(BlockPos pos) {
      if (mc.player.getBoundingBox().intersects(new Box(pos))) {
         return false;
      } else {
         for (Entity entity : Alien.THREAD.getPlayers()) {
            if (entity.getBoundingBox().intersects(new Box(pos)) && entity.isAlive()) {
               return true;
            }
         }

         return false;
      }
   }

   private void tryPlaceBlock(BlockPos pos, boolean anchor, boolean sand, boolean web) {
      if (!this.placeList.contains(pos)
         && !Alien.BREAK.isMining(pos)
         && BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())
         && this.progress < this.blocksPer.getValue()
         && !(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
         int old = mc.player.getInventory().selectedSlot;
         int block = sand
            ? this.getConcrete()
            : (web ? (this.getWeb() != -1 ? this.getWeb() : this.getBlock()) : (anchor && this.getAnchor() != -1 ? this.getAnchor() : this.getBlock()));
         if (block != -1) {
            this.placeList.add(pos);
            CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
            this.doSwap(block);
            BlockUtil.placeBlock(pos, this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            this.timer.reset();
            this.progress++;
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (pos != null
         && !this.placeList.contains(pos)
         && !Alien.BREAK.isMining(pos)
         && BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())
         && this.progress < this.blocksPer.getValue()
         && !(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
         int old = mc.player.getInventory().selectedSlot;
         int block = this.getBlock();
         if (block != -1) {
            BlockUtil.placedPos.add(pos);
            this.placeList.add(pos);
            CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
            this.doSwap(block);
            BlockUtil.placeBlock(pos, this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            this.timer.reset();
            this.progress++;
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

   private int getConcrete() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ConcretePowderBlock.class) : InventoryUtil.findClass(ConcretePowderBlock.class);
   }

   private int getWeb() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
   }

   private int getAnchor() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
   }

   private static enum Mode {
      Obsidian,
      Anchor,
      Web,
      Concrete;
   }

   public static enum TargetMode {
      Single,
      Multi;
   }
}
