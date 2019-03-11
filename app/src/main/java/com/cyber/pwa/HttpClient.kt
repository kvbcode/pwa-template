package com.cyber.pwa

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Copyright (c) 2019 Kirill Bereznyakov
 */

class HttpClient{

    companion object {

        val instance:OkHttpClient = setupOkHttp()

        /**
         * OkHttp hacks for android 4.1 - 4.4 for TLSv1.2 works
         */

        private fun setupOkHttp(): OkHttpClient {
            val sslContext = SSLContext.getInstance("TLSv1.2")

            // trust everyone
            var trustManagers: Array<X509TrustManager> = arrayOf(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
            )

            sslContext.init(null, trustManagers, null)

            // patch each socket for TLSv1.2 support
            val sslSocketFactory = Tls12SocketFactory(sslContext.socketFactory)

            val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()

            return OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustManagers[0])
                    .connectionSpecs(Collections.singletonList(spec))
                    .build()
        }

        /**
         * Enables TLS v1.2 when creating SSLSockets.
         *
         * For some reason, android supports TLS v1.2 from API 16, but enables it by
         * default only from API 20.
         * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
         * @see SSLSocketFactory
         */
        internal class Tls12SocketFactory(val base: SSLSocketFactory) : SSLSocketFactory() {

            override fun getDefaultCipherSuites(): Array<String> {
                return base.defaultCipherSuites
            }

            override fun getSupportedCipherSuites(): Array<String> {
                return base.supportedCipherSuites
            }

            @Throws(IOException::class)
            override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
                return patch(base.createSocket(s, host, port, autoClose))
            }

            @Throws(IOException::class, UnknownHostException::class)
            override fun createSocket(host: String, port: Int): Socket? {
                return patch(base.createSocket(host, port))
            }

            @Throws(IOException::class, UnknownHostException::class)
            override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
                return patch(base.createSocket(host, port, localHost, localPort))
            }

            @Throws(IOException::class)
            override fun createSocket(host: InetAddress, port: Int): Socket? {
                return patch(base.createSocket(host, port))
            }

            @Throws(IOException::class)
            override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
                return patch(base.createSocket(address, port, localAddress, localPort))
            }

            private fun patch(s: Socket): Socket {
                if (s is SSLSocket) {
                    s.enabledProtocols = TLS_V12_ONLY
                }
                return s
            }

            companion object {
                private val TLS_V12_ONLY = arrayOf("TLSv1.2")
            }
        }

    }

}