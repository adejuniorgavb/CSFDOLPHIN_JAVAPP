package com.carrefour.dolphin.utils;

import com.typesafe.config.ConfigFactory;

public class PropertiesCSF {
	
	public static final String KEY_FILE_LOCATION    = ConfigFactory.load().getString("key_file_location");
	public static final String APPLICATION_NAME     = ConfigFactory.load().getString("application_name");
	public static final int    PAGE_SIZE            = ConfigFactory.load().getInt("page_size");
	public static final String DST_PATH             = ConfigFactory.load().getString("dst_path");

	public static final String HBASE_TB_CONTROLLOAD = ConfigFactory.load().getString("hbase_table_name_control_load");
	public static final String HBASE_TB_CONTROLFEED = ConfigFactory.load().getString("hbase_table_name_control_feed");
}
