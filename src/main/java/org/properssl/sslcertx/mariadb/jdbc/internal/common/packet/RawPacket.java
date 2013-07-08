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

Copyright (c) 2009-2011, Marcus Eriksson, Trond Norbye

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

package org.properssl.sslcertx.mariadb.jdbc.internal.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.properssl.sslcertx.mariadb.jdbc.internal.common.packet.buffer.ReadUtil;


/**
 * Class to represent a raw packet as transferred over the wire. First we got 3 bytes specifying the actual length, then
 * one byte packet sequence number and then n bytes with user data.
 */
public final class RawPacket {
    private final ByteBuffer byteBuffer;
    private final int packetSeq;

    /**
     * Get the next packet from the stream
     *
     * @param is the input stream to read the next packet from
     * @return The next packet from the stream, or NULL if the stream is closed
     * @throws java.io.IOException if an error occurs while reading data
     */
    static RawPacket nextPacket(final InputStream is) throws IOException {
        byte[] lengthBuffer = new byte[4];
        ReadUtil.readFully(is, lengthBuffer);
        int length = (lengthBuffer[0] & 0xff) + ((lengthBuffer[1] & 0xff) << 8) + ((lengthBuffer[2] & 0xff) << 16);
        int packetSeq = lengthBuffer[3];

        byte [] rawBytes = new byte[length];
        ReadUtil.readFully(is, rawBytes);
        return new RawPacket(ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN),
                             packetSeq);
    }

    /**
     * create a raw packet.
     * @param byteBuffer the byte buffer containing the packet
     * @param packetSeq the packet sequence
     */
    public  RawPacket(final ByteBuffer byteBuffer, final int packetSeq) {
        this.byteBuffer = byteBuffer;
        this.packetSeq = packetSeq;
    }

    /**
     * Get the byte buffer backing this packet
     *
     * @return a read only byte buffer
     */
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Get the package sequence number
     *
     * @return the sequence number of the package
     */
    public int getPacketSeq() {
        return packetSeq;
    }
}
