package com.jibug.cetty.sample.common.jdbctemplate.impl;

import com.jibug.cetty.sample.common.jdbctemplate.CommonDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class CommonDaoImp implements CommonDao {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	public JdbcTemplate getJdbcTemplate(){
		return jdbcTemplate;
	}

	public int batchUpdate(String[] sqls) {
		Connection conn = null;
		Statement st = null;
		int result = 0;
		try {
			conn = getJdbcTemplate().getDataSource().getConnection();
			st = conn.createStatement();
			conn.setAutoCommit(false);
			for(int i=0;i<sqls.length;i++){
				st.addBatch(sqls[i]);
			}
			st.executeBatch();
			conn.commit();
			result = 1;
		} catch (Exception e) {
			if(conn!=null){
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}finally{
			try {
				if(null!=st){
					st.close();
				}
				if(null!=conn){
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	public void executSql(String sql) {
		int result = 0;
		try (
				Connection conn = getJdbcTemplate().getDataSource().getConnection();
				Statement st = conn.prepareStatement(sql)
		){

			conn.setAutoCommit(false);
			st.execute(sql);
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getTotal(String sql) {
		return getJdbcTemplate().queryForObject(sql,Integer.class);
	}

	public int getTotal(String sql, Object[] params) {
		return getJdbcTemplate().queryForObject(sql, params,Integer.class);
	}

	public List<Map<String, Object>> queryForList(String sql) {
		return getJdbcTemplate().queryForList(sql);
	}

	public List<Map<String, Object>> queryForList(String sql, Object[] params) {
		return getJdbcTemplate().queryForList(sql, params);
	}

	public int update(String sql) {
		return getJdbcTemplate().update(sql);
	}

	public int update(String sql, Object[] params) {
		return getJdbcTemplate().update(sql, params);
	}

	public int queryForInt(String sql) {
		return getJdbcTemplate().queryForObject(sql,Integer.class);
	}

	public int queryForInt(String sql, Object[] params) {
		return getJdbcTemplate().queryForObject(sql, params,Integer.class);
	}
	
	public SqlRowSet queryForRowSet(String sql, Object[] params) {
		return  getJdbcTemplate().queryForRowSet( sql, params);
	}

}
