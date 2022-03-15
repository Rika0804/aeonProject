package jp.co.aeoncredit.coupon.batch.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * バッチのファイルユーティリティクラス
 *  
 */
public class BatchFileHandler {

	Logger log = null;
	String jobid = "";
	Boolean result = false;
	
	/**
	 * コンストラクタ
	 * @param jobid ジョブID
	 */
	public BatchFileHandler(String jobid){
		log = LoggerFactory.getInstance().getLogger(this);
		this.jobid = jobid;
	}
	
	/**
	 * ファイルをコピー(バックアップ)するメソッド。
	 * @param srcDir コピー元ファイル名
	 * @param destDir コピー先ファイル名
	 * @return result 処理結果
	 */
	public Boolean copyFile(String srcDir, String destDir){
		
		result = existFile(srcDir);

		if(result) {
	        try { 	
	            Path srcPath = Paths.get(srcDir);
	            Path destPath = Paths.get(destDir);
	            Files.copy(srcPath,destPath);
	    		log.info(new BatchLogger(this.jobid).createMsg("", "ファイルをコピーしました。：" + srcDir + " → " + destDir));
	            result = true;  
	        } catch (IOException e) {
	        	log.error(new BatchLogger(this.jobid).createMsg("", "ファイルのコピーに失敗しました。：" + srcDir + " → " + destDir));
	            e.printStackTrace();
	            result = false;  
	        } 
		} else {
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルのコピーに失敗しました。コピー元ディレクトリが不正です。:" + srcDir));
			result = false;
		}
		
		return result;
		
	}

