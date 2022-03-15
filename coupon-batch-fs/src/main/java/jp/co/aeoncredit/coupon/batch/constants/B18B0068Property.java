package jp.co.aeoncredit.coupon.batch.constants;

/**
 * B18B0068 Properties info enum
 * 
 * @author HungHM
 * @version 1.0
 */
public enum B18B0068Property {
  /** Batch B18B0073 */
  B18B0068("purpose.segment.upload.batch.input.directory",
      "purpose.segment.upload.batch.csv.output.directory",
      "purpose.segment.upload.batch.zip.output.directory",
      "purpose.segment.upload.batch.output.ok.filename",
      "purpose.segment.upload.batch.output.ng.filename",
      "purpose.segment.upload.batch.output.ok.ng.zip.filename",
      "purpose.segment.upload.batch.s3.objectKey");

  /** csv input Directory */
  private String csvInputDirectory;

  /** csv output Directory */
  private String csvOutputDirectory;

  /** zip output Directory */
  private String zipOutputDirectory;

  /** output ok filename */
  private String outputOkFileName;

  /** output ng filename */
  private String outputNgFileName;

  /** zip output filename */
  private String zipOkNgFileName;
  
  /** object key */
  private String objectKey;

  /**
   * B18B0068Property
   * 
   * @param csvInputDirectory csv input directory
   * @param csvOutputDirectory csv output directory
   */
  private B18B0068Property(String csvInputDirectory, String csvOutputDirectory,
      String zipOutputDirectory, String outputOkFileName, String outputNgFileName,
      String zipOkNgFileName, String objectKey) {
    this.csvInputDirectory = csvInputDirectory;
    this.csvOutputDirectory = csvOutputDirectory;
    this.zipOutputDirectory = zipOutputDirectory;
    this.outputOkFileName = outputOkFileName;
    this.outputNgFileName = outputNgFileName;
    this.zipOkNgFileName = zipOkNgFileName;
    this.objectKey = objectKey;
  }

  /**
   * Get csvInputDirectory
   * 
   * @return csvInputDirectory
   */
  public String getCsvInputDirectory() {
    return csvInputDirectory;
  }

  /**
   * Get csvOutputDirectory
   * 
   * @return csvOutputDirectory
   */
  public String getCsvOutputDirectory() {
    return csvOutputDirectory;
  }

  /**
   * Get zipOutputDirectory
   * 
   * @return zipOutputDirectory
   */
  public String getZipOutputDirectory() {
    return zipOutputDirectory;
  }

  /**
   * Get outputOkFileName
   * 
   * @return outputOkFileName
   */
  public String getOutputOkFileName() {
    return outputOkFileName;
  }


  /**
   * Get outputNgFileName
   * 
   * @return outputNgFileName
   */
  public String getOutputNgFileName() {
    return outputNgFileName;
  }

  /**
   * Get zipOkNgFileName
   * 
   * @return outputNgFileName
   */
  public String getZipOkNgFileName() {
    return zipOkNgFileName;
  }

  /**
   * Get objectKey
   * 
   * @return objectKey
   */
  public String getObjectKey() {
    return objectKey;
  }
}
