package database

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.slf4j.LoggerFactory
import utils.Config
import utils.Session
import java.time.Duration
import java.util.*
import kotlin.concurrent.timer

const val TTL = 600000L
const val ANSWERTIMEOUT = 10000L

class RedisDBClient {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var syncCommands: RedisCommands<String, String>? = null
    private var timer: Timer? = null

    /**
     * Timer of database connection
     */
    private fun timerDatabaseConnection() {
        this.timer = timer("databaseConnectionTimer", initialDelay = TTL, period = 1L) {
            closeConnection()
            logger.info("Connection refused (Timeout TTL = $TTL)")
        }
    }

    /**
     * test Connection to database
     */
    fun isConnected(): Boolean {
        var result = true
        if (syncCommands != null) {
            try {
                syncCommands!!.ping()
            } catch (ex: RedisException) {
                result = false
                logger.error(ex.message)
            }
        }
        return connection != null && connection!!.isOpen && result
    }

    /**
     * Disconnecting from database
     */
    fun closeConnection() {
        if (connection != null) {
            connection!!.close()
        }
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
        }
    }

    /**
     * Connecting to redis database server
     * @param password - password uses for auth in redis server database
     * @param ip database server address
     * @param port database port address
     */
    constructor(password: String, ip: String, port: Int) {
        connectToDatabase(password, ip, port)
    }

    /**
     * Connecting to redis database server using info for connection from config
     */
    constructor() {
        loadConnectParametersFromConfigAndConnect()
    }

    private fun loadConnectParametersFromConfigAndConnect(): Boolean {
        logger.info("Connecting to database")

        val password = Config().loadPath("redis.password")

        val url = if (!password.isNullOrBlank()) Config().loadPath("redis.url") else null

        val strPort = if (!url.isNullOrBlank()) Config().loadPath("redis.port") else null

        val intPort = if (!strPort.isNullOrBlank()) strPort.toIntOrNull() else null

        return if (intPort != null) {
            connectToDatabase(password!!, url!!, intPort)
        } else {
            logger.error("Cant't connect to redis database")
            false
        }

    }

    private fun connectToDatabase(password: String, url: String, port: Int): Boolean {
        return try {
            redisClient = RedisClient.create("redis://$password@$url:$port/0")
            connection = redisClient!!.connect()

            syncCommands = connection!!.sync()
            syncCommands!!.setTimeout(Duration.ofSeconds(ANSWERTIMEOUT))
            logger.info("Connected to database.")
            timerDatabaseConnection()
            true
        } catch (ex: RedisConnectionException) {
            closeConnection()
            logger.error("${ex.message} (Database)")
            false
        }
    }

    fun deleteLastKeyAndAddNew(userId: String, secret: String): String? {
        val keys = getKeys(userId)
        val key = keys?.get(0)
        return if (key != null) {
            deleteSecret(userId, key) && addSecret(userId, key, secret)
            key
        } else
            null
    }

    fun keysSize(userId: String): Int? =
        getKeys(userId)?.size

    fun containsSecret(userId: String, sessionId: String): Boolean =
        getSecret(userId, sessionId) != null

    /**
     * Updating secret to user with id userId to field sessionId
     * @param userId
     * @param sessionId
     * @param secret
     * @return true if updated else false
     */
    fun updateSecret(userId: String, sessionId: String, secret: String): Boolean =
        deleteSecret(userId, sessionId) && addSecret(userId, sessionId, secret)

    /**
     * Adding secret to user with id userId to field sessionId
     * @param userId
     * @param sessionId
     * @param secret
     * @return true if added else false
     */
    fun addSecret(userId: String, sessionId: String, secret: String): Boolean {
        logger.info("Adding secret to user with id = $userId , to field  = $sessionId to redis database")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            try {
                writeSecretFun(userId, sessionId, secret)
            } catch (ex: io.lettuce.core.RedisCommandExecutionException) {
                logger.error(ex.message)
                false
            }
        } else {
            logger.info("Can't connect to redis")
            false
        }
    }

    /**
     * Getting secret from user with id userId from field with id sessionId
     * @param userId
     * @param sessionId
     * @return secret if got, else null
     */
    fun getSecret(userId: String, sessionId: String): String? {
        logger.info("Getting secret for user with id = $userId, from field = $sessionId from redis database")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            getSecretFun(userId, sessionId)
        } else {
            logger.info("Can't connect to redis")
            null
        }
    }

    /**
     * Deleting secret with id sessionId from user with userId
     * @param userId
     * @param sessionId secretId
     * @return true if deleted else false
     */
    fun deleteSecret(userId: String, sessionId: String): Boolean {
        logger.info("Deleting secret from user with id = $userId, from field = $sessionId from redis database")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            deleteSecretFun(userId, sessionId)
        } else {
            logger.info("Can't connect to redis")
            false
        }
    }

    /**
     * Getting all secret keys for current user with userId
     * @param userId
     * @return list of keys
     */
    fun getKeys(userId: String): MutableList<String>? {
        logger.info("Getting keys from userId = $userId")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            val list = syncCommands!!.hkeys(userId)
            return if (list.isEmpty())
                null
            else
                list
        } else {
            logger.info("Can't connect to redis")
            null
        }
    }

    /**
     * Getting secret from redis database by userID, sessionId
     * @param userId user id
     * @param sessionId session id (field in userId set)
     * @return secret value
     */
    private fun getSecretFun(userId: String, sessionId: String): String? {
        val secret = syncCommands!!.hget(userId, sessionId)
        return if (secret.isNullOrBlank()) {
            logger.info("Secret is not got from user with id = $userId, from field = $sessionId !")
            null
        } else {
            logger.error("Secret got from user with id = $userId, from field = $sessionId !")
            secret
        }
    }

    /**
     * Adding secret to redis database by userID, sessionId
     * @param userId user id
     * @param sessionId session id (field in userId set)
     * @param secret secret
     * @return true if added else false
     */
    private fun writeSecretFun(userId: String, sessionId: String, secret: String): Boolean {
        return when (syncCommands!!.hset(userId, sessionId, secret)) {
            true -> {
                logger.info("Secret to user with id = $userId, to field = $sessionId added to redis database!")
                true
            }
            else -> {
                logger.error("Error adding secret to user with id = $userId, to field = $sessionId !")
                false
            }

        }
    }

    /**
     * Deleting secret from redis database by userID, sessionId
     * @param userId user id
     * @param sessionId session id (field in userId set)
     * @return true if deleted else false
     */
    private fun deleteSecretFun(userId: String, sessionId: String): Boolean {
        return when (syncCommands!!.hdel(userId, sessionId)) {
            0L -> {
                logger.error("Error deleting secret from user with id = $userId, from field = $sessionId !")
                false
            }
            else -> {
                logger.info("Secret was deleted from user with id = $userId, from field = $sessionId !")
                true
            }
        }
    }

}