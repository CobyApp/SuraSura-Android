package com.coby.surasura.data.client

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor

/**
 * Adds [x-goog-api-key] for Google Cloud gRPC APIs.
 *
 * Do **not** send [x-goog-user-project] here when using only an API key: that header makes
 * Google check IAM [serviceusage.services.use] on the project, which API-key callers often
 * do not satisfy → PERMISSION_DENIED (see Service Usage Consumer / serviceUsageConsumer).
 * Quota/billing still follow the project that owns the API key. Translation v3 uses
 * [GOOGLE_CLOUD_PROJECT_ID] in the RPC [parent] body, not this header.
 */
class GoogleCloudGrpcApiKeyInterceptor(
    private val apiKey: String
) : ClientInterceptor {

    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                if (apiKey.isNotBlank()) {
                    headers.put(API_KEY_HEADER, apiKey)
                }
                super.start(responseListener, headers)
            }
        }
    }

    companion object {
        private val API_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
