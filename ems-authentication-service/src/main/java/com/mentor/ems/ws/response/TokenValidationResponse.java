package com.mentor.ems.ws.response;

public class TokenValidationResponse {

	private String tokenName;

	private String sub;

	private String[] scope;

	private ResponseJSON responseJSON;

	private String authGrantId;

	private String statusText;

	private String expires_in;

	private String iat;

	private String realm;

	private String openid;

	private String access_token;

	private String profile;

	private String grant_type;

	private String status;

	private String nbf;

	private String iss;

	private String readyState;

	private String responseText;

	private String auth_time;

	private String exp;

	private String auditTrackingId;

	private String nonce;

	private String aud;

	private String token_type;

	private String jti;
	
	private boolean cacheToRedis = false;
	
	public boolean isCacheToRedis() {
		return cacheToRedis;
	}

	public void setCacheToRedis(boolean cacheToRedis) {
		this.cacheToRedis = cacheToRedis;
	}

	public String getTokenName() {
		return tokenName;
	}

	public void setTokenName(String tokenName) {
		this.tokenName = tokenName;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	public String[] getScope() {
		return scope;
	}

	public void setScope(String[] scope) {
		this.scope = scope;
	}

	public ResponseJSON getResponseJSON() {
		return responseJSON;
	}

	public void setResponseJSON(ResponseJSON responseJSON) {
		this.responseJSON = responseJSON;
	}

	public String getAuthGrantId() {
		return authGrantId;
	}

	public void setAuthGrantId(String authGrantId) {
		this.authGrantId = authGrantId;
	}

	public String getStatusText() {
		return statusText;
	}

	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	public String getExpires_in() {
		return expires_in;
	}

	public void setExpires_in(String expires_in) {
		this.expires_in = expires_in;
	}

	public String getIat() {
		return iat;
	}

	public void setIat(String iat) {
		this.iat = iat;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getOpenid() {
		return openid;
	}

	public void setOpenid(String openid) {
		this.openid = openid;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(String grant_type) {
		this.grant_type = grant_type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getNbf() {
		return nbf;
	}

	public void setNbf(String nbf) {
		this.nbf = nbf;
	}

	public String getIss() {
		return iss;
	}

	public void setIss(String iss) {
		this.iss = iss;
	}

	public String getReadyState() {
		return readyState;
	}

	public void setReadyState(String readyState) {
		this.readyState = readyState;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public String getAuth_time() {
		return auth_time;
	}

	public void setAuth_time(String auth_time) {
		this.auth_time = auth_time;
	}

	public String getExp() {
		return exp;
	}

	public void setExp(String exp) {
		this.exp = exp;
	}

	public String getAuditTrackingId() {
		return auditTrackingId;
	}

	public void setAuditTrackingId(String auditTrackingId) {
		this.auditTrackingId = auditTrackingId;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getAud() {
		return aud;
	}

	public void setAud(String aud) {
		this.aud = aud;
	}

	public String getToken_type() {
		return token_type;
	}

	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}

	public String getJti() {
		return jti;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}

	@Override
	public String toString() {
		return "ClassPojo [tokenName = " + tokenName + ", sub = " + sub
				+ ", scope = " + scope + ", responseJSON = " + responseJSON
				+ ", authGrantId = " + authGrantId + ", statusText = "
				+ statusText + ", expires_in = " + expires_in + ", iat = "
				+ iat + ", realm = " + realm + ", openid = " + openid
				+ ", access_token = " + access_token + ", profile = " + profile
				+ ", grant_type = " + grant_type + ", status = " + status
				+ ", nbf = " + nbf + ", iss = " + iss + ", readyState = "
				+ readyState + ", responseText = " + responseText
				+ ", auth_time = " + auth_time + ", exp = " + exp
				+ ", auditTrackingId = " + auditTrackingId + ", nonce = "
				+ nonce + ", aud = " + aud + ", token_type = " + token_type
				+ ", jti = " + jti + "]";
	}
}
