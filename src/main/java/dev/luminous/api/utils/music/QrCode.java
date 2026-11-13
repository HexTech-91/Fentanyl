package dev.luminous.api.utils.music;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;

public final class QrCode {
   private QrCode() {
   }

   public static BufferedImage encode(String var0, int var1) {
      try {
         QRCodeWriter var2 = new QRCodeWriter();
         EnumMap var11 = new EnumMap<>(EncodeHintType.class);
         var11.put(EncodeHintType.MARGIN, 1);
         var11.put(EncodeHintType.CHARACTER_SET, "UTF-8");
         BitMatrix var12 = var2.encode(var0, BarcodeFormat.QR_CODE, var1, var1, var11);
         int var5 = var12.getWidth();
         int var6 = var12.getHeight();
         BufferedImage var7 = new BufferedImage(var5, var6, 1);

         for (int var8 = 0; var8 < var6; var8++) {
            for (int var9 = 0; var9 < var5; var9++) {
               var7.setRGB(var9, var8, var12.get(var9, var8) ? -16777216 : -1);
            }
         }

         return var7;
      } catch (Exception var10) {
         BufferedImage var3 = new BufferedImage(var1, var1, 1);
         Graphics2D var4 = var3.createGraphics();
         var4.setColor(Color.WHITE);
         var4.fillRect(0, 0, var1, var1);
         var4.dispose();
         return var3;
      }
   }
}
