package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.DamageUtils;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.enums.PlaceFirstMode;
import dev.luminous.mod.modules.settings.enums.RaytraceMode;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoCrystal extends Module {
   public static AutoCrystal INSTANCE;
   public BlockPos crystalPos;
   public final Timer lastBreakTimer = new Timer();
   private final EnumSetting<AutoCrystal.Page> page = this.add(new EnumSetting<>("Page", AutoCrystal.Page.General));
   final Animation animation;
   final DecimalFormat df;
   private final Timer baseTimer;
   private final Timer placeTimer;
   private final Timer noPosTimer;
   private final Timer switchTimer;
   private final Timer calcDelay;
   private final BindSetting pause;
   private final BooleanSetting witherTarget;
   private final BooleanSetting wardenTarget;
   private final BooleanSetting preferAnchor;
   private final BooleanSetting breakOnlyHasCrystal;
   private final BooleanSetting eatingPause;
   private final SliderSetting switchCooldown;
   private final SliderSetting bossRange;
   private final SliderSetting bossMinDamage;
   private final SliderSetting targetRange;
   private final SliderSetting updateDelay;
   private final BooleanSetting invincibilityTimeout;
   private final SliderSetting invincTimeout;
   private final BooleanSetting selfInvincibility;
   private final BooleanSetting rotate;
   private final BooleanSetting onPlace;
   private final BooleanSetting onBreak;
   private final BooleanSetting yawStep;
   private final BooleanSetting whenElytra;
   private final SliderSetting steps;
   private final BooleanSetting checkFov;
   private final SliderSetting fov;
   private final SliderSetting priority;
   private final SliderSetting minDamage;
   private final SliderSetting maxSelf;
   private final SliderSetting reserve;
   private final BooleanSetting balance;
   private final SliderSetting balanceOffset;
   private final BooleanSetting place;
   public final SliderSetting placeRange;
   private final SliderSetting placeDelay;
   private final EnumSetting<AutoCrystal.SwapMode> autoSwap;
   private final EnumSetting<PlaceFirstMode> placeFirst;
   private final BooleanSetting afterBreak;
   private final BooleanSetting forcePlace;
   private final BooleanSetting facePlace;
   private final SliderSetting facePlaceHealth;
   private final SliderSetting facePlaceArmor;
   private final BooleanSetting breakSetting;
   public final SliderSetting breakRange;
   private final SliderSetting breakDelay;
   private final SliderSetting minAge;
   private final BooleanSetting breakRemove;
   private final BooleanSetting onAdd;
   private final BooleanSetting resetCD;
   private final SliderSetting hitFactor;
   private final EnumSetting<Timing> timing;
   private final BooleanSetting interactOnRender;
   private final SliderSetting wallRange;
   private final EnumSetting<SwingSide> swingMode;
   private final ColorSetting text;
   private final EnumSetting<AutoCrystal.TargetESP> mode;
   private final SliderSetting animationTime;
   private final EnumSetting<Easing> ease;
   private final ColorSetting color;
   private final ColorSetting outlineColor;
   private final ColorSetting hitColor;
   private final ColorSetting hitOutlineColor;
   private final BooleanSetting render;
   private final BooleanSetting sync;
   private final BooleanSetting shrink;
   private final ColorSetting box;
   private final SliderSetting lineWidth;
   private final ColorSetting fill;
   private final SliderSetting sliderSpeed;
   private final SliderSetting startFadeTime;
   private final SliderSetting fadeSpeed;
   private final SliderSetting attackVecStep;
   private final BooleanSetting thread;
   private final BooleanSetting doCrystal;
   private final SliderSetting selfPredict;
   private final SliderSetting predictTicks;
   private final SliderSetting simulation;
   private final SliderSetting maxMotionY;
   private final BooleanSetting step;
   private final BooleanSetting doubleStep;
   private final BooleanSetting jump;
   private final BooleanSetting inBlockPause;
   private final BooleanSetting terrainIgnore;
   private final EnumSetting<RaytraceMode> raytraceMode;
   private final BooleanSetting basePlace;
   private final SliderSetting baseMin;
   private final SliderSetting baseMax;
   private final SliderSetting overrideMax;
   private final BooleanSetting baseBalance;
   private final BooleanSetting onlyBelow;
   private final BooleanSetting inventory;
   private final BooleanSetting detectMining;
   private final SliderSetting delay;
   private final BooleanSetting ignoreMine;
   private final SliderSetting constantProgress;
   private final BooleanSetting antiSurround;
   private final SliderSetting miningProgress;
   private final SliderSetting antiSurroundMax;
   private final BooleanSetting slowPlace;
   private final SliderSetting slowDelay;
   private final SliderSetting slowMinDamage;
   private final BooleanSetting lethalOverride;
   private final SliderSetting forceMaxHealth;
   private final SliderSetting forceMin;
   private final BooleanSetting armorBreaker;
   private final SliderSetting maxDurable;
   private final SliderSetting armorBreakerDamage;
   private final BooleanSetting forceWeb;
   public final BooleanSetting airPlace;
   public final BooleanSetting replace;
   private final SliderSetting hurtTime;
   private final SliderSetting waitHurt;
   private final SliderSetting syncTimeout;
   private final Timer syncTimer;
   public PlayerEntity displayTarget;
   public float breakDamage;
   public float tempDamage;
   public float lastDamage;
   public Vec3d directionVec;
   double currentFade;
   private EndCrystalEntity tempBreakCrystal;
   private EndCrystalEntity breakCrystal;
   private BlockPos tempPos;
   private BlockPos syncPos;
   private Vec3d placeVec3d;
   private Vec3d curVec3d;
   int lastSlot;
   BlockPos tempBasePos;
   BlockPos basePos;
   private boolean facePlacing;
   private final Map<UUID, Long> playerJoinTime;
   private long enableTime;
   private final Set<Integer> attackedIds;

   public AutoCrystal() {
      super("AutoCrystal", Module.Category.Combat);
      this.page.setChinese("\u9875\u9762");
      this.animation = new Animation();
      this.df = new DecimalFormat("0.0");
      this.baseTimer = new Timer();
      this.placeTimer = new Timer();
      this.noPosTimer = new Timer();
      this.switchTimer = new Timer();
      this.calcDelay = new Timer();
      this.pause = this.add(new BindSetting("Pause", -1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.is(AutoCrystal.Page.Check);
         }
      }));
      this.pause.setChinese("\u6682\u505c");
      this.preferAnchor = this.add(new BooleanSetting("PreferAnchor", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }));
      this.preferAnchor.setChinese("\u4f18\u5148\u951a");
      this.breakOnlyHasCrystal = this.add(new BooleanSetting("BreakOnlyHasCrystal", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }));
      this.breakOnlyHasCrystal.setChinese("\u4ec5\u624b\u6301");
      this.eatingPause = this.add(new BooleanSetting("EatingPause", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }));
      this.eatingPause.setChinese("\u8fdb\u98df\u6682\u505c");
      this.switchCooldown = this.add(new SliderSetting("SwitchCooldown", 100, 0, 1000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }).setSuffix("ms"));
      this.switchCooldown.setChinese("\u5207\u6362\u51b7\u5374");
      this.targetRange = this.add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }).setSuffix("m"));
      this.targetRange.setChinese("\u76ee\u6807\u8303\u56f4");
      this.updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }).setSuffix("ms"));
      this.updateDelay.setChinese("\u66f4\u65b0\u5ef6\u8fdf");
      this.witherTarget = this.add(new BooleanSetting("WitherTarget", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.witherTarget.setChinese("\u70b8\u51cb\u96f6");
      this.wardenTarget = this.add(new BooleanSetting("WardenTarget", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.wardenTarget.setChinese("\u70b8\u76d1\u5b88\u8005");
      this.bossRange = this.add(new SliderSetting("BossRange", 12.0, 3.0, 24.0, 0.5, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setSuffix("m"));
      this.bossRange.setChinese("\u602a\u7269\u8303\u56f4");
      this.bossMinDamage = this.add(new SliderSetting("BossMinDamage", 2.0, 0.0, 20.0, 0.5, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.bossMinDamage.setChinese("\u602a\u7269\u6700\u4f4e\u4f24\u5bb3");
      this.invincibilityTimeout = this.add(new BooleanSetting("InvincibilityTimeout", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check;
         }
      }).setParent());
      this.invincibilityTimeout.setChinese("\u65e0\u654c\u68c0\u6d4b");
      this.invincTimeout = this.add(new SliderSetting("InvincTimeout", 5000, 0, 20000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check && AutoCrystal.this.invincibilityTimeout.isOpen();
         }
      }).setSuffix("ms"));
      this.invincTimeout.setChinese("\u65e0\u654c\u65f6\u95f4");
      this.selfInvincibility = this.add(new BooleanSetting("SelfInvincibility", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Check && AutoCrystal.this.invincibilityTimeout.isOpen();
         }
      }));
      this.selfInvincibility.setChinese("\u81ea\u8eab\u65e0\u654c");
      this.rotate = this.add(new BooleanSetting("Rotate", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }).setParent());
      this.rotate.setChinese("\u8f6c\u5934");
      this.onPlace = this.add(new BooleanSetting("OnPlace", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }));
      this.onPlace.setChinese("\u653e\u7f6e\u65f6\u8f6c\u5934");
      this.onBreak = this.add(new BooleanSetting("OnBreak", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }));
      this.onBreak.setChinese("\u5f15\u7206\u65f6\u8f6c\u5934");
      this.yawStep = this.add(new BooleanSetting("YawStep", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }).setParent());
      this.yawStep.setChinese("Yaw\u6b65\u8fdb");
      this.whenElytra = this.add(new BooleanSetting("WhenElytra", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation && AutoCrystal.this.yawStep.isOpen();
         }
      }));
      this.whenElytra.setChinese("\u9798\u7fc5\u98de\u884c");
      this.steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.yawStep.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }));
      this.steps.setChinese("\u6b65\u957f");
      this.checkFov = this.add(new BooleanSetting("CheckFov", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.yawStep.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }));
      this.checkFov.setChinese("\u4ec5\u89c6\u7ebf\u5185");
      this.fov = this.add(
         new SliderSetting(
            "\u89c6\u91ce",
            20.0,
            0.0,
            360.0,
            0.1,
            new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return AutoCrystal.this.rotate.isOpen()
                     && AutoCrystal.this.yawStep.isOpen()
                     && AutoCrystal.this.checkFov.getValue()
                     && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
               }
            }
         )
      );
      this.priority = this.add(new SliderSetting("Priority", 10, 0, 100, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.rotate.isOpen() && AutoCrystal.this.yawStep.isOpen() && AutoCrystal.this.page.getValue() == AutoCrystal.Page.Rotation;
         }
      }));
      this.priority.setChinese("\u4f18\u5148\u7ea7");
      this.minDamage = this.add(new SliderSetting("MinDamage", 5.0, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setSuffix("dmg"));
      this.minDamage.setChinese("\u6700\u5c0f\u4f24\u5bb3");
      this.maxSelf = this.add(new SliderSetting("MaxSelf", 12.0, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setSuffix("dmg"));
      this.maxSelf.setChinese("\u6700\u5927\u81ea\u4f24");
      this.reserve = this.add(new SliderSetting("Reserve", 2.0, 0.0, 10.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setSuffix("hp"));
      this.reserve.setChinese("\u8840\u91cf\u9884\u7559");
      this.balance = this.add(new BooleanSetting("Balance", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setParent());
      this.balance.setChinese("\u5e73\u8861");
      this.balanceOffset = this.add(new SliderSetting("BalanceOffset", 0.0, -20.0, 20.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.balance.isOpen();
         }
      }).setSuffix("hp"));
      this.balanceOffset.setChinese("\u5e73\u8861\u504f\u79fb");
      this.place = this.add(new BooleanSetting("Place", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setParent());
      this.place.setChinese("\u653e\u7f6e");
      this.placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }).setSuffix("m"));
      this.placeRange.setChinese("\u653e\u7f6e\u8303\u56f4");
      this.placeDelay = this.add(new SliderSetting("PlaceDelay", 300, 0, 1000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }).setSuffix("ms"));
      this.placeDelay.setChinese("\u653e\u7f6e\u5ef6\u8fdf");
      this.autoSwap = this.add(new EnumSetting<>("AutoSwap", AutoCrystal.SwapMode.None, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }));
      this.autoSwap.setChinese("\u81ea\u52a8\u5207\u6362");
      this.placeFirst = this.add(new EnumSetting<>("PlaceFirst", PlaceFirstMode.NONE, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }));
      this.placeFirst.setChinese("\u653e\u7f6e\u4f18\u5148");
      this.afterBreak = this.add(new BooleanSetting("AfterBreak", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }));
      this.afterBreak.setChinese("\u5f15\u7206\u540e\u653e\u7f6e");
      this.forcePlace = this.add(new BooleanSetting("ForcePlace", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }));
      this.forcePlace.setChinese("\u5f3a\u5236\u653e\u7f6e");
      this.facePlace = this.add(new BooleanSetting("FacePlace", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen();
         }
      }).setParent());
      this.facePlace.setChinese("\u6253\u8138\u653e\u7f6e");
      this.facePlaceHealth = this.add(new SliderSetting("FacePlaceHealth", 10.0, 0.0, 36.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen() && AutoCrystal.this.facePlace.isOpen();
         }
      }).setSuffix("hp"));
      this.facePlaceHealth.setChinese("\u6253\u8138\u8840\u91cf");
      this.facePlaceArmor = this.add(new SliderSetting("FacePlaceArmor", 30, 0, 100, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.place.isOpen() && AutoCrystal.this.facePlace.isOpen();
         }
      }).setSuffix("%"));
      this.facePlaceArmor.setChinese("\u6253\u8138\u8010\u4e45");
      this.breakSetting = this.add(new BooleanSetting("BreakSetting", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setParent());
      this.breakSetting.setChinese("\u5f15\u7206");
      this.breakRange = this.add(new SliderSetting("BreakRange", 4.0, 0.0, 6.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }).setSuffix("m"));
      this.breakRange.setChinese("\u5f15\u7206\u8303\u56f4");
      this.breakDelay = this.add(new SliderSetting("BreakDelay", 300, 0, 1000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }).setSuffix("ms"));
      this.breakDelay.setChinese("\u5f15\u7206\u5ef6\u8fdf");
      this.minAge = this.add(new SliderSetting("MinAge", 0, 0, 20, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }).setSuffix("tick"));
      this.minAge.setChinese("\u6700\u5c0f\u5e74\u9f84");
      this.breakRemove = this.add(new BooleanSetting("BreakRemove", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }));
      this.breakRemove.setChinese("\u79fb\u9664");
      this.onAdd = this.add(new BooleanSetting("OnAdd", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }));
      this.onAdd.setChinese("\u751f\u6210\u65f6\u6253");
      this.resetCD = this.add(new BooleanSetting("ResetCD", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }));
      this.resetCD.setChinese("\u91cd\u7f6e\u653b\u51fbCD");
      this.hitFactor = this.add(new SliderSetting("HitFactor", 0, 0, 10, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General && AutoCrystal.this.breakSetting.isOpen();
         }
      }));
      this.hitFactor.setChinese("\u591a\u5305\u653b\u51fb");
      this.timing = this.add(new EnumSetting<>("Timing", Timing.All, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.timing.setChinese("\u65f6\u673a");
      this.interactOnRender = this.add(new BooleanSetting("InteractOnRender", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.interactOnRender.setChinese("\u6e32\u67d3\u65f6\u4ea4\u4e92");
      this.wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.0, 6.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }).setSuffix("m"));
      this.wallRange.setChinese("\u7a7f\u5899\u8303\u56f4");
      this.swingMode = this.add(new EnumSetting<>("SwingMode", SwingSide.All, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.General;
         }
      }));
      this.swingMode.setChinese("\u6325\u624b");
      this.text = this.add(new ColorSetting("Text", new Color(-1), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }).injectBoolean(true));
      this.text.setChinese("\u6587\u5b57");
      this.mode = this.add(new EnumSetting<>("Mode", AutoCrystal.TargetESP.Fill, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.mode.setChinese("\u76ee\u6807\u6e32\u67d3");
      this.animationTime = this.add(new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.animationTime.setChinese("\u52a8\u753b\u65f6\u95f4");
      this.ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.ease.setChinese("\u7f13\u52a8");
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.color.setChinese("\u76ee\u6807\u989c\u8272");
      this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.outlineColor.setChinese("\u76ee\u6807\u8f6e\u5ed3\u989c\u8272");
      this.hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.hitColor.setChinese("\u51fb\u4e2d\u989c\u8272");
      this.hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.hitOutlineColor.setChinese("\u51fb\u4e2d\u8f6e\u5ed3\u989c\u8272");
      this.render = this.add(new BooleanSetting("Render", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render;
         }
      }));
      this.render.setChinese("\u6e32\u67d3");
      this.sync = this.add(new BooleanSetting("Sync", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }));
      this.sync.setChinese("\u540c\u6b65");
      this.shrink = this.add(new BooleanSetting("Shrink", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }));
      this.shrink.setChinese("\u6536\u7f29");
      this.box = this.add(new ColorSetting("Box", new Color(255, 255, 255, 255), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }).injectBoolean(true));
      this.box.setChinese("\u8fb9\u6846");
      this.lineWidth = this.add(new SliderSetting("LineWidth", 1.5, 0.01, 3.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }));
      this.lineWidth.setChinese("\u7ebf\u5bbd");
      this.fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }).injectBoolean(true));
      this.fill.setChinese("\u586b\u5145");
      this.sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.2, 0.01, 1.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }));
      this.sliderSpeed.setChinese("\u6ed1\u52a8\u901f\u5ea6");
      this.startFadeTime = this.add(new SliderSetting("StartFadeTime", 0.3, 0.0, 2.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }).setSuffix("s"));
      this.startFadeTime.setChinese("\u5f00\u59cb\u6e10\u9690");
      this.fadeSpeed = this.add(new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Render && AutoCrystal.this.render.getValue();
         }
      }));
      this.fadeSpeed.setChinese("\u6e10\u9690\u901f\u5ea6");
      this.attackVecStep = this.add(new SliderSetting("AttackVecStep", 0.1, 0.01, 1.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.attackVecStep.setChinese("\u653b\u51fb\u5411\u91cf\u6b65\u957f");
      this.thread = this.add(new BooleanSetting("Thread", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.thread.setChinese("\u591a\u7ebf\u7a0b");
      this.doCrystal = this.add(new BooleanSetting("DoCrystal", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.doCrystal.setChinese("\u8ba1\u7b97\u4e2d\u4ea4\u4e92");
      this.selfPredict = this.add(new SliderSetting("SelfPredict", 0, 0, 20, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }).setSuffix("ticks"));
      this.selfPredict.setChinese("\u81ea\u8eab\u9884\u6d4b");
      this.predictTicks = this.add(new SliderSetting("PredictTicks", 4, 0, 20, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }).setSuffix("ticks"));
      this.predictTicks.setChinese("\u9884\u6d4b");
      this.simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.simulation.setChinese("\u6a21\u62df");
      this.maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.maxMotionY.setChinese("\u6700\u5927Y\u901f\u5ea6");
      this.step = this.add(new BooleanSetting("Step", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.step.setChinese("\u8de8\u6b65");
      this.doubleStep = this.add(new BooleanSetting("DoubleStep", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.doubleStep.setChinese("\u53cc\u8de8\u6b65");
      this.jump = this.add(new BooleanSetting("Jump", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.jump.setChinese("\u8df3\u8dc3");
      this.inBlockPause = this.add(new BooleanSetting("InBlockPause", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.inBlockPause.setChinese("\u65b9\u5757\u5185\u6682\u505c");
      this.terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.terrainIgnore.setChinese("\u5ffd\u7565\u5730\u5f62");
      this.raytraceMode = this.add(new EnumSetting<>("RaytraceMode", RaytraceMode.FAST, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Calc;
         }
      }));
      this.raytraceMode.setChinese("\u5c04\u7ebf\u7cbe\u5ea6");
      this.basePlace = this.add(new BooleanSetting("BasePlace", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }));
      this.basePlace.setChinese("\u5e95\u5ea7\u653e\u7f6e");
      this.baseMin = this.add(new SliderSetting("BaseMin", 6.0, 0.0, 36.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }).setSuffix("hp"));
      this.baseMin.setChinese("\u5e95\u5ea7\u6700\u5c0f");
      this.baseMax = this.add(new SliderSetting("BaseMax", 12.0, 0.0, 36.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }).setSuffix("hp"));
      this.baseMax.setChinese("\u5e95\u5ea7\u6700\u5927");
      this.overrideMax = this.add(new SliderSetting("OverrideMax", 8.0, 0.0, 36.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }).setSuffix("hp"));
      this.overrideMax.setChinese("\u8986\u76d6\u4e0a\u9650");
      this.baseBalance = this.add(new BooleanSetting("BaseBalance", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }));
      this.baseBalance.setChinese("\u5e95\u5ea7\u5e73\u8861");
      this.onlyBelow = this.add(new BooleanSetting("OnlyBelow", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }));
      this.onlyBelow.setChinese("\u4ec5\u4e0b\u65b9");
      this.inventory = this.add(new BooleanSetting("Inventory", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }));
      this.inventory.setChinese("\u80cc\u5305\u4ea4\u6362");
      this.detectMining = this.add(new BooleanSetting("DetectMining", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }));
      this.detectMining.setChinese("\u68c0\u6d4b\u6316\u6398");
      this.delay = this.add(new SliderSetting("Delay", 3000, 0, 10000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Base;
         }
      }).setSuffix("ms"));
      this.delay.setChinese("\u5ef6\u8fdf");
      this.ignoreMine = this.add(new BooleanSetting("IgnoreMine", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.ignoreMine.setChinese("\u5ffd\u7565\u6316\u6398");
      this.constantProgress = this.add(new SliderSetting("ConstantProgress", 90.0, 0.0, 100.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.ignoreMine.isOpen();
         }
      }).setSuffix("%"));
      this.constantProgress.setChinese("\u8fdb\u5ea6");
      this.antiSurround = this.add(new BooleanSetting("AntiSurround", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.antiSurround.setChinese("\u53cd\u5305\u56f4");
      this.miningProgress = this.add(new SliderSetting("MiningProgress", 90.0, 0.0, 100.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.antiSurround.isOpen();
         }
      }).setSuffix("%"));
      this.miningProgress.setChinese("\u6316\u6398\u8fdb\u5ea6");
      this.antiSurroundMax = this.add(new SliderSetting("AntiSurroundMax", 5.0, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.antiSurround.isOpen();
         }
      }).setSuffix("dmg"));
      this.antiSurroundMax.setChinese("\u4f4e\u4e8e\u6b64\u4f24\u5bb3");
      this.slowPlace = this.add(new BooleanSetting("SlowPlace", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.slowPlace.setChinese("\u8d85\u65f6");
      this.slowDelay = this.add(new SliderSetting("SlowDelay", 600, 0, 2000, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.slowPlace.isOpen();
         }
      }).setSuffix("ms"));
      this.slowDelay.setChinese("\u8d85\u65f6\u5ef6\u8fdf");
      this.slowMinDamage = this.add(new SliderSetting("SlowMinDamage", 1.5, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.slowPlace.isOpen();
         }
      }).setSuffix("dmg"));
      this.slowMinDamage.setChinese("\u8d85\u65f6\u6700\u5c0f");
      this.lethalOverride = this.add(new BooleanSetting("LethalOverride", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.lethalOverride.setChinese("\u65a9\u6740\u8986\u76d6");
      this.forceMaxHealth = this.add(new SliderSetting("ForceMaxHealth", 7.0, 0.0, 36.0, 0.1, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.lethalOverride.isOpen();
         }
      }).setSuffix("health"));
      this.forceMaxHealth.setChinese("\u4f4e\u4e8e\u8840\u91cf");
      this.forceMin = this.add(new SliderSetting("ForceMin", 1.5, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.lethalOverride.isOpen();
         }
      }).setSuffix("dmg"));
      this.forceMin.setChinese("\u5f3a\u5236\u6700\u5c0f");
      this.armorBreaker = this.add(new BooleanSetting("ArmorBreaker", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.armorBreaker.setChinese("\u7834\u7532");
      this.maxDurable = this.add(new SliderSetting("MaxDurable", 8, 0, 100, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.armorBreaker.isOpen();
         }
      }).setSuffix("%"));
      this.maxDurable.setChinese("\u6700\u5927\u8010\u4e45");
      this.armorBreakerDamage = this.add(new SliderSetting("ArmorBreakerDamage", 3.0, 0.0, 36.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.armorBreaker.isOpen();
         }
      }).setSuffix("dmg"));
      this.armorBreakerDamage.setChinese("\u7834\u7532\u6700\u5c0f");
      this.forceWeb = this.add(new BooleanSetting("ForceWeb", true, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }).setParent());
      this.forceWeb.setChinese("\u8718\u86db\u7f51\u91cd\u7f6e");
      this.airPlace = this.add(new BooleanSetting("AirPlace", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.forceWeb.isOpen();
         }
      }));
      this.airPlace.setChinese("\u7a7a\u653e");
      this.replace = this.add(new BooleanSetting("Replace", false, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc && AutoCrystal.this.forceWeb.isOpen();
         }
      }));
      this.replace.setChinese("\u66ff\u6362");
      this.hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }));
      this.hurtTime.setChinese("\u53d7\u4f24\u65f6\u95f4");
      this.waitHurt = this.add(new SliderSetting("WaitHurt", 10.0, 0.0, 10.0, 1.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }));
      this.waitHurt.setChinese("\u7b49\u5f85\u53d7\u4f24");
      this.syncTimeout = this.add(new SliderSetting("SyncTimeout", 500.0, 0.0, 2000.0, 10.0, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AutoCrystal.this.page.getValue() == AutoCrystal.Page.Misc;
         }
      }));
      this.syncTimeout.setChinese("\u7b49\u5f85\u8d85\u65f6");
      this.syncTimer = new Timer();
      this.directionVec = null;
      this.currentFade = 0.0;
      this.facePlacing = false;
      this.playerJoinTime = new ConcurrentHashMap<>();
      this.enableTime = 0L;
      this.attackedIds = new HashSet<>();
      this.setChinese("\u81ea\u52a8\u6c34\u6676");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoCrystal.CrystalRender());
   }

   @Override
   public String getInfo() {
      return this.displayTarget != null && this.lastDamage > 0.0F ? this.df.format(this.lastDamage) : null;
   }

   @Override
   public void onDisable() {
      this.crystalPos = null;
      this.tempPos = null;
   }

   @Override
   public void onEnable() {
      this.crystalPos = null;
      this.tempPos = null;
      this.tempBreakCrystal = null;
      this.displayTarget = null;
      this.syncTimer.reset();
      this.lastBreakTimer.reset();
      this.enableTime = System.currentTimeMillis();
      this.attackedIds.clear();
   }

   public void onThread() {
      if (!this.isOff() && this.thread.getValue()) {
         this.updateCrystalPos();
      }
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (!nullCheck() && (!this.timing.is(Timing.Pre) || !var1.isPost()) && (!this.timing.is(Timing.Post) || !var1.isPre())) {
         if (!this.thread.getValue()) {
            this.updateCrystalPos();
         }

         if (!this.shouldReturn()) {
            this.doInteract();
            BlockPos var2 = this.basePos;
            if (this.basePlace.getValue() && var2 != null && BlockUtil.canPlace(var2)) {
               this.doPlace(var2);
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack var1) {
      if (this.interactOnRender.getValue() && !this.shouldReturn()) {
         this.doInteract();
         BlockPos var2 = this.basePos;
         if (this.basePlace.getValue() && var2 != null && BlockUtil.canPlace(var2)) {
            this.doPlace(var2);
         }
      }

      if (this.displayTarget != null && !this.noPosTimer.passed(500L)) {
         this.doRender(var1, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget, this.mode.getValue());
      }
   }

   public void doRender(MatrixStack var1, float var2, Entity var3, AutoCrystal.TargetESP var4) {
      switch (var4) {
         case Box:
            Render3DUtil.draw3DBox(
               var1,
               ((IEntity)var3)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var3.lastRenderX, var3.getX(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderY, var3.getY(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderZ, var3.getZ(), (double)var2)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               true,
               true
            );
            break;
         case Fill:
            Render3DUtil.draw3DBox(
               var1,
               ((IEntity)var3)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(var3.lastRenderX, var3.getX(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderY, var3.getY(), (double)var2),
                        MathUtil.interpolate(var3.lastRenderZ, var3.getZ(), (double)var2)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
               ),
               false,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(var1, var3, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(var1, this.displayTarget, this.color.getValue());
      }
   }

   private void doInteract() {
      BlockPos var1 = this.crystalPos;
      if (this.placeFirst.getValue() != PlaceFirstMode.NONE && var1 != null && this.canPlaceCrystal(var1, false, false)) {
         this.doPlace(var1, this.rotate.getValue() && this.onPlace.getValue());
      }

      if (var1 != null) {
         this.doCrystal(var1);
      }

      if (this.breakCrystal != null) {
         this.doBreak(this.breakCrystal);
         this.breakCrystal = null;
      }

      if (this.placeFirst.getValue() == PlaceFirstMode.STRONG && var1 != null && this.canPlaceCrystal(var1, false, false)) {
         this.doPlace(var1, this.rotate.getValue() && this.onPlace.getValue());
      }
   }

   @EventListener
   public void onRotate(RotationEvent var1) {
      if (this.rotate.getValue()
         && this.shouldYawStep()
         && this.directionVec != null
         && this.displayTarget != null
         && !this.noPosTimer.passed(1000L)
         && !this.shouldReturn()) {
         var1.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener(
      priority = -199
   )
   public void onPacketSend(PacketEvent.Send var1) {
      if (!var1.isCancelled() && var1.getPacket() instanceof UpdateSelectedSlotC2SPacket var2 && this.lastSlot != var2.getSelectedSlot()) {
         this.lastSlot = var2.getSelectedSlot();
         this.switchTimer.reset();
      }
   }

   public Vec3d getAttackVec(Vec3d var1) {
      return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), var1, this.breakRange.getValue(), 2.0, this.attackVecStep.getValue());
   }

   private void updateCrystalPos() {
      if (this.calcDelay.passedMs(this.updateDelay.getValue())) {
         this.calcDelay.reset();
         this.calcCrystalPos();
         CombatUtil.modifyPos = null;
         CombatUtil.modifyBlockState = null;
         this.basePos = this.tempBasePos;
         this.lastDamage = this.tempDamage;
         this.breakCrystal = this.tempBreakCrystal;
         this.crystalPos = this.tempPos;
      }
   }

   private boolean shouldReturn() {
      if (this.selfInvincibility.getValue() && System.currentTimeMillis() - this.enableTime < this.invincTimeout.getValue()) {
         this.lastBreakTimer.reset();
         return true;
      } else if ((!this.eatingPause.getValue() || !mc.player.isUsingItem()) && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())) {
         if (this.preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            this.lastBreakTimer.reset();
            return true;
         } else if (this.pause.isPressed()) {
            this.lastBreakTimer.reset();
            return true;
         } else {
            return false;
         }
      } else {
         this.lastBreakTimer.reset();
         return true;
      }
   }

   private void calcCrystalPos() {
      if (!nullCheck()) {
         if (this.breakOnlyHasCrystal.getValue()
            && !mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL)
            && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)
            && !this.hasCrystal()) {
            this.tempPos = null;
            this.tempBreakCrystal = null;
            this.lastBreakTimer.reset();
         } else {
            boolean var1 = this.shouldReturn();
            boolean var2 = this.basePlace.getValue() && this.baseTimer.passedMs(this.delay.getValue()) && this.getBlock() != -1;
            this.tempBreakCrystal = null;
            this.breakDamage = 0.0F;
            this.tempPos = null;
            this.tempDamage = 0.0F;
            this.tempBasePos = null;
            float var3 = 0.0F;
            ArrayList<PlayerEntityPredict> var4 = new ArrayList<>();

            for (PlayerEntity var6 : CombatUtil.getEnemies(this.targetRange.getValueFloat())) {
               if (this.invincibilityTimeout.getValue()) {
                  UUID var7 = var6.getUuid();
                  long var8 = System.currentTimeMillis();
                  this.playerJoinTime.putIfAbsent(var7, var8);
                  if (var8 - this.playerJoinTime.get(var7) < this.invincTimeout.getValue()) {
                     continue;
                  }
               }

               if (var6.hurtTime <= this.hurtTime.getValueInt()) {
                  var4.add(
                     new PlayerEntityPredict(
                        var6,
                        this.maxMotionY.getValue(),
                        this.predictTicks.getValueInt(),
                        this.simulation.getValueInt(),
                        this.step.getValue(),
                        this.doubleStep.getValue(),
                        this.jump.getValue(),
                        this.inBlockPause.getValue()
                     )
                  );
               }
            }

            PlayerEntityPredict var19 = new PlayerEntityPredict(
               mc.player,
               this.maxMotionY.getValue(),
               this.selfPredict.getValueInt(),
               this.simulation.getValueInt(),
               this.step.getValue(),
               this.doubleStep.getValue(),
               this.jump.getValue(),
               this.inBlockPause.getValue()
            );
            if (var4.isEmpty()) {
               this.lastBreakTimer.reset();
            } else {
               for (Entity var26 : Alien.THREAD.getEntities()) {
                  if (var26 instanceof EndCrystalEntity var32 && var26.age >= this.minAge.getValueInt()) {
                     double var9 = mc.player.getEyePos().distanceTo(var32.getPos());
                     if (mc.player.canSee(var32) || var9 <= this.wallRange.getValue()) {
                        float var11 = this.calculateDamage(var32.getPos(), var19.player, var19.predict);

                        for (PlayerEntityPredict var13 : var4) {
                           float var14 = this.calculateDamage(var32.getPos(), var13.player, var13.predict);
                           if (var14 > this.breakDamage
                              && !(var11 > this.maxSelf.getValue())
                              && (
                                 !(this.reserve.getValue() > 0.0)
                                    || !(var11 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                              )
                              && (
                                 !(var14 < EntityUtil.getHealth(var13.player))
                                    || !(var14 < this.getDamage(var13.player))
                                       && (
                                          !this.balance.getValue()
                                             || (
                                                this.getDamage(var13.player) == this.forceMin.getValue()
                                                   ? !(var14 < var11 - 2.5)
                                                   : !(var14 < var11 + this.balanceOffset.getValue())
                                             )
                                       )
                              )) {
                              this.breakDamage = var14;
                              this.tempBreakCrystal = var32;
                              this.displayTarget = var13.player;
                           }
                        }
                     }
                  }
               }

               if (this.doCrystal.getValue() && this.tempBreakCrystal != null && !var1) {
                  this.doBreak(this.tempBreakCrystal);
                  this.tempBreakCrystal = null;
               }

               for (PlayerEntityPredict var27 : var4) {
                  BlockPos var33 = BlockPos.ofFloored(var27.predict.getPos());
                  BlockPos var37 = var33.up();
                  if ((BlockUtil.getBlock(var33) == Blocks.OBSIDIAN || BlockUtil.getBlock(var33) == Blocks.BEDROCK)
                     && this.canPlaceCrystal(var37, true, false)
                     && this.canTouch(var33)) {
                     Vec3d var10 = this.getAttackVec(var37.toBottomCenterPos());
                     if (var10 != null && !this.behindWall(var37, var10)) {
                        float var48 = this.calculateDamage(var37, var19.player, var19.predict);
                        float var55 = this.calculateDamage(var37, var27.player, var27.predict);
                        float var62 = var55 + 2.0F;
                        if (var62 > this.tempDamage
                           && !(var48 > this.maxSelf.getValue())
                           && (this.reserve.getValue() <= 0.0 || !(var48 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue()))
                           && (
                              var55 >= EntityUtil.getHealth(var27.player)
                                 || var55 >= this.getDamage(var27.player)
                                 || this.balance.getValue()
                                    && (
                                       this.getDamage(var27.player) == this.forceMin.getValue()
                                          ? var55 >= var48 - 2.5
                                          : var55 >= var48 + this.balanceOffset.getValue()
                                    )
                           )) {
                           this.displayTarget = var27.player;
                           this.tempPos = var37;
                           this.tempBasePos = null;
                           this.tempDamage = var62;
                        }
                     }
                  }
               }

               this.facePlacing = false;
               if (this.facePlace.getValue() && this.place.getValue()) {
                  for (PlayerEntityPredict var28 : var4) {
                     if (EntityUtil.getHealth(var28.player) <= this.facePlaceHealth.getValue()
                        && EntityUtil.isArmorLow(var28.player, this.facePlaceArmor.getValueInt())) {
                        BlockPos var34 = BlockPos.ofFloored(var28.predict.getPos());
                        BlockPos var38 = var34.up(2);
                        if (this.canPlaceCrystal(var38, true, false) && this.canTouch(var38.down())) {
                           Vec3d var43 = this.getAttackVec(var38.toBottomCenterPos());
                           if (var43 != null && !this.behindWall(var38, var43)) {
                              float var49 = this.calculateDamage(var38, var19.player, var19.predict);
                              float var56 = this.calculateDamage(var38, var28.player, var28.predict);
                              float var63 = var56 + 3.0F;
                              if (var63 > this.tempDamage
                                 && !(var49 > this.maxSelf.getValue())
                                 && (
                                    this.reserve.getValue() <= 0.0
                                       || !(var49 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                 )
                                 && (
                                    var56 >= EntityUtil.getHealth(var28.player)
                                       || var56 >= this.getDamage(var28.player)
                                       || this.balance.getValue()
                                          && (
                                             this.getDamage(var28.player) == this.forceMin.getValue()
                                                ? var56 >= var49 - 2.5
                                                : var56 >= var49 + this.balanceOffset.getValue()
                                          )
                                 )) {
                                 this.displayTarget = var28.player;
                                 this.tempPos = var38;
                                 this.tempBasePos = null;
                                 this.tempDamage = var63;
                                 this.facePlacing = true;
                              }
                           }
                        }
                     }
                  }
               }

               for (BlockPos var29 : BlockUtil.getSphere((float)this.breakRange.getValue() + 1.5F)) {
                  boolean var35 = false;
                  CombatUtil.modifyPos = null;
                  CombatUtil.modifyBlockState = null;
                  if (var2 && BlockUtil.canPlace(var29.down())) {
                     CombatUtil.modifyPos = var29.down();
                     CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                     var35 = true;
                  }

                  if (!var35 || !Alien.BREAK.isMining(var29.down()) || !this.detectMining.getValue()) {
                     Vec3d var39 = this.getAttackVec(var29.toBottomCenterPos());
                     if (var39 != null && !this.behindWall(var29, var39) && this.canTouch(var29.down()) && this.canPlaceCrystal(var29, true, false)) {
                        float var44 = var35
                           ? this.calculateBaseDamage(var29, var19.player, var19.predict)
                           : this.calculateDamage(var29, var19.player, var19.predict);

                        for (PlayerEntityPredict var57 : var4) {
                           if (!var35 || !this.onlyBelow.getValue() || !(var29.getY() - 0.5 > var57.player.getY())) {
                              float var64 = var35
                                 ? this.calculateBaseDamage(var29, var57.player, var57.predict)
                                 : this.calculateDamage(var29, var57.player, var57.predict);
                              if (var35) {
                                 if (this.tempDamage <= this.overrideMax.getValue()
                                    && var64 > this.tempDamage
                                    && var64 > var3
                                    && !(var44 > this.baseMax.getValue())
                                    && (
                                       !(this.reserve.getValue() > 0.0)
                                          || !(var44 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                    )
                                    && (
                                       !(var64 < EntityUtil.getHealth(var57.player))
                                          || !(var64 < this.baseMin.getValue()) && (!this.baseBalance.getValue() || !(var64 < var44))
                                    )) {
                                    this.displayTarget = var57.player;
                                    var3 = var64;
                                    this.tempBasePos = var29.down();
                                    this.tempPos = null;
                                 }
                              } else if (var64 > this.tempDamage
                                 && (var64 >= var3 || this.tempDamage > this.overrideMax.getValue())
                                 && !(var44 > this.maxSelf.getValue())
                                 && (
                                    !(this.reserve.getValue() > 0.0)
                                       || !(var44 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                 )
                                 && (
                                    !(var64 < EntityUtil.getHealth(var57.player))
                                       || !(var64 < this.getDamage(var57.player))
                                          && (
                                             !this.balance.getValue()
                                                || (
                                                   this.getDamage(var57.player) == this.forceMin.getValue()
                                                      ? !(var64 < var44 - 2.5)
                                                      : !(var64 < var44 + this.balanceOffset.getValue())
                                                )
                                          )
                                 )) {
                                 this.displayTarget = var57.player;
                                 this.tempPos = var29;
                                 this.tempBasePos = null;
                                 this.tempDamage = var64;
                              }
                           }
                        }
                     }
                  }
               }

               CombatUtil.modifyPos = null;
               CombatUtil.modifyBlockState = null;
               if (this.antiSurround.getValue()
                  && PacketMine.getBreakPos() != null
                  && PacketMine.progress >= this.miningProgress.getValueFloat()
                  && !BlockUtil.hasEntity(PacketMine.getBreakPos(), false)
                  && this.tempDamage <= this.antiSurroundMax.getValueFloat()) {
                  for (PlayerEntityPredict var30 : var4) {
                     BlockPosX var36 = new BlockPosX(var30.player.getPos().add(0.0, 0.5, 0.0));
                     if (!BlockUtil.canCollide(var30.player, new Box(var36))) {
                        for (Direction var58 : Direction.values()) {
                           if (var58 != Direction.DOWN && var58 != Direction.UP) {
                              BlockPos var65 = var36.offset(var58);
                              if (var65.equals(PacketMine.getBreakPos())) {
                                 for (Direction var17 : Direction.values()) {
                                    if (var17 != Direction.DOWN && var17 != Direction.UP && this.canPlaceCrystal(var65.offset(var17), false, false)) {
                                       float var18 = this.calculateDamage(var65.offset(var17), var19.player, var19.predict);
                                       if (var18 < this.maxSelf.getValue()
                                          && (
                                             !(this.reserve.getValue() > 0.0)
                                                || !(var18 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                          )) {
                                          this.tempPos = var65.offset(var17);
                                          if (this.doCrystal.getValue() && this.tempPos != null && !var1) {
                                             this.doCrystal(this.tempPos);
                                          }

                                          return;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            if ((this.witherTarget.getValue() || this.wardenTarget.getValue()) && !var1) {
               LivingEntity var25 = null;
               double var31 = Double.MAX_VALUE;

               for (Entity var46 : Alien.THREAD.getEntities()) {
                  boolean var52 = this.witherTarget.getValue() && var46 instanceof WitherEntity
                     || this.wardenTarget.getValue() && var46 instanceof WardenEntity;
                  if (var52 && var46.isAlive()) {
                     double var59 = mc.player.getEyePos().distanceTo(var46.getPos());
                     if (var59 <= this.bossRange.getValueFloat() && var59 < var31) {
                        var31 = var59;
                        var25 = (LivingEntity)var46;
                     }
                  }
               }

               if (var25 != null) {
                  EndCrystalEntity var42 = null;
                  float var47 = 0.0F;

                  for (Entity var60 : Alien.THREAD.getEntities()) {
                     if (var60 instanceof EndCrystalEntity var66 && var60.age >= this.minAge.getValueInt()) {
                        double var69 = mc.player.getEyePos().distanceTo(var66.getPos());
                        if (mc.player.canSee(var66) || var69 <= this.wallRange.getValue()) {
                           float var72 = this.calculateDamage(var66.getPos(), var19.player, var19.predict);
                           if (var72 <= this.maxSelf.getValue()) {
                              float var74 = DamageUtils.calculateDamage(var66.getPos(), var25);
                              if (var74 > var47 && var74 >= this.bossMinDamage.getValueFloat()) {
                                 var47 = var74;
                                 var42 = var66;
                              }
                           }
                        }
                     }
                  }

                  if (var42 != null) {
                     if (this.doCrystal.getValue()) {
                        this.doBreak(var42);
                     }
                  } else if (this.tempPos == null && this.tempBasePos == null && this.place.getValue() && this.getBlock() != -1) {
                     BlockPos var54 = null;
                     float var61 = 0.0F;

                     for (BlockPos var70 : BlockUtil.getSphere((float)this.breakRange.getValue() + 1.5F)) {
                        CombatUtil.modifyPos = null;
                        CombatUtil.modifyBlockState = null;
                        Vec3d var71 = this.getAttackVec(var70.toBottomCenterPos());
                        if (var71 != null && !this.behindWall(var70, var71) && this.canTouch(var70.down()) && this.canPlaceCrystal(var70, true, false)) {
                           float var73 = this.calculateDamage(var70, var19.player, var19.predict);
                           if (var73 <= this.maxSelf.getValue()
                              && (
                                 !(this.reserve.getValue() > 0.0)
                                    || !(var73 > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                              )) {
                              float var75 = DamageUtils.calculateDamage(var70.toBottomCenterPos(), var25);
                              if (var75 > var61 && var75 >= this.bossMinDamage.getValueFloat()) {
                                 var61 = var75;
                                 var54 = var70;
                              }
                           }
                        }
                     }

                     if (var54 != null) {
                        this.tempPos = var54;
                        this.tempBasePos = null;
                        this.tempDamage = var61;
                     }
                  }
               }
            }

            if (this.doCrystal.getValue() && this.tempPos != null && !var1) {
               this.doCrystal(this.tempPos);
            }
         }
      }
   }

   @EventListener
   private void onEntity(EntitySpawnedEvent var1) {
      if (this.onAdd.getValue() && var1.getEntity() instanceof EndCrystalEntity var2 && var2.getBlockPos().equals(this.syncPos)) {
         this.doBreak(var2);
      }
   }

   public boolean canPlaceCrystal(BlockPos var1, boolean var2, boolean var3) {
      BlockPos var4 = var1.down();
      BlockPos var5 = var4.up();
      BlockPos var6 = var5.up();
      return (BlockUtil.getBlock(var4) == Blocks.BEDROCK || BlockUtil.getBlock(var4) == Blocks.OBSIDIAN)
         && BlockUtil.getClickSideStrict(var4) != null
         && this.noEntityBlockCrystal(var5, var2, var3)
         && this.noEntityBlockCrystal(var6, var2, var3)
         && (mc.world.isAir(var5) || BlockUtil.getBlock(var5) == Blocks.COBWEB || BlockUtil.hasCrystal(var5) && BlockUtil.getBlock(var5) == Blocks.FIRE)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(var6) || BlockUtil.getBlock(var6) == Blocks.COBWEB);
   }

   private boolean noEntityBlockCrystal(BlockPos var1, boolean var2, boolean var3) {
      for (Entity var5 : BlockUtil.getEntities(new Box(var1))) {
         if (var5.isAlive()
            && (!(var5 instanceof ItemEntity) || !var3)
            && (
               !(var5 instanceof EndCrystalEntity)
                  || !var2
                  || this.getAttackVec(var5.getPos()) == null
                  || !mc.player.canSee(var5) && !(mc.player.getEyePos().distanceTo(var5.getPos()) <= this.wallRange.getValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public boolean behindWall(BlockPos var1, Vec3d var2) {
      Vec3d[] var3 = this.getRaytraceSamplePoints(var1);

      for (Vec3d var7 : var3) {
         BlockHitResult var8 = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), var7, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (var8 == null || var8.getType() == Type.MISS) {
            return false;
         }

         if (mc.player.getEyePos().distanceTo(var2) > this.wallRange.getValue()) {
            return true;
         }
      }

      return mc.player.getEyePos().distanceTo(var2) > this.wallRange.getValue();
   }

   private Vec3d[] getRaytraceSamplePoints(BlockPos var1) {
      switch ((RaytraceMode)this.raytraceMode.getValue()) {
         case FAST:
         default:
            return new Vec3d[]{new Vec3d(var1.getX() + 0.5, var1.getY() + 1.7, var1.getZ() + 0.5)};
         case X2:
            return new Vec3d[]{
               new Vec3d(var1.getX() + 0.5, var1.getY() + 1.0, var1.getZ() + 0.5), new Vec3d(var1.getX() + 0.5, var1.getY() + 1.7, var1.getZ() + 0.5)
            };
         case X4:
            return new Vec3d[]{
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.0, var1.getZ() + 0.3),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.0, var1.getZ() + 0.7),
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.7, var1.getZ() + 0.7),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.7, var1.getZ() + 0.3)
            };
         case X8:
            return new Vec3d[]{
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.0, var1.getZ() + 0.3),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.0, var1.getZ() + 0.3),
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.0, var1.getZ() + 0.7),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.0, var1.getZ() + 0.7),
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.7, var1.getZ() + 0.3),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.7, var1.getZ() + 0.3),
               new Vec3d(var1.getX() + 0.3, var1.getY() + 1.7, var1.getZ() + 0.7),
               new Vec3d(var1.getX() + 0.7, var1.getY() + 1.7, var1.getZ() + 0.7)
            };
      }
   }

   private boolean canTouch(BlockPos var1) {
      Direction var2 = BlockUtil.getClickSideStrict(var1);
      return var2 != null
         && var1.toCenterPos()
               .add(new Vec3d(var2.getVector().getX() * 0.5, var2.getVector().getY() * 0.5, var2.getVector().getZ() * 0.5))
               .distanceTo(mc.player.getEyePos())
            <= this.placeRange.getValue();
   }

   private void doCrystal(BlockPos var1) {
      this.doPlace(var1, this.rotate.getValue() && this.onPlace.getValue());
      this.doBreak(var1);
   }

   private void doPlace(BlockPos var1) {
      if (this.baseTimer.passed((long)this.delay.getValue()) && (!this.detectMining.getValue() || !Alien.BREAK.isMining(var1))) {
         int var2 = this.getBlock();
         if (var2 != -1) {
            int var3 = mc.player.getInventory().selectedSlot;
            this.baseSwap(var2);
            BlockUtil.placeBlock(var1, this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.baseSwap(var2);
               EntityUtil.syncInventory();
            } else {
               this.baseSwap(var3);
            }

            this.baseTimer.reset();
         }
      }
   }

   public float calculateDamage(BlockPos var1, PlayerEntity var2, PlayerEntity var3) {
      return this.calculateDamage(new Vec3d(var1.getX() + 0.5, var1.getY(), var1.getZ() + 0.5), var2, var3);
   }

   public float calculateDamage(Vec3d var1, PlayerEntity var2, PlayerEntity var3) {
      if (this.ignoreMine.getValue()
         && PacketMine.getBreakPos() != null
         && mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()
         && PacketMine.progress >= this.constantProgress.getValue() / 100.0) {
         CombatUtil.modifyPos = PacketMine.getBreakPos();
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
      }

      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float var4 = ExplosionUtil.calculateDamage(var1, var2, var3, 6.0F);
      CombatUtil.modifyPos = null;
      CombatUtil.terrainIgnore = false;
      return var4;
   }

   public float calculateBaseDamage(BlockPos var1, PlayerEntity var2, PlayerEntity var3) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float var4 = DamageUtils.overridingExplosionDamage(
         var2, var3, new Vec3d(var1.getX() + 0.5, var1.getY(), var1.getZ() + 0.5), 12.0F, var1.down(), Blocks.OBSIDIAN.getDefaultState()
      );
      CombatUtil.terrainIgnore = false;
      return var4;
   }

   private double getDamage(PlayerEntity var1) {
      if (!PacketMine.INSTANCE.obsidian.isPressed() && this.slowPlace.getValue() && this.lastBreakTimer.passed((long)this.slowDelay.getValue())) {
         return this.slowMinDamage.getValue();
      } else if (this.lethalOverride.getValue() && EntityUtil.getHealth(var1) <= this.forceMaxHealth.getValue() && !PacketMine.INSTANCE.obsidian.isPressed()) {
         return this.forceMin.getValue();
      } else {
         if (this.armorBreaker.getValue()) {
            for (ItemStack var3 : var1.getInventory().armor) {
               if (!var3.isEmpty() && !(EntityUtil.getDamagePercent(var3) > this.maxDurable.getValue())) {
                  return this.armorBreakerDamage.getValue();
               }
            }
         }

         return this.minDamage.getValue();
      }
   }

   private boolean shouldYawStep() {
      return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying()))
         && this.yawStep.getValue()
         && !Velocity.INSTANCE.noRotation();
   }

   public boolean hasCrystal() {
      return this.autoSwap.getValue() != AutoCrystal.SwapMode.None && this.getCrystal() != -1;
   }

   private void doBreak(EndCrystalEntity var1) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (mc.player.isUsingItem()) {
            return;
         }

         if (var1.isAlive()
            && (
               this.displayTarget == null || this.displayTarget.hurtTime <= this.waitHurt.getValueInt() || this.syncTimer.passedMs(this.syncTimeout.getValue())
            )) {
            this.lastBreakTimer.reset();
            this.syncTimer.reset();
            if (var1.age >= this.minAge.getValueInt()) {
               if (!this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                  if (this.forcePlace.getValue() && this.crystalPos != null) {
                     this.doPlace(this.crystalPos, false);
                  }
               } else {
                  if (this.rotate.getValue() && this.onBreak.getValue()) {
                     Vec3d var2 = this.getAttackVec(var1.getPos());
                     Vec3d var3 = var2 == null ? var1.getPos() : var2;
                     if (this.shouldYawStep()) {
                        this.directionVec = var3;
                     } else {
                        Alien.ROTATION.lookAt(var3);
                     }
                  }

                  if (!CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                     if (this.forcePlace.getValue() && this.crystalPos != null) {
                        this.doPlace(this.crystalPos, false);
                     }
                  } else {
                     this.animation.to = 1.0;
                     this.animation.from = 1.0;
                     CombatUtil.breakTimer.reset();
                     this.syncPos = var1.getBlockPos();
                     mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
                     if (this.hitFactor.getValueInt() > 0 && !this.attackedIds.contains(var1.getId())) {
                        this.attackedIds.add(var1.getId());

                        for (int var4 = 1; var4 <= this.hitFactor.getValueInt(); var4++) {
                           mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
                        }
                     }

                     if (this.resetCD.getValue()) {
                        mc.player.resetLastAttackedTicks();
                     }

                     EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                     if (this.breakRemove.getValue()) {
                        mc.world.removeEntity(var1.getId(), RemovalReason.KILLED);
                     }

                     BlockPos var5 = this.crystalPos;
                     if (var5 != null
                        && this.displayTarget != null
                        && this.lastDamage >= this.getDamage(this.displayTarget)
                        && this.afterBreak.getValue()
                        && (
                           !this.rotate.getValue()
                              || !this.shouldYawStep()
                              || !this.checkFov.getValue()
                              || Alien.ROTATION.inFov(var1.getPos(), this.fov.getValueFloat())
                        )) {
                        this.doPlace(var5, false);
                     }

                     if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) {
                        AutoWeb.force = true;
                     }

                     if (this.rotate.getValue() && !this.shouldYawStep()) {
                        Alien.ROTATION.snapBack();
                     }
                  }
               }
            }
         }
      }
   }

   private void doBreak(BlockPos var1) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (mc.player.isUsingItem()) {
            return;
         }

         if (this.displayTarget == null || this.displayTarget.hurtTime <= this.waitHurt.getValueInt() || this.syncTimer.passedMs(this.syncTimeout.getValue())) {
            this.lastBreakTimer.reset();
            this.syncTimer.reset();

            for (EndCrystalEntity var3 : BlockUtil.getEndCrystals(
               new Box(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1, var1.getY() + 2, var1.getZ() + 1)
            )) {
               if (var3.age >= this.minAge.getValueInt() && var3.isAlive()) {
                  if (!this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                     if (this.forcePlace.getValue() && this.crystalPos != null) {
                        this.doPlace(this.crystalPos, false);
                     }

                     return;
                  }

                  if (this.rotate.getValue() && this.onBreak.getValue()) {
                     Vec3d var4 = this.getAttackVec(var3.getPos());
                     Vec3d var5 = var4 == null ? var3.getPos() : var4;
                     if (this.shouldYawStep()) {
                        this.directionVec = var5;
                     } else {
                        Alien.ROTATION.lookAt(var5);
                     }
                  }

                  if (!CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                     if (this.forcePlace.getValue() && this.crystalPos != null) {
                        this.doPlace(this.crystalPos, false);
                     }

                     return;
                  }

                  this.animation.to = 1.0;
                  this.animation.from = 1.0;
                  CombatUtil.breakTimer.reset();
                  this.syncPos = var1;
                  mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var3, mc.player.isSneaking()));
                  if (this.hitFactor.getValueInt() > 0 && !this.attackedIds.contains(var3.getId())) {
                     this.attackedIds.add(var3.getId());

                     for (int var6 = 1; var6 <= this.hitFactor.getValueInt(); var6++) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(var3, mc.player.isSneaking()));
                     }
                  }

                  if (this.resetCD.getValue()) {
                     mc.player.resetLastAttackedTicks();
                  }

                  EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                  if (this.breakRemove.getValue()) {
                     mc.world.removeEntity(var3.getId(), RemovalReason.KILLED);
                  }

                  BlockPos var7 = this.crystalPos;
                  if (var7 != null
                     && this.displayTarget != null
                     && this.lastDamage >= this.getDamage(this.displayTarget)
                     && this.afterBreak.getValue()
                     && (
                        !this.rotate.getValue()
                           || !this.shouldYawStep()
                           || !this.checkFov.getValue()
                           || Alien.ROTATION.inFov(var3.getPos(), this.fov.getValueFloat())
                     )) {
                     this.doPlace(var7, false);
                  }

                  if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) {
                     AutoWeb.force = true;
                  }

                  if (this.rotate.getValue() && !this.shouldYawStep()) {
                     Alien.ROTATION.snapBack();
                  }

                  return;
               }
            }

            if (this.forcePlace.getValue() && this.crystalPos != null) {
               this.doPlace(this.crystalPos, false);
            }
         }
      }
   }

   private void doPlace(BlockPos var1, boolean var2) {
      this.noPosTimer.reset();
      if (this.place.getValue()) {
         if (mc.player.isUsingItem()) {
            return;
         }

         if (var1 == null || !this.canPlaceAt(var1)) {
            return;
         }

         if ((
               mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL)
                  || mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)
                  || this.hasCrystal()
            )
            && this.placeTimer.passed((long)this.placeDelay.getValue())) {
            if (var2) {
               BlockPos var3 = var1.down();
               Direction var4 = BlockUtil.getClickSide(var3);
               if (var4 == null) {
                  return;
               }

               Vec3d var5 = var3.toCenterPos().add(var4.getVector().getX() * 0.5, var4.getVector().getY() * 0.5, var4.getVector().getZ() * 0.5);
               if (var4 != Direction.UP && var4 != Direction.DOWN) {
                  var5 = var5.add(0.0, 0.45, 0.0);
               }

               if (!this.faceVector(var5)) {
                  return;
               }
            }

            if (!mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)) {
               int var6 = mc.player.getInventory().selectedSlot;
               int var7 = this.getCrystal();
               if (var7 == -1) {
                  return;
               }

               this.doSwap(var7);
               this.placeCrystal(var1);
               if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Silent) {
                  this.doSwap(var6);
               } else if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory) {
                  this.doSwap(var7);
                  EntityUtil.syncInventory();
               }
            } else {
               this.placeCrystal(var1);
            }

            if (var2 && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   private boolean canPlaceAt(BlockPos var1) {
      BlockPos var2 = var1.down();
      if (!mc.world.isChunkLoaded(var2.getX() >> 4, var2.getZ() >> 4)) {
         return false;
      } else if (mc.world.getBlockState(var2).isAir()) {
         return false;
      } else if (!mc.world.getOtherEntities(null, new Box(var1)).isEmpty()) {
         return false;
      } else {
         double var3 = mc.player.getEyePos().distanceTo(var1.toCenterPos());
         return !(var3 > Math.max(3.0, this.placeRange.getValue() + 0.5));
      }
   }

   private void doSwap(int var1) {
      if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Silent || this.autoSwap.getValue() == AutoCrystal.SwapMode.Normal) {
         InventoryUtil.switchToSlot(var1);
      } else if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory) {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
      }
   }

   private void baseSwap(int var1) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(var1);
      } else {
         InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private int getCrystal() {
      if (this.autoSwap.getValue() != AutoCrystal.SwapMode.Silent && this.autoSwap.getValue() != AutoCrystal.SwapMode.Normal) {
         return this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : -1;
      } else {
         return InventoryUtil.findItem(Items.END_CRYSTAL);
      }
   }

   private void placeCrystal(BlockPos var1) {
      boolean var2 = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
      BlockPos var3 = var1.down();
      Direction var4 = null;
      Vec3d var5 = null;
      Vec3d var6 = mc.player.getEyePos();
      Vec3d var7 = mc.player.getPos().add(0.0, (double)mc.player.getStandingEyeHeight(), 0.0);
      double var8 = Math.max(3.0, this.placeRange.getValue() + 0.5);
      Vec3d var9 = var3.toCenterPos();
      boolean var10 = var9.squaredDistanceTo(var7) <= var8 * var8;
      double var11 = Double.MAX_VALUE;
      double[] var13 = new double[]{0.3, 0.7};
      double[] var14 = new double[]{0.3, 0.7, 0.9};

      for (double var18 : var13) {
         for (double var22 : var14) {
            for (double var26 : var13) {
               Vec3d var30 = new Vec3d(var3.getX() + var18, var3.getY() + var22, var3.getZ() + var26);
               double var31 = var30.squaredDistanceTo(var7);
               if (!(Math.sqrt(var31) > var8) && !(Math.sqrt(var31) >= var11)) {
                  BlockHitResult var33 = mc.world.raycast(new RaycastContext(var6, var30, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                  if (var33 != null && var33.getType() == Type.BLOCK && var33.getBlockPos().equals(var3)) {
                     double var34 = var33.getPos().distanceTo(var6);
                     if (!(var34 > var8) && BlockUtil.isStrictDirection(var3, var33.getSide())) {
                        var5 = var33.getPos();
                        var4 = var33.getSide();
                        var11 = var31;
                     }
                  }
               }
            }
         }
      }

      if (var4 == null) {
         if (!var10) {
            return;
         }

         var4 = Direction.UP;
         var5 = null;
      }

      final Hand var35 = var2 ? Hand.OFF_HAND : Hand.MAIN_HAND;
      Vec3d var12 = var5 != null ? var5 : var3.toCenterPos().offset(var4, 0.5);
      final BlockHitResult var36 = new BlockHitResult(var12, var4, var3, false);
      sendSequencedPacket(new SequencedPacketCreator() {
         public PlayerInteractBlockC2SPacket predict(int var1) {
            return new PlayerInteractBlockC2SPacket(var35, var36, var1);
         }
      });
      EntityUtil.swingHand(var35, this.swingMode.getValue());
      this.syncPos = var1;
      this.placeTimer.reset();
   }

   private boolean faceVector(Vec3d var1) {
      if (var1 == null) {
         return false;
      } else if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(var1);
         return true;
      } else {
         this.directionVec = var1;
         return Alien.ROTATION.inFov(var1, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   public void setRefillPause(boolean var1) {
   }

   private class CrystalRender {
      @EventListener
      public void onRender3D(Render3DEvent var1) {
         BlockPos var2 = AutoCrystal.this.sync.getValue() && AutoCrystal.this.crystalPos != null ? AutoCrystal.this.syncPos : AutoCrystal.this.crystalPos;
         if (var2 != null) {
            AutoCrystal.this.placeVec3d = var2.down().toCenterPos();
         }

         if (AutoCrystal.this.placeVec3d != null) {
            if (AutoCrystal.this.fadeSpeed.getValue() >= 1.0) {
               AutoCrystal.this.currentFade = AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5;
            } else {
               AutoCrystal.this.currentFade = AnimateUtil.animate(
                  AutoCrystal.this.currentFade,
                  AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                  AutoCrystal.this.fadeSpeed.getValue() / 10.0
               );
            }

            if (AutoCrystal.this.currentFade == 0.0) {
               AutoCrystal.this.curVec3d = null;
            } else {
               if (AutoCrystal.this.curVec3d != null && !(AutoCrystal.this.sliderSpeed.getValue() >= 1.0)) {
                  AutoCrystal.this.curVec3d = new Vec3d(
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.x, AutoCrystal.this.placeVec3d.x, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.y, AutoCrystal.this.placeVec3d.y, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.z, AutoCrystal.this.placeVec3d.z, AutoCrystal.this.sliderSpeed.getValue() / 10.0)
                  );
               } else {
                  AutoCrystal.this.curVec3d = AutoCrystal.this.placeVec3d;
               }

               if (AutoCrystal.this.render.getValue()) {
                  Box var3 = new Box(AutoCrystal.this.curVec3d, AutoCrystal.this.curVec3d);
                  if (AutoCrystal.this.shrink.getValue()) {
                     var3 = var3.expand(AutoCrystal.this.currentFade);
                  } else {
                     var3 = var3.expand(0.5);
                  }

                  MatrixStack var4 = var1.matrixStack;
                  if (AutoCrystal.this.fill.booleanValue) {
                     Render3DUtil.drawFill(
                        var4,
                        var3,
                        ColorUtil.injectAlpha(
                           AutoCrystal.this.fill.getValue(), (int)(AutoCrystal.this.fill.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)
                        )
                     );
                  }

                  if (AutoCrystal.this.box.booleanValue) {
                     Render3DUtil.drawBox(
                        var4,
                        var3,
                        ColorUtil.injectAlpha(
                           AutoCrystal.this.box.getValue(), (int)(AutoCrystal.this.box.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)
                        ),
                        AutoCrystal.this.lineWidth.getValueFloat()
                     );
                  }
               }

               if (AutoCrystal.this.text.booleanValue
                  && AutoCrystal.this.lastDamage > 0.0F
                  && !AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0))) {
                  Render3DUtil.drawText3D(AutoCrystal.this.df.format(AutoCrystal.this.lastDamage), AutoCrystal.this.curVec3d, AutoCrystal.this.text.getValue());
               }
            }
         }
      }
   }

   private static enum Page {
      General,
      Base,
      Misc,
      Rotation,
      Check,
      Calc,
      Render;
   }

   private static enum SwapMode {
      None,
      Normal,
      Silent,
      Inventory;
   }

   public static enum TargetESP {
      Box,
      Fill,
      Jello,
      ThunderHack,
      None;
   }
}
