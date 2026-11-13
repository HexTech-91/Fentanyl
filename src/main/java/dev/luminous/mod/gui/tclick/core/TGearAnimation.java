package dev.luminous.mod.gui.tclick.core;

public class TGearAnimation {
   public static float gearStop = 25.0F;
   public static float gearDuration = 0.5F;
   private float espValue = 1.0F;
   private float prevEspValue;
   private float espSpeed = 1.0F;
   private boolean flipSpeed;

   public float getValue() {
      return TAnim.interpolateFloat(this.prevEspValue, this.espValue, TAnim.tickDelta());
   }

   public void tick() {
      this.prevEspValue = this.espValue;
      this.espValue = this.espValue + this.espSpeed;
      if (this.espSpeed > gearStop) {
         this.flipSpeed = true;
      }

      if (this.espSpeed < -gearStop) {
         this.flipSpeed = false;
      }

      this.espSpeed = this.flipSpeed ? this.espSpeed - gearDuration : this.espSpeed + gearDuration;
   }
}
