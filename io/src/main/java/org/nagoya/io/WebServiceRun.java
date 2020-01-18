package org.nagoya.io;

import io.vavr.control.Option;
import io.webfolder.cdp.session.Session;
import org.openqa.selenium.WebDriver;

import java.util.function.Consumer;

public class WebServiceRun {

    public static final WebServiceRun none = new WebServiceRun();

    private Option<Consumer<WebDriver>> consumerSelenium;
    private Option<Consumer<Session>> consumerCdp4j;

    public WebServiceRun() {
        this.consumerSelenium = Option.none();
        this.consumerCdp4j = Option.none();
    }

    public Option<Consumer<WebDriver>> getConsumerSelenium() {
        return this.consumerSelenium;
    }

    public Option<Consumer<Session>> getConsumerCdp4j() {
        return this.consumerCdp4j;
    }

    public void setConsumerSelenium(Consumer<WebDriver> consumerSelenium) {
        this.consumerSelenium = Option.of(consumerSelenium);
    }

    public void setConsumerCdp4j(Consumer<Session> consumerCdp4j) {
        this.consumerCdp4j = Option.of(consumerCdp4j);
    }
}
