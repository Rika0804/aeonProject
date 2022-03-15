package jp.co.aeoncredit.coupon.batch.constants;

/**
 * Constants
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public class Constants {
  /** FSログ取込（アプリ利用イベント）FSクーポン実績連携テーブルのシーケンス */
  public static final String SEQ_FS_EVENTS_FOR_COUPON_ID = "SEQ_FS_EVENTS_FOR_COUPON_ID";

  /** FSログ取込（アプリ利用イベント）FSクーポン配信実績のシーケンス */
  public static final String SEQ_FS_COUPON_DELIVERY_RESULT_ID = "SEQ_FS_COUPON_DELIVERY_RESULT_ID";

  /** FSログ取込（アプリ利用イベント）FSクーポン取得実績のシーケンス */
  public static final String SEQ_COUPON_ACQUISITION_RESULT_ID = "SEQ_COUPON_ACQUISITION_RESULT_ID";

  /** FSログ取込（アプリ利用イベント）FSクーポン利用実績のシーケンス */
  public static final String SEQ_FS_COUPON_USE_RESULT_ID = "SEQ_FS_COUPON_USE_RESULT_ID";

  /** FSログ取込（アプリ利用イベント）AWS S3からダウンロードする際のディレクトリ */
  public static final String S3_DIRECTORY_DETECT_EVENTS = "fs.log.import.events.s3.directory";

  /** FSログ取込（アプリ利用イベント）AWS S3からダウンロードする際のファイル名 */
  public static final String S3_FILE_NAME_DETECT_EVENTS = "fs.log.import.events.s3.file.name";

  /** FSログ取込（アプリ利用イベント）ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_DETECT_EVENTS =
      "fs.log.import.events.download.directory";

  /** FSログ取込（アプリ利用イベント）MA用配信結果ファイル格納先ディレクトリ */
  public static final String RESULT_FILE_DIRECTORY_EVENTS =
      "fs.log.import.events.result.file.directory";

  /** FSログ取込（アプリ利用イベント）AWS S3からダウンロードして解凍したファイル名 */
  public static final String UNGZ_FILE_NAME_DETECT_EVENTS = "fs.log.import.events.ungz.file.name";

  /** AWS S3からダウンロードする際のディレクトリ */
  public static final String S3_DIRECTORY_DETECT_IBEACON = "fs.log.import.ibeacon.s3.directory";

  /** AWS S3からダウンロードする際のファイル名 */
  public static final String S3_FILE_NAME_DETECT_IBEACON = "fs.log.import.ibeacon.s3.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_DETECT_IBEACON =
      "fs.log.import.ibeacon.download.directory";

  /** AWS S3からダウンロードして解凍したファイル名 */
  public static final String UNGZ_FILE_NAME_DETECT_IBEACON = "fs.log.import.ibeacon.ungz.file.name";

  /** FS API URL HEADER Content-Type */
  public static final String FS_API_URL_HEADER_CONTENT_TYPE = "Content-Type";

  /** FS API URL HEADER application/json */
  public static final String FS_API_URL_HEADER_APP_JSON = "application/json";

  /** FS API URL HEADER application/json;charset=UTF-8 */
  public static final String FS_API_URL_HEADER_APP_JSON_CHARSET = "application/json;charset=UTF-8";

  /** シーケンス */
  public static final String SEQ_FS_EVENTS_FOR_IBEACON_ID = "SEQ_FS_EVENTS_FOR_IBEACON_ID";

  /** AWS S3からダウンロードする際のディレクトリ */
  public static final String S3_DIRECTORY_IMPORT_USER = "fs.log.import.users.s3.directory";

  /** AWS S3からダウンロードする際のファイル名 */
  public static final String S3_FILE_NAME_IMPORT_USER = "fs.log.import.users.s3.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_IMPORT_USER =
      "fs.log.import.users.download.directory";

  /** AWS S3からダウンロードして解凍したファイル名 */
  public static final String UNGZ_FILE_NAME_IMPORT_USER = "fs.log.import.users.ungz.file.name";

  /** シーケンス */
  public static final String SEQ_FS_USER_ID = "SEQ_FS_USER_ID";

  /** Timestamp format */
  public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  /** SLASH */
  public static final String SYMBOL_SLASH = "/";

  /** シーケンス */
  public static final String SEQ_FS_VISITOR_ID = "SEQ_FS_VISITOR_ID";

  /** AWS S3からダウンロードする際のディレクトリ */
  public static final String S3_DIRECTORY_IMPORT_VISITORS = "fs.log.import.visitors.s3.directory";

  /** AWS S3からダウンロードする際のファイル名 */
  public static final String S3_FILE_NAME_IMPORT_VISITORS = "fs.log.import.visitors.s3.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_IMPORT_VISITORS =
      "fs.log.import.visitors.download.directory";

  /** AWS S3からダウンロードして解凍したファイル名 */
  public static final String UNGZ_FILE_NAME_IMPORT_VISITORS =
      "fs.log.import.visitors.ungz.file.name";

  /** 詳細なし */
  public static final String NO_DETAIL = "詳細なし";

  /** Text AWS */
  public static final String TEXT_AWS = "AWS";

  /** default count */
  public static final Integer DEFAULT_COUNT = 0;

  /** FS API URL HEADER User-Agent */
  public static final String FS_API_URL_HEADER_USER_AGENT = "User-Agent";

  /** AWS S3からダウンロードする際のディレクトリ（差分） */
  public static final String S3_DIRECTORY_IDLINK_DIFF = "fs.log.import.idlink.diff.s3.directory";

  /** AWS S3からダウンロードする際のファイル名（差分） */
  public static final String S3_FILE_NAME_IDLINK_DIFF = "fs.log.import.idlink.diff.s3.file.name";

  /** AWS S3からダウンロードして解凍したファイル名（差分） */
  public static final String S3_UNGZ_FILE_NAME_IDLINK_DIFF =
      "fs.log.import.idlink.diff.ungz.file.name";

  /** AWS S3からダウンロードする際のディレクトリ（全量） */
  public static final String S3_DIRECTORY_IDLINK_FULL = "fs.log.import.idlink.full.s3.directory";

  /** AWS S3からダウンロードする際のファイル名（全量） */
  public static final String S3_FILE_NAME_IDLINK_FULL = "fs.log.import.idlink.full.s3.file.name";

  /** AWS S3からダウンロードして解凍したファイル名（全量） */
  public static final String S3_UNGZ_FILE_NAME_IDLINK_FULL =
      "fs.log.import.idlink.full.ungz.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_IDLINK = "fs.log.import.idlink.download.directory";

  /** Format folder S3 */
  public static final String FORMAT_FOLDER_S3 = "yyyy/mm/dd/";

  /** 実行モード（0:通常、1:ラストラン） */
  public static final String GENERAL = "0";
  public static final String LAST_RUN = "1";

  public static final String DIRECTORY_KEY_MESS = "処理対象ディレクトリ:";

  /** HTTP status start with 5 */
  public static final String HTTP_STATUS_START_NUMBER_ERROR = "5";

  /** HTTP status start with 4 */
  public static final String HTTP_STATUS_START_NUMBER_ERROR_4 = "4";

  /** リトライ回数超過 */
  public static final String OVER_RETRY_COUNT = "リトライ回数超過";

  /** リトライ回数 = */
  public static final String RETRY_COUNT = "リトライ回数 = ";

  /** 取得不可 */
  public static final String CAN_NOT_GET_KEY_WORD = "取得不可";

  /** HHTP FORMAT ERROR */
  public static final String HTTP_RESPONSE_BODY_ERROR = "「クーポンID:[%s], エラーメッセージ:[%s]";

  /** 認証エラー */
  public static final String AUTHORIZED_ERROR = "認証エラー";

  /** 再認証エラー(DB認証トークンで再認証) */
  public static final String RE_AUTHORIZED_DB_ERROR = "再認証エラー(DB認証トークンで再認証)";

  /** 再認証エラー(FS認証トークンで再認証) */
  public static final String RE_AUTHORIZED_FS_ERROR = "再認証エラー(FS認証トークンで再認証)";
  
  /** FANSHIPメンテナンス */
  public static final String FANSHIP_MANTAINENCE = "FANSHIPメンテナンス";

  /** リクエストが制限されています。 */
  public static final String RESTRICTED_REQUEST = "リクエストが制限されています。";

  /** サーバーエラー */
  public static final String SERVER_ERROR = "サーバーエラー";

  /** クライアントエラー */
  public static final String CLIENT_ERROR = "クライアントエラー";

  /** その他エラー */
  public static final String OTHERS_ERROR = "その他エラー";

  /** タイムアウト */
  public static final String TIMEOUT = "タイムアウト";

  /** HTTP method PATCH */
  public static final String HTTP_METHOD_PATCH = "PATCH";

  /** Abnormal key word */
  public static final String ABNORMAL = "abnormal";

  /** 認証ヘッダ(X-POPINFO-MAPI-TOKEN) */
  public static final String X_POPINFO_MAPI_TOKEN = "X-POPINFO-MAPI-TOKEN";

  /** 認証ヘッダ(AUTHORIZATION) */
  public static final String AUTHORIZATION = "Authorization";

  /** 認証ヘッダ値フォーマット */
  public static final String AUTHORIZATION_POPINFOLOGIN_FORMAT = "PopinfoLogin auth=%s";

  /** Format flat date */
  public static final String FLAT_FORMAT_DATE = "yyyyMMdd";

  /** regex check date format yyyyMMdd */
  public static final String REGEX_CHECK_FLAT_DATE = "^\\d{4}\\d{2}\\d{2}$";

  /** replace comma */
  public static final String COMMA = ",";

  /** replace fs segment upload history for message */
  public static final String MSG_FS_SEGMENT_UPLOAD_HISTORY_NO_RECORD = "FSセグメント連携履歴テーブル取得";

  /** replace fs segment upload history id for message */
  public static final String MSG_FS_SEGMENT_UPLOAD_HISTORY_ID = "（FSセグメント連携履歴ID ＝ ";

  /** replace fs segment ID proccess for message */
  public static final String MSG_REPLACE_SEGMENT_ID_PROCCESS = "ID一括変換処理, ";

  /** replace name file for message */
  public static final String MSG_REPLACE_NAME = "ファイル名:";

  /** string to replace in message */
  public static final String BRACKET = "（";

  /** replace segment id */
  public static final String REPLACE_SEGMENT_ID = "[【FSセグメント連携履歴テーブル】.「FSセグメント連携履歴ID」]";

  /** string to replace in message */
  public static final String REPLACE_DATE = "[yyyymmdd_hh24miss]";

  /** string to replace in message */
  public static final String REPLACE_DATE_SlASH = "yyyy/mm/dd";

  /** string to replace in message */
  public static final String CSV_EXTENSION = ".csv";

  /** replace name file for message */
  public static final String MSG_AWS_NAME = "AWS:";

  /** slash for file path */
  public static final String SLASH = "/";
  /** table name message */
  public static final String MESSAGE_TABLE_NAME = "テーブル名：";

  /** date format */
  public static final String DATE_FORMAT_YYYYMMDD_SlASH = "yyyy/MM/dd";

  /** システム日付をyyyymmdd_hh24miss形式 */
  public static final String DATA_FORMAT_YYYYMMDD_HHMMSS = "yyyyMMdd_HHmmss";

  /** セグメント一覧取得APIのURL */
  public static final String FS_SEGMENT_GET_API_URL = "fs.segment.get.api.url";

  /** FS API 失敗時のAPI実行リトライ回数 */
  public static final String FS_SEGMENT_GET_BATCH_RETRY_COUNT = "fs.segment.get.batch.retry.count";

  /** FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒) */
  public static final String FS_SEGMENT_GET_RETRY_SLEEP_TIME = "fs.segment.get.retry.sleep.time";

  /** FS API発行時のタイムアウト期間(秒) */
  public static final String FS_SEGMENT_GET_TIMEOUT_DURATION = "fs.segment.get.timeout.duration";

  /** format export mode log */
  public static final String FORMAT_EXPORT_MODE_LOG = "%s：%s";

  /** text 実行モード */
  public static final String EXECUTION_MODE = "実行モード";

  /** text 実行日付 */
  public static final String EXECUTION_DATE = "実行日付";

  /** text 差分/全件モード指定 */
  public static final String MODE_SPECIFICATION = "差分/全件モード指定";

  /** comma full size */
  public static final String COMMA_FULL_SIZE = "，";
  /** string to replace in message */
  public static final String DOUBLE_QOUTES = "\"";
  
  /** japan charset */
  public static final String CHARSET_SHIFT_JIS = "SHIFT-JIS";
  
  /** csv upload msg */
  public static final String UPLOAD_CSV_MSG = "CSVアップロード";
  
  /** all csv upload msg */
  public static final String UPLOAD_ALL_CSV_MSG = "全てのCSVファイルのアップロード";
  
  /** 【環境変数】FANSHIPの統合API URL */
  public static final String ENV_FS_INTEGRATION_URL = "FS_INTEGRATION_URL";

  /** 【環境変数】FANSHIPのリバースプロキシ URL */
  public static final String ENV_FS_REVERSE_PROXY_URL = "FS_REVERSE_PROXY_URL";

  /** 【環境変数】FANSHIPのエンドユーザ用API URL */
  public static final String ENV_FS_END_USER_API_URL = "FS_END_USER_API_URL";

  /** 【環境変数】FANSHIPのログインAPIのユーザID */
  public static final String ENV_FS_LOGIN_API_USER_ID = "FS_LOGIN_API_USER_ID";

  /** 【環境変数】FANSHIPのアプリスキーム */
  public static final String ENV_FS_APP_SCHEME = "FS_APP_SCHEME";

  /** 【環境変数】CUSTOMER_ID */
  public static final String ENV_FS_CUSTOMER_ID = "FS_CUSTOMER_ID";

  /** 【環境変数】FANSHIPの擬似ログイン（ID Link）時のSID */
  public static final String ENV_FS_SID = "FS_SID";

  /** 【環境変数】AWS S3からFSログをダウンロードする際のバケット名 */
  public static final String ENV_FS_LOG_IMPORT_S3_BUCKET_NAME = "FS_LOG_IMPORT_S3_BUCKET_NAME";

  /** 【環境変数】AWS S3にID一括変換リストをアップロードする際のバケット名 */
  public static final String ENV_SEGMENT_UPLOAD_S3_BUCKET_NAME = "SEGMENT_UPLOAD_S3_BUCKET_NAME";

  /** 【環境変数】CMS画像登録APIのデバッグモード */
  public static final String ENV_CMS_DEBUG_MODE = "CMS_DEBUG_MODE";

  /** 【環境変数】CMS画像登録APIのURI */
  public static final String ENV_CMS_URI = "CMS_URI";

  /** 【環境変数】CMS画像登録APIのURI（Stub） */
  public static final String ENV_CMS_URI_STUB = "CMS_URI_STUB";

  /** 【環境変数】クライアント用のキーストアパス */
  public static final String ENV_FS_KEYSTORE_PATH = "FS_KEYSTORE_PATH";

  /** 【環境変数】キーストアパスワード */
  public static final String ENV_FS_KEYSTORE_PSWD = "FS_KEYSTORE_PSWD";

  /** 【環境変数】FANSHIPの統合API KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
  public static final String ENV_FS_INTEGRATION_KEYSTORE_FLAG = "FS_INTEGRATION_KEYSTORE_FLAG";

  /** 【環境変数】FANSHIPのリバースプロキシAPI KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
  public static final String ENV_FS_REVERSE_PROXY_KEYSTORE_FLAG = "FS_REVERSE_PROXY_KEYSTORE_FLAG";

  /** 【環境変数】FANSHIPのエンドユーザ用API KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
  public static final String ENV_FS_END_USER_API_KEYSTORE_FLAG = "FS_END_USER_API_KEYSTORE_FLAG";
  
  /** utf-8 charset*/
  public static final String CHARSET_UTF_8 = "UTF-8";
}
