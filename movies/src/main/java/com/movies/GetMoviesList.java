package com.movies;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetMoviesList {
    static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";

    public static void main(String[] args) {
        String imdbMoviesUrl = "https://www.imdb.com/chart/top";
        String imdbTvUrl = "https://www.imdb.com/chart/toptv";
        String doubanMoviesUrl = "https://movie.douban.com/top250";

        List<String> imdbTop250Movies = extractIMDbTop250(imdbMoviesUrl);
        List<String> imdbTop250Tv = extractIMDbTop250(imdbTvUrl);
        List<String> doubanTop250Movies = extractDoubanTop250(doubanMoviesUrl);

        getMovieNameFromTmdb(imdbTop250Movies);
        getTvNameFromTmdb(imdbTop250Tv);

        List<String> duplicateData = extractRepeatMovies(imdbTop250Movies, doubanTop250Movies);

        printList("IMDb Top 250 Tv 中文名单:", imdbTop250Tv);
        printList("共同上榜的影片数据:", duplicateData);
        printList("IMDb Top 250 Movies 中文名单:", imdbTop250Movies);

        List<String> douban250 = new ArrayList<>();
        for (String s : doubanTop250Movies) {
            douban250.add(s.split("/")[0].trim());
        }

        imdbTop250Movies.removeAll(duplicateData);
        printList("IMDb Top 250 Movies 去重名单:", imdbTop250Movies);

        douban250.removeAll(duplicateData);
        printList("豆瓣 Top 250 Movies 去重名单:", douban250);

        System.out.println("注意：你的名字。/ 哈利·波特与死亡圣器 / 加勒比海盗 / 12年级的失败 / 疯狂的麦克斯4：狂暴之路 / 指环王3：王者无敌");
    }

    private static void printList(String name, List<String> list) {
        System.out.println(name);
        int num = 1;
        for (String s : list) {
            System.out.println(num + "." + s);
            num++;
        }
        System.out.println();
    }

    /**
     * 通过ai大模型将imdb的英文片名转为中文片名
     *
     * @param imdbList
     */
    private static void aiSearch(List<String> imdbList) {
        // 创建一个 HttpClient，只需创建一次并在循环中复用
        HttpClient client = HttpClient.newHttpClient();

        // 正则表达式匹配数字：1917 (2019)  记忆碎片 Memento (2000)
        String regex = "\\((\\d+)\\)";
        Pattern pattern = Pattern.compile(regex);

        // 遍历IMDb电影列表
        for (int i = 0; i < imdbList.size(); i++) {
            String title = imdbList.get(i);
            // 构建JSON请求体,使用Java15后提供的文本块
            String jsonBody = String.format("""
                    {
                      "model": "gpt-4o",
                      "messages": [
                        {
                          "role": "system",
                          "content": "我将给出影片名或者电视剧名，请你翻译为在中国大陆上映的完整中文片名或者知名度高、民间认可度高的完整中文片名(也可使用豆瓣的完整中文片名)，切记你只需要回复中文片名和对应年份，格式如 肖申克的救赎 (1994) ，如果是系列影片请加上第几部的编号，格式如 加勒比海盗1：黑珍珠号的诅咒 (2003)，如果是电视剧请加上结束年份，格式如 绝命毒师 (2008-2013)，如果是还未完结的电视剧不需要加上结束年份，格式如 布鲁伊 (2018-)。请严格按照我所提供的格式，不需要介绍影片也不需要其他任何信息。"
                        },
                        {
                          "role": "user",
                          "content": "%s"
                        }
                      ],
                      "temperature": 0.7
                    }""", title);
            try {
                // 创建 HttpRequest 实例
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://models.inference.ai.azure.com/chat/completions#")) // 替换为你的URL
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer 123456") // 设置你的头信息
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)) // 替换为你的JSON数据
                        .build();

                // 发送请求并获取响应
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 使用 fastjson2 将响应体字符串转换为 JSONObject
                JSONObject obj = JSON.parseObject(response.body());

                // 从 JSONObject 中获取 "choices" 数组
                JSONArray choices = obj.getJSONArray("choices");

                // 假设我们只关心第一个选择，获取第一个元素
                JSONObject firstChoice = choices.getJSONObject(0);

                // 从第一个选择中获取 "message" 对象
                JSONObject message = firstChoice.getJSONObject("message");

                // 从 "message" 对象中获取 "content" 字段的值
                String content = message.getString("content");

                // 使用正则表达式匹配括号内的数字
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String aiYear = matcher.group(1);
                    matcher = pattern.matcher(title);
                    if (matcher.find()) {
                        String imdbYear = matcher.group(1);
                        // 比较年份是否相同，如果相同则更新列表中的电影标题
                        if (aiYear.equals(imdbYear)) {
                            imdbList.set(i, content);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过tmdb将imdb的英文片名转为中文片名
     *
     * @param imdbList imdb英文数据
     */
    private static void getMovieNameFromTmdb(List<String> imdbList) {
        // 创建一个 HttpClient，只需创建一次并在循环中复用
        HttpClient httpClient = HttpClient.newHttpClient();

        for (int i = 0; i < imdbList.size(); i++) {
            String title = imdbList.get(i);
            // 使用正则表达式匹配影片名和年份  1917 (2019) 记忆碎片 Memento (2000)
            String regex = "(.*?)(\\((\\d+)\\))(.*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                String title2 = matcher.group(1).trim();
                String imdbYear = matcher.group(3);
                // 替换成你的 API 密钥
                String apiKey = "029592f3b318e93d520307a1cc4c46f4";
                // 语言参数
                String language = "zh-CN";
                // 构造查询URL
                String url = String.format("https://api.themoviedb.org/3/search/movie?query=%s&year=%s&language=%s&api_key=%s",
                        URLEncoder.encode(title2, StandardCharsets.UTF_8), imdbYear, language, apiKey);

                // 创建 HttpRequest 实例
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("accept", "application/json")
                        // .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIwMjk1OTJmM2IzMThlOTNkNTIwMzA3YTFjYzRjNDZmNCIsInN1YiI6IjY2MDA1YTZjMzUyMGU4MDE3ZWQ4YjE4MiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.awR5dIdpsuIyDDhi-uh050HnxcBtN3GPt_zs5tHlO9g")
                        // .method("GET", HttpRequest.BodyPublishers.noBody())
                        .GET() // 默认就是 GET 请求，这里显式调用以便清晰表达意图
                        .build();

                // 发送请求并获取响应
                HttpResponse<String> response = null;
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 解析 JSON 字符串
                JSONObject jsonObject = JSONObject.parseObject(response.body());
                JSONArray results = jsonObject.getJSONArray("results");
                if (!results.isEmpty()) {
                    JSONObject firstMovie = results.getJSONObject(0);
                    String id = firstMovie.getString("id");
                    title = firstMovie.getString("title");
                    title = title + " (" + imdbYear + ")";
                    String releaseDate = firstMovie.getString("release_date");
                    // 提取年份
                    String tmdbYear = releaseDate.split("-")[0];
                    if (!imdbYear.equals(tmdbYear)) {
                        url = "https://api.themoviedb.org/3/movie/" + id + "/release_dates?api_key=" + apiKey;
                        request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .GET() // 默认就是 GET 请求，这里显式调用以便清晰表达意图
                                .build();
                        try {
                            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        jsonObject = JSONObject.parseObject(response.body());
                        // 解析JSON字符串
                        results = jsonObject.getJSONArray("results");
                        int minYear = Integer.MAX_VALUE;

                        for (int k = 0; k < results.size(); k++) {
                            JSONObject country = results.getJSONObject(k);
                            JSONArray releaseDates = country.getJSONArray("release_dates");
                            for (int j = 0; j < releaseDates.size(); j++) {
                                JSONObject releaseDateObj = releaseDates.getJSONObject(j);
                                releaseDate = releaseDateObj.getString("release_date");
                                // 提取年份
                                int year = Integer.parseInt(releaseDate.substring(0, 4));
                                // 更新最小年份
                                if (year < minYear) {
                                    minYear = year;
                                }
                            }
                        }
                        if (Integer.parseInt(imdbYear) != (minYear)) {
                            List<String> list = new ArrayList<>();
                            list.add(title);
                            aiSearch(list);
                            title = list.get(0);
                        }
                    }
                    regex = "[\u4e00-\u9fa5]";
                    pattern = Pattern.compile(regex);
                    matcher = pattern.matcher(title);
                    if (!matcher.find()) {
                        List<String> list = new ArrayList<>();
                        list.add(title);
                        aiSearch(list);
                        title = list.get(0);
                    }
                    imdbList.set(i, title);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(title);
                    aiSearch(list);
                    title = list.get(0);
                    imdbList.set(i, title);
                }
            }
        }
    }

    private static void getTvNameFromTmdb(List<String> imdbList) {
        // 创建一个 HttpClient，只需创建一次并在循环中复用
        HttpClient httpClient = HttpClient.newHttpClient();

        for (int i = 0; i < imdbList.size(); i++) {
            String title = imdbList.get(i);
            // 使用正则表达式匹配电视剧名和年份 Breaking Bad (2008–2013)
            String regex = "(.+)\\s\\((.+)\\)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                String title2 = matcher.group(1).trim();
                String imdbYear = matcher.group(2);
                // String startYear = matcher.group(2);
                // String endYear = matcher.group(4);
                // 替换成你的 API 密钥
                String apiKey = "029592f3b318e93d520307a1cc4c46f4";
                // 语言参数
                String language = "zh-CN";
                // 构造查询URL
                String url = String.format("https://api.themoviedb.org/3/search/tv?query=%s&year=%s&language=%s&api_key=%s",
                        URLEncoder.encode(title2, StandardCharsets.UTF_8), imdbYear, language, apiKey);

                // 创建 HttpRequest 实例
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .GET() // 默认就是 GET 请求，这里显式调用以便清晰表达意图
                        .build();

                // 发送请求并获取响应
                HttpResponse<String> response = null;
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 解析 JSON 字符串
                JSONObject jsonObject = JSONObject.parseObject(response.body());
                JSONArray results = jsonObject.getJSONArray("results");
                if (!results.isEmpty()) {
                    JSONObject firstMovie = results.getJSONObject(0);
                    title = firstMovie.getString("name");
                    // title = title + " (" + startYear + "-" + endYear + ")";
                    title = title + " (" + imdbYear + ")";
                    String releaseDate = firstMovie.getString("first_air_date");
                    // 提取年份
                    String tmdbYear = releaseDate.split("-")[0];
                    if (!imdbYear.substring(0, 4).equals(tmdbYear)) {
                        List<String> list = new ArrayList<>();
                        list.add(title);
                        aiSearch(list);
                        title = list.get(0);
                        imdbList.set(i, title);
                        continue;
                    }
                    regex = "[\u4e00-\u9fa5]";
                    pattern = Pattern.compile(regex);
                    matcher = pattern.matcher(title);
                    if (!matcher.find()) {
                        List<String> list = new ArrayList<>();
                        list.add(title);
                        aiSearch(list);
                        title = list.get(0);
                    }
                    imdbList.set(i, title);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(title);
                    aiSearch(list);
                    title = list.get(0);
                    imdbList.set(i, title);
                }
            }
        }
    }


    /**
     * 豆瓣Top250与IMDB250重复影片数据
     *
     * @param imdbList
     * @param doubanList
     */
    private static List<String> extractRepeatMovies(List<String> imdbList, List<String> doubanList) {
        List<String> repeatList = new ArrayList<>();
        for (int i = 0; i < doubanList.size(); i++) {
            String[] doubanParts = doubanList.get(i).split("/");
            outerLoop:
            for (String imdb : imdbList) {
                for (String part : doubanParts) {
                    if (part.trim().equals(imdb)) {
                        repeatList.add(imdb);
                        doubanList.set(i, imdb);
                        break outerLoop;
                    }
                }
            }
        }
        return repeatList;
    }

    // /**
    //  * 获取IMDb Top 250 数据
    //  *
    //  * @param url IMDb 250 页面的URL
    //  * @return 电影标题和年份的列表
    //  */
    // private static List<String> extractIMDbTop250(String url) {
    //     List<String> list = new ArrayList<>();
    //     try {
    //         Document document = Jsoup.connect(url).userAgent(userAgent)
    //                 .header("Accept-Language", "zh-CN;q=0.9,en;q=0.8,ja;q=0.7")
    //                 .header("Accept", "*/*")
    //                 .get();
    //         // Elements movieItems = document.select("ul.ipc-metadata-list li");
    //         Elements movieItems = document.select("ul.ipc-metadata-list li.ipc-metadata-list-summary-item");
    //
    //         if (url.endsWith("tv")) {
    //             System.out.println("IMDb Top 250 剧集:");
    //         } else {
    //             System.out.println("IMDb Top 250 电影:");
    //         }
    //
    //         int rank = 1;
    //         for (Element item : movieItems) {
    //             String title = item.select(".ipc-title-link-wrapper h3").text();
    //             title = title.substring(title.indexOf(" ") + 1);
    //             String rating = item.select(".ratingGroup--imdb-rating").text();
    //             String year = item.select(".cli-title-metadata-item").first().text();
    //             year = "(" + year + ")";
    //             list.add(title + " " + year);
    //             System.out.println(rank + "." + title + " " + year + ", 评分: " + rating);
    //             rank++;
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     return list;
    // }

    /**
     * 获取IMDb Top 250 数据
     *
     * @param url IMDb 250 页面的URL
     * @return 电影标题和年份的列表
     */
    private static List<String> extractIMDbTop250(String url) {
        // 设置ChromeDriver路径
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\emma\\Desktop\\chromedriver-win64\\chromedriver.exe");

        // 创建ChromeOptions对象并设置必要的选项
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // 如果你不想看到浏览器界面，可以启用无头模式
        WebDriver driver = new ChromeDriver(options);

        List<String> list = new ArrayList<>();

        try {
            driver.get(url);

            // 模拟页面滚动以加载所有数据
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000); // 等待2秒以确保加载完成

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    break;
                }
                lastHeight = newHeight;
            }

            // 获取所有电影条目
            List<WebElement> movieElements = driver.findElements(By.cssSelector("ul.ipc-metadata-list li.ipc-metadata-list-summary-item"));
            // Elements movieItems = document.select("ul.ipc-metadata-list li");

            if (url.endsWith("tv")) {
                System.out.println("IMDb Top 250 剧集:");
            } else {
                System.out.println("IMDb Top 250 电影:");
            }

            int rank = 1;
            for (WebElement item : movieElements) {
                String title = item.findElement(By.cssSelector(".ipc-title-link-wrapper h3")).getText();
                title = title.substring(title.indexOf(" ") + 1);
                // HTML中的&nbsp;和换行在网页上显示为一个空格，但Selenium的getText()方法可能会将它们解释为实际的换行符。
                String rating = item.findElement(By.cssSelector(".ratingGroup--imdb-rating")).getText();
                // 使用正则表达式替换所有空白字符为单个空格
                rating = rating.replaceAll("\\s+", " ").trim();
                String year = item.findElement(By.cssSelector(".cli-title-metadata-item")).getText();
                year = "(" + year + ")";
                list.add(title + " " + year);
                System.out.println(rank + "." + title + " " + year + ", 评分: " + rating);
                rank++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭浏览器
            driver.quit();
        }
        return list;
    }

    /**
     * 获取豆瓣 Top 250 电影数据
     *
     * @param url 豆瓣 Top 250 电影页面的URL
     * @return 包含电影信息的字符串列表
     */
    private static List<String> extractDoubanTop250(String url) {
        List<String> movieList = new ArrayList<>();
        try {
            int totalPages = 10;// 总页数
            int rank = 1;// 排名

            System.out.println("豆瓣 Top 250 电影:");
            for (int page = 0; page < totalPages; page++) {
                String pageUrl = url + "?start=" + (page * 25);
                Document document = Jsoup.connect(pageUrl).get();
                Elements movieItems = document.select("ol.grid_view li");

                for (Element item : movieItems) {
                    String firstTitle = item.select(".title").first().text();
                    String title = item.select(".title").text();
                    String otherTitles = item.select(".other").text();
                    String rating = item.select(".rating_num").text();

                    // 从文本中提取年份字符串
                    String year = item.select("div.bd > p").text();
                    Pattern pattern = Pattern.compile("\\d{4}"); // 匹配四个连续数字的正则表达式
                    Matcher matcher = pattern.matcher(year);
                    if (matcher.find()) year = "(" + matcher.group() + ")";

                    // 构建电影信息字符串  title 主标题  otherTitles 其他标题  year 年份
                    StringBuilder movieInfo = new StringBuilder();

                    // 豆瓣影片名与TMDB影片名不对应
                    if (title.contains("加勒比海盗") || firstTitle.equals("加勒比海盗")) {
                        title = title.replace("加勒比海盗", "加勒比海盗：黑珍珠号的诅咒");
                        firstTitle = "加勒比海盗：黑珍珠号的诅咒";
                    }

                    String[] titles = (title + otherTitles).split("/");
                    // 豆瓣：哈利·波特与死亡圣器(上)，TMDB：哈利·波特与死亡圣器（上）
                    // String[] titles = (title.replace("(", "（").replace(")", "）") + otherTitles).split("/");
                    for (int i = 0; i < titles.length; i++) {
                        movieInfo.append(titles[i].trim()).append(" ").append(year);
                        if (i < titles.length - 1) {
                            movieInfo.append(" / ");
                        }
                    }

                    movieList.add(movieInfo.toString());
                    System.out.println(rank + "." + firstTitle + " " + year);
                    rank++;
                }
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return movieList;
    }

    /**
     * 通过tmdb将imdb的英文片名转为中文片名
     *
     * @param type     电影movie 剧集tv
     * @param imdbList imdb英文数据
     */
    private static void getNameParsePage(List<String> imdbList, String... type) {
        for (int i = 0; i < imdbList.size(); i++) {
            String title = imdbList.get(i);
            String regex = "(.*?)(\\((\\d+)\\))(.*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                String title2 = matcher.group(1).trim();
                String imdbYear = matcher.group(3);
                // 构造查询URL
                String url;
                if (type.length == 0) {
                    url = "https://www.themoviedb.org/search/movie?language=zh-CN&query=" + title2 + " y:" + imdbYear;
                } else {
                    url = "https://www.themoviedb.org/search/" + type[0] + "?language=zh-CN&query=" + title2 + " y:" + imdbYear;
                }

                // 发起GET请求
                Document document = null;
                try {
                    document = Jsoup.connect(url).userAgent(userAgent)
                            .header("Accept-Language", "zh-CN,zh;q=0.9,ja;q=0.8,en;q=0.7")
                            .header("Accept", "*/*")
                            .get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Element titleElement;
                if (type[0].equals("tv")) {
                    // 解析HTML
                    titleElement = document.selectFirst("div.details div.wrapper div.title a.result[data-media-type=tv] > h2");
                } else {
                    titleElement = document.selectFirst("div.details div.wrapper div.title a.result[data-media-type=movie] > h2");
                }

                Element releaseDateElement = document.selectFirst("div.details div.wrapper div.title .release_date");
                // 如果找到了电影信息
                if (titleElement != null) {
                    regex = "[\u4e00-\u9fa5]";
                    pattern = Pattern.compile(regex);
                    matcher = pattern.matcher(titleElement.text());
                    if (matcher.find()) {
                        pattern = Pattern.compile("\\b\\d{4}\\b");
                        matcher = pattern.matcher(releaseDateElement.text());
                        if (matcher.find()) {
                            //"1994 年 09 月 23 日"
                            String tmdbYear = matcher.group();
                            if (tmdbYear.equals(imdbYear)) {
                                title = titleElement.text() + " (" + tmdbYear + ")";
                            } else {
                                List<String> list = new ArrayList<>();
                                list.add(title);
                                aiSearch(list);
                                title = list.get(0);
                            }
                        }
                    } else {
                        List<String> list = new ArrayList<>();
                        list.add(title);
                        aiSearch(list);
                        title = list.get(0);
                    }
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(title);
                    aiSearch(list);
                    title = list.get(0);
                }
                imdbList.set(i, title);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

