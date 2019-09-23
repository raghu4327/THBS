package com.mentor.ems.interceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.exception.InvalidTokenException;
import com.mentor.ems.common.util.AuthenticationUtil;
import com.mentor.ems.security.exceptions.SecurityException;
import com.mentor.ems.security.spi.HashGenerator;
import com.mentor.ems.security.spi.SecurityProvider;
import com.mentor.ems.security.util.SecurityConstants;
import com.mentor.ems.security.util.Util;
import com.mentor.ems.ws.WebServiceTemplate;
import com.mentor.ems.ws.response.TokenValidationResponse;
import com.mentor.ems.common.util.StringUtil;

public class TokenInterceptor implements HandlerInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TokenInterceptor.class);

	private WebServiceTemplate webServiceTemplate;

	private RedisTemplate<String, String> redisOpenAMTemplate;

	private HashGenerator hashGenerator;

	private SecurityProvider securityProvider;

	private String logoutRequestURI;

	private String keyingMaterial;

	public void setKeyingMaterial(String keyingMaterial) {
		this.keyingMaterial = keyingMaterial;
	}

	public void setLogoutRequestURI(String logoutRequestURI) {
		this.logoutRequestURI = logoutRequestURI;
	}

	public void setSecurityProvider(SecurityProvider securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setHashGenerator(HashGenerator hashGenerator) {
		this.hashGenerator = hashGenerator;
	}

	public void setRedisOpenAMTemplate(RedisTemplate<String, String> redisOpenAMTemplate) {
		this.redisOpenAMTemplate = redisOpenAMTemplate;
	}

	public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {
		this.webServiceTemplate = webServiceTemplate;
	}

	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		LOGGER.debug("Request execution completed.");
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		LOGGER.debug("Request postHandle execution completed");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) throws Exception {
		LOGGER.debug("Request preHandle invoked.");
		long startTime=System.currentTimeMillis();
		String subject = request.getHeader("sub");
		String requestMethod = request.getMethod().trim();
		if (AuthenticationUtil.isBypassRequest(requestMethod, subject, request.getRequestURI())) {
			return true;
		}
		
		if (AuthenticationUtil.isBypassRequestForRootUser(requestMethod, subject, request.getRequestURI())) {
			return true;
		}
		
		String bearerToken = request.getHeader("Authorization");
		String nonce = request.getHeader("nonce");
		String requestURI = request.getRequestURI();
		String clientSecret = request.getHeader("ClientAuth");
		String refToken = request.getHeader("RefreshAuthorization");
		if (bearerToken == null || "".equals(bearerToken) || !bearerToken.startsWith("Bearer ") || subject == null
				|| EMSCommonConstants.EMPTY.equals(subject)) {
			LOGGER.info("Request does not contain token/subject/nonce. Request is blocked for further processing.");
			 throw getInvalidTokenResponse();
		}
		String accessToken = bearerToken.trim().split("Bearer ")[1];
		LOGGER.info("Incoming token "+accessToken);
		boolean tokenStatus = isValidAccessToken(accessToken, nonce, subject);
		if (tokenStatus && requestURI != null && requestURI.equalsIgnoreCase(this.logoutRequestURI)) {
			revokeAccessTokenFromOpenAM(accessToken, clientSecret, refToken);
			removeAccessTokenFromRedis(accessToken);
			return true;
		}
		if (requestURI.equalsIgnoreCase(this.logoutRequestURI)) {
			return true;
		}
		if (tokenStatus) {
			long endTime = System.currentTimeMillis();
			LOGGER.info("Time Taken in TokenInterceptor-PreHandle method---:"+(endTime - startTime));
			LOGGER.info("Received valid access token. Continuing service execution.");
			return true;
		}
		 throw getInvalidTokenResponse();
		 
	}

	private void revokeAccessTokenFromOpenAM(String token, String clientSecret, String refToken) {
		TokenValidationResponse tvr = getTokenValidationResponse(token);
		this.webServiceTemplate.getTokenRevokeResponse(token, tvr.getAud(), clientSecret, refToken);
	}

	private void removeAccessTokenFromRedis(String accessToken) {
		String accessTokenHash;
		try {
			accessTokenHash = this.hashGenerator.generateSHA2Hash(accessToken);
			if (this.redisOpenAMTemplate.hasKey(accessTokenHash)) {
				LOGGER.debug("User is logged out. Removing access token information available from Redis.");
				this.redisOpenAMTemplate.delete(accessTokenHash);
			}
		} catch (SecurityException e) {
			LOGGER.error("Unable to delete access token from Redis cache. ", e);
		}
	}

	private InvalidTokenException getInvalidTokenResponse() {
		return new InvalidTokenException("Request does not contain valid access details.", HttpStatus.UNAUTHORIZED,
				SecurityConstants.UNAUTORIZED_ERROR_CODE);
	}

	private boolean isValidAccessToken(String accessToken, String nonce, String subject) {
		TokenValidationResponse tvr = getTokenValidationResponse(accessToken);
		String tokenRspNonce = tvr.getNonce();
		if (tvr.getStatus() != null && tvr.getStatus().equals(SecurityConstants.UNAUTORIZED_ERROR_CODE)) {
			LOGGER.debug("Invalid access token.");
			return false;
		} else if (!tvr.getSub().equalsIgnoreCase(subject)) {
			LOGGER.debug("Token was issued to some other user. Request will not be processed.");
			return false;
		} else if (tokenRspNonce != null && !EMSCommonConstants.EMPTY.equals(tokenRspNonce) && !tokenRspNonce.equalsIgnoreCase(nonce)) { // Added
			LOGGER.debug("Request header does not contain valid nonce. Request will not be processed.");
			return false;
		} else if (Util.getCurrentTimestamp() >= new Long(tvr.getExp() + "000")) {
			LOGGER.debug("Access token was expired.");
			return false;
		}

		try {
			if (!tvr.isCacheToRedis())
				setTokenIntoRedis(accessToken, tvr);
		} catch (SecurityException e) {
			LOGGER.error("Unable to cache access token. ", e);
		} catch (JsonProcessingException e) {
			LOGGER.error("Unable to convert token response to JSON. ", e);
		}
		return true;
	}

	private TokenValidationResponse getTokenValidationResponse(String accessToken) {
		String accessTokenHash;
		TokenValidationResponse tokenValidationResponse = null;
		try {
			accessTokenHash = this.hashGenerator.generateSHA2Hash(accessToken);

			if (this.redisOpenAMTemplate.hasKey(accessTokenHash)) {
				LOGGER.debug("Access token information is available in Redis. Not querying to OpenAM server.");
				tokenValidationResponse = getTokenInfoFromRedis(accessTokenHash);
			}

		} catch (SecurityException | JsonParseException | JsonMappingException e) {
			LOGGER.error("Unable to get Token info from redis.", e);
		} catch (IOException e) {
			LOGGER.error("IOException occurred. Unable to get Token info from redis.", e);
		}
		
		if (tokenValidationResponse != null ) {
			return tokenValidationResponse;
		} else {
			LOGGER.warn("tokenValidationResponse attempt to retrieve from Redis expired already!");
		}
		return this.webServiceTemplate.getResponse(accessToken);
	}

	private void setTokenIntoRedis(String token, TokenValidationResponse tokenInformation)
			throws SecurityException, JsonProcessingException {

		String hashedTokenKey = this.hashGenerator.generateSHA2Hash(token);
		String json = convertObjectToJSON(tokenInformation);
		String encryptedTokenInfo = this.securityProvider.encrypt(json, this.keyingMaterial);

		this.redisOpenAMTemplate.opsForValue().set(hashedTokenKey, encryptedTokenInfo);
		this.redisOpenAMTemplate.expire(hashedTokenKey, Long.parseLong(tokenInformation.getExpires_in()),
				TimeUnit.MILLISECONDS);
		LOGGER.debug("Access token was cached successfully in Redis.");
	}

	private TokenValidationResponse getTokenInfoFromRedis(String hashedTokenKey) throws SecurityException, IOException {
		String encTokenValue = this.redisOpenAMTemplate.opsForValue().get(hashedTokenKey);
		if( !StringUtil.isNullOrEmpty( encTokenValue )) {
			String tokenValue = this.securityProvider.decrypt(encTokenValue, this.keyingMaterial);
			return convertJSONToObject(tokenValue);
		} else {
			LOGGER.warn("encTokenValue value retrieved from the Redis is blank(Perhaps, expired or race condition?)");
			return null;
		}
	}

	private String convertObjectToJSON(TokenValidationResponse response) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		response.setCacheToRedis(true);
		return mapper.writeValueAsString(response);
	}

	private TokenValidationResponse convertJSONToObject(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, TokenValidationResponse.class);
	}

}
