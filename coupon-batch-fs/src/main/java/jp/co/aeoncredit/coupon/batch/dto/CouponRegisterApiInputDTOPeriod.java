package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API 配布期間INPUTDTO(アプリイベントクーポン・センサーイベントクーポン用)
 */
public class CouponRegisterApiInputDTOPeriod {

	/** 開始日時 */
	@JsonProperty("start")
	private String start;

	/** 終了日時 */
	@JsonProperty("end")
	private String end;

	/**
	 * @return start
	 */
	public String getStart() {
		return start;
	}

	/**
	 * @param start セットする start
	 */
	public void setStart(String start) {
		this.start = start;
	}

	/**
	 * @return end
	 */
	public String getEnd() {
		return end;
	}

	/**
	 * @param end セットする end
	 */
	public void setEnd(String end) {
		this.end = end;
	}

}
