package dev.luminous.api.utils.novel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FanqieApi {
   private static volatile List<FanqieApi.Book> lastTopBooks = List.of();

   private FanqieApi() {
   }

   public static CompletableFuture<List<FanqieApi.Book>> getTopBooks(String base) {
      return CompletableFuture.supplyAsync(() -> {
         if (direct(base)) {
            return getCatalogueNow(base);
         } else {
            JsonObject root = json(base, direct(base) ? "/api/author/misc/top_book_list/v1/?limit=30&offset=0" : "/api/books/top?limit=30&offset=0");
            JsonArray list = array(root, "book_list");
            if (list == null) {
               list = array(payload(root), "book_list");
            }

            List<FanqieApi.Book> result = list == null ? List.of() : books(list);
            if (direct(base) && !result.isEmpty()) {
               lastTopBooks = result;
            }

            return result;
         }
      });
   }

   public static CompletableFuture<List<FanqieApi.Book>> search(String base, String query) {
      return CompletableFuture.supplyAsync(
         () -> {
            if (!direct(base)) {
               JsonObject root;
               try {
                  root = json(base, "/api/search?q=" + encode(query));
               } catch (Exception var8) {
                  root = json(base, "/search?query=" + encode(query) + "&page=1");
               }

               JsonArray list = array(root, "book_list");
               if (list == null) {
                  list = array(root, "data");
               }

               if (list == null) {
                  list = array(payload(root), "book_list");
               }

               if (list == null) {
                  list = findBookArray(root);
               }

               return list == null ? List.of() : books(list);
            } else if (query.matches("\\d{10,}")) {
               return List.of(directBook(base, query));
            } else {
               LinkedHashMap<String, FanqieApi.Book> online = new LinkedHashMap<>();

               try {
                  for (int offset = 0; offset < 30; offset += 10) {
                     JsonObject rootx = json(
                        "https://novel.snssdk.com",
                        "/api/novel/channel/homepage/search/search/v1/?device_platform=android&aid=1967&offset=" + offset + "&q=" + encode(query)
                     );
                     JsonArray listx = array(payload(rootx), "ret_data");
                     if (listx == null || listx.isEmpty()) {
                        break;
                     }

                     for (FanqieApi.Book book : books(listx)) {
                        online.putIfAbsent(book.id, book);
                     }
                  }
               } catch (Exception var9) {
               }

               if (!online.isEmpty()) {
                  return List.copyOf(online.values());
               } else {
                  List<FanqieApi.Book> source = lastTopBooks;
                  if (source.isEmpty()) {
                     source = getCatalogueNow(base);
                  }

                  String needle = query.toLowerCase(Locale.ROOT);
                  return source.stream()
                     .filter(bookx -> bookx.title.toLowerCase(Locale.ROOT).contains(needle) || bookx.author.toLowerCase(Locale.ROOT).contains(needle))
                     .toList();
               }
            }
         }
      );
   }

   private static List<FanqieApi.Book> getCatalogueNow(String base) {
      LinkedHashMap<String, FanqieApi.Book> result = new LinkedHashMap<>();
      addBooks(result, json(base, "/api/author/misc/top_book_list/v1/?limit=100&offset=0"));
      addBooks(result, json(base, "/api/rank/recommend/list?type=2&limit=100&offset=0"));
      addBooks(result, json(base, "/api/rank/recommend/list?type=3&limit=100&offset=0"));
      addBooks(result, json(base, "/api/rank/recent/update/list?limit=100&offset=0"));
      List<FanqieApi.Book> list = List.copyOf(result.values());
      if (!list.isEmpty()) {
         lastTopBooks = list;
      }

      return list;
   }

   private static void addBooks(Map<String, FanqieApi.Book> output, JsonObject root) {
      JsonArray list = findBookArray(root);
      if (list != null) {
         for (FanqieApi.Book book : books(list)) {
            output.putIfAbsent(book.id, book);
         }
      }
   }

   private static JsonArray findBookArray(JsonObject root) {
      if (root == null) {
         return null;
      } else {
         for (String key : new String[]{"book_list", "list", "search_book_data_list"}) {
            JsonArray found = array(root, key);
            if (found != null) {
               return found;
            }
         }

         JsonElement data = root.get("data");
         if (data != null && data.isJsonArray()) {
            return data.getAsJsonArray();
         } else if (data != null && data.isJsonObject()) {
            return findBookArray(data.getAsJsonObject());
         } else {
            for (JsonElement value : root.asMap().values()) {
               if (value.isJsonObject()) {
                  JsonArray nested = findBookArray(value.getAsJsonObject());
                  if (nested != null) {
                     return nested;
                  }
               } else if (value.isJsonArray() && looksLikeBooks(value.getAsJsonArray())) {
                  return value.getAsJsonArray();
               }
            }

            return null;
         }
      }
   }

   private static boolean looksLikeBooks(JsonArray array) {
      for (JsonElement value : array) {
         if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if ((object.has("book_id") || object.has("bookId")) && (object.has("title") || object.has("book_name") || object.has("bookName"))) {
               return true;
            }
         }
      }

      return false;
   }

   private static JsonArray findChapterArray(JsonObject root) {
      if (root == null) {
         return null;
      } else {
         for (String key : new String[]{"chapters", "chapterList", "chapter_list", "catalog", "itemList", "item_list", "items", "list"}) {
            JsonArray found = array(root, key);
            if (found != null && looksLikeChapters(found)) {
               return found;
            }
         }

         for (JsonElement value : root.asMap().values()) {
            if (value.isJsonObject()) {
               JsonArray nested = findChapterArray(value.getAsJsonObject());
               if (nested != null) {
                  return nested;
               }
            } else if (value.isJsonArray()) {
               JsonArray candidate = value.getAsJsonArray();
               if (looksLikeChapters(candidate)) {
                  return candidate;
               }

               for (JsonElement nested : candidate) {
                  if (nested.isJsonObject()) {
                     JsonArray found = findChapterArray(nested.getAsJsonObject());
                     if (found != null) {
                        return found;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private static boolean looksLikeChapters(JsonArray array) {
      for (JsonElement value : array) {
         if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("itemId") || object.has("item_id") || object.has("chapterId") || object.has("chapter_id")) {
               return true;
            }
         }
      }

      return false;
   }

   private static JsonObject findObject(JsonObject root, String... keys) {
      if (root == null) {
         return null;
      } else {
         for (String key : keys) {
            if (root.has(key)) {
               return root;
            }
         }

         for (JsonElement value : root.asMap().values()) {
            if (value.isJsonObject()) {
               JsonObject found = findObject(value.getAsJsonObject(), keys);
               if (found != null) {
                  return found;
               }
            } else if (value.isJsonArray()) {
               for (JsonElement nested : value.getAsJsonArray()) {
                  if (nested.isJsonObject()) {
                     JsonObject found = findObject(nested.getAsJsonObject(), keys);
                     if (found != null) {
                        return found;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private static String recursiveString(JsonObject root, String... keys) {
      String direct = str(root, keys);
      if (!direct.isBlank()) {
         return direct;
      } else {
         JsonObject found = findObject(root, keys);
         return found == null ? "" : str(found, keys);
      }
   }

   public static CompletableFuture<FanqieApi.Book> getBook(String base, String id) {
      return CompletableFuture.supplyAsync(
         () -> {
            if (direct(base)) {
               return directBook(base, id);
            } else {
               JsonArray chapterList = null;

               JsonObject root;
               try {
                  root = payload(json(base, "/api/book/" + encode(id)));
                  chapterList = array(root, "chapters");
               } catch (Exception var7) {
                  JsonObject infoRoot = json(base, "/info?book_id=" + encode(id));
                  root = findObject(infoRoot, "bookId", "book_id", "bookName", "book_name", "title");
                  if (root == null) {
                     root = payload(infoRoot);
                  }

                  JsonObject catalog = json(base, "/catalog?book_id=" + encode(id));
                  chapterList = findChapterArray(catalog);
               }

               String bookId = str(root, "book_id", "bookId", "id");
               return new FanqieApi.Book(
                  bookId.isBlank() ? id : bookId,
                  str(root, "title", "book_name", "bookName", "name"),
                  str(root, "author", "author_name", "authorName"),
                  str(root, "category", "category_name", "categoryName"),
                  str(root, "abstract", "description"),
                  chapters(chapterList)
               );
            }
         }
      );
   }

   public static CompletableFuture<FanqieApi.Chapter> getChapter(String base, String id) {
      return CompletableFuture.supplyAsync(() -> {
         if (direct(base)) {
            try {
               JsonObject root = payload(json(base, "/api/reader/full?itemId=" + encode(id)));
               if (root.has("chapterData") && root.get("chapterData").isJsonObject()) {
                  root = root.getAsJsonObject("chapterData");
               }

               String content = str(root, "content", "text");
               if (!content.isBlank()) {
                  return new FanqieApi.Chapter(id, str(root, "chapterTitle", "title"), decrypt(content));
               }
            } catch (Exception var6) {
            }

            return directChapter(base, id);
         } else {
            JsonObject rootx;
            try {
               rootx = payload(json(base, "/api/chapter/" + encode(id)));
            } catch (Exception var7) {
               JsonObject contentRoot = json(base, "/content?item_id=" + encode(id));
               rootx = findObject(contentRoot, "content", "chapterContent", "chapter_content", "text");
               if (rootx == null) {
                  rootx = payload(contentRoot);
               }
            }

            String chapterId = str(rootx, "chapter_id", "itemId", "item_id", "id");
            String content = recursiveString(rootx, "content", "chapterContent", "chapter_content", "text");
            String title = recursiveString(rootx, "title", "chapterTitle", "chapter_name");
            return new FanqieApi.Chapter(chapterId.isBlank() ? id : chapterId, title, content);
         }
      });
   }

   private static FanqieApi.Chapter directChapter(String base, String id) {
      String html = text(base, "/reader/" + encode(id));
      String title = "";
      Matcher heading = Pattern.compile("<h1[^>]*class=[\"'][^\"']*muye-reader-title[^\"']*[\"'][^>]*>(.*?)</h1>", 32).matcher(html);
      if (heading.find()) {
         title = clean(heading.group(1)).trim();
      }

      StringBuilder content = new StringBuilder();
      Matcher bodyStart = Pattern.compile("<div[^>]*class=[\"'][^\"']*muye-reader-content(?:\\s+[^\"']*)?[\"'][^>]*>", 2).matcher(html);
      List<String> images = new ArrayList<>();
      if (bodyStart.find()) {
         int end = html.indexOf("<div class=\"muye-to-fanqie\"", bodyStart.end());
         if (end < 0) {
            end = html.indexOf("<script", bodyStart.end());
         }

         if (end < 0) {
            end = html.length();
         }

         String bodyHtml = html.substring(bodyStart.end(), end);
         Matcher paragraphs = Pattern.compile("<p[^>]*>(.*?)</p>", 34).matcher(bodyHtml);

         while (paragraphs.find()) {
            String paragraph = decrypt(richText(paragraphs.group(1), images).trim());
            if (!paragraph.isBlank()) {
               content.append(paragraph).append('\n');
            }
         }
      }

      if (content.isEmpty()) {
         Matcher description = Pattern.compile("<meta[^>]+name=[\"']description[\"'][^>]+content=[\"'](.*?)[\"']", 34).matcher(html);
         if (description.find()) {
            content.append(decrypt(clean(description.group(1)).trim()));
         }
      }

      if (content.isEmpty()) {
         throw new IllegalStateException("Chapter page did not contain readable text");
      } else {
         return new FanqieApi.Chapter(id, title, content.toString(), images);
      }
   }

   private static FanqieApi.Book directBook(String base, String id) {
      String title = "";
      String author = "";
      String description = "";
      String category = "";

      try {
         JsonObject info = payload(json(base, "/api/book/info?bookId=" + encode(id)));
         title = str(info, "bookName", "book_name", "name", "title");
         author = str(info, "authorName", "author_name", "author");
         description = str(info, "abstract", "description");
         category = str(info, "category", "categoryName");
      } catch (Exception var18) {
      }

      String html = text(base, "/page/" + encode(id));
      Matcher ld = Pattern.compile("<script[^>]*type=[\\\"']application/ld\\+json[\\\"'][^>]*>(.*?)</script>", 32).matcher(html);
      if (ld.find()) {
         try {
            JsonObject data = JsonParser.parseString(ld.group(1)).getAsJsonObject();
            if (title.isBlank()) {
               title = str(data, "name");
            }

            if (description.isBlank()) {
               description = str(data, "description");
            }

            JsonElement a = data.get("author");
            if (author.isBlank() && a != null && a.isJsonArray() && !a.getAsJsonArray().isEmpty()) {
               author = str(a.getAsJsonArray().get(0).getAsJsonObject(), "name");
            } else if (author.isBlank() && a != null && a.isJsonObject()) {
               author = str(a.getAsJsonObject(), "name");
            } else if (author.isBlank() && a != null && a.isJsonPrimitive()) {
               author = a.getAsString();
            }
         } catch (Exception var20) {
         }
      }

      if (title.isBlank()) {
         Matcher t = Pattern.compile("<title>(.*?)</title>", 32).matcher(html);
         if (t.find()) {
            title = clean(t.group(1)).split("_")[0].trim();
         }
      }

      List<FanqieApi.Chapter> chapters = new ArrayList<>();

      try {
         JsonObject directory = payload(json(base, "/api/reader/directory/detail?bookId=" + encode(id)));
         JsonArray volumes = array(directory, "chapterListWithVolume");
         if (volumes != null) {
            for (JsonElement volume : volumes) {
               if (volume.isJsonArray()) {
                  for (JsonElement entry : volume.getAsJsonArray()) {
                     if (entry.isJsonObject()) {
                        JsonObject chapter = entry.getAsJsonObject();
                        String chapterId = str(chapter, "itemId", "item_id", "chapter_id", "id");
                        boolean locked = bool(chapter, "isChapterLock", "is_chapter_lock", "needPay");
                        if (!chapterId.isBlank()) {
                           chapters.add(new FanqieApi.Chapter(chapterId, str(chapter, "title", "chapter_name"), "", List.of(), locked));
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var19) {
      }

      if (chapters.isEmpty()) {
         Set<String> seen = new HashSet<>();
         Matcher links = Pattern.compile("href=[\\\"']/reader/(\\d+)[\\\"'][^>]*>(.*?)</a>", 32).matcher(html);

         while (links.find()) {
            String chapterId = links.group(1);
            String chapterTitle = clean(links.group(2)).trim();
            if (!chapterTitle.isBlank() && seen.add(chapterId)) {
               chapters.add(new FanqieApi.Chapter(chapterId, chapterTitle, ""));
            }
         }
      }

      return new FanqieApi.Book(id, title, author, category, clean(description), chapters);
   }

   private static List<FanqieApi.Book> books(JsonArray input) {
      List<FanqieApi.Book> result = new ArrayList<>();

      for (JsonElement e : input) {
         if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            String id = str(o, "book_id", "bookId", "id");
            if (!id.isBlank()) {
               result.add(
                  new FanqieApi.Book(
                     id,
                     str(o, "book_name", "bookName", "title", "name"),
                     str(o, "author", "author_name", "authorName"),
                     str(o, "category", "category_name", "categoryName"),
                     str(o, "abstract", "description", "book_abstract"),
                     List.of()
                  )
               );
            }
         }
      }

      return result;
   }

   private static List<FanqieApi.Chapter> chapters(JsonArray input) {
      if (input == null) {
         return List.of();
      } else {
         List<FanqieApi.Chapter> result = new ArrayList<>();

         for (JsonElement e : input) {
            if (e.isJsonObject()) {
               JsonObject o = e.getAsJsonObject();
               String id = str(o, "chapter_id", "chapterId", "item_id", "itemId", "id");
               if (!id.isBlank()) {
                  result.add(new FanqieApi.Chapter(id, str(o, "title", "chapter_name", "chapterTitle"), ""));
               }
            }
         }

         return result;
      }
   }

   private static JsonObject json(String base, String path) {
      JsonElement parsed = JsonParser.parseString(text(base, path));
      if (!parsed.isJsonObject()) {
         throw new IllegalStateException("API\u8fd4\u56de\u683c\u5f0f\u9519\u8bef");
      } else {
         return parsed.getAsJsonObject();
      }
   }

   private static String text(String base, String path) {
      try {
         String root = base.trim().replaceAll("/+$", "");
         HttpURLConnection c = (HttpURLConnection)new URI(root + path).toURL().openConnection();
         c.setConnectTimeout(6000);
         c.setReadTimeout(15000);
         c.setRequestProperty("Accept", "application/json,text/html");
         c.setRequestProperty("Accept-Encoding", "identity");
         c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
         c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
         if (direct(base)) {
            c.setRequestProperty("Referer", "https://fanqienovel.com/");
            c.setRequestProperty("Cookie", "novel_web_id=" + Long.toUnsignedString(new Random().nextLong()));
         }

         int status = c.getResponseCode();
         if (status >= 200 && status < 300) {
            String var7;
            try (InputStream in = c.getInputStream()) {
               String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
               if (body.isBlank()) {
                  throw new IOException("API returned an empty response");
               }

               var7 = body;
            } finally {
               c.disconnect();
            }

            return var7;
         } else {
            throw new IOException("HTTP " + status);
         }
      } catch (Exception var16) {
         throw new IllegalStateException(var16.getMessage(), var16);
      }
   }

   private static boolean direct(String base) {
      return base != null && base.contains("fanqienovel.com");
   }

   private static String encode(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
   }

   private static JsonObject payload(JsonObject o) {
      return o.has("data") && o.get("data").isJsonObject() ? o.getAsJsonObject("data") : o;
   }

   private static JsonArray array(JsonObject o, String key) {
      return o != null && o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : null;
   }

   private static String str(JsonObject o, String... keys) {
      for (String key : keys) {
         if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
            try {
               return o.get(key).getAsString();
            } catch (Exception var7) {
            }
         }
      }

      return "";
   }

   private static boolean bool(JsonObject o, String... keys) {
      for (String key : keys) {
         if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
            try {
               JsonElement value = o.get(key);
               if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                  return value.getAsBoolean();
               }

               return value.getAsInt() != 0;
            } catch (Exception var7) {
            }
         }
      }

      return false;
   }

   private static String clean(String s) {
      return s.replaceAll("<[^>]+>", "")
         .replace("&nbsp;", " ")
         .replace("&amp;", "&")
         .replace("&quot;", String.valueOf('"'))
         .replace("&#x27;", "'")
         .replace("&lt;", "<")
         .replace("&gt;", ">");
   }

   private static String richText(String html, List<String> images) {
      StringBuilder output = new StringBuilder();
      Matcher tags = Pattern.compile("<img\\b[^>]*>|<br\\s*/?>", 2).matcher(html);

      int cursor;
      for (cursor = 0; tags.find(); cursor = tags.end()) {
         output.append(clean(html.substring(cursor, tags.start())));
         String tag = tags.group();
         if (tag.regionMatches(true, 0, "<br", 0, 3)) {
            output.append('\n');
         } else {
            Matcher source = Pattern.compile("(?:data-src|src)\\s*=\\s*[\"']([^\"']+)[\"']", 2).matcher(tag);
            Matcher alt = Pattern.compile("alt\\s*=\\s*[\"']([^\"']*)[\"']", 2).matcher(tag);
            String altText = alt.find() ? clean(alt.group(1)).trim() : "";
            if (source.find()) {
               String url = clean(source.group(1)).replace("&amp;", "&");
               int index = images.size();
               images.add(url.startsWith("//") ? "https:" + url : url);
               output.append('\n').append("[[IMAGE:").append(index).append("]]\n");
            } else if (!altText.isBlank()) {
               output.append(altText);
            }
         }
      }

      output.append(clean(html.substring(cursor)));
      return output.toString()
         .replaceAll("(?i)&lt;/?p&gt;", "\n")
         .replaceAll("(?i)</?p(?:\\s+[^>]*)?>", "\n")
         .replaceAll("(?i)(?:^|\\s)/?p/(?:\\s|$)", "\n")
         .replace("/p/", "\n")
         .replaceAll("\\n{3,}", "\n\n");
   }

   private static String decrypt(String text) {
      String codes = "58344,58345,58346,58347,58348,58349,58350,58351,58352,58353,58354,58355,58356,58357,58359,58360,58361,58362,58363,58364,58365,58368,58369,58370,58371,58372,58373,58374,58375,58376,58377,58378,58379,58380,58381,58382,58383,58384,58385,58386,58387,58388,58389,58390,58391,58392,58394,58395,58396,58397,58398,58399,58400,58401,58402,58403,58404,58405,58406,58407,58408,58409,58410,58411,58412,58413,58414,58415,58416,58417,58418,58419,58420,58421,58422,58423,58424,58425,58426,58427,58428,58429,58430,58431,58432,58433,58434,58435,58436,58437,58438,58439,58440,58441,58442,58443,58444,58445,58446,58447,58448,58449,58450,58451,58452,58453,58454,58455,58456,58457,58458,58460,58461,58462,58463,58464,58465,58466,58467,58468,58469,58470,58471,58472,58473,58474,58475,58476,58477,58478,58479,58480,58481,58482,58483,58484,58485,58486,58487,58488,58489,58490,58491,58492,58493,58494,58495,58496,58497,58498,58499,58500,58501,58502,58503,58504,58505,58506,58507,58508,58509,58510,58511,58512,58513,58514,58515,58516,58517,58518,58519,58520,58521,58522,58523,58524,58525,58526,58527,58528,58529,58530,58531,58532,58533,58534,58535,58536,58537,58538,58539,58540,58541,58542,58543,58544,58545,58546,58547,58548,58549,58551,58552,58553,58554,58555,58556,58557,58558,58559,58560,58561,58562,58563,58564,58565,58566,58567,58568,58569,58570,58571,58572,58573,58574,58575,58576,58577,58578,58579,58581,58582,58583,58585,58586,58587,58588,58589,58590,58591,58592,58593,58594,58595,58596,58597,58598,58599,58600,58601,58602,58603,58604,58605,58606,58607,58608,58609,58610,58611,58612,58613,58614,58615,58616,58617,58618,58619,58620,58621,58622,58623,58624,58625,58626,58627,58628,58629,58630,58631,58632,58633,58634,58635,58636,58637,58638,58639,58640,58641,58642,58643,58644,58645,58646,58647,58648,58649,58651,58652,58653,58654,58656,58657,58658,58659,58660,58661,58662,58663,58664,58665,58666,58667,58668,58669,58670,58671,58672,58673,58674,58675,58676,58677,58678,58679,58680,58681,58682,58683,58684,58685,58686,58687,58688,58689,58690,58691,58692,58693,58694,58695,58696,58697,58698,58699,58700,58701,58702,58703,58704,58705,58706,58707,58708,58709,58710,58711,58712,58713,58714,58715";
      String chars = "D\u5728\u4e3b\u7279\u5bb6\u519b\u7136\u8868\u573a4\u8981\u53eav\u548c6\u522b\u8fd8g\u73b0\u513f\u5c81\u6b64\u8c61\u67083\u51fa\u6218\u5de5\u76f8o\u7537\u76f4\u5931\u4e16F\u90fd\u5e73\u6587\u4ec0VO\u5c06\u771fT\u90a3\u5f53\u4f1a\u7acb\u4e9bu\u662f\u5341\u5f20\u5b66\u6c14\u5927\u7231\u4e24\u547d\u5168\u540e\u4e1c\u6027\u901a\u88ab1\u5b83\u4e50\u63a5\u800c\u611f\u8f66\u5c71\u516c\u4e86\u5e38\u4ee5\u4f55\u53ef\u8bdd\u5148pi\u53eb\u8f7bM\u58ebw\u7740\u53d8\u5c14\u5febl\u4e2a\u8bf4\u5c11\u8272\u91cc\u5b89\u82b1\u8fdc7\u96be\u5e08\u653et\u62a5\u8ba4\u9762\u9053S\u514b\u5730\u5ea6I\u597d\u673aU\u6c11\u5199\u628a\u4e07\u540c\u6c34\u65b0\u6ca1\u4e66\u7535\u5403\u50cf\u65af5\u4e3ay\u767d\u51e0\u65e5\u6559\u770b\u4f46\u7b2c\u52a0\u5019\u4f5c\u4e0a\u62c9\u4f4f\u6709\u6cd5r\u4e8b\u5e94\u4f4d\u5229\u4f60\u58f0\u8eab\u56fd\u95ee\u9a6c\u5973\u4ed6Y\u6bd4\u7236xAHNsX\u8fb9\u7f8e\u5bf9\u6240\u91d1\u6d3b\u56de\u610f\u5230z\u4ecej\u77e5\u53c8\u5185\u56e0\u70b9Q\u4e09\u5b9a8Rb\u6b63\u6216\u592b\u5411\u5fb7\u542c\u66f4\u5f97\u544a\u5e76\u672cq\u8fc7\u8bb0L\u8ba9\u6253f\u4eba\u5c31\u8005\u53bb\u539f\u6ee1\u4f53\u505a\u7ecfK\u8d70\u5982\u5b69cG\u7ed9\u4f7f\u7269\u6700\u7b11\u90e8\u5458\u7b49\u53d7k\u884c\u4e00\u6761\u679c\u52a8\u5149\u95e8\u5934\u89c1\u5f80\u81ea\u89e3\u6210\u5904\u5929\u80fd\u4e8e\u540d\u5176\u53d1\u603b\u6bcd\u7684\u6b7b\u624b\u5165\u8def\u8fdb\u5fc3\u6765h\u65f6\u529b\u591a\u5f00\u5df2\u8bb8d\u81f3\u7531\u5f88\u754cn\u5c0f\u4e0eZ\u60f3\u4ee3\u4e48\u5206\u751f\u53e3\u518d\u5988\u671b\u6b21\u897f\u98ce\u79cd\u5e26J\u5b9e\u60c5\u624d\u8fd9E\u6211\u795e\u683c\u957f\u89c9\u95f4\u5e74\u773c\u65e0\u4e0d\u4eb2\u5173\u7ed30\u53cb\u4fe1\u4e0b\u5374\u91cd\u5df1\u80012\u97f3\u5b57m\u5462\u660e\u4e4b\u524d\u9ad8PB\u76ee\u592ae9\u8d77\u7a1c\u5979\u4e5fW\u7528\u65b9\u5b50\u82f1\u6bcf\u7406\u4fbf\u56db\u6570\u671f\u4e2dC\u5916\u6837a\u6d77\u4eec\u4efb";
      chars = new String(
         Base64.getDecoder()
            .decode(
               "ROWcqOS4u+eJueWutuWGm+eEtuihqOWcujTopoHlj6p25ZKMNuWIq+i/mGfnjrDlhL/lsoHmraTosaHmnIgz5Ye65oiY5bel55u4b+eUt+ebtOWkseS4lkbpg73lubPmlofku4BWT+Wwhuecn1TpgqPlvZPkvJrnq4vkupt15piv5Y2B5byg5a2m5rCU5aSn54ix5Lik5ZG95YWo5ZCO5Lic5oCn6YCa6KKrMeWug+S5kOaOpeiAjOaEn+i9puWxseWFrOS6huW4uOS7peS9leWPr+ivneWFiHBp5Y+r6L27TeWjq3fnnYDlj5jlsJTlv6ts5Liq6K+05bCR6Imy6YeM5a6J6Iqx6L+cN+mavuW4iOaUvnTmiqXorqTpnaLpgZNT5YWL5Zyw5bqmSeWlveaculXmsJHlhpnmiorkuIflkIzmsLTmlrDmsqHkuabnlLXlkIPlg4/mlq815Li6eeeZveWHoOaXpeaVmeeci+S9huesrOWKoOWAmeS9nOS4iuaLieS9j+acieazlXLkuovlupTkvY3liKnkvaDlo7Douqvlm73pl67pqazlpbPku5ZZ5q+U54i2eEFITnNY6L65576O5a+55omA6YeR5rS75Zue5oSP5YiweuS7jmrnn6Xlj4jlhoXlm6DngrlR5LiJ5a6aOFJi5q2j5oiW5aSr5ZCR5b635ZCs5pu05b6X5ZGK5bm25pyscei/h+iusEzorqnmiZNm5Lq65bCx6ICF5Y675Y6f5ruh5L2T5YGa57uPS+i1sOWmguWtqWNH57uZ5L2/54mp5pyA56yR6YOo5ZGY562J5Y+Xa+ihjOS4gOadoeaenOWKqOWFiemXqOWktOingeW+gOiHquino+aIkOWkhOWkqeiDveS6juWQjeWFtuWPkeaAu+avjeeahOatu+aJi+WFpei3r+i/m+W/g+adpWjml7blipvlpJrlvIDlt7Lorrhk6Iez55Sx5b6I55WMbuWwj+S4jlrmg7Pku6PkuYjliIbnlJ/lj6Plho3lpojmnJvmrKHopb/po47np43luKZK5a6e5oOF5omN6L+ZReaIkeelnuagvOmVv+iniemXtOW5tOecvOaXoOS4jeS6suWFs+e7kzDlj4vkv6HkuIvljbTph43lt7HogIEy6Z+z5a2XbeWRouaYjuS5i+WJjemrmFBC55uu5aSqZTnotbfnqJzlpbnkuZ9X55So5pa55a2Q6Iux5q+P55CG5L6/5Zub5pWw5pyf5LitQ+Wkluagt2Hmtbfku6zku7s="
            ),
         StandardCharsets.UTF_8
      );
      String[] entries = codes.split(",");
      Map<Integer, Integer> map = new HashMap<>();
      int charOffset = 0;

      for (String entry : entries) {
         int cp = chars.codePointAt(charOffset);
         charOffset += Character.charCount(cp);
         map.put(Integer.parseInt(entry), cp);
      }

      StringBuilder result = new StringBuilder();
      text.codePoints().forEach(cpx -> result.appendCodePoint(map.getOrDefault(cpx, cpx)));
      return result.toString();
   }

   public static final class Book {
      public final String id;
      public final String title;
      public final String author;
      public final String category;
      public final String description;
      public final List<FanqieApi.Chapter> chapters;

      public Book(String id, String title, String author, String category, String description, List<FanqieApi.Chapter> chapters) {
         this.id = id;
         this.title = title.isBlank() ? "\u672a\u547d\u540d" : title;
         this.author = author;
         this.category = category;
         this.description = description;
         this.chapters = chapters;
      }
   }

   public static final class Chapter {
      public final String id;
      public final String title;
      public final String content;
      public final List<String> images;
      public final boolean locked;

      public Chapter(String id, String title, String content) {
         this(id, title, content, List.of(), false);
      }

      public Chapter(String id, String title, String content, List<String> images) {
         this(id, title, content, images, false);
      }

      public Chapter(String id, String title, String content, List<String> images, boolean locked) {
         this.id = id;
         this.title = title.isBlank() ? "\u672a\u547d\u540d\u7ae0\u8282" : title;
         this.content = content;
         this.images = List.copyOf(images);
         this.locked = locked;
      }
   }
}
