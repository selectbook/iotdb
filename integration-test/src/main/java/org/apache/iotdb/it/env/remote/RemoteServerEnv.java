/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.it.env.remote;

import org.apache.iotdb.common.rpc.thrift.TEndPoint;
import org.apache.iotdb.commons.client.ClientPoolFactory;
import org.apache.iotdb.commons.client.IClientManager;
import org.apache.iotdb.commons.client.exception.ClientManagerException;
import org.apache.iotdb.commons.client.sync.SyncConfigNodeIServiceClient;
import org.apache.iotdb.confignode.rpc.thrift.IConfigNodeRPCService;
import org.apache.iotdb.isession.ISession;
import org.apache.iotdb.it.env.EnvFactory;
import org.apache.iotdb.it.env.cluster.ConfigNodeWrapper;
import org.apache.iotdb.it.env.cluster.DataNodeWrapper;
import org.apache.iotdb.itbase.env.BaseEnv;
import org.apache.iotdb.itbase.env.ClusterConfig;
import org.apache.iotdb.jdbc.Config;
import org.apache.iotdb.jdbc.Constant;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.apache.iotdb.jdbc.Config.VERSION;
import static org.junit.Assert.fail;

public class RemoteServerEnv implements BaseEnv {

  private final String ip_addr = System.getProperty("RemoteIp", "127.0.0.1");
  private final String port = System.getProperty("RemotePort", "6667");
  private final String user = System.getProperty("RemoteUser", "root");
  private final String password = System.getProperty("RemotePassword", "root");
  private IClientManager<TEndPoint, SyncConfigNodeIServiceClient> clientManager;
  private RemoteClusterConfig clusterConfig = new RemoteClusterConfig();

  @Override
  public void initClusterEnvironment() {
    try (Connection connection = EnvFactory.getEnv().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE root.init;");
      statement.execute("DELETE DATABASE root;");
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    clientManager =
        new IClientManager.Factory<TEndPoint, SyncConfigNodeIServiceClient>()
            .createClientManager(new ClientPoolFactory.SyncConfigNodeIServiceClientPoolFactory());
  }

  @Override
  public void initClusterEnvironment(int configNodesNum, int dataNodesNum) {
    initClusterEnvironment();
  }

  @Override
  public void cleanClusterEnvironment() {
    clientManager.close();
    clusterConfig = new RemoteClusterConfig();
  }

  @Override
  public ClusterConfig getConfig() {
    return clusterConfig;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = null;
    try {
      Class.forName(Config.JDBC_DRIVER_NAME);
      connection =
          DriverManager.getConnection(
              Config.IOTDB_URL_PREFIX + ip_addr + ":" + port, this.user, this.password);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    return connection;
  }

  @Override
  public Connection getConnection(Constant.Version version, String username, String password)
      throws SQLException {
    Connection connection = null;
    try {
      Class.forName(Config.JDBC_DRIVER_NAME);
      connection =
          DriverManager.getConnection(
              Config.IOTDB_URL_PREFIX
                  + ip_addr
                  + ":"
                  + port
                  + "?"
                  + VERSION
                  + "="
                  + version.toString(),
              this.user,
              this.password);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    return connection;
  }

  public void setTestMethodName(String testCaseName) {
    // Do nothing
  }

  @Override
  public void dumpTestJVMSnapshot() {
    // Do nothing
  }

  @Override
  public List<ConfigNodeWrapper> getConfigNodeWrapperList() {
    return null;
  }

  @Override
  public List<DataNodeWrapper> getDataNodeWrapperList() {
    return null;
  }

  @Override
  public IConfigNodeRPCService.Iface getLeaderConfigNodeConnection() throws ClientManagerException {
    return clientManager.borrowClient(new TEndPoint(ip_addr, 10710));
  }

  @Override
  public ISession getSessionConnection() throws IoTDBConnectionException {
    Session session = new Session(ip_addr, Integer.parseInt(port));
    session.open();
    return session;
  }

  @Override
  public int getLeaderConfigNodeIndex() {
    return -1;
  }

  @Override
  public void startConfigNode(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdownConfigNode(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConfigNodeWrapper generateRandomConfigNodeWrapper() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataNodeWrapper generateRandomDataNodeWrapper() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConfigNodeWrapper getConfigNodeWrapper(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataNodeWrapper getDataNodeWrapper(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerNewDataNode(boolean isNeedVerify) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerNewConfigNode(boolean isNeedVerify) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerNewDataNode(DataNodeWrapper newDataNodeWrapper, boolean isNeedVerify) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerNewConfigNode(ConfigNodeWrapper newConfigNodeWrapper, boolean isNeedVerify) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void startDataNode(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdownDataNode(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMqttPort() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getIP() {
    return ip_addr;
  }

  @Override
  public String getPort() {
    return port;
  }

  @Override
  public String getSbinPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getToolsPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getLibPath() {
    throw new UnsupportedOperationException();
  }
}
