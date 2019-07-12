package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.AppConfig
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.getInt
import ru.lebe.dev.mrjanitor.util.getString
import java.io.File
import java.nio.file.Paths

class ReadConfigFromFile {
    private val log = LoggerFactory.getLogger(javaClass)

    fun read(file: File): Either<OperationResult, AppConfig> {
        log.info("read configuration from file '${file.absolutePath}'")

        return if (file.exists()) {

            try {
                val config = ConfigFactory.parseFile(file).getConfig("config")

                when(val defaultProfile = loadProfile(config, Defaults.PROFILE_NAME)) {
                    is Either.Right -> {

                        when(val profiles = loadProfiles(config)) {
                            is Either.Right -> {
                                Either.right(
                                    AppConfig(
                                        defaultProfile = defaultProfile.b,
                                        profiles = profiles.b
                                    )
                                )
                            }
                            is Either.Left -> {
                                log.error("unable to load profiles")
                                Either.left(OperationResult.ERROR)
                            }
                        }
                    }
                    is Either.Left -> {
                        log.error("unable to load default profile")
                        defaultProfile
                    }

                }

            } catch (e: ConfigException.Missing) {
                log.error("missing property: ${e.message}", e)
                Either.left(OperationResult.ERROR)
            }

        } else {
            log.error("config file wasn't found")
            Either.left(OperationResult.ERROR)
        }
    }

    private fun loadProfiles(config: Config): Either<OperationResult, List<Profile>> =
        if (config.hasPath("profiles")) {

            val profiles = arrayListOf<Profile>()

            var profileLoadError = false

            config.getStringList("profiles").forEach { profileName ->
                when(val profile = loadProfile(config, profileName)) {
                    is Either.Right -> {

                        if (isProfileValid(profile.b)) {
                            profiles += profile.b

                        } else {
                            log.error("invalid profile configuration: '$profileName'")
                        }
                    }
                    is Either.Left -> {
                        log.error("unable to load profile '$profileName'")
                        profileLoadError = true
                    }
                }
            }

            if (!profileLoadError) {
                Either.right(profiles)

            } else {
                Either.left(OperationResult.ERROR)
            }

        } else {
            log.error("'profiles' property hasn't found")
            Either.left(OperationResult.ERROR)
        }

    private fun loadProfile(config: Config, profileName: String): Either<OperationResult, Profile> =

        if (config.hasPath(profileName)) {
            Either.right(
                Profile(
                    name = profileName,
                    path = config.getString("$profileName.path", ""),
                    storageUnit = getStorageUnit(config, profileName),
                    keepCopies = config.getInt("$profileName.keep-copies", 7),
                    cleanAction = getCleanAction(config, profileName, CleanAction.JUST_NOTIFY)
                )
            )

        } else {
            log.error("profile '$profileName' wasn't found at path '$profileName'")
            Either.left(OperationResult.ERROR)
        }

    private fun getCleanAction(config: Config, profilePath: String, defaultValue: CleanAction): CleanAction =
        if (config.hasPath("$profilePath.action")) {
            when(config.getString("$profilePath.action")) {
                "compress" -> CleanAction.COMPRESS
                "remove" -> CleanAction.REMOVE
                else -> defaultValue
            }

        } else {
            defaultValue
        }

    private fun getStorageUnit(config: Config, profilePath: String): StorageUnit =
        when(config.getString("$profilePath.unit", "")) {
            "directory" -> StorageUnit.DIRECTORY
            else -> StorageUnit.FILE
        }

    private fun isProfileValid(profile: Profile): Boolean {
        var result = true

        if (profile.keepCopies < 1) {
            log.error("invalid keep-copies value '${profile.keepCopies}', should be > 0")
            result = false
        }

        if (!Paths.get(profile.path).toFile().exists()) {
            log.error("path '${profile.path}' doesn't exist")
            result = false
        }

        return result
    }

}
