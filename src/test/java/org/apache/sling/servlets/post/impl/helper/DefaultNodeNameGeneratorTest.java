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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.junit.Test;

/**
 * Test node name generator
 */
public class DefaultNodeNameGeneratorTest {

    protected String nodeName(Map<String, Object> parameterMap) {
        return nodeName(parameterMap, false);
    }
    protected String nodeName(Map<String, Object> parameterMap, boolean requirePrefix) {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(null);
        request.setParameterMap(parameterMap);

        String basePath = null;
        NodeNameGenerator defaultNodeNameGenerator = null;

        String[] parameterNames = new String[] {
                "title",
                "sling:subject"
            };
        int maxNameLength = 10;
        NodeNameGenerator nodeNameGenerator = new DefaultNodeNameGenerator(
                parameterNames, 
                maxNameLength);
        return nodeNameGenerator.getNodeName(request, basePath, requirePrefix, defaultNodeNameGenerator);
    }

    @Test
    public void testNameDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("message", "Hello");
        String nodeName = nodeName(map);
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testNameHint() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint", "Hello");
        String nodeName = nodeName(map);
        assertEquals("hello", nodeName);
    }

    @Test
    public void testNameHintEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testName() {
        Map<String, Object> map = new HashMap<>();
        map.put(":name", "Hello");
        String nodeName = nodeName(map);
        assertEquals("Hello", nodeName);
    }

    @Test
    public void testNameEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put(":name", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testNameHintTrimmed() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint", "HelloWorldTooLong");
        String nodeName = nodeName(map);
        assertEquals("helloworld", nodeName);
    }

    @Test
    public void testNameFromTitle() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Hello");
        String nodeName = nodeName(map);
        assertEquals("hello", nodeName);
    }

    @Test
    public void testNameFromTitleEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testNameFromTitleWithPrefix() {
        // 1. should not find any param and fallback to the default
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Hello");
        String nodeName = nodeName(map, true);
        assertTrue(nodeName.matches("\\d+_\\d+"));

        // 2. should find a param and use it
        map.clear();
        map.put("./title", "Hello");
        nodeName = nodeName(map, true);
        assertEquals("hello", nodeName);
    }

    @Test
    public void testNameFromSubject() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "Hello");
        map.put("sling:subject", "World");
        String nodeName = nodeName(map);
        assertEquals("world", nodeName);
    }

    @Test
    public void testNameHintFilter() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint", "H$lloW#rld");
        String nodeName = nodeName(map);
        assertEquals("h_llow_rld", nodeName);
    }

    /**
     * SLING-10610
     */
    @Test
    public void testNameHintValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint@ValueFrom", "message");
        map.put("message", "Hello");
        String nodeName = nodeName(map);
        assertEquals("hello", nodeName);
    }

    @Test
    public void testNameHintValueFromEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint@ValueFrom", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testNameHintValueFromEmptyRef() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint@ValueFrom", "message");
        map.put("message", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    /**
     * SLING-10610
     */
    @Test
    public void testNameValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put(":name@ValueFrom", "message");
        map.put("message", "Hello");
        String nodeName = nodeName(map);
        assertEquals("Hello", nodeName);
    }

    @Test
    public void testNameFromSubjectValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "Hello");
        map.put("sling:subject@ValueFrom", "sling:message");
        String nodeName = nodeName(map);
        assertEquals("hello", nodeName);
    }

    @Test
    public void testNameFromSubjectValueFromEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:subject@ValueFrom", "");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    @Test
    public void testNameFromSubjectValueFromEmptyRef() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "");
        map.put("sling:subject@ValueFrom", "sling:message");
        String nodeName = nodeName(map);
        // empty name should be skipped and fallback to the default
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

}
