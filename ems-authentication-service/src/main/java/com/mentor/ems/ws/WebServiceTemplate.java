package com.mentor.ems.ws;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mentor.ems.ws.response.TokenValidationResponse;
import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.ws.request.RevokeTokenRequest;
import com.mentor.ems.ws.response.TokenRevocationResponse;

public class WebServiceTemplate {

	private static Logger LOGGER = Logger.getLogger(WebServiceTemplate.class);

	private RestTemplate restWSTemplate;
	private String SERVER_URL;
	private String revokeTokenURL;

	public RestTemplate getRestWSTemplate() {
		return restWSTemplate;
	}

	public void setRestWSTemplate(RestTemplate restWSTemplate) {
		this.restWSTemplate = restWSTemplate;
		this.restWSTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
		    @Override
			protected boolean hasError(HttpStatus statusCode) {
		        return false;
		}});
	}

	public String getSERVER_URL() {
		return SERVER_URL;
	}

	public void setSERVER_URL(String sERVER_URL) {
		this.SERVER_URL = sERVER_URL;
	}
	
	public void setRevokeTokenURL(String revokeTokenURL) {
		this.revokeTokenURL = revokeTokenURL;
	}

	private HttpHeaders createHttpHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", "Bearer " + token);
		return headers;
	}

	public TokenValidationResponse getResponse(String token) {
		
		HttpHeaders headers = createHttpHeaders(token);
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        ResponseEntity<TokenValidationResponse> response = restWSTemplate.exchange(SERVER_URL, HttpMethod.GET, entity, TokenValidationResponse.class);

        LOGGER.debug("Token validation response status code from OpenAM server: "+response.getStatusCode());
        
		if (response.getStatusCode() == HttpStatus.OK) {
			
			return response.getBody();
		} else {
			LOGGER.error("Error response from OpenAM server. Response Code: "
					+ response.getStatusCode().toString() +" for token "+token);
			TokenValidationResponse tvs = new TokenValidationResponse();
			tvs.setStatus("401");
			return tvs;
		}
	}
	
	public boolean getTokenRevokeResponse(String token, String clientID, String clientSecret, String refToken) {
		RevokeTokenRequest request = getRevokeTokenRequest(token, clientID, clientSecret, refToken);
		ResponseEntity<TokenRevocationResponse> response = restWSTemplate
				.postForEntity(this.revokeTokenURL, request, TokenRevocationResponse.class);

		LOGGER.debug("Token revocation response status code from OpenAM server: "
				+ response.getStatusCode());

		if (response.getStatusCode() == HttpStatus.OK) {
			LOGGER.debug("Access token deactivated successfully.");
			return true;
		} else {
			LOGGER.debug("Error response from OpenAM server. Response Code: "
					+ response.getStatusCode().toString());
			return false;
		}
	}
	
	private RevokeTokenRequest getRevokeTokenRequest(String token, String clientID, String clientSecret, String refToken) {
		RevokeTokenRequest revoke = new RevokeTokenRequest();
		revoke.setClient_id(clientID);
		if(refToken != null && !EMSCommonConstants.EMPTY.equals(refToken))
			revoke.setToken(refToken.trim());
		else
			revoke.setToken(token);
		revoke.setClient_secret(clientSecret);
		return revoke;
	}
}
