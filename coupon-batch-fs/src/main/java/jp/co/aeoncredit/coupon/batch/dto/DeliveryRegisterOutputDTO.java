package jp.co.aeoncredit.coupon.batch.dto;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Push通知登録API(DeliveryRegister)のOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryRegisterOutputDTO extends MapiOutputDTOBase{

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** status(Status) */
	@JsonProperty("status")
	private String status;

	/** result(Result) */
	@JsonProperty("result")
	private DeliveryRegisterOutputDTOResult result;

	/**
	 * status(status)を取得する。
	 * 
	 * @return status(status)
	 */
	@XmlElement(name = "status")
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
	public DeliveryRegisterOutputDTOResult getResult() {
		return result;
	}

	/**
	 * result(result)を設定する。
	 * 
	 * @param result result(result)
	 */
	public void setResult(DeliveryRegisterOutputDTOResult result) {
		this.result = result;
	}
}
