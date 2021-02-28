/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.cclient.nifi.email.SendEmail;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSendRealEmail {

    private TestRunner runner;

    @Before
    public void init() {
        runner = TestRunners.newTestRunner(SendEmail.class);
        Map<String, String> attrs = new HashMap<>();
        attrs.put("a0", "v0");
        attrs.put("a1", "v1");
        attrs.put("a2", "v2");
        attrs.put("a3", "v3");

        MockFlowFile flowFile0 = new MockFlowFile(0);
        MockFlowFile flowFile1 = new MockFlowFile(1);
        MockFlowFile flowFile2 = new MockFlowFile(2);

        flowFile0.putAttributes(attrs);
        flowFile1.putAttributes(attrs);
        flowFile2.putAttributes(attrs);
        runner.enqueue(flowFile0, flowFile1, flowFile2);
        runner.setProperty(SendEmail.SMTP_HOSTNAME, "smtp.163.com");
        runner.setProperty(SendEmail.SMTP_TLS, "true");
        runner.setProperty(SendEmail.SMTP_PORT, "465");
        runner.setProperty(SendEmail.HEADER_XMAILER, "TestingNiFi");
        runner.setProperty(SendEmail.MESSAGE, "Message Body");
        runner.setProperty(SendEmail.GROUP_SIZE, "1000");
        runner.setProperty(SendEmail.SMTP_AUTH, "true");

        runner.setProperty(SendEmail.FROM, "[emial_from]@gmail.com");
        runner.setProperty(SendEmail.TO, "[emial_to]@hotmail.com");
        runner.setProperty(SendEmail.SMTP_USERNAME, "[smtp_username]");
        runner.setProperty(SendEmail.SMTP_PASSWORD, "[smtp account token/code]");
    }

    @Test
    public void testProcessor() {
//        runner.run();
//        List<MockFlowFile> result = runner.getFlowFilesForRelationship(SendEmail.REL_SUCCESS);
//        System.out.println(result.size());
//        Assert.assertEquals(1, result.size());
    }

}
