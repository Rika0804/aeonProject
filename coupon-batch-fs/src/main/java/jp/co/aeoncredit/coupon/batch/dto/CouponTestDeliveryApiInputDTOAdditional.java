package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * その他クーポンテスト配信API 任意追加項目INPUTDTO
 */
public class CouponTestDeliveryApiInputDTOAdditional {

	/** クーポン画像 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("image")
	private String couponImage;

	/** サムネイル */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("thumbnail")
	private String thumbnail;

	/** クーポンコード */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("couponCode")
	private String couponCode;

	/** クーポンコードリスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("couponCodeList")
	private String[] couponCodeList;

	/** クーポンコード表示設定 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("couponCodeVisible")
	private Boolean couponCodeVisible;

	/** 加盟店グループID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_merchantId")
	private String merchantId;

	/** 加盟店名 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_merchantName")
	private String merchantName;

	/** 画像見出し */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_imageHeading")
	private String imageHeading;

	/** 画像 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_image")
	private String image;

	/** バーコード */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_barcode")
	private String barcode;

	/** バーコード区分 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_barcodeType")
	private String barcodeType;

	/** 配信種別 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_category")
	private String category;

	/** クーポン利用単位 */
	@JsonProperty("_useCountType")
	private String useCountType;

	/** 背景色 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_backColor")
	private String backColor;

	/** クーポン利用方法区分 */
	@JsonProperty("_couponUseType")
	private String couponUseType;

	/** プロモーションコード用リンクテキスト見出し */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_promoLinkUrlHeading")
	private String promoLinkUrlHeading;

	/** プロモーションコード用リンクURL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_promoLinkUrl")
	private String promoLinkUrl;

	/** インセンティブテキスト */
	@JsonProperty("_incentive_text")
	private String incentiveText;

	/** インセンティブ単位 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_incentive_unit")
	private String incentiveUnit;

	/** インセンティブ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_incentive")
	private List<CouponTestDeliveryApiInputDTOIncentive> incentive;

	/** テキスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_texts")
	private String texts;

	/** リンクURL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_linkUrls")
	private String linkUrls;

	/**
	 * @return couponImage
	 */
	public String getCouponImage() {
		return couponImage;
	}

	/**
	 * @param couponImage セットする couponImage
	 */
	public void setCouponImage(String couponImage) {
		this.couponImage = couponImage;
	}

	/**
	 * @return thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * @param thumbnail セットする thumbnail
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * @return couponCode
	 */
	public String getCouponCode() {
		return couponCode;
	}

	/**
	 * @param couponCode セットする couponCode
	 */
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	/**
	 * @return couponCodeList
	 */
	public String[] getCouponCodeList() {
		return couponCodeList;
	}

	/**
	 * @param couponCodeList セットする couponCodeList
	 */
	public void setCouponCodeList(String[] couponCodeList) {
		this.couponCodeList = couponCodeList;
	}

	/**
	 * @return couponCodeVisible
	 */
	public Boolean getCouponCodeVisible() {
		return couponCodeVisible;
	}

	/**
	 * @param couponCodeVisible セットする couponCodeVisible
	 */
	public void setCouponCodeVisible(Boolean couponCodeVisible) {
		this.couponCodeVisible = couponCodeVisible;
	}

	/**
	 * @return merchantId
	 */
	public String getMerchantId() {
		return merchantId;
	}

	/**
	 * @param merchantId セットする merchantId
	 */
	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	/**
	 * @return merchantName
	 */
	public String getMerchantName() {
		return merchantName;
	}

	/**
	 * @param merchantName セットする merchantName
	 */
	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	/**
	 * @return imageHeading
	 */
	public String getImageHeading() {
		return imageHeading;
	}

	/**
	 * @param imageHeading セットする imageHeading
	 */
	public void setImageHeading(String imageHeading) {
		this.imageHeading = imageHeading;
	}

	/**
	 * @return image
	 */
	public String getImage() {
		return image;
	}

	/**
	 * @param image セットする image
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * @return barcode
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * @param barcode セットする barcode
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * @return barcodeType
	 */
	public String getBarcodeType() {
		return barcodeType;
	}

	/**
	 * @param barcodeType セットする barcodeType
	 */
	public void setBarcodeType(String barcodeType) {
		this.barcodeType = barcodeType;
	}

	/**
	 * @return category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category セットする category
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return useCountType
	 */
	public String getUseCountType() {
		return useCountType;
	}

	/**
	 * @param useCountType セットする useCountType
	 */
	public void setUseCountType(String useCountType) {
		this.useCountType = useCountType;
	}

	/**
	 * @return backColor
	 */
	public String getBackColor() {
		return backColor;
	}

	/**
	 * @param backColor セットする backColor
	 */
	public void setBackColor(String backColor) {
		this.backColor = backColor;
	}

	/**
	 * @return couponUseType
	 */
	public String getCouponUseType() {
		return couponUseType;
	}

	/**
	 * @param couponUseType セットする couponUseType
	 */
	public void setCouponUseType(String couponUseType) {
		this.couponUseType = couponUseType;
	}

	/**
	 * @return promoLinkUrlHeading
	 */
	public String getPromoLinkUrlHeading() {
		return promoLinkUrlHeading;
	}

	/**
	 * @param promoLinkUrlHeading セットする promoLinkUrlHeading
	 */
	public void setPromoLinkUrlHeading(String promoLinkUrlHeading) {
		this.promoLinkUrlHeading = promoLinkUrlHeading;
	}

	/**
	 * @return promoLinkUrl
	 */
	public String getPromoLinkUrl() {
		return promoLinkUrl;
	}

	/**
	 * @param promoLinkUrl セットする promoLinkUrl
	 */
	public void setPromoLinkUrl(String promoLinkUrl) {
		this.promoLinkUrl = promoLinkUrl;
	}

	/**
	 * @return incentiveText
	 */
	public String getIncentiveText() {
		return incentiveText;
	}

	/**
	 * @param incentiveText セットする incentiveText
	 */
	public void setIncentiveText(String incentiveText) {
		this.incentiveText = incentiveText;
	}

	/**
	 * @return incentiveUnit
	 */
	public String getIncentiveUnit() {
		return incentiveUnit;
	}

	/**
	 * @param incentiveUnit セットする incentiveUnit
	 */
	public void setIncentiveUnit(String incentiveUnit) {
		this.incentiveUnit = incentiveUnit;
	}

	/**
	 * @return incentive
	 */
	public List<CouponTestDeliveryApiInputDTOIncentive> getIncentive() {
		return incentive;
	}

	/**
	 * @param incentive セットする incentive
	 */
	public void setIncentive(List<CouponTestDeliveryApiInputDTOIncentive> incentive) {
		this.incentive = incentive;
	}

	/**
	 * @return texts
	 */
	public String getTexts() {
		return texts;
	}

	/**
	 * @param texts セットする texts
	 */
	public void setTexts(String texts) {
		this.texts = texts;
	}

	/**
	 * @return linkUrls
	 */
	public String getLinkUrls() {
		return linkUrls;
	}

	/**
	 * @param linkUrls セットする linkUrls
	 */
	public void setLinkUrls(String linkUrls) {
		this.linkUrls = linkUrls;
	}

}
