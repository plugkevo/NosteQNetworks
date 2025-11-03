package com.example.nosteq.models



import com.google.gson.annotations.SerializedName

// Reboot Response
data class RebootResponse(
    val status: Boolean,
    val response: String
)

// ONU Status Response
data class OnuStatusResponse(
    val status: Boolean,
    val response: List<OnuStatus>
)

data class OnuStatus(
    val id: Int,
    val name: String,
    val status: String,
    val signal: String?,
    val distance: String?,
    val temperature: String?,
    @SerializedName("rxPower") val rxPower: String?,
    @SerializedName("txPower") val txPower: String?,
    val voltage: String?
)

// Speed Profiles Response
data class SpeedProfilesResponse(
    val status: Boolean,
    val response: List<SpeedProfile>
)

data class SpeedProfile(
    val id: Int,
    val name: String,
    val speed: String,
    val direction: String,
    val type: String
)

// OLT List Response
data class OltListResponse(
    val status: Boolean,
    val response: List<Olt>
)

data class Olt(
    val id: String,
    val name: String,
    @SerializedName("olt_hardware_version") val oltHardwareVersion: String,
    val ip: String,
    @SerializedName("telnet_port") val telnetPort: String,
    @SerializedName("snmp_port") val snmpPort: String
)

// ONU Details Response for get_onu_details endpoint
data class OnuDetailsResponse(
    val status: Boolean,
    @SerializedName("onu_details") val onuDetails: OnuDetails
)

data class OnuDetails(
    @SerializedName("unique_external_id") val uniqueExternalId: String,
    val sn: String,
    val name: String,
    @SerializedName("olt_id") val oltId: String,
    @SerializedName("olt_name") val oltName: String,
    val board: String,
    val port: String,
    val onu: String,
    @SerializedName("onu_type_id") val onuTypeId: String,
    @SerializedName("onu_type_name") val onuTypeName: String,
    @SerializedName("zone_id") val zoneId: String,
    @SerializedName("zone_name") val zoneName: String,
    val address: String?,
    @SerializedName("odb_name") val odbName: String?,
    val mode: String?,
    @SerializedName("wan_mode") val wanMode: String?,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("subnet_mask") val subnetMask: String?,
    @SerializedName("default_gateway") val defaultGateway: String?,
    val dns1: String?,
    val dns2: String?,
    val username: String?,
    val password: String?,
    val catv: String?,
    @SerializedName("administrative_status") val administrativeStatus: String?,
    @SerializedName("service_ports") val servicePorts: List<ServicePort>?
)

data class ServicePort(
    @SerializedName("service_port") val servicePort: String,
    val vlan: String,
    val cvlan: String?,
    val svlan: String?,
    @SerializedName("tag_transform_mode") val tagTransformMode: String?,
    @SerializedName("upload_speed") val uploadSpeed: String,
    @SerializedName("download_speed") val downloadSpeed: String
)
// All Onus Details Response for get_all_onus_details endpoint
data class AllOnusDetailsResponse(
    val status: Boolean,
    @SerializedName("onus") val onus: List<OnuDetailsItem>
)

data class OnuDetailsItem(
    @SerializedName("unique_external_id") val uniqueExternalId: String,
    val sn: String,
    val name: String,
    @SerializedName("olt_id") val oltId: String,
    @SerializedName("olt_name") val oltName: String,
    val board: String,
    val port: String,
    val onu: String,
    @SerializedName("onu_type_name") val onuTypeName: String?,
    @SerializedName("zone_name") val zoneName: String?,
    val address: String?,
    @SerializedName("odb_name") val odbName: String?,
    val username: String?
)
