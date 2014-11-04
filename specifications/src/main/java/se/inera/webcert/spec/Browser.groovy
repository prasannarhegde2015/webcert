package se.inera.webcert.spec

import geb.driver.CachingDriverFactory

public class Browser {

    private static geb.Browser browser

    static void öppna() {
        if (browser) throw new IllegalStateException("Browser already initialized")
        browser = new geb.Browser()
    }

    public void stäng() {
        if (!browser) throw new IllegalStateException("Browser not initialized")
        browser.quit()
        browser = null
        CachingDriverFactory.clearCache()
    }

    public void laddaOm() {
        if (!browser) throw new IllegalStateException("Browser not initialized")
        browser.driver.navigate().refresh()
    }

    static geb.Browser drive(Closure script) {
        if (!browser) throw new IllegalStateException("Browser not initialized")
        script.delegate = browser
        script()
        browser
    }

    static String getJSession() {
        browser.getDriver().manage().getCookieNamed("JSESSIONID").getValue()
    }
}
