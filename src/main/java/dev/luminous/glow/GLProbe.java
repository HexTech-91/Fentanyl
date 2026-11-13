package dev.luminous.glow;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.IntBuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.MemoryStack;

public final class GLProbe {
   private static long lastProbeLog;
   private static boolean dbgCtlTried;
   private static int probeTick;
   private static final int[] EMPTY4 = new int[4];
   private static long lastMemCheck;
   private static long lastMemLog;
   private static long lastSoftGc;
   private static long lastGcAttempt;
   private static long lastCommitted;
   private static long lastGcLog;
   public static volatile boolean frameGlowTargets = false;

   private GLProbe() {
   }

   private static void silenceApiErrorCallback() {
      if (!dbgCtlTried) {
         try {
            KHRDebug.glDebugMessageCallback(null, 0L);
         } catch (Throwable var5) {
         }

         try {
            int[] var0 = new int[]{37190, 37191, 37192, 33387};

            for (int var4 : var0) {
               GL43.glDebugMessageControl(4358, 4358, var4, (int[])null, false);
            }

            dbgCtlTried = true;
            System.out.println("[GLProbe] GL debug callback removed (null + per-severity disable)");
         } catch (Throwable var6) {
         }
      }
   }

   public static void drainGlErrors(String var0) {
      try {
         int var1 = GL11.glGetError();
         if (var1 == 0) {
            return;
         }

         long var2 = System.currentTimeMillis();
         if (var2 - lastProbeLog < 15000L) {
            while (GL11.glGetError() != 0) {
            }

            return;
         }

         lastProbeLog = var2;
         StringBuilder var4 = new StringBuilder("[GLProbe] ").append(var0).append(" err=0x").append(Integer.toHexString(var1));

         while ((var1 = GL11.glGetError()) != 0) {
            var4.append(",0x").append(Integer.toHexString(var1));
         }

         System.out.println(var4);
      } catch (Throwable var5) {
      }
   }

   public static void resyncGlState() {
      resyncGlState("generic");
   }

   public static void resyncGlState(String var0) {
      try {
         memoryGuardTick();
         silenceApiErrorCallback();
         MemoryStack var1 = MemoryStack.stackPush();

         int var2;
         int var3;
         int var4;
         int var5;
         int var6;
         int[] var7;
         try {
            var2 = GL11.glGetInteger(3089);
            var3 = GL11.glGetInteger(34016);
            var4 = GL11.glGetInteger(3314);
            var5 = GL11.glGetInteger(3317);
            var7 = read4(var1, 3107);
            var6 = GL11.glGetInteger(3168);
         } catch (Throwable var24) {
            if (var1 != null) {
               try {
                  var1.close();
               } catch (Throwable var22) {
                  var24.addSuppressed(var22);
               }
            }

            throw var24;
         }

         if (var1 != null) {
            var1.close();
         }

         int var8 = 0;
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = 0;
         int var13 = 0;
         int var14 = 0;
         int[] var15 = EMPTY4;
         boolean var16 = var4 != 0 || var5 != 4 || var2 != 0 || var7[0] == 0 || var7[1] == 0 || var7[2] == 0 || var7[3] == 0 || var6 != 0 || var3 != 34016;
         boolean var17 = ++probeTick >= 600;
         if (var17) {
            probeTick = 0;
         }

         if (!var16 && !var17) {
            return;
         }

         MemoryStack var18 = MemoryStack.stackPush();

         try {
            var8 = GL11.glGetInteger(36006);
            var9 = GL11.glGetInteger(3042);
            var10 = GL11.glGetInteger(34229);
            var15 = read4(var18, 2978);
            var11 = GL11.glGetInteger(2929);
            var12 = GL11.glGetInteger(2884);
            var13 = GL11.glGetInteger(2930);
            var14 = GL11.glGetInteger(2932);
         } catch (Throwable var23) {
            if (var18 != null) {
               try {
                  var18.close();
               } catch (Throwable var21) {
                  var23.addSuppressed(var21);
               }
            }

            throw var23;
         }

         if (var18 != null) {
            var18.close();
         }

         if (var16 && System.currentTimeMillis() - lastProbeLog >= 15000L) {
            lastProbeLog = System.currentTimeMillis();
            System.out
               .println(
                  "[GLProbe] "
                     + var0
                     + " STALE fbo="
                     + var8
                     + " cmask="
                     + var7[0]
                     + ","
                     + var7[1]
                     + ","
                     + var7[2]
                     + ","
                     + var7[3]
                     + " scissor="
                     + (var2 != 0)
                     + " vp="
                     + var15[0]
                     + ","
                     + var15[1]
                     + ","
                     + var15[2]
                     + ","
                     + var15[3]
                     + " row="
                     + var4
                     + " align="
                     + var5
                     + " blend="
                     + var9
                     + " depth="
                     + var11
                     + " cull="
                     + var12
                     + " dmask="
                     + var13
                     + " dfunc=0x"
                     + Integer.toHexString(var14)
                     + " stencil="
                     + var6
                     + " act=0x"
                     + Integer.toHexString(var3)
                     + " vao="
                     + var10
               );
         }

         GlStateManager._glUseProgram(0);
         RenderSystem.disableBlend();
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(0, 1, 0, 1);
         RenderSystem.defaultBlendFunc();

         for (int var19 = 3; var19 >= 0; var19--) {
            GL13.glActiveTexture(33984 + var19);
            GL11.glBindTexture(3553, 0);
         }

         for (int var34 = 3; var34 >= 0; var34--) {
            RenderSystem.activeTexture(33984 + var34);
            GlStateManager._bindTexture(0);
         }

         GlStateManager._pixelStore(3314, 1);
         GlStateManager._pixelStore(3314, 0);
         GlStateManager._pixelStore(3317, 1);
         GlStateManager._pixelStore(3317, 4);
         RenderSystem.disableDepthTest();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         RenderSystem.enableCull();
         GL11.glDisable(3089);
         GlStateManager._disableScissorTest();
         GL11.glDisable(3168);
         GL11.glDepthFunc(515);
         GL11.glDisable(2848);
         GL30.glBindFramebuffer(36160, 0);
         GlStateManager._glBindFramebuffer(36160, 0);
         MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
      } catch (Throwable var25) {
      }
   }

