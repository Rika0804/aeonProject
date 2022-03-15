package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg登録API(CreateAppMst).ButtonsのInput DTOクラス。
 */
public class CreateAppMstInputDTOButtons extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** id(id) */
	@JsonProperty("id")
	private String id;

	/** ボタン表示文言(name) */
	@JsonProperty("name")
	private String name;

	/** 遷移先URL(url) */
	@JsonProperty("url")
	private String url;

	/** トラッキングイベント名(event_name) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
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
	 * @param i id(id)
	 */
	public void setId(String i) {
		this.id = i;
	}

	/**
	 * ボタン表示文言(name)を取得する。
	 * 
	 * @return ボタン表示文言(name)
	 */
	public String getName() {
		return name;
	}

	/**
	 * ボタン表示文言(name)を設定する。
	 * 
	 * @param name ボタン表示文言(name)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 遷移先URL(url)を取得する。
	 * 
	 * @return 遷移先URL(url)
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 遷移先URL(url)を設定する。
	 * 
	 * @param url 遷移先URL(url)
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * トラッキングイベント名(event_name)を取得する。
	 * 
	 * @return トラッキングイベント名(event_name)
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * トラッキングイベント名(event_name)を設定する。
	 * 
	 * @param eventName トラッキングイベント名(event_name)
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

}
