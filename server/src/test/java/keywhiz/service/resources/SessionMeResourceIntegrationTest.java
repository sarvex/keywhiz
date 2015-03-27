/*
 * Copyright (C) 2015 Square, Inc.
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
package keywhiz.service.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import keywhiz.IntegrationTestRule;
import keywhiz.KeywhizService;
import keywhiz.TestClients;
import keywhiz.auth.User;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static keywhiz.AuthHelper.buildLoginPost;
import static org.assertj.core.api.Assertions.assertThat;

public class SessionMeResourceIntegrationTest {
  ObjectMapper mapper = KeywhizService.customizeObjectMapper(Jackson.newObjectMapper());
  OkHttpClient client;

  @ClassRule public static final RuleChain chain = IntegrationTestRule.rule();

  @Before public void setUp() {
    client = TestClients.unauthenticatedClient();
  }

  @Test public void getInformation() throws IOException {
    User validUser = User.named("keywhizAdmin");
    client.newCall(buildLoginPost(validUser.getName(), "adminPass")).execute();

    Request get = new Request.Builder()
        .get()
        .url("/admin/me/")
        .build();

    Response response = client.newCall(get).execute();

    assertThat(response.body().string())
        .isEqualTo(mapper.writeValueAsString(validUser));
    assertThat(response.code()).isEqualTo(200);
  }

  @Test public void adminRejectsNonLoggedInUser() throws IOException {
    client.newCall(buildLoginPost("username", "password")).execute();

    Request get = new Request.Builder()
        .get()
        .url("/admin/me/")
        .build();

    int status = client.newCall(get).execute().code();
    assertThat(status).isEqualTo(401);
  }
}
