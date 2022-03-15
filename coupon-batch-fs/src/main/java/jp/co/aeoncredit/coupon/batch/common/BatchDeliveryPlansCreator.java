package jp.co.aeoncredit.coupon.batch.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.DeliveryPlansFileType;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryPlanListDTO;
import jp.co.aeoncredit.coupon.batch.form.DeliveryPlanListATMForm;
import jp.co.aeoncredit.coupon.batch.form.DeliveryRequestListForm;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeliveryTarget;
import jp.co.aeoncredit.coupon.constants.DeliveryType;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.TargetFlag;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.DeliveryTargetDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;

/**
 * B18B0009 配信予定・依頼リスト出力バッチ
 * @author m-omori
 *
 */
public class BatchDeliveryPlansCreator extends BatchDBAccessBase {

	/** バッチ共通処理ID*/
	protected static final String BATCH_COMMONPROCESS_ID = BatchInfo.B18B0009.getBatchId();

	/** バッチ共通処理ネーム*/
	protected static final String BATCH_COMMONPROCESS_NAME = BatchInfo.B18B0009.getBatchName();

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_COMMONPROCESS_ID);

	/** 設定ファイル */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_COMMONPROCESS_ID);

	/** ファイル操作 */
	protected BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_COMMONPROCESS_ID);

	/** DBアクセス */
	protected BatchDBAccessBase batchDBAccessBase;

	/** 正常終了_戻り値*/
	protected static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値*/
	protected static final String FAIL_RETURN_VALUE = "1";

	/** クーポンID */
	private String couponId;

	/** 枝番 */
	private String branchId;

	/** csv出力用作業ディレクトリ */
	private String csvDirectory;

	/** zip出力用ディレクトリ */
	private String zipDirectory;

	/** 配信予定リストCSVファイル名 */
	private String planListFileName;

	/** 配信依頼OKリストCSVファイル名 */
	private String requestOkFileName;

	/** 配信依頼NGリストCSVファイル名 */
	private String requestNgFileName;

	/** 配信リストzipファイル名 */
	private String zipFileName;
	
	/** 配信予定リストのファイル名接頭辞 */
	private String yoteiOkPrefix = "";
	
	/** 配信依頼OKリストのファイル名接頭辞 */
	private String iraiOkPrefix = "";
	
	/** 配信依頼NGリストのファイル名接頭辞 */
	private String iraiNgPrefix = "";

	/** 設定ファイルに指定したファイル名で、クーポンIDで置換する文字列 */
	private static final String TEMP_EXP_COUPON_ID = "{couponId}";

	/** 設定ファイルに指定したファイル名で、枝番で置換する文字列 */
	private static final String TEMP_EXP_BRANCH_ID = "{branchId}";

	/** 設定ファイルに指定したファイル名で、日時で置換する文字列 */
	private static final String TEMP_EXT_DATETIME = "{datetime}";

	/** クーポンテーブルのDAO */
	private CouponsDAOCustomize couponsDao;

	/** 配信対象者テーブルのDAO */
	private DeliveryTargetDAOCustomize deliveryTargetDao;

	/** フェッチする必要のある行数 */
	private static final int FETCH_SIZE = 10000;

	/**
	 * コンストラクタ
	 */
	public BatchDeliveryPlansCreator(BatchDBAccessBase batchDBAccessBase, CouponsDAOCustomize couponsDao,
			DeliveryTargetDAOCustomize deliveryTargetDao) {
		super();
		this.batchDBAccessBase = batchDBAccessBase;
		this.couponsDao = couponsDao;
		this.deliveryTargetDao = deliveryTargetDao;
		readProperties();
	}

	/**
	 * プロパティファイル読み込み
	 */
	private void readProperties() {
		Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_COMMONPROCESS_ID);
		csvDirectory = pro.getProperty("delivery.plans.csv.directory");
		zipDirectory = pro.getProperty("delivery.plans.zip.directory");
		planListFileName = pro.getProperty("delivery.plans.csv.yotei.ok.file.name");
		requestOkFileName = pro.getProperty("delivery.plans.csv.irai.ok.file.name");
		requestNgFileName = pro.getProperty("delivery.plans.csv.irai.ng.file.name");
		zipFileName = pro.getProperty("delivery.plans.zip.file.name");
		// ファイル名接頭辞
		yoteiOkPrefix = pro.getProperty("delivery.plans.csv.yotei.ok.file.prefix");
		iraiOkPrefix = pro.getProperty("delivery.plans.csv.irai.ok.file.prefix");
		iraiNgPrefix = pro.getProperty("delivery.plans.csv.irai.ng.file.prefix");
	}

	/**
	 * メイン処理
	 * 
	 * @throws Exception スローされた例外
	 * @return 0：正常；1：更新処理異常
	 */
	public String process(String couponId, String branchId) throws Exception {

		// 起動メッセージを出力する。
		logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_COMMONPROCESS_NAME));

		String result;

		// #1配信予定・依頼リスト出力処理起動
		if (!isCorrectArgument(couponId, branchId)) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB919));
			result = FAIL_RETURN_VALUE;
		} else {
			this.couponId = couponId;
			this.branchId = branchId;
			result = createDeliveryPlans();
		}

		// 終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_COMMONPROCESS_NAME,
				SUCCESS_RETURN_VALUE.equals(result)));

		// 戻り値を返却する。
		return result;
	}

	/**
	 * 引数で渡されたクーポンID, 枝番の形式が正しいことをチェックする
	 * @param argCouponId 引数で渡されたクーポンID
	 * @param argBranchId 引数で渡された枝番
	 * @return 正しい場合true
	 */
	private boolean isCorrectArgument(String argCouponId, String argBranchId) {
		return (argCouponId != null && argBranchId != null && argCouponId.matches("^[0-9]+$")
				&& argBranchId.matches("^[0-9]{3}$"));
	}

	/**
	 * 配信予定・依頼リスト出力バッチ主処理
	 * @return 実行結果
	 * @throws SQLException 
	 */
	private String createDeliveryPlans() throws SQLException {
		
		String result = SUCCESS_RETURN_VALUE;

		// (2.1)出力対象の確認
		// (2.1.1) 【クーポンテーブル】のレコードを取得する。
		logger.debug("クーポンテーブル取得start");
		Optional<Coupons> coupons = couponsDao.findById(ConvertUtility.stringToLong(couponId));
		logger.debug("クーポンテーブル取得end");
		
		// (2.1.1b)レコードが取得できなかった場合、戻り値に"1"を設定し、エラーメッセージをログに出力後、処理終了する。
		if (coupons.isEmpty()) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB926, "クーポンテーブル", "クーポンID=" + couponId));
			result = FAIL_RETURN_VALUE;
			return result;
		}

		Coupons coupon = coupons.get();
		
		// (2.1.2) 取得結果を元に出力対象を決定する。
		DeliveryPlansFileType fileType = determineOutputType(coupon);

		if (fileType == DeliveryPlansFileType.OTHER) {
			// 該当クーポンが条件に一致しない場合は、情報メッセージを出力し、処理を終了する（正常終了）
			String msgDetail = MessageFormat.format("クーポン種別={0},配信対象={1},配信先={2}", coupon.getCouponType(),
					coupon.getDeliveryTarget(), coupon.getDeliveryType());
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB004, msgDetail));
			return result;
		} else {
			// ファイル作成
			Object[] outputFileResult = outputFile(fileType); 
			
			result = (String) outputFileResult[0];
			@SuppressWarnings("unchecked")
			List<String> files = (List<String>) outputFileResult[1];

			// ファイル作成に成功した場合
			if (result.equals(SUCCESS_RETURN_VALUE) && !files.isEmpty()) {
				// ファイル圧縮
				result = deleteAndOutputZipFile(fileType);
			}
			return result;

		}
	}

	/**
	 * 過去のzipファイルを削除し、zipファイルを作成する
	 * 
	 * @param files ... zip化する対象ファイル
	 * @return 処理結果
	 */
	private String deleteAndOutputZipFile(DeliveryPlansFileType fileType) {
		// 過去のzipファイルの削除
		String fileNameRegex = getDeleteFileNameRegex(DeliveryPlansFileType.OTHER, true);
		boolean deleteResult = deleteFiles(zipDirectory, fileNameRegex);
		if (!deleteResult) {
			// 削除失敗時
			return FAIL_RETURN_VALUE;
		}
		
		// zip化するファイルのリスト取得
		List<String> fileList = getToZipFileList(fileType);
		
		logger.debug("以下のファイルをzip化します");
		fileList.forEach(el -> logger.debug(el));
		
		// ファイル名取得
		String zipFileNameAct = getOutputFileName(DeliveryPlansFileType.OTHER, true);

		// zip化
		boolean result = batchFileHandler.createZipFile(fileList, zipDirectory + zipFileNameAct, null);

		// zip化失敗時
		if (!result) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB905, zipDirectory, zipFileNameAct));
			return FAIL_RETURN_VALUE;
		} else {
			return SUCCESS_RETURN_VALUE;
		}

	}
	
	/**
	 * 	zip化対象のファイルリストを取得する
	 * @param fileType ファイル種別
	 * @return ファイル名のリスト
	 */
	private List<String> getToZipFileList (DeliveryPlansFileType fileType) {
		if (fileType == DeliveryPlansFileType.PLAN_LIST_ATM) {
			return getToZipFileListYotei();
		} else if (fileType == DeliveryPlansFileType.REQUEST_LIST_PASSPORT || //
				fileType == DeliveryPlansFileType.REQUEST_LIST_TARGET) {
			return getToZipFileListIrai();
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 配信予定リストの接頭辞から始まるCSVファイル名を取得する<br>
	 * ファイル名は絶対パスを返却する
	 * @return
	 */
	private List<String> getToZipFileListYotei() {
		// CSV出力ディレクトリにある接頭辞から始まるファイルを取得し返却
		String regex = "^" + yoteiOkPrefix.replace(TEMP_EXP_COUPON_ID, couponId) + ".*";
		return batchFileHandler.getFileList(csvDirectory)
				.stream()
				.filter(el -> el.getName().matches(regex))
				.map(el -> el.getAbsolutePath())
				.collect(Collectors.toList());
	}

	/**
	 * 配信依頼OK/NGリストの接頭辞から始まるCSVファイル名を取得する<br>
	 * ファイル名は絶対パスを返却する
	 * @return
	 */
	private List<String> getToZipFileListIrai() {
		// CSV出力ディレクトリにある接頭辞から始まるファイルを取得し返却
		String regexOk = "^" + iraiOkPrefix.replace(TEMP_EXP_COUPON_ID, couponId) + ".*";
		String regexNg = "^" + iraiNgPrefix.replace(TEMP_EXP_COUPON_ID, couponId) + ".*";
		return batchFileHandler.getFileList(csvDirectory)
				.stream()
				.filter(el -> el.getName().matches(regexOk) || el.getName().matches(regexNg))
				.map(el -> el.getAbsolutePath())
				.collect(Collectors.toList());
	}
	
	
	/**
	 * ファイル種別に応じたファイル作成・出力を行う
	 * 
	 * @param fileType ... ファイル種別
	 * @return [処理結果, 出力したファイル名のリスト]
	 * @throws SQLException
	 */
	private Object[] outputFile(DeliveryPlansFileType fileType) throws SQLException {

		if (fileType == DeliveryPlansFileType.PLAN_LIST_ATM) {
			// 配信予定リスト
			return outputPlanListAtmFile();
		} else if (fileType == DeliveryPlansFileType.OTHER) {
			throw new AssertionError();
		} else {
			// 配信依頼リスト
			return outputRequestListFile(fileType);
		}
	}

	/**
	 * B18P0013_配信予定リスト（ATM）ファイルを出力する ファイル出力に失敗した場合、空のリストを返却する
	 * 
	 * @return [実行結果, 出力したファイル名のリスト]
	 * @throws SQLException
	 */
	private Object[] outputPlanListAtmFile() throws SQLException {

		List<String> resultFiles = new ArrayList<>();
		DeliveryPlansFileType fileType = DeliveryPlansFileType.PLAN_LIST_ATM;

		// (2.2.1) 配信対象者テーブルを取得する。
		String result[] = outputDeliveryPlanList();
		String resultCode = result[0];
		String tmpFilePath = result[1];

		if (Objects.equals(resultCode, SUCCESS_RETURN_VALUE)) {
			// 過去ファイル削除し、仮ファイルをリネーム
			if (!deletePastFile(fileType, true)) {
				return new Object[] { FAIL_RETURN_VALUE, resultFiles };
			}
			String filePath = renameTmpFile(fileType, true, tmpFilePath);
			resultFiles.add(filePath);
			return new Object[] { SUCCESS_RETURN_VALUE, resultFiles };
		} else if (Objects.equals(resultCode, FAIL_RETURN_VALUE )){
			// 0件時、仮ファイル削除
			batchFileHandler.deleteFile(tmpFilePath);
			return new Object[] { SUCCESS_RETURN_VALUE, resultFiles };
		} else {
			// その他エラー
			batchFileHandler.deleteFile(tmpFilePath);
			return new Object[] {FAIL_RETURN_VALUE, resultFiles};
		}

	}
	
	/**
	 * 配信予定リストの仮ファイルを出力する <br>
	 * 途中で処理失敗した場合も仮ファイル削除は行わない
	 * @return 出力結果, 出力した仮ファイル名
	 * @throws SQLException
	 */
	private String[] outputDeliveryPlanList() throws SQLException {

		// 仮出力するファイル名
		String tmpFilePath = getTmpFilePath(DeliveryPlansFileType.PLAN_LIST_ATM, true);
		String processType = "配信予定リスト(ATM)出力" ;

        try {
    
          // ヘッダ書き込み
          DeliveryPlanListATMForm form = new DeliveryPlanListATMForm();
          int numberRecord;
    
          // 配信対象リストをDBから取得
          logger.debug("配信予定リスト,配信対象者取得start");
          numberRecord = getDeliveryPlanList(tmpFilePath, form.getHeaderString());
          logger.debug("配信予定リスト,配信対象者取得end");
          if (numberRecord == 0) {
            // 0件の場合、ファイルを作成しないでログ出力する。
            logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, processType, "詳細なし"));
            return new String[] {FAIL_RETURN_VALUE, tmpFilePath};
          }
    
          // (2.3.1.4)ファイル出力後、件数をログを出力する。
          logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, processType, numberRecord,
              numberRecord, 0, 0, "ファイル名：" + tmpFilePath));
    
          return new String[] {SUCCESS_RETURN_VALUE, tmpFilePath};
    
        } catch (IOException e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931, processType, e));
			return new String[] {"9", tmpFilePath};
		}
	}

	/**
	 * 配信予定リストをDBから取得する 取得できない場合、空のリストを返却する。
	 * 
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	protected int getDeliveryPlanList(String tmpFilePath, String header) throws SQLException, IOException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
        int numberRecord = 0;
		try (
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, DeliveryPlanListATMForm.CHARSET);
				BufferedWriter bw = new BufferedWriter(outputStreamWriter)
						) {
			batchDBAccessBase.getDbConnection(BATCH_COMMONPROCESS_ID);
			preparedStatement = batchDBAccessBase.prepareStatement(BATCH_COMMONPROCESS_ID, "selectDeliveryPlanList");
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();
			
			// 取得結果を設定
            bw.write(header + DeliveryPlanListATMForm.LINE_SEPARATOR);
            DeliveryPlanListDTO dto = new DeliveryPlanListDTO();
			while (resultSet.next()) {
                numberRecord++;
				index = 0;
                dto.setCustomerId(resultSet.getString(++index));
                dto.setFamilyCode(resultSet.getString(++index));
                dto.setCommonInsideId(resultSet.getString(++index));
				//write data
				bw.write(dto.toString()+ DeliveryPlanListATMForm.LINE_SEPARATOR);
			}
			return numberRecord;
		} 
	}
	/**
	 * B18P0006_配信依頼リスト ファイルを出力する ファイル出力に失敗した場合、空のリストを返却する
	 * 
	 * @return [処理結果, 出力したファイル名のリスト]
	 * @throws SQLException 
	 */
	protected Object[] outputRequestListFile(DeliveryPlansFileType fileType) throws SQLException {
		String result = SUCCESS_RETURN_VALUE;
		
		List<String> resultFiles = new ArrayList<>();
		
		// (2.3.1)配信依頼OKリスト
		// (2.3.1.1)FSクーポンユーザテーブルと配信対象者テーブルを取得する。
		String[] outputResultOk = outputRequestList(fileType, true);
		String resultOk = outputResultOk[0];
		String tmpFilePathOk = outputResultOk[1];
		
		if (Objects.equals(resultOk, SUCCESS_RETURN_VALUE)) {
			// 過去ファイル削除し、仮ファイルをリネーム
			if (!deletePastFile(fileType, true) ) {
				return new Object[] {FAIL_RETURN_VALUE, resultFiles};
			}
			String filePath = renameTmpFile(fileType, true, tmpFilePathOk);
			resultFiles.add(filePath);
		} else if (Objects.equals(resultOk, FAIL_RETURN_VALUE )){
			// 0件時、仮ファイル削除
			batchFileHandler.deleteFile(tmpFilePathOk);
		} else {
			// その他エラー
			batchFileHandler.deleteFile(tmpFilePathOk);
			return new Object[] {FAIL_RETURN_VALUE, resultFiles};
		}
		
		
		// (2.3.1)配信依頼NGリスト
		// (2.3.1.1)FSクーポンユーザテーブルと配信対象者テーブルを取得する。
		String[] outputResultNg = outputRequestList(fileType, false);
		String resultNg = outputResultNg[0];
		String tmpFilePathNg = outputResultNg[1];
		
		if (Objects.equals(resultNg, SUCCESS_RETURN_VALUE)) {
			// 過去ファイル削除し、仮ファイルをリネーム
			if (!deletePastFile(fileType, false) ) {
				return new Object[] {FAIL_RETURN_VALUE, resultFiles};
			}
			String filePath = renameTmpFile(fileType, false, tmpFilePathNg);
			resultFiles.add(filePath);
		} else if (Objects.equals(resultNg, FAIL_RETURN_VALUE )){
			// 0件時、仮ファイル削除
			batchFileHandler.deleteFile(tmpFilePathNg);
		} else {
			// その他エラー
			batchFileHandler.deleteFile(tmpFilePathNg);
			return new Object[] {FAIL_RETURN_VALUE, resultFiles};
		}
		return new Object[] { result, resultFiles };
	}
	
	/**
	 * 仮ファイルを正しい名称に変更する
	 * @param fileType ファイル種別
	 * @param isOk OKリストの場合true
	 * @param tmpFilePath 仮ファイルのパス
	 * @return リネーム後のファイルパス
	 */
	private String renameTmpFile(DeliveryPlansFileType fileType, boolean isOk, String tmpFilePath) {
		// ファイル名取得
		String fileName = getOutputFileName(fileType, isOk);
		String filePath = Paths.get(csvDirectory, fileName).toString();
		batchFileHandler.moveFile(tmpFilePath, filePath);
		return filePath;
	}
	
	/**
	 * 配信依頼OKリストの仮ファイルを出力する <br>
	 * 途中で処理に失敗した場合でも仮ファイルの削除は行わない
	 * 出力結果は、正常時<code>SUCCESS_RETURN_VALUE</code>、0件時<code>FAIL_RETURN_VALUE</code>、その他異常終了：9
	 * @param fileType ファイル種別
	 * @return 出力結果, 出力した仮ファイルのパス
	 * @throws SQLException 
	 */
	private String[] outputRequestList(DeliveryPlansFileType fileType, boolean isOk) throws SQLException {
		
		// 仮出力するファイル名
		String tmpFilePath = getTmpFilePath(fileType, isOk);
		String processType = (isOk) ? "配信依頼OKリスト出力" : "配信依頼NGリスト出力";
		try {
			
			// ヘッダ書き込み
			DeliveryRequestListForm form = new DeliveryRequestListForm();
			String header = form.getHeaderString(fileType.getCouponType());
            int numberRecord = 0;
      
      
            // 配信対象リストをDBから取得
            logger.debug("配信依頼リスト,配信対象者取得start");
            numberRecord = selectDeliveryRequestList(fileType.getCouponType(), isOk, tmpFilePath, header);
            logger.debug("配信依頼リスト,配信対象者取得end");
            if (numberRecord == 0) {
              // 0件の場合、ファイルを作成しない
              return new String[] {FAIL_RETURN_VALUE, tmpFilePath};
            }
						
			// (2.3.1.4)ファイル出力後、件数をログを出力する。
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, processType, numberRecord,
			    numberRecord, 0, 0, "ファイル名：" + tmpFilePath));
			
			return new String[] {SUCCESS_RETURN_VALUE, tmpFilePath};
			
		} catch (IOException e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931, processType, e));
			return new String[] {"9", tmpFilePath};
		}
	}
	
	/**
	 * 仮ファイルのパスを取得する
	 * @param fileType ファイル種別
	 * @param isOk OKリストの場合<code>true</code> NGリストの場合<code>false</code> ATMクーポンの場合はどちらでも可
	 * @return
	 */
	private String getTmpFilePath(DeliveryPlansFileType fileType, boolean isOk) {
		String fileName = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String datetime = sdf.format(DateUtils.now());
		if (fileType == DeliveryPlansFileType.PLAN_LIST_ATM) {
			fileName = "tmp_deliveryPlanOkAtm_" + datetime; 
		} else if (fileType == DeliveryPlansFileType.REQUEST_LIST_PASSPORT && isOk) {
			fileName = "tmp_deliveryRequestOkPassport_" + datetime;
		} else if (fileType == DeliveryPlansFileType.REQUEST_LIST_PASSPORT && !isOk) {
			fileName = "tmp_deliveryRequestNgPassport_" + datetime;
		} else if (fileType == DeliveryPlansFileType.REQUEST_LIST_TARGET && isOk) {
			fileName = "tmp_deliveryRequestOkTarget_" + datetime;
		} else if (fileType == DeliveryPlansFileType.REQUEST_LIST_TARGET && !isOk) {
			fileName = "tmp_deliveryRequestNgTarget_" + datetime;
		} else {
			throw new IllegalArgumentException();
		}
		
		return Paths.get(csvDirectory, fileName).toString();
	}

	/**
	 * 過去のcsvファイルの削除
	 * @param fileType ファイル種別
	 * @param isOk OKリストの場合<code>true</code>
	 * @return 正常時<code>true</code>
	 */
	private boolean deletePastFile(DeliveryPlansFileType fileType, boolean isOk) {
		// (2.3.1.2)過去の配信予定リストの削除
		String fileNameRegex = getDeleteFileNameRegex(fileType, isOk);
		return deleteFiles(csvDirectory, fileNameRegex);
	}

	/**
	 * 正規表現に合致するファイル名のファイルを削除する。
	 * 
	 * @param directory     ... ディレクトリ
	 * @param fileNameRegex ... ファイル名の正規表現
	 * @return 処理結果
	 */
	private boolean deleteFiles(String directory, String fileNameRegex) {

		List<File> files = batchFileHandler.getFileList(directory);
		// 正規表現に合致するファイルのみのリストに変換
		files = files.stream().filter(file -> file.getName().matches(fileNameRegex)).collect(Collectors.toList());

		// ファイル削除
		for (File file : files) {
			boolean result = batchFileHandler.deleteFile(file.getAbsolutePath());
			if (!result) {
				// ファイル削除失敗時
				logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB906, directory, file.getName()));
				return false;
			}
		}

		return true;
	}

	/**
	 * 出力ファイル名を取得する
	 * 
	 * @param fileType ... ファイル種別（zipファイルの場合、OTHERを指定する）
	 * @param isOk     ... OKリストの場合true
	 * @return
	 */
	private String getOutputFileName(DeliveryPlansFileType fileType, boolean isOk) {

		// 置換後の日時文字列
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateActual = sdf.format(DateUtils.now());

		// zipファイル置換後の日時文字列
		SimpleDateFormat sdfForZip = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String dateActualForZip = sdfForZip.format(DateUtils.now());

		if (fileType == DeliveryPlansFileType.PLAN_LIST_ATM) {
			return replaceFileName(planListFileName, dateActual);
		} else if (fileType == DeliveryPlansFileType.OTHER) {
			return replaceFileName(zipFileName, dateActualForZip);
		} else if (isOk) {
			return replaceFileName(requestOkFileName, dateActual);
		} else {
			return replaceFileName(requestNgFileName, dateActual);
		}
	}

	/**
	 * 削除対象のファイル名を正規表現で取得する
	 * 
	 * @param fileType ... ファイル種別（zipファイルの場合、OTHERを指定する）
	 * @param isOk     ... OKリストの場合true
	 * @return
	 */
	private String getDeleteFileNameRegex(DeliveryPlansFileType fileType, boolean isOk) {

		String dateRegexCsv = "[0-9]{8}_[0-9]{6}"; // CSVファイル名の日時の正規表現（yyyymmdd_hh24miss）
		String dateRegexZip = "[0-9]{17}"; // zipファイル名の日時の正規表現（yyyyMMddHHmmssSSS）

		if (fileType == DeliveryPlansFileType.PLAN_LIST_ATM) {
			// 配信予定リスト（ATM）
			return replaceFileName(planListFileName, dateRegexCsv);
		} else if (fileType == DeliveryPlansFileType.OTHER) {
			return replaceFileName(zipFileName, dateRegexZip);
		} else if (isOk) {
			// 配信依頼OKリスト
			return replaceFileName(requestOkFileName, dateRegexCsv);
		} else {
			// 配信依頼NGリスト
			return replaceFileName(requestNgFileName, dateRegexCsv);
		}

	}

	/**
	 * ファイル名のクーポンIDと枝番を置換する
	 * 
	 * @param fileName ... ファイル名
	 * @param dateTime ... 置換後の日時表現
	 * @return
	 */
	private String replaceFileName(String fileName, String datetime) {
		return fileName.replace(TEMP_EXP_COUPON_ID, couponId).replace(TEMP_EXP_BRANCH_ID, branchId)
				.replace(TEMP_EXT_DATETIME, datetime);
	}

	/**
	 * 配信依頼OK/NGリストをDBから取得する。
	 * 
	 * @param couponType ... クーポン種別
	 * @param isOK       ... OKリストの場合true, NGリストの場合false
	 * @return 取得できない場合、空のリストを返却する
	 * @throws SQLException 
	 * @throws IOException 
	 */
      private int selectDeliveryRequestList(CouponType couponType, boolean isOK, String tmpFilePath,
          String header) throws SQLException, IOException {

		String processName = isOK ? "配信依頼OKリスト作成" : "配信依頼NGリスト作成";
		int numberRecord = 0;

		if (couponType == CouponType.PASSPORT && isOK) {
			// パスポートクーポン OKリスト
		    numberRecord = selectDeliveryRequestListPassportOk(tmpFilePath, header);
		} else if (couponType == CouponType.PASSPORT && !isOK) {
			// パスポートクーポン NGリスト
		    numberRecord = selectDeliveryRequestListPassportNg(tmpFilePath, header);
		} else if (couponType == CouponType.TARGET && isOK) {
			// ターゲットクーポン OKリスト
		    numberRecord = selectDeliveryRequestListTargetOk(tmpFilePath, header);
		} else if (couponType == CouponType.TARGET && !isOK) {
			// ターゲットクーポン NGリスト
		  numberRecord = selectDeliveryRequestListTargetNg(tmpFilePath, header);
		} else {
			throw new IllegalArgumentException();
		}

		// 取得できなかった場合、ファイルを作成しないでログ出力する。
		if (numberRecord == 0) {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, processName, "詳細なし"));
		}
		
		return numberRecord;
	}
	
	/**
	 * 配信依頼OKリスト（パスポートクーポン）をDBから取得する <br>
	 * 取得できない場合、空のリストを返却する
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
  private int selectDeliveryRequestListPassportOk(String tmpFilePath, String header)
      throws SQLException, IOException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
	    int numberRecord = 0;
		try (
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, DeliveryRequestListForm.CHARSET);
				BufferedWriter bw = new BufferedWriter(outputStreamWriter)
						) {
			batchDBAccessBase.getDbConnection(BATCH_COMMONPROCESS_ID);
			preparedStatement = batchDBAccessBase.prepareStatement(BATCH_COMMONPROCESS_ID, "selectDeliveryRequestOkListPassport");
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, TargetFlag.TARGET.getValue());
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, FsDeliveryStatus.DELIVERED.getValue());
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得結果を設定
	        bw.write(header + DeliveryRequestListForm.LINE_SEPARATOR);
			while (resultSet.next()) {
	            numberRecord++;
				index = 0;
				String acsUserCardId = addQuote(resultSet.getString(++index));
				String acsUserCardFamilyCd = addQuote(resultSet.getString(++index));
				String commonInsideId = addQuote(resultSet.getString(++index));
                // write data
                bw.write(acsUserCardId + Constants.COMMA + acsUserCardFamilyCd + Constants.COMMA
                    + commonInsideId + DeliveryRequestListForm.LINE_SEPARATOR);
			}
			return numberRecord;
		} 
	}
	
	
	/**
	 * 配信依頼NGリスト（パスポートクーポン）をDBから取得する <br>
	 * 取得できない場合、空のリストを返却する
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
  private int selectDeliveryRequestListPassportNg(String tmpFilePath, String header)
      throws SQLException, IOException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
	    int numberRecord = 0;
		try (
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, DeliveryRequestListForm.CHARSET);
				BufferedWriter bw = new BufferedWriter(outputStreamWriter)
					){
			batchDBAccessBase.getDbConnection(BATCH_COMMONPROCESS_ID);
			preparedStatement = batchDBAccessBase.prepareStatement(BATCH_COMMONPROCESS_ID, "selectDeliveryRequestNgListPassport");
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, TargetFlag.TARGET.getValue());
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, FsDeliveryStatus.FAILURE.getValue());
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, TargetFlag.EXCLUDED.getValue());
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得結果を設定
            bw.write(header + DeliveryRequestListForm.LINE_SEPARATOR);
			while (resultSet.next()) {
	            numberRecord++;
				index = 0;
				String acsUserCardId =  addQuote(resultSet.getString(++index));
                String acsUserCardFamilyCd =  addQuote(resultSet.getString(++index));
                String commonInsideId =  addQuote(resultSet.getString(++index));
                bw.write(acsUserCardId + Constants.COMMA + acsUserCardFamilyCd + Constants.COMMA
                    + commonInsideId + DeliveryRequestListForm.LINE_SEPARATOR);
			}
			return numberRecord;
		}
	}
	
	/**
	 * 配信依頼OKリスト（ターゲットクーポン）をDBから取得する <br>
	 * 取得できない場合、空のリストを返却する
	 * @return 取得結果リスト
	 * @throws SQLException
	 * @throws IOException 
	 */
  private int selectDeliveryRequestListTargetOk(String tmpFilePath, String header)
      throws SQLException, IOException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
	    int numberRecord = 0;

		try (	
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, DeliveryRequestListForm.CHARSET);
				BufferedWriter bw = new BufferedWriter(outputStreamWriter)
				){
			batchDBAccessBase.getDbConnection(BATCH_COMMONPROCESS_ID);
			preparedStatement = batchDBAccessBase.prepareStatement(BATCH_COMMONPROCESS_ID, "selectDeliveryRequestOkListTarget");
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, FsDeliveryStatus.DELIVERED.getValue());
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得結果を設定
            bw.write(header + DeliveryRequestListForm.LINE_SEPARATOR);
			while (resultSet.next()) {
				index = 0;
				String commonInsideId = resultSet.getString(++index);
				//write data
				if( commonInsideId != null) {
	                bw.write(addQuote(commonInsideId) + DeliveryRequestListForm.LINE_SEPARATOR);
	                numberRecord++;
				}
			}
			return numberRecord;
		} 
	}

	/**
	 * 配信依頼NGリスト（ターゲットクーポン）をDBから取得する <br>
	 * 取得できない場合、空のリストを返却する
	 * @return 取得結果リスト
	 * @throws SQLException
	 * @throws IOException 
	 */
  private int selectDeliveryRequestListTargetNg(String tmpFilePath, String header)
      throws SQLException, IOException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
	    int numberRecord = 0;

		try (	
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, DeliveryRequestListForm.CHARSET);
				BufferedWriter bw = new BufferedWriter(outputStreamWriter)
				) {
			batchDBAccessBase.getDbConnection(BATCH_COMMONPROCESS_ID);
			preparedStatement = batchDBAccessBase.prepareStatement(BATCH_COMMONPROCESS_ID, "selectDeliveryRequestNgListTarget");
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, FsDeliveryStatus.FAILURE.getValue());
			preparedStatement.setString(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, TargetFlag.EXCLUDED.getValue());
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得結果を設定
            bw.write(header + DeliveryRequestListForm.LINE_SEPARATOR);
			while (resultSet.next()) {
				index = 0;
				// add commonInsideId
				String commonInsideId = resultSet.getString(++index);
				// write data
				if(commonInsideId != null) {
	                numberRecord++;
	                bw.write(addQuote(commonInsideId) + DeliveryRequestListForm.LINE_SEPARATOR);
				}
			}
			return numberRecord;
		} 
	}

    /**
     * Get csv column content with quote
     * 
     * @return Csv Column Content
     */
    private String addQuote(String column) {
      return column != null ? "\"" + column + "\"" : "\"\"";
    }
	/**
	 * クーポンを元に出力対象を決定する
	 * 
	 * @param coupon ... クーポン
	 * @return 出力対象のファイルタイプ
	 */
	private DeliveryPlansFileType determineOutputType(Coupons coupon) {

		if (isOutputTypeRequestListTarget(coupon)) {
			return DeliveryPlansFileType.REQUEST_LIST_TARGET;
		} else if (isOutputTypeRequestListPassport(coupon)) {
			return DeliveryPlansFileType.REQUEST_LIST_PASSPORT;
		} else if (isOutputTypePlanListAtm(coupon)) {
			return DeliveryPlansFileType.PLAN_LIST_ATM;
		} else {
			return DeliveryPlansFileType.OTHER;
		}
	}

	/*
	 * 出力タイプがB18P0006_配信依頼リスト（ターゲットクーポン）であるか
	 * 
	 * @param coupon ... クーポン
	 * 
	 * @return
	 */
	private boolean isOutputTypeRequestListTarget(Coupons coupon) {

		if (coupon.getCouponType() == null) {
			return false;
		}
		if (coupon.getDeliveryTarget() == null) {
			return false;
		}

		return coupon.getCouponType().equals(CouponType.TARGET.getValue())
				&& coupon.getDeliveryTarget().equals(DeliveryTarget.INDIVIDUAL.getValue());
	}

	/**
	 * 出力タイプがB18P0006_配信依頼リスト（パスポートクーポン）であるか
	 * 
	 * @param coupon ... coupon
	 * @return
	 */
	private boolean isOutputTypeRequestListPassport(Coupons coupon) {

		if (coupon.getCouponType() == null) {
			return false;
		}
		if (coupon.getDeliveryType() == null) {
			return false;
		}
		if (coupon.getDeliveryTarget() == null) {
			return false;
		}

		return coupon.getCouponType().equals(CouponType.PASSPORT.getValue())
				&& coupon.getDeliveryType().equals(DeliveryType.AEON_WALLET_APP.getValue())
				&& coupon.getDeliveryTarget().equals(DeliveryTarget.INDIVIDUAL.getValue());
	}

	/**
	 * 出力タイプがB18P0012_配信予定リスト（ATM）であるか
	 * 
	 * @param coupon
	 * @return
	 */
	private boolean isOutputTypePlanListAtm(Coupons coupon) {

		if (coupon.getCouponType() == null) {
			return false;
		}
		if (coupon.getDeliveryType() == null) {
			return false;
		}
		if (coupon.getDeliveryTarget() == null) {
			return false;
		}

		return coupon.getCouponType().equals(CouponType.PASSPORT.getValue())
				&& coupon.getDeliveryType().equals(DeliveryType.ATM.getValue())
				&& coupon.getDeliveryTarget().equals(DeliveryTarget.INDIVIDUAL.getValue());
	}

}
