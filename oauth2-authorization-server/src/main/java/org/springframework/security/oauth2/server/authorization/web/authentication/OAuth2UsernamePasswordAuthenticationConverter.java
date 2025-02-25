/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.server.authorization.web.authentication;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Attempts to extract an Access Token Request from {@link HttpServletRequest} for the OAuth 2.0 Password Grant
 * and then converts it to an {@link OAuth2UsernamePasswordAuthenticationToken} used for authenticating the authorization grant.
 *
 * @author leegecho
 * @since 0.1.2
 * @see AuthenticationConverter
 * @see OAuth2UsernamePasswordAuthenticationToken
 * @see OAuth2TokenEndpointFilter
 */
public final class OAuth2UsernamePasswordAuthenticationConverter implements AuthenticationConverter {

	@Nullable
	@Override
	public Authentication convert(HttpServletRequest request) {
		// grant_type (REQUIRED)
		String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
		if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
			return null;
		}

		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

		MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

		// username (REQUIRED)
		String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
		if (!StringUtils.hasText(username) ||
				parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.USERNAME);
		}

		// password (REQUIRED)
		String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
		if (StringUtils.hasText(password) &&
				parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.PASSWORD);
		}

		// @formatter:off
		Map<String, Object> additionalParameters = parameters
				.entrySet()
				.stream()
				.filter(e -> !e.getKey().equals(OAuth2ParameterNames.GRANT_TYPE) &&
						!e.getKey().equals(OAuth2ParameterNames.CLIENT_ID) &&
						!e.getKey().equals(OAuth2ParameterNames.CLIENT_SECRET) &&
						!e.getKey().equals(OAuth2ParameterNames.USERNAME) &&
						!e.getKey().equals(OAuth2ParameterNames.PASSWORD))
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        // @formatter:on

		return new OAuth2UsernamePasswordAuthenticationToken(clientPrincipal, username, password, additionalParameters);
	}
}