	/**
	 * ファイルを移動するメソッド。
	 * @param srcDir 移動元ファイル名
	 * @param destDir 移動先ファイル名
	 * @return result 処理結果
	 */
	public Boolean moveFile(String srcDir, String destDir){
		
		result = existFile(srcDir);
		
		if(result) {
	        try { 	
	            Path srcFile = Paths.get(srcDir);
	            Path destFile = Paths.get(destDir);
		        Files.move(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
	        } catch (IOException e) {
	        	log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの移動に失敗しました。：" + srcDir + " → " + destDir));
	            e.printStackTrace();
	            result = false;  
	        } 
	        //srcFile.renameTo(destFile);
	        result = existFile(destDir);
	        
	        if(result) {
	        	log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを移動しました。：" + srcDir + " → " + destDir));
		        result = true;
	        } else {
	        	log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの移動に失敗しました。：" + srcDir + " → " + destDir));
				result = false;
	        }
			
		} else {
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの移動に失敗しました。移動元ディレクトリが不正です。:" + srcDir));
			result = false;
		}
		
        return result;
        
	}

	/**
	 * ファイルを削除するメソッド。
	 * @param srcDir 削除対象ファイル名
	 * @return result 処理結果
	 * 
	 */
	public Boolean deleteFile(String srcDir){
		
	    result = existFile(srcDir);

	    if (result){
	    	
	    	File file = new File(srcDir);
	    	
	    	if (file.delete()){
	    		log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを削除しました。：" + srcDir));
	    		result = true;
	    	} else {
	    		log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの削除に失敗しました。：" + srcDir));
				result = false;
	    	}
	    	
	    }
        
        return result;
	}
	
	/**
	 * ファイルやディレクトリの存在を確認するメソッド。
	 * @param src 対象ファイル/ディレクトリ名
	 * @return result 処理結果
	 * 
	 */
	public Boolean existFile(String src){
		
		result = false;
		
	    File file = new File(src);

	    if (file.exists()){
	    	result = true;	
//	    } else {	
//	    	log.error(new BatchLogger(this.jobid).createMsg("", "ファイルまたはディレクトリが存在しません。：" + src));
	    }
        
        return result;
        
	}
	
	/**
	 * ディレクトリかどうかを確認するメソッド。
	 * @param path ディレクトリ
	 * @return result 処理結果
	 * 
	 */
	public Boolean isDirectory(String path){
		
		result = false;
		
	    File file = new File(path);

	    if (file.isDirectory()){
	    	result = true;	
	    } else {
	    	if(existFile(path)) {
	    		//log.error(new BatchLogger(this.jobid).createMsg("", "ディレクトリが指定されていません。：" + path));
	    		result = false;
	    	}
	    }
        
        return result;
        
	}
	
	/**
	 * ファイルのリストを取得するメソッド。
	 * @param path 読込対象ディレクトリ
	 * @return fileList ファイルのリスト
	 * 
	 */
	public List<File> getFileList(String path) {
		
		List<File> fileList = new ArrayList<>();

		if(isDirectory(path)){
			
			if(existFile(path)) {
				
				File file = new File(path);
				
				File[] list = file.listFiles();
				for(File f : list) {
					if(f.isFile() && !f.isHidden()){
						fileList.add(f);
					}
				}
				
				log.info(new BatchLogger(this.jobid).createMsg("", "ファイルのリストを取得しました。ディレクトリ：" + path));
				
			}else {
				log.error(new BatchLogger(this.jobid).createMsg("", "ファイルのリスト取得に失敗しました。読込ディレクトリが不正です。：" + path));
			}
			
		}else {
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルのリスト取得に失敗しました。読込ディレクトリが不正です。：" + path));
		}

		return fileList;
		
	}
	
	/**
	 * フォルダのリストを取得するメソッド。
	 * @param path 読込対象ディレクトリ
	 * @return folderList フォルダのリスト
	 * 
	 */
	public List<String> getFolderList(String path) {

		List<String> folderList = new ArrayList<>();
		
		if(isDirectory(path)){
			
			if(existFile(path)) {
				
				File file = new File(path);
				
				File[] list = file.listFiles();
				for(File f : list) {
					if(f.isDirectory()){
						folderList.add(f.getAbsolutePath());
					}
				}
				
				log.info(new BatchLogger(this.jobid).createMsg("", "フォルダのリストを取得しました。ディレクトリ：" + path));
				
			}else {
				log.error(new BatchLogger(this.jobid).createMsg("", "フォルダのリスト取得に失敗しました。読込ディレクトリが不正です。：" + path));
			}
			
		}else {
			log.error(new BatchLogger(this.jobid).createMsg("", "フォルダのリスト取得に失敗しました。読込ディレクトリが不正です。：" + path));
		}		
		
		return folderList;
	}
	
	/**
	 * CSVファイルを読み込むメソッド。
	 * @param csv 読込対象ファイル名
	 * @param hasHeader ヘッダー項目の有無
	 * @return dataMap Map形式の読込データ：{行数,{項目名,値}}
	 * 
	 */
	public Map<Integer, Map<String,String>> loadFromCSVFile(String csv, Boolean hasHeader){
		return loadFromCSVFile(csv, hasHeader, Charset.defaultCharset());
	}
	
	/**
	 * CSVファイルを読み込むメソッド。
	 * @param csv 読込対象ファイル名
	 * @param hasHeader ヘッダー項目の有無
	 * @param cs 文字コード
	 * @return dataMap Map形式の読込データ：{行数,{項目名,値}}
	 * 
	 */
	public Map<Integer, Map<String,String>> loadFromCSVFile(String csv, Boolean hasHeader, Charset cs){
		
		Map<Integer, Map<String,String>> dataMap = new HashMap<>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv), cs))) {
		
			CSVParser parser = null;
			
			if(hasHeader) {
				// ヘッダーあり
			    parser = CSVFormat
			    		.EXCEL
			    		.withIgnoreEmptyLines(true)
				        .withFirstRecordAsHeader() 
			    		.withIgnoreSurroundingSpaces(true)
			    		.parse(br);
			} else {
				// ヘッダーなし
			    parser = CSVFormat
				        .EXCEL
				        .withIgnoreEmptyLines(true)
				        .withIgnoreSurroundingSpaces(true)	
				        .parse(br);
			}
			
			List<CSVRecord> recordList = parser.getRecords();
			
			for (CSVRecord record : recordList) {
				
				Map<String,String> columnData = new HashMap<>();
		        if(!record.toMap().isEmpty()) {
		        	// ヘッダーあり
			        for(Entry<String,String> entry : record.toMap().entrySet()) {
			        	 columnData.put(entry.getKey(), entry.getValue());
			        }
		        }else {
		        	// ヘッダーなし
		        	for(int i = 0; i < record.size(); i++) {
		        		String header = "col" + String.valueOf(i+1);
		        		columnData.put(header, record.get(i));
		        	}
		        }
		        
		        dataMap.put((int)record.getRecordNumber(), columnData);
		        
		    }
	
		    log.info(new BatchLogger(this.jobid).createMsg("", "ファイルをロードしました。：" + csv));
			    
		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルのロードに失敗しました。：" + csv), e);
			throw new RuntimeException(e);
			
		} 

		return dataMap;
		
	}
	
	/**
	 * CSVファイルヘッダーを読み込むメソッド。
	 * @param csv 読込対象ファイル名
	 * @param hasHeader ヘッダー項目の有無
	 * @return fileHeader ヘッダー項目
	 * 
	 */
	public String[] loadHeaderFromCSVFile(String csv, Boolean hasHeader){

		String[] fileHeader = null;
		List<String> list = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv)))) {
		
			CSVParser parser = null;		
				
		    parser = CSVFormat
		    		.EXCEL
		    		.withIgnoreEmptyLines(true)
		    		.withIgnoreSurroundingSpaces(true)
		    		.parse(br);
			
			List<CSVRecord> recordList = parser.getRecords();
			
			for (CSVRecord record : recordList) {
				if(record.getRecordNumber() == 1) {
					for(int i = 0; i < record.size(); i++) {
						int colNum = i+1;
						if(hasHeader) {
							list.add(record.get(i));
						}else {
							list.add("col"+ String.valueOf(colNum));
						}
					}	
				}
			}
	 
	        fileHeader = list.toArray(new String[list.size()]);
	
		    log.info(new BatchLogger(this.jobid).createMsg("", "ファイルヘッダーをロードしました。：" + csv));
			    
		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルヘッダーのロードに失敗しました。：" + csv));
			e.printStackTrace();
			throw new RuntimeException(e);
			
		}
		
		return fileHeader;
		
	}

	/**
	 * CSVファイルヘッダーの列番号を取得するメソッド。
	 * @param fileHeader ヘッダー項目
	 * @return columnNumMap ヘッダー項目と列番号のマップ
	 * 
	 */
	public Map<String,Integer> getColumnNumFromFileHeader(String[] fileHeader){
		
		Map<String,Integer> columnNumMap = new HashMap<>();
		
		for(int i= 0 ; i < fileHeader.length; i++) {
			columnNumMap.put(fileHeader[i], i+1);
		}
		
		return columnNumMap;
		
	}

	/**
	 * ヘッダー無しのCSVファイルを出力するメソッド。
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @return result 処理結果
	 * 
	 */	
	public Boolean outputCSVFile(String output, List<List<String>> dataList){
		
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)))) {

		    CSVPrinter printer = CSVFormat
		        .EXCEL
		        .print(bw);
		    
		    for(int record = 0; record < dataList.size(); record++) {
		    	for(int col = 0; col < dataList.get(record).size(); col++) {	
		    		printer.print(dataList.get(record).get(col));
					if(col == dataList.get(record).size()-1 && record != dataList.size()-1) {
						printer.println();
					}
		    	}
		    }
				
			log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを出力しました。：" + output));
			result = true;

		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの出力に失敗しました。：" + output));
			e.printStackTrace();
			result = false;
			
		}
		
        return result;
		
	}

	/**
	 * ヘッダー在りのCSVファイルを出力するメソッド。
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @param fileHeader ヘッダー項目
	 * @return result 処理結果
	 * 
	 */
	public Boolean outputCSVFile(String output, List<List<String>> dataList, String[] fileHeader){
		
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)))) {

		    CSVPrinter printer = CSVFormat
		        .EXCEL
		        .withHeader(fileHeader)
		        .print(bw);
		    
		    for(int record = 0; record < dataList.size(); record++) {
		    	for(int col = 0; col < dataList.get(record).size(); col++) {	
		    		printer.print(dataList.get(record).get(col));
					if(col == dataList.get(record).size()-1 && record != dataList.size()-1) {
						printer.println();
					}
		    	}
		    }
				
			log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを出力しました。：" + output));
			result = true;

		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの出力に失敗しました。：" + output));
			e.printStackTrace();
			result = false;
			
		}
		
        return result;
		
	}

	/**
	 * ヘッダー在り、指定した囲み文字で出力する
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @param fileHeader ヘッダー項目
	 * @return result 処理結果
	 * 
	 */
	public Boolean outputCSVFileWithQuote(String output, List<List<String>> dataList, String[] fileHeader, char quote){
		
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)))) {

		    CSVPrinter printer = CSVFormat
		        .EXCEL
		        .withQuote(quote)
		        .withQuoteMode(QuoteMode.ALL)
		        .withHeader(fileHeader)
		        .print(bw);
		    
		    for(int record = 0; record < dataList.size(); record++) {
		    	for(int col = 0; col < dataList.get(record).size(); col++) {	
		    		printer.print(dataList.get(record).get(col));
					if(col == dataList.get(record).size()-1 && record != dataList.size()-1) {
						printer.println();
					}
		    	}
		    }
				
			log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを出力しました。：" + output));
			result = true;

		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの出力に失敗しました。：" + output));
			e.printStackTrace();
			result = false;
			
		}
		
        return result;
		
	}
	
	/**
	 * ヘッダー在り、指定した囲み文字で出力する
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @param fileHeader ヘッダー項目
	 * @param quote 囲み文字
	 * @param charset 文字コード
	 * @return result 処理結果
	 * 
	 */
	public Boolean outputCSVFileWithQuote(String output, List<List<String>> dataList, String[] fileHeader, char quote, String charset){
		
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), charset))) {

		    CSVPrinter printer = CSVFormat
		        .EXCEL
		        .withQuote(quote)
		        .withQuoteMode(QuoteMode.ALL)
		        .withHeader(fileHeader)
		        .print(bw);
		    
		    for(int record = 0; record < dataList.size(); record++) {
		    	for(int col = 0; col < dataList.get(record).size(); col++) {	
		    		printer.print(dataList.get(record).get(col));
					if(col == dataList.get(record).size()-1 && record != dataList.size()-1) {
						printer.println();
					}
		    	}
		    }
				
			log.info(new BatchLogger(this.jobid).createMsg("", "ファイルを出力しました。：" + output));
			result = true;

		} catch (IOException e) {
			
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの出力に失敗しました。：" + output));
			e.printStackTrace();
			result = false;
			
		}
		
        return result;
		
	}


	/**
	 * 固定長ファイルを読み込むメソッド。
	 * @param src 読込対象ファイル名
	 * @param enc 文字コード
	 * @param CR_length: 改行のバイト数(改行なし：0、CR：1、CRLF：2)
	 * @param headerFiexedLengthList: ヘッダーの桁数リスト
	 * @param bodyFixedLengthList: ボディの桁数リスト
	 * @param footerFixedLengthList: フッターの桁数リスト
	 * @return dataList 読込データのリスト
	 * 
	 * */
	public List<List<String>> loadFromFixedLengthFile(String src, Charset enc, int CR_length, int[] headerFixedLengthList, int[] bodyFixedLengthList, int[] footerFixedLengthList) {
		
		List<List<String>> dataList = new ArrayList<>();
		
		int headerAllLength = 0;
		int bodyAllLength = 0;
		int footerAllLength = 0;
		
		boolean hasHeader = false;
		if(headerFixedLengthList != null && headerFixedLengthList.length != 0) {
			hasHeader = true;
		}	
		if(hasHeader) {
			for(int headerFieldLength : headerFixedLengthList) {
				headerAllLength += headerFieldLength;
			}
		}
		
		for(int bodyFieldLength : bodyFixedLengthList) {
			bodyAllLength += bodyFieldLength;
		}
		
		boolean hasFooter = false;
		if(footerFixedLengthList != null && footerFixedLengthList.length != 0) {
			hasFooter = true;
		}
		if(hasFooter) {	
			for(int footerFieldLength : footerFixedLengthList) {
				footerAllLength += footerFieldLength;
			}			
		}
		
		try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));){

		    byte[] data = bis.readAllBytes();
		    int allLength = data.length;
			
			int beginIndex = 0;
			int endIndex = 0;

			// ヘッダー部の読込
			if(hasHeader) {
				List<String> headerColumnData = new ArrayList<>();
				for(int headerFieldLength : headerFixedLengthList) {
					beginIndex = endIndex;
					endIndex = beginIndex + headerFieldLength;
				    byte[] field = Arrays.copyOfRange(data ,beginIndex, endIndex); 	    
					String inputData = new String(field, enc);
					headerColumnData.add(inputData);						
				}
				dataList.add(headerColumnData);
			}
			
			// ボディ部の読込
			int bodyNum = 0;
			int lineBreakNum = 0;
			List<String> footerColumnData = new ArrayList<>();
			
			if(CR_length == 0) {
				
				// 改行なしの場合
				bodyNum = (allLength - headerAllLength - footerAllLength) / bodyAllLength;
				
				for(int num = 0; num < bodyNum ; num++) {
					List<String> bodyColumnData = new ArrayList<>();
					for(int bodyFieldLength : bodyFixedLengthList) {
						beginIndex = endIndex;
						endIndex = beginIndex + bodyFieldLength;
					    byte[] field = Arrays.copyOfRange(data ,beginIndex, endIndex); 	    
						String bodyData = new String(field, enc);
						bodyColumnData.add(bodyData);						
					}
					dataList.add(bodyColumnData);
				}
				
			}else {
				
				// 改行ありの場合
				lineBreakNum = CR_length;
				
				if(hasHeader) {
					
					// ヘッダーありの場合
					if(hasFooter) {
						// フッターありの場合
						bodyNum = (allLength - headerAllLength - footerAllLength - lineBreakNum) / (bodyAllLength + lineBreakNum);
					}else {
						// フッターなしの場合
						bodyNum = (allLength - headerAllLength) / (bodyAllLength + lineBreakNum);
					}
					
				}else {
					
					// ヘッダーなしの場合
					if(hasFooter) {
						// フッターありの場合
						bodyNum = (allLength - footerAllLength) / (bodyAllLength + lineBreakNum);
					} else {
						// フッターなしの場合
						bodyNum = (allLength + lineBreakNum) / (bodyAllLength + lineBreakNum);;
					}
					
				}
	
				if(hasHeader) {
					endIndex = endIndex + lineBreakNum;
				}
				
				for(int num = 0; num < bodyNum ; num++) {
					List<String> bodyColumnData = new ArrayList<>();
					for(int columnNo = 0; columnNo < bodyFixedLengthList.length; columnNo++) {
								
						beginIndex = endIndex;
						endIndex = beginIndex + bodyFixedLengthList[columnNo];
					    byte[] field = Arrays.copyOfRange(data ,beginIndex, endIndex); 	    
						String bodyData = new String(field, enc);
						bodyColumnData.add(bodyData);
						
					}
					endIndex = endIndex + lineBreakNum;
					dataList.add(bodyColumnData);
				}
				
			}
			
			// フッター部の読込
			if(hasFooter) {	
				int footerStartIndex = allLength - footerAllLength - CR_length;
				for(int i = 0; i< footerFixedLengthList.length; i++) {
					if(i == 0) {
						beginIndex = footerStartIndex;
					}else {
						beginIndex = endIndex;
					}
					endIndex = beginIndex + footerFixedLengthList[i];
				    byte[] field = Arrays.copyOfRange(data ,beginIndex, endIndex); 	    
					String footerData = new String(field, enc);
					footerColumnData.add(footerData);						
				}
				dataList.add(footerColumnData);
			}
			
			log.info(new BatchLogger(this.jobid).createMsg("", "固定長ファイルをロードしました。：" + src));
		
		} catch ( IOException e ) {
			log.error(new BatchLogger(this.jobid).createMsg("", "固定長ファイルのロードに失敗しました。：" + src));
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return dataList;
	
	}
	public class FixedLengthFile  implements Closeable{
		OutputStream os;
		Charset enc;
		byte[] binSpace = null;
		byte[] lineBreak = null;
		int[] headerFixedLengthList;
		int[] bodyFixedLengthList;
		int[] footerFixedLengthList;
		public FixedLengthFile(String output, Charset enc, int CR_length,
				int[] headerFixedLengthList, 
				int[] bodyFixedLengthList, int[] footerFixedLengthList) 
			throws IOException {
			this.enc = enc;
			this.os = new BufferedOutputStream(new FileOutputStream(output));
			if(CR_length == 1) {
				this.lineBreak = "\n".getBytes(enc);
			} else if (CR_length == 2) {
				this.lineBreak = "\r\n".getBytes(enc);
			}
			this.headerFixedLengthList = headerFixedLengthList; 
			this.bodyFixedLengthList = bodyFixedLengthList;
			this.footerFixedLengthList = footerFixedLengthList;
			this.binSpace= " ".getBytes(enc);
		}
		@Override
		public void close() throws IOException {
			os.close();
		}
		public void writeLine(List<String> lineData, int[] fixedLength) throws IOException {
			for (int i = 0; i < fixedLength.length; i++) {
				String field = lineData.get(i);
				//指定された文字コードでバイト配列化
				byte[] binData = field.getBytes(enc);
				//バイト配列の長さ
				int fieldByteLength = binData.length;
				//固定長との差
				int spaceByteCount = fixedLength[i] - fieldByteLength;
				//実際のバイト配列のほうが大きかったら
				if (spaceByteCount < 0) {
					//負の値を足すので、差分を減らす(実際には指定されたバイト数に切り詰める)
					fieldByteLength = fieldByteLength + spaceByteCount;
					spaceByteCount = 0;
				}
				// データの出力
				this.os.write(binData, 0, fieldByteLength);
				
				// たりない場合、空白の出力
				for(int count = 0; count < spaceByteCount; count++) {
					this.os.write(binSpace);
				}
			}	
			if (lineBreak != null) {
				this.os.write(lineBreak);
			}
		}
		public void writeHeader(List<String> header) throws IOException {
			writeLine(header, headerFixedLengthList);
		}
		public void writeBodyList(List<List<String>> bodyList)  throws IOException {
			for (List<String> body: bodyList) {
				writeBody(body);
			}
		}
		public void writeBody(List<String> body)  throws IOException {
			writeLine(body, bodyFixedLengthList);
		}
		public void writeFooter(List<String> footer)  throws IOException {
			writeLine(footer, footerFixedLengthList);
		}
	}
	public FixedLengthFile openFixedLengthFile(String output, Charset enc, int CR_length, int[] headerFixedLengthList, int[] bodyFixedLengthList, int[] footerFixedLengthList) throws IOException {
		FixedLengthFile file = new FixedLengthFile (output, enc, CR_length, headerFixedLengthList, bodyFixedLengthList, footerFixedLengthList);
		return file;
	}
	/**
	 * 固定長ファイルを出力するメソッド。
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @param enc 文字コード
	 * @param CR_length: 改行のバイト数(改行なし：0、CR：1、CRLF：2)
	 * @param headerFiexedLengthList: ヘッダーの桁数リスト
	 * @param bodyFixedLengthList: ボディの桁数リスト
	 * @param footerFixedLengthList: フッターの桁数リスト
	 * @return 処理結果
	 * 
	 * */
	public Boolean outputFixedLengthFile(String output, List<List<String>> dataList, Charset enc, int CR_length, int[] headerFixedLengthList, int[] bodyFixedLengthList, int[] footerFixedLengthList) {
		
		boolean hasHeader = false;
		if(headerFixedLengthList != null && headerFixedLengthList.length != 0) {
			hasHeader = true;
		}
		
		boolean hasFooter = false;
		if(footerFixedLengthList != null && footerFixedLengthList.length != 0) {
			hasFooter = true;
		}
					
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output))) {

			// 空白の設定
			String space = " ";
			byte[] binSpace= space.getBytes(enc);
			
			// 改行の設定
			String lineBreak = "";
			if(CR_length == 1) {
				lineBreak = "\r";
			} else if (CR_length == 2) {
				lineBreak = "\r\n";
			}			
			
			for(int i = 0; i < dataList.size(); i++) {

				int spaceByteCount = 0;
				
				for(int j = 0 ; j < dataList.get(i).size(); j++) {
					
					String field = dataList.get(i).get(j);
					//指定された文字コードでバイト配列化
					byte[] binData = field.getBytes(enc);
					//バイト配列の長さ
					int fieldByteLength = binData.length;
					
					//指定された項目のサイズと、比較
					if(i == 0) {
						if(hasHeader) {
							spaceByteCount = headerFixedLengthList[j] - fieldByteLength;
							
						} else {
							spaceByteCount = bodyFixedLengthList[j] - fieldByteLength;
						}
					} else {
						if(hasFooter) {
							if (i == dataList.size() - 1) {
								spaceByteCount = footerFixedLengthList[j] - fieldByteLength;
							} else {
								spaceByteCount = bodyFixedLengthList[j] - fieldByteLength;
							}	
						} else {
							spaceByteCount = bodyFixedLengthList[j] - fieldByteLength;
						}
					}
					
					//実際のバイト配列のほうが大きかったら
					if (spaceByteCount < 0) {
						//負の値を足すので、差分を減らす(実際には指定されたバイト数になる)
						fieldByteLength = fieldByteLength + spaceByteCount;
						spaceByteCount = 0;
					}
					// データの出力
					bos.write(binData, 0, fieldByteLength);
					
					// たりない場合、空白の出力
					for(int count = 0; count < spaceByteCount; count++) {
						bos.write(binSpace);
					}
					
				}
				
				// 改行の出力
				if(CR_length != 0) {
					bos.write(lineBreak.getBytes(enc));
				}
				
			}
		
			log.info(new BatchLogger(this.jobid).createMsg("", "固定長ファイルを出力しました。：" + output));
			result = true;
	
		} catch ( IOException e ) {
			log.error(new BatchLogger(this.jobid).createMsg("", "固定長ファイルの出力に失敗しました。：" + output));
			e.printStackTrace();
			result = false;
		}
		
	return result;

	}
	
	/**
	 * 二つのファイルの中身が一致するかどうかチェックする
	 * @param fileA 比較対象ファイル名
	 * @param fileB 比較対象ファイル名
	 * @return
	 */
	public Boolean compareFiles(String fileA, String fileB) {
	    // 引数チェック
	    if (fileA == null || fileB == null || fileA.length() == 0 || fileB.length() == 0) {
	    	return false;
	    }
	    File fA = new File(fileA);
	    File fB = new File(fileB);
	    
	    // ファイルが存在しない場合
	    if (!fA.exists() || !fB.exists()) {
	    	return false;
	    }
	    // 同じファイル名だったら中身も同じ
	    if (fileA.equals(fileB)) {
	    	return true;
	    }
	    // ファイルのサイズが違ったらfalse
        if( fA.length() != fB.length() ){
            return false;
        }
		try(FileInputStream fisA = new FileInputStream(fA);
			FileInputStream fisB = new FileInputStream(fB)) {
	        byte[] bufA = new byte[1024];
	        byte[] bufB = new byte[1024];
	        while (true) {
	        	//1024バイトづつ読み込む
	        	int lenA = fisA.read(bufA);
	        	int lenB = fisB.read(bufB);
	        	//読込サイズが違ったらfalse
	        	if (lenA != lenB) {
	        		return false;
	        	}
	        	// ファイルの終端に達していたらtrue
	        	if (lenA == -1) {
	        		return true;
	        	}
	        	// 配列比較
		        if (!Arrays.equals(bufA, bufB)) {
		        	//違ったらfalseを返す
		        	return false;
		        }
	        }
	    } catch (IOException e) {
			log.error(new BatchLogger(this.jobid).createMsg("", "ファイルの比較に失敗しました。：" + fileA + " == " + fileB));
			e.printStackTrace();
	    	return false;
	    }
	}
	
	/**
	 * Zip圧縮したファイルを作成します。
	 * @param sources 圧縮元のファイル名 
	 * @param zipFile 作成するZIPファイル名
	 * @param password 圧縮時のパスワード NULLの場合、パスワードによる暗号化をしません
	 * @return
	 */
	public Boolean createZipFile(List<String> sources, String zipFile, String password) {
        try (ZipOutputStream zos = createZipOutputStream(zipFile, password);) {	  
            ZipParameters baseParams = new ZipParameters();
            if (password == null) {
    	        baseParams.setEncryptFiles(false);
            } else {
    	        baseParams.setEncryptFiles(true);
    	        baseParams.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
            }
	        for (String src : sources) {
	        	Path srcPath = Paths.get(src);
	            // ソースファイルのパスを正規化済の絶対パスに変換
	            srcPath = srcPath.toAbsolutePath().normalize();
	            Path parentPath = srcPath.getParent();
                // ソースファイルのパスから、圧縮対象ファイルの相対パスを生成
                Path relPath = parentPath.relativize(srcPath);
 
                // 対象ファイル用のパラメータ定義
                ZipParameters targetParams = new ZipParameters(baseParams);
                targetParams.setFileNameInZip(relPath.toString());
 
                // ZIPストリームにファイルを出力
                zos.putNextEntry(targetParams);
                try (InputStream is = new FileInputStream(srcPath.toFile())) {
                    int readSize;
                    byte[] buf = new byte[1024];
                    while ((readSize = is.read(buf)) > 0) {
                        zos.write(buf, 0, readSize);
                    }
                }
                zos.closeEntry();
	        }
//	        zos.close();
	        
	        // 権限設定
	        if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
	        	log.info("zipファイルの権限設定を行います：" + zipFile);
	            Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.GROUP_READ);
				perms.add(PosixFilePermission.GROUP_WRITE);
				perms.add(PosixFilePermission.OTHERS_READ);
				Files.setPosixFilePermissions(Paths.get(zipFile), perms);
				log.info("zipファイルの権限を設定しました：" + zipFile);
	        }
			
	        return true;
        } catch (IOException e) {
			log.error(new BatchLogger(this.jobid).createMsg("", "Zipファイルの作成に失敗しました。：" + zipFile));
			e.printStackTrace();
	    	return false;
        }
	}
	private ZipOutputStream createZipOutputStream(String zipFile, String password) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = null;
        ZipParameters baseParams = new ZipParameters();
        if (password == null) {
	        zos = new ZipOutputStream(fos);
	        baseParams.setEncryptFiles(false);
        } else {
	        zos = new ZipOutputStream(fos, password.toCharArray());
	        baseParams.setEncryptFiles(true);
	        baseParams.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        }
        return zos;
	}

}