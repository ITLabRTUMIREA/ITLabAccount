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
import database.user.UserPropertyType
import io.ktor.application.*
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

    //TODO: insert my RefreshToken generator



    val hibernateUtil = HibernateUtil().setUpSession()
    install(Routing) {

        post("api/database/connect") {
            hibernateUtil.setUpSession()
        }

        get("api/database/RefreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            val jsonObject = if (id != null) {
                val token = when (id) {
                    0 -> hibernateUtil.getEntities(RefreshToken())
                    else -> hibernateUtil.getEntity(id, RefreshToken())
                }
                if (token != null) {
                    val jsonElement =
                        JsonParser().parse(Gson().toJson(token)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is gotten successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 61)
                    logger.error("Entity userPropertyStatus with id = $id is not gotten")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
        }

        post("api/database/addRefreshToken") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val id = if (tmp == null) {
                0
            } else {
                val param = LinkedHashMap<String, String>()
                tmp.entrySet().forEach {
                    param[it.key] = it.value.asString
                }
                val token = token.Token()
                val tokenGen = token.getTokenGen()
                param.forEach { key, value ->
                    run {
                        tokenGen.addPar(key, value)
                    }
                }
                tokenGen.createToken("123")
                hibernateUtil.addRefreshToken(token.tokenStr)
            }
            val result = JsonObject()
            if (id != 0) {
                logger.info("RefreshToken added to postgres database with id = $id")
                result.addProperty("id", id)
                result.addProperty("statusCode", 1)
            } else {
                logger.error("Can't add entity RefreshToken to postgres database")
                result.addProperty("statusCode", 63)
            }
            call.respond(result)
        }

        delete("api/database/RefreshToken") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            val jsonObject = if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, RefreshToken())
                if (resultOfDeleting) {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("id", id)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 64)
                    logger.error("Entity userPropertyStatus with id = $id is not deleted")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
        }

        get("api/database/userPropertyStatus") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            val jsonObject = if (id != null) {
                val userProperty = when (id) {
                    0 -> hibernateUtil.getEntities(UserPropertyStatus())
                    else -> hibernateUtil.getEntity(id, UserPropertyStatus())
                }
                if (userProperty != null) {
                    val jsonElement =
                        JsonParser().parse(Gson().toJson(userProperty)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is gotten successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 61)
                    logger.error("Entity userPropertyStatus with id = $id is not gotten")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
        }

        post("api/database/addUserPropertyStatus") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val value = tmp?.get("statusValue")
            val id = when (value) {
                null -> 0
                else -> hibernateUtil.addUserPropertyStatus(value.asString)
            }
            val result = JsonObject()
            if (id != 0) {
                logger.info("UserPropertyStatus added to postgres database with id = $id")
                result.addProperty("id", id)
                result.addProperty("statusCode", 1)
            } else {
                logger.error("Can't add entity UserPropertyStatus to postgres database")
                result.addProperty("statusCode", 63)
            }
            call.respond(result)
        }

        delete("api/database/userPropertyStatus") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            val jsonObject = if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyStatus())
                if (resultOfDeleting) {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("id", id)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 64)
                    logger.error("Entity userPropertyStatus with id = $id is not deleted")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
        }

        get("/api/database/userPropertyType") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }

            val jsonObject = if (id != null) {
                val userProperty = when (id) {
                    0 -> hibernateUtil.getEntities(UserPropertyType())
                    else -> hibernateUtil.getEntity(id, UserPropertyType())
                }
                if (userProperty != null) {
                    val jsonElement =
                        JsonParser().parse(Gson().toJson(userProperty)) as JsonElement
                    val jsonObject = JsonObject()
                    jsonObject.add("data", jsonElement)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is gotten successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 61)
                    logger.error("Entity userPropertyStatus with id = $id is not gotten")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
        }

        post("/api/database/addUserPropertyType") {
            val tmp: JsonObject? =
                Gson().fromJson(InputStreamReader(call.receiveStream(), "UTF-8"), JsonObject::class.java)
            val name = tmp?.get("propertyName")
            val description = tmp?.get("propertyDescription")
            val id = if (name == null || description == null) {
                0
            } else {
                hibernateUtil.addUserPropertyType(name.asString, description.asString)
            }
            val result = JsonObject()
            if (id != 0) {
                logger.info("UserPropertyType added to postgres database with id = $id")
                result.addProperty("id", id)
                result.addProperty("statusCode", 1)
            } else {
                logger.error("Can't add entity UserPropertyType to postgres database")
                result.addProperty("statusCode", 63)
            }
            call.respond(result)
        }

        delete("api/database/userPropertyType") {
            val id = when (val a = call.parameters["id"]) {
                "all", "0", "*" -> 0
                else -> a?.toIntOrNull()
            }
            val jsonObject = if (id != null) {
                val resultOfDeleting = hibernateUtil.deleteEntities(id, UserPropertyType())
                if (resultOfDeleting) {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("id", id)
                    jsonObject.addProperty("statusCode", 1)
                    logger.info("Entity userPropertyStatus with id = $id is deleted successfully")
                    jsonObject
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("statusCode", 64)
                    logger.error("Entity userPropertyStatus with id = $id is not deleted")
                    jsonObject
                }
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty("statusCode", 62)
                logger.error("Parameter id is incorrect")
                jsonObject
            }
            call.respond(jsonObject)
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