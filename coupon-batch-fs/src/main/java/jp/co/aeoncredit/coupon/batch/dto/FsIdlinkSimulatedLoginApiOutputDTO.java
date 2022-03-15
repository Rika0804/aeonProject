package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 疑似ログインAPI実行結果DTO
 * @author m-omori
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class FsIdlinkSimulatedLoginApiOutputDTO {

	@JsonProperty("status")
	private String status;
	
	@JsonProperty("timestamp")
	private String timestamp;
	
	@JsonProperty("result")
	private String result;
	
	@JsonProperty("error")
	private Error error;
	
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class Error {
		
		@JsonProperty("code")
		private String code;
		
		@JsonProperty("developerMessage")
		private String developerMessage;
		
		@JsonProperty("userMessage")
		private String userMessage;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getDeveloperMessage() {
			return developerMessage;
		}

		public void setDeveloperMessage(String developerMessage) {
			this.developerMessage = developerMessage;
		}

		public String getUserMessage() {
			return userMessage;
		}

		public void setUserMessage(String userMessage) {
			this.userMessage = userMessage;
		}
		
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}
	
	
}
