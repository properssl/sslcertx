### sslcertx - SSL Certificate Extractor

`sslcertx` is a command line tool to extract the X.509 certificate of a remote server. It connects to the remote server and prints the server certificate it receives in the SSL handshake.

### Security

This is **not** secure.

To clarify further, this is in *no way, shape, or form secure*. There is no validation of the remote server so the connection that extracts the certificate is vulnerable to a [man in the middle][MITM] attack.

If you are using this program to extract the SSL certificate of your remote server make sure to do it over a network connection that you trust.

Once the certificate has been extracted, if the transfer itself is not compromised by a MITM attack, it is safe to use to validate the remote server in the future. Only the original transfer itself is vulnerable as after that the certificate itself can be used to validate the remote server. For examples of properly validating self signed SSL certificates see [Proper SSL].

### Supported Servers

`sslcertx` supports the following types of servers:

 * PostgreSQL
 * MySQL
 * MariaDB

### Usage

    $ sslcertx --type <server-type> --host <host> [--port port]

### Output

If the SSL handshake is successful the program prints the client certificate, PEM encoded in DER format, any subject fields defined in the certificate, and the SHA1 finger print of the certificate. If the remote certificate includes a parent certificate chain then each certificate in the chain is also printed out.

### Options

 * `server-type` - `postgresql` | `mysql` | `mariadb`
 * `host` - the hostname or ip address of the remote server.
 * `port` - the port of the remote server. If this is not specified then the default port for the server type is used (eg 5432 for PostgreSQL, 3306 for MySQL)

### Building

To build the project using Maven run:

    $ mvn clean compile package

This will create an jar file in the `target` directory with all the external dependencies included in it. You can then execute it it via the `bin/sslcertx` shell script.

To install it locally either copy or symlink the jar file and shell script somewhere into your PATH (ex: ~/opt/sslcertx and ~/bin respectively) and updated the relative path in the shell script accordingly.

### Internals

For PostgreSQL `sslcertx` uses a custom SSLSocketFactory specified via the `sslFactory` connection parameter.

For MariaDB/MysQL `sslcertx` includes a slightly modified copy of the v1.1.3 MariaDB JDBC driver. The driver code has been modified to allow for a custom SSLSocketFactory.

In both cases, once the certificate has been recieved an exception is raised and the connection is immediately closed.

### Examples

Extracting the certificate of a PostgreSQL server running on it's default port:

    $ sslcertx --type postgresql --host db.example.com
    Client Certificate:
	CN -> precise64
	SHA1 Fingerprint: AA:0D:DE:37:A1:21:91:20:F9:DC:C3:E1:E2:E6:33:DF:9C:54:A3:E1
	-----BEGIN CERTIFICATE-----
	MIICpDCCAYwCCQCC22Re/9YzXjANBgkqhkiG9w0BAQUFADAUMRIwEAYDVQQDEwlwcmVjaXNlNjQw
	HhcNMTMwNjI5MjEyMjQxWhcNMjMwNjI3MjEyMjQxWjAUMRIwEAYDVQQDEwlwcmVjaXNlNjQwggEi
	MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCtIcrhCQ6UnKer3tTbuYmbvqJ2p3Zlau8iJvFr
	DcMJL9dD7i3tPwmFSmIBcqkv9AvIajLpXRv0vMjeMHzoUjoEFbzAgZlXKub5M2vUREGyyW8/2L9O
	QZmOAPSsws8c6qnQfyNyhFSg4A0Nd/MoWopQFEkLf7dV/uk6W+TV56cWA549mViGKEsFvNeq+Lmr
	ebHF8AoFNWfh0NiOxsfoxTndUUD1izqGfBdatkWASJyDYSW9kXTCWaHH34/S7k44t/UAdLnSm+RO
	rVyCF+LESyIrmWtQLraIzKUc2Djp872blcDQZW/W2uvHRbdBJ16cbg4TXYLuZBw2v8B/L3ZrtB2X
	AgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAG+S/B9q40ppmUn5RFMlLqQ0wSQFBhmK6OhiszprQ0r9
	de2Ft0nFdiNUepxnq9WFdrD99aOQ+o2OyTbuxLMKHydlyg8lDukg4/aR0J+NWRruLHujzI6gpul6
	Zkc8oBoG12eU9SsHFc3UsVeiB1t490VINS9/KX1xfO2MiLxNHiKffE+d2X71/QZ77+3fH/gaqz67
	Wvg45gpJpP9bKClva4TJmELzoc2vQRTucz+1s3JDrXGp8JXgfEDtWOHXKpFON9R7YLzenZHcXvBp
	tMf/z9SZW1W60pbFdEWR5Oo4SGvj6Xzq9OPIdvSMJk7+FwgDCkwGsfOfGrIYh92i5VZ5Y8Y=
	-----END CERTIFICATE-----

