package no.nav.security.mock

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import mu.KotlinLogging
import no.nav.security.mock.callback.DefaultTokenCallback
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.extensions.toJwksUrl
import no.nav.security.mock.extensions.toTokenEndpointUrl
import no.nav.security.mock.extensions.toWellKnownUrl
import no.nav.security.mock.oauth2.OAuth2Dispatcher
import no.nav.security.mock.oauth2.OAuth2TokenProvider
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI

private val log = KotlinLogging.logger {}

class MockOAuth2Server(
    tokenCallbacks: Set<TokenCallback> = setOf(DefaultTokenCallback())
) {
    private val mockWebServer: MockWebServer = MockWebServer()
    private val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider()

    var dispatcher: Dispatcher = OAuth2Dispatcher(tokenProvider, tokenCallbacks)

    fun start() {
        mockWebServer.start()
        mockWebServer.dispatcher = dispatcher
    }

    fun start(port: Int = 0) {
        val address = InetSocketAddress(0).address
        log.info("attempting to start server on port $port and InetAddress=$address")
        mockWebServer.start(address, port)
        mockWebServer.dispatcher = dispatcher
    }

    @Throws(IOException::class)
    fun shutdown() {
        mockWebServer.shutdown()
    }

    fun enqueueCallback(tokenCallback: TokenCallback) =
        (dispatcher as OAuth2Dispatcher).enqueueJwtCallback(tokenCallback)

    fun takeRequest(): RecordedRequest = mockWebServer.takeRequest()

    fun wellKnownUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toWellKnownUrl()
    fun tokenEndpointUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toTokenEndpointUrl()
    fun jwksUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toJwksUrl()
    fun issuerUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId)
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toAuthorizationEndpointUrl()
    fun baseUrl(): HttpUrl = mockWebServer.url("")

    fun issueToken(issuerId: String, clientId: String, tokenCallback: TokenCallback): SignedJWT {
        val uri = tokenEndpointUrl(issuerId)
        val issuerUrl = issuerUrl(issuerId)
        val tokenRequest = TokenRequest(
            uri.toUri(),
            ClientSecretBasic(ClientID(clientId), Secret("secret")),
            AuthorizationCodeGrant(AuthorizationCode("123"), URI.create("http://localhost"))
        )
        return tokenProvider.accessToken(tokenRequest, issuerUrl, null, tokenCallback)
    }
}