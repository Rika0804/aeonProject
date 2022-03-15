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
import jp.co.aeoncredit.coupon.batch.dto.RegisterUpdateDeleteFSStoreInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.RegisterUpdateDeleteFSStoreInputDTOAdditionalItems;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.properties.MstStoresProps;
import jp.co.aeoncredit.coupon.dao.custom.MstStoreDAOCustomize;
import jp.co.aeoncredit.coupon.entity.MstStore;
import jp.co.aeoncredit.coupon.util.BusinessMessage;


/**
 * FS店舗登録・更新・削除バッチ
 * 
 * @author nguyenphuongnga
 * @version 1.0
 */
@Named("B18B0013")
@Dependent
public class B18B0013 extends BatchFSApiCalloutBase {

  /** バッチID */
  private static final String BATCH_ID = "B18B0013";

  /** SQL NAME */
  private static final String SQL_GET_AVAILABLE_COUPON = "getAvailableCoupon";
  private static final String SQL_UPDATE_FS_DELIVERY_STATUS = "updateFSDeliveryStatus";

  /** FS API Function message value */
  private static final String FS_API_CREATE = "利用可能店舗新規作成";
  private static final String FS_API_UPDATE = "利用可能店舗更新";
  private static final String FS_API_DELETE = "利用可能店舗削除";

  /** FS API name value in count message */
  private static final String COUNT_MESSAGE_FS_CREATE = "FS店舗登録";
  private static final String COUNT_MESSAGE_FS_UPDATE = "FS店舗更新";
  private static final String COUNT_MESSAGE_FS_DELETE = "FS店舗削除";

  /** MESSAGE RECORD = 0 INFOR */
  private static final String MESSAGE_PROCESS_NAME = "FS店舗登録・更新・削除";

  /** "count" key word in property file */
  private static final String COUNT_PROPERTY_KEY_WORD =
      "fs.regist.update.delete.batch.store.retry.count";

  /** "sleep time" key word in property file */
  private static final String SLEEP_TIME_PROPERTY_KEY_WORD =
      "fs.regist.update.delete.batch.store.retry.sleep.time";

  /** "duration" key word in property file */
  private static final String DURATION_PROPERTY_KEY_WORD =
      "fs.regist.update.delete.batch.store.timeout.duration";

  /** skip count for create and update */
  private static final int SKIP_COUNT = 0;

  private static final int CREATE_STORE = 1;
  private static final int UPDATE_STORE = 2;
  private static final int DELETE_STORE = 3;

  /** do nothing value */
  private static final int DO_NOTHING_VALUE = 0;


  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

