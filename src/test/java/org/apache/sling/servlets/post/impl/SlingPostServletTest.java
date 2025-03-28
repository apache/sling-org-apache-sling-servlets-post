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
package org.apache.sling.servlets.post.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.request.header.JakartaMediaRangeList;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingJakartaHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingJakartaHttpServletResponseWrapper;
import org.apache.sling.servlets.post.JakartaHtmlResponse;
import org.apache.sling.servlets.post.JakartaJSONResponse;
import org.apache.sling.servlets.post.JakartaPostOperation;
import org.apache.sling.servlets.post.JakartaPostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.operations.DeleteOperation;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.mockito.Mockito.eq;

public class SlingPostServletTest extends TestCase {

    private SlingPostServlet servlet;

    private Resource fakeResource;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new SlingPostServlet();
        this.fakeResource = Mockito.mock(Resource.class);
    }

    public void testIsSetStatus() {
        SlingJakartaHttpServletRequest req =
                Builders.newRequestBuilder(fakeResource).buildJakartaRequest();

        // 1. null parameter, expect true
        assertTrue("Standard status expected for null param", servlet.isSetStatus(req));

        // 2. "standard" parameter, expect true
        req = Builders.newRequestBuilder(fakeResource)
                .withParameter(SlingPostConstants.RP_STATUS, SlingPostConstants.STATUS_VALUE_STANDARD)
                .buildJakartaRequest();
        assertTrue(
                "Standard status expected for '" + SlingPostConstants.STATUS_VALUE_STANDARD + "' param",
                servlet.isSetStatus(req));

        // 3. "browser" parameter, expect false
        req = Builders.newRequestBuilder(fakeResource)
                .withParameter(SlingPostConstants.RP_STATUS, SlingPostConstants.STATUS_VALUE_BROWSER)
                .buildJakartaRequest();
        assertFalse(
                "Browser status expected for '" + SlingPostConstants.STATUS_VALUE_BROWSER + "' param",
                servlet.isSetStatus(req));

        // 4. any parameter, expect true
        String param = "knocking on heaven's door";
        req = Builders.newRequestBuilder(fakeResource)
                .withParameter(SlingPostConstants.RP_STATUS, param)
                .buildJakartaRequest();
        assertTrue("Standard status expected for '" + param + "' param", servlet.isSetStatus(req));
    }

    public void testGetJsonResponse() {
        SlingJakartaHttpServletRequest origReg =
                Builders.newRequestBuilder(fakeResource).buildJakartaRequest();

        SlingJakartaHttpServletRequest req = new SlingJakartaHttpServletRequestWrapper(origReg) {
            @Override
            public String getHeader(String name) {
                return name.equals(JakartaMediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }
        };
        JakartaPostResponse result = servlet.createPostResponse(req);
        assertFalse(
                "Did not expect ErrorHandlingPostResponseWrapper PostResonse",
                result instanceof ErrorHandlingPostResponseWrapper);
        assertTrue(result instanceof JakartaJSONResponse);
    }

    public void testGetHtmlResponse() {
        SlingJakartaHttpServletRequest req =
                Builders.newRequestBuilder(fakeResource).buildJakartaRequest();
        JakartaPostResponse result = servlet.createPostResponse(req);
        assertFalse(
                "Did not expect ErrorHandlingPostResponseWrapper PostResonse",
                result instanceof ErrorHandlingPostResponseWrapper);
        assertTrue(result instanceof JakartaHtmlResponse);
    }

    /**
     * SLING-10006 - verify we get the error handling wrapped PostResponse
     */
    public void testGetJsonResponseWithSendError() {
        SendErrorParamSlingJakartaHttpServletRequest req = new SendErrorParamSlingJakartaHttpServletRequest() {
            @Override
            public String getHeader(String name) {
                return name.equals(JakartaMediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }
        };
        req.setSendError("true");

        JakartaPostResponse result = servlet.createPostResponse(req);
        assertTrue(
                "Expected ErrorHandlingPostResponseWrapper PostResonse",
                result instanceof ErrorHandlingPostResponseWrapper);
        result = ((ErrorHandlingPostResponseWrapper) result).getWrapped();
        assertTrue(result instanceof JakartaJSONResponse);
    }

    public void testPersistenceExceptionLogging() {
        Logger log = Mockito.mock(Logger.class);
        SlingJakartaHttpServletRequest mockRequest = Mockito.mock(SlingJakartaHttpServletRequest.class);
        Resource mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getPath()).thenReturn("/path");
        Mockito.when(mockRequest.getResource()).thenReturn(mockResource);
        servlet.setLog(log);
        JakartaPostOperation operation = new DeleteOperation();
        Exception exception = new IOException("foo");

        servlet.setLogStacktraceInExceptions(true);
        String expected = "Exception while handling POST on path [{}] with operation [{}]";
        servlet.logPersistenceException(mockRequest, operation, exception);
        Mockito.verify(log)
                .warn(
                        eq(expected),
                        eq("/path"),
                        eq("org.apache.sling.servlets.post.impl.operations.DeleteOperation"),
                        eq(exception));

        servlet.setLogStacktraceInExceptions(false);
        expected = "{} while handling POST on path [{}] with operation [{}]: {}";
        servlet.logPersistenceException(mockRequest, operation, exception);
        Mockito.verify(log)
                .warn(
                        eq(expected),
                        eq("java.io.IOException"),
                        eq("/path"),
                        eq("org.apache.sling.servlets.post.impl.operations.DeleteOperation"),
                        eq("foo"));
    }

    /**
     * SLING-10006 - verify we get the error handling wrapped PostResponse
     */
    public void testGetHtmlResponseWithSendError() {
        SendErrorParamSlingJakartaHttpServletRequest req = new SendErrorParamSlingJakartaHttpServletRequest();
        req.setSendError("true");

        JakartaPostResponse result = servlet.createPostResponse(req);
        assertTrue(result instanceof ErrorHandlingPostResponseWrapper);
        result = ((ErrorHandlingPostResponseWrapper) result).getWrapped();
        assertTrue(result instanceof JakartaHtmlResponse);
    }

    public void testRedirection() throws Exception {
        String utf8Path = "\u0414\u0440\u0443\u0433\u0430";
        String encodedUtf8 = "%D0%94%D1%80%D1%83%D0%B3%D0%B0";
        testRedirection("/", "/fred", "*.html", "/fred.html");
        testRedirection("/xyz/", "/xyz/" + utf8Path, "*", "/xyz/" + encodedUtf8);
        testRedirection("/", "/fred/" + utf8Path, "/xyz/*", "/xyz/" + encodedUtf8);
        testRedirection("/", "/fred/" + utf8Path, null, null);
        // test redirect with host information
        testRedirection("/", "/fred/abc", "http://forced", null);
        testRedirection("/", "/fred/abc", "//forced.com/test", null);
        testRedirection("/", "/fred/abc", "https://forced.com/test", null);
        // invalid URI
        testRedirection("/", "/fred/abc", "file://c:\\Users\\workspace\\test.java", null);
    }

    private void testRedirection(String requestPath, String resourcePath, String redirect, String expected)
            throws Exception {
        RedirectServletResponse resp = new RedirectServletResponse();
        SlingJakartaHttpServletRequest request = new RedirectServletRequest(redirect, requestPath);
        JakartaPostResponse htmlResponse = new JakartaHtmlResponse();
        htmlResponse.setPath(resourcePath);
        assertEquals(expected != null, servlet.redirectIfNeeded(request, htmlResponse, resp));
        assertEquals(expected, resp.redirectLocation);
    }

    /**
     *
     */
    private final class RedirectServletRequest extends SlingJakartaHttpServletRequestWrapper {

        private String requestPath;
        private String redirect;

        private RedirectServletRequest(String redirect, String requestPath) {
            super(Builders.newRequestBuilder(Mockito.mock(Resource.class)).buildJakartaRequest());
            this.requestPath = requestPath;
            this.redirect = redirect;
        }

        public String getPathInfo() {
            return requestPath;
        }

        @Override
        public String getParameter(String name) {
            return SlingPostConstants.RP_REDIRECT_TO.equals(name) ? redirect : null;
        }
    }

    private final class RedirectServletResponse extends SlingJakartaHttpServletResponseWrapper {

        private String redirectLocation;

        public RedirectServletResponse() {
            super(Builders.newResponseBuilder().buildJakartaResponseResult());
        }

        @Override
        public String encodeRedirectURL(String s) {
            StringTokenizer st = new StringTokenizer(s, "/", true);
            StringBuilder sb = new StringBuilder();
            try {
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if ("/".equals(token)) {
                        sb.append(token);
                    } else {
                        sb.append(URLEncoder.encode(token, "UTF-8"));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                fail("Should have UTF-8?? " + e);
                return null;
            }
            return sb.toString();
        }

        @Override
        public void sendRedirect(String s) throws IOException {
            redirectLocation = s;
        }
    }

    private static class SendErrorParamSlingJakartaHttpServletRequest extends SlingJakartaHttpServletRequestWrapper {

        private String sendError;

        public SendErrorParamSlingJakartaHttpServletRequest() {
            super(Builders.newRequestBuilder(Mockito.mock(Resource.class)).buildJakartaRequest());
        }

        @Override
        public String getParameter(String name) {
            if (SlingPostConstants.RP_SEND_ERROR.equals(name)) {
                return sendError;
            }

            return super.getParameter(name);
        }

        void setSendError(String sendErrorParam) {
            this.sendError = sendErrorParam;
        }
    }
}
