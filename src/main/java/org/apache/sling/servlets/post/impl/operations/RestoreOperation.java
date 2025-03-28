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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import java.util.Iterator;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * The <code>RestoreOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_RESTORE restore}
 * operation for the Sling default POST servlet.
 * The restore operation depends on the resources being backed up by a JCR node.
 */
public class RestoreOperation extends AbstractPostOperation {

    @Override
    protected void doRun(
            SlingJakartaHttpServletRequest request, JakartaPostResponse response, List<Modification> changes)
            throws PersistenceException {
        try {
            final String version = request.getParameter(SlingPostConstants.RP_VERSION);
            if (version == null || version.length() == 0) {
                throw new IllegalArgumentException("Unable to process restore. Missing version");
            }
            final String removeString = request.getParameter(SlingPostConstants.RP_REMOVE_EXISTING);
            final boolean removeExisting = Boolean.parseBoolean(removeString);

            Iterator<Resource> res = getApplyToResources(request);
            if (res == null) {
                Resource resource = request.getResource();
                Node node = resource.adaptTo(Node.class);
                if (node == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND, "Missing source " + resource + " for restore");
                    return;
                }
                restore(node, version, removeExisting);
                changes.add(Modification.onRestore(resource.getPath(), version));
            } else {
                while (res.hasNext()) {
                    Resource resource = res.next();
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        restore(node, version, removeExisting);
                        changes.add(Modification.onRestore(resource.getPath(), version));
                    }
                }
            }
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    private void restore(Node node, String versionSpecifier, boolean removeExisting) throws RepositoryException {
        final VersionManager vm = node.getSession().getWorkspace().getVersionManager();
        final VersionHistory history = vm.getVersionHistory(node.getPath());
        final Version version;
        if (history.hasVersionLabel(versionSpecifier)) {
            version = history.getVersionByLabel(versionSpecifier);
        } else if (history.hasNode(versionSpecifier)) {
            version = history.getVersion(versionSpecifier);
        } else {
            throw new IllegalArgumentException("Unable to process restore. Invalid version: " + versionSpecifier);
        }
        vm.restore(version, removeExisting);
    }
}
