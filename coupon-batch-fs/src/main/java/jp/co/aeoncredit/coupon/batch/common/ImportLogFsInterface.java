/**
 * 
 */
package jp.co.aeoncredit.coupon.batch.common;

import java.util.List;

/**
 * Interface Import Log Fs Base
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public interface ImportLogFsInterface {
  /**
   * Map content file CSV to Entity
   * 
   * @param <T>
   * @return result
   */
  public <T> List<T> mapContentCSVToEntity();

  /**
   * Process insert Fs data
   */
  public void processInsertFSData();
}
