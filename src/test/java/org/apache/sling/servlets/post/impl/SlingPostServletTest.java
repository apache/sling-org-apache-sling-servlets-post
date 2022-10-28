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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.header.MediaRangeList;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Request;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Response;
import org.apache.sling.servlets.post.impl.operations.DeleteOperation;

import junit.framework.TestCase;

import org.mockito.Mockito;
import static org.mockito.Mockito.eq;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;

public class SlingPostServletTest extends TestCase {
    
    private SlingPostServlet servlet;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new SlingPostServlet();
    }

    public void testIsSetStatus() {
        StatusParamSlingHttpServletRequest req = new StatusParamSlingHttpServletRequest();

        // 1. null parameter, expect true
        req.setStatusParam(null);
        assertTrue("Standard status expected for null param",
            servlet.isSetStatus(req));

        // 2. "standard" parameter, expect true
        req.setStatusParam(SlingPostConstants.STATUS_VALUE_STANDARD);
        assertTrue("Standard status expected for '"
            + SlingPostConstants.STATUS_VALUE_STANDARD + "' param",
            servlet.isSetStatus(req));

        // 3. "browser" parameter, expect false
        req.setStatusParam(SlingPostConstants.STATUS_VALUE_BROWSER);
        assertFalse("Browser status expected for '"
            + SlingPostConstants.STATUS_VALUE_BROWSER + "' param",
            servlet.isSetStatus(req));

        // 4. any parameter, expect true
        String param = "knocking on heaven's door";
        req.setStatusParam(param);
        assertTrue("Standard status expected for '" + param + "' param",
            servlet.isSetStatus(req));
    }

    public void testGetJsonResponse() {
        MockSlingHttpServletRequest req = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getHeader(String name) {
                return name.equals(MediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };
        PostResponse result = servlet.createPostResponse(req);
        assertFalse("Did not expect ErrorHandlingPostResponseWrapper PostResonse", result instanceof ErrorHandlingPostResponseWrapper);
        assertTrue(result instanceof JSONResponse);
    }

    public void testGetHtmlResponse() {
        MockSlingHttpServletRequest req = new MockSlingHttpServlet3Request(null, null, null, null, null);
        PostResponse result = servlet.createPostResponse(req);
        assertFalse("Did not expect ErrorHandlingPostResponseWrapper PostResonse", result instanceof ErrorHandlingPostResponseWrapper);
        assertTrue(result instanceof HtmlResponse);
    }

    /**
     * SLING-10006 - verify we get the error handling wrapped PostResponse
     */
    public void testGetJsonResponseWithSendError() {
    	SendErrorParamSlingHttpServletRequest req = new SendErrorParamSlingHttpServletRequest() {
            @Override
            public String getHeader(String name) {
                return name.equals(MediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };
        req.setSendError("true");
        
        PostResponse result = servlet.createPostResponse(req);
        assertTrue("Expected ErrorHandlingPostResponseWrapper PostResonse", result instanceof ErrorHandlingPostResponseWrapper);
       	result = ((ErrorHandlingPostResponseWrapper)result).getWrapped();
        assertTrue(result instanceof JSONResponse);
    }
    
    
    public void testPersistenceExceptionLogging() {
        Logger log = Mockito.mock(Logger.class);
        SlingHttpServletRequest mockRequest = Mockito.mock(SlingHttpServletRequest.class);
        Resource mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getPath()).thenReturn("/path");
        Mockito.when(mockRequest.getResource()).thenReturn(mockResource);
        Whitebox.setInternalState(servlet, "log", log);
        PostOperation operation = new DeleteOperation();
        Exception exception = new IOException("foo");
        
        Whitebox.setInternalState(servlet, "logStacktraceInExceptions", true);
        String expected = "Exception while handling POST on path [{}] with operation [{}]";
        servlet.logPersistenceException(mockRequest, operation, exception);
        Mockito.verify(log).warn(eq(expected),eq("/path"),eq("org.apache.sling.servlets.post.impl.operations.DeleteOperation"),eq(exception));
        
        Whitebox.setInternalState(servlet, "logStacktraceInExceptions", false);
        expected = "{} while handling POST on path [{}] with operation [{}]: {}";
        servlet.logPersistenceException(mockRequest, operation, exception);
        Mockito.verify(log).warn(eq(expected),eq("java.io.IOException"),eq("/path"),eq("org.apache.sling.servlets.post.impl.operations.DeleteOperation"),eq("foo"));  
    }
    

    /**
     * SLING-10006 - verify we get the error handling wrapped PostResponse
     */
    public void testGetHtmlResponseWithSendError() {
    	SendErrorParamSlingHttpServletRequest req = new SendErrorParamSlingHttpServletRequest();
        req.setSendError("true");
        
        PostResponse result = servlet.createPostResponse(req);
        assertTrue(result instanceof ErrorHandlingPostResponseWrapper);
       	result = ((ErrorHandlingPostResponseWrapper)result).getWrapped();
        assertTrue(result instanceof HtmlResponse);
    }

    public void testRedirection() throws Exception {
        String utf8Path = "\u0414\u0440\u0443\u0433\u0430";
        String encodedUtf8 = "%D0%94%D1%80%D1%83%D0%B3%D0%B0";
        testRedirection("/", "/fred", "*.html", "/fred.html");
        testRedirection("/xyz/", "/xyz/"+utf8Path, "*", "/xyz/"+encodedUtf8);
        testRedirection("/", "/fred/"+utf8Path, "/xyz/*", "/xyz/"+encodedUtf8);
        testRedirection("/", "/fred/"+utf8Path, null, null);
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
        SlingHttpServletRequest request = new RedirectServletRequest(redirect, requestPath);
        PostResponse htmlResponse = new HtmlResponse();
        htmlResponse.setPath(resourcePath);
        assertEquals(expected != null, servlet.redirectIfNeeded(request, htmlResponse, resp));
        assertEquals(expected, resp.redirectLocation);
    }

    /**
     *
     */
    private final class RedirectServletRequest extends MockSlingHttpServlet3Request {

        private String requestPath;
        private String redirect;

        private RedirectServletRequest(String redirect, String requestPath) {
            super(null, null, null, null, null);
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

    private final class RedirectServletResponse extends MockSlingHttpServlet3Response {

        private String redirectLocation;

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

    private static class StatusParamSlingHttpServletRequest extends
            MockSlingHttpServlet3Request {

        private String statusParam;

        public StatusParamSlingHttpServletRequest() {
            // nothing to setup, we don't care
            super(null, null, null, null, null);
        }

        @Override
        public String getParameter(String name) {
            if (SlingPostConstants.RP_STATUS.equals(name)) {
                return statusParam;
            }

            return super.getParameter(name);
        }

        void setStatusParam(String statusParam) {
            this.statusParam = statusParam;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }
    }

    private static class SendErrorParamSlingHttpServletRequest extends
	    MockSlingHttpServlet3Request {
		
		private String sendError;
		
		public SendErrorParamSlingHttpServletRequest() {
		    // nothing to setup, we don't care
		    super(null, null, null, null, null);
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
		
		public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		    return null;
		}
	}

}
