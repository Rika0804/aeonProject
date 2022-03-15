package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * Push通知登録API(DeliveryRegister).ResultのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryRegisterOutputDTOResult extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** id(Id) */
	@JsonProperty("id")
	private Long id;

	/**
	 * id(id)を取得する。
	 * 
	 * @return id(id)
	 */
	public Long getId() {
		return id;
	}

	/**
	 * id(id)を設定する。
	 * 
	 * @param id id(id)
	 */
	public void setId(Long id) {
		this.id = id;
	}
}