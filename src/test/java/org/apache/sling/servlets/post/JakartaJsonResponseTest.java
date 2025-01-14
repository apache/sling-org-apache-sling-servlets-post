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

package org.apache.sling.servlets.post;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.request.builder.SlingJakartaHttpServletResponseResult;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;

public class JakartaJsonResponseTest extends TestCase {
    protected JakartaJSONResponse res;

    public void setUp() throws Exception {
        res = new JakartaJSONResponse();
        super.setUp();
    }

    public void testOnChange() throws Exception {
        res.onChange("modified", "argument1", "argument2");
        Object prop = res.getProperty("changes");
        JsonArray changes = assertInstanceOf(prop, JsonArray.class);
        assertEquals(1, changes.size());
        Object obj = changes.getJsonObject(0);
        JsonObject change = assertInstanceOf(obj, JsonObject.class);
        assertProperty(change, JakartaJSONResponse.PROP_TYPE, "modified");
        JsonArray arguments = change.getJsonArray(JakartaJSONResponse.PROP_ARGUMENT);
        assertEquals(2, arguments.size());
    }

    public void testSetProperty() throws Exception {
        res.setProperty("prop", "value");
        assertProperty(res.getJson(), "prop", "value");
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testSetError() throws IOException {
        String errMsg = "Dummy error";
        res.setError(new Error(errMsg));
        SlingJakartaHttpServletResponseResult resp = Builders.newResponseBuilder().buildJakartaResponseResult();
        res.send(resp, true);
        JsonObject json = res.getJson();
        JsonValue error = assertProperty(json, "error");
        assertProperty((JsonObject) error, "class", Error.class.getName());
        assertProperty((JsonObject) error, "message", errMsg);
    }

    public void testSend() throws Exception {
        res.onChange("modified", "argument1");
        SlingJakartaHttpServletResponseResult response = Builders.newResponseBuilder().buildJakartaResponseResult();
        res.send(response, true);
        JsonObject result = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertProperty(result, JakartaHtmlResponse.PN_STATUS_CODE, HttpServletResponse.SC_OK);
        assertEquals(JakartaJSONResponse.RESPONSE_CONTENT_TYPE + ";charset=" + JakartaJSONResponse.RESPONSE_CHARSET, response.getContentType());
        assertEquals(JakartaJSONResponse.RESPONSE_CHARSET, response.getCharacterEncoding());
    }

    public void testSend_201() throws Exception {
        final String location = "http://example.com/test_location";
        res.onChange("modified", "argument1");
        res.setStatus(HttpServletResponse.SC_CREATED, "Created");
        res.setLocation(location);
        SlingJakartaHttpServletResponseResult response = Builders.newResponseBuilder().buildJakartaResponseResult();
        res.send(response, true);
        JsonObject result = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertProperty(result, JakartaHtmlResponse.PN_STATUS_CODE, HttpServletResponse.SC_CREATED);
        assertEquals(location, response.getHeader("Location"));
    }

    public void testSend_3xx() throws Exception {
        final String location = "http://example.com/test_location";
        res.onChange("modified", "argument1");

        for (int status = 300; status < 308; status++) {
            res.setStatus(status, "3xx Status");
            res.setLocation(location);
            SlingJakartaHttpServletResponseResult response = Builders.newResponseBuilder().buildJakartaResponseResult();
            res.send(response, true);
            JsonObject result = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
            assertProperty(result, JakartaHtmlResponse.PN_STATUS_CODE, status);
            assertEquals(location, response.getHeader("Location"));
        }
    }

    public void testNoChangesOnError() throws Exception {
        res.onChange("modified", "argument1");
        res.setError(new Exception("some exception"));
        JsonObject obj = res.getJson();
        assertTrue(obj.containsKey("changes"));
        assertEquals(0, obj.getJsonArray("changes").size());
    }

    public void testSendWithJsonAsPropertyValue() throws Exception {
        String testResponseJson = "{\"user\":\"testUser\",\"properties\":{\"id\":\"testId\", \"name\":\"test\"}}";
        JsonObject customProperty = Json.createReader(new StringReader(testResponseJson)).readObject();
        res.setProperty("response", customProperty);
        SlingJakartaHttpServletResponseResult response = Builders.newResponseBuilder().buildJakartaResponseResult();
        res.send(response, true);
        JsonObject result = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertProperty(result, "response", customProperty);
    }

    private static JsonValue assertProperty(JsonObject obj, String key) {
        assertTrue("JSON object does not have property " + key, obj.containsKey(key));
        return obj.get(key);
    }

    private static JsonValue assertProperty(JsonObject obj, String key, int expected) {
        JsonNumber res = (JsonNumber) assertProperty(obj, key);
        assertEquals(expected, res.intValue());
        return res;
    }

    private static JsonValue assertProperty(JsonObject obj, String key, String expected) {
        JsonString res = (JsonString) assertProperty(obj, key);
        assertEquals(expected, res.getString());
        return res;
    }

    private static JsonValue assertProperty(JsonObject obj, String key, JsonObject expected) {
        JsonObject res = (JsonObject) assertProperty(obj, key);
        assertEquals(expected, res);
        return res;
    }

    @SuppressWarnings({"unchecked"})
    private static <T> T assertInstanceOf(Object obj, Class<T> clazz) {
        try {
            return (T) obj;
        } catch (ClassCastException e) {
            TestCase.fail("Object is of unexpected type. Expected: " + clazz.getName() + ", actual: " + obj.getClass().getName());
            return null;
        }
    }
}
