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
package org.apache.sling.launchpad.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

public class UjaxInfoServlet extends SlingSafeMethodsServlet {
    /** Handle requests which start with this path */
    public static String PATH_PREFIX = "/ujax:";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {

        Map <String, Object> data = null;

        if(request.getPathInfo().equals(PATH_PREFIX + "sessionInfo.json")) {
            try {
                data = getSessionInfo(request);
            } catch(RepositoryException re) {
                throw new ServletException("RepositoryException in getSessionInfo(): " + re,re);
            }
        }

        if (data== null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,request.getPathInfo());
            return;
        }

        // render data in JSON format
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        final Writer out = response.getWriter();
        final JSONWriter w = new JSONWriter(out);
        try {
            w.object();
            for(Map.Entry<String, Object> e : data.entrySet()) {
                w.key(e.getKey());
                w.value(e.getValue());
            }
            w.endObject();

        } catch (JSONException jse) {
            out.write(jse.toString());

        } finally {
            out.flush();
        }
    }

    protected Map<String, Object> getSessionInfo(SlingHttpServletRequest request)
    throws RepositoryException, SlingException {
        final Map<String, Object> result = new HashMap<String, Object>();

        final Session s = (Session)request.getAttribute(Session.class.getName());
        result.put("workspace",s.getWorkspace().getName());
        result.put("userID",s.getUserID());

        return result;
    }


}