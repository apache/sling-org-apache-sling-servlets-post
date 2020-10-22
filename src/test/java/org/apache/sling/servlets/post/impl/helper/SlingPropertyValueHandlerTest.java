/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Test;
import org.mockito.Mockito;

public class SlingPropertyValueHandlerTest {

    @Test
    public void testEmptyPropertyValueWithTypeLong() throws Exception {
        final List<Modification> mods = new ArrayList<Modification>();
        final JCRSupport support = new JCRSupport();

        final SlingPropertyValueHandler handler = new SlingPropertyValueHandler(new DateParser(), support, mods);

        final Session jcrSession = Mockito.mock(Session.class);

        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resolver.adaptTo(Session.class)).thenReturn(jcrSession);

        final Node node = Mockito.mock(Node.class);
        final Property jcrProp = Mockito.mock(Property.class);
        Mockito.when(node.getProperty("property")).thenReturn(jcrProp);
        final PropertyDefinition jcrPropDef = Mockito.mock(PropertyDefinition.class);
        Mockito.when(jcrProp.getDefinition()).thenReturn(jcrPropDef);
        Mockito.when(jcrPropDef.isMandatory()).thenReturn(false);
        // throw exception for previous behaviour
        Mockito.when(node.setProperty("property", "", 3)).thenThrow(new RepositoryException());

        final Resource rsrc = Mockito.mock(Resource.class);
        final ModifiableValueMap valueMap = new ModifiableValueMapDecorator(new HashMap<String, Object>());
        valueMap.put("property", 7L);

        Mockito.when(rsrc.getPath()).thenReturn("/content");
        Mockito.when(rsrc.getName()).thenReturn("content");
        Mockito.when(rsrc.adaptTo(Node.class)).thenReturn(node);
        Mockito.when(rsrc.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);
        Mockito.when(rsrc.getResourceResolver()).thenReturn(resolver);

        final RequestParameter req = Mockito.mock(RequestParameter.class);
        Mockito.when(req.isFormField()).thenReturn(true);
        Mockito.when(req.getName()).thenReturn("property");
        Mockito.when(req.getString()).thenReturn("");

        final RequestProperty prop = new RequestProperty("/content/property");
        prop.setTypeHintValue("Long");
        prop.setValues(new RequestParameter[] { req });

        handler.setProperty(rsrc, prop);

        // value map should be empty, one change: delete
        assertTrue(valueMap.isEmpty());
        assertEquals(1, mods.size());
        assertEquals(ModificationType.DELETE, mods.get(0).getType());
        assertEquals("/content/property", mods.get(0).getSource());
    }

    @Test
    public void testEmptyPropertyValueWithoutType() throws Exception {
        final List<Modification> mods = new ArrayList<Modification>();
        final JCRSupport support = new JCRSupport();

        final SlingPropertyValueHandler handler = new SlingPropertyValueHandler(new DateParser(), support, mods);

        final Session jcrSession = Mockito.mock(Session.class);

        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resolver.adaptTo(Session.class)).thenReturn(jcrSession);

        final Node node = Mockito.mock(Node.class);
        final Property jcrProp = Mockito.mock(Property.class);
        Mockito.when(node.getProperty("property")).thenReturn(jcrProp);
        final PropertyDefinition jcrPropDef = Mockito.mock(PropertyDefinition.class);
        Mockito.when(jcrProp.getDefinition()).thenReturn(jcrPropDef);
        Mockito.when(jcrPropDef.isMandatory()).thenReturn(false);
        // throw exception for previous behaviour
        Mockito.when(node.setProperty("property", "", 3)).thenThrow(new RepositoryException());

        final Resource rsrc = Mockito.mock(Resource.class);
        final ModifiableValueMap valueMap = new ModifiableValueMapDecorator(new HashMap<String, Object>());
        valueMap.put("property", "hello");

        Mockito.when(rsrc.getPath()).thenReturn("/content");
        Mockito.when(rsrc.getName()).thenReturn("content");
        Mockito.when(rsrc.adaptTo(Node.class)).thenReturn(node);
        Mockito.when(rsrc.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);
        Mockito.when(rsrc.getResourceResolver()).thenReturn(resolver);

        final RequestParameter req = Mockito.mock(RequestParameter.class);
        Mockito.when(req.isFormField()).thenReturn(true);
        Mockito.when(req.getName()).thenReturn("property");
        Mockito.when(req.getString()).thenReturn("");

        final RequestProperty prop = new RequestProperty("/content/property");
        prop.setValues(new RequestParameter[] { req });

        handler.setProperty(rsrc, prop);

        // value map should be empty, one change: delete
        assertTrue(valueMap.isEmpty());
        assertEquals(1, mods.size());
        assertEquals(ModificationType.DELETE, mods.get(0).getType());
        assertEquals("/content/property", mods.get(0).getSource());
    }
}
