package jp.co.aeoncredit.coupon.batch.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;

import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiInputDTOAdditional;
import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiInputDTOContent;
import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiInputDTOIncentive;
import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiInputDTOPushTarget;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponTestDeliveryOutputDTO;
import jp.co.aeoncredit.coupon.constants.CouponImageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.CouponUseType;
import jp.co.aeoncredit.coupon.constants.JsonType;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;

/**
 * FS連携時のリクエストBODY作成<br>
 * ・B18B0070_FSクーポンテスト配信バッチ<br>
 *  
 */
public class BatchFsCouponTestDeliveryRequestBodyCreator {

	private static BatchFsCouponTestDeliveryRequestBodyCreator creator = new BatchFsCouponTestDeliveryRequestBodyCreator();

	private static final DateTimeFormatter dtfYyyymmddhhmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter dtfYyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private ObjectMapper mapper;

	/** 処理区分 */
	public enum ProcessKbn {
		/** クーポンテスト配信 */
		TEST_DELIVERY,
		/** 追加Push通知テスト配信 */
		ADDITIONAL_PUSH;
	}

	private BatchFsCouponTestDeliveryRequestBodyCreator() {
	}

	public static BatchFsCouponTestDeliveryRequestBodyCreator getInstance() {
		return creator;
	}

	/**
	 *　FS連携時のリクエストBODY作成(その他クーポンテスト配信API)<br>
	 * ・B18B0070_FSクーポンテスト配信バッチ<br>
	 * 
	 * @param couponInfo  FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param 処理区分
	 * 
	 * @return リクエストBODY(JSON)
	 * @throws JsonProcessingException 
	 */
	public String createRequestBody(FsCouponTestDeliveryOutputDTO couponInfo, ProcessKbn processKbn)
			throws JsonProcessingException {

		CouponTestDeliveryApiInputDTO input = new CouponTestDeliveryApiInputDTO();

		mapper = new ObjectMapper();

		Coupons coupon = couponInfo.getCoupons();

		// クーポン利用可能店舗ID
		if (couponInfo.getFsStoreUuid() != null) {
			input.setProviders(new String[] { couponInfo.getFsStoreUuid() });
		}

		// 限定クーポンフラグ
		input.setDistributable(false);
		if (!CouponType.MASS.getValue().equals(coupon.getCouponType())) {
			input.setDistributable(true);
		}

		// クーポン名
		input.setName(coupon.getCouponName());

		// クーポン説明
		input.setShortDescription(null);

		// クーポン利用条件
		input.setDescription(null);

		// 表示順ポイント
		input.setPriority(0);

		// 一人当たり利用可能枚数
		if (coupon.getCouponAvailableNumber() != null) {
			input.setUserUsableCount(coupon.getCouponAvailableNumber());
		}

		// 全体利用上限枚数
		input.setTotalUsableCount(null);

		// 限定CP一人当たり配布枚数
		if (CouponType.MASS.getValue().equals(coupon.getCouponType())) {
			input.setUserDistributableCount(null);
		} else {
			input.setUserDistributableCount(coupon.getUserDistributableCount());
		}

		// 限定CP全体上限配布枚数
		if (CouponType.MASS.getValue().equals(coupon.getCouponType())) {
			input.setTotalDistributableCount(null);
		} else {
			input.setTotalDistributableCount(coupon.getTotalDistributableCount());
		}

		// 表示開始日時
		input.setVisibleStartAt(coupon.getDisplaydateFrom().toLocalDateTime().format(dtfYyyymmddhhmmss));

		// 表示終了日時
		input.setVisibleEndAt(coupon.getDisplaydateTo().toLocalDateTime().format(dtfYyyymmddhhmmss));

		// 有効開始日時
		input.setUsableStartAt(coupon.getLimitdateFrom().toLocalDateTime().format(dtfYyyymmddhhmmss));

		// 有効終了日時
		input.setUsableEndAt(coupon.getLimitdateTo().toLocalDateTime().format(dtfYyyymmddhhmmss));

		// 限定配布後有効日数
		input.setUsableDays(null);

		// 公開ステータス
		input.setOpen(true);

		// 任意追加項目
		input.setAdditionalItems(createAdditionalItems(couponInfo));

		// Push通知設定
		if (CouponType.SENSOR_EVENT.getValue().equals(coupon.getCouponType())
				|| couponInfo.getPushNotifications() != null
				|| ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			input.setPushTarget(createPushTarget(couponInfo, processKbn));
		}

		// Json形式に変換
		return mapper.writeValueAsString(input);
	}

