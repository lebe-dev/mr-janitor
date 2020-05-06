package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.AppConfig
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.CleanUpPolicy
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.Defaults.DIRECTORY_NAME_FILTER_PATTERN
import ru.lebe.dev.mrjanitor.util.Defaults.FILENAME_FILTER_PATTERN
import ru.lebe.dev.mrjanitor.util.Defaults.PROFILE_NAME
import ru.lebe.dev.mrjanitor.util.getInt
import ru.lebe.dev.mrjanitor.util.getString
import java.io.File
import java.nio.file.Paths

class ReadConfigFromFile {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PROFILES_SECTION = "profiles"

        private const val ITEM_VALIDATION_SECTION = "item-validation"
    }

    fun read(file: File): OperationResult<AppConfig> {
        log.info("read configuration from file '${file.absolutePath}'")

        return if (file.exists()) {

            try {
                val config = ConfigFactory.parseFile(file).getConfig("config")

                getAppConfig(config)

            } catch (e: ConfigException.Missing) {
                log.error("missing property: ${e.message}", e)
                Either.left(OperationError.ERROR)
            }

        } else {
            log.error("config file wasn't found")
            Either.left(OperationError.ERROR)
        }
    }

    private fun getAppConfig(config: Config) =
        when(val defaultProfile = loadProfile(config, PROFILE_NAME, getInternalDefaultProfile())) {
            is Either.Right -> {

                when(val profiles = loadProfiles(config, defaultProfile.b)) {
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
                        Either.left(OperationError.ERROR)
                    }
                }
            }
            is Either.Left -> {
                log.error("unable to load default profile")
                Either.left(OperationError.ERROR)
            }

        }

    private fun getInternalDefaultProfile() = Profile(
        name = PROFILE_NAME,
        path = ".", storageUnit = Defaults.DEFAULT_STORAGE_UNIT,
        keepItemsQuantity = Defaults.DEFAULT_KEEP_COPIES,
        fileNameFilter = Regex(FILENAME_FILTER_PATTERN),
        directoryNameFilter = Regex(DIRECTORY_NAME_FILTER_PATTERN),
        fileItemValidationConfig = FileItemValidationConfig(
            sizeAtLeastAsPrevious = true,
            md5FileCheck = true, zipTest = true, logFileExists = true,
            useCustomValidator = false, customValidatorCommand = ""
        ),
        directoryItemValidationConfig = DirectoryItemValidationConfig(
            sizeAtLeastAsPrevious = true, filesQtyAtLeastAsInPrevious = true,
            fileSizeAtLeastAsInPrevious = true
        ),
        cleanUpPolicy = CleanUpPolicy(
            invalidItemsBeyondOfKeepQuantity = true,
            allInvalidItems = false
        ),
        cleanAction = CleanAction.JUST_NOTIFY
    )

    private fun loadProfiles(config: Config, defaultProfile: Profile): OperationResult<List<Profile>> =
        if (config.hasPath(PROFILES_SECTION)) {

            val profiles = arrayListOf<Profile>()

            var profileLoadError = false

            config.getStringList(PROFILES_SECTION).forEach { profileName ->
                when(val profile = loadProfile(config, profileName, defaultProfile)) {
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
                Either.left(OperationError.ERROR)
            }

        } else {
            log.error("'$PROFILES_SECTION' property wasn't found")
            Either.left(OperationError.ERROR)
        }

    private fun isProfileValid(profile: Profile): Boolean {
        var result = true

        if (profile.keepItemsQuantity < 1) {
            log.error("invalid keep-copies value '${profile.keepItemsQuantity}', should be > 0")
            result = false
        }

        if (!Paths.get(profile.path).toFile().exists()) {
            log.error("path '${profile.path}' doesn't exist")
            result = false
        }

        return result
    }

    private fun loadProfile(config: Config, profileName: String,
                            defaultProfile: Profile): OperationResult<Profile> =

        if (config.hasPath(profileName)) {
            Either.right(
                Profile(
                    name = profileName,
                    path = config.getString("$profileName.path", ""),
                    storageUnit = getStorageUnit(config, profileName, defaultProfile.storageUnit),
                    directoryNameFilter = Regex(
                        config.getString(
                        "$profileName.directory-name-filter", defaultProfile.directoryNameFilter.pattern
                        )
                    ),
                    fileNameFilter = Regex(
                        config.getString("$profileName.file-name-filter", defaultProfile.fileNameFilter.pattern)
                    ),
                    keepItemsQuantity = config.getInt(
                        "$profileName.keep-items-quantity", defaultProfile.keepItemsQuantity
                    ),
                    fileItemValidationConfig = getFileItemValidationConfig(
                        config, "$profileName.$ITEM_VALIDATION_SECTION.file",
                        defaultProfile.fileItemValidationConfig
                    ),
                    directoryItemValidationConfig = getDirectoryItemValidationConfig(
                        config, "$profileName.$ITEM_VALIDATION_SECTION.directory",
                        defaultProfile.directoryItemValidationConfig
                    ),
                    cleanUpPolicy = getCleanUpPolicy(
                        config, "$profileName.cleanup", defaultProfile.cleanUpPolicy
                    ),
                    cleanAction = getCleanAction(config, profileName)
                )
            )

        } else {
            log.error("profile '$profileName' wasn't found at path '$profileName'")
            Either.left(OperationError.ERROR)
        }

    private fun getDirectoryItemValidationConfig(config: Config, sectionPath: String,
                             defaultValidationConfig: DirectoryItemValidationConfig): DirectoryItemValidationConfig {

        return if (config.hasPath(sectionPath)) {

            DirectoryItemValidationConfig(
                sizeAtLeastAsPrevious = getBooleanPropertyValue(
                    config, "$sectionPath.size-at-least-as-previous",
                    defaultValidationConfig.sizeAtLeastAsPrevious
                ),
                filesQtyAtLeastAsInPrevious = getBooleanPropertyValue(
                    config, "$sectionPath.files-qty-at-least-as-in-previous",
                    defaultValidationConfig.filesQtyAtLeastAsInPrevious
                ),
                fileSizeAtLeastAsInPrevious = getBooleanPropertyValue(
                    config, "$sectionPath.file-size-at-least-as-in-previous",
                    defaultValidationConfig.filesQtyAtLeastAsInPrevious
                )
            )

        } else {
            defaultValidationConfig
        }
    }

    private fun getFileItemValidationConfig(config: Config, sectionPath: String,
                                        defaultValidationConfig: FileItemValidationConfig): FileItemValidationConfig {

        return if (config.hasPath(sectionPath)) {

            FileItemValidationConfig(
                sizeAtLeastAsPrevious = getBooleanPropertyValue(
                    config, "$sectionPath.size-at-least-as-previous",
                    defaultValidationConfig.sizeAtLeastAsPrevious
                ),
                md5FileCheck = getBooleanPropertyValue(
                    config, "$sectionPath.md5-file-check", defaultValidationConfig.md5FileCheck
                ),
                zipTest = getBooleanPropertyValue(
                    config, "$sectionPath.zip-test", defaultValidationConfig.zipTest
                ),
                logFileExists = getBooleanPropertyValue(
                    config, "$sectionPath.log-file-exists", defaultValidationConfig.logFileExists
                ),
                useCustomValidator = getBooleanPropertyValue(
                    config, "$sectionPath.use-custom-validator", defaultValidationConfig.useCustomValidator
                ),
                customValidatorCommand = config.getString(
                "$sectionPath.custom-validator-command", defaultValidationConfig.customValidatorCommand
                )
            )

        } else {
            defaultValidationConfig
        }
    }

    private fun getCleanUpPolicy(config: Config, sectionPath: String,
                                 defaultCleanUpPolicy: CleanUpPolicy): CleanUpPolicy {

        return if (config.hasPath(sectionPath)) {
            CleanUpPolicy(
                invalidItemsBeyondOfKeepQuantity = getBooleanPropertyValue(
                    config, "$sectionPath.invalid-items-beyond-of-keep-quantity",
                    defaultCleanUpPolicy.invalidItemsBeyondOfKeepQuantity
                ),
                allInvalidItems = getBooleanPropertyValue(
                    config, "$sectionPath.all-invalid-items",
                    defaultCleanUpPolicy.allInvalidItems
                )
            )

        } else {
            defaultCleanUpPolicy
        }
    }

    private fun getBooleanPropertyValue(config: Config, propertyPath: String, defaultValue: Boolean): Boolean =
        if (config.hasPath(propertyPath)) {
            config.getBoolean(propertyPath)

        } else {
            defaultValue
        }

    private fun getStorageUnit(config: Config, profilePath: String, defaultValue: StorageUnit): StorageUnit =
        when(config.getString("$profilePath.unit", "")) {
            StorageUnit.DIRECTORY.toString().toLowerCase() -> StorageUnit.DIRECTORY
            StorageUnit.FILE.toString().toLowerCase() -> StorageUnit.FILE
            else -> defaultValue
        }

    private fun getCleanAction(config: Config, profilePath: String): CleanAction {
        val actionPropertyPath = "$profilePath.action"

        return if (config.hasPath(actionPropertyPath)) {
            when (config.getString(actionPropertyPath)) {
                CleanAction.REMOVE.toString().toLowerCase() -> CleanAction.REMOVE
                else -> {
                    log.warn("unsupported action, use default action 'notify'")
                    CleanAction.JUST_NOTIFY
                }
            }

        } else {
            CleanAction.JUST_NOTIFY
        }
    }

}
