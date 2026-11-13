package dev.luminous.api.live2d;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import dev.luminous.api.utils.win.CpuFeatures;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

interface CubismCoreLibrary extends Library {
   int csmGetVersion();

   int csmGetLatestMocVersion();

   int csmGetMocVersion(Pointer var1, int var2);

   int csmHasMocConsistency(Pointer var1, int var2);

   Pointer csmReviveMocInPlace(Pointer var1, int var2);

   int csmGetSizeofModel(Pointer var1);

   Pointer csmInitializeModelInPlace(Pointer var1, Pointer var2, int var3);

   void csmUpdateModel(Pointer var1);

   void csmReadCanvasInfo(Pointer var1, float[] var2, float[] var3, float[] var4);

   int csmGetParameterCount(Pointer var1);

   Pointer csmGetParameterIds(Pointer var1);

   Pointer csmGetParameterValues(Pointer var1);

   Pointer csmGetParameterMinimumValues(Pointer var1);

   Pointer csmGetParameterMaximumValues(Pointer var1);

   Pointer csmGetParameterDefaultValues(Pointer var1);

   int csmGetPartCount(Pointer var1);

   Pointer csmGetPartIds(Pointer var1);

   Pointer csmGetPartOpacities(Pointer var1);

   int csmGetDrawableCount(Pointer var1);

   Pointer csmGetDrawableIds(Pointer var1);

   Pointer csmGetDrawableConstantFlags(Pointer var1);

   Pointer csmGetDrawableDynamicFlags(Pointer var1);

   Pointer csmGetDrawableTextureIndices(Pointer var1);

   Pointer csmGetDrawableDrawOrders(Pointer var1);

   Pointer csmGetRenderOrders(Pointer var1);

   Pointer csmGetDrawableOpacities(Pointer var1);

   Pointer csmGetDrawableMaskCounts(Pointer var1);

   Pointer csmGetDrawableMasks(Pointer var1);

   Pointer csmGetDrawableVertexCounts(Pointer var1);

   Pointer csmGetDrawableVertexPositions(Pointer var1);

   Pointer csmGetDrawableVertexUvs(Pointer var1);

   Pointer csmGetDrawableIndexCounts(Pointer var1);

   Pointer csmGetDrawableIndices(Pointer var1);

   Pointer csmGetDrawableParentPartIndices(Pointer var1);

   static CubismCoreLibrary load(Path var0) throws IOException {
      CubismCoreLibrary var1 = CubismCoreLibrary$Holder.core;
      if (var1 != null) {
         return var1;
      } else if (!CpuFeatures.isAvx2Supported()) {
         throw new UnsatisfiedLinkError("Live2DCubismCore.dll requires AVX2, which this CPU does not support");
      } else {
         synchronized (CubismCoreLibrary$Holder.class) {
            if (CubismCoreLibrary$Holder.core == null) {
               CubismCoreLibrary$Holder.core = loadFrom(extractBundledDll(var0));
            }

            return CubismCoreLibrary$Holder.core;
         }
      }
   }

   static CubismCoreLibrary loadFrom(Path var0) {
      if (var0 != null && Files.isRegularFile(var0)) {
         try {
            return (CubismCoreLibrary)Native.load(var0.toAbsolutePath().toString(), CubismCoreLibrary.class);
         } catch (UnsatisfiedLinkError var2) {
         }
      }

      return loadFromSearchPath();
   }

   static CubismCoreLibrary loadFromSearchPath() {
      UnsatisfiedLinkError var0 = null;

      for (Path var2 : searchDirs()) {
         if (Files.isDirectory(var2)) {
            NativeLibrary.addSearchPath("Live2DCubismCore", var2.toAbsolutePath().toString());
            NativeLibrary.addSearchPath("Live2DCubismCore_MD", var2.toAbsolutePath().toString());
         }
      }

      for (String var6 : List.of("Live2DCubismCore", "Live2DCubismCore_MD")) {
         try {
            return (CubismCoreLibrary)Native.load(var6, CubismCoreLibrary.class);
         } catch (UnsatisfiedLinkError var4) {
            var0 = var4;
         }
      }

      throw var0 == null ? new UnsatisfiedLinkError("Live2DCubismCore.dll not found") : var0;
   }

   static Path extractBundledDll(Path var0) throws IOException {
      String var1 = "/assets/forever/live2d/native/windows/x86_64/Live2DCubismCore.dll";

      Path var2;
      try (InputStream var3 = CubismCoreLibrary.class.getResourceAsStream(var1)) {
         if (var3 == null) {
            throw new IOException("Bundled Live2DCubismCore.dll not found: " + var1);
         }

         byte[] var4 = var3.readAllBytes();
         Path var5 = var0.resolve("forever-live2d");
         Files.createDirectories(var5);
         Path var6 = var5.resolve("Live2DCubismCore-" + sha256Prefix(var4) + ".dll");
         if (!sameContent(var6, var4)) {
            Files.write(var6, var4);
         }

         var2 = var6;
      }

      return var2;
   }

   static boolean sameContent(Path var0, byte[] var1) throws IOException {
      if (Files.isRegularFile(var0) && Files.size(var0) == var1.length) {
         byte[] var2 = Files.readAllBytes(var0);
         if (var2.length != var1.length) {
            return false;
         } else {
            for (int var3 = 0; var3 < var1.length; var3++) {
               if (var2[var3] != var1[var3]) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static String sha256Prefix(byte[] var0) {
      try {
         byte[] var1 = MessageDigest.getInstance("SHA-256").digest(var0);
         StringBuilder var2 = new StringBuilder(16);

         for (int var3 = 0; var3 < 8; var3++) {
            var2.append(String.format("%02x", var1[var3]));
         }

         return var2.toString();
      } catch (NoSuchAlgorithmException var4) {
         throw new IllegalStateException("SHA-256 unavailable", var4);
      }
   }

   static List<Path> searchDirs() {
      Path var0 = Path.of("").toAbsolutePath();
      ArrayList var1 = new ArrayList();
      var1.add(var0.resolve("run").resolve("live2d"));
      var1.add(var0.resolve("live2d"));
      var1.add(var0.resolve("lib"));
      var1.add(var0);
      return var1;
   }
}
