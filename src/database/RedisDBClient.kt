package database

import com.google.gson.JsonObject
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.slf4j.LoggerFactory
import utils.Config
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

    fun addSecret(id: String, secret: String): Boolean {
        logger.info("Adding secret with id = $id to redis database")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            try {
                writeSecret(id, secret)
            } catch (ex: io.lettuce.core.RedisCommandExecutionException) {
                logger.error(ex.message)
                false
            }
        } else {
            logger.info("Can't add secret with id = $id")
            false
        }

    }

    fun getSecret(id: String): String? {
        logger.info("Getting secret with id = $id from redis database")

        if (!isConnected()) loadConnectParametersFromConfigAndConnect()

        return if (isConnected()) {
            try {
                syncCommands!!.get(id)
            } catch (ex: io.lettuce.core.RedisCommandExecutionException) {
                logger.error(ex.message)
                null
            }
        } else {
            logger.info("Can't add secret with id = $id")
            null
        }
    }

    /**
     * Deleting secret by user id
     * @param id user id
     */
    fun deleteSecret(id: String?): Boolean {
        if (!isConnected()) loadConnectParametersFromConfigAndConnect()
        return if (isConnected()) {
            when (syncCommands!!.del(id)) {
                0L -> {
                    logger.info("User secret deleted id = $id")
                    false
                }
                else -> {
                    logger.info("User secret not deleted id = $id")
                    true
                }
            }
        } else {
            false
        }

    }

    private fun writeSecret(id: String, secret: String): Boolean {
        return when (syncCommands!!.set(id, secret)) {
            "OK" -> {
                logger.info("Secret $id added to redis database!")
                true
            }
            else -> {
                logger.error("Error adding secret $id")
                false
            }

        }
    }

}