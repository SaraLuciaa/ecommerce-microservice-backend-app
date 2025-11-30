variable "namespace" {
  type    = string
  description = "Kubernetes namespace where the service will be deployed"
  default = "default"
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

variable "memory_limit" {
  description = "Memory limit for the container"
  type        = string
  default     = "512Mi"
}

variable "memory_request" {
  description = "Memory request for the container"
  type        = string
  default     = "256Mi"
}

variable "cpu_limit" {
  description = "CPU limit for the container"
  type        = string
  default     = "250m"
}

variable "cpu_request" {
  description = "CPU request for the container"
  type        = string
  default     = "100m"
}
