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

package keywhiz.cli.configs;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.List;

@Parameters(commandDescription = "Add clients, groups, or secrets to KeyWhiz")
public class AddActionConfig {
  @Parameter(description = "<client|group|secret>")
  public List<String> addType;

  @Parameter(names = "--name", description = "Name of the item to add", required = true)
  public String name;

  @Parameter(names = "--json", description = "Metadata JSON blob")
  public String json;

  @Parameter(names = { "-s", "--with-version" }, description = "Append a version stamp to the name (secrets only)")
  public boolean withVersion = false;

  @Parameter(names = { "-g", "--group" }, description = "Also assign the secret to this group (secrets only)")
  public String group;
}
