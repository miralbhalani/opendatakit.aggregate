/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.common.security.server;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.SecurityBeanDefs;
import org.opendatakit.common.security.SecurityUtils;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.common.GrantedAuthorityNames;
import org.opendatakit.common.security.spring.RegisteredUsersTable;
import org.opendatakit.common.web.CallingContext;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * GWT Server implementation for the SecurityService interface.
 * This provides privileges context to the client and is therefore
 * accessible to anyone with a ROLE_USER privilege.
 * This should be accessed over SSL to prevent eavesdropping when
 * changing personal passwords; SSL is otherwise not required.
 *  
 * @author mitchellsundt@gmail.com
 *
 */
public class SecurityServiceImpl extends RemoteServiceServlet implements
org.opendatakit.common.security.client.security.SecurityService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7360632450727200941L;

	@Override
	public String getUserDisplayName() throws AccessDeniedException {

	    HttpServletRequest req = this.getThreadLocalRequest();
	    CallingContext cc = ContextFactory.getCallingContext(this, req);

	    User user = cc.getUserService().getCurrentUser();
		return user.getNickname();
	}

	@Override
	public boolean isRegisteredUser() throws AccessDeniedException {

	    HttpServletRequest req = this.getThreadLocalRequest();
	    CallingContext cc = ContextFactory.getCallingContext(this, req);

	    User user = cc.getUserService().getCurrentUser();
	    
	    return user.isRegistered();
	}

	@Override
	public boolean isAnonymousUser() throws AccessDeniedException {

	    HttpServletRequest req = this.getThreadLocalRequest();
	    CallingContext cc = ContextFactory.getCallingContext(this, req);

	    User user = cc.getUserService().getCurrentUser();
	    
	    return user.isAnonymous();
	}

	@Override
	public void setUserPassword(String password) throws AccessDeniedException, DatastoreFailureException {

	    HttpServletRequest req = this.getThreadLocalRequest();
	    CallingContext cc = ContextFactory.getCallingContext(this, req);

	    Datastore ds = cc.getDatastore();
	    User user = cc.getUserService().getCurrentUser();
		RegisteredUsersTable userDefinition = null;
		try {
			userDefinition = RegisteredUsersTable.getUserByUri(user.getUriUser(), ds, user);
			if ( userDefinition == null ) {
				throw new AccessDeniedException("User is not a registered user.");
			}

			MessageDigestPasswordEncoder mde = (MessageDigestPasswordEncoder) cc.getBean(SecurityBeanDefs.BASIC_AUTH_PASSWORD_ENCODER);
			String salt = UUID.randomUUID().toString().substring(0,8);
			String fullPass = mde.encodePassword(password, salt);
			userDefinition.setBasicAuthPassword(fullPass);
			userDefinition.setBasicAuthSalt(salt);
			String fullDigestAuthPass = SecurityUtils.getDigestAuthenticationPasswordHash(
												userDefinition.getUsername(),
												password, 
												cc.getUserService().getCurrentRealm() );
            userDefinition.setDigestAuthPassword(fullDigestAuthPass);
			ds.putEntity(userDefinition, user);
		} catch ( ODKDatastoreException e ) {
			e.printStackTrace();
			throw new DatastoreFailureException(e.getMessage());
		}
	}

	@Override
	public UserSecurityInfo getUserInfo() throws AccessDeniedException,
			DatastoreFailureException {

	    HttpServletRequest req = this.getThreadLocalRequest();
	    CallingContext cc = ContextFactory.getCallingContext(this, req);

	    Datastore ds = cc.getDatastore();
	    User user = cc.getCurrentUser();
	    
	    String uriUser = user.getUriUser();
    	UserSecurityInfo info;
		try {
			if ( user.isRegistered() ) {
		    	RegisteredUsersTable t;
				t = RegisteredUsersTable.getUserByUri(uriUser, ds, user);
				if ( t != null ) {
					info = new UserSecurityInfo(t.getUsername(), t.getNickname(), t.getEmail(), 
																UserSecurityInfo.UserType.REGISTERED);
					SecurityServiceUtil.setAuthenticationLists(info, t.getUri(), cc);
				} else {
					throw new DatastoreFailureException("Unable to retrieve user record");
				}
			} else if ( user.isAnonymous() ) {
	    		info = new UserSecurityInfo(User.ANONYMOUS_USER, User.ANONYMOUS_USER_NICKNAME, null, 
												UserSecurityInfo.UserType.AUTHENTICATED);
	    		SecurityServiceUtil.setAuthenticationListsForSpecialUser(info, GrantedAuthorityNames.USER_IS_ANONYMOUS, cc);
			} else {
	    		String name = uriUser.substring(SecurityUtils.MAILTO_COLON.length());
	    		String nickname = name.substring(0,name.indexOf(SecurityUtils.AT_SIGN));
	    		info = new UserSecurityInfo(name, nickname, name, 
												UserSecurityInfo.UserType.AUTHENTICATED);
	    		SecurityServiceUtil.setAuthenticationListsFromDirectAuthorities(info, user.getDirectAuthorities(), cc);
	    	}
		} catch (ODKDatastoreException e) {
			e.printStackTrace();
			throw new DatastoreFailureException(e);
		}
		return info;
	}
}
