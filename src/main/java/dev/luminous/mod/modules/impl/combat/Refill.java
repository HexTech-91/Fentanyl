package dev.luminous.mod.modules.impl.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.InventoryS2CPacketEvent;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.player.MioKitBridge;
import dev.luminous.api.utils.render.ShulkerOverlayRenderer;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Refill extends Module {
   public static Refill INSTANCE;
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("\u81ea\u52a8\u5173\u95ed", false));
   private final BooleanSetting silent = this.add(new BooleanSetting("Silent", true));
   private final BooleanSetting fastHotbar;
   private final BooleanSetting oneTick;
   private static final String KITS_DIR = "Alien/refill/kits/";
   private static final Path CONFIG_FILE = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "Alien/refill_active_kit.txt");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int SETTLE_TICKS = 2;
   private static final long SERVER_ACK_TIMEOUT_MS = 500L;
   private final StringSetting kitSelection;
   private final SliderSetting itemsPerTick;
   private static final Set<String> WHITELIST_IDS = Set.of("minecraft:end_crystal", "minecraft:totem_of_undying", "minecraft:enchanted_golden_apple");
   private static final String TURTLE_MASTER_ID = "minecraft:turtle_master";
   private String currentKitName;
   private Map<Integer, Refill.SimpleStack> currentKit;
   private int idleTicks;
   private static final int MAX_IDLE_TICKS = 20;
   private List<String> cachedKitNames;
   private long nextKitRefresh;
   private int activeHandlerSyncId;
   private int readyHandlerSyncId;
   private int emptyPasses;
   private static final int CONTENT_GATE_TIMEOUT_TICKS = 60;
   private int contentWaitTicks;
   private boolean movedInHandler;
   private boolean arrangedInHandler;
   private boolean awaitingServerAck;
   private long lastMoveBatchAt;
   private boolean autoCrystalWasOn;
   private boolean autoCrystalPaused;
   private final BooleanSetting overlay = this.add(new BooleanSetting("Overlay", true));
   private final BooleanSetting autoArrange = this.add(new BooleanSetting("AutoArrange", true));
   private final int[] overlayPanel = new int[4];
   private double overlayAnchorX = -1.0;
   private double overlayAnchorY = -1.0;
   private static boolean overlayActive = false;
   private BlockPos overlayAnchorPos;
   private BlockPos targetedShulkerPos;
   private int targetedShulkerSyncId = -1;
   private BlockPos lastShulkerPos;

   public static boolean isOverlayActive() {
      Refill var0 = INSTANCE;
      if (var0 != null && var0.isOn() && var0.overlay.getValue()) {
         return overlayActive;
      } else {
         if (overlayActive) {
            overlayActive = false;
            ShulkerOverlayRenderer.clearPops();
         }

         return false;
      }
   }

   public double getOverlayAnchorX() {
      return this.overlayAnchorX >= 0.0 ? this.overlayAnchorX : mc.getWindow().getScaledWidth() / 2.0;
   }

   public double getOverlayAnchorY() {
      return this.overlayAnchorY >= 0.0 ? this.overlayAnchorY : mc.getWindow().getScaledHeight() / 2.0;
   }

   public void setOverlayAnchor(BlockPos var1) {
      this.overlayAnchorPos = var1;
      this.reprojectAnchor();
   }

   public void reprojectAnchor() {
      if (this.overlayAnchorPos == null) {
         this.overlayAnchorX = -1.0;
         this.overlayAnchorY = -1.0;
      } else {
         Vec3d var1 = TextUtil.worldSpaceToScreenSpace(this.overlayAnchorPos.toCenterPos());
         if (var1 != null && Double.isFinite(var1.x) && Double.isFinite(var1.y)) {
            this.overlayAnchorX = var1.x;
            this.overlayAnchorY = var1.y;
         }
      }
   }

   public List<ItemStack> getOverlayBoxes() {
      ArrayList var1 = new ArrayList();
      if (this.currentKit != null && !this.currentKit.isEmpty()) {
         Map var2 = this.getPlannedShulkerSlots();
         if (var2.isEmpty()) {
            return var1;
         } else {
            ArrayList var3 = new ArrayList(var2.keySet());
            Collections.sort(var3);

            for (int var4 = 0; var4 < var3.size(); var4++) {
               int var5 = (Integer)var3.get(var4);
               if (var5 >= 0 && var5 < 36) {
                  ItemStack var6 = mc.player.getInventory().getStack(var5);
                  if (var6 != null && !var6.isEmpty()) {
                     var1.add(var6);
                  }
               }
            }

            return var1;
         }
      } else {
         return var1;
      }
   }

   private void refreshOverlayAnchor() {
      BlockPos var1 = this.findTargetedShulkerOnOpen();
      if (var1 == null) {
         BlockPos var2 = this.lastShulkerPos;
         if (this.isShulkerBoxAt(var2)) {
            var1 = var2;
         }
      }

      if (var1 == null) {
         var1 = this.findShulkerInFront();
      }

      this.setOverlayAnchor(var1);
   }

   private BlockPos findTargetedShulkerOnOpen() {
      if (mc.world == null || mc.player == null || mc.player.currentScreenHandler == null) {
         return this.targetedShulkerPos;
      } else if (this.targetedShulkerSyncId == mc.player.currentScreenHandler.syncId) {
         return this.targetedShulkerPos;
      } else {
         this.targetedShulkerSyncId = mc.player.currentScreenHandler.syncId;
         this.targetedShulkerPos = null;

         try {
            if (mc.crosshairTarget instanceof BlockHitResult var1) {
               BlockPos var4 = var1.getBlockPos();
               if (this.isShulkerBoxAt(var4)) {
                  this.targetedShulkerPos = var4;
               }
            }
         } catch (Exception var3) {
         }

         return this.targetedShulkerPos;
      }
   }

   private BlockPos findShulkerInFront() {
      if (mc.world == null) {
         return null;
      } else {
         BlockPos var1 = mc.player.getBlockPos();
         Vec3d var2 = mc.player.getCameraPosVec(1.0F);
         Direction var3 = Direction.getFacing(var2.x, 0.0, var2.z);

         for (int var4 = 1; var4 <= 2; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               BlockPos var6 = var1.offset(var3, var4).add(0, var5, 0);
               if (this.isShulkerBoxAt(var6)) {
                  return var6;
               }
            }
         }

         return null;
      }
   }

   private boolean isShulkerBoxAt(BlockPos var1) {
      if (var1 != null && mc.world != null) {
         try {
            BlockState var2 = mc.world.getBlockState(var1);
            return var2.getBlock() instanceof ShulkerBoxBlock;
         } catch (Exception var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void setLastShulkerPos(BlockPos var1) {
      this.lastShulkerPos = var1;
   }

   public Map<Integer, String> getPlannedShulkerSlots() {
      HashMap var1 = new HashMap();
      if (this.currentKit == null) {
         return var1;
      } else {
         for (Entry var3 : this.currentKit.entrySet()) {
            Refill.SimpleStack var4 = (Refill.SimpleStack)var3.getValue();
            if (var3.getKey() != null && var4 != null && var4.id != null && (var4.id.endsWith("_shulker_box") || var4.id.equals("minecraft:shulker_box"))) {
               var1.put((Integer)var3.getKey(), var4.id);
            }
         }

         return var1;
      }
   }

   public static void renderOverlay(DrawContext var0) {
      Refill var1 = INSTANCE;
      if (var1 != null && mc.player != null && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var2) {
         var1.reprojectAnchor();
         int var5 = mc.getWindow().getScaledWidth();
         int var4 = mc.getWindow().getScaledHeight();
         ShulkerOverlayRenderer.layout(var5, var4, var1.getOverlayAnchorX(), var1.getOverlayAnchorY(), var1.overlayPanel);
         ShulkerOverlayRenderer.render(var0, var2, var1.overlayPanel[0], var1.overlayPanel[1], var2.getCursorStack());
      }
   }

   private void updateOverlayState() {
      if (mc.player == null) {
         if (overlayActive) {
            overlayActive = false;
            ShulkerOverlayRenderer.clearPops();
         }
      } else {
         boolean var1 = this.overlay.getValue()
            && this.isOn()
            && mc.currentScreen instanceof ShulkerBoxScreen
            && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler
            && this.isHandlingKit();
         if (!var1 && overlayActive) {
            ShulkerOverlayRenderer.clearPops();
         }

         overlayActive = var1;
      }
   }

   public Refill() {
      super("Refill", Module.Category.Combat);
      this.silent.setChinese("\u9759\u9ed8\u8865\u7ed9");
      this.fastHotbar = this.add(new BooleanSetting("FastHotbar", true));
      this.fastHotbar.setChinese("\u5feb\u6377\u680f\u79d2\u8865");
      this.oneTick = this.add(new BooleanSetting("OneTick", true));
      this.oneTick.setChinese("\u4e00Tick\u5b8c\u6210");
      this.kitSelection = this.add(new StringSetting("Kit", readActiveKitName()).injectTask(this::applySelectedKit));
      this.kitSelection.setChinese("\u914d\u88c5");
      this.itemsPerTick = this.add(new SliderSetting("ItemsPerTick", 12.0, 1.0, 18.0, 1.0));
      this.itemsPerTick.setChinese("\u6bcfTick\u62ff\u53d6\u6570\u91cf");
      this.overlay.setChinese("\u6e32\u67d3\u754c\u9762");
      this.autoArrange.setChinese("\u5148\u6574\u7406\u518d\u4e22\u5f03");
      this.currentKitName = null;
      this.currentKit = null;
      this.idleTicks = 0;
      this.cachedKitNames = List.of();
      this.activeHandlerSyncId = -1;
      this.readyHandlerSyncId = -1;
      INSTANCE = this;
      this.setChinese("\u8865\u7ed9");
   }

   @Override
   public String getInfo() {
      return this.currentKitName != null && !this.currentKitName.isEmpty() ? this.currentKitName : null;
   }

   @Override
   public void onEnable() {
      Path var1 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/");
      if (!Files.exists(var1)) {
         try {
            Files.createDirectories(var1);
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

      if (this.currentKit == null) {
         this.loadActiveKit();
      }

      this.idleTicks = 0;
      this.resetHandlerState();
      this.autoCrystalWasOn = false;
      this.autoCrystalPaused = false;
   }

   public boolean isHandlingKit() {
      return this.isOn() && this.currentKit != null && !this.currentKit.isEmpty();
   }

   @Override
   public void onDisable() {
      overlayActive = false;
      ShulkerOverlayRenderer.clearPops();
      this.updateAutoCrystalPause(false);
      this.resetHandlerState();
   }

   private void updateAutoCrystalPause(boolean var1) {
      if (!var1) {
         if (this.autoCrystalPaused) {
            if (AutoCrystal.INSTANCE != null) {
               AutoCrystal.INSTANCE.setRefillPause(false);
            }

            if (AutoWeb.INSTANCE != null) {
               AutoWeb.INSTANCE.setRefillPause(false);
            }

            this.autoCrystalWasOn = false;
            this.autoCrystalPaused = false;
         }
      } else {
         if (!this.autoCrystalPaused) {
            this.autoCrystalWasOn = AutoCrystal.INSTANCE != null && AutoCrystal.INSTANCE.isOn();
            this.autoCrystalPaused = true;
         }

         if (AutoCrystal.INSTANCE != null) {
            AutoCrystal.INSTANCE.setRefillPause(true);
         }

         if (AutoWeb.INSTANCE != null) {
            AutoWeb.INSTANCE.setRefillPause(true);
         }
      }
   }

   public Map<Integer, Refill.KitRequirement> getActiveKitRequirements() {
      if (this.currentKit == null) {
         this.loadActiveKit();
      }

      if (this.currentKit != null && !this.currentKit.isEmpty()) {
         HashMap var1 = new HashMap();
         this.currentKit.forEach((var1x, var2) -> var1.put(var1x, new Refill.KitRequirement(var2.id, var2.count, var2.data)));
         return Collections.unmodifiableMap(var1);
      } else {
         return Map.of();
      }
   }

   public static boolean matchesKitRequirement(ItemStack var0, Refill.KitRequirement var1) {
      if (var0 != null && !var0.isEmpty() && var1 != null) {
         String var2 = Registries.ITEM.getId(var0.getItem()).toString();
         if (!var2.equals(var1.id())) {
            return false;
         } else {
            return var0.isOf(Items.END_CRYSTAL) ? true : var1.data() == null || var1.data().isBlank() || var1.data().equals(MioKitBridge.getItemData(var0));
         }
      } else {
         return false;
      }
   }

   @EventListener
   private void onOpenScreen(OpenScreenEvent var1) {
   }

   @EventListener
   private void onInventorySync(InventoryS2CPacketEvent var1) {
      if (mc.player != null && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var2 && var1.packet.getSyncId() == var2.syncId) {
         this.readyHandlerSyncId = var2.syncId;
         if (var2.syncId == this.activeHandlerSyncId && this.movedInHandler) {
            this.awaitingServerAck = false;
            this.emptyPasses = 0;
         }
      }
   }

   @EventListener
   private void onSlotSync(PacketEvent.Receive var1) {
      if (mc.player != null
         && var1.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket var2
         && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var3
         && var3.syncId == this.activeHandlerSyncId
         && var2.getSyncId() == var3.syncId
         && this.movedInHandler) {
         this.awaitingServerAck = false;
         this.emptyPasses = 0;
      }
   }

   public void onCommand(String[] var1) {
      if (var1.length != 0 && !var1[0].isEmpty()) {
         String var2 = var1[0].toLowerCase();
         String var3 = var1.length >= 2 ? var1[1] : null;
         switch (var2) {
            case "save":
               if (var3 == null) {
                  this.sendRefillMessage("\u00a7c\u8bf7\u6307\u5b9a\u5957\u88c5\u540d\u79f0");
                  return;
               }

               this.saveKit(var3);
               break;
            case "remove":
               if (var3 == null) {
                  this.sendRefillMessage("\u00a7c\u8bf7\u6307\u5b9a\u5957\u88c5\u540d\u79f0");
                  return;
               }

               this.removeKit(var3);
               break;
            case "use":
               if (var3 == null) {
                  this.sendRefillMessage("\u00a7c\u8bf7\u6307\u5b9a\u5957\u88c5\u540d\u79f0");
                  return;
               }

               this.useKit(var3);
               break;
            default:
               this.sendRefillMessage("\u00a7c\u672a\u77e5\u64cd\u4f5c: " + var2);
         }
      } else {
         this.sendRefillMessage("\u00a7c\u7528\u6cd5: " + Alien.getPrefix() + "refill save|remove|use <\u540d\u79f0>");
      }
   }

   @EventListener
   public void onTick(TickEvent var1) {
      this.updateOverlayState();
      if (mc.player != null && mc.world != null && !var1.isPost()) {
         BindSetting var2 = this.getBindSetting();
         int var3 = var2.getValue();
         boolean var4 = this.isKeyPressed(var3);
         boolean var5 = var2.isHoldEnable();
         if (var5) {
            if (!var4) {
               if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
                  mc.player.closeHandledScreen();
               }

               if (this.isOn()) {
                  this.setState(false);
               }

               this.idleTicks = 0;
               return;
            }

            if (!this.isOn()) {
               this.setState(true);
            }
         }

         boolean var6 = this.isOn();
         if (!var6) {
            this.idleTicks = 0;
         } else if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var7) {
            this.processOpenShulker(var7, var5);
         } else {
            boolean var9 = AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isOn() && AutoRegear.INSTANCE.getCurrentShulker() != null;
            this.updateAutoCrystalPause(var9);
            this.idleTicks = 0;
            this.resetHandlerState();
         }
      }
   }

   private void processOpenShulker(ShulkerBoxScreenHandler var1, boolean var2) {
      this.updateAutoCrystalPause(true);
      if ((AntiRegear.INSTANCE == null || !AntiRegear.INSTANCE.isControllingShulker()) && this.currentKit != null && !this.currentKit.isEmpty()) {
         if (var1.syncId != this.activeHandlerSyncId) {
            this.activeHandlerSyncId = var1.syncId;
            this.emptyPasses = 0;
            this.contentWaitTicks = 0;
            this.movedInHandler = false;
            this.arrangedInHandler = false;
            this.awaitingServerAck = false;
            this.lastMoveBatchAt = 0L;
            this.refreshOverlayAnchor();
         }

         if (this.readyHandlerSyncId != var1.syncId) {
            this.readyHandlerSyncId = var1.syncId;
         }

         if (this.idleTicks >= 3) {
            this.finishIfIdle(var1, var2);
         } else {
            if (this.awaitingServerAck) {
               if (System.currentTimeMillis() - this.lastMoveBatchAt < 50L) {
                  return;
               }

               this.awaitingServerAck = false;
               this.emptyPasses = 0;
            }

            if (!var1.getCursorStack().isEmpty()) {
               this.recoverCursorStack(var1);
            } else if (!this.hasPendingWork(var1)) {
               this.idleTicks++;
               this.emptyPasses++;
               this.finishIfIdle(var1, var2);
               return;
            } else {
               int var3 = this.oneTick.getValue() ? Integer.MAX_VALUE : this.itemsPerTick.getValueInt();
               int var4 = 0;

               if (this.autoArrange.getValue() && !this.arrangedInHandler) {
                  this.arrangedInHandler = true;
                  if (this.arrangeInventory(var1)) {
                     var4++;
                  }
               }

               boolean var5;
               do {
                  var5 = false;

                  for (int var6 = 0; var6 < 36 && var4 < var3; var6++) {
                     ItemStack var7 = mc.player.getInventory().getStack(var6);
                     if (var7.isEmpty() || !(var7.getItem() instanceof BlockItem var8 && var8.getBlock() instanceof ShulkerBoxBlock)) {
                        Refill.SimpleStack var18 = this.currentKit.get(var6);
                        if (var18 != null) {
                           String var10 = var7.isEmpty() ? null : Registries.ITEM.getId(var7.getItem()).toString();
                           boolean mismatch = !var7.isEmpty() && (var10 == null || !var10.equals(var18.id) || !matchesItemData(var7, var18));
                           if (var7.isEmpty() || mismatch || var7.getCount() < var18.count) {
                              Slot var11 = this.findBestContainerStack(var1, var18);
                              if (var11 != null) {
                                 if (mismatch && !this.discardSlot(var1.syncId, var6)) {
                                    continue;
                                 }

                                 int var12 = this.moveToConfiguredSlot(var1.syncId, var6, var11.id, var18);
                                 if (var12 == 0) {
                                    continue;
                                 }

                                 var5 = true;
                                 var4++;
                              } else if (mismatch) {
                                 if (!this.discardSlot(var1.syncId, var6)) {
                                    continue;
                                 }

                                 var5 = true;
                                 var4++;
                              }

                              if (var5) {
                                 break;
                              }
                           }
                        }
                     }
                  }

                  if (!var5 && var4 < var3) {
                     int var13 = this.findEmptyInventorySlot();
                     if (var13 != -1) {
                        boolean var15 = false;

                        for (Slot var19 : var1.slots) {
                           if (var19.id < 27) {
                              ItemStack var21 = var19.getStack();
                              if (!var21.isEmpty() && this.isHighPriorityWhitelist(var21)) {
                                 int var23 = this.clickMove(var1.syncId, var19.id, var13);
                                 if (var23 != 0) {
                                    var5 = true;
                                    var4++;
                                    var15 = true;
                                 }
                                 break;
                              }
                           }
                        }

                        if (!var15) {
                           for (Slot var20 : var1.slots) {
                              if (var20.id < 27) {
                                 ItemStack var22 = var20.getStack();
                                 if (!var22.isEmpty()) {
                                    String var24 = Registries.ITEM.getId(var22.getItem()).toString();
                                    if (WHITELIST_IDS.contains(var24) || this.isKitItem(var24)) {
                                       int var25 = this.clickMove(var1.syncId, var20.id, var13);
                                       if (var25 != 0) {
                                          var5 = true;
                                          var4++;
                                       }
                                       break;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               } while (var5 && var4 < var3);

               if (var4 > 0) {
                  this.idleTicks = 0;
                  this.emptyPasses = 0;
                  this.movedInHandler = true;
                  this.awaitingServerAck = true;
                  this.lastMoveBatchAt = System.currentTimeMillis();
                  this.finishIfIdle(var1, var2);
               } else {
                  this.idleTicks++;
                  this.emptyPasses++;
                  this.finishIfIdle(var1, var2);
               }
            }
         }
      }
   }

   private void finishIfIdle(ShulkerBoxScreenHandler var1, boolean var2) {
      if (!this.awaitingServerAck
         && var1.getCursorStack().isEmpty()
         && !this.hasPendingWork(var1)
         && (this.silent.getValue() || !var2 && this.autoDisable.getValue())) {
         mc.player.closeHandledScreen();
         this.resetHandlerState();
         if (!var2 && this.autoDisable.getValue()) {
            this.setState(false);
         }
      }
   }

   private boolean hasPendingWork(ShulkerBoxScreenHandler var1) {
      if (!var1.getCursorStack().isEmpty()) {
         return true;
      } else if (this.currentKit == null || this.currentKit.isEmpty()) {
         return false;
      } else if (this.autoArrange.getValue() && !this.arrangedInHandler) {
         return true;
      } else {
         for (int var2 = 0; var2 < 36; var2++) {
            Refill.SimpleStack var3 = this.currentKit.get(var2);
            if (var3 != null) {
               ItemStack var4 = mc.player.getInventory().getStack(var2);
               if (this.isShulkerStack(var4)) {
                  continue;
               }

               if (var4.isEmpty()) {
                  if (this.findBestContainerStack(var1, var3) != null) {
                     return true;
                  }
               } else {
                  String var5 = Registries.ITEM.getId(var4.getItem()).toString();
                  boolean var6 = !var5.equals(var3.id) || !matchesItemData(var4, var3);
                  if (var6) {
                     return true;
                  }

                  if (var4.getCount() < var3.count && this.findBestContainerStack(var1, var3) != null) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private void resetHandlerState() {
      this.activeHandlerSyncId = -1;
      this.readyHandlerSyncId = -1;
      this.emptyPasses = 0;
      this.contentWaitTicks = 0;
      this.movedInHandler = false;
      this.arrangedInHandler = false;
      this.awaitingServerAck = false;
      this.lastMoveBatchAt = 0L;
   }

   private boolean isKeyPressed(int var1) {
      if (var1 <= 0) {
         return false;
      } else {
         long var2 = mc.getWindow().getHandle();
         return InputUtil.isKeyPressed(var2, var1);
      }
   }

   private int moveToConfiguredSlot(int var1, int var2, int var3, Refill.SimpleStack var4) {
      if (!this.isCurrentShulkerHandler(var1)) {
         return 0;
      } else {
         Slot var5 = mc.player.currentScreenHandler.getSlot(var3);
         if (this.canFastHotbarSwap(var2, var5, var4)) {
            return this.clickSlotChecked(var1, var3, var2, SlotActionType.SWAP) ? 1 : 0;
         } else {
            int var6 = this.getScreenSlotId(var2);
            if (!this.clickSlotChecked(var1, var3)) {
               return 0;
            } else if (!this.clickSlotChecked(var1, var6)) {
               return 1;
            } else {
               int var7 = 2;
               if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                  if (!this.clickSlotChecked(var1, var3)) {
                     return var7;
                  }

                  var7++;
               }

               return var7;
            }
         }
      }
   }

   private boolean canFastHotbarSwap(int var1, Slot var2, Refill.SimpleStack var3) {
      return this.fastHotbar.getValue() && var1 >= 0 && var1 < 9 && var2 != null && !var2.getStack().isEmpty() && var2.getStack().getCount() >= var3.count;
   }

   private Slot findBestContainerStack(ShulkerBoxScreenHandler var1, Refill.SimpleStack var2) {
      Slot var3 = null;

      for (Slot var5 : var1.slots) {
         if (var5.id < 27) {
            ItemStack var6 = var5.getStack();
            if (!var6.isEmpty()
               && Registries.ITEM.getId(var6.getItem()).toString().equals(var2.id)
               && matchesItemData(var6, var2)
               && (var3 == null || var6.getCount() > var3.getStack().getCount())) {
               var3 = var5;
            }
         }
      }

      return var3;
   }

   private void recoverCursorStack(ShulkerBoxScreenHandler var1) {
      if (this.isCurrentShulkerHandler(var1.syncId)) {
         ItemStack var2 = var1.getCursorStack();
         if (!var2.isEmpty()) {
            for (int var3 = 0; var3 < 36; var3++) {
               ItemStack var4 = mc.player.getInventory().getStack(var3);
               if (!var4.isEmpty() && ItemStack.areItemsAndComponentsEqual(var4, var2) && var4.getCount() < var4.getMaxCount()) {
                  if (!this.clickSlotChecked(var1.syncId, this.getScreenSlotId(var3))) {
                     return;
                  }

                  if (var1.getCursorStack().isEmpty()) {
                     return;
                  }
               }
            }

            int var5 = this.findEmptyInventorySlot();
            if (var5 != -1) {
               this.clickSlotChecked(var1.syncId, this.getScreenSlotId(var5));
            }
         }
      }
   }

   private int getScreenSlotId(int var1) {
      return (var1 < 9 ? var1 + 36 : var1) + 18;
   }

   private boolean discardSlot(int var1, int var2) {
      return this.isCurrentShulkerHandler(var1)
         && !mc.player.getInventory().getStack(var2).isEmpty()
         && this.clickSlotChecked(var1, this.getScreenSlotId(var2), 1, SlotActionType.THROW);
   }

   /**
    * 按套装预设整理背包：把背包中已有的正确物品换到它应该在的槽位上。
    * 这样后续流程只会丢弃「套装里根本没有」的物品，而不是把只是放错位置的物品丢掉。
    */
   private boolean arrangeInventory(ShulkerBoxScreenHandler var1) {
      boolean var2 = false;
      ItemStack[] snapshot = new ItemStack[36];

      for (int i = 0; i < 36; i++) {
         snapshot[i] = mc.player.getInventory().getStack(i).copy();
      }

      for (int var3 = 0; var3 < 36; var3++) {
         Refill.SimpleStack var4 = this.currentKit.get(var3);
         if (var4 == null) {
            continue;
         }

         ItemStack var5 = snapshot[var3];
         if (this.isShulkerStack(var5) || matchesKitStack(var5, var4)) {
            continue;
         }

         int var6 = this.findArrangeSource(snapshot, var3, var4);
         if (var6 != -1 && this.swapInventorySlotsChecked(var1.syncId, var3, var6)) {
            ItemStack swap = snapshot[var3];
            snapshot[var3] = snapshot[var6];
            snapshot[var6] = swap;
            var2 = true;
         }
      }

      return var2;
   }

   private int findArrangeSource(ItemStack[] snapshot, int var1, Refill.SimpleStack var2) {
      int var3 = -1;
      int var4 = 0;

      for (int var5 = 0; var5 < 36; var5++) {
         if (var5 == var1) {
            continue;
         }

         ItemStack var6 = snapshot[var5];
         if (this.isShulkerStack(var6) || !matchesKitStack(var6, var2)) {
            continue;
         }

         Refill.SimpleStack var7 = this.currentKit.get(var5);
         if (var7 != null && matchesKitStack(var6, var7)) {
            continue;
         }

         if (var3 == -1 || var6.getCount() > var4) {
            var3 = var5;
            var4 = var6.getCount();
         }
      }

      return var3;
   }

   private boolean isShulkerStack(ItemStack var1) {
      return !var1.isEmpty() && var1.getItem() instanceof BlockItem var2 && var2.getBlock() instanceof ShulkerBoxBlock;
   }

   private static boolean matchesKitStack(ItemStack var0, Refill.SimpleStack var1) {
      if (var0 != null && !var0.isEmpty() && var1 != null) {
         String var2 = Registries.ITEM.getId(var0.getItem()).toString();
         return var2.equals(var1.id) && matchesItemData(var0, var1);
      } else {
         return false;
      }
   }

   private boolean swapInventorySlotsChecked(int var1, int var2, int var3) {
      if (!this.isCurrentShulkerHandler(var1)) {
         return false;
      } else {
         int var4 = this.getScreenSlotId(var2);
         int var5 = this.getScreenSlotId(var3);
         if (!this.clickSlotChecked(var1, var4)) {
            return false;
         } else {
            this.clickSlotChecked(var1, var5);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               this.clickSlotChecked(var1, var4);
            }

            return true;
         }
      }
   }

   private void swapInventorySlots(int var1, int var2, int var3) {
      int var4 = (var2 < 9 ? var2 + 36 : var2) + 18;
      int var5 = (var3 < 9 ? var3 + 36 : var3) + 18;
      mc.interactionManager.clickSlot(var1, var4, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(var1, var5, 0, SlotActionType.PICKUP, mc.player);
      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         mc.interactionManager.clickSlot(var1, var4, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   private int clickMove(int var1, int var2, int var3) {
      if (!this.isCurrentShulkerHandler(var1)) {
         return 0;
      } else if (!this.clickSlotChecked(var1, var2)) {
         return 0;
      } else {
         int var4 = (var3 < 9 ? var3 + 36 : var3) + 18;
         if (!this.clickSlotChecked(var1, var4)) {
            return 1;
         } else {
            int var5 = 2;
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               if (!this.clickSlotChecked(var1, var2)) {
                  return var5;
               }

               var5++;
            }

            return var5;
         }
      }
   }

   private boolean clickSlotChecked(int var1, int var2) {
      return this.clickSlotChecked(var1, var2, 0, SlotActionType.PICKUP);
   }

   private boolean clickSlotChecked(int var1, int var2, int var3, SlotActionType var4) {
      if (!this.isCurrentShulkerHandler(var1)) {
         return false;
      } else {
         mc.interactionManager.clickSlot(var1, var2, var3, var4, mc.player);
         return true;
      }
   }

   private boolean isCurrentShulkerHandler(int var1) {
      return mc.player != null
         && mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler var2
         && var2.syncId == var1
         && this.activeHandlerSyncId == var1;
   }

   private boolean isKitItem(String var1) {
      if (this.currentKit == null) {
         return false;
      } else {
         for (Refill.SimpleStack var3 : this.currentKit.values()) {
            if (var3 != null && var1.equals(var3.id)) {
               return true;
            }
         }

         return false;
      }
   }

   private int findEmptyInventorySlot() {
      for (int var1 = 0; var1 < 36; var1++) {
         if (mc.player.getInventory().getStack(var1).isEmpty()) {
            return var1;
         }
      }

      return -1;
   }

   private boolean isHighPriorityWhitelist(ItemStack var1) {
      if (var1.isOf(Items.POTION) || var1.isOf(Items.SPLASH_POTION) || var1.isOf(Items.LINGERING_POTION)) {
         PotionContentsComponent var2 = (PotionContentsComponent)var1.getComponents().get(DataComponentTypes.POTION_CONTENTS);
         if (var2 != null) {
            for (StatusEffectInstance var4 : var2.getEffects()) {
               Identifier var5 = Registries.STATUS_EFFECT.getId((StatusEffect)var4.getEffectType().value());
               if (var5 != null && var5.toString().equals("minecraft:turtle_master") && var4.getAmplifier() == 6) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void loadActiveKit() {
      if (Files.exists(CONFIG_FILE)) {
         try {
            String var1 = Files.readString(CONFIG_FILE).trim();
            if (!var1.isEmpty()) {
               Path var2 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/" + var1 + ".json");
               if (Files.exists(var2)) {
                  this.useKit(var1);
               }
            }
         } catch (IOException var3) {
         }
      }
   }

   private static String readActiveKitName() {
      if (!Files.exists(CONFIG_FILE)) {
         return "";
      } else {
         try {
            return Files.readString(CONFIG_FILE).trim();
         } catch (IOException var1) {
            return "";
         }
      }
   }

   private List<String> getAvailableKits() {
      long var1 = System.currentTimeMillis();
      if (var1 < this.nextKitRefresh) {
         return this.cachedKitNames;
      } else {
         this.nextKitRefresh = var1 + 1000L;
         Path var3 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/");
         TreeSet var4 = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
         if (Files.isDirectory(var3)) {
            try (Stream<Path> var5 = Files.list(var3)) {
               var5.filter(var0 -> Files.isRegularFile(var0))
                  .map(var0 -> var0.getFileName().toString())
                  .filter(var0 -> var0.toLowerCase(Locale.ROOT).endsWith(".json"))
                  .map(var0 -> var0.substring(0, var0.length() - 5))
                  .forEach(var4::add);
            } catch (IOException var10) {
            }
         }

         var4.addAll(MioKitBridge.getKitNames());
         this.cachedKitNames = List.copyOf(var4);
         return this.cachedKitNames;
      }
   }

   private void invalidateKitNames() {
      this.nextKitRefresh = 0L;
   }

   private void applySelectedKit() {
      String var1 = this.kitSelection.getValue().trim();
      if (!var1.isEmpty()) {
         this.useKit(var1);
      }
   }

   private void saveActiveKit(String var1) {
      try {
         Files.createDirectories(CONFIG_FILE.getParent());
         Files.writeString(CONFIG_FILE, var1);
      } catch (IOException var3) {
      }
   }

   private void saveKit(String var1) {
      if (mc.player != null) {
         HashMap var2 = new HashMap();

         for (int var3 = 0; var3 < 36; var3++) {
            ItemStack var4 = mc.player.getInventory().getStack(var3);
            if (!var4.isEmpty()) {
               String var5 = Registries.ITEM.getId(var4.getItem()).toString();
               var2.put(var3, new Refill.SimpleStack(var5, var4.getCount(), MioKitBridge.getItemData(var4)));
            }
         }

         Path var10 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/" + var1 + ".json");

         try {
            Files.createDirectories(var10.getParent());

            try (FileWriter var11 = new FileWriter(var10.toFile())) {
               GSON.toJson(var2, var11);
            }

            this.invalidateKitNames();
            this.sendRefillMessage("\u00a7a\u5957\u88c5 '" + var1 + "' \u5df2\u4fdd\u5b58\u5e76\u6fc0\u6d3b");
            this.useKit(var1);
         } catch (IOException var9) {
            this.sendRefillMessage("\u00a7c\u4fdd\u5b58\u5931\u8d25: " + var9.getMessage());
         }
      }
   }

   private void removeKit(String var1) {
      Path var2 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/" + var1 + ".json");
      boolean var3 = MioKitBridge.removeKit(var1);
      if (Files.exists(var2)) {
         try {
            Files.delete(var2);
            this.sendRefillMessage("\u00a7a\u5957\u88c5 '" + var1 + "' \u5df2\u5220\u9664");
            if (var1.equals(this.currentKitName)) {
               this.currentKitName = null;
               this.currentKit = null;
               this.kitSelection.setValueWithoutTask("");
               this.saveActiveKit("");
            }

            this.invalidateKitNames();
         } catch (IOException var5) {
            this.sendRefillMessage("\u00a7c\u5220\u9664\u5931\u8d25: " + var5.getMessage());
         }
      } else if (!var3) {
         this.sendRefillMessage("\u00a7c\u672a\u627e\u5230\u8be5\u5957\u88c5");
      }
   }

   private void useKit(String var1) {
      Path var2 = MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien/refill/kits/" + var1 + ".json");
      if (!Files.exists(var2)) {
         this.importMioKit(var1, var2);
      }

      if (!Files.exists(var2)) {
         this.sendRefillMessage("\u00a7c\u672a\u627e\u5230\u8be5\u5957\u88c5");
      } else {
         try (FileReader var3 = new FileReader(var2.toFile())) {
            Map var4 = (Map)GSON.fromJson(var3, (new TypeToken<Map<Integer, Refill.SimpleStack>>() {}).getType());
            mergeMioItemData(var1, var4);
            this.currentKitName = var1;
            this.currentKit = var4;
            this.kitSelection.setValueWithoutTask(var1);
            this.saveActiveKit(var1);
            MioKitBridge.syncKit(var1, toMioKit(var4));
            this.sendRefillMessage("\u00a7a\u5957\u88c5 '" + var1 + "' \u5df2\u6fc0\u6d3b");
         } catch (Exception var8) {
            this.sendRefillMessage("\u00a7c\u52a0\u8f7d\u5931\u8d25: " + var8.getMessage());
         }
      }
   }

   private void importMioKit(String var1, Path var2) {
      Map var3 = MioKitBridge.loadKit(var1);
      if (var3 != null && !var3.isEmpty()) {
         try {
            Files.createDirectories(var2.getParent());

            try (FileWriter var4 = new FileWriter(var2.toFile())) {
               GSON.toJson(fromMioKit(var3), var4);
            }

            this.invalidateKitNames();
         } catch (IOException var9) {
         }
      }
   }

   private static boolean matchesItemData(ItemStack var0, Refill.SimpleStack var1) {
      if (var0.isOf(Items.END_CRYSTAL) && "minecraft:end_crystal".equals(var1.id)) {
         return true;
      } else {
         return var1.data != null && !var1.data.isBlank() ? var1.data.equals(MioKitBridge.getItemData(var0)) : true;
      }
   }

   private static Map<Integer, MioKitBridge.KitEntry> toMioKit(Map<Integer, Refill.SimpleStack> var0) {
      HashMap var1 = new HashMap();
      if (var0 == null) {
         return var1;
      } else {
         var0.forEach((var1x, var2) -> var1.put(var1x, new MioKitBridge.KitEntry(var2.id, var2.count, var2.data)));
         return var1;
      }
   }

   private static void mergeMioItemData(String var0, Map<Integer, Refill.SimpleStack> var1) {
      if (var1 != null && !var1.isEmpty()) {
         Map<Integer, MioKitBridge.KitEntry> var2 = MioKitBridge.loadKit(var0);
         if (var2 != null && !var2.isEmpty()) {
            var2.forEach(
               (var1x, var2x) -> {
                  Refill.SimpleStack var3 = var1.get(var1x);
                  if (var3 != null
                     && var3.id.equals(var2x.itemId())
                     && (var3.data == null || var3.data.isBlank())
                     && var2x.data() != null
                     && !var2x.data().isBlank()) {
                     var3.data = var2x.data();
                  }
               }
            );
         }
      }
   }

   private static Map<Integer, Refill.SimpleStack> fromMioKit(Map<Integer, MioKitBridge.KitEntry> var0) {
      HashMap var1 = new HashMap();
      var0.forEach((var1x, var2) -> var1.put(var1x, new Refill.SimpleStack(var2.itemId(), var2.count(), var2.data())));
      return var1;
   }

   private void sendRefillMessage(String var1) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(var1), false);
      }
   }

   public record KitRequirement(String id, int count, String data) {
   }

   private static class SimpleStack {
      String id;
      int count;
      String data;

      SimpleStack(String var1, int var2, String var3) {
         this.id = var1;
         this.count = var2;
         this.data = var3;
      }
   }
}
