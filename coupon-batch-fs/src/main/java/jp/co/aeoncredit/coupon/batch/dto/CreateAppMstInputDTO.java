package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg登録API(CreateAppMst).DeliveryのInput DTOクラス。
 */
public class CreateAppMstInputDTO extends DTOBase {

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
	@JsonProperty("condition")
	private CreateAppMstInputDTOCondition condition;

	/** メッセージ(Message) */
	@JsonProperty("message")
	private List<CreateAppMstInputDTOMessage> message;

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
	public CreateAppMstInputDTOCondition getCondition() {
		return condition;
	}

	/**
	 * コンディション(condition)を設定する。
	 * 
	 * @param condition コンディション(condition)
	 */
	public void setCondition(CreateAppMstInputDTOCondition condition) {
		this.condition = condition;
	}

	/**
	 * メッセージ(message)を取得する。
	 * 
	 * @return メッセージ(message)
	 */
	public List<CreateAppMstInputDTOMessage> getMessage() {
		return message;
	}

	/**
	 * メッセージ(message)を設定する。
	 * 
	 * @param message メッセージ(message)
	 */
	public void setMessage(List<CreateAppMstInputDTOMessage> message) {
		this.message = message;
	}
}
