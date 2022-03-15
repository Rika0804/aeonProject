package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API ターゲットINPUTDTO(アプリイベントクーポン用)
 */
public class CouponRegisterApiInputDTOTarget {

	/** セグメントID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("segmentation_id")
	private String segmentationId;

	/**
	 * @return segmentationId
	 */
	public String getSegmentationId() {
		return segmentationId;
	}

	/**
	 * @param segmentationId セットする segmentationId
	 */
	public void setSegmentationId(String segmentationId) {
		this.segmentationId = segmentationId;
	}

}