Extracting the certificate of a MySQL server running on a non-default port. This server's certificate has been signed by an internal certificate authority so the parent certificate is also printed:

    $ sslcertx --type mysql --host foo.example.com --port 13306
	Client Certificate:
	C -> US
	ST -> Texas
	L -> Waco
	O -> SuccessBricks Inc
	OU -> Engineering
	CN -> db-mm-us-east-01
	1.2.840.113549.1.9.1 -> [B@5e5aefe
	SHA1 Fingerprint: 65:55:AD:F1:44:06:19:7A:7D:96:1B:B6:C3:EA:E3:50:B6:B5:1E:0D
	-----BEGIN CERTIFICATE-----
	MIIDtDCCApwCCQC9oXAlavEJvDANBgkqhkiG9w0BAQUFADCBmTELMAkGA1UEBhMCVVMxDjAMBgNV
	BAgMBVRleGFzMQ0wCwYDVQQHDARXYWNvMR0wGwYDVQQKDBRTdWNjZXNzQnJpY2tzIEluYyBDQTEU
	MBIGA1UECwwLRW5naW5lZXJpbmcxEjAQBgNVBAMMCUNBIE1hc3RlcjEiMCAGCSqGSIb3DQEJARYT
	c3VwcG9ydEBjbGVhcmRiLmNvbTAeFw0xMTA4MDkyMDAwMTlaFw0zODEyMjQyMDAwMTlaMIGdMQsw
	CQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxDTALBgNVBAcMBFdhY28xGjAYBgNVBAoMEVN1Y2Nl
	c3NCcmlja3MgSW5jMRQwEgYDVQQLDAtFbmdpbmVlcmluZzEZMBcGA1UEAwwQZGItbW0tdXMtZWFz
	dC0wMTEiMCAGCSqGSIb3DQEJARYTc3VwcG9ydEBjbGVhcmRiLmNvbTCCASIwDQYJKoZIhvcNAQEB
	BQADggEPADCCAQoCggEBAKPXNMtDVrVaFsYvvyAMhjHU9ggQvNO4+Qln+4+EHk6DgDmaga1iB8mA
	zVtYzfW2xSXD2j++nEi/ElP6hbM0QwvM2OvAkXkbQP5R8BhELzQii/YWrX86fZUdxAOc11cp1RMg
	/oA7MlIZAN2jc5AVfpwGPA5r8pCTdrWGkNtkj+pnqoGqaltVgHilD8H4VAERjvM4oKdhdobnxsCt
	lKFEYXrX5iJmLzLAJMU/OVtMk+MW2Sz6ErBJoNqQ50YrYml5xkqD46526L6gNJV/cAY8kz/ntX62
	SP8cLloK6UiYeZZaAkUOonAVl8qPcUWHfvkbAkWH92oaGZbzan+LKogmR88CAwEAATANBgkqhkiG
	9w0BAQUFAAOCAQEAMEuSxNf1XNJU6UibNiatSvcwDo4wyVPMCk51+PF9tBg4bPeMTJWb42K7VqC4
	Q0J01MAc/kS8GchCTCKBdx4tsZyPvsEL0b/NrFbY4WJdMxnU4MpuPrfUoSbHImRPkYtmK2WloNPK
	cikCA6mKxTmg99gUI7gLhI53vKhaLZrC4rSyhH7K2oXDms60VwOrkJAF0CDwTlISQmJOlq6jRHvw
	DgsB5HAdCxDZDu3irYNLX+j/Ul+WMXfqMUzrnfwbPc0t7BUOBeoE/cSpNfnnllNLf8r+UaJc7KEy
	bp4umJ1GOetH7uy9CPP2Lq12zwFufJ8rgxXLat95+bP2xcC9duZrKQ==
	-----END CERTIFICATE-----
    
	Parent Certificate #1:
	C -> US
	ST -> Texas
	L -> Waco
	O -> SuccessBricks Inc CA
	OU -> Engineering
	CN -> CA Master
	1.2.840.113549.1.9.1 -> [B@5a68ef48
	SHA1 Fingerprint: F1:63:B0:23:36:62:E6:EC:62:F4:5F:1D:84:FF:8C:70:11:84:A0:5D
	-----BEGIN CERTIFICATE-----
	MIIEBzCCAu+gAwIBAgIJAPs/TPnO24QSMA0GCSqGSIb3DQEBBQUAMIGZMQswCQYDVQQGEwJVUzEO
	MAwGA1UECAwFVGV4YXMxDTALBgNVBAcMBFdhY28xHTAbBgNVBAoMFFN1Y2Nlc3NCcmlja3MgSW5j
	IENBMRQwEgYDVQQLDAtFbmdpbmVlcmluZzESMBAGA1UEAwwJQ0EgTWFzdGVyMSIwIAYJKoZIhvcN
	AQkBFhNzdXBwb3J0QGNsZWFyZGIuY29tMB4XDTExMDgwOTE5NTcxOVoXDTM4MTIyNDE5NTcxOVow
	gZkxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczENMAsGA1UEBwwEV2FjbzEdMBsGA1UECgwU
	U3VjY2Vzc0JyaWNrcyBJbmMgQ0ExFDASBgNVBAsMC0VuZ2luZWVyaW5nMRIwEAYDVQQDDAlDQSBN
	YXN0ZXIxIjAgBgkqhkiG9w0BCQEWE3N1cHBvcnRAY2xlYXJkYi5jb20wggEiMA0GCSqGSIb3DQEB
	AQUAA4IBDwAwggEKAoIBAQDl3xOV5yj2XkwCgMZ2H3AVTZrGf/LhuX1EByOaoYeutBQfzb049wp4
	olmFhcL7ZXmsBJb3/7fYwyxs6rbJ0diznGFATOaEWE7yNm14gIagL6xb+Arqh9TrlF77Wts32RHI
	QvCAt1Sw8VeoBhWKLp96gCC1ZRSHEdh0qaTOFXRgEUGXOmtPtwiNaDwVsaYN82a9AfhKqdygRMzA
	PYZk29crjZy13CMgz8JZIGEKRxTqbl8ClR+A6aW3Opgf6hD/vASGigGfgbjNNPeEHUUYHj8yW3OW
	n7CrItdm/2TXG0xdks5VPJonHY5KdhSLobJZCyR9Oc00bT4gSOsDEKO4+t3JAgMBAAGjUDBOMB0G
	A1UdDgQWBBRs1gyV3ammzQYMNt78zZpXDz74GzAfBgNVHSMEGDAWgBRs1gyV3ammzQYMNt78zZpX
	Dz74GzAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4IBAQATIQy8MJ9aZ4z6ourkHeY/Rmkf
	MF2lfpknsPWkab/DpTkfQ4ZtAv8ZP+lCYzdoBm98FJoOhLNJxgI4M1jHg4ubccoL6r+MWBUMCT5K
	W6zFyom9p1wYD8dpIdzV8cTmsJTt3vrUWkC+aP2Dz3EaMHzH14JyLRxqhoOOr456y6HD4SXEwzW3
	8n8N9J15Rpp6Am/y+dVEXquUf0Qj7l67ElIgDByBitV4AVUnmmu7C/Kn+GzTKFetyLGbEXgbgalg
	gtnUItm4nFIrcOh51xxnTNtWDNktD06/0Oss5OY901VVwSm0JmV0LtNgymxXhQAJVDVaIAn4C0/H
	h8GudcAs/QKv
	-----END CERTIFICATE-----

### License

The modified MariaDB JDBC driver, moved to the package `org.properssl.sslcertx.mariadb.jdbc`, is licensed under the LGPL.

All other code for `sslcertx` is licensed under the [MIT License].

[MITM]: http://en.wikipedia.org/#TODO
[Proper SSL]: http://properssl.org/
[MIT License]: LICENSE
