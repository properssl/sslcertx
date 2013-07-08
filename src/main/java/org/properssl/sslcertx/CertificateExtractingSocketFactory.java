package org.properssl.sslcertx;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.postgresql.ssl.WrappedFactory;

public class CertificateExtractingSocketFactory extends WrappedFactory {
	public CertificateExtractingSocketFactory(String arg)
			throws GeneralSecurityException {
		this();
	}

	public CertificateExtractingSocketFactory() throws GeneralSecurityException {
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null,
				new TrustManager[] { new CertificateExtractingTrustManager() },
				null);
		_factory = ctx.getSocketFactory();
	}
}
