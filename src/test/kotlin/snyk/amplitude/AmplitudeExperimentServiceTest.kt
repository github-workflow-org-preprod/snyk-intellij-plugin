package snyk.amplitude

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.snyk.plugin.pluginSettings
import io.snyk.plugin.services.SnykApplicationSettingsStateService
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import snyk.amplitude.AmplitudeExperimentService.Companion.CHANGE_AUTHENTICATE_BUTTON
import snyk.amplitude.AmplitudeExperimentService.Companion.TREATMENT_GROUP
import snyk.amplitude.api.AmplitudeExperimentApiClient
import snyk.amplitude.api.ExperimentUser
import snyk.amplitude.api.Variant

class AmplitudeExperimentServiceTest {
    private val amplitudeApiClientMock = mockk<AmplitudeExperimentApiClient>()
    private lateinit var cut: AmplitudeExperimentService
    private lateinit var user: ExperimentUser

    private fun variantMap(value: String): MutableMap<String, Variant> {
        val variant = Variant(value, null)
        val mapEntry = Pair(CHANGE_AUTHENTICATE_BUTTON, variant)
        val expectedVariants = mutableMapOf(mapEntry)
        expectedVariants[CHANGE_AUTHENTICATE_BUTTON] = variant
        return expectedVariants
    }

    private fun stubApi(expectedVariants: MutableMap<String, Variant>) {
        every { amplitudeApiClientMock.allVariants(any()) } returns expectedVariants
        cut.fetch(user)
    }

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic("io.snyk.plugin.UtilsKt")
        every { pluginSettings() } returns SnykApplicationSettingsStateService()
        cut = AmplitudeExperimentService()
        cut.setApiClient(amplitudeApiClientMock)
        user = ExperimentUser("testUser")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isPartOfExperimentalWelcomeWorkflow should return true, if user is in the treatment group`() {
        val expectedVariants = variantMap(TREATMENT_GROUP)
        stubApi(expectedVariants)

        val isPartOfExperiment = cut.isPartOfExperimentalWelcomeWorkflow()

        assertTrue("Expected user to be part of treatment group, but wasn't.", isPartOfExperiment)
    }

    @Test
    fun `isPartOfExperimentalWelcomeWorkflow should return false, if user is in the control group`() {
        val expectedVariants = variantMap("anything that is not treatment group")
        stubApi(expectedVariants)

        val isPartOfExperiment = cut.isPartOfExperimentalWelcomeWorkflow()
        assertFalse("Expected user to be part of control group, but wasn't.", isPartOfExperiment)
    }

    @Test
    fun `fetch should call amplitude for data with the userId`() {
        every { amplitudeApiClientMock.allVariants(any()) } returns emptyMap()

        cut.fetch(user)

        verify(exactly = 1) { amplitudeApiClientMock.allVariants(user) }
    }

    @Test
    fun `isPartOfExperimentalWelcomeWorkflow should return false, if experiment not found`() {
        every { amplitudeApiClientMock.allVariants(any()) } returns emptyMap()
        cut.fetch(user)

        val partOfExperimentalWelcomeWorkflow = cut.isPartOfExperimentalWelcomeWorkflow()

        assertFalse("Expected control group, but wasn't.", partOfExperimentalWelcomeWorkflow)
    }
}
