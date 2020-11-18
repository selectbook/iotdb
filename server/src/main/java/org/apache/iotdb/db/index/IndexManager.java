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
package org.apache.iotdb.db.index;

import static org.apache.iotdb.db.index.common.IndexConstant.INDEX_DATA_DIR_NAME;
import static org.apache.iotdb.db.index.common.IndexConstant.META_DIR_NAME;
import static org.apache.iotdb.db.index.common.IndexConstant.ROUTER_DIR;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.conf.directories.DirectoryManager;
import org.apache.iotdb.db.engine.fileSystem.SystemFileFactory;
import org.apache.iotdb.db.exception.StartupException;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.index.common.IndexInfo;
import org.apache.iotdb.db.index.common.IndexMessageType;
import org.apache.iotdb.db.index.common.IndexType;
import org.apache.iotdb.db.index.common.IndexUtils;
import org.apache.iotdb.db.index.common.func.CreateIndexProcessorFunc;
import org.apache.iotdb.db.index.io.IndexBuildTaskPoolManager;
import org.apache.iotdb.db.index.io.IndexChunkMeta;
import org.apache.iotdb.db.index.router.IIndexRouter;
import org.apache.iotdb.db.metadata.PartialPath;
import org.apache.iotdb.db.service.IService;
import org.apache.iotdb.db.service.JMXService;
import org.apache.iotdb.db.service.ServiceType;
import org.apache.iotdb.db.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexManager implements IService {

  private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);
  private SystemFileFactory fsFactory = SystemFileFactory.INSTANCE;
  private final String indexMetaDirPath;
  private final String indexDataDirPath;
  private CreateIndexProcessorFunc createIndexProcessorFunc;
  private final IIndexRouter router;

  private IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();

  /**
   * Tell the index where the feature should be stored.
   *
   * @param path the path on which the index is created, e.g. Root.ery.*.Glu or Root.Wind.d1.Speed.
   * @param indexType the type of index
   * @return the feature directory path for this index.
   */
  private String getFeatureFileDirectory(PartialPath path, IndexType indexType) {
    return indexDataDirPath + File.separator + path.getFullPath() + File.separator + indexType;
  }

  /**
   * Tell the index where the memory structure should be flushed out. Now it's same as feature dir.
   *
   * @param path the path on which the index is created, e.g. Root.ery.*.Glu or Root.Wind.d1.Speed.
   * @param indexType the type of index
   * @return the feature directory path for this index.
   */
  private String getIndexDataDirectory(PartialPath path, IndexType indexType) {
    return getFeatureFileDirectory(path, indexType);
  }

  public void createIndex(List<PartialPath> prefixPaths, IndexInfo indexInfo)
      throws MetadataException {
    if (!prefixPaths.isEmpty()) {
      router.addIndexIntoRouter(prefixPaths.get(0), indexInfo, createIndexProcessorFunc);
    }
  }

  public void dropIndex(List<PartialPath> prefixPaths, IndexType indexType)
      throws MetadataException {
    if (!prefixPaths.isEmpty()) {
      router.removeIndexFromRouter(prefixPaths.get(0), indexType);
    }
  }

//  public List<IndexProcessor> getNewIndexFileProcessor(String storageGroup, boolean sequence) {
//    if (sequence) {
//      return router.getIndexProcessorByStorageGroup(storageGroup);
//    } else {
//      // In current version, we only build index for the sequence data.
//      return new ArrayList<>();
//    }
//  }
//
//  public void removeIndexProcessor(String storageGroupName, boolean sequence)
//      throws IOException {
//    if (!sequence) {
//      router.removeIndexProcessorByStorageGroup(storageGroupName);
//    }
//  }

  /**
   * TODO This function will hack into other modules.
   *
   * For index techniques which are deeply optimized for IoTDB, the index framework provides a
   * finer-grained interface to inform the index when some IoTDB events occur. Other modules can
   * call this function to pass the specified index message type and other parameters. Indexes can
   * selectively implement these interfaces and register with the index manager for listening. When
   * these events occur, the index manager passes the messages to corresponding indexes.
   *
   * @param path where events occur
   * @param indexMsgType the type of index message
   * @param params variable length of parameters
   */
  public void tellIndexMessage(PartialPath path, IndexMessageType indexMsgType, Object... params) {
    throw new UnsupportedOperationException("Not support the advanced Index Messages");
  }

