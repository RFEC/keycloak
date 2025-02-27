/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.quarkus.runtime.services.resources.DebugHostnameSettingsResource;

import static io.restassured.RestAssured.when;

@DistributionTest(keepAlive = true, enableTls = true, defaultOptions = { "--http-enabled=true" })
@RawDistOnly(reason = "Containers are immutable")
public class HostnameDistTest {

    @BeforeAll
    public static void onBeforeAll() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-strict-https=false" })
    public void testSchemeAndPortFromRequestWhenNoProxySet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "http://mykeycloak.org:8080/");
        assertFrontEndUrl("http://localhost:8080", "http://mykeycloak.org:8080/");
        assertFrontEndUrl("https://localhost:8443", "https://mykeycloak.org:8443/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org" })
    public void testForceHttpsSchemeAndPortWhenStrictHttpsEnabled() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "https://mykeycloak.org:8443/");
        assertFrontEndUrl("http://localhost:8080", "https://mykeycloak.org:8443/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-port=1234" })
    public void testForceHostnamePortWhenNoProxyIsSet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "https://mykeycloak.org:1234/");
        assertFrontEndUrl("https://mykeycloak.org:8443", "https://mykeycloak.org:1234/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--proxy=edge" })
    public void testUseDefaultPortsWhenProxyIsSet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "https://mykeycloak.org/");
        assertFrontEndUrl("https://mykeycloak.org:8443", "https://mykeycloak.org/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--proxy=edge", "--hostname-strict-https=false" })
    public void testUseDefaultPortsWhenProxyIsSetNoStrictHttps() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "http://mykeycloak.org/");
        assertFrontEndUrl("https://mykeycloak.org:8443", "https://mykeycloak.org/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--proxy=edge", "--hostname-strict-https=true" })
    public void testUseDefaultPortsAndHttpsSchemeWhenProxyIsSetAndStrictHttpsEnabled() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "https://mykeycloak.org/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org" })
    public void testBackEndUrlFromRequest() {
        assertBackEndUrl("http://localhost:8080", "http://localhost:8080/");
        assertBackEndUrl("https://localhost:8443", "https://localhost:8443/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-strict-backchannel=true" })
    public void testBackEndUrlSameAsFrontEndUrl() {
        assertBackEndUrl("http://localhost:8080", "https://mykeycloak.org:8443/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-path=/auth", "--hostname-strict-backchannel=true" })
    public void testSetHostnamePath() {
        assertFrontEndUrl("http://localhost:8080", "https://mykeycloak.org:8443/auth/");
        assertBackEndUrl("http://localhost:8080", "https://mykeycloak.org:8443/auth/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--https-port=8543", "--hostname-strict-https=true" })
    public void testDefaultTlsPortChangeWhenHttpPortSet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "https://mykeycloak.org:8543/");
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-port=8543" })
    public void testWelcomePageAdminUrl() {
        when().get("http://mykeycloak.org:8080").then().body(Matchers.containsString("http://mykeycloak.org:8080/admin/"));
        when().get("https://mykeycloak.org:8443").then().body(Matchers.containsString("https://mykeycloak.org:8443/admin/"));
        when().get("http://localhost:8080").then().body(Matchers.containsString("http://localhost:8080/admin/"));
        when().get("https://localhost:8443").then().body(Matchers.containsString("https://localhost:8443/admin/"));
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-debug=true" })
    public void testDebugHostnameSettingsEnabled() {
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().statusCode(200);
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().body(Matchers.containsString("Configuration property"));
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().body(Matchers.containsString("Server mode"));
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().body(Matchers.containsString("production [start]"));

        when().get("http://mykeycloak.org:8080/realms/master/" +
                                    DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX  +
                                    "/" + DebugHostnameSettingsResource.PATH_FOR_TEST_CORS_IN_HEADERS
        ).then().statusCode(200);
        when().get("http://localhost:8080/realms/master/" +
                   DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX +
                   "/" + DebugHostnameSettingsResource.PATH_FOR_TEST_CORS_IN_HEADERS)
              .then()
              .body(Matchers.containsString(DebugHostnameSettingsResource.PATH_FOR_TEST_CORS_IN_HEADERS + "-OK"));
        when().get("http://localhost:8080/realms/non-existent/" + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().statusCode(404);
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-debug=false" })
    public void testDebugHostnameSettingsDisabledBySetting() {
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().statusCode(404);
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org"})
    public void testDebugHostnameSettingsDisabledByDefault() {
        when().get("http://localhost:8080/realms/master/"  + DebugHostnameSettingsResource.DEFAULT_PATH_SUFFIX).then().statusCode(404);
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org", "--hostname-admin=mykeycloakadmin.org" })
    public void testHostnameAdminSet() {
        when().get("https://mykeycloak.org:8443/admin/master/console").then().body(Matchers.containsString("\"authUrl\": \"https://mykeycloakadmin.org:8443\""));
        when().get("https://mykeycloak.org:8443/realms/master/protocol/openid-connect/auth?client_id=security-admin-console&redirect_uri=https://mykeycloakadmin.org:8443/admin/master/console&state=02234324-d91e-4bf2-8396-57498e96b12a&response_mode=fragment&response_type=code&scope=openid&nonce=f8f3812e-e349-4bbf-8d15-cbba4927f5e5&code_challenge=7qjD_v11WGkt1ig-ZFHxJdrEvuTlzjFRgRGQ_5ADcko&code_challenge_method=S256").then().body(Matchers.containsString("Sign in to your account"));
    }

    @Test
    @Launch({ "start", "--hostname=mykeycloak.org" })
    public void testInvalidRedirectUriWhenAdminNotSet() {
        when().get("https://mykeycloak.org:8443/realms/master/protocol/openid-connect/auth?client_id=security-admin-console&redirect_uri=https://mykeycloakadmin.127.0.0.1.nip.io:8443/admin/master/console&state=02234324-d91e-4bf2-8396-57498e96b12a&response_mode=fragment&response_type=code&scope=openid&nonce=f8f3812e-e349-4bbf-8d15-cbba4927f5e5&code_challenge=7qjD_v11WGkt1ig-ZFHxJdrEvuTlzjFRgRGQ_5ADcko&code_challenge_method=S256").then().body(Matchers.containsString("Invalid parameter: redirect_uri"));
    }

    @Test
    @Launch({ "start", "--proxy=edge", "--hostname-url=http://mykeycloak.org:1234" })
    public void testFrontendUrl() {
        assertFrontEndUrl("https://mykeycloak.org:8443", "http://mykeycloak.org:1234/");
    }

    @Test
    @Launch({ "start", "--proxy=edge", "--hostname=mykeycloak.org", "--hostname-admin-url=http://mykeycloakadmin.org:1234" })
    public void testAdminUrl() {
        when().get("https://mykeycloak.org:8443").then().body(Matchers.containsString("http://mykeycloakadmin.org:1234/admin/"));
    }

    @Test
    @Launch({ "start", "--hostname-strict=false" })
    public void testStrictHttpsDisabledIfHostnameDisabled() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "http://mykeycloak.org:8080/");
    }

    private OIDCConfigurationRepresentation getServerMetadata(String baseUrl) {
        return when().get(baseUrl + "/realms/master/.well-known/openid-configuration").as(OIDCConfigurationRepresentation.class);
    }

    private void assertFrontEndUrl(String requestBaseUrl, String expectedBaseUrl) {
        Assert.assertEquals(expectedBaseUrl + "realms/master/protocol/openid-connect/auth", getServerMetadata(requestBaseUrl)
                .getAuthorizationEndpoint());
    }

    private void assertBackEndUrl(String requestBaseUrl, String expectedBaseUrl) {
        Assert.assertEquals(expectedBaseUrl + "realms/master/protocol/openid-connect/token", getServerMetadata(requestBaseUrl)
                .getTokenEndpoint());
    }
}