package jp.co.aeoncredit.coupon.batch.dto;

import javax.json.bind.annotation.JsonbProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Push通知配信停止API
 * エラー時のレスポンスDTO
 * @author m-omori
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelPushNotificationOutputErrorDTO {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;
	
	/** ステータス */
	@JsonbProperty("status")
	private String status;
	
	/** エラー内容 */
	@JsonProperty("error")
	private Error error;
	
	/**
	 * エラーオブジェクト
	 * @author m-omori
	 *
	 */
	public static class Error {
		
		/** エラーコード */
		@JsonProperty("code")
		private String code;
		
		/** エラーメッセージ */
		@JsonbProperty("message")
		private String message;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		
		
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}
	
	
}
