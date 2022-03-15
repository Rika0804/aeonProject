package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録・更新API インセンティブINPUTDTO(全クーポン共通)
 */
public class CouponRegisterApiInputDTOIncentive {

	/** インセンティブ表示順 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_priority")
	private String priority;

	/** クーポン本文テキスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_couponText")
	private String couponText;

	/** 割引率 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_amount")
	private String amount;

	/** 割引単位 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_unit")
	private String unit;

	/** 対象商品 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("_products")
	private String products;

	/**
	 * @return priority
	 */
	public String getPriority() {
		return priority;
	}

	/**
	 * @param priority セットする priority
	 */
	public void setPriority(String priority) {
		this.priority = priority;
	}

	/**
	 * @return couponText
	 */
	public String getCouponText() {
		return couponText;
	}

	/**
	 * @param couponText セットする couponText
	 */
	public void setCouponText(String couponText) {
		this.couponText = couponText;
	}

	/**
	 * @return amount
	 */
	public String getAmount() {
		return amount;
	}

	/**
	 * @param amount セットする amount
	 */
	public void setAmount(String amount) {
		this.amount = amount;
	}

	/**
	 * @return unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * @param unit セットする unit
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * @return products
	 */
	public String getProducts() {
		return products;
	}

	/**
	 * @param products セットする products
	 */
	public void setProducts(String products) {
		this.products = products;
	}

}
