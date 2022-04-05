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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Simple JUnit5 test extension logging the name of each test before it starts to help
 * troubleshooting output when running multiple tests at once.
 */
public class TestExecutionLogger implements BeforeEachCallback, BeforeAllCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        
        StringBuilder sb = new StringBuilder("---- Running ");
        if (context.getTestClass().isPresent()) {
            sb.append(context.getTestClass().get().getName());
        }
        sb.append("#")
          .append(context.getDisplayName());
        
        System.out.println(sb);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.out.println("---- Initializing " + context.getTestClass().orElse(null));
    }
}
