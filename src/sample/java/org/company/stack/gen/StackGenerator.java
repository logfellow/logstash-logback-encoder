package org.company.stack.gen;

public class StackGenerator {
    public static void generateSingle() {
        oneSingle();
    }
    public static void oneSingle() {
        twoSingle();
    }
    private static void twoSingle() {
        threeSingle();
    }
    private static void threeSingle() {
        one();
    }
    private static void four() {
        five();
    }
    private static void five() {
        six();
    }
    private static void six() {
        seven();
    }
    private static void seven() {
        eight();
    }
    private static void eight() {
        throw new RuntimeException("message");
    }
    
    
    public static void generateCausedBy() {
        causedBy();
    }
    private static void causedBy() {
        try {
            one();
        } catch (RuntimeException e) {
            throw new RuntimeException("Unable to invoke service", e);
        }
    }
    private static void causeByOne() {
        causedByTwo();
    }
    private static void causedByTwo() {
        try {
            seven();
        } catch (RuntimeException e) {
            throw new RuntimeException("wrapper", e);
        }
    }
    
    private static void one() {
        two();
    }
    private static void two() {
        throw new RuntimeException("Destination unreachable");
    }
    
    public static void generateSuppressed() {
        oneSuppressed();
    }
    private static void oneSuppressed() {
        twoSuppressed();
    }
    private static void twoSuppressed() {
        try {
            threeSingle();
        } catch (RuntimeException e) {
            RuntimeException newException = new RuntimeException();
            newException.addSuppressed(e);
            throw newException;
        }
    }
}
