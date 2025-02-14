/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static java.util.Arrays.asList;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SftpServer {

  public static final String USERNAME = "muletest1";
  public static final String PASSWORD = "muletest1";
  private SshServer sshdServer;
  private Integer port;
  private Path path;

  public SftpServer(int port, Path path) {
    this.port = port;
    this.path = path;
    configureSecurityProvider();
    SftpSubsystemFactory factory = createFtpSubsystemFactory();
    sshdServer = SshServer.setUpDefaultServer();
    configureSshdServer(factory);
  }

  public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
    sshdServer.setPasswordAuthenticator(passwordAuthenticator);
  }

  public void setPasswordAuthenticator() {
    sshdServer.setPasswordAuthenticator(passwordAuthenticator());
  }

  public void setPublicKeyAuthenticator(PublickeyAuthenticator publicKeyAuthenticator) {
    sshdServer.setPublickeyAuthenticator(publicKeyAuthenticator);
  }

  private void configureSshdServer(SftpSubsystemFactory factory) {
    sshdServer.setPort(port);
    sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));
    sshdServer.setSubsystemFactories(asList(factory));
    sshdServer.setCommandFactory(new ScpCommandFactory());
    sshdServer.setFileSystemFactory(new VirtualFileSystemFactory(path));
  }

  private SftpSubsystemFactory createFtpSubsystemFactory() {
    return new SftpSubsystemFactory();
  }

  private void configureSecurityProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  private static PasswordAuthenticator passwordAuthenticator() {
    return (arg0, arg1, arg2) -> USERNAME.equals(arg0) && PASSWORD.equals(arg1);
  }

  public void start() {
    try {
      if (sshdServer == null) {
        sshdServer = SshServer.setUpDefaultServer();
        configureSshdServer(createFtpSubsystemFactory());
      }
      sshdServer.start();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public void stop() {
    try {
      sshdServer.stop(false);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    sshdServer = null;
  }
}
