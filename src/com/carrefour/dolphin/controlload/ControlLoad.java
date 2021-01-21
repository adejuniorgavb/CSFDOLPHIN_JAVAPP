package com.carrefour.dolphin.controlload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.carrefour.dolphin.utils.PropertiesCSF;
import com.carrefour.dolphin.utils.HBase;


/**
 * Classe para controle dos jobs de ingestao, para gravacao no hbase 
 * 
 * @author Rodrigo Piva / Rony Brito
 *
 */
public class ControlLoad {
	
	private static final byte[] HBASE_TABLE_NAME   = Bytes.toBytes(PropertiesCSF.HBASE_TB_CONTROLLOAD);
	private static final byte[] COLUMN_FAMILY_NAME = Bytes.toBytes("cf");
	private static final byte[] COLUMN_TABLE_NAME  = Bytes.toBytes("table_name");
	private static final byte[] COLUMN_DATE_INI    = Bytes.toBytes("date_ini");
	private static final byte[] COLUMN_DATE_REF    = Bytes.toBytes("date_ref");
	
	
	private static final byte[] COLUMN_DATE_FIM    = Bytes.toBytes("date_fim");
    private static final byte[] COLUMN_QTDE_REG    = Bytes.toBytes("qtde_reg");
    private static final byte[] COLUMN_SUCCESS     = Bytes.toBytes("success");
    private static final byte[] COLUMN_MOTIVO      = Bytes.toBytes("mtv"); 

	
	private static final int INDEX_TYPE_OPERATION  = 0;
	private static final int INDEX_PROCESS_NAME    = 1;
	
	private static final int INDEX_TB_NAME         = 2;
	private static final int INDEX_DATE_REF        = 3;
	
	private static final int INDEX_QTD_FILE        = 2;
	private static final int INDEX_SUCCESS         = 3;
	private static final int INDEX_MOTIVO          = 4; //opcional
	
	private static final String TYPE_OPEARTION_START = "init";
	private static final String TYPE_OPEARTION_END   = "end";
	
	private static final int QTD_PARAMETERS_START    = 4;
	
	/**
	 * 
	 * @param args Array de parametros da execucao do inicio e fim 
	 * @throws Exception - Tipo de operacao invalida
	 * @throws Exception - Erro de quantidade de parametros
	 */
	public static void main (String args[]) throws Exception {

		//Deprecated HBase Connection
		/**
		Configuration conf = HBaseConfiguration.create();
	    conf.addResource("hbase-site.xml");
		Connection connection = ConnectionFactory.createConnection(conf);
		*/

		Connection connection = HBase.getHBaseConnection();
		TableName tableName  = TableName.valueOf(HBASE_TABLE_NAME);
		Table table          = connection.getTable(tableName);
		
		
		//VARIAVEIS RECEBIDAS VIA PARAMETRO    
		String typeOperation = args[INDEX_TYPE_OPERATION];
		String processName   = args[INDEX_PROCESS_NAME];
		
		//Definicao da chave
		Put put              = new Put(Bytes.toBytes(processName));	
		
		DateTimeFormatter formatDay   = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
		
		if(typeOperation.equals(TYPE_OPEARTION_START)) {
			
			if(args.length != QTD_PARAMETERS_START) {
				throw new Exception("Quantidade de parametros nao permitida");
			}
		
			String tbName        = args[INDEX_TB_NAME]; 
			String dateRef       = args[INDEX_DATE_REF];			    
			
			String dtIni                  = formatDay.format(LocalDateTime.now());
			    
			System.out.println("********** Tabela Hive: " + tbName);
			System.out.println("********** Data de Referencia: " + dateRef);
			System.out.println("********** Data/hora de inicio: " + dtIni);
			
			//Escrevendo no HBASE as colunas de Inicio
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_TABLE_NAME, Bytes.toBytes(tbName));
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_DATE_REF, Bytes.toBytes(dateRef));
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_DATE_INI, Bytes.toBytes(dtIni));
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_SUCCESS, Bytes.toBytes("\\N"));
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_DATE_FIM, Bytes.toBytes("\\N"));
			put.addColumn(COLUMN_FAMILY_NAME, COLUMN_MOTIVO, Bytes.toBytes("\\N"));
			    
		}else if (typeOperation.equals(TYPE_OPEARTION_END)) {
			
		    String qtdRegs     = args[INDEX_QTD_FILE];
		    String success     = args[INDEX_SUCCESS];
		      
		    String dtFim       = formatDay.format(LocalDateTime.now());
		    	    
		    System.out.println("********** Data/hora de fim: " + dtFim);
		    System.out.println("********** Quantidade de registros: " + qtdRegs);
		    System.out.println("********** Processo finalizou com sucesso?: " + success);
		    
		    //Escrevendo no HBASE as colunas de fim
		    put.addColumn(COLUMN_FAMILY_NAME, COLUMN_DATE_FIM, Bytes.toBytes(dtFim));
		    put.addColumn(COLUMN_FAMILY_NAME, COLUMN_QTDE_REG, Bytes.toBytes(qtdRegs));
		    put.addColumn(COLUMN_FAMILY_NAME, COLUMN_SUCCESS, Bytes.toBytes(success));
		    
		    //caso o processo fim possua 5 parametros, incluir o motivo
		    System.out.println("********** QTD Parametros " + QTD_PARAMETERS_START + " : " + args.length);
		    if(args.length > QTD_PARAMETERS_START) {
		    	String motivo = args[INDEX_MOTIVO];
		    	put.addColumn(COLUMN_FAMILY_NAME, COLUMN_MOTIVO, Bytes.toBytes(motivo));
		    	System.out.println("********** Motivo: " + motivo);
		    }
			
		}else {
			throw new Exception("Tipo de operacao nao permitida");
		}
		
		table.put(put);
		connection.close();
			
	}
}
