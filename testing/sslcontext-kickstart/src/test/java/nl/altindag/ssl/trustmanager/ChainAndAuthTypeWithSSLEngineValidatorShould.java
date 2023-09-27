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

import nl.altindag.ssl.util.KeyStoreUtils;
import nl.altindag.ssl.util.TrustManagerUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class ChainAndAuthTypeWithSSLEngineValidatorShould {

    private static final String TRUSTSTORE_FILE_NAME = "truststore.jks";
    private static final char[] TRUSTSTORE_PASSWORD = new char[] {'s', 'e', 'c', 'r', 'e', 't'};
    private static final String KEYSTORE_LOCATION = "keystore/";

    @Test
    void test() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore);
        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();

        ChainAndAuthTypeWithSSLEngineValidator validator = (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Google");

        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isTrue();
    }

    @Test
    void and() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore);
        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();

        ChainAndAuthTypeWithSSLEngineValidator validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Google"))
                .and((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Mountain View"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isTrue();

        validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Google"))
                .and((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Mountain Blue View"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isFalse();

        validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Amazon"))
                .and((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Mountain Blue View"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isFalse();
    }

    @Test
    void or() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore);
        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();

        ChainAndAuthTypeWithSSLEngineValidator validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Google"))
                .or((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Donald"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isTrue();

        validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Donald"))
                .or((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Google"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isTrue();

        validator = ((ChainAndAuthTypeWithSSLEngineValidator) (X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Donald"))
                .or((X509Certificate[] certificateChain, String authType, SSLEngine sslEngine) -> certificateChain[0].getSubjectX500Principal().getName().contains("Quack"));
        assertThat(validator.test(acceptedIssuers, "RSA", mock(SSLEngine.class))).isFalse();
    }

}
