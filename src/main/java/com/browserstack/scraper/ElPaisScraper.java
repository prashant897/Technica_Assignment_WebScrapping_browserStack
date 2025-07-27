package com.browserstack.scraper;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ElPaisScraper {
    private static final String USERNAME = "prashantkumarsin_xs10B2";
    private static final String ACCESS_KEY = "HGsUiuCVgEGy8s3Lmx6J";
    private static final String HUB_URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@hub.browserstack.com/wd/hub";
    private static final String TRANSLATE_API_KEY = "5053a32fbdmsh19b06ac4f38ff21p15e12ajsn80b0be3a2c36";

    public static void main(String[] args) throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        List<Capabilities> browsers = List.of(
                getChromeDesktopCaps("Scraper Test 0"),
                getSafariMacCaps("Scraper Test 1"),
                getAndroidCaps("Scraper Test 2"),
                getiOSCaps("Scraper Test 3"),
                getFirefoxWindowsCaps("Scraper Test 4")
        );

        for (Capabilities caps : browsers) {
            tasks.add(() -> {
                scrape(caps);
                return null;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.MINUTES);
        System.out.println("✅ Scraping finished. Output saved to elpais_output.csv");
    }

    public static void scrape(Capabilities caps) {
        WebDriver driver = null;
        try {
            driver = new RemoteWebDriver(new URL(HUB_URL), caps);
            driver.get("https://elpais.com/");
            Thread.sleep(3000);

            // Click "Opinión" using XPath for robustness
            try {
                WebElement opinionLink = driver.findElement(By.xpath("//a[contains(text(), 'Opinión')]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", opinionLink);
            } catch (Exception e) {
                System.out.println("❌ Couldn't find Opinión link");
                return;
            }

            Thread.sleep(3000);
            List<String[]> rows = new ArrayList<>();
            Set<String> repeatedWords = new HashSet<>();
            List<String> translatedTitles = new ArrayList<>();

            List<WebElement> articles = driver.findElements(By.cssSelector("article"));
            for (int i = 0; i < Math.min(5, articles.size()); i++) {
                WebElement article = articles.get(i);
                String title = article.getText().split("\n")[0].trim();
                String translated = translate(title);
                translatedTitles.add(translated);

                String link = "";
                try {
                    link = article.findElement(By.tagName("a")).getAttribute("href");
                } catch (Exception e) { /* skip */ }

                String content = "";
                String imgUrl = "";

                try {
                    Document doc = Jsoup.connect(link).get();
                    Elements paragraphs = doc.select("p");
                    content = paragraphs.stream().limit(5).map(org.jsoup.nodes.Element::text)
                            .collect(Collectors.joining("\n"));
                    imgUrl = doc.select("img").stream().findFirst().map(e -> e.absUrl("src")).orElse("");
                    if (!imgUrl.isEmpty()) {
                        downloadImage(imgUrl, "image" + i + ".jpg");
                    }
                } catch (IOException ignored) {}

                rows.add(new String[]{title, translated, content, imgUrl});
            }

            findRepeatedWords(translatedTitles, repeatedWords);
            saveCSV(rows);
            System.out.println("🔁 Repeated words: " + repeatedWords);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private static String translate(String text) throws IOException {
        String encoded = URLEncoder.encode(text, "UTF-8");
        String url = "https://google-translate113.p.rapidapi.com/api/v1/translator/text?from=es&to=en&text=" + encoded;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("X-RapidAPI-Key", TRANSLATE_API_KEY);
        conn.setRequestProperty("X-RapidAPI-Host", "google-translate113.p.rapidapi.com");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) json.append(line);
        reader.close();

        JSONObject obj = new JSONObject(json.toString());
        return obj.getString("trans");
    }

    private static void downloadImage(String url, String fileName) throws IOException {
        try (InputStream in = new URL(url).openStream();
             OutputStream out = new FileOutputStream(fileName)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static void findRepeatedWords(List<String> titles, Set<String> repeated) {
        Map<String, Integer> wordCount = new HashMap<>();
        for (String title : titles) {
            for (String word : title.toLowerCase().split("\\W+")) {
                if (word.length() > 3)
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        wordCount.forEach((k, v) -> {
            if (v > 1) repeated.add(k);
        });
    }

    private static void saveCSV(List<String[]> rows) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("elpais_output.csv"))) {
            pw.println("Original Title,Translated Title,Content,Image URL");
            for (String[] row : rows) {
                pw.println(Arrays.stream(row)
                        .map(r -> "\"" + r.replace("\"", "\"\"") + "\"")
                        .collect(Collectors.joining(",")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === BrowserStack Capabilities ===

    private static Capabilities getChromeDesktopCaps(String testName) {
        ChromeOptions options = new ChromeOptions();
        options.setPlatformName("Windows 11");
        options.setBrowserVersion("latest");
        options.setCapability("bstack:options", commonCaps(testName));
        return options;
    }

    private static Capabilities getSafariMacCaps(String testName) {
        SafariOptions options = new SafariOptions();
        options.setPlatformName("OS X Ventura");
        options.setBrowserVersion("latest");
        options.setCapability("bstack:options", commonCaps(testName));
        return options;
    }

    private static Capabilities getFirefoxWindowsCaps(String testName) {
        FirefoxOptions options = new FirefoxOptions();
        options.setPlatformName("Windows 10");
        options.setBrowserVersion("latest");
        options.setCapability("bstack:options", commonCaps(testName));
        return options;
    }

    private static Capabilities getAndroidCaps(String testName) {
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", "chrome");
        Map<String, Object> bstack = commonCaps(testName);
        bstack.put("deviceName", "Samsung Galaxy S22");
        bstack.put("osVersion", "12.0");
        bstack.put("realMobile", "true");
        caps.setCapability("bstack:options", bstack);
        return caps;
    }

    private static Capabilities getiOSCaps(String testName) {
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", "safari");
        Map<String, Object> bstack = commonCaps(testName);
        bstack.put("deviceName", "iPhone 13");
        bstack.put("osVersion", "15");
        bstack.put("realMobile", "true");
        caps.setCapability("bstack:options", bstack);
        return caps;
    }

    private static Map<String, Object> commonCaps(String testName) {
        Map<String, Object> bstack = new HashMap<>();
        bstack.put("projectName", "ElPaisScraper");
        bstack.put("buildName", "Build 1");
        bstack.put("sessionName", testName);
        return bstack;
    }
}