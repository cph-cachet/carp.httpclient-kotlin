package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.EmailAddress
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.ApplicationServiceHttpClient
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import dk.cachet.carp.protocols.domain.StudyProtocolSnapshot
import dk.cachet.carp.studies.application.StudyService
import dk.cachet.carp.studies.domain.users.StudyOwner
import dk.cachet.carp.studies.domain.StudyStatus
import dk.cachet.carp.studies.domain.users.AssignParticipantDevices
import dk.cachet.carp.studies.domain.users.Participant
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

    /**
     * Get status for all studies created by the specified [owner].
     */
    override suspend fun getStudiesOverview( owner: StudyOwner ): List<StudyStatus> =
        postRequest( StudyServiceRequest.GetStudiesOverview( owner ) )

    /**
     * Add a [Participant] to the study with the specified [studyId], identified by the specified [email] address.
     * In case the [email] was already added before, the same [Participant] is returned.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun addParticipant( studyId: UUID, email: EmailAddress ): Participant =
        postRequest( StudyServiceRequest.AddParticipant( studyId, email ) )

    /**
     * Get all [Participant]s for the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getParticipants( studyId: UUID ): List<Participant> =
        postRequest( StudyServiceRequest.GetParticipants( studyId) )

    /**
     * Specify the study [protocol] to use for the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist,
     * when the provided [protocol] snapshot is invalid,
     * or when the protocol contains errors preventing it from being used in deployments.
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
     * Deploy the study with the given [studyId] to a [group] of previously added participants.
     *
     * @throws IllegalArgumentException when:
     *  - a study with [studyId] does not exist
     *  - [group] is empty
     *  - any of the participants specified in [group] does not exist
     *  - any of the device roles specified in [group] are not part of the configured study protocol
     *  - not all devices part of the study have been assigned a participant
     * @throws IllegalStateException when the study is not yet ready for deployment.
     */
    override suspend fun deployParticipantGroup( studyId: UUID, group: Set<AssignParticipantDevices> ): StudyStatus =
        postRequest( StudyServiceRequest.DeployParticipantGroup( studyId, group ) )
}
