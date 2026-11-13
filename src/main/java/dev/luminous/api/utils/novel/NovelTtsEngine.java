package dev.luminous.api.utils.novel;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class NovelTtsEngine {
   private volatile Process process;
   private volatile String error = "";
   private int rate;

   public void setRate(int rate) {
      this.rate = Math.max(-10, Math.min(10, rate));
   }

   public boolean speak(String text) {
      this.stop();
      if (text != null && !text.isBlank()) {
         String script = "$ErrorActionPreference='Stop';[Console]::InputEncoding=[Text.Encoding]::UTF8;Add-Type -AssemblyName System.Speech;$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;$s.Rate="
            + this.rate
            + ";try{$s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::NotSet,[System.Speech.Synthesis.VoiceAge]::NotSet,0,[Globalization.CultureInfo]'zh-CN')}catch{};$s.Speak([Console]::In.ReadToEnd());$s.Dispose();";

         try {
            Process started = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).redirectErrorStream(true).start();
            this.process = started;
            this.error = "";
            CompletableFuture.runAsync(() -> {
               try (OutputStream output = started.getOutputStream()) {
                  output.write(text.getBytes(StandardCharsets.UTF_8));
               } catch (Exception var8) {
                  this.error = var8.getMessage() == null ? "Speech input failed." : var8.getMessage();
                  started.destroyForcibly();
               }
            });
            return true;
         } catch (Exception var4) {
            this.error = var4.getMessage() == null ? "Windows speech is unavailable." : var4.getMessage();
            this.process = null;
            return false;
         }
      } else {
         this.error = "No readable text.";
         return false;
      }
   }

   public boolean isSpeaking() {
      Process current = this.process;
      return current != null && current.isAlive();
   }

   public void stop() {
      Process current = this.process;
      this.process = null;
      if (current != null && current.isAlive()) {
         current.destroyForcibly();
      }
   }

   public String error() {
      return this.error;
   }
}
