package org.nagoya.io;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.vavr.collection.Vector;
import io.vavr.control.Try;
import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.commons.GUICommon;
import org.nagoya.commons.UtilCommon;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WebService {

    public static final int CONNECTION_TIMEOUT_VALUE = 15000;

    private static final WebService webService = new WebService(WebService.Mode.Cdp4jHeadless);

    public static WebService get() {
        return webService;
    }


    public enum Mode {
        Cdp4j,
        Cdp4jHeadless,
        SeleniumHeadless
    }

    private final WebImp webDriver;
    private final WebJSoupImp webJSoup;
    private final BooleanProperty isWorking;

    public WebService(Mode mode) {
        this.isWorking = new SimpleBooleanProperty();
        this.isWorking.set(false);

        if (mode == Mode.Cdp4j) {
            this.webDriver = new WebCdp4jImp(CONNECTION_TIMEOUT_VALUE);
        } else if (mode == Mode.Cdp4jHeadless) {
            this.webDriver = new WebCdp4jHeadlessImp(CONNECTION_TIMEOUT_VALUE);
        } else {
            this.webDriver = new WebSeleniumImp(CONNECTION_TIMEOUT_VALUE);
        }

        this.webJSoup = new WebJSoupImp(CONNECTION_TIMEOUT_VALUE);
    }

    public Try<String> getHtml(String strUrl, WebServiceRun run) {
        if (run == null) {
            run = WebServiceRun.none;
        }
        return this.webDriver.getHtml(strUrl, run);
    }

    public Try<Document> getDocument(String strUrl, WebServiceRun run, boolean runJavaScript) {
        if (run == null) {
            run = WebServiceRun.none;
        }

        GUICommon.debugMessage("getDocument");

        return runJavaScript ? this.webDriver.getJSoupDoc(strUrl, run) : this.webJSoup.getJSoupDoc(strUrl, run);
    }

    public Mono<Document> getDocumentAsync(String strUrl, WebServiceRun run, boolean runJavaScript) {
        return UtilCommon.tryToMono(() -> WebService.get().getDocument(strUrl, run, runJavaScript))
                .subscribeOn(Schedulers.fromExecutor(ExecuteSystem.getExecutorServices(ExecuteSystem.role.NORMAL)));
    }

    public BooleanProperty isWorkingProperty() {
        return this.isWorking;
    }

    public void shutdown() {
        this.webDriver.shutdown();
    }
}

abstract class WebImp {
    abstract Try<Document> getJSoupDoc(String strUrl, WebServiceRun run);

    abstract Try<String> getHtml(String strUrl, WebServiceRun run);

    abstract void shutdown();
}

class WebCdp4jImp extends WebImp {
    SessionFactory factory;
    Session dummySession;
    int timeOut;

    WebCdp4jImp() {
    }

    WebCdp4jImp(int timeOut) {
        Launcher launcher = new Launcher();
        this.factory = launcher.launch();
        this.dummySession = this.factory.create();
        this.timeOut = timeOut;
        this.dummySession.navigate("about:blank");
    }

    @Override
    public Try<Document> getJSoupDoc(String strUrl, WebServiceRun run) {
        return this.getHtml(strUrl, run).map(Jsoup::parse);
    }

    @Override
    public Try<String> getHtml(String strUrl, WebServiceRun run) {
        //this.setIsWorking(true);

        Session session = this.factory.create();
        return Try.of(() -> session.navigate(strUrl).waitDocumentReady(this.timeOut))
                .onSuccess(s -> run.getConsumerCdp4j().peek(r -> r.accept(s)))
                .map(Session::getContent)
                .onSuccess(s -> GUICommon.debugMessage("WebService >> getWebPage Success >> " + strUrl))
                .onFailure(GUICommon::errorDialog)
                .andFinally(session::close);
    }

    @Override
    public void shutdown() {
        this.dummySession.close();
        this.factory.close();

    }
}

class WebCdp4jHeadlessImp extends WebCdp4jImp {
    WebCdp4jHeadlessImp(int timeOut) {
        Launcher launcher = new Launcher();
        this.factory = launcher.launch(Vector.of("--headless", "--disable-gpu").asJava());
        this.dummySession = this.factory.create();
        this.timeOut = timeOut;
        this.dummySession.navigate("about:blank");
    }
}

class WebSeleniumImp extends WebImp {
    private final WebDriver webDriver;

    WebSeleniumImp(int timeOut) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");

        this.webDriver = new ChromeDriver(options);
        this.webDriver.manage().timeouts()
                .implicitlyWait(250, TimeUnit.MILLISECONDS)
                .pageLoadTimeout(timeOut, TimeUnit.MILLISECONDS)
                .setScriptTimeout(1, TimeUnit.SECONDS);
        this.webDriver.get("http://www.google.co.jp/");
    }

    @Override
    public Try<Document> getJSoupDoc(String strUrl, WebServiceRun run) {
        return this.getHtml(strUrl, run).map(Jsoup::parse);
    }

    @Override
    public Try<String> getHtml(String strUrl, WebServiceRun run) {
        return Try.run(() -> this.webDriver.get(strUrl))
                .map((v) -> this.webDriver)
                .onSuccess(s -> run.getConsumerSelenium().peek(r -> r.accept(s)))
                .onFailure(GUICommon::errorDialog)
                .map(WebDriver::getPageSource);
    }

    @Override
    public void shutdown() {
        this.webDriver.close();
    }
}

class WebJSoupImp extends WebImp {

    public static String getRandomUserAgent() {
        String[] userAgent = {"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6",
                "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36", "Opera/9.80 (Windows NT 6.0) Presto/2.12.388 Version/12.14",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0) Opera 12.14",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; tr-TR) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27",
                "Mozilla/5.0 (Windows; U; Windows NT 5.0; en-en) AppleWebKit/533.16 (KHTML, like Gecko) Version/4.1 Safari/533.16"};
        return userAgent[new Random().nextInt(userAgent.length)];
    }

    private final int timeOut;

    WebJSoupImp(int timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public Try<Document> getJSoupDoc(String strUrl, WebServiceRun run) {
        return Try.of(() -> Jsoup.connect(strUrl)
                .userAgent(getRandomUserAgent())
                .ignoreHttpErrors(true)
                .timeout(this.timeOut).get())
                //.onSuccess(d -> run.peek(r -> r.accept(d)))
                .onFailure(GUICommon::errorDialog);
    }

    @Override
    public Try<String> getHtml(String strUrl, WebServiceRun run) {
        return Try.failure(new Exception("Not support"));
    }

    @Override
    public void shutdown() {
    }
}