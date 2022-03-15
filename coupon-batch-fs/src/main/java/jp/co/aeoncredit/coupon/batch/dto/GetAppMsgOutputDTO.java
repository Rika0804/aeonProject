package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAppMsgOutputDTO {

	/** メッセージ名 */
	@JsonProperty("name")
	private String name;
	
	/** 表示順 */
	@JsonProperty("priority")
	private int priority;
	
	/** 配信番号 */
	@NotEmpty
	@JsonProperty("delivery_id")
	private String deliveryId;
	
	/** コンディション */
	@JsonProperty("condition")
	private GetAppMsgOutputDTOCondition condition;
	
	/** メッセージ */
	@JsonProperty("message")
	private List<GetAppMsgOutputDTOMessage> message;
	
	/** 配信ステータス */
	@JsonProperty("status")
	private String status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getDeliveryId() {
		return deliveryId;
	}

	public void setDeliveryId(String deliveryId) {
		this.deliveryId = deliveryId;
	}

	public GetAppMsgOutputDTOCondition getCondition() {
		return condition;
	}

	public void setCondition(GetAppMsgOutputDTOCondition condition) {
		this.condition = condition;
	}

	public List<GetAppMsgOutputDTOMessage> getMessage() {
		return message;
	}

	public void setMessage(List<GetAppMsgOutputDTOMessage> message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
}