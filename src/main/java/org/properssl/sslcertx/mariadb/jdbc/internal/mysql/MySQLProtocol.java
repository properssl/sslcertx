/*
MariaDB Client for Java

Copyright (c) 2012 Monty Program Ab.

This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation; either version 2.1 of the License, or (at your option)
any later version.

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
for more details.

You should have received a copy of the GNU Lesser General Public License along
with this library; if not, write to Monty Program Ab info@montyprogram.com.

This particular MariaDB Client for Java file is work
derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
the following copyright and notice provisions:


Copyright (c) 2009-2011, Marcus Eriksson, Stephane Giron, Marc Isambart, Trond Norbye

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the driver nor the names of its contributors may not be
used to endorse or promote products derived from this software without specific
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.
*/

package org.properssl.sslcertx.mariadb.jdbc.internal.mysql;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.properssl.sslcertx.mariadb.jdbc.HostAddress;
import org.properssl.sslcertx.mariadb.jdbc.JDBCUrl;
import org.properssl.sslcertx.mariadb.jdbc.internal.SQLExceptionMapper;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.BinlogDumpException;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.PacketFetcher;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.QueryException;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.ServerStatus;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.Utils;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.CompressOutputStream;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.DecompressInputStream;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.EOFPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ErrorPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.LocalInfilePacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.OKPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.PacketOutputStream;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.RawPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ResultPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ResultPacketFactory;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ResultSetPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.SyncPacketFetcher;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.buffer.ReadUtil;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.buffer.Reader;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.commands.ClosePacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.commands.SelectDBPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.commands.StreamedQueryPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.query.MySQLQuery;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.query.Query;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.CachedSelectResult;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.NoSuchColumnException;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.QueryResult;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.SelectQueryResult;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.StreamingSelectResult;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults.UpdateResult;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.MySQLGreetingReadPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.commands.AbbreviatedMySQLClientAuthPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.commands.MySQLBinlogDumpPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.commands.MySQLClientAuthPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.commands.MySQLClientOldPasswordAuthPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.commands.MySQLPingPacket;



class MyX509TrustManager implements X509TrustManager {
    boolean trustServerCeritifcate;
    String serverCertFile;
    X509TrustManager  trustManager;

    public MyX509TrustManager(Properties props) throws Exception{
        boolean trustServerCertificate  =  props.getProperty("trustServerCertificate") != null;
        if (trustServerCertificate)
            return;

        serverCertFile =  props.getProperty("serverSslCert");
        InputStream inStream;

        if (serverCertFile.startsWith("-----BEGIN CERTIFICATE-----")) {
            inStream = new ByteArrayInputStream(serverCertFile.getBytes());
        } else if (serverCertFile.startsWith("classpath:")) {
            // Load it from a classpath relative file
            String classpathFile = serverCertFile.substring("classpath:".length());
            inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFile);
        } else {
            inStream = new FileInputStream(serverCertFile);
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate ca = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            // Note: KeyStore requires it be loaded even if you don't load anything into it:
            ks.load(null);
        } catch (Exception e) {

        }
        ks.setCertificateEntry(UUID.randomUUID().toString(), ca);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for(TrustManager tm : tmf.getTrustManagers()) {
        	if (tm instanceof X509TrustManager) {
      	        trustManager = (X509TrustManager) tm;
      		    break;
            }
        }
        if (trustManager == null) {
            throw new RuntimeException("No X509TrustManager found");
        }
    }
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        if (trustManager == null) {
            return;
        }
        trustManager.checkServerTrusted(x509Certificates, s);
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

public class MySQLProtocol {
    private final static Logger log = Logger.getLogger(MySQLProtocol.class.getName());
    private boolean connected = false;
    private Socket socket;
    private PacketOutputStream writer;
    private  String version;
    private boolean readOnly = false;
    private String database;
    private final String username;
    private final String password;
    private int maxRows;  /* max rows returned by a statement */
    private SyncPacketFetcher packetFetcher;
    private final Properties info;
    private  long serverThreadId;
    public boolean moreResults = false;
    public boolean hasWarnings = false;
    public StreamingSelectResult activeResult= null;
    public int datatypeMappingFlags;
    public Set<ServerStatus> serverStatus;
    JDBCUrl jdbcUrl;
    HostAddress currentHost;
    
    private int majorVersion;
    private int minorVersion;
    private int patchVersion;

    boolean hostFailed;
    long failTimestamp;
    int reconnectCount;
    int queriesSinceFailover;

