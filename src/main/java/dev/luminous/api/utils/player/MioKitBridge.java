package dev.luminous.api.utils.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class MioKitBridge {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path KITS_FILE = MinecraftClient.getInstance().runDirectory.toPath().resolve("mio-fabric/kits.json");
   private static final Path BACKUP_FILE = KITS_FILE.resolveSibling("kits.json.alien-backup");
   private static final String MIO_GLOBALS_CLASS = "me.mioclient.m$$gCTg4HTJY2Co8utRQRaxL4PHgAP30G0J7MUCl48DEM58YoXal9Lmfil6jY3ppiJM3yhoGdok16ansmaaDnKWFyu3lNPiRb6i4";
   private static final String MIO_KIT_MANAGER_CLASS = "me.mioclient.m$$bozWMGG3YDsfjiKSBecrFVSGu4m3Vmr92d1ZfhIfVD2UvsId4MqrMHCFTCShpOQXrtChkILvVY01QuqxS6YXIvDtS4Lghic0p";
   private static final String MIO_KIT_MANAGER_FIELD = "m$$CIlbOx2W6F5qGkwydJ1NHNIG7JxfFbnumalPLvFX60d6YbxDjkoxf5XvT4zgo8Z0gat8KXFGst6IfQwdbZabIAhB7eNv3Vnwa";
   private static volatile Method mioItemDataMethod;
   private static volatile boolean itemDataMethodResolved;

   private MioKitBridge() {
   }

   public static Set<String> getKitNames() {
      JsonObject root = readRoot();
      if (root == null) {
         return Set.of();
      } else {
         Set<String> names = new LinkedHashSet<>();

         for (String key : root.keySet()) {
            if (!"current".equals(key) && root.get(key).isJsonArray()) {
               names.add(key);
            }
         }

         return names;
      }
   }

   public static Map<Integer, MioKitBridge.KitEntry> loadKit(String name) {
      JsonObject root = readRoot();
      return root != null && root.has(name) && root.get(name).isJsonArray() ? readKit(root.getAsJsonArray(name)) : null;
   }

   public static boolean syncKit(String name, Map<Integer, MioKitBridge.KitEntry> kit) {
      JsonObject root = readRoot();
      if (root == null) {
         return false;
      } else {
         Map<Integer, MioKitBridge.ExistingItem> existing = root.has(name) && root.get(name).isJsonArray()
            ? readExistingItems(root.getAsJsonArray(name))
            : Map.of();
         JsonArray slots = new JsonArray();
         kit.entrySet()
            .stream()
            .sorted(Entry.comparingByKey())
            .forEach(entry -> appendSlot(slots, entry.getKey(), entry.getValue(), existing.get(entry.getKey())));
         root.addProperty("current", name);
         root.add(name, slots);
         if (!writeRoot(root)) {
            return false;
         } else {
            reloadRuntimeManager(root);
            return true;
         }
      }
   }

   public static boolean removeKit(String name) {
      JsonObject root = readRoot();
      if (root != null && root.has(name)) {
         root.remove(name);
         if (root.has("current") && name.equals(root.get("current").getAsString())) {
            String fallback = root.entrySet()
               .stream()
               .filter(entry -> !"current".equals(entry.getKey()) && ((JsonElement)entry.getValue()).isJsonArray())
               .map(Entry::getKey)
               .sorted(String.CASE_INSENSITIVE_ORDER)
               .findFirst()
               .orElse("");
            root.addProperty("current", fallback);
         }

         if (!writeRoot(root)) {
            return false;
         } else {
            reloadRuntimeManager(root);
            return true;
         }
      } else {
         return false;
      }
   }

   public static String getItemData(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Method method = resolveMioItemDataMethod();
         if (method != null) {
            try {
               if (method.invoke(null, stack) instanceof String data && !data.isBlank()) {
                  return data;
               }
            } catch (ReflectiveOperationException var7) {
            }
         }

         PotionContentsComponent potion = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (potion == null) {
            return null;
         } else {
            StringBuilder data = new StringBuilder();

            for (StatusEffectInstance effect : potion.getEffects()) {
               Identifier id = Registries.STATUS_EFFECT.getId((StatusEffect)effect.getEffectType().value());
               if (id != null) {
                  data.append("effect.").append(id.getNamespace()).append('.').append(id.getPath()).append(' ').append(effect.getAmplifier() + 1);
               }
            }

            return data.isEmpty() ? null : data.toString();
         }
      } else {
         return null;
      }
   }

   private static void appendSlot(JsonArray slots, int slot, MioKitBridge.KitEntry entry, MioKitBridge.ExistingItem existing) {
      Identifier id = Identifier.tryParse(entry.itemId());
      if (id != null) {
         Item item = (Item)Registries.ITEM.get(id);
         if (item != Items.AIR || "minecraft:air".equals(entry.itemId())) {
            String data = entry.data();
            if ((data == null || data.isBlank()) && existing != null && existing.rawId == Item.getRawId(item)) {
               data = existing.data;
            }

            if (data == null || data.isBlank()) {
               MinecraftClient client = MinecraftClient.getInstance();
               if (client.player != null && slot >= 0 && slot < 36) {
                  ItemStack stack = client.player.getInventory().getStack(slot);
                  if (!stack.isEmpty() && stack.getItem() == item) {
                     data = getItemData(stack);
                  }
               }
            }

            JsonObject itemObject = new JsonObject();
            itemObject.addProperty("id", Item.getRawId(item));
            if (data != null && !data.isBlank()) {
               itemObject.addProperty("data", data);
            }

            JsonObject slotObject = new JsonObject();
            slotObject.addProperty("slot", slot);
            slotObject.add("item", itemObject);
            slots.add(slotObject);
         }
      }
   }

   private static Map<Integer, MioKitBridge.KitEntry> readKit(JsonArray array) {
      Map<Integer, MioKitBridge.KitEntry> kit = new LinkedHashMap<>();

      for (JsonElement element : array) {
         if (element.isJsonObject()) {
            JsonObject slotObject = element.getAsJsonObject();
            if (slotObject.has("slot") && slotObject.has("item") && slotObject.get("item").isJsonObject()) {
               JsonObject itemObject = slotObject.getAsJsonObject("item");
               if (itemObject.has("id")) {
                  Item item = (Item)Registries.ITEM.get(itemObject.get("id").getAsInt());
                  if (item != null && item != Items.AIR) {
                     String id = Registries.ITEM.getId(item).toString();
                     String data = itemObject.has("data") ? itemObject.get("data").getAsString() : null;
                     int count = Math.max(1, item.getDefaultStack().getMaxCount());
                     kit.put(slotObject.get("slot").getAsInt(), new MioKitBridge.KitEntry(id, count, data));
                  }
               }
            }
         }
      }

      return kit;
   }

   private static Map<Integer, MioKitBridge.ExistingItem> readExistingItems(JsonArray array) {
      Map<Integer, MioKitBridge.ExistingItem> items = new LinkedHashMap<>();

      for (JsonElement element : array) {
         if (element.isJsonObject()) {
            JsonObject slotObject = element.getAsJsonObject();
            if (slotObject.has("slot") && slotObject.has("item") && slotObject.get("item").isJsonObject()) {
               JsonObject itemObject = slotObject.getAsJsonObject("item");
               if (itemObject.has("id")) {
                  String data = itemObject.has("data") ? itemObject.get("data").getAsString() : null;
                  items.put(slotObject.get("slot").getAsInt(), new MioKitBridge.ExistingItem(itemObject.get("id").getAsInt(), data));
               }
            }
         }
      }

      return items;
   }

   private static JsonObject readRoot() {
      if (!Files.isRegularFile(KITS_FILE)) {
         return null;
      } else {
         try {
            JsonElement parsed = JsonParser.parseString(Files.readString(KITS_FILE, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
         } catch (Exception var1) {
            return null;
         }
      }
   }

   private static boolean writeRoot(JsonObject root) {
      Path temp = KITS_FILE.resolveSibling("kits.json.alien-tmp");

      try {
         Files.createDirectories(KITS_FILE.getParent());
         if (Files.isRegularFile(KITS_FILE)) {
            Files.copy(KITS_FILE, BACKUP_FILE, StandardCopyOption.REPLACE_EXISTING);
         }

         Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);

         try {
            Files.move(temp, KITS_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var5) {
            Files.move(temp, KITS_FILE, StandardCopyOption.REPLACE_EXISTING);
         }

         return true;
      } catch (IOException var6) {
         try {
            Files.deleteIfExists(temp);
         } catch (IOException var4) {
         }

         return false;
      }
   }

   private static void reloadRuntimeManager(JsonObject root) {
      try {
         Class<?> globalsClass = Class.forName(
            "me.mioclient.m$$gCTg4HTJY2Co8utRQRaxL4PHgAP30G0J7MUCl48DEM58YoXal9Lmfil6jY3ppiJM3yhoGdok16ansmaaDnKWFyu3lNPiRb6i4"
         );
         Field managerField = globalsClass.getDeclaredField(
            "m$$CIlbOx2W6F5qGkwydJ1NHNIG7JxfFbnumalPLvFX60d6YbxDjkoxf5XvT4zgo8Z0gat8KXFGst6IfQwdbZabIAhB7eNv3Vnwa"
         );
         managerField.setAccessible(true);
         Object manager = managerField.get(null);
         if (manager == null) {
            return;
         }

         Method fromJson = manager.getClass().getMethod("fromJson", JsonElement.class);
         fromJson.invoke(manager, root.deepCopy());
      } catch (LinkageError | ReflectiveOperationException var5) {
      }
   }

   private static Method resolveMioItemDataMethod() {
      if (itemDataMethodResolved) {
         return mioItemDataMethod;
      } else {
         synchronized (MioKitBridge.class) {
            if (itemDataMethodResolved) {
               return mioItemDataMethod;
            } else {
               try {
                  Class<?> managerClass = Class.forName(
                     "me.mioclient.m$$bozWMGG3YDsfjiKSBecrFVSGu4m3Vmr92d1ZfhIfVD2UvsId4MqrMHCFTCShpOQXrtChkILvVY01QuqxS6YXIvDtS4Lghic0p"
                  );
                  List<Method> candidates = new ArrayList<>();

                  for (Method method : managerClass.getDeclaredMethods()) {
                     if (Modifier.isStatic(method.getModifiers())
                        && method.getReturnType() == String.class
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == ItemStack.class) {
                        candidates.add(method);
                     }
                  }

                  candidates.sort(Comparator.comparing(Method::getName));
                  if (!candidates.isEmpty()) {
                     Method methodx = candidates.getFirst();
                     methodx.setAccessible(true);
                     mioItemDataMethod = methodx;
                  }
               } catch (LinkageError | ReflectiveOperationException var8) {
               }

               itemDataMethodResolved = true;
               return mioItemDataMethod;
            }
         }
      }
   }

   private record ExistingItem(int rawId, String data) {
   }

   public record KitEntry(String itemId, int count, String data) {
   }
}