//  private IndexStorageGroupProcessor createStorageGroupProcessor(String storageGroup) {
//    checkIndexSGMetaDataDir();
//    IndexStorageGroupProcessor processor = processorMap.get(storageGroup);
//    if (processor == null) {
//      processor = new IndexStorageGroupProcessor(storageGroup, indexMetaDirPath);
//      IndexStorageGroupProcessor oldProcessor = processorMap.putIfAbsent(storageGroup, processor);
//      if (oldProcessor != null) {
//        return oldProcessor;
//      }
//    }
//    return processor;
//  }

//  /**
//   * storage group name -> index storage group processor
//   */
//  private final Map<String, IndexStorageGroupProcessor> processorMap;

  private void checkIndexSGMetaDataDir() {
    IndexUtils.breakDown();
    File metaDir = fsFactory.getFile(this.indexMetaDirPath);
    if (!metaDir.exists()) {
      boolean mk = fsFactory.getFile(this.indexMetaDirPath).mkdirs();
      if (mk) {
        logger.info("create index SG metadata folder {}", this.indexMetaDirPath);
        System.out.println("create index SG metadata folder " + this.indexMetaDirPath);
      }
    }
  }


  private synchronized void close() throws IOException {
//    Iterable<IndexProcessor> iterator = router.getAllIndexProcessors();
//    for (IndexProcessor indexProcessor : iterator) {
//      indexProcessor.close();
//    }
    router.serialize();
  }

  @Override
  public void start() throws StartupException {
    if (!config.isEnableIndex()) {
      return;
    }
    IndexBuildTaskPoolManager.getInstance().start();
    try {
      // TODO it's not implemented fully.
      JMXService.registerMBean(this, ServiceType.INDEX_SERVICE.getJmxName());
      router.deserialize(createIndexProcessorFunc);
      recoverIndexData();
    } catch (Exception e) {
      throw new StartupException(this.getID().getName(), e.getMessage());
    }
  }

  @Override
  public void stop() {
    if (!config.isEnableIndex()) {
      return;
    }
    try {
      close();
    } catch (IOException e) {
      logger.error("Close IndexManager failed.", e);
    }
//    IndexBuildTaskPoolManager.getInstance().stop();
    JMXService.deregisterMBean(ServiceType.INDEX_SERVICE.getJmxName());
  }

  @Override
  public ServiceType getID() {
    return ServiceType.INDEX_SERVICE;
  }


  private IndexManager() {
    indexMetaDirPath =
        DirectoryManager.getInstance().getIndexRootFolder() + File.separator + META_DIR_NAME;
    indexDataDirPath = DirectoryManager.getInstance().getIndexRootFolder() + File.separator +
        INDEX_DATA_DIR_NAME;
    router = IIndexRouter.Factory.getIndexRouter(indexMetaDirPath + File.separator + ROUTER_DIR);
    createIndexProcessorFunc = indexSeries -> new IndexProcessor(
        indexSeries, indexDataDirPath + File.separator + indexSeries);
//    indexUsability = IIndexUsable.Factory.getIndexUsability();
  }

  public static IndexManager getInstance() {
    return InstanceHolder.instance;
  }

  public synchronized void deleteAll() throws IOException {
    logger.info("Start deleting all storage groups' timeseries");
    close();

    File indexMetaDir = fsFactory.getFile(this.indexMetaDirPath);
    if (indexMetaDir.exists()) {
      FileUtils.deleteDirectory(indexMetaDir);
    }

    File indexDataDir = fsFactory.getFile(this.indexDataDirPath);
    if (indexDataDir.exists()) {
      FileUtils.deleteDirectory(indexDataDir);
    }
    File indexRootDir = fsFactory.getFile(DirectoryManager.getInstance().getIndexRootFolder());
    if (indexRootDir.exists()) {
      FileUtils.deleteDirectory(indexRootDir);
    }
//    clear();
  }

  public IndexMemTableFlushTask getIndexMemFlushTask(String storageGroupPath, boolean sequence) {
    IIndexRouter sgRouter = router.getRouterByStorageGroup(storageGroupPath);
    return new IndexMemTableFlushTask(sgRouter, sequence);
  }


  public IndexManager getIndexRegister() {
    return null;
  }


  private static class InstanceHolder {

    private InstanceHolder() {
    }

    private static IndexManager instance = new IndexManager();
  }

  ////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////

  /**
   * When IoTDB restarts, check all index processors and let them reorganize their dirty data.
   */
  synchronized void recoverIndexData() throws IOException, IllegalPathException {
    IndexUtils.breakDown();
    // Remove files not in the routers
    for (File processorDataDir : Objects
        .requireNonNull(fsFactory.getFile(indexDataDirPath).listFiles())) {
      String processorName = processorDataDir.getName();
      if (!router.hasIndexProcessor(new PartialPath(processorName))) {
        FileUtils.deleteDirectory(processorDataDir);
      }

      // check and remove index not recorded in the routers.
//      Map<IndexType, IndexInfo> indexInfoMap = indexRouter.getAllIndexInfos(processorName);
//      if (indexInfoMap.isEmpty()) {
//        //delete seq files
//        try {
//          FileUtils.deleteDirectory(
//              fsFactory.getFile(indexDataSeqDir + File.separator + processorDataDir));
//          //delete unseq files
//          FileUtils.deleteDirectory(
//              fsFactory.getFile(indexDataUnSeqDir + File.separator + processorDataDir));
//        } catch (NoSuchFileException ignored) {
//        }
//      }
      // delete metadata of processor

//        String indexSGMetaFileName = indexMetaDirPath + File.separator + processorPathName;
//        fsFactory.getFile(indexSGMetaFileName + STORAGE_GROUP_INDEXING_SUFFIX).delete();
//        fsFactory.getFile(indexSGMetaFileName + STORAGE_GROUP_INDEXED_SUFFIX).delete();
    }
  }

  //  public IndexQueryReader getQuerySource(Path seriesPath, IndexType indexType,
