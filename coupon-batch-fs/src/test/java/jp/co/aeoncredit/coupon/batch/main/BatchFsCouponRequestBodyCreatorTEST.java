package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponRequestBodyCreator;
import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponRequestBodyCreator.CreateKbn;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponRegisterOutputDTO;
import jp.co.aeoncredit.coupon.constants.AppMessageType;
import jp.co.aeoncredit.coupon.constants.CouponImageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.CouponUseLimitFlag;
import jp.co.aeoncredit.coupon.constants.CouponUseType;
import jp.co.aeoncredit.coupon.constants.CouponUseUnit;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.DeliveryTarget;
import jp.co.aeoncredit.coupon.constants.DeliveryType;
import jp.co.aeoncredit.coupon.constants.IncentiveType;
import jp.co.aeoncredit.coupon.constants.JsonType;
import jp.co.aeoncredit.coupon.constants.MessageFormat;
import jp.co.aeoncredit.coupon.constants.MessageType;
import jp.co.aeoncredit.coupon.constants.PushNotificationStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationType;
import jp.co.aeoncredit.coupon.constants.SensorCategory;
import jp.co.aeoncredit.coupon.constants.SensorSubcategory;
import jp.co.aeoncredit.coupon.entity.AppMessageSendPeriods;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstEvent;
import jp.co.aeoncredit.coupon.entity.MstSensor;
import jp.co.aeoncredit.coupon.entity.PushNotificationSendPeriods;
import jp.co.aeoncredit.coupon.entity.PushNotifications;

