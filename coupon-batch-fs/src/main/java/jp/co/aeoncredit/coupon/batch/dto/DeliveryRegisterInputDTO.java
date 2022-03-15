package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Push通知登録API(DeliveryRegister)のInput DTOクラス。
 */
public class DeliveryRegisterInputDTO{


	/** type(Type) */
	@JsonProperty("type")
	private String type;

	/** content_type(ContentType) */
	@JsonProperty("content_type")
	private String contentType;

	/** platform(Platform) */
	@JsonProperty("platform")
	private List<String> platform;

	/** popup(Popup) */
	@JsonProperty("popup")
	private String popup;


	/** title(Title) */
	@JsonProperty("title")
	private String title;

	/** content(Content) */
	@JsonProperty("content")	
	private String content;

	/** url(Url) */
	@JsonProperty("url")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String url;

	/** category(Category) */
	@JsonProperty("category")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String category;

	/** delivery_type(DeliveryType) */
	@JsonProperty("delivery_type")
	private String deliveryType;

	/** send_time(SendTime) */
	@JsonProperty("send_time")
	private String sendTime;

	/** segmentationId(Sent) */
	@JsonProperty("segmentation_id")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long segmentationId;

	/**
	 * type(type)を取得する。
	 * 
	 * @return type(type)
	 */
	public String getType() {
		return type;
	}

	/**
	 * type(type)を設定する。
	 * 
	 * @param type type(type)
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * content_type(content_type)を取得する。
	 * 
	 * @return content_type(content_type)
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * content_type(content_type)を設定する。
	 * 
	 * @param contentType content_type(content_type)
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * platform(platform)を取得する。
	 * 
	 * @return platform(platform)
	 */
	public List<String> getPlatform() {
		return platform;
	}

	/**
	 * platform(platform)を設定する。
	 * 
	 * @param platform platform(platform)
	 */
	public void setPlatform(List<String> platform) {
		this.platform = platform;
	}

	/**
	 * popup(popup)を取得する。
	 * 
	 * @return popup(popup)
	 */
	public String getPopup() {
		return popup;
	}

	/**
	 * popup(popup)を設定する。
	 * 
	 * @param popup popup(popup)
	 */
	public void setPopup(String popup) {
		this.popup = popup;
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
	 * content(content)を取得する。
	 * 
	 * @return content(content)
	 */
	public String getContent() {
		return content;
	}

	/**
	 * content(content)を設定する。
	 * 
	 * @param content content(content)
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * url(url)を取得する。
	 * 
	 * @return url(url)
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * url(url)を設定する。
	 * 
	 * @param url url(url)
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * category(category)を取得する。
	 * 
	 * @return category(category)
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * category(category)を設定する。
	 * 
	 * @param category category(category)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * delivery_type(delivery_type)を取得する。
	 * 
	 * @return delivery_type(delivery_type)
	 */
	public String getDeliveryType() {
		return deliveryType;
	}

	/**
	 * delivery_type(delivery_type)を設定する。
	 * 
	 * @param deliveryType delivery_type(delivery_type)
	 */
	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	/**
	 * send_time(send_time)を取得する。
	 * 
	 * @return send_time(send_time)
	 */
	public String getSendTime() {
		return sendTime;
	}

	/**
	 * send_time(send_time)を設定する。
	 * 
	 * @param sendTime send_time(send_time)
	 */
	public void setSendTime(String sendTime) {
		this.sendTime = sendTime;
	}

	
	/**
	 * segmentationId(segmentationId)を取得する。
	 * 
	 * @return segmentationId(segmentationId)
	 */
	public Long getSegmentationId() {
		return segmentationId;
	}

	/**
	 * segmentationId(segmentationId)を設定する。
	 * 
	 * @param segmentationId segmentationId(segmentationId)
	 */
	public void setSegmentationId(Long segmentationId) {
		this.segmentationId = segmentationId;
	}

}