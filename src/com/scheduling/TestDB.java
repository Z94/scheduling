package com.scheduling;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDB {
	public static void main(String[] args) throws SQLException {
		Connection conn = DBUtil.getConnection();
		Statement sta = conn.createStatement();
		String sql = "select * from comm.t_contact_wechat_avatar_hist limit 2";
		ResultSet rs = sta.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1)+"  "+rs.getString(2));
		}
	}
}
