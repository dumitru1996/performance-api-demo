package org.example.ui.browser;

import com.microsoft.playwright.Page;

public interface BaseBrowser {

    void init();

    void close();

    Page getPage();
}

