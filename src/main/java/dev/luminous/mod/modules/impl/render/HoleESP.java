package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class HoleESP extends Module {
   public static HoleESP INSTANCE;
   public final SliderSetting startFade = this.add(new SliderSetting("StartFade", 5.0, 1.0, 20.0));
   public final SliderSetting getDistance;
   public final SliderSetting airHeight;
   public final BooleanSetting airYCheck;
   public final SliderSetting height;
   public final SliderSetting wallHeight;
   public final BooleanSetting sideCheck;
   private final ColorSetting airFill;
   private final ColorSetting airBox;
   private final ColorSetting airFade;
   private final ColorSetting normalFill;
   private final ColorSetting normalBox;
   private final ColorSetting normalFade;
   private final ColorSetting bedrockFill;
   private final ColorSetting bedrockBox;
   private final ColorSetting bedrockFade;
   private final ColorSetting wallFill;
   private final ColorSetting wallBox;
   private final ColorSetting wallFade;
   private final ColorSetting wallSideFill;
   private final ColorSetting wallSideBox;
   private final ColorSetting wallSideFade;
   private final SliderSetting updateDelay;
   private final List<BlockPos> tempNormalList;
   private final List<BlockPos> tempBedrockList;
   private final List<BlockPos> tempAirList;
   private final List<BlockPos> tempWallList;
   private final List<BlockPos> tempWallSideList;
   private final Timer timer;
   boolean drawing;
   private List<BlockPos> normalList;
   private List<BlockPos> bedrockList;
   private List<BlockPos> airList;
   private List<BlockPos> wallList;
   private List<BlockPos> wallSideList;

   public HoleESP() {
      super("HoleESP", Module.Category.Render);
      this.startFade.setChinese("\u5f00\u59cb\u6de1\u51fa");
      this.getDistance = this.add(new SliderSetting("GetDistance", 6.0, 1.0, 20.0));
      this.getDistance.setChinese("\u8ddd\u79bb");
      this.airHeight = this.add(new SliderSetting("AirHeight", 1.0, -3.0, 3.0, 0.01));
      this.airHeight.setChinese("\u7a7a\u6c14\u9ad8\u5ea6");
      this.airYCheck = this.add(new BooleanSetting("AirYCheck", true));
      this.airYCheck.setChinese("\u7a7a\u6c14Y\u68c0\u6d4b");
      this.height = this.add(new SliderSetting("Height", 1.0, -3.0, 3.0, 0.1));
      this.height.setChinese("\u9ad8\u5ea6");
      this.wallHeight = this.add(new SliderSetting("WallHeight", 3.0, -3.0, 3.0, 0.1));
      this.wallHeight.setChinese("\u5899\u58c1\u9ad8\u5ea6");
      this.sideCheck = this.add(new BooleanSetting("SideCheck", true));
      this.sideCheck.setChinese("\u4fa7\u9762\u68c0\u6d4b");
      this.airFill = this.add(new ColorSetting("AirFill", new Color(148, 0, 0, 100)).injectBoolean(true));
      this.airFill.setChinese("\u7a7a\u6c14\u586b\u5145");
      this.airBox = this.add(new ColorSetting("AirBox", new Color(148, 0, 0, 100)).injectBoolean(true));
      this.airBox.setChinese("\u7a7a\u6c14\u8fb9\u6846");
      this.airFade = this.add(new ColorSetting("AirFade", new Color(148, 0, 0, 0)).injectBoolean(true));
      this.airFade.setChinese("\u7a7a\u6c14\u6de1\u51fa");
      this.normalFill = this.add(new ColorSetting("NormalFill", new Color(255, 0, 0, 50)).injectBoolean(true));
      this.normalFill.setChinese("\u4e0d\u5b89\u5168\u586b\u5145");
      this.normalBox = this.add(new ColorSetting("NormalBox", new Color(255, 0, 0, 100)).injectBoolean(true));
      this.normalBox.setChinese("\u4e0d\u5b89\u5168\u8fb9\u6846");
      this.normalFade = this.add(new ColorSetting("NormalFade", new Color(255, 0, 0, 0)).injectBoolean(true));
      this.normalFade.setChinese("\u4e0d\u5b89\u5168\u6de1\u51fa");
      this.bedrockFill = this.add(new ColorSetting("BedrockFill", new Color(8, 255, 79, 50)).injectBoolean(true));
      this.bedrockFill.setChinese("\u5b89\u5168\u586b\u5145");
      this.bedrockBox = this.add(new ColorSetting("BedrockBox", new Color(8, 255, 79, 100)).injectBoolean(true));
      this.bedrockBox.setChinese("\u5b89\u5168\u8fb9\u6846");
      this.bedrockFade = this.add(new ColorSetting("BedrockFade", new Color(8, 255, 79, 100)).injectBoolean(true));
      this.bedrockFade.setChinese("\u5b89\u5168\u6de1\u51fa");
      this.wallFill = this.add(new ColorSetting("WallFill", new Color(0, 255, 255, 128)).injectBoolean(true));
      this.wallFill.setChinese("\u5899\u58c1\u586b\u5145");
      this.wallBox = this.add(new ColorSetting("WallBox", new Color(0, 225, 255, 255)).injectBoolean(true));
      this.wallBox.setChinese("\u5899\u58c1\u8fb9\u6846");
      this.wallFade = this.add(new ColorSetting("WallFade", new Color(0, 255, 255, 64)).injectBoolean(true));
      this.wallFade.setChinese("\u5899\u58c1\u6de1\u51fa");
      this.wallSideFill = this.add(new ColorSetting("WallSideFill", new Color(0, 255, 255, 128)).injectBoolean(true));
      this.wallSideFill.setChinese("\u5899\u58c1\u4fa7\u9762\u586b\u5145");
      this.wallSideBox = this.add(new ColorSetting("WallSideBox", new Color(0, 225, 255, 255)).injectBoolean(true));
      this.wallSideBox.setChinese("\u5899\u58c1\u4fa7\u9762\u8fb9\u6846");
      this.wallSideFade = this.add(new ColorSetting("WallSideFade", new Color(0, 255, 255, 64)).injectBoolean(true));
      this.wallSideFade.setChinese("\u5899\u58c1\u4fa7\u9762\u6de1\u51fa");
      this.updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000));
      this.updateDelay.setChinese("\u66f4\u65b0\u5ef6\u8fdf");
      this.tempNormalList = new ArrayList<>();
      this.tempBedrockList = new ArrayList<>();
      this.tempAirList = new ArrayList<>();
      this.tempWallList = new ArrayList<>();
      this.tempWallSideList = new ArrayList<>();
      this.timer = new Timer();
      this.drawing = false;
      this.normalList = new ArrayList<>();
      this.bedrockList = new ArrayList<>();
      this.airList = new ArrayList<>();
      this.wallList = new ArrayList<>();
      this.wallSideList = new ArrayList<>();
      this.setChinese("\u5751\u900f\u89c6");
      INSTANCE = this;
   }

   public void onThread() {
      if (!nullCheck() && !this.isOff() && !this.drawing && this.timer.passedMs(this.updateDelay.getValue())) {
         this.normalList = new ArrayList<>(this.tempNormalList);
         this.bedrockList = new ArrayList<>(this.tempBedrockList);
         this.airList = new ArrayList<>(this.tempAirList);
         this.wallList = new ArrayList<>(this.tempWallList);
         this.wallSideList = new ArrayList<>(this.tempWallSideList);
         this.timer.reset();
         this.tempBedrockList.clear();
         this.tempNormalList.clear();
         this.tempAirList.clear();
         this.tempWallList.clear();
         this.tempWallSideList.clear();

         for (BlockPos pos : BlockUtil.getSphere(this.getDistance.getValueFloat(), mc.player.getPos())) {
            if (this.isBedrock(pos) && this.isBedrock(pos.up(2)) && this.isBedrock(pos.down())) {
               Direction side = this.getWallSide(pos);
               if (side != null || !this.sideCheck.getValue()) {
                  this.tempWallList.add(pos);
               }

               if (side != null) {
                  this.tempWallSideList.add(pos.offset(side));
               }
            }

            HoleESP.Type type;
            if ((type = this.isHole(pos)) == HoleESP.Type.Bedrock) {
               this.tempBedrockList.add(pos);
            } else if (type == HoleESP.Type.Normal) {
               this.tempNormalList.add(pos);
            } else if (type == HoleESP.Type.Air) {
               this.tempAirList.add(pos);
            }
         }
      }
   }

   private Direction getWallSide(BlockPos pos) {
      double getDistance = Double.MAX_VALUE;
      Direction side = null;

      for (Direction direction : Direction.values()) {
         BlockPos offsetPos;
         if (direction != Direction.UP
            && direction != Direction.DOWN
            && BlockUtil.canCollide(new Box((offsetPos = pos.offset(direction)).down()))
            && !BlockUtil.canCollide(new Box(offsetPos))
            && !BlockUtil.canCollide(new Box(offsetPos.up()))) {
            if (side == null) {
               side = direction;
               getDistance = mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
            } else if (mc.player.getEyePos().distanceTo(offsetPos.toCenterPos()) < getDistance) {
               side = direction;
               getDistance = mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
            }
         }
      }

      return side;
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   HoleESP.Type isHole(BlockPos pos) {
      if (mc.world.isAir(pos)
         && (!this.airYCheck.getValue() || pos.getY() == mc.player.getBlockY() - 1 || pos.getY() == mc.player.getBlockY())
         && Alien.HOLE.isHard(pos.up())) {
         return HoleESP.Type.Air;
      } else {
         int blockProgress = 0;
         boolean bedRock = true;

         for (Direction i : Direction.values()) {
            if (i != Direction.UP && i != Direction.DOWN && Alien.HOLE.isHard(pos.offset(i))) {
               if (mc.world.getBlockState(pos.offset(i)).getBlock() != Blocks.BEDROCK) {
                  bedRock = false;
               }

               blockProgress++;
            }
         }

         if (mc.world.isAir(pos)
            && mc.world.isAir(pos.up())
            && mc.world.isAir(pos.up(2))
            && blockProgress > 3
            && BlockUtil.canCollide(mc.player, new Box(pos.down()))) {
            return bedRock ? HoleESP.Type.Bedrock : HoleESP.Type.Normal;
         } else {
            return Alien.HOLE.isDoubleHole(pos) ? HoleESP.Type.Normal : HoleESP.Type.None;
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      this.drawing = true;
      this.draw(matrixStack, this.bedrockList, this.bedrockFill, this.bedrockFade, this.bedrockBox, this.height.getValue());
      this.draw(matrixStack, this.airList, this.airFill, this.airFade, this.airBox, this.airHeight.getValue());
      this.draw(matrixStack, this.normalList, this.normalFill, this.normalFade, this.normalBox, this.height.getValue());
      this.draw(matrixStack, this.wallList, this.wallFill, this.wallFade, this.wallBox, this.wallHeight.getValue());
      this.draw(matrixStack, this.wallSideList, this.wallSideFill, this.wallSideFade, this.wallSideBox, this.height.getValue());
      this.drawing = false;
   }

   private void draw(MatrixStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting fade, ColorSetting box, double height) {
      for (BlockPos pos : list) {
         double getDistance = mc.player.getPos().distanceTo(pos.toCenterPos());
         double alpha = getDistance > this.startFade.getValue()
            ? Math.max(Math.min(1.0, 1.0 - (getDistance - this.startFade.getValue()) / (this.getDistance.getValue() - this.startFade.getValue())), 0.0)
            : 1.0;
         Box espBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
         if (fill.booleanValue) {
            if (fade.booleanValue) {
               Render3DUtil.drawFadeFill(
                  matrixStack,
                  espBox,
                  ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)),
                  ColorUtil.injectAlpha(fade.getValue(), (int)(fade.getValue().getAlpha() * alpha))
               );
            } else {
               Render3DUtil.drawFill(matrixStack, espBox, ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)));
            }
         }

         if (box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, espBox, ColorUtil.injectAlpha(box.getValue(), (int)(box.getValue().getAlpha() * alpha)));
         }
      }
   }

   public static enum Type {
      None,
      Air,
      Normal,
      Bedrock;
   }
}
