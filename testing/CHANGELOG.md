<h2 class="github">Changelog</h2>

This list is not intended to be all-encompassing - it will document major and breaking API 
changes with their rationale when appropriate:

### v7.4.6
 - Updated copyright owner to Thunderberry. The license type, Apache 2.0 license, has not changed
 - Added more secure hostname verifier a.k.a. FenixHostnameVerifier
 - Switched to FenixHostnameVerifier as default hostname verifier in the SSLFactory
 - Improved creating truststore having duplicate certificates
 - Improved creating base trust manager when having either unsafe trust manager, dummy trust manager, multiple trust managers and trust managers which does not have trusted certificates
 - Added Android trusted CA as system trust material option
 - Bumped dependencies
### v7.4.5
 - Bug-fix issue 196 - Use UnsafeTrustManager as base TrustManager if enabled
 - Improve issue 181 - Filtered out empty TrustManagers within TrustManagerUtils
 - Bumped dependencies
### v7.4.4
 - Added SSLFactoryUtils
 - Bumped dependencies
### v7.4.3
- Bug-fix issue 181 - Continue validation counter-party while having empty trust managers
### v7.4.2
- Moved server certificate extraction into separate utility class
- Added Dummy KeyManager and TrustManager
- Reformatted license header
- Added missing license header
- Bumped dependencies
- Removed support for system properties jdk.tls.client.cipherSuites jdk.tls.server.cipherSuites jdk.tls.client.protocols jdk.tls.server.protocols
- Improved input validation for emptiness and improved exception messages
### v7.4.1
- Improved extracting server certificates
- Bug-fix issue 167 - Resolve initialization issue of trusted certificate by having lowercase aliases
- Added input validation for loading keystores and certificates from the classpath or as input stream
### v7.4.0
- Added system property based ssl configuration
- Exposed additional method from PemUtils
- Bumped dependencies
### v7.3.0
- Added Trust Enhancer for an optional custom trust validations
- Bumped dependencies
### v7.2.1
- Fix parsing PEM on Android v28 and above
- Bumped SLF4J dependency
### v7.2.0
- Code improvements (removed code duplications)
- Removed deprecated methods
- Updated dependencies
### v7.1.0
- Added option to route server identities
### v7.0.3
- Added option for unsafe hostname verifier
- Eliminated duplicate code within CompositeX509ExtendedKeyManager
- Simplified PemUtils
- Improved immutability
- Bumped transitive dependencies
### v7.0.2
- Removed logger from UnsafeHostNameVerifier and UnsafeX509ExtendedTrustManager
- Removed client timeout for fetching remote certificates
### v7.0.1
- Fixed base64 decoding issues
- Increased client timeout for fetching remote certificates
### v7.0.0
- Removed deprecated methods
- Added Additional methods to KeyStoreUtils
- Replaced all NPE with IllegalArgumentException with explaining message
- Removed optional password caching for KeyStores (was disabled by default) 
### v6.8.0
- Added support for handling P7B and DER certificates
- Added basic Unsafe SSLSocketFactory
- Marked CertificateUtils.parseCertificate(String certificateContent) as deprecated, instead use CertificateUtils.parsePemCertificate(String certificateContent)
- Added support to return X509Certificate and PrivateKey from PemUtils
### v6.7.0
- Added OCSP support
### v6.6.3
- Disabled providing transitive dependencies for Netty, Jetty, Apache 4 and Apache 5
- Added shorter alternative for withTrustingAllCertificatesWithoutValidation option within SSLFactory called withUnsafeTrustMaterial
- Marked to be removal methods as deprecated within SSLFactory and KeyStoreUtils. Deprecated methods will be removed on version 7.0.0
### v6.6.2
- Made certificate extractor compatible with java 15+
### v6.6.1
- Included root-ca in the certificate extractor within CertificateUtils
### v6.6.0
- Added server certificate extractor to CertificateUtils
- Bumped the version of transitive dependencies
- Simplified PemUtils for parsing trust material
- Added detailed logging within the UnsafeX509ExtendedTrustManager
### v6.5.0
- Removed deprecated class and methods for SocketUtils
- Added null check for ssl sessions
- Fixed javadoc warnings
- Added close statements for streams within PemUtils
### v6.4.0
- Update client identity routes at runtime
- Performance improvements such as Lazy creation of some SSL materials
- Added SSLSessionUtils
- Marked SocketUtils as deprecated, alternative is SSLSocketUtils
### v6.3.0
- Added a toggle to hot swap identity material within the SSLFactory
- Added a toggle to hot swap trust material within the SSLFactory
- Added an option to route multiple client identities
### v6.2.0
- Added option to hot swap identity material at runtime
- Added option to hot swap trust material at runtime
- Added option to supply preconfigured ssl engine
- Added support for requiring client authentication from server side
- Removed deprecated method
### v6.1.1
- Switched to system line separator
- Simplified TrustManager
- Removed redundant wrapping of KeyManager and TrustManager
- Renamed sslContextProtocol to sslContextAlgorithm
- Marked sslContextProtocol method as deprecated
- Fixed typos
- Support for loading certificate with "-----BEGIN TRUSTED CERTIFICATE-----" header

