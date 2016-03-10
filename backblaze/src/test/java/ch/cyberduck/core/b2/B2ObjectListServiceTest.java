package ch.cyberduck.core.b2;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.http.ResponseOutputStream;
import ch.cyberduck.core.io.Checksum;
import ch.cyberduck.core.io.SHA1ChecksumCompute;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import synapticloop.b2.response.B2FileResponse;

import static org.junit.Assert.*;

public class B2ObjectListServiceTest {

    @Test
    public void testList() throws Exception {
        final B2Session session = new B2Session(
                new Host(new B2Protocol(), new B2Protocol().getDefaultHostname(),
                        new Credentials(
                                System.getProperties().getProperty("b2.user"), System.getProperties().getProperty("b2.key")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path bucket = new Path("test-cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path file = new Path(bucket, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final TransferStatus status = new TransferStatus();
        status.setChecksum(Checksum.parse("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
        final ResponseOutputStream<B2FileResponse> out = new B2WriteFeature(session).write(file, status);
        IOUtils.write(new byte[0], out);
        out.close();
        final B2FileResponse resopnse = out.getResponse();
        final List<Path> list = new B2ObjectListService(session).list(bucket, new DisabledListProgressListener());
        // Not found with missing version ID
        assertFalse(list.contains(file));
        file.attributes().setVersionId(resopnse.getFileId());
        assertTrue(list.contains(file));
        assertEquals("1", list.get(list.indexOf(file)).attributes().getRevision());
        assertEquals(0L, list.get(list.indexOf(file)).attributes().getSize());

        new B2DeleteFeature(session).delete(Collections.singletonList(file), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
                //
            }
        });
        assertFalse(new B2ObjectListService(session).list(bucket, new DisabledListProgressListener()).contains(file));
    }

    @Test
    public void testListRevisions() throws Exception {
        final B2Session session = new B2Session(
                new Host(new B2Protocol(), new B2Protocol().getDefaultHostname(),
                        new Credentials(
                                System.getProperties().getProperty("b2.user"), System.getProperties().getProperty("b2.key")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path bucket = new Path("test-cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final String name = UUID.randomUUID().toString();
        final Path file1 = new Path(bucket, name, EnumSet.of(Path.Type.file));
        final Path file2 = new Path(bucket, name, EnumSet.of(Path.Type.file));
        {
            final byte[] content = RandomUtils.nextBytes(1);
            final TransferStatus status = new TransferStatus();
            status.setLength(content.length);
            status.setChecksum(new SHA1ChecksumCompute().compute(new ByteArrayInputStream(content)));
            final ResponseOutputStream<B2FileResponse> out = new B2WriteFeature(session).write(file1, status);
            IOUtils.write(content, out);
            out.close();
            final B2FileResponse resopnse = out.getResponse();
            final List<Path> list = new B2ObjectListService(session).list(bucket, new DisabledListProgressListener());
            file1.attributes().setVersionId(resopnse.getFileId());
            assertTrue(list.contains(file1));
            assertEquals("1", list.get(list.indexOf(file1)).attributes().getRevision());
            assertEquals(content.length, list.get(list.indexOf(file1)).attributes().getSize());
        }
        // Replace
        {
            final byte[] content = RandomUtils.nextBytes(1);
            final TransferStatus status = new TransferStatus();
            status.setLength(content.length);
            status.setChecksum(new SHA1ChecksumCompute().compute(new ByteArrayInputStream(content)));
            final ResponseOutputStream<B2FileResponse> out = new B2WriteFeature(session).write(file2, status);
            IOUtils.write(content, out);
            out.close();
            final B2FileResponse resopnse = out.getResponse();
            final List<Path> list = new B2ObjectListService(session).list(bucket, new DisabledListProgressListener());
            file2.attributes().setVersionId(resopnse.getFileId());
            assertTrue(list.contains(file2));
//            assertEquals("2", list.get(list.indexOf(file2)).attributes().getRevision());
            assertFalse(list.get(list.indexOf(file2)).attributes().isDuplicate());
            assertTrue(list.contains(file1));
//            assertEquals("1", list.get(list.indexOf(file1)).attributes().getRevision());
            assertTrue(list.get(list.indexOf(file1)).attributes().isDuplicate());
        }

        new B2DeleteFeature(session).delete(Arrays.asList(file1, file2), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
                //
            }
        });
        {
            final AttributedList<Path> list = new B2ObjectListService(session).list(bucket, new DisabledListProgressListener());
            assertFalse(list.contains(file1));
            assertFalse(list.contains(file2));
        }
    }
}