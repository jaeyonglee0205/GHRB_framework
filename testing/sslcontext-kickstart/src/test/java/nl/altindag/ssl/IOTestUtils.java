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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static nl.altindag.ssl.TestConstants.HOME_DIRECTORY;

/**
 * @author Hakan Altindag
 */
public final class IOTestUtils {

    private IOTestUtils() {}

    public static Path copyFileToHomeDirectory(String path, String fileName) throws IOException {
        try (InputStream inputStream = getResourceAsStream(path + fileName)) {
            Path destination = Paths.get(HOME_DIRECTORY, fileName);
            Files.copy(Objects.requireNonNull(inputStream), destination, REPLACE_EXISTING);
            return destination;
        }
    }

    public static InputStream getResourceAsStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

}
