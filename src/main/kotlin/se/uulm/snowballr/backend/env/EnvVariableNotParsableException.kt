package se.uulm.snowballr.backend.env

class EnvVariableNotParsableException(key: String, value: Any) : Exception(
    "Invalid value '$value' for env variable with key '$key'. Please check the variables.",
)
