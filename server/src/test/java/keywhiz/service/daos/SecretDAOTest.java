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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import keywhiz.TestDBRule;
import keywhiz.api.model.SecretContent;
import keywhiz.api.model.SecretSeries;
import keywhiz.api.model.SecretSeriesAndContent;
import keywhiz.api.model.VersionGenerator;
import keywhiz.service.crypto.ContentCryptographer;
import keywhiz.service.crypto.CryptoFixtures;
import org.jooq.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skife.jdbi.v2.exceptions.StatementException;

import static keywhiz.jooq.tables.Secrets.SECRETS;
import static keywhiz.jooq.tables.SecretsContent.SECRETS_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SecretDAOTest {
  @Rule public final TestDBRule testDBRule = new TestDBRule();

  final static ContentCryptographer cryptographer = CryptoFixtures.contentCryptographer();
  final static OffsetDateTime date = OffsetDateTime.now();
  final static String version = VersionGenerator.now().toHex();

  SecretSeries series1 = new SecretSeries(1, "secret1", "desc1", date, "creator", date, "updater", null, null, null);
  String content = "c2VjcmV0MQ==";
  String encryptedContent = cryptographer.encryptionKeyDerivedFrom(series1.getName()).encrypt(content);
  SecretContent content1 = SecretContent.of(101, 1, encryptedContent, version, date, "creator", date, "updater");
  SecretSeriesAndContent secret1 = SecretSeriesAndContent.of(series1, content1);

  SecretSeries series2 = new SecretSeries(2, "secret2", "desc2", date, "creator", date, "updater", null, null, null);
  SecretContent content2 = SecretContent.of(102, 2, encryptedContent, "", date, "creator", date, "updater");
  SecretSeriesAndContent secret2 = SecretSeriesAndContent.of(series2, content2);

  SecretDAO secretDAO;
  SecretContentDAO secretContentDAO;

  @Before
  public void setUp() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    testDBRule.jooqContext().delete(SECRETS).execute();

    testDBRule.jooqContext().insertInto(SECRETS)
        .set(SECRETS.ID, (int) secret1.series().getId())
        .set(SECRETS.NAME, secret1.series().getName())
        .set(SECRETS.DESCRIPTION, secret1.series().getDescription().orElse(null))
        .set(SECRETS.CREATEDAT, secret1.series().getCreatedAt())
        .set(SECRETS.CREATEDBY, secret1.series().getCreatedBy())
        .set(SECRETS.UPDATEDBY, secret1.series().getUpdatedBy())
        .set(SECRETS.METADATA, objectMapper.writeValueAsString(secret1.series().getMetadata()))
        .execute();

    testDBRule.jooqContext().insertInto(SECRETS_CONTENT)
        .set(SECRETS_CONTENT.SECRETID, (int) secret1.series().getId())
        .set(SECRETS_CONTENT.VERSION, secret1.content().version().orElse(null))
        .set(SECRETS_CONTENT.ENCRYPTED_CONTENT, secret1.content().encryptedContent())
        .set(SECRETS_CONTENT.CREATEDAT, secret1.content().createdAt())
        .set(SECRETS_CONTENT.CREATEDBY, secret1.content().createdBy())
        .set(SECRETS_CONTENT.UPDATEDBY, secret1.content().updatedBy())
        .execute();

    testDBRule.jooqContext().insertInto(SECRETS)
        .set(SECRETS.ID, (int) secret2.series().getId())
        .set(SECRETS.NAME, secret2.series().getName())
        .set(SECRETS.DESCRIPTION, secret2.series().getDescription().orElse(null))
        .set(SECRETS.CREATEDAT, secret2.series().getCreatedAt())
        .set(SECRETS.CREATEDBY, secret2.series().getCreatedBy())
        .set(SECRETS.UPDATEDBY, secret2.series().getUpdatedBy())
        .set(SECRETS.METADATA, objectMapper.writeValueAsString(secret2.series().getMetadata()))
        .execute();

    testDBRule.jooqContext().insertInto(SECRETS_CONTENT)
        .set(SECRETS_CONTENT.SECRETID, (int) secret2.series().getId())
        .set(SECRETS_CONTENT.VERSION, secret2.content().version().orElse(null))
        .set(SECRETS_CONTENT.ENCRYPTED_CONTENT, secret2.content().encryptedContent())
        .set(SECRETS_CONTENT.CREATEDAT, secret2.content().createdAt())
        .set(SECRETS_CONTENT.CREATEDBY, secret2.content().createdBy())
        .set(SECRETS_CONTENT.UPDATEDBY, secret2.content().updatedBy())
        .execute();

    secretDAO = testDBRule.getDbi().onDemand(SecretDAO.class);
    secretContentDAO = testDBRule.getDbi().onDemand(SecretContentDAO.class);

    // Secrets created in the DB will have different id, updatedAt values.
    secret1 = secretDAO.getSecretByNameAndVersion(
        secret1.series().getName(), secret1.content().version().get()).get();
    secret2 = secretDAO.getSecretByNameAndVersion(
        secret2.series().getName(), secret2.content().version().get()).get();
  }

  @Test
  public void createSecret() {
    int secretsBefore = tableSize(SECRETS);
    int secretContentsBefore = tableSize(SECRETS_CONTENT);

    String name = "newSecret";
    String content = "c2VjcmV0MQ==";
    String encryptedContent = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    String version = VersionGenerator.now().toHex();
    long newId = secretDAO.createSecret(name, encryptedContent, version, "creator",
        ImmutableMap.of(), "", null, ImmutableMap.of());
    SecretSeriesAndContent newSecret = secretDAO.getSecretByIdAndVersion(newId, version).get();

    assertThat(tableSize(SECRETS)).isEqualTo(secretsBefore + 1);
    assertThat(tableSize(SECRETS_CONTENT)).isEqualTo(secretContentsBefore + 1);

    newSecret = secretDAO.getSecretByNameAndVersion(
        newSecret.series().getName(), newSecret.content().version().orElse("")).get();
    assertThat(secretDAO.getSecrets()).containsOnly(secret1, secret2, newSecret);
  }

  @Test
  public void createTwoVersionsOfASecret() {
    int secretsBefore = tableSize(SECRETS);
    int secretContentsBefore = tableSize(SECRETS_CONTENT);

    String name = "newSecret";
    String content = "c2VjcmV0MQ==";
    String encryptedContent1 = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    String version = VersionGenerator.fromLong(1234).toHex();
    long id = secretDAO.createSecret(name, encryptedContent1, version, "creator", ImmutableMap.of(),
        "", null, ImmutableMap.of());
    SecretSeriesAndContent newSecret1 = secretDAO.getSecretByIdAndVersion(id, version).get();

    content = "amFja2RvcnNrZXkK";
    String encryptedContent2 = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    version = VersionGenerator.fromLong(4321).toHex();
    id = secretDAO.createSecret(name, encryptedContent2, version, "creator", ImmutableMap.of(), "",
        null, ImmutableMap.of());
    SecretSeriesAndContent newSecret2 = secretDAO.getSecretByIdAndVersion(id, version).get();

    // Only one new secrets entry should be created - there should be 2 secrets_content entries though
    assertThat(tableSize(SECRETS)).isEqualTo(secretsBefore + 1);

    assertThat(newSecret1.content().encryptedContent()).isEqualTo(encryptedContent1);
    assertThat(newSecret2.content().encryptedContent()).isEqualTo(encryptedContent2);
    assertThat(secretDAO.getSecrets()).containsOnly(secret1, secret2, newSecret1, newSecret2);

    assertThat(tableSize(SECRETS_CONTENT)).isEqualTo(secretContentsBefore + 2);
  }

  @Test
  public void getSecretByNameAndVersion() {
    SecretSeriesAndContent seriesAndContent = secretDAO.getSecretByNameAndVersion(
        secret1.series().getName(), secret1.content().version().orElse(""))
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(secret1);
  }

  @Test
  public void getSecretByNameAndVersionWithoutVersion() {
    SecretSeriesAndContent seriesAndContent =
        secretDAO.getSecretByNameAndVersion(secret2.series().getName(), "")
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(secret2);
  }

  @Test
  public void getSecretByNameAndVersionWithVersion() {
    String futureStamp = new VersionGenerator(System.currentTimeMillis() + 100000).toHex();
    String name = secret1.series().getName();
    String content = "bmV3ZXJTZWNyZXQy";
    String encryptedContent = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    long newId = secretDAO.createSecret(name, encryptedContent, futureStamp, "creator", ImmutableMap.of(), "desc", null, null);
    SecretSeriesAndContent newerSecret = secretDAO.getSecretByIdAndVersion(newId, futureStamp)
        .orElseThrow(RuntimeException::new);

    SecretSeriesAndContent seriesAndContent = secretDAO.getSecretByNameAndVersion(
        secret1.series().getName(), secret1.content().version().orElse(""))
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(secret1);

    seriesAndContent = secretDAO.getSecretByNameAndVersion(secret1.series().getName(), futureStamp)
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(newerSecret);
  }

  @Test
  public void getSecretByIdAndVersionWithoutVersion() {
    SecretSeriesAndContent seriesAndContent =
        secretDAO.getSecretByIdAndVersion(secret2.series().getId(), "")
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(secret2);
  }

  @Test
  public void getSecretByIdAndVersionWithVersion() {
    String futureStamp = new VersionGenerator(System.currentTimeMillis() + 222222).toHex();
    String name = secret1.series().getName();
    String content = "bmV3ZXJTZWNyZXQy";
    String encryptedContent = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    secretDAO.createSecret(name, encryptedContent, futureStamp, "creator", ImmutableMap.of(), "desc", null, null);
    SecretSeriesAndContent newerSecret = secretDAO.getSecretByNameAndVersion(name, futureStamp)
        .orElseThrow(RuntimeException::new);

    SecretSeriesAndContent seriesAndContent = secretDAO.getSecretByIdAndVersion(
        secret1.series().getId(), secret1.content().version().orElse(""))
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(secret1);

    seriesAndContent = secretDAO.getSecretByIdAndVersion(
        secret1.series().getId(), newerSecret.content().version().orElse(""))
        .orElseThrow(RuntimeException::new);
    assertThat(seriesAndContent).isEqualTo(newerSecret);
  }

  @Test
  public void getSecretById() {
    SecretSeriesAndContent expected = secretDAO.getSecretByNameAndVersion(
        secret1.series().getName(), secret1.content().version().orElse(""))
        .orElseThrow(RuntimeException::new);
    List<SecretSeriesAndContent> actual = secretDAO.getSecretsById(expected.series().getId());
    assertThat(actual).containsExactly(expected);
  }

  @Test
  public void getNonExistentSecret() {
    assertThat(secretDAO.getSecretByNameAndVersion("non-existent", "").isPresent()).isFalse();
    assertThat(secretDAO.getSecretsById(-1231)).isEmpty();
  }

  @Test
  public void getSecrets() {
    assertThat(secretDAO.getSecrets()).containsOnly(secret1, secret2);
  }

  @Test
  public void deleteSecretsByName() {
    secretDAO.createSecret("toBeDeleted_deleteSecretsByName", "encryptedShhh", "first", "creator",
        ImmutableMap.of(), "", null, null);

    int secretsBefore = tableSize(SECRETS);
    int secretContentsBefore = tableSize(SECRETS_CONTENT);

    secretDAO.deleteSecretsByName("toBeDeleted_deleteSecretsByName");

    assertThat(tableSize(SECRETS)).isEqualTo(secretsBefore - 1);
    assertThat(tableSize(SECRETS_CONTENT)).isEqualTo(secretContentsBefore - 1);
    Optional<SecretSeriesAndContent> missingSecret =
        secretDAO.getSecretByNameAndVersion("toBeDeleted_deleteSecretsByName", "first");
    assertThat(missingSecret.isPresent()).isFalse();
  }

  @Test
  public void deleteSecretByNameAndVersion() {
    secretDAO.createSecret("shadow_deleteSecretByNameAndVersion", "encrypted1", "no1", "creator",
        ImmutableMap.of(), "desc", null, null);
    secretDAO.createSecret("shadow_deleteSecretByNameAndVersion", "encrypted2", "no2", "creator",
        ImmutableMap.of(), "desc", null, null);
    int secretsBefore = tableSize(SECRETS);
    int secretContentsBefore = tableSize(SECRETS_CONTENT);

    secretDAO.deleteSecretByNameAndVersion("shadow_deleteSecretByNameAndVersion", "no1");

    assertThat(tableSize(SECRETS)).isEqualTo(secretsBefore);
    assertThat(tableSize(SECRETS_CONTENT)).isEqualTo(secretContentsBefore - 1);
    Optional<SecretSeriesAndContent> missingSecret =
        secretDAO.getSecretByNameAndVersion("shadow_deleteSecretByNameAndVersion", "no1");
    assertThat(missingSecret.isPresent()).isFalse();
  }

  @Test
  public void deleteSecretSeriesWhenEmpty() {
    secretDAO.createSecret("toBeDeleted_deleteSecretSeriesWhenEmpty", "encryptedOvaltine", "v22",
        "creator", ImmutableMap.of(), "desc", null, null);

    int secretsBefore = tableSize(SECRETS);
    int secretContentsBefore = tableSize(SECRETS_CONTENT);

    secretDAO.deleteSecretByNameAndVersion("toBeDeleted_deleteSecretSeriesWhenEmpty", "v22");

    assertThat(tableSize(SECRETS)).isEqualTo(secretsBefore - 1);
    assertThat(tableSize(SECRETS_CONTENT)).isEqualTo(secretContentsBefore - 1);
    Optional<SecretSeriesAndContent> missingSecret =
        secretDAO.getSecretByNameAndVersion("toBeDeleted_deleteSecretSeriesWhenEmpty", "v22");
    assertThat(missingSecret.isPresent()).isFalse();
  }

  @Test(expected = StatementException.class)
  public void willNotCreateDuplicateVersionedSecret() throws Exception {
    ImmutableMap<String, String> emptyMap = ImmutableMap.of();
    secretDAO.createSecret("secret1", "encrypted1", version, "creator", emptyMap, "", null, emptyMap);
    secretDAO.createSecret("secret1", "encrypted1", version, "creator", emptyMap, "", null, emptyMap);
  }

  @Test(expected = StatementException.class)
  public void willNotCreateDuplicateSecret() throws Exception {
    ImmutableMap<String, String> emptyMap = ImmutableMap.of();
    secretDAO.createSecret("secret1", "encrypted1", "", "creator", emptyMap, "", null, emptyMap);
    secretDAO.createSecret("secret1", "encrypted1", "", "creator", emptyMap, "", null, emptyMap);
  }

  private int tableSize(Table table) {
    return testDBRule.jooqContext().fetchCount(table);
  }
}
