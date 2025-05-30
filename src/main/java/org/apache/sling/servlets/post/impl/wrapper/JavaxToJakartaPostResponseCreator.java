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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.JakartaPostResponseCreator;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.PostResponseCreator;

@SuppressWarnings("deprecation")
public class JavaxToJakartaPostResponseCreator implements JakartaPostResponseCreator {

    private final PostResponseCreator delegate;

    public JavaxToJakartaPostResponseCreator(final PostResponseCreator delegate) {
        this.delegate = delegate;
    }

    @Override
    public JakartaPostResponse createPostResponse(SlingJakartaHttpServletRequest request) {
        final PostResponse p = this.delegate.createPostResponse(JakartaToJavaxRequestWrapper.toJavaxRequest(request));
        if (p == null) {
            return null;
        }
        return new JavaxToJakartaPostResponse(p);
    }

    public PostResponseCreator getDelegate() {
        return delegate;
    }
}
