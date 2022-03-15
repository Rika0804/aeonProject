package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * テスト端末登録・解除API(TestDeviceAddOrRemove)のOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDeviceAddOrRemoveOutputDTO extends MapiOutputDTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** status(status) */
	@JsonProperty(value = "status")
	private String status;

	/** result(result) */
	@JsonProperty(value = "result")
	private TestDeviceAddOrRemoveOutputDTOResult result;

	/**
	 * status(status)を取得する。
	 * 
	 * @return status(status)
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * status(status)を設定する。
	 * 
	 * @param status status(status)
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * result(result)を取得する。
	 * 
	 * @return result(result)
	 */
	public TestDeviceAddOrRemoveOutputDTOResult getResult() {
		return result;
	}

	/**
	 * result(result)を設定する。
	 * 
	 * @param result result(result)
	 */
	public void setResult(TestDeviceAddOrRemoveOutputDTOResult result) {
		this.result = result;
	}
}
