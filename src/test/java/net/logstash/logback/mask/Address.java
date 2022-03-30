/*
 * Copyright 2013-2021 the original author or authors.
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
package net.logstash.logback.mask;

@LogMaskFields
public class Address {
    @LogMask
    private final String streetAddress;

    private final String city;

    public Address(String streetAddress, String city) {
        this.streetAddress = streetAddress;
        this.city = city;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }
}