/**
 * FS連携時のリクエストBODY作成のテスト クラスのJUnit
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BatchFsCouponRequestBodyCreatorTEST {

	@InjectMocks
	BatchFsCouponRequestBodyCreator batchFsCouponRequestBodyCreator;

	private Logger log = LoggerFactory.getInstance().getLogger(this);

	/**
	 * テスト初期化処理
	 * 
	 * @throws Exception スローされた例外
	 */
	@Before
	public void setUp() throws Exception {

		// トランザクションのモック化
		batchFsCouponRequestBodyCreator = Mockito.spy(batchFsCouponRequestBodyCreator);
	}

	/**
	 * テスト終了処理
	 * 
	 * @throws Exception スローされた例外
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 新規登録<br>
	 *          
	 * 【試験条件】： クーポン利用可能店舗ID設定あり
	 *               【クーポンテーブル】.「クーポン利用回数制限」が"1:制限あり" かつ【クーポンテーブル】.「クーポン利用単位」が"2:1回利用"の場合<br>
	 *               【クーポンテーブル】.「クーポン利用方法」が「3:プロモーションコード」の場合<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST001() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":false,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.MASS,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST001", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.MASS,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST001", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 新規登録<br>
	 *          
	 * 【試験条件】： クーポン利用可能店舗ID設定なし
	 * 　　　　　　　【クーポンテーブル】.「クーポン利用回数制限」が"1:制限あり" かつ【クーポンテーブル】.「クーポン利用単位」が"1:1日利用"の場合<br>
	 *               【クーポンテーブル】.「クーポン利用方法」が「1:バーコード」の場合<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST002() throws Exception {

		String json = "{"
				+ "\"is_distributable\":false,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"1\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"1\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid(null);
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.MASS,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONE_DAY,
				CouponUseType.BARCODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST002", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.MASS,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST002", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 新規登録<br>
	 *          
	 * 【試験条件】：【クーポンテーブル】.「クーポン利用回数制限」が"0:制限なし" かつ【クーポンテーブル】.「クーポン利用単位」が"2:1回利用"の場合<br>
	 *               【クーポンテーブル】.「クーポン利用方法」が「2:見せる」の場合<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST003() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":false,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_image\":\"画像URL3\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"2\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.MASS,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.NO_LIMIT,
				CouponUseUnit.ONCE,
				CouponUseType.SHOW);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST003", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.MASS,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST003", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：ターゲットクーポン 新規登録<br>
	 *          
	 * 【試験条件】：【クーポンテーブル】.「1人あたり配信できるクーポン枚数」指定あり<br>
	 *               【クーポンテーブル】.「クーポン全体配信上限枚数」指定あり<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST004() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.TARGET,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST004", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.TARGET,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST004", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：ターゲットクーポン 新規登録<br>
	 *          
	 * 【試験条件】： クーポン利用可能回数指定あり
	 * 　　　　　　　【クーポンテーブル】.「クーポン利用方法」が「3:プロモーションコード」の場合<br>
	 *               【クーポンテーブル】.「1人あたり配信できるクーポン枚数」指定なし(null)<br>
	 *               【クーポンテーブル】.「クーポン全体配信上限枚数」指定なし(null)<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST005() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.TARGET,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		coupons.get().setUserDistributableCount(null);
		coupons.get().setTotalDistributableCount(null);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST005", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.TARGET,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST005", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：ターゲットクーポン 新規登録<br>
	 *          
	 * 【試験条件】： クーポン利用可能回数指定なし
	 *               【クーポンテーブル】.「クーポン利用方法」が「3:プロモーションコード」の場合<br>
	 *               【クーポンテーブル】.「1人あたり配信できるクーポン枚数」指定なし(0)<br>
	 *               【クーポンテーブル】.「クーポン全体配信上限枚数」指定なし(0)<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST006() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_distributable_count\":0,"
				+ "\"total_distributable_count\":0,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.TARGET,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		coupons.get().setUserDistributableCount(ConvertUtility.stringToShort("0"));
		coupons.get().setTotalDistributableCount(ConvertUtility.stringToInteger("0"));
		coupons.get().setCouponAvailableNumber(null);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST006", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.TARGET,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST006", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：パスポートクーポン 新規登録<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST007() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_priority\":\"1\","
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_priority\":\"2\","
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_priority\":\"3\","
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.PASSPORT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST007", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.PASSPORT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST007", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：アプリイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】：【アプリ内メッセージテーブル】.「FSセグメントID」 <> 0の場合
	 *               【アプリ内メッセージテーブル】.「メッセージタイプ」が「0:ポップアップ」かつ【アプリ内メッセージテーブル】.「メッセージフォーマット」が「0:テキスト」<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST008() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"delivery\":{"
				+ "\"name\":\"メッセージ名\","
				+ "\"priority\":0,"
				+ "\"condition\":{"
				+ "\"event_name\":[\"イベント名\"],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}],"
				+ "\"target\":{"
				+ "\"segmentation_id\":\"1\""
				+ "},"
				+ "\"max_total_count\":2,"
				+ "\"max_total_count_per_day\":3"
				+ "},"
				+ "\"message\":[{"
				+ "\"template_name\":\"alert\","
				+ "\"title\":\"タイトル\","
				+ "\"body\":\"本文\","
				+ "\"image_url\":\"画像\","
				+ "\"buttons\":[{"
				+ "\"id\":1,"
				+ "\"name\":\"ボタン名称\""
				+ "}]"
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.APP_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// アプリ内メッセージテーブル
		Optional<AppMessages> appMessages = createAppMessages(MessageType.POPUP, MessageFormat.TEXT,
				ConvertUtility.stringToLong("1"));
		dto.setAppMessages(appMessages.get());

		// アプリ内メッセージ配信期間リスト
		List<AppMessageSendPeriods> appMessageSendPeriodsList = createAppMessageSendPeriodsList();
		dto.setAppMessageSendPeriodsList(appMessageSendPeriodsList);

		// イベントマスタ
		Optional<MstEvent> mstEvent = createMstEvent();
		dto.setMstEvent(mstEvent.get());

		// テスト開始 
		printMsg("TEST008", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.APP_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST008", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：アプリイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】：【アプリ内メッセージテーブル】.「FSセグメントID」 = 0の場合
	 *               【アプリ内メッセージテーブル】.「メッセージタイプ」が「0:ポップアップ」かつ【アプリ内メッセージテーブル】.「メッセージフォーマット」が「1:画像」<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST009() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"delivery\":{"
				+ "\"name\":\"メッセージ名\","
				+ "\"priority\":0,"
				+ "\"condition\":{"
				+ "\"event_name\":[\"イベント名\"],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}],"
				+ "\"target\":{\"segmentation_id\":\"0\"},"
				+ "\"max_total_count\":2,"
				+ "\"max_total_count_per_day\":3"
				+ "},"
				+ "\"message\":[{"
				+ "\"template_name\":\"alert\","
				+ "\"title\":\"タイトル\","
				+ "\"body\":\"本文\","
				+ "\"image_url\":\"画像\","
				+ "\"buttons\":[{"
				+ "\"id\":1,"
				+ "\"name\":\"ボタン名称\""
				+ "}]"
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.APP_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// アプリ内メッセージテーブル
		Optional<AppMessages> appMessages = createAppMessages(MessageType.POPUP, MessageFormat.IMAGE,
				ConvertUtility.stringToLong("0"));
		dto.setAppMessages(appMessages.get());

		// アプリ内メッセージ配信期間リスト
		List<AppMessageSendPeriods> appMessageSendPeriodsList = createAppMessageSendPeriodsList();
		dto.setAppMessageSendPeriodsList(appMessageSendPeriodsList);

		// イベントマスタ
		Optional<MstEvent> mstEvent = createMstEvent();
		dto.setMstEvent(mstEvent.get());

		// テスト開始 
		printMsg("TEST009", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.APP_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST009", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：アプリイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】：【アプリ内メッセージテーブル】.「FSセグメントID」 = nullの場合
	 *               【アプリ内メッセージテーブル】.「メッセージタイプ」が「1:フルスクリーン」<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST010() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"delivery\":{"
				+ "\"name\":\"メッセージ名\","
				+ "\"priority\":0,"
				+ "\"condition\":{"
				+ "\"event_name\":[\"イベント名\"],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}],"
				+ "\"max_total_count\":2,"
				+ "\"max_total_count_per_day\":3"
				+ "},"
				+ "\"message\":[{"
				+ "\"template_name\":\"overlay_1button\","
				+ "\"title\":\"タイトル\","
				+ "\"body\":\"本文\","
				+ "\"image_url\":\"画像\","
				+ "\"buttons\":[{"
				+ "\"id\":1,"
				+ "\"name\":\"ボタン名称\""
				+ "}]"
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.APP_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// アプリ内メッセージテーブル
		Optional<AppMessages> appMessages = createAppMessages(MessageType.FULLSCREEN, MessageFormat.TEXT,
				null);
		dto.setAppMessages(appMessages.get());

		// アプリ内メッセージ配信期間リスト
		List<AppMessageSendPeriods> appMessageSendPeriodsList = createAppMessageSendPeriodsList();
		dto.setAppMessageSendPeriodsList(appMessageSendPeriodsList);

		// イベントマスタ
		Optional<MstEvent> mstEvent = createMstEvent();
		dto.setMstEvent(mstEvent.get());

		// テスト開始 
		printMsg("TEST010", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.APP_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST010", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：センサーイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】： センサーカテゴリが"1:GPS"の場合<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST011() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"pushTarget\":{"
				+ "\"type\":\"location\","
				+ "\"content_type\":\"text/plain\","
				+ "\"platform\":[\"iphone\",\"android\"],"
				+ "\"popup\":\"Push通知本文\","
				+ "\"title\":\"お知らせ件名\","
				+ "\"content\":\"{\\\"information_text\\\":\\\"お知らせ本文\\\",\\\"information_link_title\\\":\\\"ボタン名称\\\",\\\"information_image\\\":\\\"ヘッダ画像\\\",\\\"information_displayEndDate\\\":\\\"2021-08-16\\\",\\\"information_displayEndTime\\\":\\\"01:00:00\\\"}\","
				+ "\"category\":\"coupon\","
				+ "\"delivery_type\":1,"
				+ "\"location\":[{"
				+ "\"lat\":-1.0,"
				+ "\"lon\":-2.0,"
				+ "\"radius\":-3"
				+ "},{"
				+ "\"lat\":-1.0,"
				+ "\"lon\":-2.0,"
				+ "\"radius\":-3"
				+ "},{"
				+ "\"lat\":-1.0,"
				+ "\"lon\":-2.0,"
				+ "\"radius\":-3"
				+ "}],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.SENSOR_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// Push通知テーブル
		Optional<PushNotifications> pushNotifications = createPushNotifications();
		dto.setPushNotifications(pushNotifications.get());

		// Push通知配信期間リスト
		List<PushNotificationSendPeriods> pushNotificationSendPeriodsList = createPushNotificationSendPeriodsList();
		dto.setPushNotificationSendPeriodsList(pushNotificationSendPeriodsList);

		// センサーマスタリスト
		List<MstSensor> mstSensorList = createMstSensorList(SensorCategory.GPS, SensorSubcategory.OWN);
		dto.setMstSensorList(mstSensorList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST011", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.SENSOR_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST011", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：センサーイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】： センサーカテゴリが"2:iBeacon"の場合<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST012() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"pushTarget\":{"
				+ "\"type\":\"bluetooth\","
				+ "\"content_type\":\"text/plain\","
				+ "\"platform\":[\"iphone\",\"android\"],"
				+ "\"popup\":\"Push通知本文\","
				+ "\"title\":\"お知らせ件名\","
				+ "\"content\":\"{\\\"information_text\\\":\\\"お知らせ本文\\\",\\\"information_link_title\\\":\\\"ボタン名称\\\",\\\"information_image\\\":\\\"ヘッダ画像\\\",\\\"information_displayEndDate\\\":\\\"2021-08-16\\\",\\\"information_displayEndTime\\\":\\\"01:00:00\\\"}\","
				+ "\"category\":\"coupon\","
				+ "\"delivery_type\":1,"
				+ "\"bluetooth\":[{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "}],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.SENSOR_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// Push通知テーブル
		Optional<PushNotifications> pushNotifications = createPushNotifications();
		dto.setPushNotifications(pushNotifications.get());

		// Push通知配信期間リスト
		List<PushNotificationSendPeriods> pushNotificationSendPeriodsList = createPushNotificationSendPeriodsList();
		dto.setPushNotificationSendPeriodsList(pushNotificationSendPeriodsList);

		// センサーマスタリスト
		List<MstSensor> mstSensorList = createMstSensorList(SensorCategory.IBEACON, SensorSubcategory.OWN);
		dto.setMstSensorList(mstSensorList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST012", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.SENSOR_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST012", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 新規登録<br>
	 *          
	 * 【試験条件】： クーポン画像テーブルなし<br>
	 *                クーポン特典テーブルなし<br>
	 *                FSAPI用JSONテーブルなし<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST013() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":false,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"2\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.MASS,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.NO_LIMIT,
				CouponUseUnit.ONCE,
				CouponUseType.SHOW);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = new ArrayList<>();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = new ArrayList<>();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = new ArrayList<>();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST013", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.MASS,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST013", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 更新<br>
	
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST014() throws Exception {
		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":true,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.MASS,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST014", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.MASS,
					CreateKbn.B18B0011_UPDATE,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST014", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：マスクーポン 更新<br>
	
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST015() throws Exception {

		// テスト開始 
		printMsg("TEST015", "Start");
		try {
			BatchFsCouponRequestBodyCreator creator = BatchFsCouponRequestBodyCreator.getInstance();
			assertNotNull(creator);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST015", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：アプリイベントクーポン 更新<br>
	 *               配信番号設定パターン
	 *          
	 * 【試験条件】：アプリイベントクーポンで更新<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST016() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":true,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantGroupId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"delivery_id\":999"
				+ "}";
		;

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.APP_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// アプリ内メッセージテーブル
		Optional<AppMessages> appMessages = createAppMessages(MessageType.POPUP, MessageFormat.TEXT,
				ConvertUtility.stringToLong("1"));
		appMessages.get().setFsAppMessageUuid((long) 999);
		dto.setAppMessages(appMessages.get());

		// アプリ内メッセージ配信期間リスト
		List<AppMessageSendPeriods> appMessageSendPeriodsList = createAppMessageSendPeriodsList();
		dto.setAppMessageSendPeriodsList(appMessageSendPeriodsList);

		// イベントマスタ
		Optional<MstEvent> mstEvent = createMstEvent();
		dto.setMstEvent(mstEvent.get());

		// テスト開始 
		printMsg("TEST016", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.APP_EVENT,
					CreateKbn.B18B0011_UPDATE,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST016", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：センサーイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】： セグメントIDが「-1」<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST017() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"pushTarget\":{"
				+ "\"type\":\"bluetooth\","
				+ "\"content_type\":\"text/plain\","
				+ "\"platform\":[\"iphone\",\"android\"],"
				+ "\"popup\":\"Push通知本文\","
				+ "\"title\":\"お知らせ件名\","
				+ "\"content\":\"{\\\"information_text\\\":\\\"お知らせ本文\\\",\\\"information_link_title\\\":\\\"ボタン名称\\\",\\\"information_image\\\":\\\"ヘッダ画像\\\",\\\"information_displayEndDate\\\":\\\"2021-08-16\\\",\\\"information_displayEndTime\\\":\\\"01:00:00\\\"}\","
				+ "\"category\":\"coupon\","
				+ "\"delivery_type\":1,"
				+ "\"bluetooth\":[{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "}],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}]"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.SENSOR_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// Push通知テーブル
		Optional<PushNotifications> pushNotifications = createPushNotifications();
		pushNotifications.get().setFsSegmentId(Long.valueOf("-1"));
		dto.setPushNotifications(pushNotifications.get());

		// Push通知配信期間リスト
		List<PushNotificationSendPeriods> pushNotificationSendPeriodsList = createPushNotificationSendPeriodsList();
		dto.setPushNotificationSendPeriodsList(pushNotificationSendPeriodsList);

		// センサーマスタリスト
		List<MstSensor> mstSensorList = createMstSensorList(SensorCategory.IBEACON, SensorSubcategory.OWN);
		dto.setMstSensorList(mstSensorList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST017", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.SENSOR_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST017", "End");
		}
	}

	/**
	 * 【試験対象】：BatchFsCouponRequestBodyCreator#createRequestBody()<br>
	 * 
	 * 【試験概要】：センサーイベントクーポン 新規登録<br>
	 *          
	 * 【試験条件】： セグメントIDが「null」、「-1」以外<br>
	 *            
	 * 【試験結果】： JSON想定通りであること<br>
	 *
	 */
	@Test
	public void BatchFsCouponRequestBodyCreator_TEST018() throws Exception {

		String json = "{"
				+ "\"providers\":[\"200\"],"
				+ "\"is_distributable\":true,"
				+ "\"name\":\"クーポン名\","
				+ "\"priority\":0,"
				+ "\"user_usable_count\":11,"
				+ "\"user_distributable_count\":22,"
				+ "\"total_distributable_count\":33,"
				+ "\"visible_start_at\":\"2021-08-16 00:00:00\","
				+ "\"visible_end_at\":\"2021-08-16 01:00:00\","
				+ "\"usable_start_at\":\"2021-08-16 02:00:00\","
				+ "\"usable_end_at\":\"2021-08-16 03:00:00\","
				+ "\"is_open\":false,"
				+ "\"additional_items\":{"
				+ "\"image\":\"画像URL2\","
				+ "\"thumbnail\":\"画像URL1\","
				+ "\"couponCode\":\"プロモーションコード\","
				+ "\"_merchantId\":\"300\","
				+ "\"_merchantName\":\"加盟店名\","
				+ "\"_imageHeading\":\"クーポン利用時テキスト\","
				+ "\"_barcode\":\"JSONURL4\","
				+ "\"_barcodeType\":\"バーコード種別\","
				+ "\"_category\":\"配信種別タブ\","
				+ "\"_useCountType\":\"2\","
				+ "\"_backColor\":\"背景色\","
				+ "\"_couponUseType\":\"3\","
				+ "\"_promoLinkUrlHeading\":\"リンク先URL見出し\","
				+ "\"_promoLinkUrl\":\"リンク先URL\","
				+ "\"_incentive_text\":\"インセンティブ要約テキスト\","
				+ "\"_incentive_unit\":\"インセンティブ要約種別\","
				+ "\"_incentive\":[{"
				+ "\"_amount\":\"値引き金額テキスト1\","
				+ "\"_unit\":\"2\","
				+ "\"_products\":\"JSONURL1\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト2\","
				+ "\"_unit\":\"2\""
				+ "},{"
				+ "\"_amount\":\"値引き金額テキスト3\","
				+ "\"_unit\":\"2\""
				+ "}],"
				+ "\"_texts\":\"JSONURL2\","
				+ "\"_linkUrls\":\"JSONURL3\""
				+ "},"
				+ "\"pushTarget\":{"
				+ "\"type\":\"bluetooth\","
				+ "\"content_type\":\"text/plain\","
				+ "\"platform\":[\"iphone\",\"android\"],"
				+ "\"popup\":\"Push通知本文\","
				+ "\"title\":\"お知らせ件名\","
				+ "\"content\":\"{\\\"information_text\\\":\\\"お知らせ本文\\\",\\\"information_link_title\\\":\\\"ボタン名称\\\",\\\"information_image\\\":\\\"ヘッダ画像\\\",\\\"information_displayEndDate\\\":\\\"2021-08-16\\\",\\\"information_displayEndTime\\\":\\\"01:00:00\\\"}\","
				+ "\"category\":\"coupon\","
				+ "\"delivery_type\":1,"
				+ "\"bluetooth\":[{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "},{"
				+ "\"type\":\"iBeacon\","
				+ "\"uuid\":\"-1\","
				+ "\"major\":-2,"
				+ "\"minor\":-3,"
				+ "\"rssi\":-4"
				+ "}],"
				+ "\"period\":[{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "},{"
				+ "\"start\":\"2021-08-16 00:00:00\","
				+ "\"end\":\"2021-08-16 01:00:00\""
				+ "}],"
				+ "\"segmentation_id\":1"
				+ "}"
				+ "}";

		// FSクーポン登録・更新・削除用のクーポン情報DTO
		FsCouponRegisterOutputDTO dto = new FsCouponRegisterOutputDTO();
		// クーポンID
		dto.setCouponId(ConvertUtility.stringToLong("100"));
		// FS店舗UUID
		dto.setFsStoreUuid("200");
		// 加盟店/カテゴリID
		dto.setMerchantCategoryId(ConvertUtility.stringToLong("300"));

		// クーポンテーブル
		Optional<Coupons> coupons = createCoupons(
				CouponType.SENSOR_EVENT,
				DeliveryTarget.INDIVIDUAL,
				DeliveryType.AEON_WALLET_APP,
				CouponUseLimitFlag.LIMITED,
				CouponUseUnit.ONCE,
				CouponUseType.PROMO_CODE);
		dto.setCoupons(coupons.get());

		// クーポン画像テーブルリスト
		List<CouponImages> couponImagesList = createCouponImagesList();
		dto.setCouponImagesList(couponImagesList);

		// クーポン特典テーブルリスト
		List<CouponIncents> couponIncentsList = createCouponIncentsList();
		dto.setCouponIncentsList(couponIncentsList);

		// Push通知テーブル
		Optional<PushNotifications> pushNotifications = createPushNotifications();
		pushNotifications.get().setFsSegmentId(Long.valueOf("1"));
		dto.setPushNotifications(pushNotifications.get());

		// Push通知配信期間リスト
		List<PushNotificationSendPeriods> pushNotificationSendPeriodsList = createPushNotificationSendPeriodsList();
		dto.setPushNotificationSendPeriodsList(pushNotificationSendPeriodsList);

		// センサーマスタリスト
		List<MstSensor> mstSensorList = createMstSensorList(SensorCategory.IBEACON, SensorSubcategory.OWN);
		dto.setMstSensorList(mstSensorList);

		// FSAPI用JSONテーブルリスト
		List<FsApiJson> fsApiJsonList = createFsApiJsonList();
		dto.setFsApiJsonList(fsApiJsonList);

		// テスト開始 
		printMsg("TEST018", "Start");
		try {
			String result = batchFsCouponRequestBodyCreator.createRequestBody(
					CouponType.SENSOR_EVENT,
					CreateKbn.B18B0011_REGISTER,
					dto);
			assertEquals(json, result);
		} catch (Exception e) {
			fail();
		} finally {
			// テスト終了
			printMsg("TEST018", "End");
		}
	}

	/**
	 * テスト開始終了メッセージ出力
	 * @param testId テストID
	 * @param processType 処理区分(開始/終了)
	 */
	public void printMsg(String testId, String processType) {
		if (processType.equals("Start")) {
			log.info(" [B18B0011TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0011TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	/**
	 * クーポンテーブル生成
	 * 
	 * @param couponType クーポン種別(1:マスクーポン、2:ターゲットクーポン、3:パスポートクーポン、4:アプリイベントクーポン、5:センサーイベントクーポン)
	 * @param deliveryTarget 配信対象(1:全員、2:個別、3:QRコード)
	 * @param deliveryType 配信先(1:イオンウォレットアプリ、2:ATM)
	 * @param couponUseLimitFlag クーポン利用回数制限
	 * @param couponUseUnit クーポン利用単位
	 * @param couponUseType クーポン利用方法
	 * 
	 * @return クーポンテーブル
	 * @throws ParseException 
	 * 
	 */
	private Optional<Coupons> createCoupons(CouponType couponType, DeliveryTarget deliveryTarget,
			DeliveryType deliveryType, CouponUseLimitFlag couponUseLimitFlag, CouponUseUnit couponUseUnit,
			CouponUseType couponUseType) throws ParseException {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		Coupons coupons = new Coupons();

		coupons.setCouponId(ConvertUtility.stringToLong("100")); // クーポンID
		coupons.setCouponType(couponType.getValue()); // クーポン種別
		coupons.setLimitdateFrom(new Timestamp(df.parse("2021-08-16 02:00:00").getTime())); // クーポン有効期間開始
		coupons.setLimitdateTo(new Timestamp(df.parse("2021-08-16 03:00:00").getTime())); // クーポン有効期間終了
		coupons.setDisplaydateFrom(new Timestamp(df.parse("2021-08-16 00:00:00").getTime())); // クーポン表示期間開始
		coupons.setDisplaydateTo(new Timestamp(df.parse("2021-08-16 01:00:00").getTime())); // クーポン表示期間終了
		coupons.setMerchantName("加盟店名"); // 加盟店名
		coupons.setCouponStatus("クーポンステータス"); // クーポンステータス
		coupons.setDeliveryTarget(deliveryTarget.getValue()); // 配信対象
		coupons.setCouponUseType(couponUseType.getValue()); // クーポン利用方法
		coupons.setCouponName("クーポン名"); // クーポン名
		coupons.setCouponBarcodeNo("クーポンバーコード番号"); // クーポンバーコード番号
		coupons.setPromotionCode("プロモーションコード"); // プロモーションコード
		coupons.setIncentiveSummaryText("インセンティブ要約テキスト"); // インセンティブ要約テキスト
		coupons.setIncentiveSummaryType("インセンティブ要約種別"); // インセンティブ要約種別
		coupons.setCouponUseLimitFlag(couponUseLimitFlag.getValue()); // クーポン利用回数制限
		coupons.setCouponAvailableNumber(ConvertUtility.stringToShort("11")); // クーポン利用可能回数
		coupons.setTextTitle1(null); // テキスト見出し1
		coupons.setTextBody1(null); // テキスト本文1
		coupons.setTextTitle2(null); // テキスト見出し2
		coupons.setTextBody2(null); // テキスト本文2
		coupons.setTextTitle3(null); // テキスト見出し3
		coupons.setTextBody3(null); // テキスト本文3
		coupons.setTextTitle4(null); // テキスト見出し4
		coupons.setTextBody4(null); // テキスト本文4
		coupons.setTextTitle5(null); // テキスト見出し5
		coupons.setTextBody5(null); // テキスト本文5
		coupons.setBodyLinkUrlTitle1(null); // 本文リンク先URL見出し1
		coupons.setBodyLinkUrl1(null); // 本文リンク先URL1
		coupons.setBodyLinkUrlTitle2(null); // 本文リンク先URL見出し2
		coupons.setBodyLinkUrl2(null); // 本文リンク先URL2
		coupons.setMemo(null); // メモ
		coupons.setCouponUsingText("クーポン利用時テキスト"); // クーポン利用時テキスト
		coupons.setUseInfo("利用状況"); // 利用状況
		coupons.setDeliverySaveMethod("配信先登録方法"); // 配信先登録方法
		coupons.setDeliveryType(deliveryType.toString()); // 配信先
		coupons.setPassportTypeId("パスポート種別ID"); // パスポート種別ID
		coupons.setCouponUseUnit(couponUseUnit.getValue()); // クーポン利用単位
		coupons.setFsSegmentId(ConvertUtility.stringToLong("1111")); // FSセグメントID
		coupons.setFsCouponUuid(null); // FSクーポンUUID
		coupons.setFsDeliveryStatus("1"); // FS連携状況
		coupons.setFsTestDeliveryStatus(null); // FSテスト配信状況
		coupons.setFsStopStatus(null); // FS公開停止状況
		coupons.setMerchantCategoryId(ConvertUtility.stringToLong("2222")); // 加盟店/カテゴリID
		coupons.setBarcodeType("バーコード種別"); // バーコード種別
		coupons.setPromotionLinkUrlTitle("リンク先URL見出し"); // リンク先URL見出し
		coupons.setPromotionLinkUrl("リンク先URL"); // リンク先URL
		coupons.setDeliveryTypeTab("配信種別タブ"); // 配信種別タブ
		coupons.setUserDistributableCount(ConvertUtility.stringToShort("22")); // 1人あたり配信できるクーポン枚数
		coupons.setTotalDistributableCount(ConvertUtility.stringToInteger("33")); // クーポン全体配信上限枚数
		coupons.setStoreId(ConvertUtility.stringToLong("3333")); // 利用可能店舗ID
		coupons.setBackColor("背景色"); // 背景色
		coupons.setCreateUserId(null); // 作成者ID
		coupons.setApproveDate(null); // 承認日時
		coupons.setCreateDate(null); // 作成日
		coupons.setUpdateUserId(null); // 更新者ID
		coupons.setUpdateDate(null); // 更新日
		coupons.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

		return Optional.of(coupons);

	}

	/**
	 * クーポン画像テーブルリスト生成
	 * 
	 * @return クーポン画像テーブルリスト
	 * 
	 */
	private List<CouponImages> createCouponImagesList() {

		List<CouponImages> couponImagesList = new ArrayList<CouponImages>();

		for (int i = 1; i <= 3; i++) {
			CouponImages couponImages = new CouponImages();

			couponImages.setCouponImageId(ConvertUtility.stringToLong("20" + i)); // クーポン画像ID
			couponImages.setCouponId(ConvertUtility.stringToLong("100")); // クーポンID
			// 画像種別
			if (i == 1) {
				couponImages.setCouponImageType(CouponImageType.THUMBNAIL.getValue());
			} else if (i == 2) {
				couponImages.setCouponImageType(CouponImageType.TEXT.getValue());
			} else {
				couponImages.setCouponImageType(CouponImageType.COUPON.getValue());
			}
			couponImages.setCouponImageUrl("画像URL" + i); // 画像URL
			couponImages.setImage("画像".getBytes()); // 画像
			couponImages.setMimeType(null); // MIMEタイプ
			couponImages.setCreateUserId(null); // 作成者ID
			couponImages.setCreateDate(null); // 作成日
			couponImages.setUpdateUserId(null); // 更新者ID
			couponImages.setUpdateDate(null); // 更新日
			couponImages.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			couponImagesList.add(couponImages);
		}

		return couponImagesList;

	}

	/**
	 * クーポン特典テーブルリスト生成
	 * 
	 * @return クーポン特典テーブルリスト
	 * 
	 */
	private List<CouponIncents> createCouponIncentsList() {

		List<CouponIncents> couponIncentsList = new ArrayList<CouponIncents>();

		for (int i = 1; i <= 3; i++) {
			CouponIncents couponIncents = new CouponIncents();

			couponIncents.setCouponIncentId(ConvertUtility.stringToLong("30" + i)); // クーポン特典ID
			couponIncents.setCouponId(ConvertUtility.stringToLong("100")); // クーポンID
			couponIncents.setIncentiveType(IncentiveType.PERCENT_OFF.getValue()); // インセンティブ登録種別
			couponIncents.setIncentiveText("値引き金額テキスト" + i); // 値引き金額テキスト
			couponIncents.setCreateUserId(null); // 作成者ID
			couponIncents.setCreateDate(null); // 作成日
			couponIncents.setUpdateUserId(null); // 更新者ID
			couponIncents.setUpdateDate(null); // 更新日
			couponIncents.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			couponIncentsList.add(couponIncents);
		}

		return couponIncentsList;

	}

	/**
	 * クーポン画像テーブルリスト生成
	 * 
	 * @return クーポン画像テーブルリスト
	 * 
	 */
	private List<FsApiJson> createFsApiJsonList() {

		List<FsApiJson> fsApiJsonList = new ArrayList<FsApiJson>();

		for (int i = 1; i <= 4; i++) {
			FsApiJson fsApiJson = new FsApiJson();

			fsApiJson.setCouponId(ConvertUtility.stringToLong("100")); // クーポンID
			// JSON種別
			// COUPON_INCENT_ID
			if (i == 1) {
				fsApiJson.setJsonType(JsonType.PRODUCT.getValue());
			} else if (i == 2) {
				fsApiJson.setJsonType(JsonType.TEXT.getValue());
			} else if (i == 3) {
				fsApiJson.setJsonType(JsonType.LINK.getValue());
			} else {
				fsApiJson.setJsonType(JsonType.BARCODE.getValue());
			}
			fsApiJson.setCouponIncentId(ConvertUtility.stringToLong("30" + i));
			fsApiJson.setJsonUrl("JSONURL" + i); // JSONURL
			fsApiJson.setJson("JSON" + i); // JSON
			fsApiJson.setCreateUserId(null); // 作成者ID
			fsApiJson.setCreateDate(null); // 作成日
			fsApiJson.setUpdateUserId(null); // 更新者ID
			fsApiJson.setUpdateDate(null); // 更新日
			fsApiJson.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			fsApiJsonList.add(fsApiJson);
		}

		return fsApiJsonList;

	}

	/**
	 * アプリ内メッセージテーブル生成
	 * 
	 * @param messageType メッセージタイプ
	 * @param messageFormat メッセージフォーマット
	 * @param fsSegmentId FSセグメントID
	 * 
	 * @return アプリ内メッセージテーブル
	 * 
	 */
	private Optional<AppMessages> createAppMessages(MessageType messageType, MessageFormat messageFormat,
			Long fsSegmentId) {

		AppMessages appMessages = new AppMessages();

		appMessages.setAppMessageId(ConvertUtility.stringToLong("400"));// アプリ内メッセージID
		appMessages.setFsAppMessageUuid(null);// FSアプリ内メッセージUUID
		appMessages.setAppMessageType(AppMessageType.DELIVERY.getValue());// アプリ内メッセージ種別
		appMessages.setCouponId(ConvertUtility.stringToLong("100"));// クーポンID
		appMessages.setAppMessageStatus(null);// アプリ内メッセージステータス
		appMessages.setMessageName("メッセージ名");// メッセージ名
		appMessages.setMessageType(messageType.getValue());// メッセージタイプ
		appMessages.setMessageFormat(messageFormat.getValue());// メッセージフォーマット
		appMessages.setMessageImageUrl("画像");// 画像
		appMessages.setMessageImage("画像（本体）".getBytes());// 画像（本体）
		appMessages.setMimeType("MIMEタイプ");// MIMEタイプ
		appMessages.setMessageTitle("タイトル");// タイトル
		appMessages.setMessageText("本文");// 本文
		appMessages.setButtonDisplayName("ボタン名称");// ボタン名称
		appMessages.setFsSegmentId(fsSegmentId);// FSセグメントID
		appMessages.setTotalMaxSendCount(ConvertUtility.stringToShort("2"));// 配信する累計最大回数
		appMessages.setDailyMaxSendCount(ConvertUtility.stringToShort("3"));// 1日に配信する最大回数
		appMessages.setEventId(ConvertUtility.stringToLong("1"));// 起動トリガー
		appMessages.setFsDeliveryStatus(null);// FS連携状況
		appMessages.setFsTestDeliveryStatus(null);// FSテスト配信状況
		appMessages.setFsStopStatus(null);// FS公開停止状況
		appMessages.setCreateUserId(null);// 作成者ID
		appMessages.setCreateDate(null);// 作成日
		appMessages.setUpdateUserId(null);// 更新者ID
		appMessages.setUpdateDate(null);// 更新日
		appMessages.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());// 削除フラグ

		return Optional.of(appMessages);

	}

	/**
	 * アプリ内メッセージ配信期間リスト生成
	 * 
	 * @return アプリ内メッセージ配信期間リスト
	 * @throws ParseException 
	 * 
	 */
	private List<AppMessageSendPeriods> createAppMessageSendPeriodsList() throws ParseException {

		List<AppMessageSendPeriods> appMessageSendPeriodsList = new ArrayList<AppMessageSendPeriods>();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		for (int i = 1; i <= 3; i++) {
			AppMessageSendPeriods appMessageSendPeriods = new AppMessageSendPeriods();

			appMessageSendPeriods.setAppMessageSendPeriodId(ConvertUtility.stringToLong("50" + 1)); // アプリ内メッセージ配信期間ID
			appMessageSendPeriods.setAppMessageId(ConvertUtility.stringToLong("400")); // アプリ内メッセージID
			appMessageSendPeriods.setSendPeriodFrom(new Timestamp(df.parse("2021-08-16 00:00:00").getTime())); // 配信期間日時FROM
			appMessageSendPeriods.setSendPeriodTo(new Timestamp(df.parse("2021-08-16 01:00:00").getTime())); // 配信期間日時TO
			appMessageSendPeriods.setCreateUserId(null); // 作成者ID
			appMessageSendPeriods.setCreateDate(null); // 作成日
			appMessageSendPeriods.setUpdateUserId(null); // 更新者ID
			appMessageSendPeriods.setUpdateDate(null); // 更新日
			appMessageSendPeriods.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			appMessageSendPeriodsList.add(appMessageSendPeriods);
		}

		return appMessageSendPeriodsList;

	}

	/**
	 * イベントマスタ生成
	 * 
	 * @param messageType メッセージタイプ
	 * @param messageFormat メッセージフォーマット
	 * @param fsSegmentId FSセグメントID
	 * 
	 * @return イベントマスタ
	 * 
	 */
	private Optional<MstEvent> createMstEvent() {

		MstEvent mstEvent = new MstEvent();

		mstEvent.setEventId(ConvertUtility.stringToLong("600")); // イベントID
		mstEvent.setEventName("イベント名"); // イベント名
		mstEvent.setCreateUserId(null); // 作成者ID
		mstEvent.setCreateDate(null); // 作成日
		mstEvent.setUpdateUserId(null); // 更新者ID
		mstEvent.setUpdateDate(null); // 更新日
		mstEvent.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

		return Optional.of(mstEvent);

	}

	/**
	 * Push通知テーブル生成
	 * 
	 * @param messageType メッセージタイプ
	 * @param messageFormat メッセージフォーマット
	 * @param fsSegmentId FSセグメントID
	 * 
	 * @return イベントマスタ
	 * 
	 */
	private Optional<PushNotifications> createPushNotifications() {

		PushNotifications pushNotifications = new PushNotifications();

		pushNotifications.setPushNotificationId(ConvertUtility.stringToLong("700")); // Push通知ID
		pushNotifications.setFsPushNotificationUuid(null); // FSPush通知UUID
		pushNotifications.setPushNotificationType(PushNotificationType.DELIVERY.getValue()); // Push通知種別
		pushNotifications.setCouponId(ConvertUtility.stringToLong("100")); // クーポンID
		pushNotifications.setPushNotificationStatus(PushNotificationStatus.APPROVED.getValue()); // Push通知ステータス
		pushNotifications.setPushNotificationText("Push通知本文"); // Push通知本文
		pushNotifications.setNotificationTitle("お知らせ件名"); // お知らせ件名
		pushNotifications.setNotificationBody("お知らせ本文"); // お知らせ本文
		pushNotifications.setHeaderImageUrl("ヘッダ画像"); // ヘッダ画像
		pushNotifications.setHeaderImage("ヘッダ画像（本体）".getBytes()); // ヘッダ画像（本体）
		pushNotifications.setMimeType("MIMEタイプ"); // MIMEタイプ
		pushNotifications.setButtonDisplayName("ボタン名称"); // ボタン名称
		pushNotifications.setFsSegmentId(null); // FSセグメントID
		pushNotifications.setSendDate(null); // 配信日時
		pushNotifications.setFsDeliveryStatus(null); // FS連携状況
		pushNotifications.setFsTestDeliveryStatus(null); // FSテスト配信状況
		pushNotifications.setFsStopStatus(null); // FS公開停止状況
		pushNotifications.setCreateUserId(null); // 作成者ID
		pushNotifications.setCreateDate(null); // 作成日
		pushNotifications.setUpdateUserId(null); // 更新者ID
		pushNotifications.setUpdateDate(null); // 更新日
		pushNotifications.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

		return Optional.of(pushNotifications);

	}

	/**
	 * Push通知配信期間リスト生成
	 * 
	 * @return Push通知配信期間リスト
	 * @throws ParseException 
	 * 
	 */
	private List<PushNotificationSendPeriods> createPushNotificationSendPeriodsList() throws ParseException {

		List<PushNotificationSendPeriods> pushNotificationSendPeriodsList = new ArrayList<PushNotificationSendPeriods>();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		for (int i = 1; i <= 3; i++) {
			PushNotificationSendPeriods pushNotificationSendPeriods = new PushNotificationSendPeriods();

			pushNotificationSendPeriods.setPushNotificationSendPeriodId(ConvertUtility.stringToLong("80" + 1)); // Push通知配信期間ID
			pushNotificationSendPeriods.setPushNotificationId(ConvertUtility.stringToLong("700")); // Push通知ID
			pushNotificationSendPeriods.setSendPeriodFrom(new Timestamp(df.parse("2021-08-16 00:00:00").getTime())); // 配信期間日時FROM
			pushNotificationSendPeriods.setSendPeriodTo(new Timestamp(df.parse("2021-08-16 01:00:00").getTime())); // 配信期間日時TO
			pushNotificationSendPeriods.setCreateUserId(null); // 作成者ID
			pushNotificationSendPeriods.setCreateDate(null); // 作成日
			pushNotificationSendPeriods.setUpdateUserId(null); // 更新者ID
			pushNotificationSendPeriods.setUpdateDate(null); // 更新日
			pushNotificationSendPeriods.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			pushNotificationSendPeriodsList.add(pushNotificationSendPeriods);
		}

		return pushNotificationSendPeriodsList;

	}

	/**
	 * センサーマスタリスト生成
	 * 
	 * @aram sensorCategory センサーカテゴリ
	 * @aram sensorSubcategory センサーサブカテゴリ
	 * 
	 * @return センサーマスタリスト
	 * 
	 */
	private List<MstSensor> createMstSensorList(SensorCategory sensorCategory, SensorSubcategory sensorSubcategory) {

		List<MstSensor> mstSensorList = new ArrayList<MstSensor>();

		for (int i = 1; i <= 3; i++) {
			MstSensor mstSensor = new MstSensor();

			mstSensor.setSensorId(ConvertUtility.stringToLong("90" + 1)); // センサーID
			mstSensor.setStoreId(ConvertUtility.stringToLong("1000")); // 店舗ID
			mstSensor.setSensorCategory(sensorCategory.getValue()); // センサーカテゴリ
			mstSensor.setSensorSubcategory(sensorSubcategory.getValue()); // センサーサブカテゴリ
			mstSensor.setSensorAttribute1("-1"); // センサー属性1
			mstSensor.setSensorAttribute2("-2"); // センサー属性2
			mstSensor.setSensorAttribute3("-3"); // センサー属性3
			mstSensor.setSensorAttribute4("-4"); // センサー属性4
			mstSensor.setCreateUserId(null); // 作成者ID
			mstSensor.setCreateDate(null); // 作成日
			mstSensor.setUpdateUserId(null); // 更新者ID
			mstSensor.setUpdateDate(null); // 更新日
			mstSensor.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue()); // 削除フラグ

			mstSensorList.add(mstSensor);
		}

		return mstSensorList;

	}

}
