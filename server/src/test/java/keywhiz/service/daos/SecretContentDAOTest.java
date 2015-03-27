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

package keywhiz.service.daos;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import keywhiz.TestDBRule;
import keywhiz.api.model.SecretContent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static keywhiz.jooq.tables.Secrets.SECRETS;
import static keywhiz.jooq.tables.SecretsContent.SECRETS_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SecretContentDAOTest {
  @Rule public final TestDBRule testDBRule = new TestDBRule();

  final static OffsetDateTime date = OffsetDateTime.now(ZoneId.of("UTC"));
  SecretContent secretContent1 = SecretContent.of(11, 22, "[crypted]", "", date, "creator", date,
      "creator");

  SecretContentDAO secretContentDAO;

  @Before
  public void setUp() throws Exception {
    secretContentDAO = testDBRule.getDbi().onDemand(SecretContentDAO.class);

    testDBRule.jooqContext().delete(SECRETS).execute();
    testDBRule.jooqContext().insertInto(SECRETS, SECRETS.ID, SECRETS.NAME, SECRETS.METADATA)
        .values((int) secretContent1.secretSeriesId(), "secretName", "{}")
        .execute();
    testDBRule.jooqContext().insertInto(SECRETS_CONTENT)
        .set(SECRETS_CONTENT.ID, (int) secretContent1.id())
        .set(SECRETS_CONTENT.SECRETID, (int) secretContent1.secretSeriesId())
        .set(SECRETS_CONTENT.ENCRYPTED_CONTENT, secretContent1.encryptedContent())
        .set(SECRETS_CONTENT.VERSION, secretContent1.version().orElse(null))
        .set(SECRETS_CONTENT.CREATEDAT, secretContent1.createdAt())
        .set(SECRETS_CONTENT.CREATEDBY, secretContent1.createdBy())
        .set(SECRETS_CONTENT.UPDATEDAT, secretContent1.updatedAt())
        .set(SECRETS_CONTENT.UPDATEDBY, secretContent1.updatedBy())
        .execute();
  }

  @Test public void createSecretContent() {
    int before = tableSize();
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted", "version", "creator");
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted2", "version2", "creator");
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted3", "version3", "creator");
    assertThat(tableSize()).isEqualTo(before + 3);
  }

  @Test public void getSecretContentById() {
    SecretContent actualSecretContent = secretContentDAO.getSecretContentById(secretContent1.id())
        .orElseThrow(RuntimeException::new);
    assertThat(actualSecretContent).isEqualTo(secretContent1);
  }

  @Test public void getSecretContentsBySecretId() {
    long id1 = secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted", "version", "creator");
    long id2 = secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted2", "version2", "creator");
    long id3 = secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted3", "version3", "creator");

    List<Long> actualIds = secretContentDAO.getSecretContentsBySecretId(secretContent1.secretSeriesId())
        .stream()
        .map((content) -> (content == null) ? 0 : content.id())
        .collect(toList());

    assertThat(actualIds).containsExactly(secretContent1.id(), id1, id2, id3);
  }

  @Test public void getVersionsBySecretId() {
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted", "version", "creator");
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted2", "version2", "creator");
    secretContentDAO.createSecretContent(secretContent1.secretSeriesId(), "encrypted3", "version3", "creator");

    // We have the empty string as a version from the setUp() call
    assertThat(secretContentDAO.getVersionFromSecretId(secretContent1.secretSeriesId()))
        .hasSameElementsAs(ImmutableList.of("", "version", "version2", "version3"));
  }

  private int tableSize() {
    return testDBRule.jooqContext().fetchCount(SECRETS_CONTENT);
  }
}
