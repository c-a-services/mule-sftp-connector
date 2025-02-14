/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple representation of an authentication challenge as sent in a "WWW-Authenticate" or "Proxy-Authenticate" header. Such
 * challenges start with a mechanism name, followed either by one single token, or by a list of key=value pairs.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7235#section-2.1">RFC 7235, sec. 2.1</a>
 */
public class AuthenticationChallenge {

  private final String mechanism;

  private String token;

  private Map<String, String> arguments;

  /**
   * Create a new {@link AuthenticationChallenge} with the given mechanism.
   *
   * @param mechanism for the challenge
   */
  public AuthenticationChallenge(String mechanism) {
    this.mechanism = mechanism;
  }

  /**
   * Retrieves the authentication mechanism specified by this challenge, for instance "Basic".
   *
   * @return the mechanism name
   */
  public String getMechanism() {
    return mechanism;
  }

  /**
   * Retrieves the token of the challenge, if any.
   *
   * @return the token, or {@code null} if there is none.
   */
  public String getToken() {
    return token;
  }

  /**
   * Retrieves the arguments of the challenge.
   *
   * @return a possibly empty map of the key=value arguments of the challenge
   */

  public Map<String, String> getArguments() {
    return arguments == null ? Collections.emptyMap() : arguments;
  }

  void addArgument(String key, String value) {
    if (arguments == null) {
      arguments = new LinkedHashMap<>();
    }
    arguments.put(key, value);
  }

  void setToken(String token) {
    this.token = token;
  }

  @Override
  public String toString() {
    return "AuthenticationChallenge[" + mechanism + ',' + token + ',' //$NON-NLS-1$
        + (arguments == null ? "<none>" : arguments.toString()) + ']'; //$NON-NLS-1$
  }
}
