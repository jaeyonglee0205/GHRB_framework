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
package nl.altindag.ssl.keymanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.X509KeyManager;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class X509KeyManagerWrapperShould {

    @Test
    void chooseClientAlias() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.chooseClientAlias(any(), any(), any())).thenReturn("alias");

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String clientAlias = victim.chooseClientAlias(null, null, null);

        assertThat(clientAlias).isEqualTo("alias");
        verify(keyManager, times(1)).chooseClientAlias(null, null, null);
    }

    @Test
    void chooseEngineClientAlias() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.chooseClientAlias(any(), any(), any())).thenReturn("alias");

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String clientAlias = victim.chooseEngineClientAlias(null, null, null);

        assertThat(clientAlias).isEqualTo("alias");
        verify(keyManager, times(1)).chooseClientAlias(null, null, null);
    }

    @Test
    void chooseServerAlias() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.chooseServerAlias(any(), any(), any())).thenReturn("alias");

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String clientAlias = victim.chooseServerAlias(null, null, null);

        assertThat(clientAlias).isEqualTo("alias");
        verify(keyManager, times(1)).chooseServerAlias(null, null, null);
    }

    @Test
    void chooseEngineServerAlias() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.chooseServerAlias(any(), any(), any())).thenReturn("alias");

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String clientAlias = victim.chooseEngineServerAlias(null, null, null);

        assertThat(clientAlias).isEqualTo("alias");
        verify(keyManager, times(1)).chooseServerAlias(null, null, null);
    }

    @Test
    void getPrivateKey() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.getPrivateKey(anyString())).thenReturn(mock(PrivateKey.class));

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        PrivateKey privateKey = victim.getPrivateKey("alias");

        assertThat(privateKey).isNotNull();
        verify(keyManager, times(1)).getPrivateKey("alias");
    }

    @Test
    void getCertificateChain() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.getCertificateChain(anyString())).thenReturn(new X509Certificate[] { mock(X509Certificate.class) } );

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        X509Certificate[] certificateChain = victim.getCertificateChain("alias");

        assertThat(certificateChain).hasSize(1);
        verify(keyManager, times(1)).getCertificateChain("alias");
    }

    @Test
    void getClientAliases() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.getClientAliases(any(), any())).thenReturn(new String[]{"alias-1", "alias-2"});

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String[] clientAliases = victim.getClientAliases(null, null);

        assertThat(clientAliases).containsExactlyInAnyOrder("alias-1", "alias-2");
        verify(keyManager, times(1)).getClientAliases(null, null);
    }

    @Test
    void getServerAliases() {
        X509KeyManager keyManager = mock(X509KeyManager.class);
        when(keyManager.getServerAliases(any(), any())).thenReturn(new String[]{"alias-1", "alias-2"});

        X509KeyManagerWrapper victim = new X509KeyManagerWrapper(keyManager);
        String[] clientAliases = victim.getServerAliases(null, null);

        assertThat(clientAliases).containsExactlyInAnyOrder("alias-1", "alias-2");
        verify(keyManager, times(1)).getServerAliases(null, null);
    }

    @Test
    void throwExceptionWhenKeyManagerIsNotPresent() {
        assertThatThrownBy(() -> new X509KeyManagerWrapper(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No valid KeyManager has been provided. KeyManager must be present, but was absent.");
    }

}
