/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.ibm.jp.awag.common.validator.StringFormat;
import javax.validation.Valid;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * クーポン更新API(CouponUpdate).AdditionalItemsのInput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponUpdateInputDTOAdditionalItems extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** クーポン画像(couponImage) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 150)
	@JsonProperty("image")
	private String couponImage;

	/** サムネイル(thumbnail) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 150)
	@JsonProperty("thumbnail")
	private String thumbnail;

	/** クーポンコード(couponCode) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, minLength = 1, maxLength = 20)
	@JsonProperty("couponCode")
	private String couponCode;

	/** クーポンコードリスト(couponCodeList) */
	@JsonProperty("couponCodeList")
	private List<String> couponCodeList;

	/** クーポンコード表示設定(couponCodeVisible) */
	@JsonProperty("couponCodeVisible")
	private String couponCodeVisible;

	/** 加盟店グループID(merchantGroupId) */
	@JsonProperty("_merchantGroupId")
	private String merchantGroupId;

	/** 加盟店名(merchantName) */
	@StringFormat(type = StringFormat.Type.FULL_CHAR, minLength = 1, maxLength = 12)
	@JsonProperty("_merchantName")
	private String merchantName;

	/** 画像見出し(imageHeading) */
	@StringFormat(type = StringFormat.Type.FULL_CHAR, minLength = 1, maxLength = 30)
	@JsonProperty("_imageHeading")
	private String imageHeading;

	/** 画像(image) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 150)
	@JsonProperty("_image")
	private String image;

	/** バーコード(barcode) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, maxLength = 32)
	@JsonProperty("_barcode")
	private String barcode;

	/** バーコード区分(barcodeType) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, minLength = 1, maxLength = 1)
	@JsonProperty("_barcodeType")
	private String barcodeType;

	/** カテゴリ(category) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, minLength = 1, maxLength = 99)
	@JsonProperty("_category")
	private String category;

	/** クーポン利用単位(useCountType) */
	@StringFormat(type = StringFormat.Type.HALF_NUM, minLength = 1, maxLength = 1)
	@JsonProperty("_useCountType")
	private String useCountType;

	/** 背景色(backColor) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 7)
	@JsonProperty("_backColor")
	private String backColor;

	/** クーポン利用方法区分(couponUseType) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, minLength = 1, maxLength = 1)
	@JsonProperty("_couponUseType")
	private String couponUseType;

	/** プロモーションコード用リンクテキスト見出し(promoLinkUrlHeading) */
	@StringFormat(type = StringFormat.Type.FULL_CHAR, minLength = 1, maxLength = 30)
	@JsonProperty("_promoLinkUrlHeading")
	private String promoLinkUrlHeading;

	/** プロモーションコード用リンクURL(promoLinkUrl) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, minLength = 1, maxLength = 150)
	@JsonProperty("_promoLinkUrl")
	private String promoLinkUrl;

	/** インセンティブテキスト(incentiveText) */
	@StringFormat(type = StringFormat.Type.ALL)
	@JsonProperty("_incentive_text")
	private String incentiveText;

	/** インセンティブ単位(incentiveUnit) */
	@StringFormat(type = StringFormat.Type.HALF_NUM_CHAR, minLength = 1, maxLength = 99)
	@JsonProperty("_incentive_unit")
	private String incentiveUnit;

	/** インセンティブ(incentive) */
	@Valid
	@JsonProperty("_incentive")
	private List<CouponUpdateInputDTOIncentive> incentive;

	/** テキスト(texts) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 150)
	@JsonProperty("_texts")
	private String texts;

	/** リンクURL(linkUrls) */
	@StringFormat(type = StringFormat.Type.HALF_CHAR, maxLength = 150)
	@JsonProperty("_linkUrls")
	private String linkUrls;

	/**
	 * クーポン画像(couponImage)を取得する。
	 * 
	 * @return クーポン画像(couponImage)
	 */
	public String getCouponImage() {
		return couponImage;
	}

	/**
	 * クーポン画像(couponImage)を設定する。
	 * 
	 * @param couponImage クーポン画像(couponImage)
	 */
	public void setCouponImage(String couponImage) {
		this.couponImage = couponImage;
	}

	/**
	 * サムネイル(thumbnail)を取得する。
	 * 
	 * @return サムネイル(thumbnail)
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * サムネイル(thumbnail)を設定する。
	 * 
	 * @param thumbnail サムネイル(thumbnail)
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * クーポンコード(couponCode)を取得する。
	 * 
	 * @return クーポンコード(couponCode)
	 */
	public String getCouponCode() {
		return couponCode;
	}

	/**
	 * クーポンコード(couponCode)を設定する。
	 * 
	 * @param couponCode クーポンコード(couponCode)
	 */
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	/**
	 * クーポンコードリスト(couponCodeList)を取得する。
	 * 
	 * @return クーポンコードリスト(couponCodeList)
	 */
	public List<String> getCouponCodeList() {
		return couponCodeList;
	}

	/**
	 * クーポンコードリスト(couponCodeList)を設定する。
	 * 
	 * @param couponCodeList クーポンコードリスト(couponCodeList)
	 */
	public void setCouponCodeList(List<String> couponCodeList) {
		this.couponCodeList = couponCodeList;
	}

	/**
	 * クーポンコード表示設定(couponCodeVisible)を取得する。
	 * 
	 * @return クーポンコード表示設定(couponCodeVisible)
	 */
	public String getCouponCodeVisible() {
		return couponCodeVisible;
	}

	/**
	 * クーポンコード表示設定(couponCodeVisible)を設定する。
	 * 
	 * @param couponCodeVisible クーポンコード表示設定(couponCodeVisible)
	 */
	public void setCouponCodeVisible(String couponCodeVisible) {
		this.couponCodeVisible = couponCodeVisible;
	}

	/**
	 * 加盟店グループID(merchantGroupId)を取得する。
	 * 
	 * @return 加盟店グループID(merchantGroupId)
	 */
	public String getMerchantGroupId() {
		return merchantGroupId;
	}

	/**
	 * 加盟店グループID(merchantGroupId)を設定する。
	 * 
	 * @param merchantGroupId 加盟店グループID(merchantGroupId)
	 */
	public void setMerchantGroupId(String merchantGroupId) {
		this.merchantGroupId = merchantGroupId;
	}

	/**
	 * 加盟店名(merchantName)を取得する。
	 * 
	 * @return 加盟店名(merchantName)
	 */
	public String getMerchantName() {
		return merchantName;
	}

	/**
	 * 加盟店名(merchantName)を設定する。
	 * 
	 * @param merchantName 加盟店名(merchantName)
	 */
	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	/**
	 * 画像見出し(imageHeading)を取得する。
	 * 
	 * @return 画像見出し(imageHeading)
	 */
	public String getImageHeading() {
		return imageHeading;
	}

	/**
	 * 画像見出し(imageHeading)を設定する。
	 * 
	 * @param imageHeading 画像見出し(imageHeading)
	 */
	public void setImageHeading(String imageHeading) {
		this.imageHeading = imageHeading;
	}

	/**
	 * 画像(image)を取得する。
	 * 
	 * @return 画像(image)
	 */
	public String getImage() {
		return image;
	}

	/**
	 * 画像(image)を設定する。
	 * 
	 * @param image 画像(image)
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * バーコード(barcode)を取得する。
	 * 
	 * @return バーコード(barcode)
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * バーコード(barcode)を設定する。
	 * 
	 * @param barcode バーコード(barcode)
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * バーコード区分(barcodeType)を取得する。
	 * 
	 * @return バーコード区分(barcodeType)
	 */
	public String getBarcodeType() {
		return barcodeType;
	}

	/**
	 * バーコード区分(barcodeType)を設定する。
	 * 
	 * @param barcodeType バーコード区分(barcodeType)
	 */
	public void setBarcodeType(String barcodeType) {
		this.barcodeType = barcodeType;
	}

	/**
	 * カテゴリ(category)を取得する。
	 * 
	 * @return カテゴリ(category)
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * カテゴリ(category)を設定する。
	 * 
	 * @param category カテゴリ(category)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * クーポン利用単位(useCountType)を取得する。
	 * 
	 * @return クーポン利用単位(useCountType)
	 */
	public String getUseCountType() {
		return useCountType;
	}

	/**
	 * クーポン利用単位(useCountType)を設定する。
	 * 
	 * @param useCountType クーポン利用単位(useCountType)
	 */
	public void setUseCountType(String useCountType) {
		this.useCountType = useCountType;
	}

	/**
	 * 背景色(backColor)を取得する。
	 * 
	 * @return 背景色(backColor)
	 */
	public String getBackColor() {
		return backColor;
	}

	/**
	 * 背景色(backColor)を設定する。
	 * 
	 * @param backColor 背景色(backColor)
	 */
	public void setBackColor(String backColor) {
		this.backColor = backColor;
	}

	/**
	 * クーポン利用方法区分(couponUseType)を取得する。
	 * 
	 * @return クーポン利用方法区分(couponUseType)
	 */
	public String getCouponUseType() {
		return couponUseType;
	}

	/**
	 * クーポン利用方法区分(couponUseType)を設定する。
	 * 
	 * @param couponUseType クーポン利用方法区分(couponUseType)
	 */
	public void setCouponUseType(String couponUseType) {
		this.couponUseType = couponUseType;
	}

	/**
	 * プロモーションコード用リンクテキスト見出し(promoLinkUrlHeading)を取得する。
	 * 
	 * @return プロモーションコード用リンクテキスト見出し(promoLinkUrlHeading)
	 */
	public String getPromoLinkUrlHeading() {
		return promoLinkUrlHeading;
	}

	/**
	 * プロモーションコード用リンクテキスト見出し(promoLinkUrlHeading)を設定する。
	 * 
	 * @param promoLinkUrlHeading プロモーションコード用リンクテキスト見出し(promoLinkUrlHeading)
	 */
	public void setPromoLinkUrlHeading(String promoLinkUrlHeading) {
		this.promoLinkUrlHeading = promoLinkUrlHeading;
	}

	/**
	 * プロモーションコード用リンクURL(promoLinkUrl)を取得する。
	 * 
	 * @return プロモーションコード用リンクURL(promoLinkUrl)
	 */
	public String getPromoLinkUrl() {
		return promoLinkUrl;
	}

	/**
	 * プロモーションコード用リンクURL(promoLinkUrl)を設定する。
	 * 
	 * @param promoLinkUrl プロモーションコード用リンクURL(promoLinkUrl)
	 */
	public void setPromoLinkUrl(String promoLinkUrl) {
		this.promoLinkUrl = promoLinkUrl;
	}

	/**
	 * インセンティブテキスト(incentiveText)を取得する。
	 * 
	 * @return インセンティブテキスト(incentiveText)
	 */
	public String getIncentiveText() {
		return incentiveText;
	}

	/**
	 * インセンティブテキスト(incentiveText)を設定する。
	 * 
	 * @param incentiveText インセンティブテキスト(incentiveText)
	 */
	public void setIncentiveText(String incentiveText) {
		this.incentiveText = incentiveText;
	}

	/**
	 * インセンティブ単位(incentiveUnit)を取得する。
	 * 
	 * @return インセンティブ単位(incentiveUnit)
	 */
	public String getIncentiveUnit() {
		return incentiveUnit;
	}

	/**
	 * インセンティブ単位(incentiveUnit)を設定する。
	 * 
	 * @param incentiveUnit インセンティブ単位(incentiveUnit)
	 */
	public void setIncentiveUnit(String incentiveUnit) {
		this.incentiveUnit = incentiveUnit;
	}

	/**
	 * インセンティブ(incentive)を取得する。
	 * 
	 * @return インセンティブ(incentive)
	 */
	public List<CouponUpdateInputDTOIncentive> getIncentive() {
		return incentive;
	}

	/**
	 * インセンティブ(incentive)を設定する。
	 * 
	 * @param incentive インセンティブ(incentive)
	 */
	public void setIncentive(List<CouponUpdateInputDTOIncentive> incentive) {
		this.incentive = incentive;
	}

	/**
	 * テキスト(texts)を取得する。
	 * 
	 * @return テキスト(texts)
	 */
	public String getTexts() {
		return texts;
	}

	/**
	 * テキスト(texts)を設定する。
	 * 
	 * @param texts テキスト(texts)
	 */
	public void setTexts(String texts) {
		this.texts = texts;
	}

	/**
	 * リンクURL(linkUrls)を取得する。
	 * 
	 * @return リンクURL(linkUrls)
	 */
	public String getLinkUrls() {
		return linkUrls;
	}

	/**
	 * リンクURL(linkUrls)を設定する。
	 * 
	 * @param linkUrls リンクURL(linkUrls)
	 */
	public void setLinkUrls(String linkUrls) {
		this.linkUrls = linkUrls;
	}

}
