package dev.luminous.api.utils.novel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public final class FanqieLibrary {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static FanqieLibrary.State state;

   private FanqieLibrary() {
   }

   public static synchronized List<FanqieApi.Book> shelf() {
      load();
      return state.shelf.values().stream().map(FanqieLibrary.BookEntry::book).toList();
   }

   public static synchronized List<FanqieApi.Book> history() {
      load();
      List<FanqieApi.Book> result = new ArrayList<>();

      for (String id : state.history) {
         FanqieLibrary.BookEntry entry = state.known.get(id);
         if (entry != null) {
            result.add(entry.book());
         }
      }

      return result;
   }

   public static synchronized boolean onShelf(String id) {
      load();
      return state.shelf.containsKey(id);
   }

   public static synchronized void toggleShelf(FanqieApi.Book book) {
      load();
      remember(book);
      if (state.shelf.remove(book.id) == null) {
         state.shelf.put(book.id, FanqieLibrary.BookEntry.of(book));
      }

      save();
   }

   public static synchronized void visit(FanqieApi.Book book) {
      load();
      remember(book);
      state.history.remove(book.id);
      state.history.add(0, book.id);
      if (state.history.size() > 100) {
         state.history.subList(100, state.history.size()).clear();
      }

      save();
   }

   public static synchronized FanqieLibrary.Progress progress(String bookId) {
      load();
      return state.progress.get(bookId);
   }

   public static synchronized void progress(FanqieApi.Book book, String chapterId, int scroll) {
      if (book != null && chapterId != null && !chapterId.isBlank()) {
         load();
         remember(book);
         state.progress.put(book.id, new FanqieLibrary.Progress(chapterId, Math.max(0, scroll), System.currentTimeMillis()));
         save();
      }
   }

   private static void remember(FanqieApi.Book book) {
      FanqieLibrary.BookEntry entry = FanqieLibrary.BookEntry.of(book);
      state.known.put(book.id, entry);
      if (state.shelf.containsKey(book.id)) {
         state.shelf.put(book.id, entry);
      }
   }

   private static void load() {
      if (state == null) {
         Path file = file();

         try {
            state = Files.exists(file)
               ? (FanqieLibrary.State)GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), FanqieLibrary.State.class)
               : new FanqieLibrary.State();
         } catch (Exception var2) {
            state = new FanqieLibrary.State();
         }

         if (state == null) {
            state = new FanqieLibrary.State();
         }

         state.normalize();
      }
   }

   private static void save() {
      try {
         Path file = file();
         Files.createDirectories(file.getParent());
         Files.writeString(file, GSON.toJson(state), StandardCharsets.UTF_8);
      } catch (Exception var1) {
      }
   }

   private static Path file() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("Alien").resolve("fanqie_reader.json");
   }

   private static final class BookEntry {
      String id = "";
      String title = "";
      String author = "";
      String category = "";
      String description = "";

      static FanqieLibrary.BookEntry of(FanqieApi.Book book) {
         FanqieLibrary.BookEntry entry = new FanqieLibrary.BookEntry();
         entry.id = book.id;
         entry.title = book.title;
         entry.author = book.author;
         entry.category = book.category;
         entry.description = book.description;
         return entry;
      }

      FanqieApi.Book book() {
         return new FanqieApi.Book(this.id, this.title, this.author, this.category, this.description, Collections.emptyList());
      }
   }

   public static final class Progress {
      public String chapterId;
      public int scroll;
      public long updatedAt;

      public Progress() {
      }

      public Progress(String chapterId, int scroll, long updatedAt) {
         this.chapterId = chapterId;
         this.scroll = scroll;
         this.updatedAt = updatedAt;
      }
   }

   private static final class State {
      Map<String, FanqieLibrary.BookEntry> shelf = new LinkedHashMap<>();
      Map<String, FanqieLibrary.BookEntry> known = new LinkedHashMap<>();
      List<String> history = new ArrayList<>();
      Map<String, FanqieLibrary.Progress> progress = new LinkedHashMap<>();

      void normalize() {
         if (this.shelf == null) {
            this.shelf = new LinkedHashMap<>();
         }

         if (this.known == null) {
            this.known = new LinkedHashMap<>();
         }

         if (this.history == null) {
            this.history = new ArrayList<>();
         }

         if (this.progress == null) {
            this.progress = new LinkedHashMap<>();
         }

         this.known.putAll(this.shelf);
      }
   }
}
