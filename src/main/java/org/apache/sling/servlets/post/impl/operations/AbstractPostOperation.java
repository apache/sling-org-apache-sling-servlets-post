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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.servlets.post.JakartaPostOperation;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.exceptions.PreconditionViolatedPersistenceException;
import org.apache.sling.servlets.post.exceptions.TemporaryPersistenceException;
import org.apache.sling.servlets.post.impl.helper.JCRSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AbstractPostOperation</code> class is a base implementation of the
 * {@link JakartaPostOperation} service interface providing actual implementations with
 * useful tooling and common functionality like preparing the change logs or
 * saving or refreshing.
 *
 * As this package is not exported, if you want to use this as a base class for
 * custom operations you'll need to embed it in your bundles using the appropriate
 * bnd directive.
 */
public abstract class AbstractPostOperation implements JakartaPostOperation {

    /**
     * Default logger
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** The JCR support provides additional functionality if the resources are backed by JCR. */
    protected final JCRSupport jcrSupport = JCRSupport.INSTANCE;

    /**
     * Prepares and finalizes the actual operation. Preparation encompasses
     * getting the absolute path of the item to operate on by calling the
     * {@link #getResourcePath(SlingJakartaHttpServletRequest)} method and setting the
     * location and parent location on the response. After the operation has
     * been done in the {@link #doRun(SlingJakartaHttpServletRequest, PostResponse, List)}
     * method the session is saved if there are unsaved modifications. In case
     * of errors, the unsaved changes in the session are rolled back.
     *
     * @param request the request to operate on
     * @param response The <code>PostResponse</code> to record execution
     *            progress.
     * @param processors The array of processors
     */
    @Override
    public void run(
            final SlingJakartaHttpServletRequest request,
            final JakartaPostResponse response,
            final SlingJakartaPostProcessor[] processors)
            throws PreconditionViolatedPersistenceException, TemporaryPersistenceException, PersistenceException {
        final VersioningConfiguration versionableConfiguration = getVersioningConfiguration(request);

        try {
            // calculate the paths
            String path = this.getResourcePath(request);
            response.setPath(path);

            // location
            response.setLocation(externalizePath(request, path));

            // parent location
            path = ResourceUtil.getParent(path);
            if (path != null) {
                response.setParentLocation(externalizePath(request, path));
            }

            final List<Modification> changes = new ArrayList<>();

            doRun(request, response, changes);

            // invoke processors
            try {
                if (processors != null) {
                    for (SlingJakartaPostProcessor processor : processors) {
                        request.getRequestProgressTracker()
                                .log(
                                        "Calling Sling Post Processor {0}",
                                        processor.getClass().getName());
                        processor.process(request, changes);
                    }
                }
            } catch (PreconditionViolatedPersistenceException | TemporaryPersistenceException e) {
                throw e;
            } catch (Exception e) {
                throw new PersistenceException("Exception during response processing", e);
            }

            // check modifications for remaining postfix and store the base path
            final Map<String, String> modificationSourcesContainingPostfix = new HashMap<>();
            final Set<String> allModificationSources = new HashSet<>(changes.size());
            for (final Modification modification : changes) {
                final String source = modification.getSource();
                if (source != null) {
                    allModificationSources.add(source);
                    final int atIndex = source.indexOf('@');
                    if (atIndex > 0) {
                        modificationSourcesContainingPostfix.put(source.substring(0, atIndex), source);
                    }
                }
            }

            // fail if any of the base paths (before the postfix) which had a postfix are contained in the modification
            // set
            if (modificationSourcesContainingPostfix.size() > 0) {
                for (final Map.Entry<String, String> sourceToCheck : modificationSourcesContainingPostfix.entrySet()) {
                    if (allModificationSources.contains(sourceToCheck.getKey())) {
                        throw new PersistenceException("Postfix-containing path " + sourceToCheck.getValue()
                                + " contained in the modification list. Check configuration.");
                    }
                }
            }

            final Set<String> nodesToCheckin = new LinkedHashSet<>();

            // set changes on html response
            for (Modification change : changes) {
                switch (change.getType()) {
                    case MODIFY:
                        response.onModified(change.getSource());
                        break;
                    case DELETE:
                        response.onDeleted(change.getSource());
                        break;
                    case MOVE:
                        response.onMoved(change.getSource(), change.getDestination());
                        break;
                    case COPY:
                        response.onCopied(change.getSource(), change.getDestination());
                        break;
                    case CREATE:
                        response.onCreated(change.getSource());
                        if (versionableConfiguration.isCheckinOnNewVersionableNode()) {
                            nodesToCheckin.add(change.getSource());
                        }
                        break;
                    case ORDER:
                        response.onChange("ordered", change.getSource(), change.getDestination());
                        break;
                    case CHECKOUT:
                        response.onChange("checkout", change.getSource());
                        nodesToCheckin.add(change.getSource());
                        break;
                    case CHECKIN:
                        response.onChange("checkin", change.getSource());
                        nodesToCheckin.remove(change.getSource());
                        break;
                    case RESTORE:
                        response.onChange("restore", change.getSource());
                        break;
                }
            }

            if (isResourceResolverCommitRequired(request)) {
                request.getResourceResolver().commit();
            }

            if (!isSkipCheckin(request)) {
                // now do the checkins
                for (String checkinPath : nodesToCheckin) {
                    if (this.jcrSupport.checkin(request.getResourceResolver().getResource(checkinPath))) {
                        response.onChange("checkin", checkinPath);
                    }
                }
            }

        } finally {
            if (isResourceResolverCommitRequired(request)) {
                request.getResourceResolver().revert();
            }
        }
    }

