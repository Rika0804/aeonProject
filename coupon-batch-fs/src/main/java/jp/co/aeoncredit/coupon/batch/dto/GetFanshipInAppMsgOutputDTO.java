package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg)のInput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetFanshipInAppMsgOutputDTO extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** name(name) */
	@JsonProperty("name")
	private String name;

	/** priority(priority) */
	@JsonProperty("priority")
	private Integer priority;

	/** deliveryId(deliveryId) */
	@JsonProperty("delivery_id")
	private Integer deliveryId;

	/** condition(condition) */
	@JsonProperty("condition")
	private GetFanshipInAppMsgOutputDTOCondition condition;

	/** message(message) */
	@JsonProperty("message")
	private List<GetFanshipInAppMsgOutputDTOMessage> message;

	/**
	 * name(name)を取得する。
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * name(name)を設定する。
	 * 
	 * @param name name(name)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * priority(priority)を取得する。
	 * 
	 * @return priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * priority(priority)を設定する。
	 * 
	 * @param priority priority(priority)
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * deliveryId(deliveryId)を取得する。
	 * 
	 * @return deliveryId
	 */
	public Integer getDeliveryId() {
		return deliveryId;
	}

	/**
	 * deliveryId(deliveryId)を設定する。
	 * 
	 * @param deliveryId deliveryId(deliveryId)
	 */
	public void setDeliveryId(Integer deliveryId) {
		this.deliveryId = deliveryId;
	}

	/**
	 * condition(condition)を取得する。
	 * 
	 * @return condition
	 */
	public GetFanshipInAppMsgOutputDTOCondition getCondition() {
		return condition;
	}

	/**
	 * condition(condition)を設定する。
	 * 
	 * @param condition condition(condition)
	 */
	public void setCondition(GetFanshipInAppMsgOutputDTOCondition condition) {
		this.condition = condition;
	}

	/**
	 * message(message)を取得する。
	 * 
	 * @return message
	 */
	public List<GetFanshipInAppMsgOutputDTOMessage> getMessage() {
		return message;
	}

	/**
	 * message(message)を設定する。
	 * 
	 * @param message message(message)
	 */
	public void setMessage(List<GetFanshipInAppMsgOutputDTOMessage> message) {
		this.message = message;
	}
}
