package jp.co.aeoncredit.coupon.batch.common;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.jp.awag.common.logic.ServiceDBException;
import com.ibm.jp.awag.common.logic.ServiceDBException.DBErrorType;
import com.ibm.jp.awag.common.util.Logger;

/**
 * バッチ用DBアクセスクラスのSuperクラス
 *  
 */
public class BatchDBAccessBase extends BatchMainBase {
	
	@Inject
	protected UserTransaction userTransaction;
	
	@PersistenceContext
	protected EntityManager em;
	
	protected Logger log = getLogger();
	
	@Override
	public String process() throws Exception {
		return null;
	}
	
	/**
	 * トランザクションを開始するメソッド。
	 * @param jobid ジョブID
	 */
	public void transactionBegin(String jobid) {
		
		try {
			UserTransaction txn = userTransaction;
			txn.begin();
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションを開始しました。"));
		}catch (Exception e) {
			log.error(new BatchLogger(jobid).createMsg("", "トランザクションの開始に失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}

	/**
	 * トランザクションをコミットするメソッド。
	 * @param jobid ジョブID
	 */
	public void transactionCommit(String jobid) {

		try {
			UserTransaction txn = userTransaction;
			txn.commit();
			log.debug(new BatchLogger(jobid).createMsg("", "トランザクションをコミットしました。"));
		}catch (Exception e) {
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションのコミットに失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}
	
	/**
	 * トランザクションをロールバックするメソッド。
	 * @param jobid ジョブID
	 */
	public void transactionRollback(String jobid) {

		try {
			UserTransaction txn = userTransaction;
			txn.rollback();
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションをロールバックしました。"));
		}catch (Exception e) {
			log.error(new BatchLogger(jobid).createMsg("", "トランザクションのロールバックに失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}
	
	/**
	 * SELECTのSQL文（パラメータ有り）を実行するメソッド。
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @param paramMap パラメータのマップ。keyの先頭文字を$とすると、SQLの中の文字列を置換する。
	 * @return resultList 検索結果リスト
	 * 
	 */
	public List<Object[]> sqlSelect(String jobid, String sqlid, Map<String,Object> paramMap) {
	    
		List<Object[]> resultList = execute(jobid,sqlid,paramMap);
		
	    return resultList;
	
	}

	/**
	 * SELECTのSQL文（パラメータ有り）を実行するメソッド。
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @param paramMap パラメータのマップ。keyの先頭文字を$とすると、SQLの中の文字列を置換する。
	 * @return resultList 検索結果リスト
	 * 
	 */
	public List<Object[]> sqlSelect(String jobid, String sqlid, Map<String,Object> paramMap, int startPos, int maxResults) {
	    
		List<Object[]> resultList = execute(jobid,sqlid,paramMap, startPos, maxResults);
		
	    return resultList;
	
	}

	/**
	 * SELECTのSQL文（パラメータ無し）を実行するメソッド。
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @return resultList 検索結果リスト
	 * 
	 */
	public List<Object[]> sqlSelect(String jobid, String sqlid) {
		
		List<Object[]> resultList = execute(jobid,sqlid,null);
		
	    return resultList;
		
	}
	
	private List<Object[]> execute(String jobid, String sqlid, Map<String, Object> paramMap) {
		
		Query q = setQuery(jobid, sqlid, paramMap);
		List<Object[]> resultList = q.getResultList();
		
		return resultList;
	}

	private List<Object[]> execute(String jobid, String sqlid, Map<String, Object> paramMap, int startPos, int maxResult) {
		//前回の呼び出しのキャッシュをクリアする。
	    this.em.clear();
		Query q = setQuery(jobid, sqlid, paramMap);
		List<Object[]> resultList = q.setFirstResult(startPos).setMaxResults(maxResult).getResultList();
	    String msg = "SQLを実行しました。(SQLID:" + sqlid + ")";
	    log.debug(new BatchLogger(jobid).createMsg("",msg));
		
		return resultList;
	}
	
	/**
	 * SELECTのSQL文（パラメータ有り）を実行するメソッド。※戻り値の型パラメータを指定しない
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @param paramMap パラメータのマップ。keyの先頭文字を$とすると、SQLの中の文字列を置換する。
	 * @return result 検索結果
	 * 
	 */
	public List sqlSelectAsUntypedList(String jobid, String sqlid, Map<String,Object> paramMap) {
		
		List result = executeAsUntypedList(jobid,sqlid,paramMap);
		
	    return result;
		
	}
	
	/**
	 * SELECTのSQL文（パラメータ無し）を実行するメソッド。※戻り値の型パラメータを指定しない
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @return result 検索結果
	 * 
	 */
	public List sqlSelectAsUntypedList(String jobid, String sqlid) {
		
		List result = executeAsUntypedList(jobid,sqlid,null);
		
	    return result;
		
	}	
	
	private List executeAsUntypedList(String jobid, String sqlid, Map<String, Object> paramMap) {
		
		Query q = setQuery(jobid, sqlid, paramMap);
		List result = q.getResultList();
	    
	    String msg = "SQLを実行しました。(SQLID:" + sqlid + ")";
	    log.debug(new BatchLogger(jobid).createMsg("",msg));
		
		return result;
	}
	
	/**
	 * SELECT以外のSQL文（パラメータ有り）を実行するメソッド。
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @param paramMap パラメータのマップ。keyの先頭文字を$とすると、SQLの中の文字列を置換する。
	 * @return number 更新行数
	 * 
	 */
	public int sqlExecute(String jobid, String sqlid, Map<String,Object> paramMap) {
		
	    Query q = setQuery(jobid, sqlid, paramMap);
	    
	    int number = q.executeUpdate();
	    
	    String msg = "SQLを実行しました。(SQLID:" + sqlid + ")";
	    log.debug(new BatchLogger(jobid).createMsg("",msg));

	    return number;
	}

	/**
	 * SELECT以外のSQL文（パラメータ無し）を実行するメソッド。
	 * @param jobid ジョブID
	 * @param sqlid SQLID
	 * @return number 更新行数
	 * 
	 */
	public int sqlExecute(String jobid, String sqlid) {
		
	    Query q = setQuery(jobid, sqlid, null);
	    
	    int number = q.executeUpdate();
	    
	    String msg = "SQLを実行しました。(SQLID:" + sqlid + ")";
	    log.debug(new BatchLogger(jobid).createMsg("",msg));

	    return number;
	}
	
	private Query setQuery(String jobid, String sqlid, Map<String, Object> paramMap) {
		
		BatchConfigfileLoader conf = getConfigLoader(jobid);
		Properties sqlconf  = conf.readPropertyFile(jobid + "_sql");
		String sql = sqlconf.getProperty(sqlid);

		if(paramMap != null) {
		    for (Entry<String, Object> entry : paramMap.entrySet()) {
		    	if (entry.getKey().startsWith(("$"))){
		    		sql = sql.replace(entry.getKey(), entry.getValue().toString());
		    	}
			}
	    }		
		
	    Query q = this.em.createNativeQuery(sql);
	    
	    if(paramMap != null) {
		    for (Entry<String, Object> entry : paramMap.entrySet()) {
		    	if (!entry.getKey().startsWith(("$"))){
		    		q.setParameter(entry.getKey(), entry.getValue());
		    	}
			}
	    }		
		return q;
		
	}
	

	
    @Resource(lookup = "java:/defaultDS")
    private DataSource dataSource;
	
    private Connection con;
    
	/**
	 * トランザクションを開始するメソッド。
	 * @param jobid ジョブID
	 */
	public Connection transactionBeginConnection(String jobid) {
		
		try {
			getDbConnection(jobid);
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションを開始しました。"));
			return con;
		}catch (Exception e) {
			log.error(new BatchLogger(jobid).createMsg("", "トランザクションの開始に失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}
	/**
	 * トランザクションをコミットするメソッド。
	 * @param jobid ジョブID
	 */
	public void transactionCommitConnection(String jobid) {

		try {
			con.commit();
			log.debug(new BatchLogger(jobid).createMsg("", "トランザクションをコミットしました。"));
		}catch (Exception e) {
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションのコミットに失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}
	
	/**
	 * トランザクションをロールバックするメソッド。
	 * @param jobid ジョブID
	 */
	public void transactionRollbackConnection(String jobid) {

		try {
			con.rollback();
			log.info(new BatchLogger(jobid).createMsg("", "トランザクションをロールバックしました。"));
		}catch (Exception e) {
			log.error(new BatchLogger(jobid).createMsg("", "トランザクションのロールバックに失敗しました。"));
            throw new RuntimeException(e);
        }
		
	}
	/**
	 * Connection を取得するメソッド
	 * @param jobid ジョブID
	 */
	public Connection getDbConnection(String jobid) {
		if (con != null) {
			return con;
		}
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			log.info(new BatchLogger(jobid).createMsg("", "Db Connectionを取得しました。"));
			return con;
		}catch (SQLException e) {
			log.error(new BatchLogger(jobid).createMsg("", "Db Connectionの取得に失敗しました。"));
            throw new RuntimeException(e);
        }
	}
	/**
	 * Connectionをクローズするメソッド
	 * @param jobid
	 */
	public void closeConnection(String jobid) {
		try {
			if (con == null) {
				return ;
			}
			if (con.isClosed()) {
				return ;
			}
			con.close();
			con = null;
			log.info(new BatchLogger(jobid).createMsg("", "Connectionをcloseしました。"));
		} catch (SQLException e) {
			con = null;
			log.error(new BatchLogger(jobid).createMsg("", "Connectionのcloseに失敗しました。"));
            throw new RuntimeException(e);
		}
	}
	public PreparedStatement prepareStatement(String jobid, String sqlid) throws SQLException {
		BatchConfigfileLoader conf = getConfigLoader(jobid);
		Properties sqlconf  = conf.readPropertyFile(jobid + "_sql");
		String sql = sqlconf.getProperty(sqlid);
		PreparedStatement ps = con.prepareStatement(sql);
		return ps;
	}
	protected ServiceDBException createServiceDBException(DBErrorType dbErrorType, String errorCode, Exception cause) {	
		return new ServiceDBException(dbErrorType, errorCode, cause);
	}

	/**
	 * Close Quietly
	 * 
	 * @param resultSet         ResultSet
	 * @param preparedStatement PreparedStatement
	 * @throws SQLException
	 */
	public void closeQuietly(ResultSet resultSet, PreparedStatement preparedStatement) throws SQLException {
		if (resultSet != null) {
			resultSet.close();
		}
		if (preparedStatement != null) {
			preparedStatement.close();
		}
	}

}
