import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.directlink.WalletDirectLinkViewModel
import at.gv.brz.wallet.directlink.WalletDirectLinkViewModel.DirectLinkType.BypassTokenLink
import at.gv.brz.wallet.directlink.WalletDirectLinkViewModel.DirectLinkType.SmsLink
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletDirectLinkViewModelTest {

    companion object {
        const val validDirectLinkSMSOldParameter =
            BuildConfig.smsImportLinkHost + "/result/3ec1b173b6894a27a236163b5dd2c6b3/lSts5_TgUS-XA2UdT0bjslo6tY4n6fNKucQGBbDBbHA"
        const val validDirectLinkWithBPTOldParameter =
            BuildConfig.smsImportLinkHost + "/result/3ec1b173b6894a27a236163b5dd2c6b3/lSts5_TgUS-XA2UdT0bjslo6tY4n6fNKucQGBbDBbHA/?bpt=0fe0450ceebc497d9e76c47f99bc1fd0"

        const val validDirectLinkSMSWithNewParameter =
            BuildConfig.smsImportLinkHost + "/result/3ec1b173b6894a27a236163b5dd2c6b3/lSts5_TgUS-XA2UdT0bjslo6tY4n6fNKucQGBbDBbHA/o0Mm_2CLD81QlOzD9_Y4sw/EoS9XmbS6LCAN_G1XIucmIiE7-DHz1WUkTjIF-Gu9Ro"
        const val validDirectLinkWithBPTWithNewParameter =
            BuildConfig.smsImportLinkHost + "/result/3ec1b173b6894a27a236163b5dd2c6b3/lSts5_TgUS-XA2UdT0bjslo6tY4n6fNKucQGBbDBbHA/o0Mm_2CLD81QlOzD9_Y4sw/EoS9XmbS6LCAN_G1XIucmIiE7-DHz1WUkTjIF-Gu9Ro/?bpt=0fe0450ceebc497d9e76c47f99bc1fd0"

        const val errorDirectLinkSMS =
            BuildConfig.smsImportLinkHost + "//3ec1b173b6894a27a236163b5dd2c6b3"
        const val errorDirectLinkWithBPT =
            BuildConfig.smsImportLinkHost + "/result/lSts5_TgUS-XA2UdT0bjslo6tY4n6fNKucQGBbDBbHA"

    }

    private lateinit var walletDirectLinkViewModel: WalletDirectLinkViewModel

    @Before
    fun setUp() {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        walletDirectLinkViewModel = WalletDirectLinkViewModel(applicationContext)
    }

    /** Working links with old parameter **/
    @Test
    fun testDirectLinkWithOldParameter() {
        when (walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(validDirectLinkSMSOldParameter))) {
            is SmsLink -> assert(true)
            else -> fail()
        }

        when (walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(
            validDirectLinkWithBPTOldParameter))) {
            is BypassTokenLink -> assert(true)
            else -> fail()
        }
    }

    /** Working links with new parameter **/
    @Test
    fun testDirectLinkWithNewParameter() {
        when (walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(
            validDirectLinkSMSWithNewParameter))) {
            is SmsLink -> assert(true)
            else -> fail()
        }

        when (walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(
            validDirectLinkWithBPTWithNewParameter))) {
            is BypassTokenLink -> assert(true)
            else -> fail()
        }
    }

    /** Invalid parameter **/
    @Test
    fun testDirectLinkInvalidParameter() {
        assertEquals(walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(errorDirectLinkSMS)),
            null)
        assertEquals(walletDirectLinkViewModel.checkDirectLinkType(Uri.parse(errorDirectLinkWithBPT)),
            null)
    }

}