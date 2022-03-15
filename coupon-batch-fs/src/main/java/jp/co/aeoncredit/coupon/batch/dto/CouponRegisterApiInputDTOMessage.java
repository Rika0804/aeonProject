package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API メッセージINPUTDTO(アプリイベントクーポン用)
 */
public class CouponRegisterApiInputDTOMessage {

	/** メッセージタイプ */
	@JsonProperty("template_name")
	private String templateName;

	/** タイトル */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("title")
	private String title;

	/** 本文 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("body")
	private String body;

	/** 画像URL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("image_url")
	private String imageUrl;

	/** ボタン配列 */
	@JsonProperty("buttons")
	private List<CouponRegisterApiInputDTOButtons> buttons;

	/**
	 * @return templateName
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * @param templateName セットする templateName
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title セットする title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @param body セットする body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * @return imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @param imageUrl セットする imageUrl
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * @return buttons
	 */
	public List<CouponRegisterApiInputDTOButtons> getButtons() {
		return buttons;
	}

	/**
	 * @param buttons セットする buttons
	 */
	public void setButtons(List<CouponRegisterApiInputDTOButtons> buttons) {
		this.buttons = buttons;
	}

}