    /**
     * Actually performs the desired operation filling progress into the
     * <code>changes</code> list and preparing and further information in the
     * <code>response</code>.
     * <p>
     * The <code>response</code> comes prepared with the path, location and
     * parent location set. Other properties are expected to be set by this
     * implementation.
     *
     * @param request The <code>SlingJakartaHttpServletRequest</code> providing the
     *            input, mostly in terms of request parameters, to the
     *            operation.
     * @param response The {@link org.apache.sling.servlets.post.PostResponse} to fill with response
     *            information
     * @param changes A container to add {@link Modification} instances
     *            representing the operations done.
     * @throws PersistenceException Maybe thrown if any error occurs while
     *             accessing the repository.
     */
    protected abstract void doRun(
            SlingJakartaHttpServletRequest request, JakartaPostResponse response, List<Modification> changes)
            throws PersistenceException;

    /**
     * Get the versioning configuration.
     * @param request The http request
     * @return The versioning configuration
     */
    protected VersioningConfiguration getVersioningConfiguration(final SlingJakartaHttpServletRequest request) {
        VersioningConfiguration versionableConfiguration =
                (VersioningConfiguration) request.getAttribute(VersioningConfiguration.class.getName());
        return versionableConfiguration != null ? versionableConfiguration : new VersioningConfiguration();
    }

    /**
     * Check if checkin should be skipped
     * @param request The http request
     * @return {@code true} if checkin should be skipped
     */
    protected boolean isSkipCheckin(SlingJakartaHttpServletRequest request) {
        return !getVersioningConfiguration(request).isAutoCheckin();
    }

    /**
     * Check whether changes should be written back
     * @param request The http request
     * @return {@code true} If committing be skipped
     */
    private boolean isSkipSessionHandling(SlingJakartaHttpServletRequest request) {
        return Boolean.parseBoolean((String) request.getAttribute(SlingPostConstants.ATTR_SKIP_SESSION_HANDLING))
                == true;
    }

    /**
     * Check whether commit to the resource resolver should be called.
     * @param request The http request
     * @return {@code true} if a commit is required.
     */
    private boolean isResourceResolverCommitRequired(SlingJakartaHttpServletRequest request) {
        return !isSkipSessionHandling(request) && request.getResourceResolver().hasChanges();
    }

    /**
     * Returns an iterator on <code>Resource</code> instances addressed in the
     * {@link SlingPostConstants#RP_APPLY_TO} request parameter. If the request
     * parameter is not set, <code>null</code> is returned. If the parameter
     * is set with valid resources an empty iterator is returned. Any resources
     * addressed in the {@link SlingPostConstants#RP_APPLY_TO} parameter is
     * ignored.
     *
     * @param request The <code>SlingJakartaHttpServletRequest</code> object used to
     *            get the {@link SlingPostConstants#RP_APPLY_TO} parameter.
     * @return The iterator of resources listed in the parameter or
     *         <code>null</code> if the parameter is not set in the request.
     */
    protected Iterator<Resource> getApplyToResources(final SlingJakartaHttpServletRequest request) {

        final String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
        if (applyTo == null) {
            return null;
        }

        return new ApplyToIterator(request, applyTo);
    }

