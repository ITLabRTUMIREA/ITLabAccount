@file:Suppress("IMPLICIT_CAST_TO_ANY")

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import database.user.UserPropertyStatus
import io.ktor.features.ContentNegotiation
import io.ktor.application.Application
import io.ktor.gson.*
import io.ktor.application.install
import io.ktor.routing.*
import org.slf4j.LoggerFactory
import com.google.gson.JsonObject
import database.user.RefreshToken
import database.user.UserProperty
import database.user.UserPropertyType
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.InputStreamReader
import io.ktor.request.receiveStream
import io.ktor.response.header

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

    val hibernateUtil = HibernateUtil().setUpSession()
    install(Routing) {

        post("api/database/connect") {
            hibernateUtil.setUpSession()
        }

        post("api/database/addRefreshToken") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            if (tmp != null) {
                val param = HashMap<String, String>()
                tmp.entrySet().map { param[it.key] = it.value.asString }

                val token = token.Token()
                val tokenGen = token.getTokenGen()

                //TODO: ADD FUNCTION FOR ADDING ALL MAP
                param.forEach { key, value ->
                    run {
                        tokenGen.addPar(key, value)
                    }
                }

                //TODO: JUST FOR TESTING! ITs must be random
                tokenGen.createToken("123")
                val id = hibernateUtil.addRefreshToken(token.tokenStr)

                //TODO: if id = 0 error adding token!!!
                logger.info("RefreshToken added to postgres database with id = $id")

                call.response.status(HttpStatusCode.OK)
            } else {
                logger.error("Can't add entity RefreshToken to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        get("api/database/refreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {

                val refreshToken = when (id) {
                    0 -> hibernateUtil.getEntities(RefreshToken())
                    else -> hibernateUtil.getEntity(id, RefreshToken())
                }

                if (refreshToken != null) {
                    val jsonElement = JsonParser().parse(Gson().toJson(refreshToken)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    logger.info("Entity RefreshToken with id = $id is gotten successfully")
                    call.respond(jsonObject)
                } else {
                    logger.error("Entity RefreshToken with id = $id is not gotten (NOT FOUND)")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        put("api/database/refreshToken") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                var refreshToken = hibernateUtil.getEntity(id, RefreshToken())
                if (refreshToken != null) {
                    val tmp: JsonObject? =
                        Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
                    val param = HashMap<String, String>()
                    if (tmp != null) {
                        tmp.entrySet().map { param[it.key] = it.value.asString }

                        val token = token.Token()
                        val tokenGen = token.getTokenGen()
                        param.forEach { key, value ->
                            run {
                                tokenGen.addPar(key, value)
                            }
                        }
                        tokenGen.createToken("123")
                        println(token.tokenStr)
                        refreshToken = refreshToken.copy(token = token.tokenStr)
                        if (hibernateUtil.updateEntity(refreshToken)) {
                            logger.info("Entity RefreshToken with id = $id is updated successfully")
                            call.response.status(HttpStatusCode.OK)
                        }
                    } else {
                        logger.info("Entity RefreshToken with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
                    }
                } else {
                    logger.info("Entity RefreshToken with id = $id is not found")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        delete("api/database/refreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, RefreshToken())
                if (resultOfDeleting) {
                    logger.info("Entity RefreshToken with id = $id is deleted successfully")
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("Entity RefreshToken with id = $id is not deleted")
                    call.response.status(HttpStatusCode.NotFound)
                }
            } else {
                logger.error("Parameter id is incorrect")
                call.response.status(HttpStatusCode.BadRequest)
            }
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
                result.addProperty("id", id)
                logger.info("UserPropertyStatus added to postgres database with id = $id")
                call.respond(result)
                call.response.status(HttpStatusCode.OK)
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
                    }
                    if (hibernateUtil.updateEntity(propertyStatus)) {
                        logger.info("Entity userPropertyStatus with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userPropertyStatus with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
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
            val name = tmp?.get("name")
            val description = tmp?.get("description")
            if (name != null) {
                val result = JsonObject()
                val id = hibernateUtil.addUserPropertyType(name.asString, description?.asString)
                result.addProperty("id", id)
                logger.info("UserPropertyType added to postgres database with id = $id")
                call.respond(result)
                call.response.status(HttpStatusCode.OK)
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
                    val name = tmp?.get("name")
                    val description = tmp?.get("description")
                    if (name != null) {
                        propertyType = propertyType.copy(name = name.asString)
                    }
                    if (description != null) {
                        propertyType = propertyType.copy(description = description.asString)
                    }
                    if (hibernateUtil.updateEntity(propertyType)) {
                        logger.info("Entity userPropertyType with id = $id is updated successfully")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        logger.info("Entity userPropertyType with id = $id is updated unsuccessfully")
                        call.response.status(HttpStatusCode.Conflict)
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

        get("api/database/UserProperty") {
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

        post("api/database/addFullUserProperty") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

            val value = tmp?.get("value")
            val propertyType = tmp?.get("UserPropertyType")
            val propertyStatus = tmp?.get("UserPropertyStatus")

            if (value != null && propertyType != null && propertyStatus != null) {

                val userPropertyType = Gson().fromJson(propertyType, UserPropertyType()::class.java)
                val userPropertyStatus = Gson().fromJson(propertyStatus, UserPropertyStatus()::class.java)
                val id = hibernateUtil.addUserProperty(value.asString, userPropertyType, userPropertyStatus)
                val result = JsonObject()

                call.respond(result)
                logger.info("UserProperty added to postgres database with id = $id")
                call.response.status(HttpStatusCode.OK)
            } else {
                logger.error("Can't add entity UserProperty to postgres database")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }

        post("api/database/addUserProperty") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)

            val value = tmp?.get("value")
            val typeId = tmp?.get("typeId")
            val statusId = tmp?.get("statusId")

            if (value != null && typeId != null && statusId != null) {
                val userPropertyType = hibernateUtil.getEntity(typeId.asInt, UserPropertyType())
                val userPropertyStatus = hibernateUtil.getEntity(statusId.asInt, UserPropertyStatus())
                if (userPropertyType != null && userPropertyStatus != null) {
                    val result = JsonObject()

                    val id = hibernateUtil.addUserProperty(
                        value.asString, userPropertyType, userPropertyStatus
                    )

                    result.addProperty("id", id)
                    logger.info("UserProperty added to postgres database with id = $id")
                    call.respond(result)
                    call.response.status(HttpStatusCode.OK)
                } else {
                    logger.error("id of userPropertyType or userPropertyStatus is invalid")
                    call.response.status(HttpStatusCode.NotFound)
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