package jp.co.aeoncredit.coupon.batch.common;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;

import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOAdditional;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOBluetooth;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOButtons;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOCondition;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOContent;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTODelivery;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOIncentive;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOLocation;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOMessage;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOPeriod;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOPushTarget;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiInputDTOTarget;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponRegisterOutputDTO;
import jp.co.aeoncredit.coupon.constants.CouponImageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.CouponUseType;
import jp.co.aeoncredit.coupon.constants.JsonType;
import jp.co.aeoncredit.coupon.constants.MessageType;
import jp.co.aeoncredit.coupon.constants.SensorCategory;
import jp.co.aeoncredit.coupon.entity.AppMessageSendPeriods;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstSensor;
import jp.co.aeoncredit.coupon.entity.PushNotificationSendPeriods;

/**
 * FS連携時のリクエストBODY作成<br>
 * ・B18B0011_FSクーポン登録・更新・削除バッチ（登録、更新時）<br>
 *  
 */
public class BatchFsCouponRequestBodyCreator {

	private static BatchFsCouponRequestBodyCreator creator = new BatchFsCouponRequestBodyCreator();

	private static final DateTimeFormatter dtfYyyymmddhhmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter dtfYyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter dtfHhmmss = DateTimeFormatter.ofPattern("HH:mm:ss");

	private ObjectMapper mapper;

	/** 作成区分 */
	public enum CreateKbn {
		B18B0011_REGISTER, B18B0011_UPDATE;
	}

	private BatchFsCouponRequestBodyCreator() {
	}

	public static BatchFsCouponRequestBodyCreator getInstance() {
		return creator;
	}

