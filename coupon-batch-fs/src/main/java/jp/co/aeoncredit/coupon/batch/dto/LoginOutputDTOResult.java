package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LoginOutputDTOResult {
	/** 認証トークン */
	@JsonProperty("auth_token")
	private String authToken;

	/**
	 * @return authToken
	 */
	public String getAuthToken() {
		return authToken;
	}

	/**
	 * @param authToken セットする authToken
	 */
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
}
