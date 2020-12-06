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
package org.apache.sling.servlets.post.exceptions;

import org.apache.sling.api.resource.PersistenceException;


/**
 *  Indicates that the input does not meet the necessary preconditions required by JCR.
 *  In that case the client should redo the request with a changed input.
 *
 */

public class IncorrectInputException extends PersistenceException {


  private static final long serialVersionUID = 1L;
  
  public IncorrectInputException(String message, Exception e) {
    super(message, e);
  }
  
  public IncorrectInputException(final String msg,
          final Throwable cause,
          final String resourcePath,
          final String propertyName) {
            super(msg,cause,resourcePath,propertyName);
            
            }
  

}
