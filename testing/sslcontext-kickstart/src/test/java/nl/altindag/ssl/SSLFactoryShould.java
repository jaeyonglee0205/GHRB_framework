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
package nl.altindag.ssl;

import nl.altindag.log.LogCaptor;
import nl.altindag.ssl.exception.GenericKeyManagerException;
import nl.altindag.ssl.exception.GenericKeyStoreException;
import nl.altindag.ssl.exception.GenericSecurityException;
import nl.altindag.ssl.exception.GenericTrustManagerException;
import nl.altindag.ssl.hostnameverifier.FenixHostnameVerifier;
import nl.altindag.ssl.keymanager.CompositeX509ExtendedKeyManager;
import nl.altindag.ssl.keymanager.DummyX509ExtendedKeyManager;
import nl.altindag.ssl.keymanager.HotSwappableX509ExtendedKeyManager;
import nl.altindag.ssl.trustmanager.DummyX509ExtendedTrustManager;
import nl.altindag.ssl.trustmanager.EnhanceableX509ExtendedTrustManager;
import nl.altindag.ssl.trustmanager.HotSwappableX509ExtendedTrustManager;
import nl.altindag.ssl.trustmanager.TrustAnchorTrustOptions;
import nl.altindag.ssl.trustmanager.TrustStoreTrustOptions;
import nl.altindag.ssl.trustmanager.UnsafeX509ExtendedTrustManager;
import nl.altindag.ssl.util.KeyManagerUtils;
import nl.altindag.ssl.util.KeyStoreUtils;
import nl.altindag.ssl.util.TrustManagerUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathBuilder;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nl.altindag.ssl.TestConstants.EMPTY;
import static nl.altindag.ssl.TestConstants.IDENTITY_FILE_NAME;
import static nl.altindag.ssl.TestConstants.IDENTITY_PASSWORD;
import static nl.altindag.ssl.TestConstants.KEYSTORE_LOCATION;
import static nl.altindag.ssl.TestConstants.TRUSTSTORE_FILE_NAME;
import static nl.altindag.ssl.TestConstants.TRUSTSTORE_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class SSLFactoryShould {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLFactoryShould.class);

    private static final String GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE = "Identity details are empty, which are required to be present when SSL/TLS is enabled";
    private static final String GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE = "TrustStore details are empty, which are required to be present when SSL/TLS is enabled";

    @Test
    void buildSSLFactoryWithTrustMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isNotInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialAndTrustOptions() throws NoSuchAlgorithmException {
        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD, trustStore -> {
                    PKIXBuilderParameters pkixBuilderParameters = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixBuilderParameters.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixBuilderParameters);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isNotInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithSwappableTrustMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withSwappableTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialWithoutPassword() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + "truststore-without-password.jks", null)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialWithoutPasswordAndWithTrustOptions() throws NoSuchAlgorithmException {
        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + "truststore-without-password.jks", null, trustStore -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromPath() throws IOException {
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();

        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromPathWithTrustOptions() throws IOException, NoSuchAlgorithmException {
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD, trustStore -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();

        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromInputStream() {
        InputStream trustStoreStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStoreStream, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromInputStreamWithTrustOptions() throws NoSuchAlgorithmException {
        InputStream trustStoreStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME);

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStoreStream, TRUSTSTORE_PASSWORD, trustStore -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromKeyStore() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStore)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromKeyStoreWithoutAdditionalPassword() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStore)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromKeyStoreWithTrustOptions() throws NoSuchAlgorithmException {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustStore, t -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(t, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromTrustManager() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustManager)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromTrustManagerWithOptions() throws NoSuchAlgorithmException {
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(
                KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
        );

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustManager, trustStore -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(trustManagerFactory)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromCertificates() {
        X509Certificate[] certificates = TrustManagerUtils.createTrustManagerWithJdkTrustedCertificates()
                .getAcceptedIssuers();

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(certificates)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromCertificatesWithTrustOptions() throws NoSuchAlgorithmException {
        X509Certificate[] certificates = TrustManagerUtils.createTrustManagerWithJdkTrustedCertificates()
                .getAcceptedIssuers();

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(certificates, trustStore -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromCertificatesWithTrustAnchorTrustOptions() throws NoSuchAlgorithmException {
        Set<X509Certificate> certificates = new HashSet<>(Arrays.asList(TrustManagerUtils.createTrustManagerWithJdkTrustedCertificates().getAcceptedIssuers()));

        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathBuilder.getRevocationChecker();
        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(certificates, trustAnchors -> {
                    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());
                    pkixParams.addCertPathChecker(revocationChecker);
                    return new CertPathTrustManagerParameters(pkixParams);
                })
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromOnlyJdkTrustedCertificates() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromOnlySystemTrustedCertificates() {
        String operatingSystem = System.getProperty("os.name").toLowerCase();
        if (operatingSystem.contains("mac") || operatingSystem.contains("windows")) {
            try (MockedStatic<KeyStoreUtils> mockedStatic = mockStatic(KeyStoreUtils.class, invocation -> {
                Method method = invocation.getMethod();
                if ("createKeyStore".equals(method.getName())
                        && method.getParameterCount() == 2
                        && operatingSystem.contains("mac")) {
                    return KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
                } else {
                    return invocation.callRealMethod();
                }
            })) {
                SSLFactory sslFactory = SSLFactory.builder()
                        .withSystemTrustMaterial()
                        .build();

                assertThat(sslFactory.getSslContext()).isNotNull();

                assertThat(sslFactory.getTrustManager()).isPresent();
                assertThat(sslFactory.getTrustManagerFactory()).isPresent();

                assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
                assertThat(sslFactory.getKeyManager()).isNotPresent();
                assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
            }
        }

        if (operatingSystem.contains("linux")) {
            SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder()
                    .withSystemTrustMaterial();

            assertThatThrownBy(sslFactoryBuilder::build)
                    .isInstanceOf(GenericSecurityException.class)
                    .hasMessage("Could not create instance of SSLFactory because Identity and Trust material are not present. Please provide at least a Trust material.");
        }
    }

    @Test
    void buildSSLFactoryWithSecureRandom() throws NoSuchAlgorithmException {
        SSLFactory sslFactory = SSLFactory.builder()
                .withSecureRandom(SecureRandom.getInstanceStrong())
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustMaterialFromJdkTrustedCertificatesAndCustomTrustStore() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).hasSizeGreaterThan(10);
        assertThat(sslFactory.getTrustedCertificates().stream()
                .map(X509Certificate::getSubjectX500Principal)
                .map(X500Principal::toString)).contains("CN=*.google.com, O=Google LLC, L=Mountain View, ST=California, C=US");

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManager().get()).isNotInstanceOf(HotSwappableX509ExtendedKeyManager.class);
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isNotPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isNotPresent();
        assertThat(sslFactory.getTrustedCertificates()).isEmpty();
    }

    @Test
    void buildSSLFactoryWithSwappableIdentityMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withSwappableIdentityMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManager().get()).isInstanceOf(HotSwappableX509ExtendedKeyManager.class);
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isNotPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isNotPresent();
        assertThat(sslFactory.getTrustedCertificates()).isEmpty();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialWithoutPasswordAndTrustMaterialWithoutPassword() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + "identity-without-password.jks", null, "secret".toCharArray())
                .withTrustMaterial(KEYSTORE_LOCATION + "truststore-without-password.jks", null)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialWithKeyStoreTypesIncluded() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD, KeyStore.getDefaultType())
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD, KeyStore.getDefaultType())
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromInputStream() {
        InputStream identityStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + IDENTITY_FILE_NAME);
        InputStream trustStoreStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityStream, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStoreStream, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromInputStreamWithCustomKeyStoreType() {
        InputStream identityStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + IDENTITY_FILE_NAME);
        InputStream trustStoreStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityStream, IDENTITY_PASSWORD, KeyStore.getDefaultType())
                .withTrustMaterial(trustStoreStream, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromIdentityManagerAndTrustStore() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        X509ExtendedKeyManager identityManager = KeyManagerUtils.createKeyManager(identity, IDENTITY_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityManager)
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromIdentityManagerFactoryAndTrustStore() throws Exception {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(identity, IDENTITY_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManagerFactory)
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreAndOnlyJdkTrustedCertificates() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromPrivateKey() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        PrivateKey privateKey = (PrivateKey) identity.getKey("dummy-client", IDENTITY_PASSWORD);
        Certificate[] certificateChain = identity.getCertificateChain("dummy-client");

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(privateKey, IDENTITY_PASSWORD, certificateChain)
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();
        assertThat(sslFactory.getKeyManager().get().getPrivateKey("cn=prof oak,ou=oak pokémon research lab,o=oak pokémon research lab,c=pallet town")).isNotNull();
        assertThat(sslFactory.getKeyManager().get().getCertificateChain("cn=prof oak,ou=oak pokémon research lab,o=oak pokémon research lab,c=pallet town")).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromPrivateKeyWithCustomAlias() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        PrivateKey privateKey = (PrivateKey) identity.getKey("dummy-client", IDENTITY_PASSWORD);
        Certificate[] certificateChain = identity.getCertificateChain("dummy-client");

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(privateKey, IDENTITY_PASSWORD, "thunder-client", certificateChain)
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreWithDifferentKeyPasswordAndOnlyJdkTrustedCertificates() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + "identity-with-different-key-password.jks", IDENTITY_PASSWORD, "my-precious".toCharArray())
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStorePathWithDifferentKeyPasswordAndOnlyJdkTrustedCertificates() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, "identity-with-different-key-password.jks");

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityPath, IDENTITY_PASSWORD, "my-precious".toCharArray())
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        Files.delete(identityPath);
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreAndTrustStoreWithPath() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityPath, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        Files.delete(identityPath);
        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreAndTrustStoreWithPathAndWithKeyStoreTypesIncluded() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identityPath, IDENTITY_PASSWORD, KeyStore.getDefaultType())
                .withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD, KeyStore.getDefaultType())
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();

        Files.delete(identityPath);
        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreAndTrustStore() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStore)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryWithIdentityMaterialAndTrustMaterialFromKeyStoreAndTrustStoreWithoutCachingPasswords() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStore)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
    }

    @Test
    void buildSSLFactoryByDefaultWithTlsSslContextAlgorithm() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext().getProtocol()).isEqualTo("TLS");
    }

    @Test
    void buildSSLFactoryWithSslContextAlgorithm() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSslContextAlgorithm("TLSv1.2")
                .build();

        assertThat(sslFactory.getSslContext().getProtocol()).isEqualTo("TLSv1.2");
    }

    @Test
    void buildSSLFactoryWithCustomHostnameVerifier() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withHostnameVerifier((host, sslSession) -> true)
                .build();

        HostnameVerifier hostnameVerifier = sslFactory.getHostnameVerifier();
        assertThat(hostnameVerifier.verify("qwerty", null)).isTrue();
    }

    @Test
    void buildSSLFactoryWithUnsafeHostnameVerifier() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withUnsafeHostnameVerifier()
                .build();

        HostnameVerifier hostnameVerifier = sslFactory.getHostnameVerifier();
        assertThat(hostnameVerifier.verify("qwerty", null)).isTrue();
    }

    @Test
    void buildSSLFactoryWithoutHostnameVerifierProvidesDefaultHostnameVerifier() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .build();

        HostnameVerifier hostnameVerifier = sslFactory.getHostnameVerifier();
        assertThat(hostnameVerifier).isInstanceOf(FenixHostnameVerifier.class);
    }

    @Test
    void buildSSLFactoryWithTrustingAllCertificatesWithoutValidation() {
        LogCaptor logCaptor = LogCaptor.forClass(SSLFactory.class);

        SSLFactory sslFactory = SSLFactory.builder()
                .withUnsafeTrustMaterial()
                .build();

        logCaptor.close();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getTrustedCertificates()).isEmpty();
        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(UnsafeX509ExtendedTrustManager.class);
        assertThat(logCaptor.getWarnLogs()).contains("UnsafeTrustManager is being used. Client/Server certificates will be accepted without validation.");
    }

    @Test
    void buildSSLFactoryWithSecurityProvider() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSslContextAlgorithm("TLS")
                .withSecurityProvider(Security.getProvider("SunJSSE"))
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getSslContext().getProvider().getName()).isEqualTo("SunJSSE");
    }

    @Test
    void buildSSLFactoryWithSecurityProviderName() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSslContextAlgorithm("TLS")
                .withSecurityProvider("SunJSSE")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getSslContext().getProvider().getName()).isEqualTo("SunJSSE");
    }

    @Test
    void buildSSLFactoryWithTrustValidatorBasedOnChainAndAuthType() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withTrustEnhancer(((certificateChain, authType) -> true))
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isNotInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(EnhanceableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustValidatorBasedOnChainAndAuthTypeAndSocket() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withTrustEnhancer((X509Certificate[] certificateChain, String authType, Socket socket) -> true)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isNotInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(EnhanceableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithTrustValidatorBasedOnChainAndAuthTypeAndSSLEngine() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD)
                .withTrustEnhancer((X509Certificate[] certificateChain, String authType, SSLEngine socket) -> true)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isNotInstanceOf(HotSwappableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(EnhanceableX509ExtendedTrustManager.class);
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isNotPresent();
    }

    @Test
    void buildSSLFactoryWithSystemPropertyDerivedIdentityAndTrustMaterial() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        Map<String, String> properties = new HashMap<>();
        properties.put("javax.net.ssl.keyStore", identityPath.toString());
        properties.put("javax.net.ssl.keyStorePassword", new String(IDENTITY_PASSWORD));
        properties.put("javax.net.ssl.keyStoreType", "PKCS12");
        properties.put("javax.net.ssl.trustStore", trustStorePath.toString());
        properties.put("javax.net.ssl.trustStorePassword", new String(TRUSTSTORE_PASSWORD));
        properties.put("javax.net.ssl.trustStoreType", "PKCS12");
        properties.forEach(System::setProperty);

        SSLFactory sslFactory = SSLFactory.builder()
                .withSystemPropertyDerivedIdentityMaterial()
                .withSystemPropertyDerivedTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();

        properties.forEach((propertyName, propertyValue) -> System.clearProperty(propertyName));

        Files.delete(identityPath);
        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithSystemPropertyDerivedIdentityAndTrustMaterialWithSecurityProvider() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);

        Map<String, String> properties = new HashMap<>();
        properties.put("javax.net.ssl.keyStore", identityPath.toString());
        properties.put("javax.net.ssl.keyStorePassword", new String(IDENTITY_PASSWORD));
        properties.put("javax.net.ssl.keyStoreType", "PKCS12");
        properties.put("javax.net.ssl.keyStoreProvider", "SunJSSE");
        properties.put("javax.net.ssl.trustStore", trustStorePath.toString());
        properties.put("javax.net.ssl.trustStorePassword", new String(TRUSTSTORE_PASSWORD));
        properties.put("javax.net.ssl.trustStoreType", "PKCS12");
        properties.put("javax.net.ssl.trustStoreProvider", "SunJSSE");
        properties.forEach(System::setProperty);

        SSLFactory sslFactory = SSLFactory.builder()
                .withSystemPropertyDerivedIdentityMaterial()
                .withSystemPropertyDerivedTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManagerFactory()).isPresent();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManagerFactory()).isPresent();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();

        properties.forEach((propertyName, propertyValue) -> System.clearProperty(propertyName));

        Files.delete(identityPath);
        Files.delete(trustStorePath);
    }

    @Test
    void buildSSLFactoryWithSystemPropertyDerivedProtocol() {
        String propertyName = "https.protocols";
        System.setProperty(propertyName, "TLSv1.2,   ,TLSv1.1");

        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSystemPropertyDerivedProtocols()
                .build();

        assertThat(sslFactory.getProtocols()).containsExactly("TLSv1.2", "TLSv1.1");
        System.clearProperty(propertyName);
    }

    @Test
    void buildSSLFactoryWithSystemPropertyDerivedCiphers() {
        String propertyName = "https.cipherSuites";
        System.setProperty(propertyName, "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,   ,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");

        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSystemPropertyDerivedCiphers()
                .build();

        assertThat(sslFactory.getCiphers()).containsExactly("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
        System.clearProperty(propertyName);
    }

    @Test
    void throwExceptionWhenSystemPropertyDerivedProtocolsIsEmpty() {
        String propertyName = "https.protocols";
        System.setProperty(propertyName, "");

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedProtocols)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the System property for [https.protocols] because it does not contain any value");
        System.clearProperty(propertyName);
    }

    @Test
    void throwExceptionWhenSystemPropertyDerivedProtocolsContainsInvalidValues() {
        String propertyName = "https.protocols";
        System.setProperty(propertyName, ",,");

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedProtocols)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the System property for [https.protocols] because it does not contain any value");
        System.clearProperty(propertyName);
    }

    @Test
    void throwExceptionWhenSystemPropertyDerivedCiphersIsEmpty() {
        String propertyName = "https.cipherSuites";
        System.setProperty(propertyName, "");

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedCiphers)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the System property for [https.cipherSuites] because it does not contain any value");
        System.clearProperty(propertyName);
    }

    @Test
    void throwExceptionWhenSystemPropertyDerivedCiphersContainsInvalidValues() {
        String propertyName = "https.cipherSuites";
        System.setProperty(propertyName, ",,");

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedCiphers)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the System property for [https.cipherSuites] because it does not contain any value");
        System.clearProperty(propertyName);
    }

    @Test
    void throwExceptionWhenIdentityStorePathIsAbsentFromSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.net.ssl.keyStorePassword", new String(IDENTITY_PASSWORD));
        properties.put("javax.net.ssl.keyStoreType", "PKCS12");
        properties.forEach(System::setProperty);

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedIdentityMaterial)
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("Identity details are empty, which are required to be present when SSL/TLS is enabled");

        properties.forEach((propertyName, propertyValue) -> System.clearProperty(propertyName));
    }

    @Test
    void throwExceptionWhenIdentityStorePathIsEmptyFromSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.net.ssl.keyStore", "   ");
        properties.put("javax.net.ssl.keyStorePassword", new String(IDENTITY_PASSWORD));
        properties.put("javax.net.ssl.keyStoreType", "PKCS12");
        properties.forEach(System::setProperty);

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedIdentityMaterial)
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("Identity details are empty, which are required to be present when SSL/TLS is enabled");

        properties.forEach((propertyName, propertyValue) -> System.clearProperty(propertyName));
    }

    @Test
    void throwExceptionWhenTrustStorePathIsAbsentFromSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.net.ssl.trustStorePassword", new String(TRUSTSTORE_PASSWORD));
        properties.put("javax.net.ssl.trustStoreType", "PKCS12");
        properties.forEach(System::setProperty);

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(sslFactoryBuilder::withSystemPropertyDerivedTrustMaterial)
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("TrustStore details are empty, which are required to be present when SSL/TLS is enabled");

        properties.forEach((propertyName, propertyValue) -> System.clearProperty(propertyName));
    }

    @Test
    void throwExceptionWhenSSLFactoryIsBuildWithoutIdentityAndTrustMaterial() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(factoryBuilder::build)
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("Could not create instance of SSLFactory because Identity and Trust material are not present. Please provide at least a Trust material.");
    }

    @Test
    void buildSSLFactoryWithTLSProtocolVersionOneDotThreeIfJavaVersionIsElevenOrGreater() {
        Pattern valueBeforeDotPattern = Pattern.compile("^([^.]+)");

        String javaVersion = System.getProperty("java.version");
        Matcher matcher = valueBeforeDotPattern.matcher(javaVersion);
        if (!matcher.find()) {
            fail("Could not find the java version");
        }

        int javaMajorVersion = Integer.parseInt(matcher.group(0));
        if (javaMajorVersion < 11) {
            LOGGER.info("skipping unit test [{}] because TLSv1.3 is not available for this java {} version",
                        new Object() {}.getClass().getEnclosingMethod().getName(),
                        javaVersion);
            return;
        }

        LOGGER.info("Found java version {}, including testing SSLFactory with TLSv1.3 protocol", javaMajorVersion);
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getProtocols()).contains("TLSv1.3");
    }

    @Test
    void returnSslSocketFactory() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withCiphers("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getSslSocketFactory()).isNotNull();
        assertThat(sslFactory.getSslSocketFactory().getDefaultCipherSuites()).containsExactly("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
        assertThat(sslFactory.getSslSocketFactory().getSupportedCipherSuites()).containsExactly("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
    }

    @Test
    void returnSslServerSocketFactory() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withDefaultTrustMaterial()
                .withCiphers("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getSslServerSocketFactory()).isNotNull();
        assertThat(sslFactory.getSslServerSocketFactory().getDefaultCipherSuites()).containsExactly("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
        assertThat(sslFactory.getSslServerSocketFactory().getSupportedCipherSuites()).containsExactly("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
    }

    @Test
    void returnDefaultCiphersWhenNoneSpecified() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getCiphers()).isNotEmpty();
        assertThat(sslFactory.getCiphers()).containsExactlyInAnyOrder(sslFactory.getSslContext().getDefaultSSLParameters().getCipherSuites());
    }

    @Test
    void returnSpecifiedCiphers() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withCiphers("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getCiphers())
                .containsExactlyInAnyOrder("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
    }

    @Test
    void returnSpecifiedCiphersAndProtocolsWithinSslParameters() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withCiphers("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384")
                .withProtocols("TLSv1.2", "TLSv1.1")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getSslParameters().getCipherSuites())
                .containsExactlyInAnyOrder("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
        assertThat(sslFactory.getSslParameters().getProtocols())
                .containsExactlyInAnyOrder("TLSv1.2", "TLSv1.1");
        assertThat(sslFactory.getSslParameters())
                .isNotEqualTo(sslFactory.getSslContext().getDefaultSSLParameters());
    }

    @Test
    void returnDefaultProtocolsWhenNoneSpecified() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getProtocols()).containsExactlyInAnyOrder(sslFactory.getSslContext().getDefaultSSLParameters().getProtocols());
    }

    @Test
    void returnSpecifiedProtocols() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withProtocols("TLSv1.2", "TLSv1.1")
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();
        assertThat(sslFactory.getProtocols()).containsExactlyInAnyOrder("TLSv1.2", "TLSv1.1");
    }

    @Test
    void returnSpecifiedNeedClientAuthenticationWithoutOptions() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withNeedClientAuthentication()
                .build();

        assertThat(sslFactory.getSslParameters().getNeedClientAuth()).isTrue();
        assertThat(sslFactory.getSslParameters().getWantClientAuth()).isFalse();
    }

    @Test
    void returnSpecifiedNeedClientAuthenticationWithOptions() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withNeedClientAuthentication(true)
                .build();

        assertThat(sslFactory.getSslParameters().getNeedClientAuth()).isTrue();
        assertThat(sslFactory.getSslParameters().getWantClientAuth()).isFalse();
    }

    @Test
    void returnSpecifiedWantClientAuthenticationWithoutOptions() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withWantClientAuthentication()
                .build();

        assertThat(sslFactory.getSslParameters().getWantClientAuth()).isTrue();
        assertThat(sslFactory.getSslParameters().getNeedClientAuth()).isFalse();
    }

    @Test
    void returnSpecifiedWantClientAuthenticationWithOptions() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withWantClientAuthentication(true)
                .build();

        assertThat(sslFactory.getSslParameters().getWantClientAuth()).isTrue();
        assertThat(sslFactory.getSslParameters().getNeedClientAuth()).isFalse();
    }

    @Test
    void returnSslEngineWithSslParameters() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStore)
                .withNeedClientAuthentication()
                .build();

        SSLEngine sslEngine = sslFactory.getSSLEngine();

        assertThat(sslEngine).isNotNull();
        assertThat(sslEngine.getPeerHost()).isNull();
        assertThat(sslEngine.getPeerPort()).isEqualTo(-1);
        assertThat(sslEngine.getNeedClientAuth()).isTrue();
    }

    @Test
    void returnSslEngineWithoutHostAndPortIfOnlyHostIsDefined() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStore)
                .withNeedClientAuthentication()
                .build();

        SSLEngine sslEngine = sslFactory.getSSLEngine("localhost", null);

        assertThat(sslEngine).isNotNull();
        assertThat(sslEngine.getPeerHost()).isNull();
        assertThat(sslEngine.getPeerPort()).isEqualTo(-1);
        assertThat(sslEngine.getNeedClientAuth()).isTrue();
    }

    @Test
    void returnSslEngineWithHostAndPortAndWithSslParameters() {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(identity, IDENTITY_PASSWORD)
                .withTrustMaterial(trustStore)
                .withNeedClientAuthentication()
                .build();

        SSLEngine sslEngine = sslFactory.getSSLEngine("localhost", 8443);

        assertThat(sslEngine).isNotNull();
        assertThat(sslEngine.getPeerHost()).isEqualTo("localhost");
        assertThat(sslEngine.getPeerPort()).isEqualTo(8443);
        assertThat(sslEngine.getNeedClientAuth()).isTrue();
    }

    @Test
    void createMultipleRoutesForSingleClientIdentity() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withIdentityRoute("some-client-alias", "https://localhost:8443", "https://localhost:8444")
                .build();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(KeyManagerUtils.getIdentityRoute(sslFactory.getKeyManager().get()))
                .containsKey("some-client-alias")
                .containsValue(Arrays.asList("https://localhost:8443", "https://localhost:8444"));

        assertThat(((CompositeX509ExtendedKeyManager)sslFactory.getKeyManager().get()).getIdentityRoute())
                .containsKey("some-client-alias")
                .containsValue(Arrays.asList(URI.create("https://localhost:8443"), URI.create("https://localhost:8444")));
    }

    @Test
    void createMultipleRoutesForSingleClientIdentityAndUpdateAfterCreation() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withIdentityRoute("some-client-alias", "https://localhost:8443", "https://localhost:8444")
                .build();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(KeyManagerUtils.getIdentityRoute(sslFactory.getKeyManager().get()))
                .containsKey("some-client-alias")
                .containsValue(Arrays.asList("https://localhost:8443", "https://localhost:8444"))
                .doesNotContainValue(Collections.singletonList("https://localhost:8445"));

        KeyManagerUtils.addIdentityRoute(sslFactory.getKeyManager().get(), "some-client-alias", "https://localhost:8445");

        assertThat(KeyManagerUtils.getIdentityRoute(sslFactory.getKeyManager().get()))
                .containsKey("some-client-alias")
                .containsValue(Arrays.asList("https://localhost:8443", "https://localhost:8444", "https://localhost:8445"));
    }

    @Test
    void createSSLFactoryWithSessionTimeout() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withDefaultTrustMaterial()
                .withSessionTimeout(10)
                .build();

        int clientSessionTimeout = sslFactory.getSslContext()
                .getClientSessionContext()
                .getSessionTimeout();

        int serverSessionTimeout = sslFactory.getSslContext()
                .getServerSessionContext()
                .getSessionTimeout();

        assertThat(clientSessionTimeout).isEqualTo(10);
        assertThat(serverSessionTimeout).isEqualTo(10);
    }

    @Test
    void createSSLFactoryWithSessionCacheSize() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD)
                .withDefaultTrustMaterial()
                .withSessionCacheSize(1024)
                .build();

        int clientSessionCacheSize = sslFactory.getSslContext()
                .getClientSessionContext()
                .getSessionCacheSize();

        int serverSessionCacheSize = sslFactory.getSslContext()
                .getServerSessionContext()
                .getSessionCacheSize();

        assertThat(clientSessionCacheSize).isEqualTo(1024);
        assertThat(serverSessionCacheSize).isEqualTo(1024);
    }

    @Test
    void createSSLFactoryWithDummyIdentityMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDummyIdentityMaterial()
                .build();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getKeyManager().get()).isInstanceOf(DummyX509ExtendedKeyManager.class);
    }

    @Test
    void createSSLFactoryWithDummyTrustMaterial() {
        SSLFactory sslFactory = SSLFactory.builder()
                .withDummyTrustMaterial()
                .build();

        assertThat(sslFactory.getTrustManager()).isPresent();
        assertThat(sslFactory.getTrustManager().get()).isInstanceOf(DummyX509ExtendedTrustManager.class);
    }

    @Test
    void throwIllegalArgumentExceptionWhenCertificateIsAbsent() {
        List<Certificate> certificates = Collections.emptyList();
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> sslFactoryBuilder.withTrustMaterial(certificates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the certificate(s). No certificate has been provided.");
    }

    @Test
    void throwIllegalArgumentExceptionWhenCertificateIsAbsentWhileAlsoUsingTrustOptions() {
        List<Certificate> certificates = Collections.emptyList();
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> sslFactoryBuilder.withTrustMaterial(certificates, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to load the certificate(s). No certificate has been provided.");
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithTrustStoreWhileProvidingWrongPassword() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();
        char[] trustStorePassword = "password".toCharArray();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, trustStorePassword))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("keystore password was incorrect");
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithTrustStoreFromPathWhileProvidingWrongPassword() throws IOException {
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();
        char[] trustStorePassword = "password".toCharArray();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(trustStorePath, trustStorePassword))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("keystore password was incorrect");

        Files.delete(trustStorePath);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithIdentityWhileProvidingWrongPassword() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();
        char[] identityStorePassword = "password".toCharArray();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, identityStorePassword))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("keystore password was incorrect");
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithIdentityFromPathWhileProvidingWrongPassword() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        char[] identityStorePassword = "password".toCharArray();
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(identityPath, identityStorePassword))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("keystore password was incorrect");

        Files.delete(identityPath);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithNullAsTrustStorePath() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial((Path) null, TRUSTSTORE_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithEmptyTrustStoreType() throws IOException {
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD, EMPTY))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);

        Files.delete(trustStorePath);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithTrustStoreAsNull() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial((KeyStore) null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenKeyStoreFileIsNotFound() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(KEYSTORE_LOCATION + "not-existing-truststore.jks", TRUSTSTORE_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("Failed to load the keystore from the classpath for the given path: [keystore/not-existing-truststore.jks]");
    }

    @Test
    void throwExceptionWhenTrustStorePathIsNotProvided() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(EMPTY, TRUSTSTORE_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityPathIsNotProvided() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(EMPTY, IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityPathAsStringIsNull() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial((String) null, IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityPathAsStringContainsOnlyWhiteSpace() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial("    ", IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityTypeIsNotProvided() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD, EMPTY))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityPathIsNull() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial((Path) null, IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityIsNull() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial((KeyStore) null, IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityTypeIsNotProvidedWhileUsingPath() throws IOException {
        Path identityPath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, IDENTITY_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(identityPath, IDENTITY_PASSWORD, EMPTY))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);

        Files.delete(identityPath);
    }

    @Test
    void throwExceptionWhenIdentityStreamIsNull() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial((InputStream) null, IDENTITY_PASSWORD))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenIdentityTypeIsNotProvidedWhileUsingInputStream() {
        InputStream identityStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + IDENTITY_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(identityStream, IDENTITY_PASSWORD, EMPTY))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_IDENTITY_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenUnknownIdentityTypeIsProvidedWhileUsingInputStream() {
        InputStream identityStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + IDENTITY_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withIdentityMaterial(identityStream, IDENTITY_PASSWORD, "KABOOM"))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("KABOOM not found");
    }

    @Test
    void throwExceptionWhenUnknownTrustStoreTypeIsProvidedWhileUsingInputStream() {
        InputStream trustStoreStream = IOTestUtils.getResourceAsStream(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(trustStoreStream, TRUSTSTORE_PASSWORD, "KABOOM"))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessageContaining("KABOOM not found");
    }

    @Test
    void throwExceptionWhenProvidingWrongKeyPassword() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder()
                .withIdentityMaterial(
                        KEYSTORE_LOCATION + "identity-with-different-key-password.jks",
                        IDENTITY_PASSWORD,
                        IDENTITY_PASSWORD)
                .withDefaultTrustMaterial();

        assertThatThrownBy(factoryBuilder::build)
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.UnrecoverableKeyException: Get Key failed: Given final block not properly padded. Such issues can arise if a bad key is used during decryption.");
    }

    @Test
    void throwExceptionWhenUnknownSslContextAlgorithmIsProvided() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSslContextAlgorithm("KABOOM");

        assertThatThrownBy(factoryBuilder::build)
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchAlgorithmException: KABOOM SSLContext not available");
    }

    @Test
    void throwExceptionWhenUnknownSecurityProviderNameIsProvided() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder()
                .withDefaultTrustMaterial()
                .withSslContextAlgorithm("TLS")
                .withSecurityProvider("KABOOOM");

        assertThatThrownBy(factoryBuilder::build)
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchProviderException: no such provider: KABOOOM");
    }

    @Test
    void throwExceptionNullIsIsProvidedWhenUsingPrivateKey() throws KeyStoreException {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        Certificate[] certificateChain = identity.getCertificateChain("dummy-client");

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> sslFactoryBuilder.withIdentityMaterial(null, IDENTITY_PASSWORD, certificateChain))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessageContaining("Unsupported Key type");
    }

    @Test
    void throwExceptionWhenKeyManagerFactoryDoesNotContainsKeyManagersOfX509KeyManagerType() throws Exception {
        KeyManagerFactory keyManagerFactory = spy(KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()));

        doReturn(new KeyManager[] { mock(KeyManager.class) }).when(keyManagerFactory).getKeyManagers();
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> sslFactoryBuilder.withIdentityMaterial(keyManagerFactory))
                .isInstanceOf(GenericKeyManagerException.class)
                .hasMessage("Input does not contain KeyManagers");
    }

    @Test
    void throwExceptionWhenTrustManagerFactoryDoesNotContainsTrustManagersOfX509TrustManagerType() throws Exception {
        TrustManagerFactory trustManagerFactory = spy(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()));

        doReturn(new TrustManager[] { mock(TrustManager.class) }).when(trustManagerFactory).getTrustManagers();
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> sslFactoryBuilder.withTrustMaterial(trustManagerFactory))
                .isInstanceOf(GenericTrustManagerException.class)
                .hasMessage("Input does not contain TrustManager");
    }

    @Test
    void throwExceptionWhenClientAliasIsNotPresentWhenRoutingIdentities() {
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(() -> sslFactoryBuilder.withIdentityRoute(null, "https://localhost:8443"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("alias should be present");
    }

    @Test
    void throwExceptionWhenRouteIsNotPresentForClientIdentityRoute() {
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(() -> sslFactoryBuilder.withIdentityRoute("some-client-alias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one host should be present. No host(s) found for the given alias: [some-client-alias]");
    }

    @Test
    void throwGenericSecurityExceptionWhenSomethingUnExpectedHappensWhenApplyingTheCertificatesForTheTrustAnchorOptions() throws Exception {
        Set<X509Certificate> certificates = new HashSet<>(Arrays.asList(TrustManagerUtils.createTrustManagerWithJdkTrustedCertificates().getAcceptedIssuers()));

        TrustAnchorTrustOptions<?> trustAnchorTrustOptions = mock(TrustAnchorTrustOptions.class);
        when(trustAnchorTrustOptions.apply(anySet())).thenThrow(new InvalidAlgorithmParameterException("KABOOM!"));

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(() -> sslFactoryBuilder.withTrustMaterial(certificates, trustAnchorTrustOptions))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessageContaining("KABOOM!");
    }

    @Test
    void throwGenericSecurityExceptionWhenSomethingUnExpectedHappensWhenApplyingTheTrustStoreForTheTrustStoreOptions() throws Exception {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        TrustStoreTrustOptions<?> trustOptions = mock(TrustStoreTrustOptions.class);
        when(trustOptions.apply(any())).thenThrow(new InvalidAlgorithmParameterException("KABOOM!"));

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder();
        assertThatThrownBy(() -> sslFactoryBuilder.withTrustMaterial(trustStore, trustOptions))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessageContaining("KABOOM!");
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithNullAsTrustStorePathWhileUsingTrustOptions() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial((Path) null, TRUSTSTORE_PASSWORD, (TrustStoreTrustOptions<?>) null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithEmptyTrustStoreTypeWhileUsingTrustOptions() throws IOException {
        Path trustStorePath = IOTestUtils.copyFileToHomeDirectory(KEYSTORE_LOCATION, TRUSTSTORE_FILE_NAME);
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(trustStorePath, TRUSTSTORE_PASSWORD, EMPTY, null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);

        Files.delete(trustStorePath);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithNullAsTrustStorePathWhileUsingTrustOptionsWithClassPathTrustStore() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial((String) null, TRUSTSTORE_PASSWORD, (TrustStoreTrustOptions<?>) null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithEmptyAsTrustStorePathWhileUsingTrustOptionsWithClassPathTrustStore() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial(EMPTY, TRUSTSTORE_PASSWORD, (TrustStoreTrustOptions<?>) null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithEmptyTrustStoreTypeWhileUsingTrustOptionsWithClassPathTrustStore() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial("/some-path", TRUSTSTORE_PASSWORD, EMPTY, null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithNullTrustStoreTypeWhileUsingTrustOptionsWithClassPathTrustStore() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial("/some-path", TRUSTSTORE_PASSWORD, null, null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

    @Test
    void throwExceptionWhenBuildingSSLFactoryWithNullTrustStoreTypeWhileUsingClassPathTrustStore() {
        SSLFactory.Builder factoryBuilder = SSLFactory.builder();

        assertThatThrownBy(() -> factoryBuilder.withTrustMaterial("/some-path", TRUSTSTORE_PASSWORD, (String) null))
                .isInstanceOf(GenericKeyStoreException.class)
                .hasMessage(GENERIC_TRUSTSTORE_VALIDATION_EXCEPTION_MESSAGE);
    }

}