//      Filter timeFilter) throws IOException, MetadataException {
//    // TODO it's about the reader
//    String series = seriesPath.getFullPath();
//    StorageGroupMNode storageGroup = MManager.getInstance()
//        .getStorageGroupNodeByPath(new PartialPath(series));
//    String storageGroupName = storageGroup.getName();
//    IndexStorageGroupProcessor sgProcessor = createStorageGroupProcessor(storageGroupName);
//    List<IndexChunkMeta> seq = sgProcessor.getIndexSGMetadata(true, series, indexType);
//    List<IndexChunkMeta> unseq = sgProcessor.getIndexSGMetadata(false, series, indexType);
//    return new IndexQueryReader(seriesPath, indexType, timeFilter, seq, unseq);
//  }
//
//  @TestOnly
//  public List<IndexChunkMeta> getIndexSGMetadata(String storageGroup, boolean sequence,
//      String seriesPath, IndexType indexType) throws IOException {
//    IndexStorageGroupProcessor sgProcessor = createStorageGroupProcessor(storageGroup);
//    return sgProcessor.getIndexSGMetadata(sequence, seriesPath, indexType);
//  }
//
//  /**
//   * Close all opened IndexFileProcessors and clear all data in memory. It's used to simulate the
//   * case that IndexManager re-inits from the scratch after index files have been generated and
//   * sealed. only for test now.
//   */
//  @TestOnly
//  public synchronized void closeAndClear() throws IOException {
//    for (Entry<String, IndexStorageGroupProcessor> entry : processorMap.entrySet()) {
//      IndexStorageGroupProcessor processor = entry.getValue();
//      processor.close();
//    }
//    clear();
//  }
  public void closeAndClear() {

  }

  public List<IndexChunkMeta> getIndexSGMetadata(String storageGroup, boolean b, String p1,
      IndexType elb) {
    return null;
  }

}
