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

import javax.net.ssl.X509ExtendedTrustManager;

import static nl.altindag.ssl.util.ValidationUtils.GENERIC_EXCEPTION_MESSAGE;
import static nl.altindag.ssl.util.ValidationUtils.requireNotNull;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link nl.altindag.ssl.util.TrustManagerUtils TrustManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 *
 * @author Hakan Altindag
 */
public class HotSwappableX509ExtendedTrustManager extends DelegatingX509ExtendedTrustManager {

    public HotSwappableX509ExtendedTrustManager(X509ExtendedTrustManager trustManager) {
        super(trustManager);
    }

    public void setTrustManager(X509ExtendedTrustManager trustManager) {
        this.trustManager = requireNotNull(trustManager, GENERIC_EXCEPTION_MESSAGE.apply("TrustManager"));
    }

}
