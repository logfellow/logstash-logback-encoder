/*
 * Copyright 2013-2022 the original author or authors.
 *
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
package net.logstash.logback.test;

import static org.mockito.Mockito.spy;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author brenuart
 *
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractLogbackTest {

    protected LoggerContext context = spy(new LoggerContext());
    
    protected StatusManager statusManager = new BasicStatusManager();

    
    @BeforeEach
    public void setup() throws Exception {
        // Output statuses on the console for easy debugging. Must be initialized early to capture
        // warnings emitted by setter/getter methods before the appender is started.
        OnConsoleStatusListener consoleListener = new OnConsoleStatusListener();
        consoleListener.start();
        statusManager.add(consoleListener);
        context.setStatusManager(statusManager);
        
        context.start();
    }
    
    @AfterEach
    public void tearDown() {
        context.stop();
    }

}
