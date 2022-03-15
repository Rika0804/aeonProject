package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class GetFsSegmentListOutputDTO {

  /** セグメントID */
  @JsonProperty("id")
  private Long id;

  /** セグメント名称 */
  @JsonProperty("name")
  private String name;

  /** セグメント人数 */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("number_of_people")
  private String numberOfPeople;

  /** 作成日 */
  @JsonProperty("created")
  private String created;

  /** セグメントタイプ */
  @JsonProperty("type")
  private String type;

  /** エラー情報 */
  @JsonProperty("error_infomation")
  private String errorInfomation;

  /**
   * セグメントID(id)を取得する。
   * 
   * @return セグメントID(id)
   */
  public Long getId() {
    return id;
  }

  /**
   * セグメントID(id)を設定する。
   * 
   * @param id セグメントID(id)
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * セグメント名称(name)を取得する。
   * 
   * @return セグメント名称(name)
   */
  public String getName() {
    return name;
  }

  /**
   * セグメント名称(name)を設定する。
   * 
   * @param name セグメント名称(name)
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * セグメント人数(numberOfPeople)を取得する。
   * 
   * @return セグメント人数(numberOfPeople)
   */
  public String getNumberOfPeople() {
    return numberOfPeople;
  }

  /**
   * セグメント人数(numberOfPeople)を設定する。
   * 
   * @param numberOfPeople セグメント人数(numberOfPeople)
   */
  public void setNumberOfPeople(String numberOfPeople) {
    this.numberOfPeople = numberOfPeople;
  }

  /**
   * 作成日(created)を取得する。
   * 
   * @return 作成日(created)
   */
  public String getCreated() {
    return created;
  }

  /**
   * 作成日(created)を設定する。
   * 
   * @param created 作成日(created)
   */
  public void setCreated(String created) {
    this.created = created;
  }

  /**
   * セグメントタイプ(type)を取得する。
   * 
   * @return セグメントタイプ(type)
   */
  public String getType() {
    return type;
  }

  /**
   * セグメントタイプ(type)を設定する。
   * 
   * @param type セグメントタイプ(type)
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * エラー情報(errorInfomation)を取得する。
   * 
   * @return エラー情報(errorInfomation)
   */
  public String getErrorInfomation() {
    return errorInfomation;
  }

  /**
   * エラー情報(errorInfomation)を設定する。
   * 
   * @param errorInfomation エラー情報(errorInfomation)
   */
  public void setErrorInfomation(String errorInfomation) {
    this.errorInfomation = errorInfomation;
  }

}
