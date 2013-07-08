package org.properssl.sslcertx;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Hex;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.postgresql.util.Base64;
import org.properssl.sslcertx.mariadb.MariaDBCertificateExtractor;
import org.properssl.sslcertx.postgresql.PostgreSQLCertificateExtractor;
import org.properssl.sslcertx.type.ServerType;

public class App {
	@Option(name = "--type", required = true, usage = "the type of remote server")
	ServerType type;

	@Option(name = "--host", required = true, usage = "the hostname or ip address of the remote server")
	String host;

	@Option(name = "--port", required = false, usage = "the port of the remote server")
	int port = -1;

	public static void main(String args[]) {
		new App().doMain(args);
	}

	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		
		try {
			// parse the arguments.
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar sslcertx.jar [options...]");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			return;
		}

		if (port < 0) {
			port = type.getDefaultPort();
		}

		X509Certificate chain[] = null;
		switch (type) {
		case POSTGRESQL:
			chain = PostgreSQLCertificateExtractor.extractCertificate(host,
					port);
			break;
		case MYSQL:
		case MARIADB:
			chain = MariaDBCertificateExtractor.extractCertificate(host, port);
			break;
		default:
			throw new RuntimeException("Unhandled server type: " + type);
		}

		try {
			for (int i = 0; i < chain.length; i++) {
				X509Certificate cert = chain[i];
				X500Principal principal = cert.getSubjectX500Principal();
				if (i == 0) {
					System.out.println("Client Certificate:");
				} else {
					System.out.println("\n");
					System.out.println("Parent Certificate #" + i + ":");
				}
				String dn = principal.getName();
				LdapName ldapDN = new LdapName(dn);
				for (Rdn rdn : ldapDN.getRdns()) {
					System.out.println(rdn.getType() + " -> " + rdn.getValue());
				}
				System.out.println("SHA1 Fingerprint: " + getFingerprint(cert));
				System.out.print(encodeCert(cert.getEncoded()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static String encodeCert(byte[] cert) {
		StringBuilder sb = new StringBuilder();
		sb.append("-----BEGIN CERTIFICATE-----\n");
		sb.append(Base64.encodeBytes(cert));
		sb.append("\n");
		sb.append("-----END CERTIFICATE-----\n");
		return sb.toString();
	}

	public static String getFingerprint(X509Certificate cert)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		String hex = Hex.encodeHexString(digest).toUpperCase();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hex.length(); i++) {
			if (i > 0 && i % 2 == 0) {
				sb.append(":");
			}
			sb.append(hex.charAt(i));
		}
		return sb.toString();
	}
}
