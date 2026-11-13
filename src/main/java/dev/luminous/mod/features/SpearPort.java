package dev.luminous.mod.features;

import dev.luminous.api.utils.Wrapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.TridentItem;
import net.minecraft.item.Item.Settings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SpearPort {
   private static final RegistryKey<ItemGroup> COMBAT_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "combat"));
   public static final int MAX_USE_TIME = 72000;
   private static final int VIA_WOODEN_SPEAR_ID = 1536;
   private static final int VIA_STONE_SPEAR_ID = 1537;
   private static final int VIA_COPPER_SPEAR_ID = 1538;
   private static final int VIA_IRON_SPEAR_ID = 1539;
   private static final int VIA_GOLDEN_SPEAR_ID = 1540;
   private static final int VIA_DIAMOND_SPEAR_ID = 1541;
   private static final int VIA_NETHERITE_SPEAR_ID = 1542;
   public static final Item WOODEN_SPEAR = register("wooden_spear", ToolMaterials.WOOD, 3, -2.9F);
   public static final Item STONE_SPEAR = register("stone_spear", ToolMaterials.STONE, 4, -2.9F);
   public static final Item COPPER_SPEAR = register("copper_spear", ToolMaterials.STONE, 4, -2.9F);
   public static final Item IRON_SPEAR = register("iron_spear", ToolMaterials.IRON, 5, -2.9F);
   public static final Item GOLDEN_SPEAR = register("golden_spear", ToolMaterials.GOLD, 3, -2.9F);
   public static final Item DIAMOND_SPEAR = register("diamond_spear", ToolMaterials.DIAMOND, 6, -2.9F);
   public static final Item NETHERITE_SPEAR = register("netherite_spear", ToolMaterials.NETHERITE, 7, -2.9F);
   public static final SoundEvent SPEAR_USE = registerSound("item.spear.use");
   public static final SoundEvent SPEAR_HIT = registerSound("item.spear.hit");
   public static final SoundEvent SPEAR_ATTACK = registerSound("item.spear.attack");
   public static final SoundEvent WOODEN_SPEAR_USE = registerSound("item.spear_wood.use");
   public static final SoundEvent WOODEN_SPEAR_HIT = registerSound("item.spear_wood.hit");
   public static final SoundEvent WOODEN_SPEAR_ATTACK = registerSound("item.spear_wood.attack");
   public static final SoundEvent SPEAR_LUNGE_1 = registerSound("item.spear.lunge_1");
   public static final SoundEvent SPEAR_LUNGE_2 = registerSound("item.spear.lunge_2");
   public static final SoundEvent SPEAR_LUNGE_3 = registerSound("item.spear.lunge_3");
   private static final List<Item> SPEARS = Collections.emptyList();
   private static boolean initialized;
   private static final ThreadLocal<Boolean> IN_HAND_RENDER = ThreadLocal.withInitial(() -> false);
   private static long lastFallbackSpearAttackTick = -20L;
   private static boolean stoppingAfterFallbackAttack;
   private static final int MIN_FALLBACK_CHARGE_TICKS = 8;
   private static final int FALLBACK_CONTACT_COOLDOWN_TICKS = 10;
   private static final double FALLBACK_SWEEP_RANGE = 5.25;
   private static final double FALLBACK_DIRECT_HIT_RANGE = 3.1;
   private static final double LUNGE_IMPULSE_PER_LEVEL = 0.458;
   private static final int DEFAULT_KINETIC_DELAY_TICKS = 8;

   private SpearPort() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
      }
   }

   public static boolean isSpear(Item var0) {
      return false;
   }

   public static boolean isSpear(ItemStack var0) {
      return getVisualSpear(var0) != null;
   }

   public static ItemStack getVisualStack(ItemStack var0) {
      Item var1 = getVisualSpear(var0);
      return var1 != null && !var0.isOf(var1) ? var0.copyComponentsToNewStack(var1, var0.getCount()) : var0;
   }

   public static void setInHandRender(boolean var0) {
      IN_HAND_RENDER.set(var0);
   }

   public static boolean isInHandRender() {
      return IN_HAND_RENDER.get();
   }

   public static Identifier getSpearModelId(ItemStack var0) {
      Item var1 = getVisualSpear(var0);
      if (var1 == null) {
         return null;
      } else {
         Identifier var2 = Registries.ITEM.getId(var1);
         if (var2 == null) {
            return null;
         } else {
            String var3 = var2.getPath();
            if (isInHandRender()) {
               var3 = var3 + "_in_hand";
            }

            return Identifier.of(var2.getNamespace(), var3);
         }
      }
   }

   public static Item getVisualSpear(ItemStack var0) {
      return null;
   }

   public static int getKineticDelayTicks(ItemStack var0) {
      return isSpear(var0) ? 8 : 0;
   }

   public static int getMaxKineticUseTicks(ItemStack var0) {
      Item var1 = getVisualSpear(var0);
      if (var1 == WOODEN_SPEAR) {
         return 300;
      } else if (var1 == STONE_SPEAR || var1 == GOLDEN_SPEAR) {
         return 260;
      } else if (var1 == COPPER_SPEAR) {
         return 240;
      } else if (var1 == IRON_SPEAR) {
         return 220;
      } else if (var1 == DIAMOND_SPEAR) {
         return 200;
      } else {
         return var1 == NETHERITE_SPEAR ? 160 : 0;
      }
   }

   public static int findSpearHotbarSlot() {
      if (Wrapper.mc.player == null) {
         return -1;
      } else {
         for (int var0 = 0; var0 < 9; var0++) {
            ItemStack var1 = Wrapper.mc.player.getInventory().getStack(var0);
            if (isSpear(var1)) {
               return var0;
            }
         }

         return -1;
      }
   }

   public static void onStoppedUsing(ItemStack var0, World var1, LivingEntity var2, int var3) {
      if (!stoppingAfterFallbackAttack) {
         sendReleasePacket(var2);
      }
   }

   public static void onUsageTick(ItemStack var0, World var1, LivingEntity var2, int var3) {
      PlayerEntity var4;
      if (var1 != null
         && var2 != null
         && var1.isClient
         && 72000 - var3 >= 8
         && var2 instanceof PlayerEntity
         && (var4 = (PlayerEntity)var2) == MinecraftClient.getInstance().player) {
         tryFallbackSpearContact(var4, var0, 72000 - var3);
      }
   }

   private static void sendReleasePacket(LivingEntity var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null && var1.player != null && var1.getNetworkHandler() != null && var0 == var1.player) {
         var1.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, var0.getBlockPos(), var0.getHorizontalFacing()));
      }
   }

   private static void tryFallbackSpearContact(PlayerEntity var0, ItemStack var1, int var2) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.world != null && var3.interactionManager != null && var3.getNetworkHandler() != null) {
         long var4 = var3.world.getTime();
         double var6 = getFallbackSpearSpeed(var0);
         Entity var8;
         if (var4 - lastFallbackSpearAttackTick >= 10L && (var8 = findFallbackSpearTarget(var3, var2, var6)) != null) {
            var3.interactionManager.attackEntity(var0, var8);
            var0.swingHand(Hand.MAIN_HAND);
            playHitSound(var3.world, var3.player, var1);
            applyLungeAfterHit(var3, var0, var1);
            lastFallbackSpearAttackTick = var4;
            stopUsingAfterFallbackAttack(var3, var0);
         }
      }
   }

   private static Entity findFallbackSpearTarget(MinecraftClient var0, int var1, double var2) {
      if (var0.world != null && var0.player != null) {
         Vec3d var4 = getFallbackSpearDirection(var0.player);
         double var5 = getFallbackSpearRange(var1, var2);
         Vec3d var7 = var0.player.getEyePos();
         Vec3d var8 = var7.add(var4.multiply(var5));
         Box var9 = var0.player.getBoundingBox().stretch(var4.multiply(var5)).expand(1.25, 0.8, 1.25);
         Entity var10 = null;
         double var11 = Double.MAX_VALUE;

         for (Entity var14 : var0.world.getOtherEntities(var0.player, var9, SpearPort::canFallbackSpearHit)) {
            Box var15 = var14.getBoundingBox().expand(0.65);
            boolean var16 = var15.raycast(var7, var8).isPresent();
            boolean var17 = var14.squaredDistanceTo(var0.player) <= 9.610000000000001;
            double var18;
            if ((var16 || var17) && (var18 = var14.squaredDistanceTo(var0.player)) < var11) {
               var10 = var14;
               var11 = var18;
            }
         }

         return var10;
      } else {
         return null;
      }
   }

   private static double getFallbackSpearSpeed(PlayerEntity var0) {
      Vec3d var1 = var0.getVelocity();
      return new Vec3d(var1.x, 0.0, var1.z).length() * 20.0;
   }

   private static Vec3d getFallbackSpearDirection(PlayerEntity var0) {
      Vec3d var1 = var0.getVelocity();
      Vec3d var2 = new Vec3d(var1.x, 0.0, var1.z);
      return var2.lengthSquared() > 0.0025 ? var2.normalize() : var0.getRotationVec(1.0F).normalize();
   }

   private static double getFallbackSpearRange(int var0, double var1) {
      double var3 = Math.min(5.25, 2.75 + Math.max(0, var0 - 8) * 0.08);
      return Math.max(3.1, Math.min(var3, 2.25 + var1 * 0.45));
   }

   private static boolean canFallbackSpearHit(Entity var0) {
      return var0 instanceof LivingEntity && var0.isAlive() && !var0.isSpectator() && var0.canHit();
   }

   private static void stopUsingAfterFallbackAttack(MinecraftClient var0, PlayerEntity var1) {
      stoppingAfterFallbackAttack = true;

      try {
         sendReleasePacket(var1);
         var1.stopUsingItem();
      } finally {
         stoppingAfterFallbackAttack = false;
      }
   }

   private static void applyLungeAfterHit(MinecraftClient var0, PlayerEntity var1, ItemStack var2) {
      int var3 = getLungeLevel(var2);
      if (var3 > 0 && !var1.hasVehicle() && !var1.isFallFlying() && !var1.isTouchingWater()) {
         Vec3d var4 = var1.getRotationVec(1.0F);
         Vec3d var5 = new Vec3d(var4.x, 0.0, var4.z);
         if (!(var5.lengthSquared() < 1.0E-4)) {
            Vec3d var6 = var5.normalize().multiply(0.458 * Math.min(var3, 3));
            var1.addVelocity(var6.x, 0.0, var6.z);
            var1.velocityModified = true;
            playLungeSound(var0.world, var1);
         }
      }
   }

   private static int getLungeLevel(ItemStack var0) {
      int var1 = getLungeLevelFromEnchantments(var0.getEnchantments());
      if (var1 > 0) {
         return var1;
      } else {
         ItemEnchantmentsComponent var2 = (ItemEnchantmentsComponent)var0.get(DataComponentTypes.STORED_ENCHANTMENTS);
         var1 = getLungeLevelFromEnchantments(var2);
         if (var1 > 0) {
            return var1;
         } else {
            String var3 = getMarkerText(var0);
            if (!var3.contains("lunge") && !var3.contains("\u7a81\u8fdb")) {
               return var3.contains("1.21.11") && var0.hasEnchantments() ? 1 : 0;
            } else {
               return readLungeLevelFromMarker(var3);
            }
         }
      }
   }

   private static int getLungeLevelFromEnchantments(ItemEnchantmentsComponent var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (Entry var2 : var0.getEnchantmentEntries()) {
            String var3 = ((RegistryEntry)var2.getKey()).getIdAsString().toLowerCase(Locale.ROOT);
            if (var3.equals("minecraft:lunge") || var3.endsWith(":lunge") || var3.endsWith("/lunge")) {
               return Math.max(1, var2.getIntValue());
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static int readLungeLevelFromMarker(String var0) {
      if (!var0.contains("lunge:3") && !var0.contains("lunge 3") && !var0.contains("lvl:3") && !var0.contains("level:3")) {
         return !var0.contains("lunge:2") && !var0.contains("lunge 2") && !var0.contains("lvl:2") && !var0.contains("level:2") ? 1 : 2;
      } else {
         return 3;
      }
   }

   private static void playLungeSound(World var0, PlayerEntity var1) {
      if (var0 != null && var1 != null && var0.isClient) {
         SoundEvent var2 = switch ((int)(Math.random() * 3.0)) {
            case 0 -> SPEAR_LUNGE_1;
            case 1 -> SPEAR_LUNGE_2;
            default -> SPEAR_LUNGE_3;
         };
         var0.playSound(var1, var1.getX(), var1.getY(), var1.getZ(), var2, SoundCategory.PLAYERS, 1.0F, 1.0F);
      }
   }

   public static void playUseSound(World var0, LivingEntity var1, ItemStack var2) {
      playSound(var0, var1, getUseSound(var2), 0.85F, 1.0F);
   }

   public static void playAttackSound(World var0, LivingEntity var1, ItemStack var2) {
      playSound(var0, var1, getAttackSound(var2), 1.0F, 1.0F);
   }

   public static void playHitSound(World var0, LivingEntity var1, ItemStack var2) {
      playSound(var0, var1, getHitSound(var2), 1.0F, 1.0F);
   }

   private static void playSound(World var0, LivingEntity var1, SoundEvent var2, float var3, float var4) {
      if (var0 != null && var1 != null && var0.isClient) {
         PlayerEntity var5;
         var0.playSound(
            var1 instanceof PlayerEntity ? (var5 = (PlayerEntity)var1) : null, var1.getX(), var1.getY(), var1.getZ(), var2, SoundCategory.PLAYERS, var3, var4
         );
      }
   }

   private static SoundEvent getUseSound(ItemStack var0) {
      return getVisualSpear(var0) == WOODEN_SPEAR ? WOODEN_SPEAR_USE : SPEAR_USE;
   }

   private static SoundEvent getAttackSound(ItemStack var0) {
      return getVisualSpear(var0) == WOODEN_SPEAR ? WOODEN_SPEAR_ATTACK : SPEAR_ATTACK;
   }

   private static SoundEvent getHitSound(ItemStack var0) {
      return getVisualSpear(var0) == WOODEN_SPEAR ? WOODEN_SPEAR_HIT : SPEAR_HIT;
   }

   public static Text getNameOverride(ItemStack var0) {
      Item var1 = getVisualSpear(var0);
      String var2 = getMarkerText(var0);
      if (var1 == null) {
         var1 = getSpearByMarker(var2);
      }

      if (var1 == null) {
         return null;
      } else {
         return !var2.contains("added_custom_name")
               && !var2.contains("1.21.11")
               && !var2.contains("netherie")
               && !var2.contains("spare")
               && !var2.contains("spaer")
            ? null
            : Text.translatable(var1.getTranslationKey());
      }
   }

   private static String getMarkerText(ItemStack var0) {
      StringBuilder var1 = new StringBuilder();
      appendTextComponent(var1, (Text)var0.get(DataComponentTypes.ITEM_NAME));
      appendTextComponent(var1, (Text)var0.get(DataComponentTypes.CUSTOM_NAME));
      NbtComponent var2 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 != null && !var2.isEmpty()) {
         NbtCompound var3 = var2.copyNbt();
         var1.append(' ').append(var3);

         for (String var5 : var3.getKeys()) {
            var1.append(' ').append(var5);
            if (var3.contains(var5, 8)) {
               var1.append(' ').append(var3.getString(var5));
            }
         }
      }

      CustomModelDataComponent var7;
      if ((var7 = (CustomModelDataComponent)var0.get(DataComponentTypes.CUSTOM_MODEL_DATA)) != null) {
         var1.append(" custom_model_data=").append(var7.value());
      }

      LoreComponent var8;
      if ((var8 = (LoreComponent)var0.get(DataComponentTypes.LORE)) != null) {
         for (Text var6 : var8.lines()) {
            appendTextComponent(var1, var6);
         }
      }

      return var1.toString().toLowerCase(Locale.ROOT);
   }

   private static void appendTextComponent(StringBuilder var0, Text var1) {
      if (var1 != null) {
         var0.append(' ').append(var1.getString());
      }
   }

   private static Item getViaOriginalSpear(ItemStack var0) {
      NbtComponent var1 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
      if (var1 != null && !var1.isEmpty()) {
         Integer var2 = findViaOriginalItemId(var1.copyNbt());
         return var2 == null ? null : getSpearByViaId(var2);
      } else {
         return null;
      }
   }

   private static Integer findViaOriginalItemId(NbtCompound var0) {
      for (String var2 : var0.getKeys()) {
         byte var3 = var0.getType(var2);
         String var4 = var2.toLowerCase(Locale.ROOT);
         if (isViaIdKey(var4) && isNumericNbt(var3)) {
            int var9 = switch (var3) {
               case 1 -> var0.getByte(var2);
               case 2 -> var0.getShort(var2);
               case 3 -> var0.getInt(var2);
               case 4 -> (int)var0.getLong(var2);
               default -> Integer.MIN_VALUE;
            };
            if (getSpearByViaId(var9) != null) {
               return var9;
            }
         } else if (isViaIdKey(var4) && var3 == 8) {
            Item var8 = getSpearByIdentifier(var0.getString(var2));
            if (var8 != null) {
               return getViaIdBySpear(var8);
            }
         } else if (var3 == 10) {
            Integer var5 = findViaOriginalItemId(var0.getCompound(var2));
            if (var5 != null) {
               return var5;
            }
         } else {
            Integer var7;
            if (var3 == 9 && (var7 = findViaOriginalItemId(var0.getList(var2, 10))) != null) {
               return var7;
            }
         }
      }

      return null;
   }

   private static Integer findViaOriginalItemId(NbtList var0) {
      for (int var1 = 0; var1 < var0.size(); var1++) {
         NbtElement var2 = var0.get(var1);
         Integer var3;
         if (var2 != null && var2.getType() == 10 && (var3 = findViaOriginalItemId(var0.getCompound(var1))) != null) {
            return var3;
         }
      }

      return null;
   }

   private static boolean isViaSpearBaseItem(ItemStack var0) {
      return var0.isOf(Items.NETHERITE_SWORD)
         || var0.isOf(Items.DIAMOND_SWORD)
         || var0.isOf(Items.IRON_SWORD)
         || var0.isOf(Items.GOLDEN_SWORD)
         || var0.isOf(Items.STONE_SWORD)
         || var0.isOf(Items.WOODEN_SWORD)
         || var0.isOf(Items.TRIDENT)
         || var0.isOf(Items.STONE);
   }

   private static boolean isViaIdKey(String var0) {
      return var0.equals("id")
         || var0.endsWith("|id")
         || var0.endsWith(":id")
         || var0.endsWith(".id")
         || var0.contains("original")
         || var0.contains("identifier");
   }

   private static boolean isNumericNbt(byte var0) {
      return var0 == 1 || var0 == 2 || var0 == 3 || var0 == 4;
   }

   private static Item getSpearByCustomModelData(ItemStack var0) {
      CustomModelDataComponent var1 = (CustomModelDataComponent)var0.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      return var1 == null ? null : getSpearByViaId(var1.value());
   }

   private static Item getSpearByMarker(String var0) {
      if (var0 != null && (var0.contains("spear") || var0.contains("spare") || var0.contains("spaer"))) {
         if (var0.contains("wooden_spear") || var0.contains("wooden spear") || var0.contains("wooden spare") || var0.contains("wooden spaer")) {
            return WOODEN_SPEAR;
         } else if (var0.contains("stone_spear") || var0.contains("stone spear") || var0.contains("stone spare") || var0.contains("stone spaer")) {
            return STONE_SPEAR;
         } else if (var0.contains("copper_spear") || var0.contains("copper spear") || var0.contains("copper spare") || var0.contains("copper spaer")) {
            return COPPER_SPEAR;
         } else if (var0.contains("iron_spear") || var0.contains("iron spear") || var0.contains("iron spare") || var0.contains("iron spaer")) {
            return IRON_SPEAR;
         } else if (var0.contains("golden_spear") || var0.contains("golden spear") || var0.contains("golden spare") || var0.contains("golden spaer")) {
            return GOLDEN_SPEAR;
         } else if (var0.contains("diamond_spear") || var0.contains("diamond spear") || var0.contains("diamond spare") || var0.contains("diamond spaer")) {
            return DIAMOND_SPEAR;
         } else if (!var0.contains("netherite_spear")
            && !var0.contains("netherite spear")
            && !var0.contains("netherite spare")
            && !var0.contains("netherite spaer")
            && !var0.contains("netherie spear")
            && !var0.contains("netherie spare")
            && !var0.contains("netherie spaer")) {
            return var0.contains("1.21.11") ? NETHERITE_SPEAR : null;
         } else {
            return NETHERITE_SPEAR;
         }
      } else {
         return null;
      }
   }

   private static Item getSpearByIdentifier(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0.toLowerCase(Locale.ROOT).replace("minecraft:", "");

         return switch (var1) {
            case "wooden_spear" -> WOODEN_SPEAR;
            case "stone_spear" -> STONE_SPEAR;
            case "copper_spear" -> COPPER_SPEAR;
            case "iron_spear" -> IRON_SPEAR;
            case "golden_spear" -> GOLDEN_SPEAR;
            case "diamond_spear" -> DIAMOND_SPEAR;
            case "netherite_spear" -> NETHERITE_SPEAR;
            default -> null;
         };
      }
   }

   private static Item getSpearByRegistryId(Item var0) {
      if (SPEARS.contains(var0)) {
         return var0;
      } else {
         Identifier var1 = Registries.ITEM.getId(var0);
         return var1 == null ? null : getSpearByIdentifier(var1.toString());
      }
   }

   private static Item getSpearByViaId(int var0) {
      return switch (var0) {
         case 1536 -> WOODEN_SPEAR;
         case 1537 -> STONE_SPEAR;
         case 1538 -> COPPER_SPEAR;
         case 1539 -> IRON_SPEAR;
         case 1540 -> GOLDEN_SPEAR;
         case 1541 -> DIAMOND_SPEAR;
         case 1542 -> NETHERITE_SPEAR;
         default -> null;
      };
   }

   private static Integer getViaIdBySpear(Item var0) {
      if (var0 == WOODEN_SPEAR) {
         return 1536;
      } else if (var0 == STONE_SPEAR) {
         return 1537;
      } else if (var0 == COPPER_SPEAR) {
         return 1538;
      } else if (var0 == IRON_SPEAR) {
         return 1539;
      } else if (var0 == GOLDEN_SPEAR) {
         return 1540;
      } else if (var0 == DIAMOND_SPEAR) {
         return 1541;
      } else {
         return var0 == NETHERITE_SPEAR ? 1542 : null;
      }
   }

   private static Item register(String var0, ToolMaterial var1, int var2, float var3) {
      return null;
   }

   private static SoundEvent registerSound(String var0) {
      Identifier var1 = Identifier.of("minecraft", var0);
      return Registries.SOUND_EVENT.containsId(var1)
         ? (SoundEvent)Registries.SOUND_EVENT.get(var1)
         : (SoundEvent)Registry.register(Registries.SOUND_EVENT, var1, SoundEvent.of(var1));
   }

   private static Settings settings(ToolMaterial var0, int var1, float var2) {
      Settings var3 = new Settings()
         .maxDamage(var0.getDurability())
         .attributeModifiers(SwordItem.createAttributeModifiers(var0, var1, var2))
         .component(DataComponentTypes.TOOL, TridentItem.createToolComponent());
      if (var0 == ToolMaterials.NETHERITE) {
         var3.fireproof();
      }

      return var3;
   }

   public static String getSpearMaterialName(ItemStack var0) {
      Item var1 = getVisualSpear(var0);
      if (var1 == null) {
         return null;
      } else if (var1 == WOODEN_SPEAR) {
         return "wooden";
      } else if (var1 == STONE_SPEAR) {
         return "stone";
      } else if (var1 == COPPER_SPEAR) {
         return "copper";
      } else if (var1 == IRON_SPEAR) {
         return "iron";
      } else if (var1 == GOLDEN_SPEAR) {
         return "golden";
      } else if (var1 == DIAMOND_SPEAR) {
         return "diamond";
      } else {
         return var1 == NETHERITE_SPEAR ? "netherite" : null;
      }
   }
}
