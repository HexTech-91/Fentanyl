package dev.luminous.api.utils.win;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

public class CpuFeatures {
   private static final int PF_AVX2_INSTRUCTIONS_AVAILABLE = 40;
   private static volatile Boolean avx2Supported;

   public static boolean isAvx2Supported() {
      Boolean var0 = avx2Supported;
      if (var0 != null) {
         return var0;
      } else {
         synchronized (CpuFeatures.class) {
            if (avx2Supported == null) {
               avx2Supported = detectAvx2();
            }

            return avx2Supported;
         }
      }
   }

   private static boolean detectAvx2() {
      String var0 = System.getProperty("os.name", "").toLowerCase();
      if (!var0.contains("win")) {
         return true;
      } else {
         Boolean var1 = detectViaHotspotUseAvx();
         if (var1 != null) {
            return var1;
         } else {
            try {
               return CpuFeatures$Kernel32Lib.INSTANCE.IsProcessorFeaturePresent(40);
            } catch (Throwable var3) {
               return true;
            }
         }
      }
   }

   private static Boolean detectViaHotspotUseAvx() {
      try {
         HotSpotDiagnosticMXBean var0 = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
         if (var0 == null) {
            return null;
         } else {
            String var1 = var0.getVMOption("UseAVX").getValue();
            return Integer.parseInt(var1.trim()) >= 2;
         }
      } catch (Throwable var2) {
         return null;
      }
   }

   private CpuFeatures() {
   }
}
