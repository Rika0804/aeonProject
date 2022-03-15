package jp.co.aeoncredit.coupon.batch.constants;

/**
 * Fs Import Data Process Mode enum
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public enum FsImportDataProcessMode {
  /** 差分 */
  DIFF("diff", "差分"),
  /** 全量 */
  FULL("full", "全量");

  /** 管理種別 */
  private String value;

  /** コード定義 */
  private String definition;

  /**
   * Fs Import Data Process Mode Info
   * 
   * @param value 管理種別
   * @param definition コード定義
   */
  private FsImportDataProcessMode(String value, String definition) {
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
