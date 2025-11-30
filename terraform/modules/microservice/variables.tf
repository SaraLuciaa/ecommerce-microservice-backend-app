variable "namespace" {
  type    = string
  description = "Kubernetes namespace where the service will be deployed"
}

variable "name" {
  description = "Name of the microservice"
  type        = string
}

variable "image" {
  description = "Docker image for the microservice"
  type        = string
}

variable "replicas" {
  description = "Number of replicas"
  type        = number
  default     = 1
}

variable "ports" {
  description = "List of ports to expose"
  type        = list(number)
  default     = [8080]
}

variable "env_vars" {
  description = "Map of environment variables"
  type        = map(string)
  default     = {}
}

variable "service_type" {
  description = "Kubernetes service type"
  type        = string
  default     = "ClusterIP"
}
