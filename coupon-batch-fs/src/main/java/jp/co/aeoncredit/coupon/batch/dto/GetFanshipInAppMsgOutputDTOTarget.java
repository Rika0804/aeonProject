package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).TargetのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOTarget extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** segmentationId(segmentationId) */
	@JsonProperty("segmentation_id")
	private Integer segmentationId;

	/**
	 * segmentationId(segmentationId)を取得する。
	 * 
	 * @return segmentationId(segmentationId)
	 */
	public Integer getSegmentationId() {
		return segmentationId;
	}

	/**
	 * segmentationId(segmentationId)を設定する。
	 * 
	 * @param segmentationId segmentationId(segmentationId)
	 */
	public void setSegmentationId(Integer segmentationId) {
		this.segmentationId = segmentationId;
	}
}
