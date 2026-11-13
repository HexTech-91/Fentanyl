package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.FreeLook;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class AutoTame extends Module {
   public static AutoTame INSTANCE;
   private final EnumSetting<AutoTame.Mode> mode = this.add(new EnumSetting<>("\u6a21\u5f0f", AutoTame.Mode.Both));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0, 1.0));
   private final BooleanSetting renderEnabled;
   private final SliderSetting renderRange;
   private Entity currentTarget;
   private int grabbedSlot;
   private boolean hasGrabbed;
   private DonkeyEntity tamingDonkey;
   private volatile boolean sneakingToDismount;
   private ScheduledExecutorService dismountExecutor;
   private ScheduledFuture<?> dismountTask;
   private int prevHotbarSlot;
   private boolean switched;
   private DonkeyEntity[] breedingPair;
   private int bredIndex;
   private static final long BREED_COOLDOWN_MS = 305000L;
   private final Map<UUID, Long> cooldownMap;
   private DonkeyEntity lastFedDonkey;
   private final List<ChunkPos> chunkOrder;
   private int chunkIndex;
   private ChunkPos currentDestination;
   private boolean isMoving;
   private long lastScanTime;
   private static final long SCAN_INTERVAL_MS = 5000L;
   private Entity moveTarget;
   private int stuckTicks;
   private static final int STUCK_THRESHOLD = 40;

   public AutoTame() {
      super("AutoTame", Module.Category.Survival);
      this.range.setChinese("\u8303\u56f4");
      this.renderEnabled = this.add(new BooleanSetting("RenderEnabled", true));
      this.renderEnabled.setChinese("\u6e32\u67d3\u6807\u7b7e");
      this.renderRange = this.add(new SliderSetting("RenderRange", 64.0, 1.0, 128.0, 1.0));
      this.renderRange.setChinese("\u6e32\u67d3\u8303\u56f4");
      this.currentTarget = null;
      this.grabbedSlot = -1;
      this.hasGrabbed = false;
      this.tamingDonkey = null;
      this.sneakingToDismount = false;
      this.dismountExecutor = null;
      this.dismountTask = null;
      this.prevHotbarSlot = -1;
      this.switched = false;
      this.breedingPair = null;
      this.bredIndex = 0;
      this.cooldownMap = new HashMap<>();
      this.lastFedDonkey = null;
      this.chunkOrder = new ArrayList<>();
      this.chunkIndex = 0;
      this.currentDestination = null;
      this.isMoving = false;
      this.lastScanTime = 0L;
      this.moveTarget = null;
      this.stuckTicks = 0;
      this.setChinese("\u81ea\u52a8\u9a6f\u670d");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.currentTarget = null;
      this.tamingDonkey = null;
      this.moveTarget = null;
      this.restoreGrabbedItem();
      this.resetSwitchedSlot();
      this.stopDismounting();
      this.resetBreeding();
      this.cooldownMap.clear();
      this.lastFedDonkey = null;
      this.chunkOrder.clear();
      this.chunkIndex = 0;
      this.currentDestination = null;
      this.isMoving = false;
      this.lastScanTime = 0L;
      this.stuckTicks = 0;
      if (FreeLook.INSTANCE != null && !FreeLook.INSTANCE.isOn()) {
         FreeLook.INSTANCE.toggle();
      }
   }

   @Override
   public void onDisable() {
      this.restoreGrabbedItem();
      this.resetSwitchedSlot();
      this.currentTarget = null;
      this.tamingDonkey = null;
      this.moveTarget = null;
      this.stopDismounting();
      this.resetBreeding();
      this.shutdownDismountExecutor();
      this.cooldownMap.clear();
      this.lastFedDonkey = null;
      this.stopMoving();
      if (FreeLook.INSTANCE != null && FreeLook.INSTANCE.isOn()) {
         FreeLook.INSTANCE.toggle();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         if (FreeLook.INSTANCE != null && !FreeLook.INSTANCE.isOn()) {
            FreeLook.INSTANCE.toggle();
         }

         if (this.tamingDonkey != null) {
            if (mc.player.getVehicle() == this.tamingDonkey) {
               this.tamingDonkey = null;
               this.stuckTicks = 0;
            } else if (this.tamingDonkey.isAlive() && !this.tamingDonkey.isTame()) {
               this.tameLoop(this.tamingDonkey);
               this.stuckTicks = 0;
            } else {
               this.tamingDonkey = null;
               this.stuckTicks = 0;
            }
         } else {
            if (this.moveTarget != null) {
               if (this.moveTarget.isAlive() && !(this.moveTarget instanceof DonkeyEntity d && d.isTame()) && !(mc.player.distanceTo(this.moveTarget) <= 4.0)) {
                  this.startMoving(this.moveTarget.getPos());
                  this.stuckTicks = 0;
                  return;
               }

               this.moveTarget = null;
               this.stopMoving();
               this.stuckTicks = 0;
            }

            boolean shouldClean = this.hasGrabbed || mc.player.getVehicle() instanceof DonkeyEntity dd && !dd.isTame();
            if (shouldClean) {
               if (this.hasGrabbed && !mc.player.getMainHandStack().isEmpty()) {
                  this.dropNewItem();
               }

               this.throwShulkerBoxIfAny();
            }

            if (mc.player.getVehicle() instanceof DonkeyEntity donkey) {
               if (donkey.isTame()) {
                  this.startDismounting();
               }

               this.stuckTicks = 0;
            } else {
               if (this.sneakingToDismount) {
                  this.stopDismounting();
                  this.stuckTicks = 0;
               }

               if (!this.groundLogic()) {
                  this.stuckTicks = 0;
                  this.patrolUpdate();
               } else {
                  if (mc.player.getVehicle() == null && !this.sneakingToDismount) {
                     this.stuckTicks++;
                     if (this.stuckTicks > 40) {
                        this.currentTarget = null;
                        this.moveTarget = null;
                        this.tamingDonkey = null;
                        this.resetBreeding();
                        this.restoreGrabbedItem();
                        this.resetSwitchedSlot();
                        this.stuckTicks = 0;
                        this.patrolUpdate();
                        return;
                     }
                  } else {
                     this.stuckTicks = 0;
                  }

                  this.stopMoving();
               }
            }
         }
      }
   }

   private void tameLoop(DonkeyEntity donkey) {
      if (mc.player != null && donkey != null) {
         if (!mc.player.getMainHandStack().isEmpty()) {
            if (!this.hasGrabbed) {
               this.grabMainHandItem();
            } else {
               this.restoreGrabbedItem();
               this.dropCursorItem();
               this.grabMainHandItem();
            }
         }

         if (mc.player.getMainHandStack().isEmpty()) {
            mc.interactionManager.interactEntity(mc.player, donkey, Hand.MAIN_HAND);
         }
      }
   }

   private void dropCursorItem() {
      if (mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, -999, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null && mc.player.getVehicle() == null && !this.sneakingToDismount) {
         this.patrolUpdate();
         this.stuckTicks = 0;
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null && this.renderEnabled.getValue()) {
         this.renderDonkeyTags(event);
      }
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.isMoving && mc.currentScreen != null) {
         mc.options.forwardKey.setPressed(true);
         mc.options.jumpKey.setPressed(mc.player.horizontalCollision);
         mc.player.input.movementForward = 1.0F;
         mc.player.input.movementSideways = 0.0F;
         mc.player.input.jumping = mc.options.jumpKey.isPressed();
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            if (mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
               int entityId = packet.entityId;

               if (entityId != -1) {
                  if (mc.world.getEntityById(entityId) instanceof DonkeyEntity donkey) {
                     if (donkey.isTame() && !donkey.isBaby()) {
                        if (!this.isCoolingDown(donkey)) {
                           if (this.lastFedDonkey == null) {
                              this.lastFedDonkey = donkey;
                           } else if (!this.lastFedDonkey.equals(donkey)) {
                              long endTime = System.currentTimeMillis() + 305000L;
                              this.cooldownMap.put(this.lastFedDonkey.getUuid(), endTime);
                              this.cooldownMap.put(donkey.getUuid(), endTime);
                              this.lastFedDonkey = null;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void renderDonkeyTags(Render3DEvent event) {
      List<DonkeyEntity> all = this.getAllDonkeysForRender();
      long now = System.currentTimeMillis();

      for (DonkeyEntity donkey : all) {
         if (donkey.isAlive() && !donkey.isBaby()) {
            Long endTime = this.cooldownMap.get(donkey.getUuid());
            String text;
            int color;
            if (endTime != null && now < endTime) {
               long secLeft = (endTime - now) / 1000L;
               text = String.valueOf(secLeft);
               color = 16711680;
            } else if (donkey.isInLove()) {
               text = "\u6c42\u7231\u4e2d";
               color = 16776960;
            } else {
               text = "\u53ef\u7e41\u6b96";
               color = 65280;
            }

            Vec3d pos = donkey.getPos().add(0.0, donkey.getHeight() + 0.5, 0.0);
            this.renderTag(event.matrixStack, event.tickDelta, pos, text, color);
         }
      }
   }

   private void renderTag(MatrixStack matrices, float tickDelta, Vec3d pos, String text, int color) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d camPos = camera.getPos();
      double x = pos.x - camPos.x;
      double y = pos.y - camPos.y;
      double z = pos.z - camPos.z;
      matrices.push();
      matrices.translate(x, y, z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(-0.025F, -0.025F, 0.025F);
      float width = mc.textRenderer.getWidth(text) / 2.0F;
      Render2DUtil.drawRect(matrices, -width - 2.0F, -1.0F, width * 2.0F + 4.0F, 11.0F, new Color(0, 0, 0, 100));
      Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.textRenderer.drawLayer(text, -width, 1.0F, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      mc.textRenderer.draw(text, -width, 1.0F, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      matrices.pop();
   }

   private int countDonkeysInChunk(ChunkPos cp) {
      Box box = new Box(cp.getStartX(), mc.world.getBottomY(), cp.getStartZ(), cp.getEndX() + 1, mc.world.getTopY(), cp.getEndZ() + 1);
      return mc.world.getEntitiesByClass(DonkeyEntity.class, box, e -> true).size();
   }

   private int countBreedableDonkeysInChunk(ChunkPos cp) {
      Box box = new Box(cp.getStartX(), mc.world.getBottomY(), cp.getStartZ(), cp.getEndX() + 1, mc.world.getTopY(), cp.getEndZ() + 1);
      return mc.world.getEntitiesByClass(DonkeyEntity.class, box, e -> !e.isBaby() && e.isTame() && !e.isInLove() && !this.isCoolingDown(e)).size();
   }

   private boolean allDonkeysTamedInChunk(ChunkPos cp) {
      Box box = new Box(cp.getStartX(), mc.world.getBottomY(), cp.getStartZ(), cp.getEndX() + 1, mc.world.getTopY(), cp.getEndZ() + 1);
      List<DonkeyEntity> donkeys = mc.world.getEntitiesByClass(DonkeyEntity.class, box, e -> true);
      return donkeys.stream().allMatch(AbstractHorseEntity::isTame);
   }

   private int countDonkeysInCurrentChunk() {
      return this.countDonkeysInChunk(new ChunkPos(mc.player.getBlockPos()));
   }

   private void startDismounting() {
      if (!this.sneakingToDismount) {
         this.sneakingToDismount = true;
         this.dismountExecutor = Executors.newSingleThreadScheduledExecutor();
         this.dismountTask = this.dismountExecutor.scheduleAtFixedRate(() -> {
            if (mc.player != null && mc.world != null && mc.player.getVehicle() != null && this.sneakingToDismount) {
               mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
            } else {
               mc.execute(() -> this.stopDismounting());
            }
         }, 0L, 50L, TimeUnit.MILLISECONDS);
      }
   }

   private void stopDismounting() {
      if (this.sneakingToDismount) {
         this.sneakingToDismount = false;
         if (this.dismountTask != null) {
            this.dismountTask.cancel(false);
            this.dismountTask = null;
         }

         if (this.dismountExecutor != null) {
            this.dismountExecutor.shutdownNow();
            this.dismountExecutor = null;
         }

         if (mc.player != null) {
            mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, false));
            this.restoreGrabbedItem();
            this.resetSwitchedSlot();
         }
      }
   }

   private void shutdownDismountExecutor() {
      if (this.dismountExecutor != null && !this.dismountExecutor.isShutdown()) {
         this.dismountExecutor.shutdownNow();
         this.dismountExecutor = null;
      }
   }

   private boolean groundLogic() {
      if (mc.player != null && mc.world != null) {
         boolean doTame = this.mode.getValue() == AutoTame.Mode.Tame || this.mode.getValue() == AutoTame.Mode.Both;
         boolean doFeed = this.mode.getValue() == AutoTame.Mode.Feed || this.mode.getValue() == AutoTame.Mode.Both;
         Entity best = this.findBestTarget(doTame, doFeed);
         if (this.hasGrabbed && !this.isSameTarget(best)) {
            this.restoreGrabbedItem();
         }

         if (best != null) {
            this.currentTarget = best;
            if (mc.player.distanceTo(this.currentTarget) > 4.0) {
               this.moveTarget = this.currentTarget;
               return false;
            } else if (doFeed && this.currentTarget instanceof DonkeyEntity d && d.isBaby()) {
               if (mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
                  this.switchToItem(Items.ENCHANTED_GOLDEN_APPLE);
               }

               if (mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                  mc.interactionManager.interactEntity(mc.player, d, Hand.MAIN_HAND);
               }

               return true;
            } else if (doTame && this.currentTarget instanceof DonkeyEntity d && !d.isBaby() && !d.isTame()) {
               this.tamingDonkey = d;
               return true;
            } else {
               this.resetSwitchedSlot();
               return false;
            }
         } else {
            if (this.mode.getValue() == AutoTame.Mode.Both) {
               DonkeyEntity cd = this.findAnyTamedWithoutChest();
               if (cd != null) {
                  if (mc.player.getMainHandStack().getItem() != Items.CHEST) {
                     this.switchToItem(Items.CHEST);
                  }

                  if (mc.player.distanceTo(cd) <= 4.0 && mc.player.getMainHandStack().getItem() == Items.CHEST) {
                     mc.interactionManager.interactEntity(mc.player, cd, Hand.MAIN_HAND);
                  }

                  return true;
               }
            }

            if (doFeed) {
               if (this.breedingPair != null) {
                  return this.handleBreeding();
               }

               List<DonkeyEntity> breedable = this.findBreedableDonkeys();
               if (breedable.size() >= 2) {
                  return this.handleBreeding();
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean handleBreeding() {
      if (mc.player == null) {
         return false;
      } else if (this.breedingPair != null) {
         return this.continueBreeding();
      } else {
         List<DonkeyEntity> breedable = this.findBreedableDonkeys();
         if (breedable.size() >= 2) {
            int count = breedable.size();
            if (count % 2 != 0) {
               count--;
            }

            this.breedingPair = new DonkeyEntity[]{breedable.get(0), breedable.get(1)};
            this.bredIndex = 0;
            return this.continueBreeding();
         } else {
            return false;
         }
      }
   }

   private boolean continueBreeding() {
      if (this.breedingPair != null && mc.player != null) {
         DonkeyEntity a = this.breedingPair[0];
         DonkeyEntity b = this.breedingPair[1];
         if (a != null
            && b != null
            && a.isAlive()
            && b.isAlive()
            && !a.isBaby()
            && !b.isBaby()
            && !(mc.player.distanceTo(a) > this.range.getValue() + 2.0)
            && !(mc.player.distanceTo(b) > this.range.getValue() + 2.0)
            && !this.isCoolingDown(a)
            && !this.isCoolingDown(b)
            && !a.isInLove()
            && !b.isInLove()) {
            if (mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
               this.switchToItem(Items.ENCHANTED_GOLDEN_APPLE);
            }

            if (mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
               return true;
            } else {
               DonkeyEntity target = this.bredIndex == 0 ? a : b;
               if (mc.player.distanceTo(target) > this.range.getValue()) {
                  return true;
               } else {
                  mc.interactionManager.interactEntity(mc.player, target, Hand.MAIN_HAND);
                  this.bredIndex++;
                  if (this.bredIndex >= 2) {
                     this.resetBreeding();
                  }

                  return true;
               }
            }
         } else {
            this.resetBreeding();
            return false;
         }
      } else {
         this.resetBreeding();
         return false;
      }
   }

   private boolean isCoolingDown(DonkeyEntity d) {
      Long end = this.cooldownMap.get(d.getUuid());
      return end != null && System.currentTimeMillis() < end;
   }

   private List<DonkeyEntity> findBreedableDonkeys() {
      double r = this.range.getValue();
      Box box = new Box(mc.player.getX() - r, mc.world.getBottomY(), mc.player.getZ() - r, mc.player.getX() + r, mc.world.getTopY(), mc.player.getZ() + r);
      return mc.world
         .getEntitiesByClass(DonkeyEntity.class, box, e -> !e.isBaby() && e.isTame() && !e.isInLove() && !this.isCoolingDown(e) && mc.player.distanceTo(e) <= r)
         .stream()
         .sorted(Comparator.comparingDouble(mc.player::distanceTo))
         .toList();
   }

   private List<DonkeyEntity> getAllDonkeys() {
      double r = this.range.getValue();
      Box box = new Box(mc.player.getX() - r, mc.world.getBottomY(), mc.player.getZ() - r, mc.player.getX() + r, mc.world.getTopY(), mc.player.getZ() + r);
      return mc.world.getEntitiesByClass(DonkeyEntity.class, box, e -> mc.player.distanceTo(e) <= r);
   }

   private List<DonkeyEntity> getAllDonkeysForRender() {
      double r = this.renderRange.getValue();
      Box box = new Box(mc.player.getX() - r, mc.world.getBottomY(), mc.player.getZ() - r, mc.player.getX() + r, mc.world.getTopY(), mc.player.getZ() + r);
      return mc.world.getEntitiesByClass(DonkeyEntity.class, box, e -> true);
   }

   private void resetBreeding() {
      this.breedingPair = null;
      this.bredIndex = 0;
   }

   private void dropNewItem() {
      int slot = mc.player.getInventory().selectedSlot;
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 1, SlotActionType.THROW, mc.player);
   }

   private void throwShulkerBoxIfAny() {
      for (int i = 0; i < 36; i++) {
         if (i != mc.player.getInventory().selectedSlot) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
               int syncId = mc.player.playerScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, i < 9 ? i + 36 : i, 1, SlotActionType.THROW, mc.player);
               break;
            }
         }
      }
   }

   private boolean isSameTarget(Entity e) {
      return this.currentTarget == null ? e == null : this.currentTarget.equals(e);
   }

   private boolean grabMainHandItem() {
      int slot = mc.player.getInventory().selectedSlot;
      this.grabbedSlot = slot;
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 0, SlotActionType.PICKUP, mc.player);
      this.hasGrabbed = true;
      return true;
   }

   private void restoreGrabbedItem() {
      if (this.hasGrabbed && this.grabbedSlot != -1) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, this.grabbedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
         this.hasGrabbed = false;
         this.grabbedSlot = -1;
      }
   }

   private void switchToItem(Item item) {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         if (mc.player.getMainHandStack().getItem() != item) {
            for (int i = 0; i < 9; i++) {
               if (mc.player.getInventory().getStack(i).getItem() == item) {
                  if (!this.switched) {
                     this.prevHotbarSlot = mc.player.getInventory().selectedSlot;
                     this.switched = true;
                  }

                  mc.player.getInventory().selectedSlot = i;
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                  return;
               }
            }

            int hotbarTarget = -1;

            for (int ix = 0; ix < 9; ix++) {
               if (mc.player.getInventory().getStack(ix).isEmpty()) {
                  hotbarTarget = ix;
                  break;
               }
            }

            if (hotbarTarget == -1) {
               hotbarTarget = mc.player.getInventory().selectedSlot;
            }

            for (int src = 9; src < 36; src++) {
               if (mc.player.getInventory().getStack(src).getItem() == item) {
                  if (!this.switched) {
                     this.prevHotbarSlot = mc.player.getInventory().selectedSlot;
                     this.switched = true;
                  }

                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, src, hotbarTarget, SlotActionType.SWAP, mc.player);
                  mc.player.getInventory().selectedSlot = hotbarTarget;
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarTarget));
                  return;
               }
            }
         }
      }
   }

   private void resetSwitchedSlot() {
      if (this.switched && this.prevHotbarSlot != -1) {
         mc.player.getInventory().selectedSlot = this.prevHotbarSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.prevHotbarSlot));
         this.switched = false;
         this.prevHotbarSlot = -1;
      }
   }

   private DonkeyEntity findAnyTamedWithoutChest() {
      return this.getAllDonkeys().stream().filter(e -> e.isTame() && !e.hasChest()).min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
   }

   private Entity findBestTarget(boolean doTame, boolean doFeed) {
      List<DonkeyEntity> all = this.getAllDonkeys();
      if (all.isEmpty()) {
         return null;
      } else {
         DonkeyEntity tame = null;
         DonkeyEntity feed = null;
         if (doTame) {
            tame = all.stream().filter(e -> !e.isBaby() && !e.isTame()).min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
         }

         if (doFeed) {
            feed = all.stream().filter(PassiveEntity::isBaby).min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
         }

         if (tame == null && feed == null) {
            return null;
         } else if (tame == null) {
            return feed;
         } else if (feed == null) {
            return tame;
         } else {
            return mc.player.distanceTo(tame) <= mc.player.distanceTo(feed) ? tame : feed;
         }
      }
   }

   private void patrolUpdate() {
      if (mc.player != null && mc.world != null) {
         long now = System.currentTimeMillis();
         if (now - this.lastScanTime >= 5000L) {
            this.updateChunkQueue();
            this.lastScanTime = now;
         }

         if (this.chunkOrder.isEmpty()) {
            this.stopMoving();
         } else {
            if (this.chunkIndex >= this.chunkOrder.size()) {
               this.fullRescanAndSort();
               this.chunkIndex = 0;
               if (this.chunkOrder.isEmpty()) {
                  this.stopMoving();
                  return;
               }
            }

            this.currentDestination = this.chunkOrder.get(this.chunkIndex);
            double cx = this.currentDestination.getStartX() + 8.5;
            double cz = this.currentDestination.getStartZ() + 8.5;
            Vec3d target = new Vec3d(cx, mc.player.getY(), cz);
            int currentDonkeys = this.countDonkeysInChunk(this.currentDestination);
            if (this.isAtChunkCenter(this.currentDestination, 2.0)) {
               int breedable = this.countBreedableDonkeysInChunk(this.currentDestination);
               boolean allTamed = this.allDonkeysTamedInChunk(this.currentDestination);
               if (currentDonkeys >= 10 || breedable <= 1 && allTamed) {
                  this.chunkIndex++;
                  this.stopMoving();
                  return;
               }

               if (currentDonkeys < 10) {
                  this.stopMoving();
                  return;
               }
            }

            this.startMoving(target);
         }
      }
   }

   private void fullRescanAndSort() {
      double r = this.renderRange.getValue();
      int viewDist = (int)Math.ceil(r / 16.0);
      ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
      List<ChunkPos> allChunks = new ArrayList<>();

      for (int dx = -viewDist; dx <= viewDist; dx++) {
         for (int dz = -viewDist; dz <= viewDist; dz++) {
            ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
            if (this.countDonkeysInChunk(cp) > 0) {
               allChunks.add(cp);
            }
         }
      }

      this.sortChunksByRules(allChunks, null);
      this.chunkOrder.clear();
      this.chunkOrder.addAll(allChunks);
   }

   private void updateChunkQueue() {
      double r = this.renderRange.getValue();
      int viewDist = (int)Math.ceil(r / 16.0);
      ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
      Set<ChunkPos> currentChunksWithDonkeys = new HashSet<>();

      for (int dx = -viewDist; dx <= viewDist; dx++) {
         for (int dz = -viewDist; dz <= viewDist; dz++) {
            ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
            if (this.countDonkeysInChunk(cp) > 0) {
               currentChunksWithDonkeys.add(cp);
            }
         }
      }

      List<ChunkPos> newChunks = new ArrayList<>();

      for (ChunkPos cp : currentChunksWithDonkeys) {
         if (!this.chunkOrder.contains(cp)) {
            newChunks.add(cp);
         }
      }

      if (!newChunks.isEmpty()) {
         ChunkPos reference = this.chunkOrder.isEmpty() ? null : this.chunkOrder.get(this.chunkOrder.size() - 1);
         this.sortChunksByRules(newChunks, reference);
         this.chunkOrder.addAll(newChunks);
      }
   }

   private void sortChunksByRules(List<ChunkPos> chunks, ChunkPos reference) {
      Comparator<ChunkPos> comp;
      if (reference == null) {
         comp = Comparator.<ChunkPos>comparingInt(cp -> this.countDonkeysInChunk(cp)).thenComparingDouble(cp -> {
            double cx = cp.getStartX() + 8.5;
            double cz = cp.getStartZ() + 8.5;
            return mc.player.getPos().distanceTo(new Vec3d(cx, mc.player.getY(), cz));
         });
      } else {
         Vec3d refPos = new Vec3d(reference.getStartX() + 8.5, mc.player.getY(), reference.getStartZ() + 8.5);
         comp = Comparator.<ChunkPos>comparingInt(cp -> this.countDonkeysInChunk(cp)).thenComparingDouble(cp -> {
            double cx = cp.getStartX() + 8.5;
            double cz = cp.getStartZ() + 8.5;
            return refPos.distanceTo(new Vec3d(cx, mc.player.getY(), cz));
         });
      }

      chunks.sort(comp);
   }

   private boolean isAtChunkCenter(ChunkPos cp, double threshold) {
      double cx = cp.getStartX() + 8.5;
      double cz = cp.getStartZ() + 8.5;
      double dx = mc.player.getX() - cx;
      double dz = mc.player.getZ() - cz;
      return Math.sqrt(dx * dx + dz * dz) < threshold;
   }

   private void startMoving(Vec3d target) {
      if (!this.isMoving) {
         this.isMoving = true;
      }

      double dx = target.x - mc.player.getX();
      double dz = target.z - mc.player.getZ();
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      mc.player.setYaw(yaw);
      mc.player.setPitch(90.0F);
      mc.player.networkHandler.sendPacket(new LookAndOnGround(yaw, 90.0F, mc.player.isOnGround()));
      mc.options.forwardKey.setPressed(true);
      mc.options.jumpKey.setPressed(mc.player.horizontalCollision);
      mc.player.input.movementForward = 1.0F;
      mc.player.input.movementSideways = 0.0F;
   }

   private void stopMoving() {
      this.isMoving = false;
      mc.options.forwardKey.setPressed(false);
      mc.options.jumpKey.setPressed(false);
      if (mc.player != null) {
         mc.player.input.movementForward = 0.0F;
         mc.player.input.movementSideways = 0.0F;
      }
   }

   public static enum Mode {
      Tame,
      Feed,
      Both;
   }
}
