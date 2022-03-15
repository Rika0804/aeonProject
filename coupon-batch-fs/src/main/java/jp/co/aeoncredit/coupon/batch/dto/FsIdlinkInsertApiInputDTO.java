package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDリンク操作（紐付けを行う）
 * リクエストBody DTO
 * @author m-omori
 *
 */
public class FsIdlinkInsertApiInputDTO {

	@JsonProperty("from")
	private From from = new From();

	public static class From {
		@JsonProperty("schema")
		private String schema = "AEON";

		@JsonProperty("key")
		private Key key = new Key();

		public static class Key {
			@JsonProperty("type")
			private String type = "awtID";

			@JsonProperty("id")
			private String id;
		}
	}

	@JsonProperty("to")
	private To to = new To();

	public static class To {
		@JsonProperty("schema")
		private String schema = "CreditCard";

		@JsonProperty("key")
		private Key key = new Key();

		public static class Key {
			@JsonProperty("type")
			private String type = "CPPassID";

			@JsonProperty("id")
			private String id;
		}
	}

	/**
	 * イオンウォレットトラッキングIDをセットする
	 * @param awTrackingId
	 */
	public void setAwTrackingId(String awTrackingId) {
		this.from.key.id = awTrackingId;
	}

	/**
	 * CPパスポートIDをセットする
	 * @param cpPassportId
	 */
	public void setCPPassId(String cpPassportId) {
		this.to.key.id = cpPassportId;
	}
}
