package org.apache.iotdb.db.index.common.func;

import java.io.IOException;
import org.apache.iotdb.db.index.IndexProcessor;
import org.apache.iotdb.db.metadata.PartialPath;

/**
 * Do something without input and output.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #act(PartialPath indexSeries)}.
 */
@FunctionalInterface
public interface CreateIndexProcessorFunc {

  /**
   * Do something.
   */
  IndexProcessor act(PartialPath indexSeries);
}
