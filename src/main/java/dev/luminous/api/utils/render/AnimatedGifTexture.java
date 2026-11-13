package dev.luminous.api.utils.render;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class AnimatedGifTexture {
   private static final int DEFAULT_FRAME_DELAY_MS = 100;
   private static final int MAX_CATCH_UP_FRAMES = 5;
   private final Identifier gifResource;
   private final Identifier fallbackTexture;
   private final Identifier dynamicTextureId;
   private byte[] gifData;
   private ImageReader reader;
   private ImageInputStream imageInput;
   private BufferedImage canvas;
   private BufferedImage previousSnapshot;
   private AnimatedGifTexture.FrameInfo previousInfo;
   private int[] pixelBuffer;
   private NativeImageBackedTexture texture;
   private int frameCount;
   private int frameIndex = -1;
   private long nextFrameAt;
   private boolean loadAttempted;
   private boolean animationFailed;
   private boolean pausedForFocusLoss;

   public AnimatedGifTexture(Identifier gifResource, Identifier fallbackTexture, Identifier dynamicTextureId) {
      this.gifResource = gifResource;
      this.fallbackTexture = fallbackTexture;
      this.dynamicTextureId = dynamicTextureId;
   }

   public Identifier getTexture() {
      if (!this.loadAttempted) {
         this.load();
      }

      if (this.texture != null && !this.animationFailed) {
         this.updateFrame();
         return this.animationFailed ? this.fallbackTexture : this.dynamicTextureId;
      } else {
         return this.fallbackTexture;
      }
   }

   private void load() {
      this.loadAttempted = true;
      MinecraftClient client = MinecraftClient.getInstance();
      Optional<Resource> resource = client.getResourceManager().getResource(this.gifResource);
      if (!resource.isEmpty()) {
         try (InputStream input = resource.get().getInputStream()) {
            this.gifData = input.readAllBytes();
            this.openDecoder(true);
            int firstDelay = this.decodeNextFrame();
            NativeImage textureImage = new NativeImage(this.canvas.getWidth(), this.canvas.getHeight(), true);
            this.texture = new NativeImageBackedTexture(textureImage);
            client.getTextureManager().registerTexture(this.dynamicTextureId, this.texture);
            this.uploadCanvas();
            this.nextFrameAt = System.currentTimeMillis() + firstDelay;
         } catch (Exception var8) {
            this.fail(var8);
         }
      }
   }

   private void openDecoder(boolean initializeCanvas) throws IOException {
      this.closeDecoder();
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
      if (!readers.hasNext()) {
         throw new IOException("No GIF reader is available");
      } else {
         this.reader = readers.next();
         this.imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(this.gifData));
         this.reader.setInput(this.imageInput, false, false);
         if (initializeCanvas) {
            this.frameCount = this.reader.getNumImages(true);
            if (this.frameCount == 0) {
               throw new IOException("GIF contains no frames");
            }

            int[] logicalSize = readLogicalSize(this.reader.getStreamMetadata());
            int width = logicalSize[0] > 0 ? logicalSize[0] : this.reader.getWidth(0);
            int height = logicalSize[1] > 0 ? logicalSize[1] : this.reader.getHeight(0);
            this.canvas = new BufferedImage(width, height, 2);
            this.pixelBuffer = new int[width * height];
         } else {
            this.canvas = new BufferedImage(this.canvas.getWidth(), this.canvas.getHeight(), 2);
         }

         this.frameIndex = -1;
         this.previousInfo = null;
         this.previousSnapshot = null;
      }
   }

   private int decodeNextFrame() throws IOException {
      if (this.frameIndex + 1 >= this.frameCount) {
         this.openDecoder(false);
      }

      if (this.previousInfo != null) {
         applyDisposal(this.canvas, this.previousInfo, this.previousSnapshot);
      }

      int nextFrame = this.frameIndex + 1;
      AnimatedGifTexture.FrameInfo info = readFrameInfo(this.reader.getImageMetadata(nextFrame));
      BufferedImage snapshot = info.disposal() == AnimatedGifTexture.Disposal.RESTORE_PREVIOUS ? copyImage(this.canvas) : null;
      BufferedImage rawFrame = this.reader.read(nextFrame);
      Graphics2D graphics = this.canvas.createGraphics();
      graphics.setComposite(AlphaComposite.SrcOver);
      graphics.drawImage(rawFrame, info.left(), info.top(), null);
      graphics.dispose();
      this.frameIndex = nextFrame;
      this.previousInfo = info;
      this.previousSnapshot = snapshot;
      if (this.texture != null) {
         this.uploadCanvas();
      }

      return info.delayMs();
   }

   private void updateFrame() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!client.isWindowFocused()) {
         this.pausedForFocusLoss = true;
      } else {
         long now = System.currentTimeMillis();
         if (this.pausedForFocusLoss) {
            this.pausedForFocusLoss = false;
            int resumeDelay = this.previousInfo == null ? 100 : this.previousInfo.delayMs();
            this.nextFrameAt = now + resumeDelay;
         } else if (now >= this.nextFrameAt) {
            if (now - this.nextFrameAt > 1000L) {
               this.nextFrameAt = now;
            }

            int advanced = 0;
            int lastDelay = 100;

            try {
               while (true) {
                  lastDelay = this.decodeNextFrame();
                  this.nextFrameAt += lastDelay;
                  if (now < this.nextFrameAt || ++advanced >= 5) {
                     if (now >= this.nextFrameAt) {
                        this.nextFrameAt = now + lastDelay;
                     }
                     break;
                  }
               }
            } catch (Exception var7) {
               this.fail(var7);
            }
         }
      }
   }

   private void uploadCanvas() {
      NativeImage image = this.texture.getImage();
      if (image != null) {
         this.canvas.getRGB(0, 0, this.canvas.getWidth(), this.canvas.getHeight(), this.pixelBuffer, 0, this.canvas.getWidth());

         for (int y = 0; y < this.canvas.getHeight(); y++) {
            int rowOffset = y * this.canvas.getWidth();

            for (int x = 0; x < this.canvas.getWidth(); x++) {
               int argb = this.pixelBuffer[rowOffset + x];
               int abgr = argb & -16711936 | (argb & 0xFF0000) >> 16 | (argb & 0xFF) << 16;
               image.setColor(x, y, abgr);
            }
         }

         this.texture.upload();
      }
   }

   private void fail(Exception exception) {
      this.animationFailed = true;
      this.closeDecoder();
      System.err.println("[Alien] Failed to load animated background " + this.gifResource + ": " + exception.getMessage());
   }

   private void closeDecoder() {
      if (this.reader != null) {
         this.reader.dispose();
         this.reader = null;
      }

      if (this.imageInput != null) {
         try {
            this.imageInput.close();
         } catch (IOException var2) {
         }

         this.imageInput = null;
      }
   }

   private static void applyDisposal(BufferedImage canvas, AnimatedGifTexture.FrameInfo info, BufferedImage snapshot) {
      Graphics2D graphics = canvas.createGraphics();
      if (info.disposal() == AnimatedGifTexture.Disposal.RESTORE_BACKGROUND) {
         graphics.setComposite(AlphaComposite.Clear);
         graphics.fillRect(info.left(), info.top(), info.width(), info.height());
      } else if (info.disposal() == AnimatedGifTexture.Disposal.RESTORE_PREVIOUS && snapshot != null) {
         graphics.setComposite(AlphaComposite.Src);
         graphics.drawImage(snapshot, 0, 0, null);
      }

      graphics.dispose();
   }

   private static BufferedImage copyImage(BufferedImage source) {
      BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), 2);
      Graphics2D graphics = copy.createGraphics();
      graphics.setComposite(AlphaComposite.Src);
      graphics.drawImage(source, 0, 0, null);
      graphics.dispose();
      return copy;
   }

   private static int[] readLogicalSize(IIOMetadata metadata) {
      if (metadata == null) {
         return new int[]{0, 0};
      } else {
         Node descriptor = findNode(metadata.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
         return descriptor == null
            ? new int[]{0, 0}
            : new int[]{intAttribute(descriptor, "logicalScreenWidth", 0), intAttribute(descriptor, "logicalScreenHeight", 0)};
      }
   }

   private static AnimatedGifTexture.FrameInfo readFrameInfo(IIOMetadata metadata) {
      Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
      Node descriptor = findNode(root, "ImageDescriptor");
      Node control = findNode(root, "GraphicControlExtension");
      int left = intAttribute(descriptor, "imageLeftPosition", 0);
      int top = intAttribute(descriptor, "imageTopPosition", 0);
      int width = intAttribute(descriptor, "imageWidth", 0);
      int height = intAttribute(descriptor, "imageHeight", 0);
      int delayCentiseconds = intAttribute(control, "delayTime", -1);
      int delay = delayCentiseconds < 0 ? 100 : Math.max(1, delayCentiseconds * 10);
      String disposalName = stringAttribute(control, "disposalMethod", "none");

      AnimatedGifTexture.Disposal disposal = switch (disposalName) {
         case "restoreToBackgroundColor" -> AnimatedGifTexture.Disposal.RESTORE_BACKGROUND;
         case "restoreToPrevious" -> AnimatedGifTexture.Disposal.RESTORE_PREVIOUS;
         default -> AnimatedGifTexture.Disposal.KEEP;
      };
      return new AnimatedGifTexture.FrameInfo(left, top, width, height, delay, disposal);
   }

   private static Node findNode(Node node, String name) {
      if (node == null) {
         return null;
      } else if (name.equals(node.getNodeName())) {
         return node;
      } else {
         for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node found = findNode(child, name);
            if (found != null) {
               return found;
            }
         }

         return null;
      }
   }

   private static int intAttribute(Node node, String name, int fallback) {
      try {
         return Integer.parseInt(stringAttribute(node, name, String.valueOf(fallback)));
      } catch (NumberFormatException var4) {
         return fallback;
      }
   }

   private static String stringAttribute(Node node, String name, String fallback) {
      if (node == null) {
         return fallback;
      } else {
         NamedNodeMap attributes = node.getAttributes();
         Node attribute = attributes == null ? null : attributes.getNamedItem(name);
         return attribute == null ? fallback : attribute.getNodeValue();
      }
   }

   private static enum Disposal {
      KEEP,
      RESTORE_BACKGROUND,
      RESTORE_PREVIOUS;
   }

   private record FrameInfo(int left, int top, int width, int height, int delayMs, AnimatedGifTexture.Disposal disposal) {
   }
}
