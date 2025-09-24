/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.servlets.post.impl.operations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.servlets.post.AbstractJakartaPostResponse;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.exceptions.PreconditionViolatedPersistenceException;
import org.apache.sling.servlets.post.exceptions.TemporaryPersistenceException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Execute with maven-failsafe-plugin as this requires a slightly patched classpath
 * to make the test runnable with Java 17+ which requires a newer Oak version than we are supporting with the actual bundle
 */
public class ModifyOperationIT {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    /**
     * A resource decorator that decorates the resource to add a property "foo" with value "bar"
     * to the modifiable value map which is not backed by the JCR node.
     * This only happens when the underlying resource does not have the property "foo" yet.
     */
    private static final class TestResourceDecorator implements ResourceDecorator {

        @Override
        public Resource decorate(@NotNull Resource resource) {
            return new ResourceWrapper(resource) {

                public @NotNull ModifiableValueMap getModifiableValueMap() {
                    ModifiableValueMap originalValueMap = getResource().adaptTo(ModifiableValueMap.class);
                    Map<String, Object> properties = new HashMap<>(originalValueMap);
                    if (!properties.containsKey("foo")) {
                        properties.put("foo", "bar");
                    }
                    // make sure that write operations on the ModifiableValueMap are passed to the underlying resource
                    return new ModifiableValueMapDecorator(properties) {
                        @Override
                        public Object put(String key, Object value) {
                            return originalValueMap.put(key, value);
                        }
                    };
                }

                @Override
                public @NotNull ValueMap getValueMap() {
                    return getModifiableValueMap();
                }

                @SuppressWarnings("unchecked")
                @Override
                public <AdapterType> AdapterType adaptTo(@NotNull Class<AdapterType> type) {
                    if (type == ModifiableValueMap.class) {
                        return (AdapterType) getModifiableValueMap();
                    } else if (type == ValueMap.class) {
                        return (AdapterType) getValueMap();
                    }
                    return super.adaptTo(type);
                }
            };
        }
    }

    @Test
    public void testModifyingPropertyNotBackedByJcrNode()
            throws PreconditionViolatedPersistenceException, TemporaryPersistenceException, PersistenceException {
        ModifyOperation op = new ModifyOperation();
        context.registerService(ResourceDecorator.class, new TestResourceDecorator());
        Resource resource = context.create().resource("/test", "prop1", "value1");
        SlingJakartaHttpServletRequest request = Builders.newRequestBuilder(resource)
                .withParameter("foo", "bar2")
                .buildJakartaRequest();

        JakartaPostResponse response = new AbstractJakartaPostResponse() {
            @Override
            protected void doSend(HttpServletResponse response) throws IOException {}

            @Override
            public void onChange(String type, String... arguments) {}

            @Override
            public String getPath() {
                return "/test";
            }
        };
        final List<Modification> changes = new java.util.ArrayList<>();
        op.doRun(request, response, changes);

        assertEquals(1, changes.size());
        resource = context.resourceResolver().getResource("/test");
        assertEquals("bar2", resource.getValueMap().get("foo", String.class));
        assertEquals("value1", resource.getValueMap().get("prop1", String.class));
    }
}
