import database.user.*
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import utils.Config


class HibernateUtil {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private var sessionFactory: SessionFactory? = null
    private var registry: StandardServiceRegistry? = null

    /**
     * Setup session (connect) to database
     */
    fun setUpSession(): HibernateUtil {

        logger.info("Connecting to postgres database")
        val configuration = Configuration()

        configuration.addAnnotatedClass(database.user.User::class.java)
        configuration.addAnnotatedClass(database.user.UserCredentials::class.java)
        configuration.addAnnotatedClass(database.user.UserProperty::class.java)
        configuration.addAnnotatedClass(UserPropertyStatus::class.java)
        configuration.addAnnotatedClass(database.user.UserPropertyType::class.java)

        configuration.configure("hibernate.cfg.xml")

        if (Config("resources/applicationSecure.conf").config != null) {

            val username = Config().loadPath("postgres.username")
            if (username != null) configuration.setProperty("hibernate.connection.username", username)

            val password = Config().loadPath("postgres.password")
            if (password != null) configuration.setProperty("hibernate.connection.password", password)

            val url = Config().loadPath("postgres.url")
            if (url != null) configuration.setProperty("hibernate.connection.url", url)

        }

        registry = StandardServiceRegistryBuilder().applySettings(configuration.properties).build()

        try {
            sessionFactory = configuration.buildSessionFactory(registry)
            logger.info("Connection established")
        } catch (ex1: PSQLException) {
            logger.error(ex1.message)
            if (sessionFactory != null)
                StandardServiceRegistryBuilder.destroy(registry)
        } catch (ex: Exception) {
            logger.error(ex.message)
            if (sessionFactory != null)
                StandardServiceRegistryBuilder.destroy(registry)
        }

        return this
    }

    /**
     * Close session
     */
    fun closeSession(): HibernateUtil {
        logger.info("Closing sessionFactory session")
        if (sessionFactory != null && sessionFactory!!.isOpen) {
            sessionFactory!!.close()
            logger.info("Session is closed")
        } else {
            logger.info("Session has already been closed")
        }
        return this
    }

    /**
     * Adding refresh token to postgres database
     * @param token refresh token
     * @param
     */
//    fun addRefreshToken(token: String): Int {
//        var session: Session? = null
//
//        if (sessionFactory == null || sessionFactory!!.isClosed)
//            setUpSession()
//
//        return try {
//            session = sessionFactory!!.openSession()
//            session.beginTransaction()
//            val refreshToken = RefreshToken(token = token)
//
//            session.save(refreshToken)
//            session.transaction.commit()
//            session.close()
//            refreshToken.id
//        } catch (ex: Exception) {
//            if (session != null) session.close()
//            logger.error(ex.message)
//            0
//        }
//    }

    /**
     * Adding user property status (e.g. confirmed) postgres database
     * @param statusValue
     * @return id entity that was added, if id = 0 - not added
     */
    fun addUserPropertyStatus(statusValue: String): Int {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {
            session = sessionFactory!!.openSession()
            session.beginTransaction()

            val propertyStatus = UserPropertyStatus(value = statusValue)

            session.save(propertyStatus)
            session.transaction.commit()
            session.close()
            propertyStatus.id ?: 0
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            0
        }
    }

    /**
     * Adding user property type (e.g. email) to  postgres database
     * @param typeName (e.g email, phone)
     * @return id of added propertyType, if id = 0 - not added
     */
    fun addUserPropertyType(typeName: String): Int {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {

            session = sessionFactory!!.openSession()
            session.beginTransaction()
            var propertyType = UserPropertyType(name = typeName)

            session.save(propertyType)
            session.transaction.commit()
            session.close()
            propertyType.id ?: 0
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            0
        }
    }

    /**
     * Adding user property to postgres database
     * @param value value of user property (e.g test@gmail.com)
     * @param propertyType type of this property
     * @param propertyStatus status of this property
     * @param userId id of user with this property
     * @return id of added property, if id = 0 - not added
     */
    fun addUserProperty(
        value: String,
        propertyType: UserPropertyType,
        propertyStatus: UserPropertyStatus? = null,
        userId: Int
    ): Int {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {

            session = sessionFactory!!.openSession()
            session.beginTransaction()


            val userProperty =
                UserProperty(
                    value = value,
                    userPropertyType = propertyType,
                    userPropertyStatus = propertyStatus,
                    userId = getEntity(userId, User())
                )

            session.save(userProperty)
            session.transaction.commit()
            session.close()
            userProperty.id ?: 0
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            0
        }
    }

