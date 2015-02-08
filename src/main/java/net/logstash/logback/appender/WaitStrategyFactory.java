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
package net.logstash.logback.appender;

import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

/**
 * Creates {@link WaitStrategy} objects from strings.
 */
public class WaitStrategyFactory {
    
    /**
     * Creates a {@link WaitStrategy} from a string.
     * <p>
     * The following strategies are supported:
     * <p>
     * <ul>
     * <li><tt>blocking</tt> - {@link BlockingWaitStrategy}</li>
     * <li><tt>busySpin</tt> - {@link BusySpinWaitStrategy}</li>
     * <li><tt>liteBlocking</tt> - {@link LiteBlockingWaitStrategy}</li>
     * <li><tt>sleeping</tt> - {@link SleepingWaitStrategy}</li>
     * <li><tt>yielding</tt> - {@link YieldingWaitStrategy}</li>
     * <li><tt>phasedBackoff{spinTimeout,yieldTimeout,timeUnit,fallackStrategy}</tt> - {@link PhasedBackoffWaitStrategy}
     *         - <tt>spinTimeout</tt> and <tt>yieldTimeout</tt> are long values.
     *           <tt>timeUnit</tt> is a string name of one of the {@link TimeUnit} values.
     *           <tt>fallbackStrategy</tt> is a wait strategy string (e.g. <tt>blocking</tt>).
     * </li>
     * <li><tt>timeoutBlocking{timeout,timeUnit}</tt> - {@link TimeoutBlockingWaitStrategy}
     *         - <tt>timeout</tt> is a long value.
     *           <tt>timeUnit</tt> is a string name of one of the {@link TimeUnit} values.
     * </li>
     * </ul>
     * 
     * @throws IllegalArgumentException if an unknown wait strategy type is given, or the parameters are unable to be parsed.
     */
    public static WaitStrategy createWaitStrategyFromString(String waitStrategyType) {
        if (waitStrategyType == null) {
            return null;
        }
        waitStrategyType = waitStrategyType.trim().toLowerCase();
        if (waitStrategyType.isEmpty()) {
            return null;
        }
        if (waitStrategyType.equals("blocking")) {
            return new BlockingWaitStrategy();
        }
        if (waitStrategyType.equals("busyspin")) {
            return new BusySpinWaitStrategy();
        }
        if (waitStrategyType.equals("liteblocking")) {
            return new LiteBlockingWaitStrategy();
        }
        if (waitStrategyType.equals("sleeping")) {
            return new SleepingWaitStrategy();
        }
        if (waitStrategyType.equals("yielding")) {
            return new YieldingWaitStrategy();
        }
        if (waitStrategyType.startsWith("phasedbackoff")) {
            Object[] params = parseParams(waitStrategyType, Long.class, Long.class, TimeUnit.class, WaitStrategy.class);
            return new PhasedBackoffWaitStrategy((Long) params[0], (Long) params[1], (TimeUnit) params[2], (WaitStrategy) params[3]);
        }
        if (waitStrategyType.startsWith("timeoutblocking")) {
            Object[] params = parseParams(waitStrategyType, Long.class, TimeUnit.class);
            return new TimeoutBlockingWaitStrategy((Long) params[0], (TimeUnit) params[1]);
        }
        
        throw new IllegalArgumentException("Unknown wait strategy type: " + waitStrategyType);
        
    }

    private static Object[] parseParams(String waitStrategyType, Class<?>... paramTypes) {
        int startIndex = waitStrategyType.indexOf('{');
        if (startIndex == -1) {
            throw new IllegalArgumentException(
                    String.format("%d parameters must be provided for waitStrategyType %s."
                            + " None were provided."
                            + " To provide parameters, add a comma separated value list within curly braces ({}) to the end of the waitStrategyType string.",
                            paramTypes.length,
                            waitStrategyType));
        }
        int endIndex = waitStrategyType.lastIndexOf('}');
        if (endIndex == -1) {
            throw new IllegalArgumentException(String.format("Parameters of %s must end with '}'", waitStrategyType));
        }
        
        String[] paramStrings = waitStrategyType.substring(startIndex + 1, endIndex).split(",");
        if (paramStrings.length != paramTypes.length) {
            throw new IllegalArgumentException(String.format("%d parameters must be provided for waitStrategyType %s. Only %d were provided.", paramTypes.length, waitStrategyType, paramStrings.length));
        }
        
        Object[] params = new Object[paramStrings.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (Long.class.equals(paramTypes[i])) {
                params[i] = Long.valueOf(paramStrings[i].trim());
            } else if (TimeUnit.class.equals(paramTypes[i])) {
                params[i] = TimeUnit.valueOf(paramStrings[i].trim().toUpperCase());
            } else if (WaitStrategy.class.equals(paramTypes[i])) {
                params[i] = createWaitStrategyFromString(paramStrings[i].trim());
            } else {
                throw new IllegalArgumentException("Unknown paramType " + paramTypes[i]);
            }
        }
        
        return params;
    }

}