    /* =========================== HA  parameters ========================================= */
    /**
     * 	Should the driver try to re-establish stale and/or dead connections?
     * 	NOTE: exceptions will still be thrown, yet the next retry will repair the connection
     */
    private boolean autoReconnect = false;

    /**
     * Maximum number of reconnects to attempt if autoReconnect is true, default is 3
     */
    private int maxReconnects=3;

    /**
     * When using loadbalancing, the number of times the driver should cycle through available hosts, attempting to connect.
     * Between cycles, the driver will pause for 250ms if no servers are available.	120
     */
    int retriesAllDown = 120;
    /**
     * If autoReconnect is enabled, the initial time to wait between re-connect attempts (in seconds, defaults to 2)
     */
    int initialTimeout = 2;
    /**
     * When autoReconnect is enabled, and failoverReadonly is false, should we pick hosts to connect to on a round-robin
     * basis?
     */

    boolean roundRobinLoadBalance  = false;
    /**
     * 	Number of queries to issue before falling back to master when failed over (when using multi-host failover).
     * 	Whichever condition is met first, 'queriesBeforeRetryMaster' or 'secondsBeforeRetryMaster' will cause an
     * 	attempt to be made to reconnect to the master. Defaults to 50
     */
    int queriesBeforeRetryMaster =  50;

    /**
     * How long should the driver wait, when failed over, before attempting	30
     */
    int secondsBeforeRetryMaster =  30;