	/**
	 * リクエストBody 任意追加項目部生成
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * 
	 * @return その他クーポンテスト配信API 任意追加項目INPUTDTO
	 */
	private CouponTestDeliveryApiInputDTOAdditional createAdditionalItems(FsCouponTestDeliveryOutputDTO couponInfo) {

		CouponTestDeliveryApiInputDTOAdditional input = new CouponTestDeliveryApiInputDTOAdditional();

		Coupons coupon = couponInfo.getCoupons();

		// 画像種別「1:サムネイル画像」のクーポン画像テーブル取得
		List<CouponImages> thumbnailImagesList = couponInfo.getCouponImagesList().stream()
				.filter(s -> CouponImageType.THUMBNAIL.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());
		// 画像種別「2:本文画像」のクーポン画像テーブル取得
		List<CouponImages> textImagesList = couponInfo.getCouponImagesList().stream()
				.filter(s -> CouponImageType.TEXT.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());
		// 画像種別「3:見せるクーポン画像」のクーポン画像テーブル取得
		List<CouponImages> showImagesList = couponInfo.getCouponImagesList().stream()
				.filter(s -> CouponImageType.COUPON.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());

		// JSON種別「2:テキストオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> textObjectList = couponInfo.getFsApiJsonList().stream()
				.filter(s -> JsonType.TEXT.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());
		// JSON種別「3:リンクURLオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> linkObjectList = couponInfo.getFsApiJsonList().stream()
				.filter(s -> JsonType.LINK.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());
		// JSON種別「4:バーコードオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> barcodeObjectList = couponInfo.getFsApiJsonList().stream()
				.filter(s -> JsonType.BARCODE.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());

		// クーポン画像
		if (!textImagesList.isEmpty()) {
			input.setCouponImage(textImagesList.get(0).getCouponImageUrl());
		}

		// サムネイル
		if (!thumbnailImagesList.isEmpty()) {
			input.setThumbnail(thumbnailImagesList.get(0).getCouponImageUrl());
		}

		// クーポンコード
		if (CouponUseType.PROMO_CODE.getValue().equals(coupon.getCouponUseType())) {
			// 【クーポンテーブル】.「クーポン利用方法」が「3:プロモーションコード」の場合
			input.setCouponCode(coupon.getPromotionCode());
		}

		// クーポンコードリスト
		input.setCouponCodeList(null);

		// クーポンコード表示設定
		input.setCouponCodeVisible(null);

		// 加盟店グループID
		input.setMerchantId(ConvertUtility.longToString(couponInfo.getMerchantCategoryId()));

		// 加盟店名
		input.setMerchantName(coupon.getMerchantName());

		// 画像見出し
		if (!CouponType.PASSPORT.getValue().equals(coupon.getCouponType())) {
			input.setImageHeading(coupon.getCouponUsingText());
		}

		// 画像
		if (!CouponType.PASSPORT.getValue().equals(coupon.getCouponType())
				&& CouponUseType.SHOW.getValue().equals(coupon.getCouponUseType())
				&& !showImagesList.isEmpty()) {
			// 【クーポンテーブル】.「クーポン利用方法」が「2:見せる」の場合
			input.setImage(showImagesList.get(0).getCouponImageUrl());
		}

		// バーコード
		if (!barcodeObjectList.isEmpty()) {
			input.setBarcode(barcodeObjectList.get(0).getJsonUrl());
		}

		// バーコード区分
		input.setBarcodeType(coupon.getBarcodeType());

		// 配信種別
		input.setCategory(coupon.getDeliveryTypeTab());

		// クーポン利用単位
		input.setUseCountType(coupon.getCouponUseUnit());

		// 背景色
		input.setBackColor(coupon.getBackColor());

		// クーポン利用方法区分
		input.setCouponUseType(coupon.getCouponUseType());

		// プロモーションコード用リンクテキスト見出し
		input.setPromoLinkUrlHeading(coupon.getPromotionLinkUrlTitle());

		// プロモーションコード用リンクURL
		input.setPromoLinkUrl(coupon.getPromotionLinkUrl());

		// インセンティブテキスト
		input.setIncentiveText(coupon.getIncentiveSummaryText());

		// インセンティブ単位
		input.setIncentiveUnit(coupon.getIncentiveSummaryType());

		// インセンティブ生成
		if (!couponInfo.getCouponIncentsList().isEmpty()) {
			input.setIncentive(createIncentive(couponInfo, coupon));
		}

		// テキスト
		if (!textObjectList.isEmpty()) {
			input.setTexts(textObjectList.get(0).getJsonUrl());
		}

		// リンクURL
		if (!linkObjectList.isEmpty()) {
			input.setLinkUrls(linkObjectList.get(0).getJsonUrl());
		}

		return input;
	}

