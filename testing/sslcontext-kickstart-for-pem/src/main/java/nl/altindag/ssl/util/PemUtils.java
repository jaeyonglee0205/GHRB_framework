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
package nl.altindag.ssl.util;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.decryptor.PemDecryptor;
import nl.altindag.ssl.decryptor.Pkcs8Decryptor;
import nl.altindag.ssl.exception.CertificateParseException;
import nl.altindag.ssl.exception.GenericIOException;
import nl.altindag.ssl.exception.GenericKeyStoreException;
import nl.altindag.ssl.exception.PemParseException;
import nl.altindag.ssl.exception.PrivateKeyParseException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static nl.altindag.ssl.util.ValidationUtils.requireNotEmpty;
import static nl.altindag.ssl.util.ValidationUtils.requireNotNull;

/**
 * Reads PEM formatted private keys and certificates
 * as identity material and trust material and maps it
 * to either a {@link X509ExtendedKeyManager} or {@link X509ExtendedTrustManager}.
 * <br>
 * <br>
 * The PemUtils provides also other methods for example to:
 * <pre>
 * - load trusted certificates and map it into a list of {@link X509Certificate}
 * - load identity material and map it into a {@link PrivateKey}
 * </pre>
 * <p>
 * The PemUtils serves mainly as a helper class to easily supply the PEM formatted SSL material
 * for the {@link SSLFactory}, but can also be used for other purposes.
 *
 * @author Hakan Altindag
 */
