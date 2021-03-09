import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    //private static final String BASE_URL_STR = "https://flashdeals.aliexpress.com/en.htm?";
    private static final String BASE_URL_QUERY_BEFORE = "https://gpsfront.aliexpress.com/getRecommendingResults.do?callback=jQuery18305422764862341505_1615102112161&widget_id=5547572&platform=pc&limit=12&offset=";
    private static final String BASE_URL_QUERY_AFTER = "&phase=1&productIds2Top=&postback=f4087a36-428a-47b1-bf61-c0df94b762a4&_=1615105432030";
    private static final String PATH_TO_FILE = "data/map.csv";
    private static int countAvailableProcessors;

    public static void main(String[] args) throws IOException, ParseException {
        countAvailableProcessors = Runtime.getRuntime().availableProcessors();

        //Получаем список товаров
        System.out.println("Parsing...");
        long start = System.currentTimeMillis();
        ArrayList<Product> products = parseUrl();

        //Пишем в файл после завершения всех потоков
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Writing to file...");
            try {
                writeToFile(products);
            } catch (IOException e) {
                e.printStackTrace();
            }
            long finish = (System.currentTimeMillis() - start) / 1000;
            System.out.println("Done in " + finish + " seconds!");
        }));
    }

    //Вывод на экран (в целях проверки)
    private static void writeToScreen(ArrayList<Product> products) {
        int counter = 0;
        for (Product p : products) {
            counter++;
            System.out.println(counter + ": " + p);
        }
    }

    //Запись в файл
    private static void writeToFile(ArrayList<Product> products) throws IOException {
        File file = new File(PATH_TO_FILE);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        StringBuilder csvProduct = new StringBuilder();
        csvProduct.append("Name, URL, Rating, Sold, MinPrice, MaxPrice;\n");
        products.forEach(p -> csvProduct.append(p.getTitle()).append(",").append(p.getUrl()).append(",")
                .append(p.getSold()).append(",").append(p.getMinPrice()).append(",")
                .append(p.getMaxPrice()).append(";\n"));
            try {
                bufferedWriter.write(String.valueOf(csvProduct));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    //Получаем список товаров для дальнейшего парсинга
    private static ArrayList<Product> parseUrl() throws IOException, ParseException {
        ArrayList<Product> products = new ArrayList<>();
        JSONArray resultGoods = new JSONArray();
        for (int offset = 12; offset <= 108; offset = offset + 12) {
            String fullQuery = Main.BASE_URL_QUERY_BEFORE + offset + Main.BASE_URL_QUERY_AFTER;
            String json = Jsoup.connect(fullQuery).ignoreContentType(true).execute().body();
            json = json.substring(json.indexOf("{"), json.length() - 2);
            Object obj = new JSONParser().parse(json);
            JSONObject jo = (JSONObject) obj;
            JSONArray pageGoods = (JSONArray) jo.get("results");
            resultGoods.addAll(pageGoods);
        }
        Iterator productItr = resultGoods.iterator();
        int counter = 0;
        while (productItr.hasNext()) {
            counter++;
            Product product = new Product();
            JSONObject productJson = (JSONObject) productItr.next();
            //Парсинг свойств со страницы предложений
            product.setTitle(productJson.get("productTitle").toString());
            product.setUrl(productJson.get("productDetailUrl").toString());
            product.setMinPrice(productJson.get("minPrice").toString());
            product.setMaxPrice(productJson.get("maxPrice").toString());
            if (counter <= 100) {
                products.add(product);
            }
        }
        System.out.println("Flashdeals page parsed");
        System.out.println("Parsing internal properties...");
        return fillProductProperties(products);
    }

    //Многопоточно считываем свойства и возвращаем список
    private static ArrayList<Product> fillProductProperties(ArrayList<Product> products) {
        //Разделяем товары на количество логических ядер процессора и многопоточно достаем свойства
        Callable<Boolean> task = () -> {
            String threadNumber = Thread.currentThread().getName();
            threadNumber = threadNumber.substring(threadNumber.lastIndexOf("-")+1);
            int offsetIndex = Integer.parseInt(threadNumber) - 1;
            WebClient webClient = new WebClient(BrowserVersion.FIREFOX_78);
            java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setDoNotTrackEnabled(true);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
//            webClient.setCssErrorHandler(new SilentCssErrorHandler());
//            webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
//            webClient.getOptions().setUseInsecureSSL(false);
//            webClient.setCssErrorHandler(new SilentCssErrorHandler());
//            webClient.setIncorrectnessListener((s, o) -> { });
//            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
//            webClient.getCache().setMaxSize(0);
//            webClient.getOptions().setRedirectEnabled(true);
            int tail = 0;
            int part = products.size() / countAvailableProcessors;
            if (offsetIndex == countAvailableProcessors-1) {
                tail = products.size() % countAvailableProcessors;
            }
            for (int i = part * offsetIndex; i < part*offsetIndex+part + tail; i++) {
                Product p = products.get(i);
                HtmlPage myPage = null;
                try {
                    myPage = webClient.getPage("https://" + p.getUrl().substring(2));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Парсинг внутренних свойств со страницы товара
                Document doc = Jsoup.parse(myPage.asXml());
                Element elRating = doc.selectFirst(".overview-rating-average");
                Element elSold = doc.selectFirst(".product-reviewer-sold");
                synchronized ("") {
                    p.setRating(elRating.text());
                    p.setSold(elSold.text().substring(0,elSold.text().indexOf(" ")));
                }
            }
            webClient.close();
            return true;
        };
        ExecutorService service = Executors.newFixedThreadPool(countAvailableProcessors);
        for (int i = 0; i < countAvailableProcessors; i++) {
            service.submit(task);
        }
        service.shutdown();
        return products;
    }
}
