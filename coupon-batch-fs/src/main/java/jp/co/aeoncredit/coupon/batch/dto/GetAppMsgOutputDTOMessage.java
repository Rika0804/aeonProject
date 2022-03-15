package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ内Msg一覧取得API(GetAppMsgOutput).MessageのOutput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAppMsgOutputDTOMessage {

	/** メッセージタイプ */
	@JsonProperty("template_name")
	private String templateName;
	
	/** タイトル */
	@JsonProperty("title")
	private String title;
	
	/** 本文 */
	@JsonProperty("body")
	private String body;
	
	/** 画像URL */
	@JsonProperty("image_url")
	private String imageUrl;
	
	/** 累計配信数 */
	@JsonProperty("total_delivery_count")
	private int totalDeliveryCount;
	
	/** ボタン配列 */
	@JsonProperty("buttons")
	private List<GetAppMsgOutputDTOMessageButton> buttons;
	
	/** メッセージID */
	@JsonProperty("message_id")
	private int messageId;

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public int getTotalDeliveryCount() {
		return totalDeliveryCount;
	}

	public void setTotalDeliveryCount(int totalDeliveryCount) {
		this.totalDeliveryCount = totalDeliveryCount;
	}

	public List<GetAppMsgOutputDTOMessageButton> getButtons() {
		return buttons;
	}

	public void setButtons(List<GetAppMsgOutputDTOMessageButton> buttons) {
		this.buttons = buttons;
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}
	
}






