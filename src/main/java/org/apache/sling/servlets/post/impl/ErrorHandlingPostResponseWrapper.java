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

import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.AbstractPostResponseWrapper;

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
            response.sendError(getStatusCode(), getError().toString());
        } else {
            super.send(response, setStatus);
        }
    }

}