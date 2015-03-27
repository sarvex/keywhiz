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

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import keywhiz.cli.Printing;
import keywhiz.cli.configs.ListActionConfig;
import keywhiz.client.KeywhizClient;

public class ListAction implements Runnable {

  private final ListActionConfig listActionConfig;
  private final KeywhizClient keywhizClient;
  private final Printing printing;

  public ListAction(ListActionConfig listActionConfig, KeywhizClient client, Printing printing) {
    this.listActionConfig = listActionConfig;
    this.keywhizClient = client;
    this.printing = printing;
  }

  @Override public void run() {
    List<String> listOptions = listActionConfig.listOptions;
    if (listOptions == null) {
      try {
        printing.printAllSanitizedSecrets(keywhizClient.allSecrets(),
            Arrays.asList("groups", "clients", "metadata"));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
      return;
    }

    List<String> options = Arrays.asList(listOptions.get(0).split(","));

    String firstOption = options.get(0).toLowerCase().trim();
    try {
      switch (firstOption) {
        case "groups":
          printing.printAllGroups(keywhizClient.allGroups(), options);
          break;

        case "clients":
          printing.printAllClients(keywhizClient.allClients(), options);
          break;

        case "secrets":
          printing.printAllSanitizedSecrets(keywhizClient.allSecrets(), options);
          break;

        default:
          throw new AssertionError("Invalid list option: " + firstOption);
      }
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
