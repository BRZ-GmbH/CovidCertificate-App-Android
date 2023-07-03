package at.gv.brz.common.net

import at.gv.brz.common.config.DirectLinkModel
import at.gv.brz.common.config.DirectLinkResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DirectLinkService {

    @Headers("Content-Type: application/json")
    @POST("result/wallet/v1/directLink")
    suspend fun fetchCertificateWithBirthdateOrBypassToken(@Body directLinkModel: DirectLinkModel): Response<DirectLinkResponse>

}