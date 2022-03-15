package jp.co.aeoncredit.coupon.batch.constants;

/**
 * B18B0077_FSID-Link登録・更新・削除バッチ
 * 起動モード
 * @author m-omori
 *
 */
public enum FsIdlinkProcessMode {

	/** ID-Linkモード */
	IDLINK("1", "ID-Linkモード"),
	/** register-userモード */
	REGIST_USER("2", "register-userモード"),
	/** deleteモード */
	DELETE("3", "deleteモード");
	
	FsIdlinkProcessMode(String value, String modeName) {
		this.value = value;
		this.modeName = modeName;
	}

	private String value;
	private String modeName;

	/**
	 * 実行時に引数で渡されるコード値
	 * @return
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * 起動モード名称
	 * @return
	 */
	public String getModeName() {
		return modeName;
	}
}
