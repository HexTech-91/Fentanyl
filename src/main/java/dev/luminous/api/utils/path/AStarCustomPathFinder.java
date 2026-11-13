package dev.luminous.api.utils.path;

import dev.luminous.api.utils.Wrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.util.math.BlockPos;

public class AStarCustomPathFinder implements Wrapper {
   private static final Vec3[] DIRECTIONS = new Vec3[]{
      new Vec3(1.0, 0.0, 0.0),
      new Vec3(-1.0, 0.0, 0.0),
      new Vec3(0.0, 0.0, 1.0),
      new Vec3(0.0, 0.0, -1.0),
      new Vec3(1.0, 0.0, 1.0),
      new Vec3(1.0, 0.0, -1.0),
      new Vec3(-1.0, 0.0, 1.0),
      new Vec3(-1.0, 0.0, -1.0),
      new Vec3(0.0, 1.0, 0.0),
      new Vec3(0.0, -1.0, 0.0)
   };
   private final Vec3 startVec3;
   private final Vec3 endVec3;
   private ArrayList<Vec3> path = new ArrayList<>();

   public AStarCustomPathFinder(Vec3 startVec3, Vec3 endVec3) {
      this.startVec3 = startVec3.addVector(0.0, 0.0, 0.0).floor();
      this.endVec3 = endVec3.addVector(0.0, 0.0, 0.0).floor();
   }

   public static boolean checkPositionValidity(Vec3 loc, boolean checkGround) {
      return checkPositionValidity((int)loc.x(), (int)loc.y(), (int)loc.z(), checkGround);
   }

   public static boolean checkPositionValidity(int x, int y, int z, boolean checkGround) {
      BlockPos feet = new BlockPos(x, y, z);
      BlockPos head = new BlockPos(x, y + 1, z);
      BlockPos ground = new BlockPos(x, y - 1, z);
      return !isBlockSolid(feet) && !isBlockSolid(head) && (isBlockSolid(ground) || !checkGround) && isSafeToWalkOn(ground);
   }

   private static boolean isBlockSolid(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      Block block = state.getBlock();
      return state.isSolidBlock(mc.world, pos)
         ? true
         : block instanceof CactusBlock
            || block instanceof PistonBlock
            || block instanceof PistonExtensionBlock
            || block instanceof PistonHeadBlock
            || block instanceof SlabBlock
            || block instanceof StairsBlock
            || block instanceof TrapdoorBlock;
   }

   private static boolean isSafeToWalkOn(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return true;
   }

   public ArrayList<Vec3> getPath() {
      return this.path;
   }

   public void compute() {
      this.compute(10000);
   }

   public void compute(int maxIterations) {
      this.path.clear();
      PriorityQueue<AStarCustomPathFinder.Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
      Map<Vec3, AStarCustomPathFinder.Node> allNodes = new HashMap<>();
      AStarCustomPathFinder.Node startNode = new AStarCustomPathFinder.Node(this.startVec3, 0.0, this.heuristic(this.startVec3, this.endVec3), null);
      openSet.add(startNode);
      allNodes.put(this.startVec3, startNode);
      int iter = 0;

      while (!openSet.isEmpty() && iter < maxIterations) {
         AStarCustomPathFinder.Node current = openSet.poll();
         iter++;
         if (current.pos.squareDistanceTo(this.endVec3) <= 4.0) {
            this.path = this.reconstructPath(current);
            return;
         }

         for (Vec3 dir : DIRECTIONS) {
            Vec3 neighborPos = current.pos.add(dir).floor();
            if (checkPositionValidity(neighborPos, false)) {
               double moveCost = 1.0;
               if (dir.y() != 0.0) {
                  moveCost = 1.5;
               } else if (Math.abs(dir.x()) + Math.abs(dir.z()) == 2.0) {
                  moveCost = 1.4;
               }

               double tentativeG = current.g + moveCost;
               AStarCustomPathFinder.Node existing = allNodes.get(neighborPos);
               if (existing == null || tentativeG < existing.g) {
                  double h = this.heuristic(neighborPos, this.endVec3);
                  AStarCustomPathFinder.Node neighborNode = new AStarCustomPathFinder.Node(neighborPos, tentativeG, h, current);
                  if (existing != null) {
                     openSet.remove(existing);
                     allNodes.remove(neighborPos);
                  }

                  openSet.add(neighborNode);
                  allNodes.put(neighborPos, neighborNode);
               }
            }
         }
      }
   }

   private double heuristic(Vec3 a, Vec3 b) {
      double dx = a.x() - b.x();
      double dy = a.y() - b.y();
      double dz = a.z() - b.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private ArrayList<Vec3> reconstructPath(AStarCustomPathFinder.Node end) {
      ArrayList<Vec3> path = new ArrayList<>();

      for (AStarCustomPathFinder.Node cur = end; cur != null; cur = cur.parent) {
         path.add(cur.pos);
      }

      Collections.reverse(path);
      return path;
   }

   private static class Node {
      final Vec3 pos;
      final double g;
      final double f;
      final AStarCustomPathFinder.Node parent;

      Node(Vec3 pos, double g, double h, AStarCustomPathFinder.Node parent) {
         this.pos = pos;
         this.g = g;
         this.f = g + h;
         this.parent = parent;
      }
   }
}
