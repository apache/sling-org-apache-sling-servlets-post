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
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.wrappers.JavaxToJakartaRequestWrapper;
import org.apache.sling.servlets.post.JakartaPostOperation;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.exceptions.PreconditionViolatedPersistenceException;
import org.apache.sling.servlets.post.exceptions.TemporaryPersistenceException;

@SuppressWarnings("deprecation")
public class JakartaToJavaxPostOperation implements PostOperation {

    private static final SlingPostProcessor[] EMPTY_PROCESSORS = new SlingPostProcessor[0];

    private final JakartaPostOperation delegate;

    public JakartaToJavaxPostOperation(final JakartaPostOperation delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run(final SlingHttpServletRequest request, final PostResponse response, SlingPostProcessor[] processors)
            throws PreconditionViolatedPersistenceException, TemporaryPersistenceException, PersistenceException {
        if (processors == null) {
            processors = EMPTY_PROCESSORS;
        }
        final SlingJakartaPostProcessor[] wrappers = new SlingJakartaPostProcessor[processors.length];
        for (int i = 0; i < processors.length; i++) {
            wrappers[i] = new JavaxToSlingJakartaPostProcessor(processors[i]);
        }
        this.delegate.run(
                JavaxToJakartaRequestWrapper.toJakartaRequest(request),
                new JavaxToJakartaPostResponse(response),
                wrappers);
    }
}
