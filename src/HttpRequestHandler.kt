@file:Suppress("IMPLICIT_CAST_TO_ANY")

import com.google.gson.*
import io.ktor.features.ContentNegotiation
import io.ktor.application.Application
import io.ktor.gson.*
import io.ktor.application.install
import io.ktor.routing.*
import org.slf4j.LoggerFactory
import database.user.*
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.InputStreamReader
import io.ktor.request.receiveStream

@Suppress("requestHandler")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = true) {

    val logger = LoggerFactory.getLogger("HttpRequestHandler")

    //val userSource: UserSource = UserSourceImpl()
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        }
    }

//    install(Authentication) {
//        jwt {
//            verifier(JwtConfig.verifier)
//            val config = Config().config!!
//
//            if(config.hasPath("ktor.jwt.realm")) {
//                realm = config.getString("ktor.jwt.realm")
//                logger.info("Realm loaded from config = $realm")
//            }else{
//                realm = "ru.rtuitlab.account"
//                logger.info("Realm not loaded from config, default realm = $realm")
//            }
//
////            validate {
////                it.payload.id.toInt().let (userSource::findUserById)
////            }
//        }
//    }

    //TODO: Refactor code and do more readable ?

    val hibernateUtil = HibernateUtil().setUpSession()
    install(Routing) {

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
                    call.response.status(HttpStatusCode.InternalServerError)
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
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
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
                    call.response.status(HttpStatusCode.InternalServerError)
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
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
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
            val statusId = tmp?.get("propertystatus_id")
            val userId = tmp?.get("user_id")

            if (value != null && typeId != null && statusId != null && userId != null) {
                val userPropertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                val userPropertyStatus = hibernateUtil.getEntity(statusId.asInt, UserPropertyStatus())
                if (userPropertyType != null && userPropertyStatus != null) {
                    val result = JsonObject()

                    val id = hibernateUtil.addUserProperty(
                        value = value.asString,
                        propertyType = userPropertyType,
                        propertyStatus = userPropertyStatus,
                        userId = userId.asInt
                    )

                    result.addProperty("id", id)
                    logger.info("UserProperty added to postgres database with id = $id")
                    call.respond(result)
                } else {
                    logger.error("id of userPropertyType or userPropertyStatus is invalid")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Can't add entity UserProperty to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

//        post("api/database/addFullUserProperty") {
//            val tmp: JsonObject? =
//                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
//
//            val value = tmp?.get("value")
//            val propertyType = Gson().fromJson(tmp?.get("UserPropertyType"), UserPropertyType()::class.java)
//            val propertyStatus = Gson().fromJson(tmp?.get("UserPropertyStatus"), UserPropertyStatus()::class.java)
//            if (value != null && propertyType != null && propertyStatus != null) {
//                val id = hibernateUtil.addUserProperty(value.asString, propertyType, propertyStatus)
//                val result = JsonObject()
//                result.addProperty("id", id)
//                call.respond(result)
//                logger.info("UserProperty added to postgres database with id = $id")
//            } else {
//                logger.error("Can't add entity UserProperty to postgres database")
//                call.response.status(HttpStatusCode.BadRequest)
//            }
//        }

        put("api/database/userProperty") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var userProperty = hibernateUtil.getEntity(id, UserProperty())
                if (userProperty != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val value = tmp?.get("value")
                    val typeId = tmp?.get("typeId")
                    val statusId = tmp?.get("statusId")

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

//        put("api/database/user") {
//            val id = call.parameters["id"]?.toIntOrNull()
//            if (id != null) {
//                var user = hibernateUtil.getEntity(id, User())
//                if (user != null) {
//                    val tmp: JsonObject? =
//                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
//                    val fName = tmp?.get("firstName")
//                    val lName = tmp?.get("lastName")
//                    val mName = tmp?.get("middleName")
//
//                    val userProperties = tmp?.get("userPropertiesId")?.asJsonArray
//
//                    if (fName != null) {
//                        user = user.copy(firstName = fName.asString)
//                    }
//
//                    if (lName != null) {
//                        user = user.copy(lastName = lName.asString)
//                    }
//
//                    if (mName != null) {
//                        user = user.copy(middleName = mName.asString)
//                    }
//
//                    if (userProperties != null) {
//                        val properties = HashSet<UserProperty>()
//                        userProperties.forEach {
//                            val property = hibernateUtil.getEntity(
//                                it.asInt,
//                                UserProperty()
//                            )
//                            if (property != null) {
//                                properties.add(property)
//                            }
//                        }
//                        user = user.copy(userProperties = properties)
//                    }
//
//                    if (hibernateUtil.updateEntity(user)) {
//                        logger.info("Entity User with id = $id is updated successfully")
//                        call.response.status(HttpStatusCode.OK)
//                    } else {
//                        logger.info("Entity User with id = $id is not updated")
//                        call.response.status(HttpStatusCode.Conflict)
//                    }
//
//                } else {
//                    logger.info("Entity User with id = $id is not found")
//                    call.response.status(HttpStatusCode.NotFound)
//                }
//            } else {
//                logger.error("Parameter id is incorrect")
//                call.response.status(HttpStatusCode.BadRequest)
//            }
//        }

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

        get("api/database/userCredentials") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val user = when (id) {
                    0 -> hibernateUtil.getEntities(UserCredentials())
                    else -> hibernateUtil.getEntity(id, UserCredentials())
                }

                if (user != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(user)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity userCredentials with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity userCredentials with id = $id is not gotten")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUserCredentials") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val username = tmp?.get("username")
            val password = tmp?.get("password")
            val userId = tmp?.get("userId")
            if (username != null && password != null && userId != null) {
                val result = JsonObject()
                val id = hibernateUtil.addUserCredentials(
                    username = username.asString,
                    password = password.asString,
                    userId = userId.asInt
                )
                result.addProperty("id", id)
                logger.info("UserPropertyType added to postgres database with id = $id")
                call.respond(result)
            } else {
                logger.error("Can't add entity UserPropertyType to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/userCredentials") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var userCredentials = hibernateUtil.getEntity(id, UserCredentials())
                if (userCredentials != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val password = tmp?.get("password")
                    if (password != null) {
                        userCredentials = userCredentials.copy(password = password.asString)
                    }

                    if (hibernateUtil.updateEntity(userCredentials)) {
                        logger.info("Entity userCredentials with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userCredentials with id = $id is not updated")
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

        delete("api/database/userCredentials") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserCredentials())
                if (resultOfDeleting) {
                    logger.info("Entity userCredentials with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity userCredentials with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

//        post("api/registration") {
//            val tmp: JsonObject? =
//                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
//            val userCredentials = Gson().fromJson(tmp?.get("userCredentials"), JsonObject::class.java)
//            val userInfo = Gson().fromJson(tmp?.get("user"), JsonObject::class.java)
//            val userProperties = Gson().fromJson(tmp?.get("userProperty"), JsonArray::class.java)
//            if (userCredentials != null && userInfo != null && userProperties != null) {
//
//                val userPropertiesInSet = HashSet<UserProperty>()
//
//
//                val id = hibernateUtil.addUser(
//                    firstName = userInfo.get("firstName").asString,
//                    lastName = userInfo.get("lastName").asString,
//                    middleName = userInfo.get("middleName").asString,
//                    userProperties = userPropertiesInSet
//                )
//                if (id != 0) {
//
//                    userProperties.forEach {
//                        val property = it.asJsonObject
//                        val value = property.get("value").asString
//                        val userPropertyType =
//                            hibernateUtil.getEntity(property.get("userPropertyTypeId").asInt, UserPropertyType())
//
//                        val userPropertyStatus = hibernateUtil.getUserPropertyStatusByValue("not confirmed")
//
//                        if (value != null && userPropertyType != null && userPropertyStatus != null) {
//                            userPropertiesInSet.add(
//                                UserProperty(
//                                    //user
//                                    value = value,
//                                    userPropertyType = userPropertyType,
//                                    userPropertyStatus = userPropertyStatus
//                                )
//                            )
//                        }
//                    }
//
//                    hibernateUtil.addUserCredentials(
//                        username = userCredentials.get("username").asString,
//                        password = userCredentials.get("password").asString,
//                        userId = id
//                    )
//                    logger.info("User added to postgres database with id = $id")
//                } else {
//                    logger.error("Can't add user ${userInfo.get("firstName").asString} ${userInfo.get("lastName").asString} to database")
//                    call.response.status(HttpStatusCode.BadRequest)
//                }
//
//                val result = JsonObject()
//                result.addProperty("id", id)
//
//                call.respond(result)
//            } else {
//                logger.error("Can't add entity User to postgres database")
//                call.response.status(HttpStatusCode.BadRequest)
//            }
//        }

        post("api/authentication/login") {

            val hibernateUtil = HibernateUtil()
            hibernateUtil.setUpSession()
            //hibernateUtil.addRefreshToken()
            //logger.info("HERE")
            //            val response = JsonObject()
//            val credentials = call.receive< ru.rtuitlab.account.authorization.UserCredential>()
//
//            val user = userSource.findUserByCredentials(credentials)
//            val accessToken = JwtConfig.makeAccessToken(user)
//            val refreshToken = JwtConfig.makeRefreshToken(user)
//            response.addProperty("accessToken", accessToken)
//            response.addProperty("refreshToken", refreshToken)
//            response.addProperty("expiresIn", JWT.decode(accessToken).expiresAt.time)
//
//            call.respond(response)

        }


//        get("optional") {
//            val hibernateUtil = HibernateUtil()
//            hibernateUtil.setUpSession()
//            hibernateUtil.addRefreshToken()
//            //val user = call.user
//            //val response = if (user != null) "authenticated!" else "optional"
//            call.respond("OK")
//        }

        /**
         * All [Route]s in the authentication block are secured.
         */
//        authenticate {
//            route("secret") {
//                get {
//                    //val user = call.user!!
//                    //call.respond(user.countries)
//                }
//
//                put {
//                    println("PUT")
//                    TODO("All your secret routes can follow here")
//                }
//
//            }
//        }

//        /**
//         * Routes with optional authentication
//         */
//        authenticate(optional = true) {
//            get("optional") {
//                val hibernateUtil = HibernateUtil()
//                hibernateUtil.setUpSession()
//                hibernateUtil.addRefreshToken()
//                //val user = call.user
//                //val response = if (user != null) "authenticated!" else "optional"
//                call.respond("OK")
//            }
//        }
    }
}