package jp.co.aeoncredit.coupon.batch.constants;

/**
 * process result enum
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public enum ProcessResult {
  /** 正常終了_戻り値 */
  SUCCESS("0", "正常終了_戻り値"),
  /** 異常終了_戻り値 */
  FAILURE("1", "異常終了_戻り値");

  /** 管理種別 */
  private String value;

  /** コード定義 */
  private String definition;

  /**
   * Process Result Info
   * 
   * @param value 管理種別
   * @param definition コード定義
   */
  private ProcessResult(String value, String definition) {
    this.value = value;
    this.definition = definition;
  }

  /**
   * 管理種別を取得する。
   * 
   * @return 管理種別
   */
  public String getValue() {
    return value;
  }

  /**
   * コード定義を取得する。
   * 
   * @return コード定義
   */
  public String getDefinition() {
    return definition;
  }
}
