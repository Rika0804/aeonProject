package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * FS店舗登録・更新・削除APIリクエストパラメータDTO
 * 
 * @author nguyenphuongnga
 * @version 1.0
 */
public class RegisterUpdateDeleteFSStoreInputDTO extends DTOBase {

  /**
   * 
   */
  private static final long serialVersionUID = 4993470910187554619L;

  /** 店舗名(STORE_NAME) */
  @JsonProperty("name")
  private String storeName;

  /** additional_items オブジェクト */
  @JsonProperty("additional_items")
  private RegisterUpdateDeleteFSStoreInputDTOAdditionalItems additionalItems;

  /** 店舗GPS緯度(STORE_LATITUDE) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("latitude")
  private String storeLatitude;

  /** 店舗GPS経度(STORE_LONGITUDE) */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("longitude")
  private String storeLongitude;


  /**
   * 店舗名(STORE_NAME)を取得する。
   * 
   * @return 店舗名(STORE_NAME)
   */
  public String getStoreName() {
    return this.storeName;
  }

  /**
   * additional_items オブジェクトを設定する。
   * 
   * @param storeName 店舗名(STORE_NAME)
   */
  public void setAdditionalItems(
      RegisterUpdateDeleteFSStoreInputDTOAdditionalItems additionalItems) {
    this.additionalItems = additionalItems;
  }

  /**
   * additional_items オブジェクトを取得する。
   * 
   * @return 店舗名(STORE_NAME)
   */
  public RegisterUpdateDeleteFSStoreInputDTOAdditionalItems getAdditionalItems() {
    return this.additionalItems;
  }

  /**
   * 店舗名(STORE_NAME)を設定する。
   * 
   * @param storeName 店舗名(STORE_NAME)
   */
  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }


  /**
   * 店舗GPS緯度(STORE_LATITUDE)を取得する。
   * 
   * @return 店舗GPS緯度(STORE_LATITUDE)
   */
  public String getStoreLatitude() {
    return this.storeLatitude;
  }

  /**
   * 店舗GPS緯度(STORE_LATITUDE)を設定する。
   * 
   * @param storeLatitude 店舗GPS緯度(STORE_LATITUDE)
   */
  public void setStoreLatitude(String storeLatitude) {
    this.storeLatitude = storeLatitude;
  }

  /**
   * 店舗GPS経度(STORE_LONGITUDE)を取得する。
   * 
   * @return 店舗GPS経度(STORE_LONGITUDE)
   */
  public String getStoreLongitude() {
    return this.storeLongitude;
  }

  /**
   * 店舗GPS経度(STORE_LONGITUDE)を設定する。
   * 
   * @param storeLongitude 店舗GPS経度(STORE_LONGITUDE)
   */
  public void setStoreLongitude(String storeLongitude) {
    this.storeLongitude = storeLongitude;
  }


}
