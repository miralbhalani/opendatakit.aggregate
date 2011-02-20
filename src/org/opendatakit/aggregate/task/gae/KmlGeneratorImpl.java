/*
 * Copyright (C) 2010 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.task.gae;

import java.util.Map;

import org.opendatakit.aggregate.CallingContext;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.form.PersistentResults;
import org.opendatakit.aggregate.servlet.KmlServlet;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionKey;
import org.opendatakit.aggregate.task.KmlGenerator;
import org.opendatakit.aggregate.task.gae.servlet.KmlGeneratorTaskServlet;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

/**
 * This is a singleton bean. It cannot have any per-request state. It uses a
 * static inner class to encapsulate the per-request state of a running
 * background task.
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class KmlGeneratorImpl implements KmlGenerator {

  @Override
  public void createKmlTask(Form form, SubmissionKey persistentResultsKey, long attemptCount,
      CallingContext cc) throws ODKDatastoreException,
      ODKFormNotFoundException {
    Submission s = Submission.fetchSubmission(persistentResultsKey.splitSubmissionKey(), cc);
    PersistentResults r = new PersistentResults(s);
    Map<String, String> params = r.getRequestParameters();
    TaskOptions task = TaskOptions.Builder.withUrl(ServletConsts.WEB_ROOT
        + KmlGeneratorTaskServlet.ADDR);
    task.method(TaskOptions.Method.GET);
    task.countdownMillis(1);
    task.param(ServletConsts.FORM_ID, form.getFormId());
    task.param(ServletConsts.PERSISTENT_RESULTS_KEY, persistentResultsKey.toString());
    task.param(ServletConsts.ATTEMPT_COUNT, Long.toString(attemptCount));
    task.param(KmlServlet.GEOPOINT_FIELD, params.get(KmlServlet.GEOPOINT_FIELD));
    task.param(KmlServlet.TITLE_FIELD, params.get(KmlServlet.TITLE_FIELD));
    task.param(KmlServlet.IMAGE_FIELD, params.get(KmlServlet.IMAGE_FIELD));
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(task);
  }
}
