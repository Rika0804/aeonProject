package jp.co.aeoncredit.coupon.batch.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API 配布対象位置INPUTDTO(センサーイベントクーポン用)
 */
public class CouponRegisterApiInputDTOLocation {

	/** 緯度 */
	@JsonProperty("lat")
	private BigDecimal lat;

	/** 経度 */
	@JsonProperty("lon")
	private BigDecimal lon;

	/** 範囲 */
	@JsonProperty("radius")
	private Integer radius;

	/**
	 * @return lat
	 */
	public BigDecimal getLat() {
		return lat;
	}

	/**
	 * @param lat セットする lat
	 */
	public void setLat(BigDecimal lat) {
		this.lat = lat;
	}

	/**
	 * @return lon
	 */
	public BigDecimal getLon() {
		return lon;
	}

	/**
	 * @param lon セットする lon
	 */
	public void setLon(BigDecimal lon) {
		this.lon = lon;
	}

	/**
	 * @return radius
	 */
	public Integer getRadius() {
		return radius;
	}

	/**
	 * @param radius セットする radius
	 */
	public void setRadius(Integer radius) {
		this.radius = radius;
	}

}
