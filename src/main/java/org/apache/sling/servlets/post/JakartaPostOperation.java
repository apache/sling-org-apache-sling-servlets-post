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
package org.apache.sling.servlets.post;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.servlets.post.exceptions.PreconditionViolatedPersistenceException;
import org.apache.sling.servlets.post.exceptions.TemporaryPersistenceException;

/**
 * The <code>JakartaPostOperation</code> interface defines the service API to be
 * implemented by service providers extending the Sling POST servlet. Service
 * providers may register OSGi services of this type to be used by the Sling
 * default POST servlet to handle specific operations.
 * <p>
 * The <code>PostOperation</code> service must be registered with a
 * {@link #PROP_OPERATION_NAME} registration property giving the name(s) of the
 * operations supported by the service. The names will be used to find the
 * actual operation from the {@link SlingPostConstants#RP_OPERATION
 * :operation} request parameter.
 * <p>
 * The Sling POST servlet itself provides various operations (see the
 * <code>OPERATION_</code> constants in the {@link SlingPostConstants}
 * interface.These names should not be used by <code>SlingPostOperation</code>
 * service providers.
 * <p>
 * This interface replaces the old {@link JakartaPostOperation} service interface
 * adding support for extensible responses by means of the {@link JakartaPostResponse}
 * interface as well as operation postprocessing.
 * <p>
 * Implementors of this interface are advised to extend the
 * {@link AbstractJakartaPostOperation} class to benefit from various processings
 * implemented by that abstract class.
 * @since 2.5.0
 */
public interface JakartaPostOperation {

    /**
     * The name of the Sling POST operation service.
     */
    public static final String SERVICE_NAME = "org.apache.sling.servlets.post.PostOperation";

    /**
     * The name of the service registration property indicating the name(s) of
     * the operation provided by the operation implementation. The value of this
     * service property must be a single String or an array or
     * <code>java.util.Collection</code> of Strings. If multiple strings are
     * defined, the service is registered for all operation names.
     */
    public static final String PROP_OPERATION_NAME = "sling.post.operation";

    /**
     * Executes the operation provided by this service implementation. This
     * method is called by the Sling POST servlet.
     *
     * @param request The <code>SlingJakartaHttpServletRequest</code> object providing
     *            the request input for the operation.
     * @param response The <code>JakartaPostResponse</code> into which the operation
     *            steps should be recorded.
     * @param processors The {@link SlingJakartaPostProcessor} services to be called
     *            after applying the operation. This may be <code>null</code> if
     *            there are none.
     * @throws PersistenceException when the commit fails
     * @throws org.apache.sling.api.resource.ResourceNotFoundException May be
     *             thrown if the operation requires an existing request
     *             resource. If this exception is thrown the Sling POST servlet
     *             sends back a <code>404/NOT FOUND</code> response to the
     *             client.
     * @throws org.apache.sling.api.SlingException May be thrown if an error
     *             Occurs running the operation.
     * @throws PreconditionViolatedPersistenceException when a necessary precondition failed,
     *             and a retry without further changes doesn't make sense.
     * @throws TemporaryPersistenceException when a commit failed, but a retry could make
     *             the operation work successfully.
     */
    void run(SlingJakartaHttpServletRequest request, JakartaPostResponse response,
            SlingJakartaPostProcessor[] processors) throws PreconditionViolatedPersistenceException, TemporaryPersistenceException, PersistenceException;
}
