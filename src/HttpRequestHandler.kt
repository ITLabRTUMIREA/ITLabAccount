@file:Suppress("IMPLICIT_CAST_TO_ANY")

import com.auth0.jwt.JWT
import com.google.gson.*
import database.RedisDBClient
import io.ktor.features.ContentNegotiation
import io.ktor.application.Application
import io.ktor.gson.*
import io.ktor.application.install
import io.ktor.routing.*
import org.slf4j.LoggerFactory
import database.tables.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.InputStreamReader
import io.ktor.request.receiveStream
import io.ktor.application.call
import utils.*

@Suppress("requestHandler")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = true) {


    val logger = LoggerFactory.getLogger("HttpRequestHandler")

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        }
    }

    val redisClient = RedisDBClient()
    install(Authentication) {

        jwt(name = "access") {
            realm = Config().loadPath("ktor.jwt.realm") ?: "ru.rtuitlab.account"
            this.verifier {
                val userId = JWT.decode(it.render().removePrefix("Bearer ")).getClaim("user_id").asInt()
                val secret = when (val a = redisClient.getSecret(userId.toString())) {
                    null -> "qmvn13knfj3344k1"
                    else -> a
                }
                JwtConfig(secret).accessVerifier
            }
            validate {
                HibernateUtil().getEntity(it.payload.getClaim("user_id").asInt(), User())
            }
        }

        jwt(name = "refresh") {
            realm = Config().loadPath("ktor.jwt.realm") ?: "ru.rtuitlab.account"
            this.verifier {
                val userId = JWT.decode(it.render().removePrefix("Bearer ")).getClaim("user_id").asInt()
                val secret = when (val a = redisClient.getSecret(userId.toString())) {
                    null -> Config().loadPath("ktor.jwt.defaultSecret") ?: "qmvn13knfj3344k1"
                    else -> a
                }
                JwtConfig(secret).accessVerifier
            }
            validate {
                HibernateUtil().getEntity(it.payload.getClaim("user_id").asInt(), User())
            }
        }
    }

    val hibernateUtil = HibernateUtil().setUpSession()
    install(Routing) {

        post("api/registration") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

            val userCredentials = Gson().fromJson(tmp?.get("userCredentials"), JsonObject::class.java)
            val userInfo = Gson().fromJson(tmp?.get("user"), JsonObject::class.java)
            val userProperties = Gson().fromJson(tmp?.get("userProperty"), JsonArray::class.java)

            if (userCredentials != null && userInfo != null && userProperties != null) {

                val id = hibernateUtil.addUser(
                    firstName = userInfo.get("firstName").asString,
                    lastName = userInfo.get("lastName").asString,
                    middleName = userInfo.get("middleName").asString
                )

                val result = JsonObject()

                if (id != 0) {

                    val resultCredAdding = hibernateUtil.addUserCredentials(
                        username = userCredentials.get("username").asString,
                        password = PasswordConfig.generatePassword(userCredentials.get("password").asString),
                        userId = id
                    )

                    if (resultCredAdding) {
                        userProperties.forEach {
                            val property = it.asJsonObject
                            val value = property.get("value").asString
                            val userPropertyType =
                                hibernateUtil.getEntity(property.get("userpropertytype_id").asInt, UserPropertyType())

                            val userPropertyStatus = hibernateUtil.getUserPropertyStatusByValue("not confirmed")

                            if (value != null && userPropertyType != null && userPropertyStatus != null) {

                                val propertyId = hibernateUtil.addUserProperty(
                                    userId = id,
                                    value = value,
                                    propertyType = userPropertyType,
                                    propertyStatus = userPropertyStatus
                                )

                                if (propertyId == 0) {
                                    logger.error(
                                        "Error adding ${userPropertyType.name} for user ${userInfo.get("firstName").asString} ${userInfo.get(
                                            "lastName"
                                        ).asString}"
                                    )
                                    result.addProperty(userPropertyType.name, "error")
                                }
                            }
                        }
                        logger.info("User added to postgres database with id = $id")
                        result.addProperty("id", id)
                        //TODO: send confirmation to phone, email
                    } else {
                        logger.error("Error adding credentials")
                        result.addProperty("credentials", "error")
                        hibernateUtil.deleteEntities(id, User())
                        call.response.status(HttpStatusCode.BadRequest)
                    }

                } else {
                    logger.error("Can't add user ${userInfo.get("firstName").asString} ${userInfo.get("lastName").asString} to database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
                call.respond(result)
            } else {
                logger.error("Can't add entity User to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/authentication") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val userLog = tmp?.get("username")
            val userPas = tmp?.get("password")
            if (userLog != null && userPas != null) {
                val userCredentials = hibernateUtil.getUserCredentialsByUserName(userLog.asString)

                if (userCredentials != null && PasswordConfig.matchPassword(
                        userPas.asString,
                        userCredentials.password!!
                    )
                ) {
                    val result = JsonObject()

                    val secret = Secret.generate()
                    val jwt = JwtConfig(secret)
                    val refreshToken = jwt.makeRefresh(userCredentials.user!!)
                    val accessToken = jwt.makeAccess(userCredentials.user)
                    redisClient.addSecret(userCredentials.user.id.toString(), secret)

                    result.addProperty("refreshToken", refreshToken)
                    result.addProperty("refresh_expire_in", JWT.decode(refreshToken).expiresAt.time)
                    result.addProperty("accessToken", accessToken)
                    result.addProperty("access_expire_in", JWT.decode(accessToken).expiresAt.time)
                    call.respond(result)
                } else {
                    logger.error("Invalid username or password")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            } else {
                logger.error("Wrong json")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        authenticate("refresh") {

            get("api/updateTokens") {
                val user = call.user
                val result = JsonObject()
                if (user != null) {

                    val secret = Secret.generate()
                    val jwt = JwtConfig(secret)
                    val refreshToken = jwt.makeRefresh(user!!)
                    val accessToken = jwt.makeAccess(user)
                    redisClient.addSecret(user.id.toString(), secret)

                    result.addProperty("refreshToken", refreshToken)
                    result.addProperty("refresh_expire_in", JWT.decode(refreshToken).expiresAt.time)
                    result.addProperty("accessToken", accessToken)
                    result.addProperty("access_expire_in", JWT.decode(accessToken).expiresAt.time)
                    call.respond(result)
                } else {
                    logger.error("Access denied")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }
        }

        authenticate("access") {

            post("api/database/connect") {
                hibernateUtil.setUpSession()
            }

            get("api/database/userPropertyStatus") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }

                if (id != null) {

                    val userPropertyStatus = when (id) {
                        0 -> hibernateUtil.getEntities(UserPropertyStatus())
                        else -> hibernateUtil.getEntity(id, UserPropertyStatus())
                    }

                    if (userPropertyStatus != null) {
                        val jsonElement = JsonParser().parse(Gson().toJson(userPropertyStatus)) as JsonElement
                        val jsonObject = JsonObject()
                        jsonObject.add("data", jsonElement)
                        logger.info("Entity userPropertyStatus with id = $id is gotten successfully")
                        call.respond(jsonObject)
                    } else {
                        logger.error("Entity userPropertyStatus with id = $id is not gotten")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            post("api/database/addUserPropertyStatus") {
                val tmp: JsonObject? =
                    Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                val value = tmp?.get("statusValue")
                if (value != null) {
                    val result = JsonObject()
                    val id = hibernateUtil.addUserPropertyStatus(value.asString)

                    if (id != 0) {
                        result.addProperty("id", id)
                        logger.info("UserPropertyStatus added to postgres database with id = $id")
                        call.respond(result)
                    } else {
                        call.response.status(HttpStatusCode.BadRequest)
                    }


                } else {
                    logger.error("Can't add entity UserPropertyStatus to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            put("api/database/userPropertyStatus") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    var propertyStatus = hibernateUtil.getEntity(id, UserPropertyStatus())
                    if (propertyStatus != null) {
                        val tmp: JsonObject? =
                            Gson().fromJson(
                                InputStreamReader(call.receiveStream(), "UTF-8"),
                                JsonObject::class.java
                            )
                        val value = tmp?.get("statusValue")
                        if (value != null) {
                            propertyStatus = propertyStatus.copy(value = value.asString)

                            if (hibernateUtil.updateEntity(propertyStatus)) {
                                logger.info("Entity userPropertyStatus with id = $id is updated successfully")
                                call.response.status(HttpStatusCode.OK)
                            } else {
                                logger.info("Entity userPropertyStatus with id = $id is updated unsuccessfully")
                                call.response.status(HttpStatusCode.Conflict)
                            }

                        } else {
                            call.response.status(HttpStatusCode.BadRequest)
                        }

                    } else {
                        logger.info("Entity userPropertyStatus with id = $id is not found")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            delete("api/database/userPropertyStatus") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }
                if (id != null) {
                    val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyStatus())
                    if (resultOfDeleting) {
                        logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.error("Entity userPropertyStatus with id = $id is not deleted")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("/api/database/userPropertyType") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }

                if (id != null) {

                    val userPropertyType = when (id) {
                        0 -> hibernateUtil.getEntities(UserPropertyType())
                        else -> hibernateUtil.getEntity(id, UserPropertyType())
                    }

                    if (userPropertyType != null) {
                        val jsonElement = JsonParser().parse(Gson().toJson(userPropertyType)) as JsonElement
                        val jsonObject = JsonObject()
                        jsonObject.add("data", jsonElement)
                        logger.info("Entity userPropertyType with id = $id is gotten successfully")
                        call.respond(jsonObject)
                    } else {
                        logger.error("Entity userPropertyType with id = $id is not gotten")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            post("/api/database/addUserPropertyType") {
                val tmp: JsonObject? =
                    Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                val name = tmp?.get("typeName")
                if (name != null) {
                    val result = JsonObject()
                    val id = hibernateUtil.addUserPropertyType(name.asString)
                    if (id != 0) {
                        result.addProperty("id", id)
                        logger.info("UserPropertyType added to postgres database with id = $id")
                        call.respond(result)
                    } else {
                        call.response.status(HttpStatusCode.BadRequest)
                    }
                } else {
                    logger.error("Can't add entity UserPropertyType to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            put("api/database/userPropertyType") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    var propertyType = hibernateUtil.getEntity(id, UserPropertyType())
                    if (propertyType != null) {
                        val tmp: JsonObject? =
                            Gson().fromJson(
                                InputStreamReader(call.receiveStream(), "UTF-8"),
                                JsonObject::class.java
                            )
                        val name = tmp?.get("typeName")

                        if (name != null) {
                            propertyType = propertyType.copy(name = name.asString)
                            if (hibernateUtil.updateEntity(propertyType)) {
                                logger.info("Entity userPropertyType with id = $id is updated successfully")
                                call.response.status(HttpStatusCode.OK)
                            } else {
                                logger.info("Entity userPropertyType with id = $id is updated unsuccessfully")
                                call.response.status(HttpStatusCode.Conflict)
                            }
                        } else {
                            call.response.status(HttpStatusCode.BadRequest)
                        }

                    } else {
                        logger.info("Entity userPropertyType with id = $id is not found")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            delete("api/database/userPropertyType") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }
                if (id != null) {
                    val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyType())
                    if (resultOfDeleting) {
                        logger.info("Entity UserPropertyType with id = $id is deleted successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.error("Entity UserPropertyType with id = $id is not deleted")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("api/database/userProperty") {

                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }

                if (id != null) {
                    val userProperty = when (id) {
                        0 -> hibernateUtil.getEntities(UserProperty())
                        else -> hibernateUtil.getEntity(id, UserProperty())
                    }

                    if (userProperty != null) {
                        val jsonElement = JsonParser().parse(Gson().toJson(userProperty)) as JsonElement
                        val jsonObject = JsonObject()
                        jsonObject.add("data", jsonElement)
                        logger.info("Entity UserProperty with id = $id is gotten successfully")
                        call.respond(jsonObject)
                    } else {
                        logger.error("Entity UserProperty with id = $id is not gotten")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            post("api/database/addUserProperty") {
                val tmp: JsonObject? =
                    Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

                val value = tmp?.get("value")
                val typeId = tmp?.get("propertytype_id")
                val userId = tmp?.get("user_id")

                if (value != null && typeId != null && userId != null) {

                    val userPropertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                    val userPropertyStatus = hibernateUtil.getUserPropertyStatusByValue("not confirmed")

                    if (userPropertyType != null && userPropertyStatus != null) {
                        val result = JsonObject()

                        val id = hibernateUtil.addUserProperty(
                            value = value.asString,
                            propertyType = userPropertyType,
                            propertyStatus = userPropertyStatus,
                            userId = userId.asInt
                        )
                        if (id != 0) {
                            result.addProperty("id", id)
                            logger.info("UserProperty added to postgres database with id = $id")
                            call.respond(result)
                        } else {
                            call.response.status(HttpStatusCode.BadRequest)
                        }
                    } else {
                        logger.error("id of userPropertyType or userPropertyStatus is invalid")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Can't add entity UserProperty to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            post("api/database/addFullUserProperty") {
                val tmp: JsonObject? =
                    Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

                val value = tmp?.get("value")
                val propertyType = Gson().fromJson(tmp?.get("UserPropertyType"), UserPropertyType()::class.java)
                val propertyStatus =
                    Gson().fromJson(tmp?.get("UserPropertyStatus"), UserPropertyStatus()::class.java)
                val userId = tmp?.get("userId")

                if (value != null && propertyType != null && propertyStatus != null && userId != null) {
                    val id = hibernateUtil.addUserProperty(
                        value = value.asString,
                        propertyType = propertyType,
                        propertyStatus = propertyStatus,
                        userId = userId.asInt
                    )
                    if (id != 0) {
                        val result = JsonObject()
                        result.addProperty("id", id)
                        call.respond(result)
                        logger.info("UserProperty added to postgres database with id = $id")
                    } else {
                        call.response.status(HttpStatusCode.BadRequest)
                    }
                } else {
                    logger.error("Can't add entity UserProperty to postgres database")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            put("api/database/userProperty") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    var userProperty = hibernateUtil.getEntity(id, UserProperty())
                    if (userProperty != null) {
                        val tmp: JsonObject? =
                            Gson().fromJson(
                                InputStreamReader(call.receiveStream(), "UTF-8"),
                                JsonObject::class.java
                            )
                        val value = tmp?.get("value")
                        val typeId = tmp?.get("propertytype_id")
                        val statusId = tmp?.get("propertystatus_id")

                        if (value != null) {
                            userProperty = userProperty.copy(value = value.asString)
                        }

                        if (statusId != null) {
                            val propertyStatus = hibernateUtil.getEntity(statusId.asInt, UserPropertyStatus())
                            userProperty = userProperty.copy(userPropertyStatus = propertyStatus)
                        }

                        if (typeId != null) {
                            val propertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                            userProperty = userProperty.copy(userPropertyType = propertyType)
                        }

                        if (hibernateUtil.updateEntity(userProperty)) {
                            logger.info("Entity UserProperty with id = $id is updated successfully")
                            call.response.status(HttpStatusCode.OK)
                        } else {
                            logger.info("Entity UserProperty with id = $id is not updated")
                            call.response.status(HttpStatusCode.Conflict)
                        }

                    } else {
                        logger.info("Entity UserProperty with id = $id is not found")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            delete("api/database/userProperty") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }
                if (id != null) {
                    val resultOfDeleting = hibernateUtil.deleteEntities(id, UserProperty())
                    if (resultOfDeleting) {
                        logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.error("Entity userPropertyStatus with id = $id is not deleted")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("api/database/user") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }

                //val type = call.parameters["type"].toString()

                if (id != null) {
                    val user = when (id) {
                        0 -> hibernateUtil.getEntities(User())
                        else -> hibernateUtil.getEntity(id, User())
                    }

                    if (user != null) {
                        val jsonElement = JsonParser().parse(Gson().toJson(user)) as JsonElement
                        val jsonObject = JsonObject()
                        jsonObject.add("data", jsonElement)
                        logger.info("Entity User with id = $id is gotten successfully")
                        call.respond(jsonObject)
                    } else {
                        logger.error("Entity User with id = $id is not gotten")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            put("api/database/user") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    var user = hibernateUtil.getEntity(id, User())
                    if (user != null) {
                        val tmp: JsonObject? =
                            Gson().fromJson(
                                InputStreamReader(call.receiveStream(), "UTF-8"),
                                JsonObject::class.java
                            )
                        val firstName = tmp?.get("firstName")
                        val lastName = tmp?.get("lastName")
                        val middleName = tmp?.get("middleName")

                        if (firstName != null) {
                            user = user.copy(firstName = firstName.asString)
                        }

                        if (lastName != null) {
                            user = user.copy(lastName = lastName.asString)
                        }

                        if (middleName != null) {
                            user = user.copy(middleName = middleName.asString)
                        }

                        if (hibernateUtil.updateEntity(user)) {
                            logger.info("Entity User with id = $id is updated successfully")
                            call.response.status(HttpStatusCode.OK)
                        } else {
                            logger.info("Entity User with id = $id is not updated")
                            call.response.status(HttpStatusCode.Conflict)
                        }

                    } else {
                        logger.info("Entity User with id = $id is not found")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            delete("api/database/user") {
                val id = when (val a = call.parameters["id"]) {
                    "all", "0", "*" -> 0
                    else -> a?.toIntOrNull()
                }
                if (id != null) {
                    val resultOfDeleting = hibernateUtil.deleteEntities(id, User())
                    if (resultOfDeleting) {
                        logger.info("Entity User with id = $id is deleted successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.error("Entity User with id = $id is not deleted")
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    logger.error("Parameter id is incorrect")
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}




