package se.uulm.snowballr.backend.env

class EnvVariableNotFoundException(key: String) : Exception(
    "The env variable with key '$key' could not be found. Please check the variables.",
)
