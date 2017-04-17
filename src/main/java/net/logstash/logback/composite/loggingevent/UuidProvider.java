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
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

import java.io.IOException;

public class UuidProvider extends AbstractFieldJsonProvider<ILoggingEvent>
{
    public static final String FIELD_UUID = "uuid";
    private NoArgGenerator uuids = Generators.randomBasedGenerator();

    private String strategy = "random";  // random | time
    private String ethernet;             // 'interface' | ethernet MAC address for time based strategy

    public UuidProvider()
    {
        setFieldName(FIELD_UUID);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) throws IOException
    {
        JsonWritingUtils.writeStringField(generator, getFieldName(), uuids.generate().toString());
    }

    public String getStrategy()
    {
        return strategy;
    }

    public void setStrategy(String strategy)
    {
        this.strategy = strategy;

        uuids = newUuidStrategy(strategy, ethernet);
    }

    public String getEthernet()
    {
        return ethernet;
    }

    public void setEthernet(String ethernet)
    {
        this.ethernet = ethernet;

        uuids = newUuidStrategy(this.strategy, this.ethernet);
    }

    private NoArgGenerator newUuidStrategy(String strategy, String ethernet)
    {
        if(strategy.equalsIgnoreCase("time"))
        {
            return newTimeBasedGenerator(ethernet);
        }

        //default to 'random' type-4 uuid generator
        return Generators.randomBasedGenerator();
    }

    private TimeBasedGenerator newTimeBasedGenerator(String ethernet)
    {
        if(ethernet == null)
        {
            return Generators.timeBasedGenerator();
        }

        if("interface".equalsIgnoreCase(ethernet))
        {
            return Generators.timeBasedGenerator(EthernetAddress.fromInterface());
        }

        return Generators.timeBasedGenerator(EthernetAddress.valueOf(ethernet));
    }
}
