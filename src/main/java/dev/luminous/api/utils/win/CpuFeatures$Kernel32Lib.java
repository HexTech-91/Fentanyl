package dev.luminous.api.utils.win;

import com.sun.jna.Library;
import com.sun.jna.Native;

interface CpuFeatures$Kernel32Lib extends Library {
   CpuFeatures$Kernel32Lib INSTANCE = (CpuFeatures$Kernel32Lib)Native.load("kernel32", CpuFeatures$Kernel32Lib.class);

   boolean IsProcessorFeaturePresent(int var1);
}
