package jp.co.aeoncredit.coupon.batch.constants;

/**
 * batch info enum
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public enum BatchInfo {
	/** QRコードリスト作成 */
	B18BC002("B18BC002", "QRコードリスト作成"),
	/** 配信予定・依頼リスト出力 */
	B18B0009("B18B0009", "配信予定・依頼リスト出力"),
	/** FSクーポン登録・更新・削除バッチ */
	B18B0011("B18B0011", "FSクーポン登録・更新・削除"),
	/** FSクーポン配信バッチ */
	B18B0012("B18B0012", "FSクーポン配信"),
	/** FS店舗登録・更新・削除バッチ */
	B18B0013("B18B0013", "FS店舗登録・更新・削除"),
	/** FSログ取込（アプリ利用イベント） */
	B18B0030("B18B0030", "FSログ取込（アプリ利用イベント）"),
	/** FSログ取込（iBeacon検出） */
	B18B0032("B18B0032", "FSログ取込（iBeacon検出）"),
	/** FSログ取込（ユーザ情報） */
	B18B0034("B18B0034", "FSログ取込（ユーザ情報）"),
	/** FSログ取込（登録店舗データ） */
	B18B0036("B18B0036", "FSログ取込（登録店舗データ）"),
	/** FSログ取込（来店ユーザ） */
	B18B0037("B18B0037", "FSログ取込（来店ユーザ）"),
	/** FSログ取込（セグメントマッチユーザー） */
	B18B0038("B18B0038", "FSログ取込（セグメントマッチユーザー）"),
	/** FSログ取込（ひも付きデータ） */
	B18B0039("B18B0039", "FSログ取込（ひも付きデータ）"),
	/** FSテスト端末登録・削除バッチ */
	B18B0046("B18B0046", "FSテスト端末登録・削除"),
	/** FSサイト参照取得バッチ */
	B18B0049("B18B0049", "FSサイト参照取得"),
	/** FSクーポン一覧取得バッチ */
	B18B0050("B18B0050", "FSクーポン一覧取得"),
	/** FSサイト参照取得バッチ */
	B18B0051("B18B0051", "FSPush通知一覧取得"),
	/** FSアプリ内Msg一覧取得バッチ */
	B18B0052("B18B0052", "FSアプリ内Msg一覧取得"),
	/** FSクーポン公開停止バッチ */
	B18B0053("B18B0053", "FSクーポン公開停止"),
	/** FS Push通知発行バッチ */
	B18B0056("B18B0056", "FS Push通知発行"),
	/** FS アプリ内メッセージ発行バッチ */
	B18B0060("B18B0060", "FS アプリ内メッセージ発行"),
	/** 目的別セグメントアップロードバッチ */
	B18B0068("B18B0068", "目的別セグメントアップロード"),
	/** FSクーポンテスト配信バッチ */
	B18B0070("B18B0070", "FSクーポンテスト配信"),
	/** FSアプリ内メッセージテスト配信バッチ */
	B18B0071("B18B0071", "FSアプリ内メッセージテスト配信"),
	/** FSアプリ内Msg配信停止バッチ */
	B18B0075("B18B0075", "FSアプリ内Msg配信停止"),
	/** FSPush通知配信停止バッチ */
	B18B0076("B18B0076", "FSPush通知配信停止"),
	/** FSID-Link登録・更新・削除バッチ */
	B18B0077("B18B0077", "FSID-Link登録・更新・削除"),
    /** FS目的別セグメント一覧取得バッチ */
    B18B0079("B18B0079", "FS目的別セグメント一覧取得バッチ");
    
	/** Batch id */
	private String batchId;

	/** Batch name */
	private String batchName;

	/**
	 * Batch Info
	 * 
	 * @param batchId batch id
	 * @param batchName batch name
	 */
	private BatchInfo(String batchId, String batchName) {
		this.batchId = batchId;
		this.batchName = batchName;
	}

	/**
	 * Get batch id
	 * 
	 * @return batch id
	 */
	public String getBatchId() {
		return batchId;
	}

	/**
	 * Get batch name
	 * 
	 * @return batch name
	 */
	public String getBatchName() {
		return batchName;
	}
}
