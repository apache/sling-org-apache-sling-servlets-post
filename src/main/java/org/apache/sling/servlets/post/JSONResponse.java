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
package org.apache.sling.servlets.post;


import org.apache.sling.jcr.contentparser.impl.JsonTicksConverter;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>JSONResponse</code> is an {@link AbstractPostResponse} preparing
 * the response in JSON.
 */
public class JSONResponse extends AbstractPostResponse {

    public static final String RESPONSE_CONTENT_TYPE = "application/json";

    // package private because it is used by the unit test
    static final String PROP_TYPE = "type";

    // package private because it is used by the unit test
    static final String PROP_ARGUMENT = "argument";

    // package private because it is used by the unit test
    static final String RESPONSE_CHARSET = "UTF-8";

    private static final String PROP_CHANGES = "changes";

    private Map<String, Object> json = new HashMap<>();

    private Map<String, JsonStructure> jsonCached = new HashMap<>();

    private List<Map<String, Object>> changes = new ArrayList<>();

    private Throwable error;

    public void onChange(String type, String... arguments) {
        Map<String,Object> change = new HashMap<>();
        change.put(PROP_TYPE, type);
        
        if (arguments.length > 1) {
            change.put(PROP_ARGUMENT, Arrays.asList(arguments));
        }
        else if (arguments.length == 1) {
            change.put(PROP_ARGUMENT, arguments[0]);
        }
        changes.add(change);
    }

    @Override
    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public Throwable getError() {
        return this.error;
    }

    /**
     * This method accepts values that correspond  to json primitives or otherwise assumes that the toString() of the value
     * can be parsed as json. If neither is the case it will throw an Exception.
     *
     * Assuming the above holds, it will put the value as json directly into the json value part of the response.
     *
     * @param name name of the property
     * @param value value of the property - either of type {String, Boolean, Number, null}
     *             or the toString() is parseable as json
     * @throws JSONResponseException if the value is not usable
     */
    @Override
    public void setProperty(String name, Object value) {
        if (value instanceof String || value instanceof Boolean || value instanceof Number || value == null) {
            json.put(name, value);
        }
        else {
            try {
                String valueString = JsonTicksConverter.tickToDoubleQuote(value.toString());
                jsonCached.put(name, Json.createReader(new StringReader(valueString)).read());
                json.put(name, value);
            } catch (Exception ex) {
                throw new JSONResponseException(ex);
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        return PROP_CHANGES.equals(name) ? getJson().getJsonArray(PROP_CHANGES) : 
            "error".equals(name) && this.error != null ? getJson().get("error") : json.get(name);
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Override
    protected void doSend(HttpServletResponse response) throws IOException {

        response.setContentType(RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(RESPONSE_CHARSET);

        Json.createGenerator(response.getWriter()).write(getJson()).close();
    }

    JsonObject getJson() {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                jsonBuilder.add(entry.getKey(), (String) entry.getValue());
            }
            else if (value instanceof Boolean) {
                jsonBuilder.add(entry.getKey(), (Boolean) value);
            }
            else if (value instanceof BigInteger) {
                jsonBuilder.add(entry.getKey(), (BigInteger) value);
            }
            else if (value instanceof BigDecimal) {
                jsonBuilder.add(entry.getKey(), (BigDecimal) value);
            }
            else if (value instanceof Byte) {
                jsonBuilder.add(entry.getKey(), (Byte) value);
            }
            else if (value instanceof Short) {
                jsonBuilder.add(entry.getKey(), (Short) value);
            }
            else if (value instanceof Integer) {
                jsonBuilder.add(entry.getKey(), (Integer) value);
            }
            else if (value instanceof Long) {
                jsonBuilder.add(entry.getKey(), (Long) value);
            }
            else if (value instanceof Double) {
                jsonBuilder.add(entry.getKey(), (Double) value);
            }
            else if (value instanceof Float) {
                jsonBuilder.add(entry.getKey(), (Float) value);
            }
            else if (value == null) {
                jsonBuilder.addNull(entry.getKey());
            }
            else {
                jsonBuilder.add(entry.getKey(), jsonCached.get(entry.getKey()));
            }
        }
        if (this.error != null) {
            jsonBuilder
                .add("error", Json.createObjectBuilder()
                    .add("class", error.getClass().getName())
                    .add("message", error.getMessage()));
        }
        JsonArrayBuilder changesBuilder = Json.createArrayBuilder();
        if (this.error == null) {
            for (Map<String, Object> entry : changes) {
                JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
                entryBuilder.add(PROP_TYPE, (String) entry.get(PROP_TYPE));

                Object arguments = entry.get(PROP_ARGUMENT);

                if (arguments != null) {
                    if (arguments instanceof List) {
                        JsonArrayBuilder argumentsBuilder = Json.createArrayBuilder();

                        for (String argument : ((List<String>) arguments)) {
                        	if(argument != null) {
                                argumentsBuilder.add(argument);
                        	}
                        }

                        entryBuilder.add(PROP_ARGUMENT, argumentsBuilder);
                    } else {
                        entryBuilder.add(PROP_ARGUMENT, (String) arguments);
                    }
                }
                changesBuilder.add(entryBuilder);
            }
        }
        jsonBuilder.add(PROP_CHANGES, changesBuilder);
        return jsonBuilder.build();
    }
    
    public class JSONResponseException extends RuntimeException {
        public JSONResponseException(String message, Throwable exception) {
           super(message, exception);
        }
        public JSONResponseException(Throwable e) {
           super("Error building JSON response", e);
        }
    }
}
