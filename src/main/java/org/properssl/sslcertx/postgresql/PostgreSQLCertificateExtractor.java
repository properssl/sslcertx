package org.properssl.sslcertx.postgresql;

import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.properssl.sslcertx.CertificateExtractingTrustManager;

public class PostgreSQLCertificateExtractor {
	public static X509Certificate[] extractCertificate(String host, int port) {
		try {
			// this can be anything as the connection never gets established
			String database = "foobar";

			String url = "jdbc:postgresql://" + host + ":" + port + "/"
					+ database;
			Properties info = new Properties();
			info.setProperty("ssl", "true");
			info.setProperty("sslfactory",
					"org.properssl.sslcertx.CertificateExtractingSocketFactory");
			Connection conn = null;
			Throwable connectException = null;
			try {
				conn = DriverManager.getConnection(url, info);
			} catch (SQLException e) {
				connectException = e;
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (Exception e) {
					}
				}
			}
			X509Certificate chain[] = CertificateExtractingTrustManager.chain;
			if (chain != null && chain.length > 0) {
				return chain;
			}
			throw new RuntimeException("Could not extract certificate",
					connectException);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
