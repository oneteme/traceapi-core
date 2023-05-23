package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class DataSourceWrapper implements DataSource {

	@Delegate
	private final DataSource ds;

	@Override
	public Connection getConnection() throws SQLException {
		var beg = currentTimeMillis();
		return new ConnectionWrapper(ds.getConnection(), beg);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException{
		var beg = currentTimeMillis();
		return new ConnectionWrapper(ds.getConnection(username, password), beg);
	}
}