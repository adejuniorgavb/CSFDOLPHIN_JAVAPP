package com.carrefour.dolphin.ingestioncontrol;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.carrefour.dolphin.utils.PropertiesCSF;
import com.carrefour.dolphin.utils.HBase;

public class ControlExec {
	
	private final static String DELIMITER = "\\|" ;
	private static final byte[] TABLE_NAME         = Bytes.toBytes(PropertiesCSF.HBASE_TB_CONTROLFEED);
	private static final byte[] COLUMN_FAMILY_NAME = Bytes.toBytes("cf");
    private static final byte[] COLUMN_QT          = Bytes.toBytes("qt");
    private static final byte[] COLUMN_QF          = Bytes.toBytes("qf");
    
    private static final byte[] COLUMN_FD          = Bytes.toBytes("fd");
    private static final byte[] COLUMN_DE          = Bytes.toBytes("de");
    private static final byte[] COLUMN_TE          = Bytes.toBytes("te");
    private static final byte[] COLUMN_SC          = Bytes.toBytes("sc");
        
    private static final int    SUCCESS            = 0;
    private static final int    ERROR              = 85;
    private static final int    FILE_NOT_FOUND     = 86;
    private static final int    ERRO_CONECTION     = 5;
    
    private static final int    ARGS_FUNCTION      = 0;
    private static final int    ARGS_PATH          = 1;
    private static final int    ARGS_FILE_DATE     = 2;
    private static final int    ARGS_DATE_EXEC     = 3;
    private static final int    ARGS_TIME_EXEC     = 4;
    
    private static final int    ARGS_FILE_NAME     = 1;
    private static final int    ARGS_FILE_COUNT    = 2;

	public static void main(String[] args) throws IOException {
		
		Logger log = Logger.getLogger("processo_validacao");
		try {
			String function = args[ARGS_FUNCTION];
			int returnCode = SUCCESS;

			//Deprecated HBase Connection
			/**
			Configuration conf = HBaseConfiguration.create();
			conf.addResource("hbase-site.xml");
			Connection connection = ConnectionFactory.createConnection(conf);
			//Admin admin = connection.getAdmin();
			*/

			Configuration conf = HBase.getHaddopConfig();
			Connection connection = HBase.getHBaseConnection();
			TableName tableName = TableName.valueOf(TABLE_NAME);
			Table table  = connection.getTable(tableName);
			
			// -i - ingestao do registros feeds
			if(function.equals("-i")) {
								
				String strPath         = args[ARGS_PATH];
				String strFileDate     = args[ARGS_FILE_DATE];
				String strDateExec     = args[ARGS_DATE_EXEC];
				String strTimeExec     = args[ARGS_TIME_EXEC];
				
				log.log(Level.INFO, String.format("Ingestao dos registros do arquivo control feed: %s", strPath));
							
			    Path path = new Path(strPath);
			    FileSystem fs = path.getFileSystem(conf);
			    
			    FSDataInputStream is    = fs.open(path);
		        BufferedInputStream bis = new BufferedInputStream(is);
			    byte[] buffer = new byte[1024];
				int bytesRead = 0;
				StringBuilder sb =  new StringBuilder();
				while ((bytesRead = bis.read(buffer)) != -1) {
					sb.append(new String(buffer, 0, bytesRead));         
				}	
						
				String lines[] = sb.toString().split("\n");
				for(String line : lines) {
					String token[]  = line.split(DELIMITER);
					
					log.log(Level.INFO, String.format("Append dos registros: %s %s", token[1], token[2]));
			    
					Put p = new Put(Bytes.toBytes(token[1]));
			        p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_QT, Bytes.toBytes(token[2]));		        
			        p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_FD, Bytes.toBytes(strFileDate));
			        p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_DE, Bytes.toBytes(strDateExec));
			        p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_TE, Bytes.toBytes(strTimeExec));		        
			        table.put(p);
				}
			}else if(function.equals("-v")){
				
				log.log(Level.INFO, "Processo de verificacao do arquivo e quantidade de registros");
				
				byte[] fileChecker    = Bytes.toBytes(args[ARGS_FILE_NAME]);
				int    countRecords   = Integer.parseInt(args[ARGS_FILE_COUNT]);
				
			    Get rowFile          = new Get(fileChecker);
				Result result        = table.get(rowFile);
				byte[] byteCountDoc  = result.getValue(COLUMN_FAMILY_NAME, COLUMN_QT);
				if(Bytes.toString(byteCountDoc) == null) {
				
					log.log(Level.WARNING,String.format("O arquivo %s não esta contido na tabela de controle", args[1]));
					returnCode = FILE_NOT_FOUND;	
				
				} else { 				
					int countDoc         = Integer.parseInt(Bytes.toString(byteCountDoc));
					Put p = new Put(fileChecker);
			        p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_QF, Bytes.toBytes(args[2]));
			        if(countRecords == countDoc) {
						p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_SC, Bytes.toBytes("s"));
					
						log.log(Level.INFO,String.format("A quantidade de registros do arquivo %s esta de acordo "
								+ "com a registrada na tabela de controle: %d", args[1], countDoc));
						
						returnCode = SUCCESS;												
					}else {
						p.addImmutable(COLUMN_FAMILY_NAME, COLUMN_SC, Bytes.toBytes("n"));
						
						log.log(Level.WARNING,String.format("A quantidade de registro do arquivo %s esta em desacordo"
								+ " aos da tabela de registro: no arquivo(%d) <> no registro(%d)"
								, args[1], countDoc, countRecords));						
				
						returnCode = ERROR;			
					}
					table.put(p);
				}
			}		
			connection.close();
		    System.exit(returnCode);
		}catch(IOException ioe) {
			
			log.log(Level.SEVERE, "Erro de conexão ao HBASE-Zookeper");	
            System.exit(ERRO_CONECTION);
		}
	}
}
