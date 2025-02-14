/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.sshd.client.session.ClientSession;

/**
 * Basic common functionality for a {@link StatefulProxyConnector}.
 */
public abstract class AbstractClientProxyConnector implements StatefulProxyConnector {

  private static final long DEFAULT_PROXY_TIMEOUT_MILLIS = TimeUnit.SECONDS
      .toMillis(30L);

  /**
   * Guards {@link #done} and {@link #bufferedCommands}.
   */
  private final Object lock = new Object();
  private boolean done;

  private List<Callable<Void>> bufferedCommands = new ArrayList<>();

  private AtomicReference<Runnable> unregister = new AtomicReference<>();

  private long remainingProxyProtocolTime = DEFAULT_PROXY_TIMEOUT_MILLIS;

  private long lastProxyOperationTime = 0L;

  /**
   * The ultimate remote address to connect to.
   */
  protected final InetSocketAddress remoteAddress;

  public InetSocketAddress getProxyAddress() {
    return proxyAddress;
  }

  /**
   * The proxy address.
   */
  protected final InetSocketAddress proxyAddress;

  public String getProxyUser() {
    return proxyUser;
  }

  public char[] getProxyPassword() {
    return proxyPassword;
  }

  /**
   * The user to authenticate at the proxy with.
   */
  protected String proxyUser;

  /**
   * The password to use for authentication at the proxy.
   */
  protected char[] proxyPassword;

  /**
   * Creates a new {@link AbstractClientProxyConnector}.
   *
   * @param proxyAddress  of the proxy server we're connecting to
   * @param remoteAddress of the target server to connect to
   * @param proxyUser     to authenticate at the proxy with; may be {@code null}
   * @param proxyPassword to authenticate at the proxy with; may be {@code null}
   */
  protected AbstractClientProxyConnector(InetSocketAddress proxyAddress,
                                         InetSocketAddress remoteAddress, String proxyUser,
                                         char[] proxyPassword) {
    this.proxyAddress = proxyAddress;
    this.remoteAddress = remoteAddress;
    this.proxyUser = proxyUser;
    this.proxyPassword = proxyPassword == null ? new char[0]
        : proxyPassword;
  }

  /**
   * Initializes this instance. Installs itself as proxy handler on the session.
   *
   * @param session to initialize for
   */
  protected void init(ClientSession session) {
    remainingProxyProtocolTime = session.getLongProperty(
                                                         StatefulProxyConnector.TIMEOUT_PROPERTY,
                                                         DEFAULT_PROXY_TIMEOUT_MILLIS);
    if (remainingProxyProtocolTime <= 0L) {
      remainingProxyProtocolTime = DEFAULT_PROXY_TIMEOUT_MILLIS;
    }
  }

  /**
   * Obtains the timeout for the whole rest of the proxy connection protocol.
   *
   * @return the timeout in milliseconds, always &gt; 0L
   */
  protected long getTimeout() {
    long last = lastProxyOperationTime;
    long now = System.nanoTime();
    lastProxyOperationTime = now;
    long remaining = remainingProxyProtocolTime;
    if (last != 0L) {
      long elapsed = now - last;
      remaining -= elapsed;
      if (remaining < 0L) {
        remaining = 10L; // Give it grace period.
      }
    }
    remainingProxyProtocolTime = remaining;
    return remaining;
  }

  /**
   * Adjusts the timeout calculation to not account of elapsed time since the last time the timeout was gotten. Can be used for
   * instance to ignore time spent in user dialogs be counted against the overall proxy connection protocol timeout.
   */
  public void adjustTimeout() {
    lastProxyOperationTime = System.nanoTime();
  }

  /**
   * Sets the "done" flag.
   *
   * @param success whether the connector terminated successfully.
   * @throws Exception if starting ssh fails
   */
  public void setDone(boolean success) throws Exception {
    List<Callable<Void>> buffered;
    Runnable unset = unregister.getAndSet(null);
    if (unset != null) {
      unset.run();
    }
    synchronized (lock) {
      done = true;
      buffered = bufferedCommands;
      bufferedCommands = null;
    }
    if (success && buffered != null) {
      for (Callable<Void> starter : buffered) {
        starter.call();
      }
    }
  }

  @Override
  public void runWhenDone(Callable<Void> starter) throws Exception {
    synchronized (lock) {
      if (!done) {
        bufferedCommands.add(starter);
        return;
      }
    }
    starter.call();
  }

  /**
   * Clears the proxy password.
   */
  public void clearPassword() {
    Arrays.fill(proxyPassword, '\000');
    proxyPassword = new char[0];
  }

  public boolean isDone() {
    return done;
  }
}
