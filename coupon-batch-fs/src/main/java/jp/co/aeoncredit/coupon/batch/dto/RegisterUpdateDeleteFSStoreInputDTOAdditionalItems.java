package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;
/**
 * FS店舗登録・更新・削除APIリクエスト追加のアイテムパラメータDTO
 * 
 * @author nguyenphuongnga
 * @version 1.0
 */
public class RegisterUpdateDeleteFSStoreInputDTOAdditionalItems extends DTOBase {
  
  /**
   * 
   */
  private static final long serialVersionUID = 7228444496477916413L;

  /** 店舗サムネイル画像URL(STORE_THUMBNAIL_URL) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("thumbnail")
  private String storeThumbnailUrl;

  /** 店舗名略称(STORE_SHORTNAME) */
  @JsonProperty("shortName")
  private String storeShortname;

  /** 店舗住所(STORE_ADDRESS) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("address")
  private String storeAddress;

  /** 電話番号(STORE_PHONE) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("phone")
  private String storePhone;

  /** 営業時間(STORE_HOURS) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("hours")
  private String storeHours;

  /** 定休日(STORE_HOLIDAY) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("holiday")
  private String storeHoliday;


  /**
   * 店舗サムネイル画像URL(STORE_THUMBNAIL_URL)を取得する。
   * 
   * @return 店舗サムネイル画像URL(STORE_THUMBNAIL_URL)
   */
  public String getStoreThumbnailUrl() {
    return this.storeThumbnailUrl;
  }

  /**
   * 店舗サムネイル画像URL(STORE_THUMBNAIL_URL)を設定する。
   * 
   * @param storeThumbnailUrl 店舗サムネイル画像URL(STORE_THUMBNAIL_URL)
   */
  public void setStoreThumbnailUrl(String storeThumbnailUrl) {
    this.storeThumbnailUrl = storeThumbnailUrl;
  }

  /**
   * 店舗名略称(STORE_SHORTNAME)を取得する。
   * 
   * @return 店舗名略称(STORE_SHORTNAME)
   */
  public String getStoreShortname() {
    return this.storeShortname;
  }

  /**
   * 店舗名略称(STORE_SHORTNAME)を設定する。
   * 
   * @param storeShortname 店舗名略称(STORE_SHORTNAME)
   */
  public void setStoreShortname(String storeShortname) {
    this.storeShortname = storeShortname;
  }

  /**
   * 店舗住所(STORE_ADDRESS)を取得する。
   * 
   * @return 店舗住所(STORE_ADDRESS)
   */
  public String getStoreAddress() {
    return this.storeAddress;
  }

  /**
   * 店舗住所(STORE_ADDRESS)を設定する。
   * 
   * @param storeAddress 店舗住所(STORE_ADDRESS)
   */
  public void setStoreAddress(String storeAddress) {
    this.storeAddress = storeAddress;
  }

  /**
   * 電話番号(STORE_PHONE)を取得する。
   * 
   * @return 電話番号(STORE_PHONE)
   */
  public String getStorePhone() {
    return this.storePhone;
  }

  /**
   * 電話番号(STORE_PHONE)を設定する。
   * 
   * @param storePhone 電話番号(STORE_PHONE)
   */
  public void setStorePhone(String storePhone) {
    this.storePhone = storePhone;
  }

  /**
   * 営業時間(STORE_HOURS)を取得する。
   * 
   * @return 営業時間(STORE_HOURS)
   */
  public String getStoreHours() {
    return this.storeHours;
  }

  /**
   * 営業時間(STORE_HOURS)を設定する。
   * 
   * @param storeHours 営業時間(STORE_HOURS)
   */
  public void setStoreHours(String storeHours) {
    this.storeHours = storeHours;
  }

  /**
   * 定休日(STORE_HOLIDAY)を取得する。
   * 
   * @return 定休日(STORE_HOLIDAY)
   */
  public String getStoreHoliday() {
    return this.storeHoliday;
  }

  /**
   * 定休日(STORE_HOLIDAY)を設定する。
   * 
   * @param storeHoliday 定休日(STORE_HOLIDAY)
   */
  public void setStoreHoliday(String storeHoliday) {
    this.storeHoliday = storeHoliday;
  }



}
