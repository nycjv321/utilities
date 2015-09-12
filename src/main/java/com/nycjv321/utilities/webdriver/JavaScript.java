package com.nycjv321.utilities.webdriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by jvelasquez on 9/12/15.
 */
public class JavaScript {

    private final JavascriptExecutor javascriptExecutor;

    public JavaScript(WebDriver javascriptExecutor) {
        this.javascriptExecutor = (JavascriptExecutor) javascriptExecutor;
    }

    public Object executeScript(String script) {
        return executeScript(script, new Object[]{});
    }

    public Object executeScript(String script, Object... args) {
        return javascriptExecutor.executeScript(script, args);
    }

    /**
     * @return the output of javascript:document.domain
     */
    public String getDocumentDomain() {
        return String.valueOf(executeScript("return document.domain"));
    }

    public void click(WebElement element) {
        executeScript("arguments[0].click();", element);
    }

    public void delete(WebElement element) {
        executeScript("$(arguments[0]).remove();", element);
    }

    public void mouseOver(WebElement element) {
        executeScript("$(arguments[0]).mouseover();", element);
    }

    public String text(WebElement element) {
        return String.valueOf(executeScript("return $(arguments[0]).text()", element));

    }
}