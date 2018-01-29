/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.sources.v2.streaming.writer;

import org.apache.spark.annotation.InterfaceStability;
import org.apache.spark.sql.sources.v2.writer.DataSourceV2Writer;
import org.apache.spark.sql.sources.v2.writer.DataWriter;
import org.apache.spark.sql.sources.v2.writer.WriterCommitMessage;

/**
 * A {@link DataSourceV2Writer} for use with structured streaming. This writer handles commits and
 * aborts relative to an epoch ID determined by the execution engine.
 *
 * {@link DataWriter} implementations generated by a StreamWriter may be reused for multiple epochs,
 * and so must reset any internal state after a successful commit.
 */
@InterfaceStability.Evolving
public interface StreamWriter extends DataSourceV2Writer {
  /**
   * Commits this writing job for the specified epoch with a list of commit messages. The commit
   * messages are collected from successful data writers and are produced by
   * {@link DataWriter#commit()}.
   *
   * If this method fails (by throwing an exception), this writing job is considered to have been
   * failed, and the execution engine will attempt to call {@link #abort(WriterCommitMessage[])}.
   *
   * To support exactly-once processing, writer implementations should ensure that this method is
   * idempotent. The execution engine may call commit() multiple times for the same epoch
   * in some circumstances.
   */
  void commit(long epochId, WriterCommitMessage[] messages);

  /**
   * Aborts this writing job because some data writers are failed and keep failing when retry, or
   * the Spark job fails with some unknown reasons, or {@link #commit(WriterCommitMessage[])} fails.
   *
   * If this method fails (by throwing an exception), the underlying data source may require manual
   * cleanup.
   *
   * Unless the abort is triggered by the failure of commit, the given messages should have some
   * null slots as there maybe only a few data writers that are committed before the abort
   * happens, or some data writers were committed but their commit messages haven't reached the
   * driver when the abort is triggered. So this is just a "best effort" for data sources to
   * clean up the data left by data writers.
   */
  void abort(long epochId, WriterCommitMessage[] messages);

  default void commit(WriterCommitMessage[] messages) {
    throw new UnsupportedOperationException(
        "Commit without epoch should not be called with StreamWriter");
  }

  default void abort(WriterCommitMessage[] messages) {
    throw new UnsupportedOperationException(
        "Abort without epoch should not be called with StreamWriter");
  }
}