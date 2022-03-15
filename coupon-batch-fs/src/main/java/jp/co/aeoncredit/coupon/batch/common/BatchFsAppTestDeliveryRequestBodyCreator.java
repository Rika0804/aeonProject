package jp.co.aeoncredit.coupon.batch.common;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;

import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTOAdditionalItems;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTOButtons;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTODelivery;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTOIncentive;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateInputDTOMessage;
import jp.co.aeoncredit.coupon.constants.CouponImageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.CouponUseType;
import jp.co.aeoncredit.coupon.constants.JsonType;
import jp.co.aeoncredit.coupon.constants.MessageType;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstMerchantCategory;
import jp.co.aeoncredit.coupon.entity.MstStore;

/**
 * FS連携時のリクエストBODY作成<br>
 * ・B18B0071_FSアプリ内メッセージテスト配信バッチ<br>
 *  
 */
public class BatchFsAppTestDeliveryRequestBodyCreator {

	private static BatchFsAppTestDeliveryRequestBodyCreator creator = new BatchFsAppTestDeliveryRequestBodyCreator();

	private static final DateTimeFormatter dtfYyyymmddhhmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private ObjectMapper mapper;

	private BatchFsAppTestDeliveryRequestBodyCreator() {
	}

	public static BatchFsAppTestDeliveryRequestBodyCreator getInstance() {
		return creator;
	}

