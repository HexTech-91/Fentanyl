package dev.luminous.mod.gui.tclick.core;

import net.minecraft.util.Identifier;

public enum TImageMode {
   None("", 0, 0),
   Bdsm("bdsm.png", 1414, 823),
   Cutie("cutie.png", 476, 490),
   Cutie2("cutie2.png", 540, 540),
   Nya("nya.png", 246, 238),
   Smile("smile.png", 324, 406),
   Bat("bat.png", 352, 503),
   Gacha("gacha.png", 592, 592),
   NFT("nft.png", 562, 494),
   ThousandSeven("thousand_seven.png", 380, 336);

   public final int fileWidth;
   public final int fileHeight;
   public final Identifier file;

   private TImageMode(String nullxx, int nullxxx, int nullxxxx) {
      this.fileWidth = nullxxx;
      this.fileHeight = nullxxxx;
      this.file = Identifier.of("bullwwclient", "textures/gui/images/" + nullxx);
   }
}
