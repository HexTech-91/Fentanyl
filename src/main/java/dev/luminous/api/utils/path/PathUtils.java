package dev.luminous.api.utils.path;

import dev.luminous.api.utils.Wrapper;
import java.util.ArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class PathUtils implements Wrapper {
   public static ArrayList<Vec3> computePath(Vec3d target) {
      return computePath(new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()), new Vec3(target.x, target.y, target.z));
   }

   public static ArrayList<Vec3> computePath(LivingEntity from, LivingEntity to) {
      return computePath(new Vec3(from.getX(), from.getY(), from.getZ()), new Vec3(to.getX(), to.getY(), to.getZ()));
   }

   public static ArrayList<Vec3> computePath(Vec3 topFrom, Vec3 to) {
      AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
      pathfinder.compute();
      ArrayList<Vec3> rawPath = pathfinder.getPath();
      ArrayList<Vec3> centered = new ArrayList<>();

      for (Vec3 p : rawPath) {
         centered.add(p.addVector(0.5, 0.0, 0.5));
      }

      return centered;
   }
}