### v6.1.0
- Added license header
- Added author
- Added option to create TrustManagerFactory from SSLFactory and TrustManagerUtils
- Added option to create KeyManagerFactory from SSLFactory and KeyManagerUtils
- Made SSLFactory less strict by supporting X509KeyManager and X509TrustManager
- Added option to supply identity as a InputStream for the SSLFactory Builder
- Added option to supply trustStore as a InputStream for the SSLFactory Builder
- Moved KeyManagerBuilder to KeyManagerUtils
- Moved TrustManagerBuilder to TrustManagerUtils
- Added SocketUtils
- Added SSLContextUtils
- Simplified SSLFactory
- Wrapped checked exceptions with unchecked exceptions

### v6.0.0
- Renamed package from nl.altindag.sslcontext to nl.altindag.ssl
- Added UnsafeTrustManager into the TrustManagerUtils
- Removed deprecated methods
- Renamed ApacheSslContextUtils to Apache4SslUtils
- Added Apache5SslUtils
- Renamed NettySslContextUtils to NettySslUtils
- Renamed JettySslContextUtils to JettySslUtils
- Added option to create X509ExtendedKeyManager from PEM as String 
- Added option to create X509ExtendedTrustManager from PEM as String 

### v5.4.0
- Ability to wrap old X509KeyManager into X509ExtendedKeyManager
- Ability to wrap old X509TrustManager into X509ExtendedTrustManager
- Added CertificateUtils
- Support for custom Security Provider and SSLContext protocol
- Marked ApacheSslContextUtils#toLayeredConnectionSocketFactory deprecated

### v5.3.0
- Added wrapped class for SSLServerSocketFactory and SSLSocketFactory
- Enriched SSLFactory with SSLServerSocketFactory and SSLSocketFactory

### v5.2.4
- Disabled lazy initialization of list of protocols and ciphers

### v5.2.3
- Support for custom list of ciphers and protocols

### v5.2.2
- Improved the algorithm for parsing PEM formatted private key

### v5.2.1
- Added support for parsing different types of PEM formatted private keys

### v5.2.0
- Support for loading PEM formatted ssl materials for SSLFactory
- With support for private key, certificate chain and trusted certificates

### v5.1.0
- Support for loading Windows and Mac OS X trusted certificates

### v5.0.1
- Added option to build SSLFactory with KeyStore for trust material without supplying password

### v5.0.0
- Construct SSLFactory with either key material or trust material
- Marked TrustManager as Optional

### v4.1.0
- Removed default SecureRandom object
- Disabled password validation

### v4.0.0
- Removed deprecated methods withTrustStore and withIdentity

### v3.1.2
- Marked withDefaultJdkTrustStore as deprecated

### v3.1.1
- Renamed method withTrustStore to withTrustMaterial
- Renamed method withIdentity to withIdentityMaterial
- Marked withTrustStore and withIdentity as deprecated
- Removed commons-lang3 lib of Apache

### v3.1.0
- Disabled password caching by default and added option to enable it
- Added option to initialize KeyMaterial with custom KeyStore password and Key password

### v3.0.9
- Limited the support of creating CompositeX509ExtendedKeyManager only with X509ExtendedKeyManager
- Limited the support of creating CompositeX509ExtendedTrustManager only with X509ExtendedTrustManager
- Removed support for less secure X509KeyManager and X509TrustManager

### v3.0.8
- Removed isSecurityEnabled function
- Removed isOneWayAuthenticationEnabled function
- Removed isTwoWayAuthenticationEnabled function
- Marked KeyManager as Optional

### v3.0.7
- Added Apache license
- Removed CompositeX509TrustManager
- Added CompositeX509ExtendedTrustManager
- Added CompositeX509ExtendedKeyManager

### v3.0.6
- Removed not required libraries from core library
- Created mapper as separate project for Netty
- Created mapper as separate project for Jetty

### v3.0.5
- Added JettySslContextUtils

### v3.0.4
- Added NettySslContextUtils
- Renamed KeyStoreUtils
- Added Netty mapper into the SSLFactory

### v3.0.3
- Improved exception handler of CompositeX509TrustManager

### v3.0.2
- Updated license and copyright

### v3.0.1
- Added logger into CompositeX509TrustManager
- Marked KeyManagerUtils as final
- Renamed getTrustedCertificate to getTrustedCertificates

### v3.0.0
- Improved exception handling
- Added support for using multiple key materials
- Added logger into UnsafeTrustManager
- Added KeyManagerUtils
- Renamed SSLContextHelper to SSLFactory

### v2.0.0
- Changed data type of passwords from String to char array

### v1.0.3
- Added license

### v1.0.2
- Made trust material optional for SSLFactory

### v1.0.1
- Added jar type within the pom

### v1.0.0
- Initial release
