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

import java.util.List;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.apache.sling.servlets.post.SlingPostProcessor;

@SuppressWarnings("deprecation")
public class JavaxToSlingJakartaPostProcessor implements SlingJakartaPostProcessor {

    private final SlingPostProcessor delegate;

    public JavaxToSlingJakartaPostProcessor(final SlingPostProcessor delegate) {
        this.delegate = delegate;
    }

    public SlingPostProcessor getDelegate() {
        return delegate;
    }

    @Override
    public void process(SlingJakartaHttpServletRequest request, List<Modification> changes) throws Exception {
        this.delegate.process(JakartaToJavaxRequestWrapper.toJavaxRequest(request), changes);
    }
}
