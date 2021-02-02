package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.EmailAddress
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.data.Data
import dk.cachet.carp.common.data.input.InputDataType
import dk.cachet.carp.common.ddd.ApplicationServiceHttpClient
import dk.cachet.carp.studies.application.ParticipantService
import dk.cachet.carp.studies.domain.users.AssignParticipantDevices
import dk.cachet.carp.studies.domain.users.Participant
import dk.cachet.carp.studies.domain.users.ParticipantGroupStatus
import io.ktor.client.*


/**
 * Add participants to studies and create deployments for them on a CARP HTTP participants endpoint.
 */
class ParticipantServiceHttpClient(
    /**
     * [host] name of the studies endpoint, without port (domain) and protocol.
     */
    host: String,
    /**
     * [HttpClient] configuration builder used to override the default CARP configuration.
     */
    configureBlock: HttpClientConfig<*>.() -> Unit = {}
) : ApplicationServiceHttpClient<ParticipantServiceRequest>(
        host,
        createStudiesSerializer(),
        ParticipantServiceRequest.serializer(),
        configureBlock
    ),
    ParticipantService
{
    /**
     * Add a [Participant] to the study with the specified [studyId], identified by the specified [email] address.
     * In case the [email] was already added before, the same [Participant] is returned.
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun addParticipant(studyId: UUID, email: EmailAddress): Participant =
        postRequest( ParticipantServiceRequest.AddParticipant( studyId, email ) )

    /**
     * Returns a participant of a study with the specified [studyId], identified by [participantId].
     *
     * @throws IllegalArgumentException when a study with [studyId] or participant with [participantId] does not exist.
     */
    override suspend fun getParticipant( studyId: UUID, participantId: UUID ): Participant =
        postRequest( ParticipantServiceRequest.GetParticipant( studyId, participantId ) )

    /**
     * Get all [Participant]s for the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getParticipants( studyId: UUID): List<Participant> =
        postRequest( ParticipantServiceRequest.GetParticipants( studyId ) )

    /**
     * Deploy the study with the given [studyId] to a [group] of previously added participants.
     * In case a group with the same participants has already been deployed and is still running (not stopped),
     * the latest status for this group is simply returned.
     *
     * @throws IllegalArgumentException when:
     *  - a study with [studyId] does not exist
     *  - [group] is empty
     *  - any of the participants specified in [group] does not exist
     *  - any of the device roles specified in [group] are not part of the configured study protocol
     *  - not all devices part of the study have been assigned a participant
     * @throws IllegalStateException when the study is not yet ready for deployment.
     */
    override suspend fun deployParticipantGroup( studyId: UUID, group: Set<AssignParticipantDevices> ): ParticipantGroupStatus =
        postRequest( ParticipantServiceRequest.DeployParticipantGroup( studyId, group ) )

    /**
     * Get the status of all deployed participant groups in the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when a study with [studyId] does not exist.
     */
    override suspend fun getParticipantGroupStatusList( studyId: UUID ): List<ParticipantGroupStatus> =
        postRequest( ParticipantServiceRequest.GetParticipantGroupStatusList( studyId ) )

    /**
     * Stop the study deployment in the study with the given [studyId]
     * of the participant group with the specified [groupId] (equivalent to the studyDeploymentId).
     * No further changes to this deployment will be allowed and no more data will be collected.
     *
     * @throws IllegalArgumentException when a study with [studyId] or participant group with [groupId] does not exist.
     */
    override suspend fun stopParticipantGroup( studyId: UUID, groupId: UUID ): ParticipantGroupStatus =
        postRequest( ParticipantServiceRequest.StopParticipantGroup( studyId, groupId ) )

    /**
     * Set participant [data] for the given [inputDataType],
     * related to participants of the participant group with [groupId] in the study with the specified [studyId].
     *
     * @throws IllegalArgumentException when:
     *   - a study with [studyId] or participant group with [groupId] does not exist.
     *   - [inputDataType] is not configured as expected participant data in the study protocol
     *   - [data] is invalid data for [inputDataType]
     */
    override suspend fun setParticipantGroupData(
        studyId: UUID,
        groupId: UUID,
        inputDataType: InputDataType,
        data: Data?
    ): ParticipantGroupStatus =
        postRequest( ParticipantServiceRequest.SetParticipantGroupData( studyId, groupId, inputDataType, data ) )
}
