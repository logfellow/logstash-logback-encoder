/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.composite.accessevent;

import org.junit.Assert;
import org.junit.Test;

public class IncludeExcludeHeaderFilterTest {
    
    private IncludeExcludeHeaderFilter filter = new IncludeExcludeHeaderFilter();
    
    @Test
    public void testEmpty() {
        Assert.assertTrue(filter.includeHeader("HeaderA", "1"));
        Assert.assertTrue(filter.includeHeader("Headera", "2"));
        Assert.assertTrue(filter.includeHeader("HeaderB", "3"));
        Assert.assertTrue(filter.includeHeader("Headerb", "4"));
    }
    @Test
    public void testIncludeOnly() {
        filter.addInclude("headera");
        Assert.assertTrue(filter.includeHeader("HeaderA", "1"));
        Assert.assertTrue(filter.includeHeader("Headera", "2"));
        Assert.assertFalse(filter.includeHeader("HeaderB", "3"));
        Assert.assertFalse(filter.includeHeader("Headerb", "3"));
    }

    @Test
    public void testExcludeOnly() {
        filter.addExclude("headera");
        Assert.assertFalse(filter.includeHeader("HeaderA", "1"));
        Assert.assertFalse(filter.includeHeader("Headera", "2"));
        Assert.assertTrue(filter.includeHeader("HeaderB", "3"));
        Assert.assertTrue(filter.includeHeader("Headerb", "4"));
    }

    @Test(expected = IllegalStateException.class)
    public void testBoth() {
        filter.addInclude("headera");
        filter.addExclude("headerb");
        filter.includeHeader("HeaderA", "1");
    }

}
