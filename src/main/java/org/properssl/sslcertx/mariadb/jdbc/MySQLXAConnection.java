package org.properssl.sslcertx.mariadb.jdbc;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;


public class MySQLXAConnection extends MySQLPooledConnection implements XAConnection {
    public MySQLXAConnection(MySQLConnection connection) {
      super(connection);
    }
    public XAResource getXAResource() throws SQLException {
       return new MySQLXAResource(connection);
    }
}
