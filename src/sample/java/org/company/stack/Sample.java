package org.company.stack;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.company.stack.framework.Dispatcher;
import org.company.stack.gen.StackGenerator;

public class Sample {

    public static void main(String[] args) {
        exclude();
    }

    private static void omitCommonFrames() {
        execute(
                StackGenerator::generateCausedBy,
                c -> c.setOmitCommonFrames(true)
        );
    }
    
    
    private static void maxDepthPerThroable() {
        execute(
                StackGenerator::generateCausedBy,
                c -> c.setMaxDepthPerThrowable(2)
        );
    }
    
    private static void truncateAfter() {
        execute(
                StackGenerator::generateCausedBy,
                c -> {
                    c.addTruncateAfter("^org\\.company\\.stack\\.framework\\..*");
                    c.addTruncateAfter("\\.StackGenerator\\.one");
                }
        );
    }
    
    private static void maxLength() {
        execute(
                StackGenerator::generateCausedBy,
                c -> {
                    c.setMaxLength(256);
                }
        );
    }
    
    private static void lineSeparator() {
        execute(
                StackGenerator::generateCausedBy,
                c -> {
                    c.setLineSeparator("|");
                }
        );
    }
    
    
    private static void rootCauseFirst() {
        execute(
                StackGenerator::generateCausedBy,
                c -> {
                    c.setRootCauseFirst(true);
                }
        );
    }
    
    
    private static void classNameShortening() {
        execute(
                StackGenerator::generateCausedBy,
                c -> {
                    c.setShortenedClassNameLength(25);
                }
        );
    }
    
    private static void shortClassNamesSample() {
        String s = "org.company.stack.Sample";
        for (int i = 0; i < 30; i++) {
            System.out.println(i + ":   " + new TargetLengthBasedClassNameAbbreviator(i).abbreviate(s));
        }
    }
    
    
    private static void exclude() {
        execute(
                StackGenerator::generateSingle,
                c -> {
                    c.addExclude("Single$");
                    c.addExclude("^org\\.company\\.stack\\.Sample\\.*");
                    c.addExclude("execute$");
                }
        );
    }
    
    // --------------------------------------------------------------------------------------------
    
    private static void execute(Runnable gen, Consumer<ShortenedThrowableConverter> configurator) {
        try {
            Dispatcher.dispatch(gen);
        }
        catch (Throwable e) {
            e.printStackTrace();
            
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            configurator.accept(converter);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            System.out.println(formatted);
        }
    }
    
    private static ILoggingEvent createEvent(Throwable e) {
        return createEvent(new ThrowableProxy(e));
    }
    
    private static ILoggingEvent createEvent(ThrowableProxy proxy) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getThrowableProxy()).thenReturn(proxy);
        return event;
    }
}
