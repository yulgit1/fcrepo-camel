/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Aaron Coburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FcrepoTransactionManagerTest {

    @Mock
    private FcrepoClient mockClient;

    @Test
    public void testProperties() {
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        final String baseUrl = "http://localhost:8080/rest";
        final String authUsername = "foo";
        final String authPassword = "bar";
        final String authHost = "baz";
        final String transactionId = "1234567890";

        assertNull(txMgr.getAuthUsername());
        assertNull(txMgr.getAuthPassword());
        assertNull(txMgr.getAuthHost());
        assertNull(txMgr.getBaseUrl());

        txMgr.setBaseUrl(baseUrl);
        txMgr.setAuthUsername(authUsername);
        txMgr.setAuthPassword(authPassword);
        txMgr.setAuthHost(authHost);

        assertEquals(baseUrl, txMgr.getBaseUrl());
        assertEquals(authUsername, txMgr.getAuthUsername());
        assertEquals(authPassword, txMgr.getAuthPassword());
        assertEquals(authHost, txMgr.getAuthHost());
    }

    @Test
    public void testTransactionCommit() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI commitUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:commit");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(mockClient.post(eq(commitUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(commitUri, 201, null, null, null));

        DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();

        assertEquals(tx, txObj.getSessionId());
        assertFalse(status.isCompleted());

        status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        txMgr.commit(status);

        txObj = (FcrepoTransactionObject)status.getTransaction();

        assertNull(txObj.getSessionId());
        assertTrue(status.isCompleted());
    }

    @Test
    public void testTransactionRollback() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI commitUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:commit");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(mockClient.post(eq(commitUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(commitUri, 201, null, null, null));

        DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();

        assertEquals(tx, txObj.getSessionId());
        assertFalse(status.isCompleted());

        status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        txMgr.rollback(status);

        txObj = (FcrepoTransactionObject)status.getTransaction();

        assertNull(txObj.getSessionId());
        assertTrue(status.isCompleted());
    }

    @Test (expected = CannotCreateTransactionException.class)
    public void testTransactionBeginError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenThrow(
                new FcrepoOperationFailedException(beginUri, 400, "Bad Request"));

        txMgr.getTransaction(txDef);
    }

    @Test (expected = CannotCreateTransactionException.class)
    public void testTransactionBeginNoLocationError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, null, null));

        txMgr.getTransaction(txDef);
    }

    @Test (expected = CannotCreateTransactionException.class)
    public void testTransactionNullResponseError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(null);

        txMgr.getTransaction(txDef);
    }

    @Test (expected = TransactionSystemException.class)
    public void testTransactionCommitError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI commitUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:commit");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(mockClient.post(eq(commitUri), any(InputStream.class), anyString())).thenThrow(
                new FcrepoOperationFailedException(commitUri, 400, "Bad Request"));

        DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();
        assertEquals(tx, txObj.getSessionId());
        assertFalse(status.isCompleted());

        status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        txMgr.commit(status);
    }

    @Test (expected = TransactionSystemException.class)
    public void testTransactionRollbackError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI rollbackUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:rollback");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "client", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(mockClient.post(eq(rollbackUri), any(InputStream.class), anyString())).thenThrow(
                new FcrepoOperationFailedException(rollbackUri, 400, "Bad Request"));

        DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();
        assertEquals(tx, txObj.getSessionId());
        assertFalse(status.isCompleted());

        status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        txMgr.rollback(status);
    }
}
