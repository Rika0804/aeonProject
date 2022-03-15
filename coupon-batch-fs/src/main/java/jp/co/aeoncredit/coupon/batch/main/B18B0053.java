package jp.co.aeoncredit.coupon.batch.main;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.jp.awag.common.dao.DAOParameter;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsApiUri;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.CouponUpdateInputDTO;
import jp.co.aeoncredit.coupon.constants.AppMessageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsStopStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationType;
import jp.co.aeoncredit.coupon.constants.properties.AppMessagesProps;
import jp.co.aeoncredit.coupon.constants.properties.CouponsProps;
import jp.co.aeoncredit.coupon.constants.properties.PushNotificationsProps;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSクーポン公開停止バッチ
 * 
 * @author truongduyphuong
 * @version 1.0
 */
@Named("B18B0053")
@Dependent
public class B18B0053 extends BatchFSApiCalloutBase {

  /** バッチID */
  protected static final String BATCH_ID = "B18B0053";

  /** SQL NAME */
  protected static final String SQL_UPDATE_FS_STOP_STATUS = "updateFsStopStatus";
  protected static final String SQL_SELECT_APP_MESSAGES = "selectAppMessages";
  protected static final String SQL_SELECT_PUSH_NOTIFICATIONS = "selectPushNotifications";

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

  /** ログ */
  protected Logger myLog = getLogger();
  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

  /** URL */
  private String proUrl;

  /** 処理対象件数 */
  private int sum = 0;

  /** 処理成功件数 */
  private int success = 0;

  /** 処理失敗件数 */
  private int fail = 0;

  /** 処理スキップ件数 */
  private int skip = 0;

  /** "count" key word in property file */
  private static final String COUNT_PROPERTY_KEY_WORD = "fs.coupon.cancel.batch.retry.count";

  /** "sleep time" key word in property file */
  private static final String SLEEP_TIME_PROPERTY_KEY_WORD =
      "fs.coupon.cancel.batch.retry.sleep.time";

  /** "duration" key word in property file */
  private static final String DURATION_PROPERTY_KEY_WORD =
      "fs.coupon.cancel.batch.timeout.duration";

  /** API実行リトライ回数 */
  private int apiRetryCount;

  /** API実行リトライ時スリープ時間(ミリ秒) */
  private int apiSleepTime;

  /** タイムアウト期間(秒) */
  private int apiTimeoutDuration;

  /** クーポン更新 */
  private static final String COUPON_UPDATE = "クーポン更新";

  /** Push通知テーブル */
  private static final String PUSH_NOTIFICATION_TABLE = "Push通知テーブル";

  /** FS連携済み */
  private static final String FS_LINKED = "FS連携済み";

  /** アプリ内メッセージテーブル */
  private static final Object APP_MESSAGE_TABLE = "アプリ内メッセージテーブル";

  /** date now */
  private Timestamp dateNow = DateUtils.now();

  /** Object mapper */
  private ObjectMapper mapper = new ObjectMapper();

  @Inject
  protected CouponsDAOCustomize couponsDao;

