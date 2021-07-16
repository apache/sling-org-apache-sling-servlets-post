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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Generates a node name based on a set of well-known request parameters
 * like title, description, etc.
 * See SLING-128.
 */
public class DefaultNodeNameGenerator implements NodeNameGenerator {

    private final String[] parameterNames;
    private final NodeNameFilter filter = new NodeNameFilter();

    public static final int DEFAULT_MAX_NAME_LENGTH = 20;

    private int maxLength = DEFAULT_MAX_NAME_LENGTH;
    private int counter;

    public DefaultNodeNameGenerator() {
        this(null, -1);
    }

    public DefaultNodeNameGenerator(String[] parameterNames, int maxNameLength) {
        if (parameterNames == null) {
            this.parameterNames = new String[0];
        } else {
            this.parameterNames = parameterNames;
        }

        this.maxLength = (maxNameLength > 0)
                ? maxNameLength
                : DEFAULT_MAX_NAME_LENGTH;
    }

    /**
     * Determine the value to use for the specified parameter. This also
     * considers the parameter with a {@link SlingPostConstants#VALUE_FROM_SUFFIX}
     *
     * @param parameters the map of request parameters
     * @param paramName the parameter to get the value for
     * @return the value to use for the parameter or null if it could not be determined
     */
    protected String getValueToUse(RequestParameterMap parameters, String paramName) {
        String valueToUse = null;
        RequestParameter[] pp = parameters.getValues(paramName);
        if (pp != null) {
            for (RequestParameter specialParam : pp) {
                if (specialParam != null && !specialParam.getString().isEmpty()) {
                    valueToUse = specialParam.getString();
                }

                if (valueToUse != null) {
                    if (valueToUse.isEmpty()) {
                        // empty value is not usable
                        valueToUse = null;
                    } else {
                        // found value, so stop looping
                        break;
                    }
                }
            }
        } else {
            // check for a paramName@ValueFrom param
            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            pp = parameters.getValues(String.format("%s%s", paramName, SlingPostConstants.VALUE_FROM_SUFFIX));
            if (pp != null) {
                for (RequestParameter specialParam : pp) {
                    if (specialParam != null && !specialParam.getString().isEmpty()) {
                        // retrieve the reference parameter value
                        RequestParameter[] refParams = parameters.getValues(specialParam.getString());
                        // @ValueFrom params must have exactly one value, else ignored
                        if (refParams != null && refParams.length == 1) {
                            specialParam = refParams[0];
                            if (specialParam != null && !specialParam.getString().isEmpty()) {
                                valueToUse = specialParam.getString();
                            }
                        }
                    }

                    if (valueToUse != null) {
                        if (valueToUse.isEmpty()) {
                            // empty value is not usable
                            valueToUse = null;
                        } else {
                            // found value, so stop looping
                            break;
                        }
                    }
                }
            }
        }
        return valueToUse;
    }

    /**
     * Get a "nice" node name, if possible, based on given request
     *
     * @param request the request
     * @param basePath the base path
     * @param requirePrefix <code>true</code> if the parameter names for
     *      properties requires a prefix
     * @param defaultNodeNameGenerator a default generator
     * @return a nice node name
     */
    public String getNodeName(SlingHttpServletRequest request, String basePath,
            boolean requirePrefix, NodeNameGenerator defaultNodeNameGenerator) {
        RequestParameterMap parameters = request.getRequestParameterMap();
        String valueToUse = null;
        boolean doFilter = true;

        // find the first request parameter that matches one of
        // our parameterNames, in order, and has a value
        if (parameters!=null) {
            // we first check for the special sling parameters
            valueToUse = getValueToUse(parameters, SlingPostConstants.RP_NODE_NAME);
            if (valueToUse != null) {
                doFilter = false;
            }
            if ( valueToUse == null ) {
                valueToUse = getValueToUse(parameters, SlingPostConstants.RP_NODE_NAME_HINT);
            }

            if (valueToUse == null) {
                for (String param : parameterNames) {
                    if (requirePrefix) {
                        param = SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT.concat(param);
                    }
                    valueToUse = getValueToUse(parameters, param);
                    if (valueToUse != null) {
                        break;
                    }
                }
            }
        }
        String result;
        // should we filter?
        if (valueToUse != null) {
            if ( doFilter ) {
                // filter value so that it works as a node name
                result = filter.filter(valueToUse);
            } else {
                result = valueToUse;
            }
        } else {
            // default value if none provided
            result = nextCounter() + "_" + System.currentTimeMillis();
        }

        if ( doFilter ) {
            // max length
            if (result.length() > maxLength) {
                result = result.substring(0,maxLength);
            }
        }

        return result;
    }

    public synchronized int nextCounter() {
        return ++counter;
    }
}
