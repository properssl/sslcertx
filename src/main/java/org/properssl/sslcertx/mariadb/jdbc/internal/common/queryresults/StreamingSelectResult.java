package org.properssl.sslcertx.mariadb.jdbc.internal.common.queryresults;

import java.io.IOException;

import org.properssl.sslcertx.mariadb.jdbc.internal.common.ColumnInformation;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.PacketFetcher;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.QueryException;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.ServerStatus;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.ValueObject;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.EOFPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ErrorPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.RawPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ResultPacketFactory;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.ResultSetPacket;
import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.buffer.ReadUtil;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.MySQLColumnInformation;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.MySQLProtocol;
import org.properssl.sslcertx.mariadb.jdbc.internal.mysql.packet.MySQLRowPacket;

public class StreamingSelectResult extends SelectQueryResult {
    PacketFetcher packetFetcher;
    public ValueObject[] values;
    MySQLProtocol protocol;
    boolean isEOF;
    boolean beforeFirst;


    private StreamingSelectResult(ColumnInformation[] info, MySQLProtocol protocol, PacketFetcher fetcher) throws QueryException {
        this.columnInformation = info;
        this.protocol = protocol;
        this.packetFetcher = fetcher;
        this.beforeFirst = true;
        this.isEOF = false;

        protocol.activeResult = this;
    }
     /**
     * create StreamingResultSet - precondition is that a result set packet has been read
     *
     * @param packet the result set packet from the server
     * @return a StreamingQueryResult
     * @throws java.io.IOException when something goes wrong while reading/writing from the server
     */
    public static StreamingSelectResult createStreamingSelectResult(
            ResultSetPacket packet, PacketFetcher packetFetcher, MySQLProtocol protocol)
            throws IOException, QueryException {

        if (protocol.activeResult != null) {
            throw new  QueryException("There is an active result set on the current connection, "+
                    "which must be closed prior to opening a new one");
        }
        long fieldCount = packet.getFieldCount();
        ColumnInformation[] ci = new ColumnInformation[(int)fieldCount];
        
        for (int i = 0; i < fieldCount; i++) {
            final RawPacket rawPacket = packetFetcher.getRawPacket();

            // We do not expect an error packet, but check it just for safety
            if (ReadUtil.isErrorPacket(rawPacket)) {
                ErrorPacket errorPacket = new ErrorPacket(rawPacket);
                throw new QueryException("error when reading field packet " + errorPacket.getMessage(),
                        errorPacket.getErrorNumber(), errorPacket.getSqlState());
            }
            // We do not expect OK or EOF packets either
            byte b = rawPacket.getByteBuffer().get(0);
            if (b == 0 || b == (byte)0xfe) {
                throw new QueryException("Packets out of order when trying to read field packet - " +
                    "got packet starting with byte " + b + "packet content (hex) = "
                        + MySQLProtocol.hexdump(rawPacket.getByteBuffer(), 0));
            }
            
            try {
                ci[i] = new MySQLColumnInformation(rawPacket);
            } catch (Exception e) {
                throw new QueryException("Error when trying to parse field packet : " + e + ",packet content (hex) = " +
                        MySQLProtocol.hexdump(rawPacket.getByteBuffer(), 0) , 0, "HY000", e);
            }
        }
        RawPacket fieldEOF = packetFetcher.getRawPacket();
        if (!ReadUtil.eofIsNext(fieldEOF)) {
            throw new QueryException("Packets out of order when reading field packets, expected was EOF packet. " +
                    "Packet contents (hex) = " + MySQLProtocol.hexdump(fieldEOF.getByteBuffer(),0));
        }
        return new StreamingSelectResult(ci, protocol, packetFetcher);

    }

    @Override
    public boolean next() throws IOException,QueryException{
       if (isEOF)
            return false;

            RawPacket rawPacket = packetFetcher.getRawPacket();

            if (ReadUtil.isErrorPacket(rawPacket)) {
                protocol.activeResult = null;
                protocol.moreResults = false;
                ErrorPacket errorPacket = (ErrorPacket) ResultPacketFactory.createResultPacket(rawPacket);
                throw new QueryException(errorPacket.getMessage(), errorPacket.getErrorNumber(), errorPacket.getSqlState());
            }

            if (ReadUtil.eofIsNext(rawPacket)) {
                final EOFPacket eofPacket = (EOFPacket) ResultPacketFactory.createResultPacket(rawPacket);
                protocol.activeResult = null;
                protocol.moreResults = eofPacket.getStatusFlags().contains(ServerStatus.MORE_RESULTS_EXISTS);
                warningCount = eofPacket.getWarningCount();
                protocol.hasWarnings = (warningCount > 0);
                isEOF = true;
                values = null;
                return false;
            }


           MySQLRowPacket rowPacket = new MySQLRowPacket(rawPacket, columnInformation);
           values = rowPacket.getRow(packetFetcher);

           return true;

    }

    public void close() {
        super.close();
        if (protocol != null && protocol.activeResult == this)
        {
            try {
                for (;;) {
                    try {
                        if(protocol.activeResult == null) {
                            return;
                        }
                        if (!next())
                            return;
                    }
                    catch (QueryException qe) {
                        return;
                    }
                    catch (IOException ioe) {
                        return;
                    }
                }
            }   finally   {
                   protocol.activeResult = null;
                   protocol = null;
                   packetFetcher = null;
            }
        }
    }

    /**
     * gets the value at position i in the result set. i starts at zero!
     *
     * @param i index, starts at 0
     * @return
     */
    @Override
    public ValueObject getValueObject(int i) throws NoSuchColumnException {
        return values[i];
    }

    public int getRows() {
        return -1;
    }

    public boolean isBeforeFirst() {
        return beforeFirst;
    }
    public boolean  isAfterLast()  {
        return isEOF;
    }
}
