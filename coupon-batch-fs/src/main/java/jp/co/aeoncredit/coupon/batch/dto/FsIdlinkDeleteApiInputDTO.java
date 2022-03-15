package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDリンク操作（削除）
 * リクエストBody DTO
 * @author m-omori
 *
 */
public class FsIdlinkDeleteApiInputDTO {

	@JsonProperty("from")
	private From from = new From();
	
	public static class From {
		@JsonProperty("schema")
		private String schema = "AEON";
		
		@JsonProperty("key")
		private Key key = new Key();;
		
		public static class Key {
			@JsonProperty("type")
			private String type = "awtID";
			
			@JsonProperty("id")
			private String id;
		}
	}
	
	@JsonProperty("where")
	private Where where = new Where();
	
	public static class Where {
		
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
	}
	
	/**
	 * イオンウォレットトラッキングIDをセット
	 * @param awTrackingId ... イオンウォレットトラッキングID
	 */
	public void setAwTrackingId(String awTrackingId) {
		this.from.key.id = awTrackingId;
	}
	
	/**
	 * CPパスポートIDをセット
	 * @param cpPassportId ... CPパスポートID
	 */
	public void setCPPassId(String cpPassportId) {
		this.where.to.key.id = cpPassportId;
	}
}
