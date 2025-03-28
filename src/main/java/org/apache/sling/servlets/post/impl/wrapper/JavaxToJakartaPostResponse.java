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
package org.apache.sling.servlets.post.impl.wrapper;

import java.io.IOException;

import org.apache.sling.api.wrappers.JakartaToJavaxResponseWrapper;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.PostResponse;

import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("deprecation")
public class JavaxToJakartaPostResponse implements JakartaPostResponse{

    private final PostResponse delegate;

    public JavaxToJakartaPostResponse(final PostResponse p) {
        this.delegate = p;
    }
    @Override
    public Throwable getError() {
        return this.delegate.getError();
    }

    @Override
    public String getLocation() {
        return this.delegate.getLocation();
    }

    @Override
    public String getParentLocation() {
        return this.delegate.getParentLocation();
    }

    @Override
    public String getPath() {
        return this.delegate.getPath();
    }

    @Override
    public String getReferer() {
        return this.delegate.getReferer();
    }

    @Override
    public int getStatusCode() {
        return this.delegate.getStatusCode();
    }

    @Override
    public String getStatusMessage() {
        return this.delegate.getStatusMessage();
    }

    @Override
    public boolean isCreateRequest() {
        return this.delegate.isCreateRequest();
    }

    @Override
    public boolean isSuccessful() {
        return this.delegate.isSuccessful();
    }

    @Override
    public void onChange(final String type, final String... arguments) {
        this.delegate.onChange(type, arguments);
    }

    @Override
    public void onCopied(final String srcPath, final String dstPath) {
        this.delegate.onCopied(srcPath, dstPath);
    }

    @Override
    public void onCreated(final String path) {
        this.delegate.onCreated(path);
    }

    @Override
    public void onDeleted(final String path) {
        this.delegate.onDeleted(path);
    }

    @Override
    public void onModified(final String path) {
        this.delegate.onModified(path);
    }

    @Override
    public void onMoved(final String srcPath, final String dstPath) {
        this.delegate.onMoved(srcPath, dstPath);
    }

    @Override
    public void send(final HttpServletResponse response, final boolean setStatus) throws IOException {
        this.delegate.send(JakartaToJavaxResponseWrapper.toJavaxResponse(response), setStatus);
    }

    @Override
    public void setCreateRequest(final boolean isCreateRequest) {
        this.delegate.setCreateRequest(isCreateRequest);
    }

    @Override
    public void setError(final Throwable error) {
        this.delegate.setError(error);
    }

    @Override
    public void setLocation(final String location) {
        this.delegate.setLocation(location);
    }

    @Override
    public void setParentLocation(final String parentLocation) {
        this.delegate.setParentLocation(parentLocation);
    }

    @Override
    public void setPath(final String path) {
        this.delegate.setPath(path);
    }

    @Override
    public void setReferer(final String referer) {
        this.delegate.setReferer(referer);
    }

    @Override
    public void setStatus(final int code, final String message) {
        this.delegate.setStatus(code, message);
    }

    @Override
    public void setTitle(final String title) {
        this.delegate.setTitle(title);
    }
}
