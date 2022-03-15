package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).MessageのOutput DTOクラス。
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOMessage extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** templateId(templateId) */
	@JsonProperty("template_id")
	private String templateId;

	/** title(title) */
	@JsonProperty("title")
	private String title;

	/** body(body) */
	@JsonProperty("body")
	private String body;

	/** imageUrl(imageUrl) */
	@JsonProperty("image_url")
	private String imageUrl;

	/** buttons(Buttons) */
	@JsonProperty("buttons")
	private List<GetFanshipInAppMsgOutputDTOButtons> buttons;

	/** messageId(messageId) */
	@JsonProperty("message_id")
	private Integer messageId;

	/**
	 * templateId(templateId)を取得する。
	 * 
	 * @return templateId(templateId)
	 */
	public String getTemplateId() {
		return templateId;
	}
	
	/**
	 * templateId(templateId)を設定する。
	 * 
	 * @param templateId templateId(templateId)
	 */
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	/**
	 * title(title)を取得する。
	 * 
	 * @return title(title)
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * title(title)を設定する。
	 * 
	 * @param title title(title)
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * body(body)を取得する。
	 * 
	 * @return body(body)
	 */
	public String getBody() {
		return body;
	}

	/**
	 * body(body)を設定する。
	 * 
	 * @param body body(body)
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * imageUrl(imageUrl)を取得する。
	 * 
	 * @return imageUrl(imageUrl)
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * imageUrl(imageUrl)を設定する。
	 * 
	 * @param imageUrl imageUrl(imageUrl)
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * buttons(buttons)を取得する。
	 * 
	 * @return buttons(buttons)
	 */
	public List<GetFanshipInAppMsgOutputDTOButtons> getButtons() {
		return buttons;
	}

	/**
	 * buttons(buttons)を設定する。
	 * 
	 * @param buttons buttons(buttons)
	 */
	public void setButtons(List<GetFanshipInAppMsgOutputDTOButtons> buttons) {
		this.buttons = buttons;
	}

	/**
	 * messageId(messageId)を取得する。
	 * 
	 * @return messageId
	 */
	public Integer getMessageId() {
		return messageId;
	}

	/**
	 * messageId(messageId)を設定する。
	 * 
	 * @param messageId messageId(messageId)
	 */
	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}
}