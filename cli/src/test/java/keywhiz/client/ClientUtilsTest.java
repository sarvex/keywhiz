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

package keywhiz.client;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.squareup.okhttp.OkHttpClient;
import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import keywhiz.cli.ClientUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientUtilsTest {
  
  private HttpCookie xsrfCookie = new HttpCookie("XSRF-TOKEN", "xsrf-contents");
  private HttpCookie sessionCookie = new HttpCookie("session", "session-contents");
  {
    xsrfCookie.setPath("/");
    xsrfCookie.setDomain("localhost");
    xsrfCookie.setVersion(1);
    xsrfCookie.setHttpOnly(false);
    xsrfCookie.setSecure(true);

    sessionCookie.setPath("/admin");
    sessionCookie.setDomain("localhost");
    sessionCookie.setVersion(0);
    sessionCookie.setHttpOnly(true);
    sessionCookie.setSecure(true);
  }

  private ImmutableList<HttpCookie> cookieList = ImmutableList.of(sessionCookie, xsrfCookie);


  @Mock CookieManager cookieManager;
  @Mock CookieStore cookieStore;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final String COOKIE_EXTENSION = "/.keywhiz.cookies";
  private Path COOKIE_PATH;

  @Before
  public void setup() throws IOException {
    tempFolder.create();
    COOKIE_PATH = Paths.get(tempFolder.getRoot().getPath(),
        COOKIE_EXTENSION);
  }

  @Test
  public void testSslOkHttpClientCreation() throws Exception {
    OkHttpClient sslClient = ClientUtils.sslOkHttpClient(ImmutableList.of());

    assertThat(sslClient.getFollowSslRedirects()).isFalse();
    assertThat(sslClient.getSslSocketFactory()).isNotNull();
    assertThat(sslClient.networkInterceptors()).isNotEmpty();

    assertThat(sslClient.getCookieHandler()).isNotNull();
    java.util.List<HttpCookie>
        cookieList = ((CookieManager) sslClient.getCookieHandler()).getCookieStore().getCookies();
    assertThat(cookieList).isEmpty();
  }

  @Test
  public void testSslOkHttpClientCreationWithCookies() throws Exception {
    OkHttpClient sslClient = ClientUtils.sslOkHttpClient(cookieList);

    assertThat(sslClient.getFollowSslRedirects()).isFalse();
    assertThat(sslClient.getCookieHandler()).isNotNull();
    assertThat(sslClient.getSslSocketFactory()).isNotNull();
    assertThat(sslClient.networkInterceptors()).isNotEmpty();

    java.util.List<HttpCookie>
        cookieList = ((CookieManager) sslClient.getCookieHandler()).getCookieStore().getCookies();
    assertThat(cookieList).contains(xsrfCookie);
    assertThat(cookieList).contains(sessionCookie);
  }

  @Test
  public void testSaveCookies() throws Exception {
    when(cookieManager.getCookieStore()).thenReturn(cookieStore);
    when(cookieStore.getCookies()).thenReturn(ImmutableList.of(xsrfCookie, sessionCookie));

    ClientUtils.saveCookies(cookieManager, COOKIE_PATH);

    File cookieFile = COOKIE_PATH.toFile();

    assertThat(cookieFile.exists()).isTrue();
    assertThat(Files.getPosixFilePermissions(cookieFile.toPath())).containsOnly(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
  }

  @Test(expected = NoSuchFileException.class)
  public void testLoadCookiesFailWithoutFile() throws Exception {
    ClientUtils.loadCookies(COOKIE_PATH);
  }


  @Test
  public void testLoadCookiesWithFile() throws Exception {
    Path savedCookies = Paths.get(Resources.getResource("cookies.json").getPath());
    List<HttpCookie> loadedCookies = ClientUtils.loadCookies(savedCookies);

    assertThat(loadedCookies).hasSameElementsAs(cookieList);
  }

  @Test
  public void testSaveAndLoadCookies() throws Exception {
    when(cookieManager.getCookieStore()).thenReturn(cookieStore);
    when(cookieStore.getCookies()).thenReturn(ImmutableList.of(xsrfCookie, sessionCookie));
    ClientUtils.saveCookies(cookieManager, COOKIE_PATH);

    assertThat(ClientUtils.loadCookies(COOKIE_PATH)).hasSameElementsAs(cookieList);
  }
}