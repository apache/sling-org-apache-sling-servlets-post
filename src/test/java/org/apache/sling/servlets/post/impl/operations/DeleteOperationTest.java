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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.PostResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class DeleteOperationTest {

    @Test
    public void testDeletingNonExistingResource() throws Exception {

        DeleteOperation deleteOperation = new DeleteOperation();

        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
        RequestProgressTracker requestProgressTracker = Mockito.mock(RequestProgressTracker.class);
        NonExistingResource resource = Mockito.mock(NonExistingResource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);

        Mockito.when(request.getParameter(":operation")).thenReturn("delete");
        Mockito.when(request.getRequestProgressTracker()).thenReturn(requestProgressTracker);
        Mockito.when(request.getResource()).thenReturn(resource);
        Mockito.when(resource.getPath()).thenReturn("/content/pat/123");
        Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(requestPathInfo.getSuffix()).thenReturn(null);
        Mockito.doNothing().when(requestProgressTracker).log(Mockito.anyString(), Mockito.any());

        Method doRun = deleteOperation.getClass().getDeclaredMethod("doRun", SlingHttpServletRequest.class,
                PostResponse.class, List.class);
        doRun.setAccessible(true);

        try {
            doRun.invoke(deleteOperation, request, null, null);
        } catch (Exception e) {
            assertEquals("org.apache.sling.api.resource.ResourceNotFoundException", e.getCause().getClass().getName());
        }
    }

    @Test
    public void testDeletingNonExistingResourcewithApplyTo() throws Exception {

        DeleteOperation deleteOperation = new DeleteOperation();

        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
        RequestProgressTracker requestProgressTracker = Mockito.mock(RequestProgressTracker.class);
        Resource resource = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        NonExistingResource nonExistingResource = Mockito.mock(NonExistingResource.class, Mockito.RETURNS_DEEP_STUBS);
        NonExistingResource nonExistingResource_1 = Mockito.mock(NonExistingResource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(nonExistingResource_1.getPath()).thenReturn("/content/pat/123/existing1");
        NonExistingResource nonExistingResource_2 = Mockito.mock(NonExistingResource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(nonExistingResource_2.getPath()).thenReturn("/content/pat/123/existing2");
        List<Resource> nonExistinResources = Arrays.asList(nonExistingResource_1, nonExistingResource_2);

        Mockito.when(resourceResolver.getResource(resource, "/content/pat/123")).thenReturn(nonExistingResource);
        Mockito.when(nonExistingResource.listChildren()).thenReturn(nonExistinResources.iterator());
        Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
        Mockito.when(request.getParameter(":operation")).thenReturn("delete");
        Mockito.when(request.getParameterValues(":applyTo")).thenReturn(new String[]{"/content/pat/123/*", "/content/serge/123/456", "/content/justin/123/456"});
        Mockito.when(request.getRequestProgressTracker()).thenReturn(requestProgressTracker);
        Mockito.when(request.getResource()).thenReturn(resource);
        Mockito.when(resource.getPath()).thenReturn("/content/pat/123");
        Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(requestPathInfo.getSuffix()).thenReturn(null);
        Mockito.doNothing().when(requestProgressTracker).log(Mockito.anyString(), Mockito.any());

        Method doRun = deleteOperation.getClass().getDeclaredMethod("doRun", SlingHttpServletRequest.class, PostResponse.class, List.class);
        doRun.setAccessible(true);

        try {
            doRun.invoke(deleteOperation, request, null, null);
        } catch (Exception e) {
            assertEquals("org.apache.sling.api.resource.ResourceNotFoundException", e.getCause().getClass().getName());
        }
    }

    @Test
    public void testDeletingExistingResource() throws Exception {

        DeleteOperation deleteOperation = new DeleteOperation();

        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
        RequestProgressTracker requestProgressTracker = Mockito.mock(RequestProgressTracker.class);
        Resource resource = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);

        Mockito.when(request.getParameter(":operation")).thenReturn("delete");
        Mockito.when(request.getRequestProgressTracker()).thenReturn(requestProgressTracker);
        Mockito.when(request.getResource()).thenReturn(resource);
        Mockito.when(resource.getPath()).thenReturn("/content/pat/123");
        Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(requestPathInfo.getSuffix()).thenReturn(null);
        Mockito.doNothing().when(requestProgressTracker).log(Mockito.anyString(), Mockito.any());

        Method doRun = deleteOperation.getClass().getDeclaredMethod("doRun", SlingHttpServletRequest.class,
                PostResponse.class, List.class);
        doRun.setAccessible(true);

        List<Modification> changes = new ArrayList<>();
        doRun.invoke(deleteOperation, request, null, changes);
        assertEquals(1, changes.size());
        Modification modification = changes.get(0);
        assertEquals(ModificationType.DELETE, modification.getType());
        assertEquals("/content/pat/123", modification.getSource());
    }

    @Test
    public void testDeletingExistingResourcewithApplyTo() throws Exception {

        DeleteOperation deleteOperation = new DeleteOperation();

        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
        RequestProgressTracker requestProgressTracker = Mockito.mock(RequestProgressTracker.class);
        Resource resource = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        Resource existingResource = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        Resource existingResource_1 = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(existingResource_1.getPath()).thenReturn("/content/pat/123/existing1");
        Resource existingResource_2 = Mockito.mock(Resource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(existingResource_2.getPath()).thenReturn("/content/pat/123/existing2");
        List<Resource> existingResources = Arrays.asList(existingResource_1, existingResource_2);

        Mockito.when(resourceResolver.getResource(resource, "/content/pat/123")).thenReturn(existingResource);
        Mockito.when(existingResource.listChildren()).thenReturn(existingResources.iterator());
        Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
        Mockito.when(request.getParameter(":operation")).thenReturn("delete");
        Mockito.when(request.getParameterValues(":applyTo")).thenReturn(new String[]{"/content/pat/123/*", "/content/serge/123/456", "/content/justin/123/456"});
        Mockito.when(request.getRequestProgressTracker()).thenReturn(requestProgressTracker);
        Mockito.when(request.getResource()).thenReturn(resource);
        Mockito.when(resource.getPath()).thenReturn("/content/pat/123");
        Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(requestPathInfo.getSuffix()).thenReturn(null);
        Mockito.doNothing().when(requestProgressTracker).log(Mockito.anyString(), Mockito.any());

        Method doRun = deleteOperation.getClass().getDeclaredMethod("doRun", SlingHttpServletRequest.class, PostResponse.class, List.class);
        doRun.setAccessible(true);

        List<Modification> changes = new ArrayList<>();
        doRun.invoke(deleteOperation, request, null, changes);
        assertEquals(2, changes.size());
        Modification modification1 = changes.get(0);
        assertEquals(ModificationType.DELETE, modification1.getType());
        assertEquals("/content/pat/123/existing1", modification1.getSource());
        Modification modification2 = changes.get(1);
        assertEquals(ModificationType.DELETE, modification2.getType());
        assertEquals("/content/pat/123/existing2", modification2.getSource());
    }

}
