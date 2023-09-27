/*
 * Copyright 2019 Thunderberry.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.altindag.ssl.trustmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class HotSwappableX509ExtendedTrustManagerShould {

    @Test
    void checkClientTrusted() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkClientTrusted(null, null);

        verify(trustManager, times(1)).checkClientTrusted(null, null);
    }

    @Test
    void checkClientTrustedWithSocket() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkClientTrusted(null, null, (Socket) null);

        verify(trustManager, times(1)).checkClientTrusted(null, null, (Socket) null);
    }

    @Test
    void checkClientTrustedWithSslEngine() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkClientTrusted(null, null, (SSLEngine) null);

        verify(trustManager, times(1)).checkClientTrusted(null, null, (SSLEngine) null);
    }

    @Test
    void checkServerTrusted() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkServerTrusted(null, null);

        verify(trustManager, times(1)).checkServerTrusted(null, null);
    }

    @Test
    void checkServerTrustedWithSocket() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkServerTrusted(null, null, (Socket) null);

        verify(trustManager, times(1)).checkServerTrusted(null, null, (Socket) null);
    }

    @Test
    void checkServerTrustedWithSslEngine() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkServerTrusted(null, null, (SSLEngine) null);

        verify(trustManager, times(1)).checkServerTrusted(null, null, (SSLEngine) null);
    }

    @Test
    void getAcceptedIssuers() {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        when(trustManager.getAcceptedIssuers()).thenReturn(new X509Certificate[]{mock(X509Certificate.class)});

        X509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        X509Certificate[] acceptedIssuers = victim.getAcceptedIssuers();

        assertThat(acceptedIssuers).hasSize(1);
        verify(trustManager, times(1)).getAcceptedIssuers();
    }

    @Test
    void setTrustManager() throws CertificateException {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);

        HotSwappableX509ExtendedTrustManager victim = new HotSwappableX509ExtendedTrustManager(trustManager);
        victim.checkServerTrusted(null, null, (SSLEngine) null);

        victim.setTrustManager(mock(X509ExtendedTrustManager.class));
        victim.checkServerTrusted(null, null, (SSLEngine) null);

        verify(trustManager, times(1)).checkServerTrusted(null, null, (SSLEngine) null);
    }

    @Test
    void throwExceptionWhenKeyManagerIsNotPresent() {
        assertThatThrownBy(() -> new HotSwappableX509ExtendedTrustManager(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No valid TrustManager has been provided. TrustManager must be present, but was absent.");
    }

}
