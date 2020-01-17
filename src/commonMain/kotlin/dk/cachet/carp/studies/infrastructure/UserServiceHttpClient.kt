package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.EmailAddress
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.ApplicationServiceHttpClient
import dk.cachet.carp.studies.application.UserService
import dk.cachet.carp.studies.domain.users.Account
import dk.cachet.carp.studies.domain.users.Participant
import dk.cachet.carp.studies.domain.users.Username
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig


/**
 * Create [Account]'s and include them as [Participant]'s for a study on a CARP HTTP users endpoint.
 */
class UserServiceHttpClient(
    /**
     * [host] name of the users endpoint, without port (domain) and protocol.
     */
    host: String,
    /**
     * [HttpClient] configuration builder used to override the default CARP configuration.
     */
    configureBlock: HttpClientConfig<*>.() -> Unit = {}
) : ApplicationServiceHttpClient<UserServiceRequest>(
        host,
        createStudiesSerializer(),
        UserServiceRequest.serializer(),
        configureBlock
    ),
    UserService
{
    /**
     * Create an account which is identified by an [emailAddress] someone has access to.
     * In case no [Account] is associated with the specified [emailAddress], send out a confirmation email.
     */
    override suspend fun createAccount( emailAddress: EmailAddress ) =
        postRequest<Unit>( UserServiceRequest.CreateAccountWithEmailAddress( emailAddress ) )

    /**
     * Create an account which is identified by a unique [username].
     *
     * @throws IllegalArgumentException when an [Account] with the specified [username] already exists.
     */
    override suspend fun createAccount( username: Username ): Account =
        postRequest( UserServiceRequest.CreateAccountWithUsername( username ) )

    /**
     * Create a participant for the study with the specified [studyId] and [Account] identified by [accountId].
     *
     * @throws IllegalArgumentException when an [Account] with the specified [accountId] does not exist.
     */
    override suspend fun createParticipant( studyId: UUID, accountId: UUID ): Participant =
        postRequest( UserServiceRequest.CreateParticipant( studyId, accountId ) )

    /**
     * Get all participants included in a study for the given [studyId].
     */
    override suspend fun getParticipantsForStudy( studyId: UUID ): List<Participant> =
        postRequest( UserServiceRequest.GetParticipantsForStudy( studyId ) )

    /**
     * Create a participant for the study with the specified [studyId] and [Account] identified by [emailAddress].
     * In case no [Account] is associated with the specified [emailAddress], send out an invitation to register in order to participate in the study.
     * TODO: studyId should be replaced with specific information about the study in order to prevent a dependency on study service here.
     */
    override suspend fun inviteParticipant( studyId: UUID, emailAddress: EmailAddress ): Participant =
        postRequest( UserServiceRequest.InviteParticipant( studyId, emailAddress ))
}
