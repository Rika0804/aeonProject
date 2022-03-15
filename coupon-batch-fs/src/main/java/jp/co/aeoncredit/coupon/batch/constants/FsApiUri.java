package jp.co.aeoncredit.coupon.batch.constants;

/**
 * Fs Api Uri
 * 
 * @author nguyenphuongnga
 * @version 1.0
 */
public enum FsApiUri {

  /** FS API連携（新規登録・更新・更新） */
  AVAILABLE_STORE ("fs.regist.update.delete.batch.store.api.url.regist"),
  /** FS API連携(Push通知配信停止) */
  STOP_DELIVERY_PUSH_NOTIFICATION ("fs.cancel.batch.push.notification.api.url"),
  /** FS クーポン公開停止 */
  CANCEL_FS_COUPON ("fs.coupon.cancel.batch.api.url");
  /** 管理種別 */
  private String value;


  /**
   * Process Result Info
   * 
   * @param value 管理種別
   */
  private FsApiUri(String value) {
    this.value = value;
  }

  /**
   * 管理種別を取得する。
   * 
   * @return 管理種別
   */
  public String getValue() {
    return value;
  }

}