	/**
	 *　FS連携時のリクエストBODY作成<br>
	 * ・B18B0011_FSクーポン登録・更新・削除バッチ（登録、更新時）<br>
	 * 
	 * @param couponType クーポン種別
	 * @param createKbn 作成区分
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return リクエストBODY(JSON)
	 * @throws JsonProcessingException 
	 */
	public String createRequestBody(CouponType couponType, CreateKbn createKbn,
			FsCouponRegisterOutputDTO couponInfo) throws JsonProcessingException {

		CouponRegisterApiInputDTO input = new CouponRegisterApiInputDTO();

		mapper = new ObjectMapper();

		Coupons coupon = couponInfo.getCoupons();

		// クーポン利用可能店舗ID
		if (couponInfo.getFsStoreUuid() != null) {
			input.setProviders(new String[] { couponInfo.getFsStoreUuid() });
		}

		// 限定クーポンフラグ
		if (CreateKbn.B18B0011_UPDATE.equals(createKbn)) {
			// B18B0011(更新)は項目不要
			input.setDistributable(null);
		} else {
			input.setDistributable(false);
			if (!CouponType.MASS.equals(couponType)) {
				input.setDistributable(true);
			}
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
		if (CouponType.MASS.equals(couponType)) {
			input.setUserDistributableCount(null);
		} else {
			input.setUserDistributableCount(coupon.getUserDistributableCount());
		}

		// 限定CP全体上限配布枚数
		if (CouponType.MASS.equals(couponType)) {
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
		input.setOpen(false);
		if (!CreateKbn.B18B0011_REGISTER.equals(createKbn)) {
			// B18B0011(新規登録)以外
			input.setOpen(true);
		}

		// 任意追加項目
		input.setAdditionalItems(createAdditionalItems(couponType, createKbn, couponInfo));

		if (CreateKbn.B18B0011_REGISTER.equals(createKbn)) {
			// B18B0011(新規登録)の場合

			// アプリ内メッセージ設定
			if (CouponType.APP_EVENT.equals(couponType)) {
				input.setDelivery(createDelivery(couponInfo));
			}

			// Push通知設定
			if (CouponType.SENSOR_EVENT.equals(couponType)) {
				input.setPushTarget(createPushTarget(couponInfo));
			}
		}

		// 配信番号(更新でアプリイベントクーポンのみ)
		if (CreateKbn.B18B0011_UPDATE.equals(createKbn)
				&& CouponType.APP_EVENT.equals(couponType)) {
			input.setDeliveryId(couponInfo.getAppMessages().getFsAppMessageUuid());
		}

		// Json形式に変換
		return mapper.writeValueAsString(input);

	}

	/**
	 * リクエストBody 任意追加項目部生成（全クーポン共通）
	 * 
	 * @param couponType クーポン種別
	 * @param createKbn 作成区分
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録・更新API 任意追加項目INPUTDTO
	 */
	private CouponRegisterApiInputDTOAdditional createAdditionalItems(CouponType couponType, CreateKbn createKbn,
			FsCouponRegisterOutputDTO couponInfo) {

		CouponRegisterApiInputDTOAdditional input = new CouponRegisterApiInputDTOAdditional();

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
		if (CreateKbn.B18B0011_REGISTER.equals(createKbn)
				&& (CouponType.APP_EVENT.equals(couponType)
						|| CouponType.SENSOR_EVENT.equals(couponType))) {
			input.setMerchantId(ConvertUtility.longToString(couponInfo.getMerchantCategoryId()));
		} else {
			input.setMerchantGroupId(ConvertUtility.longToString(couponInfo.getMerchantCategoryId()));
		}

		// 加盟店名
		input.setMerchantName(coupon.getMerchantName());

		// 画像見出し
		if (!CouponType.PASSPORT.equals(couponType)) {
			input.setImageHeading(coupon.getCouponUsingText());
		}

		// 画像
		if (!CouponType.PASSPORT.equals(couponType)
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
			input.setIncentive(createIncentive(couponInfo, couponType));
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
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * @param couponType クーポン種別
	 * 
	 * @return クーポン新規登録・更新API インセンティブINPUTDTOリスト
	 */
	private List<CouponRegisterApiInputDTOIncentive> createIncentive(FsCouponRegisterOutputDTO couponInfo,
			CouponType couponType) {

		List<CouponRegisterApiInputDTOIncentive> inputList = new ArrayList<>();

		Integer priority = 1;

		// クーポン特典テーブルのレコード分繰り返す
		for (CouponIncents couponIncents : couponInfo.getCouponIncentsList()) {

			CouponRegisterApiInputDTOIncentive input = new CouponRegisterApiInputDTOIncentive();

			// JSON種別「1:対象商品オブジェクト」のFSAPI用JSONテーブル取得
			List<FsApiJson> linkObjectList = couponInfo.getFsApiJsonList().stream()
					.filter(s -> JsonType.PRODUCT.getValue().equals(s.getJsonType())
							&& s.getCouponIncentId().equals(couponIncents.getCouponIncentId()))
					.collect(Collectors.toList());

			// インセンティブ表示順
			if (CouponType.PASSPORT.equals(couponType)) {
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
	 * リクエストBody アプリ内メッセージ設定部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API アプリ内メッセージ設定INPUTDTO
	 */
	private CouponRegisterApiInputDTODelivery createDelivery(FsCouponRegisterOutputDTO couponInfo) {

		CouponRegisterApiInputDTODelivery input = new CouponRegisterApiInputDTODelivery();

		// メッセージ名
		input.setName(couponInfo.getAppMessages().getMessageName());

		// 表示順
		input.setPriority(0);

		// コンディション
		input.setCondition(createCondition(couponInfo));

		// メッセージ
		input.setMessage(createMessage(couponInfo));

		return input;
	}

	/**
	 * リクエストBody コンディション部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API コンディションINPUTDTO
	 */
	private CouponRegisterApiInputDTOCondition createCondition(FsCouponRegisterOutputDTO couponInfo) {

		CouponRegisterApiInputDTOCondition input = new CouponRegisterApiInputDTOCondition();

		// 機動トリガーイベント名
		input.setEventName(new String[] { couponInfo.getMstEvent().getEventName() });

		// 配布期間
		input.setPeriod(createPeriodApp(couponInfo));

		// ターゲット
		if (couponInfo.getAppMessages().getFsSegmentId() != null
				&& couponInfo.getAppMessages().getFsSegmentId() != -1) {
			input.setTarget(createTarget(couponInfo));
		}

		// 配信する累計最大回数
		if (couponInfo.getAppMessages().getTotalMaxSendCount() != 0) {
			input.setMaxTotalCount(couponInfo.getAppMessages().getTotalMaxSendCount());
		}

		// １日に配信する最大回数
		if (couponInfo.getAppMessages().getDailyMaxSendCount() != 0) {
			input.setMaxTotalCountPerDay(couponInfo.getAppMessages().getDailyMaxSendCount());
		}

		// カテゴリ
		input.setCategory(null);

		return input;
	}

	/**
	 * リクエストBody 配布期間部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API 配布期間INPUTDTO
	 */
	private List<CouponRegisterApiInputDTOPeriod> createPeriodApp(FsCouponRegisterOutputDTO couponInfo) {

		List<CouponRegisterApiInputDTOPeriod> inputList = new ArrayList<>();

		// アプリ内メッセージ配信期間のレコード分繰り返す
		for (AppMessageSendPeriods appMessageSendPeriods : couponInfo.getAppMessageSendPeriodsList()) {

			CouponRegisterApiInputDTOPeriod input = new CouponRegisterApiInputDTOPeriod();

			// 開始日時
			input.setStart(appMessageSendPeriods.getSendPeriodFrom().toLocalDateTime().format(dtfYyyymmddhhmmss));

			// 終了日時
			input.setEnd(appMessageSendPeriods.getSendPeriodTo().toLocalDateTime().format(dtfYyyymmddhhmmss));

			inputList.add(input);
		}

		return inputList;
	}

	/**
	 * リクエストBody ターゲット部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API ターゲットINPUTDTO
	 */
	private CouponRegisterApiInputDTOTarget createTarget(FsCouponRegisterOutputDTO couponInfo) {

		CouponRegisterApiInputDTOTarget input = new CouponRegisterApiInputDTOTarget();

		// セグメントID
		input.setSegmentationId(couponInfo.getAppMessages().getFsSegmentId().toString());

		return input;
	}

	/**
	 * リクエストBody メッセージ部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API メッセージINPUTDTO
	 */
	private List<CouponRegisterApiInputDTOMessage> createMessage(FsCouponRegisterOutputDTO couponInfo) {

		List<CouponRegisterApiInputDTOMessage> inputList = new ArrayList<>();

		CouponRegisterApiInputDTOMessage input = new CouponRegisterApiInputDTOMessage();

		// メッセージタイプ
		if (MessageType.POPUP.getValue().equals(couponInfo.getAppMessages().getMessageType())) {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「0:ポップアップ」

			input.setTemplateName("alert");

		} else if (MessageType.FULLSCREEN.getValue().equals(couponInfo.getAppMessages().getMessageType())) {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「1:フルスクリーン」のとき

			input.setTemplateName("overlay_1button");
		}

		// タイトル
		input.setTitle(couponInfo.getAppMessages().getMessageTitle());

		// 本文
		input.setBody(couponInfo.getAppMessages().getMessageText());

		// 画像URL
		input.setImageUrl(couponInfo.getAppMessages().getMessageImageUrl());

		// ボタン配列
		input.setButtons(createButton(couponInfo));

		inputList.add(input);

		return inputList;
	}

	/**
	 * リクエストBody ボタン配列部生成（アプリイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API ボタン配列INPUTDTOリスト
	 */
	private List<CouponRegisterApiInputDTOButtons> createButton(FsCouponRegisterOutputDTO couponInfo) {

		List<CouponRegisterApiInputDTOButtons> inputList = new ArrayList<>();

		CouponRegisterApiInputDTOButtons input = new CouponRegisterApiInputDTOButtons();

		// id
		input.setId(1);

		// ボタン表示文言
		input.setName(couponInfo.getAppMessages().getButtonDisplayName());

		// 遷移先URL
		input.setUrl(null);

		// トラッキングイベント名
		input.setEventName(null);

		inputList.add(input);

		return inputList;
	}

	/**
	 * リクエストBody Push通知設定部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API 配布対象位置INPUTDTO
	 * @throws JsonProcessingException 
	 */
	private CouponRegisterApiInputDTOPushTarget createPushTarget(FsCouponRegisterOutputDTO couponInfo)
			throws JsonProcessingException {

		CouponRegisterApiInputDTOPushTarget input = new CouponRegisterApiInputDTOPushTarget();

		// センサーマスタからセンサーサブカテゴリを取得
		String sensorCategory = couponInfo.getMstSensorList().get(0).getSensorCategory();

		// 通知タイプ
		if (SensorCategory.GPS.getValue().equals(sensorCategory)) {
			// GPSの場合
			input.setType("location");
		} else {
			// iBeaconの場合
			input.setType("bluetooth");
		}

		// コンテンツタイプ
		input.setContentType("text/plain");

		// プラットフォーム
		input.setPlatform(new String[] { "iphone", "android" });

		// Push通知本文
		input.setPopup(couponInfo.getPushNotifications().getPushNotificationText());

		// 件名
		input.setTitle(couponInfo.getPushNotifications().getNotificationTitle());

		// 本文
		input.setContent(createContent(couponInfo));

		// リンク先URL
		input.setUrl(null);

		// アイコン
		input.setIcon(null);

		// カテゴリー
		input.setCategory("coupon");

		// 配信タイプ
		input.setDeliveryType(1);

		// 配布対象位置リスト
		if (SensorCategory.GPS.getValue().equals(sensorCategory)) {
			// GPSの場合
			input.setLocation(createLocation(couponInfo));
		}

		// bluetooth端末情報リスト
		if (SensorCategory.IBEACON.getValue().equals(sensorCategory)) {
			// iBEACONの場合
			input.setBluetooth(createBluetooth(couponInfo));
		}

		// 配布期間
		input.setPeriod(createPeriodSenser(couponInfo));

		// セグメントID
		if (couponInfo.getPushNotifications().getFsSegmentId() != null
				&& couponInfo.getPushNotifications().getFsSegmentId() != -1) {
			input.setSegmentationId(couponInfo.getPushNotifications().getFsSegmentId());
		}

		return input;
	}

	/**
	 * リクエストBody 本文部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return 本文
	 * @throws JsonProcessingException 
	 */
	private String createContent(FsCouponRegisterOutputDTO couponInfo) throws JsonProcessingException {

		CouponRegisterApiInputDTOContent content = new CouponRegisterApiInputDTOContent();

		// コンテンツ本文
		content.setInformationText(couponInfo.getPushNotifications().getNotificationBody());
		// リンクテキスト
		content.setInformationLinkTitle(couponInfo.getPushNotifications().getButtonDisplayName());
		// リンクURL
		content.setInformationImage(couponInfo.getPushNotifications().getHeaderImageUrl());
		// 表示終了日
		content.setInformationDisplayEndDate(
				couponInfo.getCoupons().getDisplaydateTo().toLocalDateTime().format(dtfYyyymmdd));
		// 表示終了時刻
		content.setInformationDisplayEndTime(
				couponInfo.getCoupons().getDisplaydateTo().toLocalDateTime().format(dtfHhmmss));

		return mapper.writeValueAsString(content);

	}

	/**
	 * リクエストBody 配布対象位置部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API 配布対象位置INPUTDTOリスト
	 */
	private List<CouponRegisterApiInputDTOLocation> createLocation(FsCouponRegisterOutputDTO couponInfo) {

		List<CouponRegisterApiInputDTOLocation> inputList = new ArrayList<>();

		// センサーマスタのレコード分繰り返す
		for (MstSensor mstSensor : couponInfo.getMstSensorList()) {

			CouponRegisterApiInputDTOLocation input = new CouponRegisterApiInputDTOLocation();

			// 緯度
			input.setLat(ConvertUtility.stringToBigDecimal(mstSensor.getSensorAttribute1()));

			// 経度
			input.setLon(ConvertUtility.stringToBigDecimal(mstSensor.getSensorAttribute2()));

			// 範囲
			input.setRadius(ConvertUtility.stringToInteger(mstSensor.getSensorAttribute3()));

			inputList.add(input);
		}

		return inputList;
	}

	/**
	 * リクエストBody bluetooth端末情報部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API bluetooth端末情報INPUTDTOリスト
	 */
	private List<CouponRegisterApiInputDTOBluetooth> createBluetooth(FsCouponRegisterOutputDTO coupon) {

		List<CouponRegisterApiInputDTOBluetooth> inputList = new ArrayList<>();

		// センサーマスタのレコード分繰り返す
		for (MstSensor mstSensor : coupon.getMstSensorList()) {

			CouponRegisterApiInputDTOBluetooth input = new CouponRegisterApiInputDTOBluetooth();

			// 種類
			input.setType("iBeacon");

			// uuid
			input.setUuid(mstSensor.getSensorAttribute1());

			// メジャー番号
			input.setMajor(ConvertUtility.stringToInteger(mstSensor.getSensorAttribute2()));

			// マイナー番号
			input.setMinor(ConvertUtility.stringToInteger(mstSensor.getSensorAttribute3()));

			// 電波強度 
			input.setRssi(ConvertUtility.stringToInteger(mstSensor.getSensorAttribute4()));

			inputList.add(input);
		}

		return inputList;
	}

	/**
	 * リクエストBody 配布期間部生成（センサーイベントクーポン用）
	 * 
	 * @param couponInfo FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return クーポン新規登録API 配布期間INPUTDTOリスト
	 */
	private List<CouponRegisterApiInputDTOPeriod> createPeriodSenser(FsCouponRegisterOutputDTO couponInfo) {

		List<CouponRegisterApiInputDTOPeriod> inputList = new ArrayList<>();

		// Push通知配信期間のレコード分繰り返す
		for (PushNotificationSendPeriods pushNotificationSendPeriods : couponInfo
				.getPushNotificationSendPeriodsList()) {

			CouponRegisterApiInputDTOPeriod input = new CouponRegisterApiInputDTOPeriod();

			// 開始日時
			input.setStart(pushNotificationSendPeriods.getSendPeriodFrom().toLocalDateTime().format(dtfYyyymmddhhmmss));

			// 終了日時
			input.setEnd(pushNotificationSendPeriods.getSendPeriodTo().toLocalDateTime().format(dtfYyyymmddhhmmss));

			inputList.add(input);
		}

		return inputList;
	}

}
