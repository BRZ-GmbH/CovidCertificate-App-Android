package at.gv.brz.sdk.chain

import ehn.techiop.hcert.kotlin.chain.CwtService
import ehn.techiop.hcert.kotlin.chain.Error
import ehn.techiop.hcert.kotlin.chain.VerificationException
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import ehn.techiop.hcert.kotlin.crypto.CwtHeaderKeys
import ehn.techiop.hcert.kotlin.data.CborObject
import ehn.techiop.hcert.kotlin.trust.CwtAdapter
import ehn.techiop.hcert.kotlin.trust.CwtHelper
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Custom CWT service that ignores the CWT expiry because this is checked via business rules
 */
class LocalCwtService constructor(
    private val clock: Clock = Clock.System,
) : CwtService {

    override fun encode(input: ByteArray): ByteArray {
        return ByteArray(0)
    }

    override fun decode(input: ByteArray, verificationResult: VerificationResult): CborObject {
        try {
            val now = clock.now()
            val map = CwtHelper.fromCbor(input)
            // issuer is truly optional
            map.getString(CwtHeaderKeys.ISSUER.intVal)?.let {
                verificationResult.issuer = it
            }

            val issuedAtSeconds = map.getNumber(CwtHeaderKeys.ISSUED_AT.intVal)
                ?: throw VerificationException(Error.CWT_EXPIRED, details = mapOf("issuedAt" to "null"))
            val issuedAt = Instant.fromEpochSeconds(issuedAtSeconds.toLong())
            verificationResult.issuedAt = issuedAt

            if (issuedAt > now)
                throw VerificationException(
                    Error.CWT_NOT_YET_VALID, details = mapOf(
                        "issuedAt" to issuedAt.toString(),
                        "currentTime" to now.toString()
                    )
                )
            val expirationSeconds = map.getNumber(CwtHeaderKeys.EXPIRATION.intVal)
                ?: throw VerificationException(Error.CWT_EXPIRED, details = mapOf("expirationTime" to "null"))
            val expirationTime = Instant.fromEpochSeconds(expirationSeconds.toLong())
            verificationResult.expirationTime = expirationTime

            val hcert: CwtAdapter = map.getMap(CwtHeaderKeys.HCERT.intVal)
                ?: throw VerificationException(Error.CBOR_DESERIALIZATION_FAILED, "CWT contains no HCERT")

            val dgc = hcert.getMap(CwtHeaderKeys.EUDGC_IN_HCERT.intVal)
                ?: throw VerificationException(Error.CBOR_DESERIALIZATION_FAILED, "CWT contains no EUDGC")

            return dgc.toCborObject()
        } catch (e: VerificationException) {
            throw e
        } catch (e: Throwable) {
            throw VerificationException(Error.CBOR_DESERIALIZATION_FAILED, e.message, e)
        }
    }

}
