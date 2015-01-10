package net.logstash.logback;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.pattern.Abbreviator;

/**
 * An {@link Abbreviator} that caches results from a {@link #delegate} abbreviator.
 * 
 * Logger names are typically reused constantly, so caching abbreviations
 * of class names helps performance.
 */
public class CachingAbbreviator implements Abbreviator {
    
    private final Abbreviator delegate;
    
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<String, String>();
    
    public CachingAbbreviator(Abbreviator delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String abbreviate(String in) {
        String abbreviation = cache.get(in);
        if (abbreviation == null) {
            abbreviation = delegate.abbreviate(in);
            cache.putIfAbsent(in, abbreviation);
        }
        return abbreviation;
    }
    
    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
    }

}
