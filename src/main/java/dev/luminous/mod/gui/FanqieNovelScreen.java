package dev.luminous.mod.gui;

import dev.luminous.api.utils.novel.FanqieApi;
import dev.luminous.api.utils.novel.FanqieLibrary;
import dev.luminous.api.utils.novel.NovelTtsEngine;
import dev.luminous.mod.gui.fonts.GlyphMap;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FanqieNovelScreen extends Screen {
   private static final int MARGIN = 18;
   private static final int HEADER_HEIGHT = 42;
   private static final int ROW_HEIGHT = 24;
   private static final int TEXT_LINE_HEIGHT = 12;
   private final MinecraftClient client = MinecraftClient.getInstance();
   private final String apiBase;
   private TextFieldWidget searchField;
   private List<FanqieApi.Book> books = Collections.emptyList();
   private FanqieApi.Book selectedBook;
   private FanqieApi.Chapter selectedChapter;
   private List<FanqieNovelScreen.ReaderLine> readerLines = Collections.emptyList();
   private final Map<String, FanqieNovelScreen.ImageAsset> imageTextures = new HashMap<>();
   private final Set<String> loadingImages = new HashSet<>();
   private String wrappedChapterId = "";
   private int wrappedWidth;
   private int bookScroll;
   private int chapterScroll;
   private int readerScroll;
   private int requestVersion;
   private boolean loading;
   private String status = "Loading books...";
   private FanqieNovelScreen.View view = FanqieNovelScreen.View.DISCOVER;
   private int pendingRestoreScroll = -1;
   private final NovelTtsEngine tts = new NovelTtsEngine();
   private boolean ttsStarted;
   private boolean autoNext = true;
   private boolean speakAfterLoad;
   private int ttsRate;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int booksX;
   private int booksW;
   private int chaptersX;
   private int chaptersW;
   private int readerX;
   private int readerW;
   private int listY;
   private int listH;

   public FanqieNovelScreen(String apiBase) {
      super(Text.literal("Novel Reader"));
      this.apiBase = apiBase;
   }

   protected void init() {
      this.panelX = 18;
      this.panelY = 18;
      this.panelW = Math.max(320, this.width - 36);
      this.panelH = Math.max(180, this.height - 36);
      this.booksW = Math.max(120, this.panelW / 4);
      this.chaptersW = Math.max(120, this.panelW / 4);
      this.readerW = this.panelW - this.booksW - this.chaptersW - 2;
      this.booksX = this.panelX;
      this.chaptersX = this.booksX + this.booksW + 1;
      this.readerX = this.chaptersX + this.chaptersW + 1;
      this.listY = this.panelY + 42;
      this.listH = this.panelH - 42;
      this.searchField = new TextFieldWidget(this.textRenderer, this.booksX + 8, this.panelY + 13, this.booksW - 52, 18, Text.literal("Search"));
      this.searchField.setMaxLength(100);
      this.searchField.setPlaceholder(Text.literal("Search books"));
      this.addDrawableChild(this.searchField);
      this.loadTopBooks();
   }

   private void loadTopBooks() {
      this.view = FanqieNovelScreen.View.DISCOVER;
      int version = ++this.requestVersion;
      this.loading = true;
      this.status = "Loading popular books...";
      FanqieApi.getTopBooks(this.apiBase).whenComplete((result, error) -> this.client.execute(() -> {
         if (version == this.requestVersion) {
            this.loading = false;
            if (error != null) {
               this.status = "Cannot connect to API: " + rootMessage(error);
               this.books = Collections.emptyList();
            } else {
               this.books = result == null ? Collections.emptyList() : result;
               this.bookScroll = 0;
               this.status = this.books.isEmpty() ? "No books returned by API." : this.books.size() + " books";
            }
         }
      }));
   }

   private void showShelf() {
      this.saveProgress();
      this.view = FanqieNovelScreen.View.SHELF;
      this.books = FanqieLibrary.shelf();
      this.bookScroll = 0;
      this.status = this.books.isEmpty() ? "Shelf is empty." : this.books.size() + " books on shelf";
   }

   private void showHistory() {
      this.saveProgress();
      this.view = FanqieNovelScreen.View.HISTORY;
      this.books = FanqieLibrary.history();
      this.bookScroll = 0;
      this.status = this.books.isEmpty() ? "No reading history." : this.books.size() + " recently read";
   }

   private void search() {
      String query = this.searchField.getText().trim();
      if (query.isEmpty()) {
         this.loadTopBooks();
      } else {
         int version = ++this.requestVersion;
         this.loading = true;
         this.status = "Searching...";
         FanqieApi.search(this.apiBase, query).whenComplete((result, error) -> this.client.execute(() -> {
            if (version == this.requestVersion) {
               this.loading = false;
               if (error != null) {
                  this.status = "Search failed: " + rootMessage(error);
               } else {
                  this.books = result == null ? Collections.emptyList() : result;
                  this.view = FanqieNovelScreen.View.SEARCH;
                  this.bookScroll = 0;
                  this.status = this.books.isEmpty() ? "No matching books." : this.books.size() + " results";
               }
            }
         }));
      }
   }

   private void openBook(FanqieApi.Book book) {
      this.tts.stop();
      this.ttsStarted = false;
      this.speakAfterLoad = false;
      this.saveProgress();
      int version = ++this.requestVersion;
      this.loading = true;
      this.status = "Loading " + book.title + "...";
      this.selectedBook = book;
      this.selectedChapter = null;
      this.readerLines = Collections.emptyList();
      this.chapterScroll = 0;
      this.readerScroll = 0;
      FanqieApi.getBook(this.apiBase, book.id).whenComplete((detail, error) -> this.client.execute(() -> {
         if (version == this.requestVersion) {
            this.loading = false;
            if (error != null) {
               this.status = "Book loading failed: " + rootMessage(error);
            } else {
               this.selectedBook = detail;
               FanqieLibrary.visit(detail);
               this.status = detail.chapters.isEmpty() ? "No chapters returned by API." : detail.chapters.size() + " chapters";
               if (!detail.chapters.isEmpty()) {
                  FanqieLibrary.Progress progress = FanqieLibrary.progress(detail.id);
                  FanqieApi.Chapter resume = detail.chapters.get(0);
                  int restore = 0;
                  if (progress != null) {
                     for (FanqieApi.Chapter candidate : detail.chapters) {
                        if (candidate.id.equals(progress.chapterId)) {
                           resume = candidate;
                           restore = progress.scroll;
                           break;
                        }
                     }
                  }

                  this.openChapter(resume, restore);
               }
            }
         }
      }));
   }

   private void openChapter(FanqieApi.Chapter chapter) {
      this.speakAfterLoad = false;
      this.openChapter(chapter, 0);
   }

   private void openChapter(FanqieApi.Chapter chapter, int restoreScroll) {
      this.tts.stop();
      this.ttsStarted = false;
      this.saveProgress();
      int version = ++this.requestVersion;
      this.loading = true;
      this.status = "Loading chapter...";
      this.selectedChapter = chapter;
      this.readerScroll = 0;
      this.readerLines = Collections.emptyList();
      this.pendingRestoreScroll = restoreScroll;
      FanqieApi.getChapter(this.apiBase, chapter.id)
         .whenComplete(
            (content, error) -> this.client
               .execute(
                  () -> {
                     if (version == this.requestVersion) {
                        this.loading = false;
                        if (error != null) {
                           this.status = "Chapter loading failed: " + rootMessage(error);
                        } else {
                           this.selectedChapter = new FanqieApi.Chapter(content.id, content.title, content.content, content.images, chapter.locked);
                           this.wrappedChapterId = "";
                           this.ensureWrappedContent(Math.max(20, this.readerW - 24));
                           this.readerScroll = clampScroll(this.pendingRestoreScroll, this.readerContentHeight(), this.listH);
                           this.pendingRestoreScroll = -1;
                           this.saveProgress();
                           int length = content.content == null ? 0 : content.content.length();
                           this.status = length == 0
                              ? "No readable chapter text returned."
                              : (
                                 chapter.locked && this.apiBase.contains("fanqienovel.com")
                                    ? "Official web preview - full text requires authorized access."
                                    : "Loaded " + length + " characters"
                              );
                           if (this.speakAfterLoad) {
                              this.speakAfterLoad = false;
                              this.startSpeech();
                           }
                        }
                     }
                  }
               )
         );
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -669968608);
      context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -216127178);
      context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 42, -14206903);
      context.fill(this.booksX + this.booksW, this.listY, this.booksX + this.booksW + 1, this.panelY + this.panelH, -12496280);
      context.fill(this.chaptersX + this.chaptersW, this.listY, this.chaptersX + this.chaptersW + 1, this.panelY + this.panelH, -12496280);
      context.drawTextWithShadow(this.textRenderer, "Library", this.booksX + 8, this.panelY + 4, -854536);
      this.drawTab(context, this.chaptersX + 8, this.panelY + 3, "Discover", this.view == FanqieNovelScreen.View.DISCOVER, mouseX, mouseY);
      this.drawTab(context, this.chaptersX + 66, this.panelY + 3, "Shelf", this.view == FanqieNovelScreen.View.SHELF, mouseX, mouseY);
      this.drawTab(context, this.chaptersX + 103, this.panelY + 3, "History", this.view == FanqieNovelScreen.View.HISTORY, mouseX, mouseY);
      context.drawTextWithShadow(this.textRenderer, "Chapters", this.chaptersX + 8, this.panelY + 17, -4732712);
      context.drawTextWithShadow(
         this.textRenderer,
         this.selectedBook == null ? "Reader" : this.truncate(this.selectedBook.title, this.readerW - 20),
         this.readerX + 10,
         this.panelY + 17,
         -854536
      );
      if (this.selectedBook != null) {
         this.drawButton(context, this.readerX + 8, this.panelY + 2, 44, 14, this.tts.isSpeaking() ? "Stop" : "Listen", mouseX, mouseY);
         this.drawButton(context, this.readerX + 56, this.panelY + 2, 30, 14, this.speedLabel(), mouseX, mouseY);
         this.drawButton(context, this.readerX + 90, this.panelY + 2, 34, 14, this.autoNext ? "Auto" : "Once", mouseX, mouseY);
         String shelfLabel = FanqieLibrary.onShelf(this.selectedBook.id) ? "On shelf" : "+ Shelf";
         this.drawButton(context, this.readerX + this.readerW - 58, this.panelY + 2, 50, 14, shelfLabel, mouseX, mouseY);
      }

      this.drawButton(context, this.booksX + this.booksW - 38, this.panelY + 13, 30, 18, "Go", mouseX, mouseY);
      this.renderBooks(context, mouseX, mouseY);
      this.renderChapters(context, mouseX, mouseY);
      this.renderReader(context);
      if (!this.status.isBlank()) {
         context.drawTextWithShadow(
            this.textRenderer, this.truncate(this.status, this.panelW - 18), this.panelX + 9, this.panelY + this.panelH - 12, this.loading ? -11136 : -4732712
         );
      }

      super.render(context, mouseX, mouseY, delta);
   }

   private void renderBooks(DrawContext context, int mouseX, int mouseY) {
      int start = this.bookScroll / 24;
      int offset = -(this.bookScroll % 24);

      for (int index = start; index < this.books.size(); index++) {
         int y = this.listY + offset + (index - start) * 24;
         if (y >= this.listY + this.listH - 14) {
            break;
         }

         FanqieApi.Book book = this.books.get(index);
         boolean selected = this.selectedBook != null && this.selectedBook.id.equals(book.id);
         boolean hover = inside(mouseX, mouseY, this.booksX, y, this.booksW, 24);
         if (selected || hover) {
            context.fill(this.booksX + 3, y + 1, this.booksX + this.booksW - 3, y + 24 - 1, selected ? -13546135 : -14140079);
         }

         context.drawText(this.textRenderer, this.truncate(book.title, this.booksW - 16), this.booksX + 8, y + 4, -1445642, false);
         String sub = book.author.isBlank() ? book.category : book.author;
         if (!sub.isBlank()) {
            context.drawText(this.textRenderer, this.truncate(sub, this.booksW - 16), this.booksX + 8, y + 14, -6442041, false);
         }
      }
   }

   private void renderChapters(DrawContext context, int mouseX, int mouseY) {
      if (this.selectedBook == null) {
         context.drawText(this.textRenderer, "Choose a book", this.chaptersX + 8, this.listY + 10, -7428680, false);
      } else {
         int start = this.chapterScroll / 24;
         int offset = -(this.chapterScroll % 24);

         for (int index = start; index < this.selectedBook.chapters.size(); index++) {
            int y = this.listY + offset + (index - start) * 24;
            if (y >= this.listY + this.listH - 14) {
               break;
            }

            FanqieApi.Chapter chapter = this.selectedBook.chapters.get(index);
            boolean selected = this.selectedChapter != null && this.selectedChapter.id.equals(chapter.id);
            boolean hover = inside(mouseX, mouseY, this.chaptersX, y, this.chaptersW, 24);
            if (selected || hover) {
               context.fill(this.chaptersX + 3, y + 1, this.chaptersX + this.chaptersW - 3, y + 24 - 1, selected ? -13546135 : -14140079);
            }

            String chapterLabel = chapter.locked && this.apiBase.contains("fanqienovel.com") ? "[Preview] " + chapter.title : chapter.title;
            context.drawText(this.textRenderer, this.truncate(chapterLabel, this.chaptersW - 16), this.chaptersX + 8, y + 7, -1445642, false);
         }
      }
   }

   private void renderReader(DrawContext context) {
      int textX = this.readerX + 12;
      int textY = this.listY + 10;
      int contentW = this.readerW - 24;
      if (this.selectedBook == null) {
         context.drawText(this.textRenderer, "Select a book to start reading.", textX, textY, -4732712, false);
      } else if (this.selectedChapter == null) {
         context.drawText(this.textRenderer, "Select a chapter.", textX, textY, -4732712, false);
      } else {
         context.drawTextWithShadow(this.textRenderer, this.truncate(this.selectedChapter.title, contentW), textX, textY, -722950);
         this.ensureWrappedContent(contentW);
         int y = textY + 20 - this.readerScroll;

         for (FanqieNovelScreen.ReaderLine line : this.readerLines) {
            if (y + line.height >= this.listY + 2 && y <= this.panelY + this.panelH - 18) {
               if (line.imageUrl != null) {
                  FanqieNovelScreen.ImageAsset image = this.imageTextures.get(line.imageUrl);
                  if (image != null) {
                     int drawW = image.drawWidth(contentW);
                     int drawH = image.drawHeight(contentW);
                     context.drawTexture(image.texture, textX + Math.max(0, (contentW - drawW) / 2), y, 0.0F, 0.0F, drawW, drawH, image.width, image.height);
                  } else {
                     context.drawText(this.textRenderer, "[Loading image...]", textX, y + 4, -7428680, false);
                  }
               } else {
                  context.drawText(this.textRenderer, line.text, textX, y, -2760471, false);
               }
            }

            y += line.height;
            if (y > this.panelY + this.panelH - 18) {
               break;
            }
         }
      }
   }

   private void ensureWrappedContent(int contentWidth) {
      if (!this.selectedChapter.id.equals(this.wrappedChapterId) || this.wrappedWidth != contentWidth) {
         int previousScroll = this.readerScroll;
         this.wrappedChapterId = this.selectedChapter.id;
         this.wrappedWidth = contentWidth;
         List<FanqieNovelScreen.ReaderLine> lines = new ArrayList<>();
         String content = this.selectedChapter.content == null ? "" : this.selectedChapter.content.replace("\\r", "");

         for (String paragraph : content.split("\\n", -1)) {
            if (paragraph.matches("\\[\\[IMAGE:\\d+]]")) {
               int imageIndex = Integer.parseInt(paragraph.substring(8, paragraph.length() - 2));
               if (imageIndex >= 0 && imageIndex < this.selectedChapter.images.size()) {
                  String url = this.selectedChapter.images.get(imageIndex);
                  this.loadImage(url);
                  FanqieNovelScreen.ImageAsset image = this.imageTextures.get(url);
                  lines.add(FanqieNovelScreen.ReaderLine.image(url, image == null ? 24 : image.drawHeight(contentWidth) + 4));
               }
            } else if (paragraph.isBlank()) {
               lines.add(FanqieNovelScreen.ReaderLine.text(OrderedText.EMPTY));
            } else {
               for (OrderedText wrapped : this.textRenderer.wrapLines(Text.literal(paragraph), contentWidth)) {
                  lines.add(FanqieNovelScreen.ReaderLine.text(wrapped));
               }
            }
         }

         this.readerLines = lines.isEmpty()
            ? Collections.singletonList(FanqieNovelScreen.ReaderLine.text(Text.literal("No readable content returned by API.").asOrderedText()))
            : lines;
         this.readerScroll = clampScroll(previousScroll, this.readerContentHeight(), this.listH);
      }
   }

   private void loadImage(String url) {
      if (url != null && !url.isBlank() && !this.imageTextures.containsKey(url) && this.loadingImages.add(url)) {
         CompletableFuture.runAsync(() -> {
            try {
               HttpURLConnection connection = (HttpURLConnection)new URI(url).toURL().openConnection();
               connection.setConnectTimeout(6000);
               connection.setReadTimeout(15000);
               connection.setRequestProperty("User-Agent", "Mozilla/5.0");
               connection.setRequestProperty("Referer", "https://fanqienovel.com/");
               BufferedImage image = ImageIO.read(connection.getInputStream());
               connection.disconnect();
               if (image != null) {
                  Identifier texture = Identifier.of("xigua", "fanqie/" + Integer.toUnsignedString(url.hashCode()));
                  GlyphMap.registerBufferedImageTexture(texture, image);
                  this.client.execute(() -> {
                     this.imageTextures.put(url, new FanqieNovelScreen.ImageAsset(texture, image.getWidth(), image.getHeight()));
                     this.wrappedChapterId = "";
                  });
                  return;
               }
            } catch (Exception var8) {
               return;
            } finally {
               this.loadingImages.remove(url);
            }
         });
      }
   }

   private int readerContentHeight() {
      int height = 20;

      for (FanqieNovelScreen.ReaderLine line : this.readerLines) {
         height += line.height;
      }

      return height;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         int x = (int)mouseX;
         int y = (int)mouseY;
         if (inside(x, y, this.chaptersX + 5, this.panelY + 2, 57, 12)) {
            this.loadTopBooks();
            return true;
         } else if (inside(x, y, this.chaptersX + 63, this.panelY + 2, 37, 12)) {
            this.showShelf();
            return true;
         } else if (inside(x, y, this.chaptersX + 100, this.panelY + 2, 48, 12)) {
            this.showHistory();
            return true;
         } else if (this.selectedBook != null && inside(x, y, this.readerX + 8, this.panelY + 2, 44, 14)) {
            if (this.tts.isSpeaking()) {
               this.tts.stop();
               this.ttsStarted = false;
               this.status = "Listening stopped.";
            } else {
               this.startSpeech();
            }

            return true;
         } else if (this.selectedBook != null && inside(x, y, this.readerX + 56, this.panelY + 2, 30, 14)) {
            this.ttsRate = this.ttsRate >= 4 ? -2 : this.ttsRate + 1;
            this.tts.setRate(this.ttsRate);
            this.status = "Speech speed: " + this.speedLabel();
            if (this.tts.isSpeaking()) {
               this.startSpeech();
            }

            return true;
         } else if (this.selectedBook != null && inside(x, y, this.readerX + 90, this.panelY + 2, 34, 14)) {
            this.autoNext = !this.autoNext;
            this.status = this.autoNext ? "Auto next chapter enabled." : "Single chapter listening.";
            return true;
         } else if (this.selectedBook != null && inside(x, y, this.readerX + this.readerW - 58, this.panelY + 2, 50, 14)) {
            FanqieLibrary.toggleShelf(this.selectedBook);
            if (this.view == FanqieNovelScreen.View.SHELF) {
               this.showShelf();
            }

            this.status = FanqieLibrary.onShelf(this.selectedBook.id) ? "Added to shelf." : "Removed from shelf.";
            return true;
         } else if (inside(x, y, this.booksX + 8, this.panelY + 13, this.booksW - 52, 18)) {
            return super.mouseClicked(mouseX, mouseY, button);
         } else if (inside(x, y, this.booksX + this.booksW - 38, this.panelY + 13, 30, 18)) {
            this.search();
            return true;
         } else if (inside(x, y, this.booksX, this.listY, this.booksW, this.listH)) {
            int index = (y - this.listY + this.bookScroll) / 24;
            if (index >= 0 && index < this.books.size()) {
               this.openBook(this.books.get(index));
            }

            return true;
         } else if (this.selectedBook != null && inside(x, y, this.chaptersX, this.listY, this.chaptersW, this.listH)) {
            int index = (y - this.listY + this.chapterScroll) / 24;
            if (index >= 0 && index < this.selectedBook.chapters.size()) {
               this.openChapter(this.selectedBook.chapters.get(index));
            }

            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int amount = (int)(-verticalAmount * 30.0);
      if (inside(mouseX, mouseY, this.booksX, this.listY, this.booksW, this.listH)) {
         this.bookScroll = clampScroll(this.bookScroll + amount, this.books.size() * 24, this.listH);
         return true;
      } else if (inside(mouseX, mouseY, this.chaptersX, this.listY, this.chaptersW, this.listH) && this.selectedBook != null) {
         this.chapterScroll = clampScroll(this.chapterScroll + amount, this.selectedBook.chapters.size() * 24, this.listH);
         return true;
      } else if (inside(mouseX, mouseY, this.readerX, this.listY, this.readerW, this.listH)) {
         this.readerScroll = clampScroll(this.readerScroll + amount, this.readerContentHeight(), this.listH);
         this.saveProgress();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.close();
         return true;
      } else if ((keyCode == 257 || keyCode == 335) && this.searchField.isFocused()) {
         this.search();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      this.saveProgress();
      this.tts.stop();
      super.close();
   }

   public void tick() {
      super.tick();
      if (this.ttsStarted && !this.tts.isSpeaking()) {
         this.ttsStarted = false;
         if (this.autoNext) {
            this.playNextChapter();
         }
      }
   }

   private void startSpeech() {
      if (this.selectedChapter != null && this.selectedChapter.content != null && !this.selectedChapter.content.isBlank()) {
         this.tts.setRate(this.ttsRate);
         String speech = this.selectedChapter.content.replaceAll("\\[\\[IMAGE:\\d+]]", " ").replaceAll("\\s+", " ").trim();
         this.ttsStarted = this.tts.speak(this.selectedChapter.title + "\u3002" + speech);
         this.status = this.ttsStarted ? "Listening: " + this.selectedChapter.title : "Listening failed: " + this.tts.error();
      } else {
         this.status = "Load a chapter before listening.";
      }
   }

   private void playNextChapter() {
      if (this.selectedBook != null && this.selectedChapter != null) {
         for (int i = 0; i < this.selectedBook.chapters.size() - 1; i++) {
            if (this.selectedBook.chapters.get(i).id.equals(this.selectedChapter.id)) {
               this.speakAfterLoad = true;
               this.openChapter(this.selectedBook.chapters.get(i + 1), 0);
               return;
            }
         }

         this.status = "Reached the last chapter.";
      }
   }

   private String speedLabel() {
      return String.format(Locale.ROOT, "%.1fx", 1.0 + this.ttsRate * 0.1);
   }

   private void saveProgress() {
      if (this.selectedBook != null && this.selectedChapter != null) {
         FanqieLibrary.progress(this.selectedBook, this.selectedChapter.id, this.readerScroll);
      }
   }

   private void drawButton(DrawContext context, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
      context.fill(x, y, x + w, y + h, inside(mouseX, mouseY, x, y, w, h) ? -10844249 : -12293241);
      int textX = x + (w - this.textRenderer.getWidth(label)) / 2;
      context.drawTextWithShadow(this.textRenderer, label, textX, y + 5, -1);
   }

   private void drawTab(DrawContext context, int x, int y, String label, boolean active, int mouseX, int mouseY) {
      int width = this.textRenderer.getWidth(label) + 6;
      if (active || inside(mouseX, mouseY, x - 3, y - 1, width, 12)) {
         context.fill(x - 3, y - 1, x + width - 3, y + 10, active ? -12293241 : -13349533);
      }

      context.drawText(this.textRenderer, label, x, y, active ? -1 : -6442041, false);
   }

   private String truncate(String text, int maxWidth) {
      if (this.textRenderer.getWidth(text) <= maxWidth) {
         return text;
      } else {
         String suffix = "...";
         int end = text.length();

         while (end > 0 && this.textRenderer.getWidth(text.substring(0, end) + suffix) > maxWidth) {
            end--;
         }

         return text.substring(0, end) + suffix;
      }
   }

   private static boolean inside(double x, double y, int boxX, int boxY, int boxW, int boxH) {
      return x >= boxX && x < boxX + boxW && y >= boxY && y < boxY + boxH;
   }

   private static int clampScroll(int scroll, int contentHeight, int viewportHeight) {
      return Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewportHeight)));
   }

   private static String rootMessage(Throwable error) {
      Throwable current = error;

      while (current.getCause() != null) {
         current = current.getCause();
      }

      return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
   }

   private static final class ImageAsset {
      final Identifier texture;
      final int width;
      final int height;

      ImageAsset(Identifier texture, int width, int height) {
         this.texture = texture;
         this.width = Math.max(1, width);
         this.height = Math.max(1, height);
      }

      int drawWidth(int available) {
         return this.width <= 64 && this.height <= 64 ? Math.min(32, this.width) : Math.min(available, this.width);
      }

      int drawHeight(int available) {
         int drawWidth = this.drawWidth(available);
         return Math.max(1, Math.min(180, (int)Math.round((double)this.height * drawWidth / this.width)));
      }
   }

   private static final class ReaderLine {
      final OrderedText text;
      final String imageUrl;
      final int height;

      private ReaderLine(OrderedText text, String imageUrl, int height) {
         this.text = text;
         this.imageUrl = imageUrl;
         this.height = height;
      }

      static FanqieNovelScreen.ReaderLine text(OrderedText text) {
         return new FanqieNovelScreen.ReaderLine(text, null, 12);
      }

      static FanqieNovelScreen.ReaderLine image(String url, int height) {
         return new FanqieNovelScreen.ReaderLine(OrderedText.EMPTY, url, height);
      }
   }

   private static enum View {
      DISCOVER,
      SEARCH,
      SHELF,
      HISTORY;
   }
}
