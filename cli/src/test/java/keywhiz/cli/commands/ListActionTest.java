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

package keywhiz.cli.commands;

import java.util.Arrays;
import keywhiz.cli.Printing;
import keywhiz.cli.configs.ListActionConfig;
import keywhiz.client.KeywhizClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ListActionTest {

  @Mock KeywhizClient keywhizClient;
  @Mock Printing printing;

  ListActionConfig listActionConfig;
  ListAction listAction;

  @Before
  public void setUp() {
    listActionConfig = new ListActionConfig();
    listAction = new ListAction(listActionConfig, keywhizClient, printing);
  }

  @Test
  public void listCallsPrintForListAll() throws Exception {
    listActionConfig.listOptions = null;
    listAction.run();
    verify(printing).printAllSanitizedSecrets(keywhizClient.allSecrets(), Arrays.asList("groups", "clients", "metadata"));
  }

  @Test
  public void listCallsPrintForListGroups() throws Exception {
    listActionConfig.listOptions = Arrays.asList("groups");
    listAction.run();

    verify(printing).printAllGroups(keywhizClient.allGroups(), listActionConfig.listOptions);
  }

  @Test
  public void listCallsPrintForListClients() throws Exception {
    listActionConfig.listOptions = Arrays.asList("clients");
    listAction.run();

    verify(printing).printAllClients(keywhizClient.allClients(), listActionConfig.listOptions);
  }

  @Test
  public void listCallsPrintForListSecrets() throws Exception {
    listActionConfig.listOptions = Arrays.asList("secrets");
    listAction.run();

    verify(printing).printAllSanitizedSecrets(keywhizClient.allSecrets(), listActionConfig.listOptions);
  }

  @Test(expected = AssertionError.class)
  public void listThrowsIfInvalidType() throws Exception {
    listActionConfig.listOptions = Arrays.asList("invalid_type");
    listAction.run();
  }
}