  /** ログ */
  private Logger myLog = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0013.getBatchId());

  /** URL */
  private String proUrl;

  /** createTotalCount */
  private int createTotalCount;

  /** updateTotalCount */
  private int updateTotalCount;

  /** deleteTotalCount */
  private int deleteTotalCount;

  /** スキップ件数 */
  private int deleteskipCount;

  /** 登録処理成功件数 */
  private int createSuccessCount;

  /** 登録処理エラー件数 */
  private int createFailCount;

  /** 更新成功件数 */
  private int updateSuccessCount;

  /** 更新エラー件数 */
  private int updateFailCount;

  /** 削除成功件数 */
  private int deleteSuccessCount;

  /** 削除エラー件数 */
  private int deleteFailCount;

  /** API実行リトライ回数 */
  private int apiRetryCount;

  /** API実行リトライ時スリープ時間(ミリ秒) */
  private int apiSleepTime;

  /** タイムアウト期間(秒) */
  private int apiTimeoutDuration;

  /** date now */
  private Timestamp dateNow = DateUtils.now();

  /** Object mapper */
  private ObjectMapper mapper = new ObjectMapper();

  @Inject
  protected MstStoreDAOCustomize mstStoreDao;

  /** List of MstStore */
  private List<MstStore> mstStoreList;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1) export start message
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0013.getBatchName()));
    // read file properties
    readProperties();
    String returnCode = processBatch();
    // (5) Export end message
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0013.getBatchName(), ProcessResult.SUCCESS.getValue().equals(returnCode)));
    return setExitStatus(returnCode);
  }

  /**
   * バッチの処理
   * 
   * @return value of batch's processing: "0" if batch ends successfully, "1" if batch ends abnormal
   */

  private String processBatch() {
    try {
      // (2) Get info from MST_STORE (mstStoreList)
      getMstStoreList();

      // (2.1b) If mstStoreList is empty
      if (CollectionUtils.isEmpty(mstStoreList)) {
        // export message B18MB006
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(),
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
                MESSAGE_PROCESS_NAME, Constants.NO_DETAIL)));

        // return 0
        return ProcessResult.SUCCESS.getValue();
      }

      // (2.1a) If mstStoreList isn't empty
      // (3) Get FS AUTH token
      AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);


      // (3.1b) If get FS AUTH token fail
      // (3.1b.1) If authTokenResult is maintain
      if (AuthTokenResult.MAINTENANCE.equals(authTokenResult)) {
        return ProcessResult.SUCCESS.getValue();
      }

      // (3.1b.2) If B18BC001 is failure
      if (AuthTokenResult.FAILURE.equals(authTokenResult)) {
        return ProcessResult.FAILURE.getValue();
      }

      // (3.1a) If B18BC001 is success
      if (AuthTokenResult.SUCCESS.equals(authTokenResult)) {

        // (4) Connect API FS.

        // (4.1) Update MST_STORE (FSDeliveryStatus, UpdateUserID, UpdateDate)
        updateFSDeliveryStatusList(mstStoreList, FsDeliveryStatus.DELIVERING.getValue());


        // For each record from MST_STORE 店舗マスタ repeat (4.2) to(4.3)
        if (!cooperateFSAPI()) {
          exportCountRecordLog();
          return ProcessResult.FAILURE.getValue();
        }

        exportCountRecordLog();
        return ProcessResult.SUCCESS.getValue();
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      // Set "1" to return value at abnormal end
      return ProcessResult.FAILURE.getValue();

    }

    return ProcessResult.FAILURE.getValue();
  }


  /**
   * Get master store list with condition FS連携状況 = 1:FS連携待ち or FS連携状況 = 2：FS連携中
   * 
   */
  private void getMstStoreList() {

    mstStoreList = new ArrayList<MstStore>();
    // (2.1) Get info from MST_STORE with condition FS連携状況 = 1:FS連携待ち
    DAOParameter daoParamStatusWaiting = new DAOParameter();
    daoParamStatusWaiting.set(MstStoresProps.FS_DELIVERY_STATUS,
        FsDeliveryStatus.WAITING.getValue());
    List<MstStore> mstStoreListStatusWaiting =
        mstStoreDao.find(daoParamStatusWaiting, null, true, null);

    // (2.1) Get info from MST_STORE with condition FS連携状況 = 2：FS連携中
    DAOParameter daoParamStatusDelivering = new DAOParameter();
    daoParamStatusDelivering.set(MstStoresProps.FS_DELIVERY_STATUS,
        FsDeliveryStatus.DELIVERING.getValue());
    List<MstStore> mstStoreListStatusDelivering =
        mstStoreDao.find(daoParamStatusDelivering, null, true, null);

    if (!CollectionUtils.isEmpty(mstStoreListStatusWaiting)) {
      mstStoreList.addAll(mstStoreListStatusWaiting);
    }
    if (!CollectionUtils.isEmpty(mstStoreListStatusDelivering)) {
      mstStoreList.addAll(mstStoreListStatusDelivering);
    }

  }

  /**
   * Export log for each processing
   * 
   */
  private void exportCountRecordLog() {

    // export log 処理終了前（登録）
    String countCreateRecordMessage =
        String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
            COUNT_MESSAGE_FS_CREATE, createTotalCount, createSuccessCount, createFailCount,
            SKIP_COUNT, Constants.NO_DETAIL);
    myLog.info(
        batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), countCreateRecordMessage));


    // export log 処理終了前（更新）
    String countUpdateRecordMessage =
        String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
            COUNT_MESSAGE_FS_UPDATE, updateTotalCount, updateSuccessCount, updateFailCount,
            SKIP_COUNT, Constants.NO_DETAIL);
    myLog.info(
        batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), countUpdateRecordMessage));


    // export log 処理終了前（削除）
    String countDeleteRecordMessage =
        String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
            COUNT_MESSAGE_FS_DELETE, deleteTotalCount, deleteSuccessCount, deleteFailCount,
            deleteskipCount, Constants.NO_DETAIL);
    myLog.info(
        batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), countDeleteRecordMessage));

  }

  /**
   * check how to handler a store (for insert, update or delete)
   * 
   * @param fsStoreUUID
   * @param deleteFlag
   * @return type of handler
   */
  private Integer determineStore(String fsStoreUUID, String deleteFlag) {
    log.info("determineStore.fsStoreUUID: " + fsStoreUUID);
    log.info("determineStore.deleteFlag: " + fsStoreUUID);
    if (fsStoreUUID == null && DeleteFlag.NOT_DELETED.getValue().equals(deleteFlag)) {
      return CREATE_STORE;
    }

    if (fsStoreUUID != null && DeleteFlag.NOT_DELETED.getValue().equals(deleteFlag)) {
      return UPDATE_STORE;
    }

    if (fsStoreUUID != null && DeleteFlag.DELETED.getValue().equals(deleteFlag)) {
      return DELETE_STORE;
    }

    return DO_NOTHING_VALUE;
  }

  /**
   * FS API 連携
   * 
   * @return false if fail, true if success
   * @throws JsonProcessingException Intermediate base class for all problems encountered when
   *         processing (parsing, generating) JSON content that are not pure I/O problems.
   */
  private boolean cooperateFSAPI() throws JsonProcessingException {

	boolean resultFlg = true;
    String fsStoreUUID;
    String deleteFlag;
    String url = null;
    RegisterUpdateDeleteFSStoreInputDTO inputDTO = null;
    String script = null;
    List<Integer> successHttpStatusList = null;
    FsApiCallResponse response = null;
    String fsApiCallType = null;
    RetryKbn serverErrorType = RetryKbn.NONE;
    HttpMethodType methodType = HttpMethodType.POST;
    
    List<MstStore> updateList = new ArrayList<MstStore>();

    // for all records in mstStoreList, loop the after process
    // (4.2) Decide process for each object
    for (MstStore mstStore : mstStoreList) {

      fsStoreUUID = mstStore.getFsStoreUuid();
      deleteFlag = mstStore.getDeleteFlag();

      if (determineStore(fsStoreUUID, deleteFlag) == DO_NOTHING_VALUE) {
        continue;
      }

      if (CREATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
        // If fsStoreUUID = null and deleteFlag = 0 than create mstStore in FS (4.2a)
        createTotalCount++;

        url = reverseProxyUrl + proUrl;
        inputDTO = convertMstStoreToInputDTO(mstStore);
        script = createScriptFromParam(inputDTO);
        successHttpStatusList = Arrays.asList(HTTPStatus.HTTP_STATUS_CREATED.getValue());
        fsApiCallType = FS_API_CREATE;
        serverErrorType = RetryKbn.SERVER_ERROR;
        methodType = HttpMethodType.POST;


      } else if (UPDATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
        // If fsStoreUUID != null and deleteFlag = 0 than update mstStore in FS (4.2b)
        updateTotalCount++;

        url = reverseProxyUrl + proUrl + mstStore.getFsStoreUuid() + Constants.SYMBOL_SLASH;
        inputDTO = convertMstStoreToInputDTO(mstStore);
        script = createScriptFromParam(inputDTO);
        successHttpStatusList = Arrays.asList(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());
        fsApiCallType = FS_API_UPDATE;
        serverErrorType = RetryKbn.SERVER_ERROR_TIMEOUT;
        methodType = HttpMethodType.PATCH;


      } else if (DELETE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
        // If fsStoreUUID != null and deleteFlag = 1 than delete mstStore in FS (4.2c)
        deleteTotalCount++;

        if (getAvailableCoupon(mstStore) > 0) {
          deleteskipCount++;
          mstStore.setFsDeliveryStatus(FsDeliveryStatus.WAITING.getValue());
          updateList.add(mstStore);
          continue;
        }

        url = reverseProxyUrl + proUrl + mstStore.getFsStoreUuid() + Constants.SYMBOL_SLASH;
        script = null;
        successHttpStatusList = Arrays.asList(HTTPStatus.HTTP_STATUS_DELETED.getValue());
        fsApiCallType = FS_API_DELETE;
        serverErrorType = RetryKbn.SERVER_ERROR_TIMEOUT;
        methodType = HttpMethodType.DELETE;

      }

      response = callFanshipApi(BATCH_ID, fsApiCallType, url, script, methodType,
          ContentType.APPLICATION_JSON, TokenHeaderType.X_POPINFO_MAPI_TOKEN, successHttpStatusList,
          apiRetryCount, apiSleepTime, apiTimeoutDuration, serverErrorType);

      // (4.2a.1a) If API's return value is success status => (4.2a.2)
      if (FsApiCallResult.SUCCESS.equals(response.getFsApiCallResult())) {

        mstStore.setFsDeliveryStatus(FsDeliveryStatus.DELIVERED.getValue());

        if (CREATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          createSuccessCount++;

          String location = response.getResponse().headers().firstValue("Location").orElse("");
          if (!location.isEmpty() && location != null) {
            mstStore.setFsStoreUuid(getUUID(location));
          }

        } else if (UPDATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          updateSuccessCount++;
        } else if (DELETE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          deleteSuccessCount++;
        }

      } else {
        mstStore.setFsDeliveryStatus(FsDeliveryStatus.WAITING.getValue());

        if (CREATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          createFailCount++;
        } else if (UPDATE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          updateFailCount++;
        } else if (DELETE_STORE == determineStore(fsStoreUUID, deleteFlag)) {
          deleteFailCount++;
        }

        if (FsApiCallResult.AUTHENTICATION_ERROR.equals(response.getFsApiCallResult())
				|| FsApiCallResult.CLIENT_ERROR.equals(response.getFsApiCallResult())
				|| FsApiCallResult.SERVER_ERROR.equals(response.getFsApiCallResult())
				|| FsApiCallResult.TIMEOUT.equals(response.getFsApiCallResult())
				|| FsApiCallResult.OTHERS_ERROR.equals(response.getFsApiCallResult())) {
        	resultFlg = false;
		}

      }
      
      mstStore.setUpdateDate(dateNow);
      updateList.add(mstStore);
      // end for
    }

    updateAllStores(updateList);

    return resultFlg;
  }

  /**
   * Update all store after call FS API
   * 
   * @param updateList
   */
  private void updateAllStores(List<MstStore> updateList) {
    try {
      transactionBegin(BATCH_ID);
      for (MstStore mstStore : updateList) {
        log.info("updateAllStores.storeId: " + mstStore.getStoreId());
        mstStoreDao.update(mstStore);
      }
      transactionCommit(BATCH_ID);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      transactionRollback(BATCH_ID);
      throw e;
    }
  }

  /**
   * Get UUID from location
   * 
   * @param location from returned HTTP response
   * @return uuid FS店舗UUID
   */
  private String getUUID(String location) {
    log.info("getUUID.location: " + location);
    String[] locationSplit = location.split(Constants.SYMBOL_SLASH);
    return locationSplit[locationSplit.length - 1];
  }

  /**
   * Convert parameter master store to RegisterUpdateDeleteFSStoreInputDTO
   * 
   * @param mstStore entity of MST_STORE
   * @return inputDTO FS店舗登録・更新・削除APIリクエストパラメータDTO
   */
  private RegisterUpdateDeleteFSStoreInputDTO convertMstStoreToInputDTO(MstStore mstStore) {
    log.info("convertMstStoreToInputDTO.storeId: " + mstStore.getStoreId());
    
    RegisterUpdateDeleteFSStoreInputDTO inputDTO = new RegisterUpdateDeleteFSStoreInputDTO();
    RegisterUpdateDeleteFSStoreInputDTOAdditionalItems inputDTOAddItems =
        new RegisterUpdateDeleteFSStoreInputDTOAdditionalItems();
    

    // add parameter to additional items RegisterUpdateDeleteFSStoreInputDTOAdditionalItems
    inputDTOAddItems.setStoreThumbnailUrl(mstStore.getStoreThumbnailUrl());
    inputDTOAddItems.setStoreShortname(mstStore.getStoreShortname());
    inputDTOAddItems.setStoreAddress(mstStore.getStoreAddress());
    inputDTOAddItems.setStorePhone(mstStore.getStorePhone());
    inputDTOAddItems.setStoreHours(mstStore.getStoreHours());
    inputDTOAddItems.setStoreHoliday(mstStore.getStoreHoliday());


    // add parameter to inputDTO RegisterUpdateDeleteFSStoreInputDTO
    inputDTO.setStoreName(mstStore.getStoreName());
    inputDTO.setAdditionalItems(inputDTOAddItems);
    inputDTO.setStoreLatitude(mstStore.getStoreLatitude());
    inputDTO.setStoreLongitude(mstStore.getStoreLongitude());
    return inputDTO;
  }

  /**
   * 有効クーポンチェック
   * 
   * @param mstStore entity of MST_STORE
   * @return couponCount count of COUPON's records
   */
  private int getAvailableCoupon(MstStore mstStore) {
    log.info("getAvailableCoupon.storeId: " + mstStore.getStoreId());
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(MstStoresProps.STORE_ID, mstStore.getStoreId());
    paramMap.put(MstStoresProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());

    List<Object[]> resultList = sqlSelect(BATCH_ID, SQL_GET_AVAILABLE_COUPON, paramMap);

    if (CollectionUtils.isNotEmpty(resultList)) {
      return resultList.size();
    }

    return 0;
  }

  /**
   * FS連携状況を更新 (List)
   * 
   * @param mstStoreList List of MST_STORE's entity
   * @param fsDeliveryStatus FS連携状況
   */
  private void updateFSDeliveryStatusList(List<MstStore> mstStoreList, String fsDeliveryStatus) {
    log.info("updateFSDeliveryStatusList.fsDeliveryStatus: " + fsDeliveryStatus);
    try {
      transactionBegin(BATCH_ID);
      for (MstStore mstStore : mstStoreList) {
        log.info("updateFSDeliveryStatusList.storeId: " + mstStore.getStoreId());
        updateFSDeliverStatus(mstStore, fsDeliveryStatus);
      }
      transactionCommit(BATCH_ID);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      transactionRollback(BATCH_ID);
      throw e;
    }
  }

  /**
   * FS連携状況を更新
   * 
   * @param mstStore MST_STORE's entity
   * @param fsDeliveryStatus FS連携状況
   */
  private int updateFSDeliverStatus(MstStore mstStore, String fsDeliveryStatus) {
    log.info("updateFSDeliverStatus.storeId: " + mstStore.getStoreId());
    log.info("updateFSDeliverStatus.fsDeliveryStatus: " + fsDeliveryStatus);
    int count;
    Map<String, Object> paramMap = new HashMap<>();

    paramMap.put(MstStoresProps.FS_DELIVERY_STATUS, fsDeliveryStatus);
    paramMap.put(MstStoresProps.UPDATE_DATE, dateNow);
    paramMap.put(MstStoresProps.STORE_ID, mstStore.getStoreId());
    count = sqlExecute(BATCH_ID, SQL_UPDATE_FS_DELIVERY_STATUS, paramMap);
    return count;
  }

  /**
   * Create string from parameter
   * 
   * @param registerUpdateDeleteFSStoreInputDTO FS店舗登録・更新・削除APIリクエストパラメータDTO
   * @throws JsonProcessingException Intermediate base class for all problems encountered when
   *         processing (parsing, generating) JSON content that are not pure I/O problems. Regular
   *         {@link java.io.IOException}s will be passed through as is. Sub-class of
   *         {@link java.io.IOException} for convenience.
   */
  private String createScriptFromParam(
      RegisterUpdateDeleteFSStoreInputDTO registerUpdateDeleteFSStoreInputDTO)
      throws JsonProcessingException {
    log.info("createScriptFromParam.storeName: " + registerUpdateDeleteFSStoreInputDTO.getStoreName());
    String script = "";
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    script = mapper.writeValueAsString(registerUpdateDeleteFSStoreInputDTO);
    return script;
  }

  /**
   * プロパティファイルを読み込む
   * 
   */
  private void readProperties() {

    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0013.getBatchId());
    proUrl = properties.getProperty(FsApiUri.AVAILABLE_STORE.getValue());
    apiRetryCount = ConvertUtility.stringToInteger(properties.getProperty(COUNT_PROPERTY_KEY_WORD));
    apiSleepTime =
        ConvertUtility.stringToInteger(properties.getProperty(SLEEP_TIME_PROPERTY_KEY_WORD));
    apiTimeoutDuration =
        ConvertUtility.stringToInteger(properties.getProperty(DURATION_PROPERTY_KEY_WORD));
  }

}
