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

import java.util.Collections;
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

    protected void assertDefaultName(Map<String, Object> parameterMap) {
        assertDefaultName(parameterMap, false);
    }
    protected void assertDefaultName(Map<String, Object> parameterMap, boolean requirePrefix) {
        String nodeName = nodeName(parameterMap, requirePrefix);
        assertTrue(nodeName.matches("\\d+_\\d+"));
    }

    protected void assertExpectedName(Map<String, Object> parameterMap, String expectedName) {
        assertExpectedName(parameterMap, expectedName, false);
    }
    protected void assertExpectedName(Map<String, Object> parameterMap, String expectedName, boolean requirePrefix) {
        String nodeName = nodeName(parameterMap, requirePrefix);
        assertEquals(expectedName, nodeName);
    }

    @Test
    public void testNameDefault() {
        assertDefaultName(Collections.singletonMap("message", "Hello"));
    }

    @Test
    public void testNameHint() {
        assertExpectedName(Collections.singletonMap(":nameHint", "Hello"), "hello");
    }

    @Test
    public void testNameHintEmpty() {
        assertDefaultName(Collections.singletonMap(":nameHint", ""));
    }

    @Test
    public void testName() {
        assertExpectedName(Collections.singletonMap(":name", "Hello"), "Hello");
    }

    @Test
    public void testNameEmpty() {
        assertDefaultName(Collections.singletonMap(":name", ""));
    }

    @Test
    public void testNameHintTrimmed() {
        assertExpectedName(Collections.singletonMap(":nameHint", "HelloWorldTooLong"), "helloworld");
    }

    @Test
    public void testNameFromTitle() {
        assertExpectedName(Collections.singletonMap("title", "Hello"), "hello");
    }

    @Test
    public void testNameFromTitleEmpty() {
        assertDefaultName(Collections.singletonMap("title", ""));
    }

    @Test
    public void testNameFromTitleWithPrefix() {
        // 1. should not find any param and fallback to the default
        assertDefaultName(Collections.singletonMap("title", "Hello"), true);

        // 2. should find a param and use it
        assertExpectedName(Collections.singletonMap("./title", "Hello"), "hello", true);
    }

    @Test
    public void testNameFromSubject() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "Hello");
        map.put("sling:subject", "World");
        assertExpectedName(map, "world");
    }

    @Test
    public void testNameHintFilter() {
        assertExpectedName(Collections.singletonMap(":nameHint", "H$lloW#rld"), "h_llow_rld");
    }

    /**
     * SLING-10610
     */
    @Test
    public void testNameHintValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint@ValueFrom", "message");
        map.put("message", "Hello");
        assertExpectedName(map, "hello");
    }

    @Test
    public void testNameHintValueFromEmpty() {
        assertDefaultName(Collections.singletonMap(":nameHint@ValueFrom", ""));
    }

    @Test
    public void testNameHintValueFromEmptyRef() {
        Map<String, Object> map = new HashMap<>();
        map.put(":nameHint@ValueFrom", "message");
        map.put("message", "");
        assertDefaultName(map);
    }

    /**
     * SLING-10610
     */
    @Test
    public void testNameValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put(":name@ValueFrom", "message");
        map.put("message", "Hello");
        assertExpectedName(map, "Hello");
    }

    @Test
    public void testNameFromSubjectValueFrom() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "Hello");
        map.put("sling:subject@ValueFrom", "sling:message");
        assertExpectedName(map, "hello");
    }

    @Test
    public void testNameFromSubjectValueFromEmpty() {
        assertDefaultName(Collections.singletonMap("sling:subject@ValueFrom", ""));
    }

    @Test
    public void testNameFromSubjectValueFromEmptyRef() {
        Map<String, Object> map = new HashMap<>();
        map.put("sling:message", "");
        map.put("sling:subject@ValueFrom", "sling:message");
        assertDefaultName(map);
    }

}
