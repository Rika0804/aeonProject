package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API ボタン配列INPUTDTO(アプリイベントクーポン用)
 */
public class CouponRegisterApiInputDTOButtons {

	/** id */
	@JsonProperty("id")
	private Integer id;

	/** ボタン表示文言 */
	@JsonProperty("name")
	private String name;

	/** 遷移先URL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("url")
	private String url;

	/** トラッキングイベント名 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("event_name")
	private String eventName;

	/**
	 * @return id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id セットする id
	 */
	public void setId(Integer id) {
		this.id = id;
	}

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
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url セットする url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return eventName
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * @param eventName セットする eventName
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

}