    /**
     * Returns an external form of the given path prepending the context path
     * and appending a display extension.
     *
     * @param request The http request
     * @param path the path to externalize
     * @return the url
     */
    protected final String externalizePath(final SlingJakartaHttpServletRequest request, final String path) {
        StringBuilder ret = new StringBuilder();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(SlingPostConstants.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }

    /**
     * Returns the path of the resource of the request as the resource path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     * @param request The http request
     * @return The resource path
     */
    protected String getResourcePath(SlingJakartaHttpServletRequest request) {
        return request.getResource().getPath();
    }

    /**
     * Orders the given resource according to the specified command. The following
     * syntax is supported: &lt;xmp&gt; | first | before all child nodes | before A |
     * before child node A | after A | after child node A | last | after all
     * nodes | N | at a specific position, N being an integer &lt;/xmp&gt;
     *
     * @param request The http request
     * @param resource the resource to order
     * @param changes the list of modifications
     * @throws PersistenceException in case the operation is not successful
     */
    protected void orderResource(
            final SlingJakartaHttpServletRequest request, final Resource resource, final List<Modification> changes)
            throws PersistenceException {

        final String command = request.getParameter(SlingPostConstants.RP_ORDER);
        if (command == null || command.length() == 0) {
            // nothing to do
            return;
        }

        final Resource parent = resource.getParent();

        String next = null;
        if (command.equals(SlingPostConstants.ORDER_FIRST)) {

            next = parent.listChildren().next().getName();

        } else if (command.equals(SlingPostConstants.ORDER_LAST)) {

            next = "";

        } else if (command.startsWith(SlingPostConstants.ORDER_BEFORE)) {

            next = command.substring(SlingPostConstants.ORDER_BEFORE.length());

        } else if (command.startsWith(SlingPostConstants.ORDER_AFTER)) {

            String name = command.substring(SlingPostConstants.ORDER_AFTER.length());
            Iterator<Resource> iter = parent.listChildren();
            while (iter.hasNext()) {
                Resource r = iter.next();
                if (r.getName().equals(name)) {
                    if (iter.hasNext()) {
                        next = iter.next().getName();
                    } else {
                        next = "";
                    }
                }
            }

        } else {
            // check for integer
            try {
                // 01234
                // abcde move a -> 2 (above 3)
                // bcade move a -> 1 (above 1)
                // bacde
                int newPos = Integer.parseInt(command);
                next = "";
                Iterator<Resource> iter = parent.listChildren();
                while (iter.hasNext() && newPos >= 0) {
                    Resource r = iter.next();
                    if (r.getName().equals(resource.getName())) {
                        // if old resource is found before index, need to
                        // inc index
                        newPos++;
                    }
                    if (newPos == 0) {
                        next = r.getName();
                        break;
                    }
                    newPos--;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("provided node ordering command is invalid: " + command);
            }
        }

        if (next != null) {
            if (next.equals("")) {
                next = null;
            }
            resource.getResourceResolver().orderBefore(parent, resource.getName(), next);
            changes.add(Modification.onOrder(resource.getPath(), next));
            if (log.isDebugEnabled()) {
                log.debug("Resource {} ordered '{}'", resource.getPath(), command);
            }
        } else {
            throw new IllegalArgumentException("provided resource ordering command is invalid: " + command);
        }
    }

    private static class ApplyToIterator implements Iterator<Resource> {

        private final ResourceResolver resolver;
        private final Resource baseResource;
        private final String[] paths;

        private int pathIndex;

        private Resource nextResource;

        private Iterator<Resource> resourceIterator = null;

        ApplyToIterator(SlingJakartaHttpServletRequest request, String[] paths) {
            this.resolver = request.getResourceResolver();
            this.baseResource = request.getResource();
            this.paths = paths;
            this.pathIndex = 0;

            nextResource = seek();
        }

        @Override
        public boolean hasNext() {
            return nextResource != null;
        }

        @Override
        public Resource next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Resource result = nextResource;
            nextResource = seek();

            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Resource seek() {
            if (resourceIterator != null) {
                if (resourceIterator.hasNext()) {
                    // return the next resource in the iterator
                    Resource res = resourceIterator.next();
                    return res;
                }
                resourceIterator = null;
            }
            while (pathIndex < paths.length) {
                String path = paths[pathIndex];
                pathIndex++;

                // SLING-2415 - support wildcard as the last segment of the applyTo path
                if (path.endsWith("*")) {
                    if (path.length() == 1) {
                        resourceIterator = baseResource.listChildren();
                    } else if (path.endsWith("/*")) {
                        path = path.substring(0, path.length() - 2);
                        if (path.length() == 0) {
                            resourceIterator = baseResource.listChildren();
                        } else {
                            Resource res = resolver.getResource(baseResource, path);
                            if (res != null) {
                                resourceIterator = res.listChildren();
                            }
                        }
                    }
                    if (resourceIterator != null) {
                        // return the first resource in the iterator
                        if (resourceIterator.hasNext()) {
                            Resource res = resourceIterator.next();
                            return res;
                        }
                        resourceIterator = null;
                    }
                } else {
                    Resource res = resolver.getResource(baseResource, path);
                    if (res != null) {
                        return res;
                    }
                }
            }

            // no more elements in the array
            return null;
        }
    }
}
