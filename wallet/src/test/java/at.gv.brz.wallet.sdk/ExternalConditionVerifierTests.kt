package at.gv.brz.wallet.sdk

import at.gv.brz.brvc.model.data.BusinessRuleCertificateType
import at.gv.brz.sdk.ExternalCondition
import at.gv.brz.sdk.ExternalConditionParameter
import at.gv.brz.sdk.ExternalConditionVerifier
import at.gv.brz.sdk.data.state.DecodeState
import at.gv.brz.sdk.decoder.CertificateDecoder
import at.gv.brz.sdk.readableName
import at.gv.brz.sdk.utils.TestType
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class ExternalConditionVerifierTests {

    companion object {
        val pcrTestCertificateApril25 =
            "HC1:NCF5W17C8OJ2P10DCBCNGVLG.JKA6HROKUPQIN65LRL1FUJOQPN0D0+NB/SJGT0FP7YNP8*MIP09:4NK5MQ2:75 SRPDKWTBSBLZC2:C8:4V48FZ-1SX72825DHP+3. T7YS7EU\$E85*OJX6Q0WGDBCGWATDD9VLLJ-Y1J6T2PCFN3%EU7RU*RH-8W %HXAQJD8CCV446Z3FQL5OKE5VL7LJ.JI2:GKNRDB6FM11G4YKSR+MVLOWXBT66023LQ9C-5Q QYEU3*8AXM8WTTIL24LT 70%D6PLBG7LFJ$-D:.4*JCNTNRWE%E4L%BBIH..TJ:H*KOD*IHDO/0M$*ODZG. ORCH+MAG:HXAU*/PVI84GDZLCD-GDOVZXAJ*KPBG30I FH.TLYM88FBO7PSE2/987KJ3D91\$GUH9*OOOPN-M2Y0FXZGE%O5UL0TF/SRPCRCRH-QR UK2OTI 1T9KQ+0N%CQL9GCH3JAAP0OO5WRDUOJX034B1E5R\$BO/Y6N+M*8SMBF.YE-XNFFK4/QK3S%HL3ZUQ+3I:N746.VV34C%REWKT6YSH0FY*MNHOQDSNCRX1QENQ1OQ9TL718:RF:+VBKF MN+8T7+I:EU:8TD1P0TM.GBU1S/ SZZT%/H25WA0AK:I"
        val ratTestCertificateApril25 =
            "HC1:NCF5W1W08$\$QWS3*61HNKR/RQ7JUHQO3LOFV2AU5G7ULNMUP5S6KSJE+1VQ1ZCS46F+ I3WN9*N2ONQH74U2JS0$8EGLU IAS4DB21RW0VJLPMAE+E5Y9*XR:EUFOVN/HU\$G8/IQMB/TPU\$VC6HKTG-DT/+IKFJ -B6SL6URW3F%8R2QF1AT67LO65BTA-DR 0J8WSFUTOE90MRF9200OZ+M1/KET6BX2E17D3JAPM+WC.YF0.UF4D-AJR265DO1%IBTU4ZV CJ \$N3.OW EXWABV7QZI8X0VSJ8P9+OO5W31MK/55\$R4E12 73/A6IT9X:RC%9XZ0N.E57T3\$PL0LM$5426H1GA%G\$XA/I6XI32H6P+0NI17B33CI:Z9S22HLNVONHEM6-JF3L2IO5-6-MA0E4EKM199UK6IMH46BNHUC9B8NA-47F67E-AHFUOF6WD1455G5OCLR5FP6FJ4ICK+MG:5YF1YQQBAMCV16 OE940JGP:8ZON6KRB.B5 NN+2Z:MZ6EUWP3CTT4WJCQ3WVO9WFOUK VT5WP4GB38OKR7ECG9UTMQRO6V5RIESY0O%1W1/N27WT/2LQ72COF\$T*/V77HVOSO37Y5SP+U9DI"
        val vaccination2of2April25 =
            "HC1:NCFMX1VX7YUO%20TDCF.O332/ZOGIAQZJG:N0Q46B532L0Y9E2L-YMQG7E%5EVQ4\$DUSQ0EL0NGV74O10OW5IFUST2IJO*98I-0ADEX.4G-02-08/496OUGUB5EEZ9SYL*PUF/3P6O:FN-G7RLCT 2L*SQ5D7K9218MV04SE:B9$7PLVQND3A15.XK+9ADIABX7+TJ-B0S-I:AA9R8I92W$56MUI1PANCT69OR7%PH-%7F/4Q-DY.80I96S4EJ3K0J%Q8A84EGUQC21/L0R1W*QL 6:MB.%A%IKH%D-.VC9F.3MN-1J99C65LY6S GE00.24XBIS.M%:U2O1042ZB0%E9BC19CSYD4MO0ND0XLITNL441H+OF1USBJ3\$GWGKGFH358BMJV\$BE\$RJHF-3MAIFQWPW-8AYAP0MA8VV T8%3K*R*HCG8IGZ4V*P*/F*.Q6FB+-O%EB7LA%67:D9RR2PIJIWT2P280L3LU $6U2NOTO%P66/26*QV8K 8TV+PUVEX\$AU93-NQ0.JP5JU.FXDRTGVB\$R-ZFY:VD:E68TV:OS2T%WN MOM1HN37-3JRLB2Q1T5WS%DS/BXIKZXTYEH"
        val secondVaccination2of2April25 =
            "HC1:NCFMX1BM7YUO%20JFC US.24B1O/MH9UIF5FRYK4NHNILJXQBZAIK3E03\$W4YPJ2UBQLJ X0%80:USJ34X25/.9YHQ-D80I7SMKZKHVIT5XMVQUE8H6DC6+EDR1+Q44TF-1TZAM4UHNZ8+SLEV8J*M2KBBDQN4R7TSZ2UYW9P82-N6IQBR\$OM3BJ4MDW8QOVFTC0GOSNI8G8M70 TSDGN7K58K07T0QTE+DR-H2KHR*Q0MIO%B68W1S9BFGHKA8S80+9B-TMM441HFL26PKA T3HVLWIC*ZIR%H 9IPZ12.I6DEBQSY6SE3L112DCO*M00TS%D1U:1536WCC454 ZIQ 4K774ZO6HGW80PBIQ851B4MD7IEA CD/A95SP3ZOS%OEAH%W18.8B\$MGND-3N-1VVF1E6R2WTD*5.WDX47S5LU\$O71UG\$D5EDLF9ZKTK7RVOJD7IPTM+*JT5QY6BPU3.%S8Y4%Z41R6%VL 5E*72+B042PSRN ASEOF7T72HLO+NCHQY/EH3WUZ51PU7KRCOQ64A1FVV/NDCS:SV*EV743A4S7DTC6JCFE/0E/6EKRL//V*9F0ZD8J08+HV0"
        val recoveryApril04 =
            "HC1:NCFOXN%TSMAHN-H3ZSUZK+.V0ET9%6-AH-XI1ROR\$SIOOU.IROUG*I%E57\$FSSLNO4*J8OX4UZ85XPWLI+J53O8J.V J8\$XJK*L5R1ZP3LYLGS9/ZJ/T1H\$JH\$JGS9-.P:%BJS54-REP11Y9C+H1Y9SU3G 9L/N:PIWEG%*4AZKZ734234LTW9JSCA+G9AXGT6DGTSFW2TAFCNNG.8G%8VD98-O+SQ4IJZJJ1W4*\$I*NVPC1LJL4A7N832F14+KJJIU%O0QIRR97I2HOAXL92L0G+SB.V Q5FN9ML1X/BB-S-*O5W41FD+.K588/HL*DD2IHJSN37HMX3.7KO7JDKBLZI19JA2K7VA\$IJVTI ZJY1B QTOD3CQSP\$SR\$S3NDC9UOD3Y.TJET9G3:ZJ83BDPSCFTB.SBVT6NJF0JEYI1DLCZKO63VEKCXDZNCHPE+ZSNPU3P7S16I:N.NNYJ6Q6QUDTZ0AP\$DBAMN7OCY2Q9M*CVT:9H UO+S:\$O+F69Y2U%P72QY%C9R4T6Q+%T8M2U\$I%00: 0%1"
    }

    @Test
    fun testConditionsWithTestCertificate() {
        val parsedTestState = CertificateDecoder.decode(pcrTestCertificateApril25)
        if (parsedTestState is DecodeState.SUCCESS) {
            var verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.PCR.readableName(),
                    ExternalConditionParameter.AGE_IN_HOURS_MORE_THAN.parameterName to "2",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.PCR.readableName(),
                    ExternalConditionParameter.AGE_IN_HOURS_LESS_THAN.parameterName to "2",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)


            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.RAT.readableName()

                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)


            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.PCR.readableName(),
                    ExternalConditionParameter.AGE_IN_DAYS_LESS_THAN.parameterName to "0",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)


            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.PCR.readableName(),
                    ExternalConditionParameter.AGE_IN_DAYS_MORE_THAN.parameterName to "100000",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedTestState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.TEST.value,
                    ExternalConditionParameter.TEST_TYPE.parameterName to TestType.PCR.readableName(),
                    ExternalConditionParameter.AGE_IN_HOURS_MORE_THAN.parameterName to "100000",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)
        } else {
            fail()
        }
    }

    @Test
    fun testIssueDateConditions() {
        val parsedVaccinationState = CertificateDecoder.decode(vaccination2of2April25)
        val secondParsedVaccinationState = CertificateDecoder.decode(secondVaccination2of2April25)
        if (parsedVaccinationState is DecodeState.SUCCESS && secondParsedVaccinationState is DecodeState.SUCCESS) {
            var verifier = ExternalConditionVerifier(
                originalCertificate = parsedVaccinationState.dccHolder,
                otherCertificates = listOf(secondParsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.IS_ISSUED_BEFORE_CURRENT_CERTIFICATE.parameterName to "true"

                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = parsedVaccinationState.dccHolder,
                otherCertificates = listOf(secondParsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.IS_ISSUED_BEFORE_CURRENT_CERTIFICATE.parameterName to "false"

                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = parsedVaccinationState.dccHolder,
                otherCertificates = listOf(secondParsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.IS_ISSUED_AFTER_CURRENT_CERTIFICATE.parameterName to "true"

                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = parsedVaccinationState.dccHolder,
                otherCertificates = listOf(secondParsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.IS_ISSUED_AFTER_CURRENT_CERTIFICATE.parameterName to "false"

                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)
        } else {
            fail()
        }
    }

    @Test
    fun testVaccineConditions() {
        val parsedVaccinationState = CertificateDecoder.decode(vaccination2of2April25)
        if (parsedVaccinationState is DecodeState.SUCCESS) {
            var verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_EQUAL.parameterName to "15"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_EQUAL.parameterName to "2"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOES_NOT_EQUAL.parameterName to "2"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOES_NOT_EQUAL.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_LESS_THAN_OR_EQUAL.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_LESS_THAN.parameterName to "3"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_LESS_THAN_OR_EQUAL.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_LESS_THAN_OR_EQUAL.parameterName to "2"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_GREATER_THAN.parameterName to "10"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_GREATER_THAN.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_GREATER_THAN_OR_EQUAL.parameterName to "10"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOSE_GREATER_THAN_OR_EQUAL.parameterName to "2"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.VACCINE_TYPE.parameterName to "EU/1/20/1528"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.VACCINE_TYPE.parameterName to "EU/1/20/1529"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.VACCINE_TYPE_NOT_EQUAL.parameterName to "EU/1/20/1528"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.DOES_NOT_EQUAL.parameterName to "EU/1/20/1529"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == true)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.AGE_IN_HOURS_MORE_THAN.parameterName to "1000000"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.AGE_IN_HOURS_LESS_THAN.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.AGE_IN_DAYS_MORE_THAN.parameterName to "1000000"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.AGE_IN_DAYS_LESS_THAN.parameterName to "1"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)
        } else {
            fail()
        }
    }

    @Test
    fun testRecoveryConditions() {
        val parsedRecoveryState = CertificateDecoder.decode(recoveryApril04)
        if (parsedRecoveryState is DecodeState.SUCCESS) {
            var verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedRecoveryState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.RECOVERY.value,
                    ExternalConditionParameter.AGE_IN_HOURS_MORE_THAN.parameterName to "1000000",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedRecoveryState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.RECOVERY.value,
                    ExternalConditionParameter.AGE_IN_HOURS_LESS_THAN.parameterName to "1",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedRecoveryState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.RECOVERY.value,
                    ExternalConditionParameter.AGE_IN_DAYS_MORE_THAN.parameterName to "1000000",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedRecoveryState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.RECOVERY.value,
                    ExternalConditionParameter.AGE_IN_DAYS_LESS_THAN.parameterName to "1",

                    ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)
        } else {
            fail()
        }
    }

    @Test
    fun testPersonAgeConditions() {
        val parsedVaccinationState = CertificateDecoder.decode(vaccination2of2April25)
        if (parsedVaccinationState is DecodeState.SUCCESS) {
            var verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.PERSON_AGE_IN_YEARS_LESS_THAN.parameterName to "5"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.PERSON_AGE_IN_YEARS_MORE_THAN.parameterName to "100"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.PERSON_AGE_IN_MONTHS_LESS_THAN.parameterName to "5"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)

            verifier = ExternalConditionVerifier(
                originalCertificate = null,
                otherCertificates = listOf(parsedVaccinationState.dccHolder),
                otherCertificatesForSamePerson = listOf(),
                condition = ExternalCondition.HAS_CERTIFICATE.conditionName,
                parameters = mapOf(
                    ExternalConditionParameter.TYPE.parameterName to BusinessRuleCertificateType.VACCINATION.value,
                    ExternalConditionParameter.PERSON_AGE_IN_MONTHS_MORE_THAN.parameterName to "10000"
                ),
                region = "",
                profile = "",
                validationTime = ZonedDateTime.now(),
                validationCore = null
            )

            assertTrue((verifier.evaluateCondition() as? Boolean) == false)
        } else {
            fail()
        }
    }
}