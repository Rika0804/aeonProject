package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).ButtonsのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOButtons extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** id(id) */
	@JsonProperty("id")
	private String id;

	/** name(name) */
	@JsonProperty("name")
	private String name;

	/** url(url) */
	@JsonProperty("url")
	private String url;

	/** eventName(eventName) */
	@JsonProperty("event_name")
	private String eventName;

	/**
	 * id(id)を取得する。
	 * 
	 * @return id(id)
	 */
	public String getId() {
		return id;
	}

	/**
	 * id(id)を設定する。
	 * 
	 * @param id id(id)
	 */
	public void setId(String id) {
		this.id = id;
	}

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
	 * url(url)を取得する。
	 * 
	 * @return url
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
	 * eventName(eventName)を取得する。
	 * 
	 * @return eventName
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * eventName(eventName)を設定する。
	 * 
	 * @param eventName eventName(eventName)
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
}