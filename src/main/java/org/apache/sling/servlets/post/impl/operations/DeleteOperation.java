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
package org.apache.sling.servlets.post.impl.operations;

import java.util.Iterator;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.SlingFileUploadHandler;

/**
 * The <code>DeleteOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_DELETE
 * delete} operation for the Sling default POST servlet.
 */
public class DeleteOperation extends AbstractPostOperation {

    /**
     * handler that deals with file upload
     */
    private final SlingFileUploadHandler uploadHandler;

    public DeleteOperation() {
        this.uploadHandler = new SlingFileUploadHandler();
    }

    @Override
    protected void doRun(
            final SlingJakartaHttpServletRequest request,
            final JakartaPostResponse response,
            final List<Modification> changes)
            throws PersistenceException {

        // SLING-3203: selectors, extension and suffix make no sense here and
        // might lead to deleting other resources than the one the user means.
        final RequestPathInfo rpi = request.getRequestPathInfo();
        if ((rpi.getSelectors() != null && rpi.getSelectors().length > 0)
                || (rpi.getExtension() != null && rpi.getExtension().length() > 0)
                || (rpi.getSuffix() != null && rpi.getSuffix().length() > 0)) {
            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN,
                    "DeleteOperation request cannot include any selectors, extension or suffix");
            return;
        }

        final VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);
        final boolean deleteChunks = isDeleteChunkRequest(request);
        final Iterator<Resource> res = getApplyToResources(request);
        if (res == null) {
            final Resource resource = request.getResource();
            deleteResource(resource, changes, versioningConfiguration, deleteChunks);
        } else {
            while (res.hasNext()) {
                final Resource resource = res.next();
                deleteResource(resource, changes, versioningConfiguration, deleteChunks);
            }
        }
    }

    /**
     * Delete chunks if
     * {@link DeleteOperation#isDeleteChunkRequest(SlingJakartaHttpServletRequest)} is
     * true otherwise delete resource.
     */
    private void deleteResource(
            final Resource resource,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration,
            final boolean deleteChunks)
            throws PersistenceException {
        if (deleteChunks) {
            uploadHandler.deleteChunks(resource);
        } else {
            this.jcrSupport.checkoutIfNecessary(resource.getParent(), changes, versioningConfiguration);
        }

        resource.getResourceResolver().delete(resource);

        changes.add(Modification.onDeleted(resource.getPath()));
    }

    /**
     * Return true if request is to delete chunks. To return true, request will
     * should parameter ":applyToChunks" and it should be true.
     * @param request the request
     * @return is the request is to delete chunks
     */
    protected boolean isDeleteChunkRequest(SlingJakartaHttpServletRequest request) {

        return Boolean.parseBoolean(request.getParameter(SlingPostConstants.RP_APPLY_TO_CHUNKS));
    }
}
