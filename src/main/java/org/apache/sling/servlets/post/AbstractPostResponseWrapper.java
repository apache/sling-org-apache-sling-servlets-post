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

import javax.servlet.http.HttpServletResponse;

/**
 * Provides a simple implementation of PostResponse that can be subclassed by developers wishing to provide specialized behavior 
 * to an existing PostResponse instance. The default implementation of all methods is to call through to the wrapped 
 * PostResponse instance.
 */
public abstract class AbstractPostResponseWrapper implements PostResponse {

    /**
     * Use this method to return an instance of the class being wrapped.
     * 
     * @return the wrapped PostResponse instance
     */
    public abstract PostResponse getWrapped();

    @Override
    public void setReferer(String referer) {
        getWrapped().setReferer(referer);
    }

    @Override
    public String getReferer() {
        return getWrapped().getReferer();
    }

    @Override
    public void setPath(String path) {
        getWrapped().setPath(path);
    }

    @Override
    public String getPath() {
        return getWrapped().getPath();
    }

    @Override
    public void setCreateRequest(boolean isCreateRequest) {
        getWrapped().setCreateRequest(isCreateRequest);
    }

    @Override
    public boolean isCreateRequest() {
        return getWrapped().isCreateRequest();
    }

    @Override
    public void setLocation(String location) {
        getWrapped().setLocation(location);
    }

    @Override
    public String getLocation() {
        return getWrapped().getLocation();
    }

    @Override
    public void setParentLocation(String parentLocation) {
        getWrapped().setParentLocation(parentLocation);
    }

    @Override
    public String getParentLocation() {
        return getWrapped().getParentLocation();
    }

    @Override
    public void setTitle(String title) {
        getWrapped().setTitle(title);
    }

    @Override
    public void setStatus(int code, String message) {
        getWrapped().setStatus(code, message);
    }

    @Override
    public int getStatusCode() {
        return getWrapped().getStatusCode();
    }

    @Override
    public String getStatusMessage() {
        return getWrapped().getStatusMessage();
    }

    @Override
    public void setError(Throwable error) {
        getWrapped().setError(error);
    }

    @Override
    public Throwable getError() {
        return getWrapped().getError();
    }

    @Override
    public boolean isSuccessful() {
        return getWrapped().isSuccessful();
    }

    @Override
    public void onCreated(String path) {
        getWrapped().onCreated(path);
    }

    @Override
    public void onModified(String path) {
        getWrapped().onModified(path);
    }

    @Override
    public void onDeleted(String path) {
        getWrapped().onDeleted(path);
    }

    @Override
    public void onMoved(String srcPath, String dstPath) {
        getWrapped().onMoved(srcPath, dstPath);
    }

    @Override
    public void onCopied(String srcPath, String dstPath) {
        getWrapped().onCopied(srcPath, dstPath);
    }

    @Override
    public void onChange(String type, String... arguments) {
        getWrapped().onChange(type, arguments);
    }

    @Override
    public void send(HttpServletResponse response, boolean setStatus) throws IOException {
        getWrapped().send(response, setStatus);
    }

}
