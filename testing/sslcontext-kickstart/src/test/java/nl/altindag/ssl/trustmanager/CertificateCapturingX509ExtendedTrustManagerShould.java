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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class CertificateCapturingX509ExtendedTrustManagerShould {

    @Test
    void checkClientTrusted() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkClientTrusted(chain, null);

        verify(trustManager, times(1)).checkClientTrusted(chain, null);
        assertThat(certificatesCollector).contains(certificate);
    }

    @Test
    void checkClientTrustedWithSocket() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkClientTrusted(chain, null, (Socket) null);

        verify(trustManager, times(1)).checkClientTrusted(chain, null, (Socket) null);
        assertThat(certificatesCollector).contains(certificate);
    }

    @Test
    void checkClientTrustedWithSslEngine() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkClientTrusted(chain, null, (SSLEngine) null);

        verify(trustManager, times(1)).checkClientTrusted(chain, null, (SSLEngine) null);
        assertThat(certificatesCollector).contains(certificate);
    }

    @Test
    void checkServerTrusted() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkServerTrusted(chain, null);

        verify(trustManager, times(1)).checkServerTrusted(chain, null);
        assertThat(certificatesCollector).contains(certificate);
    }

    @Test
    void checkServerTrustedWithSocket() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkServerTrusted(chain, null, (Socket) null);

        verify(trustManager, times(1)).checkServerTrusted(chain, null, (Socket) null);
        assertThat(certificatesCollector).contains(certificate);
    }

    @Test
    void checkServerTrustedWithSslEngine() throws CertificateException {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[]{certificate};
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        List<X509Certificate> certificatesCollector = new ArrayList<>();

        CertificateCapturingX509ExtendedTrustManager victim = new CertificateCapturingX509ExtendedTrustManager(trustManager, certificatesCollector);
        victim.checkServerTrusted(chain, null, (SSLEngine) null);

        verify(trustManager, times(1)).checkServerTrusted(chain, null, (SSLEngine) null);
        assertThat(certificatesCollector).contains(certificate);
    }

}
