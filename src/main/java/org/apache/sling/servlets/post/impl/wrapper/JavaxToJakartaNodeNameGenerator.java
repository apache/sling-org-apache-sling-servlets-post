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
package org.apache.sling.servlets.post.impl.wrapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.servlets.post.JakartaNodeNameGenerator;
import org.apache.sling.servlets.post.NodeNameGenerator;

public class JavaxToJakartaNodeNameGenerator implements JakartaNodeNameGenerator {

    private final NodeNameGenerator delegate;

    public JavaxToJakartaNodeNameGenerator(final NodeNameGenerator delegate) {
        this.delegate = delegate;
    }

    public NodeNameGenerator getDelegate() {
        return delegate;
    }

    @Override
    public String getNodeName(
            final SlingJakartaHttpServletRequest request,
            final String parentPath,
            final boolean requirePrefix,
            final JakartaNodeNameGenerator defaultNodeNameGenerator) {
        final NodeNameGenerator dng = new NodeNameGenerator() {
            @Override
            public String getNodeName(
                    final SlingHttpServletRequest internalRequest,
                    final String internalParentPath,
                    final boolean internalRequirePrefix,
                    final NodeNameGenerator defaultInternalGenerator) {
                return defaultNodeNameGenerator.getNodeName(
                        request, internalParentPath, internalRequirePrefix, defaultNodeNameGenerator);
            }
        };
        return this.delegate.getNodeName(
                JakartaToJavaxRequestWrapper.toJavaxRequest(request), parentPath, requirePrefix, dng);
    }
}
