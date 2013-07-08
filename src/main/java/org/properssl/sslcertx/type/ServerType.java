package org.properssl.sslcertx.type;

public enum ServerType {
	POSTGRESQL(5432), //
	MYSQL(3306), //
	MARIADB(3306), //
	;

	private final int defaultPort;

	private ServerType(int defaultPort) {
		this.defaultPort = defaultPort;
	}

	public int getDefaultPort() {
		return defaultPort;
	}
}
