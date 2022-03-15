package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ログインAPI用レスポンスDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class LoginOutputDTO extends MapiOutputDTOBase {
	/** 結果 */
	@JsonProperty("result")
	private LoginOutputDTOResult result;

	/**
	 * @return result
	 */
	public LoginOutputDTOResult getResult() {
		return result;
	}

	/**
	 * @param result セットする result
	 */
	public void setResult(LoginOutputDTOResult result) {
		this.result = result;
	}

}
