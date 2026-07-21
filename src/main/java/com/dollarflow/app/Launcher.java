package com.dollarflow.app;

/**
 * Entry point for the packaged jar/installer. Launching a shaded jar with a Main-Class that
 * extends javafx.application.Application directly fails with "JavaFX runtime components are
 * missing" because java checks the module path before the class is even loaded. Routing through
 * a plain class here sidesteps that check.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
