package jp.co.aeoncredit.coupon.batch.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

/**
 * バッチの設定ファイル読込ユーティリティクラス
 *  
 */
public class BatchConfigfileLoader  {
	
	Logger log = null;
	String jobid = "";
	Map<String, Properties> cache = new HashMap<String, Properties>();
	
	/**
	 * コンストラクタ
	 * @param jobid ジョブID
	 */
	public BatchConfigfileLoader(String jobid){
		log = LoggerFactory.getInstance().getLogger(this);
		this.jobid = jobid;
	}

	/**
	 * プロパティ値を読み込むメソッド。
	 * @param filename ファイル名
	 * @return prop プロパティ
	 */
	public Properties readPropertyFile(String filename){

		if (cache.containsKey(filename)) {
			return cache.get(filename);
		}
		Properties prop = new Properties();
		String file = filename + ".properties";
        
        try (InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(file)){
        
            if (inStream == null) {
            	log.error(new BatchLogger(this.jobid).createMsg("", "設定ファイルが存在しません。：" + file));
                throw new IllegalArgumentException();
            }
            prop.load(new InputStreamReader(inStream, "UTF-8"));
            
            log.info(new BatchLogger(this.jobid).createMsg("", "設定ファイルを読み込みました。：" + file));    
            
        } catch (IOException e) {
        	
        	log.error(new BatchLogger(this.jobid).createMsg("", "設定ファイルの読み込みに失敗しました。：" + file));
            e.printStackTrace();
            
        }
		cache.put(filename, prop);
		return prop;
		
	}
	
}