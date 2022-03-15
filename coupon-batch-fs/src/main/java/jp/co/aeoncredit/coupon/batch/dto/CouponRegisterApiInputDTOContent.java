package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API 本文INPUTDTO(センサーイベントクーポン用)
 */
public class CouponRegisterApiInputDTOContent {

	/** コンテンツ本文 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("information_text")
	private String informationText;

	/** リンクテキスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("information_link_title")
	private String informationLinkTitle;

	/** リンクURL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("information_image")
	private String informationImage;

	/** 表示終了日 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("information_displayEndDate")
	private String informationDisplayEndDate;

	/** 表示終了時刻 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("information_displayEndTime")
	private String informationDisplayEndTime;

	/**
	 * @return informationText
	 */
	public String getInformationText() {
		return informationText;
	}

	/**
	 * @param informationText セットする informationText
	 */
	public void setInformationText(String informationText) {
		this.informationText = informationText;
	}

	/**
	 * @return informationLinkTitle
	 */
	public String getInformationLinkTitle() {
		return informationLinkTitle;
	}

	/**
	 * @param informationLinkTitle セットする informationLinkTitle
	 */
	public void setInformationLinkTitle(String informationLinkTitle) {
		this.informationLinkTitle = informationLinkTitle;
	}

	/**
	 * @return informationImage
	 */
	public String getInformationImage() {
		return informationImage;
	}

	/**
	 * @param informationImage セットする informationImage
	 */
	public void setInformationImage(String informationImage) {
		this.informationImage = informationImage;
	}

	/**
	 * @return informationDisplayEndDate
	 */
	public String getInformationDisplayEndDate() {
		return informationDisplayEndDate;
	}

	/**
	 * @param informationDisplayEndDate セットする informationDisplayEndDate
	 */
	public void setInformationDisplayEndDate(String informationDisplayEndDate) {
		this.informationDisplayEndDate = informationDisplayEndDate;
	}

	/**
	 * @return informationDisplayEndTime
	 */
	public String getInformationDisplayEndTime() {
		return informationDisplayEndTime;
	}

	/**
	 * @param informationDisplayEndTime セットする informationDisplayEndTime
	 */
	public void setInformationDisplayEndTime(String informationDisplayEndTime) {
		this.informationDisplayEndTime = informationDisplayEndTime;
	}

}
