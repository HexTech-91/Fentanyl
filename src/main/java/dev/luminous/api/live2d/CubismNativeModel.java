package dev.luminous.api.live2d;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class CubismNativeModel implements Closeable {
   private static final int DYNAMIC_FLAG_IS_VISIBLE = 1;
   private static final int CONSTANT_FLAG_BLEND_ADDITIVE = 1;
   private static final int CONSTANT_FLAG_BLEND_MULTIPLICATIVE = 2;
   private static final int MOC_ALIGNMENT = 64;
   private static final int MODEL_ALIGNMENT = 16;
   private final CubismCoreLibrary core;
   private final Memory mocMemory;
   private final Memory modelMemory;
   private final Pointer model;
   private final Map parameterIndices = new HashMap();
   private final Map<Integer, String> partIds = new HashMap<>();
   private final int drawableCount;
   private final int parameterCount;
   private final float canvasWidth;
   private final float canvasHeight;
   private final float originX;
   private final float originY;
   private final float pixelsPerUnit;
   private float boundsMinX;
   private float boundsMinY;
   private float boundsMaxX;
   private float boundsMaxY;
   private byte[] constantFlags;
   private String[] drawableIdCache;
   private boolean frameCacheReady;
   private Pointer dynFlagsPtr;
   private Pointer renderOrdersPtr;
   private Pointer drawOrdersPtr;
   private Pointer texIdxPtr;
   private Pointer opacityPtr;
   private Pointer maskCountPtr;
   private Pointer vtxCountPtr;
   private Pointer idxCountPtr;
   private Pointer vtxPtrArr;
   private Pointer uvPtrArr;
   private Pointer idxPtrArr;

   public static CubismNativeModel load(MinecraftClient var0, Identifier var1, Path var2) throws IOException {
      CubismNativeModel var3;
      try (InputStream var4 = var0.getResourceManager().getResourceOrThrow(var1).getInputStream()) {
         CubismCoreLibrary var5 = CubismCoreLibrary.load(var2);
         CubismNativeModel var6 = new CubismNativeModel(var4.readAllBytes(), var5);
         var3 = var6;
      }

      return var3;
   }

   private CubismNativeModel(byte[] var1, CubismCoreLibrary var2) {
      this.core = var2;
      this.mocMemory = new Memory(var1.length + 64);
      Pointer var3 = alignedPointer(this.mocMemory, 64);
      var3.write(0L, var1, 0, var1.length);
      int var4 = this.core.csmHasMocConsistency(var3, var1.length);
      if (var4 == 0) {
         throw new IllegalStateException("Invalid Cubism moc3 data");
      } else {
         Pointer var5 = this.core.csmReviveMocInPlace(var3, var1.length);
         if (var5 != null && Pointer.nativeValue(var5) != 0L) {
            int var6 = this.core.csmGetSizeofModel(var5);
            this.modelMemory = new Memory(var6 + 16);
            Pointer var7 = alignedPointer(this.modelMemory, 16);
            this.model = this.core.csmInitializeModelInPlace(var5, var7, var6);
            if (this.model != null && Pointer.nativeValue(this.model) != 0L) {
               this.parameterCount = this.core.csmGetParameterCount(this.model);
               this.drawableCount = this.core.csmGetDrawableCount(this.model);
               this.cacheParameterIndices();
               this.cachePartIds();
               this.cacheConstantFlags();
               this.cacheDrawableIds();
               float[] var8 = new float[2];
               float[] var9 = new float[2];
               float[] var10 = new float[1];
               this.core.csmReadCanvasInfo(this.model, var8, var9, var10);
               this.canvasWidth = var8[0] / Math.max(1.0F, var10[0]);
               this.canvasHeight = var8[1] / Math.max(1.0F, var10[0]);
               this.originX = var9[0] / Math.max(1.0F, var10[0]);
               this.originY = var9[1] / Math.max(1.0F, var10[0]);
               this.pixelsPerUnit = var10[0];
               this.update();
               this.refreshBounds();
            } else {
               throw new IllegalStateException("Cubism Core failed to initialize model");
            }
         } else {
            throw new IllegalStateException("Cubism Core failed to revive moc");
         }
      }
   }

   private static Pointer alignedPointer(Pointer var0, int var1) {
      long var2 = Pointer.nativeValue(var0);
      long var4 = var2 + var1 - 1L & -var1;
      return var0.share(var4 - var2);
   }

   public void update() {
      this.core.csmUpdateModel(this.model);
   }

   public float getPartOpacity(String var1) {
      Pointer var2 = this.core.csmGetPartIds(this.model);
      int var3 = this.core.csmGetPartCount(this.model);
      Pointer var4 = this.core.csmGetPartOpacities(this.model);
      if (var2 == null || var4 == null) {
         return 1.0F;
      } else {
         for (int var5 = 0; var5 < var3; var5++) {
            Pointer var6 = var2.getPointer((long)var5 * Native.POINTER_SIZE);
            if (var6 != null && var1.equals(this.readCString(var6))) {
               return var4.getFloat(var5 * 4L);
            }
         }

         return 1.0F;
      }
   }

   public void setPartOpacity(String var1, float var2) {
      Pointer var3 = this.core.csmGetPartIds(this.model);
      int var4 = this.core.csmGetPartCount(this.model);
      Pointer var5 = this.core.csmGetPartOpacities(this.model);
      if (var3 != null && var5 != null) {
         float var6 = Math.max(0.0F, Math.min(1.0F, var2));

         for (int var7 = 0; var7 < var4; var7++) {
            Pointer var8 = var3.getPointer((long)var7 * Native.POINTER_SIZE);
            if (var8 != null && var1.equals(this.readCString(var8))) {
               var5.setFloat(var7 * 4L, var6);
               return;
            }
         }
      }
   }

   public void setParameter(String var1, float var2) {
      Integer var3 = (Integer)this.parameterIndices.get(var1);
      if (var3 != null) {
         this.setParameter(var3, var2);
      }
   }

   public float getParameterValue(String var1) {
      Integer var2 = (Integer)this.parameterIndices.get(var1);
      return var2 == null ? Float.NaN : this.core.csmGetParameterValues(this.model).getFloat(var2 * 4L);
   }

   public void setParameter(int var1, float var2) {
      if (var1 >= 0 && var1 < this.parameterCount) {
         float var3 = this.core.csmGetParameterMinimumValues(this.model).getFloat(var1 * 4L);
         float var4 = this.core.csmGetParameterMaximumValues(this.model).getFloat(var1 * 4L);
         float var5 = Math.max(var3, Math.min(var4, var2));
         this.core.csmGetParameterValues(this.model).setFloat(var1 * 4L, var5);
      }
   }

   public int getDrawableCount() {
      return this.drawableCount;
   }

   public float getCanvasWidth() {
      return this.canvasWidth;
   }

   public float getCanvasHeight() {
      return this.canvasHeight;
   }

   public float getOriginX() {
      return this.originX;
   }

   public float getOriginY() {
      return this.originY;
   }

   public float getPixelsPerUnit() {
      return this.pixelsPerUnit;
   }

   public float getBoundsMinX() {
      return this.boundsMinX;
   }

   public float getBoundsMinY() {
      return this.boundsMinY;
   }

   public float getBoundsMaxX() {
      return this.boundsMaxX;
   }

   public float getBoundsMaxY() {
      return this.boundsMaxY;
   }

   public float getBoundsWidth() {
      return Math.max(0.001F, this.boundsMaxX - this.boundsMinX);
   }

   public float getBoundsHeight() {
      return Math.max(0.001F, this.boundsMaxY - this.boundsMinY);
   }

   public int getDrawableRenderOrder(int var1) {
      Pointer var2 = this.renderOrdersPtr;
      if (var2 == null) {
         var2 = this.core.csmGetRenderOrders(this.model);
      }

      return var2.getInt(var1 * 4L);
   }

   public int getDrawableRenderOrderIndex(int var1) {
      return this.getDrawableRenderOrder(var1);
   }

   public int getDrawableDrawOrder(int var1) {
      Pointer var2 = this.drawOrdersPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableDrawOrders(this.model);
      }

      return var2 == null ? var1 : var2.getInt(var1 * 4L);
   }

   public String getDrawableId(int var1) {
      String[] var2 = this.drawableIdCache;
      if (var2 != null) {
         return var2[var1];
      } else {
         Pointer var3 = this.core.csmGetDrawableIds(this.model);
         Pointer var4 = var3.getPointer((long)var1 * Native.POINTER_SIZE);
         return var4 == null ? "" : this.readCString(var4);
      }
   }

   public int getDrawableParentPartIndex(int var1) {
      return this.core.csmGetDrawableParentPartIndices(this.model).getInt(var1 * 4L);
   }

   public String getDrawableParentPartId(int var1) {
      return this.partIds.getOrDefault(this.getDrawableParentPartIndex(var1), "");
   }

   public boolean isDrawableVisible(int var1) {
      Pointer var2 = this.dynFlagsPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableDynamicFlags(this.model);
      }

      byte var3 = var2.getByte(var1);
      return (var3 & 1) != 0;
   }

   public boolean isDrawableAdditive(int var1) {
      return (this.constantFlags[var1] & 1) != 0;
   }

   public boolean isDrawableMultiplicative(int var1) {
      return (this.constantFlags[var1] & 2) != 0;
   }

   public int getDrawableTextureIndex(int var1) {
      Pointer var2 = this.texIdxPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableTextureIndices(this.model);
      }

      return var2.getInt(var1 * 4L);
   }

   public float getDrawableOpacity(int var1) {
      Pointer var2 = this.opacityPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableOpacities(this.model);
      }

      return var2.getFloat(var1 * 4L);
   }

   public int getDrawableVertexCount(int var1) {
      Pointer var2 = this.vtxCountPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableVertexCounts(this.model);
      }

      return var2.getInt(var1 * 4L);
   }

   public int getDrawableIndexCount(int var1) {
      Pointer var2 = this.idxCountPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableIndexCounts(this.model);
      }

      return var2.getInt(var1 * 4L);
   }

   public int getDrawableMaskCount(int var1) {
      Pointer var2 = this.maskCountPtr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableMaskCounts(this.model);
      }

      return var2.getInt(var1 * 4L);
   }

   public int[] getDrawableMasks(int var1) {
      Pointer var2 = this.core.csmGetDrawableMasks(this.model);
      int var3 = this.getDrawableMaskCount(var1);
      if (var3 <= 0 || var2 == null) {
         return new int[0];
      }

      Pointer var4 = var2.getPointer((long)var1 * Native.POINTER_SIZE);
      return var4 == null ? new int[0] : var4.getIntArray(0L, var3);
   }

   public float[] getDrawableVertexPositions(int var1) {
      Pointer var2 = this.vtxPtrArr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableVertexPositions(this.model);
      }

      int var3 = this.getDrawableVertexCount(var1) * 2;
      Pointer var4 = var2.getPointer((long)var1 * Native.POINTER_SIZE);
      return var4 == null ? new float[0] : var4.getFloatArray(0L, var3);
   }

   public float[] getDrawableVertexUvs(int var1) {
      Pointer var2 = this.uvPtrArr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableVertexUvs(this.model);
      }

      int var3 = this.getDrawableVertexCount(var1) * 2;
      Pointer var4 = var2.getPointer((long)var1 * Native.POINTER_SIZE);
      return var4 == null ? new float[0] : var4.getFloatArray(0L, var3);
   }

   public short[] getDrawableIndices(int var1) {
      Pointer var2 = this.idxPtrArr;
      if (var2 == null) {
         var2 = this.core.csmGetDrawableIndices(this.model);
      }

      int var3 = this.getDrawableIndexCount(var1);
      Pointer var4 = var2.getPointer((long)var1 * Native.POINTER_SIZE);
      return var4 == null ? new short[0] : var4.getShortArray(0L, var3);
   }

   public void beginFrameCache() {
      this.dynFlagsPtr = this.core.csmGetDrawableDynamicFlags(this.model);
      this.renderOrdersPtr = this.core.csmGetRenderOrders(this.model);
      this.drawOrdersPtr = this.core.csmGetDrawableDrawOrders(this.model);
      this.texIdxPtr = this.core.csmGetDrawableTextureIndices(this.model);
      this.opacityPtr = this.core.csmGetDrawableOpacities(this.model);
      this.maskCountPtr = this.core.csmGetDrawableMaskCounts(this.model);
      this.vtxCountPtr = this.core.csmGetDrawableVertexCounts(this.model);
      this.idxCountPtr = this.core.csmGetDrawableIndexCounts(this.model);
      this.vtxPtrArr = this.core.csmGetDrawableVertexPositions(this.model);
      this.uvPtrArr = this.core.csmGetDrawableVertexUvs(this.model);
      this.idxPtrArr = this.core.csmGetDrawableIndices(this.model);
      this.frameCacheReady = true;
   }

   private void cacheConstantFlags() {
      Pointer var1 = this.core.csmGetDrawableConstantFlags(this.model);
      this.constantFlags = var1 == null ? new byte[this.drawableCount] : var1.getByteArray(0L, this.drawableCount);
   }

   private void cacheDrawableIds() {
      this.drawableIdCache = new String[this.drawableCount];
      Pointer var1 = this.core.csmGetDrawableIds(this.model);

      for (int var2 = 0; var2 < this.drawableCount; var2++) {
         Pointer var3 = var1.getPointer((long)var2 * Native.POINTER_SIZE);
         this.drawableIdCache[var2] = var3 == null ? "" : this.readCString(var3);
      }
   }

   private void cacheParameterIndices() {
      Pointer var1 = this.core.csmGetParameterIds(this.model);

      for (int var2 = 0; var2 < this.parameterCount; var2++) {
         Pointer var3 = var1.getPointer((long)var2 * Native.POINTER_SIZE);
         String var4;
         if (var3 != null && !(var4 = this.readCString(var3)).isEmpty()) {
            this.parameterIndices.put(var4, var2);
         }
      }
   }

   private void cachePartIds() {
      int var1 = this.core.csmGetPartCount(this.model);
      Pointer var2 = this.core.csmGetPartIds(this.model);

      for (int var3 = 0; var3 < var1; var3++) {
         Pointer var4 = var2.getPointer((long)var3 * Native.POINTER_SIZE);
         if (var4 != null) {
            this.partIds.put(var3, this.readCString(var4));
         }
      }
   }

   private void refreshBounds() {
      float var1 = Float.POSITIVE_INFINITY;
      float var2 = Float.POSITIVE_INFINITY;
      float var3 = Float.NEGATIVE_INFINITY;
      float var4 = Float.NEGATIVE_INFINITY;

      for (int var5 = 0; var5 < this.drawableCount; var5++) {
         if (this.isDrawableVisible(var5)) {
            float[] var6 = this.getDrawableVertexPositions(var5);

            for (byte var7 = 0; var7 + 1 < var6.length; var7 = (byte)(var7 + 2)) {
               var1 = Math.min(var1, var6[var7]);
               var3 = Math.max(var3, var6[var7]);
               var2 = Math.min(var2, var6[var7 + 1]);
               var4 = Math.max(var4, var6[var7 + 1]);
            }
         }
      }

      if (var1 == Float.POSITIVE_INFINITY) {
         var1 = this.originX - this.canvasWidth * 0.5F;
         var3 = this.originX + this.canvasWidth * 0.5F;
         var2 = this.originY - this.canvasHeight * 0.5F;
         var4 = this.originY + this.canvasHeight * 0.5F;
      }

      this.boundsMinX = var1;
      this.boundsMinY = var2;
      this.boundsMaxX = var3;
      this.boundsMaxY = var4;
   }

   private String readCString(Pointer var1) {
      byte[] var2 = var1.getByteArray(0L, 128);
      int var3 = 0;

      while (var3 < var2.length && var2[var3] != 0) {
         var3++;
      }

      return new String(Arrays.copyOf(var2, var3), StandardCharsets.UTF_8);
   }

   @Override
   public void close() {
      this.parameterIndices.clear();
      this.partIds.clear();
      this.modelMemory.close();
      this.mocMemory.close();
   }
}
