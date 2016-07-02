package com.scheduling;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DBUtil {
	private static ComboPooledDataSource cpds=new ComboPooledDataSource(true);
	public static Connection  getConnection(){    
        try {    
            return cpds.getConnection();    
        } catch (SQLException e) {    
            e.printStackTrace();    
        }    
        return null;    
    } 
	
	public static void close(ResultSet rs,Statement sta,Connection conn) throws SQLException{
		rs.close();
		sta.close();
		conn.close();
	}
}
