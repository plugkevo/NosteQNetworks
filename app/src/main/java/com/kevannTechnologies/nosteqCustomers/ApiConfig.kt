package com.kevannTechnologies.nosteqCustomers

object ApiConfig {
    // Base URL for PHP Radius API
    // Change this to match your PHP Radius domain
    private const val BASE_DOMAIN = "nosteq.phpradius.com"

    fun getBaseUrl(): String {
        return "https://$BASE_DOMAIN/index.php/"
    }
}
