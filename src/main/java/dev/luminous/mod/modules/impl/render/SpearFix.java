package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.Locale;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SpearFix extends Module {
   public static SpearFix INSTANCE;
   public final SliderSetting strength = this.add(new SliderSetting("Strength", 1.0, 0.0, 1.0, 0.01));
   public final BooleanSetting itemId = this.add(new BooleanSetting("ItemId", true));
   public final BooleanSetting itemName = this.add(new BooleanSetting("ItemName", true));
   public final BooleanSetting firstPerson = this.add(new BooleanSetting("FirstPerson", true));
   public final BooleanSetting thirdPerson = this.add(new BooleanSetting("ThirdPerson", true));
   public final BooleanSetting suppressEquip = this.add(new BooleanSetting("SuppressEquip", true));
   public final BooleanSetting useAnimation = this.add(new BooleanSetting("UseAnimation", true));
   public final BooleanSetting itemTransform = this.add(new BooleanSetting("ItemTransform", true));
   public final BooleanSetting swingDuration = this.add(new BooleanSetting("SwingDuration", true));
   public final BooleanSetting grimSafe = this.add(new BooleanSetting("GrimSafe", true));
   public final BooleanSetting allowSprint = this.add(new BooleanSetting("AllowSprint", false, () -> !this.grimSafe.getValue()));
   public final BooleanSetting viaUseFallback = this.add(new BooleanSetting("ViaUseFallback", true));
   public final BooleanSetting model = this.add(new BooleanSetting("Model", true));
   public final BooleanSetting sounds = this.add(new BooleanSetting("Sounds", true));
   public final BooleanSetting hitSounds = this.add(new BooleanSetting("HitSounds", true));
   public final BooleanSetting useSounds = this.add(new BooleanSetting("UseSounds", true));
   public final SliderSetting swingTicks = this.add(new SliderSetting("SwingTicks", 18, 6, 30));
   public final SliderSetting chargeDelay = this.add(new SliderSetting("ChargeDelay", 12, 0, 30));
   public final SliderSetting forwardMovement = this.add(new SliderSetting("ForwardMove", 0.38, 0.0, 1.0, 0.01));
   private static final Identifier SPEAR_ATTACK_ID = Identifier.of("minecraft", "item.spear.attack");
   private static final Identifier SPEAR_HIT_ID = Identifier.of("minecraft", "item.spear.hit");
   private static final Identifier SPEAR_USE_ID = Identifier.of("minecraft", "item.spear.use");
   private static final Identifier SPEAR_LUNGE_ID = Identifier.of("minecraft", "item.spear.lunge");
   private static final Identifier SPEAR_WOOD_ATTACK_ID = Identifier.of("minecraft", "item.spear_wood.attack");
   private static final Identifier SPEAR_WOOD_HIT_ID = Identifier.of("minecraft", "item.spear_wood.hit");
   private static final Identifier SPEAR_WOOD_USE_ID = Identifier.of("minecraft", "item.spear_wood.use");
   private static final Identifier[] SPEAR_MODELS = new Identifier[]{
      Identifier.of("minecraft", "item/wooden_spear"),
      Identifier.of("minecraft", "item/wooden_spear_in_hand"),
      Identifier.of("minecraft", "item/stone_spear"),
      Identifier.of("minecraft", "item/stone_spear_in_hand"),
      Identifier.of("minecraft", "item/copper_spear"),
      Identifier.of("minecraft", "item/copper_spear_in_hand"),
      Identifier.of("minecraft", "item/golden_spear"),
      Identifier.of("minecraft", "item/golden_spear_in_hand"),
      Identifier.of("minecraft", "item/iron_spear"),
      Identifier.of("minecraft", "item/iron_spear_in_hand"),
      Identifier.of("minecraft", "item/diamond_spear"),
      Identifier.of("minecraft", "item/diamond_spear_in_hand"),
      Identifier.of("minecraft", "item/netherite_spear"),
      Identifier.of("minecraft", "item/netherite_spear_in_hand"),
      Identifier.of("minecraft", "item/spear_in_hand")
   };
   private static SoundEvent SPEAR_ATTACK;
   private static SoundEvent SPEAR_HIT;
   private static SoundEvent SPEAR_USE;
   private static SoundEvent SPEAR_LUNGE;
   private static SoundEvent SPEAR_WOOD_ATTACK;
   private static SoundEvent SPEAR_WOOD_HIT;
   private static SoundEvent SPEAR_WOOD_USE;
   private long lastAttackSound;
   private long lastUseSound;
   private long lastLungeSound;
   private int fallbackUseTicks;
   private int fallbackKeepAliveTicks;

   public SpearFix() {
      super("SpearFix", Module.Category.Render);
      this.setChinese("\u957f\u77db\u4fee\u8865");
      INSTANCE = this;
      registerSounds();
      registerModels();
   }

   public static void registerModels() {
      try {
         ModelLoadingPlugin.register(var0 -> var0.addModels(SPEAR_MODELS));
      } catch (Throwable var1) {
      }
   }

   public static void registerSounds() {
      SPEAR_ATTACK = registerSound(SPEAR_ATTACK_ID);
      SPEAR_HIT = registerSound(SPEAR_HIT_ID);
      SPEAR_USE = registerSound(SPEAR_USE_ID);
      SPEAR_LUNGE = registerSound(SPEAR_LUNGE_ID);
      SPEAR_WOOD_ATTACK = registerSound(SPEAR_WOOD_ATTACK_ID);
      SPEAR_WOOD_HIT = registerSound(SPEAR_WOOD_HIT_ID);
      SPEAR_WOOD_USE = registerSound(SPEAR_WOOD_USE_ID);
   }

   private static SoundEvent registerSound(Identifier var0) {
      try {
         return (SoundEvent)Registry.register(Registries.SOUND_EVENT, var0, SoundEvent.of(var0));
      } catch (RuntimeException var3) {
         SoundEvent var2 = (SoundEvent)Registries.SOUND_EVENT.get(var0);
         return var2 == null ? SoundEvent.of(var0) : var2;
      }
   }

   public boolean shouldAnimate(ItemStack var1) {
      if (var1 != null && !var1.isEmpty() && !this.isOff()) {
         if (this.itemId.getValue()) {
            String var2 = Registries.ITEM.getId(var1.getItem()).getPath().toLowerCase(Locale.ROOT);
            if (var2.contains("spear")) {
               return true;
            }
         }

         if (this.itemName.getValue()) {
            String var3 = var1.getName().getString().toLowerCase(Locale.ROOT);
            return var3.contains("spear");
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean shouldAnimateFirstPerson(ItemStack var1) {
      return this.firstPerson.getValue() && this.shouldAnimate(var1);
   }

   public boolean shouldAnimateThirdPerson(ItemStack var1) {
      return this.thirdPerson.getValue() && this.shouldAnimate(var1);
   }

   public boolean shouldSuppressEquip(ItemStack var1, float var2) {
      return this.suppressEquip.getValue() && this.shouldAnimateFirstPerson(var1) && var2 > 0.0F;
   }

   public boolean shouldUseAnimate(ItemStack var1) {
      return this.useAnimation.getValue() && this.shouldAnimateFirstPerson(var1);
   }

   public boolean isFallbackUseActive() {
      return this.viaUseFallback.getValue() && this.fallbackUseTicks > 0;
   }

   public float getFallbackUseTicks(float var1) {
      return Math.max(0.0F, this.fallbackUseTicks + var1);
   }

   public boolean shouldTransformHeldItem(ItemStack var1) {
      return this.itemTransform.getValue() && this.shouldAnimateThirdPerson(var1);
   }

   public int getSwingTicks(LivingEntity var1) {
      return this.swingDuration.getValue() && var1 != null && this.shouldAnimate(var1.getMainHandStack())
         ? Math.max(1, this.getSpearProfile(var1.getMainHandStack()).swingTicks())
         : -1;
   }

   public boolean shouldAllowSprint(LivingEntity var1) {
      return !this.grimSafe.getValue() && this.allowSprint.getValue() && var1 != null && var1.isUsingItem() && this.shouldAnimate(var1.getActiveItem());
   }

   public boolean shouldReplaceModel(ItemStack var1) {
      return this.model.getValue() && this.shouldAnimate(var1);
   }

   public Identifier getModelId(ItemStack var1, ModelTransformationMode var2) {
      String var3 = this.getSpearMaterial(var1);
      boolean var4 = var2 == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
         || var2 == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
         || var2 == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
         || var2 == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
      return Identifier.of("minecraft", "item/" + var3 + "_spear" + (var4 ? "_in_hand" : ""));
   }

   public String getSpearMaterial(ItemStack var1) {
      String var2 = "";
      if (var1 != null && !var1.isEmpty()) {
         var2 = Registries.ITEM.getId(var1.getItem()).getPath() + " " + var1.getName().getString();
      }

      var2 = var2.toLowerCase(Locale.ROOT);
      if (var2.contains("wood")) {
         return "wooden";
      } else if (var2.contains("stone")) {
         return "stone";
      } else if (var2.contains("copper")) {
         return "copper";
      } else if (var2.contains("gold")) {
         return "golden";
      } else if (var2.contains("diamond")) {
         return "diamond";
      } else {
         return var2.contains("netherite") ? "netherite" : "iron";
      }
   }

   public void playAttackSound(ItemStack var1, boolean var2) {
      if (this.sounds.getValue() && this.shouldAnimate(var1)) {
         long var3 = System.currentTimeMillis();
         if (var3 - this.lastAttackSound >= 120L) {
            this.lastAttackSound = var3;
            boolean var5 = this.getSpearMaterial(var1).equals("wooden");
            this.playSound(var5 ? SPEAR_WOOD_ATTACK : SPEAR_ATTACK, 1.0F);
            if (var2 && this.hitSounds.getValue()) {
               this.playSound(var5 ? SPEAR_WOOD_HIT : SPEAR_HIT, 1.0F);
            }
         }
      }
   }

   public void playUseSound(ItemStack var1) {
      if (this.sounds.getValue() && this.useSounds.getValue() && this.shouldAnimate(var1)) {
         long var2 = System.currentTimeMillis();
         if (var2 - this.lastUseSound >= 250L) {
            this.lastUseSound = var2;
            this.playSound(this.getSpearMaterial(var1).equals("wooden") ? SPEAR_WOOD_USE : SPEAR_USE, 1.0F);
         }
      }
   }

   public void tickUseSound(LivingEntity var1) {
      if (this.sounds.getValue() && var1 != null) {
         ItemStack var2 = var1.isUsingItem() ? var1.getActiveItem() : var1.getMainHandStack();
         int var3 = var1.isUsingItem() ? var1.getItemUseTime() : this.fallbackUseTicks;
         if (this.shouldAnimate(var2) && var3 >= Math.max(1, (int)this.getSpearProfile(var2).delayTicks())) {
            long var4 = System.currentTimeMillis();
            if (var4 - this.lastLungeSound >= 900L) {
               this.lastLungeSound = var4;
               this.playSound(SPEAR_LUNGE, 1.0F);
            }
         }
      }
   }

   private void playSound(SoundEvent var1, float var2) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var1 != null && var3.getSoundManager() != null) {
         var3.getSoundManager().play(PositionedSoundInstance.master(var1, var2));
      }
   }

   public void applyStabAnimation(MatrixStack var1, float var2, ItemStack var3) {
      float var4 = this.strength.getValueFloat();
      if (!(var4 <= 0.0F)) {
         float var5 = -(MathHelper.cos((float) Math.PI * MathHelper.clamp(MathHelper.getLerpProgress(var2, 0.0F, 0.05F), 0.0F, 1.0F)) - 1.0F) / 2.0F;
         float var6 = MathHelper.clamp(MathHelper.getLerpProgress(var2, 0.05F, 0.2F), 0.0F, 1.0F);
         var6 *= var6;
         float var7 = MathHelper.clamp(MathHelper.getLerpProgress(var2, 0.4F, 1.0F), 0.0F, 1.0F);
         if (var7 < 0.5F) {
            var7 = var7 == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0F * var7 - 10.0F) / 2.0);
         } else {
            var7 = var7 == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0F * var7 + 10.0F)) / 2.0);
         }

         boolean var8 = var3 != null && var3.isOf(Items.TRIDENT);
         var1.translate(
            var4 * var7 * 0.1F * (var5 - var6), var4 * -0.075F * (var5 - var7), var4 * ((var8 ? -0.4F * (var5 - var7) : 0.0F) + 0.65F * (var5 - var6))
         );
         var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var4 * -(var8 ? 50.0F : 70.0F) * (var5 - var7)));
         var1.translate(0.0F, 0.0F, var4 * -0.25F * (var7 - var6));
      }
   }

   public float getThirdPersonSwing(float var1) {
      float var2 = -(MathHelper.cos((float) Math.PI * MathHelper.clamp(MathHelper.getLerpProgress(var1, 0.0F, 0.05F), 0.0F, 1.0F)) - 1.0F) / 2.0F;
      float var3 = MathHelper.clamp(MathHelper.getLerpProgress(var1, 0.05F, 0.2F), 0.0F, 1.0F);
      var3 *= var3;
      float var4 = MathHelper.clamp(MathHelper.getLerpProgress(var1, 0.4F, 1.0F), 0.0F, 1.0F);
      if (var4 < 0.5F) {
         var4 = var4 == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0F * var4 - 10.0F) / 2.0);
      } else {
         var4 = var4 == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0F * var4 + 10.0F)) / 2.0);
      }

      return this.strength.getValueFloat() * (90.0F * var2 - 120.0F * var3 + 30.0F * var4) * (float) (Math.PI / 180.0);
   }

   public void applyFirstPersonUseAnimation(MatrixStack var1, LivingEntity var2, Arm var3, ItemStack var4, float var5) {
      float var6 = this.strength.getValueFloat();
      if (!(var6 <= 0.0F) && var2 != null) {
         int var7 = var3 == Arm.RIGHT ? 1 : -1;
         SpearFix.HoldUpAnimation var8 = this.getHoldUpAnimation(var5, var4);
         var1.translate(var7 * 0.56F, -0.52F, -0.72F);
         var1.translate(
            var6 * var7 * (var8.raiseProgress() * 0.15F + var8.raiseProgressEnd() * -0.05F + var8.swayProgress() * -0.1F + var8.swayScaleSlow() * 0.005F),
            var6 * (var8.raiseProgress() * -0.075F + var8.raiseProgressMiddle() * 0.075F + var8.swayScaleFast() * 0.01F),
            var6 * (var8.raiseProgressStart() * 0.05F + var8.raiseProgressEnd() * -0.05F + var8.swayScaleSlow() * 0.005F)
         );
         float var9 = var8.raiseProgress();
         if (var9 < 0.5F) {
            var9 = 4.0F * var9 * var9 * (7.189819F * var9 - 2.5949094F) / 2.0F;
         } else {
            float var10 = 2.0F * var9 - 2.0F;
            var9 = (var10 * var10 * (3.5949094F * var10 + 2.5949094F) + 2.0F) / 2.0F;
         }

         var1.multiply(
            RotationAxis.POSITIVE_X
               .rotationDegrees(
                  var6 * (-65.0F * var9 - 35.0F * (1.0F - var8.lowerProgress()) + 100.0F * var8.raiseBackProgress() + -0.5F * var8.swayScaleFast())
               ),
            0.0F,
            0.1F,
            0.0F
         );
         var1.multiply(
            RotationAxis.NEGATIVE_Y
               .rotationDegrees(
                  var6
                     * var7
                     * (
                        -90.0F * MathHelper.clamp(MathHelper.getLerpProgress(var8.raiseProgress(), 0.5F, 0.55F), 0.0F, 1.0F)
                           + 90.0F * var8.swayProgress()
                           + 2.0F * var8.swayScaleSlow()
                     )
               ),
            var7 * 0.15F,
            0.0F,
            0.0F
         );
      }
   }

   @EventListener
   public void onTick(ClientTickEvent var1) {
      if (var1.isPost()) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2.player != null && var2.options != null && this.viaUseFallback.getValue()) {
            boolean var3 = var2.options.useKey.isPressed() && this.shouldUseAnimate(var2.player.getMainHandStack());
            boolean var4 = var2.player.isUsingItem()
               && (this.shouldUseAnimate(var2.player.getActiveItem()) || this.shouldUseAnimate(var2.player.getMainHandStack()));
            if (!var3 && !var4 && this.fallbackKeepAliveTicks <= 0) {
               this.fallbackUseTicks = 0;
               this.fallbackKeepAliveTicks = 0;
            } else {
               this.fallbackUseTicks++;
               if (!var3 && !var4) {
                  this.fallbackKeepAliveTicks--;
               } else {
                  this.fallbackKeepAliveTicks = 6;
               }

               if (this.fallbackUseTicks == 1) {
                  this.playUseSound(var2.player.getMainHandStack());
               }
            }
         }
      }
   }

   public void applyThirdPersonHeldItemAnimation(MatrixStack var1, LivingEntity var2, ItemStack var3, Arm var4) {
      if (this.shouldTransformHeldItem(var3) && var2 != null) {
         float var5 = this.strength.getValueFloat();
         float var6 = this.getTickDelta();
         if (var2.handSwingProgress > 0.0F && var2.getMainArm() == var4) {
            float var7 = 0.125F;
            float var8 = var2.isUsingItem() ? 1.0F : var2.getHandSwingProgress(var6);
            float var9 = MathHelper.clamp(MathHelper.getLerpProgress(var8, 0.05F, 0.2F), 0.0F, 1.0F);
            var9 *= var9;
            float var10 = easeOutExpo(MathHelper.clamp(MathHelper.getLerpProgress(var8, 0.4F, 1.0F), 0.0F, 1.0F));
            var1.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(var5 * 70.0F * (var9 - var10)), 0.0F, -var7, var7);
            var1.translate(0.0F, var5 * this.getSpearProfile(var3).forwardMovement() * (var9 - var10), 0.0F);
         }

         if (var2.isUsingItem() && ItemStack.areEqual(var2.getActiveItem(), var3)) {
            float var11 = var3.getMaxUseTime(var2) - (var2.getItemUseTimeLeft() - var6 + 1.0F);
            if (var11 <= 0.0F) {
               return;
            }

            SpearFix.HoldUpAnimation var12 = this.getHoldUpAnimation(var11, var3);
            int var14 = var4 == Arm.RIGHT ? 1 : -1;
            float var15 = 1.0F - var12.raiseProgress() - 1.0F;
            var15 = 1.0F - (1.0F + 2.70158F * var15 * var15 * var15 + 1.70158F * MathHelper.square(var15));
            var1.translate(0.0F, 0.0F, var5 * -this.getSpearProfile(var3).forwardMovement() * (var15 - var12.raiseBackProgress()));
            var1.multiply(
               RotationAxis.NEGATIVE_X.rotationDegrees(var5 * (var12.raiseProgress() * 70.0F - var12.raiseBackProgress() * 70.0F)), 0.0F, -0.03125F, 0.125F
            );
            var1.multiply(
               RotationAxis.POSITIVE_Y.rotationDegrees(var5 * (var12.raiseProgress() * var14 * 90.0F - var12.swayProgress() * var14 * 90.0F)),
               0.0F,
               0.0F,
               0.125F
            );
         }
      }
   }

   public SpearFix.HoldUpAnimation getHoldUpAnimation(float var1) {
      SpearFix.SpearProfile var2 = SpearFix.SpearProfile.IRON;
      return this.getHoldUpAnimation(var1, var2);
   }

   public SpearFix.HoldUpAnimation getHoldUpAnimation(float var1, ItemStack var2) {
      return this.getHoldUpAnimation(var1, this.getSpearProfile(var2));
   }

   private SpearFix.HoldUpAnimation getHoldUpAnimation(float var1, SpearFix.SpearProfile var2) {
      float var3 = var2.delayTicks();
      float var4 = var3 + var2.dismountTicks();
      float var5 = var3 + var2.knockbackTicks();
      float var6 = var3 + var2.damageTicks();
      float var7 = var3 <= 0.0F ? 1.0F : MathHelper.clamp(MathHelper.getLerpProgress(var1, 0.0F, var3), 0.0F, 1.0F);
      float var8 = MathHelper.clamp(MathHelper.getLerpProgress(var7, 0.0F, 0.5F), 0.0F, 1.0F);
      float var9 = MathHelper.clamp(MathHelper.getLerpProgress(var7, 0.5F, 0.8F), 0.0F, 1.0F);
      float var10 = MathHelper.clamp(MathHelper.getLerpProgress(var7, 0.8F, 1.0F), 0.0F, 1.0F);
      float var11 = MathHelper.clamp(MathHelper.getLerpProgress(var1, var4, var5), 0.0F, 1.0F);
      float var12 = MathHelper.clamp(MathHelper.getLerpProgress(var1, var5, var6 - 5.0F), 0.0F, 1.0F);
      if (var12 > 0.0F && var12 < 1.0F) {
         double var13 = Math.sin((20.0 * var12 - 11.125) * Math.PI * 4.0 / 9.0);
         var12 = var12 < 0.5F ? (float)(-(Math.pow(2.0, 20.0 * var12 - 10.0) * var13) / 2.0) : (float)(Math.pow(2.0, -20.0 * var12 + 10.0) * var13 / 2.0 + 1.0);
      }

      var12 = 1.0F - var12 * var12 * var12;
      float var19 = MathHelper.clamp(MathHelper.getLerpProgress(var1, var6 - 5.0F, var6), 0.0F, 1.0F);
      float var14 = 2.0F * (float)Math.sqrt(1.0 + MathHelper.square(1.0F - var11)) - 2.0F * (-((float)Math.sqrt(1.0 - var11 * var11)) + 1.0F);
      float var15 = MathHelper.sin(var1 * 19.0F * (float) (Math.PI / 180.0));
      float var16 = MathHelper.sin(var1 * 30.0F * (float) (Math.PI / 180.0));
      float var17 = Math.clamp((1.0F - var1 / Math.max(Math.max(var4, var5), var6)) * 20.0F, 0.0F, 1.0F) * (2.9F - var14);
      return new SpearFix.HoldUpAnimation(var7, var8, var9, var10, var11, var12, var19, var14, var15 * var17, var16 * var17);
   }

   private SpearFix.SpearProfile getSpearProfile(ItemStack var1) {
      String var2 = this.getSpearMaterial(var1);

      return switch (var2) {
         case "wooden" -> SpearFix.SpearProfile.WOODEN;
         case "stone" -> SpearFix.SpearProfile.STONE;
         case "copper" -> SpearFix.SpearProfile.COPPER;
         case "golden" -> SpearFix.SpearProfile.GOLDEN;
         case "diamond" -> SpearFix.SpearProfile.DIAMOND;
         case "netherite" -> SpearFix.SpearProfile.NETHERITE;
         default -> SpearFix.SpearProfile.IRON;
      };
   }

   private static float easeOutExpo(float var0) {
      if (var0 < 0.5F) {
         return var0 == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0F * var0 - 10.0F) / 2.0);
      } else {
         return var0 == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0F * var0 + 10.0F)) / 2.0);
      }
   }

   public float getTickDelta() {
      return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
   }

   public record HoldUpAnimation(
      float raiseProgress,
      float raiseProgressStart,
      float raiseProgressMiddle,
      float raiseProgressEnd,
      float swayProgress,
      float lowerProgress,
      float raiseBackProgress,
      float swayIntensity,
      float swayScaleSlow,
      float swayScaleFast
   ) {
   }

   private record SpearProfile(int swingTicks, float delayTicks, float dismountTicks, float knockbackTicks, float damageTicks, float forwardMovement) {
      private static final SpearFix.SpearProfile WOODEN = new SpearFix.SpearProfile(13, 15.0F, 100.0F, 120.0F, 300.0F, 0.38F);
      private static final SpearFix.SpearProfile STONE = new SpearFix.SpearProfile(15, 14.0F, 90.0F, 110.0F, 275.0F, 0.38F);
      private static final SpearFix.SpearProfile COPPER = new SpearFix.SpearProfile(17, 13.0F, 80.0F, 100.0F, 250.0F, 0.38F);
      private static final SpearFix.SpearProfile GOLDEN = new SpearFix.SpearProfile(19, 14.0F, 70.0F, 110.0F, 275.0F, 0.38F);
      private static final SpearFix.SpearProfile IRON = new SpearFix.SpearProfile(19, 12.0F, 50.0F, 90.0F, 225.0F, 0.38F);
      private static final SpearFix.SpearProfile DIAMOND = new SpearFix.SpearProfile(21, 10.0F, 60.0F, 80.0F, 200.0F, 0.38F);
      private static final SpearFix.SpearProfile NETHERITE = new SpearFix.SpearProfile(23, 8.0F, 50.0F, 70.0F, 175.0F, 0.38F);
   }
}
