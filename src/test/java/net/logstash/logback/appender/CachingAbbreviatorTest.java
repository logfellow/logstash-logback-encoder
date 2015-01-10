package net.logstash.logback.appender;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.logstash.logback.CachingAbbreviator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.classic.pattern.Abbreviator;

@RunWith(MockitoJUnitRunner.class)
public class CachingAbbreviatorTest {
    
    @Mock
    private Abbreviator delegate;
    
    @Test
    public void test() {
        when(delegate.abbreviate("full")).thenReturn("abbreviation");
        
        CachingAbbreviator abbreviator = new CachingAbbreviator(delegate);
        
        Assert.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        Assert.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        
        verify(delegate, times(1)).abbreviate("full");
        
        abbreviator.clear();
        
        Assert.assertEquals("abbreviation", abbreviator.abbreviate("full"));
        
        verify(delegate, times(2)).abbreviate("full");
    }

}