	/**
	 * リクエストBody インセンティブ部生成（全クーポン共通）
	 * 
	 * @param couponInfo  FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param coupon クーポンテーブル
	 * 
	 * @return その他クーポンテスト配信API インセンティブINPUTDTOリスト
	 */
	private List<CouponTestDeliveryApiInputDTOIncentive> createIncentive(FsCouponTestDeliveryOutputDTO couponInfo,
			Coupons coupon) {

		List<CouponTestDeliveryApiInputDTOIncentive> inputList = new ArrayList<>();

		Integer priority = 1;

		// クーポン特典テーブルのレコード分繰り返す
		for (CouponIncents couponIncents : couponInfo.getCouponIncentsList()) {

			CouponTestDeliveryApiInputDTOIncentive input = new CouponTestDeliveryApiInputDTOIncentive();

			// JSON種別「1:対象商品オブジェクト」のFSAPI用JSONテーブル取得
			List<FsApiJson> linkObjectList = couponInfo.getFsApiJsonList().stream()
					.filter(s -> JsonType.PRODUCT.getValue().equals(s.getJsonType())
							&& s.getCouponIncentId().equals(couponIncents.getCouponIncentId()))
					.collect(Collectors.toList());

			// インセンティブ表示順
			if (CouponType.PASSPORT.getValue().equals(coupon.getCouponType())) {
				input.setPriority(priority.toString());
				priority++;
			}

			// クーポン本文テキスト
			input.setCouponText(null);

			// 割引率
			input.setAmount(couponIncents.getIncentiveText());

			// 割引単位
			input.setUnit(couponIncents.getIncentiveType());

			// 対象商品
			if (!linkObjectList.isEmpty()) {
				input.setProducts(linkObjectList.get(0).getJsonUrl());
			}

			inputList.add(input);
		}

		return inputList;
	}

	/**
	 * リクエストBody Push通知設定部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo  FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param 処理区分
	 * 
	 * @return その他クーポンテスト配信API Push通知設定INPUTDTO
	 * @throws JsonProcessingException 
	 */
	private CouponTestDeliveryApiInputDTOPushTarget createPushTarget(FsCouponTestDeliveryOutputDTO couponInfo,
			ProcessKbn processKbn)
			throws JsonProcessingException {

		CouponTestDeliveryApiInputDTOPushTarget input = new CouponTestDeliveryApiInputDTOPushTarget();

		// コンテンツタイプ
		input.setContentType("text/plain");

		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)
				&& !CouponType.SENSOR_EVENT.getValue().equals(couponInfo.getCoupons().getCouponType())) {
			// クーポンテスト配信 かつ センサーイベント以外の場合

			// Push通知本文
			input.setPopup(couponInfo.getCoupons().getCouponName());

			// 追加Push通知テスト配信の場合
			input.setTitle(couponInfo.getCoupons().getCouponName());

			// 本文
			input.setContent(createContentTestDelivery(couponInfo));

		} else {
			// 追加Push通知テスト配信 または センサークーポンの場合

			// Push通知本文
			input.setPopup(couponInfo.getPushNotifications().getPushNotificationText());

			// 件名
			input.setTitle(couponInfo.getPushNotifications().getNotificationTitle());

			// 本文
			input.setContent(createContent(couponInfo));
		}

		// リンク先URL
		input.setUrl(null);

		// アイコン
		input.setIcon(null);

		// カテゴリー
		input.setCategory("coupon");

		return input;
	}

	/**
	 * リクエストBody 本文部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo  FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * 
	 * @return 本文
	 * @throws JsonProcessingException 
	 */
	private String createContent(FsCouponTestDeliveryOutputDTO couponInfo) throws JsonProcessingException {

		CouponTestDeliveryApiInputDTOContent content = new CouponTestDeliveryApiInputDTOContent();

		// コンテンツ本文
		if (couponInfo.getPushNotifications().getNotificationBody() == null) {
			content.setInformationText("[[test]]");
		} else {
			content.setInformationText(couponInfo.getPushNotifications().getNotificationBody() + "[[test]]");
		}
		// リンクテキスト
		content.setInformationLinkTitle(couponInfo.getPushNotifications().getButtonDisplayName());
		// リンクURL
		content.setInformationImage(couponInfo.getPushNotifications().getHeaderImageUrl());
		// 表示終了日
		content.setInformationDisplayEndDate(LocalDate.now().format(dtfYyyymmdd));
		// 表示終了時刻
		content.setInformationDisplayEndTime("23:59:59");

		return mapper.writeValueAsString(content);

	}

	/**
	 * リクエストBody 本文部生成（テスト配信用）
	 * 
	 * @param couponInfo  FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * 
	 * @return 本文
	 * @throws JsonProcessingException 
	 */
	private String createContentTestDelivery(FsCouponTestDeliveryOutputDTO couponInfo) throws JsonProcessingException {

		CouponTestDeliveryApiInputDTOContent content = new CouponTestDeliveryApiInputDTOContent();

		// コンテンツ本文
		content.setInformationText(couponInfo.getCoupons().getCouponName() + "[[test]]");
		// リンクテキスト
		content.setInformationLinkTitle("詳細はこちら");
		// 表示終了日
		content.setInformationDisplayEndDate(LocalDate.now().format(dtfYyyymmdd));
		// 表示終了時刻
		content.setInformationDisplayEndTime("23:59:59");

		return mapper.writeValueAsString(content);

	}

}
