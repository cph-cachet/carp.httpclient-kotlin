package dk.cachet.carp.common.ddd

import dk.cachet.carp.common.serialization.customSerializerWrapper
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.utils.io.core.use
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json


/**
 * A client for a CARP HTTP application service endpoint.
 * Requests are posted to a single endpoint as polymorphic JSON objects.
 */
abstract class ApplicationServiceHttpClient<TServiceRequest : Any>(
    /**
     * [host] name without port (domain) and protocol.
     */
    protected val host: String,
    /**
     * The JSON serializer with all the necessary types for the subsystem this application service is part of registered.
     */
    private val json: Json,
    /**
     * The polymorphic serializer for [TServiceRequest].
     */
    protected val requestSerializer: KSerializer<TServiceRequest>,
    /**
     * [HttpClient] configuration builder used to override the default configuration.
     */
    private val configureBlock: HttpClientConfig<*>.() -> Unit
)
{
    fun createHttpClient( host: String ): HttpClient =
        HttpClient {
            defaultRequest {
                contentType( ContentType.Application.Json )
                url {
                    it.host = host
                    protocol = URLProtocol.HTTPS
                }
            }
            install( JsonFeature ) {
                serializer = KotlinxSerializer( json )
            }
        }.config( configureBlock )

    protected suspend inline fun <reified T> postRequest( request: TServiceRequest ): T
    {
        // The request object needs polymorphic serialization, as done by the base class serializer.
        val polymorphicRequest = customSerializerWrapper( request, requestSerializer )

        // Post request and return result.
        return createHttpClient( host ).use { it.post { body = polymorphicRequest } }
    }
}
