package jp.co.aeoncredit.coupon.batch.common;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.dto.QrCodeListInputDTO;

/**
 * QRコードリスト作成
 */
public class BatchQrCodeListCreateor extends BatchDBAccessBase {

	/** バッチ共通処理ID*/
	protected static final String BATCH_COMMONPROCESS_ID = BatchInfo.B18BC002.getBatchId();

	/** バッチ共通処理ネーム*/
	protected static final String BATCH_COMMONPROCESS_NAME = BatchInfo.B18BC002.getBatchName();

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_COMMONPROCESS_ID);

	/** DBアクセス */
	protected BatchDBAccessBase batchDBAccessBase;

	/** レコードが取得できなかった場合の戻り値 */
	protected static final String EMPTY_VALUE = "[]";
	
	/** 成功時の終了コード（ログ出力用） */
	protected static final int SUCCESS_VALUE = 1;

	/**
	 * コンストラクタ
	 * @param batchDBAccessBase ... 呼び出し元バッチ
	 */
	public BatchQrCodeListCreateor(BatchDBAccessBase batchDBAccessBase) {
		super();
		this.batchDBAccessBase = batchDBAccessBase;
	}

	/**
	 * QRコードリストを取得する
	 */
	public String getQrCodeList() {

		// (1)処理開始メッセージを出力する。
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB001, BATCH_COMMONPROCESS_NAME));

		// (2)QRコードリスト取得
		List<Object[]> qrObjList = batchDBAccessBase.sqlSelect(BATCH_COMMONPROCESS_ID, "selectQrcodes");

		// (2.1b)取得した結果が0件の場合、メッセージを出力し、nullを返却して処理終了する。
		if (qrObjList.isEmpty()) {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, BATCH_COMMONPROCESS_NAME, ""));
			return EMPTY_VALUE;
		}

		// 型変換
		List<QrCodeListInputDTO> qrCodeList = convertObjectToClass(qrObjList);

		// JSONオブジェクト形式に変換
		List<QrCodeListJson> qrList = convertToJsonClass(qrCodeList);

		// (4)処理終了メッセージを出力し、処理を終了する。設定した戻り値を返却する。
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB002, BATCH_COMMONPROCESS_NAME, SUCCESS_VALUE));

		// JSON形式に変換
		String result = qrCodeListToJson(qrList);
		
		// ログを出力する
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB012, result));
		
		return result;
	}

	/**
	 * QRコードリストをログ出力用に文字列に変換する
	 * @param qrCodeList QRコードリスト
	 * @return
	 */
	private Object dtoListToString(List<QrCodeListInputDTO> qrCodeList) {
		StringBuilder bld = new StringBuilder();
		for (QrCodeListInputDTO qrCode : qrCodeList) {
			bld.append(qrCode.toString());
		}
		return bld.toString();
	}

	/**
	 * DBから取得したオブジェクト型のデータを各型に変換する
	 * @param qrObjList ... DBから取得したQRコードのリスト
	 * @return
	 */
	private List<QrCodeListInputDTO> convertObjectToClass(List<Object[]> qrObjList) {

		List<QrCodeListInputDTO> result = new ArrayList<>();
		for (Object[] objData : qrObjList) {
			QrCodeListInputDTO qrCodeListInputDTO = new QrCodeListInputDTO();
			qrCodeListInputDTO.setQrCodeId((String) objData[0]);
			qrCodeListInputDTO.setUuid((String) objData[1]);
			qrCodeListInputDTO.setValidStartDate((Timestamp) objData[2]);
			qrCodeListInputDTO.setValidEndDate((Timestamp) objData[3]);
			result.add(qrCodeListInputDTO);
		}

		return result;
	}

	/**
	 * DBから取得したオブジェクト→Jsonクラスの変換
	 * @param qrListData ... DBから取得したデータ
	 * @return
	 */
	private List<QrCodeListJson> convertToJsonClass(List<QrCodeListInputDTO> qrListData) {

		List<QrCodeListJson> result = new ArrayList<>();
		Map<String, List<CouponListJson>> dataMap = new TreeMap<>();

		// 階層化のため一度Map形式に変換する
		for (QrCodeListInputDTO data : qrListData) {
			if (!dataMap.containsKey(data.getQrCodeId())) {
				// 未集計のQRコードの場合、紐づくクーポンのリストをMap登録する
				List<CouponListJson> coupons = getCouponJsonsOf(data.getQrCodeId(), qrListData);
				if (coupons.isEmpty()) {
					// 紐づくクーポンが存在しない場合
					logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB926,"クーポンテーブル", "QRコードID:" + data.getQrCodeId()));
				} else {
					dataMap.put(data.getQrCodeId(), coupons);
				}
			}
		}

		// Map→DTO変換
		for (Map.Entry<String, List<CouponListJson>> entry : dataMap.entrySet()) {
			QrCodeListJson qrCodeJson = new QrCodeListJson();
			qrCodeJson.setQrCodeId(entry.getKey());
			qrCodeJson.setCoupons(entry.getValue());
			result.add(qrCodeJson);
		}

		return result;
	}

	/**
	 * QRコードIDに紐づくクーポンのリストを取得する
	 * @param qrCodeId
	 * @return
	 */
	private List<CouponListJson> getCouponJsonsOf(String qrCodeId, List<QrCodeListInputDTO> qrListData) {

		// 合致するクーポンの一覧を取得
		List<QrCodeListInputDTO> extractedCouponList = qrListData.stream()
				.filter(element -> element.getQrCodeId().equals(qrCodeId))
				.collect(Collectors.toList());

		// JSONで使用する形式に変換
		List<CouponListJson> resultCouponList = new ArrayList<>();
		for (QrCodeListInputDTO extractCupon : extractedCouponList) {
			CouponListJson coupon = new CouponListJson();
			coupon.setUuid(extractCupon.getUuid());
			coupon.setValidStartDate(extractCupon.getValidStartDate());
			coupon.setValidEndDate(extractCupon.getValidEndDate());
			resultCouponList.add(coupon);
		}

		return resultCouponList;

	}

	/**
	 * QRコードリストDTOからJSON形式の文字列に変換する
	 * @param qrCodeList
	 * @return JSON 文字列
	 */
	private String qrCodeListToJson(List<QrCodeListJson> qrCodeList) {

		List<String> jsons = new ArrayList<>();

		for (QrCodeListJson qrCode : qrCodeList) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				jsons.add(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(qrCode));
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage(), e);
				throw new AssertionError();
			}
		}

		// リストをカンマ区切りで整形
		return "[" + jsons.stream().collect(Collectors.joining(",\n")).replace("\r\n", "\n") + "]"; 
	}
	
	/**
	 * JSON形式
	 * @author m-omori
	 */
	private static class QrCodeListJson {

		/** QRコードID */
		private String qrCodeId;

		/** クーポンオブジェクト */
		private List<CouponListJson> coupons;

		public String getQrCodeId() {
			return qrCodeId;
		}

		public void setQrCodeId(String qrCodeId) {
			this.qrCodeId = qrCodeId;
		}

		public List<CouponListJson> getCoupons() {
			return coupons;
		}

		public void setCoupons(List<CouponListJson> coupons) {
			this.coupons = coupons;
		}
	}

	
	/**
	 * JSON形式中のクーポンオブジェクト
	 * @author m-omori
	 *
	 */
	private static class CouponListJson {
		/** FSクーポンUUID */
		private String uuid;

		/**
		 * 有効日（開始）
		 */
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
		private Timestamp validStartDate;

		/**
		 * 有効日（終了）
		 */
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
		private Timestamp validEndDate;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public Timestamp getValidStartDate() {
			return validStartDate;
		}

		public void setValidStartDate(Timestamp validStartDate) {
			this.validStartDate = validStartDate;
		}

		public Timestamp getValidEndDate() {
			return validEndDate;
		}

		public void setValidEndDate(Timestamp validEndDate) {
			this.validEndDate = validEndDate;
		}

	}
}