   private static int[] read4(MemoryStack var0, int var1) {
      IntBuffer var2 = var0.mallocInt(4);
      GL11.glGetIntegerv(var1, var2);
      return new int[]{var2.get(0), var2.get(1), var2.get(2), var2.get(3)};
   }

   private static void memoryGuardTick() {
      long var0 = System.currentTimeMillis();
      if (var0 - lastMemCheck >= 5000L) {
         lastMemCheck = var0;

         long var2;
         try {
            OperatingSystemMXBean var4 = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
            var2 = var4.getCommittedVirtualMemorySize();
         } catch (Throwable var12) {
            return;
         }

         if (var2 > 0L) {
            long var13 = var2 >> 30;
            if (var13 >= 26L && var0 - lastGcLog >= 300000L) {
               lastGcLog = var0;
               System.out
                  .println(
                     "[MemoryGuard] CRITICAL committed="
                        + (var2 >> 20)
                        + "MB - native memory leak, please close and restart the client NOW (page thrash imminent) "
                        + memoryDetail(var2)
                  );
            }

            if (var13 >= 4L && var13 < 16L && var0 - lastSoftGc >= 45000L) {
               lastSoftGc = var0;
               System.gc();
               long var6 = var2;

               try {
                  OperatingSystemMXBean var8 = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
                  var6 = var8.getCommittedVirtualMemorySize();
               } catch (Throwable var11) {
               }

               if (var0 - lastMemLog >= 30000L) {
                  lastMemLog = var0;
                  long var16 = var2 - var6;
                  System.out
                     .println(
                        "[MemoryGuard] committed="
                           + (var2 >> 20)
                           + "MB -> softgc -> "
                           + (var6 >> 20)
                           + "MB (reclaimed "
                           + (var16 >> 20)
                           + "MB) "
                           + memoryDetail(var6)
                           + " "
                           + (var16 > 268435456L ? "= GC-recoverable native (lower -Xmx to fix)" : "= native not freed by GC (Skia/JNA/driver)")
                     );
               }
            }

            if (var13 >= 16L) {
               if (var0 - lastGcAttempt >= 600000L) {
                  lastGcAttempt = var0;
                  System.gc();
                  long var14 = 0L;

                  try {
                     OperatingSystemMXBean var17 = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
                     var14 = var17.getCommittedVirtualMemorySize();
                  } catch (Throwable var10) {
                  }

                  long var18 = var2 - var14;
                  if (var0 - lastMemLog >= 30000L) {
                     lastMemLog = var0;
                     System.out
                        .println(
                           "[MemoryGuard] committed="
                              + (var2 >> 20)
                              + "MB -> GC -> "
                              + (var14 >> 20)
                              + "MB (reclaimed "
                              + (var18 >> 20)
                              + "MB) "
                              + memoryDetail(var14)
                              + " "
                              + (var18 > 536870912L ? "= GC-recoverable native (direct buffers)" : "= unrecoverable C-level leak (Skia/JNA/driver)")
                        );
                  }
               }
            } else {
               if (var13 >= 8L) {
                  long var15 = lastCommitted > 0L ? (var2 - lastCommitted) / 5L : 0L;
                  if (var0 - lastMemLog >= 900000L) {
                     lastMemLog = var0;
                     System.out.println("[MemoryGuard] committed=" + (var2 >> 20) + "MB rate=" + (var15 >> 10) + "KB/s (15min notice) " + memoryDetail(var2));
                  }
               }

               lastCommitted = var2;
            }
         }
      }
   }

   private static String memoryDetail(long var0) {
      try {
         MemoryMXBean var2 = ManagementFactory.getMemoryMXBean();
         MemoryUsage var3 = var2.getHeapMemoryUsage();
         long var4 = 0L;

         for (BufferPoolMXBean var7 : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if ("direct".equals(var7.getName())) {
               var4 = var7.getMemoryUsed();
            }
         }

         long var9 = var0 - var3.getCommitted() - var4;
         return "| heap="
            + (var3.getCommitted() >> 20)
            + "MB/used="
            + (var3.getUsed() >> 20)
            + "MB direct="
            + (var4 >> 20)
            + "MB native-other="
            + (var9 >> 20)
            + "MB";
      } catch (Throwable var8) {
         return "| detail-unavailable";
      }
   }
}
