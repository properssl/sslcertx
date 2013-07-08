package org.properssl.sslcertx;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class CertificateExtractingTrustManager implements X509TrustManager {
	public static X509Certificate chain[];

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		CertificateExtractingTrustManager.chain = chain;
		throw new CertificateException("Simulated failure to reject connection attempt");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[] {  };
	}
}
