package jp.co.aeoncredit.coupon.batch.dto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ内Msg一覧取得API(GetAppMsgOutput).PeriodのOutput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

public class GetAppMsgOutputDTOConditionTriggerPeriod {

	/**開始日時*/
	@JsonProperty("start")
	private Timestamp start;

	/**終了日時*/
	@JsonProperty("end")
	private Timestamp end;

	/**
	 * startを取得する
	 * @return start
	 */
	public Timestamp getStart() {
		return start;
	}

	/**
	 * startを設定する
	 * @param start
	 */
	public void setStart(Timestamp start) {
		this.start = start;
	}

	/**
	 * endを取得する
	 * @return end
	 */
	public Timestamp getEnd() {
		return end;
	}

	/**
	 * end
	 * @param end
	 */
	public void setEnd(Timestamp end) {
		this.end = end;
	}
}