	/**
	 *　FS連携時のリクエストBODY作成(その他クーポンテスト配信API)<br>
	 * ・B18B0071_FSアプリ内メッセージテスト配信バッチ<br>
	 * 
	 * @param coupon 前処理で取得したcouponテーブルのレコード
	 * @param couponImage クーポン画像テーブル
	 * @param couponIncent クーポン特典テーブル
	 * @param appMessege アプリ内メッセージテーブル
	 * @param fsApiJson FSAPI用JSONテーブル
	 * @param mstStore 店舗マスタテーブル
	 * @param mstMerchantCategory 加盟店/カテゴリテーブル
	 * @param appScheme アプリスキーム
	 * 
	 * @return リクエストBODY(JSON)
	 * @throws JsonProcessingException 
	 */
	public String createRequestBody(Coupons coupon, List<CouponImages> couponImage,
			List<CouponIncents> couponIncent, AppMessages appMessege, List<FsApiJson> fsApiJson, MstStore mstStore,
			MstMerchantCategory mstMerchantCategory, String appScheme) throws JsonProcessingException {

		EventCouponAppIntroTestCreateInputDTO input = new EventCouponAppIntroTestCreateInputDTO();

		mapper = new ObjectMapper();

		// クーポン利用可能店舗ID
		if (mstStore.getFsStoreUuid() != null) {
			input.setProviders(new String[] { mstStore.getFsStoreUuid() });
		}

		// 限定クーポンフラグ
		input.setIsDistributable(false);
		if (!CouponType.MASS.getValue().equals(coupon.getCouponType())) {
			input.setIsDistributable(true);
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
		input.setIsOpen(true);

		// 任意追加項目
		input.setAdditionalItems(
				createAdditionalItems(coupon, couponImage, couponIncent, fsApiJson, mstMerchantCategory));

		// アプリ内メッセージ設定
		input.setDelivery(createDelivery(coupon, appMessege, appScheme));

		// Json形式に変換
		return mapper.writeValueAsString(input);
	}

	/**
	 * リクエストBody 任意追加項目部生成
	 * 
	 * @param coupon 前処理で取得したcouponテーブルのレコード
	 * @param couponImage クーポン画像テーブル
	 * @param couponIncent クーポン特典テーブル
	 * @param fsApiJson FSAPI用JSONテーブル
	 * @param mstMerchantCategory 加盟店/カテゴリテーブル
	 * 
	 * @return イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate).AdditionalItemsのInputDTOクラス
	 */
	private EventCouponAppIntroTestCreateInputDTOAdditionalItems createAdditionalItems(Coupons coupon,
			List<CouponImages> couponImage, List<CouponIncents> couponIncent, List<FsApiJson> fsApiJson,
			MstMerchantCategory mstMerchantCategory) {

		EventCouponAppIntroTestCreateInputDTOAdditionalItems input = new EventCouponAppIntroTestCreateInputDTOAdditionalItems();

		// 画像種別「1:サムネイル画像」のクーポン画像テーブル取得
		List<CouponImages> thumbnailImagesList = couponImage.stream()
				.filter(s -> CouponImageType.THUMBNAIL.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());
		// 画像種別「2:本文画像」のクーポン画像テーブル取得
		List<CouponImages> textImagesList = couponImage.stream()
				.filter(s -> CouponImageType.TEXT.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());
		// 画像種別「3:見せるクーポン画像」のクーポン画像テーブル取得
		List<CouponImages> showImagesList = couponImage.stream()
				.filter(s -> CouponImageType.COUPON.getValue().equals(s.getCouponImageType()))
				.collect(Collectors.toList());

		// JSON種別「1:対象商品オブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> productObjectList = fsApiJson.stream()
				.filter(s -> JsonType.PRODUCT.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());
		// JSON種別「2:テキストオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> textObjectList = fsApiJson.stream()
				.filter(s -> JsonType.TEXT.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());
		// JSON種別「3:リンクURLオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> linkObjectList = fsApiJson.stream()
				.filter(s -> JsonType.LINK.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());
		// JSON種別「4:バーコードオブジェクト」のFSAPI用JSONテーブル取得
		List<FsApiJson> barcodeObjectList = fsApiJson.stream()
				.filter(s -> JsonType.BARCODE.getValue().equals(s.getJsonType()))
				.collect(Collectors.toList());

		// クーポン画像
		if (!textImagesList.isEmpty()) {
			input.setImage(textImagesList.get(0).getCouponImageUrl());
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
		input.set_merchantId(ConvertUtility.longToString(mstMerchantCategory.getMerchantCategoryId()));

		// 加盟店名
		input.set_merchantName(coupon.getMerchantName());

		// 画像見出し
		if (!CouponType.PASSPORT.getValue().equals(coupon.getCouponType())) {
			input.set_imageHeading(coupon.getCouponUsingText());
		}

		// 画像
		if (!CouponType.PASSPORT.getValue().equals(coupon.getCouponType())
				&& CouponUseType.SHOW.getValue().equals(coupon.getCouponUseType())
				&& !showImagesList.isEmpty()) {
			// 【クーポンテーブル】.「クーポン利用方法」が「2:見せる」の場合
			input.set_image(showImagesList.get(0).getCouponImageUrl());
		}

		// バーコード
		if (!barcodeObjectList.isEmpty()) {
			input.set_barcode(barcodeObjectList.get(0).getJsonUrl());
		}

		// バーコード区分
		input.set_barcodeType(coupon.getBarcodeType());

		// 配信種別
		input.set_category(coupon.getDeliveryTypeTab());

		// クーポン利用単位
		input.set_useCountType(coupon.getCouponUseUnit());

		// 背景色
		input.set_backColor(coupon.getBackColor());

		// クーポン利用方法区分
		input.set_couponUseType(coupon.getCouponUseType());

		// プロモーションコード用リンクテキスト見出し
		input.set_promoLinkUrlHeading(coupon.getPromotionLinkUrlTitle());

		// プロモーションコード用リンクURL
		input.set_promoLinkUrl(coupon.getPromotionLinkUrl());

		// インセンティブテキスト
		input.set_incentive_text(coupon.getIncentiveSummaryText());

		// インセンティブ単位
		input.set_incentive_unit(coupon.getIncentiveSummaryType());

		// インセンティブ生成
		if (!couponIncent.isEmpty()) {
			input.set_incentive(createIncentive(couponIncent, productObjectList, coupon));
		}

		// テキスト
		if (!textObjectList.isEmpty()) {
			input.set_texts(textObjectList.get(0).getJsonUrl());
		}

		// リンクURL
		if (!linkObjectList.isEmpty()) {
			input.set_linkUrls(linkObjectList.get(0).getJsonUrl());
		}

		return input;
	}

	/**
	 * リクエストBody インセンティブ部生成（全クーポン共通）
	 * 
	 * @param coupon 前処理で取得したcouponテーブルのレコード
	 * @param couponImage クーポン画像テーブル
	 * @param couponIncent クーポン特典テーブル
	 * @param fsApiJson FSAPI用JSONテーブル
	 * @param coupon クーポンテーブル
	 * 
	 * @return イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate)._incentiveのInput DTOクラス
	 */
	private List<EventCouponAppIntroTestCreateInputDTOIncentive> createIncentive(List<CouponIncents> couponIncent,
			List<FsApiJson> fsApiJson, Coupons coupon) {

		List<EventCouponAppIntroTestCreateInputDTOIncentive> inputList = new ArrayList<>();

		Integer priority = 1;

		// クーポン特典テーブルのレコード分繰り返す
		for (CouponIncents couponIncents : couponIncent) {

			EventCouponAppIntroTestCreateInputDTOIncentive input = new EventCouponAppIntroTestCreateInputDTOIncentive();

			// JSON種別「1:対象商品オブジェクト」のFSAPI用JSONテーブル取得
			List<FsApiJson> productObjectList = fsApiJson.stream()
					.filter(s -> s.getCouponIncentId().equals(couponIncents.getCouponIncentId()))
					.collect(Collectors.toList());

			// インセンティブ表示順
			if (CouponType.PASSPORT.getValue().equals(coupon.getCouponType())) {
				input.set_priority(priority.toString());
				priority++;
			}

			// クーポン本文テキスト
			input.set_couponText(null);

			// 割引率
			input.set_amount(couponIncents.getIncentiveText());

			// 割引単位
			input.set_unit(couponIncents.getIncentiveType());

			// 対象商品
			if (!productObjectList.isEmpty()) {
				input.set_products(productObjectList.get(0).getJsonUrl());
			}

			inputList.add(input);
		}

		return inputList;
	}

	/**
	 * リクエストBody アプリ内メッセージ設定部生成（アプリイベントクーポン用）
	 * 
	 * @param coupon 前処理で取得したcouponテーブルのレコード
	 * @param appMessege アプリ内メッセージテーブル
	 * @param appScheme アプリスキーム
	 * 
	 * @return イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate).DeliveryのInputDTOクラス
	 */
	private EventCouponAppIntroTestCreateInputDTODelivery createDelivery(Coupons coupon, AppMessages appMessege,
			String appScheme) {

		EventCouponAppIntroTestCreateInputDTODelivery input = new EventCouponAppIntroTestCreateInputDTODelivery();

		if (appMessege != null) {
			// メッセージ名
			input.setName(appMessege.getMessageName());
			// メッセージ
			input.setMessage(createMessage(coupon, appMessege, appScheme));
		}

		// 表示順
		input.setPriority(0);

		// コンディション
		input.setCondition(null);

		return input;
	}

	/**
	 * リクエストBody メッセージ部生成（アプリイベントクーポン用）
	 * 
	 * @param coupon 前処理で取得したcouponテーブルのレコード
	 * @param appMessege アプリ内メッセージテーブル
	 * @param appScheme アプリスキーム
	 * 
	 * @return イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate).MessageのInputDTOクラス
	 */
	private List<EventCouponAppIntroTestCreateInputDTOMessage> createMessage(Coupons coupon, AppMessages appMessege,
			String appScheme) {

		List<EventCouponAppIntroTestCreateInputDTOMessage> inputList = new ArrayList<>();

		EventCouponAppIntroTestCreateInputDTOMessage input = new EventCouponAppIntroTestCreateInputDTOMessage();

		String url = null;
		if (coupon.getFsCouponUuid() != null) {
			url = "inappmsg://action?url=" + appScheme + "://jp.popinfo.coupon/coupons/?uuid=`"
					+ coupon.getFsCouponUuid() + "`&action=open";

		}

		// メッセージタイプ
		if (MessageType.POPUP.getValue().equals(appMessege.getMessageType())) {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「0:ポップアップ」

			input.setTemplateName("alert");

		} else if (MessageType.FULLSCREEN.getValue().equals(appMessege.getMessageType())) {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「1:フルスクリーン」のとき

			input.setTemplateName("overlay_1button");
		}

		// タイトル
		input.setTitle(appMessege.getMessageTitle());

		// 本文
		input.setBody(appMessege.getMessageText());

		// 画像URL
		input.setImageUrl(appMessege.getMessageImageUrl());

		// ボタン配列
		input.setButtons(createButton(url, appMessege));

		inputList.add(input);

		return inputList;
	}

	/**
	 * リクエストBody ボタン配列部生成（アプリイベントクーポン用）
	 * 
	 * @param url buttonsのurl
	 * @param appMessege アプリ内メッセージテーブル
	 * 
	 * @return イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate).MessageのInput DTOクラスリスト
	 */
	private List<EventCouponAppIntroTestCreateInputDTOButtons> createButton(String url, AppMessages appMessege) {

		EventCouponAppIntroTestCreateInputDTOButtons buttonsInput = new EventCouponAppIntroTestCreateInputDTOButtons();
		List<EventCouponAppIntroTestCreateInputDTOButtons> buttonsInputList = new ArrayList<>();

		// id
		buttonsInput.setId(1);

		// ボタン表示文言
		buttonsInput.setName(appMessege.getButtonDisplayName());

		// 遷移先URL
		buttonsInput.setUrl(url);

		// トラッキングイベント名
		buttonsInput.setEventName(null);

		buttonsInputList.add(buttonsInput);

		return buttonsInputList;
	}

}