    private SSLSocketFactory getSSLSocketFactory(boolean trustServerCertificate)  throws QueryException
    {
        if (info.getProperty("trustServerCertificate") == null
            && info.getProperty("serverSslCert") == null) {
            return (SSLSocketFactory)SSLSocketFactory.getDefault();
        }
        
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509TrustManager[] m = {new MyX509TrustManager(info)};
            sslContext.init(null, m ,null);
        return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new QueryException(e.getMessage(),0, "HY000", e);
        }

    }
    /**
     * Get a protocol instance
     * @param url connection URL
     * @param username the username
     * @param password the password
     * @param info
     * @throws org.properssl.sslcertx.mariadb.jdbc.internal.common.QueryException
     *          if there is a problem reading / sending the packets
     */
    public MySQLProtocol(JDBCUrl url,
                         final String username,
                         final String password,
                         Properties info)
            throws QueryException {
        this.info = info;
        this.jdbcUrl = url;
        this.database = (jdbcUrl.getDatabase() == null ? "" : jdbcUrl.getDatabase());
        this.username = (username == null ? "" : username);
        this.password = (password == null ? "" : password);
        

        String logLevel = info.getProperty("MySQLProtocolLogLevel");
        if (logLevel != null)
        	log.setLevel(Level.parse(logLevel));
        else
        	log.setLevel(Level.OFF);

        setDatatypeMappingFlags();
        parseHAOptions();
        connect();
    }

    private void parseHAOptions() {
        String s = info.getProperty("autoReconnect");
        if (s != null && s.equals("true"))
            autoReconnect = true;
        s = info.getProperty("maxReconnects");
        if (s != null)
            maxReconnects = Integer.parseInt(s);
        s = info.getProperty("queriesBeforeRetryMaster");
        if (s != null)
            queriesBeforeRetryMaster = Integer.parseInt(s);
        s = info.getProperty("secondsBeforeRetryMaster");
        if (s != null)
            secondsBeforeRetryMaster = Integer.parseInt(s);
    }
    /**
     * Connect the client and perform handshake
     *
     * @throws QueryException  : handshake error, e.g wrong user or password
     * @throws IOException : connection error (host/port not available)
     */
    void connect(String host, int port) throws QueryException, IOException{
        SocketFactory socketFactory = null;
        String socketFactoryName = info.getProperty("socketFactory");
        if (socketFactoryName != null) {
            try {
                socketFactory = (SocketFactory) (Class.forName(socketFactoryName).newInstance());
            } catch (Exception sfex){
                log.info("Failed to create socket factory " + socketFactoryName);
                socketFactory = SocketFactory.getDefault();
            }
        }  else {
            socketFactory = SocketFactory.getDefault();
        }

        // Extract connectTimeout URL parameter
        String connectTimeoutString = info.getProperty("connectTimeout");
        Integer connectTimeout = null;
        if (connectTimeoutString != null) {
           try {
               connectTimeout = Integer.valueOf(connectTimeoutString);
           } catch (Exception e) {
               connectTimeout = null;
           }
        }
        // Create socket with timeout if required
        if (info.getProperty("pipe") != null) {
            socket = new org.properssl.sslcertx.mariadb.jdbc.internal.mysql.NamedPipeSocket(host, info.getProperty("pipe"));
        } else {
            socket = socketFactory.createSocket();
        }

        try {
            String value = info.getProperty("tcpNoDelay", "false");
            if (value.equalsIgnoreCase("true"))
                socket.setTcpNoDelay(true);
            
            value = info.getProperty("tcpKeepAlive", "false");
            if (value.equalsIgnoreCase("true"))
                socket.setKeepAlive(true);

            value = info.getProperty("tcpRcvBuf");
            if (value != null)
                socket.setReceiveBufferSize(Integer.parseInt(value));

            value = info.getProperty("tcpSndBuf");
            if (value != null)
                socket.setSendBufferSize(Integer.parseInt(value));
            
            value = info.getProperty("tcpAbortiveClose","false");
            if (value.equalsIgnoreCase("true"))
                socket.setSoLinger(true, 0);
                
       } catch (Exception e) {
            log.finest("Failed to set socket option: " + e.getLocalizedMessage());
       }

       // Bind the socket to a particular interface if the connection property
       // localSocketAddress has been defined.
       String localHost = info.getProperty("localSocketAddress");
       if (localHost != null) {
           InetSocketAddress localAddress = new InetSocketAddress(localHost, 0);
           socket.bind(localAddress);
       }

       if (!socket.isConnected()) {
            InetSocketAddress sockAddr = new InetSocketAddress(host, port);
            if (connectTimeout != null) {
                socket.connect(sockAddr, connectTimeout * 1000);
            } else {
                socket.connect(sockAddr);
            }
       }

       // Extract socketTimeout URL parameter
       String socketTimeoutString = info.getProperty("socketTimeout");
       Integer socketTimeout = null;
       if (socketTimeoutString != null) {
           try {
               socketTimeout = Integer.valueOf(socketTimeoutString);
           } catch (Exception e) {
               socketTimeout = null;
           }
       }
       if (socketTimeout != null)
           socket.setSoTimeout(socketTimeout);

       try {
           InputStream reader;
           reader = new BufferedInputStream(socket.getInputStream(), 32768);
           packetFetcher = new SyncPacketFetcher(reader);
           writer = new PacketOutputStream(socket.getOutputStream());
           RawPacket packet =  packetFetcher.getRawPacket();
           if (ReadUtil.isErrorPacket(packet)) {
               reader.close();
               ErrorPacket errorPacket = (ErrorPacket)ResultPacketFactory.createResultPacket(packet);
               throw new QueryException(errorPacket.getMessage());
           }
           final MySQLGreetingReadPacket greetingPacket = new MySQLGreetingReadPacket(packet);
           this.serverThreadId = greetingPacket.getServerThreadID();
           boolean useCompression = false;

           log.finest("Got greeting packet");
           this.version = greetingPacket.getServerVersion();
           parseVersion();
           byte packetSeq = 1;
           final Set<MySQLServerCapabilities> capabilities = EnumSet.of(MySQLServerCapabilities.LONG_PASSWORD,
                   MySQLServerCapabilities.IGNORE_SPACE,
                   MySQLServerCapabilities.CLIENT_PROTOCOL_41,
                   MySQLServerCapabilities.TRANSACTIONS,
                   MySQLServerCapabilities.SECURE_CONNECTION,
                   MySQLServerCapabilities.LOCAL_FILES,
                   MySQLServerCapabilities.MULTI_RESULTS,
                   MySQLServerCapabilities.FOUND_ROWS);
           if(info.getProperty("allowMultiQueries") != null) {
               capabilities.add(MySQLServerCapabilities.MULTI_STATEMENTS);
           }
           if(info.getProperty("useCompression") != null) {
               capabilities.add(MySQLServerCapabilities.COMPRESS);
               useCompression = true;
           }
           if(info.getProperty("interactiveClient") != null) {
               capabilities.add(MySQLServerCapabilities.CLIENT_INTERACTIVE);
           }
           if(info.getProperty("useSSL") != null && greetingPacket.getServerCapabilities().contains(MySQLServerCapabilities.SSL)) {
               capabilities.add(MySQLServerCapabilities.SSL);
               AbbreviatedMySQLClientAuthPacket amcap = new AbbreviatedMySQLClientAuthPacket(capabilities);
               amcap.send(writer);

               boolean trustServerCertificate  =  info.getProperty("trustServerCertificate") != null;
               
               // Modified to always use the custom socket factory:
               String sslSocketFactoryClass = info.getProperty("sslSocketFactory");
               SSLSocketFactory f;
               try {
            	   f = (SSLSocketFactory) Class.forName(sslSocketFactoryClass).newInstance();
               } catch( Exception e) {
            	   throw new RuntimeException(e);
               }
               SSLSocket sslSocket = (SSLSocket)f.createSocket(socket,
                       socket.getInetAddress().getHostAddress(),  socket.getPort(),  false);

               sslSocket.setEnabledProtocols(new String [] {"TLSv1"});
               sslSocket.setUseClientMode(true);
               sslSocket.startHandshake();
               socket = sslSocket;
               writer = new PacketOutputStream(socket.getOutputStream());
               reader = new BufferedInputStream(socket.getInputStream(), 32768);
               packetFetcher = new SyncPacketFetcher(reader);

               packetSeq++;
           } else if(info.getProperty("useSSL") != null){
               throw new QueryException("Trying to connect with ssl, but ssl not enabled in the server");
           }

           // If a database is given, but createDB is not defined or is false,
           // then just try to connect to the given database
           if (database != null && !createDB())
               capabilities.add(MySQLServerCapabilities.CONNECT_WITH_DB);

           final MySQLClientAuthPacket cap = new MySQLClientAuthPacket(this.username,
                   this.password,
                   database,
                   capabilities,
                   greetingPacket.getSeed(),
                   packetSeq);
           cap.send(writer);
           log.finest("Sending auth packet");

           RawPacket rp = packetFetcher.getRawPacket();

           if ((rp.getByteBuffer().get(0) & 0xFF) == 0xFE) {   // Server asking for old format password
               final MySQLClientOldPasswordAuthPacket oldPassPacket = new MySQLClientOldPasswordAuthPacket(
                       this.password, Utils.copyWithLength(greetingPacket.getSeed(),
                       8), rp.getPacketSeq() + 1);
               oldPassPacket.send(writer);

               rp = packetFetcher.getRawPacket();
           }

           if (useCompression) {
               writer = new PacketOutputStream(new CompressOutputStream(socket.getOutputStream()));
               packetFetcher = new SyncPacketFetcher(new DecompressInputStream(socket.getInputStream()));
           }
           checkErrorPacket(rp);
           ResultPacket resultPacket = ResultPacketFactory.createResultPacket(rp);
           OKPacket ok = (OKPacket)resultPacket;
           serverStatus = ok.getServerStatus();
           
           // In JDBC, connection must start in autocommit mode.
           if (!serverStatus.contains(ServerStatus.AUTOCOMMIT)) {
               executeQuery(new MySQLQuery("set autocommit=1"));
           }

           // At this point, the driver is connected to the database, if createDB is true,
           // then just try to create the database and to use it
           if (createDB()) {
               // Try to create the database if it does not exist
               executeQuery(new MySQLQuery("CREATE DATABASE IF NOT EXISTS " + this.database));
               // and switch to this database
               executeQuery(new MySQLQuery("USE " + this.database));
           }

           activeResult = null;
           moreResults = false;
           hasWarnings = false;
           connected = true;
           hostFailed = false; // Prevent reconnects
       } catch (IOException e) {
           throw new QueryException("Could not connect to " + host + ":" +
                   port + ": " + e.getMessage(),
                   -1,
                   SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                   e);
       }

    }

    void checkErrorPacket(RawPacket rp) throws QueryException{
        if (rp.getByteBuffer().get(0) == -1) {
            ErrorPacket ep = new ErrorPacket(rp);
            String message = ep.getMessage();
            throw new QueryException("Could not connect: " + message);
        }
    }
    
    
    void readEOFPacket() throws QueryException, IOException {
        RawPacket rp = packetFetcher.getRawPacket();
        checkErrorPacket(rp);
        ResultPacket resultPacket = ResultPacketFactory.createResultPacket(rp);
        if (resultPacket.getResultType() != ResultPacket.ResultType.EOF) {
            throw new QueryException("Unexpected packet type " + resultPacket.getResultType()  + 
                    "insted of EOF");
        }
        EOFPacket eof = (EOFPacket)resultPacket;
        this.hasWarnings = eof.getWarningCount() > 0;
        this.serverStatus = eof.getStatusFlags();
    }
    
    void readOKPacket()  throws QueryException, IOException  {
        RawPacket rp = packetFetcher.getRawPacket();
        checkErrorPacket(rp);
        ResultPacket resultPacket = ResultPacketFactory.createResultPacket(rp);
        if (resultPacket.getResultType() != ResultPacket.ResultType.OK) {
            throw new QueryException("Unexpected packet type " + resultPacket.getResultType()  + 
                    "insted of OK");
        }
        OKPacket ok = (OKPacket)resultPacket;
        this.hasWarnings = ok.getWarnings() > 0;
        this.serverStatus = ok.getServerStatus();
    }
    
    public class PrepareResult {
        public int statementId;
        public MySQLColumnInformation[] columns;
        public MySQLColumnInformation[] parameters;
        public PrepareResult(int statementId, MySQLColumnInformation[] columns,  MySQLColumnInformation parameters[]) {
            this.statementId = statementId;
            this.columns = columns;
            this.parameters = parameters;
        }
    }

    public  PrepareResult prepare(String sql) throws QueryException {
        try {
            writer.startPacket(0);
            writer.write(0x16);
            writer.write(sql.getBytes("UTF8"));
            writer.finishPacket();
            
            RawPacket rp  = packetFetcher.getRawPacket();
            checkErrorPacket(rp);
            byte b = rp.getByteBuffer().get(0);
            if (b == 0) {
                /* Prepared Statement OK */
                Reader r = new Reader(rp);
                r.readByte(); /* skip field count */
                int statementId = r.readInt();
                int numColumns = r.readShort();
                int numParams = r.readShort();
                r.readByte(); // reserved
                this.hasWarnings = r.readShort() > 0;
                MySQLColumnInformation[] params = new MySQLColumnInformation[numParams];
                if (numParams > 0) {
                    for (int i = 0; i < numParams; i++) {
                        params[i] = new MySQLColumnInformation(packetFetcher.getRawPacket());
                    }
                    readEOFPacket();
                }
                MySQLColumnInformation[] columns = new MySQLColumnInformation[numColumns];
                if (numColumns > 0) {
                    for (int i = 0; i < numColumns; i++) {
                        columns[i] = new MySQLColumnInformation(packetFetcher.getRawPacket());
                    }
                    readEOFPacket();
                }
                
                return new PrepareResult(statementId,columns,params);
            } else {
                throw new QueryException("Unexpected packet returned by server, first byte " + b);
            }
        } catch (IOException e) {
            throw new QueryException(e.getMessage(), -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                   e);
        }
    }
    
    public synchronized void closePreparedStatement(int statementId) throws QueryException{
        try {
            writer.startPacket(0);
            writer.write(0x19); /*COM_STMT_CLOSE*/
            writer.write(statementId); 
            writer.finishPacket();
        } catch(IOException e) {
            throw new QueryException(e.getMessage(), -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                    e);
        }
    }
    public void setHostFailed() {
        hostFailed = true;
        failTimestamp = System.currentTimeMillis();
    }


    public boolean shouldReconnect() {
        return (!inTransaction() && hostFailed && autoReconnect && reconnectCount < maxReconnects);
    }

    public boolean getAutocommit() {
        return serverStatus.contains(ServerStatus.AUTOCOMMIT);
    }
    public void reconnectToMaster() throws IOException,QueryException {
        SyncPacketFetcher saveFetcher = this.packetFetcher;
        PacketOutputStream saveWriter = this.writer;
        Socket saveSocket = this.socket;
        HostAddress[] addrs = jdbcUrl.getHostAddresses();
        boolean success = false;
        try {
           connect(addrs[0].host, addrs[0].port);
           try {
            close(saveFetcher, saveWriter, saveSocket);
           } catch (Exception e) {
           }
           success = true;
        } finally {
            if (!success) {
                failTimestamp = System.currentTimeMillis();
                queriesSinceFailover = 0;
                this.packetFetcher = saveFetcher;
                this.writer = saveWriter;
                this.socket = saveSocket;
            }
        }
    }
    public void connect() throws QueryException {
        if (!isClosed()) {
            close();
        }

        HostAddress[] addrs = jdbcUrl.getHostAddresses();

        // There could be several addresses given in the URL spec, try all of them, and throw exception if all hosts
        // fail.
        for(int i = 0; i < addrs.length; i++) {
            currentHost = addrs[i];
            try {
                connect(currentHost.host, currentHost.port);
                return;
            } catch (IOException e) {
                if (i == addrs.length - 1) {
                    throw new QueryException("Could not connect to " + HostAddress.toString(addrs) +
                      " : " + e.getMessage(),  -1,  SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(), e);
                }
            }
        }
    }
    public boolean isMasterConnection() {
        return currentHost == jdbcUrl.getHostAddresses()[0];
    }

    /**
     * Check if fail back to master connection is desired,
     * @return
     */
    public boolean shouldTryFailback() {
        if (isMasterConnection())
            return false;

        if (inTransaction())
            return false;
        if (reconnectCount >= maxReconnects)
            return false;

        long now = System.currentTimeMillis();
        if ((now - failTimestamp)/1000 > secondsBeforeRetryMaster)
            return true;
        if (queriesSinceFailover > queriesBeforeRetryMaster)
            return true;
        return false;
    }

    public boolean inTransaction()
    {
        if(serverStatus != null)
            return serverStatus.contains(ServerStatus.IN_TRANSACTION);
        return false;
    }

    private void setDatatypeMappingFlags() {
        datatypeMappingFlags = 0;
        String tinyInt1isBit = info.getProperty("tinyInt1isBit");
        String yearIsDateType = info.getProperty("yearIsDateType");

        if (tinyInt1isBit == null || tinyInt1isBit.equals("1") || tinyInt1isBit.equals("true")) {
            datatypeMappingFlags |= MySQLValueObject.TINYINT1_IS_BIT;
        }
        if (yearIsDateType == null || yearIsDateType.equals("1") || yearIsDateType.equals("true")) {
            datatypeMappingFlags |= MySQLValueObject.YEAR_IS_DATE_TYPE;
        }
    }

    public Properties getInfo() {
        return info;
    }
    void skip() throws IOException, QueryException{
         if (activeResult != null) {
             activeResult.close();
         }

         while (moreResults) {
            getMoreResults(true);
         }

    }

    public boolean  hasMoreResults() {
        return moreResults;
    }


    private static void close(PacketFetcher fetcher, PacketOutputStream packetOutputStream, Socket socket)
            throws QueryException
    {
        ClosePacket closePacket = new ClosePacket();
        try {
            closePacket.send(packetOutputStream);
            try {
                socket.shutdownOutput();
                socket.setSoTimeout(3);
                InputStream is = socket.getInputStream();
                while(is.read() != -1) {}
            } catch (Throwable t) {
            }
            packetOutputStream.close();
            fetcher.close();
        } catch (IOException e) {
                 throw new QueryException("Could not close connection: " + e.getMessage(),
                         -1,
                         SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                         e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.warning("Could not close socket");
            }
        }
    }
    /**
     * Closes socket and stream readers/writers
     * Attempts graceful shutdown.
     * @throws org.properssl.sslcertx.mariadb.jdbc.internal.common.QueryException
     *          if the socket or readers/writes cannot be closed
     */
    public void close()  {
        try {
            /* If a streaming result set is open, close it.*/ 
            skip();
        } catch (Exception e) {
            /* eat exception */
        }
        try {
           close(packetFetcher,writer, socket);
        }
        catch (Exception e) {
            // socket is closed, so it is ok to ignore exception
            log.info("got exception " + e + " while closing connection");
        }
        finally {
            this.connected = false;
        }
    }

    /**
     * @return true if the connection is closed
     */
    public boolean isClosed() {
        return !this.connected;
    }

    /**
     * create a CachedSelectResult - precondition is that a result set packet has been read
     *
     * @param packet the result set packet from the server
     * @return a CachedSelectResult
     * @throws java.io.IOException when something goes wrong while reading/writing from the server
     */
    private SelectQueryResult createQueryResult(final ResultSetPacket packet, boolean streaming) throws IOException, QueryException {

        StreamingSelectResult streamingResult =   StreamingSelectResult.createStreamingSelectResult(packet, packetFetcher, this);
        if (streaming)
            return streamingResult;

        return CachedSelectResult.createCachedSelectResult(streamingResult);
    }

    public void selectDB(final String database) throws QueryException {
        log.finest("Selecting db " + database);
        final SelectDBPacket packet = new SelectDBPacket(database);
        try {
            packet.send(writer);
            final RawPacket rawPacket = packetFetcher.getRawPacket();
            ResultPacketFactory.createResultPacket(rawPacket);
        } catch (IOException e) {
            throw new QueryException("Could not select database: " + e.getMessage(),
                    -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                    e);
        }
        this.database = database;
    }

    public String getServerVersion() {
        return version;
    }

    public void setReadonly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean getReadonly() {
        return readOnly;
    }


    public String getHost() {
        return currentHost.host;
    }

    public int getPort() {
        return currentHost.port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean ping() throws QueryException {
        final MySQLPingPacket pingPacket = new MySQLPingPacket();
        try {
            pingPacket.send(writer);
            log.finest("Sent ping packet");
            final RawPacket rawPacket = packetFetcher.getRawPacket();
            return ResultPacketFactory.createResultPacket(rawPacket).getResultType() == ResultPacket.ResultType.OK;
        } catch (IOException e) {
            throw new QueryException("Could not ping: " + e.getMessage(),
                    -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                    e);
        }
    }

    public QueryResult executeQuery(Query dQuery)  throws QueryException{
       return executeQuery(dQuery, false);
    }


    public QueryResult getResult(Query dQuery, boolean streaming) throws QueryException{
             RawPacket rawPacket;
        ResultPacket resultPacket;
        try {
            rawPacket = packetFetcher.getRawPacket();
            resultPacket = ResultPacketFactory.createResultPacket(rawPacket);

            if (resultPacket.getResultType() == ResultPacket.ResultType.LOCALINFILE) {
                // Server request the local file (LOCAL DATA LOCAL INFILE)
                // We do accept general URLs, too

                LocalInfilePacket localInfilePacket= (LocalInfilePacket)resultPacket;
                log.fine("sending local file " + localInfilePacket.getFileName());
                String localInfile = localInfilePacket.getFileName();

                InputStream is;
                try {
                    URL u = new URL(localInfile);
                    is = u.openStream();
                } catch (IOException ioe)   {
                    is = new FileInputStream(localInfile);
                }

                writer.sendFile(is, rawPacket.getPacketSeq()+1);
                is.close();
                rawPacket = packetFetcher.getRawPacket();
                resultPacket = ResultPacketFactory.createResultPacket(rawPacket);
            }
        }
        catch (IOException e) {
            throw new QueryException("Could not read resultset: " + e.getMessage(),
                    -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                    e);
        }

        switch (resultPacket.getResultType()) {
            case ERROR:
                this.moreResults = false;
                this.hasWarnings = false;
                ErrorPacket ep = (ErrorPacket) resultPacket;
                if (dQuery != null) {
                    log.warning("Could not execute query " + dQuery + ": " + ((ErrorPacket) resultPacket).getMessage());
                } else {
                    log.warning("Got error from server: " + ((ErrorPacket) resultPacket).getMessage());
                }
                throw new QueryException(ep.getMessage(), ep.getErrorNumber(),  ep.getSqlState());

            case OK:
                final OKPacket okpacket = (OKPacket) resultPacket;
                serverStatus = okpacket.getServerStatus();
                this.moreResults = serverStatus.contains(ServerStatus.MORE_RESULTS_EXISTS);
                this.hasWarnings = (okpacket.getWarnings() > 0);
                final QueryResult updateResult = new UpdateResult(okpacket.getAffectedRows(),
                        okpacket.getWarnings(),
                        okpacket.getMessage(),
                        okpacket.getInsertId());
                log.fine("OK, " + okpacket.getAffectedRows());
                return updateResult;
            case RESULTSET:
                this.hasWarnings = false;
                log.fine("SELECT executed, fetching result set");
                ResultSetPacket resultSetPacket = (ResultSetPacket)resultPacket;
                try {
                    return this.createQueryResult(resultSetPacket, streaming);
                } catch (IOException e) {

                    throw new QueryException("Could not read result set: " + e.getMessage(),
                            -1,
                            SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                            e);
                }
            default:
                log.severe("Could not parse result..." + resultPacket.getResultType());
                throw new QueryException("Could not parse result", (short) -1, SQLExceptionMapper.SQLStates.INTERRUPTED_EXCEPTION.getSqlState());
        }
    }

    public QueryResult executeQuery(final Query dQuery, boolean streaming) throws QueryException
    {
        dQuery.validate();
        log.log(Level.FINEST, "Executing streamed query: {0}", dQuery);
        this.moreResults = false;
        final StreamedQueryPacket packet = new StreamedQueryPacket(dQuery);

        try {
            packet.send(writer);
        } catch (IOException e) {
            throw new QueryException("Could not send query: " + e.getMessage(),
                    -1,
                    SQLExceptionMapper.SQLStates.CONNECTION_EXCEPTION.getSqlState(),
                    e);
        }
        if (!isMasterConnection())
            queriesSinceFailover++;
        return getResult(dQuery, streaming);
    }


    public List<RawPacket> startBinlogDump(final int startPos, final String filename) throws BinlogDumpException {
        final MySQLBinlogDumpPacket mbdp = new MySQLBinlogDumpPacket(startPos, filename);
        try {
            mbdp.send(writer);
            final List<RawPacket> rpList = new LinkedList<RawPacket>();
            while (true) {
                final RawPacket rp = this.packetFetcher.getRawPacket();
                if (ReadUtil.eofIsNext(rp)) {
                    return rpList;
                }
                rpList.add(rp);
            }
        } catch (IOException e) {
            throw new BinlogDumpException("Could not read binlog", e);
        }
    }



    public String getServerVariable(String variable) throws QueryException {
        CachedSelectResult qr = (CachedSelectResult) executeQuery(new MySQLQuery("select @@" + variable));
        try {
            if (!qr.next()) {
                throw new QueryException("Could not get variable: " + variable);
            }
        }
        catch (IOException ioe ){
            throw new QueryException(ioe.getMessage(), 0, "HYOOO", ioe);
        }


        try {
            String value = qr.getValueObject(0).getString();
            return value;
        } catch (NoSuchColumnException e) {
            throw new QueryException("Could not get variable: " + variable);
        }
    }


    /**
     * cancels the current query - clones the current protocol and executes a query using the new connection
     * <p/>
     * thread safe
     *
     * @throws QueryException
     */
    public  void cancelCurrentQuery() throws QueryException, IOException {
        MySQLProtocol copiedProtocol = new MySQLProtocol(jdbcUrl, username, password, info);
        copiedProtocol.executeQuery(new MySQLQuery("KILL QUERY " + serverThreadId));
        copiedProtocol.close();
    }

    public boolean createDB() {
    	String alias = info.getProperty("createDatabaseIfNotExist");
        return info != null
                && (info.getProperty("createDB", "").equalsIgnoreCase("true")
                		|| (alias != null && alias.equalsIgnoreCase("true")));
    }



    public QueryResult getMoreResults(boolean streaming) throws QueryException {
        if(!moreResults)
            return null;
        return getResult(null, streaming);
    }

    public static String hexdump(byte[] buffer, int offset) {
        StringBuffer dump = new StringBuffer();
        if ((buffer.length - offset) > 0) {
            dump.append(String.format("%02x", buffer[offset]));
            for (int i = offset + 1; i < buffer.length; i++) {
                dump.append(String.format("%02x", buffer[i]));
            }
        }
        return dump.toString();
    }

    public static String hexdump(ByteBuffer bb, int offset) {
        byte[] b = new byte[bb.remaining()];
        bb.mark();
        bb.get(b);
        bb.reset();
        return hexdump(b, offset);
    }


    public boolean hasUnreadData() {
        return (activeResult != null);
    }

    public void setMaxRows(int max) throws QueryException{
        if (maxRows != max) {
            if (max == 0) {
                executeQuery(new MySQLQuery("set @@SQL_SELECT_LIMIT=DEFAULT"));
            } else {
                executeQuery(new MySQLQuery("set @@SQL_SELECT_LIMIT=" + max));
            }
            maxRows = max;
        }
    }
    
    void parseVersion() {
    	String[] a = version.split("[^0-9]");
    	if (a.length > 0)
    		majorVersion = Integer.parseInt(a[0]);
    	if (a.length > 1)
    		minorVersion = Integer.parseInt(a[1]);
    	if (a.length > 2)
    		patchVersion = Integer.parseInt(a[2]);
    }
    
    public int getMajorServerVersion() {
    	return majorVersion;
    		
    }
    public int getMinorServerVersion() {
    	return minorVersion;
    }
    
    public boolean versionGreaterOrEqual(int major, int minor, int patch) {
    	if (this.majorVersion > major)
    		return true;
    	if (this.majorVersion < major)
    		return false;
    	/*
    	 * Major versions are equal, compare minor versions
    	 */
    	if (this.minorVersion > minor)
    		return true;
    	if (this.minorVersion < minor)
    		return false;
    	
    	/*
    	 * Minor versions are equal, compare patch version
    	 */
    	if (this.patchVersion > patch)
    		return true;
    	if (this.patchVersion < patch)
    		return false;
    	
    	/* Patch versions are equal => versions are equal */
    	return true;
    }
    
}
