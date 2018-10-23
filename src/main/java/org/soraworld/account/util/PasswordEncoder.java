/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.soraworld.account.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordEncoder {

    private final int strength;
    private final BCryptVersion version;
    private final SecureRandom random;

    private final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2([ayb])?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    public PasswordEncoder() {
        this(-1);
    }

    public PasswordEncoder(int strength) {
        this(strength, null);
    }

    public PasswordEncoder(BCryptVersion version) {
        this(version, null);
    }

    public PasswordEncoder(BCryptVersion version, SecureRandom random) {
        this(version, -1, random);
    }

    public PasswordEncoder(int strength, SecureRandom random) {
        this(BCryptVersion.$2A, strength, random);
    }

    public PasswordEncoder(BCryptVersion version, int strength) {
        this(version, strength, null);
    }

    public PasswordEncoder(BCryptVersion version, int strength, SecureRandom random) {
        if (strength != -1 && (strength < BCrypt.MIN_LOG_ROUNDS || strength > BCrypt.MAX_LOG_ROUNDS)) {
            throw new IllegalArgumentException("Bad strength");
        }
        this.version = version;
        this.strength = strength;
        this.random = random;
    }

    public String encode(CharSequence rawPassword) {
        String salt;
        if (strength > 0) {
            if (random != null) {
                salt = BCrypt.gensalt(version.getVersion(), strength, random);
            } else {
                salt = BCrypt.gensalt(version.getVersion(), strength);
            }
        } else {
            salt = BCrypt.gensalt(version.getVersion());
        }
        return BCrypt.hashpw(rawPassword.toString(), salt);
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.length() == 0) {
            System.out.println("Empty encoded password");
            return false;
        }

        if (!BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            System.out.println("Encoded password does not look like BCrypt");
            return false;
        }

        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }

    public enum BCryptVersion {
        $2A("$2a"),
        $2Y("$2y"),
        $2B("$2b");

        private final String version;

        BCryptVersion(String version) {
            this.version = version;
        }

        public String getVersion() {
            return this.version;
        }
    }
}
