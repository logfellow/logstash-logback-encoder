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
package net.logstash.logback.decorate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * Combines a list of decorators into a single decorator, so multiple decorators can be used together.
 */
public class CompositeDecorator<T, D extends Decorator<T>> implements Decorator<T> {
    
    private final List<D> decorators = new CopyOnWriteArrayList<>();

    public T decorate(T decoratable) {
        T decorated = decoratable;
        for (Decorator<T> decorator : decorators) {
            decorated = decorator.decorate(decorated);
        }
        return decorated;
    }

    public void addDecorator(D decorator) {
        decorators.add(decorator);
    }
    
    public boolean removeDecorator(D decorator) {
        return decorators.remove(decorator);
    }

}