  /** List of Coupons */
  private List<Coupons> couponsList;


  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {

    // (1) Export start message.
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0053.getBatchName()));

    // read file properties
    readProperties();

    String returnCode = processBatch();

    // Export end message
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0053.getBatchName(), ProcessResult.SUCCESS.getValue().equals(returnCode)));

    return setExitStatus(returnCode);
  }

  /**
   * バッチの処理
   * 
   * @return value of batch's processing ("0" or "1")
   */
  private String processBatch() {

    try {
      // (2) Get token confirm B18BC001
      AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

      // (2.1b) If get fail
      // (2.1b.1) If authTokenResult is maintain
      if (AuthTokenResult.MAINTENANCE.equals(authTokenResult)) {
        // set "0" to return value at abnormal end
        return ProcessResult.SUCCESS.getValue();
      }

      // (2.1b.2) If B18BC001 is fail
      if (AuthTokenResult.FAILURE.equals(authTokenResult)) {
        // ((2.1b.2.2) return 1, end process
        return ProcessResult.FAILURE.getValue();
      }

      // (2.1a) If B18BC001 is success
      if (AuthTokenResult.SUCCESS.equals(authTokenResult)) {

        // (3) Get record from database
        getCouponList();

        if (CollectionUtils.isEmpty(couponsList)) {
          // export message
          myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(),
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
                  BatchInfo.B18B0053.getBatchName(), Constants.NO_DETAIL)));
          // return 0, end process
          return ProcessResult.SUCCESS.getValue();

        }

        // total number of records to be processed
        sum = couponsList.size();

        // (3.2) update fs_stop_status
        updateCouponsFsStopStatus(couponsList, FsStopStatus.CONNECTING.getValue());

        // (4) process list coupon and connect FS API
        if (!processCouponListWithFsAPI()) {
          exportCountRecordLog();
          return ProcessResult.FAILURE.getValue();
        }

        exportCountRecordLog();
        // Set "0" to return value at abnormal end
        return ProcessResult.SUCCESS.getValue();

      }

    } catch (Exception e) {
      myLog.error(e.getMessage(), e);
      // Set "1" to return value at abnormal end
    	myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
      return ProcessResult.FAILURE.getValue();
    }

    return ProcessResult.FAILURE.getValue();
  }

  /**
   * Get coupons list with condition FS STOP STATUS = 1:FS連携待ち or FS STOP STATUS = 2：FS連携中
   * 
   */
  private void getCouponList() {

    couponsList = new ArrayList<Coupons>();

    // (3.1)Get record from table クーポン/coupon.
    DAOParameter daoParamStatusWaiting = new DAOParameter();
    daoParamStatusWaiting.set(CouponsProps.FS_STOP_STATUS, FsStopStatus.WAITING.getValue());
    daoParamStatusWaiting.set(CouponsProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
    List<Coupons> couponsListStatusWaiting =
        couponsDao.find(daoParamStatusWaiting, null, false, null);


    DAOParameter daoParamStatusConnecting = new DAOParameter();
    daoParamStatusConnecting.set(CouponsProps.FS_STOP_STATUS, FsStopStatus.CONNECTING.getValue());
    daoParamStatusConnecting.set(CouponsProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
    List<Coupons> couponsListStatusConnecting =
        couponsDao.find(daoParamStatusConnecting, null, false, null);


    if (!CollectionUtils.isEmpty(couponsListStatusWaiting)) {
      couponsList.addAll(couponsListStatusWaiting);
    }
    if (!CollectionUtils.isEmpty(couponsListStatusConnecting)) {
      couponsList.addAll(couponsListStatusConnecting);
    }

  }

  /**
   * Export log for each processing
   * 
   */
  private void exportCountRecordLog() {
    // (5) Log processing number
    String countRecordMessage =
        String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
            BatchInfo.B18B0053.getBatchName(), sum, success, fail, skip, Constants.NO_DETAIL);
    myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), countRecordMessage));
  }

  /**
   * Update FS stop status
   * 
   * @param couponList coupon list
   * @param fsStopStatus FS stop status
   */
  private void updateCouponsFsStopStatus(List<Coupons> couponList, String fsStopStatus) {
    try {
      transactionBegin(BATCH_ID);
      for (Coupons coupons : couponList) {
        updateFsStopStatus(coupons, fsStopStatus);
      }
      transactionCommit(BATCH_ID);
    } catch (Exception e) {
      myLog.error(e.getMessage(), e);
      transactionRollback(BATCH_ID);
      throw e;
    }
  }

  /**
   * Update FS stop status
   * 
   * @param coupons coupon
   * @param fsStopStatus FS stop status
   */
  private void updateFsStopStatus(Coupons coupons, String fsStopStatus) {

    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(CouponsProps.FS_STOP_STATUS, fsStopStatus);
    paramMap.put(CouponsProps.UPDATE_USER_ID, BATCH_ID);
    paramMap.put(CouponsProps.UPDATE_DATE, dateNow);
    paramMap.put(CouponsProps.COUPON_ID, coupons.getCouponId());

    sqlExecute(BATCH_ID, SQL_UPDATE_FS_STOP_STATUS, paramMap);
  }

  /**
   * Process coupon list and connect to API
   * 
   * @return true if success and false if fail
   * @throws JsonProcessingException Intermediate base class for all problems encountered when
   *         processing (parsing, generating) JSON content that are not pure I/O problems.
   */
  private boolean processCouponListWithFsAPI() throws JsonProcessingException {
    List<Coupons> updateList = new ArrayList<Coupons>();
    FsApiCallResponse response = null;

    // (4) Connect FS API
    for (Coupons coupons : couponsList) {
    	
    	//検証用ログ出力
    	myLog.info(String.format("処理対象：クーポンID = %s",coupons.getCouponId().toString()));
    	
      // (4.1)
      // (4.1.1)
      if (coupons.getCouponType().equals(CouponType.APP_EVENT.getValue())) {
        List<Object[]> resultList = getListAppMessage(coupons);
        // (4.1.1b)
        if (CollectionUtils.isEmpty(resultList)) {
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB011, APP_MESSAGE_TABLE,
              FS_LINKED, coupons.getCouponId().toString()));
          coupons.setFsStopStatus(FsStopStatus.WAITING.getValue());
          coupons.setUpdateDate(dateNow);
          coupons.setUpdateUserId(BATCH_ID);
          updateList.add(coupons);
          continue;
        }


        // (4.1.2)
      } else if (coupons.getCouponType().equals(CouponType.SENSOR_EVENT.getValue())) {
        List<Object[]> resultList = getListPushNotifications(coupons);

        // (4.1.2b)
        if (CollectionUtils.isEmpty(resultList)) {
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB011, PUSH_NOTIFICATION_TABLE,
              FS_LINKED, coupons.getCouponId().toString()));
          coupons.setFsStopStatus(FsStopStatus.WAITING.getValue());
          coupons.setUpdateDate(dateNow);
          coupons.setUpdateUserId(BATCH_ID);
          updateList.add(coupons);
          continue;
        }

      }

      // (4.2) Connect to FANSHIP API
      // ※ set ${provider_uuid} in URI
      response = connectFsAPI(coupons);
      
      
      // (4.2a.1a) If API's return value is success status => (4.2a.2)
      if (FsApiCallResult.SUCCESS.equals(response.getFsApiCallResult())) {

        success++;
        // 4.3 Update table クーポン/coupon
        coupons.setFsStopStatus(FsStopStatus.CONNECTED.getValue());
        //log end
        myLog.info(String.format("処理正常終了：クーポンID = %s",coupons.getCouponId().toString()));
      } else {
        fail++;
        // Update table クーポン/coupon
        coupons.setFsStopStatus(FsStopStatus.WAITING.getValue());
      }

      coupons.setUpdateDate(dateNow);
      coupons.setUpdateUserId(BATCH_ID);
      updateList.add(coupons);
      
      // end for
    }

    updateAllCoupons(updateList);
    
    FsApiCallResult fsApiCallResult = response.getFsApiCallResult();
    if (response != null && (FsApiCallResult.OTHERS_ERROR.equals(fsApiCallResult)
        || FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResult)
        || FsApiCallResult.SERVER_ERROR.equals(fsApiCallResult)
        || FsApiCallResult.TIMEOUT.equals(fsApiCallResult))) {
      return false;
    }

    return true;
  }


  /**
   * Update all coupons after call FS API
   * 
   * @param updateList
   */
  private void updateAllCoupons(List<Coupons> updateList) {
    try {
      transactionBegin(BATCH_ID);
      for (Coupons coupons : updateList) {
        couponsDao.update(coupons);
      }
      transactionCommit(BATCH_ID);
    } catch (Exception e) {
      myLog.error(e.getMessage(), e);
      transactionRollback(BATCH_ID);
      throw e;
    }
  }

  /**
   * Get list from PUSH_NOTIFICATIONS table
   * 
   * @param coupons coupon
   * @return List<Object[]> list of PUSH_NOTIFICATIONS
   */
  private List<Object[]> getListPushNotifications(Coupons coupons) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PushNotificationsProps.COUPON_ID, coupons.getCouponId());
    paramMap.put(PushNotificationsProps.PUSH_NOTIFICATION_TYPE,
        PushNotificationType.DELIVERY.getValue());
    paramMap.put(PushNotificationsProps.FS_STOP_STATUS, FsStopStatus.CONNECTED.getValue());
    paramMap.put(PushNotificationsProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
    return sqlSelect(BATCH_ID, SQL_SELECT_PUSH_NOTIFICATIONS, paramMap);
  }


  /**
   * Get list from APP_MESSAGES table
   * 
   * @param coupons coupon
   * @return List<Object[]> list of APP_MESSAGES
   */
  private List<Object[]> getListAppMessage(Coupons coupons) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(AppMessagesProps.COUPON_ID, coupons.getCouponId());
    paramMap.put(AppMessagesProps.APP_MESSAGE_TYPE, AppMessageType.DELIVERY.getValue());
    paramMap.put(AppMessagesProps.FS_STOP_STATUS, FsStopStatus.CONNECTED.getValue());
    paramMap.put(AppMessagesProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
    return sqlSelect(BATCH_ID, SQL_SELECT_APP_MESSAGES, paramMap);
  }


  /**
   * Execute connect API
   * 
   * @param coupons coupon
   * @return FsApiCallResponse HTTP response
   * @throws JsonProcessingException Intermediate base class for all problems encountered when
   *         processing (parsing, generating) JSON content that are not pure I/O problems.
   */
  private FsApiCallResponse connectFsAPI(Coupons coupons) throws JsonProcessingException {
    String url = reverseProxyUrl + proUrl.replace("${uuid}", coupons.getFsCouponUuid());

    CouponUpdateInputDTO couponUpdateInputDTO = new CouponUpdateInputDTO();
    couponUpdateInputDTO.setIsOpen("false");
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    String script = mapper.writeValueAsString(couponUpdateInputDTO);

    List<Integer> successHttpStatusList = Arrays.asList(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

    return callFanshipApi(BATCH_ID, COUPON_UPDATE, url, script, HttpMethodType.PATCH,
        ContentType.APPLICATION_JSON, TokenHeaderType.X_POPINFO_MAPI_TOKEN, successHttpStatusList,
        apiRetryCount, apiSleepTime, apiTimeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);
  }

  /**
   * Read properties file
   * 
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0053.getBatchId());
    proUrl = properties.getProperty(FsApiUri.CANCEL_FS_COUPON.getValue());
    apiRetryCount = ConvertUtility.stringToInteger(properties.getProperty(COUNT_PROPERTY_KEY_WORD));
    apiSleepTime =
        ConvertUtility.stringToInteger(properties.getProperty(SLEEP_TIME_PROPERTY_KEY_WORD));
    apiTimeoutDuration =
        ConvertUtility.stringToInteger(properties.getProperty(DURATION_PROPERTY_KEY_WORD));
  }


}
