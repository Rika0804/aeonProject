package jp.co.aeoncredit.coupon.batch.dto;

/**
 * B18B0077_FSID-Link登録・更新・削除バッチ
 * DBから取得したレコードを格納するDTO
 * 
 * @author m-omori
 *
 */
public class FsIdlinkDTO {

	/** イオンウォレットトラッキングID */
	private String awTrackingId;

	/** CPパスポートID */
	private String cpPassportId;
	
	public String getAwTrackingId() {
		return awTrackingId;
	}

	public void setAwTrackingId(String awTrackingId) {
		this.awTrackingId = awTrackingId;
	}

	public String getCpPassportId() {
		return cpPassportId;
	}

	public void setCpPassportId(String cpPassportId) {
		this.cpPassportId = cpPassportId;
	}
	
	/**
	 * DBから取得したデータをセットする
	 * @param data ... DBから取得したデータ
	 */
	public void setValue(Object[] data) {
		this.awTrackingId = (String) data[0];
		this.cpPassportId = (String) data[1];
	}
	
}