public final class PemUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String EMPTY_INPUT_STREAM_EXCEPTION_MESSAGE = "Failed to load the certificate from the provided InputStream because it is null";
    private static final UnaryOperator<String> CERTIFICATE_NOT_FOUND_EXCEPTION_MESSAGE = certificatePath -> String.format("Failed to load the certificate from the classpath for the given path: [%s]", certificatePath);
    private static final char[] DUMMY_PASSWORD = KeyStoreUtils.DUMMY_PASSWORD.toCharArray();
    private static final char[] NO_PASSWORD = null;
    private static final PemUtils INSTANCE = new PemUtils(
            new JcaPEMKeyConverter(),
            new JcaX509CertificateConverter()
    );

    private final JcaPEMKeyConverter keyConverter;
    private final JcaX509CertificateConverter certificateConverter;

    PemUtils(JcaPEMKeyConverter keyConverter,
             JcaX509CertificateConverter certificateConverter) {

        this.keyConverter = keyConverter;
        this.certificateConverter = certificateConverter;
    }

    /**
     * Loads certificates from the classpath and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedTrustManager loadTrustMaterial(String... certificatePaths) {
        return mapTrustMaterial(
                loadCertificate(certificatePaths)
        );
    }

    /**
     * Loads certificates from the filesystem and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedTrustManager loadTrustMaterial(Path... certificatePaths) {
        return mapTrustMaterial(
                loadCertificate(certificatePaths)
        );
    }

    /**
     * Loads certificates from multiple InputStreams and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedTrustManager loadTrustMaterial(InputStream... certificateStreams) {
        return mapTrustMaterial(
                loadCertificate(certificateStreams)
        );
    }

    /**
     * Loads certificates from the classpath and maps it to a list of {@link X509Certificate}
     */
    public static List<X509Certificate> loadCertificate(String... certificatePaths) {
        return loadCertificate(
                certificatePaths,
                certificatePath -> requireNotNull(
                        IOUtils.getResourceAsStream(certificatePath),
                        CERTIFICATE_NOT_FOUND_EXCEPTION_MESSAGE.apply(certificatePath)
                )
        );
    }

    /**
     * Loads certificates from the filesystem and maps it to a list of {@link X509Certificate}
     */
    public static List<X509Certificate> loadCertificate(Path... certificatePaths) {
        return loadCertificate(certificatePaths, IOUtils::getFileAsStream);
    }

    /**
     * Loads certificates from multiple InputStreams and maps it to a list of {@link X509Certificate}
     */
    public static List<X509Certificate> loadCertificate(InputStream... certificateStreams) {
        return loadCertificate(
                certificateStreams,
                certificateStream -> requireNotNull(certificateStream, EMPTY_INPUT_STREAM_EXCEPTION_MESSAGE)
        );
    }

    private static <T> List<X509Certificate> loadCertificate(T[] resources, Function<T, InputStream> resourceMapper) {
        return Arrays.stream(resources)
                .map(resourceMapper)
                .map(IOUtils::getContent)
                .map(PemUtils::parseCertificate)
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public static List<X509Certificate> parseCertificate(String certContent) {
        List<X509Certificate> certificates = parsePemContent(certContent).stream()
                .map(PemUtils::extractCertificate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

        return requireNotEmpty(certificates, () -> new CertificateParseException("Received an unsupported certificate type"));
    }

    private static List<Object> parsePemContent(String pemContent) {
        try(Reader stringReader = new StringReader(pemContent);
            PEMParser pemParser = new PEMParser(stringReader)) {

            List<Object> objects = new ArrayList<>();
            for (Object object = pemParser.readObject(); object != null; object = pemParser.readObject()) {
                objects.add(object);
            }

            return objects;
        } catch (IOException e) {
            throw new PemParseException(e);
        }
    }

    private static Optional<X509Certificate> extractCertificate(Object object) {
        try {

            X509Certificate certificate = null;
            if (object instanceof X509CertificateHolder) {
                certificate = PemUtils.getInstance().getCertificateConverter().getCertificate((X509CertificateHolder) object);
            } else if (object instanceof X509TrustedCertificateBlock) {
                X509CertificateHolder certificateHolder = ((X509TrustedCertificateBlock) object).getCertificateHolder();
                certificate = PemUtils.getInstance().getCertificateConverter().getCertificate(certificateHolder);
            }

            return Optional.ofNullable(certificate);
        } catch (CertificateException e) {
            throw new CertificateParseException(e);
        }
    }

    /**
     * Parses one or more certificates as a string representation
     * and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedTrustManager parseTrustMaterial(String... certificateContents) {
        return Arrays.stream(certificateContents)
                .map(PemUtils::parseCertificate)
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PemUtils::mapTrustMaterial));
    }

    private static X509ExtendedTrustManager mapTrustMaterial(List<X509Certificate> certificates) {
        KeyStore trustStore = KeyStoreUtils.createTrustStore(certificates);
        return TrustManagerUtils.createTrustManager(trustStore);
    }

    /**
     * Loads the identity material based on a certificate chain and a private key
     * from the classpath and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(String certificateChainPath, String privateKeyPath) {
        return loadIdentityMaterial(certificateChainPath, privateKeyPath, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a certificate chain and a private key from
     * the classpath and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(String certificateChainPath, String privateKeyPath, char[] keyPassword) {
        return loadIdentityMaterial(
                certificateChainPath,
                privateKeyPath,
                keyPassword,
                certificatePath -> requireNotNull(
                        IOUtils.getResourceAsStream(certificatePath),
                        CERTIFICATE_NOT_FOUND_EXCEPTION_MESSAGE.apply(certificatePath)
                )
        );
    }

    /**
     * Loads the identity material based on a certificate chain and a private key
     * as an InputStream and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(InputStream certificateChainStream, InputStream privateKeyStream) {
        return loadIdentityMaterial(certificateChainStream, privateKeyStream, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a certificate chain and a private key
     * as an InputStream and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(InputStream certificateChainStream, InputStream privateKeyStream, char[] keyPassword) {
        return loadIdentityMaterial(
                certificateChainStream,
                privateKeyStream,
                keyPassword,
                inputStream -> requireNotNull(inputStream, EMPTY_INPUT_STREAM_EXCEPTION_MESSAGE)
        );
    }

    /**
     * Loads the identity material based on a certificate chain and a private key
     * from the filesystem and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(Path certificateChainPath, Path privateKeyPath) {
        return loadIdentityMaterial(certificateChainPath, privateKeyPath, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a certificate chain and a private key
     * from the filesystem and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(Path certificateChainPath, Path privateKeyPath, char[] keyPassword) {
        return loadIdentityMaterial(certificateChainPath, privateKeyPath, keyPassword, IOUtils::getFileAsStream);
    }

    /**
     * Loads the identity material based on a combined file containing the certificate chain and the private key
     * from the classpath and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(String identityPath) {
        return loadIdentityMaterial(identityPath, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a combined file containing the certificate chain and the private key
     * from the classpath and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(String identityPath, char[] keyPassword) {
        return loadIdentityMaterial(
                identityPath,
                keyPassword,
                certificatePath -> requireNotNull(
                        IOUtils.getResourceAsStream(certificatePath),
                        CERTIFICATE_NOT_FOUND_EXCEPTION_MESSAGE.apply(certificatePath)
                )
        );
    }

    /**
     * Loads the identity material based on a combined file containing the certificate chain and the private key
     * from the filesystem and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(Path identityPath) {
        return loadIdentityMaterial(identityPath, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a combined file containing the certificate chain and the private key
     * from the filesystem and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(Path identityPath, char[] keyPassword) {
        return loadIdentityMaterial(identityPath, keyPassword, IOUtils::getFileAsStream);
    }

    /**
     * Loads the identity material based on a combined entity containing the certificate chain and the private key
     * from an InputStream and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(InputStream identityStream) {
        return loadIdentityMaterial(identityStream, NO_PASSWORD);
    }

    /**
     * Loads the identity material based on a combined entity containing the certificate chain and the private key
     * from an InputStream and maps it to an instance of {@link X509ExtendedKeyManager}
     */
    public static X509ExtendedKeyManager loadIdentityMaterial(InputStream identityStream, char[] keyPassword) {
        return loadIdentityMaterial(
                identityStream,
                keyPassword,
                inputStream -> requireNotNull(inputStream, EMPTY_INPUT_STREAM_EXCEPTION_MESSAGE)
        );
    }

    private static <T> X509ExtendedKeyManager loadIdentityMaterial(T certificateChain, T privateKey, char[] keyPassword, Function<T, InputStream> resourceMapper) {
        try(InputStream certificateChainStream = resourceMapper.apply(certificateChain);
            InputStream privateKeyStream = resourceMapper.apply(privateKey)) {

            String certificateChainContent = IOUtils.getContent(certificateChainStream);
            String privateKeyContent = IOUtils.getContent(privateKeyStream);

            return parseIdentityMaterial(certificateChainContent, privateKeyContent, keyPassword);
        } catch (IOException exception) {
            throw new GenericIOException(exception);
        }
    }

    private static <T> X509ExtendedKeyManager loadIdentityMaterial(T identity, char[] keyPassword, Function<T, InputStream> resourceMapper) {
        try(InputStream identityStream = resourceMapper.apply(identity)) {
            String identityContent = IOUtils.getContent(identityStream);
            return parseIdentityMaterial(identityContent, identityContent, keyPassword);
        } catch (IOException exception) {
            throw new GenericIOException(exception);
        }
    }

    /**
     * Parses the identity material based on a string representation containing the certificate chain and the private key
     * and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedKeyManager parseIdentityMaterial(String identityContent, char[] keyPassword) {
        return parseIdentityMaterial(identityContent, identityContent, keyPassword);
    }

    /**
     * Parses the identity material based on a string representation of the certificate chain and the private key
     * and maps it to an instance of {@link X509ExtendedTrustManager}
     */
    public static X509ExtendedKeyManager parseIdentityMaterial(String certificateChainContent, String privateKeyContent, char[] keyPassword) {
        PrivateKey privateKey = parsePrivateKey(privateKeyContent, keyPassword);
        Certificate[] certificateChain = PemUtils.parseCertificate(certificateChainContent)
                .toArray(new Certificate[]{});

        return parseIdentityMaterial(certificateChain, privateKey);
    }

    /**
     * Loads the private key from the classpath and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(String identityPath) {
        return loadPrivateKey(identityPath, NO_PASSWORD);
    }

    /**
     * Loads the private key from the classpath and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(String identityPath, char[] keyPassword) {
        return loadPrivateKey(
                identityPath,
                keyPassword,
                certificatePath -> requireNotNull(
                        IOUtils.getResourceAsStream(certificatePath),
                        CERTIFICATE_NOT_FOUND_EXCEPTION_MESSAGE.apply(certificatePath)
                )
        );
    }

    /**
     * Loads the private key from the filesystem and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(Path identityPath) {
        return loadPrivateKey(identityPath, NO_PASSWORD);
    }

    /**
     * Loads the private key from the filesystem and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(Path identityPath, char[] keyPassword) {
        return loadPrivateKey(identityPath, keyPassword, IOUtils::getFileAsStream);
    }

    /**
     * Loads the private key from an InputStream and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(InputStream identityStream) {
        return loadPrivateKey(identityStream, NO_PASSWORD);
    }

    /**
     * Loads the private key from an InputStream and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey loadPrivateKey(InputStream identityStream, char[] keyPassword) {
        return loadPrivateKey(
                identityStream,
                keyPassword,
                inputStream -> requireNotNull(inputStream, EMPTY_INPUT_STREAM_EXCEPTION_MESSAGE)
        );
    }

    private static <T> PrivateKey loadPrivateKey(T privateKey, char[] keyPassword, Function<T, InputStream> resourceMapper) {
        try(InputStream privateKeyStream = resourceMapper.apply(privateKey)) {
            String privateKeyContent = IOUtils.getContent(privateKeyStream);
            return parsePrivateKey(privateKeyContent, keyPassword);
        } catch (IOException exception) {
            throw new GenericIOException(exception);
        }
    }

    /**
     * Parses the private key based on a string representation of the private key
     * and maps it to an instance of {@link PrivateKey}
     */
    public static PrivateKey parsePrivateKey(String identityContent) {
        return parsePrivateKey(identityContent, NO_PASSWORD);
    }

    /**
     * Parses the private key based on a string representation of the private key
     * and maps it to an instance of {@link PrivateKey}. If the identity content
     * contains multiple private keys it will use only the first one.
     */
    public static PrivateKey parsePrivateKey(String identityContent, char[] keyPassword) {
        return parsePemContent(identityContent).stream()
                .map(object -> extractPrivateKeyInfo(object, keyPassword))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(PemUtils::extractPrivateKey)
                .orElseThrow(() -> new PrivateKeyParseException("Received an unsupported private key type"));
    }

    private static Optional<PrivateKeyInfo> extractPrivateKeyInfo(Object object, char[] keyPassword) {
        try {
            PrivateKeyInfo privateKeyInfo = null;

            if (object instanceof PrivateKeyInfo) {
                privateKeyInfo = (PrivateKeyInfo) object;
            } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                privateKeyInfo = Pkcs8Decryptor.getInstance()
                        .andThen(((PKCS8EncryptedPrivateKeyInfo) object)::decryptPrivateKeyInfo)
                        .apply(keyPassword);
            } else if (object instanceof PEMKeyPair) {
                privateKeyInfo = ((PEMKeyPair) object).getPrivateKeyInfo();
            } else if (object instanceof PEMEncryptedKeyPair) {
                privateKeyInfo = PemDecryptor.getInstance()
                        .andThen(((PEMEncryptedKeyPair) object)::decryptKeyPair)
                        .andThen(PEMKeyPair::getPrivateKeyInfo)
                        .apply(keyPassword);
            }

            return Optional.ofNullable(privateKeyInfo);
        } catch (IOException | OperatorCreationException | PKCSException e) {
            throw new PrivateKeyParseException(e);
        }
    }

    private static PrivateKey extractPrivateKey(PrivateKeyInfo privateKeyInfo) {
        try {
            return PemUtils.getInstance().getKeyConverter().getPrivateKey(privateKeyInfo);
        } catch (PEMException exception) {
            throw new PrivateKeyParseException(exception);
        }
    }

    private static X509ExtendedKeyManager parseIdentityMaterial(Certificate[] certificatesChain, PrivateKey privateKey) {
        try {
            KeyStore keyStore = KeyStoreUtils.createKeyStore();
            keyStore.setKeyEntry(CertificateUtils.generateAlias(certificatesChain[0]), privateKey, DUMMY_PASSWORD, certificatesChain);
            return KeyManagerUtils.createKeyManager(keyStore, DUMMY_PASSWORD);
        } catch (KeyStoreException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    static PemUtils getInstance() {
        return INSTANCE;
    }

    private JcaPEMKeyConverter getKeyConverter() {
        return keyConverter;
    }

    private JcaX509CertificateConverter getCertificateConverter() {
        return certificateConverter;
    }
}
