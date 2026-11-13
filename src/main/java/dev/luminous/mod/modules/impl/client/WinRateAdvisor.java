package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.FeatureTranslations;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WinRateAdvisor extends Module {
   public static WinRateAdvisor INSTANCE;
   public final BooleanSetting background = this.add(new BooleanSetting("Background", true));
   public final ColorSetting bgColor = this.add(new ColorSetting("BgColor", new Color(0, 0, 0, 180)));
   public final ColorSetting highWinRateColor = this.add(new ColorSetting("HighWinRateColor", new Color(85, 255, 85)));
   public final ColorSetting mediumWinRateColor = this.add(new ColorSetting("MediumWinRateColor", new Color(255, 255, 85)));
   public final ColorSetting lowWinRateColor = this.add(new ColorSetting("LowWinRateColor", new Color(255, 85, 85)));
   public final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(255, 255, 255)));
   public final SliderSetting xOffset = this.add(new SliderSetting("XOffset", 0.0, -300.0, 300.0, 1.0));
   public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 100.0, 0.0, 500.0, 1.0));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1));
   public final SliderSetting rounding = this.add(new SliderSetting("Rounding", 10.0, 0.0, 30.0, 0.5));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   public final SliderSetting checkRange = this.add(new SliderSetting("CheckRange", 30.0, 5.0, 100.0, 1.0));
   public final SliderSetting healthWeight = this.add(new SliderSetting("HealthWeight", 0.3, 0.0, 1.0, 0.05));
   public final SliderSetting armorWeight = this.add(new SliderSetting("ArmorWeight", 0.25, 0.0, 1.0, 0.05));
   public final SliderSetting distanceWeight = this.add(new SliderSetting("DistanceWeight", 0.2, 0.0, 1.0, 0.05));
   public final SliderSetting countWeight = this.add(new SliderSetting("CountWeight", 0.25, 0.0, 1.0, 0.05));
   public final SliderSetting strafingWeight = this.add(new SliderSetting("StrafingWeight", 0.15, 0.0, 1.0, 0.05));
   public final SliderSetting burrowWeight = this.add(new SliderSetting("BurrowWeight", 0.2, 0.0, 1.0, 0.05));
   public final SliderSetting gapWeight = this.add(new SliderSetting("GapWeight", 0.25, 0.0, 1.0, 0.05));
   public final SliderSetting chasingWeight = this.add(new SliderSetting("ChasingWeight", 0.15, 0.0, 1.0, 0.05));
   private boolean playerInBurrow = false;
   private boolean playerStrafing = false;
   private boolean playerChasing = false;
   private boolean inCombat = false;
   private Map<String, Integer> enemyPops = new HashMap<>();
   private String lastPoppedEnemy = "";
   private long lastPopTime = 0L;
   private static Set<String> friends = new HashSet<>();
   private float lastWinRate = 0.5F;
   private String lastAdvice = "\u7b49\u5f85\u6570\u636e...";
   private long lastUpdateTime = 0L;
   private final long UPDATE_INTERVAL = 2000L;

   public WinRateAdvisor() {
      super("WinRateAdvisor", Module.Category.Client);
      this.setChinese("\u80dc\u7387\u5efa\u8bae");
      INSTANCE = this;
      FeatureTranslations.localizeModule(this);
   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && player != mc.player) {
         String playerName = player.getName().getString();
         if (!this.isFriend(playerName)) {
            this.enemyPops.put(playerName, this.enemyPops.getOrDefault(playerName, 0) + 1);
            this.lastPoppedEnemy = playerName;
            this.lastPopTime = System.currentTimeMillis();
            this.updateAnalysis();
         }
      }
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (!nullCheck()) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastUpdateTime > 2000L) {
            this.updateAnalysis();
            this.lastUpdateTime = currentTime;
         }

         DrawContext context = event.drawContext;
         float scaledWidth = mc.getWindow().getScaledWidth();
         float scaledHeight = mc.getWindow().getScaledHeight();
         float x = scaledWidth / 2.0F + this.xOffset.getValueFloat();
         float y = this.yOffset.getValueFloat();
         float scaleValue = this.scale.getValueFloat();
         float padding = 6.0F * scaleValue;
         float spacing = 5.0F * scaleValue;
         float lineHeight = 10.0F * scaleValue;
         String winRateText = String.format("\u80dc\u7387: %.1f%%", this.lastWinRate * 100.0F);
         String fullText = "\u80dc\u7387\u5206\u6790 " + winRateText + " | \u5efa\u8bae: " + this.lastAdvice;
         float textWidth = mc.textRenderer.getWidth(fullText) * scaleValue;
         float totalWidth = textWidth + padding * 2.0F;
         float totalHeight = lineHeight + padding * 2.0F;
         if (this.background.getValue()) {
            float halfWidth = totalWidth / 2.0F;
            float radius = this.rounding.getValueFloat();
            Render2DUtil.drawRoundedRect(context.getMatrices(), x - halfWidth, y, totalWidth, radius * 2.0F, radius, this.bgColor.getValue());
            float rectHeight = totalHeight - radius;
            if (rectHeight > 0.0F) {
               Render2DUtil.drawRect(context.getMatrices(), x - halfWidth, y + radius, totalWidth, rectHeight, this.bgColor.getValue());
            }
         }

         float textX = x - mc.textRenderer.getWidth(fullText) * scaleValue / 2.0F;
         float textY = y + padding;
         int color = this.textColor.getValue().getRGB();
         if (this.lastWinRate >= 0.7F) {
            color = this.highWinRateColor.getValue().getRGB();
         } else if (this.lastWinRate >= 0.4F) {
            color = this.mediumWinRateColor.getValue().getRGB();
         } else {
            color = this.lowWinRateColor.getValue().getRGB();
         }

         context.drawText(mc.textRenderer, fullText, (int)textX, (int)textY, color, this.shadow.getValue());
         if (this.background.getValue()) {
            float halfWidth = totalWidth / 2.0F;
            float lineY = y + totalHeight - 2.0F;
            float gradientHeight = 2.0F;
            Color startColor;
            Color endColor;
            if (this.lastWinRate >= 0.7F) {
               startColor = new Color(0, 255, 255, 255);
               endColor = new Color(138, 43, 226, 255);
            } else if (this.lastWinRate >= 0.4F) {
               startColor = new Color(30, 144, 255, 255);
               endColor = new Color(0, 255, 255, 255);
            } else {
               startColor = new Color(255, 0, 0, 255);
               endColor = new Color(255, 165, 0, 255);
            }

            float time = (float)System.currentTimeMillis() / 1000.0F;
            float offset = (float)Math.sin(time * 2.0F) * 0.5F + 0.5F;
            Color animStartColor = new Color(
               (int)(startColor.getRed() + (endColor.getRed() - startColor.getRed()) * offset),
               (int)(startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * offset),
               (int)(startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * offset),
               255
            );
            Color animEndColor = new Color(
               (int)(endColor.getRed() + (startColor.getRed() - endColor.getRed()) * offset),
               (int)(endColor.getGreen() + (startColor.getGreen() - endColor.getGreen()) * offset),
               (int)(endColor.getBlue() + (startColor.getBlue() - endColor.getBlue()) * offset),
               255
            );
            Render2DUtil.horizontalGradient(context.getMatrices(), x - halfWidth, lineY, x + halfWidth, lineY + gradientHeight, animStartColor, animEndColor);
         }
      }
   }

   private void updateAnalysis() {
      if (mc.player != null && mc.world != null) {
         this.updatePlayerStatus();
         float playerScore = this.calculatePlayerScore(mc.player);
         float totalEnemyScore = 0.0F;
         int enemyCount = 0;
         int enemiesInBurrow = 0;
         int enemiesStrafing = 0;
         double range = this.checkRange.getValue();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
               double distance = mc.player.squaredDistanceTo(entity);
               if (distance <= range * range) {
                  PlayerEntity enemy = (PlayerEntity)entity;
                  String playerName = enemy.getName().getString();
                  if (!this.isFriend(playerName)) {
                     totalEnemyScore += this.calculatePlayerScore(enemy);
                     enemyCount++;
                     if (this.isInBurrow(enemy)) {
                        enemiesInBurrow++;
                     }

                     if (this.isStrafing(enemy)) {
                        enemiesStrafing++;
                     }
                  }
               }
            }
         }

         if (enemyCount > 0) {
            float avgEnemyScore = totalEnemyScore / enemyCount;
            this.lastWinRate = this.calculateWinRate(playerScore, avgEnemyScore, enemyCount);
            this.lastAdvice = this.generateDetailedAdvice(playerScore, avgEnemyScore, enemyCount, enemiesInBurrow, enemiesStrafing);
         } else {
            this.lastWinRate = 1.0F;
            this.lastAdvice = "\u9644\u8fd1\u65e0\u654c\u4eba";
         }
      } else {
         this.lastWinRate = 0.5F;
         this.lastAdvice = "\u7b49\u5f85\u6570\u636e...";
      }
   }

   private float calculatePlayerScore(PlayerEntity player) {
      float healthScore = player.getHealth() / player.getMaxHealth() * this.healthWeight.getValueFloat();
      float armorScore = 0.0F;
      if (player.getArmor() > 0) {
         armorScore = player.getArmor() / 20.0F * this.armorWeight.getValueFloat();
      }

      float burrowScore = 0.0F;
      if (player == mc.player && this.playerInBurrow) {
         burrowScore = 0.3F * this.burrowWeight.getValueFloat();
      }

      float strafingScore = 0.0F;
      if (player == mc.player && this.playerStrafing) {
         strafingScore = 0.2F * this.strafingWeight.getValueFloat();
      }

      float chasingScore = 0.0F;
      if (player == mc.player && this.playerChasing) {
         chasingScore = 0.15F * this.chasingWeight.getValueFloat();
      }

      return healthScore + armorScore + burrowScore + strafingScore + chasingScore;
   }

   private float calculateWinRate(float playerScore, float avgEnemyScore, int enemyCount) {
      float baseWinRate = playerScore / (playerScore + avgEnemyScore);
      float countFactor = 1.0F - (enemyCount - 1) * 0.1F * this.countWeight.getValueFloat();
      countFactor = Math.max(0.1F, countFactor);
      float finalWinRate = baseWinRate * countFactor;
      return Math.max(0.0F, Math.min(1.0F, finalWinRate));
   }

   private String generateDetailedAdvice(float playerScore, float avgEnemyScore, int enemyCount, int enemiesInBurrow, int enemiesStrafing) {
      if (enemyCount == 0) {
         return "\u9644\u8fd1\u65e0\u654c\u4eba";
      } else {
         long timeSincePop = System.currentTimeMillis() - this.lastPopTime;
         if (timeSincePop < 3000L && !this.lastPoppedEnemy.isEmpty()) {
            int pops = this.enemyPops.getOrDefault(this.lastPoppedEnemy, 0);
            if (pops >= 8) {
               return this.lastPoppedEnemy + " \u7206\u4e86" + pops + "\u4e2a\u56fe\u817e\uff0c\u7acb\u5373\u7ec8\u7ed3\u4ed6\uff01";
            } else if (pops >= 5) {
               return this.lastPoppedEnemy + " \u7206\u4e86" + pops + "\u4e2a\u56fe\u817e\uff0c\u5efa\u8bae\u7acb\u5373\u8ffd\u51fb\uff01";
            } else {
               return pops >= 3
                  ? this.lastPoppedEnemy + " \u7206\u4e86" + pops + "\u4e2a\u56fe\u817e\uff0c\u5efa\u8bae\u7ee7\u7eed\u8ffd\u51fb\uff01"
                  : this.lastPoppedEnemy + " \u7206\u4e86\u56fe\u817e\uff0c\u5efa\u8bae\u8ffd\u51fb\uff01";
            }
         } else if (!this.playerInBurrow && enemiesInBurrow > 0) {
            if (enemiesInBurrow >= 3) {
               return "\u591a\u4e2a\u654c\u4eba\u5728\u65b9\u5757\u4e2d\uff0c\u5efa\u8bae\u4f7f\u7528\u7206\u7834\u6216\u7b49\u5f85";
            } else {
               return enemiesInBurrow >= 2
                  ? "\u591a\u4e2a\u654c\u4eba\u5728\u65b9\u5757\u4e2d\uff0c\u5efa\u8bae\u4f7f\u7528\u65b9\u5757\u63a9\u4f53"
                  : "\u5efa\u8bae\u4f7f\u7528\u65b9\u5757\u63a9\u4f53\u4fdd\u62a4\u81ea\u5df1";
            }
         } else {
            if (this.inCombat) {
               if (this.playerInBurrow) {
                  if (enemiesInBurrow > 0) {
                     if (this.lastWinRate >= 0.7) {
                        return "\u5730\u9053\u6218\u4e2d\u5360\u636e\u4f18\u52bf\uff0c\u5c1d\u8bd5\u7a81\u7834\u654c\u4eba\u9632\u7ebf";
                     }

                     if (this.lastWinRate >= 0.5) {
                        return "\u5730\u9053\u6218\u4e2d\u52bf\u5747\u529b\u654c\uff0c\u4fdd\u6301\u8ddd\u79bb\u5e76\u5bfb\u627e\u7834\u7efd";
                     }

                     return "\u5730\u9053\u6218\u4e2d\u5904\u4e8e\u52a3\u52bf\uff0c\u51c6\u5907\u64a4\u9000\u6216\u5bfb\u627e\u652f\u63f4";
                  }

                  if (this.lastWinRate >= 0.7) {
                     return "\u5730\u9053\u6218\u4e2d\u5360\u636e\u4f18\u52bf\uff0c\u5f15\u8bf1\u654c\u4eba\u8fdb\u5165\u5730\u9053";
                  }

                  if (this.lastWinRate >= 0.5) {
                     return "\u5730\u9053\u6218\u4e2d\u52bf\u5747\u529b\u654c\uff0c\u4fdd\u6301\u8b66\u60d5\u5e76\u51c6\u5907\u53cd\u51fb";
                  }

                  return "\u5730\u9053\u6218\u4e2d\u5904\u4e8e\u52a3\u52bf\uff0c\u5c3d\u5feb\u79bb\u5f00\u5730\u9053\u5bfb\u627e\u673a\u4f1a";
               }

               if (enemiesInBurrow > 0) {
                  if (enemiesInBurrow >= 3) {
                     return "\u591a\u4e2a\u654c\u4eba\u5728\u5730\u9053\u4e2d\uff0c\u5efa\u8bae\u4f7f\u7528\u7206\u7834\u6216\u7b49\u5f85\u65f6\u673a";
                  }

                  if (enemiesInBurrow >= 2) {
                     return "\u4e24\u4e2a\u654c\u4eba\u5728\u5730\u9053\u4e2d\uff0c\u5c1d\u8bd5\u5206\u5272\u6216\u4f7f\u7528\u8303\u56f4\u653b\u51fb";
                  }

                  return "\u654c\u4eba\u5728\u5730\u9053\u4e2d\uff0c\u5c1d\u8bd5\u5f15\u8bf1\u51fa\u6765\u6216\u4f7f\u7528\u9677\u9631";
               }
            }

            if (!this.playerStrafing && enemiesStrafing > 0) {
               if (enemiesStrafing >= 3) {
                  return "\u591a\u4e2a\u654c\u4eba\u5728\u8d70\u4f4d\uff0c\u5efa\u8bae\u4f7f\u7528\u8303\u56f4\u653b\u51fb\u6216\u9677\u9631";
               } else {
                  return enemiesStrafing >= 2
                     ? "\u591a\u4e2a\u654c\u4eba\u5728\u8d70\u4f4d\uff0c\u5efa\u8bae\u4f7f\u7528\u7cbe\u51c6\u653b\u51fb\u6216\u9677\u9631"
                     : "\u5efa\u8bae\u4f7f\u7528\u8d70\u4f4d\u8eb2\u907f\u653b\u51fb";
               }
            } else if (!this.playerChasing && this.lastWinRate >= 0.6F) {
               return "\u5efa\u8bae\u8ffd\u51fb\u654c\u4eba";
            } else if (this.lastWinRate >= 0.95F) {
               return "\u7edd\u5bf9\u78be\u538b\uff0c\u7acb\u5373\u7ec8\u7ed3\u6218\u6597\uff01";
            } else if (this.lastWinRate >= 0.9F) {
               return "\u7edd\u5bf9\u4f18\u52bf\uff0c\u5168\u529b\u8fdb\u653b\uff01";
            } else if (this.lastWinRate >= 0.85F) {
               return "\u4f18\u52bf\u5de8\u5927\uff0c\u4e3b\u52a8\u51fa\u51fb";
            } else if (this.lastWinRate >= 0.8F) {
               return "\u4f18\u52bf\u660e\u663e\uff0c\u53ef\u4ee5\u8fdb\u653b";
            } else if (this.lastWinRate >= 0.75F) {
               return "\u4f18\u52bf\u8f83\u5927\uff0c\u4fdd\u6301\u8282\u594f";
            } else if (this.lastWinRate >= 0.7F) {
               return "\u4f18\u52bf\u8f83\u5927\uff0c\u4fdd\u6301\u538b\u529b";
            } else if (this.lastWinRate >= 0.65F) {
               return "\u6709\u4e00\u5b9a\u4f18\u52bf\uff0c\u7a33\u6b65\u63a8\u8fdb";
            } else if (this.lastWinRate >= 0.6F) {
               return "\u6709\u4e00\u5b9a\u4f18\u52bf\uff0c\u8c28\u614e\u8fdb\u653b";
            } else if (this.lastWinRate >= 0.55F) {
               return "\u5fae\u5f31\u4f18\u52bf\uff0c\u628a\u63e1\u65f6\u673a";
            } else if (this.lastWinRate >= 0.5F) {
               return "\u52bf\u5747\u529b\u654c\uff0c\u5bfb\u627e\u7834\u7efd";
            } else if (this.lastWinRate >= 0.45F) {
               return "\u52bf\u5747\u529b\u654c\uff0c\u4fdd\u6301\u8010\u5fc3";
            } else if (this.lastWinRate >= 0.4F) {
               return "\u52bf\u5747\u529b\u654c\uff0c\u5bfb\u627e\u673a\u4f1a";
            } else if (this.lastWinRate >= 0.35F) {
               return "\u7565\u6709\u52a3\u52bf\uff0c\u8c03\u6574\u6218\u672f";
            } else if (this.lastWinRate >= 0.3F) {
               return "\u7565\u6709\u52a3\u52bf\uff0c\u4fdd\u6301\u8ddd\u79bb";
            } else if (this.lastWinRate >= 0.25F) {
               return "\u5904\u4e8e\u52a3\u52bf\uff0c\u5bfb\u627e\u652f\u63f4";
            } else if (this.lastWinRate >= 0.2F) {
               return "\u5904\u4e8e\u52a3\u52bf\uff0c\u8003\u8651\u64a4\u9000";
            } else {
               return this.lastWinRate >= 0.15F
                  ? "\u52a3\u52bf\u660e\u663e\uff0c\u907f\u514d\u6b63\u9762\u51b2\u7a81"
                  : "\u4e25\u91cd\u52a3\u52bf\uff0c\u7acb\u5373\u64a4\u9000";
            }
         }
      }
   }

   private void updatePlayerStatus() {
      if (mc.player != null) {
         this.playerInBurrow = this.isInBurrow(mc.player);
         this.playerStrafing = this.isStrafing(mc.player);
         this.playerChasing = this.isChasing();
         this.inCombat = this.isInCombat();
      }
   }

   private boolean isInCombat() {
      if (mc.player != null && mc.world != null) {
         if (mc.player.getHealth() < mc.player.getMaxHealth()) {
            return true;
         } else if (mc.player.isUsingItem()) {
            return true;
         } else if (mc.player.getAttackCooldownProgress(0.0F) < 1.0F) {
            return true;
         } else {
            double range = this.checkRange.getValue();

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof PlayerEntity && entity != mc.player) {
                  double distance = mc.player.squaredDistanceTo(entity);
                  if (distance <= range * range) {
                     PlayerEntity enemy = (PlayerEntity)entity;
                     if (enemy.getHealth() < enemy.getMaxHealth()) {
                        return true;
                     }

                     if (enemy.isUsingItem()) {
                        return true;
                     }

                     if (enemy.getAttackCooldownProgress(0.0F) < 1.0F) {
                        return true;
                     }

                     if (Math.abs(enemy.getVelocity().x) > 0.05 || Math.abs(enemy.getVelocity().z) > 0.05) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isInBurrow(PlayerEntity player) {
      BlockPos playerPos = player.getBlockPos();
      BlockPos headPos = player.getBlockPos().up();
      boolean feetBurrowed = !mc.world.getBlockState(playerPos).isAir() && !mc.world.getBlockState(playerPos).isReplaceable();
      boolean headBurrowed = !mc.world.getBlockState(headPos).isAir() && !mc.world.getBlockState(headPos).isReplaceable();
      return feetBurrowed || headBurrowed;
   }

   private boolean isStrafing(PlayerEntity player) {
      double sidewaysMovement = Math.abs(player.getVelocity().x) + Math.abs(player.getVelocity().z);
      return sidewaysMovement > 0.15 && !player.isSprinting();
   }

   private boolean isChasing() {
      if (mc.player != null && mc.world != null) {
         double range = this.checkRange.getValue();
         PlayerEntity nearestEnemy = null;
         double nearestDistance = range * range;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
               double distance = mc.player.squaredDistanceTo(entity);
               if (distance < nearestDistance) {
                  nearestDistance = distance;
                  nearestEnemy = (PlayerEntity)entity;
               }
            }
         }

         if (nearestEnemy == null) {
            return false;
         } else {
            Vec3d playerLook = mc.player.getRotationVec(1.0F);
            Vec3d toEnemy = nearestEnemy.getPos().subtract(mc.player.getPos()).normalize();
            double dotProduct = playerLook.dotProduct(toEnemy);
            boolean isMoving = Math.abs(mc.player.getVelocity().x) > 0.05 || Math.abs(mc.player.getVelocity().z) > 0.05;
            return isMoving && dotProduct > 0.7;
         }
      } else {
         return false;
      }
   }

   private boolean isFriend(String playerName) {
      try {
         if (Alien.FRIEND != null) {
            return Alien.FRIEND.isFriend(playerName);
         }
      } catch (Exception var3) {
      }

      return friends.contains(playerName);
   }

   public void addFriend(String playerName) {
      friends.add(playerName);
   }

   public void removeFriend(String playerName) {
      friends.remove(playerName);
   }

   public Set<String> getFriends() {
      return new HashSet<>(friends);
   }

   static {
      try {
         if (Alien.FRIEND != null) {
            friends.addAll(Alien.FRIEND.friendList);
         }
      } catch (Exception var1) {
      }
   }
}
