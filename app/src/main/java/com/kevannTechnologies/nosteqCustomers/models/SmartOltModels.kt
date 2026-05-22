package com.kevannTechnologies.nosteqCustomers.models

data class AdministrativeStatusResponse(
    val status: Boolean,
    val administrative_status: String? = null
)

data class OnuStatusResponse(
    val status: Boolean,
    val onus_statuses: List<OnuStatus> = emptyList()
)

data class OnuStatus(
    val unique_external_id: String,
    val administrative_status: String,
    val onu_status: String? = null,
    val signal: String? = null
)

data class SingleOnuStatusResponse(
    val status: Boolean,
    val onu_status: OnuStatus? = null
)

data class RebootResponse(
    val status: Boolean,
    val message: String? = null
)

data class OltListResponse(
    val status: Boolean,
    val olts: List<OltInfo> = emptyList()
)

data class OltInfo(
    val id: String,
    val name: String,
    val type: String? = null
)

data class SpeedProfilesResponse(
    val status: Boolean,
    val speed_profiles: List<SpeedProfile> = emptyList()
)

data class SpeedProfile(
    val id: String,
    val name: String,
    val upload_speed: String? = null,
    val download_speed: String? = null
)