    /**
     * Adding user to postgres database
     * @param firstName users first name
     * @param lastName users last name
     * @param middleName users middle name
     * @return id of added user, if id = 0 - not added
     */
    fun addUser(
        firstName: String,
        lastName: String,
        middleName: String
    ): Int {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {

            session = sessionFactory!!.openSession()
            session.beginTransaction()

            val user =
                User(
                    firstName = firstName,
                    lastName = lastName,
                    middleName = middleName
                )

            session.save(user)
            session.transaction.commit()
            session.close()
            user.id
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            0
        }
    }

    //TODO: TEST THIS FUNCTION on adding and result!
    /**
     * Add UserCredentials to postgres database
     * @param username users username
     * @param password users password
     * @param userId user id, with credentials
     * @return true if added else false
     */
    fun addUserCredentials(
        username: String,
        password: String,
        userId: Int
    ): Boolean {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {

            session = sessionFactory!!.openSession()
            session.beginTransaction()

            val userCredentials =
                UserCredentials(
                    username = username,
                    password = password,
                    userId = getEntity(userId, User())
                )
            session.save(userCredentials)
            session.transaction.commit()
            session.close()
            true
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            false
        }
    }

    /**
     * Getting entity by id
     * @param id id of entity
     * @param classRef class reference T
     * @return T
     */
    fun <T : Any> getEntity(id: Int, classRef: T): T? {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {
            session = sessionFactory!!.openSession()
            session.beginTransaction()
            val userProperty = session.get(classRef::class.java, id)
            session.close()
            userProperty
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            null
        }
    }

    /**
     * Getting propertyStatusByValue
     * @param value - value of statusProperty
     * @return userPropertyStatus if found else null
     */
    fun getUserPropertyStatusByValue(value: String): UserPropertyStatus? {
        val entities = getEntities(UserPropertyStatus())
        var entity: UserPropertyStatus? = null
        if (entities != null) {
            for (it in entities) {
                if (it.value == value) {
                    entity = it
                    break
                }
            }
        }
        return entity
    }

    /**
     * Getting entities from table
     * @param classRef class reference T
     * @return List<T>
     */
    fun <T : Any> getEntities(classRef: T): List<T>? {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {
            session = sessionFactory!!.openSession()
            session.beginTransaction()

            val builder = session.criteriaBuilder
            val criteria = builder.createQuery(classRef::class.java)
            criteria.from(classRef::class.java)
            val userProperties = session.createQuery(criteria).resultList

            session.close()
            userProperties
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            null
        }
    }

    /**
     * Delete entity(ies) from table by id
     * @param id id of entity, default = 0 -> all entities
     * @param classRef  class reference T
     * @return boolean value: true if deleted else false
     */
    fun <T : Any> deleteEntities(id: Int = 0, classRef: T): Boolean {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {
            session = sessionFactory!!.openSession()
            session.beginTransaction()
            if (id != 0) {
                session.delete(session.get(classRef::class.java, id))
            } else {
                getEntities(classRef)?.forEach {
                    session.delete(it)
                }
            }
            session.transaction.commit()
            session.close()

            true
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            false
        }
    }

    /**
     * Updating entetys
     * @param classRef class reference T
     * @return true if updated else false
     */
    fun <T : Any> updateEntity(classRef: T): Boolean {
        var session: Session? = null

        if (sessionFactory == null || sessionFactory!!.isClosed)
            setUpSession()

        return try {

            session = sessionFactory!!.openSession()
            session.beginTransaction()

            session.merge(classRef)
            session.evict(classRef)
            session.transaction.commit()
            session.close()
            true
        } catch (ex: Exception) {
            if (session != null) session.close()
            logger.error(ex.message + " sessionFactory")
            false
        }
    }
}
