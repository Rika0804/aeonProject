/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate).DeliveryのInput
 * DTOクラス。
 */
public class EventCouponAppIntroTestCreateInputDTODelivery extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** メッセージ名(name) */
	@JsonProperty("name")
	private String name;

	/** 表示順(priority) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("priority")
	private Integer priority;

	/** コンディション(Condition) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("condition")
	private EventCouponAppIntroTestCreateInputDTOCondition condition;

	/** メッセージ(Message) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("message")
	private List<EventCouponAppIntroTestCreateInputDTOMessage> message;

	/**
	 * メッセージ名(name)を取得する。
	 * 
	 * @return メッセージ名(name)
	 */
	public String getName() {
		return name;
	}

	/**
	 * メッセージ名(name)を設定する。
	 * 
	 * @param name メッセージ名(name)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 表示順(priority)を取得する。
	 * 
	 * @return 表示順(priority)
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * 表示順(priority)を設定する。
	 * 
	 * @param priority 表示順(priority)
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * コンディション(condition)を取得する。
	 * 
	 * @return コンディション(condition)
	 */
	public EventCouponAppIntroTestCreateInputDTOCondition getCondition() {
		return condition;
	}

	/**
	 * コンディション(condition)を設定する。
	 * 
	 * @param condition コンディション(condition)
	 */
	public void setCondition(EventCouponAppIntroTestCreateInputDTOCondition condition) {
		this.condition = condition;
	}

	/**
	 * @return message
	 */
	public List<EventCouponAppIntroTestCreateInputDTOMessage> getMessage() {
		return message;
	}

	/**
	 * @param message セットする message
	 */
	public void setMessage(List<EventCouponAppIntroTestCreateInputDTOMessage> message) {
		this.message = message;
	}

}