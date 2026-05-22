package com.kevannTechnologies.nosteqCustomers.models

data class OnuDetailsResponse(
    val status: Boolean,
    val onu_details: OnuDetails
)

data class OnuDetails(
    val unique_external_id: String,
    val name: String,
    val sn: String,
    val administrative_status: String,
    val ethernet_ports: List<EthernetPort> = emptyList(),
    val wifi_ports: List<WiFiPort> = emptyList()
)

data class EthernetPort(
    val port: String,
    val admin_state: String,
    val mode: String? = null,
    val dhcp: String? = null
)

data class WiFiPort(
    val port: String,
    val admin_state: String,
    val ssid: String? = null,
    val mode: String? = null
)

data class AllOnusDetailsResponse(
    val status: Boolean,
    val onus_details: List<OnuDetails> = emptyList()
)
