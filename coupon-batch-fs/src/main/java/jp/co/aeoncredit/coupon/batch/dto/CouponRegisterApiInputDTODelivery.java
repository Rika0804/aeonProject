package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API アプリ内メッセージ設定INPUTDTO(アプリイベントクーポン用)
 */
public class CouponRegisterApiInputDTODelivery {

	/** メッセージ名 */
	@JsonProperty("name")
	private String name;

	/** 表示順 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("priority")
	private Integer priority;

	/** コンディション */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("condition")
	private CouponRegisterApiInputDTOCondition condition;

	/** メッセージ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("message")
	private List<CouponRegisterApiInputDTOMessage> message;

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name セットする name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * @param priority セットする priority
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * @return condition
	 */
	public CouponRegisterApiInputDTOCondition getCondition() {
		return condition;
	}

	/**
	 * @param condition セットする condition
	 */
	public void setCondition(CouponRegisterApiInputDTOCondition condition) {
		this.condition = condition;
	}

	/**
	 * @return message
	 */
	public List<CouponRegisterApiInputDTOMessage> getMessage() {
		return message;
	}

	/**
	 * @param message セットする message
	 */
	public void setMessage(List<CouponRegisterApiInputDTOMessage> message) {
		this.message = message;
	}

}
