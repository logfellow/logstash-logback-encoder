package org.company.stack.framework;

public class Dispatcher {
    public static void handle(Runnable r) {
        dispatch(r);
    }
    public static void dispatch(Runnable r) {
        invoke(r);
    }
    private static void invoke(Runnable r) {
        r.run();
    }
}
