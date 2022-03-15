package jp.co.aeoncredit.coupon.batch.common;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import com.ibm.jp.awag.common.logic.ServiceDBException;
import com.ibm.jp.awag.common.logic.ServiceDBException.DBErrorType;
import com.ibm.jp.awag.common.util.Logger;

/**
 * バッチ用DBアクセスクラスのSuperクラス
 *  
 */
public class BatchDbConnectionBase extends BatchMainBase {
	
    @Resource(lookup = "java:/defaultDS")
    private DataSource dataSource;
	
    private Connection con;
    
	Logger log = getLogger();
	
	@Override
	public String process() throws Exception {
		return null;
	}
	/**
	 * トランザクションを開始するメソッド。
	 * @param jobid ジョブID
	 */
	public Connection transactionBegin(String jobid) {
		
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
	public void transactionCommit(String jobid) {

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
	public void transactionRollback(String jobid) {

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

}
