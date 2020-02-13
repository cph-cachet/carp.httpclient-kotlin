package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.ApplicationServiceHttpClient
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import dk.cachet.carp.studies.application.StudyService
import dk.cachet.carp.studies.domain.StudyOwner
import dk.cachet.carp.studies.domain.StudyStatus
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig


/**
 * Create and manage studies on a CARP HTTP studies endpoint.
 */
class StudyServiceHttpClient(
    /**
     * [host] name of the studies endpoint, without port (domain) and protocol.
     */
    host: String,
    /**
     * [HttpClient] configuration builder used to override the default CARP configuration.
     */
    configureBlock: HttpClientConfig<*>.() -> Unit = {}
) : ApplicationServiceHttpClient<StudyServiceRequest>(
        host,
        createStudiesSerializer(),
        StudyServiceRequest.serializer(),
        configureBlock
    ),
    StudyService
{
    /**
     * Create a new study for the specified [owner].
     *
     * @param name A descriptive name for the study, assigned by, and only visible to, the [owner].
     * @param invitation
     *  An optional description of the study, shared with participants once they are invited.
     *  In case no description is specified, [name] is used as the name in [invitation].
     */
    override suspend fun createStudy( owner: StudyOwner, name: String, invitation: StudyInvitation? ): StudyStatus =
        postRequest( StudyServiceRequest.CreateStudy( owner, name, invitation ) )

    /**
     * Get the status for a study with the given [studyId].
     *
     * @param studyId The id of the study to return [StudyStatus] for.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getStudyStatus( studyId: UUID ): StudyStatus =
        postRequest( StudyServiceRequest.GetStudyStatus( studyId ) )
}
