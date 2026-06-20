package com.endiar.purrodoro.soundscape

import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.net.InetAddress
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

/**
 * Shared HTTP client for cloud soundscape bytes (MP3 + thumbnails).
 *
 * Some networks (notably Indonesian ISP DNS filtering) hijack `*.r2.dev` to a
 * local block page with an invalid TLS certificate. Resolve the R2 public host
 * via DNS-over-HTTPS so OkHttp connects to Cloudflare directly.
 *
 * Long-term: attach a custom domain to the R2 bucket and point
 * [SoundscapeConfig.R2_PUBLIC_URL] at it.
 */
object SoundscapeHttp {

    fun createKtorClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = createOkHttpClient()
        }
    }

    fun createOkHttpClient(): OkHttpClient {
        val bootstrap = OkHttpClient.Builder().build()
        val doh = DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
            )
            .build()
        val r2Host = r2PublicHost()
        val dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                if (hostname == r2Host) {
                    doh.lookup(hostname)
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
        }
        return OkHttpClient.Builder()
            .dns(dns)
            .build()
    }

    private fun r2PublicHost(): String =
        Uri.parse(SoundscapeConfig.R2_PUBLIC_URL).host
            ?: error("SoundscapeConfig.R2_PUBLIC_URL must be an https URL with a host")
}
