package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.ApplicationServiceHttpClient
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import dk.cachet.carp.protocols.domain.StudyProtocolSnapshot
import dk.cachet.carp.studies.application.StudyService
import dk.cachet.carp.studies.domain.StudyDetails
import dk.cachet.carp.studies.domain.users.StudyOwner
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
     */
    override suspend fun createStudy(
        owner: StudyOwner,
        /**
         * A descriptive name for the study, assigned by, and only visible to, the [owner].
         */
        name: String,
        /**
         * An optional description of the study, assigned by, and only visible to, the [owner].
         */
        description: String,
        /**
         * An optional description of the study, shared with participants once they are invited.
         * In case no description is specified, [name] is used as the name in [invitation].
         */
        invitation: StudyInvitation?
    ): StudyStatus = postRequest( StudyServiceRequest.CreateStudy( owner, name, description, invitation ) )

    /**
     * Set study details which are visible only to the [StudyOwner].
     *
     * @param studyId The id of the study to update the study details for.
     * @param name A descriptive name for the study.
     * @param description A description of the study.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun setInternalDescription( studyId: UUID, name: String, description: String ): StudyStatus =
        postRequest( StudyServiceRequest.SetInternalDescription( studyId, name, description ) )

    /**
     * Gets detailed information about the study with the specified [studyId], including which study protocol is set.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getStudyDetails( studyId: UUID ): StudyDetails =
        postRequest( StudyServiceRequest.GetStudyDetails( studyId ) )

    /**
     * Get the status for a study with the given [studyId].
     *
     * @param studyId The id of the study to return [StudyStatus] for.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getStudyStatus( studyId: UUID ): StudyStatus =
        postRequest( StudyServiceRequest.GetStudyStatus( studyId ) )

    /**
     * Get status for all studies created by the specified [owner].
     */
    override suspend fun getStudiesOverview( owner: StudyOwner ): List<StudyStatus> =
        postRequest( StudyServiceRequest.GetStudiesOverview( owner ) )

    /**
     * Specify an [invitation], shared with participants once they are invited to the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun setInvitation( studyId: UUID, invitation: StudyInvitation ): StudyStatus =
        postRequest( StudyServiceRequest.SetInvitation( studyId, invitation ) )

    /**
     * Specify the study [protocol] to use for the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist,
     * when the provided [protocol] snapshot is invalid,
     * or when the protocol contains errors preventing it from being used in deployments.
     * @throws IllegalStateException when the study protocol can no longer be set since the study went 'live'.
     */
    override suspend fun setProtocol( studyId: UUID, protocol: StudyProtocolSnapshot ): StudyStatus =
        postRequest( StudyServiceRequest.SetProtocol( studyId, protocol ) )

    /**
     * Lock in the current study protocol so that the study may be deployed to participants.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     * @throws IllegalStateException when no study protocol for the given study is set yet.
     */
    override suspend fun goLive( studyId: UUID ): StudyStatus =
        postRequest( StudyServiceRequest.GoLive( studyId ) )

    /**
     * Remove the study with the specified [studyId].
     *
     * @return True when the study has been deleted, or false when there is no study to delete.
     */
    override suspend fun remove( studyId: UUID ): Boolean =
        postRequest( StudyServiceRequest.Remove( studyId ) )
}
