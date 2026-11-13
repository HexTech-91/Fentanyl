package dev.luminous.api.utils.rotation;

import dev.luminous.core.impl.RotationManager;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
   public static Rotation calculate(Vec3d to) {
      float[] var1 = RotationManager.getRotation(to);
      return new Rotation(var1[0], var1[1]);
   }
}
