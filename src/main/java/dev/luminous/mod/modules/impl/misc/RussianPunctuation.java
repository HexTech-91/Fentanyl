package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RussianPunctuation extends Module {
   public static RussianPunctuation INSTANCE;
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true)).setChinese("\u58f0\u97f3");
   private final SliderSetting clearTime = this.add(new SliderSetting("ClearTime", 10.0, 0.0, 100.0, 0.1).setSuffix("s"))
      .setChinese("\u6e05\u9664\u65f6\u95f4");
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100))).setChinese("\u989c\u8272");
   private final BindSetting enemySpot = this.add(new BindSetting("EnemySpot", -1)).setChinese("\u654c\u4eba\u53d1\u73b0");
   private final StringSetting key = this.add(new StringSetting("EncryptKey", "IDKWTFTHIS")).setChinese("\u52a0\u5bc6\u5bc6\u94a5");
   private final BooleanSetting ignoreAffix = this.add(new BooleanSetting("IgnoreAffix", true)).setChinese("\u65e0\u89c6\u524d\u540e\u7f00");
   private final ConcurrentHashMap<String, RussianPunctuation.Spot> waypoint = new ConcurrentHashMap<>();
   private boolean pressed = false;
   private static final char[] ALPHABET = "\u0431\u043d\u044a\u0445\u043a\u0444\u0448\u0439\u043f\u043b\u0437\u0443\u0435\u044b\u0441\u0446\u0442\u0449\u0436\u0434\u044f\u0438\u044e\u043c\u0432\u044d\u0433\u0440\u0430\u043e\u0447\u044c"
      .toCharArray();
   private static final int[] REVERSE = new int[80];
   private static final byte MAGIC = 77;
   private static final int ENCODED_LENGTH = 26;

   public RussianPunctuation() {
      super("Punctuation", Module.Category.Misc);
      this.setChinese("MX\u6807\u70b9");
      INSTANCE = this;
   }

   public static SecretKeySpec getKey(String myKey) {
      try {
         byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
         MessageDigest sha = MessageDigest.getInstance("SHA-256");
         key = sha.digest(key);
         key = Arrays.copyOf(key, 16);
         return new SecretKeySpec(key, "AES");
      } catch (Exception var3) {
         return null;
      }
   }

   @Override
   public void onDisable() {
      this.waypoint.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.waypoint.values().removeIf(t -> t.timer.passedS(this.clearTime.getValue()));
      if (this.enemySpot.isPressed() && mc.currentScreen == null) {
         HitResult hitResult;
         if (!this.pressed && (hitResult = mc.getCameraEntity().raycast(256.0, 0.0F, false)) instanceof BlockHitResult) {
            BlockHitResult blockHitResult = (BlockHitResult)hitResult;
            BlockPos pos = blockHitResult.getBlockPos();
            byte[] payload = new byte[14];
            payload[0] = 77;
            writeInt(payload, 1, pos.getX());
            writeInt(payload, 5, pos.getY());
            writeInt(payload, 9, pos.getZ());
            payload[13] = checksum(payload);
            String encrypted = this.Encrypt(payload);
            if (encrypted != null && mc.player != null) {
               mc.getNetworkHandler().sendChatMessage(encrypted);
            }

            this.waypoint
               .put("Local", new RussianPunctuation.Spot("Local", new BlockPosX(pos.getX(), pos.getY(), pos.getZ()), this.color.getValue(), new Timer()));
            CommandManager.sendMessage("Marked locally at \u00a7r(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
         }

         this.pressed = true;
      } else {
         this.pressed = false;
      }
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      for (RussianPunctuation.Spot spot : this.waypoint.values()) {
         Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos.toCenterPos().add(0.0, 1.0, 0.0));
         String text = "\u00a7a" + spot.name + " \u00a7f(" + spot.pos.getX() + ", " + spot.pos.getY() + ", " + spot.pos.getZ() + ")";
         if (vector.z > 0.0 && vector.z < 1.0) {
            double posX = vector.x;
            double posY = vector.y;
            double endPosX = Math.max(vector.x, vector.z);
            float diff = (float)(endPosX - posX) / 2.0F;
            float textWidth = mc.textRenderer.getWidth(text);
            float tagX = (float)(posX + diff - textWidth / 4.0F);
            context.getMatrices().push();
            context.getMatrices().scale(0.5F, 0.5F, 1.0F);
            TextRenderer textRenderer = mc.textRenderer;
            int n = (int)tagX * 2;
            Objects.requireNonNull(mc.textRenderer);
            context.drawText(textRenderer, text, n, (int)(posY - 11.0 + 10.799999999999999) * 2, -1, true);
            context.getMatrices().pop();
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      for (RussianPunctuation.Spot spot : this.waypoint.values()) {
         Render3DUtil.drawFill(
            matrixStack, new Box(spot.pos.getX() + 0.25, -60.0, spot.pos.getZ() + 0.25, spot.pos.getX() + 0.75, 320.0, spot.pos.getZ() + 0.75), spot.color
         );
      }
   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         Packet<?> packet2 = event.getPacket();
         GameMessageS2CPacket packet;
         if (packet2 instanceof GameMessageS2CPacket && (packet = (GameMessageS2CPacket)packet2).content() != null) {
            mc.execute(() -> this.receive(packet.content().getString()));
         }

         if ((packet2 = event.getPacket()) instanceof ChatMessageS2CPacket) {
            ChatMessageS2CPacket chatPacket = (ChatMessageS2CPacket)packet2;
            if (chatPacket.unsignedContent() != null) {
               mc.execute(() -> this.receive(chatPacket.unsignedContent().getString()));
            } else {
               mc.execute(() -> this.receive(chatPacket.body().content()));
            }
         }
      }
   }

   public void receive(String s) {
      try {
         if (s == null) {
            return;
         }

         byte[] data = this.findEnemyHere(stripFormat(s), this.ignoreAffix.getValue());
         if (data == null) {
            return;
         }

         if (this.sound.getValue()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }

         int x = readInt(data, 1);
         int y = readInt(data, 5);
         int z = readInt(data, 9);
         Matcher matcher = Pattern.compile("<(.*?)>").matcher(s);
         if (matcher.find()) {
            String sender = matcher.group(1);
            this.waypoint.put(sender, new RussianPunctuation.Spot(sender, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
            CommandManager.sendMessage(sender + " marked at \u00a7r(" + x + ", " + y + ", " + z + ")");
         } else {
            this.waypoint
               .put(MathUtil.random(0.0F, 1.0E9F) + "", new RussianPunctuation.Spot("Unknown", new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
            CommandManager.sendMessage("Unknown marked at \u00a7r(" + x + ", " + y + ", " + z + ")");
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   public byte[] Decrypt(String strToDecrypt) {
      try {
         byte[] data = decodeCyrillic(this.key.getValue(), strToDecrypt);
         if (data != null && data.length != 0 && data.length % 16 == 0) {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = getKey(this.key.getValue());
            byte[] iv = new byte[16];
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(2, secretKey, ivParams);
            return cipher.doFinal(data);
         } else {
            return null;
         }
      } catch (Exception var7) {
         return null;
      }
   }

   public String Encrypt(byte[] payload) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         SecretKeySpec secretKey = getKey(this.key.getValue());
         byte[] iv = new byte[16];
         IvParameterSpec ivParams = new IvParameterSpec(iv);
         cipher.init(1, secretKey, ivParams);
         return encodeCyrillic(this.key.getValue(), cipher.doFinal(payload));
      } catch (Exception var6) {
         return null;
      }
   }

   private static void writeInt(byte[] data, int offset, int value) {
      data[offset] = (byte)(value >>> 24);
      data[offset + 1] = (byte)(value >>> 16);
      data[offset + 2] = (byte)(value >>> 8);
      data[offset + 3] = (byte)value;
   }

   private static int readInt(byte[] data, int offset) {
      return (data[offset] & 0xFF) << 24 | (data[offset + 1] & 0xFF) << 16 | (data[offset + 2] & 0xFF) << 8 | data[offset + 3] & 0xFF;
   }

   private static byte checksum(byte[] data) {
      int result = 0;

      for (int i = 0; i < 13; i++) {
         result ^= data[i] & 255;
      }

      return (byte)result;
   }

   private static boolean isValid(byte[] data) {
      return data != null && data.length == 14 && data[0] == 77 ? data[13] == checksum(data) : false;
   }

   private static String encodeCyrillic(String key, byte[] data) {
      int[] stream = keystream(key, data.length * 8 / 5 + 1);
      StringBuilder sb = new StringBuilder(data.length * 8 / 5 + 1);
      int buffer = 0;
      int bits = 0;
      int index = 0;

      for (byte b : data) {
         buffer = buffer << 8 | b & 255;
         bits += 8;

         while (bits >= 5) {
            bits -= 5;
            int symbol = buffer >> bits & 31;
            sb.append(ALPHABET[symbol + stream[index++] & 31]);
         }

         buffer &= (1 << bits) - 1;
      }

      if (bits > 0) {
         int symbol = buffer << 5 - bits & 31;
         sb.append(ALPHABET[symbol + stream[index++] & 31]);
      }

      return sb.toString();
   }

   private static byte[] decodeCyrillic(String key, String text) {
      if (text.isEmpty()) {
         return null;
      } else {
         int[] stream = keystream(key, text.length());
         byte[] data = new byte[text.length() * 5 / 8];
         int buffer = 0;
         int bits = 0;
         int length = 0;
         int index = 0;

         for (int i = 0; i < text.length(); i++) {
            int value = indexOf(text.charAt(i));
            if (value < 0) {
               return null;
            }

            buffer = buffer << 5 | value - stream[index++] & 31;
            bits += 5;
            if (bits >= 8) {
               bits -= 8;
               data[length++] = (byte)(buffer >> bits & 0xFF);
               buffer &= (1 << bits) - 1;
            }
         }

         if (bits > 0 && buffer != 0) {
            return null;
         } else {
            return length == data.length ? data : Arrays.copyOf(data, length);
         }
      }
   }

   private static int[] keystream(String key, int length) {
      int[] stream = new int[length];

      try {
         byte[] seed = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
         int position = 0;

         for (int counter = 0; position < length; counter++) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(seed);
            digest.update((byte)(counter >>> 24));
            digest.update((byte)(counter >>> 16));
            digest.update((byte)(counter >>> 8));
            digest.update((byte)counter);

            for (byte b : digest.digest()) {
               if (position >= length) {
                  break;
               }

               stream[position++] = b & 31;
            }
         }
      } catch (Exception var11) {
      }

      return stream;
   }

   private static int indexOf(char c) {
      int index = c - 1024;
      return index >= 0 && index < REVERSE.length ? REVERSE[index] : -1;
   }

   private byte[] findEnemyHere(String text, boolean ignoreAffix) {
      if (!ignoreAffix) {
         if (text.length() != 26) {
            return null;
         } else {
            byte[] data = this.Decrypt(text);
            return isValid(data) ? data : null;
         }
      } else {
         for (int start = 0; start < text.length(); start++) {
            if (indexOf(text.charAt(start)) >= 0) {
               int end = start + 26;
               if (end > text.length()) {
                  break;
               }

               boolean valid = true;

               for (int i = start; i < end; i++) {
                  if (indexOf(text.charAt(i)) < 0) {
                     valid = false;
                     break;
                  }
               }

               if (valid) {
                  byte[] data = this.Decrypt(text.substring(start, end));
                  if (isValid(data)) {
                     return data;
                  }
               }
            }
         }

         return null;
      }
   }

   private static String stripFormat(String text) {
      return text.replaceAll("\u00a7[0-9a-fk-or]", "").replaceAll("<[^>]*> ", "");
   }

   static {
      Arrays.fill(REVERSE, -1);
      int i = 0;

      while (i < ALPHABET.length) {
         REVERSE[ALPHABET[i] - 1024] = i++;
      }
   }

   private record Spot(String name, BlockPos pos, Color color, Timer timer) {
   }
}
