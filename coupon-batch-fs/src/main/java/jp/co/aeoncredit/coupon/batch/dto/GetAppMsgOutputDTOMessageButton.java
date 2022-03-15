package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ内Msg一覧取得API(GetAppMsgOutput).ButtonsのOutput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAppMsgOutputDTOMessageButton {

	/** id */
	@JsonProperty("id")
	private int id;
	
	/** ボタン表示文言 */
	@JsonProperty("name")
	private String name;
	
	/** 遷移先URL */
	@JsonProperty("url")
	private String url;
	
	/** トラッキングイベント名 */
	@JsonProperty("event_name")
	private String eventName;
	
	/** 累計タップ数 */
	@JsonProperty("total_tap_count")
	private int totalTapCount;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public int getTotalTapCount() {
		return totalTapCount;
	}

	public void setTotalTapCount(int totalTapCount) {
		this.totalTapCount = totalTapCount;
	}
	
	
}
