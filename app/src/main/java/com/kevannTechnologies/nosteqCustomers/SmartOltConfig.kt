package com.kevannTechnologies.nosteqCustomers


/**
 * SmartOLT API Configuration
 *
 * TODO: Replace these values with your actual SmartOLT credentials:
 * 1. SUBDOMAIN: Your SmartOLT subdomain (e.g., "mycompany" for mycompany.smartolt.com)
 * 2. API_KEY: Your SmartOLT API key from Settings > General > API KEY

 */
object SmartOltConfig {
    // TODO: Add your SmartOLT subdomain here
    const val SUBDOMAIN = "nosteq"

    // TODO: Add your SmartOLT API key here
    const val API_KEY = "c9767e1d55694735a99b793c8e973b6d"



    // Optional: Add OLT details for filtering ONU status
    val OLT_ID: Int? = null  // e.g., 1
     val BOARD: Int? = null   // e.g., 2
     val PORT: Int? = null    // e.g., 3
     val ZONE: String? = null // e.g., "City Centre"

    fun getBaseUrl(): String = "https://$SUBDOMAIN.smartolt.com/api/"
}
