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
package org.apache.sling.servlets.post.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.post.AbstractPostResponseWrapper;
import org.apache.sling.servlets.post.PostResponse;

/**
 * SLING-10006 Wrap another PostResponse impl to change the error handling behavior
 */
public class ErrorHandlingPostResponseWrapper extends AbstractPostResponseWrapper {

    private final PostResponse wrapped;

    public ErrorHandlingPostResponseWrapper(PostResponse wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public PostResponse getWrapped() {
        return this.wrapped;
    }

    @Override
    public void send(HttpServletResponse response, boolean setStatus) throws IOException {
        if (!isSuccessful()) {
            prepare(response, setStatus);

            // delegate the error rendering
            String statusMsg = getStatusMessage();
            if (statusMsg == null) {
                // fallback to the exception string
                Throwable error = getError();
                if (error != null) {
                    statusMsg = error.toString();
                }
            }
            if (statusMsg == null) {
                response.sendError(getStatusCode());
            } else {
                response.sendError(getStatusCode(), statusMsg);
            }
        } else {
            super.send(response, setStatus);
        }
    }

    /**
     * prepares the response properties
     */
    private void prepare(final HttpServletResponse response, final boolean setStatus) {
        if (setStatus) {
            // for backward compatibility, set the response status
            // in case the error rendering script doesn't do the same
            int statusCode = getStatusCode();
            response.setStatus(statusCode);
        }
    }

}
