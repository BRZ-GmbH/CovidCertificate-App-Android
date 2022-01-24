package at.gv.brz.common.qr

import at.gv.brz.eval.data.state.StateError

sealed class DecodeCertificateState {
    data class SUCCESS(val qrCode: String?) : DecodeCertificateState()
    object SCANNING : DecodeCertificateState()
    data class ERROR(val error: StateError) : DecodeCertificateState()
}